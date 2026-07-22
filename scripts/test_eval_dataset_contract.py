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
