#!/usr/bin/env python3
"""Local C20 DRAFT observations, not threshold approval or a release decision."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
from typing import Any

import eval_dataset_contract as dataset
import evaluate_quality_gate as gate

DEFAULT_PROFILE = Path("docs/eval/config/c20-objective-judge-draft-v1.json")
OBJECTIVE = set(gate.OBJECTIVE_RATIOS) | set(gate.OBJECTIVE_COUNTS) | {
    "objective_claim_support_rate", "no_answer_accuracy"}
JUDGE = {"judge_pass_rate", "faithfulness_avg", "relevance_avg"}


def _require(value: bool, code: str) -> None:
    if not value:
        raise gate.GateContractError(code)


def _path(root: Path, path: Path) -> Path:
    result = (root / path).resolve()
    _require(result.is_relative_to(root.resolve()), "path_outside_repository")
    return result


def _draft(profile: dict[str, Any]) -> None:
    gate._validate_profile(profile)
    _require(profile["profileId"] == "c20-objective-judge-draft"
             and profile["profileVersion"] == "v1-draft"
             and profile["status"] == "DRAFT"
             and profile["requiredChannels"] == ["objective"], "c20_draft_required")
    _require(profile["slices"] == [{"id": "overall", "axis": "all", "value": "all",
                                   "minimumDenominator": 1}], "c20_slice_invalid")
    _require(profile["dataset"]["releaseVersion"] == "rag-eval-dev-v2"
             and profile["dataset"]["expectedSampleCount"] == 150
             and profile["errorPolicy"] == {"retrieveErrorsMax": 0, "rateLimitErrorsMax": 0,
                                           "retryCountMax": 0}, "c20_policy_invalid")
    _require(len(profile["rules"]) == len(OBJECTIVE | JUDGE)
             and {r["metric"] for r in profile["rules"]} == OBJECTIVE | JUDGE,
             "c20_rules_invalid")
    for rule in profile["rules"]:
        metric = rule["metric"]
        _require(rule["id"] == metric.replace("_", "-")
                 and rule["slice"] == "overall"
                 and rule["operator"] == ("maxInclusive" if metric.endswith("_count") else "minInclusive")
                 and rule["required"] == (metric in OBJECTIVE)
                 and rule["target"] is None and rule.get("maxAbsoluteRegression") is None,
                 "c20_unapproved_rule")


def review(root: Path, profile_path: Path, details_path: Path) -> dict[str, Any]:
    """Return only allowlisted fields, even for malformed or private input."""
    result: dict[str, Any] = {"schemaVersion": "c20-profile-review-v1",
        "gateStatus": "INVALID", "exitCode": 2, "reason": "input_invalid",
        "providerCalls": 0, "businessDataOutbound": False, "activationPerformed": False,
        "observations": []}
    try:
        pp, dp = _path(root, profile_path), _path(root, details_path)
        profile = gate._read_json(pp, "profile_invalid")
        _draft(profile)
        decision = gate.evaluate_gate(root, pp, dp)
        result.update({"profileSha256": hashlib.sha256(pp.read_bytes()).hexdigest(),
            "detailsSha256": hashlib.sha256(dp.read_bytes()).hexdigest(),
            "gateStatus": decision["gateStatus"], "exitCode": decision["exitCode"],
            "reason": decision.get("reason", "profile_draft")})
        if decision.get("reason") != "profile_draft":
            return result
        details = gate._read_json(dp, "details_invalid")
        identity = dataset.validate_versioned_release(root, Path(profile["dataset"]["manifestPath"]))
        annotations = gate._read_jsonl(root / identity["questionSet"]["path"])
        samples = details["samples"]
        _require(all(isinstance(s, dict) for s in samples), "evidence_sample_invalid")
        counts = details.get("runCounts")
        keys = ("retrieveErrors", "askErrors", "judgeErrors", "rateLimitErrors", "retryCount")
        _require(isinstance(counts, dict) and all(type(counts.get(k)) is int and counts[k] >= 0
                                                for k in keys), "run_counts_invalid")
        channels = details.get("metricChannels")
        _require(isinstance(channels, dict), "channel_identity_invalid")
        objective = channels.get("objective") or {}
        judge = channels.get("judge") or {}
        _require(isinstance(objective, dict) and isinstance(judge, dict), "channel_identity_invalid")
        objective_ok = (objective.get("status") == details.get("objectiveMetricStatus") == "COMPLETE"
                        and objective.get("comparisonSafety") == "ELIGIBLE"
                        and counts["askErrors"] == counts["retrieveErrors"] == 0)
        judge_ok = (judge.get("status") == details.get("judgeMetricStatus") == "COMPLETE"
                    and judge.get("comparisonSafety") == "ELIGIBLE" and counts["judgeErrors"] == 0)
        result.update({"objectiveMeasurementComplete": objective_ok,
            "judgeAdvisoryStatus": "AVAILABLE" if judge_ok else "UNAVAILABLE",
            "policyBlocker": gate._evidence_blocker(profile, details, counts),
            "runCounts": {k: counts[k] for k in keys}, "profileStatus": "DRAFT",
            "thresholdStatus": "PENDING_REFERENCE_EVIDENCE", "sampleCount": len(samples)})
        annotated = list(zip(annotations, samples, strict=True))
        judge_ok = judge_ok and all(not gate._metric_value_missing(metric, annotated) for metric in JUDGE)
        result["judgeAdvisoryStatus"] = "AVAILABLE" if judge_ok else "UNAVAILABLE"
        for rule in profile["rules"]:
            eligible = objective_ok if rule["channel"] == "objective" else judge_ok
            if eligible:
                observation = gate.calculate_rule_observation(rule, profile["slices"][0], annotated)
                if observation["status"] != "COMPLETE":
                    observation["observed"] = None
                if rule["channel"] == "judge" and observation["status"] != "COMPLETE":
                    result["judgeAdvisoryStatus"] = "UNAVAILABLE"
            else:
                observation = {"id": rule["id"], "channel": rule["channel"],
                    "metric": rule["metric"], "required": rule["required"],
                    "observed": None, "denominator": None, "status": "INCOMPLETE",
                    "reason": "objective_channel_incomplete" if rule["channel"] == "objective"
                              else "judge_channel_incomplete"}
            result["observations"].append({**observation, "target": None,
                "result": "NOT_EVALUABLE" if rule["required"] else "SKIPPED"})
        result["objectiveMeasurementComplete"] = objective_ok and all(
            x["status"] == "COMPLETE" for x in result["observations"] if x["channel"] == "objective")
        return result
    except (gate.GateContractError, dataset.DatasetContractError) as exc:
        code = getattr(exc, "code", "input_invalid")
        result.update({"gateStatus": "INVALID", "exitCode": 2,
            "reason": code if re.fullmatch(r"[a-z_]{1,80}", code) else "input_invalid",
            "objectiveMeasurementComplete": False, "judgeAdvisoryStatus": "UNAVAILABLE",
            "observations": []})
    except (OSError, ValueError, TypeError, KeyError, OverflowError, AttributeError):
        result.update({"gateStatus": "INVALID", "exitCode": 2,
                       "objectiveMeasurementComplete": False, "judgeAdvisoryStatus": "UNAVAILABLE",
                       "reason": "input_invalid", "observations": []})
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, default=DEFAULT_PROFILE)
    parser.add_argument("--details", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    result = review(root, args.profile, args.details)
    if args.output:
        try:
            out = _path(root, args.output)
            _require(out not in {_path(root, args.profile), _path(root, args.details)},
                     "output_aliases_input")
            # Exclusive creation; no implicit directories, no source mutation.
            with out.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + "\n")
        except (OSError, gate.GateContractError):
            result = {"gateStatus": "INVALID", "exitCode": 2, "reason": "output_refused",
                      "providerCalls": 0, "businessDataOutbound": False, "activationPerformed": False}
    print(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False))
    return result["exitCode"]


if __name__ == "__main__":
    raise SystemExit(main())
