#!/usr/bin/env python3
"""Compare fixed-identity C7 reranker evaluation arms."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import statistics
import sys
from pathlib import Path
from typing import Any

from run_rag_eval import latency_summary


METRIC_FIELDS = {
    "recallAt5": "recall_at_5",
    "mrr": "mrr",
    "top1SourceAccuracy": "top1_source_accuracy",
}
COMPARISON_SCHEMA = "c7-reranker-ab-comparison-v1"
REASON_ORDER = [
    "invalid_evidence_schema",
    "identity_mismatch",
    "run_count_mismatch",
    "sample_pair_mismatch",
    "arm_report_not_retrieval_only",
    "retrieve_error",
    "heuristic_arm_contaminated",
    "model_provider_mismatch",
    "model_fallback_observed",
    "model_coverage_incomplete",
    "candidate_coverage_incomplete",
    "zero_candidate_mismatch",
    "manifest_observation_mismatch",
]


def _mean(values: list[float]) -> float | None:
    return sum(values) / len(values) if values else None


def _valid_latency(value: Any) -> float | None:
    if not isinstance(value, (int, float)):
        return None
    converted = float(value)
    return converted if math.isfinite(converted) and converted >= 0 else None


def _arm_summary(runs: list[dict[str, Any]], arm_id: str) -> dict[str, Any]:
    metrics = {
        output_name: _mean([
            float(run.get("metrics", {}).get(input_name, 0.0))
            for run in runs
        ])
        for output_name, input_name in METRIC_FIELDS.items()
    }
    samples = [sample for run in runs for sample in run.get("samples", [])]
    provider_counts: dict[str, int] = {}
    fallback_histogram: dict[str, int] = {}
    for sample in samples:
        attribution = sample.get("rerankAttribution") or {}
        provider = str(attribution.get("effectiveProvider") or "unknown")
        provider_counts[provider] = provider_counts.get(provider, 0) + 1
        fallback_count = int(attribution.get("fallbackCount", 0) or 0)
        if fallback_count:
            reason = str(attribution.get("fallbackReason") or "unknown")
            fallback_histogram[reason] = fallback_histogram.get(reason, 0) + fallback_count
    eligible = [
        sample for sample in samples
        if int(sample.get("rerankAttribution", {}).get("candidateCount", 0)) > 0
    ]
    if arm_id == "model":
        valid_model = [
            sample for sample in eligible
            if int(sample.get("rerankAttribution", {}).get("modelCallCount", 0)) == 1
            and float(sample.get("rerankAttribution", {}).get("candidateCoverage", 0.0)) == 1.0
        ]
        coverage = len(valid_model) / len(eligible) if eligible else 1.0
    else:
        coverage = None
    return {
        **metrics,
        "sampleCount": len(samples),
        "eligibleSampleCount": len(eligible),
        "eligibleModelCoverage": coverage,
        "effectiveProviderCounts": provider_counts,
        "fallbackReasonHistogram": fallback_histogram,
        "retrievalLatencyMillis": latency_summary([
            sample.get("retrieveLatencyMillis") for sample in samples
        ]),
        "rerankLatencyMillis": latency_summary([
            sample.get("rerankAttribution", {}).get("latencyMillis")
            for sample in samples
        ]),
        "repeatMetrics": [
            {
                "runIndex": run.get("runMetadata", {}).get("repeat", {}).get("index"),
                **{
                    output_name: run.get("metrics", {}).get(input_name)
                    for output_name, input_name in METRIC_FIELDS.items()
                },
            }
            for run in runs
        ],
    }


def _per_sample_latency_summaries(
    heuristic_pairs: dict[str, dict[str, Any]],
    model_pairs: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    grouped: dict[str, dict[str, list[float]]] = {}
    for key in sorted(set(heuristic_pairs) & set(model_pairs)):
        _, sample_id = key.split(":", 1)
        bucket = grouped.setdefault(sample_id, {
            "heuristicRetrieve": [],
            "modelRetrieve": [],
            "pairedRetrieveDelta": [],
            "heuristicRerank": [],
            "modelRerank": [],
            "pairedRerankDelta": [],
        })
        heuristic = heuristic_pairs[key]
        model = model_pairs[key]
        heuristic_retrieve = _valid_latency(heuristic.get("retrieveLatencyMillis"))
        model_retrieve = _valid_latency(model.get("retrieveLatencyMillis"))
        heuristic_rerank = _valid_latency((heuristic.get("rerankAttribution") or {}).get("latencyMillis"))
        model_rerank = _valid_latency((model.get("rerankAttribution") or {}).get("latencyMillis"))
        if heuristic_retrieve is not None:
            bucket["heuristicRetrieve"].append(heuristic_retrieve)
        if model_retrieve is not None:
            bucket["modelRetrieve"].append(model_retrieve)
        if heuristic_retrieve is not None and model_retrieve is not None:
            bucket["pairedRetrieveDelta"].append(model_retrieve - heuristic_retrieve)
        if heuristic_rerank is not None:
            bucket["heuristicRerank"].append(heuristic_rerank)
        if model_rerank is not None:
            bucket["modelRerank"].append(model_rerank)
        if heuristic_rerank is not None and model_rerank is not None:
            bucket["pairedRerankDelta"].append(model_rerank - heuristic_rerank)

    def median(values: list[float]) -> float | None:
        return float(statistics.median(values)) if values else None

    return [
        {
            "sampleId": sample_id,
            "observationCount": len(values["pairedRetrieveDelta"]),
            "retrieveLatencyMillis": {
                "heuristicMedian": median(values["heuristicRetrieve"]),
                "modelMedian": median(values["modelRetrieve"]),
                "pairedDeltaMedian": median(values["pairedRetrieveDelta"]),
            },
            "rerankLatencyMillis": {
                "heuristicMedian": median(values["heuristicRerank"]),
                "modelMedian": median(values["modelRerank"]),
                "pairedDeltaMedian": median(values["pairedRerankDelta"]),
            },
        }
        for sample_id, values in sorted(grouped.items())
    ]


def _strict_identity(metadata: dict[str, Any]) -> dict[str, Any]:
    manifest = metadata.get("armManifest") or {}
    kb = metadata.get("knowledgeBase") or {}
    return {
        "evaluationSchema": metadata.get("evaluationSchema"),
        "evalSetIdentity": metadata.get("evalSetIdentity"),
        "sampleSelection": metadata.get("sampleSelection"),
        "fixtures": metadata.get("fixtures", []),
        "knowledgeBase": {
            "id": kb.get("id"),
            "name": kb.get("name"),
            "description": kb.get("description"),
            "vectorCollection": kb.get("vectorCollection"),
            "documentCount": kb.get("documentCount"),
            "chunkCount": kb.get("chunkCount"),
            "documents": kb.get("documents"),
        },
        "topK": metadata.get("topK"),
        "minScore": metadata.get("minScore"),
        "enableRerank": metadata.get("enableRerank"),
        "configSnapshot": metadata.get("configSnapshot"),
        "git": metadata.get("git"),
        "measuredRepeats": manifest.get("measuredRepeats", (metadata.get("repeat") or {}).get("total")),
        "warmupCalls": manifest.get("warmupCalls", (metadata.get("warmup") or {}).get("calls")),
    }


def _pair_map(runs: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    pairs: dict[str, dict[str, Any]] = {}
    for run in runs:
        run_index = (run.get("runMetadata") or {}).get("repeat", {}).get("index")
        for sample in run.get("samples", []):
            key = f"{run_index}:{sample.get('id')}"
            pairs[key] = sample
    return pairs


def _reason_list(reasons: set[str]) -> list[str]:
    return [reason for reason in REASON_ORDER if reason in reasons]


def _valid_run_schema(run: Any, expected_arm: str) -> bool:
    if not isinstance(run, dict):
        return False
    metadata = run.get("runMetadata")
    manifest = metadata.get("armManifest") if isinstance(metadata, dict) else None
    repeat = metadata.get("repeat") if isinstance(metadata, dict) else None
    metrics = run.get("metrics")
    if (
        not isinstance(metadata, dict)
        or metadata.get("evaluationSchema") != "c7-reranker-ab-v1"
        or not isinstance(manifest, dict)
        or manifest.get("armId") != expected_arm
        or not isinstance(repeat, dict)
        or not isinstance(repeat.get("index"), int)
        or not isinstance(repeat.get("total"), int)
        or not isinstance(run.get("samples"), list)
        or not isinstance(run.get("runCounts"), dict)
        or not isinstance(metrics, dict)
    ):
        return False
    return all(isinstance(metrics.get(field), (int, float)) for field in METRIC_FIELDS.values())


def _attribution_validity(
    arm_id: str,
    runs: list[dict[str, Any]],
    reasons: set[str],
) -> tuple[dict[str, int], int, int]:
    fallback_histogram: dict[str, int] = {}
    eligible_count = 0
    valid_model_count = 0
    for run in runs:
        manifest = (run.get("runMetadata") or {}).get("armManifest") or {}
        expected_requested = manifest.get("expectedRequestedProvider")
        expected_effective = manifest.get("expectedEffectiveProvider")
        expected_model = manifest.get("model")
        expected_protocol = manifest.get("protocol")
        for sample in run.get("samples", []):
            attribution = sample.get("rerankAttribution") or {}
            candidate_count = int(attribution.get("candidateCount", 0) or 0)
            if candidate_count <= 0:
                continue
            eligible_count += 1
            fallback_count = int(attribution.get("fallbackCount", 0) or 0)
            if fallback_count:
                reason = str(attribution.get("fallbackReason") or "unknown")
                fallback_histogram[reason] = fallback_histogram.get(reason, 0) + fallback_count
            if arm_id == "heuristic":
                if (
                    attribution.get("requestedProvider") != "heuristic"
                    or attribution.get("effectiveProvider") != "heuristic"
                    or fallback_count != 0
                    or int(attribution.get("modelCallCount", 0) or 0) != 0
                ):
                    reasons.add("heuristic_arm_contaminated")
                if (
                    attribution.get("requestedProvider") != expected_requested
                    or attribution.get("effectiveProvider") != expected_effective
                ):
                    reasons.add("manifest_observation_mismatch")
                continue

            provider_ok = (
                attribution.get("requestedProvider") == expected_requested
                and attribution.get("effectiveProvider") == expected_effective
            )
            model_call_ok = int(attribution.get("modelCallCount", 0) or 0) == 1
            coverage_ok = float(attribution.get("candidateCoverage", 0.0) or 0.0) == 1.0
            manifest_ok = (
                attribution.get("model") == expected_model
                and attribution.get("protocol") == expected_protocol
            )
            if not provider_ok:
                reasons.add("model_provider_mismatch")
            if fallback_count:
                reasons.add("model_fallback_observed")
            if not model_call_ok:
                reasons.add("model_coverage_incomplete")
            if not coverage_ok:
                reasons.add("candidate_coverage_incomplete")
            if not manifest_ok or not provider_ok:
                reasons.add("manifest_observation_mismatch")
            if provider_ok and fallback_count == 0 and model_call_ok and coverage_ok and manifest_ok:
                valid_model_count += 1
    if arm_id == "model" and valid_model_count != eligible_count:
        reasons.add("model_coverage_incomplete")
    return fallback_histogram, eligible_count, valid_model_count


def compare_runs(
    heuristic_runs: list[dict[str, Any]],
    model_runs: list[dict[str, Any]],
) -> dict[str, Any]:
    """Validate and compare two complete fixed-identity C7 run groups."""
    reasons: set[str] = set()
    if (
        not heuristic_runs
        or not model_runs
        or any(not _valid_run_schema(run, "heuristic") for run in heuristic_runs)
        or any(not _valid_run_schema(run, "model") for run in model_runs)
    ):
        return {
            "comparisonSchema": COMPARISON_SCHEMA,
            "comparisonStatus": "FAILED",
            "comparisonReasons": ["invalid_evidence_schema"],
            "armSummaries": {},
            "diagnostics": {},
            "deltas": None,
        }

    heuristic_identities = [_strict_identity(run.get("runMetadata") or {}) for run in heuristic_runs]
    model_identities = [_strict_identity(run.get("runMetadata") or {}) for run in model_runs]
    identity = heuristic_identities[0]
    if (
        any(item != identity for item in heuristic_identities[1:])
        or any(item != identity for item in model_identities)
    ):
        reasons.add("identity_mismatch")
    if len(heuristic_runs) != len(model_runs):
        reasons.add("run_count_mismatch")
    expected_repeats = identity.get("measuredRepeats")
    if isinstance(expected_repeats, int) and (
        len(heuristic_runs) != expected_repeats or len(model_runs) != expected_repeats
    ):
        reasons.add("run_count_mismatch")
    expected_indexes = set(range(1, expected_repeats + 1)) if isinstance(expected_repeats, int) else set()
    if expected_indexes:
        heuristic_indexes = {
            (run.get("runMetadata") or {}).get("repeat", {}).get("index") for run in heuristic_runs
        }
        model_indexes = {
            (run.get("runMetadata") or {}).get("repeat", {}).get("index") for run in model_runs
        }
        if heuristic_indexes != expected_indexes or model_indexes != expected_indexes:
            reasons.add("run_count_mismatch")

    for run in heuristic_runs + model_runs:
        if run.get("reportStatus") != "RETRIEVAL_ONLY":
            reasons.add("arm_report_not_retrieval_only")
        if int((run.get("runCounts") or {}).get("retrieveErrors", 0) or 0) > 0:
            reasons.add("retrieve_error")
        metadata = run.get("runMetadata") or {}
        selection = metadata.get("sampleSelection") or {}
        declared_ids = selection.get("ids")
        actual_ids = [sample.get("id") for sample in run.get("samples", [])]
        if (
            not isinstance(declared_ids, list)
            or int(selection.get("count", -1)) != len(declared_ids)
            or actual_ids != declared_ids
        ):
            reasons.add("sample_pair_mismatch")
        if any((sample.get("errors") or {}).get("retrieval") for sample in run.get("samples", [])):
            reasons.add("retrieve_error")

    heuristic_pairs = _pair_map(heuristic_runs)
    model_pairs = _pair_map(model_runs)
    missing_heuristic = sorted(set(model_pairs) - set(heuristic_pairs))
    missing_model = sorted(set(heuristic_pairs) - set(model_pairs))
    if missing_heuristic or missing_model:
        reasons.add("sample_pair_mismatch")

    not_applicable_pairs = 0
    zero_candidate_mismatches: list[str] = []
    per_sample_facts: list[dict[str, Any]] = []
    for key in sorted(set(heuristic_pairs) & set(model_pairs)):
        heuristic_sample = heuristic_pairs[key]
        model_sample = model_pairs[key]
        heuristic_attr = heuristic_sample.get("rerankAttribution") or {}
        model_attr = model_sample.get("rerankAttribution") or {}
        heuristic_zero = int(heuristic_attr.get("candidateCount", 0) or 0) == 0
        model_zero = int(model_attr.get("candidateCount", 0) or 0) == 0
        if heuristic_zero and model_zero:
            not_applicable_pairs += 1
        elif heuristic_zero != model_zero:
            zero_candidate_mismatches.append(key)
            reasons.add("zero_candidate_mismatch")
        per_sample_facts.append({
            "pair": key,
            "notApplicable": heuristic_zero and model_zero,
            "heuristic": {
                "retrieveLatencyMillis": heuristic_sample.get("retrieveLatencyMillis"),
                "rerankLatencyMillis": heuristic_attr.get("latencyMillis"),
                "candidateCount": heuristic_attr.get("candidateCount"),
                "scoredCount": heuristic_attr.get("scoredCount"),
                "metricCalculationDetails": heuristic_sample.get("metricCalculationDetails"),
            },
            "model": {
                "retrieveLatencyMillis": model_sample.get("retrieveLatencyMillis"),
                "rerankLatencyMillis": model_attr.get("latencyMillis"),
                "candidateCount": model_attr.get("candidateCount"),
                "scoredCount": model_attr.get("scoredCount"),
                "metricCalculationDetails": model_sample.get("metricCalculationDetails"),
            },
        })

    heuristic_fallbacks, _, _ = _attribution_validity("heuristic", heuristic_runs, reasons)
    model_fallbacks, model_eligible, model_valid = _attribution_validity("model", model_runs, reasons)
    heuristic = _arm_summary(heuristic_runs, "heuristic")
    model = _arm_summary(model_runs, "model")
    model["eligibleModelCoverage"] = model_valid / model_eligible if model_eligible else 1.0
    sample_count = int((identity.get("sampleSelection") or {}).get("count", 0) or 0)
    measured_repeats = int(identity.get("measuredRepeats", 0) or 0)
    warmup_calls = int(identity.get("warmupCalls", 0) or 0)
    per_arm_calls = sample_count * measured_repeats + warmup_calls
    ordered_reasons = _reason_list(reasons)
    deltas = None
    if not ordered_reasons:
        deltas = {
            field: model[field] - heuristic[field]
            for field in METRIC_FIELDS
        }
    return {
        "comparisonSchema": COMPARISON_SCHEMA,
        "comparisonStatus": "COMPARABLE" if not ordered_reasons else "NOT_COMPARABLE",
        "comparisonReasons": ordered_reasons,
        "strictIdentity": identity,
        "armSummaries": {"heuristic": heuristic, "model": model},
        "diagnostics": {
            "missingHeuristicPairs": missing_heuristic,
            "missingModelPairs": missing_model,
            "zeroCandidateMismatchPairs": zero_candidate_mismatches,
            "notApplicablePairCount": not_applicable_pairs,
            "fallbackReasonHistogram": {
                key: heuristic_fallbacks.get(key, 0) + model_fallbacks.get(key, 0)
                for key in sorted(set(heuristic_fallbacks) | set(model_fallbacks))
            },
        },
        "perSampleFacts": per_sample_facts,
        "perSampleSummaries": _per_sample_latency_summaries(heuristic_pairs, model_pairs),
        "callBudget": {
            "debugRetrievalUpperBound": per_arm_calls * 2,
            "queryEmbeddingUpperBound": per_arm_calls * 2,
            "modelRerankUpperBound": per_arm_calls,
            "ask": 0,
            "judge": 0,
            "generation": 0,
        },
        "deltas": deltas,
    }


def _load_details(paths: list[str]) -> list[dict[str, Any]]:
    runs: list[dict[str, Any]] = []
    for path_value in paths:
        path = Path(path_value)
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ValueError(f"Cannot read details JSON {path}: {exc}") from exc
        if not isinstance(value, dict):
            raise ValueError(f"Details JSON must be an object: {path}")
        runs.append(value)
    return runs


def _source_files(role: str, paths: list[str]) -> list[dict[str, Any]]:
    files: list[dict[str, Any]] = []
    for path_value in paths:
        path = Path(path_value)
        data = path.read_bytes()
        files.append({
            "role": role,
            "name": path.name,
            "sha256": hashlib.sha256(data).hexdigest(),
            "bytes": len(data),
        })
    return files


def _format_number(value: Any) -> str:
    if value is None:
        return "unavailable"
    return f"{float(value):.6f}"


def render_markdown(result: dict[str, Any]) -> str:
    lines = [
        "# C7 Reranker A/B Comparison",
        "",
        f"Comparison status: {result['comparisonStatus']}",
        "",
        "Reasons: " + (", ".join(result.get("comparisonReasons", [])) or "none"),
        "",
        "## Strict identity",
        "",
    ]
    identity = result.get("strictIdentity") or {}
    eval_identity = identity.get("evalSetIdentity") or {}
    selection = identity.get("sampleSelection") or {}
    kb = identity.get("knowledgeBase") or {}
    git = identity.get("git") or {}
    lines.extend([
        f"- Evaluation schema: `{identity.get('evaluationSchema')}`",
        f"- Eval-set SHA-256: `{eval_identity.get('sha256')}`",
        f"- Selected samples: {selection.get('count', 0)} in fixed order",
        f"- KB ID / vector collection: `{kb.get('id')}` / `{kb.get('vectorCollection')}`",
        f"- Git HEAD: `{git.get('head')}`",
        f"- Measured repeats / warm-up calls per arm: {identity.get('measuredRepeats')} / {identity.get('warmupCalls')}",
        "",
        "## Quality deltas",
        "",
    ])
    deltas = result.get("deltas")
    if deltas is None:
        lines.append("Unavailable because the comparison is not comparable.")
    else:
        lines.extend([
            "| Metric | Model - heuristic |",
            "|---|---:|",
            *[f"| {name} | {_format_number(value)} |" for name, value in deltas.items()],
        ])
    lines.extend(["", "## Arm summaries", ""])
    for arm_id, summary in result.get("armSummaries", {}).items():
        retrieval = summary.get("retrievalLatencyMillis", {})
        rerank = summary.get("rerankLatencyMillis", {})
        lines.extend([
            f"### {arm_id}",
            "",
            f"- Recall@5: {_format_number(summary.get('recallAt5'))}",
            f"- MRR: {_format_number(summary.get('mrr'))}",
            f"- Top1 source accuracy: {_format_number(summary.get('top1SourceAccuracy'))}",
            f"- Effective provider counts: `{json.dumps(summary.get('effectiveProviderCounts', {}), sort_keys=True)}`",
            f"- Fallback reasons: `{json.dumps(summary.get('fallbackReasonHistogram', {}), sort_keys=True)}`",
            f"- Model eligible coverage: {_format_number(summary.get('eligibleModelCoverage'))}",
            f"- Retrieval latency count/P50/P95: {retrieval.get('count', 0)} / {retrieval.get('p50')} / {retrieval.get('p95')} ms",
            f"- Rerank latency count/P50/P95: {rerank.get('count', 0)} / {rerank.get('p50')} / {rerank.get('p95')} ms",
            "",
        ])
    budget = result.get("callBudget") or {}
    lines.extend([
        "## Call budget",
        "",
        f"- debug retrieval upper bound: {budget.get('debugRetrievalUpperBound', 0)}",
        f"- query embedding upper bound: {budget.get('queryEmbeddingUpperBound', 0)}",
        f"- model rerank upper bound: {budget.get('modelRerankUpperBound', 0)}",
        f"- ask / judge / generation: {budget.get('ask', 0)} / {budget.get('judge', 0)} / {budget.get('generation', 0)}",
        "",
    ])
    source_files = result.get("sourceFiles") or []
    if source_files:
        lines.extend(["## Source evidence", ""])
        for item in source_files:
            lines.append(
                f"- {item.get('role')} `{item.get('name')}`: sha256={item.get('sha256')}, bytes={item.get('bytes')}"
            )
        lines.append("")
    lines.extend([
        "## Scope boundary",
        "",
        "This is retrieval-only evidence. It does not establish generation, citation, no-answer, judge quality, production SLA, or a new default reranker.",
        "",
    ])
    return "\n".join(lines)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compare C7 reranker A/B details JSON files.")
    parser.add_argument("--heuristic-details", action="append", required=True)
    parser.add_argument("--model-details", action="append", required=True)
    parser.add_argument("--output-json", required=True)
    parser.add_argument("--output-markdown", required=True)
    parser.add_argument("--no-overwrite", action=argparse.BooleanOptionalAction, default=True)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    output_json = Path(args.output_json)
    output_markdown = Path(args.output_markdown)
    if args.no_overwrite and (output_json.exists() or output_markdown.exists()):
        raise ValueError("--no-overwrite refused to replace an existing comparison output")
    try:
        result = compare_runs(
            _load_details(args.heuristic_details),
            _load_details(args.model_details),
        )
        result["sourceFiles"] = (
            _source_files("heuristic", args.heuristic_details)
            + _source_files("model", args.model_details)
        )
    except ValueError:
        result = {
            "comparisonSchema": COMPARISON_SCHEMA,
            "comparisonStatus": "FAILED",
            "comparisonReasons": ["invalid_evidence_schema"],
            "strictIdentity": {},
            "armSummaries": {},
            "diagnostics": {"errorCategory": "invalid_evidence_schema"},
            "perSampleFacts": [],
            "perSampleSummaries": [],
            "callBudget": {},
            "sourceFiles": [],
            "deltas": None,
        }
        print("Comparison failed: errorCategory=invalid_evidence_schema", file=sys.stderr)
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_markdown.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    output_markdown.write_text(render_markdown(result), encoding="utf-8")
    print(f"Comparison status: {result['comparisonStatus']}")
    return 2 if result["comparisonStatus"] == "FAILED" else 0


if __name__ == "__main__":
    raise SystemExit(main())
