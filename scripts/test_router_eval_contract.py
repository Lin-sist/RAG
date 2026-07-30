#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
import unittest
from pathlib import Path

import router_eval_contract as contract


class RouterEvalContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[1]
        self.manifest_path = Path(
            "docs/eval/router/bounded-query-router-eval-v1-manifest.json"
        )

    def test_tracked_release_is_valid_and_zero_egress(self) -> None:
        identity = contract.validate_release(self.repo_root, self.manifest_path)

        self.assertEqual("bounded-query-router-eval-v1", identity["releaseVersion"])
        self.assertEqual(20, identity["selectionCount"])
        self.assertEqual({"FACT": 10, "UNSUPPORTED": 10, "INVALID": 0},
                         identity["distribution"]["expectedIntent"])
        self.assertEqual(0, identity["providerCallCount"])
        self.assertFalse(identity["businessDataOutbound"])

    def test_plan_is_local_and_does_not_start_execution(self) -> None:
        plan = contract.build_plan(self.repo_root, self.manifest_path)

        self.assertEqual("VALID", plan["status"])
        self.assertFalse(plan["executionStarted"])
        self.assertEqual("SKIPPED", plan["liveEvaluationStatus"])

    def test_missing_artifact_is_rejected(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            (root / manifest["artifacts"]["expectations"]["path"]).unlink()

            self.assert_contract_error("artifact_missing", root, manifest_path)

    def test_unsafe_artifact_path_is_rejected_before_read(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            manifest["artifacts"]["budget"]["path"] = "../outside.json"
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("unsafe_artifact_path", root, manifest_path)

    def test_hash_or_byte_drift_is_rejected(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            manifest["artifacts"]["budget"]["bytes"] += 1
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("artifact_hash_mismatch", root, manifest_path)

    def test_dataset_identity_mismatch_is_rejected(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            manifest["datasetSampleCount"] -= 1
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("dataset_count_mismatch", root, manifest_path)

    def test_selection_count_or_order_drift_is_rejected(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            manifest["selectionOrderedIdsSha256"] = "0" * 64
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("selection_order_mismatch", root, manifest_path)

    def test_duplicate_sidecar_id_is_rejected_after_descriptor_refresh(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            path = root / manifest["artifacts"]["expectations"]["path"]
            rows = path.read_text(encoding="utf-8").splitlines()
            rows[-1] = rows[0]
            self.rewrite_artifact(path, rows, manifest["artifacts"]["expectations"])
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("duplicate_sample_id", root, manifest_path)

    def test_missing_or_unexpected_selection_id_is_rejected(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            path = root / manifest["artifacts"]["expectations"]["path"]
            rows = path.read_text(encoding="utf-8").splitlines()
            row = json.loads(rows[0])
            row["sampleId"] = "not-in-dataset"
            rows[0] = json.dumps(row, ensure_ascii=False, separators=(",", ":"))
            self.rewrite_artifact(path, rows, manifest["artifacts"]["expectations"])
            ids = [json.loads(line)["sampleId"] for line in rows]
            manifest["selectionOrderedIdsSha256"] = contract.ordered_ids_hash(ids)
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("unexpected_sample_id", root, manifest_path)

    def test_unknown_enum_and_channel_are_rejected(self) -> None:
        with self.subTest("enum"):
            with self.copied_release() as (root, manifest_path):
                manifest = self.load_manifest(root, manifest_path)
                path = root / manifest["artifacts"]["expectations"]["path"]
                rows = path.read_text(encoding="utf-8").splitlines()
                row = json.loads(rows[0])
                row["expectedIntent"] = "AGENTIC"
                rows[0] = json.dumps(row, ensure_ascii=False, separators=(",", ":"))
                self.rewrite_artifact(path, rows, manifest["artifacts"]["expectations"])
                self.write_manifest(root, manifest_path, manifest)
                self.assert_contract_error("unknown_expected_intent", root, manifest_path)
        with self.subTest("channel"):
            with self.copied_release() as (root, manifest_path):
                manifest = self.load_manifest(root, manifest_path)
                manifest["requiredChannels"][0] = "agentic_planner"
                self.write_manifest(root, manifest_path, manifest)
                self.assert_contract_error("channel_contract_mismatch", root, manifest_path)

    def test_sidecar_coverage_gap_is_rejected(self) -> None:
        with self.copied_release() as (root, manifest_path):
            manifest = self.load_manifest(root, manifest_path)
            path = root / manifest["artifacts"]["expectations"]["path"]
            rows = path.read_text(encoding="utf-8").splitlines()[:-1]
            self.rewrite_artifact(path, rows, manifest["artifacts"]["expectations"])
            ids = [json.loads(line)["sampleId"] for line in rows]
            manifest["selectionCount"] = len(rows)
            manifest["selectionOrderedIdsSha256"] = contract.ordered_ids_hash(ids)
            self.write_manifest(root, manifest_path, manifest)

            self.assert_contract_error("distribution_mismatch", root, manifest_path)

    def assert_contract_error(self, code: str, root: Path, manifest_path: Path) -> None:
        with self.assertRaises(contract.RouterEvalContractError) as raised:
            contract.validate_release(root, manifest_path)
        self.assertEqual(code, raised.exception.code)

    def copied_release(self):
        temp = tempfile.TemporaryDirectory()
        root = Path(temp.name)
        for relative in (
            self.manifest_path,
            Path("docs/eval/router/bounded-query-router-expectation-v1.schema.json"),
            Path("docs/eval/router/bounded-query-router-eval-v1-expectations.jsonl"),
            Path("docs/eval/router/bounded-query-router-budget-v1.json"),
            Path("docs/eval/releases/rag-eval-dev-v2.jsonl"),
        ):
            target = root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.repo_root / relative, target)

        class ReleaseContext:
            def __enter__(self_nonlocal):
                return root, self.manifest_path

            def __exit__(self_nonlocal, exc_type, exc, tb):
                temp.cleanup()

        return ReleaseContext()

    @staticmethod
    def load_manifest(root: Path, manifest_path: Path) -> dict:
        return json.loads((root / manifest_path).read_text(encoding="utf-8"))

    @staticmethod
    def write_manifest(root: Path, manifest_path: Path, manifest: dict) -> None:
        (root / manifest_path).write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    @staticmethod
    def rewrite_artifact(path: Path, rows: list[str], descriptor: dict) -> None:
        path.write_text("\n".join(rows) + "\n", encoding="utf-8")
        raw = path.read_bytes()
        descriptor["bytes"] = len(raw)
        descriptor["sha256"] = hashlib.sha256(raw).hexdigest()


if __name__ == "__main__":
    unittest.main()
