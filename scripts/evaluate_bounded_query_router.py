#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

import router_eval_contract as contract


EVIDENCE_SCHEMA_VERSION = "bounded-query-router-evidence-v1"
CASE_OUTCOMES = {"PASS", "FAIL", "ERROR", "SKIPPED"}
CHANNEL_VALUES = {"PASS", "FAIL", "NOT_EVALUABLE", "NOT_APPLICABLE"}
ROUTE_REASONS = {
    "DEFINITION_CUE", "FACT_LOOKUP_CUE", "MULTI_HOP_CUE", "GLOBAL_CUE",
    "HIGH_RISK_CUE", "AMBIGUOUS", "INVALID_INPUT",
}
FINAL_STATES = {"ANSWER", "NO_ANSWER", "ERROR", "UNSUPPORTED", "INVALID"}
NO_ANSWER_REASONS = {
    "NONE", "INSUFFICIENT_EVIDENCE", "MODEL_REFUSAL", "UNVALIDATED_EVIDENCE"
}
BUDGET_OUTCOMES = {"WITHIN_BUDGET", "CALL_LIMIT_EXCEEDED", "DEADLINE_EXCEEDED"}
TOP_LEVEL_FIELDS = {
    "schemaVersion", "releaseVersion", "manifestSha256", "evaluatorVersion",
    "classifierVersion", "strategyVersion", "policyVersion", "budgetProfileId",
    "gitHead", "providerCallCount", "businessDataOutbound", "liveEvaluationStatus",
    "classification", "cases",
}
CLASSIFICATION_FIELDS = {"expectedCount", "observedCount", "factExpected", "factObserved"}
CASE_FIELDS = {
    "sampleId", "outcome", "observedIntent", "routeReason", "effectiveStrategy",
    "finalState", "noAnswerReason", "usage", "channels",
}
USAGE_FIELDS = {
    "queryVariants", "retrievalPasses", "rerankCalls", "generationCalls",
    "candidateCount", "contextCount", "estimatedContextTokens",
    "estimatedOutputTokens", "elapsedMillis", "budgetOutcome", "providerCalls",
}


def _invalid(reason: str, identity: dict[str, Any] | None = None) -> dict[str, Any]:
    result: dict[str, Any] = {
        "reportStatus": "INVALID",
        "exitCode": contract.STATUS_EXIT_CODES["INVALID"],
        "reason": reason,
    }
    if identity:
        result.update({
            "releaseVersion": identity.get("releaseVersion"),
            "manifestSha256": identity.get("manifestSha256"),
        })
    return result


def _load_expectations(repo_root: Path, manifest_path: Path) -> list[dict[str, Any]]:
    manifest = json.loads((repo_root / manifest_path).read_text(encoding="utf-8"))
    relative = manifest["artifacts"]["expectations"]["path"]
    return [
        json.loads(line)
        for line in (repo_root / relative).read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]


def _identity_matches(identity: dict[str, Any], evidence: dict[str, Any]) -> bool:
    expected = {
        "schemaVersion": EVIDENCE_SCHEMA_VERSION,
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "evaluatorVersion": identity["evaluatorVersion"],
        "classifierVersion": identity["classifierVersion"],
        "strategyVersion": identity["strategyVersion"],
        "policyVersion": identity["policyVersion"],
        "budgetProfileId": identity["budgetProfileId"],
    }
    return all(evidence.get(field) == value for field, value in expected.items())


def _aggregate(values: list[str], expected_count: int) -> str:
    applicable = [value for value in values if value != "NOT_APPLICABLE"]
    if any(value == "FAIL" for value in applicable):
        return "FAIL"
    if len(applicable) != expected_count or any(value == "NOT_EVALUABLE" for value in applicable):
        return "NOT_EVALUABLE"
    return "PASS"


def _safe_nonnegative_int(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and value >= 0


def _computed_invariant_channels(case: dict[str, Any]) -> dict[str, str]:
    intent = case["observedIntent"]
    outcome = case["outcome"]
    usage = case["usage"]
    classification_status = "PASS"

    if intent == "FACT":
        strategy_ok = case["effectiveStrategy"] == contract.STRATEGY_VERSION
    else:
        strategy_ok = case["effectiveStrategy"] == "NONE"
    if intent == "UNSUPPORTED":
        strategy_ok = strategy_ok and case["finalState"] == "UNSUPPORTED"
    elif intent == "INVALID":
        strategy_ok = strategy_ok and case["finalState"] == "INVALID"

    limits = contract.EXPECTED_BUDGET
    budget_ok = (
        usage["queryVariants"] <= limits["maxQueryVariants"]
        and usage["retrievalPasses"] <= limits["maxRetrievalPasses"]
        and usage["rerankCalls"] <= limits["maxRerankCalls"]
        and usage["generationCalls"] <= limits["maxGenerationCalls"]
        and usage["estimatedContextTokens"] <= limits["maxContextTokens"]
        and usage["estimatedOutputTokens"] <= limits["maxOutputTokens"]
        and usage["elapsedMillis"] <= limits["deadlineMillis"]
        and usage["budgetOutcome"] == "WITHIN_BUDGET"
        and usage["providerCalls"] == 0
        and usage["contextCount"] <= usage["candidateCount"]
    )
    if intent != "FACT":
        strategy_ok = strategy_ok and all(
            usage[field] == 0
            for field in ("retrievalPasses", "rerankCalls", "generationCalls", "providerCalls")
        )
        budget_ok = budget_ok and all(
            usage[field] == 0
            for field in (
                "queryVariants", "retrievalPasses", "rerankCalls", "generationCalls",
                "candidateCount", "contextCount", "estimatedContextTokens",
                "estimatedOutputTokens", "providerCalls",
            )
        )
    else:
        strategy_ok = strategy_ok and usage["queryVariants"] >= 1

    if case["finalState"] == "NO_ANSWER":
        no_answer_consistent = case["noAnswerReason"] != "NONE"
    else:
        no_answer_consistent = case["noAnswerReason"] == "NONE"
    strategy_ok = strategy_ok and no_answer_consistent

    if outcome in {"ERROR", "SKIPPED"}:
        error_status = "NOT_EVALUABLE"
    elif outcome == "FAIL":
        error_status = "FAIL"
    else:
        error_status = "PASS"
    return {
        "classification": classification_status,
        "strategy_execution": "PASS" if strategy_ok else "FAIL",
        "budget": "PASS" if budget_ok else "FAIL",
        "errors": error_status,
    }


def evaluate_evidence(
    repo_root: Path,
    manifest_path: Path,
    evidence: Any,
) -> dict[str, Any]:
    try:
        root = repo_root.resolve()
        identity = contract.validate_release(root, manifest_path)
        expectations = _load_expectations(root, manifest_path)
    except (contract.RouterEvalContractError, OSError, KeyError, json.JSONDecodeError) as exc:
        reason = exc.code if isinstance(exc, contract.RouterEvalContractError) else "release_load_failed"
        return _invalid(reason)
    if not isinstance(evidence, dict) or set(evidence) != TOP_LEVEL_FIELDS:
        return _invalid("evidence_fields_invalid", identity)
    if not _identity_matches(identity, evidence):
        return _invalid("evidence_identity_mismatch", identity)
    if not isinstance(evidence.get("gitHead"), str) or not re.fullmatch(r"[0-9a-f]{40}", evidence["gitHead"]):
        return _invalid("git_head_invalid", identity)
    if (
        not _safe_nonnegative_int(evidence.get("providerCallCount"))
        or not isinstance(evidence.get("businessDataOutbound"), bool)
        or evidence.get("liveEvaluationStatus") != "SKIPPED"
    ):
        return _invalid("execution_boundary_invalid", identity)

    declared = evidence.get("classification")
    if not isinstance(declared, dict) or set(declared) != CLASSIFICATION_FIELDS or not all(
        _safe_nonnegative_int(declared.get(field)) for field in CLASSIFICATION_FIELDS
    ):
        return _invalid("classification_denominator_invalid", identity)
    observed = evidence.get("cases")
    if not isinstance(observed, list) or not all(isinstance(item, dict) for item in observed):
        return _invalid("case_results_invalid", identity)
    ids = [item.get("sampleId") for item in observed]
    if not all(isinstance(sample_id, str) for sample_id in ids):
        return _invalid("case_result_id_invalid", identity)
    if len(set(ids)) != len(ids):
        return _invalid("duplicate_case_result", identity)
    expected_by_id = {item["sampleId"]: item for item in expectations}
    unexpected = sorted(set(ids) - set(expected_by_id))
    if unexpected:
        return _invalid("unexpected_case_result", identity)
    by_id = {item["sampleId"]: item for item in observed}
    missing = [sample_id for sample_id in identity["orderedSampleIds"] if sample_id not in by_id]

    actual_denominators = {
        "expectedCount": len(expectations),
        "observedCount": len(observed),
        "factExpected": sum(1 for item in expectations if item["expectedIntent"] == "FACT"),
        "factObserved": sum(1 for item in observed if item.get("observedIntent") == "FACT"),
    }
    if declared != actual_denominators:
        return _invalid("classification_denominator_mismatch", identity)

    confusion = {
        expected: {actual: 0 for actual in ("FACT", "UNSUPPORTED", "INVALID")}
        for expected in ("FACT", "UNSUPPORTED", "INVALID")
    }
    channel_values = {channel: [] for channel in contract.REQUIRED_CHANNELS}
    details: list[dict[str, Any]] = []
    leakage = 0
    sum_provider_calls = 0

    for expectation in expectations:
        case = by_id.get(expectation["sampleId"])
        if case is None:
            continue
        if set(case) != CASE_FIELDS:
            return _invalid("case_result_fields_invalid", identity)
        if case.get("outcome") not in CASE_OUTCOMES:
            return _invalid("case_outcome_invalid", identity)
        observed_intent = case.get("observedIntent")
        if observed_intent not in contract.ALLOWED_INTENTS:
            return _invalid("unknown_observed_intent", identity)
        if case.get("routeReason") not in ROUTE_REASONS:
            return _invalid("unknown_route_reason", identity)
        if case.get("effectiveStrategy") not in {"NONE", contract.STRATEGY_VERSION}:
            return _invalid("unknown_strategy", identity)
        if case.get("finalState") not in FINAL_STATES:
            return _invalid("unknown_final_state", identity)
        if case.get("noAnswerReason") not in NO_ANSWER_REASONS:
            return _invalid("unknown_no_answer_reason", identity)
        usage = case.get("usage")
        if not isinstance(usage, dict) or set(usage) != USAGE_FIELDS:
            return _invalid("usage_fields_invalid", identity)
        numeric_fields = USAGE_FIELDS - {"budgetOutcome"}
        if not all(_safe_nonnegative_int(usage.get(field)) for field in numeric_fields):
            return _invalid("usage_value_invalid", identity)
        if usage.get("budgetOutcome") not in BUDGET_OUTCOMES:
            return _invalid("unknown_budget_outcome", identity)
        channels = case.get("channels")
        if (
            not isinstance(channels, dict)
            or list(channels) != contract.REQUIRED_CHANNELS
            or any(value not in CHANNEL_VALUES for value in channels.values())
        ):
            return _invalid("case_channels_invalid", identity)

        expected_intent = expectation["expectedIntent"]
        confusion[expected_intent][observed_intent] += 1
        computed = _computed_invariant_channels(case)
        computed["classification"] = "PASS" if expected_intent == observed_intent else "FAIL"
        for channel, value in computed.items():
            if channels[channel] != value:
                return _invalid("channel_status_mismatch", identity)
        fact_expected = expected_intent == "FACT"
        for channel in ("retrieval", "generation_citation", "no_answer"):
            if fact_expected and channels[channel] == "NOT_APPLICABLE":
                return _invalid("channel_applicability_mismatch", identity)
            if not fact_expected and channels[channel] != "NOT_APPLICABLE":
                return _invalid("channel_applicability_mismatch", identity)
        for channel in contract.REQUIRED_CHANNELS:
            channel_values[channel].append(channels[channel])

        if expected_intent in {"UNSUPPORTED", "INVALID"} and any(
            usage[field] > 0
            for field in ("retrievalPasses", "rerankCalls", "generationCalls", "providerCalls")
        ):
            leakage += 1
        sum_provider_calls += usage["providerCalls"]
        details.append({
            "sampleId": case["sampleId"],
            "expectedIntent": expected_intent,
            "observedIntent": observed_intent,
            "effectiveStrategy": case["effectiveStrategy"],
            "finalState": case["finalState"],
            "noAnswerReason": case["noAnswerReason"],
            "outcome": case["outcome"],
            "channels": dict(channels),
            "usage": dict(usage),
        })

    if evidence["providerCallCount"] != sum_provider_calls:
        return _invalid("provider_call_count_mismatch", identity)

    expected_fact = actual_denominators["factExpected"]
    observed_fact = actual_denominators["factObserved"]
    unsupported_denominator = sum(
        1 for item in expectations if item["expectedIntent"] in {"UNSUPPORTED", "INVALID"}
    )
    if expected_fact <= 0 or unsupported_denominator <= 0 or actual_denominators["expectedCount"] <= 0:
        return _invalid("classification_denominator_zero", identity)
    true_fact = confusion["FACT"]["FACT"]
    metrics = {
        "confusion": confusion,
        "factPrecision": round(true_fact / observed_fact, 6) if observed_fact else 0.0,
        "factRecall": round(true_fact / expected_fact, 6),
        "coverage": round(actual_denominators["observedCount"] / actual_denominators["expectedCount"], 6),
        "unsupportedLeakage": round(leakage / unsupported_denominator, 6),
        "unexpectedCount": len(unexpected),
        "invalidObservedCount": observed_fact * 0 + sum(
            row["INVALID"] for row in confusion.values()
        ),
    }
    expected_channel_counts = {
        "classification": len(expectations),
        "strategy_execution": len(expectations),
        "budget": len(expectations),
        "retrieval": expected_fact,
        "generation_citation": expected_fact,
        "no_answer": expected_fact,
        "errors": len(expectations),
    }
    statuses = {
        channel: _aggregate(channel_values[channel], expected_channel_counts[channel])
        for channel in contract.REQUIRED_CHANNELS
    }
    outcome_counts = Counter(str(case["outcome"]) for case in observed)
    derived_failed = sum(
        1 for case in observed
        if case["outcome"] == "FAIL" or any(value == "FAIL" for value in case["channels"].values())
    )
    case_counts = {
        "expected": len(expectations),
        "observed": len(observed),
        "missing": len(missing),
        "unexpected": len(unexpected),
        "failed": derived_failed,
        "errors": outcome_counts["ERROR"],
        "skipped": outcome_counts["SKIPPED"],
    }

    if any(status == "FAIL" for status in statuses.values()) or derived_failed or leakage:
        report_status = "FAIL"
        reason = "required_router_invariant_failed"
    elif (
        missing
        or any(status == "NOT_EVALUABLE" for status in statuses.values())
        or case_counts["errors"]
        or case_counts["skipped"]
        or evidence["providerCallCount"] != 0
        or evidence["businessDataOutbound"] is not False
    ):
        report_status = "NOT_EVALUABLE"
        reason = "required_evidence_incomplete"
    else:
        report_status = "PASS"
        reason = "all_required_channels_passed"

    return {
        "reportStatus": report_status,
        "exitCode": contract.STATUS_EXIT_CODES[report_status],
        "reason": reason,
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "evaluatorVersion": identity["evaluatorVersion"],
        "classifierVersion": identity["classifierVersion"],
        "strategyVersion": identity["strategyVersion"],
        "policyVersion": identity["policyVersion"],
        "budgetProfileId": identity["budgetProfileId"],
        "gitHead": evidence["gitHead"],
        "channelStatuses": statuses,
        "classificationMetrics": metrics,
        "caseCounts": case_counts,
        "providerCallCount": evidence["providerCallCount"],
        "businessDataOutbound": evidence["businessDataOutbound"],
        "liveEvaluationStatus": evidence["liveEvaluationStatus"],
        "details": details,
        "claimBoundary": "default-off-bounded-fact-v1-deterministic-only",
    }


def render_markdown(result: dict[str, Any]) -> str:
    counts = result["caseCounts"]
    metrics = result["classificationMetrics"]
    lines = [
        "# C16 Bounded Query Router Evaluation",
        "",
        f"- Report status: `{result['reportStatus']}`",
        f"- Reason: `{result['reason']}`",
        f"- Release: `{result['releaseVersion']}`",
        f"- Git HEAD: `{result['gitHead']}`",
    ]
    for channel in contract.REQUIRED_CHANNELS:
        lines.append(f"- {channel}: `{result['channelStatuses'][channel]}`")
    lines.extend([
        f"- Cases: expected={counts['expected']}, observed={counts['observed']}, "
        f"missing={counts['missing']}, unexpected={counts['unexpected']}, "
        f"failed={counts['failed']}, errors={counts['errors']}, skipped={counts['skipped']}",
        f"- FACT precision: `{metrics['factPrecision']}`",
        f"- FACT recall: `{metrics['factRecall']}`",
        f"- Coverage: `{metrics['coverage']}`",
        f"- Unsupported leakage: `{metrics['unsupportedLeakage']}`",
        f"- Provider calls: `{result['providerCallCount']}`",
        f"- Business data outbound: `{result['businessDataOutbound']}`",
        f"- Live evaluation: `{result['liveEvaluationStatus']}`",
        "",
        "结论只适用于 default-off、固定 fact-v1 budget 与 deterministic evidence；不代表真实 provider 质量、生产 SLA、multi-hop 或 Agentic RAG。",
        "",
    ])
    return "\n".join(lines)


def write_new(path: Path, content: str) -> None:
    if path.exists():
        raise FileExistsError(f"refusing to overwrite existing evidence: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate C16 bounded router evidence.")
    parser.add_argument(
        "--manifest",
        default="docs/eval/router/bounded-query-router-eval-v1-manifest.json",
    )
    parser.add_argument("--evidence", required=True)
    parser.add_argument("--details-json", required=True)
    parser.add_argument("--report", required=True)
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.no_overwrite:
        print("formal output requires --no-overwrite", file=sys.stderr)
        return contract.STATUS_EXIT_CODES["INVALID"]
    repo_root = Path(__file__).resolve().parents[1]
    try:
        evidence = json.loads(Path(args.evidence).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        print(json.dumps({"reportStatus": "INVALID", "reason": "evidence_load_failed"}))
        return contract.STATUS_EXIT_CODES["INVALID"]
    result = evaluate_evidence(repo_root, Path(args.manifest), evidence)
    try:
        write_new(Path(args.details_json), json.dumps(
            result, ensure_ascii=False, sort_keys=True, indent=2
        ) + "\n")
        write_new(Path(args.report), render_markdown(result) if "caseCounts" in result else (
            f"# C16 Bounded Query Router Evaluation\n\n- Report status: `{result['reportStatus']}`\n"
            f"- Reason: `{result['reason']}`\n"
        ))
    except FileExistsError as exc:
        print(str(exc), file=sys.stderr)
        return contract.STATUS_EXIT_CODES["INVALID"]
    return result["exitCode"]


if __name__ == "__main__":
    raise SystemExit(main())
