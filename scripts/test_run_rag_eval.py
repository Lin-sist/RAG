#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

import run_rag_eval as runner


class RunRagEvalJudgeTest(unittest.TestCase):
    def test_require_credentials_rejects_missing_values(self) -> None:
        args = argparse.Namespace(username="", password="")

        with self.assertRaisesRegex(RuntimeError, "explicit credentials"):
            runner.require_credentials(args)

    def test_parse_judge_content_accepts_fenced_json_and_clamps_scores(self) -> None:
        parsed = runner.parse_judge_content(
            """```json
            {"faithfulnessScore": 1.2, "relevanceScore": "0.65", "pass": "yes", "reason": "grounded"}
            ```"""
        )

        self.assertEqual(1.0, parsed["faithfulnessScore"])
        self.assertEqual(0.65, parsed["relevanceScore"])
        self.assertTrue(parsed["pass"])
        self.assertEqual("grounded", parsed["reason"])

    def test_parse_judge_content_derives_pass_when_missing(self) -> None:
        parsed = runner.parse_judge_content('{"faithfulness": 0.71, "relevance": 0.70}')

        self.assertTrue(parsed["pass"])

    def test_should_run_judge_requires_llm_mode_ask_and_answerable_sample(self) -> None:
        args = argparse.Namespace(judge_mode="llm")

        self.assertTrue(runner.should_run_judge(args, ask_evaluable=True, should_answer=True))
        self.assertFalse(runner.should_run_judge(args, ask_evaluable=False, should_answer=True))
        self.assertFalse(runner.should_run_judge(args, ask_evaluable=True, should_answer=False))

        args.judge_mode = "off"
        self.assertFalse(runner.should_run_judge(args, ask_evaluable=True, should_answer=True))

    def test_select_samples_filters_ids_then_applies_limit(self) -> None:
        samples = [
            {"id": "fact-001"},
            {"id": "definition-001"},
            {"id": "reasoning-001"},
        ]

        selected = runner.select_samples(samples, ["definition-001", "reasoning-001"], 1)

        self.assertEqual([{"id": "definition-001"}], selected)

    def test_eval_plan_estimates_ask_and_judge_calls(self) -> None:
        args = argparse.Namespace(
            skip_ask=False,
            judge_mode="llm",
            ask_timeout=12.0,
            retry_ask_timeouts=True,
            report="report.md",
            after_report="",
            details_json="details.json",
        )
        samples = [
            {"id": "fact-001", "should_answer": True},
            {"id": "no-answer-001", "should_answer": False},
        ]

        plan = runner.eval_plan(samples, args)

        self.assertEqual(2, plan["estimatedBackendCalls"]["debugRetrieve"])
        self.assertEqual(2, plan["estimatedBackendCalls"]["ask"])
        self.assertEqual(1, plan["estimatedBackendCalls"]["llmJudge"])
        self.assertEqual(12.0, plan["askTimeout"])
        self.assertTrue(plan["retryAskTimeouts"])

    def test_should_retry_ask_timeout_can_be_disabled(self) -> None:
        error = TimeoutError("timed out")

        self.assertTrue(runner.should_retry_ask_error(error, retry_timeouts=True))
        self.assertFalse(runner.should_retry_ask_error(error, retry_timeouts=False))

    def test_is_no_answer_recognizes_context_missing_response(self) -> None:
        response = {"metadata": {}, "citations": []}
        answer = "提供的上下文未包含有关 Kubernetes 的信息，因此无法根据现有内容回答该问题。"

        self.assertTrue(runner.is_no_answer(response, answer))

    def test_format_error_metadata_includes_llm_diagnostics(self) -> None:
        metadata = {
            "status": "error",
            "errorCategory": "llm",
            "llmProvider": "openai",
            "llmEndpoint": "/chat/completions",
            "llmModel": "nvidia/test",
            "llmTimeoutSeconds": 120,
            "llmMaxRetries": 3,
            "llmErrorType": "TimeoutException",
            "llmErrorCategory": "timeout",
        }

        summary = runner.format_error_metadata(metadata)

        self.assertIn("category=llm", summary)
        self.assertIn("provider=openai", summary)
        self.assertIn("endpoint=/chat/completions", summary)
        self.assertIn("timeoutSeconds=120", summary)
        self.assertIn("llmErrorCategory=timeout", summary)

    def test_extract_rerank_attribution_keeps_only_stable_facts(self) -> None:
        debug_response = {
            "diagnostics": {
                "rerankRequestedProvider": "nvidia",
                "rerankEffectiveProvider": "heuristic",
                "rerankFallbackCount": 1,
                "rerankFallbackReason": "timeout",
                "rerankModelCallCount": 1,
                "rerankCandidateCount": 20,
                "rerankScoredCount": 20,
                "rerankCoverage": 1.0,
                "rerankLatencyMillis": 125,
                "rerankModel": "nvidia/test",
                "rerankProtocol": "nvidia-ranking-v1",
                "query": "must-not-leak",
                "Authorization": "must-not-leak",
            }
        }

        attribution = runner.extract_rerank_attribution(debug_response)

        self.assertEqual("nvidia", attribution["requestedProvider"])
        self.assertEqual("heuristic", attribution["effectiveProvider"])
        self.assertEqual("timeout", attribution["fallbackReason"])
        self.assertEqual(1, attribution["modelCallCount"])
        self.assertEqual(1.0, attribution["candidateCoverage"])
        self.assertNotIn("query", attribution)
        self.assertNotIn("Authorization", attribution)

    def test_aggregate_rerank_attributions_separates_effective_provider_and_fallback(self) -> None:
        summary = runner.aggregate_rerank_attributions([
            {
                "requestedProvider": "nvidia",
                "effectiveProvider": "nvidia",
                "fallbackCount": 0,
                "fallbackReason": "none",
                "modelCallCount": 1,
                "candidateCount": 10,
                "scoredCount": 10,
            },
            {
                "requestedProvider": "nvidia",
                "effectiveProvider": "heuristic",
                "fallbackCount": 1,
                "fallbackReason": "timeout",
                "modelCallCount": 1,
                "candidateCount": 20,
                "scoredCount": 20,
            },
        ])

        self.assertEqual({"nvidia": 1, "heuristic": 1}, summary["effectiveProviderCounts"])
        self.assertEqual(0.5, summary["modelCoverage"])
        self.assertEqual(1, summary["fallbackCount"])
        self.assertEqual({"timeout": 1}, summary["fallbackReasonHistogram"])
        self.assertEqual(2, summary["totalModelCalls"])
        self.assertEqual(1.0, summary["candidateCoverage"])

    def test_latency_summary_uses_nearest_rank_and_ignores_invalid_values(self) -> None:
        summary = runner.latency_summary([30, 10, float("nan"), -1, 20])

        self.assertEqual(
            {"count": 3, "min": 10.0, "p50": 20.0, "p95": 30.0, "max": 30.0},
            summary,
        )
        self.assertEqual(10.0, runner.latency_summary([10])["p95"])
        self.assertEqual(20.0, runner.latency_summary([10, 20, 30, 40])["p50"])
        self.assertEqual(
            {"count": 0, "min": None, "p50": None, "p95": None, "max": None},
            runner.latency_summary([]),
        )

    def test_aggregate_rerank_attributions_reports_latency_percentiles(self) -> None:
        summary = runner.aggregate_rerank_attributions([
            {"effectiveProvider": "nvidia", "latencyMillis": 10},
            {"effectiveProvider": "nvidia", "latencyMillis": 20},
            {"effectiveProvider": "nvidia", "latencyMillis": 40},
        ])

        self.assertEqual(
            {"count": 3, "min": 10.0, "p50": 20.0, "p95": 40.0, "max": 40.0},
            summary["latencyMillis"],
        )

    def test_run_sample_records_debug_retrieval_wall_clock_latency(self) -> None:
        args = argparse.Namespace(
            base_url="http://localhost:8080",
            kb_id=7,
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            timeout=60.0,
            skip_ask=True,
            judge_mode="off",
        )
        sample = {"id": "no-answer-001", "question": "q", "should_answer": False}

        with (
            mock.patch.object(runner.time, "monotonic", side_effect=[1.0, 1.125]),
            mock.patch.object(runner, "call_json", return_value={"data": {"status": "ok", "contexts": []}}),
        ):
            result = runner.run_sample(sample, args, "token")

        self.assertEqual(125.0, result.details["retrieveLatencyMillis"])

    def test_run_sample_records_latency_when_debug_retrieval_fails(self) -> None:
        args = argparse.Namespace(
            base_url="http://localhost:8080",
            kb_id=7,
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            timeout=60.0,
            skip_ask=True,
            judge_mode="off",
        )
        sample = {"id": "no-answer-001", "question": "q", "should_answer": False}

        with (
            mock.patch.object(runner.time, "monotonic", side_effect=[1.0, 1.050]),
            mock.patch.object(runner, "call_json", side_effect=TimeoutError("timed out")),
        ):
            result = runner.run_sample(sample, args, "token")

        self.assertEqual(50.0, result.details["retrieveLatencyMillis"])
        self.assertIn("timed out", result.retrieval_error)

    def test_aggregate_reports_retrieval_latency_separately_from_rerank_latency(self) -> None:
        result = argparse.Namespace(
            sample={"should_answer": False},
            skipped_ask=True,
            ask_error=None,
            ask_response=None,
            skipped_judge=True,
            judge_error=None,
            judge_pass=None,
            faithfulness_score=None,
            relevance_score=None,
            recall_total=0,
            recall3_hits=0,
            recall5_hits=0,
            first_match_rank=None,
            top1_source_hit=None,
            keyword_hits=0,
            keyword_total=0,
            citation_hits=0,
            citation_total=0,
            citation_snippet_hits=0,
            citation_snippet_total=0,
            unsupported_citation_count=0,
            no_answer_citation_violation_count=0,
            no_answer_ok=None,
            details={
                "retrieveLatencyMillis": 250,
                "rerankAttribution": {"effectiveProvider": "heuristic", "latencyMillis": 15},
            },
        )

        summary = runner.aggregate([result])

        self.assertEqual(250.0, summary["retrieval_latency_millis"]["p50"])
        self.assertEqual(15.0, summary["rerank_attribution"]["latencyMillis"]["p50"])

    def test_latency_fact_rows_names_retrieval_and_rerank_separately(self) -> None:
        rows = runner.latency_fact_rows({
            "retrieval_latency_millis": {"count": 3, "min": 80.0, "p50": 90.0, "p95": 110.0, "max": 110.0},
            "rerank_attribution": {
                "latencyMillis": {"count": 3, "min": 10.0, "p50": 20.0, "p95": 40.0, "max": 40.0},
            },
        })

        rendered = "\n".join(rows)
        self.assertIn("Client-observed debug retrieval latency", rendered)
        self.assertIn("Server-side rerank stage latency", rendered)
        self.assertIn("count=3, min=80.0, P50=90.0, P95=110.0, max=110.0 ms", rendered)

    def test_main_rejects_unversioned_custom_eval_set_before_backend_calls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            eval_set = tmp / "custom.jsonl"
            report = tmp / "report.md"
            eval_set.write_text('{"id":"fact-001","should_answer":true}\n', encoding="utf-8")
            argv = [
                "run_rag_eval.py",
                "--eval-set",
                str(eval_set),
                "--kb-id",
                "1",
                "--plan-only",
                "--report",
                str(report),
            ]

            with mock.patch("sys.argv", argv), mock.patch.object(runner, "login") as login:
                self.assertEqual(2, runner.main())

            login.assert_not_called()

    def test_main_rejects_drifted_release_before_backend_calls(self) -> None:
        error = runner.dataset_contract.DatasetContractError(
            "artifact_hash_mismatch",
            "docs/eval/rag_eval_set.jsonl",
            "bytes or sha256 drifted",
        )

        with (
            mock.patch("sys.argv", ["run_rag_eval.py", "--kb-id", "1", "--plan-only"]),
            mock.patch.object(runner, "validate_eval_dataset", side_effect=error),
            mock.patch.object(runner, "login") as login,
        ):
            self.assertEqual(2, runner.main())

        login.assert_not_called()

    def test_unversioned_results_are_never_safe_for_comparison(self) -> None:
        counts = {
            "askErrors": 0,
            "retrieveErrors": 0,
            "skippedAsk": 1,
            "judgeErrors": 0,
            "skippedJudge": 1,
            "rateLimitErrors": 0,
            "retryCount": 0,
        }

        result = runner.metrics_safe_for_comparison(
            "RETRIEVAL_ONLY",
            counts,
            dataset_validation="UNVERSIONED",
        )

        self.assertEqual("no; dataset is UNVERSIONED", result)

    def test_run_metadata_dataset_identity_mismatch_is_rejected(self) -> None:
        current = {
            "validationStatus": "VALID",
            "releaseVersion": "rag-eval-dev-v1",
            "manifestSha256": "current",
        }
        metadata = {
            "datasetValidation": "VALID",
            "datasetReleaseIdentity": {**current, "manifestSha256": "stale"},
        }

        with self.assertRaises(runner.dataset_contract.DatasetContractError) as raised:
            runner.bind_dataset_identity(metadata, current)

        self.assertEqual("release_identity_mismatch", raised.exception.code)

    def test_versioned_plan_reports_release_identity_before_backend_calls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            report = Path(tmp_dir) / "report.md"
            argv = [
                "run_rag_eval.py",
                "--eval-set",
                str(Path(runner.__file__).resolve().parents[1] / "docs/eval/rag_eval_set.jsonl"),
                "--kb-id",
                "1",
                "--plan-only",
                "--sample-limit",
                "1",
                "--report",
                str(report),
            ]
            output = io.StringIO()

            with mock.patch("sys.argv", argv), mock.patch.object(runner, "login") as login:
                with redirect_stdout(output):
                    self.assertEqual(0, runner.main())

            login.assert_not_called()
            self.assertIn('"validationStatus": "VALID"', output.getvalue())
            self.assertIn('"releaseVersion": "rag-eval-dev-v1"', output.getvalue())


if __name__ == "__main__":
    unittest.main()
