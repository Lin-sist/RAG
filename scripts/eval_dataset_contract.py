#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
from collections import Counter
from pathlib import Path, PurePosixPath
from typing import Any


MANIFEST_SCHEMA_VERSION = "rag-eval-dataset-manifest-v1"


class DatasetContractError(ValueError):
    def __init__(self, code: str, artifact: str, detail: str) -> None:
        self.code = code
        self.artifact = artifact
        self.detail = detail
        super().__init__(f"{code}: artifact={artifact} detail={detail}")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _safe_repo_path(repo_root: Path, value: Any, artifact: str) -> tuple[str, Path]:
    if not isinstance(value, str) or not value.strip():
        raise DatasetContractError("manifest_invalid", artifact, "path must be a non-empty string")
    normalized = value.replace("\\", "/")
    pure = PurePosixPath(normalized)
    if (
        pure.is_absolute()
        or re.match(r"^[A-Za-z]:/", normalized)
        or ".." in pure.parts
        or "." in pure.parts
    ):
        raise DatasetContractError("unsafe_artifact_path", artifact, "path must be repo-relative")
    root = repo_root.resolve()
    resolved = (root / Path(*pure.parts)).resolve()
    try:
        resolved.relative_to(root)
    except ValueError as exc:
        raise DatasetContractError(
            "unsafe_artifact_path",
            artifact,
            "path resolves outside repo root",
        ) from exc
    return pure.as_posix(), resolved


def _load_json_object(path: Path, artifact: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise DatasetContractError("artifact_missing", artifact, "file does not exist") from exc
    except json.JSONDecodeError as exc:
        raise DatasetContractError("manifest_invalid", artifact, "file is not valid JSON") from exc
    if not isinstance(value, dict):
        raise DatasetContractError("manifest_invalid", artifact, "JSON root must be an object")
    return value


def _read_jsonl_objects(path: Path, artifact: str) -> list[dict[str, Any]]:
    samples: list[dict[str, Any]] = []
    try:
        with path.open("r", encoding="utf-8") as file:
            for line_number, line in enumerate(file, start=1):
                if not line.strip():
                    continue
                try:
                    item = json.loads(line)
                except json.JSONDecodeError as exc:
                    raise DatasetContractError(
                        "sample_invalid_json",
                        artifact,
                        f"line={line_number}",
                    ) from exc
                if not isinstance(item, dict):
                    raise DatasetContractError(
                        "sample_not_object",
                        artifact,
                        f"line={line_number}",
                    )
                samples.append(item)
    except FileNotFoundError as exc:
        raise DatasetContractError("artifact_missing", artifact, "file does not exist") from exc
    return samples


def _require_string(value: Any, field: str, artifact: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise DatasetContractError("manifest_invalid", artifact, f"field={field}")
    return value


def _validate_file_identity(
    repo_root: Path,
    descriptor: Any,
    descriptor_name: str,
) -> tuple[dict[str, Any], Path]:
    if not isinstance(descriptor, dict):
        raise DatasetContractError("manifest_invalid", descriptor_name, "descriptor must be an object")
    relative, resolved = _safe_repo_path(repo_root, descriptor.get("path"), descriptor_name)
    if not resolved.is_file():
        raise DatasetContractError("artifact_missing", relative, "file does not exist")
    expected_hash = _require_string(descriptor.get("sha256"), "sha256", descriptor_name).lower()
    expected_bytes = descriptor.get("bytes")
    if not isinstance(expected_bytes, int) or isinstance(expected_bytes, bool) or expected_bytes < 0:
        raise DatasetContractError("manifest_invalid", descriptor_name, "field=bytes")
    actual_hash = sha256_file(resolved)
    actual_bytes = resolved.stat().st_size
    if actual_hash != expected_hash or actual_bytes != expected_bytes:
        raise DatasetContractError("artifact_hash_mismatch", relative, "bytes or sha256 drifted")
    return {
        "path": relative,
        "sha256": actual_hash,
        "bytes": actual_bytes,
    }, resolved


def _ordered_sample_ids_sha256(samples: list[dict[str, Any]]) -> str:
    ordered_ids = [str(sample.get("id", "")) for sample in samples]
    canonical = json.dumps(ordered_ids, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(canonical).hexdigest()


def _validate_allowed_sample_fields(
    samples: list[dict[str, Any]],
    schema: dict[str, Any],
    artifact: str,
    fixture_names: set[str],
) -> None:
    allowed_value = schema.get("allowedFields")
    required_value = schema.get("requiredFields")
    if not isinstance(allowed_value, list) or not all(isinstance(item, str) for item in allowed_value):
        raise DatasetContractError("manifest_invalid", artifact, "schema allowedFields invalid")
    if not isinstance(required_value, list) or not all(isinstance(item, str) for item in required_value):
        raise DatasetContractError("manifest_invalid", artifact, "schema requiredFields invalid")
    allowed = set(allowed_value)
    required = set(required_value)
    field_types = schema.get("fieldTypes")
    if not isinstance(field_types, dict):
        raise DatasetContractError("manifest_invalid", artifact, "schema fieldTypes invalid")
    enums = schema.get("enums")
    if not isinstance(enums, dict):
        raise DatasetContractError("manifest_invalid", artifact, "schema enums invalid")
    context_fields_value = schema.get("contextFields")
    if not isinstance(context_fields_value, list) or not all(
        isinstance(item, str) for item in context_fields_value
    ):
        raise DatasetContractError("manifest_invalid", artifact, "schema contextFields invalid")
    context_fields = set(context_fields_value)
    id_pattern_value = schema.get("idPattern")
    if not isinstance(id_pattern_value, str) or not id_pattern_value:
        raise DatasetContractError("manifest_invalid", artifact, "schema idPattern invalid")
    try:
        id_pattern = re.compile(id_pattern_value)
    except re.error as exc:
        raise DatasetContractError("manifest_invalid", artifact, "schema idPattern invalid") from exc
    seen_ids: set[str] = set()
    for line_number, sample in enumerate(samples, start=1):
        missing = sorted(required - set(sample))
        if missing:
            sample_id = sample.get("id") if isinstance(sample.get("id"), str) else "unknown"
            raise DatasetContractError(
                "missing_field",
                artifact,
                f"line={line_number} sample={sample_id} field={missing[0]}",
            )
        unknown = sorted(set(sample) - allowed)
        if unknown:
            sample_id = sample.get("id") if isinstance(sample.get("id"), str) else "unknown"
            raise DatasetContractError(
                "unknown_field",
                artifact,
                f"line={line_number} sample={sample_id} field={unknown[0]}",
            )
        for field, type_name in field_types.items():
            value = sample.get(field)
            valid = False
            if type_name == "nonEmptyString":
                valid = isinstance(value, str) and bool(value.strip())
            elif type_name == "boolean":
                valid = isinstance(value, bool)
            elif type_name == "stringArray":
                valid = isinstance(value, list) and all(
                    isinstance(item, str) and bool(item.strip()) for item in value
                )
            elif type_name == "contextArray":
                valid = isinstance(value, list) and all(
                    isinstance(item, dict)
                    and set(item) == context_fields
                    and all(isinstance(item[name], str) and bool(item[name].strip()) for name in context_fields)
                    for item in value
                )
            else:
                raise DatasetContractError(
                    "manifest_invalid",
                    artifact,
                    f"schema type unsupported field={field}",
                )
            if not valid:
                sample_id = sample.get("id") if isinstance(sample.get("id"), str) else "unknown"
                raise DatasetContractError(
                    "invalid_field_type",
                    artifact,
                    f"line={line_number} sample={sample_id} field={field}",
                )
        for field, allowed_values in enums.items():
            if not isinstance(allowed_values, list) or sample.get(field) not in allowed_values:
                sample_id = sample.get("id") if isinstance(sample.get("id"), str) else "unknown"
                raise DatasetContractError(
                    "invalid_enum",
                    artifact,
                    f"line={line_number} sample={sample_id} field={field}",
                )
        sample_id = str(sample.get("id", ""))
        if id_pattern.fullmatch(sample_id) is None:
            raise DatasetContractError(
                "invalid_id_pattern",
                artifact,
                f"line={line_number} sample={sample_id} field=id",
            )
        if sample_id in seen_ids:
            raise DatasetContractError(
                "duplicate_sample_id",
                artifact,
                f"line={line_number} sample={sample_id}",
            )
        seen_ids.add(sample_id)

        rules = schema.get("conditionalRules")
        if not isinstance(rules, dict):
            raise DatasetContractError("manifest_invalid", artifact, "schema conditionalRules invalid")
        answerable = rules.get("answerable")
        no_answer = rules.get("noAnswer")
        if not isinstance(answerable, dict) or not isinstance(no_answer, dict):
            raise DatasetContractError("manifest_invalid", artifact, "schema conditional rule invalid")
        should_answer = sample.get("should_answer")
        if should_answer is True:
            if sample.get("type") == no_answer.get("type"):
                raise DatasetContractError(
                    "answerability_conflict",
                    artifact,
                    f"line={line_number} sample={sample_id} field=type",
                )
            required_non_empty = answerable.get("nonEmptyFields")
            if not isinstance(required_non_empty, list):
                raise DatasetContractError("manifest_invalid", artifact, "answerable fields invalid")
            for field in required_non_empty:
                if not sample.get(field):
                    raise DatasetContractError(
                        "answerability_conflict",
                        artifact,
                        f"line={line_number} sample={sample_id} field={field}",
                    )
            referenced_sources = list(sample.get("expected_sources") or [])
            referenced_sources.extend(
                context.get("source")
                for context in (sample.get("expected_contexts") or [])
                if isinstance(context, dict)
            )
            for source in referenced_sources:
                if source not in fixture_names:
                    raise DatasetContractError(
                        "unknown_fixture_source",
                        artifact,
                        f"line={line_number} sample={sample_id} field=source",
                    )
        else:
            if sample.get("type") != no_answer.get("type"):
                raise DatasetContractError(
                    "answerability_conflict",
                    artifact,
                    f"line={line_number} sample={sample_id} field=type",
                )
            empty_fields = no_answer.get("emptyFields")
            non_empty_fields = no_answer.get("nonEmptyFields")
            if not isinstance(empty_fields, list) or not isinstance(non_empty_fields, list):
                raise DatasetContractError("manifest_invalid", artifact, "no-answer fields invalid")
            for field in empty_fields:
                if sample.get(field):
                    raise DatasetContractError(
                        "answerability_conflict",
                        artifact,
                        f"line={line_number} sample={sample_id} field={field}",
                    )
            for field in non_empty_fields:
                if not sample.get(field):
                    raise DatasetContractError(
                        "answerability_conflict",
                        artifact,
                        f"line={line_number} sample={sample_id} field={field}",
                    )


def _validate_distribution(
    samples: list[dict[str, Any]],
    distribution: Any,
    artifact: str,
) -> dict[str, dict[str, int]]:
    if not isinstance(distribution, dict):
        raise DatasetContractError("manifest_invalid", artifact, "distribution must be an object")
    actual = {
        "type": dict(sorted(Counter(str(sample.get("type")) for sample in samples).items())),
        "difficulty": dict(
            sorted(Counter(str(sample.get("difficulty")) for sample in samples).items())
        ),
        "shouldAnswer": {
            "false": sum(sample.get("should_answer") is False for sample in samples),
            "true": sum(sample.get("should_answer") is True for sample in samples),
        },
    }
    if distribution != actual:
        raise DatasetContractError(
            "distribution_mismatch",
            artifact,
            "declared distribution does not match samples",
        )
    return actual


def validate_versioned_release(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    manifest_relative, manifest_resolved = _safe_repo_path(
        repo_root,
        manifest_path.as_posix(),
        "dataset-manifest",
    )
    manifest = _load_json_object(manifest_resolved, manifest_relative)
    if manifest.get("manifestSchemaVersion") != MANIFEST_SCHEMA_VERSION:
        raise DatasetContractError(
            "manifest_invalid",
            manifest_relative,
            "manifestSchemaVersion is unsupported",
        )

    version_fields = (
        "releaseVersion",
        "questionSetVersion",
        "sampleSchemaVersion",
        "annotationVersion",
        "fixtureCorpusVersion",
    )
    versions = {
        field: _require_string(manifest.get(field), field, manifest_relative)
        for field in version_fields
    }

    question_identity, question_path = _validate_file_identity(
        repo_root,
        manifest.get("questionSet"),
        "questionSet",
    )
    samples = _read_jsonl_objects(question_path, question_identity["path"])
    question_descriptor = manifest["questionSet"]
    expected_count = question_descriptor.get("sampleCount")
    expected_order_hash = _require_string(
        question_descriptor.get("orderedSampleIdsSha256"),
        "orderedSampleIdsSha256",
        "questionSet",
    ).lower()
    actual_order_hash = _ordered_sample_ids_sha256(samples)
    if expected_count != len(samples) or expected_order_hash != actual_order_hash:
        raise DatasetContractError(
            "release_identity_mismatch",
            question_identity["path"],
            "sample count or order identity drifted",
        )
    question_identity.update({
        "sampleCount": len(samples),
        "orderedSampleIdsSha256": actual_order_hash,
    })

    schema_identity, schema_path = _validate_file_identity(
        repo_root,
        manifest.get("sampleSchema"),
        "sampleSchema",
    )
    schema = _load_json_object(schema_path, schema_identity["path"])
    if (
        schema.get("contractKind") != "rag-eval-sample-schema"
        or schema.get("contractFormatVersion") != "1"
        or schema.get("unknownFields") != "reject"
    ):
        raise DatasetContractError(
            "manifest_invalid",
            schema_identity["path"],
            "sample schema contract header invalid",
        )
    if schema.get("sampleSchemaVersion") != versions["sampleSchemaVersion"]:
        raise DatasetContractError(
            "release_identity_mismatch",
            schema_identity["path"],
            "sample schema version drifted",
        )
    fixtures_value = manifest.get("fixtures")
    if not isinstance(fixtures_value, list) or not fixtures_value:
        raise DatasetContractError("manifest_invalid", manifest_relative, "fixtures must be non-empty")
    fixtures = [
        _validate_file_identity(repo_root, descriptor, f"fixtures[{index}]")[0]
        for index, descriptor in enumerate(fixtures_value)
    ]
    fixture_names = {PurePosixPath(item["path"]).name for item in fixtures}
    _validate_allowed_sample_fields(
        samples,
        schema,
        question_identity["path"],
        fixture_names,
    )
    distribution = _validate_distribution(
        samples,
        manifest.get("distribution"),
        manifest_relative,
    )

    logical_kb = manifest.get("logicalKnowledgeBase")
    if not isinstance(logical_kb, dict):
        raise DatasetContractError(
            "manifest_invalid",
            manifest_relative,
            "logicalKnowledgeBase must be an object",
        )
    _require_string(logical_kb.get("name"), "logicalKnowledgeBase.name", manifest_relative)
    _require_string(logical_kb.get("marker"), "logicalKnowledgeBase.marker", manifest_relative)
    expected_names = logical_kb.get("expectedDocumentNames")
    if (
        not isinstance(expected_names, list)
        or not expected_names
        or not all(isinstance(item, str) and item.strip() for item in expected_names)
        or set(expected_names) != fixture_names
    ):
        raise DatasetContractError(
            "manifest_invalid",
            manifest_relative,
            "logicalKnowledgeBase.expectedDocumentNames mismatch",
        )

    return {
        "validationStatus": "VALID",
        "manifestSchemaVersion": MANIFEST_SCHEMA_VERSION,
        "manifestPath": manifest_relative,
        "manifestSha256": sha256_file(manifest_resolved),
        **versions,
        "questionSet": question_identity,
        "sampleSchema": schema_identity,
        "fixtures": fixtures,
        "logicalKnowledgeBase": logical_kb,
        "distribution": distribution,
    }


def validate_unversioned_eval_set(path: Path) -> dict[str, Any]:
    artifact = path.name or "custom-eval-set"
    samples = _read_jsonl_objects(path, artifact)
    seen_ids: set[str] = set()
    for line_number, sample in enumerate(samples, start=1):
        sample_id = sample.get("id")
        if not isinstance(sample_id, str) or not sample_id.strip():
            raise DatasetContractError("missing_field", artifact, f"line={line_number} field=id")
        if sample_id in seen_ids:
            raise DatasetContractError(
                "duplicate_sample_id",
                artifact,
                f"line={line_number} sample={sample_id}",
            )
        seen_ids.add(sample_id)
    return {
        "validationStatus": "UNVERSIONED",
        "releaseVersion": None,
        "questionSet": {
            "path": artifact,
            "sha256": sha256_file(path),
            "bytes": path.stat().st_size,
            "sampleCount": len(samples),
            "orderedSampleIdsSha256": _ordered_sample_ids_sha256(samples),
        },
        "fixtures": [],
    }


def ensure_release_version_consistent(
    candidate: dict[str, Any],
    tracked: dict[str, Any],
) -> None:
    if (
        candidate.get("releaseVersion") == tracked.get("releaseVersion")
        and candidate.get("manifestSha256") != tracked.get("manifestSha256")
    ):
        raise DatasetContractError(
            "release_identity_mismatch",
            str(candidate.get("manifestPath") or "dataset-manifest"),
            "releaseVersion already has a different tracked manifest identity",
        )
