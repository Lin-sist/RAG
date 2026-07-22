#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import unittest
from pathlib import Path

import eval_dataset_contract as contract


class EvalDatasetContractTest(unittest.TestCase):
    def test_current_tracked_release_validates_with_versioned_identity(self) -> None:
        repo_root = Path(__file__).resolve().parents[1]

        identity = contract.validate_versioned_release(
            repo_root,
            Path("docs/eval/dataset-manifest.json"),
        )

        self.assertEqual("VALID", identity["validationStatus"])
        self.assertEqual("rag-eval-dev-v1", identity["releaseVersion"])
        self.assertEqual("questions-v1", identity["questionSetVersion"])
        self.assertEqual("rag-eval-sample-v1", identity["sampleSchemaVersion"])
        self.assertEqual("annotations-v1", identity["annotationVersion"])
        self.assertEqual("fixtures-v1", identity["fixtureCorpusVersion"])
        self.assertEqual(30, identity["questionSet"]["sampleCount"])
        self.assertEqual(3, len(identity["fixtures"]))

    def test_tracked_v1_and_v2_release_manifests_validate_concurrently(self) -> None:
        repo_root = Path(__file__).resolve().parents[1]

        default_v1 = contract.validate_versioned_release(
            repo_root,
            Path("docs/eval/dataset-manifest.json"),
        )
        explicit_v1 = contract.validate_versioned_release(
            repo_root,
            Path("docs/eval/releases/rag-eval-dev-v1-manifest.json"),
        )
        explicit_v2 = contract.validate_versioned_release(
            repo_root,
            Path("docs/eval/releases/rag-eval-dev-v2-manifest.json"),
        )

        self.assertEqual(default_v1["manifestSha256"], explicit_v1["manifestSha256"])
        self.assertEqual("rag-eval-dev-v1", explicit_v1["releaseVersion"])
        self.assertEqual("rag-eval-dev-v2", explicit_v2["releaseVersion"])
        self.assertEqual(150, explicit_v2["questionSet"]["sampleCount"])
        self.assertEqual(30, explicit_v2["expandedDataset"]["seedSampleCount"])
        self.assertEqual(120, explicit_v2["expandedDataset"]["newSampleCount"])
        self.assertEqual(150, explicit_v2["annotationReview"]["reviewedSampleCount"])
        self.assertEqual(
            {
                "java-interview-guide.md": 49,
                "rag-technology-guide.md": 43,
                "springboot-basics.md": 44,
            },
            explicit_v2["expandedDataset"]["fixtureCoverage"]["counts"],
        )

    def test_manifest_v2_binds_review_and_expanded_identity_while_v1_stays_supported(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            rows = [
                json.loads(line)
                for line in (repo_root / manifest["questionSet"]["path"])
                .read_text(encoding="utf-8")
                .splitlines()
            ]
            self.upgrade_to_v2(repo_root, manifest, rows)

            identity = contract.validate_versioned_release(
                repo_root,
                Path("docs/eval/dataset-manifest.json"),
            )

        self.assertEqual("rag-eval-dataset-manifest-v2", identity["manifestSchemaVersion"])
        self.assertEqual(30, identity["annotationReview"]["reviewedSampleCount"])
        self.assertEqual(30, identity["expandedDataset"]["seedSampleCount"])
        self.assertEqual(0, identity["expandedDataset"]["newSampleCount"])

    def test_manifest_v2_rejects_fixture_coverage_outside_declared_boundaries(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            rows = [
                json.loads(line)
                for line in (repo_root / manifest["questionSet"]["path"])
                .read_text(encoding="utf-8")
                .splitlines()
            ]
            self.upgrade_to_v2(repo_root, manifest, rows)
            manifest["expandedDataset"]["fixtureCoverage"] = {
                "minimumAnswerableSamples": 100,
                "maximumAnswerableRatio": 1.0,
            }
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(
                    repo_root,
                    Path("docs/eval/dataset-manifest.json"),
                )

        self.assertEqual("fixture_coverage_mismatch", raised.exception.code)

    def test_manifest_v2_rejects_new_context_that_is_not_grounded_in_fixture(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            new_row = dict(rows[0])
            new_row["id"] = "fact-011"
            new_row["question"] = "Spring Boot 快速开发脚手架的定位是什么？"
            new_row["expected_contexts"] = [
                {
                    "source": "springboot-basics.md",
                    "contains": "fixture 中并不存在的私有证据文本",
                }
            ]
            rows.append(new_row)
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)
            self.upgrade_to_v2(repo_root, manifest, rows, seed_count=30)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(
                    repo_root,
                    Path("docs/eval/dataset-manifest.json"),
                )

        self.assertEqual("context_grounding_mismatch", raised.exception.code)

    def test_manifest_v2_rejects_normalized_exact_duplicate_question(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            new_row = dict(rows[0])
            new_row["id"] = "fact-011"
            new_row["question"] = "spring boot的核心特性有哪些"
            rows.append(new_row)
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)
            self.upgrade_to_v2(repo_root, manifest, rows, seed_count=30)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(
                    repo_root,
                    Path("docs/eval/dataset-manifest.json"),
                )

        self.assertEqual("duplicate_normalized_question", raised.exception.code)

    def test_manifest_v2_rejects_reusing_seed_release_versions(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            rows = [
                json.loads(line)
                for line in (repo_root / manifest["questionSet"]["path"])
                .read_text(encoding="utf-8")
                .splitlines()
            ]
            self.upgrade_to_v2(repo_root, manifest, rows)
            manifest["releaseVersion"] = "rag-eval-dev-v1"
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(
                    repo_root,
                    Path("docs/eval/dataset-manifest.json"),
                )

        self.assertEqual("release_version_mismatch", raised.exception.code)

    def test_manifest_v2_rejects_unjustified_schema_or_fixture_version_bump(self) -> None:
        for field, value, expected_code in (
            ("sampleSchemaVersion", "rag-eval-sample-v2", "release_identity_mismatch"),
            ("fixtureCorpusVersion", "fixtures-v2", "release_version_mismatch"),
        ):
            with self.subTest(field=field), tempfile.TemporaryDirectory() as tmp_dir:
                repo_root = Path(tmp_dir)
                manifest = self.copy_tracked_release(repo_root)
                rows = [
                    json.loads(line)
                    for line in (repo_root / manifest["questionSet"]["path"])
                    .read_text(encoding="utf-8")
                    .splitlines()
                ]
                self.upgrade_to_v2(repo_root, manifest, rows)
                manifest[field] = value
                self.write_manifest(repo_root, manifest)

                with self.assertRaises(contract.DatasetContractError) as raised:
                    contract.validate_versioned_release(
                        repo_root,
                        Path("docs/eval/dataset-manifest.json"),
                    )

                self.assertEqual(expected_code, raised.exception.code)

    def test_manifest_v2_rejects_cross_quota_drift(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            rows = [
                json.loads(line)
                for line in (repo_root / manifest["questionSet"]["path"])
                .read_text(encoding="utf-8")
                .splitlines()
            ]
            self.upgrade_to_v2(repo_root, manifest, rows)
            manifest["expandedDataset"]["quotas"]["typeDifficulty"]["fact"]["easy"] += 1
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("quota_mismatch", raised.exception.code)

    def test_manifest_v2_rejects_seed_object_drift(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            self.upgrade_to_v2(repo_root, manifest, rows)
            rows[0]["notes"] = "seed annotation drift"
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("seed_identity_mismatch", raised.exception.code)

    def test_manifest_v2_rejects_new_multi_hop_with_one_evidence_point(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            new_row = dict(rows[24])
            new_row["id"] = "multi-hop-004"
            new_row["question"] = "RAG 离线处理链路应从哪一步开始？"
            new_row["expected_contexts"] = [
                {"source": "rag-technology-guide.md", "contains": "文档处理（离线）"}
            ]
            rows.append(new_row)
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)
            self.upgrade_to_v2(repo_root, manifest, rows, seed_count=30)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("multi_hop_evidence_incomplete", raised.exception.code)

    def test_manifest_v2_rejects_review_coverage_gap(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            rows = [
                json.loads(line)
                for line in (repo_root / manifest["questionSet"]["path"])
                .read_text(encoding="utf-8")
                .splitlines()
            ]
            self.upgrade_to_v2(repo_root, manifest, rows)
            review_path = repo_root / manifest["annotationReview"]["path"]
            review_path.write_bytes(b"".join(review_path.read_bytes().splitlines(keepends=True)[:-1]))
            manifest["annotationReview"].update(
                {
                    "sha256": self.sha256(review_path),
                    "bytes": review_path.stat().st_size,
                    "reviewedSampleCount": len(rows) - 1,
                }
            )
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("review_coverage_mismatch", raised.exception.code)

    def test_manifest_v2_requires_explicit_near_duplicate_review(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            new_row = dict(rows[0])
            new_row["id"] = "fact-011"
            new_row["question"] = "Spring Boot 的核心特性具体有哪些？"
            rows.append(new_row)
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)
            self.upgrade_to_v2(repo_root, manifest, rows, seed_count=30)
            manifest["expandedDataset"]["nearDuplicateThreshold"] = 0.65
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("near_duplicate_review_missing", raised.exception.code)

    def test_each_required_version_is_rejected_when_missing(self) -> None:
        for field in (
            "releaseVersion",
            "questionSetVersion",
            "sampleSchemaVersion",
            "annotationVersion",
            "fixtureCorpusVersion",
        ):
            with self.subTest(field=field), tempfile.TemporaryDirectory() as tmp_dir:
                repo_root = Path(tmp_dir)
                manifest = self.copy_tracked_release(repo_root)
                manifest.pop(field)
                self.write_manifest(repo_root, manifest)

                with self.assertRaises(contract.DatasetContractError) as raised:
                    contract.validate_versioned_release(
                        repo_root,
                        Path("docs/eval/dataset-manifest.json"),
                    )

                self.assertEqual("manifest_invalid", raised.exception.code)

    def test_parent_traversal_artifact_path_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            manifest["questionSet"]["path"] = "../outside.jsonl"
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("unsafe_artifact_path", raised.exception.code)
        self.assertNotIn(str(repo_root), str(raised.exception))

    def test_absolute_artifact_path_is_rejected_without_echoing_local_path(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            absolute = str((repo_root / "private" / "questions.jsonl").resolve())
            manifest["questionSet"]["path"] = absolute
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("unsafe_artifact_path", raised.exception.code)
        self.assertNotIn(absolute, str(raised.exception))

    def test_fixture_hash_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            fixture = repo_root / manifest["fixtures"][0]["path"]
            fixture.write_bytes(fixture.read_bytes() + b"\n")

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("artifact_hash_mismatch", raised.exception.code)

    def test_missing_fixture_artifact_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            fixture = repo_root / manifest["fixtures"][0]["path"]
            fixture.unlink()

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("artifact_missing", raised.exception.code)

    def test_non_object_sample_is_rejected_without_echoing_content(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            sensitive_content = "private-question-content-must-not-leak"
            eval_path.write_text(json.dumps([sensitive_content]) + "\n", encoding="utf-8")
            manifest["questionSet"]["sha256"] = self.sha256(eval_path)
            manifest["questionSet"]["bytes"] = eval_path.stat().st_size
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("sample_not_object", raised.exception.code)
        self.assertNotIn(sensitive_content, str(raised.exception))

    def test_unknown_sample_field_is_rejected_by_versioned_schema(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0]["unexpected_label"] = "must-not-be-accepted"
            eval_path.write_text(
                "\n".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in rows) + "\n",
                encoding="utf-8",
            )
            manifest["questionSet"]["sha256"] = self.sha256(eval_path)
            manifest["questionSet"]["bytes"] = eval_path.stat().st_size
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("unknown_field", raised.exception.code)

    def test_missing_required_sample_field_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0].pop("question")
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("missing_field", raised.exception.code)

    def test_invalid_sample_field_type_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0]["should_answer"] = "true"
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("invalid_field_type", raised.exception.code)

    def test_invalid_sample_enum_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0]["type"] = "unsupported"
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("invalid_enum", raised.exception.code)

    def test_duplicate_sample_id_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[1]["id"] = rows[0]["id"]
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("duplicate_sample_id", raised.exception.code)

    def test_no_answer_annotation_conflict_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[-1]["should_answer"] = True
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("answerability_conflict", raised.exception.code)

    def test_answerable_source_must_belong_to_versioned_fixture_corpus(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0]["expected_sources"].append("unversioned.md")
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("unknown_fixture_source", raised.exception.code)

    def test_context_annotation_requires_exact_non_empty_fields(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0]["expected_contexts"][0].pop("contains")
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("invalid_field_type", raised.exception.code)

    def test_sample_id_must_match_versioned_pattern(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            eval_path = repo_root / manifest["questionSet"]["path"]
            rows = [json.loads(line) for line in eval_path.read_text(encoding="utf-8").splitlines()]
            rows[0]["id"] = "custom-id"
            self.write_rows_and_refresh_manifest(repo_root, manifest, rows)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("invalid_id_pattern", raised.exception.code)

    def test_declared_distribution_must_match_samples(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest = self.copy_tracked_release(repo_root)
            manifest["distribution"]["type"]["fact"] += 1
            self.write_manifest(repo_root, manifest)

            with self.assertRaises(contract.DatasetContractError) as raised:
                contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))

        self.assertEqual("distribution_mismatch", raised.exception.code)

    def test_same_release_version_with_different_manifest_identity_is_rejected(self) -> None:
        tracked = {
            "releaseVersion": "rag-eval-dev-v1",
            "manifestPath": "docs/eval/dataset-manifest.json",
            "manifestSha256": "tracked",
        }
        candidate = {
            "releaseVersion": "rag-eval-dev-v1",
            "manifestPath": "docs/eval/alternate-manifest.json",
            "manifestSha256": "different",
        }

        with self.assertRaises(contract.DatasetContractError) as raised:
            contract.ensure_release_version_consistent(candidate, tracked)

        self.assertEqual("release_identity_mismatch", raised.exception.code)

    @staticmethod
    def sha256(path: Path) -> str:
        return hashlib.sha256(path.read_bytes()).hexdigest()

    @staticmethod
    def expanded_contract(rows: list[dict], seed_count: int) -> dict:
        types: dict[str, int] = {}
        difficulties: dict[str, int] = {}
        type_difficulty: dict[str, dict[str, int]] = {}
        for row in rows:
            sample_type = row["type"]
            difficulty = row["difficulty"]
            types[sample_type] = types.get(sample_type, 0) + 1
            difficulties[difficulty] = difficulties.get(difficulty, 0) + 1
            cells = type_difficulty.setdefault(sample_type, {})
            cells[difficulty] = cells.get(difficulty, 0) + 1
        return {
            "totalSampleCount": len(rows),
            "seedSampleCount": seed_count,
            "newSampleCount": len(rows) - seed_count,
            "quotas": {
                "type": dict(sorted(types.items())),
                "difficulty": dict(sorted(difficulties.items())),
                "shouldAnswer": {
                    "false": sum(row["should_answer"] is False for row in rows),
                    "true": sum(row["should_answer"] is True for row in rows),
                },
                "typeDifficulty": {
                    sample_type: dict(sorted(cells.items()))
                    for sample_type, cells in sorted(type_difficulty.items())
                },
            },
            "fixtureCoverage": {
                "minimumAnswerableSamples": 0,
                "maximumAnswerableRatio": 1.0,
            },
            "nearDuplicateThreshold": 0.82,
        }

    @classmethod
    def upgrade_to_v2(
        cls,
        repo_root: Path,
        manifest: dict,
        rows: list[dict],
        seed_count: int | None = None,
    ) -> None:
        if seed_count is None:
            seed_count = len(rows)
        seed_path = repo_root / "docs/eval/releases/seed-v1.jsonl"
        seed_path.parent.mkdir(parents=True, exist_ok=True)
        seed_rows = rows[:seed_count]
        question_path = repo_root / manifest["questionSet"]["path"]
        seed_bytes = b"".join(question_path.read_bytes().splitlines(keepends=True)[:seed_count])
        seed_path.write_bytes(seed_bytes)
        seed_distribution = cls.expanded_contract(seed_rows, seed_count=seed_count)["quotas"]
        seed_manifest = json.loads(json.dumps(manifest))
        seed_manifest["questionSet"] = {
            "path": "docs/eval/releases/seed-v1.jsonl",
            "sha256": cls.sha256(seed_path),
            "bytes": seed_path.stat().st_size,
            "sampleCount": seed_count,
            "orderedSampleIdsSha256": hashlib.sha256(
                json.dumps(
                    [row["id"] for row in seed_rows],
                    ensure_ascii=False,
                    separators=(",", ":"),
                ).encode("utf-8")
            ).hexdigest(),
        }
        seed_manifest["distribution"] = {
            "type": seed_distribution["type"],
            "difficulty": seed_distribution["difficulty"],
            "shouldAnswer": seed_distribution["shouldAnswer"],
        }
        seed_manifest_path = repo_root / "docs/eval/releases/seed-v1-manifest.json"
        seed_manifest_path.write_text(
            json.dumps(seed_manifest, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        review_path = repo_root / "docs/eval/review/review-v2.jsonl"
        review_path.parent.mkdir(parents=True, exist_ok=True)
        review_path.write_text(
            "\n".join(
                json.dumps(
                    {
                        "sampleId": row["id"],
                        "structureStatus": "passed",
                        "groundingStatus": (
                            "passed" if row["should_answer"] else "not_applicable"
                        ),
                        "duplicateStatus": "unique",
                        "semanticReviewStatus": "passed",
                        "reviewNotes": "reviewed",
                    },
                    ensure_ascii=False,
                    separators=(",", ":"),
                )
                for row in rows
            )
            + "\n",
            encoding="utf-8",
        )
        manifest["manifestSchemaVersion"] = "rag-eval-dataset-manifest-v2"
        manifest["releaseVersion"] = "rag-eval-dev-v2-test"
        manifest["questionSetVersion"] = "questions-v2-test"
        manifest["annotationVersion"] = "annotations-v2-test"
        seed_ids = json.dumps(
            [row["id"] for row in seed_rows],
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode("utf-8")
        manifest["seedQuestionSet"] = {
            "path": "docs/eval/releases/seed-v1.jsonl",
            "sha256": cls.sha256(seed_path),
            "bytes": seed_path.stat().st_size,
            "sampleCount": seed_count,
            "orderedSampleIdsSha256": hashlib.sha256(seed_ids).hexdigest(),
        }
        manifest["seedReleaseManifest"] = {
            "path": "docs/eval/releases/seed-v1-manifest.json",
            "sha256": cls.sha256(seed_manifest_path),
            "bytes": seed_manifest_path.stat().st_size,
        }
        manifest["annotationReview"] = {
            "path": "docs/eval/review/review-v2.jsonl",
            "sha256": cls.sha256(review_path),
            "bytes": review_path.stat().st_size,
            "reviewedSampleCount": len(rows),
        }
        manifest["expandedDataset"] = cls.expanded_contract(rows, seed_count=seed_count)
        quotas = manifest["expandedDataset"]["quotas"]
        manifest["distribution"] = {
            "type": quotas["type"],
            "difficulty": quotas["difficulty"],
            "shouldAnswer": quotas["shouldAnswer"],
        }
        cls.write_manifest(repo_root, manifest)

    @classmethod
    def copy_tracked_release(cls, repo_root: Path) -> dict:
        source_root = Path(__file__).resolve().parents[1]
        manifest = json.loads(
            (source_root / "docs/eval/dataset-manifest.json").read_text(encoding="utf-8")
        )
        descriptors = [manifest["questionSet"], manifest["sampleSchema"], *manifest["fixtures"]]
        for descriptor in descriptors:
            source = source_root / descriptor["path"]
            target = repo_root / descriptor["path"]
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
        cls.write_manifest(repo_root, manifest)
        return manifest

    @staticmethod
    def write_manifest(repo_root: Path, manifest: dict) -> None:
        target = repo_root / "docs/eval/dataset-manifest.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    @classmethod
    def write_rows_and_refresh_manifest(
        cls,
        repo_root: Path,
        manifest: dict,
        rows: list[dict],
    ) -> None:
        eval_path = repo_root / manifest["questionSet"]["path"]
        eval_path.write_text(
            "\n".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in rows) + "\n",
            encoding="utf-8",
        )
        manifest["questionSet"]["sha256"] = cls.sha256(eval_path)
        manifest["questionSet"]["bytes"] = eval_path.stat().st_size
        manifest["questionSet"]["sampleCount"] = len(rows)
        ordered_ids = json.dumps(
            [row.get("id", "") for row in rows],
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode("utf-8")
        manifest["questionSet"]["orderedSampleIdsSha256"] = hashlib.sha256(ordered_ids).hexdigest()
        cls.write_manifest(repo_root, manifest)


if __name__ == "__main__":
    unittest.main()
