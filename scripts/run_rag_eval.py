#!/usr/bin/env python3
"""
Run a reproducible RAG baseline evaluation against the local backend.

The runner intentionally uses only Python standard library modules so it can run
without adding project dependencies.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import re
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import eval_dataset_contract as dataset_contract


DEFAULT_BASE_URL = "http://localhost:8080"
DEFAULT_EVAL_SET = Path("docs/eval/releases/rag-eval-dev-v2.jsonl")
DEFAULT_DATASET_MANIFEST = Path("docs/eval/dataset-manifest.json")
DEFAULT_REPORT = Path("tmp/eval/local-eval.md")
NO_ANSWER_CUES = (
    "没有找到",
    "未能找到",
    "没有相关",
    "没有足够",
    "无法回答",
    "无法确定",
    "知识库中没有",
    "文档中没有",
    "不包含",
    "未包含",
    "无法根据现有内容回答",
    "cannot answer",
    "no result",
    "not enough information",
)


@dataclass
class SampleResult:
    sample: dict[str, Any]
    retrieve_ok: bool
    ask_ok: bool
    retrieval_error: str | None
    ask_error: str | None
    recall3_hits: int
    recall5_hits: int
    recall_total: int
    first_match_rank: int | None
    top1_source_hit: bool | None
    keyword_hits: int
    keyword_total: int
    citation_hits: int
    citation_total: int
    citation_snippet_hits: int
    citation_snippet_total: int
    unsupported_citation_count: int
    no_answer_citation_violation_count: int
    no_answer_ok: bool | None
    faithfulness_score: float | None
    relevance_score: float | None
    judge_pass: bool | None
    judge_error: str | None
    judge_response: dict[str, Any] | None
    skipped_judge: bool
    debug_response: dict[str, Any] | None
    ask_response: dict[str, Any] | None
    details: dict[str, Any]
    skipped_ask: bool
    ask_attempts: int
    ask_retry_count: int
    rate_limit_errors: int


class ApiCallError(RuntimeError):
    def __init__(self, message: str, http_status: int | None = None):
        super().__init__(message)
        self.http_status = http_status


class AskRetryError(RuntimeError):
    def __init__(self, error: Exception, attempts: int, retries: int, rate_limit_errors: int):
        super().__init__(str(error))
        self.error = error
        self.attempts = attempts
        self.retries = retries
        self.rate_limit_errors = rate_limit_errors


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run RAG eval baseline.")
    parser.add_argument("--base-url", default=os.getenv("RAG_BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--eval-set", default=os.getenv("RAG_EVAL_SET", str(DEFAULT_EVAL_SET)))
    parser.add_argument(
        "--dataset-manifest",
        default=os.getenv("RAG_EVAL_DATASET_MANIFEST", str(DEFAULT_DATASET_MANIFEST)),
        help="Repo-relative versioned eval dataset manifest. Required for formal evidence.",
    )
    parser.add_argument(
        "--allow-unversioned-eval-set",
        action="store_true",
        help="Allow a custom eval set only as UNVERSIONED local diagnosis, never as formal baseline evidence.",
    )
    parser.add_argument("--report", default=os.getenv("RAG_EVAL_REPORT", str(DEFAULT_REPORT)))
    parser.add_argument("--after-report", default=os.getenv("RAG_EVAL_AFTER_REPORT", ""))
    parser.add_argument("--kb-id", type=int, default=parse_int_env("RAG_EVAL_KB_ID"))
    parser.add_argument("--username", default=os.getenv("RAG_EVAL_USERNAME", ""))
    parser.add_argument("--password", default=os.getenv("RAG_EVAL_PASSWORD", ""))
    parser.add_argument("--top-k", type=int, default=int(os.getenv("RAG_EVAL_TOP_K", "5")))
    parser.add_argument("--min-score", type=float, default=float(os.getenv("RAG_EVAL_MIN_SCORE", "0.3")))
    parser.add_argument("--enable-rerank", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--timeout", type=float, default=float(os.getenv("RAG_EVAL_TIMEOUT", "60")))
    parser.add_argument("--skip-ask", action="store_true", help="Only run debug retrieve metrics.")
    parser.add_argument("--ask-timeout", type=float, default=parse_float_env("RAG_EVAL_ASK_TIMEOUT"), help="Timeout for /api/qa/ask calls. Defaults to --timeout.")
    parser.add_argument("--ask-delay-seconds", type=float, default=float(os.getenv("RAG_EVAL_ASK_DELAY_SECONDS", "0")))
    parser.add_argument("--max-ask-retries", type=int, default=int(os.getenv("RAG_EVAL_MAX_ASK_RETRIES", "0")))
    parser.add_argument("--retry-backoff-seconds", type=float, default=float(os.getenv("RAG_EVAL_RETRY_BACKOFF_SECONDS", "0")))
    parser.add_argument("--retry-ask-timeouts", action=argparse.BooleanOptionalAction, default=parse_bool_env("RAG_EVAL_RETRY_ASK_TIMEOUTS", True), help="Retry /api/qa/ask timeout errors when --max-ask-retries is positive.")
    parser.add_argument("--fail-on-ask-errors", action="store_true")
    parser.add_argument("--no-overwrite", action="store_true")
    parser.add_argument("--details-json", default=os.getenv("RAG_EVAL_DETAILS_JSON", ""))
    parser.add_argument("--sample-id", action="append", dest="sample_ids", help="Run only the given eval sample id. Repeatable.")
    parser.add_argument("--sample-limit", type=int, default=int(os.getenv("RAG_EVAL_SAMPLE_LIMIT", "0")), help="Run only the first N selected samples. 0 means no limit.")
    parser.add_argument("--plan-only", action="store_true", help="Print selected samples and estimated live calls without logging in or calling backend/model APIs.")
    parser.add_argument("--judge-mode", choices=("off", "llm"), default=os.getenv("RAG_EVAL_JUDGE_MODE", "off"))
    parser.add_argument("--judge-base-url", default=os.getenv("RAG_EVAL_JUDGE_BASE_URL", os.getenv("OPENAI_BASE_URL", "https://integrate.api.nvidia.com/v1")))
    parser.add_argument("--judge-api-key", default=os.getenv("RAG_EVAL_JUDGE_API_KEY", os.getenv("NVIDIA_API_KEY", "")))
    parser.add_argument("--judge-model", default=os.getenv("RAG_EVAL_JUDGE_MODEL", ""))
    parser.add_argument("--judge-temperature", type=float, default=float(os.getenv("RAG_EVAL_JUDGE_TEMPERATURE", "0")))
    parser.add_argument("--judge-timeout", type=float, default=float(os.getenv("RAG_EVAL_JUDGE_TIMEOUT", "60")))
    parser.add_argument("--judge-max-context-chars", type=int, default=int(os.getenv("RAG_EVAL_JUDGE_MAX_CONTEXT_CHARS", "6000")))
    parser.add_argument("--fail-on-judge-errors", action="store_true")
    parser.add_argument(
        "--run-metadata-json",
        default=os.getenv("RAG_EVAL_RUN_METADATA_JSON", ""),
        help="Optional JSON metadata to include in the report header and details JSON.",
    )
    args = parser.parse_args()
    if args.ask_timeout is None:
        args.ask_timeout = args.timeout
    return args


def require_credentials(args: argparse.Namespace) -> None:
    if not str(args.username).strip() or not str(args.password).strip():
        raise RuntimeError(
            "RAG eval requires explicit credentials via --username/--password "
            "or RAG_EVAL_USERNAME/RAG_EVAL_PASSWORD."
        )


def parse_int_env(name: str) -> int | None:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return None
    return int(value)


def parse_float_env(name: str) -> float | None:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return None
    return float(value)


def parse_bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return default
    return value.strip().lower() not in {"0", "false", "no", "off"}


def load_eval_set(path: Path) -> list[dict[str, Any]]:
    samples: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as file:
        for line_no, line in enumerate(file, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                sample = json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(f"{path}:{line_no} is not valid JSON: {exc}") from exc
            samples.append(sample)
    return samples


def select_samples(samples: list[dict[str, Any]], sample_ids: list[str] | None, sample_limit: int) -> list[dict[str, Any]]:
    selected = samples
    if sample_ids:
        wanted = set(sample_ids)
        selected = [sample for sample in selected if str(sample.get("id")) in wanted]
    if sample_limit > 0:
        selected = selected[:sample_limit]
    return selected


def eval_plan(samples: list[dict[str, Any]], args: argparse.Namespace) -> dict[str, Any]:
    answerable = [sample for sample in samples if sample.get("should_answer", True)]
    no_answer = [sample for sample in samples if not sample.get("should_answer", True)]
    ask_calls = 0 if args.skip_ask else len(samples)
    judge_calls = 0
    if args.judge_mode == "llm" and not args.skip_ask:
        judge_calls = len(answerable)
    plan = {
        "sampleCount": len(samples),
        "answerableCount": len(answerable),
        "noAnswerCount": len(no_answer),
        "sampleIds": [sample.get("id") for sample in samples],
        "skipAsk": args.skip_ask,
        "judgeMode": args.judge_mode,
        "askTimeout": args.ask_timeout,
        "retryAskTimeouts": args.retry_ask_timeouts,
        "estimatedBackendCalls": {
            "debugRetrieve": len(samples),
            "ask": ask_calls,
            "llmJudge": judge_calls,
        },
        "outputs": {
            "report": args.report,
            "afterReport": args.after_report,
            "detailsJson": args.details_json,
        },
    }
    dataset_identity = getattr(args, "dataset_release_identity", None)
    if isinstance(dataset_identity, dict):
        plan["datasetReleaseIdentity"] = dataset_identity
    return plan


def validate_eval_dataset(args: argparse.Namespace) -> dict[str, Any]:
    repo_root = Path(__file__).resolve().parents[1]
    manifest_path = Path(args.dataset_manifest)
    release_identity = dataset_contract.validate_versioned_release(
        repo_root,
        manifest_path,
    )
    selected_manifest = (repo_root / manifest_path).resolve()
    default_manifest = (repo_root / DEFAULT_DATASET_MANIFEST).resolve()
    if selected_manifest != default_manifest and default_manifest.is_file():
        tracked_identity = dataset_contract.validate_versioned_release(
            repo_root,
            DEFAULT_DATASET_MANIFEST,
        )
        dataset_contract.ensure_release_version_consistent(release_identity, tracked_identity)
    expected_eval = (repo_root / release_identity["questionSet"]["path"]).resolve()
    actual_eval = Path(args.eval_set).resolve()
    if actual_eval == expected_eval:
        return release_identity
    if args.allow_unversioned_eval_set:
        return dataset_contract.validate_unversioned_eval_set(actual_eval)
    raise dataset_contract.DatasetContractError(
        "unversioned_eval_set",
        actual_eval.name or "custom-eval-set",
        "custom eval set requires --allow-unversioned-eval-set",
    )


def print_eval_plan(plan: dict[str, Any]) -> None:
    print(json.dumps(plan, ensure_ascii=False, indent=2))


def call_json(
    method: str,
    url: str,
    payload: dict[str, Any] | None,
    token: str | None,
    timeout: float,
) -> dict[str, Any]:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            text = response.read().decode("utf-8")
            return json.loads(text) if text else {}
    except urllib.error.HTTPError as exc:
        text = exc.read().decode("utf-8", errors="replace")
        raise ApiCallError(f"HTTP {exc.code} {url}: {text}", exc.code) from exc
    except urllib.error.URLError as exc:
        raise ApiCallError(f"Cannot connect to {url}: {exc.reason}") from exc


def api_data(response: dict[str, Any]) -> dict[str, Any]:
    if response.get("code") not in (None, 200):
        raise RuntimeError(f"API returned code={response.get('code')} message={response.get('message')}")
    data = response.get("data")
    return data if isinstance(data, dict) else {}


def fail_hint(error: str) -> str:
    lowered = error.lower()
    if "cannot connect" in lowered or "actively refused" in lowered or "connection refused" in lowered:
        return "后端未启动或端口不对。请先启动 MySQL/Redis/Milvus，再启动 RagQaApplication.main()。"
    if "401" in error or "403" in error or "bad credentials" in lowered:
        return "登录失败。请检查显式提供的 --username/--password 或对应环境变量。"
    if "404" in error and "knowledge" in lowered:
        return "知识库不存在。请先创建 eval 知识库，并用 --kb-id 指定返回的 id。"
    if "collection not found" in lowered or "index" in lowered:
        return "向量索引不存在或文档尚未完成索引。请上传 test-data 文档并等待状态变为 COMPLETED。"
    return "请查看后端日志，重点检查数据库连接、Redis、Milvus、NVIDIA_API_KEY/QWEN_API_KEY。"


def login(base_url: str, username: str, password: str, timeout: float) -> str:
    response = call_json(
        "POST",
        f"{base_url}/auth/login",
        {"username": username, "password": password},
        None,
        timeout,
    )
    data = api_data(response)
    token = data.get("accessToken")
    if not token:
        raise RuntimeError("Login response did not contain data.accessToken")
    return str(token)


def should_run_judge(args: argparse.Namespace, ask_evaluable: bool, should_answer: bool) -> bool:
    return args.judge_mode == "llm" and ask_evaluable and should_answer


def call_llm_judge(
    sample: dict[str, Any],
    answer: str,
    ask_response: dict[str, Any],
    args: argparse.Namespace,
) -> dict[str, Any]:
    if not args.judge_model:
        raise RuntimeError("LLM judge is enabled but --judge-model/RAG_EVAL_JUDGE_MODEL is empty")
    if not args.judge_api_key:
        raise RuntimeError("LLM judge is enabled but --judge-api-key/RAG_EVAL_JUDGE_API_KEY is empty")

    payload = {
        "model": args.judge_model,
        "temperature": args.judge_temperature,
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a strict RAG answer evaluator. Judge only from the provided retrieved contexts. "
                    "Return JSON only, with fields: faithfulnessScore, relevanceScore, pass, reason. "
                    "Scores must be numbers between 0 and 1. pass is true only when the answer is both faithful and relevant."
                ),
            },
            {
                "role": "user",
                "content": build_judge_prompt(sample, answer, ask_response, args.judge_max_context_chars),
            },
        ],
    }
    response = call_json(
        "POST",
        f"{args.judge_base_url.rstrip('/')}/chat/completions",
        payload,
        args.judge_api_key,
        args.judge_timeout,
    )
    content = extract_openai_message_content(response)
    parsed = parse_judge_content(content)
    parsed["rawContent"] = content
    return parsed


def build_judge_prompt(
    sample: dict[str, Any],
    answer: str,
    ask_response: dict[str, Any],
    max_context_chars: int,
) -> str:
    contexts = ask_response.get("contexts", []) if isinstance(ask_response, dict) else []
    context_lines: list[str] = []
    remaining = max(0, max_context_chars)
    for index, context in enumerate(contexts or [], start=1):
        source = context.get("source") or context.get("sourceFileName") or context.get("documentTitle") or ""
        content = str(context.get("content") or context.get("contentPreview") or "")
        if remaining <= 0:
            break
        clipped = content[:remaining]
        remaining -= len(clipped)
        context_lines.append(f"[{index}] source={source}\n{clipped}")

    expected_points = sample.get("expected_answer_points") or []
    expected_keywords = sample.get("expected_keywords") or []
    return "\n\n".join([
        f"Question:\n{sample.get('question', '')}",
        f"Expected answer points for reference, not as hidden context:\n{json.dumps(expected_points, ensure_ascii=False)}",
        f"Expected keywords for reference:\n{json.dumps(expected_keywords, ensure_ascii=False)}",
        "Retrieved contexts:\n" + ("\n\n".join(context_lines) if context_lines else "(none)"),
        f"Answer to judge:\n{answer}",
    ])


def extract_openai_message_content(response: dict[str, Any]) -> str:
    choices = response.get("choices")
    if not isinstance(choices, list) or not choices:
        raise RuntimeError("Judge response did not contain choices")
    message = choices[0].get("message") if isinstance(choices[0], dict) else None
    content = message.get("content") if isinstance(message, dict) else None
    if not isinstance(content, str) or not content.strip():
        raise RuntimeError("Judge response did not contain message.content")
    return content


def parse_judge_content(content: str) -> dict[str, Any]:
    payload = parse_json_object_from_text(content)
    faithfulness = clamp_score(first_present(payload, "faithfulnessScore", "faithfulness", "faithfulness_score"))
    relevance = clamp_score(first_present(payload, "relevanceScore", "relevance", "relevance_score"))
    passed = parse_bool(first_present(payload, "pass", "passed", "ok"))
    if passed is None and faithfulness is not None and relevance is not None:
        passed = faithfulness >= 0.7 and relevance >= 0.7
    return {
        "faithfulnessScore": faithfulness,
        "relevanceScore": relevance,
        "pass": passed,
        "reason": str(payload.get("reason") or payload.get("rationale") or "").strip(),
    }


def parse_json_object_from_text(text: str) -> dict[str, Any]:
    stripped = text.strip()
    if stripped.startswith("```"):
        stripped = re.sub(r"^```(?:json)?\s*", "", stripped, flags=re.IGNORECASE)
        stripped = re.sub(r"\s*```$", "", stripped)
    try:
        value = json.loads(stripped)
    except json.JSONDecodeError:
        start = stripped.find("{")
        end = stripped.rfind("}")
        if start < 0 or end <= start:
            raise RuntimeError("Judge response was not JSON") from None
        value = json.loads(stripped[start:end + 1])
    if not isinstance(value, dict):
        raise RuntimeError("Judge response JSON must be an object")
    return value


def first_present(payload: dict[str, Any], *keys: str) -> Any:
    for key in keys:
        if key in payload:
            return payload[key]
    return None


def clamp_score(value: Any) -> float | None:
    if value is None or value == "":
        return None
    try:
        score = float(value)
    except (TypeError, ValueError):
        return None
    return min(1.0, max(0.0, score))


def parse_bool(value: Any) -> bool | None:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        lowered = value.strip().lower()
        if lowered in {"true", "yes", "pass", "passed", "1"}:
            return True
        if lowered in {"false", "no", "fail", "failed", "0"}:
            return False
    return None


def extract_rerank_attribution(debug_response: dict[str, Any] | None) -> dict[str, Any]:
    diagnostics = debug_response.get("diagnostics", {}) if isinstance(debug_response, dict) else {}
    if not isinstance(diagnostics, dict):
        diagnostics = {}

    def integer(key: str) -> int:
        try:
            return max(0, int(diagnostics.get(key, 0)))
        except (TypeError, ValueError):
            return 0

    def number(key: str) -> float:
        try:
            value = float(diagnostics.get(key, 0.0))
            return value if math.isfinite(value) else 0.0
        except (TypeError, ValueError):
            return 0.0

    return {
        "requestedProvider": str(diagnostics.get("rerankRequestedProvider", "unknown")),
        "effectiveProvider": str(diagnostics.get("rerankEffectiveProvider", "unknown")),
        "fallbackCount": integer("rerankFallbackCount"),
        "fallbackReason": str(diagnostics.get("rerankFallbackReason", "unknown")),
        "modelCallCount": integer("rerankModelCallCount"),
        "candidateCount": integer("rerankCandidateCount"),
        "scoredCount": integer("rerankScoredCount"),
        "candidateCoverage": min(1.0, max(0.0, number("rerankCoverage"))),
        "latencyMillis": integer("rerankLatencyMillis"),
        "model": str(diagnostics.get("rerankModel", "")),
        "protocol": str(diagnostics.get("rerankProtocol", "unknown")),
    }


def run_sample(sample: dict[str, Any], args: argparse.Namespace, token: str) -> SampleResult:
    question = sample["question"]
    debug_response: dict[str, Any] | None = None
    ask_response: dict[str, Any] | None = None
    retrieval_error = None
    ask_error = None
    skipped_ask = bool(args.skip_ask)
    skipped_judge = True
    ask_attempts = 0
    ask_retry_count = 0
    rate_limit_errors = 0
    judge_error = None
    judge_response = None
    faithfulness_score = None
    relevance_score = None
    judge_pass = None

    retrieval_started_at = time.monotonic()
    try:
        debug_response = api_data(call_json(
            "POST",
            f"{args.base_url}/api/qa/debug/retrieve",
            {
                "kbId": args.kb_id,
                "question": question,
                "topK": max(args.top_k, 5),
                "minScore": args.min_score,
                "enableRerank": args.enable_rerank,
            },
            token,
            args.timeout,
        ))
    except Exception as exc:  # noqa: BLE001 - keep runner resilient per sample
        retrieval_error = str(exc)
    retrieve_latency_millis = max(0.0, (time.monotonic() - retrieval_started_at) * 1000.0)

    if not args.skip_ask:
        try:
            ask_response, ask_meta = call_ask_with_retries(question, args, token)
            ask_attempts = ask_meta["attempts"]
            ask_retry_count = ask_meta["retries"]
            rate_limit_errors = ask_meta["rateLimitErrors"]
        except Exception as exc:  # noqa: BLE001
            ask_error = str(exc)
            if isinstance(exc, AskRetryError):
                ask_attempts = exc.attempts
                ask_retry_count = exc.retries
                rate_limit_errors = exc.rate_limit_errors
            if isinstance(exc, ApiCallError) and exc.http_status == 429:
                rate_limit_errors += 1
            if "HTTP 429" in ask_error and not isinstance(exc, AskRetryError):
                rate_limit_errors += 1

    contexts = debug_response.get("contexts", []) if debug_response else []
    if retrieval_error is None and debug_response is not None:
        debug_status = debug_response.get("status")
        if debug_status not in (None, "", "ok"):
            retrieval_error = str(debug_response.get("message") or f"debug retrieve returned status={debug_status}")

    ask_answer = str(ask_response.get("answer", "")) if ask_response else ""
    citations = ask_response.get("citations", []) if ask_response else []
    should_answer = bool(sample.get("should_answer", True))
    ask_evaluable = not skipped_ask and ask_response is not None

    recall_total = expected_context_total(sample)
    recall3_hits = count_expected_matches(sample, contexts, 3)
    recall5_hits = count_expected_matches(sample, contexts, 5)
    first_rank = first_match_rank(sample, contexts)
    top1_hit = top1_source_hit(sample, contexts) if should_answer else None
    keyword_hits, keyword_total = count_keyword_hits(sample, ask_answer) if should_answer and ask_evaluable else (0, 0)
    citation_hits, citation_total = count_citation_hits(sample, citations) if should_answer and ask_evaluable else (0, 0)
    citation_snippet_hits, citation_snippet_total, unsupported_citation_count = count_citation_snippet_support(
        citations,
        ask_response.get("contexts", []) if ask_response else [],
    ) if ask_evaluable else (0, 0, 0)
    no_answer_citation_violation_count = 1 if (ask_evaluable and not should_answer and len(citations or []) > 0) else 0
    no_answer_ok = is_no_answer(ask_response, ask_answer) if not should_answer and ask_evaluable else None
    if should_run_judge(args, ask_evaluable, should_answer):
        skipped_judge = False
        try:
            judge_response = call_llm_judge(sample, ask_answer, ask_response or {}, args)
            faithfulness_score = judge_response.get("faithfulnessScore")
            relevance_score = judge_response.get("relevanceScore")
            judge_pass = judge_response.get("pass")
        except Exception as exc:  # noqa: BLE001 - judge must not poison objective metrics
            judge_error = str(exc)

    if retrieval_error is None and should_answer and not contexts:
        retrieval_error = "No contexts returned; knowledge base may not exist, documents may still be indexing, or minScore is too high."
    if ask_error is None and ask_response is not None and ask_response.get("metadata", {}).get("status") == "error":
        ask_error = f"QA returned error status: {ask_response.get('answer')}"

    normalized_sources = {
        "expected_sources": [
            {
                "raw": source,
                "normalized": sorted(normalized_source_forms(str(source))),
            }
            for source in sample.get("expected_sources") or []
        ],
        "retrieved_candidates": [
            {
                "rank": ctx.get("rank", index),
                "sourceCandidates": source_candidates(ctx),
                "normalizedCandidates": sorted(normalized_source_candidates(ctx)),
            }
            for index, ctx in enumerate(contexts or [], start=1)
        ],
        "citation_candidates": [
            {
                "sourceCandidates": source_candidates(citation),
                "normalizedCandidates": sorted(normalized_source_candidates(citation)),
            }
            for citation in citations or []
        ],
    }
    details = {
        "id": sample.get("id"),
        "question": question,
        "type": sample.get("type"),
        "should_answer": should_answer,
        "expected": {
            "sources": sample.get("expected_sources") or [],
            "contexts": sample.get("expected_contexts") or [],
            "keywords": sample.get("expected_keywords") or [],
            "answer_points": sample.get("expected_answer_points") or [],
        },
        "debugRetrieveRawResponse": debug_response,
        "retrieveLatencyMillis": round(retrieve_latency_millis, 3),
        "rerankAttribution": extract_rerank_attribution(debug_response),
        "normalizedSources": normalized_sources,
        "askRawResponse": ask_response,
        "judgeRawResponse": judge_response,
        "returnedCitations": citations or [],
        "metricCalculationDetails": {
            "retrieveHitRatio": f"{recall5_hits}/{recall_total}" if recall_total else "-",
            "recall3Hits": recall3_hits,
            "recall5Hits": recall5_hits,
            "recallTotal": recall_total,
            "firstMatchRank": first_rank,
            "top1SourceHit": top1_hit,
            "keywordHits": keyword_hits,
            "keywordTotal": keyword_total,
            "citationHits": citation_hits,
            "citationTotal": citation_total,
            "citationSnippetHits": citation_snippet_hits,
            "citationSnippetTotal": citation_snippet_total,
            "unsupportedCitationCount": unsupported_citation_count,
            "noAnswerCitationViolationCount": no_answer_citation_violation_count,
            "noAnswerOk": no_answer_ok,
            "faithfulnessScore": faithfulness_score,
            "relevanceScore": relevance_score,
            "judgePass": judge_pass,
            "judgeSkipped": skipped_judge,
            "judgeError": judge_error,
            "citationValidation": citation_validation_metadata(ask_response),
            "askSkipped": skipped_ask,
            "askAttempts": ask_attempts,
            "askRetries": ask_retry_count,
            "rateLimitErrors": rate_limit_errors,
        },
        "errors": {
            "retrieval": retrieval_error,
            "ask": ask_error,
        },
    }

    return SampleResult(
        sample=sample,
        retrieve_ok=retrieval_error is None,
        ask_ok=args.skip_ask or ask_error is None,
        retrieval_error=retrieval_error,
        ask_error=ask_error,
        recall3_hits=recall3_hits,
        recall5_hits=recall5_hits,
        recall_total=recall_total,
        first_match_rank=first_rank,
        top1_source_hit=top1_hit,
        keyword_hits=keyword_hits,
        keyword_total=keyword_total,
        citation_hits=citation_hits,
        citation_total=citation_total,
        citation_snippet_hits=citation_snippet_hits,
        citation_snippet_total=citation_snippet_total,
        unsupported_citation_count=unsupported_citation_count,
        no_answer_citation_violation_count=no_answer_citation_violation_count,
        no_answer_ok=no_answer_ok,
        faithfulness_score=faithfulness_score,
        relevance_score=relevance_score,
        judge_pass=judge_pass,
        judge_error=judge_error,
        judge_response=judge_response,
        skipped_judge=skipped_judge,
        debug_response=debug_response,
        ask_response=ask_response,
        details=details,
        skipped_ask=skipped_ask,
        ask_attempts=ask_attempts,
        ask_retry_count=ask_retry_count,
        rate_limit_errors=rate_limit_errors,
    )


def call_ask_with_retries(
    question: str,
    args: argparse.Namespace,
    token: str,
) -> tuple[dict[str, Any], dict[str, int]]:
    max_retries = max(0, args.max_ask_retries)
    attempts = 0
    retries = 0
    rate_limit_errors = 0
    last_error: Exception | None = None

    for attempt in range(max_retries + 1):
        attempts += 1
        if args.ask_delay_seconds > 0:
            time.sleep(args.ask_delay_seconds)
        try:
            response = api_data(call_json(
                "POST",
                f"{args.base_url}/api/qa/ask",
                {
                    "kbId": args.kb_id,
                    "question": question,
                    "topK": args.top_k,
                    "minScore": args.min_score,
                    "enableCache": False,
                },
                token,
                args.ask_timeout,
            ))
            metadata = response.get("metadata") if isinstance(response.get("metadata"), dict) else {}
            if metadata.get("status") == "error":
                message = str(response.get("answer") or metadata.get("message") or "QA returned error status")
                diagnostics = format_error_metadata(metadata)
                if diagnostics:
                    message = f"{message} [{diagnostics}]"
                raise ApiCallError(f"QA returned error status: {message}", infer_http_status(message))
            if args.ask_delay_seconds > 0:
                time.sleep(args.ask_delay_seconds)
            return response, {
                "attempts": attempts,
                "retries": retries,
                "rateLimitErrors": rate_limit_errors,
            }
        except Exception as exc:  # noqa: BLE001 - retry classification is explicit below
            last_error = exc
            if isinstance(exc, ApiCallError) and exc.http_status == 429:
                rate_limit_errors += 1
            if args.ask_delay_seconds > 0:
                time.sleep(args.ask_delay_seconds)
            if attempt >= max_retries or not should_retry_ask_error(exc, args.retry_ask_timeouts):
                break
            retries += 1
            backoff = retry_wait_seconds(args.retry_backoff_seconds, retries)
            if backoff > 0:
                time.sleep(backoff)

    if last_error is None:
        raise RuntimeError("ask failed without an exception")
    raise AskRetryError(last_error, attempts, retries, rate_limit_errors)


def should_retry_ask_error(error: Exception, retry_timeouts: bool = True) -> bool:
    if isinstance(error, ApiCallError):
        if error.http_status == 429:
            return True
        if error.http_status is not None:
            return 500 <= error.http_status <= 599
        return True
    text = str(error).lower()
    if "http 429" in text or "too many requests" in text:
        return True
    if "timeout" in text or "timed out" in text:
        return retry_timeouts
    return bool(re.search(r"http 5\d\d", text) or re.search(r"code=5\d\d", text))


def infer_http_status(text: str) -> int | None:
    match = re.search(r"\bHTTP\s+(\d{3})\b", text, re.IGNORECASE)
    if match:
        return int(match.group(1))
    if "too many requests" in text.lower() or "rate limit" in text.lower():
        return 429
    return None


def format_error_metadata(metadata: dict[str, Any]) -> str:
    fields = [
        ("errorCategory", "category"),
        ("errorType", "type"),
        ("llmProvider", "provider"),
        ("llmEndpoint", "endpoint"),
        ("llmModel", "model"),
        ("llmTimeoutSeconds", "timeoutSeconds"),
        ("llmMaxRetries", "maxRetries"),
        ("llmErrorType", "llmErrorType"),
        ("llmErrorCategory", "llmErrorCategory"),
        ("llmHttpStatus", "httpStatus"),
    ]
    parts: list[str] = []
    for key, label in fields:
        value = metadata.get(key)
        if value is None or value == "":
            continue
        parts.append(f"{label}={value}")
    return ", ".join(parts)


def retry_wait_seconds(base_seconds: float, retry_number: int) -> float:
    if base_seconds <= 0:
        return 0.0
    return base_seconds * retry_number


def expected_context_total(sample: dict[str, Any]) -> int:
    contexts = sample.get("expected_contexts") or []
    if contexts:
        return len(contexts)
    return len(sample.get("expected_sources") or [])


def count_expected_matches(sample: dict[str, Any], contexts: list[dict[str, Any]], k: int) -> int:
    expected_contexts = sample.get("expected_contexts") or []
    if expected_contexts:
        return sum(1 for expected in expected_contexts if any(context_matches(expected, ctx) for ctx in contexts[:k]))

    expected_sources = sample.get("expected_sources") or []
    return sum(1 for source in expected_sources if any(source_matches(source, ctx) for ctx in contexts[:k]))


def first_match_rank(sample: dict[str, Any], contexts: list[dict[str, Any]]) -> int | None:
    for index, ctx in enumerate(contexts, start=1):
        expected_contexts = sample.get("expected_contexts") or []
        if expected_contexts and any(context_matches(expected, ctx) for expected in expected_contexts):
            return index
        if not expected_contexts and any(source_matches(source, ctx) for source in sample.get("expected_sources") or []):
            return index
    return None


def top1_source_hit(sample: dict[str, Any], contexts: list[dict[str, Any]]) -> bool:
    if not contexts:
        return False
    expected_sources = sample.get("expected_sources") or []
    return any(source_matches(source, contexts[0]) for source in expected_sources)


def context_matches(expected: dict[str, Any], ctx: dict[str, Any]) -> bool:
    source = str(expected.get("source") or "")
    contains = str(expected.get("contains") or "")
    source_ok = True if not source else source_matches(source, ctx)
    contains_ok = True if not contains else contains_matches(contains, ctx)
    return source_ok and contains_ok


def source_matches(expected_source: str, ctx: dict[str, Any]) -> bool:
    expected_forms = normalized_source_forms(expected_source)
    if not expected_forms:
        return False
    return bool(expected_forms & normalized_source_candidates(ctx))


def contains_matches(expected_text: str, ctx: dict[str, Any]) -> bool:
    content_blob = normalize(" ".join([
        str(ctx.get("contentPreview") or ""),
        str(ctx.get("snippet") or ""),
    ]))
    return normalize(expected_text) in content_blob


def count_keyword_hits(sample: dict[str, Any], answer: str) -> tuple[int, int]:
    keywords = sample.get("expected_keywords") or []
    answer_norm = normalize(answer)
    hits = sum(1 for keyword in keywords if normalize(str(keyword)) in answer_norm)
    return hits, len(keywords)


def count_citation_hits(sample: dict[str, Any], citations: list[dict[str, Any]]) -> tuple[int, int]:
    expected_sources = sample.get("expected_sources") or []
    if not expected_sources:
        return 0, 0
    hits = sum(
        1
        for source in expected_sources
        if any(source_matches(str(source), citation) for citation in citations or [])
    )
    return hits, len(expected_sources)


def count_citation_snippet_support(
    citations: list[dict[str, Any]],
    contexts: list[dict[str, Any]],
) -> tuple[int, int, int]:
    if not citations:
        return 0, 0, 0
    hits = 0
    for citation in citations:
        if any(citation_supported_by_context(citation, context) for context in contexts or []):
            hits += 1
    total = len(citations)
    return hits, total, total - hits


def citation_supported_by_context(citation: dict[str, Any], context: dict[str, Any]) -> bool:
    if not citation_identity_matches(citation, context):
        return False
    snippet = str(citation.get("snippet") or "")
    content = str(context.get("content") or context.get("contentPreview") or context.get("snippet") or "")
    return text_contains_or_overlaps(snippet, content)


def citation_identity_matches(citation: dict[str, Any], context: dict[str, Any]) -> bool:
    citation_chunk_id = normalize(str(citation.get("chunkId") or ""))
    context_chunk_id = normalize(str(context.get("source") or context.get("chunkId") or ""))
    if citation_chunk_id:
        return citation_chunk_id == context_chunk_id

    citation_document_id = citation.get("documentId")
    context_document_id = (context.get("metadata") or {}).get("documentId")
    if citation_document_id is not None:
        return str(citation_document_id) == str(context_document_id)

    return bool(normalized_source_candidates(citation) & normalized_source_candidates(context))


def text_contains_or_overlaps(snippet: str, content: str) -> bool:
    snippet_norm = normalize(snippet)
    content_norm = normalize(content)
    if not snippet_norm or not content_norm:
        return False
    if snippet_norm in content_norm:
        return True
    snippet_tokens = token_set(snippet_norm)
    content_tokens = token_set(content_norm)
    if not snippet_tokens or not content_tokens:
        return False
    overlap = sum(1 for token in snippet_tokens if token in content_tokens)
    return overlap / len(snippet_tokens) >= 0.58


def token_set(text: str) -> set[str]:
    tokens: set[str] = set()
    current = []
    for ch in text:
        if ch.isascii() and ch.isalnum():
            current.append(ch.lower())
        else:
            if len(current) >= 2:
                tokens.add("".join(current))
            current = []
    if len(current) >= 2:
        tokens.add("".join(current))

    cjk_chars = [ch for ch in text if "\u4e00" <= ch <= "\u9fff"]
    if len(cjk_chars) == 1:
        tokens.add(cjk_chars[0])
    for index in range(len(cjk_chars) - 1):
        tokens.add("".join(cjk_chars[index:index + 2]))
    return tokens


def is_no_answer(ask_response: dict[str, Any] | None, answer: str) -> bool:
    if ask_response is None:
        return False
    metadata = ask_response.get("metadata") or {}
    if metadata.get("status") == "no_result":
        return True
    answer_norm = normalize(answer)
    return any(normalize(cue) in answer_norm for cue in NO_ANSWER_CUES)


def normalize(text: str) -> str:
    return " ".join(text.casefold().split())


def normalize_source(text: str) -> str:
    value = str(text or "").replace("\\", "/").strip().casefold()
    if not value:
        return ""
    value = value.rsplit("/", 1)[-1]
    value = re.sub(r"\.md$", "", value)
    value = re.sub(r"[-_\s]+", " ", value)
    value = re.sub(r"[^\w\u4e00-\u9fff. ]+", " ", value)
    return " ".join(value.split())


def normalized_source_forms(text: str) -> set[str]:
    normalized = normalize_source(text)
    if not normalized:
        return set()
    forms = {normalized}
    forms.add(normalized.replace(" ", ""))
    forms.add(normalized.replace(".", " "))
    compact = normalized.replace(".", " ")
    forms.add(" ".join(compact.split()))
    return {form for form in forms if form}


SOURCE_METADATA_KEYS = (
    "source",
    "sourceFileName",
    "displaySource",
    "documentTitle",
    "originalFilename",
    "originalFileName",
    "fileName",
    "filename",
    "title",
)


def source_candidates(item: dict[str, Any] | None) -> list[str]:
    if not isinstance(item, dict):
        return []
    candidates: list[str] = []
    for key in ("source", "sourceFileName", "displaySource", "documentTitle"):
        append_string_candidate(candidates, item.get(key))

    metadata = item.get("metadata") or {}
    if isinstance(metadata, dict):
        for key in SOURCE_METADATA_KEYS:
            append_string_candidate(candidates, metadata.get(key))

    deduped: list[str] = []
    seen = set()
    for candidate in candidates:
        if candidate not in seen:
            seen.add(candidate)
            deduped.append(candidate)
    return deduped


def append_string_candidate(candidates: list[str], value: Any) -> None:
    if value is None:
        return
    text = str(value).strip()
    if text:
        candidates.append(text)


def normalized_source_candidates(item: dict[str, Any] | None) -> set[str]:
    forms: set[str] = set()
    for candidate in source_candidates(item):
        forms.update(normalized_source_forms(candidate))
    return forms


def latency_summary(values: list[int | float]) -> dict[str, int | float | None]:
    ordered = sorted(
        float(value)
        for value in values
        if isinstance(value, (int, float)) and math.isfinite(float(value)) and float(value) >= 0
    )
    if not ordered:
        return {"count": 0, "min": None, "p50": None, "p95": None, "max": None}

    def nearest_rank(percentile: float) -> float:
        return ordered[max(0, math.ceil(percentile * len(ordered)) - 1)]

    return {
        "count": len(ordered),
        "min": ordered[0],
        "p50": nearest_rank(0.50),
        "p95": nearest_rank(0.95),
        "max": ordered[-1],
    }


def aggregate_rerank_attributions(attributions: list[dict[str, Any]]) -> dict[str, Any]:
    effective_counts: dict[str, int] = {}
    fallback_reasons: dict[str, int] = {}
    total_fallbacks = 0
    total_model_calls = 0
    total_candidates = 0
    total_scored = 0
    effective_model_samples = 0
    attributed_samples = 0
    latencies: list[int | float] = []

    for attribution in attributions:
        if not isinstance(attribution, dict):
            continue
        effective = str(attribution.get("effectiveProvider", "unknown"))
        if effective != "unknown":
            attributed_samples += 1
        effective_counts[effective] = effective_counts.get(effective, 0) + 1
        if effective not in {"heuristic", "disabled", "not_run", "unknown"}:
            effective_model_samples += 1

        fallback_count = max(0, int(attribution.get("fallbackCount", 0) or 0))
        total_fallbacks += fallback_count
        reason = str(attribution.get("fallbackReason", "unknown"))
        if fallback_count > 0:
            fallback_reasons[reason] = fallback_reasons.get(reason, 0) + fallback_count
        total_model_calls += max(0, int(attribution.get("modelCallCount", 0) or 0))
        total_candidates += max(0, int(attribution.get("candidateCount", 0) or 0))
        total_scored += max(0, int(attribution.get("scoredCount", 0) or 0))
        if "latencyMillis" in attribution:
            latencies.append(attribution["latencyMillis"])

    return {
        "attributedSamples": attributed_samples,
        "effectiveProviderCounts": effective_counts,
        "modelCoverage": ratio(effective_model_samples, attributed_samples),
        "fallbackCount": total_fallbacks,
        "fallbackReasonHistogram": fallback_reasons,
        "totalModelCalls": total_model_calls,
        "candidateCoverage": ratio(total_scored, total_candidates),
        "candidateCount": total_candidates,
        "scoredCount": total_scored,
        "latencyMillis": latency_summary(latencies),
    }


def aggregate(results: list[SampleResult]) -> dict[str, Any]:
    answerable = [result for result in results if result.sample.get("should_answer", True)]
    no_answer = [result for result in results if not result.sample.get("should_answer", True)]
    ask_success = [
        result
        for result in results
        if not result.skipped_ask and result.ask_error is None and result.ask_response is not None
    ]
    answerable_ask_success = [result for result in ask_success if result.sample.get("should_answer", True)]
    no_answer_ask_success = [result for result in ask_success if not result.sample.get("should_answer", True)]
    judge_evaluable = [
        result for result in answerable_ask_success
        if not result.skipped_judge and result.judge_error is None and result.judge_pass is not None
    ]
    ask_skipped = sum(1 for result in results if result.skipped_ask)

    recall_total = sum(result.recall_total for result in answerable)
    recall3_hits = sum(result.recall3_hits for result in answerable)
    recall5_hits = sum(result.recall5_hits for result in answerable)
    reciprocal_ranks = [
        1.0 / result.first_match_rank
        for result in answerable
        if result.first_match_rank is not None and result.first_match_rank > 0
    ]
    top1_values = [result.top1_source_hit for result in answerable if result.top1_source_hit is not None]
    keyword_hits = sum(result.keyword_hits for result in answerable_ask_success)
    keyword_total = sum(result.keyword_total for result in answerable_ask_success)
    citation_hits = sum(result.citation_hits for result in answerable_ask_success)
    citation_total = sum(result.citation_total for result in answerable_ask_success)
    citation_snippet_hits = sum(result.citation_snippet_hits for result in ask_success)
    citation_snippet_total = sum(result.citation_snippet_total for result in ask_success)
    unsupported_citation_count = sum(result.unsupported_citation_count for result in ask_success)
    no_answer_citation_violation_count = sum(result.no_answer_citation_violation_count for result in no_answer_ask_success)
    no_answer_values = [result.no_answer_ok for result in no_answer_ask_success if result.no_answer_ok is not None]
    judge_pass_values = [result.judge_pass for result in judge_evaluable if result.judge_pass is not None]
    faithfulness_scores = [
        result.faithfulness_score for result in judge_evaluable if result.faithfulness_score is not None
    ]
    relevance_scores = [
        result.relevance_score for result in judge_evaluable if result.relevance_score is not None
    ]
    generation_skipped = ask_skipped == len(results) if results else False
    judge_skipped = sum(1 for result in results if result.skipped_judge) == len(results) if results else False
    no_answer_ok_count = sum(1 for value in no_answer_values if value)
    rerank_attribution = aggregate_rerank_attributions([
        result.details.get("rerankAttribution", {}) for result in results
    ])
    retrieval_latency = latency_summary([
        result.details.get("retrieveLatencyMillis")
        for result in results
        if "retrieveLatencyMillis" in result.details
    ])

    return {
        "samples": len(results),
        "answerable_samples": len(answerable),
        "no_answer_samples": len(no_answer),
        "recall_at_3": ratio(recall3_hits, recall_total),
        "recall_at_5": ratio(recall5_hits, recall_total),
        "mrr": sum(reciprocal_ranks) / len(answerable) if answerable else 0.0,
        "top1_source_accuracy": ratio(sum(1 for value in top1_values if value), len(top1_values)),
        "answer_keyword_hit_rate": metric_ratio(keyword_hits, keyword_total, generation_skipped),
        "citation_hit_rate": metric_ratio(citation_hits, citation_total, generation_skipped),
        "citation_source_hit_rate": metric_ratio(citation_hits, citation_total, generation_skipped),
        "citation_snippet_hit_rate": metric_ratio(citation_snippet_hits, citation_snippet_total, generation_skipped),
        "unsupported_citation_count": unsupported_citation_count,
        "no_answer_citation_violation_count": no_answer_citation_violation_count,
        "no_answer_accuracy": metric_ratio(no_answer_ok_count, len(no_answer_values), generation_skipped),
        "judge_pass_rate": metric_ratio(sum(1 for value in judge_pass_values if value), len(judge_pass_values), judge_skipped),
        "faithfulness_avg": metric_average(faithfulness_scores, judge_skipped),
        "relevance_avg": metric_average(relevance_scores, judge_skipped),
        "ask_success_samples": len(ask_success),
        "answerable_ask_success_samples": len(answerable_ask_success),
        "no_answer_ask_success_samples": len(no_answer_ask_success),
        "judge_evaluable_samples": len(judge_evaluable),
        "answer_keyword_hits": keyword_hits,
        "answer_keyword_total": keyword_total,
        "citation_source_hits": citation_hits,
        "citation_source_total": citation_total,
        "citation_snippet_hits": citation_snippet_hits,
        "citation_snippet_total": citation_snippet_total,
        "no_answer_ok_count": no_answer_ok_count,
        "no_answer_evaluable_total": len(no_answer_values),
        "judge_pass_count": sum(1 for value in judge_pass_values if value),
        "judge_pass_total": len(judge_pass_values),
        "rerank_attribution": rerank_attribution,
        "retrieval_latency_millis": retrieval_latency,
    }


def ratio(numerator: int | float, denominator: int | float) -> float:
    return 0.0 if denominator == 0 else float(numerator) / float(denominator)


def metric_ratio(numerator: int, denominator: int, skipped: bool) -> float | str:
    if skipped:
        return "skipped"
    if denominator == 0:
        return "N/A"
    return ratio(numerator, denominator)


def metric_average(values: list[float], skipped: bool) -> float | str:
    if skipped:
        return "skipped"
    if not values:
        return "N/A"
    return sum(values) / len(values)


def pct(value: float | int | str) -> str:
    if isinstance(value, str):
        return value
    return f"{float(value) * 100:.2f}%"


def latency_fact_rows(metrics: dict[str, Any]) -> list[str]:
    retrieval = metrics.get("retrieval_latency_millis") or {}
    rerank = (metrics.get("rerank_attribution") or {}).get("latencyMillis") or {}

    def row(label: str, summary: dict[str, Any]) -> str:
        return (
            f"| {label} | count={summary.get('count', 0)}, min={summary.get('min')}, "
            f"P50={summary.get('p50')}, P95={summary.get('p95')}, max={summary.get('max')} ms |"
        )

    return [
        row("Client-observed debug retrieval latency", retrieval),
        row("Server-side rerank stage latency", rerank),
    ]


def load_run_metadata(path_value: str) -> dict[str, Any]:
    if not path_value:
        return {}
    path = Path(path_value)
    with path.open("r", encoding="utf-8") as file:
        value = json.load(file)
    if not isinstance(value, dict):
        raise ValueError(f"Run metadata must be a JSON object: {path}")
    return value


def bind_dataset_identity(
    run_metadata: dict[str, Any],
    dataset_identity: dict[str, Any],
) -> dict[str, Any]:
    bound = dict(run_metadata)
    existing_identity = bound.get("datasetReleaseIdentity")
    existing_status = bound.get("datasetValidation")
    current_status = dataset_identity.get("validationStatus")
    if existing_identity is not None and existing_identity != dataset_identity:
        raise dataset_contract.DatasetContractError(
            "release_identity_mismatch",
            str(dataset_identity.get("manifestPath") or "custom-eval-set"),
            "run metadata dataset identity differs from local validation",
        )
    if existing_status is not None and existing_status != current_status:
        raise dataset_contract.DatasetContractError(
            "release_identity_mismatch",
            str(dataset_identity.get("manifestPath") or "custom-eval-set"),
            "run metadata dataset validation status differs from local validation",
        )
    bound["datasetReleaseIdentity"] = dataset_identity
    bound["datasetValidation"] = current_status
    return bound


def run_counts(results: list[SampleResult]) -> dict[str, int]:
    return {
        "askErrors": sum(1 for result in results if result.ask_error is not None),
        "retrieveErrors": sum(1 for result in results if result.retrieval_error is not None),
        "skippedAsk": sum(1 for result in results if result.skipped_ask),
        "judgeErrors": sum(1 for result in results if result.judge_error is not None),
        "skippedJudge": sum(1 for result in results if result.skipped_judge),
        "rateLimitErrors": sum(result.rate_limit_errors for result in results),
        "retryCount": sum(result.ask_retry_count for result in results),
    }


def report_status(args: argparse.Namespace, results: list[SampleResult], login_error: str | None) -> str:
    if login_error:
        return "FAILED"
    counts = run_counts(results)
    retrieve_errors = counts["retrieveErrors"]
    if results and retrieve_errors >= max(1, len(results) // 2):
        return "FAILED"
    if args.skip_ask:
        return "RETRIEVAL_ONLY"
    if retrieve_errors == 0 and counts["askErrors"] == 0:
        return "CLEAN"
    return "PARTIAL"


def metrics_safe_for_comparison(
    status: str,
    counts: dict[str, int],
    dataset_validation: str = "VALID",
) -> str:
    if dataset_validation != "VALID":
        return f"no; dataset is {dataset_validation or 'UNVALIDATED'}"
    if status == "CLEAN":
        return "yes"
    if status == "RETRIEVAL_ONLY":
        return "retrieval metrics only" if counts["retrieveErrors"] == 0 else "no"
    if status == "PARTIAL" and counts["retrieveErrors"] == 0:
        return "retrieval metrics only; generation/citation metrics are partial"
    return "no"


def write_report(
    path: Path,
    args: argparse.Namespace,
    samples: list[dict[str, Any]],
    results: list[SampleResult],
    login_error: str | None,
    started_at: float,
    run_metadata: dict[str, Any],
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    finished = time.time()
    status = report_status(args, results, login_error)
    counts = run_counts(results)
    dataset_validation = str(run_metadata.get("datasetValidation") or "UNVALIDATED")
    lines: list[str] = []
    lines.append("# RAG Eval Report")
    lines.append("")
    lines.append(f"- Generated at: {datetime.now(timezone.utc).isoformat()}")
    lines.append(f"- Report status: `{status}`")
    lines.append(f"- askErrors count: `{counts['askErrors']}`")
    lines.append(f"- retrieveErrors count: `{counts['retrieveErrors']}`")
    lines.append(f"- skippedAsk count: `{counts['skippedAsk']}`")
    lines.append(f"- judgeErrors count: `{counts['judgeErrors']}`")
    lines.append(f"- skippedJudge count: `{counts['skippedJudge']}`")
    lines.append(f"- rateLimitErrors count: `{counts['rateLimitErrors']}`")
    lines.append(f"- retry count: `{counts['retryCount']}`")
    lines.append(
        f"- Metrics safe for comparison: "
        f"`{metrics_safe_for_comparison(status, counts, dataset_validation)}`"
    )
    lines.append(f"- Base URL: `{args.base_url}`")
    lines.append(f"- Knowledge base ID: `{args.kb_id}`")
    lines.append(f"- Eval set: `{args.eval_set}`")
    lines.append(f"- sampleIds: `{','.join(args.sample_ids or [])}`")
    lines.append(f"- sampleLimit: `{args.sample_limit}`")
    lines.append(f"- topK: `{args.top_k}`")
    lines.append(f"- minScore: `{args.min_score}`")
    lines.append(f"- enableRerank: `{args.enable_rerank}`")
    lines.append(f"- skipAsk: `{args.skip_ask}`")
    lines.append(f"- judgeMode: `{args.judge_mode}`")
    lines.append(f"- judgeModel: `{args.judge_model or ''}`")
    lines.append(f"- judgeBaseUrl: `{args.judge_base_url}`")
    lines.append(f"- judgeTemperature: `{args.judge_temperature}`")
    if run_metadata:
        append_header_metadata(lines, run_metadata)
    lines.append(f"- askTimeout: `{args.ask_timeout}`")
    lines.append(f"- askDelaySeconds: `{args.ask_delay_seconds}`")
    lines.append(f"- maxAskRetries: `{args.max_ask_retries}`")
    lines.append(f"- retryBackoffSeconds: `{args.retry_backoff_seconds}`")
    lines.append(f"- retryAskTimeouts: `{args.retry_ask_timeouts}`")
    lines.append(f"- Duration: `{finished - started_at:.2f}s`")
    lines.append("")

    if login_error:
        lines.append("## Run Status")
        lines.append("")
        lines.append("Backend login failed, so no live metrics were collected.")
        lines.append("")
        lines.append(f"```text\n{login_error}\n```")
        lines.append("")
        lines.append(f"Diagnosis: {fail_hint(login_error)}")
        lines.append("")
        lines.append("The eval set and runner are still ready to use after the backend, database, Redis, Milvus, and model credentials are available.")
        lines.append("")
        append_case_section(lines, "Failed Retrieval Cases", [])
        append_case_section(lines, "Failed Citation Cases", [])
        append_case_section(lines, "Low Answer Keyword Hit Cases", [])
        append_case_section(lines, "No-answer Cases", [])
        append_source_normalization_diagnostics(lines, [])
        append_citation_diagnostics(lines, [])
        path.write_text("\n".join(lines), encoding="utf-8")
        return

    metrics = aggregate(results)
    lines.append("## Summary Metrics")
    lines.append("")
    lines.append("| Metric | Value |")
    lines.append("|---|---:|")
    lines.append(f"| Samples | {metrics['samples']} |")
    lines.append(f"| Answerable samples | {metrics['answerable_samples']} |")
    lines.append(f"| No-answer samples | {metrics['no_answer_samples']} |")
    lines.append(f"| Recall@3 | {pct(metrics['recall_at_3'])} |")
    lines.append(f"| Recall@5 | {pct(metrics['recall_at_5'])} |")
    lines.append(f"| MRR | {float(metrics['mrr']):.4f} |")
    lines.append(f"| Top1 source accuracy | {pct(metrics['top1_source_accuracy'])} |")
    lines.append(f"| Ask successful samples | {metrics['ask_success_samples']} |")
    lines.append(f"| Answerable ask successful samples | {metrics['answerable_ask_success_samples']} |")
    lines.append(f"| No-answer ask successful samples | {metrics['no_answer_ask_success_samples']} |")
    lines.append(f"| Answer keyword hit rate on successful ask samples | {pct(metrics['answer_keyword_hit_rate'])} ({metrics['answer_keyword_hits']}/{metrics['answer_keyword_total']}) |")
    lines.append(f"| Citation hit rate on successful ask samples | {pct(metrics['citation_hit_rate'])} ({metrics['citation_source_hits']}/{metrics['citation_source_total']}) |")
    lines.append(f"| Citation source hit rate on successful ask samples | {pct(metrics['citation_source_hit_rate'])} ({metrics['citation_source_hits']}/{metrics['citation_source_total']}) |")
    lines.append(f"| Citation snippet hit rate on successful ask samples | {pct(metrics['citation_snippet_hit_rate'])} ({metrics['citation_snippet_hits']}/{metrics['citation_snippet_total']}) |")
    lines.append(f"| Unsupported citation count | {metrics['unsupported_citation_count']} |")
    lines.append(f"| No-answer citation violation count | {metrics['no_answer_citation_violation_count']} |")
    lines.append(f"| No-answer accuracy on successful ask samples | {pct(metrics['no_answer_accuracy'])} ({metrics['no_answer_ok_count']}/{metrics['no_answer_evaluable_total']}) |")
    lines.append(f"| Judge evaluable samples | {metrics['judge_evaluable_samples']} |")
    lines.append(f"| LLM judge pass rate | {pct(metrics['judge_pass_rate'])} ({metrics['judge_pass_count']}/{metrics['judge_pass_total']}) |")
    lines.append(f"| Faithfulness average | {pct(metrics['faithfulness_avg'])} |")
    lines.append(f"| Relevance average | {pct(metrics['relevance_avg'])} |")
    lines.append("")

    rerank = metrics["rerank_attribution"]
    lines.append("## Rerank Attribution")
    lines.append("")
    lines.append("| Fact | Value |")
    lines.append("|---|---:|")
    lines.append(f"| Attributed samples | {rerank['attributedSamples']} |")
    lines.append(f"| Effective provider counts | `{json.dumps(rerank['effectiveProviderCounts'], sort_keys=True)}` |")
    lines.append(f"| Effective model coverage | {pct(rerank['modelCoverage'])} |")
    lines.append(f"| Fallback count | {rerank['fallbackCount']} |")
    lines.append(f"| Fallback reason histogram | `{json.dumps(rerank['fallbackReasonHistogram'], sort_keys=True)}` |")
    lines.append(f"| Total model calls | {rerank['totalModelCalls']} |")
    lines.append(f"| Candidate coverage | {pct(rerank['candidateCoverage'])} ({rerank['scoredCount']}/{rerank['candidateCount']}) |")
    lines.append("")

    lines.append("## Latency")
    lines.append("")
    lines.append("| Scope | Observation distribution |")
    lines.append("|---|---|")
    lines.extend(latency_fact_rows(metrics))
    lines.append("")
    lines.append("Rerank latency is the server-side rerank stage measurement; retrieval latency is client-observed debug retrieval wall-clock time. Both use nearest-rank percentiles over measured observations.")
    lines.append("")

    lines.append("## Sample Results")
    lines.append("")
    lines.append("| ID | Type | Retrieve | First Match | Ask | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Judge | Errors |")
    lines.append("|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|---|")
    for result in results:
        retrieve = f"{result.recall5_hits}/{result.recall_total}" if result.recall_total else "-"
        first = result.first_match_rank if result.first_match_rank is not None else "-"
        ask_state = "skipped" if result.skipped_ask else ("ok" if result.ask_error is None else "error")
        keyword = "skipped" if result.skipped_ask else (f"{result.keyword_hits}/{result.keyword_total}" if result.keyword_total else "-")
        citation = "skipped" if result.skipped_ask else (f"{result.citation_hits}/{result.citation_total}" if result.citation_total else "-")
        citation_snippet = "skipped" if result.skipped_ask else (f"{result.citation_snippet_hits}/{result.citation_snippet_total}" if result.citation_snippet_total else "-")
        no_answer = format_bool(result.no_answer_ok)
        judge = "skipped" if result.skipped_judge else ("error" if result.judge_error else format_bool(result.judge_pass))
        errors = "; ".join(error for error in [result.retrieval_error, result.ask_error, result.judge_error] if error) or ""
        lines.append(
            f"| {result.sample.get('id')} | {result.sample.get('type')} | {retrieve} | {first} | {ask_state} | "
            f"{keyword} | {citation} | {citation_snippet} | {result.unsupported_citation_count} | "
            f"{no_answer} | {judge} | {escape_table(errors)} |"
        )
    lines.append("")

    lines.append("## Field Coverage")
    lines.append("")
    lines.append("- `debug/retrieve` is used for Recall@3, Recall@5, MRR, and Top1 source accuracy.")
    lines.append("- `ask` is used for answer keyword hit rate, citation hit rate, and no-answer accuracy.")
    lines.append("- `LLM judge` is optional and only runs when `--judge-mode llm` is explicitly enabled with judge credentials.")
    lines.append("- When `--skip-ask` is enabled, generation/citation/no-answer metrics are marked as skipped instead of being counted as zero.")
    lines.append("- When ask errors occur, generation/citation/no-answer metrics are calculated only on successful ask samples and the report status becomes PARTIAL.")
    lines.append("- When judge is disabled or unavailable, faithfulness/relevance metrics are marked as skipped or partial; objective citation/no-answer metrics remain reportable.")
    lines.append("- `queryVariants`, `rank`, `score`, `source`, `documentId`, `chunkId`, `contentPreview`, and `metadata` are expected in debug output.")
    lines.append("")

    lines.append("## Current Limitations")
    lines.append("")
    lines.append("- `contentPreview` is a preview, not the full chunk, so long expected snippets may undercount recall.")
    lines.append("- Citation source hit rate checks expected source names in returned citations.")
    lines.append("- Citation snippet hit rate verifies each returned citation against the `contexts` returned by `/api/qa/ask` using exact match or token overlap.")
    lines.append("- Answer keyword scoring is lexical; optional LLM judge metrics are reported separately when explicitly enabled.")
    lines.append("- Metrics assume the three `test-data/*.md` files were uploaded with recognizable file names or document titles.")
    lines.append("")

    append_failed_retrieval_cases(lines, results)
    append_failed_citation_cases(lines, results)
    append_low_keyword_cases(lines, results)
    append_no_answer_cases(lines, results)
    append_source_normalization_diagnostics(lines, results)
    append_citation_diagnostics(lines, results)

    path.write_text("\n".join(lines), encoding="utf-8")


def append_header_metadata(lines: list[str], metadata: dict[str, Any]) -> None:
    dataset_identity = metadata.get("datasetReleaseIdentity")
    if isinstance(dataset_identity, dict):
        lines.append(f"- Dataset validation: `{dataset_identity.get('validationStatus', '')}`")
        lines.append(f"- Dataset release: `{dataset_identity.get('releaseVersion', '')}`")
        lines.append(f"- Dataset manifest SHA-256: `{dataset_identity.get('manifestSha256', '')}`")
        question_set = dataset_identity.get("questionSet")
        if isinstance(question_set, dict):
            lines.append(f"- Dataset sample count: `{question_set.get('sampleCount', '')}`")
            lines.append(f"- Dataset question SHA-256: `{question_set.get('sha256', '')}`")
    kb = metadata.get("knowledgeBase")
    if isinstance(kb, dict):
        lines.append(f"- Eval KB name: `{kb.get('name', '')}`")
        lines.append(f"- Eval KB vector collection: `{kb.get('vectorCollection', '')}`")
        lines.append(f"- Eval KB document count: `{kb.get('documentCount', '')}`")
        lines.append(f"- Eval KB chunk count: `{kb.get('chunkCount', '')}`")
    fixtures = metadata.get("fixtures")
    if isinstance(fixtures, list) and fixtures:
        fixture_bits = []
        for fixture in fixtures:
            if not isinstance(fixture, dict):
                continue
            fixture_bits.append(f"{fixture.get('name')} sha256={fixture.get('sha256')}")
        if fixture_bits:
            lines.append(f"- Fixture files: `{'; '.join(fixture_bits)}`")
    config = metadata.get("configSnapshot")
    if isinstance(config, dict):
        snapshot_bits = []
        for name, item in config.items():
            if isinstance(item, dict):
                snapshot_bits.append(f"{name} sha256={item.get('sha256')}")
        if snapshot_bits:
            lines.append(f"- Config snapshot: `{'; '.join(snapshot_bits)}`")
    git = metadata.get("git")
    if isinstance(git, dict):
        lines.append(f"- Git HEAD: `{git.get('head', '')}`")


def append_failed_retrieval_cases(lines: list[str], results: list[SampleResult]) -> None:
    cases = [
        result
        for result in results
        if result.sample.get("should_answer", True)
        and result.recall_total > 0
        and result.recall5_hits < result.recall_total
    ]
    append_case_section(lines, "Failed Retrieval Cases", cases)


def append_failed_citation_cases(lines: list[str], results: list[SampleResult]) -> None:
    cases = [
        result
        for result in results
        if result.sample.get("should_answer", True)
        and result.citation_total > 0
        and result.citation_hits < result.citation_total
    ]
    append_case_section(lines, "Failed Citation Cases", cases)


def append_low_keyword_cases(lines: list[str], results: list[SampleResult]) -> None:
    cases = [
        result
        for result in results
        if result.sample.get("should_answer", True)
        and result.keyword_total > 0
        and ratio(result.keyword_hits, result.keyword_total) < 0.8
    ]
    append_case_section(lines, "Low Answer Keyword Hit Cases", cases)


def append_no_answer_cases(lines: list[str], results: list[SampleResult]) -> None:
    cases = [result for result in results if not result.sample.get("should_answer", True)]
    append_case_section(lines, "No-answer Cases", cases)


def append_source_normalization_diagnostics(lines: list[str], results: list[SampleResult]) -> None:
    lines.append("## Source Normalization Diagnostics")
    lines.append("")
    lines.append("| ID | Expected normalized | Retrieved candidate normalized | Citation candidate normalized |")
    lines.append("|---|---|---|---|")
    for result in results:
        normalized = result.details.get("normalizedSources", {})
        expected = [
            f"{item.get('raw')} => {', '.join(item.get('normalized') or [])}"
            for item in normalized.get("expected_sources") or []
        ]
        retrieved = [
            f"r{item.get('rank')}: {', '.join(item.get('normalizedCandidates') or [])}"
            for item in (normalized.get("retrieved_candidates") or [])[:5]
        ]
        citations = [
            ", ".join(item.get("normalizedCandidates") or [])
            for item in normalized.get("citation_candidates") or []
        ]
        lines.append(
            f"| {result.sample.get('id')} | {escape_table('; '.join(expected))} | "
            f"{escape_table('; '.join(retrieved))} | {escape_table('; '.join(citations))} |"
        )
    lines.append("")


def append_citation_diagnostics(lines: list[str], results: list[SampleResult]) -> None:
    lines.append("## Citation Diagnostics")
    lines.append("")
    lines.append("| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |")
    lines.append("|---|---:|---:|---:|---|---:|")
    for result in results:
        citations = result.ask_response.get("citations", []) if result.ask_response else []
        validation = citation_validation_metadata(result.ask_response)
        validation_text = (
            f"valid={validation.get('validCitations')}, "
            f"dropped={validation.get('droppedCitations')}, "
            f"coverage={validation.get('citationCoverage')}"
        )
        source_hit = f"{result.citation_hits}/{result.citation_total}" if result.citation_total else "-"
        snippet_hit = (
            f"{result.citation_snippet_hits}/{result.citation_snippet_total}"
            if result.citation_snippet_total else "-"
        )
        lines.append(
            f"| {result.sample.get('id')} | {len(citations or [])} | {source_hit} | {snippet_hit} | "
            f"{escape_table(validation_text)} | {result.unsupported_citation_count} |"
        )
    lines.append("")


def append_case_section(lines: list[str], title: str, cases: list[SampleResult]) -> None:
    lines.append(f"## {title}")
    lines.append("")
    if not cases:
        lines.append("No cases.")
        lines.append("")
        return

    for result in cases:
        append_case(lines, result)


def append_case(lines: list[str], result: SampleResult) -> None:
    sample = result.sample
    ask_response = result.ask_response or {}
    contexts = result.debug_response.get("contexts", []) if result.debug_response else []
    citations = ask_response.get("citations", []) or []
    validation = citation_validation_metadata(ask_response)

    lines.append(f"### {sample.get('id')} ({sample.get('type')})")
    lines.append("")
    lines.append(f"- id: `{sample.get('id')}`")
    lines.append(f"- type: `{sample.get('type')}`")
    lines.append(f"- question: {sample.get('question')}")
    lines.append(f"- expected_sources: `{json.dumps(sample.get('expected_sources') or [], ensure_ascii=False)}`")
    contains = [item.get("contains") for item in sample.get("expected_contexts") or []]
    lines.append(f"- expected_contexts.contains: `{json.dumps(contains, ensure_ascii=False)}`")
    lines.append(f"- retrieve hit ratio: `{result.recall5_hits}/{result.recall_total}`")
    lines.append(f"- first_match_rank: `{result.first_match_rank if result.first_match_rank is not None else '-'}`")
    answer = str(ask_response.get("answer", ""))
    lines.append("- answer:" if answer == "" else f"- answer: {answer}")
    lines.append(f"- expected_keywords: `{json.dumps(sample.get('expected_keywords') or [], ensure_ascii=False)}`")
    lines.append(f"- keyword_hit: `{result.keyword_hits}/{result.keyword_total}`")
    lines.append("- top5 retrieved results:")
    append_json_block(lines, [format_retrieved_context(ctx, index) for index, ctx in enumerate(contexts[:5], start=1)])
    lines.append("- returned citations:")
    append_json_block(lines, [format_citation(citation) for citation in citations])
    lines.append("- citation validation metadata:")
    append_json_block(lines, {
        "validCitations": validation.get("validCitations"),
        "droppedCitations": validation.get("droppedCitations"),
        "citationCoverage": validation.get("citationCoverage"),
        "unsupportedCitationCount": result.unsupported_citation_count,
    })
    if result.retrieval_error or result.ask_error:
        lines.append(f"- errors: `{escape_table('; '.join(error for error in [result.retrieval_error, result.ask_error] if error))}`")
    lines.append("")


def format_retrieved_context(ctx: dict[str, Any], fallback_rank: int) -> dict[str, Any]:
    metadata = ctx.get("metadata") if isinstance(ctx.get("metadata"), dict) else {}
    return {
        "rank": ctx.get("rank", fallback_rank),
        "score": ctx.get("score"),
        "source": ctx.get("source"),
        "sourceFileName": ctx.get("sourceFileName") or metadata.get("sourceFileName") or metadata.get("fileName"),
        "documentTitle": ctx.get("documentTitle") or metadata.get("documentTitle") or metadata.get("title"),
        "documentId": ctx.get("documentId") or metadata.get("documentId"),
        "chunkId": ctx.get("chunkId") or metadata.get("chunkId"),
        "contentPreview": ctx.get("contentPreview") or ctx.get("snippet"),
        "metadata": metadata,
    }


def format_citation(citation: dict[str, Any]) -> dict[str, Any]:
    return {
        "source": citation.get("source"),
        "sourceFileName": citation.get("sourceFileName"),
        "documentTitle": citation.get("documentTitle"),
        "documentId": citation.get("documentId"),
        "chunkId": citation.get("chunkId"),
        "score": citation.get("score"),
        "snippet": citation.get("snippet"),
    }


def append_json_block(lines: list[str], value: Any) -> None:
    lines.append("```json")
    lines.append(json.dumps(value, ensure_ascii=False, indent=2))
    lines.append("```")


def citation_validation_metadata(ask_response: dict[str, Any] | None) -> dict[str, Any]:
    metadata = ask_response.get("metadata", {}) if ask_response else {}
    validation = metadata.get("citationValidation") if isinstance(metadata, dict) else {}
    if not isinstance(validation, dict):
        validation = {}
    return {
        "validCitations": metadata.get("validCitations", validation.get("validCitations")) if isinstance(metadata, dict) else validation.get("validCitations"),
        "droppedCitations": metadata.get("droppedCitations", validation.get("droppedCitations")) if isinstance(metadata, dict) else validation.get("droppedCitations"),
        "citationCoverage": metadata.get("citationCoverage", validation.get("citationCoverage")) if isinstance(metadata, dict) else validation.get("citationCoverage"),
    }


def format_bool(value: bool | None) -> str:
    if value is None:
        return "-"
    return "yes" if value else "no"


def escape_table(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")


SENSITIVE_KEY_PATTERN = re.compile(r"(token|password|secret|api[_-]?key|authorization)", re.IGNORECASE)


def sanitize_sensitive(value: Any) -> Any:
    if isinstance(value, dict):
        sanitized: dict[str, Any] = {}
        for key, item in value.items():
            key_text = str(key)
            if SENSITIVE_KEY_PATTERN.search(key_text):
                sanitized[key_text] = "[REDACTED]"
            else:
                sanitized[key_text] = sanitize_sensitive(item)
        return sanitized
    if isinstance(value, list):
        return [sanitize_sensitive(item) for item in value]
    return value


def write_details_json(
    path: Path,
    args: argparse.Namespace,
    samples: list[dict[str, Any]],
    results: list[SampleResult],
    login_error: str | None,
    started_at: float,
    run_metadata: dict[str, Any],
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    status = report_status(args, results, login_error)
    counts = run_counts(results)
    dataset_identity = run_metadata.get("datasetReleaseIdentity")
    dataset_validation = str(run_metadata.get("datasetValidation") or "UNVALIDATED")
    payload = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "reportStatus": status,
        "runCounts": counts,
        "metricsSafeForComparison": metrics_safe_for_comparison(
            status,
            counts,
            dataset_validation,
        ),
        "datasetValidation": dataset_validation,
        "datasetReleaseIdentity": dataset_identity,
        "baseUrl": args.base_url,
        "kbId": args.kb_id,
        "evalSet": args.eval_set,
        "sampleIds": args.sample_ids or [],
        "sampleLimit": args.sample_limit,
        "topK": args.top_k,
        "minScore": args.min_score,
        "enableRerank": args.enable_rerank,
        "skipAsk": args.skip_ask,
        "askTimeout": args.ask_timeout,
        "judge": {
            "mode": args.judge_mode,
            "baseUrl": args.judge_base_url,
            "model": args.judge_model,
            "temperature": args.judge_temperature,
            "timeout": args.judge_timeout,
            "maxContextChars": args.judge_max_context_chars,
        },
        "askDelaySeconds": args.ask_delay_seconds,
        "maxAskRetries": args.max_ask_retries,
        "retryBackoffSeconds": args.retry_backoff_seconds,
        "retryAskTimeouts": args.retry_ask_timeouts,
        "durationSeconds": round(time.time() - started_at, 4),
        "runMetadata": run_metadata,
        "metrics": aggregate(results) if not login_error else None,
        "loginError": login_error,
        "sampleCount": len(samples),
        "samples": [result.details for result in results],
    }
    path.write_text(json.dumps(sanitize_sensitive(payload), ensure_ascii=False, indent=2), encoding="utf-8")


def ensure_no_overwrite(paths: list[Path]) -> bool:
    existing = [path for path in paths if path.exists()]
    if not existing:
        return True
    print("--no-overwrite refused to overwrite existing output file(s):", file=sys.stderr)
    for path in existing:
        print(f"- {path}", file=sys.stderr)
    return False


def main() -> int:
    started_at = time.time()
    args = parse_args()
    args.base_url = args.base_url.rstrip("/")
    try:
        args.dataset_release_identity = validate_eval_dataset(args)
    except dataset_contract.DatasetContractError as exc:
        print(
            f"Dataset validation failed: errorCode={exc.code} artifact={exc.artifact}",
            file=sys.stderr,
        )
        return 2
    eval_path = Path(args.eval_set)
    report_path = Path(args.report)
    after_report_path = Path(args.after_report) if args.after_report else None
    details_json_path = Path(args.details_json) if args.details_json else None
    try:
        run_metadata = bind_dataset_identity(
            load_run_metadata(args.run_metadata_json),
            args.dataset_release_identity,
        )
    except dataset_contract.DatasetContractError as exc:
        print(
            f"Dataset validation failed: errorCode={exc.code} artifact={exc.artifact}",
            file=sys.stderr,
        )
        return 2

    if args.no_overwrite:
        output_paths = [report_path]
        if after_report_path is not None:
            output_paths.append(after_report_path)
        if details_json_path is not None:
            output_paths.append(details_json_path)
        if not ensure_no_overwrite(output_paths):
            return 2

    if args.kb_id is None:
        print("Missing --kb-id or RAG_EVAL_KB_ID. Create an eval knowledge base first, then pass its id.", file=sys.stderr)
        return 2

    all_samples = load_eval_set(eval_path)
    samples = select_samples(all_samples, args.sample_ids, args.sample_limit)
    if not samples:
        print("No eval samples selected. Check --sample-id/--sample-limit.", file=sys.stderr)
        return 2
    if args.plan_only:
        print_eval_plan(eval_plan(samples, args))
        return 0
    try:
        require_credentials(args)
    except RuntimeError as exc:
        print(str(exc), file=sys.stderr)
        return 2
    token = ""
    login_error = None
    results: list[SampleResult] = []

    try:
        token = login(args.base_url, args.username, args.password, args.timeout)
    except Exception as exc:  # noqa: BLE001
        login_error = str(exc)

    if not login_error:
        for index, sample in enumerate(samples, start=1):
            print(f"[{index}/{len(samples)}] sampleId={sample['id']}")
            results.append(run_sample(sample, args, token))

    write_report(report_path, args, samples, results, login_error, started_at, run_metadata)
    if after_report_path is not None:
        write_report(after_report_path, args, samples, results, login_error, started_at, run_metadata)
    if details_json_path is not None:
        write_details_json(details_json_path, args, samples, results, login_error, started_at, run_metadata)

    if login_error:
        print("Eval could not run: login or backend request failed", file=sys.stderr)
        print(f"Hint: {fail_hint(login_error)}", file=sys.stderr)
        print(f"Wrote report: {report_path}")
        if after_report_path is not None:
            print(f"Wrote report: {after_report_path}")
        if details_json_path is not None:
            print(f"Wrote details JSON: {details_json_path}")
        return 1

    metrics = aggregate(results)
    status = report_status(args, results, login_error)
    counts = run_counts(results)
    print("\nRAG eval complete")
    print(f"Report status: {status}")
    print(f"askErrors: {counts['askErrors']}")
    print(f"retrieveErrors: {counts['retrieveErrors']}")
    print(f"skippedAsk: {counts['skippedAsk']}")
    print(f"judgeErrors: {counts['judgeErrors']}")
    print(f"skippedJudge: {counts['skippedJudge']}")
    print(f"rateLimitErrors: {counts['rateLimitErrors']}")
    print(f"retry count: {counts['retryCount']}")
    print(
        "Metrics safe for comparison: "
        f"{metrics_safe_for_comparison(status, counts, str(run_metadata.get('datasetValidation')))}"
    )
    print(f"Recall@3: {pct(metrics['recall_at_3'])}")
    print(f"Recall@5: {pct(metrics['recall_at_5'])}")
    print(f"MRR: {float(metrics['mrr']):.4f}")
    print(f"Top1 source accuracy: {pct(metrics['top1_source_accuracy'])}")
    print(f"Answer keyword hit rate on successful ask samples: {pct(metrics['answer_keyword_hit_rate'])}")
    print(f"Citation hit rate on successful ask samples: {pct(metrics['citation_hit_rate'])}")
    print(f"Citation source hit rate on successful ask samples: {pct(metrics['citation_source_hit_rate'])}")
    print(f"Citation snippet hit rate on successful ask samples: {pct(metrics['citation_snippet_hit_rate'])}")
    print(f"Unsupported citation count: {metrics['unsupported_citation_count']}")
    print(f"No-answer citation violation count: {metrics['no_answer_citation_violation_count']}")
    print(f"No-answer accuracy: {pct(metrics['no_answer_accuracy'])}")
    print(f"LLM judge pass rate: {pct(metrics['judge_pass_rate'])}")
    print(f"Faithfulness average: {pct(metrics['faithfulness_avg'])}")
    print(f"Relevance average: {pct(metrics['relevance_avg'])}")
    print(f"Wrote report: {report_path}")
    if after_report_path is not None:
        print(f"Wrote report: {after_report_path}")
    if details_json_path is not None:
        print(f"Wrote details JSON: {details_json_path}")
    if args.fail_on_ask_errors and counts["askErrors"] > 0:
        print("--fail-on-ask-errors enabled and askErrors > 0.", file=sys.stderr)
        return 1
    if args.fail_on_judge_errors and counts["judgeErrors"] > 0:
        print("--fail-on-judge-errors enabled and judgeErrors > 0.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
