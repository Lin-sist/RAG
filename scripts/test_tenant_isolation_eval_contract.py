#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import tempfile
import unittest
from pathlib import Path

import tenant_isolation_eval_contract as contract


class TenantIsolationEvalContractTest(unittest.TestCase):
    def test_tracked_release_is_valid_and_covers_every_category(self) -> None:
        repo_root = Path(__file__).resolve().parents[1]
        identity = contract.validate_release(
            repo_root,
            Path("docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json"),
        )

        self.assertEqual(26, identity["caseCount"])
        self.assertEqual(set(contract.ALLOWED_CATEGORIES), set(identity["distribution"]["category"]))
        self.assertEqual("tenant-isolation-evidence-map-v1", identity["evidenceMapVersion"])
        self.assertEqual(26, identity["evidenceMapCaseCount"])

    def test_valid_release_returns_stable_identity(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)

            identity = contract.validate_release(repo_root, manifest_path)

        self.assertEqual("tenant-isolation-adversarial-v1", identity["releaseVersion"])
        self.assertEqual(2, identity["caseCount"])
        self.assertEqual(["case-001", "case-002"], identity["orderedCaseIds"])
        self.assertEqual(0, identity["providerCallCount"])
        self.assertEqual(2, identity["evidenceMapCaseCount"])

    def test_evidence_map_missing_case_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            map_path = repo_root / manifest["artifacts"]["evidenceMap"]["path"]
            evidence_map = json.loads(map_path.read_text(encoding="utf-8"))
            del evidence_map["cases"]["case-001"]
            map_path.write_text(json.dumps(evidence_map), encoding="utf-8")
            manifest["artifacts"]["evidenceMap"] = self.descriptor(repo_root, map_path)
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("evidence_map_case_mismatch", raised.exception.code)

    def test_timing_evidence_key_must_match_case_id(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            map_path = repo_root / manifest["artifacts"]["evidenceMap"]["path"]
            evidence_map = json.loads(map_path.read_text(encoding="utf-8"))
            evidence_map["cases"]["case-002"]["driverEvidenceKey"] = "wrong-key"
            map_path.write_text(json.dumps(evidence_map), encoding="utf-8")
            manifest["artifacts"]["evidenceMap"] = self.descriptor(repo_root, map_path)
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("evidence_driver_key_mismatch", raised.exception.code)

    def test_case_artifact_hash_drift_fails_before_execution(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            cases_path = repo_root / "docs/eval/isolation/cases.jsonl"
            cases_path.write_text(cases_path.read_text(encoding="utf-8") + "\n", encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("artifact_hash_mismatch", raised.exception.code)

    def test_missing_artifact_fails_before_execution(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            cases_path = repo_root / "docs/eval/isolation/cases.jsonl"
            cases_path.unlink()

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("artifact_missing", raised.exception.code)

    def test_duplicate_case_id_is_invalid_even_when_manifest_hashes_match(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            self.rewrite_cases(repo_root, manifest_path, [
                self.case("case-001", "id_guessing", "http", "foreign-kb", "control-404"),
                self.case("case-001", "timing_disclosure", "timing", "foreign-kb", "case-001"),
            ])

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("duplicate_case_id", raised.exception.code)

    def test_unknown_driver_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            cases = [
                self.case("case-001", "id_guessing", "reflection", "foreign-kb", "control-404"),
                self.case("case-002", "timing_disclosure", "timing", "foreign-kb", "case-001"),
            ]
            self.rewrite_cases(repo_root, manifest_path, cases)

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("unknown_case_driver", raised.exception.code)

    def test_schema_registry_drift_is_invalid_even_when_hash_is_refreshed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            schema_path = repo_root / manifest["artifacts"]["schema"]["path"]
            schema = json.loads(schema_path.read_text(encoding="utf-8"))
            schema["drivers"] = ["timing"]
            schema_path.write_text(json.dumps(schema), encoding="utf-8")
            manifest["artifacts"]["schema"] = self.descriptor(repo_root, schema_path)
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("case_schema_contract_mismatch", raised.exception.code)

    def test_plan_only_returns_validated_identity_without_business_calls(self) -> None:
        repo_root = Path(__file__).resolve().parents[1]

        plan = contract.build_plan(
            repo_root,
            Path("docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json"),
        )

        self.assertEqual("VALID", plan["status"])
        self.assertEqual(26, plan["caseCount"])
        self.assertEqual(0, plan["providerCallCount"])
        self.assertFalse(plan["businessDataOutbound"])

    def test_unsafe_artifact_path_is_rejected_before_file_access(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            manifest["artifacts"]["cases"]["path"] = "../outside.jsonl"
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("unsafe_artifact_path", raised.exception.code)

    def test_disclosure_case_requires_an_existing_distinct_control(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            cases = [
                self.case("case-001", "id_guessing", "http", "foreign-kb", "control-404"),
                self.case("case-002", "timing_disclosure", "timing", "foreign-kb", "missing-control"),
            ]
            self.rewrite_cases(repo_root, manifest_path, cases)

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("missing_control_case", raised.exception.code)

    def test_distribution_quota_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            manifest["distribution"]["driver"] = {"http": 2}
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("case_distribution_mismatch", raised.exception.code)

    def test_manifest_case_count_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            manifest["caseCount"] = 3
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("case_count_mismatch", raised.exception.code)

    def test_manifest_case_order_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = Path(tmp_dir)
            manifest_path = self.write_minimal_release(repo_root)
            manifest_file = repo_root / manifest_path
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
            manifest["orderedCaseIdsSha256"] = "0" * 64
            manifest_file.write_text(json.dumps(manifest), encoding="utf-8")

            with self.assertRaises(contract.IsolationContractError) as raised:
                contract.validate_release(repo_root, manifest_path)

        self.assertEqual("case_order_mismatch", raised.exception.code)

    def write_minimal_release(self, repo_root: Path) -> Path:
        schema_path = repo_root / "docs/eval/schema/tenant-isolation-case-v1.json"
        cases_path = repo_root / "docs/eval/isolation/cases.jsonl"
        evidence_map_path = repo_root / "docs/eval/isolation/evidence-map.json"
        manifest_path = repo_root / "docs/eval/isolation/manifest.json"
        schema_path.parent.mkdir(parents=True)
        cases_path.parent.mkdir(parents=True)
        schema_path.write_text(json.dumps({
            "schemaVersion": "tenant-isolation-case-v1",
            "allowedFields": sorted(contract.CASE_FIELDS),
            "categories": sorted(contract.ALLOWED_CATEGORIES),
            "drivers": sorted(contract.ALLOWED_DRIVERS),
            "actors": sorted(contract.ALLOWED_ACTORS),
            "expectedFields": sorted(contract.EXPECTED_FIELDS),
            "identityBoundary": "synthetic-only-no-credentials-or-runtime-paths",
        }), encoding="utf-8")
        cases = [
            self.case("case-001", "id_guessing", "http", "foreign-kb", "control-404"),
            self.case("case-002", "timing_disclosure", "timing", "foreign-kb", "case-001"),
        ]
        cases_path.write_text(
            "".join(json.dumps(case, ensure_ascii=False, separators=(",", ":")) + "\n" for case in cases),
            encoding="utf-8",
        )
        evidence_map_path.write_text(json.dumps({
            "mapVersion": "tenant-isolation-evidence-map-v1",
            "releaseVersion": "tenant-isolation-adversarial-v1",
            "driverVersion": "tenant-isolation-driver-v1",
            "cases": {
                "case-001": {"selectors": [{
                    "report": "surefire",
                    "className": "example.IsolationTest",
                    "testNamePrefix": "case001IsIsolated",
                }]},
                "case-002": {
                    "selectors": [{
                        "report": "failsafe",
                        "className": "example.IsolationIT",
                        "testNamePrefix": "case002TimingIsOpaque",
                    }],
                    "driverEvidenceKey": "case-002",
                },
            },
        }), encoding="utf-8")
        manifest = {
            "manifestSchemaVersion": "tenant-isolation-manifest-v1",
            "releaseVersion": "tenant-isolation-adversarial-v1",
            "caseSchemaVersion": "tenant-isolation-case-v1",
            "driverVersion": "tenant-isolation-driver-v1",
            "fixtureVersion": "tenant-isolation-fixture-v1",
            "profileVersion": "tenant-isolation-profile-v1",
            "evidenceMapVersion": "tenant-isolation-evidence-map-v1",
            "artifacts": {
                "schema": self.descriptor(repo_root, schema_path),
                "cases": self.descriptor(repo_root, cases_path),
                "evidenceMap": self.descriptor(repo_root, evidence_map_path),
            },
            "caseCount": 2,
            "orderedCaseIdsSha256": hashlib.sha256(
                json.dumps(["case-001", "case-002"], separators=(",", ":")).encode("utf-8")
            ).hexdigest(),
            "distribution": {
                "category": {"id_guessing": 1, "timing_disclosure": 1},
                "driver": {"http": 1, "timing": 1},
                "required": {"true": 2, "false": 0},
            },
            "timingProfile": {
                "warmupPairs": 10,
                "measuredPairs": 40,
                "interleavingSeed": 14001,
                "medianAbsoluteFloorMs": 10.0,
                "p95AbsoluteFloorMs": 25.0,
                "relativeTolerance": 0.5,
            },
            "statusExitCodes": {"PASS": 0, "INVALID": 2, "FAIL": 3, "NOT_EVALUABLE": 4},
        }
        manifest_path.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
        return manifest_path.relative_to(repo_root)

    @staticmethod
    def case(case_id: str, category: str, driver: str, target: str, control: str) -> dict:
        return {
            "id": case_id,
            "category": category,
            "driver": driver,
            "actor": "tenant-a-owner",
            "target": target,
            "mutation": "foreign-id",
            "controlCaseId": control,
            "expected": {
                "httpStatus": 404,
                "errorCode": "KB_001",
                "forbiddenCanaryClasses": ["foreign-content", "foreign-identity"],
                "stateInvariants": ["foreign-state-unchanged"],
            },
            "required": True,
        }

    @staticmethod
    def descriptor(repo_root: Path, path: Path) -> dict:
        raw = path.read_bytes()
        return {
            "path": path.relative_to(repo_root).as_posix(),
            "bytes": len(raw),
            "sha256": hashlib.sha256(raw).hexdigest(),
        }

    def rewrite_cases(self, repo_root: Path, manifest_path: Path, cases: list[dict]) -> None:
        manifest_file = repo_root / manifest_path
        manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
        cases_path = repo_root / manifest["artifacts"]["cases"]["path"]
        cases_path.write_text(
            "".join(json.dumps(case, ensure_ascii=False, separators=(",", ":")) + "\n" for case in cases),
            encoding="utf-8",
        )
        manifest["artifacts"]["cases"] = self.descriptor(repo_root, cases_path)
        manifest["caseCount"] = len(cases)
        manifest["orderedCaseIdsSha256"] = hashlib.sha256(
            json.dumps([case["id"] for case in cases], separators=(",", ":")).encode("utf-8")
        ).hexdigest()
        manifest_file.write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
