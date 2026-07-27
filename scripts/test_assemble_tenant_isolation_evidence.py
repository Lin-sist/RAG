#!/usr/bin/env python3
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import assemble_tenant_isolation_evidence as assembler
import tenant_isolation_eval_contract as contract


class AssembleTenantIsolationEvidenceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[1]
        self.manifest_path = Path(
            "docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json"
        )
        self.identity = contract.validate_release(self.repo_root, self.manifest_path)
        self.evidence_map = json.loads(
            (self.repo_root / self.identity["evidenceMapPath"]).read_text(encoding="utf-8")
        )

    def test_complete_mapped_junit_and_driver_evidence_assembles_26_passes(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            report_dirs = self.write_reports(root)
            driver_path = self.write_driver(root)

            evidence = assembler.assemble_evidence(
                self.repo_root,
                self.manifest_path,
                report_dirs,
                driver_path,
                git_head="a" * 40,
            )

        self.assertEqual(26, len(evidence["cases"]))
        self.assertTrue(all(item["outcome"] == "PASS" for item in evidence["cases"]))
        self.assertEqual(0, evidence["providerCallCount"])
        timing = [item for item in evidence["cases"] if item["timing"] is not None]
        self.assertEqual(2, len(timing))
        self.assertTrue(all(item["checks"]["timingDisclosure"] == "PASS" for item in timing))

    def test_missing_mapped_testcase_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            report_dirs = self.write_reports(root, omit_first=True)
            driver_path = self.write_driver(root)

            with self.assertRaises(assembler.EvidenceAssemblyError) as raised:
                assembler.assemble_evidence(
                    self.repo_root,
                    self.manifest_path,
                    report_dirs,
                    driver_path,
                    git_head="a" * 40,
                )

        self.assertEqual("mapped_test_missing", raised.exception.code)

    def test_failed_junit_test_becomes_failed_case_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            report_dirs = self.write_reports(root, fail_first=True)
            driver_path = self.write_driver(root)

            evidence = assembler.assemble_evidence(
                self.repo_root,
                self.manifest_path,
                report_dirs,
                driver_path,
                git_head="a" * 40,
            )

        self.assertTrue(any(item["outcome"] == "FAIL" for item in evidence["cases"]))

    def test_driver_head_must_match_evidence_head(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            report_dirs = self.write_reports(root)
            driver_path = self.write_driver(root, git_head="b" * 40)

            with self.assertRaises(assembler.EvidenceAssemblyError) as raised:
                assembler.assemble_evidence(
                    self.repo_root,
                    self.manifest_path,
                    report_dirs,
                    driver_path,
                    git_head="a" * 40,
                )

        self.assertEqual("driver_identity_mismatch", raised.exception.code)

    def test_parameterized_selector_requires_every_expected_invocation_to_pass(self) -> None:
        selector = {
            "report": "surefire",
            "className": "example.ParameterizedTest",
            "testNamePrefix": "rejectsReservedAlias",
            "expectedMatches": 2,
        }
        indexed = {
            "surefire": [
                {
                    "className": "example.ParameterizedTest",
                    "testName": "rejectsReservedAlias(String)[1]",
                    "outcome": "PASS",
                    "source": "TEST-example.xml",
                },
                {
                    "className": "example.ParameterizedTest",
                    "testName": "rejectsReservedAlias(String)[2]",
                    "outcome": "FAIL",
                    "source": "TEST-example.xml",
                },
            ],
            "failsafe": [],
        }

        self.assertEqual("FAIL", assembler._selector_outcome(indexed, selector))

    def write_reports(
        self,
        root: Path,
        *,
        omit_first: bool = False,
        fail_first: bool = False,
    ) -> dict[str, list[Path]]:
        report_dirs = {"surefire": [root / "surefire"], "failsafe": [root / "failsafe"]}
        selectors = []
        seen = set()
        for mapping in self.evidence_map["cases"].values():
            for selector in mapping["selectors"]:
                key = (selector["report"], selector["className"], selector["testNamePrefix"])
                if key not in seen:
                    seen.add(key)
                    selectors.append(selector)
        if omit_first:
            selectors.pop(0)
        by_report: dict[str, list[dict]] = {"surefire": [], "failsafe": []}
        for selector in selectors:
            by_report[selector["report"]].append(selector)
        for report_type, values in by_report.items():
            report_dir = report_dirs[report_type][0]
            report_dir.mkdir(parents=True)
            testcases = []
            for index, selector in enumerate(values):
                child = "<failure message=\"synthetic failure\"/>" if fail_first and index == 0 else ""
                expected_matches = selector.get("expectedMatches", 1)
                for match_index in range(expected_matches):
                    suffix = f"(String)[{match_index + 1}]" if expected_matches > 1 else "()"
                    testcases.append(
                        f'<testcase classname="{selector["className"]}" '
                        f'name="{selector["testNamePrefix"]}{suffix}">{child}</testcase>'
                    )
            (report_dir / "TEST-c14.xml").write_text(
                "<testsuite>" + "".join(testcases) + "</testsuite>", encoding="utf-8"
            )
        return report_dirs

    def write_driver(self, root: Path, *, git_head: str = "a" * 40) -> Path:
        timing = {
            "warmupPairs": 10,
            "measuredPairs": 40,
            "interleavingSeed": 14001,
            "foreignMedianMs": 4.0,
            "controlMedianMs": 4.5,
            "foreignP95Ms": 8.0,
            "controlP95Ms": 9.0,
            "requestErrors": 0,
        }
        path = root / "driver.json"
        path.write_text(json.dumps({
            "schemaVersion": "tenant-isolation-driver-evidence-v1",
            "releaseVersion": self.identity["releaseVersion"],
            "driverVersion": self.identity["driverVersion"],
            "fixtureVersion": self.identity["fixtureVersion"],
            "profileVersion": self.identity["profileVersion"],
            "gitHead": git_head,
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
            "timing": {
                "timing-foreign-kb": timing,
                "timing-foreign-public-kb": timing,
            },
        }), encoding="utf-8")
        return path


if __name__ == "__main__":
    unittest.main()
