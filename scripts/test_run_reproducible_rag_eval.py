#!/usr/bin/env python3
from __future__ import annotations

import argparse
import tempfile
import unittest
from pathlib import Path

import run_reproducible_rag_eval as runner


class ReproducibleRagEvalTest(unittest.TestCase):
    def test_repeat_path_suffixes_only_when_repeated(self) -> None:
        self.assertEqual(
            Path("docs/eval/reports/stage1.md"),
            runner.repeat_path("docs/eval/reports/stage1.md", 1, 1),
        )
        self.assertEqual(
            Path("docs/eval/reports/stage1-run2.md"),
            runner.repeat_path("docs/eval/reports/stage1.md", 2, 2),
        )

    def test_docs_for_expected_files_matches_title_file_name_or_path_name(self) -> None:
        docs = [
            {"title": "springboot-basics.md", "status": "COMPLETED"},
            {"fileName": "java-interview-guide.md", "status": "COMPLETED"},
            {"filePath": "C:/tmp/rag-technology-guide.md", "status": "COMPLETED"},
            {"title": "other.md", "status": "COMPLETED"},
        ]

        matched = runner.docs_for_expected_files(
            docs,
            {"springboot-basics.md", "java-interview-guide.md", "rag-technology-guide.md"},
        )

        self.assertEqual(3, len(matched))

    def test_build_metadata_contains_kb_fixture_config_and_chunk_counts(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            fixture = tmp / "fixture.md"
            fixture.write_text("# Fixture\ncontent\n", encoding="utf-8")
            config = tmp / "application.yml"
            config.write_text("retrieval:\n  rerank:\n    provider: heuristic\n", encoding="utf-8")

            original_snapshot = runner.DEFAULT_CONFIG_SNAPSHOT
            try:
                runner.DEFAULT_CONFIG_SNAPSHOT = [config]
                args = argparse.Namespace(
                    base_url="http://localhost:8080",
                    eval_set="docs/eval/rag_eval_set.jsonl",
                    top_k=5,
                    min_score=0.3,
                    enable_rerank=True,
                )
                metadata = runner.build_metadata(
                    args,
                    {"id": 9, "name": "codex-stage1-repro-eval", "description": "marker", "vectorCollection": "kb_test"},
                    [
                        {"id": 1, "title": "a.md", "status": "COMPLETED", "chunkCount": 10, "contentHash": "a"},
                        {"id": 2, "title": "b.md", "status": "COMPLETED", "chunkCount": 31, "contentHash": "b"},
                    ],
                    [fixture],
                )
            finally:
                runner.DEFAULT_CONFIG_SNAPSHOT = original_snapshot

        self.assertEqual(41, metadata["knowledgeBase"]["chunkCount"])
        self.assertEqual("kb_test", metadata["knowledgeBase"]["vectorCollection"])
        self.assertEqual("fixture.md", metadata["fixtures"][0]["name"])
        self.assertIn(str(config), metadata["configSnapshot"])

    def test_write_json_respects_no_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            path = Path(tmp_dir) / "metadata.json"
            runner.write_json(path, {"first": True}, no_overwrite=False)

            with self.assertRaises(runner.ApiError):
                runner.write_json(path, {"second": True}, no_overwrite=True)

    def test_build_eval_command_defaults_to_retrieval_only(self) -> None:
        command = runner.build_eval_command(
            self.eval_command_args(include_ask=False),
            7,
            Path("report.md"),
            Path("details.json"),
            Path("metadata.json"),
        )

        self.assertIn("--skip-ask", command)
        self.assertNotIn("--judge-mode", command)

    def test_build_eval_command_can_include_ask_and_judge_without_api_key_arg(self) -> None:
        command = runner.build_eval_command(
            self.eval_command_args(include_ask=True),
            7,
            Path("report.md"),
            Path("details.json"),
            Path("metadata.json"),
        )

        self.assertNotIn("--skip-ask", command)
        self.assertIn("--judge-mode", command)
        self.assertIn("llm", command)
        self.assertNotIn("--judge-api-key", command)

    def test_build_eval_command_passes_sample_selection(self) -> None:
        command = runner.build_eval_command(
            self.eval_command_args(include_ask=True),
            7,
            Path("report.md"),
            Path("details.json"),
            Path("metadata.json"),
        )

        self.assertIn("--sample-limit", command)
        self.assertIn("2", command)
        self.assertEqual(2, command.count("--sample-id"))
        self.assertIn("fact-001", command)
        self.assertIn("definition-001", command)

    def test_build_plan_includes_command_shape_without_api_key(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            eval_set = Path(tmp_dir) / "eval.jsonl"
            eval_set.write_text(
                "\n".join([
                    '{"id":"fact-001","should_answer":true}',
                    '{"id":"definition-001","should_answer":true}',
                    '{"id":"no-answer-001","should_answer":false}',
                ]),
                encoding="utf-8",
            )
            args = self.eval_command_args(include_ask=True)
            args.eval_set = str(eval_set)

            plan = runner.build_plan(
                args,
                [Path("test-data/springboot-basics.md")],
            )

        self.assertEqual("generation/citation", plan["mode"])
        self.assertEqual(["fact-001", "definition-001"], plan["sampleIds"])
        self.assertEqual(2, plan["sampleLimit"])
        self.assertEqual(2, plan["selectedSampleCount"])
        self.assertEqual(2, plan["answerableCount"])
        self.assertEqual(0, plan["noAnswerCount"])
        self.assertEqual({"debugRetrieve": 2, "ask": 2, "llmJudge": 2}, plan["estimatedLiveCalls"])
        self.assertIn("--sample-id", plan["childCommandShape"])
        self.assertNotIn("--judge-api-key", plan["childCommandShape"])

    @staticmethod
    def eval_command_args(include_ask: bool) -> argparse.Namespace:
        return argparse.Namespace(
            python="python",
            base_url="http://localhost:8080",
            kb_name="codex-stage1-repro-eval",
            eval_set="docs/eval/rag_eval_set.jsonl",
            report="docs/eval/reports/stage1.md",
            details_json="docs/eval/reports/stage1-details.json",
            metadata_json="docs/eval/reports/stage1-metadata.json",
            username="admin",
            password="admin123",
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            no_overwrite=False,
            sample_ids=["fact-001", "definition-001"],
            sample_limit=2,
            repeat=1,
            include_ask=include_ask,
            ask_delay_seconds=1.0,
            max_ask_retries=2,
            retry_backoff_seconds=3.0,
            judge_mode="llm",
            judge_base_url="https://example.test/v1",
            judge_api_key="secret",
            judge_model="judge-model",
            judge_temperature=0.0,
            judge_timeout=30.0,
            judge_max_context_chars=4000,
        )


if __name__ == "__main__":
    unittest.main()
