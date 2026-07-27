#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

import tenant_isolation_eval_contract as contract


EVIDENCE_SCHEMA_VERSION = "tenant-isolation-evidence-v1"
STATUS_EXIT_CODES = {"PASS": 0, "INVALID": 2, "FAIL": 3, "NOT_EVALUABLE": 4}
CHECK_VALUES = {"PASS", "FAIL", "NOT_EVALUABLE", "NOT_APPLICABLE"}
CASE_OUTCOMES = {"PASS", "FAIL", "ERROR", "SKIPPED"}


def _invalid(reason: str, identity: dict[str, Any] | None = None) -> dict[str, Any]:
    return {
        "reportStatus": "INVALID",
        "exitCode": STATUS_EXIT_CODES["INVALID"],
        "reason": reason,
        "releaseVersion": identity.get("releaseVersion") if identity else None,
        "functionalIsolationStatus": "INVALID",
        "contentDisclosureStatus": "INVALID",
        "errorDisclosureStatus": "INVALID",
        "timingDisclosureStatus": "INVALID",
        "caseCounts": {"expected": identity.get("caseCount", 0) if identity else 0,
                       "observed": 0, "missing": 0, "unexpected": 0, "failed": 0,
                       "errors": 0, "skipped": 0},
        "providerCallCount": None,
        "businessDataOutbound": None,
        "realMaintenanceStatus": None,
    }


def _load_cases(repo_root: Path, manifest_path: Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    identity = contract.validate_release(repo_root, manifest_path)
    manifest = json.loads((repo_root / manifest_path).read_text(encoding="utf-8"))
    case_path = repo_root / manifest["artifacts"]["cases"]["path"]
    cases = [json.loads(line) for line in case_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    return identity, cases


def _identity_matches(identity: dict[str, Any], evidence: dict[str, Any]) -> bool:
    expected = {
        "schemaVersion": EVIDENCE_SCHEMA_VERSION,
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "driverVersion": identity["driverVersion"],
        "fixtureVersion": identity["fixtureVersion"],
        "profileVersion": identity["profileVersion"],
    }
    return all(evidence.get(field) == value for field, value in expected.items())


def _aggregate(values: list[str], required_count: int) -> str:
    if len(values) != required_count or required_count == 0:
        return "NOT_EVALUABLE"
    if any(value == "FAIL" for value in values):
        return "FAIL"
    if any(value == "NOT_EVALUABLE" for value in values):
        return "NOT_EVALUABLE"
    return "PASS" if all(value == "PASS" for value in values) else "NOT_EVALUABLE"


def _timing_status(result: dict[str, Any], profile: dict[str, Any]) -> str:
    timing = result.get("timing")
    if not isinstance(timing, dict):
        return "NOT_EVALUABLE"
    for field in ("warmupPairs", "measuredPairs", "interleavingSeed"):
        if timing.get(field) != profile[field]:
            return "NOT_EVALUABLE"
    if timing.get("requestErrors") != 0:
        return "NOT_EVALUABLE"
    values: dict[str, float] = {}
    for field in ("foreignMedianMs", "controlMedianMs", "foreignP95Ms", "controlP95Ms"):
        value = timing.get(field)
        if not isinstance(value, (int, float)) or isinstance(value, bool) or value < 0:
            return "NOT_EVALUABLE"
        values[field] = float(value)
    median_limit = max(
        float(profile["medianAbsoluteFloorMs"]),
        float(profile["relativeTolerance"]) * values["controlMedianMs"],
    )
    p95_limit = max(
        float(profile["p95AbsoluteFloorMs"]),
        float(profile["relativeTolerance"]) * values["controlP95Ms"],
    )
    if abs(values["foreignMedianMs"] - values["controlMedianMs"]) > median_limit:
        return "FAIL"
    if abs(values["foreignP95Ms"] - values["controlP95Ms"]) > p95_limit:
        return "FAIL"
    return "PASS"


def evaluate_evidence(
    repo_root: Path,
    manifest_path: Path,
    evidence: Any,
) -> dict[str, Any]:
    try:
        identity, cases = _load_cases(repo_root.resolve(), manifest_path)
    except contract.IsolationContractError as exc:
        return _invalid(exc.code)
    if not isinstance(evidence, dict):
        return _invalid("evidence_not_object", identity)
    if not _identity_matches(identity, evidence):
        return _invalid("evidence_identity_mismatch", identity)
    git_head = evidence.get("gitHead")
    if not isinstance(git_head, str) or not re.fullmatch(r"[0-9a-f]{40}", git_head):
        return _invalid("git_head_invalid", identity)
    infrastructure = evidence.get("infrastructure")
    if not isinstance(infrastructure, dict):
        return _invalid("infrastructure_invalid", identity)
    required_infra = {"owned", "healthy", "mysqlImage", "redisImage", "milvusImage"}
    if set(infrastructure) != required_infra:
        return _invalid("infrastructure_invalid", identity)
    if not all(isinstance(infrastructure.get(field), str) and infrastructure[field]
               for field in ("mysqlImage", "redisImage", "milvusImage")):
        return _invalid("infrastructure_invalid", identity)

    observed = evidence.get("cases")
    if not isinstance(observed, list) or not all(isinstance(item, dict) for item in observed):
        return _invalid("case_results_invalid", identity)
    observed_ids = [item.get("id") for item in observed]
    if not all(isinstance(case_id, str) for case_id in observed_ids):
        return _invalid("case_result_id_invalid", identity)
    if len(set(observed_ids)) != len(observed_ids):
        return _invalid("duplicate_case_result", identity)
    expected_ids = identity["orderedCaseIds"]
    expected_set = set(expected_ids)
    observed_set = set(observed_ids)
    unexpected = sorted(observed_set - expected_set)
    if unexpected:
        return _invalid("unexpected_case_result", identity)

    by_id = {item["id"]: item for item in observed}
    missing = [case_id for case_id in expected_ids if case_id not in by_id]
    counts = Counter(str(item.get("outcome", "INVALID")) for item in observed)
    case_counts = {
        "expected": len(expected_ids),
        "observed": len(observed),
        "missing": len(missing),
        "unexpected": len(unexpected),
        "failed": counts["FAIL"],
        "errors": counts["ERROR"],
        "skipped": counts["SKIPPED"],
    }

    functional: list[str] = []
    content: list[str] = []
    error: list[str] = []
    timing: list[str] = []
    invalid_reason: str | None = None
    case_failures = False
    case_not_evaluable = bool(missing)
    profile = identity["timingProfile"]

    for case in cases:
        result = by_id.get(case["id"])
        if result is None:
            continue
        if set(result) != {
            "id", "outcome", "checks", "forbiddenCanaryCount", "foreignStateUnchanged",
            "errorCategory", "timing",
        }:
            invalid_reason = "case_result_fields_invalid"
            break
        outcome = result.get("outcome")
        checks = result.get("checks")
        if outcome not in CASE_OUTCOMES or not isinstance(checks, dict) or set(checks) != {
            "functionalIsolation", "contentDisclosure", "errorDisclosure", "timingDisclosure"
        } or any(value not in CHECK_VALUES for value in checks.values()):
            invalid_reason = "case_result_contract_invalid"
            break
        if not isinstance(result.get("forbiddenCanaryCount"), int) or result["forbiddenCanaryCount"] < 0:
            invalid_reason = "case_result_contract_invalid"
            break
        if not isinstance(result.get("foreignStateUnchanged"), bool):
            invalid_reason = "case_result_contract_invalid"
            break
        if not isinstance(result.get("errorCategory"), str):
            invalid_reason = "case_result_contract_invalid"
            break

        functional_value = checks["functionalIsolation"]
        content_value = checks["contentDisclosure"]
        if result["forbiddenCanaryCount"] > 0 or not result["foreignStateUnchanged"]:
            functional_value = "FAIL"
            content_value = "FAIL"
        if outcome == "FAIL":
            case_failures = True
        elif outcome in {"ERROR", "SKIPPED"}:
            case_not_evaluable = True
            if functional_value == "PASS":
                functional_value = "NOT_EVALUABLE"
            if content_value == "PASS":
                content_value = "NOT_EVALUABLE"
        functional.append(functional_value)
        content.append(content_value)

        if case["category"] == "error_disclosure":
            error.append(checks["errorDisclosure"])
        elif checks["errorDisclosure"] != "NOT_APPLICABLE":
            invalid_reason = "error_channel_not_applicable_mismatch"
            break
        if case["category"] == "timing_disclosure":
            measured = _timing_status(result, profile)
            declared = checks["timingDisclosure"]
            if declared != measured:
                invalid_reason = "timing_status_mismatch"
                break
            timing.append(measured)
        elif checks["timingDisclosure"] != "NOT_APPLICABLE" or result["timing"] is not None:
            invalid_reason = "timing_channel_not_applicable_mismatch"
            break

    if invalid_reason:
        result = _invalid(invalid_reason, identity)
        result["caseCounts"] = case_counts
        return result

    channel_statuses = {
        "functionalIsolationStatus": _aggregate(functional, len(cases)),
        "contentDisclosureStatus": _aggregate(content, len(cases)),
        "errorDisclosureStatus": _aggregate(error, sum(1 for case in cases if case["category"] == "error_disclosure")),
        "timingDisclosureStatus": _aggregate(timing, sum(1 for case in cases if case["category"] == "timing_disclosure")),
    }
    if any(status == "FAIL" for status in channel_statuses.values()) or case_failures:
        report_status = "FAIL"
        reason = "isolation_invariant_failed"
    elif (
        any(status == "NOT_EVALUABLE" for status in channel_statuses.values())
        or case_not_evaluable
        or infrastructure.get("owned") is not True
        or infrastructure.get("healthy") is not True
        or evidence.get("providerCallCount") != 0
        or evidence.get("businessDataOutbound") is not False
        or evidence.get("realMaintenanceStatus") != "SKIPPED"
    ):
        report_status = "NOT_EVALUABLE"
        reason = "required_evidence_incomplete"
    else:
        report_status = "PASS"
        reason = "all_required_channels_passed"

    return {
        "reportStatus": report_status,
        "exitCode": STATUS_EXIT_CODES[report_status],
        "reason": reason,
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "driverVersion": identity["driverVersion"],
        "fixtureVersion": identity["fixtureVersion"],
        "profileVersion": identity["profileVersion"],
        "gitHead": git_head,
        **channel_statuses,
        "caseCounts": case_counts,
        "providerCallCount": evidence.get("providerCallCount"),
        "businessDataOutbound": evidence.get("businessDataOutbound"),
        "realMaintenanceStatus": evidence.get("realMaintenanceStatus"),
        "timingBoundary": "local-synthetic-coarse-oracle-only",
        "claimBoundary": "milvus-supported-profile-fixed-synthetic-matrix-only",
    }


def render_markdown(result: dict[str, Any]) -> str:
    counts = result["caseCounts"]
    return "\n".join([
        "# C14 Tenant Isolation Adversarial Evaluation",
        "",
        f"- Report status: `{result['reportStatus']}`",
        f"- Reason: `{result['reason']}`",
        f"- Release: `{result.get('releaseVersion')}`",
        f"- Git HEAD: `{result.get('gitHead')}`",
        f"- Functional isolation: `{result['functionalIsolationStatus']}`",
        f"- Content disclosure: `{result['contentDisclosureStatus']}`",
        f"- Error disclosure: `{result['errorDisclosureStatus']}`",
        f"- Timing disclosure: `{result['timingDisclosureStatus']}`",
        f"- Cases: expected={counts['expected']}, observed={counts['observed']}, "
        f"missing={counts['missing']}, unexpected={counts['unexpected']}, "
        f"failed={counts['failed']}, errors={counts['errors']}, skipped={counts['skipped']}",
        f"- Provider calls: `{result.get('providerCallCount')}`",
        f"- Business data outbound: `{result.get('businessDataOutbound')}`",
        f"- Real maintenance: `{result.get('realMaintenanceStatus')}`",
        "",
        "结论仅适用于 Milvus 受支持配置与固定 synthetic attack matrix；不代表生产级多租户、全 adapter、真实迁移或所有 timing side-channel 已验证。",
        "",
    ])


def _write_new(path: Path, content: str) -> None:
    if path.exists():
        raise FileExistsError(f"refusing to overwrite existing evidence: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate C14 tenant isolation evidence.")
    parser.add_argument("--manifest", default="docs/eval/isolation/tenant-isolation-adversarial-v1-manifest.json")
    parser.add_argument("--evidence", required=True)
    parser.add_argument("--details-json", required=True)
    parser.add_argument("--report", required=True)
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    repo_root = Path(__file__).resolve().parents[1]
    evidence = json.loads(Path(args.evidence).read_text(encoding="utf-8"))
    result = evaluate_evidence(repo_root, Path(args.manifest), evidence)
    details_path = Path(args.details_json)
    report_path = Path(args.report)
    if not args.no_overwrite:
        print("formal C14 evidence requires --no-overwrite", file=sys.stderr)
        return STATUS_EXIT_CODES["INVALID"]
    try:
        _write_new(details_path, json.dumps(result, ensure_ascii=False, indent=2) + "\n")
        _write_new(report_path, render_markdown(result))
    except FileExistsError as exc:
        print(str(exc), file=sys.stderr)
        return STATUS_EXIT_CODES["INVALID"]
    print(f"Report status: {result['reportStatus']}")
    return int(result["exitCode"])


if __name__ == "__main__":
    raise SystemExit(main())
