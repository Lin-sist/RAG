#!/usr/bin/env python3
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import evaluate_tenant_isolation as evaluator
import tenant_isolation_eval_contract as contract


class EvaluateTenantIsolationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[1]
        self.manifest_path = Path(
            "docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json"
        )
        self.identity = contract.validate_release(self.repo_root, self.manifest_path)
        cases_path = self.repo_root / "docs/eval/isolation/tenant-isolation-adversarial-v1-cases.jsonl"
        self.cases = [json.loads(line) for line in cases_path.read_text(encoding="utf-8").splitlines()]

    def test_complete_evidence_passes_all_four_channels(self) -> None:
        result = evaluator.evaluate_evidence(
            self.repo_root,
            self.manifest_path,
            self.complete_evidence(),
        )

        self.assertEqual("PASS", result["reportStatus"])
        self.assertEqual(0, result["exitCode"])
        self.assertEqual("PASS", result["functionalIsolationStatus"])
        self.assertEqual("PASS", result["contentDisclosureStatus"])
        self.assertEqual("PASS", result["errorDisclosureStatus"])
        self.assertEqual("PASS", result["timingDisclosureStatus"])
        self.assertEqual(26, result["caseCounts"]["expected"])
        self.assertEqual(0, result["caseCounts"]["missing"])

    def test_missing_required_case_is_not_evaluable(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"].pop()

        result = evaluator.evaluate_evidence(
            self.repo_root,
            self.manifest_path,
            evidence,
        )

        self.assertEqual("NOT_EVALUABLE", result["reportStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual(1, result["caseCounts"]["missing"])

    def test_duplicate_case_result_is_invalid(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"].append(dict(evidence["cases"][0]))

        result = evaluator.evaluate_evidence(
            self.repo_root,
            self.manifest_path,
            evidence,
        )

        self.assertEqual("INVALID", result["reportStatus"])
        self.assertEqual(2, result["exitCode"])
        self.assertEqual("duplicate_case_result", result["reason"])

    def test_foreign_canary_or_state_change_fails_required_channels(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"][0]["forbiddenCanaryCount"] = 1
        evidence["cases"][0]["foreignStateUnchanged"] = False

        result = evaluator.evaluate_evidence(
            self.repo_root,
            self.manifest_path,
            evidence,
        )

        self.assertEqual("FAIL", result["reportStatus"])
        self.assertEqual(3, result["exitCode"])
        self.assertEqual("FAIL", result["functionalIsolationStatus"])
        self.assertEqual("FAIL", result["contentDisclosureStatus"])

    def test_incomplete_timing_profile_is_not_evaluable(self) -> None:
        evidence = self.complete_evidence()
        timing_result = next(
            item for item in evidence["cases"]
            if item["checks"]["timingDisclosure"] == "PASS"
        )
        timing_result["timing"]["measuredPairs"] = 39
        timing_result["checks"]["timingDisclosure"] = "NOT_EVALUABLE"

        result = evaluator.evaluate_evidence(
            self.repo_root,
            self.manifest_path,
            evidence,
        )

        self.assertEqual("NOT_EVALUABLE", result["reportStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual("NOT_EVALUABLE", result["timingDisclosureStatus"])

    def test_formal_output_refuses_to_overwrite_existing_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "details.json"
            evaluator._write_new(path, "first\n")

            with self.assertRaises(FileExistsError):
                evaluator._write_new(path, "second\n")

            self.assertEqual("first\n", path.read_text(encoding="utf-8"))

    def test_unexpected_case_result_is_invalid(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"][0]["id"] = "unexpected-case"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("INVALID", result["reportStatus"])
        self.assertEqual("unexpected_case_result", result["reason"])

    def test_case_error_cannot_be_hidden_by_passing_checks(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"][0]["outcome"] = "ERROR"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("NOT_EVALUABLE", result["reportStatus"])
        self.assertEqual(1, result["caseCounts"]["errors"])

    def test_required_skip_cannot_be_hidden_by_passing_checks(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"][0]["outcome"] = "SKIPPED"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("NOT_EVALUABLE", result["reportStatus"])
        self.assertEqual(1, result["caseCounts"]["skipped"])

    def test_error_channel_not_evaluable_blocks_global_pass(self) -> None:
        evidence = self.complete_evidence()
        error_result = next(
            item for item in evidence["cases"]
            if item["checks"]["errorDisclosure"] == "PASS"
        )
        error_result["checks"]["errorDisclosure"] = "NOT_EVALUABLE"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("NOT_EVALUABLE", result["reportStatus"])
        self.assertEqual("NOT_EVALUABLE", result["errorDisclosureStatus"])

    def test_evidence_identity_drift_is_invalid(self) -> None:
        evidence = self.complete_evidence()
        evidence["driverVersion"] = "tenant-isolation-driver-v2"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("INVALID", result["reportStatus"])
        self.assertEqual("evidence_identity_mismatch", result["reason"])

    def complete_evidence(self) -> dict:
        results = []
        for case in self.cases:
            checks = {
                "functionalIsolation": "PASS",
                "contentDisclosure": "PASS",
                "errorDisclosure": "PASS" if case["category"] == "error_disclosure" else "NOT_APPLICABLE",
                "timingDisclosure": "PASS" if case["category"] == "timing_disclosure" else "NOT_APPLICABLE",
            }
            result = {
                "id": case["id"],
                "outcome": "PASS",
                "checks": checks,
                "forbiddenCanaryCount": 0,
                "foreignStateUnchanged": True,
                "errorCategory": "none",
            }
            if case["category"] == "timing_disclosure":
                result["timing"] = {
                    "warmupPairs": 10,
                    "measuredPairs": 40,
                    "interleavingSeed": 14001,
                    "foreignMedianMs": 4.0,
                    "controlMedianMs": 4.5,
                    "foreignP95Ms": 8.0,
                    "controlP95Ms": 9.0,
                    "requestErrors": 0,
                }
            else:
                result["timing"] = None
            results.append(result)
        return {
            "schemaVersion": "tenant-isolation-evidence-v1",
            "releaseVersion": self.identity["releaseVersion"],
            "manifestSha256": self.identity["manifestSha256"],
            "driverVersion": self.identity["driverVersion"],
            "fixtureVersion": self.identity["fixtureVersion"],
            "profileVersion": self.identity["profileVersion"],
            "gitHead": "a" * 40,
            "infrastructure": {
                "owned": True,
                "healthy": True,
                "mysqlImage": "mysql:8.0.36",
                "redisImage": "redis:7-alpine@sha256:test-only",
                "milvusImage": "milvusdb/milvus:v2.3.4",
            },
            "providerCallCount": 0,
            "businessDataOutbound": False,
            "realMaintenanceStatus": "SKIPPED",
            "cases": results,
        }


if __name__ == "__main__":
    unittest.main()
