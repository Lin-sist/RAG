#!/usr/bin/env python3
from __future__ import annotations

import unittest
from copy import deepcopy
import json
import tempfile
from pathlib import Path

import compare_reranker_ab as comparator


class CompareRerankerAbTest(unittest.TestCase):
    def test_clean_fixed_identity_arms_are_comparable(self) -> None:
        heuristic = [self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)]
        model = [self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)]

        result = comparator.compare_runs(heuristic, model)

        self.assertEqual("COMPARABLE", result["comparisonStatus"])
        self.assertEqual([], result["comparisonReasons"])
        self.assertAlmostEqual(0.1, result["deltas"]["recallAt5"])
        self.assertAlmostEqual(0.05, result["deltas"]["mrr"])
        self.assertEqual(1.0, result["armSummaries"]["model"]["eligibleModelCoverage"])
        self.assertEqual(30.0, result["armSummaries"]["model"]["rerankLatencyMillis"]["p50"])

    def test_identity_mismatch_is_not_comparable_and_suppresses_deltas(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        model["runMetadata"]["git"]["head"] = "b" * 40

        result = comparator.compare_runs([heuristic], [model])

        self.assertEqual("NOT_COMPARABLE", result["comparisonStatus"])
        self.assertIn("identity_mismatch", result["comparisonReasons"])
        self.assertIsNone(result["deltas"])

    def test_model_fallback_is_not_comparable_and_does_not_compare_success_subset(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        attribution = model["samples"][0]["rerankAttribution"]
        attribution.update({
            "effectiveProvider": "heuristic",
            "fallbackCount": 1,
            "fallbackReason": "rate_limit",
            "modelCallCount": 0,
        })

        result = comparator.compare_runs([heuristic], [model])

        self.assertEqual("NOT_COMPARABLE", result["comparisonStatus"])
        self.assertIn("model_fallback_observed", result["comparisonReasons"])
        self.assertIsNone(result["deltas"])
        self.assertEqual({"rate_limit": 1}, result["diagnostics"]["fallbackReasonHistogram"])

    def test_zero_candidate_pair_is_not_applicable_but_one_sided_zero_is_invalid(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0, mrr=0, top1=0, rerank_ms=0, retrieve_ms=80)
        model = self.make_run("model", recall5=0, mrr=0, top1=0, rerank_ms=0, retrieve_ms=115)
        for run in (heuristic, model):
            run["samples"][0]["rerankAttribution"].update({
                "candidateCount": 0,
                "scoredCount": 0,
                "candidateCoverage": 0.0,
                "modelCallCount": 0,
            })

        clean = comparator.compare_runs([heuristic], [model])
        self.assertEqual("COMPARABLE", clean["comparisonStatus"])
        self.assertEqual(1, clean["diagnostics"]["notApplicablePairCount"])
        self.assertEqual(1.0, clean["armSummaries"]["model"]["eligibleModelCoverage"])

        drifted = deepcopy(model)
        drifted["samples"][0]["rerankAttribution"].update({
            "candidateCount": 10,
            "scoredCount": 10,
            "candidateCoverage": 1.0,
            "modelCallCount": 1,
        })
        invalid = comparator.compare_runs([heuristic], [drifted])
        self.assertEqual("NOT_COMPARABLE", invalid["comparisonStatus"])
        self.assertIn("zero_candidate_mismatch", invalid["comparisonReasons"])

    def test_missing_pair_and_retrieve_error_are_reported(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        model["samples"] = []
        model["runCounts"]["retrieveErrors"] = 1

        result = comparator.compare_runs([heuristic], [model])

        self.assertEqual("NOT_COMPARABLE", result["comparisonStatus"])
        self.assertIn("sample_pair_mismatch", result["comparisonReasons"])
        self.assertIn("retrieve_error", result["comparisonReasons"])
        self.assertEqual(["1:fact-001"], result["diagnostics"]["missingModelPairs"])

    def test_details_must_match_declared_sample_selection_and_sample_errors(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        for run in (heuristic, model):
            run["samples"][0]["id"] = "undeclared-001"
        model["samples"][0]["errors"]["retrieval"] = "timed out"

        result = comparator.compare_runs([heuristic], [model])

        self.assertEqual("NOT_COMPARABLE", result["comparisonStatus"])
        self.assertIn("sample_pair_mismatch", result["comparisonReasons"])
        self.assertIn("retrieve_error", result["comparisonReasons"])

    def test_incomplete_measured_repeats_are_not_comparable(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        for run in (heuristic, model):
            run["runMetadata"]["repeat"]["total"] = 3
            run["runMetadata"]["armManifest"]["measuredRepeats"] = 3

        result = comparator.compare_runs([heuristic], [model])

        self.assertEqual("NOT_COMPARABLE", result["comparisonStatus"])
        self.assertIn("run_count_mismatch", result["comparisonReasons"])

    def test_observed_model_identity_must_match_manifest(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        model["samples"][0]["rerankAttribution"]["model"] = "nvidia/unapproved"

        result = comparator.compare_runs([heuristic], [model])

        self.assertEqual("NOT_COMPARABLE", result["comparisonStatus"])
        self.assertIn("manifest_observation_mismatch", result["comparisonReasons"])
        self.assertIsNone(result["deltas"])

    def test_invalid_schema_fails_instead_of_becoming_not_comparable(self) -> None:
        result = comparator.compare_runs([{"reportStatus": "RETRIEVAL_ONLY"}], [{"reportStatus": "RETRIEVAL_ONLY"}])

        self.assertEqual("FAILED", result["comparisonStatus"])
        self.assertEqual(["invalid_evidence_schema"], result["comparisonReasons"])
        self.assertIsNone(result["deltas"])

    def test_compact_result_does_not_copy_question_context_or_raw_response(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        heuristic["samples"][0].update({
            "question": "SECRET_QUESTION",
            "contexts": ["SECRET_CONTEXT"],
            "rawResponse": {"payload": "SECRET_RAW"},
        })

        rendered = json.dumps(comparator.compare_runs([heuristic], [model]))

        self.assertNotIn("SECRET_QUESTION", rendered)
        self.assertNotIn("SECRET_CONTEXT", rendered)
        self.assertNotIn("SECRET_RAW", rendered)

    def test_per_sample_summary_reports_cross_repeat_medians_and_paired_delta(self) -> None:
        h1 = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        h2 = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=4, retrieve_ms=100)
        m1 = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        m2 = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=50, retrieve_ms=135)
        for run, index in ((h1, 1), (h2, 2), (m1, 1), (m2, 2)):
            run["runMetadata"]["repeat"] = {"index": index, "total": 2}
            run["runMetadata"]["armManifest"]["measuredRepeats"] = 2

        result = comparator.compare_runs([h1, h2], [m1, m2])

        summary = result["perSampleSummaries"][0]
        self.assertEqual("fact-001", summary["sampleId"])
        self.assertEqual(3.0, summary["rerankLatencyMillis"]["heuristicMedian"])
        self.assertEqual(40.0, summary["rerankLatencyMillis"]["modelMedian"])
        self.assertEqual(37.0, summary["rerankLatencyMillis"]["pairedDeltaMedian"])
        self.assertEqual(35.0, summary["retrieveLatencyMillis"]["pairedDeltaMedian"])

    def test_cli_writes_compact_json_and_markdown(self) -> None:
        heuristic = self.make_run("heuristic", recall5=0.6, mrr=0.7, top1=0.8, rerank_ms=2, retrieve_ms=80)
        model = self.make_run("model", recall5=0.7, mrr=0.75, top1=0.9, rerank_ms=30, retrieve_ms=115)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            heuristic_path = root / "heuristic.json"
            model_path = root / "model.json"
            output_json = root / "comparison.json"
            output_md = root / "comparison.md"
            heuristic_path.write_text(json.dumps(heuristic), encoding="utf-8")
            model_path.write_text(json.dumps(model), encoding="utf-8")

            exit_code = comparator.main([
                "--heuristic-details", str(heuristic_path),
                "--model-details", str(model_path),
                "--output-json", str(output_json),
                "--output-markdown", str(output_md),
            ])

            self.assertEqual(0, exit_code)
            output = json.loads(output_json.read_text(encoding="utf-8"))
            self.assertEqual("COMPARABLE", output["comparisonStatus"])
            self.assertEqual("c7-reranker-ab-comparison-v1", output["comparisonSchema"])
            self.assertEqual(2, len(output["sourceFiles"]))
            self.assertRegex(output["sourceFiles"][0]["sha256"], r"^[0-9a-f]{64}$")
            markdown = output_md.read_text(encoding="utf-8")
            self.assertIn("Comparison status: COMPARABLE", markdown)
            self.assertIn("Model eligible coverage", markdown)
            self.assertIn("debug retrieval upper bound", markdown)

    def test_cli_writes_failed_evidence_for_unreadable_schema(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            heuristic_path = root / "broken.json"
            model_path = root / "model.json"
            output_json = root / "comparison.json"
            output_md = root / "comparison.md"
            heuristic_path.write_text("{broken", encoding="utf-8")
            model_path.write_text("{}", encoding="utf-8")

            exit_code = comparator.main([
                "--heuristic-details", str(heuristic_path),
                "--model-details", str(model_path),
                "--output-json", str(output_json),
                "--output-markdown", str(output_md),
            ])

            self.assertEqual(2, exit_code)
            result = json.loads(output_json.read_text(encoding="utf-8"))
            self.assertEqual("FAILED", result["comparisonStatus"])
            self.assertEqual(["invalid_evidence_schema"], result["comparisonReasons"])
            self.assertNotIn("broken", json.dumps(result))

    @staticmethod
    def make_run(
        arm_id: str,
        *,
        recall5: float,
        mrr: float,
        top1: float,
        rerank_ms: int,
        retrieve_ms: int,
    ) -> dict:
        provider = "heuristic" if arm_id == "heuristic" else "nvidia"
        model = "" if arm_id == "heuristic" else "nvidia/test"
        protocol = "heuristic-v1" if arm_id == "heuristic" else "nvidia-ranking-v1"
        metadata = {
            "evaluationSchema": "c7-reranker-ab-v1",
            "evalSetIdentity": {"path": "eval.jsonl", "sha256": "e" * 64, "bytes": 10},
            "sampleSelection": {"ids": ["fact-001"], "count": 1},
            "knowledgeBase": {
                "id": 7,
                "name": "eval",
                "description": "marker",
                "vectorCollection": "kb_eval",
                "documentCount": 1,
                "chunkCount": 10,
                "documents": [{"id": 1, "title": "fixture.md", "status": "COMPLETED", "chunkCount": 10, "contentHash": "h"}],
            },
            "topK": 5,
            "minScore": 0.3,
            "enableRerank": True,
            "configSnapshot": {"application.yml": {"sha256": "c" * 64, "bytes": 20}},
            "git": {"head": "a" * 40},
            "repeat": {"index": 1, "total": 1},
            "warmup": {"calls": 0},
            "armManifest": {
                "schemaVersion": "c7-reranker-ab-v1",
                "armId": arm_id,
                "expectedRequestedProvider": provider,
                "expectedEffectiveProvider": provider,
                "model": model,
                "protocol": protocol,
                "measuredRepeats": 1,
                "warmupCalls": 0,
            },
        }
        attribution = {
            "requestedProvider": provider,
            "effectiveProvider": provider,
            "fallbackCount": 0,
            "fallbackReason": "none",
            "modelCallCount": 0 if arm_id == "heuristic" else 1,
            "candidateCount": 10,
            "scoredCount": 10,
            "candidateCoverage": 1.0,
            "latencyMillis": rerank_ms,
            "model": model,
            "protocol": protocol,
        }
        return {
            "reportStatus": "RETRIEVAL_ONLY",
            "runCounts": {"retrieveErrors": 0, "askErrors": 0, "skippedAsk": 1, "retryCount": 0, "rateLimitErrors": 0},
            "runMetadata": metadata,
            "metrics": {
                "recall_at_5": recall5,
                "mrr": mrr,
                "top1_source_accuracy": top1,
            },
            "samples": [{
                "id": "fact-001",
                "retrieveLatencyMillis": retrieve_ms,
                "rerankAttribution": attribution,
                "metricCalculationDetails": {
                    "recall5Hits": 1,
                    "recallTotal": 1,
                    "firstMatchRank": 1,
                    "top1SourceHit": True,
                },
                "errors": {"retrieval": None, "ask": None},
            }],
        }


if __name__ == "__main__":
    unittest.main()
