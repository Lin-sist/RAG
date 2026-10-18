#!/usr/bin/env python3
"""Recompute C19 evidence locally; emit only allowlisted diagnostics."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from typing import Any

import c19_judge_contract as c19
import rag_judge_contract as judge
import run_judge_calibration as calibration

EXIT_CODES = {"COMPLETE": 0, "INVALID": 2, "NOT_COMPARABLE": 3, "INCOMPLETE": 4}


def _base(manifest: dict[str, Any]) -> dict[str, Any]:
    return {"schemaVersion": "c19-judge-evidence-v1", "status": "INVALID", "reasonCodes": [],
            "manifestSha256": c19.canonical_hash(manifest), "actual": {}, "metrics": {},
            "comparisonEligible": False, "liveEvidenceEligible": False,
            "compilerProviderCalls": 0, "compilerDataOutbound": False}


def _validate_identity(root: Path, manifest: dict[str, Any], identity: dict[str, Any]) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    c19.fields(identity, {"schemaVersion", "runId", "profile", "evidenceKind", "manifestSha256",
        "calibrationReleaseIdentity", "judgeContractConfig", "executionConfig", "sourceSha256",
        "git", "expectedKeys", "callBudget", "automaticRetries"}, "identity_fields_invalid")
    c19.require(identity["schemaVersion"] == c19.VERSION, "identity_version_invalid")
    c19.require(isinstance(identity["runId"], str) and re.fullmatch(r"[0-9a-f-]{36}", identity["runId"]) is not None,
                "run_id_invalid")
    c19.require(identity["evidenceKind"] in {"LIVE", "SYNTHETIC"}, "evidence_kind_invalid")
    c19.require(identity["manifestSha256"] == c19.canonical_hash(manifest), "execution_identity_mismatch")
    plan = c19.build_plan(root, manifest, identity["profile"])["calibrationPlan"]
    keys = [[case_id, repeat] for case_id in plan["selectedCaseIds"] for repeat in plan["repeatIndexes"]]
    c19.require(identity["expectedKeys"] == keys and all(type(pair[1]) is int for pair in identity["expectedKeys"]),
                "selection_identity_mismatch")
    c19.require(type(identity["callBudget"]) is int and identity["callBudget"] == len(keys)
                and type(identity["automaticRetries"]) is int and identity["automaticRetries"] == 0,
                "budget_identity_mismatch")
    for name, expected in (("calibrationReleaseIdentity", plan["calibrationReleaseIdentity"]),
                           ("judgeContractConfig", manifest["judgeContractConfig"]),
                           ("executionConfig", manifest["judge"]), ("sourceSha256", manifest["sourceSha256"])):
        c19.require(identity[name] == expected, "execution_identity_mismatch")
    git = identity["git"]
    c19.fields(git, {"head", "clean", "sourceBlobsMatch"}, "git_identity_invalid")
    c19.require(isinstance(git["head"], str) and re.fullmatch(r"[0-9a-f]{40}", git["head"]) is not None
                and type(git["clean"]) is bool and type(git["sourceBlobsMatch"]) is bool, "git_identity_invalid")
    if identity["evidenceKind"] == "LIVE":
        c19.require(git["clean"] and git["sourceBlobsMatch"] and manifest["judge"]["model"] != "UNCONFIGURED",
                    "live_source_not_frozen")
        for name, expected in manifest["sourceSha256"].items():
            blob = subprocess.run(["git", "show", f"{git['head']}:{name}"], cwd=root, capture_output=True)
            c19.require(blob.returncode == 0 and hashlib.sha256(blob.stdout).hexdigest() == expected,
                        "git_blob_mismatch")
    cases = calibration._selected_cases(root, plan["calibrationReleaseIdentity"], plan["selectedCaseIds"])
    return plan, cases


def compile_run(root: Path, manifest: dict[str, Any], directory: Path) -> dict[str, Any]:
    result = _base(manifest)
    try:
        c19.validate_manifest(root, manifest)
        directory = c19.run_path(root, directory)
        identity_file = directory / "identity.json"
        ledger_file = directory / "ledger.jsonl"
        identity = c19.read_json(identity_file)
        plan, cases = _validate_identity(root, manifest, identity)
        keys = [tuple(pair) for pair in identity["expectedKeys"]]
        if not ledger_file.exists():
            result.update(status="INCOMPLETE", reasonCodes=["ledger_missing"])
            return result
        raw = ledger_file.read_bytes()
        truncated = bool(raw) and not raw.endswith(b"\n")
        lines = raw.decode("utf-8").splitlines()
        if truncated:
            lines = lines[:-1]
        events = [json.loads(line, object_pairs_hook=c19._pairs,
            parse_constant=lambda _: (_ for _ in ()).throw(c19.ContractError("non_finite_json"))) for line in lines]
        starts: list[tuple[str, int]] = []
        observations: list[dict[str, Any]] = []
        http_responses = 0
        active: dict[str, Any] | None = None
        error_seen = False
        not_comparable = False
        for event in events:
            c19.require(isinstance(event, dict), "ledger_event_invalid")
            common = {"event", "caseId", "repeatIndex", "attemptIndex"}
            c19.require(type(event.get("repeatIndex")) is int and type(event.get("attemptIndex")) is int
                        and isinstance(event.get("caseId"), str), "ledger_key_invalid")
            key = (event["caseId"], event["repeatIndex"])
            if event.get("event") == "start":
                c19.fields(event, common, "ledger_start_fields_invalid")
                c19.require(active is None and not error_seen, "ledger_order_invalid")
                c19.require(len(starts) < identity["callBudget"], "budget_exceeded")
                c19.require(event["attemptIndex"] == len(starts) + 1, "attempt_index_invalid")
                if key != keys[len(starts)] or key in starts:
                    not_comparable = True
                starts.append(key)
                active = event
            elif event.get("event") == "outcome":
                expected = common | {"elapsedMs", "httpStatus", "errorCategory"}
                if "rawContent" in event:
                    expected.add("rawContent")
                c19.fields(event, expected, "ledger_outcome_fields_invalid")
                c19.require(active is not None and all(event[k] == active[k] for k in ("caseId", "repeatIndex", "attemptIndex")),
                            "ledger_unpaired_outcome")
                c19.require(type(event["elapsedMs"]) is int and event["elapsedMs"] >= 0, "latency_invalid")
                status = event["httpStatus"]
                c19.require(status is None or type(status) is int and 100 <= status <= 599, "http_status_invalid")
                http_responses += int(status is not None)
                error = event["errorCategory"]
                c19.require(error is None or isinstance(error, str) and error in c19.ERRORS, "error_category_invalid")
                observation: dict[str, Any] = {"caseId": key[0], "repeatIndex": key[1],
                    "attemptCount": 1, "errorCategory": error}
                content = event.get("rawContent")
                parsed = None
                if content is not None:
                    c19.require(isinstance(content, str), "raw_content_invalid")
                    try:
                        parsed = judge.parse_judge_content(content)
                    except judge.JudgePayloadError:
                        c19.require(error == "invalid_judge_payload", "payload_error_mismatch")
                if error is None:
                    c19.require(parsed is not None, "valid_payload_missing")
                    c19.require(identity["evidenceKind"] != "LIVE" or status == 200, "success_http_status_invalid")
                    observation.update({"faithfulnessScore": parsed["faithfulnessScore"],
                        "relevanceScore": parsed["relevanceScore"], "judgePass": parsed["pass"],
                        "providerPassMismatch": parsed["providerPassMismatch"]})
                else:
                    c19.require(parsed is None, "unexpected_valid_payload_on_error")
                    error_seen = True
                observations.append(observation)
                active = None
            else:
                raise c19.ContractError("ledger_event_invalid")
        completion_file = directory / "completion.json"
        finalized = completion_file.exists()
        if finalized:
            completion = c19.read_json(completion_file)
            c19.fields(completion, {"schemaVersion", "identitySha256", "ledgerSha256", "attemptsReserved",
                "outcomesRecorded", "stoppedOnError", "automaticRetries"}, "completion_fields_invalid")
            c19.require(completion["schemaVersion"] == c19.VERSION
                and completion["identitySha256"] == c19.file_hash(identity_file)
                and completion["ledgerSha256"] == c19.file_hash(ledger_file), "artifact_hash_mismatch")
            c19.require(type(completion["attemptsReserved"]) is int and completion["attemptsReserved"] == len(starts)
                and type(completion["outcomesRecorded"]) is int and completion["outcomesRecorded"] == len(observations)
                and type(completion["automaticRetries"]) is int and completion["automaticRetries"] == 0
                and type(completion["stoppedOnError"]) is bool and completion["stoppedOnError"] == error_seen,
                "completion_counter_mismatch")
            c19.require(active is None and not truncated, "finalized_ledger_incomplete")
        summary = calibration.aggregate_calibration(cases, observations, repeat_count=plan["repeatCount"])
        # The shared aggregator excludes unexpected keys from gold lookup and quality denominators.
        if not_comparable:
            summary.update(calibrationStatus="NOT_COMPARABLE", comparisonEligible=False,
                           confusionScope="partial_diagnostic_only", repeatConsistentCaseRate="N/A")
            for name in ("faithfulness", "relevance", "jointPass"):
                summary[name]["agreement"] = "N/A"
            result.update(status="NOT_COMPARABLE", reasonCodes=["case_repeat_order_mismatch"],
                          actual={"reservedAttempts": len(starts), "recordedOutcomes": len(observations)}, metrics=summary)
            return result
        complete = summary["calibrationStatus"] == "COMPLETE" and finalized and not truncated and active is None
        if not complete:
            summary["calibrationStatus"] = "PARTIAL"
            summary["comparisonEligible"] = False
            summary["confusionScope"] = "partial_diagnostic_only"
            for name in ("faithfulness", "relevance", "jointPass"):
                summary[name]["agreement"] = "N/A"
            summary["repeatConsistentCaseRate"] = "N/A"
        live = identity["evidenceKind"] == "LIVE"
        result.update({"status": "COMPLETE" if complete else "INCOMPLETE",
            "reasonCodes": [] if complete else ["incomplete_run"], "profile": identity["profile"],
            "runId": identity["runId"], "evidenceKind": identity["evidenceKind"],
            "sourceHead": identity["git"]["head"], "judgeContractConfig": manifest["judgeContractConfig"],
            "executionConfig": manifest["judge"], "comparisonEligible": complete,
            "liveEvidenceEligible": complete and live,
            "actual": {"reservedAttempts": len(starts), "recordedOutcomes": len(observations),
                "outcomeUnknownCount": int(active is not None) + int(truncated and active is None),
                "unattemptedCount": len(keys) - len(starts), "automaticRetries": 0,
                "providerAttemptsUpperBound": len(starts) if live else 0,
                "confirmedHttpResponses": http_responses if live else 0},
            "metrics": summary,
            "artifacts": {"identitySha256": c19.file_hash(identity_file), "ledgerSha256": c19.file_hash(ledger_file)}})
        return result
    except c19.ContractError as exc:
        code = str(exc)
        # All ContractError callsites use fixed codes; reject unknown/unsafe text as well.
        result["reasonCodes"] = [code if re.fullmatch(r"[a-z_]{1,80}", code) else "invalid_evidence"]
    except (OSError, ValueError, TypeError, KeyError, UnicodeError, calibration.CalibrationContractError):
        result["reasonCodes"] = ["invalid_evidence"]
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=c19.DEFAULT_MANIFEST)
    parser.add_argument("--run-directory", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    try:
        manifest = c19.load_manifest(root, args.manifest)
        result = compile_run(root, manifest, args.run_directory)
        if args.output:
            c19.write_new(args.output, result)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return EXIT_CODES[result["status"]]
    except (c19.ContractError, OSError, ValueError, calibration.CalibrationContractError):
        print(json.dumps({"status": "INVALID", "reasonCodes": ["compilation_refused"]}))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
