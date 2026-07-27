#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

import tenant_isolation_eval_contract as contract


DRIVER_EVIDENCE_SCHEMA_VERSION = "tenant-isolation-driver-evidence-v1"
EVIDENCE_SCHEMA_VERSION = "tenant-isolation-evidence-v1"
DRIVER_FIELDS = {
    "schemaVersion",
    "releaseVersion",
    "driverVersion",
    "fixtureVersion",
    "profileVersion",
    "gitHead",
    "infrastructure",
    "providerCallCount",
    "businessDataOutbound",
    "realMaintenanceStatus",
    "timing",
}
TIMING_FIELDS = {
    "warmupPairs",
    "measuredPairs",
    "interleavingSeed",
    "foreignMedianMs",
    "controlMedianMs",
    "foreignP95Ms",
    "controlP95Ms",
    "requestErrors",
}


class EvidenceAssemblyError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        self.code = code
        self.detail = detail
        super().__init__(f"{code}: {detail}")


def _load_object(path: Path, artifact: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise EvidenceAssemblyError("artifact_missing", artifact) from exc
    except json.JSONDecodeError as exc:
        raise EvidenceAssemblyError("artifact_invalid_json", artifact) from exc
    if not isinstance(value, dict):
        raise EvidenceAssemblyError("artifact_not_object", artifact)
    return value


def _read_junit(report_dirs: dict[str, list[Path]]) -> dict[str, list[dict[str, str]]]:
    indexed: dict[str, list[dict[str, str]]] = {"surefire": [], "failsafe": []}
    for report_type in indexed:
        for report_dir in report_dirs.get(report_type, []):
            if not report_dir.is_dir():
                continue
            for report_path in sorted(report_dir.glob("TEST-*.xml")):
                try:
                    root = ET.parse(report_path).getroot()
                except ET.ParseError as exc:
                    raise EvidenceAssemblyError("junit_invalid_xml", report_path.as_posix()) from exc
                for testcase in root.iter("testcase"):
                    class_name = testcase.get("classname")
                    test_name = testcase.get("name")
                    if not class_name or not test_name:
                        continue
                    if testcase.find("error") is not None:
                        outcome = "ERROR"
                    elif testcase.find("failure") is not None:
                        outcome = "FAIL"
                    elif testcase.find("skipped") is not None:
                        outcome = "SKIPPED"
                    else:
                        outcome = "PASS"
                    indexed[report_type].append({
                        "className": class_name,
                        "testName": test_name,
                        "outcome": outcome,
                        "source": report_path.as_posix(),
                    })
    return indexed


def _selector_outcome(
    indexed: dict[str, list[dict[str, str]]], selector: dict[str, str]
) -> str:
    matches = [
        item for item in indexed[selector["report"]]
        if item["className"] == selector["className"]
        and item["testName"].startswith(selector["testNamePrefix"])
    ]
    label = f"{selector['className']}#{selector['testNamePrefix']}"
    if not matches:
        raise EvidenceAssemblyError("mapped_test_missing", label)
    if len(matches) != 1:
        raise EvidenceAssemblyError("mapped_test_ambiguous", label)
    return matches[0]["outcome"]


def _timing_status(timing: Any, profile: dict[str, Any]) -> str:
    if not isinstance(timing, dict) or set(timing) != TIMING_FIELDS:
        return "NOT_EVALUABLE"
    for field in ("warmupPairs", "measuredPairs", "interleavingSeed"):
        if timing.get(field) != profile[field]:
            return "NOT_EVALUABLE"
    if timing.get("requestErrors") != 0:
        return "NOT_EVALUABLE"
    numbers: dict[str, float] = {}
    for field in ("foreignMedianMs", "controlMedianMs", "foreignP95Ms", "controlP95Ms"):
        value = timing.get(field)
        if not isinstance(value, (int, float)) or isinstance(value, bool) or value < 0:
            return "NOT_EVALUABLE"
        numbers[field] = float(value)
    median_limit = max(
        float(profile["medianAbsoluteFloorMs"]),
        float(profile["relativeTolerance"]) * numbers["controlMedianMs"],
    )
    p95_limit = max(
        float(profile["p95AbsoluteFloorMs"]),
        float(profile["relativeTolerance"]) * numbers["controlP95Ms"],
    )
    if abs(numbers["foreignMedianMs"] - numbers["controlMedianMs"]) > median_limit:
        return "FAIL"
    if abs(numbers["foreignP95Ms"] - numbers["controlP95Ms"]) > p95_limit:
        return "FAIL"
    return "PASS"


def _validate_driver(
    driver: dict[str, Any], identity: dict[str, Any], evidence_map: dict[str, Any], git_head: str
) -> None:
    if set(driver) != DRIVER_FIELDS:
        raise EvidenceAssemblyError("driver_contract_invalid", "root fields")
    expected = {
        "schemaVersion": DRIVER_EVIDENCE_SCHEMA_VERSION,
        "releaseVersion": identity["releaseVersion"],
        "driverVersion": identity["driverVersion"],
        "fixtureVersion": identity["fixtureVersion"],
        "profileVersion": identity["profileVersion"],
        "gitHead": git_head,
    }
    if any(driver.get(field) != value for field, value in expected.items()):
        raise EvidenceAssemblyError("driver_identity_mismatch", "release or Git HEAD")
    timing_keys = {
        mapping["driverEvidenceKey"]
        for mapping in evidence_map["cases"].values()
        if "driverEvidenceKey" in mapping
    }
    timing = driver.get("timing")
    if not isinstance(timing, dict) or set(timing) != timing_keys:
        raise EvidenceAssemblyError("driver_timing_mismatch", "timing keys")
    if driver.get("providerCallCount") != 0 or driver.get("businessDataOutbound") is not False:
        raise EvidenceAssemblyError("driver_external_call_boundary_failed", "provider or outbound data")


def assemble_evidence(
    repo_root: Path,
    manifest_path: Path,
    report_dirs: dict[str, list[Path]],
    driver_path: Path,
    *,
    git_head: str,
) -> dict[str, Any]:
    if not re.fullmatch(r"[0-9a-f]{40}", git_head):
        raise EvidenceAssemblyError("git_head_invalid", git_head)
    root = repo_root.resolve()
    identity = contract.validate_release(root, manifest_path)
    evidence_map = _load_object(root / identity["evidenceMapPath"], "evidenceMap")
    driver = _load_object(driver_path, "driverEvidence")
    _validate_driver(driver, identity, evidence_map, git_head)
    indexed = _read_junit(report_dirs)
    manifest = _load_object(root / manifest_path, "manifest")
    cases_path = root / manifest["artifacts"]["cases"]["path"]
    cases = [json.loads(line) for line in cases_path.read_text(encoding="utf-8").splitlines() if line]
    results = []
    priority = {"PASS": 0, "SKIPPED": 1, "FAIL": 2, "ERROR": 3}
    for case in cases:
        mapping = evidence_map["cases"][case["id"]]
        outcomes = [_selector_outcome(indexed, selector) for selector in mapping["selectors"]]
        outcome = max(outcomes, key=priority.__getitem__)
        channel_value = "PASS" if outcome == "PASS" else (
            "FAIL" if outcome == "FAIL" else "NOT_EVALUABLE"
        )
        timing = None
        timing_value = "NOT_APPLICABLE"
        if case["category"] == "timing_disclosure":
            timing = driver["timing"].get(mapping["driverEvidenceKey"])
            timing_value = _timing_status(timing, identity["timingProfile"])
        result = {
            "id": case["id"],
            "outcome": outcome,
            "checks": {
                "functionalIsolation": channel_value,
                "contentDisclosure": channel_value,
                "errorDisclosure": channel_value
                if case["category"] == "error_disclosure" else "NOT_APPLICABLE",
                "timingDisclosure": timing_value,
            },
            "forbiddenCanaryCount": 0,
            "foreignStateUnchanged": outcome == "PASS",
            "errorCategory": "none" if outcome == "PASS" else f"junit-{outcome.lower()}",
            "timing": timing,
        }
        results.append(result)
    return {
        "schemaVersion": EVIDENCE_SCHEMA_VERSION,
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "driverVersion": identity["driverVersion"],
        "fixtureVersion": identity["fixtureVersion"],
        "profileVersion": identity["profileVersion"],
        "gitHead": git_head,
        "infrastructure": driver["infrastructure"],
        "providerCallCount": driver["providerCallCount"],
        "businessDataOutbound": driver["businessDataOutbound"],
        "realMaintenanceStatus": driver["realMaintenanceStatus"],
        "cases": results,
    }


def _git_identity(repo_root: Path) -> str:
    status = subprocess.run(
        ["git", "status", "--porcelain"], cwd=repo_root, check=True,
        capture_output=True, text=True, encoding="utf-8",
    )
    if status.stdout.strip():
        raise EvidenceAssemblyError("git_worktree_dirty", "formal evidence requires clean HEAD")
    head = subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=repo_root, check=True,
        capture_output=True, text=True, encoding="utf-8",
    ).stdout.strip()
    if not re.fullmatch(r"[0-9a-f]{40}", head):
        raise EvidenceAssemblyError("git_head_invalid", head)
    return head


def _write_new(path: Path, content: str) -> None:
    if path.exists():
        raise FileExistsError(f"refusing to overwrite existing evidence: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Assemble mapped JUnit evidence for C14.")
    parser.add_argument(
        "--manifest", default="docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json"
    )
    parser.add_argument("--surefire-dir", action="append")
    parser.add_argument("--failsafe-dir", action="append")
    parser.add_argument(
        "--driver-evidence", default="rag-admin/target/c14-isolation-driver-evidence.json"
    )
    parser.add_argument("--output", required=True)
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.no_overwrite:
        print("formal C14 evidence requires --no-overwrite", file=sys.stderr)
        return 2
    repo_root = Path(__file__).resolve().parents[1]
    defaults = {
        "surefire": [repo_root / module / "target/surefire-reports"
                     for module in ("rag-common", "rag-core", "rag-admin")],
        "failsafe": [repo_root / "rag-admin/target/failsafe-reports"],
    }
    report_dirs = {
        "surefire": [Path(value) for value in args.surefire_dir] if args.surefire_dir else defaults["surefire"],
        "failsafe": [Path(value) for value in args.failsafe_dir] if args.failsafe_dir else defaults["failsafe"],
    }
    try:
        git_head = _git_identity(repo_root)
        evidence = assemble_evidence(
            repo_root,
            Path(args.manifest),
            report_dirs,
            Path(args.driver_evidence),
            git_head=git_head,
        )
        _write_new(Path(args.output), json.dumps(evidence, ensure_ascii=False, indent=2) + "\n")
    except (EvidenceAssemblyError, FileExistsError, subprocess.CalledProcessError) as exc:
        print(str(exc), file=sys.stderr)
        return 2
    print(f"Assembled {len(evidence['cases'])} mapped C14 cases at Git HEAD {git_head}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
