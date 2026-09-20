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
    def test_generated_no_answer_is_not_a_generation_bypass(self) -> None:
        response = {"contexts": [{"content": "synthetic"}],
                    "metadata": {"status": "no_result", "model": c18.EXPECTED_RUNTIME["model"]}}
        facts = c18.execution_facts(response, 1, 0, 0, {})
        self.assertEqual((facts["generationCalls"], facts["generationBypassCount"]), (1, 0))
        facts = c18.execution_facts({"metadata": {"status": "no_result"}}, 1, 0, 0, {})
        self.assertEqual((facts["generationCalls"], facts["generationBypassCount"]), (0, 1))

    def test_execution_facts_include_actual_embedding_observations(self) -> None:
        debug = {"diagnostics": {
            "queryEmbeddingLogicalCallCount": 2,
            "queryEmbeddingCacheHitCount": 1,
            "queryEmbeddingProviderCallCount": 1,
            "queryEmbeddingProviderFallbackCount": 0,
        }}
        ask = {"contexts": [{"content": "synthetic"}], "metadata": {
            "status": "success",
            "model": c18.EXPECTED_RUNTIME["model"],
            "queryEmbeddingLogicalCallCount": 3,
            "queryEmbeddingCacheHitCount": 0,
            "queryEmbeddingProviderCallCount": 3,
            "queryEmbeddingProviderFallbackCount": 0,
        }}

        facts = c18.execution_facts(ask, 1, 0, 0, {}, debug_response=debug)

        self.assertEqual(facts["queryEmbeddingLogicalCallCount"], 5)
        self.assertEqual(facts["queryEmbeddingCacheHitCount"], 1)
        self.assertEqual(facts["queryEmbeddingProviderCallCount"], 4)
        self.assertEqual(facts["queryEmbeddingObservation"], "DIRECT_RUNTIME_DIAGNOSTICS")

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
        self.assertEqual(plan["callBudget"]["queryEmbeddingUpperBound"], 136)
        self.assertEqual(len(plan["selection"]["ids"]), 5)

    def test_full_plan_keeps_exact_order_and_budget(self) -> None:
        plan = c18.build_plan(self.manifest, "full")
        self.assertEqual(plan["selection"]["count"], 150)
        self.assertEqual(plan["callBudget"]["queryEmbeddingUpperBound"], 5968)
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

    def test_runner_requires_distinct_output_paths(self) -> None:
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
                "report": "tmp/eval/c18/shared.json",
                "details_json": "tmp/eval/c18/shared.json",
                "metadata_json": "tmp/eval/c18/metadata.json",
                "no_overwrite": True,
            },
        )()
        with self.assertRaisesRegex(c18.C18ContractError, "c18_output_paths_not_distinct"):
            c18.validate_runner_configuration(args, self.manifest, "canary", list(c18.CANARY_IDS), [1])

    def test_guard_rejects_before_a_sixth_request(self) -> None:
        guard = c18.C18BudgetGuard(self.manifest["budgets"]["canary"])
        for _ in range(20):
            guard.before_request("debugRetrieve")
        with self.assertRaisesRegex(c18.C18ContractError, "c18_request_rejected"):
            guard.before_request("debugRetrieve")
        self.assertEqual(guard.snapshot()["counts"]["debugRetrieve"], 20)
        self.assertEqual(guard.snapshot()["rejections"][0]["requestSent"], False)

    def test_exact_premature_close_provider_error_retries_and_is_ledgered(self) -> None:
        failure = {
            "data": {
                "metadata": {
                    "status": "error",
                    "llmErrorCategory": "network",
                    "llmErrorType": "PrematureCloseException",
                }
            }
        }
        success = {"data": {"metadata": {"status": "success", "model": c18.EXPECTED_RUNTIME["model"]}}}
        guard = c18.C18BudgetGuard(self.manifest["budgets"]["canary"])
        with mock.patch.object(runner, "C18_GUARD", guard), mock.patch.object(
            runner, "_call_json_once", side_effect=[failure, success]
        ), mock.patch.object(runner.time, "sleep") as sleep:
            response = runner.call_json("POST", "http://localhost/api/qa/ask", {}, "token", 1)
        self.assertEqual(response, success)
        self.assertEqual(len(guard.attempts), 2)
        self.assertEqual(guard.attempts[0]["providerErrorType"], "PrematureCloseException")
        self.assertEqual(guard.attempts[0]["providerErrorCategory"], "network")
        self.assertTrue(guard.attempts[0]["retry"])
        self.assertFalse(guard.attempts[1]["retry"])
        sleep.assert_called_once_with(5)

    def test_other_network_provider_error_does_not_retry(self) -> None:
        failure = {
            "data": {
                "metadata": {
                    "status": "error",
                    "llmErrorCategory": "network",
                    "llmErrorType": "ConnectTimeoutException",
                }
            }
        }
        guard = c18.C18BudgetGuard(self.manifest["budgets"]["canary"])
        with mock.patch.object(runner, "C18_GUARD", guard), mock.patch.object(
            runner, "_call_json_once", return_value=failure
        ), mock.patch.object(runner.time, "sleep") as sleep:
            response = runner.call_json("POST", "http://localhost/api/qa/ask", {}, "token", 1)
        self.assertEqual(response, failure)
        self.assertEqual(len(guard.attempts), 1)
        self.assertFalse(guard.attempts[0]["retry"])
        sleep.assert_not_called()

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
