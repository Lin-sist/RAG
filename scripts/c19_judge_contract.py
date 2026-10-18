#!/usr/bin/env python3
"""Local C19 identity validation and bounded, append-only calibration execution."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import subprocess
import time
from typing import Any, Callable
from urllib.parse import urlsplit
import uuid

import rag_judge_contract as judge
import run_judge_calibration as calibration

DEFAULT_MANIFEST = Path("docs/eval/config/c19-judge-calibration-v1.json")
SCHEMA_PATH = Path("docs/eval/schema/c19-judge-calibration-manifest-v1.json")
SOURCE_PATHS = (
    "scripts/c19_judge_contract.py", "scripts/compile_judge_calibration_evidence.py",
    "scripts/run_judge_calibration.py", "scripts/rag_judge_contract.py",
)
VERSION = "c19-judge-calibration-v1"
ERRORS = frozenset({"invalid_judge_payload", "timeout", "rate_limited", "judge_http_error",
                    "judge_network_error", "invalid_provider_response", "judge_call_error"})


class ContractError(ValueError):
    """Contains a stable error code only; never echo untrusted inputs."""


def require(condition: bool, code: str) -> None:
    if not condition:
        raise ContractError(code)


def canonical_hash(value: Any) -> str:
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True,
                                    separators=(",", ":"), allow_nan=False).encode("utf-8")).hexdigest()


def file_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        require(key not in result, "duplicate_json_key")
        result[key] = value
    return result


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=_pairs,
                           parse_constant=lambda _: (_ for _ in ()).throw(ContractError("non_finite_json")))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise ContractError("invalid_json_artifact") from exc
    require(isinstance(value, dict), "invalid_json_object")
    return value


def fields(value: Any, expected: set[str], code: str) -> None:
    require(isinstance(value, dict) and set(value) == expected, code)


def repo_path(root: Path, value: str) -> Path:
    require(isinstance(value, str) and "\\" not in value and ":" not in value,
            "invalid_repo_path")
    p = Path(value)
    require(not p.is_absolute() and ".." not in p.parts, "invalid_repo_path")
    result = (root / p).resolve()
    require(result.is_relative_to(root.resolve()), "invalid_repo_path")
    return result


def endpoint_identity(value: Any) -> str:
    require(isinstance(value, str) and re.fullmatch(r"https://[A-Za-z0-9.:-]+(?:/[A-Za-z0-9_./-]+)?", value) is not None,
            "invalid_endpoint")
    try:
        url = urlsplit(value)
        port = url.port
    except ValueError as exc:
        raise ContractError("invalid_endpoint") from exc
    require(bool(url.hostname) and url.username is None and url.password is None
            and not url.query and not url.fragment and url.scheme == "https"
            and ".." not in url.path.split("/") and "//" not in url.path,
            "invalid_endpoint")
    return f"https://{url.hostname.lower()}" + (f":{port}" if port and port != 443 else "") + url.path.rstrip("/")


def judge_args(manifest: dict[str, Any], api_key: str = "") -> argparse.Namespace:
    config = manifest["judge"]
    return argparse.Namespace(judge_base_url=config["baseUrl"], judge_model=config["model"],
        judge_temperature=config["temperature"], judge_timeout=config["timeoutSeconds"],
        judge_max_context_chars=config["maxContextChars"],
        judge_max_output_tokens=config["maxOutputTokens"], judge_reject_redirects=True,
        judge_http_status=None, judge_api_key=api_key)


def validate_manifest(root: Path, manifest: dict[str, Any]) -> dict[str, Any]:
    fields(manifest, {"schemaVersion", "manifestId", "schemaSha256", "calibrationManifest",
                      "judge", "judgeContractConfig", "sourceSha256"}, "manifest_fields_invalid")
    require(manifest["schemaVersion"] == VERSION and manifest["manifestId"] == VERSION,
            "manifest_version_invalid")
    require(file_hash(root / SCHEMA_PATH) == manifest["schemaSha256"], "schema_hash_mismatch")
    config = manifest["judge"]
    fields(config, {"provider", "model", "baseUrl", "temperature", "timeoutSeconds",
                    "maxOutputTokens", "maxContextChars", "minIntervalSeconds", "retries"},
           "judge_fields_invalid")
    require(config["provider"] == "nvidia-nim", "provider_invalid")
    require(isinstance(config["model"], str) and re.fullmatch(r"[A-Za-z0-9_./-]{1,160}", config["model"]) is not None,
            "model_invalid")
    require(config["baseUrl"] == endpoint_identity(config["baseUrl"]), "endpoint_not_canonical")
    for name, low, high, integer in (("temperature", 0, 2, False), ("timeoutSeconds", 1, 120, False),
            ("maxOutputTokens", 1, 4096, True), ("maxContextChars", 1, 6000, True),
            ("minIntervalSeconds", 2, 60, False), ("retries", 0, 0, True)):
        number = config[name]
        require(type(number) in ((int,) if integer else (int, float)) and math.isfinite(number)
                and low <= number <= high, "judge_numeric_setting_invalid")
    require(manifest["judgeContractConfig"] == judge.contract_config(judge_args(manifest)),
            "shared_contract_drift")
    descriptor = manifest["calibrationManifest"]
    fields(descriptor, {"path", "sha256"}, "calibration_descriptor_invalid")
    require(descriptor["path"] == str(calibration.DEFAULT_MANIFEST).replace("\\", "/"),
            "calibration_path_invalid")
    require(file_hash(repo_path(root, descriptor["path"])) == descriptor["sha256"], "calibration_hash_mismatch")
    calibration.validate_calibration_release(root, calibration.DEFAULT_MANIFEST)
    fields(manifest["sourceSha256"], set(SOURCE_PATHS), "source_fields_invalid")
    for name, expected in manifest["sourceSha256"].items():
        raw = repo_path(root, name).read_bytes().replace(b"\r\n", b"\n")
        require(hashlib.sha256(raw).hexdigest() == expected, "source_hash_mismatch")
    return manifest


def load_manifest(root: Path, path: Path = DEFAULT_MANIFEST) -> dict[str, Any]:
    return validate_manifest(root, read_json(repo_path(root, path.as_posix())))


def git_identity(root: Path, manifest: dict[str, Any]) -> dict[str, Any]:
    def git(*args: str) -> bytes:
        result = subprocess.run(["git", *args], cwd=root, capture_output=True, check=False)
        require(result.returncode == 0, "git_unavailable")
        return result.stdout
    head = git("rev-parse", "HEAD").decode("ascii").strip()
    require(re.fullmatch(r"[0-9a-f]{40}", head) is not None, "git_head_invalid")
    clean = not bool(git("status", "--porcelain", "--untracked-files=all").strip())
    blobs_match = all(hashlib.sha256(git("show", f"HEAD:{name}")).hexdigest() == expected
                      for name, expected in manifest["sourceSha256"].items()) if clean else False
    return {"head": head, "clean": clean, "sourceBlobsMatch": blobs_match}


def build_plan(root: Path, manifest: dict[str, Any], profile: str) -> dict[str, Any]:
    require(profile in {"canary", "full"}, "profile_invalid")
    plan = calibration.calibration_plan(root, calibration.DEFAULT_MANIFEST,
                                       profile=profile, judge_args=judge_args(manifest))
    return {"schemaVersion": VERSION, "manifestSha256": canonical_hash(manifest),
            "profile": profile, "calibrationPlan": plan, "executionConfig": manifest["judge"],
            "sourceSha256": manifest["sourceSha256"], "actualProviderCalls": 0,
            "businessDataOutbound": False}


def preflight(root: Path, manifest: dict[str, Any], profile: str) -> dict[str, Any]:
    validate_manifest(root, manifest)
    plan = build_plan(root, manifest, profile)
    identity = git_identity(root, manifest)
    reasons = []
    if manifest["judge"]["model"] == "UNCONFIGURED":
        reasons.append("model_not_selected")
    if not identity["clean"] or not identity["sourceBlobsMatch"]:
        reasons.append("clean_frozen_source_required")
    return {**plan, "status": "BLOCKED" if reasons else "READY", "reasonCodes": reasons,
            "git": identity, "providerAuthentication": "NOT_CHECKED"}


class AttemptBudget:
    def __init__(self, limit: int):
        require(type(limit) is int and limit in {4, 72}, "budget_invalid")
        self.limit = limit
        self.used = 0

    def reserve(self) -> int:
        require(self.used < self.limit, "attempt_budget_exhausted")
        self.used += 1
        return self.used


def run_path(root: Path, path: Path) -> Path:
    result = path.resolve() if path.is_absolute() else (root / path).resolve()
    boundary = root.resolve() / "tmp" / "eval"
    require(result != boundary and result.is_relative_to(boundary), "run_path_not_local_ignored")
    return result


def write_new(path: Path, value: dict[str, Any]) -> None:
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2, allow_nan=False)
        stream.write("\n")
        stream.flush()
        os.fsync(stream.fileno())


def execute_run(root: Path, manifest: dict[str, Any], profile: str, directory: Path, *,
                api_key: str = "", transport: Callable[..., str] | None = None,
                sleep: Callable[[float], None] = time.sleep) -> Path:
    """Injected transports are always labelled SYNTHETIC, never real provider evidence."""
    validate_manifest(root, manifest)
    ready = preflight(root, manifest, profile)
    if transport is None:
        require(ready["status"] == "READY", "live_preflight_blocked")
        require(bool(api_key), "credential_missing")
    directory = run_path(root, directory)
    directory.parent.mkdir(parents=True, exist_ok=True)
    try:
        directory.mkdir(exist_ok=False)
    except FileExistsError as exc:
        raise ContractError("run_directory_exists") from exc
    plan = ready["calibrationPlan"]
    keys = [[case_id, repeat] for case_id in plan["selectedCaseIds"] for repeat in plan["repeatIndexes"]]
    identity = {"schemaVersion": VERSION, "runId": str(uuid.uuid4()), "profile": profile,
        "evidenceKind": "SYNTHETIC" if transport is not None else "LIVE",
        "manifestSha256": canonical_hash(manifest), "calibrationReleaseIdentity": plan["calibrationReleaseIdentity"],
        "judgeContractConfig": manifest["judgeContractConfig"], "executionConfig": manifest["judge"],
        "sourceSha256": manifest["sourceSha256"], "git": ready["git"], "expectedKeys": keys,
        "callBudget": plan["estimatedJudgeCalls"], "automaticRetries": 0}
    write_new(directory / "identity.json", identity)
    cases = calibration._selected_cases(root, plan["calibrationReleaseIdentity"], plan["selectedCaseIds"])
    by_id = {case["id"]: case for case in cases}
    args = judge_args(manifest, api_key)
    budget = AttemptBudget(plan["estimatedJudgeCalls"])
    failed = False
    completed = 0
    with (directory / "ledger.jsonl").open("x", encoding="utf-8", newline="\n") as ledger:
        def append(event: dict[str, Any]) -> None:
            ledger.write(json.dumps(event, ensure_ascii=False, allow_nan=False) + "\n")
            ledger.flush()
            os.fsync(ledger.fileno())
        for case_id, repeat in keys:
            if budget.used:
                sleep(manifest["judge"]["minIntervalSeconds"])
            index = budget.reserve()
            base = {"caseId": case_id, "repeatIndex": repeat, "attemptIndex": index}
            append({**base, "event": "start"})
            started = time.monotonic()
            args.judge_http_status = None
            event: dict[str, Any] = {**base, "event": "outcome"}
            case = by_id[case_id]
            # Only explicit question/answer/context fields cross the transport boundary.
            payload_case = {"question": case["question"], "answer": case["answer"]}
            contexts = calibration._case_contexts(root, case)
            try:
                content = (transport or calibration.call_live_judge)(payload_case, contexts, args)
                event["rawContent"] = content
                judge.parse_judge_content(content)
                event["errorCategory"] = None
            except Exception as exc:
                category = calibration._error_category(exc)
                event["errorCategory"] = category if category in ERRORS else "judge_call_error"
                failed = True
            event["elapsedMs"] = max(0, int((time.monotonic() - started) * 1000))
            event["httpStatus"] = args.judge_http_status
            append(event)
            completed += 1
            if failed:
                break
    write_new(directory / "completion.json", {"schemaVersion": VERSION,
        "identitySha256": file_hash(directory / "identity.json"),
        "ledgerSha256": file_hash(directory / "ledger.jsonl"),
        "attemptsReserved": budget.used, "outcomesRecorded": completed,
        "stoppedOnError": failed, "automaticRetries": 0})
    return directory


def main_for_runner(root: Path, args: argparse.Namespace) -> int:
    try:
        manifest = load_manifest(root, Path(args.c19_manifest))
        # The manifest owns execution settings; conflicting CLI/env values must not be ignored.
        expected = judge_args(manifest)
        for name in ("judge_base_url", "judge_temperature", "judge_timeout", "judge_max_context_chars"):
            require(getattr(args, name) == getattr(expected, name), "runtime_override_refused")
        require(not args.judge_model or args.judge_model == expected.judge_model, "runtime_override_refused")
        require(Path(args.manifest) == calibration.DEFAULT_MANIFEST, "calibration_override_refused")
        if args.plan_only:
            print(json.dumps(build_plan(root, manifest, args.profile), ensure_ascii=False, indent=2))
            return 0
        if args.preflight_only:
            result = preflight(root, manifest, args.profile)
            print(json.dumps(result, ensure_ascii=False, indent=2))
            return 0 if result["status"] == "READY" else 4
        require(args.execute_live_judge, "live_execution_not_requested")
        require(bool(args.run_directory), "run_directory_required")
        directory = execute_run(root, manifest, args.profile, Path(args.run_directory),
            api_key=args.judge_api_key or os.getenv("RAG_EVAL_JUDGE_API_KEY", ""))
        import compile_judge_calibration_evidence as compiler
        result = compiler.compile_run(root, manifest, directory)
        write_new(directory / "safe-summary.json", result)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return compiler.EXIT_CODES[result["status"]]
    except (ContractError, calibration.CalibrationContractError, OSError, ValueError) as exc:
        code = str(exc) if isinstance(exc, ContractError) else "c19_execution_refused"
        safe_code = code if re.fullmatch(r"[a-z_]{1,80}", code) else "c19_execution_refused"
        print(json.dumps({"status": "INVALID", "reasonCodes": [safe_code]}))
        return 2
