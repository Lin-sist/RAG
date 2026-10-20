"""Local test fixtures only; never refresh accepted evidence or authorize a run."""
from __future__ import annotations

from contextlib import contextmanager
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
from uuid import uuid4

import c18_budget_contract as budget
import c18_generation_contract as c18


REPO_ROOT = Path(__file__).resolve().parents[1]


@contextmanager
def workspace_temporary_directory(root: Path):
    """Fail on denied workspace writes instead of Windows mkdtemp's retry loop."""
    directory = root / "tmp" / ("eval-unit-" + uuid4().hex)
    try:
        directory.mkdir(mode=0o700)
    except PermissionError as exc:
        raise PermissionError("workspace tmp is not writable; run tests with workspace write permission") from exc
    try:
        yield str(directory)
    finally:
        shutil.rmtree(directory)


@contextmanager
def isolated_c18_repository():
    """Make a synthetic, internally consistent snapshot for contract unit tests.

    Only temporary copies get new source/audit hashes. The checkout's frozen
    audit, manifest and accepted reports remain untouched. This snapshot is
    neither a re-audit of today's call graph nor live execution evidence.
    """
    with tempfile.TemporaryDirectory(prefix="rag-c18-unit-") as directory:
        root = Path(directory)

        def copy_file(relative):
            source = (REPO_ROOT / relative).resolve()
            if not source.is_relative_to(REPO_ROOT):
                raise ValueError("test fixture path outside repository")
            target = root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read_bytes())

        def copy_dataset(relative):
            copy_file(relative)
            manifest = json.loads((REPO_ROOT / relative).read_text(encoding="utf-8"))
            for field in ("questionSet", "sampleSchema", "annotationReview", "seedQuestionSet"):
                if field in manifest:
                    copy_file(manifest[field]["path"])
            for fixture in manifest["fixtures"]:
                copy_file(fixture["path"])
            if "seedReleaseManifest" in manifest:
                copy_dataset(manifest["seedReleaseManifest"]["path"])

        for relative in (*budget.SOURCE_PATHS, *c18.TOOLING_SOURCE_PATHS,
                         budget.AUDIT_PATH, c18.DEFAULT_MANIFEST):
            copy_file(relative)
        copy_dataset("docs/eval/dataset-manifest.json")

        audit_path = root / budget.AUDIT_PATH
        audit = budget.read_json(audit_path)
        audit["sourceSha256"] = {
            relative: hashlib.sha256((root / relative).read_text(encoding="utf-8").encode("utf-8")).hexdigest()
            for relative in budget.SOURCE_PATHS
        }
        audit_path.write_text(json.dumps(audit, indent=2) + "\n", encoding="utf-8")
        manifest_path = root / c18.DEFAULT_MANIFEST
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["budgetAudit"]["sha256"] = c18.sha256_file(audit_path)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        yield root
