#!/usr/bin/env python3
"""Compile local C20 repeat candidates for review, never activate a gate."""
from __future__ import annotations

import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import re
import statistics
from typing import Any

import evaluate_quality_gate as gate
import eval_dataset_contract as dataset
import review_objective_judge_profile as c20


def _hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _canonical(value: Any) -> str:
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True,
        separators=(",", ":"), allow_nan=False).encode("utf-8")).hexdigest()


def _require(condition: bool, code: str) -> None:
    if not condition:
        raise gate.GateContractError(code)


def _metadata_identity(metadata: dict[str, Any], details: dict[str, Any],
                       profile: dict[str, Any], release: dict[str, Any], root: Path) -> str:
    _require(details.get("runMetadata") == metadata, "metadata_binding_mismatch")
    _require(metadata.get("datasetValidation") == "VALID"
             and metadata.get("datasetReleaseIdentity") == details.get("datasetReleaseIdentity") == release,
             "metadata_dataset_mismatch")
    question = metadata.get("evalSetIdentity")
    expected_question = {k: release["questionSet"][k] for k in ("path", "bytes", "sha256")}
    _require(isinstance(question, dict) and set(question) == set(expected_question)
             and isinstance(question.get("path"), str)
             and all(question[k] == expected_question[k] for k in ("bytes", "sha256")),
             "metadata_question_set_mismatch")
    _require(c20._path(root, Path(question["path"])) == c20._path(root, Path(expected_question["path"])),
             "metadata_question_set_mismatch")
    ids = [s["id"] for s in details["samples"]]
    _require(metadata.get("sampleSelection") == {"ids": ids, "count": len(ids)},
             "metadata_selection_mismatch")
    for key in ("topK", "minScore", "enableRerank"):
        _require(metadata.get(key) == profile["runIdentity"][key], "metadata_runtime_mismatch")
    source = metadata.get("git")
    _require(isinstance(source, dict) and source.get("clean") is True
             and isinstance(source.get("head"), str)
             and re.fullmatch(r"[0-9a-f]{40}", source["head"]) is not None,
             "clean_source_provenance_required")
    # Only a hash of potentially private runtime/KB identities is exposed.
    keys = ("evaluationSchema", "datasetReleaseIdentity", "evalSetIdentity", "sampleSelection",
            "topK", "minScore", "enableRerank", "git", "configSnapshot", "knowledgeBase",
            "fixtures", "claimMetricConfig", "judgeContractConfig")
    for key in ("configSnapshot", "knowledgeBase", "claimMetricConfig", "judgeContractConfig"):
        _require(isinstance(metadata.get(key), dict) and bool(metadata[key]), "runtime_identity_missing")
    _require(isinstance(metadata.get("fixtures"), list) and bool(metadata["fixtures"])
             and isinstance(metadata.get("evaluationSchema"), str)
             and bool(metadata["evaluationSchema"])
             and isinstance(metadata.get("evalSetIdentity"), dict)
             and bool(metadata["evalSetIdentity"]), "runtime_identity_missing")
    for key in ("claimMetricConfig", "judgeContractConfig"):
        _require(metadata[key] == details.get(key), "metric_identity_mismatch")
    identity = {key: metadata[key] for key in keys}
    identity["evalSetIdentity"] = expected_question
    # Provider/model fingerprint is part of objective identity when the runner supplies one.
    fingerprint = metadata.get("c18RuntimeFingerprint") or metadata.get("runtimeFingerprint")
    _require(isinstance(fingerprint, dict) and bool(fingerprint), "provider_identity_missing")
    identity["providerRuntimeFingerprint"] = fingerprint
    execution_key = "c18Execution" if metadata.get("c18Execution") else "execution"
    execution = metadata.get(execution_key)
    contract = (metadata.get("c18Manifest") or metadata.get("executionManifest")
                or metadata.get("referenceManifest"))
    # metadata is already bound exactly to details.runMetadata. C18 details.c18Execution
    # is the attempt ledger, whereas metadata.c18Execution is the execution policy.
    _require(isinstance(execution, dict) and bool(execution)
             and isinstance(contract, dict) and bool(contract), "execution_identity_missing")
    identity.update(executionPolicy=execution, executionManifest=contract, warmup=metadata.get("warmup"))
    return _canonical(identity)


def compile_reference(root: Path, profile_path: Path, details_paths: list[Path],
                      metadata_paths: list[Path]) -> dict[str, Any]:
    result: dict[str, Any] = {"schemaVersion": "c20-reference-intake-v1", "status": "INVALID",
        "exitCode": 2, "gateStatus": "NOT_EVALUABLE", "activationEligible": False,
        "activationPerformed": False, "providerCalls": 0, "businessDataOutbound": False,
        "providerExecutionVerified": False, "expectedRepeats": 3, "suppliedRuns": len(details_paths),
        "reasonCodes": [], "runs": [], "rules": []}
    try:
        _require(len(details_paths) == len(metadata_paths) and len(details_paths) <= 3,
                 "input_pair_count_invalid")
        pp = c20._path(root, profile_path)
        profile = gate._read_json(pp, "profile_invalid")
        c20._draft(profile)
        release = dataset.validate_versioned_release(root, Path(profile["dataset"]["manifestPath"]))
        _require(profile["dataset"]["manifestSha256"] == release["manifestSha256"]
                 and profile["dataset"]["releaseVersion"] == release["releaseVersion"]
                 and profile["dataset"]["expectedSampleCount"] == release["questionSet"]["sampleCount"],
                 "profile_dataset_mismatch")
        result["profileSha256"] = _hash(pp)
        result["datasetManifestSha256"] = release["manifestSha256"]
        result["profileStatus"] = "DRAFT"
        required = [r for r in profile["rules"] if r["channel"] == "objective"]
        incomplete: set[str] = set()
        drift: set[str] = set()
        seen_indexes: set[int] = set()
        seen_details: set[str] = set()
        seen_metadata: set[str] = set()
        seen_time: set[datetime] = set()
        identity_hashes: set[str] = set()
        observations: dict[int, dict[str, Any]] = {}
        for dp_arg, mp_arg in zip(details_paths, metadata_paths, strict=True):
            dp, mp = c20._path(root, dp_arg), c20._path(root, mp_arg)
            details_hash, metadata_hash = _hash(dp), _hash(mp)
            metadata = gate._read_json(mp, "metadata_invalid")
            details = gate._read_json(dp, "details_invalid")
            repeat = metadata.get("repeat")
            _require(isinstance(repeat, dict) and type(repeat.get("index")) is int
                     and repeat["index"] in {1, 2, 3} and type(repeat.get("total")) is int,
                     "repeat_identity_invalid")
            index = repeat["index"]
            timestamp = metadata.get("generatedAt")
            _require(isinstance(timestamp, str), "run_time_invalid")
            moment = datetime.fromisoformat(timestamp)
            _require(moment.utcoffset() is not None, "run_time_invalid")
            duplicate = (index in seen_indexes or details_hash in seen_details
                         or metadata_hash in seen_metadata or moment in seen_time)
            if duplicate:
                drift.add("duplicate_run")
            seen_indexes.add(index); seen_details.add(details_hash)
            seen_metadata.add(metadata_hash); seen_time.add(moment)
            if repeat["total"] != 3:
                incomplete.add("repeat_total_incompatible")
            review = c20.review(root, pp, dp)
            eligible = (review.get("reason") == "profile_draft"
                        and review.get("objectiveMeasurementComplete") is True
                        and review.get("policyBlocker") is None)
            if review.get("gateStatus") == "INVALID":
                raise gate.GateContractError("candidate_input_invalid")
            identity_valid = False
            identity_hash = None
            if review.get("reason") != "profile_draft":
                drift.add("candidate_selection_or_run_mismatch")
            else:
                try:
                    identity_hash = _metadata_identity(metadata, details, profile, release, root)
                    identity_hashes.add(identity_hash)
                    identity_valid = True
                except gate.GateContractError as exc:
                    drift.add(exc.code)
            if not eligible:
                incomplete.add("candidate_ineligible")
            row = {"repeatIndex": index, "declaredRepeatTotal": repeat["total"],
                "detailsSha256": details_hash, "metadataSha256": metadata_hash,
                "objectiveMeasurementComplete": review.get("objectiveMeasurementComplete") is True,
                "runtimeIdentitySha256": identity_hash,
                "policyBlocker": review.get("policyBlocker"), "observationEligible": eligible,
                "candidateEligible": eligible and identity_valid and repeat["total"] == 3 and not duplicate,
                "duplicate": duplicate}
            result["runs"].append(row)
            if not duplicate:
                observations[index] = {o["id"]: o for o in review.get("observations", [])
                                       if o["channel"] == "objective"}
        if seen_indexes != {1, 2, 3}:
            incomplete.add("repeat_index_incomplete")
        if len(identity_hashes) > 1:
            drift.add("repeat_runtime_identity_drift")
        complete = not drift and not incomplete and len(details_paths) == 3
        result.update({"status": "NOT_COMPARABLE" if drift else "FOR_REVIEW" if complete else "INCOMPLETE",
            "exitCode": 4, "reasonCodes": sorted(drift | incomplete),
            "compatibleIdentityCount": len(identity_hashes),
            "suppliedRepeatIndexes": sorted(seen_indexes),
            "missingRepeatIndexes": sorted({1, 2, 3} - seen_indexes)})
        for rule in required:
            slots = []
            for index in (1, 2, 3):
                obs = observations.get(index, {}).get(rule["id"], {})
                slots.append({"repeatIndex": index,
                    "observed": obs.get("observed") if obs.get("status") == "COMPLETE" else None,
                    "denominator": obs.get("denominator")})
            distribution = None
            if complete:
                values = [slot["observed"] for slot in slots]
                _require(all(gate._finite_number(v) for v in values), "distribution_value_invalid")
                distribution = {"minimum": min(values), "median": statistics.median(values),
                    "maximum": max(values), "spread": max(values) - min(values)}
            result["rules"].append({"id": rule["id"], "metric": rule["metric"],
                "operator": rule["operator"], "repeats": slots, "distribution": distribution,
                "target": None, "maxAbsoluteRegression": None, "approvalStatus": "PENDING"})
        return result
    except gate.GateContractError as exc:
        code = exc.code if re.fullmatch(r"[a-z_]{1,80}", exc.code) else "input_invalid"
    except (OSError, ValueError, TypeError, KeyError, AttributeError, OverflowError):
        code = "input_invalid"
    # Discard partial observations after malformed input; do not leak exception text or paths.
    result.update({"status": "INVALID", "exitCode": 2, "reasonCodes": [code], "runs": [], "rules": []})
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, default=c20.DEFAULT_PROFILE)
    parser.add_argument("--details", type=Path, action="append", default=[])
    parser.add_argument("--metadata", type=Path, action="append", default=[])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    result = compile_reference(root, args.profile, args.details, args.metadata)
    if args.output:
        try:
            out = c20._path(root, args.output)
            inputs = [args.profile, *args.details, *args.metadata]
            _require(out not in {c20._path(root, p) for p in inputs}, "output_aliases_input")
            with out.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + "\n")
        except (OSError, gate.GateContractError):
            result = {"status": "INVALID", "exitCode": 2, "reasonCodes": ["output_refused"],
                "gateStatus": "NOT_EVALUABLE", "activationEligible": False, "providerCalls": 0,
                "businessDataOutbound": False}
    print(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False))
    return result["exitCode"]


if __name__ == "__main__":
    raise SystemExit(main())
