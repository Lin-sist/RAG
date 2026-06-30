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


if __name__ == "__main__":
    unittest.main()
