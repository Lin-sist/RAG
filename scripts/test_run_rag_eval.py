#!/usr/bin/env python3
from __future__ import annotations

import argparse
import unittest

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


if __name__ == "__main__":
    unittest.main()
