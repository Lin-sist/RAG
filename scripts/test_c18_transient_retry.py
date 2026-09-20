import unittest
from unittest.mock import patch
import run_rag_eval as runner
import c18_generation_contract as c18


class TransientRetryTest(unittest.TestCase):
    def run_calls(self, responses, kind="ask"):
        guard = c18.C18BudgetGuard(dict(debugRetrieve=20, ask=20, generationUpperBound=20, llmJudge=0))
        path = "ask" if kind == "ask" else "debug/retrieve"
        with patch.object(runner, "C18_GUARD", guard), patch.object(runner, "_call_json_once", side_effect=responses) as call, patch.object(runner.time, "sleep") as sleep:
            try:
                value = runner.call_json("POST", "http://localhost/api/qa/" + path, {}, None, 10)
            except Exception as error:
                value = error
        return value, guard.attempts, call.call_count, sleep.call_args_list

    def test_provider_503_recovers_with_full_ledger(self):
        failure = {"data": {"metadata": {"status": "error", "llmHttpStatus": 503}}}
        success = {"data": {"metadata": {"status": "success", "model": c18.EXPECTED_RUNTIME["model"]}}}
        value, events, count, sleeps = self.run_calls([failure, failure, success])
        self.assertEqual(value, success)
        self.assertEqual(count, 3)
        self.assertEqual([e["retry"] for e in events], [True, True, False])
        self.assertEqual([c.args[0] for c in sleeps], [5, 10])
        self.assertEqual(sum(e["generationHttpAttempts"] for e in events), 3)

    def test_fourth_failure_stops(self):
        error = runner.ApiCallError("busy", 503)
        value, events, count, sleeps = self.run_calls([error] * 5)
        self.assertIs(value, error)
        self.assertEqual(count, 4)
        self.assertFalse(events[-1]["retry"])
        self.assertEqual([c.args[0] for c in sleeps], [5, 10, 20])

    def test_retrieval_429_recovers(self):
        value, events, count, sleeps = self.run_calls([runner.ApiCallError("limited", 429), {"data": {}}], "debugRetrieve")
        self.assertEqual(count, 2)
        self.assertEqual(events[0]["httpStatus"], 429)
        self.assertTrue(events[0]["retry"])
        self.assertEqual(events[1]["generationHttpAttempts"], 0)

    def test_guard_blocks_network_on_exhaustion(self):
        guard = c18.C18BudgetGuard(dict(debugRetrieve=0, ask=0, generationUpperBound=0, llmJudge=0))
        with patch.object(runner, "C18_GUARD", guard), patch.object(runner.urllib.request, "urlopen") as network:
            with self.assertRaises(c18.C18ContractError):
                runner.call_json("POST", "http://localhost/api/qa/ask", {}, None, 10)
        network.assert_not_called()

    def test_other_failures_never_retry(self):
        for status in (None, 401, 500, 502, 504):
            with self.subTest(status=status):
                error = runner.ApiCallError("failure", status)
                value, events, count, sleeps = self.run_calls([error])
                self.assertIs(value, error)
                self.assertEqual(count, 1)
                self.assertEqual(len(events), 1)
                self.assertFalse(sleeps)

    def test_non_c18_unchanged(self):
        with patch.object(runner, "C18_GUARD", None), patch.object(runner, "_call_json_once", side_effect=runner.ApiCallError("busy", 503)) as call:
            with self.assertRaises(runner.ApiCallError):
                runner.call_json("POST", "http://localhost/api/qa/ask", {}, None, 10)
        self.assertEqual(call.call_count, 1)

    def test_c18_ask_rejects_missing_or_drifted_generation_model_immediately(self):
        for metadata in (
            {"status": "success"},
            {"status": "success", "model": "unexpected/model"},
        ):
            with self.subTest(metadata=metadata):
                response = {"data": {"answer": "synthetic", "contexts": [{}], "metadata": metadata}}
                value, events, count, sleeps = self.run_calls([response])
                self.assertIsInstance(value, c18.C18ContractError)
                self.assertEqual(value.code, "c18_generation_runtime_identity_mismatch")
                self.assertEqual(count, 1)
                self.assertEqual(len(events), 1)
                self.assertFalse(sleeps)
