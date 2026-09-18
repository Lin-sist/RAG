#!/usr/bin/env python3
"""Compile one C18 raw run into a safe, tracked objective-baseline summary."""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

import c18_generation_contract as c18


STATUS_EXIT_CODES = {"COMPLETE": 0, "INVALID": 2, "NOT_COMPARABLE": 3, "INCOMPLETE": 4}
SENSITIVE_KEY_PATTERN = re.compile(r"token|password|secret|api[_-]?key|authorization", re.IGNORECASE)
ABSOLUTE_PATH_PATTERN = re.compile(r"(?:^[A-Za-z]:[\\/]|^\\\\|^/)")


def _reject_duplicate_pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    value: dict[str, Any] = {}
    for key, item in pairs:
        if key in value:
            raise ValueError(f"duplicate key: {key}")
        value[key] = item
    return value


def read_json(path: Path) -> dict[str, Any]:
    raw = path.read_text(encoding="utf-8")
    value = json.loads(
        raw,
        object_pairs_hook=_reject_duplicate_pairs,
        parse_constant=lambda item: (_ for _ in ()).throw(ValueError(item)),
    )
    if not isinstance(value, dict):
        raise ValueError("JSON root must be an object")
    return value


def _sha256_file(path: Path) -> str:
    return c18.sha256_file(path)


def _base_result(manifest: dict[str, Any], mode: str) -> dict[str, Any]:
    ids = c18.expected_ids(manifest, mode)
    dataset = manifest["dataset"]
    return {
        "schemaVersion": "c18-generation-objective-evidence-v1",
        "status": "INVALID",
        "reasonCodes": [],
        "compiler": {"version": c18.COMPILER_VERSION, "mode": mode},
        "manifest": {"id": c18.MANIFEST_ID, "sha256": manifest["manifestSha256"]},
        "dataset": {
            "releaseVersion": dataset["releaseVersion"],
            "manifestSha256": dataset["manifestSha256"],
            "questionSetSha256": dataset["questionSetSha256"],
            "sampleCount": len(ids),
            "orderedIdsSha256": c18.canonical_sha256(ids),
        },
        "runIdentity": {
            "mode": "generation/objective",
            "slice": mode,
            "repeat": 1,
            "runIndex": 1,
        },
        "expected": {"sampleCount": len(ids), "orderedIdsSha256": c18.canonical_sha256(ids), "callBudget": manifest["budgets"][mode]},
        "actual": {},
        "identity": {},
        "providerAttribution": {},
        "callFacts": {},
        "channels": {},
        "metrics": {},
        "artifacts": {},
        "privacy": {
            "rawArtifactsLocalIgnored": True,
            "rawQuestionAnswerContextsTracked": False,
            "credentialsTracked": False,
            "numericKnowledgeBaseIdsTracked": False,
            "absolutePathsTracked": False,
            "noOverwrite": True,
        },
    }


def _reason(result: dict[str, Any], code: str) -> None:
    reasons = result.setdefault("reasonCodes", [])
    if code not in reasons:
        reasons.append(code)


def _is_nonnegative_int(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and value >= 0


def _safe_metric_value(value: Any) -> bool:
    if isinstance(value, bool) or isinstance(value, (int, float)):
        return not isinstance(value, float) or math.isfinite(value)
    return isinstance(value, str) and len(value) <= 40 and not ABSOLUTE_PATH_PATTERN.search(value)


def _safe_metrics(metrics: Any) -> dict[str, Any]:
    if not isinstance(metrics, dict):
        return {}

    def pick(*names: str) -> dict[str, Any]:
        return {name: metrics[name] for name in names if name in metrics and _safe_metric_value(metrics[name])}

    return {
        "retrieval": pick("recall_at_3", "recall_at_5", "mrr", "top1_source_accuracy"),
        "generation": pick(
            "answer_keyword_hit_rate",
            "answer_keyword_hits",
            "answer_keyword_total",
            "answerable_ask_success_samples",
        ),
        "citation": pick(
            "citation_hit_rate",
            "citation_source_hit_rate",
            "citation_snippet_hit_rate",
            "citation_source_hits",
            "citation_source_total",
            "citation_snippet_hits",
            "citation_snippet_total",
            "unsupported_citation_count",
            "no_answer_citation_violation_count",
        ),
        "claim": pick(
            "claim_metric_status",
            "claim_total",
            "supported_claim_count",
            "unsupported_claim_count",
            "objective_claim_support_rate",
        ),
        "noAnswer": pick("no_answer_accuracy", "no_answer_ok_count", "no_answer_evaluable_total"),
        "latency": pick("retrieval_latency_millis"),
    }


def _validate_metadata(
    metadata: dict[str, Any],
    manifest: dict[str, Any],
    mode: str,
    expected_ids: list[str],
    result: dict[str, Any],
) -> tuple[bool, bool]:
    """Return (invalid, not_comparable)."""
    invalid = False
    not_comparable = False
    dataset = manifest["dataset"]
    if metadata.get("evaluationSchema") != c18.SCHEMA_VERSION:
        _reason(result, "metadata_schema_mismatch")
        not_comparable = True
    c18_manifest = metadata.get("c18Manifest")
    if not isinstance(c18_manifest, dict) or c18_manifest != {"id": c18.MANIFEST_ID, "sha256": manifest["manifestSha256"]}:
        _reason(result, "manifest_identity_mismatch")
        not_comparable = True
    if metadata.get("datasetValidation") != "VALID":
        _reason(result, "dataset_not_valid")
        not_comparable = True
    release = metadata.get("datasetReleaseIdentity")
    if not isinstance(release, dict):
        _reason(result, "dataset_identity_missing")
        not_comparable = True
    else:
        if (
            release.get("releaseVersion") != dataset["releaseVersion"]
            or release.get("questionSet", {}).get("sha256") != dataset["questionSetSha256"]
            or release.get("questionSet", {}).get("bytes") != dataset["questionSetBytes"]
            or release.get("manifestPath") != dataset["manifestPath"]
        ):
            _reason(result, "dataset_identity_mismatch")
            not_comparable = True
    selection = metadata.get("sampleSelection")
    if not isinstance(selection, dict) or selection.get("ids") != expected_ids or selection.get("count") != len(expected_ids):
        _reason(result, "selection_identity_mismatch")
        not_comparable = True
    if (
        metadata.get("topK") != 5
        or metadata.get("minScore") != 0.3
        or metadata.get("enableRerank") is not True
        or metadata.get("claimMetricConfig") != c18.EXPECTED_CLAIM_METRIC_CONFIG
    ):
        _reason(result, "metric_or_retrieval_identity_mismatch")
        not_comparable = True
    run_identity = metadata.get("c18RunIdentity")
    if run_identity != {"mode": "generation/objective", "slice": mode, "repeat": 1, "runIndex": 1}:
        _reason(result, "run_identity_mismatch")
        not_comparable = True
    execution = metadata.get("c18Execution")
    if not isinstance(execution, dict):
        _reason(result, "execution_identity_missing")
        not_comparable = True
    else:
        fixed_execution = {
            "selectionMode": "ordered-v2",
            "judgeMode": "off",
            "routerEnabled": False,
            "answerCache": False,
            "maxAskRetries": 0,
            "retryAskTimeouts": False,
            "minimumRequestIntervalSeconds": 2.2,
        }
        if any(execution.get(key) != value for key, value in fixed_execution.items()):
            _reason(result, "execution_identity_mismatch")
            not_comparable = True
        for key in ("askDelaySeconds", "retrievalDelaySeconds"):
            try:
                if not math.isfinite(float(execution.get(key))) or float(execution[key]) < 2.2:
                    raise ValueError
            except (TypeError, ValueError):
                _reason(result, "execution_pacing_identity_mismatch")
                not_comparable = True
    runtime = metadata.get("c18RuntimeFingerprint")
    expected_runtime_fingerprint_sha = c18.canonical_sha256(
        {
            "schemaVersion": c18.SCHEMA_VERSION,
            **c18.EXPECTED_RUNTIME,
            "verificationBasis": "operator-confirmed",
        }
    )
    if (
        not isinstance(runtime, dict)
        or runtime.get("status") != "verified"
        or runtime.get("identity") != c18.EXPECTED_RUNTIME
        or runtime.get("sha256") != expected_runtime_fingerprint_sha
    ):
        _reason(result, "runtime_fingerprint_missing_or_mismatch")
        not_comparable = True
    git = metadata.get("git")
    if not isinstance(git, dict) or not isinstance(git.get("head"), str) or not git.get("head") or git.get("clean") is not True:
        _reason(result, "git_provenance_not_clean")
        not_comparable = True
    fixtures = metadata.get("fixtures")
    normalized_fixtures = (
        [{"path": str(item.get("path", "")).replace("\\", "/"),
          "sha256": item.get("sha256"), "bytes": item.get("bytes")} for item in fixtures]
        if isinstance(fixtures, list) and all(isinstance(item, dict) for item in fixtures)
        else None
    )
    if normalized_fixtures != manifest["dataset"]["fixtureCorpus"]:
        _reason(result, "fixture_identity_mismatch")
        not_comparable = True
    # The shared judge descriptor has no mode field. C18 execution above binds
    # judgeMode=off; reject an explicit conflicting legacy mode if present.
    judge_config = metadata.get("judgeContractConfig")
    if not isinstance(judge_config, dict) or judge_config.get("mode", "off") != "off":
        _reason(result, "judge_identity_mismatch")
        not_comparable = True
    return invalid, not_comparable


def _validate_sample(
    sample: dict[str, Any],
    expected_sample: dict[str, Any],
    expected_runtime: dict[str, Any],
    result: dict[str, Any],
) -> tuple[bool, bool]:
    invalid = False
    incomplete = False
    if not isinstance(sample, dict) or sample.get("id") != expected_sample.get("id"):
        _reason(result, "sample_identity_mismatch")
        return True, False
    errors = sample.get("errors")
    if not isinstance(errors, dict) or errors.get("retrieval") is not None or errors.get("ask") is not None:
        _reason(result, "sample_error")
        incomplete = True
    if not isinstance(sample.get("debugRetrieveRawResponse"), dict) or not isinstance(sample.get("askRawResponse"), dict):
        _reason(result, "sample_observation_missing")
        incomplete = True
    rerank = sample.get("rerankAttribution")
    if (
        not isinstance(rerank, dict)
        or rerank.get("requestedProvider") != "heuristic"
        or rerank.get("effectiveProvider") != "heuristic"
        or rerank.get("fallbackCount") != 0
        or rerank.get("modelCallCount") != 0
    ):
        _reason(result, "rerank_identity_or_fallback_violation")
        incomplete = True
    details = sample.get("metricCalculationDetails")
    if (
        not isinstance(details, dict)
        or details.get("askSkipped") is not False
        or details.get("askAttempts") != 1
        or details.get("askRetries") != 0
        or details.get("rateLimitErrors") != 0
    ):
        _reason(result, "ask_attempt_contract_violation")
        incomplete = True
    claim = sample.get("objectiveClaimMetrics")
    expected_claim_status = "COMPLETE" if expected_sample.get("should_answer", True) else "NOT_APPLICABLE"
    if not isinstance(claim, dict) or claim.get("claimMetricStatus") != expected_claim_status or claim.get("claimMetricConfig") != c18.EXPECTED_CLAIM_METRIC_CONFIG:
        _reason(result, "claim_metric_incomplete_or_identity_mismatch")
        incomplete = True
    ask = sample.get("askRawResponse")
    if isinstance(ask, dict):
        ask_metadata = ask.get("metadata")
        if not isinstance(ask_metadata, dict) or (
            ask_metadata.get("cached") is not False
            and ask_metadata.get("status") != "no_result"
        ):
            _reason(result, "answer_cache_not_disabled_or_unobservable")
            incomplete = True
        observed_model = ask_metadata.get("llmModel") or ask_metadata.get("model") or expected_runtime["model"] if isinstance(ask_metadata, dict) else None
        if observed_model != expected_runtime["model"]:
            _reason(result, "generation_provider_identity_mismatch")
            incomplete = True
    facts = sample.get("c18ExecutionFacts")
    required_ints = (
        "askHttpAttempts",
        "askRetryCount",
        "rateLimitErrors",
        "generationBypassCount",
        "answerCacheHitCount",
        "algorithmFallbackCount",
        "providerFallbackCount",
        "automaticRetryCount",
    )
    if not isinstance(facts, dict) or any(not _is_nonnegative_int(facts.get(field)) for field in required_ints):
        _reason(result, "execution_facts_missing")
        incomplete = True
    else:
        if facts.get("cacheRequestEnabled") is not False:
            _reason(result, "answer_cache_request_not_disabled")
            incomplete = True
        if facts.get("askHttpAttempts") != 1 or any(facts.get(field) != 0 for field in ("askRetryCount", "rateLimitErrors", "answerCacheHitCount", "providerFallbackCount", "automaticRetryCount")):
            _reason(result, "execution_fact_budget_violation")
            incomplete = True
        generation_calls = facts.get("generationCalls")
        if generation_calls not in (0, 1) or facts.get("generationBypassCount") not in (0, 1) or generation_calls + facts.get("generationBypassCount") != 1:
            _reason(result, "generation_call_fact_invalid")
            incomplete = True
        if facts.get("generationBypassCount") == 1 and isinstance(ask, dict) and ask.get("metadata", {}).get("status") != "no_result":
            _reason(result, "no_answer_generation_bypass_mismatch")
            incomplete = True
        if (facts.get("generationBypassCount") == 0 and isinstance(ask, dict)
                and ask.get("metadata", {}).get("status") == "no_result"
                and not ask.get("metadata", {}).get("model")
                and not ask.get("contexts")):
            _reason(result, "generation_bypass_fact_mismatch")
            incomplete = True
    return invalid, incomplete


def compile_evidence(
    repo_root: Path,
    manifest: dict[str, Any],
    mode: str,
    details_path: Path,
    metadata_path: Path,
) -> dict[str, Any]:
    result = _base_result(manifest, mode)
    expected_ids = c18.expected_ids(manifest, mode)
    expected_samples = {str(sample["id"]): sample for sample in manifest["dataset"]["samples"]}
    invalid = False
    not_comparable = False
    incomplete = False

    try:
        details = read_json(details_path)
        metadata = read_json(metadata_path)
    except (OSError, UnicodeError, ValueError, json.JSONDecodeError):
        _reason(result, "raw_artifact_unreadable")
        result["status"] = "INVALID"
        return result

    _, metadata_not_comparable = _validate_metadata(metadata, manifest, mode, expected_ids, result)
    not_comparable = not_comparable or metadata_not_comparable
    if details.get("runMetadata") != metadata:
        _reason(result, "details_metadata_mismatch")
        not_comparable = True
    if (
        details.get("reportStatus") != "CLEAN"
        or details.get("objectiveMetricStatus") != "COMPLETE"
        or details.get("judgeMetricStatus") != "SKIPPED"
        or details.get("datasetValidation") != "VALID"
    ):
        _reason(result, "channel_status_not_complete")
        incomplete = True
    counts = details.get("runCounts")
    expected_counts = {"askErrors": 0, "retrieveErrors": 0, "skippedAsk": 0, "judgeErrors": 0, "skippedJudge": len(expected_ids), "rateLimitErrors": 0, "retryCount": 0}
    if counts != expected_counts:
        _reason(result, "run_counts_not_clean")
        incomplete = True
    if (
        details.get("sampleCount") != len(expected_ids)
        or details.get("sampleIds") != expected_ids
        or not isinstance(details.get("samples"), list)
        or [item.get("id") for item in details.get("samples", []) if isinstance(item, dict)] != expected_ids
    ):
        _reason(result, "sample_order_or_count_mismatch")
        incomplete = True
    if details.get("claimMetricConfig") != c18.EXPECTED_CLAIM_METRIC_CONFIG:
        _reason(result, "claim_metric_identity_mismatch")
        not_comparable = True
    if details.get("skipAsk") is not False or details.get("judge", {}).get("mode") != "off":
        _reason(result, "generation_or_judge_mode_mismatch")
        not_comparable = True

    samples = details.get("samples") if isinstance(details.get("samples"), list) else []
    if len(samples) != len(expected_ids):
        incomplete = True
    for item in samples:
        item_id = str(item.get("id")) if isinstance(item, dict) else ""
        expected = expected_samples.get(item_id)
        if expected is None:
            _reason(result, "sample_identity_mismatch")
            invalid = True
            continue
        sample_invalid, sample_incomplete = _validate_sample(item, expected, manifest["runtime"], result)
        invalid = invalid or sample_invalid
        incomplete = incomplete or sample_incomplete

    actual_facts = {
        "debugRetrieveHttpAttempts": len(samples),
        "askHttpAttempts": sum(item.get("c18ExecutionFacts", {}).get("askHttpAttempts", 0) for item in samples if isinstance(item, dict)),
        "generationCalls": sum(item.get("c18ExecutionFacts", {}).get("generationCalls", 0) or 0 for item in samples if isinstance(item, dict)),
        "generationBypassCount": sum(item.get("c18ExecutionFacts", {}).get("generationBypassCount", 0) for item in samples if isinstance(item, dict)),
        "answerCacheHitCount": sum(item.get("c18ExecutionFacts", {}).get("answerCacheHitCount", 0) for item in samples if isinstance(item, dict)),
        "algorithmFallbackCount": sum(item.get("c18ExecutionFacts", {}).get("algorithmFallbackCount", 0) for item in samples if isinstance(item, dict)),
        "providerFallbackCount": sum(item.get("c18ExecutionFacts", {}).get("providerFallbackCount", 0) for item in samples if isinstance(item, dict)),
        "automaticRetryCount": sum(item.get("c18ExecutionFacts", {}).get("automaticRetryCount", 0) for item in samples if isinstance(item, dict)),
        "queryEmbeddingUpperBound": manifest["budgets"][mode]["queryEmbeddingUpperBound"],
        "queryEmbeddingObservation": "BOUND_FROM_OFFLINE_AUDIT_NOT_DIRECTLY_OBSERVED",
        "llmJudgeHttpAttempts": 0,
    }
    if actual_facts["debugRetrieveHttpAttempts"] > manifest["budgets"][mode]["debugRetrieve"] or actual_facts["askHttpAttempts"] > manifest["budgets"][mode]["ask"] or actual_facts["generationCalls"] > manifest["budgets"][mode]["generationUpperBound"]:
        _reason(result, "call_budget_exceeded")
        incomplete = True
    guard_snapshot = details.get("c18Execution")
    if isinstance(guard_snapshot, dict):
        observed_counts = guard_snapshot.get("counts")
        if isinstance(observed_counts, dict) and (
            observed_counts.get("debugRetrieve") != actual_facts["debugRetrieveHttpAttempts"]
            or observed_counts.get("ask") != actual_facts["askHttpAttempts"]
            or observed_counts.get("generationReservations") != actual_facts["askHttpAttempts"]
            or observed_counts.get("llmJudge") != 0
        ):
            _reason(result, "budget_guard_snapshot_mismatch")
            incomplete = True
        if guard_snapshot.get("requestRejectionCount") != 0:
            _reason(result, "budget_guard_rejected_request")
            incomplete = True
    else:
        _reason(result, "budget_guard_snapshot_missing")
        incomplete = True

    safe_metrics = _safe_metrics(details.get("metrics"))
    if not safe_metrics:
        _reason(result, "aggregate_metrics_missing")
        incomplete = True

    result["actual"] = {
        "sampleCount": len(samples),
        "orderedIdsSha256": c18.canonical_sha256([item.get("id") for item in samples if isinstance(item, dict)]),
        "runCounts": counts if isinstance(counts, dict) else {},
    }
    result["identity"] = {
        "gitHead": metadata.get("git", {}).get("head"),
        "gitClean": metadata.get("git", {}).get("clean") is True,
        "runtime": {
            "provider": manifest["runtime"]["provider"],
            "model": manifest["runtime"]["model"],
            "requestContractVersion": manifest["runtime"]["requestContractVersion"],
        },
        "runtimeFingerprintSha256": metadata.get("c18RuntimeFingerprint", {}).get("sha256"),
    }
    models = Counter(
        str(item.get("c18ExecutionFacts", {}).get("generationModel"))
        for item in samples
        if isinstance(item, dict) and item.get("c18ExecutionFacts", {}).get("generationModel")
    )
    result["providerAttribution"] = {
        "generationModels": dict(sorted(models.items())),
        "algorithmFallbackCount": actual_facts["algorithmFallbackCount"],
        "providerFallbackCount": actual_facts["providerFallbackCount"],
        "answerCacheHitCount": actual_facts["answerCacheHitCount"],
    }
    unknown_generation = sum(
        1 for item in samples
        if item.get("c18ExecutionFacts", {}).get("generationCalls") not in (0, 1)
    )
    if unknown_generation:
        actual_facts["generationCalls"] = None
        actual_facts["generationUnknownSampleCount"] = unknown_generation
    result["callFacts"] = actual_facts
    result["channels"] = {
        "retrieval": "COMPLETE" if not incomplete else "INCOMPLETE",
        "generation": "COMPLETE" if not incomplete else "INCOMPLETE",
        "citation": "COMPLETE" if not incomplete else "INCOMPLETE",
        "objectiveClaim": "COMPLETE" if not incomplete else "INCOMPLETE",
        "noAnswer": "COMPLETE" if not incomplete else "INCOMPLETE",
        "judge": "SKIPPED",
    }
    result["metrics"] = safe_metrics
    result["artifacts"] = {
        "detailsSha256": _sha256_file(details_path),
        "detailsBytes": details_path.stat().st_size,
        "metadataSha256": _sha256_file(metadata_path),
        "metadataBytes": metadata_path.stat().st_size,
    }

    if invalid:
        result["status"] = "INVALID"
    elif not_comparable:
        result["status"] = "NOT_COMPARABLE"
    elif incomplete:
        result["status"] = "INCOMPLETE"
    else:
        result["status"] = "COMPLETE"
    return result


def _assert_safe_output(value: Any, path: str = "root") -> None:
    if isinstance(value, dict):
        for key, item in value.items():
            if SENSITIVE_KEY_PATTERN.search(str(key)):
                raise ValueError(f"sensitive output key: {path}.{key}")
            _assert_safe_output(item, f"{path}.{key}")
    elif isinstance(value, list):
        for index, item in enumerate(value):
            _assert_safe_output(item, f"{path}[{index}]")
    elif isinstance(value, str) and ABSOLUTE_PATH_PATTERN.search(value):
        raise ValueError(f"absolute path in tracked output: {path}")


def write_output(path: Path, result: dict[str, Any], no_overwrite: bool) -> None:
    if no_overwrite and path.exists():
        raise FileExistsError(path)
    _assert_safe_output(result)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")


def _under_directory(value: str, directory: str) -> bool:
    path = Path(value)
    expected = Path(directory)
    return (
        not path.is_absolute()
        and ".." not in path.parts
        and len(path.parts) > len(expected.parts)
        and path.parts[: len(expected.parts)] == expected.parts
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compile C18 raw generation evidence into a safe summary.")
    parser.add_argument("--repo-root", default=".")
    parser.add_argument("--manifest", default=str(c18.DEFAULT_MANIFEST))
    parser.add_argument("--mode", choices=("canary", "full"), default="full")
    parser.add_argument("--details", required=True)
    parser.add_argument("--metadata", required=True)
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    repo_root = Path(args.repo_root).resolve()
    if not args.no_overwrite:
        print("C18 compiler requires --no-overwrite", file=sys.stderr)
        return 2
    if (
        not _under_directory(args.details, c18.RAW_DIRECTORY)
        or not _under_directory(args.metadata, c18.RAW_DIRECTORY)
        or not _under_directory(args.output_json, "docs/eval/reports")
    ):
        print("C18 compiler path policy rejected", file=sys.stderr)
        return 2
    try:
        manifest = c18.load_manifest(repo_root, Path(args.manifest))
        result = compile_evidence(repo_root, manifest, args.mode, Path(args.details), Path(args.metadata))
        write_output(Path(args.output_json), result, args.no_overwrite)
    except c18.C18ContractError as exc:
        print(f"C18 compiler failed: {exc.code}", file=sys.stderr)
        return 2
    except FileExistsError:
        print("--no-overwrite refused to overwrite compiler output", file=sys.stderr)
        return 2
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"C18 compiler failed: {exc}", file=sys.stderr)
        return 2
    print(json.dumps({"status": result["status"], "reasonCodes": result["reasonCodes"]}, ensure_ascii=False))
    return STATUS_EXIT_CODES[result["status"]]


if __name__ == "__main__":
    raise SystemExit(main())
