from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import c18_budget_contract as budget
import c18_generation_contract as c18
from eval_test_support import REPO_ROOT, isolated_c18_repository, workspace_temporary_directory


class EvalTestSupportTest(unittest.TestCase):
    def test_denied_workspace_creation_fails_once_without_tempfile_retry(self):
        with patch.object(Path, "mkdir", side_effect=PermissionError("denied")) as mkdir:
            with self.assertRaisesRegex(PermissionError, "workspace tmp is not writable"):
                with workspace_temporary_directory(REPO_ROOT):
                    self.fail("denied directory must not be yielded")
        mkdir.assert_called_once()

    def test_workspace_directory_is_removed_when_the_test_body_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "tmp").mkdir()
            with self.assertRaisesRegex(RuntimeError, "test body"):
                with workspace_temporary_directory(root) as folder:
                    created = Path(folder)
                    (created / "input.json").write_text("{}", encoding="utf-8")
                    raise RuntimeError("test body")
            self.assertFalse(created.exists())

    def test_isolated_audit_validates_and_still_rejects_drift_without_rewriting_history(self):
        protected = (budget.AUDIT_PATH, c18.DEFAULT_MANIFEST)
        originals = {str(path): (REPO_ROOT / path).read_bytes() for path in protected}
        with isolated_c18_repository() as root:
            audit = budget.read_json(root / budget.AUDIT_PATH)
            budget.validate_audit(root, audit)
            manifest = c18.load_manifest(root)
            self.assertFalse(c18.build_plan(manifest, "full")["authorization"]["liveAuthorized"])
            source = root / "rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java"
            source.write_bytes(source.read_bytes() + b"\n// synthetic source drift\n")
            with self.assertRaisesRegex(budget.BudgetContractError, "budget_source_drift"):
                budget.validate_audit(root, audit)
            with self.assertRaisesRegex(c18.C18ContractError, "c18_budget_audit_invalid"):
                c18.load_manifest(root)
        for relative, original in originals.items():
            self.assertEqual(original, (REPO_ROOT / relative).read_bytes())


if __name__ == "__main__":
    unittest.main()
