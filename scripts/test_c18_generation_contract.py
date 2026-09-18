from __future__ import annotations

import copy
import json
import sys
import unittest
from unittest import mock
from argparse import Namespace
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import c18_generation_contract as c18
import run_rag_eval as runner


ROOT = Path(__file__).resolve().parents[1]


class C18GenerationContractTest(unittest.TestCase):
    def test_retrieval_failure_never_calls_ask_in_c18(self) -> None:
        args = Namespace(skip_ask=False, base_url="http://localhost", kb_id=1,
                         top_k=5, min_score=0.3, enable_rerank=True, timeout=1,
                         judge_mode="off")
        sample = {"id": "fact-001", "question": "synthetic", "should_answer": True}
        for response in (RuntimeError("HTTP 429"), {"code": 200, "data": {"status": "error"}}):
            with self.subTest(response=type(response).__name__), mock.patch.object(
                runner, "C18_GUARD", c18.C18BudgetGuard(self.manifest["budgets"]["canary"])
            ), mock.patch.object(runner, "call_json", side_effect=response if isinstance(response, Exception) else None,
                                 return_value=response), mock.patch.object(runner, "call_ask_with_retries") as ask:
                result = runner.run_sample(sample, args, "synthetic")
                ask.assert_not_called()
                self.assertTrue(result.retrieval_error)
                self.assertTrue(result.skipped_ask)

    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = c18.load_manifest(ROOT)

    def test_manifest_validates_and_plan_is_offline(self) -> None:
        plan = c18.build_plan(self.manifest, "canary")
        self.assertEqual(plan["status"], "OFFLINE_VERIFIED")
        self.assertFalse(plan["authorization"]["liveAuthorized"])
        self.assertEqual(plan["callBudget"]["queryEmbeddingUpperBound"], 34)
        self.assertEqual(len(plan["selection"]["ids"]), 5)

    def test_full_plan_keeps_exact_order_and_budget(self) -> None:
        plan = c18.build_plan(self.manifest, "full")
        self.assertEqual(plan["selection"]["count"], 150)
        self.assertEqual(plan["callBudget"]["queryEmbeddingUpperBound"], 1492)
        self.assertEqual(plan["selection"]["ids"][0], "fact-001")
        self.assertEqual(plan["selection"]["ids"][-1], "no-answer-020")

    def test_manifest_budget_drift_fails_closed(self) -> None:
        raw = json.loads((ROOT / c18.DEFAULT_MANIFEST).read_text(encoding="utf-8"))
        mutated = copy.deepcopy(raw)
        mutated["budgets"]["full"]["ask"] = 149
        with self.assertRaisesRegex(c18.C18ContractError, "c18_budget_identity_invalid"):
            c18.validate_manifest(ROOT, mutated)

    def test_w1_tooling_source_drift_fails_closed(self) -> None:
        raw = json.loads((ROOT / c18.DEFAULT_MANIFEST).read_text(encoding="utf-8"))
        mutated = copy.deepcopy(raw)
        mutated["toolingSourceSha256"]["scripts/c18_generation_contract.py"] = "0" * 64
        with self.assertRaisesRegex(c18.C18ContractError, "c18_tooling_source_drift"):
            c18.validate_manifest(ROOT, mutated)

    def test_runner_conflict_and_pacing_fail_closed(self) -> None:
        args = type(
            "Args",
            (),
            {
                "arm_manifest": "",
                "reference_manifest": "",
                "keep_existing": True,
                "include_ask": True,
                "skip_ask": False,
                "judge_mode": "off",
                "repeat": 1,
                "max_ask_retries": 0,
                "retry_ask_timeouts": False,
                "retrieval_delay_seconds": 2.2,
                "ask_delay_seconds": 2.2,
                "top_k": 5,
                "min_score": 0.3,
                "enable_rerank": True,
                "sample_limit": 0,
                "sample_ids": list(c18.CANARY_IDS),
                "report": "tmp/eval/c18/report.md",
                "details_json": "tmp/eval/c18/details.json",
                "metadata_json": "tmp/eval/c18/metadata.json",
                "no_overwrite": True,
            },
        )()
        with self.assertRaisesRegex(c18.C18ContractError, "c18_pacing_interval_required"):
            args.ask_delay_seconds = 2.1
            c18.validate_runner_configuration(args, self.manifest, "canary", list(c18.CANARY_IDS), [1])

    def test_guard_rejects_before_a_sixth_request(self) -> None:
        guard = c18.C18BudgetGuard(self.manifest["budgets"]["canary"])
        for _ in range(5):
            guard.before_request("debugRetrieve")
        with self.assertRaisesRegex(c18.C18ContractError, "c18_request_rejected"):
            guard.before_request("debugRetrieve")
        self.assertEqual(guard.snapshot()["counts"]["debugRetrieve"], 5)
        self.assertEqual(guard.snapshot()["rejections"][0]["requestSent"], False)

    def test_runtime_fingerprint_requires_exact_safe_descriptor(self) -> None:
        import tempfile

        value = {
            "schemaVersion": c18.SCHEMA_VERSION,
            **c18.EXPECTED_RUNTIME,
            "verificationBasis": "operator-confirmed",
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.json"
            path.write_text(json.dumps(value), encoding="utf-8")
            fingerprint = c18.validate_runtime_fingerprint(path, self.manifest)
        self.assertEqual(fingerprint["verificationBasis"], "operator-confirmed")
        self.assertEqual(len(fingerprint["sha256"]), 64)


if __name__ == "__main__":
    unittest.main()
