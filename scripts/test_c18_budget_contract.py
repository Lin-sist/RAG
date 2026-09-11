import unittest
import copy
import json
import tempfile
from pathlib import Path
from unittest.mock import patch

import c18_budget_contract as contract


class C18BudgetContractTest(unittest.TestCase):
    def setUp(self):
        self.root = Path(__file__).resolve().parents[1]
        self.audit = contract.read_json(self.root / contract.AUDIT_PATH)

    def test_full_plan_counts_nested_calls_and_is_not_live_authorization(self):
        plan = contract.build_plan(Path(__file__).resolve().parents[1], "full")
        self.assertEqual(150, plan["callBudget"]["ask"])
        self.assertEqual(1492, plan["callBudget"]["queryEmbeddingUpperBound"])
        self.assertEqual(150, plan["callBudget"]["generationUpperBound"])
        self.assertFalse(plan["liveAuthorized"])
        self.assertEqual(2.2, plan["minimumRequestIntervalSeconds"])

    def test_canary_excludes_full_calls_and_counts_all_fallback_variants(self):
        plan = contract.build_plan(self.root, "canary")
        self.assertEqual(34, plan["callBudget"]["queryEmbeddingUpperBound"])
        self.assertEqual(12, plan["callBudget"]["explanatoryFallbackVariants"])
        self.assertEqual(5, len(plan["selection"]))
        self.assertFalse(plan["executionReady"])

    def test_changed_source_blocks_even_if_sample_counts_are_unchanged(self):
        original = Path.read_text
        def changed(path, *args, **kwargs):
            text = original(path, *args, **kwargs)
            return text + "\n// changed" if path.name == "RAGServiceImpl.java" else text
        with patch.object(Path, "read_text", changed):
            with self.assertRaisesRegex(contract.BudgetContractError, "budget_source_drift"):
                contract.build_plan(self.root, "full")

    def test_missing_duplicate_or_reordered_samples_are_rejected(self):
        for action in ("missing", "duplicate", "reordered"):
            audit = copy.deepcopy(self.audit)
            if action == "missing": audit["samples"].pop()
            elif action == "duplicate": audit["samples"][1] = audit["samples"][0]
            else: audit["samples"].reverse()
            with self.subTest(action=action), self.assertRaises(contract.BudgetContractError):
                contract.validate_audit(self.root, audit)

    def test_bool_or_omitted_fallback_budget_is_not_a_number(self):
        for value in (True, 0, -1, 902):
            audit = copy.deepcopy(self.audit)
            audit["samples"][0]["queryEmbeddingUpperBound"] = value
            with self.subTest(value=value), self.assertRaises(contract.BudgetContractError):
                contract.validate_audit(self.root, audit)

    def test_unknown_sensitive_fields_are_not_forwarded(self):
        audit = copy.deepcopy(self.audit)
        audit["samples"][0]["question"] = "PRIVATE_CONTENT"
        with self.assertRaisesRegex(contract.BudgetContractError, "sample_fields_invalid"):
            contract.validate_audit(self.root, audit)

    def test_duplicate_json_keys_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "audit.json"
            path.write_text('{"full":1,"full":2}', encoding="utf-8")
            with self.assertRaisesRegex(contract.BudgetContractError, "duplicate_json_key"):
                contract.read_json(path)

    def test_cli_never_overwrites_an_existing_artifact(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "plan.json"
            path.write_text("existing evidence", encoding="utf-8")
            result = contract.main(["--repo-root", str(self.root), "--mode", "full", "--output-json", str(path)])
            self.assertEqual(2, result)
            self.assertEqual("existing evidence", path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
