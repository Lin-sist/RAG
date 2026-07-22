#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import unicodedata
from collections import Counter
from pathlib import Path, PurePosixPath
from typing import Any


MANIFEST_SCHEMA_VERSION_V1 = "rag-eval-dataset-manifest-v1"
MANIFEST_SCHEMA_VERSION_V2 = "rag-eval-dataset-manifest-v2"
SUPPORTED_MANIFEST_SCHEMA_VERSIONS = {
    MANIFEST_SCHEMA_VERSION_V1,
    MANIFEST_SCHEMA_VERSION_V2,
}
APPROVED_EXPANDED_RELEASES = {
    "rag-eval-dev-v2": {
        "totalSampleCount": 150,
        "seedSampleCount": 30,
        "newSampleCount": 120,
        "quotas": {
            "type": {
                "definition": 30,
                "fact": 35,
                "multi_hop": 25,
                "no_answer": 20,
                "reasoning": 40,
            },
            "difficulty": {"easy": 50, "hard": 35, "medium": 65},
            "shouldAnswer": {"false": 20, "true": 130},
            "typeDifficulty": {
                "definition": {"easy": 14, "hard": 4, "medium": 12},
                "fact": {"easy": 18, "hard": 4, "medium": 13},
                "multi_hop": {"easy": 3, "hard": 12, "medium": 10},
                "no_answer": {"easy": 7, "hard": 5, "medium": 8},
                "reasoning": {"easy": 8, "hard": 10, "medium": 22},
            },
        },
        "fixtureCoverage": {
            "minimumAnswerableSamples": 35,
            "maximumAnswerableRatio": 0.45,
        },
        "nearDuplicateThreshold": 0.82,
    }
}


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


def _type_difficulty_distribution(samples: list[dict[str, Any]]) -> dict[str, dict[str, int]]:
    result: dict[str, dict[str, int]] = {}
    for sample in samples:
        sample_type = str(sample.get("type"))
        difficulty = str(sample.get("difficulty"))
        cells = result.setdefault(sample_type, {})
        cells[difficulty] = cells.get(difficulty, 0) + 1
    return {
        sample_type: dict(sorted(cells.items()))
        for sample_type, cells in sorted(result.items())
    }


def _validate_review_artifact(
    repo_root: Path,
    descriptor: Any,
    samples: list[dict[str, Any]],
) -> tuple[dict[str, Any], dict[str, dict[str, Any]]]:
    identity, review_path = _validate_file_identity(
        repo_root,
        descriptor,
        "annotationReview",
    )
    records = _read_jsonl_objects(review_path, identity["path"])
    expected_fields = {
        "sampleId",
        "structureStatus",
        "groundingStatus",
        "duplicateStatus",
        "semanticReviewStatus",
        "reviewNotes",
    }
    status_fields = {
        "structureStatus": {"passed"},
        "groundingStatus": {"passed", "not_applicable"},
        "duplicateStatus": {"unique", "accepted_distinct", "rejected_duplicate"},
        "semanticReviewStatus": {"passed"},
    }
    review_by_id: dict[str, dict[str, Any]] = {}
    for line_number, record in enumerate(records, start=1):
        if set(record) != expected_fields:
            raise DatasetContractError(
                "review_invalid",
                identity["path"],
                f"line={line_number} fields mismatch",
            )
        sample_id = record.get("sampleId")
        if not isinstance(sample_id, str) or not sample_id.strip():
            raise DatasetContractError(
                "review_invalid",
                identity["path"],
                f"line={line_number} field=sampleId",
            )
        if sample_id in review_by_id:
            raise DatasetContractError(
                "review_duplicate_sample_id",
                identity["path"],
                f"line={line_number} sample={sample_id}",
            )
        for field, allowed in status_fields.items():
            if record.get(field) not in allowed:
                raise DatasetContractError(
                    "review_invalid",
                    identity["path"],
                    f"line={line_number} sample={sample_id} field={field}",
                )
        if record.get("duplicateStatus") == "rejected_duplicate":
            raise DatasetContractError(
                "review_rejected_sample",
                identity["path"],
                f"line={line_number} sample={sample_id}",
            )
        notes = record.get("reviewNotes")
        if not isinstance(notes, str) or not notes.strip():
            raise DatasetContractError(
                "review_invalid",
                identity["path"],
                f"line={line_number} sample={sample_id} field=reviewNotes",
            )
        review_by_id[sample_id] = record
    sample_ids = [str(sample.get("id")) for sample in samples]
    if set(review_by_id) != set(sample_ids) or len(records) != len(samples):
        raise DatasetContractError(
            "review_coverage_mismatch",
            identity["path"],
            "review sample IDs do not match question set",
        )
    for sample in samples:
        sample_id = str(sample.get("id"))
        expected_grounding_status = (
            "passed" if sample.get("should_answer") is True else "not_applicable"
        )
        if review_by_id[sample_id].get("groundingStatus") != expected_grounding_status:
            raise DatasetContractError(
                "review_invalid",
                identity["path"],
                f"sample={sample_id} field=groundingStatus",
            )
    expected_count = descriptor.get("reviewedSampleCount") if isinstance(descriptor, dict) else None
    if expected_count != len(records):
        raise DatasetContractError(
            "release_identity_mismatch",
            identity["path"],
            "reviewed sample count drifted",
        )
    identity["reviewedSampleCount"] = len(records)
    return identity, review_by_id


def _validate_expanded_dataset(
    repo_root: Path,
    manifest: dict[str, Any],
    samples: list[dict[str, Any]],
    distribution: dict[str, dict[str, int]],
    fixture_names: set[str],
) -> dict[str, Any]:
    artifact = "expandedDataset"
    expanded = manifest.get("expandedDataset")
    if not isinstance(expanded, dict):
        raise DatasetContractError("manifest_invalid", artifact, "descriptor must be an object")
    for field in ("totalSampleCount", "seedSampleCount", "newSampleCount"):
        value = expanded.get(field)
        if not isinstance(value, int) or isinstance(value, bool) or value < 0:
            raise DatasetContractError("manifest_invalid", artifact, f"field={field}")
    total = expanded["totalSampleCount"]
    seed_count = expanded["seedSampleCount"]
    new_count = expanded["newSampleCount"]
    if total != len(samples) or seed_count + new_count != total:
        raise DatasetContractError("quota_mismatch", artifact, "sample counts do not match")

    quotas = expanded.get("quotas")
    if not isinstance(quotas, dict):
        raise DatasetContractError("manifest_invalid", artifact, "field=quotas")
    actual_quotas = {
        "type": distribution["type"],
        "difficulty": distribution["difficulty"],
        "shouldAnswer": distribution["shouldAnswer"],
        "typeDifficulty": _type_difficulty_distribution(samples),
    }
    if quotas != actual_quotas:
        raise DatasetContractError("quota_mismatch", artifact, "declared quotas do not match samples")

    approved = APPROVED_EXPANDED_RELEASES.get(str(manifest.get("releaseVersion")))
    if approved is not None:
        approved_actual = {
            "totalSampleCount": total,
            "seedSampleCount": seed_count,
            "newSampleCount": new_count,
            "quotas": quotas,
            "fixtureCoverage": expanded.get("fixtureCoverage"),
            "nearDuplicateThreshold": expanded.get("nearDuplicateThreshold"),
        }
        if approved_actual != approved:
            raise DatasetContractError(
                "quota_mismatch",
                artifact,
                "release does not match approved expanded contract",
            )

    seed_identity, seed_path = _validate_file_identity(
        repo_root,
        manifest.get("seedQuestionSet"),
        "seedQuestionSet",
    )
    seed_samples = _read_jsonl_objects(seed_path, seed_identity["path"])
    seed_descriptor = manifest.get("seedQuestionSet")
    expected_seed_count = seed_descriptor.get("sampleCount") if isinstance(seed_descriptor, dict) else None
    expected_seed_order = (
        seed_descriptor.get("orderedSampleIdsSha256")
        if isinstance(seed_descriptor, dict)
        else None
    )
    if (
        expected_seed_count != len(seed_samples)
        or expected_seed_order != _ordered_sample_ids_sha256(seed_samples)
        or seed_count != len(seed_samples)
        or samples[:seed_count] != seed_samples
    ):
        raise DatasetContractError(
            "seed_identity_mismatch",
            seed_identity["path"],
            "seed objects or order drifted",
        )
    question_relative, question_resolved = _safe_repo_path(
        repo_root,
        manifest["questionSet"].get("path"),
        "questionSet",
    )
    if not question_resolved.read_bytes().startswith(seed_path.read_bytes()):
        raise DatasetContractError(
            "seed_identity_mismatch",
            question_relative,
            "seed raw line bytes drifted",
        )
    seed_identity.update({
        "sampleCount": len(seed_samples),
        "orderedSampleIdsSha256": _ordered_sample_ids_sha256(seed_samples),
    })

    coverage = expanded.get("fixtureCoverage")
    if not isinstance(coverage, dict):
        raise DatasetContractError("manifest_invalid", artifact, "field=fixtureCoverage")
    minimum = coverage.get("minimumAnswerableSamples")
    maximum_ratio = coverage.get("maximumAnswerableRatio")
    if (
        not isinstance(minimum, int)
        or isinstance(minimum, bool)
        or minimum < 0
        or not isinstance(maximum_ratio, (int, float))
        or isinstance(maximum_ratio, bool)
        or not 0 < maximum_ratio <= 1
    ):
        raise DatasetContractError("manifest_invalid", artifact, "fixture coverage boundary invalid")
    answerable_count = sum(sample.get("should_answer") is True for sample in samples)
    coverage_counts = {name: 0 for name in sorted(fixture_names)}
    for sample in samples:
        if sample.get("should_answer") is not True:
            continue
        referenced = set(sample.get("expected_sources") or [])
        referenced.update(
            context.get("source")
            for context in (sample.get("expected_contexts") or [])
            if isinstance(context, dict)
        )
        for name in referenced:
            if name in coverage_counts:
                coverage_counts[name] += 1
    if any(count < minimum for count in coverage_counts.values()) or any(
        answerable_count and count / answerable_count > maximum_ratio
        for count in coverage_counts.values()
    ):
        raise DatasetContractError(
            "fixture_coverage_mismatch",
            artifact,
            "fixture coverage is outside declared boundaries",
        )
    threshold = expanded.get("nearDuplicateThreshold")
    if (
        not isinstance(threshold, (int, float))
        or isinstance(threshold, bool)
        or not 0 < threshold <= 1
    ):
        raise DatasetContractError("manifest_invalid", artifact, "nearDuplicateThreshold invalid")
    return {
        "totalSampleCount": total,
        "seedSampleCount": seed_count,
        "newSampleCount": new_count,
        "quotas": actual_quotas,
        "fixtureCoverage": {
            **coverage,
            "answerableSampleCount": answerable_count,
            "counts": coverage_counts,
        },
        "nearDuplicateThreshold": threshold,
        "seedQuestionSet": seed_identity,
    }


def _normalize_whitespace(value: str) -> str:
    return re.sub(r"\s+", "", value)


def _normalize_question(value: str) -> str:
    normalized = unicodedata.normalize("NFKC", value).casefold()
    return "".join(character for character in normalized if character.isalnum())


def _question_features(value: str) -> set[str]:
    normalized = unicodedata.normalize("NFKC", value).casefold()
    features = {f"ascii:{token}" for token in re.findall(r"[a-z0-9]+", normalized)}
    chinese = "".join(re.findall(r"[\u3400-\u9fff]", normalized))
    features.update(
        f"cjk:{chinese[index:index + 2]}"
        for index in range(max(0, len(chinese) - 1))
    )
    if features:
        return features
    compact = _normalize_question(value)
    return {
        f"char:{compact[index:index + 2]}"
        for index in range(max(0, len(compact) - 1))
    }


def _jaccard(left: set[str], right: set[str]) -> float:
    union = left | right
    if not union:
        return 1.0
    return len(left & right) / len(union)


def _validate_question_duplicates(
    samples: list[dict[str, Any]],
    reviews: dict[str, dict[str, Any]],
    threshold: float,
) -> dict[str, int | float]:
    normalized_seen: dict[str, str] = {}
    features: list[tuple[str, set[str]]] = []
    near_candidates = 0
    for sample in samples:
        sample_id = str(sample.get("id"))
        question = str(sample.get("question"))
        normalized = _normalize_question(question)
        if normalized in normalized_seen:
            raise DatasetContractError(
                "duplicate_normalized_question",
                "expandedDataset",
                f"sample={sample_id}",
            )
        normalized_seen[normalized] = sample_id
        current_features = _question_features(question)
        for prior_id, prior_features in features:
            if _jaccard(prior_features, current_features) >= threshold:
                near_candidates += 1
                if reviews[sample_id].get("duplicateStatus") != "accepted_distinct":
                    raise DatasetContractError(
                        "near_duplicate_review_missing",
                        "annotationReview",
                        f"sample={sample_id} candidate={prior_id}",
                    )
        features.append((sample_id, current_features))
    return {
        "normalizedExactDuplicateCount": 0,
        "nearDuplicateCandidateCount": near_candidates,
        "nearDuplicateThreshold": threshold,
    }


def _validate_new_sample_grounding(
    repo_root: Path,
    fixtures: list[dict[str, Any]],
    samples: list[dict[str, Any]],
    seed_count: int,
) -> dict[str, int]:
    fixture_text: dict[str, str] = {}
    for fixture in fixtures:
        relative, resolved = _safe_repo_path(repo_root, fixture["path"], "fixture-grounding")
        fixture_text[PurePosixPath(relative).name] = resolved.read_text(encoding="utf-8")
    answerable_checked = 0
    context_checked = 0
    normalized_matches = 0
    for line_number, sample in enumerate(samples[seed_count:], start=seed_count + 1):
        sample_id = str(sample.get("id"))
        if sample.get("should_answer") is not True:
            continue
        answerable_checked += 1
        contexts = sample.get("expected_contexts") or []
        evidence_points: set[tuple[str, str]] = set()
        for context in contexts:
            source = context["source"]
            contains = context["contains"]
            text = fixture_text[source]
            context_checked += 1
            if contains not in text:
                if _normalize_whitespace(contains) not in _normalize_whitespace(text):
                    raise DatasetContractError(
                        "context_grounding_mismatch",
                        "expandedDataset",
                        f"line={line_number} sample={sample_id} field=expected_contexts",
                    )
                normalized_matches += 1
            evidence_points.add((source, _normalize_whitespace(contains)))
        if sample.get("type") == "multi_hop" and len(evidence_points) < 2:
            raise DatasetContractError(
                "multi_hop_evidence_incomplete",
                "expandedDataset",
                f"line={line_number} sample={sample_id}",
            )
    return {
        "newAnswerableSamplesChecked": answerable_checked,
        "contextsChecked": context_checked,
        "normalizedWhitespaceMatches": normalized_matches,
    }


def _validate_seed_release_compatibility(
    repo_root: Path,
    manifest: dict[str, Any],
    versions: dict[str, str],
) -> dict[str, Any]:
    descriptor_identity, _ = _validate_file_identity(
        repo_root,
        manifest.get("seedReleaseManifest"),
        "seedReleaseManifest",
    )
    seed_identity = validate_versioned_release(repo_root, Path(descriptor_identity["path"]))
    for field in ("releaseVersion", "questionSetVersion", "annotationVersion"):
        if versions[field] == seed_identity[field]:
            raise DatasetContractError(
                "release_version_mismatch",
                "seedReleaseManifest",
                f"field={field} must change for expanded identity",
            )

    same_schema = manifest.get("sampleSchema") == {
        key: seed_identity["sampleSchema"][key]
        for key in ("path", "sha256", "bytes")
    }
    schema_version_changed = versions["sampleSchemaVersion"] != seed_identity["sampleSchemaVersion"]
    if same_schema == schema_version_changed:
        raise DatasetContractError(
            "release_version_mismatch",
            "seedReleaseManifest",
            "sample schema version does not match artifact change",
        )

    current_fixtures = manifest.get("fixtures")
    seed_fixtures = [
        {key: fixture[key] for key in ("path", "sha256", "bytes")}
        for fixture in seed_identity["fixtures"]
    ]
    same_fixtures = current_fixtures == seed_fixtures
    fixture_version_changed = versions["fixtureCorpusVersion"] != seed_identity["fixtureCorpusVersion"]
    if same_fixtures == fixture_version_changed:
        raise DatasetContractError(
            "release_version_mismatch",
            "seedReleaseManifest",
            "fixture corpus version does not match artifact change",
        )

    seed_question = manifest.get("seedQuestionSet")
    expected_seed_question = {
        key: seed_identity["questionSet"][key]
        for key in ("path", "sha256", "bytes", "sampleCount", "orderedSampleIdsSha256")
    }
    if seed_question != expected_seed_question:
        raise DatasetContractError(
            "seed_identity_mismatch",
            "seedReleaseManifest",
            "seed question descriptor does not match seed release",
        )
    return {
        **descriptor_identity,
        "releaseVersion": seed_identity["releaseVersion"],
        "questionSetVersion": seed_identity["questionSetVersion"],
        "annotationVersion": seed_identity["annotationVersion"],
        "sampleSchemaVersion": seed_identity["sampleSchemaVersion"],
        "fixtureCorpusVersion": seed_identity["fixtureCorpusVersion"],
    }


def validate_versioned_release(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    manifest_relative, manifest_resolved = _safe_repo_path(
        repo_root,
        manifest_path.as_posix(),
        "dataset-manifest",
    )
    manifest = _load_json_object(manifest_resolved, manifest_relative)
    manifest_schema_version = manifest.get("manifestSchemaVersion")
    if manifest_schema_version not in SUPPORTED_MANIFEST_SCHEMA_VERSIONS:
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

    annotation_review = None
    expanded_dataset = None
    if manifest_schema_version == MANIFEST_SCHEMA_VERSION_V2:
        annotation_review, review_by_id = _validate_review_artifact(
            repo_root,
            manifest.get("annotationReview"),
            samples,
        )
        expanded_dataset = _validate_expanded_dataset(
            repo_root,
            manifest,
            samples,
            distribution,
            fixture_names,
        )
        expanded_dataset["grounding"] = _validate_new_sample_grounding(
            repo_root,
            fixtures,
            samples,
            expanded_dataset["seedSampleCount"],
        )
        expanded_dataset["duplicates"] = _validate_question_duplicates(
            samples,
            review_by_id,
            float(expanded_dataset["nearDuplicateThreshold"]),
        )
        expanded_dataset["seedRelease"] = _validate_seed_release_compatibility(
            repo_root,
            manifest,
            versions,
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
        "manifestSchemaVersion": manifest_schema_version,
        "manifestPath": manifest_relative,
        "manifestSha256": sha256_file(manifest_resolved),
        **versions,
        "questionSet": question_identity,
        "sampleSchema": schema_identity,
        "fixtures": fixtures,
        "logicalKnowledgeBase": logical_kb,
        "distribution": distribution,
        **({"annotationReview": annotation_review} if annotation_review is not None else {}),
        **({"expandedDataset": expanded_dataset} if expanded_dataset is not None else {}),
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
