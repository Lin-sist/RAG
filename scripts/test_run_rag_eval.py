#!/usr/bin/env python3
from __future__ import annotations

import argparse
import unittest

import run_rag_eval as runner


class RunRagEvalJudgeTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
