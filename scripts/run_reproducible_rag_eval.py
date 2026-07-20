#!/usr/bin/env python3
"""
Prepare a reproducible retrieval-only RAG evaluation KB, then delegate metrics
to run_rag_eval.py without changing metric definitions.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = "http://localhost:8080"
DEFAULT_EVAL_SET = Path("docs/eval/rag_eval_set.jsonl")
DEFAULT_REPORT = Path("docs/eval/reports/stage1-reproducible-eval.md")
DEFAULT_DETAILS_JSON = Path("docs/eval/reports/stage1-reproducible-eval-details.json")
DEFAULT_METADATA_JSON = Path("docs/eval/reports/stage1-reproducible-eval-metadata.json")
DEFAULT_FIXTURES = [
    Path("test-data/springboot-basics.md"),
    Path("test-data/java-interview-guide.md"),
    Path("test-data/rag-technology-guide.md"),
]
DEFAULT_CONFIG_SNAPSHOT = [
    Path("rag-admin/src/main/resources/application.yml"),
    Path("rag-core/src/main/java/com/enterprise/rag/core/rag/query/RetrievalProperties.java"),
    Path("rag-document/src/main/java/com/enterprise/rag/document/chunker/ChunkConfig.java"),
]
DEFAULT_KB_NAME = "codex-stage1-repro-eval"
DEFAULT_KB_MARKER = "codex reproducible retrieval-only eval fixture"
C7_ARM_SCHEMA = "c7-reranker-ab-v1"
C7_ARM_MANIFEST_FIELDS = {
    "schemaVersion",
    "armId",
    "expectedRequestedProvider",
    "expectedEffectiveProvider",
    "model",
    "protocol",
    "topN",
    "topK",
    "truncate",
    "timeoutMillis",
    "healthCheckEnabled",
    "endpointPath",
    "measuredRepeats",
    "warmupCalls",
}
TERMINAL_TASK_STATES = {"COMPLETED", "FAILED", "CANCELLED"}
SUCCESS_DOCUMENT_STATES = {"COMPLETED"}
FAILED_DOCUMENT_STATES = {"FAILED", "CANCELLED"}


class ApiError(RuntimeError):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prepare and run a reproducible RAG eval.")
    parser.add_argument("--base-url", default=os.getenv("RAG_BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--username", default=os.getenv("RAG_EVAL_USERNAME", ""))
    parser.add_argument("--password", default=os.getenv("RAG_EVAL_PASSWORD", ""))
    parser.add_argument("--kb-name", default=os.getenv("RAG_EVAL_KB_NAME", DEFAULT_KB_NAME))
    parser.add_argument("--kb-description", default=os.getenv("RAG_EVAL_KB_DESCRIPTION", DEFAULT_KB_MARKER))
    parser.add_argument("--eval-set", default=os.getenv("RAG_EVAL_SET", str(DEFAULT_EVAL_SET)))
    parser.add_argument("--report", default=os.getenv("RAG_EVAL_REPORT", str(DEFAULT_REPORT)))
    parser.add_argument("--details-json", default=os.getenv("RAG_EVAL_DETAILS_JSON", str(DEFAULT_DETAILS_JSON)))
    parser.add_argument("--metadata-json", default=os.getenv("RAG_EVAL_METADATA_JSON", str(DEFAULT_METADATA_JSON)))
    parser.add_argument("--fixture", action="append", dest="fixtures", help="Fixture file to upload. Repeatable.")
    parser.add_argument("--sample-id", action="append", dest="sample_ids", help="Run only the given eval sample id. Repeatable.")
    parser.add_argument("--sample-limit", type=int, default=int(os.getenv("RAG_EVAL_SAMPLE_LIMIT", "0")), help="Run only the first N selected samples. 0 means no limit.")
    parser.add_argument("--top-k", type=int, default=int(os.getenv("RAG_EVAL_TOP_K", "5")))
    parser.add_argument("--min-score", type=float, default=float(os.getenv("RAG_EVAL_MIN_SCORE", "0.3")))
    parser.add_argument("--enable-rerank", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--timeout", type=float, default=float(os.getenv("RAG_EVAL_TIMEOUT", "60")))
    parser.add_argument("--include-ask", action="store_true", help="Also call /api/qa/ask for generation/citation metrics. Default remains retrieval-only.")
    parser.add_argument("--ask-timeout", type=float, default=parse_float_env("RAG_EVAL_ASK_TIMEOUT"), help="Timeout for child /api/qa/ask calls. Defaults to --timeout.")
    parser.add_argument("--ask-delay-seconds", type=float, default=float(os.getenv("RAG_EVAL_ASK_DELAY_SECONDS", "0")))
    parser.add_argument("--max-ask-retries", type=int, default=int(os.getenv("RAG_EVAL_MAX_ASK_RETRIES", "0")))
    parser.add_argument("--retry-backoff-seconds", type=float, default=float(os.getenv("RAG_EVAL_RETRY_BACKOFF_SECONDS", "0")))
    parser.add_argument("--retry-ask-timeouts", action=argparse.BooleanOptionalAction, default=parse_bool_env("RAG_EVAL_RETRY_ASK_TIMEOUTS", True), help="Retry /api/qa/ask timeout errors when --max-ask-retries is positive.")
    parser.add_argument("--judge-mode", choices=("off", "llm"), default=os.getenv("RAG_EVAL_JUDGE_MODE", "off"))
    parser.add_argument("--judge-base-url", default=os.getenv("RAG_EVAL_JUDGE_BASE_URL", os.getenv("OPENAI_BASE_URL", "https://integrate.api.nvidia.com/v1")))
    parser.add_argument("--judge-api-key", default=os.getenv("RAG_EVAL_JUDGE_API_KEY", os.getenv("NVIDIA_API_KEY", "")))
    parser.add_argument("--judge-model", default=os.getenv("RAG_EVAL_JUDGE_MODEL", ""))
    parser.add_argument("--judge-temperature", type=float, default=float(os.getenv("RAG_EVAL_JUDGE_TEMPERATURE", "0")))
    parser.add_argument("--judge-timeout", type=float, default=float(os.getenv("RAG_EVAL_JUDGE_TIMEOUT", "60")))
    parser.add_argument("--judge-max-context-chars", type=int, default=int(os.getenv("RAG_EVAL_JUDGE_MAX_CONTEXT_CHARS", "6000")))
    parser.add_argument("--poll-interval-seconds", type=float, default=2.0)
    parser.add_argument("--index-timeout-seconds", type=float, default=180.0)
    parser.add_argument("--repeat", type=int, default=1, help="Run retrieval-only eval repeatedly against the same prepared KB.")
    parser.add_argument(
        "--run-index",
        action="append",
        type=int,
        dest="run_indexes",
        help="Run only this measured repeat index while retaining manifest repeat identity. Repeatable.",
    )
    parser.add_argument(
        "--skip-warmup",
        action="store_true",
        help="Skip this invocation's C7 warm-up because the same logical arm was already warmed up.",
    )
    parser.add_argument(
        "--arm-manifest",
        default="",
        help="Optional C7 sanitized reranker arm manifest JSON. Required for official C7 A/B arms.",
    )
    parser.add_argument(
        "--keep-existing",
        action="store_true",
        help="Reuse a matching KB without mutation; fail if it does not exist instead of creating or uploading data.",
    )
    parser.add_argument("--no-overwrite", action="store_true")
    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument("--plan-only", action="store_true", help="Print planned files, sample selection, and eval command shape without contacting the backend.")
    mode_group.add_argument(
        "--preflight-only",
        action="store_true",
        help="Verify backend login, the existing eval KB, and fixture indexing without mutating data or running eval calls.",
    )
    parser.add_argument("--python", default=sys.executable)
    args = parser.parse_args()
    if args.ask_timeout is None:
        args.ask_timeout = args.timeout
    return args


def require_credentials(args: argparse.Namespace) -> None:
    if not str(args.username).strip() or not str(args.password).strip():
        raise ApiError(
            "Reproducible RAG eval requires explicit credentials via "
            "--username/--password or RAG_EVAL_USERNAME/RAG_EVAL_PASSWORD."
        )


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


def call_json(
    method: str,
    url: str,
    payload: Any,
    token: str | None,
    timeout: float,
) -> dict[str, Any]:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            text = response.read().decode("utf-8")
            return json.loads(text) if text else {}
    except urllib.error.HTTPError as exc:
        text = exc.read().decode("utf-8", errors="replace")
        raise ApiError(f"HTTP {exc.code} {url}: {text}") from exc
    except urllib.error.URLError as exc:
        raise ApiError(f"Cannot connect to {url}: {exc.reason}") from exc


def unwrap(response: dict[str, Any]) -> Any:
    code = response.get("code")
    if code is not None and not (200 <= int(code) < 300):
        raise ApiError(f"API returned code={code} message={response.get('message')}")
    return response.get("data")


def login(args: argparse.Namespace) -> str:
    data = unwrap(call_json(
        "POST",
        f"{args.base_url}/auth/login",
        {"username": args.username, "password": args.password},
        None,
        args.timeout,
    ))
    if not isinstance(data, dict) or not data.get("accessToken"):
        raise ApiError("Login response did not contain data.accessToken")
    return str(data["accessToken"])


def list_kbs(args: argparse.Namespace, token: str) -> list[dict[str, Any]]:
    data = unwrap(call_json("GET", f"{args.base_url}/api/knowledge-bases", None, token, args.timeout))
    if not isinstance(data, list):
        raise ApiError("Knowledge base list response data was not a list")
    return [item for item in data if isinstance(item, dict)]


def find_existing_eval_kb(args: argparse.Namespace, token: str) -> dict[str, Any] | None:
    matches = [kb for kb in list_kbs(args, token) if kb.get("name") == args.kb_name]
    if len(matches) > 1:
        raise ApiError(f"Multiple KBs named {args.kb_name!r}; clean them before reusing the eval KB")
    if not matches:
        return None
    kb = matches[0]
    if kb.get("description") != args.kb_description:
        raise ApiError(f"Existing KB {args.kb_name!r} does not have the eval marker description")
    return kb


def delete_matching_kbs(args: argparse.Namespace, token: str) -> None:
    for kb in list_kbs(args, token):
        if kb.get("name") != args.kb_name:
            continue
        if kb.get("description") != args.kb_description:
            raise ApiError(
                f"Refusing to delete KB named {args.kb_name!r} because its description does not match the eval marker."
            )
        kb_id = kb.get("id")
        print(f"Deleting old eval KB id={kb_id}")
        unwrap(call_json("DELETE", f"{args.base_url}/api/knowledge-bases/{kb_id}", None, token, args.timeout))


def create_kb(args: argparse.Namespace, token: str) -> dict[str, Any]:
    data = unwrap(call_json(
        "POST",
        f"{args.base_url}/api/knowledge-bases",
        {"name": args.kb_name, "description": args.kb_description, "isPublic": False},
        token,
        args.timeout,
    ))
    if not isinstance(data, dict) or data.get("id") is None:
        raise ApiError("Create KB response did not contain data.id")
    print(f"Created eval KB id={data.get('id')} collection={data.get('vectorCollection')}")
    return data


def get_or_create_kb(args: argparse.Namespace, token: str) -> dict[str, Any]:
    if args.keep_existing:
        kb = find_existing_eval_kb(args, token)
        if kb is None:
            raise ApiError(
                f"--keep-existing requested but eval KB {args.kb_name!r} does not exist; "
                "rerun without --keep-existing to create the KB and upload fixtures"
            )
        print(f"Reusing eval KB id={kb.get('id')} collection={kb.get('vectorCollection')}")
        return kb

    delete_matching_kbs(args, token)
    return create_kb(args, token)


def upload_fixture(args: argparse.Namespace, token: str, kb_id: int, path: Path) -> dict[str, Any]:
    boundary = f"----codex-rag-eval-{int(time.time() * 1000)}"
    file_bytes = path.read_bytes()
    parts: list[bytes] = []
    parts.append(f"--{boundary}\r\n".encode("utf-8"))
    parts.append(
        (
            f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
            "Content-Type: text/markdown\r\n\r\n"
        ).encode("utf-8")
    )
    parts.append(file_bytes)
    parts.append(b"\r\n")
    parts.append(f"--{boundary}\r\n".encode("utf-8"))
    parts.append(f'Content-Disposition: form-data; name="title"\r\n\r\n{path.name}\r\n'.encode("utf-8"))
    parts.append(f"--{boundary}--\r\n".encode("utf-8"))

    headers = {
        "Accept": "application/json",
        "Authorization": f"Bearer {token}",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    request = urllib.request.Request(
        f"{args.base_url}/api/knowledge-bases/{kb_id}/documents",
        data=b"".join(parts),
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=args.timeout) as response:
            text = response.read().decode("utf-8")
            data = unwrap(json.loads(text) if text else {})
            if not isinstance(data, dict):
                raise ApiError(f"Upload response for {path} did not contain object data")
            return data
    except urllib.error.HTTPError as exc:
        text = exc.read().decode("utf-8", errors="replace")
        raise ApiError(f"HTTP {exc.code} upload {path}: {text}") from exc
    except urllib.error.URLError as exc:
        raise ApiError(f"Cannot upload {path}: {exc.reason}") from exc


def get_task(args: argparse.Namespace, token: str, task_id: str) -> dict[str, Any]:
    data = unwrap(call_json("GET", f"{args.base_url}/api/tasks/{task_id}", None, token, args.timeout))
    if not isinstance(data, dict):
        raise ApiError(f"Task response {task_id} did not contain object data")
    return data


def list_documents(args: argparse.Namespace, token: str, kb_id: int) -> list[dict[str, Any]]:
    data = unwrap(call_json("GET", f"{args.base_url}/api/knowledge-bases/{kb_id}/documents", None, token, args.timeout))
    if not isinstance(data, list):
        raise ApiError("Document list response data was not a list")
    return [item for item in data if isinstance(item, dict)]


def wait_for_indexing(
    args: argparse.Namespace,
    token: str,
    kb_id: int,
    task_ids: list[str],
    expected_files: set[str],
) -> list[dict[str, Any]]:
    deadline = time.time() + args.index_timeout_seconds
    last_task_states: dict[str, str] = {}
    while time.time() < deadline:
        for task_id in task_ids:
            task = get_task(args, token, task_id)
            last_task_states[task_id] = str(task.get("state"))
            if last_task_states[task_id] in {"FAILED", "CANCELLED"}:
                raise ApiError(f"Index task {task_id} ended with {last_task_states[task_id]}: {task.get('error')}")

        docs = list_documents(args, token, kb_id)
        relevant = docs_for_expected_files(docs, expected_files)
        doc_states = {str(doc.get("title") or doc.get("fileName")): str(doc.get("status")) for doc in relevant}
        if len(relevant) >= len(expected_files) and all(state in SUCCESS_DOCUMENT_STATES for state in doc_states.values()):
            if all(state in TERMINAL_TASK_STATES for state in last_task_states.values()):
                return relevant
        if any(state in FAILED_DOCUMENT_STATES for state in doc_states.values()):
            raise ApiError(f"Document indexing failed: {doc_states}")

        print(f"Waiting for indexing tasks={last_task_states} documents={doc_states}")
        time.sleep(args.poll_interval_seconds)

    raise ApiError(f"Timed out waiting for indexing. tasks={last_task_states}")


def docs_for_expected_files(docs: list[dict[str, Any]], expected_files: set[str]) -> list[dict[str, Any]]:
    relevant: list[dict[str, Any]] = []
    for doc in docs:
        names = {
            str(doc.get("title") or ""),
            str(doc.get("fileName") or ""),
            Path(str(doc.get("filePath") or "")).name,
        }
        if names & expected_files:
            relevant.append(doc)
    return relevant


def build_preflight(
    args: argparse.Namespace,
    kb: dict[str, Any],
    docs: list[dict[str, Any]],
    fixtures: list[Path],
) -> dict[str, Any]:
    expected_files = {path.name for path in fixtures}
    relevant = docs_for_expected_files(docs, expected_files)
    by_name: dict[str, dict[str, Any]] = {}
    for doc in relevant:
        names = [
            str(doc.get("title") or ""),
            str(doc.get("fileName") or ""),
            Path(str(doc.get("filePath") or "")).name,
        ]
        for name in names:
            if name in expected_files:
                by_name[name] = doc

    missing = sorted(expected_files - set(by_name))
    incomplete = sorted(
        name
        for name, doc in by_name.items()
        if str(doc.get("status") or "") not in SUCCESS_DOCUMENT_STATES
    )
    ready = not missing and not incomplete
    return {
        "status": "READY" if ready else "BLOCKED",
        "mutationFree": True,
        "baseUrl": args.base_url,
        "knowledgeBase": {
            "id": kb.get("id"),
            "name": kb.get("name"),
            "vectorCollection": kb.get("vectorCollection"),
        },
        "fixtures": {
            "expectedCount": len(expected_files),
            "matchedCount": len(by_name),
            "missing": missing,
            "incomplete": incomplete,
            "documents": [
                {
                    "name": name,
                    "status": doc.get("status"),
                    "chunkCount": int(doc.get("chunkCount") or 0),
                }
                for name, doc in sorted(by_name.items())
            ],
        },
        "nextStep": (
            "Run a small generation/citation smoke with --keep-existing --include-ask."
            if ready
            else "Restore the missing or incomplete eval fixtures before running evaluation."
        ),
    }


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_arm_manifest(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ApiError(f"Arm manifest not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ApiError(f"Arm manifest JSON is invalid: {path}") from exc
    if not isinstance(value, dict):
        raise ApiError("Arm manifest must be a JSON object")

    missing = sorted(C7_ARM_MANIFEST_FIELDS - set(value))
    unexpected = sorted(set(value) - C7_ARM_MANIFEST_FIELDS)
    if missing or unexpected:
        raise ApiError(
            f"Arm manifest fields invalid: missing={missing}, unexpected={unexpected}"
        )
    if value.get("schemaVersion") != C7_ARM_SCHEMA:
        raise ApiError(f"Arm manifest schemaVersion must be {C7_ARM_SCHEMA}")
    if value.get("armId") not in {"heuristic", "model"}:
        raise ApiError("Arm manifest armId must be heuristic or model")
    for field in (
        "expectedRequestedProvider",
        "expectedEffectiveProvider",
        "model",
        "protocol",
        "truncate",
        "endpointPath",
    ):
        if not isinstance(value.get(field), str):
            raise ApiError(f"Arm manifest {field} must be a string")
    for field in ("topN", "topK", "timeoutMillis", "measuredRepeats", "warmupCalls"):
        item = value.get(field)
        if isinstance(item, bool) or not isinstance(item, int):
            raise ApiError(f"Arm manifest {field} must be an integer")
    if value["topN"] < 1 or value["topK"] < 1 or value["timeoutMillis"] < 0:
        raise ApiError("Arm manifest topN/topK must be positive and timeoutMillis non-negative")
    if value["topK"] > value["topN"]:
        raise ApiError("Arm manifest topK must not exceed topN")
    if value["measuredRepeats"] < 1 or value["warmupCalls"] < 0:
        raise ApiError("Arm manifest measuredRepeats must be positive and warmupCalls non-negative")
    if not isinstance(value.get("healthCheckEnabled"), bool):
        raise ApiError("Arm manifest healthCheckEnabled must be boolean")
    if value["truncate"] not in {"NONE", "END"}:
        raise ApiError("Arm manifest truncate must be NONE or END")
    if value["armId"] == "heuristic" and (
        value["expectedRequestedProvider"] != "heuristic"
        or value["expectedEffectiveProvider"] != "heuristic"
        or value["model"] != ""
        or value["protocol"] != "heuristic-v1"
        or value["endpointPath"] != ""
        or value["timeoutMillis"] != 0
        or value["healthCheckEnabled"]
    ):
        raise ApiError("Heuristic arm manifest contains model-specific values")
    if value["armId"] == "model" and (
        value["expectedRequestedProvider"] in {"", "heuristic"}
        or value["expectedEffectiveProvider"] in {"", "heuristic"}
        or not value["model"]
        or not value["protocol"]
        or not value["endpointPath"].startswith("/")
    ):
        raise ApiError("Model arm manifest is incomplete")

    canonical = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return {**value, "sha256": hashlib.sha256(canonical).hexdigest()}


def git_head() -> str:
    try:
        return subprocess.check_output(["git", "rev-parse", "HEAD"], text=True, stderr=subprocess.DEVNULL).strip()
    except Exception:  # noqa: BLE001
        return ""


def build_metadata(
    args: argparse.Namespace,
    kb: dict[str, Any],
    docs: list[dict[str, Any]],
    fixtures: list[Path],
    selected_samples: list[dict[str, Any]] | None = None,
    arm_manifest: dict[str, Any] | None = None,
    run_index: int = 1,
) -> dict[str, Any]:
    chunk_count = sum(int(doc.get("chunkCount") or 0) for doc in docs)
    eval_path = Path(args.eval_set)
    repeat_total = int(
        (arm_manifest or {}).get("measuredRepeats")
        or getattr(args, "repeat", 1)
    )
    return {
        "evaluationSchema": C7_ARM_SCHEMA if arm_manifest else "rag-eval-v1",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "baseUrl": args.base_url,
        "evalSet": args.eval_set,
        "evalSetIdentity": {
            "path": str(eval_path),
            "sha256": sha256_file(eval_path),
            "bytes": eval_path.stat().st_size,
        },
        "sampleSelection": {
            "ids": [str(sample.get("id")) for sample in (selected_samples or [])],
            "count": len(selected_samples or []),
        },
        "topK": args.top_k,
        "minScore": args.min_score,
        "enableRerank": args.enable_rerank,
        "knowledgeBase": {
            "id": kb.get("id"),
            "name": kb.get("name"),
            "description": kb.get("description"),
            "vectorCollection": kb.get("vectorCollection"),
            "documentCount": len(docs),
            "chunkCount": chunk_count,
            "documents": [
                {
                    "id": doc.get("id"),
                    "title": doc.get("title"),
                    "status": doc.get("status"),
                    "chunkCount": doc.get("chunkCount"),
                    "contentHash": doc.get("contentHash"),
                }
                for doc in sorted(docs, key=lambda item: str(item.get("title")))
            ],
        },
        "fixtures": [
            {
                "path": str(path),
                "name": path.name,
                "sha256": sha256_file(path),
                "bytes": path.stat().st_size,
            }
            for path in fixtures
        ],
        "configSnapshot": {
            str(path): {"sha256": sha256_file(path), "bytes": path.stat().st_size}
            for path in DEFAULT_CONFIG_SNAPSHOT
            if path.exists()
        },
        "git": {"head": git_head()},
        "armManifest": dict(arm_manifest) if arm_manifest else None,
        "repeat": {"index": run_index, "total": repeat_total},
        "warmup": {"calls": int((arm_manifest or {}).get("warmupCalls", 0))},
    }


def repeat_path(path_value: str, repeat: int, index: int) -> Path:
    path = Path(path_value)
    if repeat <= 1:
        return path
    return path.with_name(f"{path.stem}-run{index}{path.suffix}")


def selected_run_indexes(requested: list[int] | None, repeat: int) -> list[int]:
    indexes = requested or list(range(1, repeat + 1))
    if len(set(indexes)) != len(indexes):
        raise ApiError("--run-index values must be unique")
    invalid = [index for index in indexes if index < 1 or index > repeat]
    if invalid:
        raise ApiError(f"--run-index must be between 1 and {repeat}: {invalid}")
    return indexes


def write_json(path: Path, payload: dict[str, Any], no_overwrite: bool) -> None:
    if no_overwrite and path.exists():
        raise ApiError(f"--no-overwrite refused to overwrite {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def run_c7_warmup(
    args: argparse.Namespace,
    kb_id: int,
    kb: dict[str, Any],
    docs: list[dict[str, Any]],
    fixtures: list[Path],
    selected_samples: list[dict[str, Any]],
    output_dir: Path = Path("tmp/eval"),
) -> dict[str, Any]:
    manifest = getattr(args, "arm_manifest_data", None)
    warmup_calls = int((manifest or {}).get("warmupCalls", 0) or 0)
    if not isinstance(manifest, dict) or warmup_calls <= 0:
        return {"sampleIds": [], "skipped": True}
    if warmup_calls > len(selected_samples):
        raise ApiError(
            f"C7 warm-up requires {warmup_calls} fixed samples but only {len(selected_samples)} were selected"
        )
    warmup_samples = selected_samples[:warmup_calls]
    selection_bytes = json.dumps(
        [str(sample.get("id")) for sample in selected_samples],
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    selection_hash = hashlib.sha256(selection_bytes).hexdigest()[:12]
    prefix = f"c7-{manifest['armId']}-{manifest['sha256'][:12]}-warmup-{selection_hash}"
    metadata_path = output_dir / f"{prefix}-metadata.json"
    report_path = output_dir / f"{prefix}-report.md"
    details_path = output_dir / f"{prefix}-details.json"
    metadata = build_metadata(
        args,
        kb,
        docs,
        fixtures,
        selected_samples=warmup_samples,
        arm_manifest=manifest,
        run_index=0,
    )
    metadata["phase"] = "warmup"
    metadata["warmup"] = {
        "calls": warmup_calls,
        "excludedFromMeasured": True,
    }
    write_json(metadata_path, metadata, args.no_overwrite)
    warmup_args = copy.copy(args)
    warmup_args.sample_ids = [str(sample.get("id")) for sample in warmup_samples]
    warmup_args.sample_limit = warmup_calls
    run_eval(warmup_args, kb_id, report_path, details_path, metadata_path)
    return {
        "sampleIds": warmup_args.sample_ids,
        "metadataPath": metadata_path,
        "reportPath": report_path,
        "detailsPath": details_path,
        "skipped": False,
    }


def build_eval_command(args: argparse.Namespace, kb_id: int, report: Path, details: Path, metadata: Path) -> list[str]:
    command = [
        args.python,
        "-B",
        "scripts/run_rag_eval.py",
        "--base-url",
        args.base_url,
        "--eval-set",
        args.eval_set,
        "--kb-id",
        str(kb_id),
        "--sample-limit",
        str(args.sample_limit),
        "--username",
        args.username,
        "--password",
        args.password,
        "--top-k",
        str(args.top_k),
        "--min-score",
        str(args.min_score),
        "--report",
        str(report),
        "--details-json",
        str(details),
        "--run-metadata-json",
        str(metadata),
    ]
    if args.include_ask:
        command.extend([
            "--ask-delay-seconds",
            str(args.ask_delay_seconds),
            "--max-ask-retries",
            str(args.max_ask_retries),
            "--retry-backoff-seconds",
            str(args.retry_backoff_seconds),
            "--ask-timeout",
            str(args.ask_timeout),
            "--retry-ask-timeouts" if args.retry_ask_timeouts else "--no-retry-ask-timeouts",
            "--judge-mode",
            args.judge_mode,
            "--judge-base-url",
            args.judge_base_url,
            "--judge-model",
            args.judge_model,
            "--judge-temperature",
            str(args.judge_temperature),
            "--judge-timeout",
            str(args.judge_timeout),
            "--judge-max-context-chars",
            str(args.judge_max_context_chars),
        ])
    else:
        command.append("--skip-ask")
    for sample_id in args.sample_ids or []:
        command.extend(["--sample-id", sample_id])
    if args.enable_rerank:
        command.append("--enable-rerank")
    else:
        command.append("--no-enable-rerank")
    if args.no_overwrite:
        command.append("--no-overwrite")
    return command


def read_eval_samples(path: Path) -> list[dict[str, Any]]:
    samples: list[dict[str, Any]] = []
    try:
        with path.open("r", encoding="utf-8") as file:
            for line_number, line in enumerate(file, start=1):
                if not line.strip():
                    continue
                item = json.loads(line)
                if not isinstance(item, dict):
                    raise ApiError(f"Eval sample at {path}:{line_number} is not an object")
                samples.append(item)
    except FileNotFoundError as exc:
        raise ApiError(f"Eval set not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ApiError(f"Eval set JSONL parse failed at {path}:{exc.lineno}: {exc.msg}") from exc
    return samples


def select_eval_samples(
    samples: list[dict[str, Any]],
    sample_ids: list[str] | None,
    sample_limit: int,
) -> list[dict[str, Any]]:
    selected = samples
    if sample_ids:
        requested = set(sample_ids)
        selected = [sample for sample in selected if str(sample.get("id")) in requested]
    if sample_limit > 0:
        selected = selected[:sample_limit]
    return selected


def estimate_live_calls(args: argparse.Namespace, samples: list[dict[str, Any]], repeat: int) -> dict[str, int]:
    answerable_count = sum(1 for sample in samples if sample.get("should_answer", True))
    ask_calls = len(samples) * repeat if args.include_ask else 0
    judge_calls = answerable_count * repeat if args.include_ask and args.judge_mode == "llm" else 0
    calls = {
        "debugRetrieve": len(samples) * repeat,
        "ask": ask_calls,
        "llmJudge": judge_calls,
    }
    manifest = getattr(args, "arm_manifest_data", None)
    if isinstance(manifest, dict):
        warmup_calls = (
            0
            if getattr(args, "skip_warmup", False)
            else max(0, int(manifest.get("warmupCalls", 0) or 0))
        )
        calls["debugRetrieve"] += warmup_calls
        calls["queryEmbeddingUpperBound"] = calls["debugRetrieve"]
        calls["modelRerankUpperBound"] = (
            calls["debugRetrieve"] if manifest.get("armId") == "model" else 0
        )
    return calls


def run_eval(args: argparse.Namespace, kb_id: int, report: Path, details: Path, metadata: Path) -> None:
    command = build_eval_command(args, kb_id, report, details, metadata)
    env = os.environ.copy()
    if args.judge_api_key:
        env["RAG_EVAL_JUDGE_API_KEY"] = args.judge_api_key
    print("Running:", " ".join(redact_command(command)))
    subprocess.run(command, check=True, env=env)


def redact_command(command: list[str]) -> list[str]:
    redacted = list(command)
    for option in ("--password", "--judge-api-key"):
        if option in redacted:
            value_index = redacted.index(option) + 1
            if value_index < len(redacted):
                redacted[value_index] = "***"
    return redacted


def build_plan(
    args: argparse.Namespace,
    fixtures: list[Path],
    selected_samples: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    run_indexes = selected_run_indexes(getattr(args, "run_indexes", None), max(1, args.repeat))
    execution_count = len(run_indexes)
    if selected_samples is None:
        selected_samples = select_eval_samples(read_eval_samples(Path(args.eval_set)), args.sample_ids, args.sample_limit)
    answerable_count = sum(1 for sample in selected_samples if sample.get("should_answer", True))
    no_answer_count = len(selected_samples) - answerable_count
    plan = {
        "mode": "generation/citation" if args.include_ask else "retrieval-only",
        "baseUrl": args.base_url,
        "fixtureCount": len(fixtures),
        "keepExisting": args.keep_existing,
        "willUploadFixtures": not args.keep_existing,
        "expectedFixtureUploads": 0 if args.keep_existing else len(fixtures),
        "sampleIds": args.sample_ids or [],
        "sampleLimit": args.sample_limit,
        "selectedSampleCount": len(selected_samples),
        "answerableCount": answerable_count,
        "noAnswerCount": no_answer_count,
        "selectedSampleIds": [sample.get("id") for sample in selected_samples],
        "includeAsk": args.include_ask,
        "judgeMode": args.judge_mode,
        "askTimeout": args.ask_timeout,
        "retryAskTimeouts": args.retry_ask_timeouts,
        "repeat": args.repeat,
        "runIndexes": run_indexes,
        "estimatedLiveCalls": estimate_live_calls(args, selected_samples, execution_count),
        "outputSetCount": execution_count,
        "childCommandShape": redact_command(
            build_eval_command(args, 0, Path(args.report), Path(args.details_json), Path(args.metadata_json))
        ),
    }
    manifest = getattr(args, "arm_manifest_data", None)
    if isinstance(manifest, dict):
        plan["armId"] = manifest.get("armId")
        plan["armManifestSha256"] = manifest.get("sha256")
        plan["warmupCalls"] = 0 if getattr(args, "skip_warmup", False) else manifest.get("warmupCalls", 0)
    return plan


def print_plan(plan: dict[str, Any]) -> None:
    print(json.dumps(plan, ensure_ascii=False, indent=2))


def redact_preflight_for_display(preflight: dict[str, Any]) -> dict[str, Any]:
    knowledge_base = preflight.get("knowledgeBase") or {}
    fixtures = preflight.get("fixtures") or {}
    documents = fixtures.get("documents") or []
    return {
        "status": preflight.get("status"),
        "mutationFree": preflight.get("mutationFree"),
        "baseUrl": preflight.get("baseUrl"),
        "knowledgeBase": {
            "id": knowledge_base.get("id"),
            "vectorCollection": knowledge_base.get("vectorCollection"),
        },
        "fixtures": {
            "expectedCount": fixtures.get("expectedCount", 0),
            "matchedCount": fixtures.get("matchedCount", 0),
            "missingCount": len(fixtures.get("missing") or []),
            "incompleteCount": len(fixtures.get("incomplete") or []),
            "documentStates": [
                {
                    "status": document.get("status"),
                    "chunkCount": document.get("chunkCount", 0),
                }
                for document in documents
            ],
        },
        "nextStep": preflight.get("nextStep"),
    }


def main() -> int:
    args = parse_args()
    args.base_url = args.base_url.rstrip("/")
    fixtures = [Path(value) for value in (args.fixtures or [str(path) for path in DEFAULT_FIXTURES])]
    missing = [str(path) for path in fixtures if not path.exists()]
    if missing:
        print(f"Missing fixture file count: {len(missing)}", file=sys.stderr)
        return 2
    if args.repeat < 1:
        print("--repeat must be >= 1", file=sys.stderr)
        return 2
    try:
        run_indexes = selected_run_indexes(args.run_indexes, args.repeat)
    except ApiError as exc:
        print(str(exc), file=sys.stderr)
        return 2
    args.arm_manifest_data = load_arm_manifest(Path(args.arm_manifest)) if args.arm_manifest else None
    if args.arm_manifest_data:
        if args.repeat != int(args.arm_manifest_data["measuredRepeats"]):
            print("--repeat must match arm manifest measuredRepeats", file=sys.stderr)
            return 2
        if args.top_k != int(args.arm_manifest_data["topK"]):
            print("--top-k must match arm manifest topK", file=sys.stderr)
            return 2
        if not args.enable_rerank or args.include_ask:
            print("C7 arm manifest requires retrieval-only with rerank enabled", file=sys.stderr)
            return 2
    selected_samples = select_eval_samples(read_eval_samples(Path(args.eval_set)), args.sample_ids, args.sample_limit)
    if not selected_samples:
        print("No eval samples selected. Check --sample-id/--sample-limit.", file=sys.stderr)
        return 2
    if args.plan_only:
        print_plan(build_plan(args, fixtures, selected_samples))
        return 0

    try:
        require_credentials(args)
    except ApiError as exc:
        print(str(exc), file=sys.stderr)
        return 2

    token = login(args)
    if args.preflight_only:
        kb = find_existing_eval_kb(args, token)
        if kb is None:
            raise ApiError(f"Eval KB {args.kb_name!r} does not exist; preflight never creates it")
        kb_id = int(kb["id"])
        preflight = build_preflight(args, kb, list_documents(args, token, kb_id), fixtures)
        print(json.dumps(redact_preflight_for_display(preflight), ensure_ascii=False, indent=2))
        return 0 if preflight["status"] == "READY" else 1

    kb = get_or_create_kb(args, token)
    kb_id = int(kb["id"])

    if not args.keep_existing:
        task_ids: list[str] = []
        for fixture in fixtures:
            upload = upload_fixture(args, token, kb_id, fixture)
            task_id = str(upload.get("taskId") or "")
            if not task_id:
                raise ApiError(f"Upload response for {fixture} did not include taskId")
            task_ids.append(task_id)
            print(f"Uploaded fixture taskId={task_id}")
        docs = wait_for_indexing(args, token, kb_id, task_ids, {path.name for path in fixtures})
    else:
        docs = docs_for_expected_files(list_documents(args, token, kb_id), {path.name for path in fixtures})
        if len(docs) < len(fixtures):
            raise ApiError("--keep-existing KB does not contain all expected fixture documents")

    if args.arm_manifest_data and not args.skip_warmup:
        warmup = run_c7_warmup(args, kb_id, kb, docs, fixtures, selected_samples)
        print(
            "Completed C7 warm-up: "
            f"arm={args.arm_manifest_data['armId']} calls={len(warmup['sampleIds'])} excludedFromMeasured=true"
        )

    for index in run_indexes:
        metadata_path = repeat_path(args.metadata_json, args.repeat, index)
        report_path = repeat_path(args.report, args.repeat, index)
        details_path = repeat_path(args.details_json, args.repeat, index)
        metadata = build_metadata(
            args,
            kb,
            docs,
            fixtures,
            selected_samples=selected_samples,
            arm_manifest=args.arm_manifest_data,
            run_index=index,
        )
        write_json(metadata_path, metadata, args.no_overwrite)
        run_eval(args, kb_id, report_path, details_path, metadata_path)

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ApiError as exc:
        print(f"Reproducible eval failed: errorType={type(exc).__name__}", file=sys.stderr)
        raise SystemExit(1) from exc
