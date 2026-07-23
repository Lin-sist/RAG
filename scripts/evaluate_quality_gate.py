#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path
from typing import Any

import eval_dataset_contract


PROFILE_SCHEMA_VERSION = "rag-quality-gate-profile-v1"
EVALUATOR_VERSION = "rag-quality-gate-evaluator-v1"


class GateContractError(RuntimeError):
    def __init__(self, code: str) -> None:
        super().__init__(code)
        self.code = code


def evaluate_gate(
    repo_root: Path,
    profile_path: Path,
    details_path: Path,
    reference_path: Path | None = None,
) -> dict[str, Any]:
    try:
        return _evaluate_gate(repo_root, profile_path, details_path, reference_path)
    except (GateContractError, eval_dataset_contract.DatasetContractError) as exc:
        return {
            "gateStatus": "INVALID",
            "exitCode": 2,
            "reason": getattr(exc, "code", "input_contract_invalid"),
            "rules": [],
        }


def _evaluate_gate(
    repo_root: Path,
    profile_path: Path,
    details_path: Path,
    reference_path: Path | None = None,
) -> dict[str, Any]:
    profile_bytes = profile_path.read_bytes()
    profile_sha256 = hashlib.sha256(profile_bytes).hexdigest()
    profile = _read_json(profile_path, "profile_invalid")
    details = _read_json(details_path, "details_invalid")
    _validate_profile(profile)

    dataset = profile["dataset"]
    dataset_identity = eval_dataset_contract.validate_versioned_release(
        repo_root,
        Path(dataset["manifestPath"]),
    )
    _require_equal(dataset.get("manifestSha256"), dataset_identity.get("manifestSha256"), "profile_dataset_mismatch")
    _require_equal(dataset.get("releaseVersion"), dataset_identity.get("releaseVersion"), "profile_dataset_mismatch")
    _require_equal(dataset.get("expectedSampleCount"), dataset_identity["questionSet"].get("sampleCount"), "profile_dataset_mismatch")

    evidence_identity = details.get("datasetReleaseIdentity") or {}
    if details.get("datasetValidation") != "VALID":
        return _not_evaluable_result(profile, profile_sha256, dataset_identity, "evidence_dataset_invalid")
    if (
        evidence_identity.get("manifestSha256") != dataset.get("manifestSha256")
        or evidence_identity.get("releaseVersion") != dataset.get("releaseVersion")
    ):
        return _not_evaluable_result(profile, profile_sha256, dataset_identity, "evidence_dataset_mismatch")

    dataset_samples = _read_jsonl(repo_root / dataset_identity["questionSet"]["path"])
    evidence_samples = details.get("samples")
    if not isinstance(evidence_samples, list):
        raise GateContractError("evidence_samples_missing")
    expected_ids = [str(sample["id"]) for sample in dataset_samples]
    actual_ids = [str(sample.get("id")) for sample in evidence_samples if isinstance(sample, dict)]
    if actual_ids != expected_ids or details.get("sampleCount") != len(expected_ids):
        return _not_evaluable_result(profile, profile_sha256, dataset_identity, "evidence_selection_mismatch")

    try:
        _validate_run_identity(profile["runIdentity"], details)
    except GateContractError as exc:
        return _not_evaluable_result(profile, profile_sha256, dataset_identity, exc.code)
    run_counts = details.get("runCounts") or {}
    reference_rules: dict[str, dict[str, Any]] = {}
    if reference_path is not None:
        reference = _read_json(reference_path, "reference_invalid")
        try:
            _validate_reference(reference, profile, profile_sha256, dataset_identity)
        except GateContractError as exc:
            if exc.code.startswith("reference_"):
                return _not_evaluable_result(
                    profile,
                    profile_sha256,
                    dataset_identity,
                    exc.code,
                )
            raise
        reference_rules = {
            str(rule.get("id")): rule
            for rule in reference.get("rules", [])
            if isinstance(rule, dict)
        }

    if profile["status"] == "DRAFT":
        return _not_evaluable_result(
            profile,
            profile_sha256,
            dataset_identity,
            "profile_draft",
        )

    blocker = _evidence_blocker(profile, details, run_counts)
    if blocker is not None:
        return _not_evaluable_result(profile, profile_sha256, dataset_identity, blocker)

    slices = {item["id"]: item for item in profile["slices"]}
    annotated_evidence = list(zip(dataset_samples, evidence_samples, strict=True))
    rule_results = [
        _evaluate_rule(
            rule,
            slices[rule["slice"]],
            annotated_evidence,
            reference_rules.get(str(rule.get("id"))),
        )
        for rule in profile["rules"]
    ]
    if any(item["result"] == "NOT_EVALUABLE" and item["required"] for item in rule_results):
        gate_status = "NOT_EVALUABLE"
    elif any(item["result"] == "FAIL" and item["required"] for item in rule_results):
        gate_status = "FAIL"
    else:
        gate_status = "PASS"
    return {
        "gateStatus": gate_status,
        "exitCode": {"PASS": 0, "FAIL": 3, "NOT_EVALUABLE": 4}[gate_status],
        "profile": {
            "id": profile["profileId"],
            "version": profile["profileVersion"],
            "sha256": profile_sha256,
        },
        "dataset": {
            "releaseVersion": dataset_identity["releaseVersion"],
            "manifestSha256": dataset_identity["manifestSha256"],
        },
        "runIdentity": dict(profile["runIdentity"]),
        "rules": rule_results,
    }


def _read_json(path: Path, error_code: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise GateContractError(error_code) from exc
    if not isinstance(value, dict):
        raise GateContractError(error_code)
    return value


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    return [
        json.loads(line)
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]


def _validate_profile(profile: dict[str, Any]) -> None:
    allowed_fields = {
        "schemaVersion",
        "profileId",
        "profileVersion",
        "status",
        "thresholdStatus",
        "dataset",
        "runIdentity",
        "requiredChannels",
        "errorPolicy",
        "missingPolicy",
        "slices",
        "rules",
    }
    if set(profile) - allowed_fields:
        raise GateContractError("profile_unknown_field")
    if profile.get("schemaVersion") != PROFILE_SCHEMA_VERSION:
        raise GateContractError("profile_schema_version_invalid")
    for field in (
        "profileId",
        "profileVersion",
        "status",
        "thresholdStatus",
        "dataset",
        "runIdentity",
        "requiredChannels",
        "errorPolicy",
        "missingPolicy",
        "slices",
        "rules",
    ):
        if field not in profile:
            raise GateContractError("profile_missing_field")
    if profile["status"] not in {"ACTIVE", "DRAFT"}:
        raise GateContractError("profile_status_invalid")
    if profile["status"] == "ACTIVE" and profile["thresholdStatus"] != "APPROVED":
        raise GateContractError("profile_threshold_status_invalid")
    if profile["status"] == "DRAFT" and profile["thresholdStatus"] != "PENDING_REFERENCE_EVIDENCE":
        raise GateContractError("profile_threshold_status_invalid")
    channels = profile["requiredChannels"]
    if (
        not isinstance(channels, list)
        or not channels
        or any(not isinstance(channel, str) for channel in channels)
        or len(channels) != len(set(channels))
        or any(channel not in {"retrieval", "objective", "judge"} for channel in channels)
    ):
        raise GateContractError("profile_channel_invalid")
    _validate_profile_dataset(profile["dataset"])
    _validate_profile_run_identity(profile["runIdentity"])
    _validate_profile_error_policy(profile["errorPolicy"])
    if profile["missingPolicy"] != "NOT_EVALUABLE":
        raise GateContractError("profile_missing_policy_invalid")
    slice_ids = _validate_profile_slices(profile["slices"])
    _validate_profile_rules(profile["rules"], slice_ids, channels, profile["status"])


def _validate_profile_dataset(dataset: Any) -> None:
    fields = {"manifestPath", "manifestSha256", "releaseVersion", "expectedSampleCount", "selectionMode"}
    if not isinstance(dataset, dict) or set(dataset) != fields:
        raise GateContractError("profile_dataset_invalid")
    if (
        not isinstance(dataset["manifestPath"], str)
        or not dataset["manifestPath"]
        or not isinstance(dataset["manifestSha256"], str)
        or len(dataset["manifestSha256"]) != 64
        or any(char not in "0123456789abcdef" for char in dataset["manifestSha256"])
        or not isinstance(dataset["releaseVersion"], str)
        or not dataset["releaseVersion"]
        or not isinstance(dataset["expectedSampleCount"], int)
        or isinstance(dataset["expectedSampleCount"], bool)
        or dataset["expectedSampleCount"] < 1
        or dataset["selectionMode"] != "full"
    ):
        raise GateContractError("profile_dataset_invalid")


def _validate_profile_run_identity(identity: Any) -> None:
    fields = {"mode", "topK", "minScore", "enableRerank"}
    if not isinstance(identity, dict) or set(identity) != fields:
        raise GateContractError("profile_run_identity_invalid")
    if (
        identity["mode"] not in {"retrieval-only", "generation/citation"}
        or not isinstance(identity["topK"], int)
        or isinstance(identity["topK"], bool)
        or identity["topK"] < 1
        or not _finite_number(identity["minScore"])
        or not isinstance(identity["enableRerank"], bool)
    ):
        raise GateContractError("profile_run_identity_invalid")


def _validate_profile_error_policy(policy: Any) -> None:
    fields = {"retrieveErrorsMax", "rateLimitErrorsMax", "retryCountMax"}
    if not isinstance(policy, dict) or set(policy) != fields:
        raise GateContractError("profile_error_policy_invalid")
    if any(
        not isinstance(policy[field], int) or isinstance(policy[field], bool) or policy[field] < 0
        for field in fields
    ):
        raise GateContractError("profile_error_policy_invalid")


def _validate_profile_slices(slices: Any) -> set[str]:
    fields = {"id", "axis", "value", "minimumDenominator"}
    if not isinstance(slices, list) or not slices:
        raise GateContractError("profile_slice_invalid")
    ids: set[str] = set()
    for item in slices:
        if not isinstance(item, dict) or set(item) != fields:
            raise GateContractError("profile_slice_invalid")
        slice_id = item["id"]
        if not isinstance(slice_id, str) or not slice_id or slice_id in ids:
            raise GateContractError("profile_slice_invalid")
        ids.add(slice_id)
        axis = item["axis"]
        value = item["value"]
        if axis not in {"all", "type", "difficulty", "answerability"}:
            raise GateContractError("profile_slice_invalid")
        if axis == "all" and value != "all":
            raise GateContractError("profile_slice_invalid")
        if axis in {"type", "difficulty"} and (not isinstance(value, str) or not value):
            raise GateContractError("profile_slice_invalid")
        if axis == "answerability" and not isinstance(value, bool):
            raise GateContractError("profile_slice_invalid")
        denominator = item["minimumDenominator"]
        if not isinstance(denominator, int) or isinstance(denominator, bool) or denominator < 1:
            raise GateContractError("profile_slice_invalid")
    return ids


def _validate_profile_rules(
    rules: Any,
    slice_ids: set[str],
    required_channels: list[str],
    status: str,
) -> None:
    required_fields = {"id", "channel", "slice", "metric", "operator", "target", "required"}
    allowed_fields = required_fields | {"maxAbsoluteRegression"}
    metric_channels = {
        "recall_at_3": "retrieval",
        "recall_at_5": "retrieval",
        "mrr": "retrieval",
        "top1_source_accuracy": "retrieval",
        "retrieve_success_rate": "retrieval",
        "retrieval_latency_p95": "retrieval",
        "objective_claim_support_rate": "objective",
        "judge_pass_rate": "judge",
        "faithfulness_avg": "judge",
        "relevance_avg": "judge",
    }
    if not isinstance(rules, list) or not rules:
        raise GateContractError("profile_rule_invalid")
    ids: set[str] = set()
    for rule in rules:
        if (
            not isinstance(rule, dict)
            or not required_fields.issubset(rule)
            or set(rule) - allowed_fields
        ):
            raise GateContractError("profile_rule_invalid")
        rule_id = rule["id"]
        if not isinstance(rule_id, str) or not rule_id or rule_id in ids:
            raise GateContractError("profile_rule_invalid")
        ids.add(rule_id)
        if rule["slice"] not in slice_ids:
            raise GateContractError("profile_rule_slice_unknown")
        if rule["channel"] not in {"retrieval", "objective", "judge"}:
            raise GateContractError("profile_channel_invalid")
        if rule["required"] is True and rule["channel"] not in required_channels:
            raise GateContractError("profile_channel_invalid")
        if not isinstance(rule["required"], bool):
            raise GateContractError("profile_rule_invalid")
        if rule["metric"] not in metric_channels:
            raise GateContractError("profile_metric_invalid")
        if metric_channels[rule["metric"]] != rule["channel"]:
            raise GateContractError("profile_metric_channel_invalid")
        if rule["operator"] not in {"minInclusive", "maxInclusive"}:
            raise GateContractError("profile_rule_invalid")
        target = rule["target"]
        if status == "ACTIVE" and not _finite_number(target):
            raise GateContractError("profile_threshold_invalid")
        if status == "DRAFT" and target is not None and not _finite_number(target):
            raise GateContractError("profile_threshold_invalid")
        tolerance = rule.get("maxAbsoluteRegression")
        if tolerance is not None and (not _finite_number(tolerance) or float(tolerance) < 0):
            raise GateContractError("profile_tolerance_invalid")


def _validate_run_identity(expected: dict[str, Any], details: dict[str, Any]) -> None:
    actual_mode = "retrieval-only" if details.get("skipAsk") is True else "generation/citation"
    for field, actual in (
        ("mode", actual_mode),
        ("topK", details.get("topK")),
        ("minScore", details.get("minScore")),
        ("enableRerank", details.get("enableRerank")),
    ):
        _require_equal(expected.get(field), actual, "evidence_run_identity_mismatch")


def _validate_reference(
    reference: dict[str, Any],
    profile: dict[str, Any],
    profile_sha256: str,
    dataset_identity: dict[str, Any],
) -> None:
    reference_profile = reference.get("profile") or {}
    reference_dataset = reference.get("dataset") or {}
    _require_equal(reference_profile.get("id"), profile.get("profileId"), "reference_identity_mismatch")
    _require_equal(reference_profile.get("version"), profile.get("profileVersion"), "reference_identity_mismatch")
    _require_equal(reference_profile.get("sha256"), profile_sha256, "reference_identity_mismatch")
    _require_equal(
        reference_dataset.get("releaseVersion"),
        dataset_identity.get("releaseVersion"),
        "reference_identity_mismatch",
    )
    _require_equal(
        reference_dataset.get("manifestSha256"),
        dataset_identity.get("manifestSha256"),
        "reference_identity_mismatch",
    )
    _require_equal(reference.get("runIdentity"), profile.get("runIdentity"), "reference_identity_mismatch")
    reference_rules = reference.get("rules")
    if not isinstance(reference_rules, list):
        raise GateContractError("reference_rules_missing")
    if any(not isinstance(rule, dict) for rule in reference_rules):
        raise GateContractError("reference_rules_missing")
    expected_by_id = {str(rule["id"]): rule for rule in profile["rules"]}
    actual_by_id = {str(rule.get("id")): rule for rule in reference_rules}
    if len(actual_by_id) != len(reference_rules) or set(actual_by_id) != set(expected_by_id):
        raise GateContractError("reference_rule_identity_mismatch")
    for rule_id, expected_rule in expected_by_id.items():
        actual_rule = actual_by_id[rule_id]
        for field in ("channel", "slice", "metric", "operator"):
            _require_equal(
                actual_rule.get(field),
                expected_rule.get(field),
                "reference_rule_identity_mismatch",
            )
        if expected_rule.get("maxAbsoluteRegression") is not None and not _finite_number(
            actual_rule.get("observed")
        ):
            raise GateContractError("reference_rule_missing")


def _evidence_blocker(
    profile: dict[str, Any],
    details: dict[str, Any],
    run_counts: dict[str, Any],
) -> str | None:
    for field, policy_field, reason in (
        ("retrieveErrors", "retrieveErrorsMax", "retrieve_errors_exceeded"),
        ("rateLimitErrors", "rateLimitErrorsMax", "rate_limit_errors_exceeded"),
        ("retryCount", "retryCountMax", "retry_count_exceeded"),
    ):
        if int(run_counts.get(field, 0)) > int(profile["errorPolicy"][policy_field]):
            return reason
    if "retrieval" in profile["requiredChannels"]:
        objective_channel = (details.get("metricChannels") or {}).get("objective") or {}
        if objective_channel.get("comparisonSafety") not in {"RETRIEVAL_ONLY", "ELIGIBLE"}:
            return "retrieval_channel_incomplete"
    if "objective" in profile["requiredChannels"]:
        objective_channel = (details.get("metricChannels") or {}).get("objective") or {}
        if (
            objective_channel.get("status") != "COMPLETE"
            or objective_channel.get("comparisonSafety") != "ELIGIBLE"
        ):
            return "objective_channel_incomplete"
    if "judge" in profile["requiredChannels"]:
        judge_channel = (details.get("metricChannels") or {}).get("judge") or {}
        if (
            judge_channel.get("status") != "COMPLETE"
            or judge_channel.get("comparisonSafety") != "ELIGIBLE"
        ):
            return "judge_channel_incomplete"
    return None


def _blocked_rule(rule: dict[str, Any], reason: str) -> dict[str, Any]:
    return {
        "id": rule.get("id"),
        "channel": rule.get("channel"),
        "slice": rule.get("slice"),
        "metric": rule.get("metric"),
        "operator": rule.get("operator"),
        "target": rule.get("target"),
        "denominator": None,
        "observed": None,
        "required": rule.get("required") is True,
        "result": "NOT_EVALUABLE" if rule.get("required") is True else "SKIPPED",
        "reason": reason,
    }


def _not_evaluable_result(
    profile: dict[str, Any],
    profile_sha256: str,
    dataset_identity: dict[str, Any],
    reason: str,
) -> dict[str, Any]:
    return {
        "gateStatus": "NOT_EVALUABLE",
        "exitCode": 4,
        "reason": reason,
        "profile": {
            "id": profile["profileId"],
            "version": profile["profileVersion"],
            "sha256": profile_sha256,
        },
        "dataset": {
            "releaseVersion": dataset_identity["releaseVersion"],
            "manifestSha256": dataset_identity["manifestSha256"],
        },
        "runIdentity": dict(profile["runIdentity"]),
        "rules": [_blocked_rule(rule, reason) for rule in profile["rules"]],
    }


def _evaluate_rule(
    rule: dict[str, Any],
    slice_definition: dict[str, Any],
    annotated_evidence: list[tuple[dict[str, Any], dict[str, Any]]],
    reference_rule: dict[str, Any] | None = None,
) -> dict[str, Any]:
    selected = _select_slice(slice_definition, annotated_evidence)
    operator = rule.get("operator")
    if operator not in {"minInclusive", "maxInclusive"} or rule.get("channel") not in {
        "retrieval",
        "objective",
        "judge",
    }:
        raise GateContractError("profile_rule_invalid")
    target = rule.get("target")
    if not _finite_number(target):
        raise GateContractError("profile_threshold_invalid")
    required = rule.get("required") is True
    common = {
        "id": rule["id"],
        "channel": rule["channel"],
        "slice": rule["slice"],
        "metric": rule["metric"],
        "operator": rule["operator"],
        "target": float(target),
        "denominator": None,
        "required": required,
    }
    if _metric_value_missing(str(rule.get("metric")), selected):
        return {
            **common,
            "observed": None,
            "result": "NOT_EVALUABLE" if required else "SKIPPED",
            "reason": "required_metric_missing" if required else "optional_metric_missing",
        }
    observed, total = _calculate_metric(str(rule.get("metric")), selected)
    common["denominator"] = total
    if total < int(slice_definition.get("minimumDenominator") or 0):
        return {
            **common,
            "observed": observed,
            "result": "NOT_EVALUABLE" if required else "SKIPPED",
            "reason": "denominator_insufficient",
        }
    hard_passed = observed >= float(target) if operator == "minInclusive" else observed <= float(target)
    reference_passed: bool | None = None
    reference_observed: float | None = None
    max_regression = rule.get("maxAbsoluteRegression")
    if max_regression is not None:
        if not _finite_number(max_regression) or float(max_regression) < 0:
            raise GateContractError("profile_tolerance_invalid")
        if reference_rule is None or not _finite_number(reference_rule.get("observed")):
            return {
                **common,
                "observed": observed,
                "result": "NOT_EVALUABLE" if required else "SKIPPED",
                "reason": "reference_missing",
                "hardThresholdPassed": hard_passed,
                "referenceThresholdPassed": None,
                "referenceObserved": None,
                "maxAbsoluteRegression": float(max_regression),
            }
        reference_observed = float(reference_rule["observed"])
        if operator == "minInclusive":
            reference_passed = observed >= reference_observed - float(max_regression)
        else:
            reference_passed = observed <= reference_observed + float(max_regression)
    passed = hard_passed and reference_passed is not False
    if not hard_passed:
        reason = "hard_threshold_failed"
    elif reference_passed is False:
        reason = "reference_regression_exceeded"
    else:
        reason = "threshold_satisfied"
    return {
        **common,
        "observed": observed,
        "result": "PASS" if passed else "FAIL",
        "reason": reason,
        "hardThresholdPassed": hard_passed,
        "referenceThresholdPassed": reference_passed,
        "referenceObserved": reference_observed,
        "maxAbsoluteRegression": float(max_regression) if max_regression is not None else None,
    }


def _select_slice(
    definition: dict[str, Any],
    annotated_evidence: list[tuple[dict[str, Any], dict[str, Any]]],
) -> list[tuple[dict[str, Any], dict[str, Any]]]:
    axis = definition.get("axis")
    value = definition.get("value")
    if axis == "all" and value == "all":
        return annotated_evidence
    if axis == "type" and isinstance(value, str):
        return [item for item in annotated_evidence if item[0].get("type") == value]
    if axis == "difficulty" and isinstance(value, str):
        return [item for item in annotated_evidence if item[0].get("difficulty") == value]
    if axis == "answerability" and isinstance(value, bool):
        return [item for item in annotated_evidence if bool(item[0].get("should_answer")) is value]
    raise GateContractError("profile_slice_invalid")


def _calculate_metric(
    metric: str,
    selected: list[tuple[dict[str, Any], dict[str, Any]]],
) -> tuple[float, int]:
    if metric in {"recall_at_3", "recall_at_5"}:
        hit_field = "recall3Hits" if metric == "recall_at_3" else "recall5Hits"
        hits = 0
        total = 0
        for _, evidence in selected:
            calculation = evidence.get("metricCalculationDetails") or {}
            hits += int(calculation.get(hit_field) or 0)
            total += int(calculation.get("recallTotal") or 0)
        return (hits / total if total else 0.0), total
    answerable = [item for item in selected if bool(item[0].get("should_answer"))]
    if metric == "mrr":
        reciprocal_rank_total = 0.0
        for _, evidence in answerable:
            rank = (evidence.get("metricCalculationDetails") or {}).get("firstMatchRank")
            if isinstance(rank, int) and rank > 0:
                reciprocal_rank_total += 1.0 / rank
        return (reciprocal_rank_total / len(answerable) if answerable else 0.0), len(answerable)
    if metric == "top1_source_accuracy":
        hits = sum(
            1
            for _, evidence in answerable
            if (evidence.get("metricCalculationDetails") or {}).get("top1SourceHit") is True
        )
        return (hits / len(answerable) if answerable else 0.0), len(answerable)
    if metric == "objective_claim_support_rate":
        supported = 0
        total = 0
        for _, evidence in answerable:
            claim_metrics = evidence.get("objectiveClaimMetrics") or {}
            supported += int(claim_metrics.get("supportedClaimCount") or 0)
            total += int(claim_metrics.get("claimTotal") or 0)
        return (supported / total if total else 0.0), total
    if metric == "judge_pass_rate":
        passed = sum(
            1
            for _, evidence in answerable
            if (evidence.get("metricCalculationDetails") or {}).get("judgePass") is True
        )
        return (passed / len(answerable) if answerable else 0.0), len(answerable)
    if metric in {"faithfulness_avg", "relevance_avg"}:
        field = "faithfulnessScore" if metric == "faithfulness_avg" else "relevanceScore"
        values = [
            float((evidence.get("metricCalculationDetails") or {})[field])
            for _, evidence in answerable
        ]
        return (sum(values) / len(values) if values else 0.0), len(values)
    if metric == "retrieve_success_rate":
        successes = sum(
            1
            for _, evidence in selected
            if isinstance(evidence.get("errors"), dict)
            and evidence["errors"].get("retrieval") is None
        )
        return (successes / len(selected) if selected else 0.0), len(selected)
    if metric == "retrieval_latency_p95":
        values = sorted(float(evidence["retrieveLatencyMillis"]) for _, evidence in selected)
        index = max(0, math.ceil(0.95 * len(values)) - 1)
        return (values[index] if values else 0.0), len(values)
    raise GateContractError("profile_metric_invalid")


def _metric_value_missing(
    metric: str,
    selected: list[tuple[dict[str, Any], dict[str, Any]]],
) -> bool:
    answerable = [item for item in selected if bool(item[0].get("should_answer"))]
    if metric in {"recall_at_3", "recall_at_5"}:
        hit_field = "recall3Hits" if metric == "recall_at_3" else "recall5Hits"
        return any(
            "recallTotal" not in (evidence.get("metricCalculationDetails") or {})
            or (
                int((evidence.get("metricCalculationDetails") or {}).get("recallTotal") or 0) > 0
                and hit_field not in (evidence.get("metricCalculationDetails") or {})
            )
            for _, evidence in selected
        )
    if metric == "mrr":
        return any(
            "firstMatchRank" not in (evidence.get("metricCalculationDetails") or {})
            for _, evidence in answerable
        )
    if metric == "top1_source_accuracy":
        return any(
            "top1SourceHit" not in (evidence.get("metricCalculationDetails") or {})
            for _, evidence in answerable
        )
    if metric == "objective_claim_support_rate":
        return any(
            not isinstance(evidence.get("objectiveClaimMetrics"), dict)
            or "claimTotal" not in evidence["objectiveClaimMetrics"]
            or "supportedClaimCount" not in evidence["objectiveClaimMetrics"]
            for _, evidence in answerable
        )
    if metric == "judge_pass_rate":
        return any(
            (evidence.get("metricCalculationDetails") or {}).get("judgePass") not in {True, False}
            for _, evidence in answerable
        )
    if metric in {"faithfulness_avg", "relevance_avg"}:
        field = "faithfulnessScore" if metric == "faithfulness_avg" else "relevanceScore"
        return any(
            not _finite_number((evidence.get("metricCalculationDetails") or {}).get(field))
            for _, evidence in answerable
        )
    if metric == "retrieve_success_rate":
        return any(
            not isinstance(evidence.get("errors"), dict) or "retrieval" not in evidence["errors"]
            for _, evidence in selected
        )
    if metric == "retrieval_latency_p95":
        return any(
            not _finite_number(evidence.get("retrieveLatencyMillis"))
            or float(evidence["retrieveLatencyMillis"]) < 0
            for _, evidence in selected
        )
    return True


def _finite_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(float(value))


def _require_equal(actual: Any, expected: Any, code: str) -> None:
    if actual != expected:
        raise GateContractError(code)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate a local RAG quality gate artifact.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[1]))
    parser.add_argument("--profile", required=True)
    parser.add_argument("--details", required=True)
    parser.add_argument("--reference")
    parser.add_argument("--output-json")
    parser.add_argument("--output-markdown")
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    output_paths = [Path(value) for value in (args.output_json, args.output_markdown) if value]
    if args.no_overwrite and any(path.exists() for path in output_paths):
        print("Quality gate output refused: output_exists", file=sys.stderr)
        return 2
    try:
        result = evaluate_gate(
            Path(args.repo_root),
            Path(args.profile),
            Path(args.details),
            Path(args.reference) if args.reference else None,
        )
        result = {"evaluatorVersion": EVALUATOR_VERSION, **result}
        if args.output_json:
            _write_text(
                Path(args.output_json),
                json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            )
        if args.output_markdown:
            _write_text(Path(args.output_markdown), _render_markdown(result))
    except (OSError, UnicodeError, ValueError, TypeError):
        print("Quality gate evaluator failed: runtime_error", file=sys.stderr)
        return 1
    print(f"Gate status: {result['gateStatus']}")
    print(f"Exit code: {result['exitCode']}")
    if result.get("reason"):
        print(f"Reason: {result['reason']}")
    print(f"Rule count: {len(result.get('rules') or [])}")
    return int(result["exitCode"])


def _write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content.encode("utf-8"))


def _render_markdown(result: dict[str, Any]) -> str:
    lines = [
        "# RAG Quality Gate Result",
        "",
        f"- Evaluator version: `{result.get('evaluatorVersion', EVALUATOR_VERSION)}`",
        f"- Gate status: `{result.get('gateStatus')}`",
        f"- Exit code: `{result.get('exitCode')}`",
    ]
    if result.get("reason"):
        lines.append(f"- Reason: `{result['reason']}`")
    profile = result.get("profile") or {}
    dataset = result.get("dataset") or {}
    if profile:
        lines.extend(
            [
                f"- Profile: `{profile.get('id')}` / `{profile.get('version')}`",
                f"- Profile SHA-256: `{profile.get('sha256')}`",
            ]
        )
    if dataset:
        lines.extend(
            [
                f"- Dataset release: `{dataset.get('releaseVersion')}`",
                f"- Dataset manifest SHA-256: `{dataset.get('manifestSha256')}`",
            ]
        )
    lines.extend(
        [
            "",
            "| Rule | Channel | Slice | Metric | Denominator | Observed | Target | Result | Reason |",
            "| --- | --- | --- | --- | ---: | ---: | ---: | --- | --- |",
        ]
    )
    for rule in result.get("rules") or []:
        lines.append(
            "| {id} | {channel} | {slice} | {metric} | {denominator} | {observed} | {target} | {result} | {reason} |".format(
                id=rule.get("id"),
                channel=rule.get("channel"),
                slice=rule.get("slice"),
                metric=rule.get("metric"),
                denominator=_display(rule.get("denominator")),
                observed=_display(rule.get("observed")),
                target=_display(rule.get("target")),
                result=rule.get("result"),
                reason=rule.get("reason"),
            )
        )
    lines.append("")
    return "\n".join(lines)


def _display(value: Any) -> str:
    if value is None:
        return "-"
    if isinstance(value, float):
        return f"{value:.6f}"
    return str(value)


if __name__ == "__main__":
    raise SystemExit(main())
