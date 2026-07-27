#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from pathlib import PurePosixPath
from typing import Any


MANIFEST_SCHEMA_VERSION = "tenant-isolation-manifest-v1"
RELEASE_VERSION = "tenant-isolation-adversarial-v1"
CASE_SCHEMA_VERSION = "tenant-isolation-case-v1"
DRIVER_VERSION = "tenant-isolation-driver-v1"
FIXTURE_VERSION = "tenant-isolation-fixture-v1"
PROFILE_VERSION = "tenant-isolation-profile-v1"
EVIDENCE_MAP_VERSION = "tenant-isolation-evidence-map-v1"
ALLOWED_CATEGORIES = {
    "identity_override",
    "id_guessing",
    "public_permission",
    "reserved_filter",
    "cache_idempotency",
    "task_recovery",
    "vector_keyword",
    "rag_sync",
    "rag_stream",
    "history_feedback",
    "error_disclosure",
    "timing_disclosure",
}
ALLOWED_DRIVERS = {"http", "sse", "cache", "task_recovery", "vector", "keyword", "timing"}
ALLOWED_ACTORS = {
    "tenant-a-owner",
    "tenant-a-reader",
    "tenant-a-user",
    "anonymous",
    "invalid-token",
    "system-recovery",
}
CASE_FIELDS = {
    "id",
    "category",
    "driver",
    "actor",
    "target",
    "mutation",
    "controlCaseId",
    "expected",
    "required",
}
EXPECTED_FIELDS = {
    "httpStatus",
    "errorCode",
    "forbiddenCanaryClasses",
    "stateInvariants",
}
EVIDENCE_MAP_FIELDS = {"mapVersion", "releaseVersion", "driverVersion", "cases"}
EVIDENCE_CASE_FIELDS = {"selectors"}
EVIDENCE_SELECTOR_FIELDS = {"report", "className", "testNamePrefix"}
ALLOWED_REPORT_TYPES = {"surefire", "failsafe"}


class IsolationContractError(ValueError):
    def __init__(self, code: str, artifact: str, detail: str) -> None:
        self.code = code
        self.artifact = artifact
        self.detail = detail
        super().__init__(f"{code}: artifact={artifact} detail={detail}")


def _load_object(path: Path, artifact: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise IsolationContractError("artifact_missing", artifact, "file does not exist") from exc
    except json.JSONDecodeError as exc:
        raise IsolationContractError("artifact_invalid_json", artifact, "invalid JSON") from exc
    if not isinstance(value, dict):
        raise IsolationContractError("artifact_not_object", artifact, "JSON root must be object")
    return value


def _read_cases(path: Path) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    try:
        with path.open("r", encoding="utf-8") as file:
            for line_number, line in enumerate(file, start=1):
                if not line.strip():
                    continue
                try:
                    value = json.loads(line)
                except json.JSONDecodeError as exc:
                    raise IsolationContractError(
                        "case_invalid_json", path.as_posix(), f"line={line_number}"
                    ) from exc
                if not isinstance(value, dict):
                    raise IsolationContractError(
                        "case_not_object", path.as_posix(), f"line={line_number}"
                    )
                cases.append(value)
    except FileNotFoundError as exc:
        raise IsolationContractError("artifact_missing", path.as_posix(), "file does not exist") from exc
    return cases


def _validate_artifact(root: Path, descriptor: Any, name: str) -> Path:
    if not isinstance(descriptor, dict):
        raise IsolationContractError("manifest_invalid", name, "descriptor must be object")
    value = descriptor.get("path")
    if not isinstance(value, str) or not value.strip():
        raise IsolationContractError("unsafe_artifact_path", name, "path must be repo-relative")
    normalized = value.replace("\\", "/")
    pure = PurePosixPath(normalized)
    if pure.is_absolute() or re.match(r"^[A-Za-z]:/", normalized) or ".." in pure.parts:
        raise IsolationContractError("unsafe_artifact_path", name, "path must be repo-relative")
    resolved = (root / Path(*pure.parts)).resolve()
    try:
        resolved.relative_to(root)
    except ValueError as exc:
        raise IsolationContractError("unsafe_artifact_path", name, "path escapes repo") from exc
    try:
        raw = resolved.read_bytes()
    except FileNotFoundError as exc:
        raise IsolationContractError("artifact_missing", normalized, "file does not exist") from exc
    if descriptor.get("bytes") != len(raw) or descriptor.get("sha256") != hashlib.sha256(raw).hexdigest():
        raise IsolationContractError("artifact_hash_mismatch", normalized, "bytes or sha256 drifted")
    return resolved


def validate_release(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    root = repo_root.resolve()
    manifest_file = (root / manifest_path).resolve()
    manifest = _load_object(manifest_file, manifest_path.as_posix())
    expected_versions = {
        "manifestSchemaVersion": MANIFEST_SCHEMA_VERSION,
        "releaseVersion": RELEASE_VERSION,
        "caseSchemaVersion": CASE_SCHEMA_VERSION,
        "driverVersion": DRIVER_VERSION,
        "fixtureVersion": FIXTURE_VERSION,
        "profileVersion": PROFILE_VERSION,
        "evidenceMapVersion": EVIDENCE_MAP_VERSION,
    }
    for field, expected in expected_versions.items():
        if manifest.get(field) != expected:
            raise IsolationContractError("release_identity_mismatch", manifest_path.as_posix(), field)

    artifacts = manifest.get("artifacts")
    if not isinstance(artifacts, dict):
        raise IsolationContractError("manifest_invalid", manifest_path.as_posix(), "artifacts")
    schema_path = _validate_artifact(root, artifacts.get("schema"), "schema")
    cases_path = _validate_artifact(root, artifacts.get("cases"), "cases")
    evidence_map_path = _validate_artifact(root, artifacts.get("evidenceMap"), "evidenceMap")
    schema = _load_object(schema_path, "schema")
    if schema.get("schemaVersion") != CASE_SCHEMA_VERSION:
        raise IsolationContractError("case_schema_mismatch", schema_path.as_posix(), "schemaVersion")
    schema_contract = {
        "allowedFields": CASE_FIELDS,
        "categories": ALLOWED_CATEGORIES,
        "drivers": ALLOWED_DRIVERS,
        "actors": ALLOWED_ACTORS,
        "expectedFields": EXPECTED_FIELDS,
    }
    for field, expected_values in schema_contract.items():
        values = schema.get(field)
        if not isinstance(values, list) or set(values) != expected_values or len(values) != len(expected_values):
            raise IsolationContractError("case_schema_contract_mismatch", schema_path.as_posix(), field)
    if schema.get("identityBoundary") != "synthetic-only-no-credentials-or-runtime-paths":
        raise IsolationContractError(
            "case_schema_contract_mismatch", schema_path.as_posix(), "identityBoundary"
        )
    cases = _read_cases(cases_path)
    ordered_ids = [str(case.get("id", "")) for case in cases]
    if len(set(ordered_ids)) != len(ordered_ids):
        raise IsolationContractError("duplicate_case_id", cases_path.as_posix(), "case IDs must be unique")
    case_ids = set(ordered_ids)
    for index, case in enumerate(cases, start=1):
        artifact = f"{cases_path.as_posix()}:{index}"
        if set(case) != CASE_FIELDS:
            raise IsolationContractError("case_fields_invalid", artifact, "case fields must exact match")
        case_id = case.get("id")
        if not isinstance(case_id, str) or not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,63}", case_id):
            raise IsolationContractError("case_id_invalid", artifact, "id")
        if case.get("category") not in ALLOWED_CATEGORIES:
            raise IsolationContractError("unknown_case_category", artifact, "category")
        if case.get("driver") not in ALLOWED_DRIVERS:
            raise IsolationContractError("unknown_case_driver", artifact, "driver")
        if case.get("actor") not in ALLOWED_ACTORS:
            raise IsolationContractError("unknown_case_actor", artifact, "actor")
        for field in ("target", "mutation"):
            value = case.get(field)
            if not isinstance(value, str) or not re.fullmatch(r"[a-z0-9][a-z0-9-]{1,63}", value):
                raise IsolationContractError("case_value_invalid", artifact, field)
        if not isinstance(case.get("required"), bool):
            raise IsolationContractError("case_required_invalid", artifact, "required")
        expected = case.get("expected")
        if not isinstance(expected, dict) or set(expected) != EXPECTED_FIELDS:
            raise IsolationContractError("case_expected_invalid", artifact, "expected fields")
        if not isinstance(expected.get("httpStatus"), int) or isinstance(expected.get("httpStatus"), bool):
            raise IsolationContractError("case_expected_invalid", artifact, "httpStatus")
        if not isinstance(expected.get("errorCode"), str):
            raise IsolationContractError("case_expected_invalid", artifact, "errorCode")
        for field in ("forbiddenCanaryClasses", "stateInvariants"):
            values = expected.get(field)
            if not isinstance(values, list) or not values or not all(isinstance(value, str) and value for value in values):
                raise IsolationContractError("case_expected_invalid", artifact, field)
        if case.get("category") in {"error_disclosure", "timing_disclosure"}:
            control = case.get("controlCaseId")
            if not isinstance(control, str) or control not in case_ids or control == case_id:
                raise IsolationContractError("missing_control_case", artifact, "controlCaseId")
    ordered_hash = hashlib.sha256(
        json.dumps(ordered_ids, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    ).hexdigest()
    if manifest.get("caseCount") != len(cases):
        raise IsolationContractError("case_count_mismatch", cases_path.as_posix(), "caseCount")
    if manifest.get("orderedCaseIdsSha256") != ordered_hash:
        raise IsolationContractError("case_order_mismatch", cases_path.as_posix(), "ordered IDs")

    distribution = manifest.get("distribution")
    actual_distribution = {
        "category": dict(sorted(Counter(str(case["category"]) for case in cases).items())),
        "driver": dict(sorted(Counter(str(case["driver"]) for case in cases).items())),
        "required": {
            "true": sum(1 for case in cases if case["required"] is True),
            "false": sum(1 for case in cases if case["required"] is False),
        },
    }
    if distribution != actual_distribution:
        raise IsolationContractError("case_distribution_mismatch", cases_path.as_posix(), "distribution")

    evidence_map = _load_object(evidence_map_path, "evidenceMap")
    if set(evidence_map) != EVIDENCE_MAP_FIELDS:
        raise IsolationContractError(
            "evidence_map_invalid", evidence_map_path.as_posix(), "root fields must exact match"
        )
    if evidence_map.get("mapVersion") != EVIDENCE_MAP_VERSION:
        raise IsolationContractError(
            "evidence_map_identity_mismatch", evidence_map_path.as_posix(), "mapVersion"
        )
    if evidence_map.get("releaseVersion") != RELEASE_VERSION:
        raise IsolationContractError(
            "evidence_map_identity_mismatch", evidence_map_path.as_posix(), "releaseVersion"
        )
    if evidence_map.get("driverVersion") != DRIVER_VERSION:
        raise IsolationContractError(
            "evidence_map_identity_mismatch", evidence_map_path.as_posix(), "driverVersion"
        )
    evidence_cases = evidence_map.get("cases")
    if not isinstance(evidence_cases, dict) or set(evidence_cases) != case_ids:
        raise IsolationContractError(
            "evidence_map_case_mismatch", evidence_map_path.as_posix(), "case IDs must exact match release"
        )
    safe_class_name = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+")
    safe_test_prefix = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]{2,127}")
    timing_case_ids = {str(case["id"]) for case in cases if case["category"] == "timing_disclosure"}
    for case_id in ordered_ids:
        mapping = evidence_cases[case_id]
        artifact = f"{evidence_map_path.as_posix()}:{case_id}"
        allowed_fields = EVIDENCE_CASE_FIELDS | ({"driverEvidenceKey"} if case_id in timing_case_ids else set())
        if not isinstance(mapping, dict) or set(mapping) != allowed_fields:
            raise IsolationContractError("evidence_map_invalid", artifact, "case fields must exact match")
        selectors = mapping.get("selectors")
        if not isinstance(selectors, list) or not selectors:
            raise IsolationContractError("evidence_map_invalid", artifact, "selectors must be non-empty")
        for selector in selectors:
            if not isinstance(selector, dict) or set(selector) != EVIDENCE_SELECTOR_FIELDS:
                raise IsolationContractError("evidence_selector_invalid", artifact, "selector fields")
            if selector.get("report") not in ALLOWED_REPORT_TYPES:
                raise IsolationContractError("evidence_selector_invalid", artifact, "report")
            if not isinstance(selector.get("className"), str) or not safe_class_name.fullmatch(
                selector["className"]
            ):
                raise IsolationContractError("evidence_selector_invalid", artifact, "className")
            if not isinstance(selector.get("testNamePrefix"), str) or not safe_test_prefix.fullmatch(
                selector["testNamePrefix"]
            ):
                raise IsolationContractError("evidence_selector_invalid", artifact, "testNamePrefix")
        if case_id in timing_case_ids:
            key = mapping.get("driverEvidenceKey")
            if key != case_id:
                raise IsolationContractError(
                    "evidence_driver_key_mismatch", artifact, "timing driver key must equal case ID"
                )

    timing = manifest.get("timingProfile")
    if not isinstance(timing, dict) or timing != {
        "warmupPairs": 10,
        "measuredPairs": 40,
        "interleavingSeed": 14001,
        "medianAbsoluteFloorMs": 10.0,
        "p95AbsoluteFloorMs": 25.0,
        "relativeTolerance": 0.5,
    }:
        raise IsolationContractError("timing_profile_mismatch", manifest_path.as_posix(), "timingProfile")
    if manifest.get("statusExitCodes") != {"PASS": 0, "INVALID": 2, "FAIL": 3, "NOT_EVALUABLE": 4}:
        raise IsolationContractError("status_contract_mismatch", manifest_path.as_posix(), "statusExitCodes")

    return {
        "manifestSchemaVersion": MANIFEST_SCHEMA_VERSION,
        "releaseVersion": RELEASE_VERSION,
        "caseSchemaVersion": CASE_SCHEMA_VERSION,
        "driverVersion": DRIVER_VERSION,
        "fixtureVersion": FIXTURE_VERSION,
        "profileVersion": PROFILE_VERSION,
        "evidenceMapVersion": EVIDENCE_MAP_VERSION,
        "evidenceMapPath": evidence_map_path.relative_to(root).as_posix(),
        "evidenceMapCaseCount": len(evidence_cases),
        "manifestPath": manifest_path.as_posix(),
        "manifestSha256": hashlib.sha256(manifest_file.read_bytes()).hexdigest(),
        "caseCount": len(cases),
        "orderedCaseIds": ordered_ids,
        "distribution": actual_distribution,
        "timingProfile": timing,
        "providerCallCount": 0,
    }


def build_plan(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    identity = validate_release(repo_root, manifest_path)
    return {
        "status": "VALID",
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "driverVersion": identity["driverVersion"],
        "fixtureVersion": identity["fixtureVersion"],
        "profileVersion": identity["profileVersion"],
        "caseCount": identity["caseCount"],
        "distribution": identity["distribution"],
        "timingProfile": identity["timingProfile"],
        "providerCallCount": 0,
        "businessDataOutbound": False,
        "executionStarted": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate the frozen C14 isolation release.")
    parser.add_argument(
        "--manifest",
        default="docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json",
    )
    parser.add_argument("--plan-only", action="store_true")
    args = parser.parse_args()
    if not args.plan_only:
        print("validation requires --plan-only", file=sys.stderr)
        return 2
    repo_root = Path(__file__).resolve().parents[1]
    try:
        plan = build_plan(repo_root, Path(args.manifest))
    except IsolationContractError as exc:
        print(json.dumps({"status": "INVALID", "error": exc.code}, separators=(",", ":")))
        return 2
    print(json.dumps(plan, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
