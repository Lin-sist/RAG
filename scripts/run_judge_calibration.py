#!/usr/bin/env python3
"""Validate and plan the versioned C9b judge calibration corpus."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import urllib.error
import urllib.request
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import rag_judge_contract as judge_contract


DEFAULT_MANIFEST = Path("docs/eval/calibration/judge-calibration-v1-manifest.json")
EXPECTED_MANIFEST_SCHEMA = "judge-calibration-manifest-v1"
EXPECTED_RELEASE = "judge-calibration-v1"
EXPECTED_CASE_SCHEMA = "judge-calibration-case-v1"
EXPECTED_REVIEW_VERSION = "human-review-v1"
EXPECTED_QUADRANTS = {"ft_rt": 6, "ft_rf": 6, "ff_rt": 6, "ff_rf": 6}


class CalibrationContractError(ValueError):
    def __init__(self, code: str, artifact: str, case_id: str = "", field: str = ""):
        super().__init__(code)
        self.code = code
        self.artifact = artifact
        self.case_id = case_id
        self.field = field


class CalibrationCallError(RuntimeError):
    def __init__(self, code: str):
        super().__init__(code)
        self.code = code


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate or run the C9b judge calibration corpus.")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST))
    parser.add_argument("--profile", choices=("canary", "full"), default="canary")
    parser.add_argument("--plan-only", action="store_true")
    parser.add_argument("--execute-live-judge", action="store_true")
    parser.add_argument("--judge-base-url", default=os.getenv("RAG_EVAL_JUDGE_BASE_URL", "https://integrate.api.nvidia.com/v1"))
    parser.add_argument("--judge-api-key", default=os.getenv("RAG_EVAL_JUDGE_API_KEY", ""))
    parser.add_argument("--judge-model", default=os.getenv("RAG_EVAL_JUDGE_MODEL", ""))
    parser.add_argument("--judge-temperature", type=float, default=0.0)
    parser.add_argument("--judge-timeout", type=float, default=60.0)
    parser.add_argument("--judge-max-context-chars", type=int, default=6000)
    parser.add_argument("--report", default="tmp/eval/judge-calibration.md")
    parser.add_argument("--details-json", default="tmp/eval/judge-calibration-details.json")
    parser.add_argument("--no-overwrite", action="store_true")
    return parser.parse_args()


def _repo_file(repo_root: Path, raw_path: str) -> Path:
    relative = Path(raw_path)
    if relative.is_absolute() or ".." in relative.parts:
        raise CalibrationContractError("invalid_artifact_path", "<invalid-path>")
    resolved = (repo_root / relative).resolve()
    try:
        resolved.relative_to(repo_root.resolve())
    except ValueError as exc:
        raise CalibrationContractError("invalid_artifact_path", "<invalid-path>") from exc
    if not resolved.is_file():
        raise CalibrationContractError("missing_artifact", raw_path)
    return resolved


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _verify_artifact(repo_root: Path, descriptor: dict[str, Any]) -> Path:
    raw_path = str(descriptor.get("path") or "")
    path = _repo_file(repo_root, raw_path)
    if path.stat().st_size != descriptor.get("bytes") or _sha256(path) != descriptor.get("sha256"):
        raise CalibrationContractError("artifact_identity_mismatch", raw_path)
    return path


def _load_json(path: Path, artifact: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise CalibrationContractError("invalid_json", artifact) from exc
    if not isinstance(value, dict):
        raise CalibrationContractError("invalid_json_object", artifact)
    return value


def _load_cases(path: Path, artifact: str) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            value = json.loads(line)
        except json.JSONDecodeError as exc:
            raise CalibrationContractError("invalid_case_json", artifact, field=str(line_no)) from exc
        if not isinstance(value, dict):
            raise CalibrationContractError("invalid_case_object", artifact, field=str(line_no))
        cases.append(value)
    return cases


def _expected_quadrant(faithful: bool, relevant: bool) -> str:
    return f"{'ft' if faithful else 'ff'}_{'rt' if relevant else 'rf'}"


def _validate_cases(
    repo_root: Path,
    cases: list[dict[str, Any]],
    schema: dict[str, Any],
    fixture_paths: set[str],
    case_artifact: str,
) -> Counter[str]:
    required = set(schema.get("requiredFields") or [])
    allowed_languages = set(schema.get("allowedLanguages") or [])
    allowed_types = set(schema.get("allowedCaseTypes") or [])
    allowed_quadrants = set(schema.get("allowedQuadrants") or [])
    required_review = schema.get("requiredReviewStatus")
    seen: set[str] = set()
    quadrants: Counter[str] = Counter()
    fixture_text: dict[str, str] = {}

    for case in cases:
        case_id = str(case.get("id") or "")
        if not case_id or case_id in seen:
            raise CalibrationContractError("duplicate_or_missing_case_id", case_artifact, case_id, "id")
        seen.add(case_id)
        if set(case) != required:
            raise CalibrationContractError("case_schema_mismatch", case_artifact, case_id)
        if case.get("language") not in allowed_languages or case.get("caseType") not in allowed_types:
            raise CalibrationContractError("case_enum_mismatch", case_artifact, case_id)
        if not isinstance(case.get("question"), str) or not case["question"].strip():
            raise CalibrationContractError("case_text_missing", case_artifact, case_id, "question")
        if not isinstance(case.get("answer"), str) or not case["answer"].strip():
            raise CalibrationContractError("case_text_missing", case_artifact, case_id, "answer")
        faithful = case.get("goldFaithful")
        relevant = case.get("goldRelevant")
        joint = case.get("goldJointPass")
        if not all(isinstance(value, bool) for value in (faithful, relevant, joint)):
            raise CalibrationContractError("invalid_gold_type", case_artifact, case_id)
        expected_quadrant = _expected_quadrant(faithful, relevant)
        if joint is not (faithful and relevant) or case.get("quadrant") != expected_quadrant:
            raise CalibrationContractError("gold_consistency_mismatch", case_artifact, case_id)
        if expected_quadrant not in allowed_quadrants:
            raise CalibrationContractError("case_enum_mismatch", case_artifact, case_id, "quadrant")
        if case.get("reviewStatus") != required_review or case.get("reviewVersion") != EXPECTED_REVIEW_VERSION:
            raise CalibrationContractError("review_incomplete", case_artifact, case_id)
        refs = case.get("contextRefs")
        if not isinstance(refs, list) or not refs:
            raise CalibrationContractError("context_ref_missing", case_artifact, case_id)
        for ref in refs:
            if not isinstance(ref, dict) or set(ref) != {"source", "contains"}:
                raise CalibrationContractError("context_ref_schema_mismatch", case_artifact, case_id)
            source = str(ref.get("source") or "")
            contains = str(ref.get("contains") or "")
            if source not in fixture_paths or not contains:
                raise CalibrationContractError("unknown_fixture_source", case_artifact, case_id, "contextRefs")
            if source not in fixture_text:
                fixture_text[source] = _repo_file(repo_root, source).read_text(encoding="utf-8")
            if contains not in fixture_text[source]:
                raise CalibrationContractError("fixture_grounding_mismatch", case_artifact, case_id, "contextRefs")
        quadrants[expected_quadrant] += 1
    return quadrants


def validate_calibration_release(repo_root: Path, manifest_path: Path) -> dict[str, Any]:
    manifest_file = _repo_file(repo_root, str(manifest_path))
    manifest = _load_json(manifest_file, str(manifest_path))
    if manifest.get("manifestSchemaVersion") != EXPECTED_MANIFEST_SCHEMA:
        raise CalibrationContractError("manifest_schema_mismatch", str(manifest_path))
    if manifest.get("releaseVersion") != EXPECTED_RELEASE:
        raise CalibrationContractError("release_version_mismatch", str(manifest_path))
    if manifest.get("caseSchemaVersion") != EXPECTED_CASE_SCHEMA:
        raise CalibrationContractError("case_schema_version_mismatch", str(manifest_path))
    if manifest.get("reviewVersion") != EXPECTED_REVIEW_VERSION:
        raise CalibrationContractError("review_version_mismatch", str(manifest_path))
    if manifest.get("judgeContractVersion") != judge_contract.JUDGE_CONTRACT_VERSION:
        raise CalibrationContractError("judge_contract_identity_mismatch", str(manifest_path))
    if manifest.get("promptVersion") != judge_contract.JUDGE_PROMPT_VERSION:
        raise CalibrationContractError("judge_contract_identity_mismatch", str(manifest_path))
    expected_prompt_hash = hashlib.sha256(judge_contract.JUDGE_SYSTEM_PROMPT.encode("utf-8")).hexdigest()
    if manifest.get("promptSha256") != expected_prompt_hash:
        raise CalibrationContractError("judge_contract_identity_mismatch", str(manifest_path))

    schema_file = _verify_artifact(repo_root, manifest.get("caseSchema") or {})
    case_descriptor = manifest.get("cases") or {}
    case_file = _verify_artifact(repo_root, case_descriptor)
    fixtures = manifest.get("fixtures") or []
    if not isinstance(fixtures, list) or not fixtures:
        raise CalibrationContractError("fixture_manifest_missing", str(manifest_path))
    fixture_paths: set[str] = set()
    for descriptor in fixtures:
        _verify_artifact(repo_root, descriptor)
        fixture_paths.add(str(descriptor.get("path") or ""))

    schema = _load_json(schema_file, str(manifest["caseSchema"]["path"]))
    if schema.get("schemaVersion") != EXPECTED_CASE_SCHEMA:
        raise CalibrationContractError("case_schema_version_mismatch", str(manifest["caseSchema"]["path"]))
    cases = _load_cases(case_file, str(case_descriptor.get("path") or ""))
    if len(cases) != case_descriptor.get("count"):
        raise CalibrationContractError("case_count_mismatch", str(case_descriptor.get("path") or ""))
    quadrants = _validate_cases(
        repo_root,
        cases,
        schema,
        fixture_paths,
        str(case_descriptor.get("path") or ""),
    )
    if dict(quadrants) != EXPECTED_QUADRANTS or manifest.get("quadrantQuotas") != EXPECTED_QUADRANTS:
        raise CalibrationContractError("quadrant_quota_mismatch", str(case_descriptor.get("path") or ""))
    return {
        "validationStatus": "VALID",
        "releaseVersion": manifest["releaseVersion"],
        "manifestPath": str(manifest_path).replace("\\", "/"),
        "manifestSha256": _sha256(manifest_file),
        "caseSchemaVersion": manifest["caseSchemaVersion"],
        "reviewVersion": manifest["reviewVersion"],
        "caseCount": len(cases),
        "caseIds": [str(case["id"]) for case in cases],
        "quadrantCounts": dict(sorted(quadrants.items())),
        "cases": case_descriptor,
        "fixtures": fixtures,
    }


def calibration_plan(
    repo_root: Path,
    manifest_path: Path,
    *,
    profile: str,
    judge_args: Any,
) -> dict[str, Any]:
    identity = validate_calibration_release(repo_root, manifest_path)
    case_path = _repo_file(repo_root, str(identity["cases"]["path"]))
    cases = _load_cases(case_path, str(identity["cases"]["path"]))
    if profile == "canary":
        selected: list[dict[str, Any]] = []
        seen: set[str] = set()
        for case in cases:
            quadrant = str(case["quadrant"])
            if quadrant not in seen:
                selected.append(case)
                seen.add(quadrant)
        repeat_count = 1
    elif profile == "full":
        selected = cases
        repeat_count = 3
    else:
        raise CalibrationContractError("invalid_profile", str(manifest_path), field=profile)
    return {
        "profile": profile,
        "calibrationReleaseIdentity": identity,
        "judgeContractConfig": judge_contract.contract_config(judge_args),
        "selectedCaseCount": len(selected),
        "selectedCaseIds": [str(case["id"]) for case in selected],
        "selectedQuadrants": [str(case["quadrant"]) for case in selected],
        "repeatCount": repeat_count,
        "repeatIndexes": list(range(1, repeat_count + 1)),
        "estimatedJudgeCalls": len(selected) * repeat_count,
        "backendCalls": 0,
        "embeddingCalls": 0,
        "rerankCalls": 0,
        "askCalls": 0,
        "generationCalls": 0,
    }


def _selected_cases(
    repo_root: Path,
    identity: dict[str, Any],
    selected_ids: list[str],
) -> list[dict[str, Any]]:
    case_path = _repo_file(repo_root, str(identity["cases"]["path"]))
    cases = _load_cases(case_path, str(identity["cases"]["path"]))
    by_id = {str(case["id"]): case for case in cases}
    try:
        return [by_id[case_id] for case_id in selected_ids]
    except KeyError as exc:
        raise CalibrationContractError(
            "case_selection_identity_mismatch",
            str(identity["cases"]["path"]),
            str(exc.args[0]),
        ) from exc


def _case_contexts(repo_root: Path, case: dict[str, Any]) -> list[dict[str, str]]:
    return [
        {
            "source": str(ref["source"]),
            "content": str(ref["contains"]),
        }
        for ref in case["contextRefs"]
    ]


def _error_category(exc: Exception) -> str:
    if isinstance(exc, judge_contract.JudgePayloadError):
        return "invalid_judge_payload"
    if isinstance(exc, TimeoutError):
        return "timeout"
    code = getattr(exc, "code", None)
    if isinstance(code, str) and code:
        return code
    return "judge_call_error"


def call_live_judge(
    case: dict[str, Any],
    contexts: list[dict[str, str]],
    args: Any,
) -> str:
    if not str(getattr(args, "judge_model", "")).strip():
        raise CalibrationCallError("judge_model_missing")
    api_key = str(getattr(args, "judge_api_key", ""))
    if not api_key:
        raise CalibrationCallError("judge_api_key_missing")
    user_prompt = judge_contract.build_judge_prompt(
        question=str(case.get("question") or ""),
        answer=str(case.get("answer") or ""),
        contexts=contexts,
        expected_points=[],
        expected_keywords=[],
        max_context_chars=int(getattr(args, "judge_max_context_chars", 6000)),
    )
    payload = {
        "model": str(args.judge_model),
        "temperature": float(getattr(args, "judge_temperature", 0.0)),
        "messages": [
            {"role": "system", "content": judge_contract.JUDGE_SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
    }
    request = urllib.request.Request(
        f"{str(args.judge_base_url).rstrip('/')}/chat/completions",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=float(args.judge_timeout)) as response:
            body = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        if exc.code == 429:
            raise CalibrationCallError("rate_limited") from exc
        raise CalibrationCallError("judge_http_error") from exc
    except urllib.error.URLError as exc:
        if isinstance(exc.reason, TimeoutError):
            raise CalibrationCallError("timeout") from exc
        raise CalibrationCallError("judge_network_error") from exc
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise CalibrationCallError("invalid_provider_response") from exc
    try:
        content = body["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise CalibrationCallError("invalid_provider_response") from exc
    if not isinstance(content, str) or not content.strip():
        raise CalibrationCallError("invalid_provider_response")
    return content


def execute_calibration(
    repo_root: Path,
    manifest_path: Path,
    *,
    profile: str,
    judge_args: Any,
    judge_call: Any,
) -> dict[str, Any]:
    plan = calibration_plan(
        repo_root,
        manifest_path,
        profile=profile,
        judge_args=judge_args,
    )
    cases = _selected_cases(
        repo_root,
        plan["calibrationReleaseIdentity"],
        plan["selectedCaseIds"],
    )
    observations: list[dict[str, Any]] = []
    for case in cases:
        contexts = _case_contexts(repo_root, case)
        for repeat_index in plan["repeatIndexes"]:
            observation: dict[str, Any] = {
                "caseId": str(case["id"]),
                "quadrant": str(case["quadrant"]),
                "repeatIndex": int(repeat_index),
                "attemptCount": 1,
            }
            try:
                raw_content = judge_call(case, contexts, judge_args)
                parsed = judge_contract.parse_judge_content(raw_content)
                observation.update({
                    "faithfulnessScore": parsed["faithfulnessScore"],
                    "relevanceScore": parsed["relevanceScore"],
                    "judgePass": parsed["pass"],
                    "providerReportedPass": parsed["providerReportedPass"],
                    "providerPassMismatch": parsed["providerPassMismatch"],
                    "reason": parsed["reason"],
                    "rawContent": raw_content,
                    "errorCategory": None,
                })
            except Exception as exc:  # noqa: BLE001 - every expected observation must be retained
                observation.update({
                    "errorCategory": _error_category(exc),
                    "errorType": type(exc).__name__,
                })
            observations.append(observation)
    return {
        "plan": plan,
        "summary": aggregate_calibration(
            cases,
            observations,
            repeat_count=int(plan["repeatCount"]),
        ),
        "observations": observations,
    }


def _confusion(gold: list[bool], predicted: list[bool]) -> dict[str, Any]:
    tp = sum(1 for expected, actual in zip(gold, predicted) if expected and actual)
    tn = sum(1 for expected, actual in zip(gold, predicted) if not expected and not actual)
    fp = sum(1 for expected, actual in zip(gold, predicted) if not expected and actual)
    fn = sum(1 for expected, actual in zip(gold, predicted) if expected and not actual)
    total = len(gold)
    return {
        "tp": tp,
        "tn": tn,
        "fp": fp,
        "fn": fn,
        "agreement": (tp + tn) / total if total else "N/A",
    }


def aggregate_calibration(
    cases: list[dict[str, Any]],
    observations: list[dict[str, Any]],
    *,
    repeat_count: int,
) -> dict[str, Any]:
    cases_by_id = {str(case["id"]): case for case in cases}
    expected_keys = {
        (case_id, repeat_index)
        for case_id in cases_by_id
        for repeat_index in range(1, repeat_count + 1)
    }
    observed: dict[tuple[str, int], dict[str, Any]] = {}
    duplicate_count = 0
    unexpected_count = 0
    for observation in observations:
        key = (str(observation.get("caseId") or ""), int(observation.get("repeatIndex") or 0))
        if key not in expected_keys:
            unexpected_count += 1
        elif key in observed:
            duplicate_count += 1
        else:
            observed[key] = observation
    valid = [item for item in observed.values() if not item.get("errorCategory")]
    missing_count = len(expected_keys - set(observed))
    invalid_count = len(observed) - len(valid)

    faith_gold: list[bool] = []
    faith_predicted: list[bool] = []
    relevance_gold: list[bool] = []
    relevance_predicted: list[bool] = []
    joint_gold: list[bool] = []
    joint_predicted: list[bool] = []
    predictions_by_case: dict[str, list[tuple[bool, bool, bool]]] = {}
    for item in valid:
        case = cases_by_id[str(item["caseId"])]
        faith_prediction = float(item["faithfulnessScore"]) >= judge_contract.JUDGE_SCORE_THRESHOLD
        relevance_prediction = float(item["relevanceScore"]) >= judge_contract.JUDGE_SCORE_THRESHOLD
        joint_prediction = bool(item["judgePass"])
        faith_gold.append(bool(case["goldFaithful"]))
        faith_predicted.append(faith_prediction)
        relevance_gold.append(bool(case["goldRelevant"]))
        relevance_predicted.append(relevance_prediction)
        joint_gold.append(bool(case["goldJointPass"]))
        joint_predicted.append(joint_prediction)
        predictions_by_case.setdefault(str(item["caseId"]), []).append(
            (faith_prediction, relevance_prediction, joint_prediction)
        )

    consistent_case_ids = [
        case_id
        for case_id, values in predictions_by_case.items()
        if len(values) == repeat_count and len(set(values)) == 1
    ]
    inconsistent_case_ids = [
        case_id
        for case_id in cases_by_id
        if case_id not in consistent_case_ids
    ]
    complete = (
        missing_count == 0
        and invalid_count == 0
        and duplicate_count == 0
        and unexpected_count == 0
        and len(valid) == len(expected_keys)
    )
    error_categories = Counter(
        str(item.get("errorCategory"))
        for item in observed.values()
        if item.get("errorCategory")
    )
    if missing_count:
        error_categories["missing_observation"] += missing_count
    if duplicate_count:
        error_categories["duplicate_observation"] += duplicate_count
    if unexpected_count:
        error_categories["unexpected_observation"] += unexpected_count
    if duplicate_count or unexpected_count:
        calibration_status = "NOT_COMPARABLE"
    else:
        calibration_status = "COMPLETE" if complete else "PARTIAL"
    return {
        "calibrationStatus": calibration_status,
        "expectedObservationCount": len(expected_keys),
        "receivedObservationCount": len(observed),
        "validObservationCount": len(valid),
        "errorObservationCount": invalid_count + missing_count + duplicate_count + unexpected_count,
        "missingObservationCount": missing_count,
        "duplicateObservationCount": duplicate_count,
        "unexpectedObservationCount": unexpected_count,
        "parseCoverage": len(valid) / len(expected_keys) if expected_keys else "N/A",
        "faithfulness": _confusion(faith_gold, faith_predicted),
        "relevance": _confusion(relevance_gold, relevance_predicted),
        "jointPass": _confusion(joint_gold, joint_predicted),
        "providerPassMismatchCount": sum(bool(item.get("providerPassMismatch")) for item in valid),
        "repeatConsistentCaseCount": len(consistent_case_ids),
        "repeatConsistentCaseRate": len(consistent_case_ids) / len(cases_by_id) if cases_by_id else "N/A",
        "inconsistentCaseIds": inconsistent_case_ids,
        "errorCategoryHistogram": dict(sorted(error_categories.items())),
    }


def _format_ratio(value: Any) -> str:
    if isinstance(value, (int, float)):
        return f"{float(value):.4f}"
    return str(value)


def write_calibration_outputs(
    report_path: Path,
    details_path: Path,
    result: dict[str, Any],
    *,
    no_overwrite: bool,
) -> None:
    if no_overwrite:
        for path in (report_path, details_path):
            if path.exists():
                raise CalibrationContractError("output_exists", path.name)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    details_path.parent.mkdir(parents=True, exist_ok=True)
    plan = result["plan"]
    summary = result["summary"]
    identity = plan["calibrationReleaseIdentity"]
    lines = [
        "# Judge Calibration Report",
        "",
        f"- Generated at: `{datetime.now(timezone.utc).isoformat()}`",
        f"- Calibration status: `{summary['calibrationStatus']}`",
        f"- Profile: `{plan['profile']}`",
        f"- Release: `{identity['releaseVersion']}`",
        f"- Manifest SHA-256: `{identity['manifestSha256']}`",
        f"- Selected cases: `{plan['selectedCaseCount']}`",
        f"- Repeats: `{plan['repeatCount']}`",
        f"- Expected judge calls: `{plan['estimatedJudgeCalls']}`",
        f"- Judge contract config: `{json.dumps(plan['judgeContractConfig'], sort_keys=True)}`",
        "",
        "## Coverage And Agreement",
        "",
        "| Metric | Value |",
        "|---|---:|",
        f"| Expected observations | {summary['expectedObservationCount']} |",
        f"| Valid observations | {summary['validObservationCount']} |",
        f"| Missing observations | {summary.get('missingObservationCount', 0)} |",
        f"| Duplicate observations | {summary.get('duplicateObservationCount', 0)} |",
        f"| Unexpected observations | {summary.get('unexpectedObservationCount', 0)} |",
        f"| Parse coverage | {_format_ratio(summary['parseCoverage'])} |",
        f"| Faithfulness agreement | {_format_ratio(summary['faithfulness']['agreement'])} |",
        f"| Relevance agreement | {_format_ratio(summary['relevance']['agreement'])} |",
        f"| Joint pass agreement | {_format_ratio(summary['jointPass']['agreement'])} |",
        f"| Provider pass mismatches | {summary['providerPassMismatchCount']} |",
        f"| Repeat-consistent cases | {summary['repeatConsistentCaseCount']} |",
        f"| Repeat consistency rate | {_format_ratio(summary['repeatConsistentCaseRate'])} |",
        "",
        "## Safe Diagnostics",
        "",
        f"- Error category histogram: `{json.dumps(summary.get('errorCategoryHistogram', {}), sort_keys=True)}`",
        f"- Inconsistent case IDs: `{json.dumps(summary['inconsistentCaseIds'], ensure_ascii=False)}`",
        "- This report does not define a production threshold or go/no-go decision.",
        "- Raw question, answer, context, reason, provider response, credentials, and absolute paths are omitted.",
        "",
    ]
    report_path.write_text("\n".join(lines), encoding="utf-8")
    details_payload = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        **result,
    }
    details_path.write_text(
        json.dumps(details_payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def main() -> int:
    args = parse_args()
    repo_root = Path(__file__).resolve().parents[1]
    try:
        plan = calibration_plan(
            repo_root,
            Path(args.manifest),
            profile=args.profile,
            judge_args=args,
        )
    except CalibrationContractError as exc:
        print(
            f"Calibration validation failed: errorCode={exc.code} artifact={exc.artifact}",
            file=sys.stderr,
        )
        return 2
    if args.plan_only:
        print(json.dumps(plan, ensure_ascii=False, indent=2))
        return 0
    if not args.execute_live_judge:
        print(
            "Live judge calibration is disabled unless --execute-live-judge is explicitly supplied.",
            file=sys.stderr,
        )
        return 2
    if not args.judge_model or not args.judge_api_key:
        print("Live judge calibration requires explicit model and API key configuration.", file=sys.stderr)
        return 2
    report_path = Path(args.report)
    details_path = Path(args.details_json)
    if args.no_overwrite and (report_path.exists() or details_path.exists()):
        print("Live judge calibration refused to overwrite an existing output.", file=sys.stderr)
        return 2
    result = execute_calibration(
        repo_root,
        Path(args.manifest),
        profile=args.profile,
        judge_args=args,
        judge_call=call_live_judge,
    )
    try:
        write_calibration_outputs(
            report_path,
            details_path,
            result,
            no_overwrite=args.no_overwrite,
        )
    except CalibrationContractError as exc:
        print(f"Calibration output failed: errorCode={exc.code} artifact={exc.artifact}", file=sys.stderr)
        return 2
    summary = result["summary"]
    print(
        json.dumps(
            {
                "calibrationStatus": summary["calibrationStatus"],
                "profile": args.profile,
                "expectedJudgeCalls": plan["estimatedJudgeCalls"],
                "validObservations": summary["validObservationCount"],
                "errorCategoryHistogram": summary["errorCategoryHistogram"],
                "report": report_path.name,
                "details": details_path.name,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0 if summary["calibrationStatus"] == "COMPLETE" else 1


if __name__ == "__main__":
    raise SystemExit(main())
