#!/usr/bin/env python3
"""Versioned, secret-free identity for the shared RAG judge contract."""

from __future__ import annotations

import hashlib
import json
import math
import re
from typing import Any
from urllib.parse import urlsplit


JUDGE_SYSTEM_PROMPT = (
    "You are a strict RAG answer evaluator. Judge only from the provided retrieved contexts. "
    "Return JSON only, with fields: faithfulnessScore, relevanceScore, pass, reason. "
    "Scores must be numbers between 0 and 1. pass is true only when the answer is both faithful and relevant."
)
JUDGE_CONTRACT_VERSION = "rag-judge-v1"
JUDGE_PROMPT_VERSION = "rag-judge-prompt-v1"
JUDGE_PARSER_VERSION = "strict-json-scores-v1"
JUDGE_SCORE_THRESHOLD = 0.70
JUDGE_JOINT_PASS_RULE = "both-scores-gte-threshold-v1"


class JudgePayloadError(ValueError):
    pass


def contract_config(args: Any) -> dict[str, Any]:
    endpoint = urlsplit(str(getattr(args, "judge_base_url", "")))
    endpoint_identity = f"{endpoint.scheme}://{endpoint.netloc}" if endpoint.scheme and endpoint.netloc else ""
    return {
        "judgeContractVersion": JUDGE_CONTRACT_VERSION,
        "promptVersion": JUDGE_PROMPT_VERSION,
        "promptSha256": hashlib.sha256(JUDGE_SYSTEM_PROMPT.encode("utf-8")).hexdigest(),
        "parserVersion": JUDGE_PARSER_VERSION,
        "faithfulnessThreshold": JUDGE_SCORE_THRESHOLD,
        "relevanceThreshold": JUDGE_SCORE_THRESHOLD,
        "jointPassRule": JUDGE_JOINT_PASS_RULE,
        "maxContextChars": int(getattr(args, "judge_max_context_chars", 6000)),
        "model": str(getattr(args, "judge_model", "")),
        "temperature": float(getattr(args, "judge_temperature", 0.0)),
        "endpointIdentity": endpoint_identity,
    }


def build_judge_prompt(
    *,
    question: str,
    answer: str,
    contexts: list[dict[str, Any]],
    expected_points: list[Any] | None = None,
    expected_keywords: list[Any] | None = None,
    max_context_chars: int = 6000,
) -> str:
    context_lines: list[str] = []
    remaining = max(0, int(max_context_chars))
    for index, context in enumerate(contexts or [], start=1):
        source = context.get("source") or context.get("sourceFileName") or context.get("documentTitle") or ""
        content = str(context.get("content") or context.get("contentPreview") or "")
        if remaining <= 0:
            break
        clipped = content[:remaining]
        remaining -= len(clipped)
        context_lines.append(f"[{index}] source={source}\n{clipped}")
    return "\n\n".join([
        f"Question:\n{question}",
        "Expected answer points for reference, not as hidden context:\n"
        + json.dumps(expected_points or [], ensure_ascii=False),
        "Expected keywords for reference:\n"
        + json.dumps(expected_keywords or [], ensure_ascii=False),
        "Retrieved contexts:\n" + ("\n\n".join(context_lines) if context_lines else "(none)"),
        f"Answer to judge:\n{answer}",
    ])


def parse_judge_content(content: str) -> dict[str, Any]:
    payload = _parse_json_object(content)
    faithfulness = _strict_score(payload.get("faithfulnessScore"))
    relevance = _strict_score(payload.get("relevanceScore"))
    provider_pass = payload.get("pass")
    if provider_pass is not None and not isinstance(provider_pass, bool):
        raise JudgePayloadError("invalid_judge_payload")
    passed = faithfulness >= JUDGE_SCORE_THRESHOLD and relevance >= JUDGE_SCORE_THRESHOLD
    return {
        "faithfulnessScore": faithfulness,
        "relevanceScore": relevance,
        "pass": passed,
        "providerReportedPass": provider_pass,
        "providerPassMismatch": provider_pass is not None and provider_pass != passed,
        "reason": str(payload.get("reason") or "").strip(),
    }


def _strict_score(value: Any) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise JudgePayloadError("invalid_judge_payload")
    score = float(value)
    if not math.isfinite(score) or score < 0.0 or score > 1.0:
        raise JudgePayloadError("invalid_judge_payload")
    return score


def _parse_json_object(text: str) -> dict[str, Any]:
    stripped = text.strip()
    if stripped.startswith("```"):
        stripped = re.sub(r"^```(?:json)?\s*", "", stripped, flags=re.IGNORECASE)
        stripped = re.sub(r"\s*```$", "", stripped)
    try:
        value = json.loads(stripped)
    except json.JSONDecodeError as exc:
        raise JudgePayloadError("invalid_judge_payload") from exc
    if not isinstance(value, dict):
        raise JudgePayloadError("invalid_judge_payload")
    return value
