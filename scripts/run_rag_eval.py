#!/usr/bin/env python3
"""
Run a reproducible RAG baseline evaluation against the local backend.

The runner intentionally uses only Python standard library modules so it can run
without adding project dependencies.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = "http://localhost:8080"
DEFAULT_EVAL_SET = Path("docs/eval/rag_eval_set.jsonl")
DEFAULT_REPORT = Path("docs/eval/reports/baseline-002-local.md")
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
    debug_response: dict[str, Any] | None
    ask_response: dict[str, Any] | None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run RAG eval baseline.")
    parser.add_argument("--base-url", default=os.getenv("RAG_BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--eval-set", default=os.getenv("RAG_EVAL_SET", str(DEFAULT_EVAL_SET)))
    parser.add_argument("--report", default=os.getenv("RAG_EVAL_REPORT", str(DEFAULT_REPORT)))
    parser.add_argument("--after-report", default=os.getenv("RAG_EVAL_AFTER_REPORT", ""))
    parser.add_argument("--kb-id", type=int, default=parse_int_env("RAG_EVAL_KB_ID"))
    parser.add_argument("--username", default=os.getenv("RAG_EVAL_USERNAME", "admin"))
    parser.add_argument("--password", default=os.getenv("RAG_EVAL_PASSWORD", "admin123"))
    parser.add_argument("--top-k", type=int, default=int(os.getenv("RAG_EVAL_TOP_K", "5")))
    parser.add_argument("--min-score", type=float, default=float(os.getenv("RAG_EVAL_MIN_SCORE", "0.3")))
    parser.add_argument("--enable-rerank", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--timeout", type=float, default=float(os.getenv("RAG_EVAL_TIMEOUT", "60")))
    parser.add_argument("--skip-ask", action="store_true", help="Only run debug retrieve metrics.")
    return parser.parse_args()


def parse_int_env(name: str) -> int | None:
    value = os.getenv(name)
    if value is None or value.strip() == "":
        return None
    return int(value)


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
        raise RuntimeError(f"HTTP {exc.code} {url}: {text}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Cannot connect to {url}: {exc.reason}") from exc


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
        return "登录失败。请检查 --username/--password，默认账号通常是 admin/admin123。"
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


def run_sample(sample: dict[str, Any], args: argparse.Namespace, token: str) -> SampleResult:
    question = sample["question"]
    debug_response: dict[str, Any] | None = None
    ask_response: dict[str, Any] | None = None
    retrieval_error = None
    ask_error = None

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

    if not args.skip_ask:
        try:
            ask_response = api_data(call_json(
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
                args.timeout,
            ))
        except Exception as exc:  # noqa: BLE001
            ask_error = str(exc)

    contexts = debug_response.get("contexts", []) if debug_response else []
    if retrieval_error is None and debug_response is not None:
        debug_status = debug_response.get("status")
        if debug_status not in (None, "", "ok"):
            retrieval_error = str(debug_response.get("message") or f"debug retrieve returned status={debug_status}")

    ask_answer = str(ask_response.get("answer", "")) if ask_response else ""
    citations = ask_response.get("citations", []) if ask_response else []
    should_answer = bool(sample.get("should_answer", True))

    recall_total = expected_context_total(sample)
    recall3_hits = count_expected_matches(sample, contexts, 3)
    recall5_hits = count_expected_matches(sample, contexts, 5)
    first_rank = first_match_rank(sample, contexts)
    top1_hit = top1_source_hit(sample, contexts) if should_answer else None
    keyword_hits, keyword_total = count_keyword_hits(sample, ask_answer) if should_answer else (0, 0)
    citation_hits, citation_total = count_citation_hits(sample, citations) if should_answer else (0, 0)
    citation_snippet_hits, citation_snippet_total, unsupported_citation_count = count_citation_snippet_support(
        citations,
        ask_response.get("contexts", []) if ask_response else [],
    )
    no_answer_citation_violation_count = 1 if (not should_answer and len(citations or []) > 0) else 0
    no_answer_ok = is_no_answer(ask_response, ask_answer) if not should_answer and ask_response is not None else None

    if retrieval_error is None and should_answer and not contexts:
        retrieval_error = "No contexts returned; knowledge base may not exist, documents may still be indexing, or minScore is too high."
    if ask_error is None and ask_response is not None and ask_response.get("metadata", {}).get("status") == "error":
        ask_error = f"QA returned error status: {ask_response.get('answer')}"

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
        debug_response=debug_response,
        ask_response=ask_response,
    )


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
    source_blob = normalize(" ".join([
        str(ctx.get("source") or ""),
        str(ctx.get("displaySource") or ""),
        str(ctx.get("chunkId") or ""),
        json.dumps(ctx.get("metadata") or {}, ensure_ascii=False),
    ]))
    return normalize(expected_source) in source_blob


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
    citation_blob = normalize(json.dumps(citations or [], ensure_ascii=False))
    hits = sum(1 for source in expected_sources if normalize(str(source)) in citation_blob)
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

    citation_source = normalize(str(citation.get("source") or ""))
    context_source = normalize(str(context.get("source") or ""))
    return bool(citation_source and citation_source == context_source)


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


def aggregate(results: list[SampleResult]) -> dict[str, float | int | str]:
    answerable = [result for result in results if result.sample.get("should_answer", True)]
    no_answer = [result for result in results if not result.sample.get("should_answer", True)]

    recall_total = sum(result.recall_total for result in answerable)
    recall3_hits = sum(result.recall3_hits for result in answerable)
    recall5_hits = sum(result.recall5_hits for result in answerable)
    reciprocal_ranks = [
        1.0 / result.first_match_rank
        for result in answerable
        if result.first_match_rank is not None and result.first_match_rank > 0
    ]
    top1_values = [result.top1_source_hit for result in answerable if result.top1_source_hit is not None]
    keyword_hits = sum(result.keyword_hits for result in answerable)
    keyword_total = sum(result.keyword_total for result in answerable)
    citation_hits = sum(result.citation_hits for result in answerable)
    citation_total = sum(result.citation_total for result in answerable)
    citation_snippet_hits = sum(result.citation_snippet_hits for result in results)
    citation_snippet_total = sum(result.citation_snippet_total for result in results)
    unsupported_citation_count = sum(result.unsupported_citation_count for result in results)
    no_answer_citation_violation_count = sum(result.no_answer_citation_violation_count for result in results)
    no_answer_values = [result.no_answer_ok for result in no_answer if result.no_answer_ok is not None]

    return {
        "samples": len(results),
        "answerable_samples": len(answerable),
        "no_answer_samples": len(no_answer),
        "recall_at_3": ratio(recall3_hits, recall_total),
        "recall_at_5": ratio(recall5_hits, recall_total),
        "mrr": sum(reciprocal_ranks) / len(answerable) if answerable else 0.0,
        "top1_source_accuracy": ratio(sum(1 for value in top1_values if value), len(top1_values)),
        "answer_keyword_hit_rate": ratio(keyword_hits, keyword_total),
        "citation_hit_rate": ratio(citation_hits, citation_total),
        "citation_source_hit_rate": ratio(citation_hits, citation_total),
        "citation_snippet_hit_rate": ratio(citation_snippet_hits, citation_snippet_total),
        "unsupported_citation_count": unsupported_citation_count,
        "no_answer_citation_violation_count": no_answer_citation_violation_count,
        "no_answer_accuracy": ratio(sum(1 for value in no_answer_values if value), len(no_answer_values)),
    }


def ratio(numerator: int | float, denominator: int | float) -> float:
    return 0.0 if denominator == 0 else float(numerator) / float(denominator)


def pct(value: float | int | str) -> str:
    if isinstance(value, str):
        return value
    return f"{float(value) * 100:.2f}%"


def write_report(
    path: Path,
    args: argparse.Namespace,
    samples: list[dict[str, Any]],
    results: list[SampleResult],
    login_error: str | None,
    started_at: float,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    finished = time.time()
    lines: list[str] = []
    lines.append("# RAG Eval Baseline 002 Local")
    lines.append("")
    lines.append(f"- Generated at: {datetime.now(timezone.utc).isoformat()}")
    lines.append(f"- Base URL: `{args.base_url}`")
    lines.append(f"- Knowledge base ID: `{args.kb_id}`")
    lines.append(f"- Eval set: `{args.eval_set}`")
    lines.append(f"- topK: `{args.top_k}`")
    lines.append(f"- minScore: `{args.min_score}`")
    lines.append(f"- enableRerank: `{args.enable_rerank}`")
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
    lines.append(f"| Answer keyword hit rate | {pct(metrics['answer_keyword_hit_rate'])} |")
    lines.append(f"| Citation hit rate | {pct(metrics['citation_hit_rate'])} |")
    lines.append(f"| Citation source hit rate | {pct(metrics['citation_source_hit_rate'])} |")
    lines.append(f"| Citation snippet hit rate | {pct(metrics['citation_snippet_hit_rate'])} |")
    lines.append(f"| Unsupported citation count | {metrics['unsupported_citation_count']} |")
    lines.append(f"| No-answer citation violation count | {metrics['no_answer_citation_violation_count']} |")
    lines.append(f"| No-answer accuracy | {pct(metrics['no_answer_accuracy'])} |")
    lines.append("")

    lines.append("## Sample Results")
    lines.append("")
    lines.append("| ID | Type | Retrieve | First Match | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Errors |")
    lines.append("|---|---|---:|---:|---:|---:|---:|---:|---:|---|")
    for result in results:
        retrieve = f"{result.recall5_hits}/{result.recall_total}" if result.recall_total else "-"
        first = result.first_match_rank if result.first_match_rank is not None else "-"
        keyword = f"{result.keyword_hits}/{result.keyword_total}" if result.keyword_total else "-"
        citation = f"{result.citation_hits}/{result.citation_total}" if result.citation_total else "-"
        citation_snippet = f"{result.citation_snippet_hits}/{result.citation_snippet_total}" if result.citation_snippet_total else "-"
        no_answer = format_bool(result.no_answer_ok)
        errors = "; ".join(error for error in [result.retrieval_error, result.ask_error] if error) or ""
        lines.append(
            f"| {result.sample.get('id')} | {result.sample.get('type')} | {retrieve} | {first} | "
            f"{keyword} | {citation} | {citation_snippet} | {result.unsupported_citation_count} | "
            f"{no_answer} | {escape_table(errors)} |"
        )
    lines.append("")

    lines.append("## Field Coverage")
    lines.append("")
    lines.append("- `debug/retrieve` is used for Recall@3, Recall@5, MRR, and Top1 source accuracy.")
    lines.append("- `ask` is used for answer keyword hit rate, citation hit rate, and no-answer accuracy.")
    lines.append("- `queryVariants`, `rank`, `score`, `source`, `documentId`, `chunkId`, `contentPreview`, and `metadata` are expected in debug output.")
    lines.append("")

    lines.append("## Current Limitations")
    lines.append("")
    lines.append("- `contentPreview` is a preview, not the full chunk, so long expected snippets may undercount recall.")
    lines.append("- Citation source hit rate checks expected source names in returned citations.")
    lines.append("- Citation snippet hit rate verifies each returned citation against the `contexts` returned by `/api/qa/ask` using exact match or token overlap.")
    lines.append("- Answer scoring is keyword based and does not use an LLM judge.")
    lines.append("- Metrics assume the three `test-data/*.md` files were uploaded with recognizable file names or document titles.")
    lines.append("")

    path.write_text("\n".join(lines), encoding="utf-8")


def format_bool(value: bool | None) -> str:
    if value is None:
        return "-"
    return "yes" if value else "no"


def escape_table(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")


def main() -> int:
    started_at = time.time()
    args = parse_args()
    args.base_url = args.base_url.rstrip("/")
    eval_path = Path(args.eval_set)
    report_path = Path(args.report)
    after_report_path = Path(args.after_report) if args.after_report else None

    if args.kb_id is None:
        print("Missing --kb-id or RAG_EVAL_KB_ID. Create an eval knowledge base first, then pass its id.", file=sys.stderr)
        return 2

    samples = load_eval_set(eval_path)
    token = ""
    login_error = None
    results: list[SampleResult] = []

    try:
        token = login(args.base_url, args.username, args.password, args.timeout)
    except Exception as exc:  # noqa: BLE001
        login_error = str(exc)

    if not login_error:
        for index, sample in enumerate(samples, start=1):
            print(f"[{index}/{len(samples)}] {sample['id']} {sample['question']}")
            results.append(run_sample(sample, args, token))

    write_report(report_path, args, samples, results, login_error, started_at)
    if after_report_path is not None:
        write_report(after_report_path, args, samples, results, login_error, started_at)

    if login_error:
        print(f"Eval could not run: {login_error}", file=sys.stderr)
        print(f"Hint: {fail_hint(login_error)}", file=sys.stderr)
        print(f"Wrote report: {report_path}")
        if after_report_path is not None:
            print(f"Wrote report: {after_report_path}")
        return 1

    metrics = aggregate(results)
    print("\nRAG eval complete")
    print(f"Recall@3: {pct(metrics['recall_at_3'])}")
    print(f"Recall@5: {pct(metrics['recall_at_5'])}")
    print(f"MRR: {float(metrics['mrr']):.4f}")
    print(f"Top1 source accuracy: {pct(metrics['top1_source_accuracy'])}")
    print(f"Answer keyword hit rate: {pct(metrics['answer_keyword_hit_rate'])}")
    print(f"Citation hit rate: {pct(metrics['citation_hit_rate'])}")
    print(f"Citation source hit rate: {pct(metrics['citation_source_hit_rate'])}")
    print(f"Citation snippet hit rate: {pct(metrics['citation_snippet_hit_rate'])}")
    print(f"Unsupported citation count: {metrics['unsupported_citation_count']}")
    print(f"No-answer citation violation count: {metrics['no_answer_citation_violation_count']}")
    print(f"No-answer accuracy: {pct(metrics['no_answer_accuracy'])}")
    print(f"Wrote report: {report_path}")
    if after_report_path is not None:
        print(f"Wrote report: {after_report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
