#!/usr/bin/env python3
from __future__ import annotations

import argparse
import tempfile
import unittest
from unittest import mock
from pathlib import Path

import run_reproducible_rag_eval as runner


class ReproducibleRagEvalTest(unittest.TestCase):
    def test_require_credentials_rejects_missing_values(self) -> None:
        args = argparse.Namespace(username="", password="")

        with self.assertRaisesRegex(runner.ApiError, "explicit credentials"):
            runner.require_credentials(args)

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

    def test_build_eval_command_passes_ask_timeout(self) -> None:
        args = self.eval_command_args(include_ask=True)
        args.ask_timeout = 12.0

        command = runner.build_eval_command(
            args,
            7,
            Path("report.md"),
            Path("details.json"),
            Path("metadata.json"),
        )

        self.assertIn("--ask-timeout", command)
        self.assertEqual("12.0", command[command.index("--ask-timeout") + 1])

    def test_build_eval_command_can_disable_timeout_retries(self) -> None:
        args = self.eval_command_args(include_ask=True)
        args.retry_ask_timeouts = False

        command = runner.build_eval_command(
            args,
            7,
            Path("report.md"),
            Path("details.json"),
            Path("metadata.json"),
        )

        self.assertIn("--no-retry-ask-timeouts", command)
        self.assertNotIn("--retry-ask-timeouts", command)

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
        self.assertFalse(plan["keepExisting"])
        self.assertTrue(plan["willUploadFixtures"])
        self.assertEqual(1, plan["expectedFixtureUploads"])
        self.assertEqual({"debugRetrieve": 2, "ask": 2, "llmJudge": 2}, plan["estimatedLiveCalls"])
        self.assertEqual(60, plan["askTimeout"])
        self.assertTrue(plan["retryAskTimeouts"])
        self.assertIn("--sample-id", plan["childCommandShape"])
        self.assertNotIn("--judge-api-key", plan["childCommandShape"])
        password_index = plan["childCommandShape"].index("--password") + 1
        self.assertEqual("***", plan["childCommandShape"][password_index])

    def test_build_plan_marks_keep_existing_as_no_fixture_uploads(self) -> None:
        args = self.eval_command_args(include_ask=True)
        args.keep_existing = True

        plan = runner.build_plan(
            args,
            [Path("test-data/springboot-basics.md"), Path("test-data/java-interview-guide.md")],
            [{"id": "fact-001", "should_answer": True}],
        )

        self.assertTrue(plan["keepExisting"])
        self.assertFalse(plan["willUploadFixtures"])
        self.assertEqual(0, plan["expectedFixtureUploads"])

    def test_build_preflight_reports_ready_without_eval_calls(self) -> None:
        args = self.eval_command_args(include_ask=True)
        result = runner.build_preflight(
            args,
            {"id": 15, "name": "codex-stage1-repro-eval", "vectorCollection": "kb_test"},
            [
                {"title": "springboot-basics.md", "status": "COMPLETED", "chunkCount": 18},
                {"fileName": "java-interview-guide.md", "status": "COMPLETED", "chunkCount": 20},
            ],
            [Path("test-data/springboot-basics.md"), Path("test-data/java-interview-guide.md")],
        )

        self.assertEqual("READY", result["status"])
        self.assertTrue(result["mutationFree"])
        self.assertEqual(2, result["fixtures"]["matchedCount"])
        self.assertEqual([], result["fixtures"]["missing"])
        self.assertEqual([], result["fixtures"]["incomplete"])

    def test_build_preflight_reports_missing_and_incomplete_fixtures(self) -> None:
        args = self.eval_command_args(include_ask=False)
        result = runner.build_preflight(
            args,
            {"id": 15, "name": "codex-stage1-repro-eval"},
            [{"title": "springboot-basics.md", "status": "PROCESSING", "chunkCount": 0}],
            [Path("test-data/springboot-basics.md"), Path("test-data/java-interview-guide.md")],
        )

        self.assertEqual("BLOCKED", result["status"])
        self.assertEqual(["java-interview-guide.md"], result["fixtures"]["missing"])
        self.assertEqual(["springboot-basics.md"], result["fixtures"]["incomplete"])

    def test_find_existing_eval_kb_rejects_marker_mismatch(self) -> None:
        args = self.eval_command_args(include_ask=False)
        args.kb_description = "expected marker"
        with mock.patch.object(
            runner,
            "list_kbs",
            return_value=[{"id": 15, "name": args.kb_name, "description": "other"}],
        ):
            with self.assertRaises(runner.ApiError):
                runner.find_existing_eval_kb(args, "token")

    def test_get_or_create_kb_keep_existing_missing_does_not_create(self) -> None:
        args = self.eval_command_args(include_ask=False)
        args.keep_existing = True

        with (
            mock.patch.object(runner, "find_existing_eval_kb", return_value=None),
            mock.patch.object(runner, "create_kb") as create_kb,
        ):
            with self.assertRaisesRegex(runner.ApiError, "--keep-existing"):
                runner.get_or_create_kb(args, "token")

        create_kb.assert_not_called()

    def test_main_refuses_empty_sample_selection_before_backend_calls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            eval_set = tmp / "eval.jsonl"
            fixture = tmp / "fixture.md"
            eval_set.write_text('{"id":"fact-001","should_answer":true}\n', encoding="utf-8")
            fixture.write_text("# Fixture\n", encoding="utf-8")
            argv = [
                "run_reproducible_rag_eval.py",
                "--eval-set",
                str(eval_set),
                "--fixture",
                str(fixture),
                "--sample-id",
                "missing-001",
            ]

            with mock.patch("sys.argv", argv), mock.patch.object(runner, "login") as login:
                self.assertEqual(2, runner.main())

            login.assert_not_called()

    def test_main_preflight_does_not_create_kb_or_run_eval(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            eval_set = tmp / "eval.jsonl"
            fixture = tmp / "fixture.md"
            eval_set.write_text('{"id":"fact-001","should_answer":true}\n', encoding="utf-8")
            fixture.write_text("# Fixture\n", encoding="utf-8")
            argv = [
                "run_reproducible_rag_eval.py",
                "--preflight-only",
                "--eval-set",
                str(eval_set),
                "--fixture",
                str(fixture),
                "--username",
                "test-operator",
                "--password",
                "test-only-password",
            ]
            kb = {
                "id": 15,
                "name": runner.DEFAULT_KB_NAME,
                "description": runner.DEFAULT_KB_MARKER,
                "vectorCollection": "kb_test",
            }
            docs = [{"title": fixture.name, "status": "COMPLETED", "chunkCount": 1}]

            with (
                mock.patch("sys.argv", argv),
                mock.patch.object(runner, "login", return_value="token"),
                mock.patch.object(runner, "find_existing_eval_kb", return_value=kb),
                mock.patch.object(runner, "list_documents", return_value=docs),
                mock.patch.object(runner, "get_or_create_kb") as get_or_create_kb,
                mock.patch.object(runner, "run_eval") as run_eval,
            ):
                self.assertEqual(0, runner.main())

            get_or_create_kb.assert_not_called()
            run_eval.assert_not_called()

    @staticmethod
    def eval_command_args(include_ask: bool) -> argparse.Namespace:
        return argparse.Namespace(
            python="python",
            base_url="http://localhost:8080",
            kb_name="codex-stage1-repro-eval",
            kb_description="codex reproducible retrieval-only eval fixture",
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
            keep_existing=False,
            include_ask=include_ask,
            ask_timeout=60,
            ask_delay_seconds=1.0,
            max_ask_retries=2,
            retry_backoff_seconds=3.0,
            retry_ask_timeouts=True,
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
