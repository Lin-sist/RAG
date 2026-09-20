from __future__ import annotations

import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import c18_generation_contract as c18
import compile_generation_objective_baseline as compiler
import eval_dataset_contract as dataset_contract
import run_rag_eval as runner
from argparse import Namespace


ROOT = Path(__file__).resolve().parents[1]


class C18CompilerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = c18.load_manifest(ROOT)
        cls.release = dataset_contract.validate_versioned_release(ROOT, Path("docs/eval/dataset-manifest.json"))

    def make_artifacts(self) -> tuple[dict, dict]:
        ids = c18.expected_ids(self.manifest, "full")
        metadata = {
            "evaluationSchema": c18.SCHEMA_VERSION,
            "c18Manifest": {"id": c18.MANIFEST_ID, "sha256": self.manifest["manifestSha256"]},
            "datasetValidation": "VALID",
            "datasetReleaseIdentity": self.release,
            "sampleSelection": {"ids": ids, "count": len(ids)},
            "topK": 5,
            "minScore": 0.3,
            "enableRerank": True,
            "claimMetricConfig": c18.EXPECTED_CLAIM_METRIC_CONFIG,
            "c18RunIdentity": {"mode": "generation/objective", "slice": "full", "repeat": 1, "runIndex": 1},
            "c18Execution": {
                "selectionMode": "ordered-v2",
                "judgeMode": "off",
                "routerEnabled": False,
                "answerCache": False,
                "maxAskRetries": 0,
                "transientRetryPolicy": c18.TRANSIENT_RETRY_POLICY,
                "retryAskTimeouts": False,
                "minimumRequestIntervalSeconds": 2.2,
                "askDelaySeconds": 2.2,
                "retrievalDelaySeconds": 2.2,
            },
            "c18RuntimeFingerprint": {
                "status": "verified",
                "identity": c18.EXPECTED_RUNTIME,
                "sha256": c18.canonical_sha256({"schemaVersion": c18.SCHEMA_VERSION, **c18.EXPECTED_RUNTIME, "verificationBasis": "operator-confirmed"}),
            },
            "git": {"head": "a" * 40, "clean": True},
            "fixtures": self.manifest["dataset"]["fixtureCorpus"],
            "judgeContractConfig": {"mode": "off"},
        }
        samples = []
        for sample in self.manifest["dataset"]["samples"]:
            answerable = bool(sample.get("should_answer", True))
            ask_status = "success" if answerable else "no_result"
            samples.append(
                {
                    "id": sample["id"],
                    "question": "raw question must stay in ignored details",
                    "should_answer": answerable,
                    "debugRetrieveRawResponse": {"contexts": [{"source": "fixture.md", "snippet": "debug only"}]},
                    "askRawResponse": {
                        "answer": "raw answer must stay in ignored details",
                        "contexts": [{"source": "fixture.md", "snippet": "ask provenance"}],
                        "citations": [],
                        "metadata": {"cached": False, "model": c18.EXPECTED_RUNTIME["model"], "status": ask_status},
                    },
                    "rerankAttribution": {
                        "requestedProvider": "heuristic",
                        "effectiveProvider": "heuristic",
                        "fallbackCount": 0,
                        "modelCallCount": 0,
                    },
                    "objectiveClaimMetrics": {
                        "claimMetricStatus": "COMPLETE" if answerable else "NOT_APPLICABLE",
                        "claimMetricConfig": c18.EXPECTED_CLAIM_METRIC_CONFIG,
                    },
                    "metricCalculationDetails": {
                        "askSkipped": False,
                        "askAttempts": 1,
                        "askRetries": 0,
                        "rateLimitErrors": 0,
                    },
                    "errors": {"retrieval": None, "ask": None},
                    "c18ExecutionFacts": {
                        "askHttpAttempts": 1,
                        "askRetryCount": 0,
                        "rateLimitErrors": 0,
                        "generationCalls": 1 if answerable else 0,
                        "generationBypassCount": 0 if answerable else 1,
                        "cacheRequestEnabled": False,
                        "answerCacheHitCount": 0,
                        "algorithmFallbackCount": 0,
                        "algorithmFallbackReason": "none",
                        "providerFallbackCount": 0,
                        "automaticRetryCount": 0,
                        "generationModel": c18.EXPECTED_RUNTIME["model"],
                    },
                }
            )
        for index, sample in enumerate(samples):
            sample["c18Attempts"] = [dict(requestId=index*2+j+1, kind=kind, attempt=1,
                httpStatus=200, providerHttpStatus=None, providerErrorType=None,
                providerErrorCategory=None, errorType=None, retry=False,
                generationHttpAttempts=sample["c18ExecutionFacts"]["generationCalls"] if kind == "ask" else 0,
                elapsedMillis=1) for j, kind in enumerate(("debugRetrieve", "ask"))]
        details = {
            "reportStatus": "CLEAN",
            "objectiveMetricStatus": "COMPLETE",
            "judgeMetricStatus": "SKIPPED",
            "datasetValidation": "VALID",
            "runCounts": {
                "askErrors": 0,
                "retrieveErrors": 0,
                "skippedAsk": 0,
                "judgeErrors": 0,
                "skippedJudge": 150,
                "rateLimitErrors": 0,
                "retryCount": 0,
            },
            "claimMetricConfig": c18.EXPECTED_CLAIM_METRIC_CONFIG,
            "skipAsk": False,
            "judge": {"mode": "off"},
            "sampleCount": 150,
            "sampleIds": ids,
            "samples": samples,
            "metrics": {
                "recall_at_3": 1.0,
                "recall_at_5": 1.0,
                "mrr": 1.0,
                "top1_source_accuracy": 1.0,
                "answer_keyword_hit_rate": 1.0,
                "answer_keyword_hits": 130,
                "answer_keyword_total": 130,
                "citation_hit_rate": 1.0,
                "citation_source_hit_rate": 1.0,
                "citation_snippet_hit_rate": 1.0,
                "citation_source_hits": 130,
                "citation_source_total": 130,
                "citation_snippet_hits": 130,
                "citation_snippet_total": 130,
                "unsupported_citation_count": 0,
                "no_answer_citation_violation_count": 0,
                "claim_metric_status": "COMPLETE",
                "claim_total": 130,
                "supported_claim_count": 130,
                "unsupported_claim_count": 0,
                "objective_claim_support_rate": 1.0,
                "no_answer_accuracy": 1.0,
                "no_answer_ok_count": 20,
                "no_answer_evaluable_total": 20,
                "retrieval_latency_millis": {"count": 150, "min": 1, "p50": 2, "p95": 3, "max": 4},
            },
            "runMetadata": metadata,
            "c18Execution": {
                "counts": {"debugRetrieve": 150, "ask": 150, "generationReservations": 150, "llmJudge": 0},
                "budget": self.manifest["budgets"]["full"],
                "attempts": [e for s in samples for e in s["c18Attempts"]],
                "rejections": [],
                "requestRejectionCount": 0,
            },
        }
        return details, metadata

    def compile_temp(self, details: dict, metadata: dict) -> dict:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            details_path = root / "details.json"
            metadata_path = root / "metadata.json"
            details_path.write_text(json.dumps(details, ensure_ascii=False), encoding="utf-8")
            metadata_path.write_text(json.dumps(metadata, ensure_ascii=False), encoding="utf-8")
            return compiler.compile_evidence(ROOT, self.manifest, "full", details_path, metadata_path)

    def test_complete_summary_is_safe_and_reuses_runner_metrics(self) -> None:
        details, metadata = self.make_artifacts()
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "COMPLETE")
        self.assertEqual(result["callFacts"]["generationCalls"], 130)
        self.assertEqual(result["callFacts"]["generationBypassCount"], 20)
        encoded = json.dumps(result, ensure_ascii=False)
        self.assertNotIn("raw question", encoded)
        self.assertNotIn("raw answer", encoded)
        self.assertNotIn("fixture.md", encoded)
        self.assertNotIn("kbId", encoded)

    def test_missing_sample_is_incomplete(self) -> None:
        details, metadata = self.make_artifacts()
        details["samples"].pop()
        details["sampleCount"] = 149
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "INCOMPLETE")
        self.assertIn("sample_order_or_count_mismatch", result["reasonCodes"])

    def test_recovered_failure_requires_complete_consistent_ledger(self) -> None:
        details, metadata = self.make_artifacts()
        sample = details["samples"][0]
        failed = copy.deepcopy(sample["c18Attempts"][1])
        failed.update(providerHttpStatus=503, retry=True)
        sample["c18Attempts"][1]["attempt"] = 2
        sample["c18Attempts"].insert(1, failed)
        sample["metricCalculationDetails"].update(askAttempts=2, askRetries=1)
        sample["c18ExecutionFacts"].update(askHttpAttempts=2, askRetryCount=1, automaticRetryCount=1)
        details["runCounts"]["retryCount"] = 1
        details["c18Execution"]["counts"].update(ask=151, generationReservations=151)
        details["c18Execution"]["attempts"] = [e for s in details["samples"] for e in s["c18Attempts"]]
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "COMPLETE", result["reasonCodes"])
        self.assertEqual(result["callFacts"]["recoveredFailureCount"], 1)
        self.assertEqual(result["callFacts"]["failedAttemptStatuses"], {"503": 1})
        failed["providerHttpStatus"] = 500
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "INCOMPLETE")
        self.assertIn("transient_attempt_ledger_invalid", result["reasonCodes"])

    def test_missing_attempt_ledger_is_incomplete(self) -> None:
        details, metadata = self.make_artifacts()
        details["samples"][0].pop("c18Attempts")
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "INCOMPLETE")

    def test_recovered_premature_close_is_complete_and_visible(self) -> None:
        details, metadata = self.make_artifacts()
        sample = details["samples"][0]
        failed = copy.deepcopy(sample["c18Attempts"][1])
        failed.update(providerErrorType="PrematureCloseException", providerErrorCategory="network", retry=True,
                      generationHttpAttempts=None)
        sample["c18Attempts"][1]["attempt"] = 2
        sample["c18Attempts"].insert(1, failed)
        sample["metricCalculationDetails"].update(askAttempts=2, askRetries=1)
        sample["c18ExecutionFacts"].update(askHttpAttempts=2, askRetryCount=1, automaticRetryCount=1)
        details["runCounts"]["retryCount"] = 1
        details["c18Execution"]["counts"].update(ask=151, generationReservations=151)
        details["c18Execution"]["attempts"] = [e for s in details["samples"] for e in s["c18Attempts"]]
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "COMPLETE", result["reasonCodes"])
        self.assertEqual(result["callFacts"]["failedAttemptStatuses"], {"network:PrematureCloseException": 1})

    def test_real_runner_windows_descriptors_survive_sanitization(self) -> None:
        details, metadata = self.make_artifacts()
        metadata["fixtures"] = copy.deepcopy(metadata["fixtures"])
        for item in metadata["fixtures"]:
            item["path"] = item["path"].replace("/", "\\")
        metadata["judgeContractConfig"] = runner.judge_contract.contract_config(
            Namespace(judge_mode="off", judge_model="", judge_temperature=0.0,
                      judge_base_url="https://integrate.api.nvidia.com/v1", judge_max_context_chars=6000)
        )
        details = runner.sanitize_sensitive(details)
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "COMPLETE", result["reasonCodes"])
        self.assertEqual(runner.sanitize_sensitive({"maxOutputTokens": "secret"}),
                         {"maxOutputTokens": "[REDACTED]"})

    def test_failed_generation_count_stays_unknown(self) -> None:
        details, metadata = self.make_artifacts()
        details["samples"][0]["c18ExecutionFacts"]["generationCalls"] = None
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "INCOMPLETE")
        self.assertIsNone(result["callFacts"]["generationCalls"])
        self.assertEqual(result["callFacts"]["generationUnknownSampleCount"], 1)

    def test_error_observation_is_incomplete_not_scored_from_successes(self) -> None:
        details, metadata = self.make_artifacts()
        details["samples"][0]["errors"]["ask"] = "timeout"
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "INCOMPLETE")
        self.assertIn("sample_error", result["reasonCodes"])

    def test_git_or_runtime_drift_is_not_comparable(self) -> None:
        details, metadata = self.make_artifacts()
        metadata["git"]["clean"] = False
        details["runMetadata"] = metadata
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "NOT_COMPARABLE")
        self.assertIn("git_provenance_not_clean", result["reasonCodes"])

    def test_debug_only_claim_evidence_cannot_be_complete(self) -> None:
        details, metadata = self.make_artifacts()
        details["samples"][0]["objectiveClaimMetrics"]["claimMetricConfig"] = copy.deepcopy(c18.EXPECTED_CLAIM_METRIC_CONFIG)
        details["samples"][0]["objectiveClaimMetrics"]["claimMetricConfig"]["evidencePolicy"] = "debug-contexts-v1"
        result = self.compile_temp(details, metadata)
        self.assertEqual(result["status"], "INCOMPLETE")
        self.assertIn("claim_metric_incomplete_or_identity_mismatch", result["reasonCodes"])

    def test_safe_summary_honors_no_overwrite(self) -> None:
        details, metadata = self.make_artifacts()
        result = self.compile_temp(details, metadata)
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "summary.json"
            compiler.write_output(output, result, no_overwrite=True)
            with self.assertRaises(FileExistsError):
                compiler.write_output(output, result, no_overwrite=True)


if __name__ == "__main__":
    unittest.main()
