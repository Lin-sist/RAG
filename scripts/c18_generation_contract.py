#!/usr/bin/env python3
"""Offline C18 generation/objective execution contract and request guard.

This module deliberately owns only the C18 boundary.  It validates the frozen
plan before credentials or provider calls are touched, and it exposes a small
request guard for the two application HTTP calls that can consume the C18
budget.  It does not login, create a KB, call a provider, or score a result.
"""

from __future__ import annotations

import hashlib
import json
import math
import re
from pathlib import Path, PurePosixPath
from typing import Any
from urllib.parse import urlsplit

import c18_budget_contract as budget_contract
import eval_dataset_contract as dataset_contract


SCHEMA_VERSION = "c18-generation-objective-v1"
COMPILER_VERSION = "c18-generation-objective-compiler-v1"
MANIFEST_ID = "rag-eval-dev-v2-generation-objective-nemotron3-super-r2"
DEFAULT_MANIFEST = Path("docs/eval/config/c18-generation-objective-v1.json")
RAW_DIRECTORY = "tmp/eval/c18"
CANARY_IDS = ["fact-001", "definition-001", "reasoning-001", "multi-hop-001", "no-answer-001"]
EXPECTED_RUNTIME = {
    "provider": "openai",
    "endpointIdentity": "https://integrate.api.nvidia.com/v1/chat/completions",
    "model": "nvidia/nemotron-3-super-120b-a12b",
    "requestContractVersion": "openai-compatible-chat-json-v1",
    "temperature": 0.2,
    "maxOutputTokens": 2048,
    "timeoutSeconds": 120,
    "maxRetries": 0,
}
RUNTIME_FINGERPRINT_FIELDS = {
    "schemaVersion",
    "provider",
    "endpointIdentity",
    "model",
    "requestContractVersion",
    "temperature",
    "maxOutputTokens",
    "timeoutSeconds",
    "maxRetries",
    "verificationBasis",
}
EXPECTED_CLAIM_METRIC_CONFIG = {
    "claimMetricVersion": "claim-lexical-v1",
    "claimSplitterVersion": "sentence-list-v1",
    "tokenizerVersion": "ascii-cjk-bigram-v1",
    "lexicalThreshold": 0.70,
    "minClaimTokens": 2,
    "evidencePolicy": "validated-returned-citations-only-v1",
}
TRACKED_OUTPUT_ALLOWLIST = [
    "schemaVersion",
    "status",
    "reasonCodes",
    "compiler",
    "manifest",
    "dataset",
    "runIdentity",
    "expected",
    "actual",
    "identity",
    "providerAttribution",
    "callFacts",
    "channels",
    "metrics",
    "artifacts",
    "privacy",
]
TOOLING_SOURCE_PATHS = (
    "scripts/c18_generation_contract.py",
    "scripts/compile_generation_objective_baseline.py",
    "docs/eval/schema/c18-generation-objective-v1.json",
    "docs/eval/schema/c18-generation-objective-evidence-v1.json",
)
TOOLING_SOURCE_PATHS = (
    "scripts/c18_generation_contract.py",
    "scripts/compile_generation_objective_baseline.py",
    "docs/eval/schema/c18-generation-objective-v1.json",
    "docs/eval/schema/c18-generation-objective-evidence-v1.json",
)


class C18ContractError(ValueError):
    """A stable, user-facing C18 contract failure."""

    def __init__(self, code: str, detail: str = "") -> None:
        self.code = code
        self.detail = detail
        message = code if not detail else f"{code}: {detail}"
        super().__init__(message)


def _fail(code: str, detail: str = "") -> None:
    raise C18ContractError(code, detail)


def _exact(value: Any, fields: set[str], code: str) -> dict[str, Any]:
    if not isinstance(value, dict) or set(value) != fields:
        _fail(code)
    return value


def _sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_sha256(value: Any) -> str:
    return _sha256_bytes(
        json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    )


def _ordered_ids_sha256(ids: list[str]) -> str:
    return canonical_sha256(ids)


def _safe_repo_path(repo_root: Path, value: Any, code: str) -> tuple[str, Path]:
    if not isinstance(value, str) or not value.strip():
        _fail(code)
    normalized = value.replace("\\", "/")
    pure = PurePosixPath(normalized)
    if (
        pure.is_absolute()
        or re.match(r"^[A-Za-z]:/", normalized)
        or ".." in pure.parts
        or "." in pure.parts
    ):
        _fail(code)
    root = repo_root.resolve()
    resolved = (root / Path(*pure.parts)).resolve()
    try:
        resolved.relative_to(root)
    except ValueError:
        _fail(code)
    return pure.as_posix(), resolved


def _load_json(path: Path, code: str) -> tuple[Any, bytes]:
    try:
        raw = path.read_bytes()
        value = json.loads(
            raw.decode("utf-8"),
            parse_constant=lambda item: (_ for _ in ()).throw(ValueError(item)),
            object_pairs_hook=_reject_duplicate_pairs,
        )
    except (OSError, UnicodeError, json.JSONDecodeError, ValueError) as exc:
        _fail(code, str(exc))
    return value, raw


def _reject_duplicate_pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate key: {key}")
        result[key] = value
    return result


def _finite_number(value: Any, code: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(float(value)):
        _fail(code)
    return float(value)


def _descriptor(repo_root: Path, value: Any, code: str) -> dict[str, Any]:
    descriptor = _exact(value, {"path", "sha256", "bytes"}, code)
    relative, resolved = _safe_repo_path(repo_root, descriptor.get("path"), code)
    if not resolved.is_file():
        _fail(code, relative)
    expected_hash = descriptor.get("sha256")
    expected_bytes = descriptor.get("bytes")
    if (
        not isinstance(expected_hash, str)
        or not re.fullmatch(r"[0-9a-f]{64}", expected_hash)
        or isinstance(expected_bytes, bool)
        or not isinstance(expected_bytes, int)
        or expected_bytes < 0
        or sha256_file(resolved) != expected_hash
        or resolved.stat().st_size != expected_bytes
    ):
        _fail("c18_source_hash_drift", relative)
    return {"path": relative, "sha256": expected_hash, "bytes": expected_bytes}


def _expected_dataset(repo_root: Path) -> dict[str, Any]:
    try:
        identity = dataset_contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))
    except dataset_contract.DatasetContractError as exc:
        _fail("c18_dataset_contract_invalid", exc.code)
    question = identity["questionSet"]
    samples = _read_jsonl((repo_root / question["path"]).resolve(), "c18_question_set_invalid")
    ordered_ids = [str(sample.get("id")) for sample in samples]
    return {
        "manifestPath": "docs/eval/dataset-manifest.json",
        "manifestSha256": sha256_file(repo_root / "docs/eval/dataset-manifest.json"),
        "releaseVersion": identity["releaseVersion"],
        "questionSetPath": question["path"],
        "questionSetSha256": question["sha256"],
        "questionSetBytes": question["bytes"],
        "expectedSampleCount": len(samples),
        "orderedIdsSha256": _ordered_ids_sha256(ordered_ids),
        "fixtureCorpus": identity["fixtures"],
        "samples": samples,
    }


def _read_jsonl(path: Path, code: str) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    try:
        with path.open("r", encoding="utf-8") as file:
            for line in file:
                if line.strip():
                    item = json.loads(line)
                    if not isinstance(item, dict):
                        _fail(code)
                    result.append(item)
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        _fail(code, str(exc))
    return result


def _expected_budgets(audit: dict[str, Any]) -> dict[str, dict[str, int]]:
    return {
        mode: {
            "debugRetrieve": int(audit[mode]["debugRetrieval"]),
            "ask": int(audit[mode]["ask"]),
            "generationUpperBound": int(audit[mode]["generationUpperBound"]),
            "queryEmbeddingUpperBound": int(audit[mode]["queryEmbeddingUpperBound"]),
            "modelRerank": int(audit[mode]["modelRerank"]),
            "llmJudge": int(audit[mode]["judge"]),
        }
        for mode in ("canary", "full")
    }


def validate_manifest(repo_root: Path, manifest: dict[str, Any]) -> dict[str, Any]:
    fields = {
        "schemaVersion",
        "manifestId",
        "compilerVersion",
        "toolingSourceSha256",
        "dataset",
        "budgetAudit",
        "execution",
        "retrieval",
        "runtime",
        "metrics",
        "budgets",
        "authorizationBoundary",
        "rawArtifactPolicy",
        "trackedOutputAllowlist",
    }
    _exact(manifest, fields, "c18_manifest_fields_invalid")
    if (
        manifest.get("schemaVersion") != SCHEMA_VERSION
        or manifest.get("manifestId") != MANIFEST_ID
        or manifest.get("compilerVersion") != COMPILER_VERSION
    ):
        _fail("c18_manifest_identity_invalid")

    tooling = _exact(
        manifest.get("toolingSourceSha256"),
        set(TOOLING_SOURCE_PATHS),
        "c18_tooling_identity_invalid",
    )
    for path_value in TOOLING_SOURCE_PATHS:
        path = repo_root / path_value
        if (
            not isinstance(tooling.get(path_value), str)
            or not re.fullmatch(r"[0-9a-f]{64}", tooling[path_value])
            or not path.is_file()
            or sha256_file(path) != tooling[path_value]
        ):
            _fail("c18_tooling_source_drift", path_value)

    expected_dataset = _expected_dataset(repo_root)
    dataset = _exact(
        manifest.get("dataset"),
        {
            "manifestPath",
            "manifestSha256",
            "releaseVersion",
            "questionSetPath",
            "questionSetSha256",
            "questionSetBytes",
            "expectedSampleCount",
            "orderedIdsSha256",
            "fixtureCorpus",
        },
        "c18_dataset_identity_invalid",
    )
    if dataset != {key: expected_dataset[key] for key in dataset}:
        _fail("c18_dataset_identity_invalid")
    if dataset["expectedSampleCount"] != 150:
        _fail("c18_dataset_identity_invalid")
    if [item.get("path") for item in dataset["fixtureCorpus"]] != [item["path"] for item in expected_dataset["fixtureCorpus"]]:
        _fail("c18_fixture_identity_invalid")

    audit = _exact(
        manifest.get("budgetAudit"),
        {"path", "sha256", "schemaVersion", "canaryBudget", "fullBudget"},
        "c18_budget_audit_identity_invalid",
    )
    audit_relative, audit_path = _safe_repo_path(repo_root, audit.get("path"), "c18_budget_audit_identity_invalid")
    if audit_relative != "docs/eval/reports/c18-query-budget-audit-v1.json" or not audit_path.is_file():
        _fail("c18_budget_audit_identity_invalid")
    if audit.get("sha256") != sha256_file(audit_path):
        _fail("c18_budget_audit_source_drift")
    try:
        audit_value = budget_contract.read_json(audit_path)
        budget_contract.validate_audit(repo_root, audit_value)
    except (AttributeError, budget_contract.BudgetContractError, OSError, ValueError) as exc:
        _fail("c18_budget_audit_invalid", str(exc))
    if audit.get("schemaVersion") != "c18-query-budget-audit-v1":
        _fail("c18_budget_audit_identity_invalid")
    expected_budgets = _expected_budgets(audit_value)
    if audit.get("canaryBudget") != expected_budgets["canary"] or audit.get("fullBudget") != expected_budgets["full"]:
        _fail("c18_budget_identity_invalid")

    execution = _exact(
        manifest.get("execution"),
        {
            "mode",
            "measuredRepeats",
            "runIndexes",
            "selectionMode",
            "judgeMode",
            "routerEnabled",
            "answerCache",
            "maxAskRetries",
            "retryAskTimeouts",
            "minimumRequestIntervalSeconds",
            "askDelaySeconds",
            "retrievalDelaySeconds",
        },
        "c18_execution_identity_invalid",
    )
    if execution != {
        "mode": "generation/objective",
        "measuredRepeats": 1,
        "runIndexes": [1],
        "selectionMode": "ordered-v2",
        "judgeMode": "off",
        "routerEnabled": False,
        "answerCache": False,
        "maxAskRetries": 0,
        "retryAskTimeouts": False,
        "minimumRequestIntervalSeconds": 2.2,
        "askDelaySeconds": 2.2,
        "retrievalDelaySeconds": 2.2,
    }:
        _fail("c18_execution_identity_invalid")

    retrieval = _exact(
        manifest.get("retrieval"),
        {"topK", "minScore", "enableRerank", "requestedProvider", "effectiveProvider", "maxFallbackCount", "maxModelCallCount"},
        "c18_retrieval_identity_invalid",
    )
    if retrieval != {
        "topK": 5,
        "minScore": 0.3,
        "enableRerank": True,
        "requestedProvider": "heuristic",
        "effectiveProvider": "heuristic",
        "maxFallbackCount": 0,
        "maxModelCallCount": 0,
    }:
        _fail("c18_retrieval_identity_invalid")

    runtime = _exact(
        manifest.get("runtime"),
        set(EXPECTED_RUNTIME),
        "c18_runtime_identity_invalid",
    )
    if runtime != EXPECTED_RUNTIME:
        _fail("c18_runtime_identity_invalid")
    metrics = _exact(
        manifest.get("metrics"),
        {"claimMetricConfig", "citationPolicy", "noAnswerPolicy"},
        "c18_metric_identity_invalid",
    )
    if metrics.get("claimMetricConfig") != EXPECTED_CLAIM_METRIC_CONFIG:
        _fail("c18_claim_metric_identity_invalid")
    if metrics.get("citationPolicy") != "validated-returned-citations-only-v1" or metrics.get("noAnswerPolicy") != "existing-runner-v1":
        _fail("c18_metric_identity_invalid")

    budgets = _exact(manifest.get("budgets"), {"canary", "full"}, "c18_budget_identity_invalid")
    if budgets != expected_budgets:
        _fail("c18_budget_identity_invalid")

    authorization = _exact(
        manifest.get("authorizationBoundary"),
        {"offlineProviderCalls", "businessDataOutbound", "liveAuthorized", "egress", "localSideEffects", "directCostBasis"},
        "c18_authorization_boundary_invalid",
    )
    if authorization != {
        "offlineProviderCalls": 0,
        "businessDataOutbound": False,
        "liveAuthorized": False,
        "egress": ["tracked_v2_questions", "deterministic_query_variants", "fixture_contexts", "fixed_prompt"],
        "localSideEffects": ["query_count", "successful_answer_history"],
        "directCostBasis": "user-declared-free-no-payment-method",
    }:
        _fail("c18_authorization_boundary_invalid")

    raw_policy = _exact(manifest.get("rawArtifactPolicy"), {"directory", "noOverwrite", "tracked"}, "c18_raw_artifact_policy_invalid")
    if raw_policy != {"directory": RAW_DIRECTORY, "noOverwrite": True, "tracked": False}:
        _fail("c18_raw_artifact_policy_invalid")
    if manifest.get("trackedOutputAllowlist") != TRACKED_OUTPUT_ALLOWLIST:
        _fail("c18_output_allowlist_invalid")

    return {
        "manifest": manifest,
        "dataset": expected_dataset,
        "budgets": expected_budgets,
        "audit": audit_value,
        "manifestSha256": canonical_sha256(manifest),
    }


def load_manifest(repo_root: Path, path: Path | None = None) -> dict[str, Any]:
    manifest_path = path or DEFAULT_MANIFEST
    relative, resolved = _safe_repo_path(repo_root, str(manifest_path), "c18_manifest_path_invalid")
    value, _ = _load_json(resolved, "c18_manifest_json_invalid")
    if not isinstance(value, dict):
        _fail("c18_manifest_json_invalid")
    validated = validate_manifest(repo_root, value)
    return {
        **value,
        "dataset": validated["dataset"],
        "budgets": validated["budgets"],
        "audit": validated["audit"],
        "manifestPath": relative,
        "manifestSha256": validated["manifestSha256"],
    }


def expected_ids(manifest: dict[str, Any], mode: str) -> list[str]:
    if mode == "canary":
        return list(CANARY_IDS)
    if mode == "full":
        return [str(sample["id"]) for sample in manifest["dataset"]["samples"]]
    _fail("c18_mode_invalid")
    return []


def _raw_output_path(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    path = Path(value)
    directory = Path(RAW_DIRECTORY)
    return (
        not path.is_absolute()
        and ".." not in path.parts
        and len(path.parts) > len(directory.parts)
        and path.parts[: len(directory.parts)] == directory.parts
    )


def validate_runner_configuration(
    args: Any,
    manifest: dict[str, Any],
    mode: str,
    selected_ids: list[str],
    run_indexes: list[int],
) -> None:
    if mode not in {"canary", "full"}:
        _fail("c18_mode_invalid")
    if getattr(args, "arm_manifest", "") or getattr(args, "reference_manifest", ""):
        _fail("c18_c17_c7_manifest_conflict")
    if not getattr(args, "keep_existing", False):
        _fail("c18_keep_existing_required")
    if not getattr(args, "include_ask", not getattr(args, "skip_ask", False)):
        _fail("c18_generation_required")
    if getattr(args, "skip_ask", False) or getattr(args, "judge_mode", "off") != "off":
        _fail("c18_judge_or_skip_ask_conflict")
    if int(getattr(args, "repeat", 1)) != 1 or run_indexes != [1]:
        _fail("c18_repeat_identity_invalid")
    if int(getattr(args, "max_ask_retries", 0)) != 0 or getattr(args, "retry_ask_timeouts", False):
        _fail("c18_zero_retry_required")
    retrieval_delay = float(getattr(args, "retrieval_delay_seconds", 0.0))
    ask_delay = float(getattr(args, "ask_delay_seconds", 0.0))
    if retrieval_delay < 2.2 or ask_delay < 2.2:
        _fail("c18_pacing_interval_required")
    if (
        int(getattr(args, "top_k", 0)) != 5
        or float(getattr(args, "min_score", -1)) != 0.3
        or getattr(args, "enable_rerank", False) is not True
    ):
        _fail("c18_run_identity_mismatch")
    if int(getattr(args, "sample_limit", 0)) != 0:
        _fail("c18_selection_mismatch")
    requested_ids = [str(item) for item in (getattr(args, "sample_ids", None) or [])]
    expected = expected_ids(manifest, mode)
    if requested_ids and requested_ids != expected:
        _fail("c18_selection_mismatch")
    if selected_ids != expected:
        _fail("c18_selection_mismatch")
    if mode == "canary" and requested_ids != expected:
        _fail("c18_canary_selection_mismatch")
    if mode == "full" and requested_ids:
        _fail("c18_full_selection_mismatch")
    for value in (getattr(args, "report", ""), getattr(args, "details_json", ""), getattr(args, "metadata_json", "")):
        if not _raw_output_path(value):
            _fail("c18_raw_output_path_required")
    if not getattr(args, "no_overwrite", False):
        _fail("c18_no_overwrite_required")


def validate_runtime_fingerprint(path: Path, manifest: dict[str, Any]) -> dict[str, Any]:
    value, _ = _load_json(path, "c18_runtime_fingerprint_invalid")
    if not isinstance(value, dict) or set(value) != RUNTIME_FINGERPRINT_FIELDS:
        _fail("c18_runtime_fingerprint_invalid")
    for key in value:
        if key not in RUNTIME_FINGERPRINT_FIELDS and re.search(r"token|password|secret|api[_-]?key|authorization", str(key), re.IGNORECASE):
            _fail("c18_runtime_fingerprint_sensitive")
    expected = {
        "schemaVersion": SCHEMA_VERSION,
        **EXPECTED_RUNTIME,
        "verificationBasis": "operator-confirmed",
    }
    if value != expected:
        _fail("c18_runtime_fingerprint_mismatch")
    return {**value, "sha256": canonical_sha256(value)}


def build_plan(manifest: dict[str, Any], mode: str) -> dict[str, Any]:
    ids = expected_ids(manifest, mode)
    budget = manifest["budgets"][mode]
    return {
        "schemaVersion": SCHEMA_VERSION,
        "status": "OFFLINE_VERIFIED",
        "mode": "generation/objective",
        "slice": mode,
        "manifest": {"id": MANIFEST_ID, "sha256": manifest["manifestSha256"]},
        "dataset": {
            "releaseVersion": manifest["dataset"]["releaseVersion"],
            "manifestSha256": manifest["dataset"]["manifestSha256"],
            "questionSetSha256": manifest["dataset"]["questionSetSha256"],
            "sampleCount": len(ids),
            "orderedIdsSha256": _ordered_ids_sha256(ids),
        },
        "selection": {"ids": ids, "count": len(ids), "repeat": 1, "runIndexes": [1]},
        "execution": {
            "topK": 5,
            "minScore": 0.3,
            "enableRerank": True,
            "judgeMode": "off",
            "routerEnabled": False,
            "answerCache": False,
            "maxAskRetries": 0,
            "retryAskTimeouts": False,
            "minimumRequestIntervalSeconds": 2.2,
        },
        "runtime": {**EXPECTED_RUNTIME, "fingerprintStatus": "REQUIRED_BEFORE_LOGIN"},
        "callBudget": budget,
        "authorization": {
            "liveAuthorized": False,
            "offlineProviderCalls": 0,
            "businessDataOutbound": False,
            "egress": list(manifest["authorizationBoundary"]["egress"]),
            "localSideEffects": list(manifest["authorizationBoundary"]["localSideEffects"]),
        },
        "outputs": {
            "rawDirectory": RAW_DIRECTORY,
            "trackedCompilerOutput": True,
            "noOverwrite": True,
        },
        "remainingGates": ["runtime_fingerprint", "pre_provider_budget_guard", "complete_evidence_compiler", "live_authorization"],
    }


class C18BudgetGuard:
    """Fail closed immediately before an owned request would be sent."""

    def __init__(self, budget: dict[str, int]) -> None:
        self.budget = dict(budget)
        self.counts = {
            "debugRetrieve": 0,
            "ask": 0,
            "generationReservations": 0,
            "llmJudge": 0,
        }
        self.rejections: list[dict[str, Any]] = []

    def before_request(self, kind: str) -> None:
        if kind not in {"debugRetrieve", "ask", "llmJudge"}:
            return
        if kind == "llmJudge":
            self._reject(kind, "judge_disabled")
        field = kind
        if self.counts[field] >= int(self.budget[field]):
            self._reject(kind, "budget_exceeded")
        if kind == "ask" and self.counts["generationReservations"] >= int(self.budget["generationUpperBound"]):
            self._reject(kind, "generation_budget_exceeded")
        self.counts[field] += 1
        if kind == "ask":
            self.counts["generationReservations"] += 1

    def _reject(self, kind: str, reason: str) -> None:
        event = {"kind": kind, "reason": reason, "requestSent": False}
        self.rejections.append(event)
        raise C18ContractError("c18_request_rejected", json.dumps(event, ensure_ascii=False, sort_keys=True))

    def snapshot(self) -> dict[str, Any]:
        return {
            "counts": dict(self.counts),
            "budget": dict(self.budget),
            "rejections": list(self.rejections),
            "requestRejectionCount": len(self.rejections),
        }


def request_kind(url: str) -> str | None:
    path = urlsplit(url).path
    if path.endswith("/api/qa/debug/retrieve"):
        return "debugRetrieve"
    if path.endswith("/api/qa/ask"):
        return "ask"
    if path.endswith("/chat/completions"):
        return "llmJudge"
    return None


def execution_facts(
    ask_response: dict[str, Any] | None,
    ask_attempts: int,
    ask_retries: int,
    rate_limit_errors: int,
    rerank_attribution: dict[str, Any],
    expected_runtime: dict[str, Any] | None = None,
) -> dict[str, Any]:
    metadata = ask_response.get("metadata", {}) if isinstance(ask_response, dict) else {}
    if not isinstance(metadata, dict):
        metadata = {}
    status = str(metadata.get("status") or "")
    # A model can itself refuse. Only the service's empty-context no-result
    # branch lacks both a model identity and returned contexts.
    generation_bypass = 1 if (status == "no_result" and not metadata.get("model")
                             and not metadata.get("llmModel")
                             and not (ask_response or {}).get("contexts")) else 0
    generation_calls = 0 if generation_bypass else 1 if ask_response is not None else None
    cache_hit = bool(metadata.get("cached") or metadata.get("cacheHit"))
    expected_model = (expected_runtime or EXPECTED_RUNTIME).get("model")
    observed_model = metadata.get("llmModel") or metadata.get("model") or expected_model
    provider_fallback = 0 if observed_model == expected_model else 1
    llm_retry_count = metadata.get("llmRetryCount", 0)
    try:
        llm_retry_count = int(llm_retry_count)
    except (TypeError, ValueError):
        llm_retry_count = None
    return {
        "askHttpAttempts": ask_attempts,
        "askRetryCount": ask_retries,
        "rateLimitErrors": rate_limit_errors,
        "generationCalls": generation_calls,
        "generationBypassCount": generation_bypass,
        "cacheRequestEnabled": False,
        "answerCacheHitCount": 1 if cache_hit else 0,
        "algorithmFallbackCount": int(rerank_attribution.get("fallbackCount", 0) or 0),
        "algorithmFallbackReason": str(rerank_attribution.get("fallbackReason", "unknown")),
        "providerFallbackCount": provider_fallback,
        "automaticRetryCount": ask_retries + max(0, llm_retry_count or 0),
        "generationModel": str(observed_model),
    }
