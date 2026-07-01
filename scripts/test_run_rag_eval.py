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


if __name__ == "__main__":
    unittest.main()
