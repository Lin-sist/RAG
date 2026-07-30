#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path, PurePosixPath
from typing import Any


MANIFEST_SCHEMA_VERSION = "bounded-query-router-eval-manifest-v1"
RELEASE_VERSION = "bounded-query-router-eval-v1"
EXPECTATION_SCHEMA_VERSION = "bounded-query-router-expectation-v1"
EVALUATOR_VERSION = "bounded-query-router-evaluator-v1"
CLASSIFIER_VERSION = "fact-intent-v1"
STRATEGY_VERSION = "fact-v1"
POLICY_VERSION = "evidence-no-answer-v1"
BUDGET_PROFILE_ID = "fact-v1-default-budget"
ALLOWED_INTENTS = {"FACT", "UNSUPPORTED", "INVALID"}
ALLOWED_REQUIRED_STATUSES = {"REQUIRED", "OPTIONAL"}
REQUIRED_CHANNELS = [
    "classification",
    "strategy_execution",
    "budget",
    "retrieval",
    "generation_citation",
    "no_answer",
    "errors",
]
EXPECTATION_FIELDS = {"sampleId", "expectedIntent", "requiredStatus"}
STATUS_EXIT_CODES = {"PASS": 0, "INVALID": 2, "FAIL": 3, "NOT_EVALUABLE": 4}
EXPECTED_BUDGET = {
    "schemaVersion": "bounded-query-router-budget-v1",
    "profileId": BUDGET_PROFILE_ID,
    "maxQueryVariants": 8,
    "maxRetrievalPasses": 1,
    "maxRerankCalls": 1,
    "maxGenerationCalls": 1,
    "maxContextTokens": 1200,
    "maxOutputTokens": 2048,
    "deadlineMillis": 120000,
}


class RouterEvalContractError(ValueError):
    def __init__(self, code: str, artifact: str, detail: str) -> None:
        self.code = code
        self.artifact = artifact
        self.detail = detail
        super().__init__(f"{code}: artifact={artifact} detail={detail}")


def ordered_ids_hash(ids: list[str]) -> str:
    raw = json.dumps(ids, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def _load_object(path: Path, artifact: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise RouterEvalContractError("artifact_missing", artifact, "file does not exist") from exc
    except json.JSONDecodeError as exc:
        raise RouterEvalContractError("artifact_invalid_json", artifact, "invalid JSON") from exc
    if not isinstance(value, dict):
        raise RouterEvalContractError("artifact_not_object", artifact, "JSON root must be object")
    return value


def _read_jsonl(path: Path, artifact: str) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except FileNotFoundError as exc:
        raise RouterEvalContractError("artifact_missing", artifact, "file does not exist") from exc
    for line_number, line in enumerate(lines, start=1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as exc:
            raise RouterEvalContractError(
                "artifact_invalid_json", f"{artifact}:{line_number}", "invalid JSONL row"
            ) from exc
        if not isinstance(row, dict):
            raise RouterEvalContractError(
                "artifact_not_object", f"{artifact}:{line_number}", "JSONL row must be object"
            )
        rows.append(row)
    return rows


def _safe_artifact(root: Path, descriptor: Any, name: str) -> Path:
    if not isinstance(descriptor, dict) or set(descriptor) != {"path", "bytes", "sha256"}:
        raise RouterEvalContractError("manifest_invalid", name, "invalid artifact descriptor")
    value = descriptor.get("path")
    if not isinstance(value, str) or not value:
        raise RouterEvalContractError("unsafe_artifact_path", name, "path must be repo-relative")
    normalized = value.replace("\\", "/")
    pure = PurePosixPath(normalized)
    if pure.is_absolute() or re.match(r"^[A-Za-z]:/", normalized) or ".." in pure.parts:
        raise RouterEvalContractError("unsafe_artifact_path", name, "path must be repo-relative")
    path = (root / Path(*pure.parts)).resolve()
    try:
        path.relative_to(root)
    except ValueError as exc:
        raise RouterEvalContractError("unsafe_artifact_path", name, "path escapes repository") from exc
    try:
        raw = path.read_bytes()
    except FileNotFoundError as exc:
        raise RouterEvalContractError("artifact_missing", normalized, "file does not exist") from exc
    if (
        not isinstance(descriptor.get("bytes"), int)
        or isinstance(descriptor.get("bytes"), bool)
        or descriptor["bytes"] != len(raw)
        or descriptor.get("sha256") != hashlib.sha256(raw).hexdigest()
    ):
        raise RouterEvalContractError("artifact_hash_mismatch", normalized, "bytes or sha256 drifted")
    return path


def _validate_manifest_identity(manifest: dict[str, Any], artifact: str) -> None:
    identities = {
        "manifestSchemaVersion": MANIFEST_SCHEMA_VERSION,
        "releaseVersion": RELEASE_VERSION,
        "expectationSchemaVersion": EXPECTATION_SCHEMA_VERSION,
        "evaluatorVersion": EVALUATOR_VERSION,
        "classifierVersion": CLASSIFIER_VERSION,
        "strategyVersion": STRATEGY_VERSION,
        "policyVersion": POLICY_VERSION,
        "budgetProfileId": BUDGET_PROFILE_ID,
    }
    for field, expected in identities.items():
        if manifest.get(field) != expected:
            raise RouterEvalContractError("release_identity_mismatch", artifact, field)
    if manifest.get("requiredChannels") != REQUIRED_CHANNELS:
        raise RouterEvalContractError("channel_contract_mismatch", artifact, "requiredChannels")
    if manifest.get("statusExitCodes") != STATUS_EXIT_CODES:
        raise RouterEvalContractError("status_contract_mismatch", artifact, "statusExitCodes")
    boundary = manifest.get("executionBoundary")
    if boundary != {
        "providerCallCount": 0,
        "businessDataOutbound": False,
        "liveEvaluationStatus": "SKIPPED",
    }:
        raise RouterEvalContractError("execution_boundary_mismatch", artifact, "executionBoundary")


def _validate_expectation_schema(schema: dict[str, Any], artifact: str) -> None:
    if schema.get("$id") != EXPECTATION_SCHEMA_VERSION:
        raise RouterEvalContractError("expectation_schema_mismatch", artifact, "$id")
    if schema.get("type") != "object" or schema.get("additionalProperties") is not False:
        raise RouterEvalContractError("expectation_schema_mismatch", artifact, "object boundary")
    if set(schema.get("required", [])) != EXPECTATION_FIELDS:
        raise RouterEvalContractError("expectation_schema_mismatch", artifact, "required")
    properties = schema.get("properties")
    if not isinstance(properties, dict) or set(properties) != EXPECTATION_FIELDS:
        raise RouterEvalContractError("expectation_schema_mismatch", artifact, "properties")
    if set(properties.get("expectedIntent", {}).get("enum", [])) != ALLOWED_INTENTS:
        raise RouterEvalContractError("expectation_schema_mismatch", artifact, "expectedIntent")
    if set(properties.get("requiredStatus", {}).get("enum", [])) != ALLOWED_REQUIRED_STATUSES:
        raise RouterEvalContractError("expectation_schema_mismatch", artifact, "requiredStatus")


def validate_release(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    root = repo_root.resolve()
    manifest_file = (root / manifest_path).resolve()
    try:
        manifest_file.relative_to(root)
    except ValueError as exc:
        raise RouterEvalContractError("unsafe_artifact_path", manifest_path.as_posix(), "manifest") from exc
    manifest = _load_object(manifest_file, manifest_path.as_posix())
    _validate_manifest_identity(manifest, manifest_path.as_posix())

    artifacts = manifest.get("artifacts")
    if not isinstance(artifacts, dict) or set(artifacts) != {
        "dataset", "expectationSchema", "expectations", "budget"
    }:
        raise RouterEvalContractError("manifest_invalid", manifest_path.as_posix(), "artifacts")
    dataset_path = _safe_artifact(root, artifacts["dataset"], "dataset")
    schema_path = _safe_artifact(root, artifacts["expectationSchema"], "expectationSchema")
    expectations_path = _safe_artifact(root, artifacts["expectations"], "expectations")
    budget_path = _safe_artifact(root, artifacts["budget"], "budget")

    dataset = _read_jsonl(dataset_path, artifacts["dataset"]["path"])
    dataset_ids: list[str] = []
    for index, row in enumerate(dataset, start=1):
        sample_id = row.get("id")
        if not isinstance(sample_id, str) or not sample_id:
            raise RouterEvalContractError("dataset_sample_id_invalid", f"dataset:{index}", "id")
        dataset_ids.append(sample_id)
    if len(set(dataset_ids)) != len(dataset_ids):
        raise RouterEvalContractError("dataset_duplicate_id", "dataset", "IDs must be unique")
    if manifest.get("datasetSampleCount") != len(dataset_ids):
        raise RouterEvalContractError("dataset_count_mismatch", "dataset", "datasetSampleCount")
    if manifest.get("datasetOrderedIdsSha256") != ordered_ids_hash(dataset_ids):
        raise RouterEvalContractError("dataset_order_mismatch", "dataset", "ordered IDs")

    schema = _load_object(schema_path, artifacts["expectationSchema"]["path"])
    _validate_expectation_schema(schema, artifacts["expectationSchema"]["path"])
    budget = _load_object(budget_path, artifacts["budget"]["path"])
    if budget != EXPECTED_BUDGET:
        raise RouterEvalContractError("budget_profile_mismatch", artifacts["budget"]["path"], "budget")

    expectations = _read_jsonl(expectations_path, artifacts["expectations"]["path"])
    selection_ids: list[str] = []
    for index, row in enumerate(expectations, start=1):
        artifact = f"{artifacts['expectations']['path']}:{index}"
        if set(row) != EXPECTATION_FIELDS:
            raise RouterEvalContractError("expectation_fields_invalid", artifact, "fields")
        sample_id = row.get("sampleId")
        if not isinstance(sample_id, str) or not sample_id:
            raise RouterEvalContractError("sample_id_invalid", artifact, "sampleId")
        selection_ids.append(sample_id)
        if row.get("expectedIntent") not in ALLOWED_INTENTS:
            raise RouterEvalContractError("unknown_expected_intent", artifact, "expectedIntent")
        if row.get("requiredStatus") not in ALLOWED_REQUIRED_STATUSES:
            raise RouterEvalContractError("unknown_required_status", artifact, "requiredStatus")
    if len(set(selection_ids)) != len(selection_ids):
        raise RouterEvalContractError("duplicate_sample_id", "expectations", "IDs must be unique")
    dataset_set = set(dataset_ids)
    unexpected = [sample_id for sample_id in selection_ids if sample_id not in dataset_set]
    if unexpected:
        raise RouterEvalContractError("unexpected_sample_id", "expectations", unexpected[0])
    if manifest.get("selectionCount") != len(selection_ids):
        raise RouterEvalContractError("selection_count_mismatch", "expectations", "selectionCount")
    if manifest.get("selectionOrderedIdsSha256") != ordered_ids_hash(selection_ids):
        raise RouterEvalContractError("selection_order_mismatch", "expectations", "ordered IDs")

    distribution = {
        "expectedIntent": {
            intent: sum(1 for row in expectations if row["expectedIntent"] == intent)
            for intent in ("FACT", "UNSUPPORTED", "INVALID")
        },
        "requiredStatus": {
            status: sum(1 for row in expectations if row["requiredStatus"] == status)
            for status in ("REQUIRED", "OPTIONAL")
        },
    }
    if manifest.get("distribution") != distribution:
        raise RouterEvalContractError("distribution_mismatch", "expectations", "distribution")

    return {
        "manifestSchemaVersion": MANIFEST_SCHEMA_VERSION,
        "releaseVersion": RELEASE_VERSION,
        "evaluatorVersion": EVALUATOR_VERSION,
        "classifierVersion": CLASSIFIER_VERSION,
        "strategyVersion": STRATEGY_VERSION,
        "policyVersion": POLICY_VERSION,
        "budgetProfileId": BUDGET_PROFILE_ID,
        "manifestPath": manifest_path.as_posix(),
        "manifestSha256": hashlib.sha256(manifest_file.read_bytes()).hexdigest(),
        "datasetSampleCount": len(dataset_ids),
        "selectionCount": len(selection_ids),
        "orderedSampleIds": selection_ids,
        "distribution": distribution,
        "requiredChannels": list(REQUIRED_CHANNELS),
        "providerCallCount": 0,
        "businessDataOutbound": False,
        "liveEvaluationStatus": "SKIPPED",
    }


def build_plan(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    identity = validate_release(repo_root, manifest_path)
    return {
        "status": "VALID",
        "releaseVersion": identity["releaseVersion"],
        "manifestSha256": identity["manifestSha256"],
        "classifierVersion": identity["classifierVersion"],
        "strategyVersion": identity["strategyVersion"],
        "policyVersion": identity["policyVersion"],
        "budgetProfileId": identity["budgetProfileId"],
        "selectionCount": identity["selectionCount"],
        "distribution": identity["distribution"],
        "requiredChannels": identity["requiredChannels"],
        "providerCallCount": 0,
        "businessDataOutbound": False,
        "liveEvaluationStatus": "SKIPPED",
        "executionStarted": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate the frozen C16 router evaluation release.")
    parser.add_argument(
        "--manifest",
        default="docs/eval/router/bounded-query-router-eval-v1-manifest.json",
    )
    parser.add_argument("--plan-only", action="store_true")
    args = parser.parse_args()
    if not args.plan_only:
        print("validation requires --plan-only", file=sys.stderr)
        return 2
    repo_root = Path(__file__).resolve().parents[1]
    try:
        plan = build_plan(repo_root, Path(args.manifest))
    except RouterEvalContractError as exc:
        print(json.dumps({"status": "INVALID", "error": exc.code}, separators=(",", ":")))
        return 2
    print(json.dumps(plan, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
