#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import math
import statistics
import sys
from pathlib import Path
from typing import Any

import eval_dataset_contract
import evaluate_quality_gate
import run_reproducible_rag_eval as runner


OUTPUT_SCHEMA = "c17-retrieval-reference-evidence-v1"
LOCKED_REFERENCE_SCHEMA = "rag-quality-gate-reference-v1"
DISTRIBUTION_DECIMAL_PLACES = 12


class ReferenceCompileError(RuntimeError):
    def __init__(self, code: str) -> None:
        super().__init__(code)
        self.code = code


def _read_json(path: Path, code: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise ReferenceCompileError(code) from exc
    if not isinstance(value, dict):
        raise ReferenceCompileError(code)
    return value


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    try:
        return [
            json.loads(line)
            for line in path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise ReferenceCompileError("dataset_samples_invalid") from exc


def _sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _sha256_file(path: Path) -> str:
    try:
        return _sha256_bytes(path.read_bytes())
    except OSError as exc:
        raise ReferenceCompileError("artifact_missing") from exc


def _canonical_hash(value: Any) -> str:
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return _sha256_bytes(payload)


def _finite_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(float(value))


def summarize_observations(values: list[float]) -> dict[str, float]:
    if not values or any(not _finite_number(value) for value in values):
        raise ReferenceCompileError("distribution_value_invalid")
    minimum = min(float(value) for value in values)
    maximum = max(float(value) for value in values)
    return {
        "minimum": round(minimum, DISTRIBUTION_DECIMAL_PLACES),
        "median": round(float(statistics.median(values)), DISTRIBUTION_DECIMAL_PLACES),
        "maximum": round(maximum, DISTRIBUTION_DECIMAL_PLACES),
        "spread": round(maximum - minimum, DISTRIBUTION_DECIMAL_PLACES),
    }


def _safe_document_identity(metadata: dict[str, Any]) -> dict[str, Any]:
    knowledge_base = metadata.get("knowledgeBase") or {}
    documents = knowledge_base.get("documents") or []
    fixtures = metadata.get("fixtures") or []
    return {
        "knowledgeBase": {
            "name": knowledge_base.get("name"),
            "description": knowledge_base.get("description"),
            "documentCount": knowledge_base.get("documentCount"),
            "chunkCount": knowledge_base.get("chunkCount"),
            "documents": sorted(
                [
                    {
                        "title": document.get("title"),
                        "status": document.get("status"),
                        "chunkCount": document.get("chunkCount"),
                        "contentHash": document.get("contentHash"),
                    }
                    for document in documents
                    if isinstance(document, dict)
                ],
                key=lambda item: str(item.get("title")),
            ),
        },
        "fixtures": sorted(
            [
                {
                    "name": fixture.get("name"),
                    "sha256": fixture.get("sha256"),
                    "bytes": fixture.get("bytes"),
                }
                for fixture in fixtures
                if isinstance(fixture, dict)
            ],
            key=lambda item: str(item.get("name")),
        ),
    }


def _strict_identity(metadata: dict[str, Any]) -> dict[str, Any]:
    repeat = metadata.get("repeat") or {}
    return {
        "evaluationSchema": metadata.get("evaluationSchema"),
        "datasetValidation": metadata.get("datasetValidation"),
        "datasetReleaseIdentity": metadata.get("datasetReleaseIdentity"),
        "evalSetIdentity": metadata.get("evalSetIdentity"),
        "sampleSelection": metadata.get("sampleSelection"),
        "runIdentity": {
            "topK": metadata.get("topK"),
            "minScore": metadata.get("minScore"),
            "enableRerank": metadata.get("enableRerank"),
        },
        "documents": _safe_document_identity(metadata),
        "configSnapshot": metadata.get("configSnapshot"),
        "git": metadata.get("git"),
        "claimMetricConfig": metadata.get("claimMetricConfig"),
        "judgeContractConfig": metadata.get("judgeContractConfig"),
        "referenceManifest": metadata.get("referenceManifest"),
        "repeatTotal": repeat.get("total"),
        "warmup": metadata.get("warmup"),
    }


def _metadata_matches_release(
    metadata: dict[str, Any],
    manifest: dict[str, Any],
    dataset_identity: dict[str, Any],
) -> bool:
    question_set = dataset_identity["questionSet"]
    eval_set_identity = metadata.get("evalSetIdentity") or {}
    logical_kb = dataset_identity.get("logicalKnowledgeBase") or {}
    knowledge_base = metadata.get("knowledgeBase") or {}
    documents = knowledge_base.get("documents") or []
    fixtures = metadata.get("fixtures") or []
    expected_fixtures = {
        Path(item["path"]).name: {
            "name": Path(item["path"]).name,
            "sha256": item["sha256"],
            "bytes": item["bytes"],
        }
        for item in dataset_identity["fixtures"]
    }
    actual_fixtures = {
        str(item.get("name")): {
            "name": item.get("name"),
            "sha256": item.get("sha256"),
            "bytes": item.get("bytes"),
        }
        for item in fixtures
        if isinstance(item, dict)
    }
    actual_documents = {
        str(item.get("title")): item
        for item in documents
        if isinstance(item, dict)
    }
    expected_document_names = set(logical_kb.get("expectedDocumentNames") or [])
    if set(actual_documents) != expected_document_names:
        return False
    if any(
        document.get("status") != "COMPLETED"
        or not isinstance(document.get("chunkCount"), int)
        or isinstance(document.get("chunkCount"), bool)
        or document["chunkCount"] <= 0
        or not isinstance(document.get("contentHash"), str)
        or not document["contentHash"]
        for name, document in actual_documents.items()
    ):
        return False
    config_snapshot = metadata.get("configSnapshot")
    git_head = (metadata.get("git") or {}).get("head")
    if not isinstance(config_snapshot, dict) or not config_snapshot:
        return False
    if any(
        not isinstance(item, dict)
        or not isinstance(item.get("sha256"), str)
        or len(item["sha256"]) != 64
        or not isinstance(item.get("bytes"), int)
        or isinstance(item.get("bytes"), bool)
        or item["bytes"] <= 0
        for item in config_snapshot.values()
    ):
        return False
    return (
        metadata.get("evaluationSchema") == runner.C17_REFERENCE_SCHEMA
        and eval_set_identity.get("sha256") == question_set["sha256"]
        and eval_set_identity.get("bytes") == question_set["bytes"]
        and knowledge_base.get("name") == logical_kb.get("name")
        and knowledge_base.get("description") == logical_kb.get("marker")
        and knowledge_base.get("documentCount") == len(expected_document_names)
        and knowledge_base.get("chunkCount")
        == sum(document["chunkCount"] for document in actual_documents.values())
        and actual_fixtures == expected_fixtures
        and isinstance(git_head, str)
        and bool(git_head)
        and metadata.get("armManifest") is None
        and metadata.get("referenceManifest")
        == {
            "referenceId": manifest["referenceId"],
            "sha256": manifest["sha256"],
            "mode": "full",
        }
        and metadata.get("warmup") == {"calls": 0}
    )


def _same_json(left: Any, right: Any) -> bool:
    return _canonical_hash(left) == _canonical_hash(right)


def _base_result(
    manifest: dict[str, Any],
    profile: dict[str, Any],
    profile_sha256: str,
    dataset_identity: dict[str, Any],
) -> dict[str, Any]:
    return {
        "schemaVersion": OUTPUT_SCHEMA,
        "status": "INVALID",
        "activationStatus": "BLOCKED",
        "reasonCodes": [],
        "compiler": {"version": manifest["compilerVersion"]},
        "manifest": {
            "referenceId": manifest["referenceId"],
            "sha256": manifest["sha256"],
        },
        "profile": {
            "id": profile.get("profileId"),
            "version": profile.get("profileVersion"),
            "status": profile.get("status"),
            "thresholdStatus": profile.get("thresholdStatus"),
            "sha256": profile_sha256,
        },
        "dataset": {
            "releaseVersion": dataset_identity.get("releaseVersion"),
            "manifestSha256": dataset_identity.get("manifestSha256"),
        },
        "runIdentity": dict(manifest["execution"]["runIdentity"]),
        "expected": {
            "repeatCount": manifest["full"]["measuredRepeats"],
            "runIndexes": list(manifest["execution"]["runIndexes"]),
            "samplesPerRepeat": manifest["full"]["expectedSampleCount"],
            "observationCount": (
                manifest["full"]["expectedSampleCount"]
                * manifest["full"]["measuredRepeats"]
            ),
        },
        "actual": {
            "repeatCount": 0,
            "runIndexes": [],
            "samplesPerRepeat": [],
            "observationCount": 0,
        },
        "identity": {},
        "providerAttribution": {
            "status": "PENDING",
            "requestedProvider": manifest["providerPolicy"]["requestedProvider"],
            "effectiveProvider": manifest["providerPolicy"]["effectiveProvider"],
            "fallbackCount": 0,
            "modelCallCount": 0,
        },
        "callFacts": {
            "basis": "manifest_upper_bounds",
            **manifest["full"]["callBudget"],
        },
        "artifacts": [],
        "rules": [],
    }


def _invalid_result(code: str) -> dict[str, Any]:
    return {
        "schemaVersion": OUTPUT_SCHEMA,
        "status": "INVALID",
        "activationStatus": "BLOCKED",
        "reasonCodes": [code],
        "compiler": {"version": "c17-retrieval-reference-compiler-v1"},
        "manifest": {},
        "profile": {},
        "dataset": {},
        "runIdentity": {},
        "expected": {},
        "actual": {},
        "identity": {},
        "providerAttribution": {},
        "callFacts": {},
        "artifacts": [],
        "rules": [],
    }


def compile_reference(
    repo_root: Path,
    manifest_path: Path,
    profile_path: Path,
    details_paths: list[Path],
    metadata_paths: list[Path],
) -> dict[str, Any]:
    try:
        return _compile_reference(
            repo_root.resolve(),
            manifest_path.resolve(),
            profile_path.resolve(),
            details_paths,
            metadata_paths,
        )
    except (
        ReferenceCompileError,
        runner.ApiError,
        evaluate_quality_gate.GateContractError,
        eval_dataset_contract.DatasetContractError,
        KeyError,
        TypeError,
        ValueError,
        OverflowError,
    ) as exc:
        return _invalid_result(getattr(exc, "code", "input_contract_invalid"))


def _compile_reference(
    repo_root: Path,
    manifest_path: Path,
    profile_path: Path,
    details_paths: list[Path],
    metadata_paths: list[Path],
) -> dict[str, Any]:
    manifest = runner.load_reference_manifest(manifest_path)
    expected_manifest_path = (repo_root / "docs/eval/config/c17-retrieval-reference-v1.json").resolve()
    expected_profile_path = (repo_root / manifest["targetProfile"]["path"]).resolve()
    if manifest_path != expected_manifest_path:
        raise ReferenceCompileError("manifest_path_mismatch")
    if profile_path != expected_profile_path:
        raise ReferenceCompileError("profile_path_mismatch")

    profile_bytes = profile_path.read_bytes()
    profile_sha256 = _sha256_bytes(profile_bytes)
    profile = _read_json(profile_path, "profile_invalid")
    evaluate_quality_gate._validate_profile(profile)
    target = manifest["targetProfile"]
    if profile.get("profileId") != target["profileId"]:
        raise ReferenceCompileError("profile_identity_mismatch")
    allowed_versions = {target["draftVersion"], target["activeVersion"]}
    if profile.get("profileVersion") not in allowed_versions:
        raise ReferenceCompileError("profile_version_mismatch")

    dataset_identity = eval_dataset_contract.validate_versioned_release(
        repo_root,
        Path(manifest["dataset"]["manifestPath"]),
    )
    expected_dataset = manifest["dataset"]
    if (
        dataset_identity.get("manifestSha256") != expected_dataset["manifestSha256"]
        or dataset_identity.get("releaseVersion") != expected_dataset["releaseVersion"]
        or dataset_identity["questionSet"].get("sampleCount") != expected_dataset["expectedSampleCount"]
        or profile.get("dataset") != expected_dataset
        or profile.get("runIdentity") != manifest["execution"]["runIdentity"]
    ):
        raise ReferenceCompileError("tracked_identity_mismatch")

    result = _base_result(manifest, profile, profile_sha256, dataset_identity)
    expected_repeat_count = manifest["full"]["measuredRepeats"]
    if len(details_paths) != len(metadata_paths) or len(details_paths) != expected_repeat_count:
        result["status"] = "INCOMPLETE"
        result["reasonCodes"] = ["repeat_count_incomplete"]
        return result

    dataset_samples = _read_jsonl(repo_root / dataset_identity["questionSet"]["path"])
    expected_ids = [str(sample["id"]) for sample in dataset_samples]
    incomplete: set[str] = set()
    not_comparable: set[str] = set()
    run_entries: dict[int, tuple[dict[str, Any], dict[str, Any]]] = {}
    strict_hashes: list[str] = []
    total_fallbacks = 0
    total_model_calls = 0

    for details_path, metadata_path in zip(details_paths, metadata_paths, strict=True):
        details = _read_json(details_path, "details_invalid")
        metadata = _read_json(metadata_path, "metadata_invalid")
        repeat = metadata.get("repeat") or {}
        run_index = repeat.get("index")
        if isinstance(run_index, bool) or not isinstance(run_index, int):
            raise ReferenceCompileError("repeat_identity_invalid")
        if run_index in run_entries:
            raise ReferenceCompileError("repeat_identity_duplicate")
        run_entries[run_index] = (details, metadata)
        result["artifacts"].append(
            {
                "runIndex": run_index,
                "detailsSha256": _sha256_file(details_path),
                "detailsBytes": details_path.stat().st_size,
                "metadataSha256": _sha256_file(metadata_path),
                "metadataBytes": metadata_path.stat().st_size,
            }
        )

        if not _same_json(details.get("runMetadata"), metadata):
            not_comparable.add("repeat_identity_drift")
        strict_hashes.append(_canonical_hash(_strict_identity(metadata)))

        if (
            repeat.get("total") != expected_repeat_count
            or metadata.get("datasetValidation") != "VALID"
            or metadata.get("datasetReleaseIdentity") != dataset_identity
            or metadata.get("sampleSelection")
            != {"ids": expected_ids, "count": len(expected_ids)}
            or metadata.get("topK") != manifest["execution"]["runIdentity"]["topK"]
            or metadata.get("minScore") != manifest["execution"]["runIdentity"]["minScore"]
            or metadata.get("enableRerank") is not True
        ):
            not_comparable.add("repeat_identity_drift")
        if not _metadata_matches_release(metadata, manifest, dataset_identity):
            not_comparable.add("fixture_document_identity_mismatch")

        evidence_samples = details.get("samples")
        if not isinstance(evidence_samples, list):
            incomplete.add("sample_selection_incomplete")
            evidence_samples = []
        actual_ids = [
            str(sample.get("id"))
            for sample in evidence_samples
            if isinstance(sample, dict)
        ]
        if (
            actual_ids != expected_ids
            or details.get("sampleCount") != len(expected_ids)
            or len(actual_ids) != len(evidence_samples)
        ):
            incomplete.add("sample_selection_incomplete")

        objective_channel = (details.get("metricChannels") or {}).get("objective") or {}
        run_counts = details.get("runCounts") or {}
        if (
            details.get("reportStatus") != "RETRIEVAL_ONLY"
            or objective_channel.get("comparisonSafety") != "RETRIEVAL_ONLY"
            or details.get("datasetValidation") != "VALID"
            or details.get("datasetReleaseIdentity") != dataset_identity
            or details.get("skipAsk") is not True
            or details.get("topK") != manifest["execution"]["runIdentity"]["topK"]
            or details.get("minScore") != manifest["execution"]["runIdentity"]["minScore"]
            or details.get("enableRerank") is not True
            or details.get("claimMetricConfig") != metadata.get("claimMetricConfig")
            or details.get("judgeContractConfig") != metadata.get("judgeContractConfig")
        ):
            not_comparable.add("evidence_contract_mismatch")
        for field, policy_field in (
            ("retrieveErrors", "retrieveErrorsMax"),
            ("rateLimitErrors", "rateLimitErrorsMax"),
            ("retryCount", "retryCountMax"),
        ):
            value = run_counts.get(field)
            if isinstance(value, bool) or not isinstance(value, int):
                incomplete.add("run_count_missing")
            elif value > manifest["errorPolicy"][policy_field]:
                incomplete.add("zero_error_policy_exceeded")

        provider = manifest["providerPolicy"]
        for evidence in evidence_samples:
            if not isinstance(evidence, dict):
                incomplete.add("sample_selection_incomplete")
                continue
            attribution = evidence.get("rerankAttribution") or {}
            fallback_count = attribution.get("fallbackCount")
            model_call_count = attribution.get("modelCallCount")
            if (
                attribution.get("requestedProvider") != provider["requestedProvider"]
                or attribution.get("effectiveProvider") != provider["effectiveProvider"]
                or isinstance(fallback_count, bool)
                or not isinstance(fallback_count, int)
                or fallback_count > provider["fallbackCountMax"]
                or isinstance(model_call_count, bool)
                or not isinstance(model_call_count, int)
                or model_call_count > provider["modelCallCountMax"]
            ):
                not_comparable.add("provider_attribution_mismatch")
            if isinstance(fallback_count, int) and not isinstance(fallback_count, bool):
                total_fallbacks += fallback_count
            if isinstance(model_call_count, int) and not isinstance(model_call_count, bool):
                total_model_calls += model_call_count
            errors = evidence.get("errors") or {}
            if not isinstance(errors, dict) or errors.get("retrieval") is not None:
                incomplete.add("retrieval_observation_failed")

    actual_indexes = sorted(run_entries)
    result["actual"] = {
        "repeatCount": len(run_entries),
        "runIndexes": actual_indexes,
        "samplesPerRepeat": [
            len((run_entries[index][0].get("samples") or []))
            if isinstance(run_entries[index][0].get("samples"), list)
            else 0
            for index in actual_indexes
        ],
        "observationCount": sum(
            len((run_entries[index][0].get("samples") or []))
            if isinstance(run_entries[index][0].get("samples"), list)
            else 0
            for index in actual_indexes
        ),
    }
    result["artifacts"] = sorted(result["artifacts"], key=lambda item: item["runIndex"])
    result["providerAttribution"]["fallbackCount"] = total_fallbacks
    result["providerAttribution"]["modelCallCount"] = total_model_calls
    if actual_indexes != manifest["execution"]["runIndexes"]:
        incomplete.add("repeat_index_incomplete")
    if len(set(strict_hashes)) != 1:
        not_comparable.add("repeat_identity_drift")

    if not_comparable:
        result["providerAttribution"]["status"] = "MISMATCH"
        result["status"] = "NOT_COMPARABLE"
        result["reasonCodes"] = sorted(not_comparable | incomplete)
        return result
    if incomplete:
        result["providerAttribution"]["status"] = "INCOMPLETE"
        result["status"] = "INCOMPLETE"
        result["reasonCodes"] = sorted(incomplete)
        return result

    slices = {item["id"]: item for item in profile["slices"]}
    distributions: list[dict[str, Any]] = []
    for rule in profile["rules"]:
        observations: list[dict[str, Any]] = []
        for run_index in manifest["execution"]["runIndexes"]:
            details = run_entries[run_index][0]
            annotated = list(zip(dataset_samples, details["samples"], strict=True))
            observed = evaluate_quality_gate.calculate_rule_observation(
                rule,
                slices[rule["slice"]],
                annotated,
            )
            if observed["status"] != "COMPLETE" or not _finite_number(observed.get("observed")):
                incomplete.add("rule_observation_incomplete")
                break
            observations.append(
                {
                    "runIndex": run_index,
                    "denominator": observed["denominator"],
                    "observed": round(float(observed["observed"]), DISTRIBUTION_DECIMAL_PLACES),
                }
            )
        if incomplete:
            break
        values = [item["observed"] for item in observations]
        summary = summarize_observations(values)
        distributions.append(
            {
                "id": rule["id"],
                "channel": rule["channel"],
                "slice": rule["slice"],
                "metric": rule["metric"],
                "operator": rule["operator"],
                "runs": observations,
                **summary,
            }
        )
    if incomplete:
        result["status"] = "INCOMPLETE"
        result["reasonCodes"] = sorted(incomplete)
        return result

    result["status"] = "COMPLETE"
    result["reasonCodes"] = []
    result["rules"] = distributions
    result["providerAttribution"]["status"] = "MATCHED"
    result["identity"] = {
        "strictIdentitySha256": strict_hashes[0],
        "gitHead": (run_entries[actual_indexes[0]][1].get("git") or {}).get("head"),
    }
    if profile["status"] == "DRAFT":
        result["activationStatus"] = "PENDING_THRESHOLD_APPROVAL"
        return result

    locked_reference = build_locked_reference(profile, profile_sha256, distributions)
    reference_by_id = {rule["id"]: rule for rule in locked_reference["rules"]}
    for run_index in manifest["execution"]["runIndexes"]:
        annotated = list(zip(dataset_samples, run_entries[run_index][0]["samples"], strict=True))
        replay = [
            evaluate_quality_gate._evaluate_rule(
                rule,
                slices[rule["slice"]],
                annotated,
                reference_by_id[rule["id"]],
            )
            for rule in profile["rules"]
        ]
        if any(item["required"] and item["result"] != "PASS" for item in replay):
            result["status"] = "INCOMPLETE"
            result["activationStatus"] = "BLOCKED"
            result["reasonCodes"] = ["active_reference_replay_failed"]
            result["rules"] = []
            return result
    result["activationStatus"] = "ACTIVE_REFERENCE_LOCKED"
    result["lockedReference"] = locked_reference
    return result


def build_locked_reference(
    profile: dict[str, Any],
    profile_sha256: str,
    distributions: list[dict[str, Any]],
) -> dict[str, Any]:
    evaluate_quality_gate._validate_profile(profile)
    if (
        profile.get("profileVersion") != "v1"
        or profile.get("status") != "ACTIVE"
        or profile.get("thresholdStatus") != "APPROVED"
        or not isinstance(profile_sha256, str)
        or len(profile_sha256) != 64
    ):
        raise ReferenceCompileError("active_profile_binding_invalid")
    expected_rules = {str(rule["id"]): rule for rule in profile["rules"]}
    actual_rules = {str(rule.get("id")): rule for rule in distributions}
    if set(expected_rules) != set(actual_rules) or len(actual_rules) != len(distributions):
        raise ReferenceCompileError("active_rule_binding_invalid")

    locked_rules: list[dict[str, Any]] = []
    for rule_id, profile_rule in expected_rules.items():
        distribution = actual_rules[rule_id]
        if not _finite_number(profile_rule.get("target")) or not _finite_number(
            profile_rule.get("maxAbsoluteRegression")
        ):
            raise ReferenceCompileError("active_threshold_approval_incomplete")
        if any(
            distribution.get(field) != profile_rule.get(field)
            for field in ("channel", "slice", "metric", "operator")
        ) or not _finite_number(distribution.get("median")):
            raise ReferenceCompileError("active_rule_binding_invalid")
        locked_rules.append(
            {
                "id": rule_id,
                "channel": profile_rule["channel"],
                "slice": profile_rule["slice"],
                "metric": profile_rule["metric"],
                "operator": profile_rule["operator"],
                "observed": float(distribution["median"]),
            }
        )
    return {
        "schemaVersion": LOCKED_REFERENCE_SCHEMA,
        "profile": {
            "id": profile["profileId"],
            "version": profile["profileVersion"],
            "sha256": profile_sha256,
        },
        "dataset": {
            "releaseVersion": profile["dataset"]["releaseVersion"],
            "manifestSha256": profile["dataset"]["manifestSha256"],
        },
        "runIdentity": dict(profile["runIdentity"]),
        "rules": locked_rules,
    }


def _write_json(path: Path, value: dict[str, Any], no_overwrite: bool) -> None:
    if no_overwrite and path.exists():
        raise ReferenceCompileError("output_exists")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compile three C17 retrieval reference repeats locally.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[1]))
    parser.add_argument(
        "--manifest",
        default="docs/eval/config/c17-retrieval-reference-v1.json",
    )
    parser.add_argument(
        "--profile",
        default="docs/eval/gates/rag-eval-dev-v2-retrieval-regression-v1.json",
    )
    parser.add_argument("--details", action="append", required=True)
    parser.add_argument("--metadata", action="append", required=True)
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    repo_root = Path(args.repo_root).resolve()
    manifest_path = Path(args.manifest)
    profile_path = Path(args.profile)
    if not manifest_path.is_absolute():
        manifest_path = repo_root / manifest_path
    if not profile_path.is_absolute():
        profile_path = repo_root / profile_path
    result = compile_reference(
        repo_root,
        manifest_path,
        profile_path,
        [Path(value) for value in args.details],
        [Path(value) for value in args.metadata],
    )
    try:
        _write_json(Path(args.output_json), result, args.no_overwrite)
    except ReferenceCompileError as exc:
        print(exc.code, file=sys.stderr)
        return 2
    return {"COMPLETE": 0, "INVALID": 2, "NOT_COMPARABLE": 3, "INCOMPLETE": 4}[result["status"]]


if __name__ == "__main__":
    raise SystemExit(main())
