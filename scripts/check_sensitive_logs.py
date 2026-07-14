#!/usr/bin/env python3
"""Heuristic regression gate for sensitive data written to ordinary logs."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


OUTPUT_CALL_RE = re.compile(
    r"(?:\b(?:log|logger|logging)\.(?:trace|debug|info|warn|warning|error|exception)\s*\("
    r"|\bconsole\.(?:trace|debug|info|warn|error|log)\s*\("
    r"|\bprint\s*\("
    r"|\bSystem\.(?:out|err)\.print(?:ln)?\s*\()",
    re.IGNORECASE,
)
SENSITIVE_IDENTIFIER_RE = re.compile(
    r"\b(?:"
    r"question|query(?:Variants?)?|prompt|context|snippet|"
    r"fileName|filename|originalFilename|documentTitle|knowledgeBaseName|kbName|"
    r"username|password|apiKey|accessToken|refreshToken|authorization|credential|secret|"
    r"idempotencyKey|rateLimitKey|responseBody"
    r")\b",
    re.IGNORECASE,
)
HIGH_RISK_EXPRESSION_RE = re.compile(
    r"getMessage\s*\(|getResponseBodyAsString\s*\(|"
    r",\s*(?:e|ex|exception|throwable)\s*\)?\s*;?\s*$",
    re.IGNORECASE,
)
STRING_LITERAL_RE = re.compile(
    r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|`(?:\\.|[^`\\])*`',
    re.DOTALL,
)
INTERPOLATION_RE = re.compile(r"\$?\{([^{}]+)\}")
SAFE_DERIVATION_RE = re.compile(
    r"\b(?:question|query\w*|prompt|context\w*|snippet|fileName|filename|documentTitle)"
    r"\.(?:size|length|count|weight|score|relevanceScore)\s*\(\s*\)",
    re.IGNORECASE,
)
SOURCE_SUFFIXES = {".java", ".py", ".ts", ".tsx", ".js", ".jsx", ".vue"}
EXCLUDED_PARTS = {".git", "node_modules", "target", "dist", "docs", "test", "tests", "__tests__"}
EXCLUDED_FILES = {"check_sensitive_logs.py", "test_check_sensitive_logs.py"}


@dataclass(frozen=True)
class Finding:
    path: str
    line: int
    rule: str


def _collect_output_statements(text: str) -> list[tuple[int, str]]:
    lines = text.splitlines()
    statements: list[tuple[int, str]] = []
    index = 0
    while index < len(lines):
        if not OUTPUT_CALL_RE.search(lines[index]):
            index += 1
            continue

        start = index
        parts = [lines[index]]
        balance = lines[index].count("(") - lines[index].count(")")
        while balance > 0 and index + 1 < len(lines) and index - start < 11:
            index += 1
            parts.append(lines[index])
            balance += lines[index].count("(") - lines[index].count(")")
        statements.append((start + 1, "\n".join(parts)))
        index += 1
    return statements


def _expression_text(statement: str) -> str:
    interpolations = " ".join(INTERPOLATION_RE.findall(statement))
    without_literals = STRING_LITERAL_RE.sub("", statement)
    return SAFE_DERIVATION_RE.sub("", f"{without_literals} {interpolations}")


def scan_text(text: str, path: str = "<memory>") -> list[Finding]:
    findings: list[Finding] = []
    for line, statement in _collect_output_statements(text):
        expressions = _expression_text(statement)
        if SENSITIVE_IDENTIFIER_RE.search(expressions):
            findings.append(Finding(path, line, "sensitive-identifier"))
        elif HIGH_RISK_EXPRESSION_RE.search(expressions):
            findings.append(Finding(path, line, "exception-or-response-body"))
    return findings


def _repository_files(root: Path) -> list[Path]:
    completed = subprocess.run(
        ["git", "-C", str(root), "ls-files", "--cached", "--others", "--exclude-standard"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if completed.returncode != 0:
        raise RuntimeError(completed.stderr.strip() or "git ls-files failed")

    files: list[Path] = []
    for relative in completed.stdout.splitlines():
        path = root / relative
        if path.suffix not in SOURCE_SUFFIXES:
            continue
        if any(part in EXCLUDED_PARTS for part in path.relative_to(root).parts):
            continue
        if path.name in EXCLUDED_FILES or path.name.startswith("test_") or not path.is_file():
            continue
        files.append(path)
    return files


def scan_repository(root: Path) -> tuple[list[Finding], int]:
    findings: list[Finding] = []
    files = _repository_files(root)
    for path in files:
        relative = path.relative_to(root).as_posix()
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            text = path.read_text(encoding="utf-8", errors="replace")
        findings.extend(scan_text(text, relative))
    return findings, len(files)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="repository root (defaults to the parent of scripts/)",
    )
    args = parser.parse_args()
    root = args.root.resolve()

    try:
        findings, file_count = scan_repository(root)
    except RuntimeError as exc:
        print(f"[sensitive-log-gate] ERROR: {exc}", file=sys.stderr)
        return 2

    if findings:
        print(f"[sensitive-log-gate] FAIL: {len(findings)} suspicious output call(s)")
        for finding in findings:
            print(f"{finding.path}:{finding.line}: {finding.rule}")
        return 1

    print(f"[sensitive-log-gate] PASS: scanned {file_count} source file(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
