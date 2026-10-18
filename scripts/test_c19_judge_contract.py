from __future__ import annotations

import copy
import json
from pathlib import Path
import unittest
from unittest import mock
import urllib.error

import c19_judge_contract as c19
import run_judge_calibration as calibration

ROOT = Path(__file__).resolve().parents[1]


class C19ContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.manifest = c19.load_manifest(ROOT)

    def test_local_plan_and_blocked_preflight_do_not_read_credentials_or_call_network(self) -> None:
        with mock.patch.object(c19.os, "getenv", side_effect=AssertionError("credential read")), \
             mock.patch.object(calibration.urllib.request, "urlopen", side_effect=AssertionError("network")):
            for profile, count in (("canary", 4), ("full", 72)):
                plan = c19.build_plan(ROOT, self.manifest, profile)
                self.assertEqual(count, plan["calibrationPlan"]["estimatedJudgeCalls"])
                self.assertEqual(0, plan["actualProviderCalls"])
            ready = c19.preflight(ROOT, self.manifest, "canary")
        self.assertEqual("BLOCKED", ready["status"])
        self.assertIn("model_not_selected", ready["reasonCodes"])
        self.assertFalse(ready["businessDataOutbound"])

    def test_url_credentials_query_fragment_and_noncanonical_endpoints_are_refused_safely(self) -> None:
        for endpoint in ("https://private:password@example.test/v1", "https://example.test/v1?key=private",
                         "https://example.test/v1#private", "http://example.test/v1", "https://example.test/a/../v1"):
            with self.subTest(endpoint=endpoint), self.assertRaises(c19.ContractError) as error:
                c19.endpoint_identity(endpoint)
            self.assertNotIn("private", str(error.exception))
        self.assertEqual("https://example.test/v1", c19.endpoint_identity("https://EXAMPLE.test:443/v1/"))

    def test_manifest_rejects_unknown_fields_hash_and_numeric_drift(self) -> None:
        for edit in (lambda m: m.update(extra=True),
                     lambda m: m["sourceSha256"].update({c19.SOURCE_PATHS[0]: "0" * 64}),
                     lambda m: m["judge"].update(retries=1),
                     lambda m: m["judge"].update(timeoutSeconds=float("nan")),
                     lambda m: m["judge"].update(maxOutputTokens=True),
                     lambda m: m["judgeContractConfig"].update(faithfulnessThreshold=0.5)):
            value = copy.deepcopy(self.manifest)
            edit(value)
            with self.assertRaises(c19.ContractError):
                c19.validate_manifest(ROOT, value)

    def test_budget_rejects_fifth_reservation(self) -> None:
        budget = c19.AttemptBudget(4)
        self.assertEqual([1, 2, 3, 4], [budget.reserve() for _ in range(4)])
        with self.assertRaisesRegex(c19.ContractError, "attempt_budget_exhausted"):
            budget.reserve()
        self.assertEqual(4, budget.used)

    def test_live_refused_before_transport_or_directory_creation(self) -> None:
        with mock.patch.object(calibration.urllib.request, "urlopen") as network:
            with self.assertRaisesRegex(c19.ContractError, "live_preflight_blocked"):
                c19.execute_run(ROOT, self.manifest, "canary", Path("tmp/eval/c19-must-not-exist"))
            network.assert_not_called()

    def test_payload_is_bounded_and_redirects_are_rejected_without_retry(self) -> None:
        args = c19.judge_args(self.manifest, "synthetic-credential")
        opener = mock.Mock()
        opener.open.side_effect = urllib.error.HTTPError("https://example.test/v1", 302, "private", {}, None)
        with mock.patch.object(calibration.urllib.request, "build_opener", return_value=opener) as factory:
            with self.assertRaises(calibration.CalibrationCallError):
                calibration.call_live_judge({"question": "synthetic?", "answer": "synthetic."},
                    [{"source": "fixture", "content": "context"}], args)
        self.assertEqual(1, opener.open.call_count)
        payload = json.loads(opener.open.call_args.args[0].data)
        self.assertEqual(1024, payload["max_tokens"])
        self.assertEqual(60, opener.open.call_args.kwargs["timeout"])
        self.assertEqual(302, args.judge_http_status)
        self.assertIsNone(factory.call_args.args[0].redirect_request(None, None, None, None, None, None))

    def test_paths_and_duplicate_json_keys_fail_closed(self) -> None:
        with self.assertRaises(c19.ContractError):
            c19.repo_path(ROOT, "../outside.json")
        with self.assertRaises(c19.ContractError):
            c19.run_path(ROOT, Path("docs/eval/reports/raw"))
        with self.assertRaisesRegex(c19.ContractError, "duplicate_json_key"):
            c19._pairs([("same", 1), ("same", 2)])

    def test_cli_conflicting_runtime_settings_are_refused_before_calls(self) -> None:
        args = calibration.parse_args
        with mock.patch("sys.argv", ["run_judge_calibration.py", "--c19-manifest", "--plan-only",
                                     "--judge-timeout", "61"]):
            parsed = args()
        with mock.patch.object(calibration.urllib.request, "urlopen") as network, mock.patch("builtins.print"):
            self.assertEqual(2, c19.main_for_runner(ROOT, parsed))
            network.assert_not_called()


if __name__ == "__main__":
    unittest.main()
