"""Offline C18 budget freeze. Never opens a backend or authorizes live execution."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import eval_dataset_contract

AUDIT_PATH = "docs/eval/reports/c18-query-budget-audit-v1.json"
CANARY = ("fact-001", "definition-001", "reasoning-001", "multi-hop-001", "no-answer-001")
SOURCE_PATHS = (
    "docs/eval/dataset-manifest.json", "docs/eval/releases/rag-eval-dev-v2.jsonl",
    "rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java",
    "rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java",
    "rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java",
    "rag-core/src/main/java/com/enterprise/rag/core/rag/generator/LLMProperties.java",
    "rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java",
    "rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java",
    "rag-common/src/main/java/com/enterprise/rag/common/ratelimit/RateLimitInterceptor.java",
    "rag-common/src/main/java/com/enterprise/rag/common/ratelimit/SlidingWindowRateLimiter.java",
    "rag-admin/src/main/resources/application.yml",
    "rag-core/src/test/java/com/enterprise/rag/core/rag/C18BudgetAudit.java",
    "scripts/run_rag_eval.py", "scripts/run_reproducible_rag_eval.py",
)


class BudgetContractError(ValueError):
    pass


def require(condition, code):
    if not condition:
        raise BudgetContractError(code)


def _pairs(items):
    result = {}
    for key, value in items:
        require(key not in result, "duplicate_json_key")
        result[key] = value
    return result


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=_pairs,
                          parse_constant=lambda _: (_ for _ in ()).throw(BudgetContractError("nonfinite_json")))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise BudgetContractError("input_unreadable") from error


def _fields(value, expected, code):
    require(isinstance(value, dict) and set(value) == set(expected), code)


def _positive(value):
    return type(value) is int and value > 0


def _budget(samples):
    n = len(samples)
    initial = sum(s["initialVariants"] for s in samples)
    query = sum(s["queryEmbeddingUpperBound"] for s in samples)
    return dict(debugRetrieval=n, ask=n, generationUpperBound=n, initialVariants=initial,
                explanatoryFallbackVariants=query - 2 * initial,
                queryEmbeddingUpperBound=query, judge=0, modelRerank=0)


def validate_audit(repo_root: Path, audit: dict) -> dict:
    _fields(audit, ("schemaVersion", "evidenceKind", "sourceSha256", "samples", "canary", "full",
                    "providerCalls", "businessDataOutbound"), "audit_fields_invalid")
    require(audit["schemaVersion"] == "c18-query-budget-audit-v1" and
            audit["evidenceKind"] == "OFFLINE_CALL_GRAPH_NOT_LIVE_QUALITY", "audit_version_invalid")
    require(type(audit["providerCalls"]) is int and audit["providerCalls"] == 0 and
            audit["businessDataOutbound"] is False, "audit_offline_boundary_invalid")
    _fields(audit["sourceSha256"], SOURCE_PATHS, "source_set_mismatch")
    for name in SOURCE_PATHS:
        try:
            actual = hashlib.sha256((repo_root / name).read_text(encoding="utf-8").encode("utf-8")).hexdigest()
        except (OSError, UnicodeError) as error:
            raise BudgetContractError("source_unavailable") from error
        require(audit["sourceSha256"][name] == actual, "budget_source_drift")
    identity = eval_dataset_contract.validate_versioned_release(repo_root, Path("docs/eval/dataset-manifest.json"))
    dataset = [json.loads(line) for line in (repo_root / identity["questionSet"]["path"])
               .read_text(encoding="utf-8").splitlines() if line.strip()]
    samples = audit["samples"]
    require(isinstance(samples, list) and len(samples) == 150, "sample_count_invalid")
    for sample in samples:
        _fields(sample, ("id", "initialVariants", "askVariantsPerPass", "askRetrievalPasses",
                         "queryEmbeddingUpperBound"), "sample_fields_invalid")
        counts = sample["askVariantsPerPass"]
        require(isinstance(counts, list) and counts and all(_positive(v) for v in counts), "pass_counts_invalid")
        require(all(_positive(sample[k]) for k in ("initialVariants", "askRetrievalPasses", "queryEmbeddingUpperBound")),
                "budget_number_invalid")
        require(sample["initialVariants"] == counts[0] and sample["askRetrievalPasses"] == len(counts) and
                sample["queryEmbeddingUpperBound"] == sample["initialVariants"] + sum(counts), "budget_sum_mismatch")
    require([s["id"] for s in samples] == [s["id"] for s in dataset], "sample_identity_mismatch")
    for mode, selected in (("full", samples), ("canary", [s for s in samples if s["id"] in CANARY])):
        expected = _budget(selected)
        _fields(audit[mode], expected, "aggregate_fields_invalid")
        require(all(type(v) is int for v in audit[mode].values()) and audit[mode] == expected, "aggregate_mismatch")
    # Frozen measured control-flow counts; changes require a new audit/version, not a silent higher allowance.
    require(audit["canary"]["queryEmbeddingUpperBound"] == 34 and
            audit["full"]["queryEmbeddingUpperBound"] == 1492 and
            audit["canary"]["initialVariants"] == 11 and audit["full"]["initialVariants"] == 451,
            "frozen_budget_mismatch")
    return identity


def build_plan(repo_root: Path, mode: str, audit_path: Path | None = None) -> dict:
    require(mode in ("canary", "full"), "mode_invalid")
    audit = read_json(audit_path or repo_root / AUDIT_PATH)
    identity = validate_audit(repo_root, audit)
    return {
        "schemaVersion": "c18-offline-budget-plan-v1", "status": "PLAN_VALID", "mode": mode,
        "datasetRelease": identity["releaseVersion"], "datasetManifestSha256": identity["manifestSha256"],
        "auditSha256": hashlib.sha256(json.dumps(audit, sort_keys=True, separators=(",", ":")).encode()).hexdigest(),
        "selection": list(CANARY) if mode == "canary" else [s["id"] for s in audit["samples"]],
        "repeat": 1, "runIndex": 1, "callBudget": dict(audit[mode]),
        "minimumRequestIntervalSeconds": 2.2, "automaticRetries": 0,
        "answerCache": False, "judgeMode": "off", "routerEnabled": False,
        "liveAuthorized": False, "executionReady": False,
        "remainingGates": ["runtime_fingerprint", "provider_budget_guard", "complete_evidence_compiler", "live_authorization"],
        "localSideEffects": ["query_count", "successful_answer_history"],
        "actualBackendCalls": 0, "actualProviderCalls": 0,
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--mode", choices=("canary", "full"), required=True)
    parser.add_argument("--output-json", type=Path)
    args = parser.parse_args(argv)
    try:
        plan = build_plan(args.repo_root, args.mode)
        payload = json.dumps(plan, ensure_ascii=False, indent=2) + "\n"
        if args.output_json:
            with args.output_json.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(payload)
        else:
            print(payload, end="")
        return 0
    except (BudgetContractError, eval_dataset_contract.DatasetContractError) as error:
        code = str(error) if isinstance(error, BudgetContractError) else "dataset_invalid"
        print(json.dumps({"status": "BLOCKED", "reason": code}))
        return 2
    except OSError:
        print(json.dumps({"status": "BLOCKED", "reason": "output_unavailable_or_exists"}))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
