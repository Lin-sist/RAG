from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

import c19_judge_contract as c19
import compile_judge_calibration_evidence as compiler

ROOT = Path(__file__).resolve().parents[1]


class C19EvidenceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.manifest = c19.load_manifest(ROOT)
        boundary = ROOT / "tmp/eval"
        boundary.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix="c19-unit-", dir=boundary)
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name) / "run"
        self.calls = 0

    def transport(self, case: dict, contexts: list, args: object) -> str:
        self.calls += 1
        self.assertEqual({"question", "answer"}, set(case))
        self.assertTrue(contexts)
        self.assertNotIn("goldFaithful", json.dumps(case))
        return json.dumps({"faithfulnessScore": 0.9, "relevanceScore": 0.9,
                           "pass": False, "reason": "PRIVATE_RESPONSE_SENTINEL"})

    def run_fixture(self, profile: str = "canary", transport=None) -> dict:
        c19.execute_run(ROOT, self.manifest, profile, self.directory,
                        transport=transport or self.transport, sleep=lambda _: None)
        return compiler.compile_run(ROOT, self.manifest, self.directory)

    def rewrite_completion(self) -> None:
        path = self.directory / "completion.json"
        completion = c19.read_json(path)
        completion["ledgerSha256"] = c19.file_hash(self.directory / "ledger.jsonl")
        completion["identitySha256"] = c19.file_hash(self.directory / "identity.json")
        path.write_text(json.dumps(completion), encoding="utf-8")

    def test_complete_full_recomputed_low_agreement_and_safe_output(self) -> None:
        result = self.run_fixture("full")
        self.assertEqual(72, self.calls)
        self.assertEqual("COMPLETE", result["status"])
        self.assertEqual(0.25, result["metrics"]["jointPass"]["agreement"])
        self.assertEqual(1.0, result["metrics"]["repeatConsistentCaseRate"])
        self.assertEqual(72, result["metrics"]["providerPassMismatchCount"])
        self.assertEqual("SYNTHETIC", result["evidenceKind"])
        self.assertFalse(result["liveEvidenceEligible"])
        self.assertEqual(0, result["actual"]["providerAttemptsUpperBound"])
        rendered = json.dumps(result)
        self.assertNotIn("PRIVATE_RESPONSE_SENTINEL", rendered)
        self.assertNotIn(str(ROOT), rendered)
        self.assertNotIn('"rawContent"', rendered)

    def test_first_parse_error_stops_and_suppresses_subset_agreement(self) -> None:
        def invalid(case, contexts, args):
            if self.calls == 1:
                self.calls += 1
                return '{"faithfulnessScore":"0.9","relevanceScore":0.9}'
            return self.transport(case, contexts, args)
        result = self.run_fixture(transport=invalid)
        self.assertEqual(2, self.calls)
        self.assertEqual("INCOMPLETE", result["status"])
        self.assertEqual(2, result["actual"]["unattemptedCount"])
        self.assertEqual(0.25, result["metrics"]["parseCoverage"])
        self.assertEqual("N/A", result["metrics"]["jointPass"]["agreement"])
        self.assertEqual(1, result["metrics"]["errorCategoryHistogram"]["invalid_judge_payload"])

    def test_failure_is_persisted_without_sensitive_exception_message(self) -> None:
        def failure(*_):
            raise RuntimeError("PRIVATE_EXCEPTION_SENTINEL")
        result = self.run_fixture(transport=failure)
        self.assertEqual("INCOMPLETE", result["status"])
        self.assertEqual(1, result["actual"]["reservedAttempts"])
        self.assertNotIn("PRIVATE_EXCEPTION_SENTINEL", (self.directory / "ledger.jsonl").read_text())

    def test_unknown_outcome_survives_interrupt_and_cannot_resume(self) -> None:
        def interrupt(*_):
            raise KeyboardInterrupt()
        with self.assertRaises(KeyboardInterrupt):
            self.run_fixture(transport=interrupt)
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("INCOMPLETE", result["status"])
        self.assertEqual(1, result["actual"]["outcomeUnknownCount"])
        self.assertEqual(0, result["actual"]["recordedOutcomes"])
        with self.assertRaisesRegex(c19.ContractError, "run_directory_exists"):
            self.run_fixture()

    def test_no_overwrite_preserves_raw_artifacts(self) -> None:
        self.run_fixture()
        before = c19.file_hash(self.directory / "ledger.jsonl")
        with self.assertRaisesRegex(c19.ContractError, "run_directory_exists"):
            self.run_fixture()
        self.assertEqual(before, c19.file_hash(self.directory / "ledger.jsonl"))

    def test_missing_finalization_and_truncated_tail_suppress_quality(self) -> None:
        self.run_fixture()
        (self.directory / "completion.json").unlink()
        with (self.directory / "ledger.jsonl").open("ab") as stream:
            stream.write(b'{"event":"start"')
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("INCOMPLETE", result["status"])
        self.assertEqual("N/A", result["metrics"]["faithfulness"]["agreement"])

    def test_hash_tamper_and_untrusted_summary_do_not_pass(self) -> None:
        self.run_fixture()
        path = self.directory / "completion.json"
        data = c19.read_json(path)
        data["summary"] = {"calibrationStatus": "COMPLETE"}
        path.write_text(json.dumps(data), encoding="utf-8")
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("INVALID", result["status"])
        self.assertEqual({}, result["metrics"])

    def test_duplicate_and_unexpected_keys_are_not_comparable(self) -> None:
        self.run_fixture()
        path = self.directory / "ledger.jsonl"
        events = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]
        for event in events[-2:]:
            event["caseId"] = events[0]["caseId"]
        path.write_text("".join(json.dumps(e) + "\n" for e in events), encoding="utf-8")
        self.rewrite_completion()
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("NOT_COMPARABLE", result["status"])
        self.assertEqual("N/A", result["metrics"]["jointPass"]["agreement"])
        self.assertEqual(1, result["metrics"]["duplicateObservationCount"])
        self.assertEqual(1, result["metrics"]["missingObservationCount"])

    def test_valid_scores_cannot_hide_raw_hash_tamper(self) -> None:
        self.run_fixture()
        path = self.directory / "ledger.jsonl"
        path.write_bytes(path.read_bytes().replace(b'0.9', b'0.8'))
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("INVALID", result["status"])
        self.assertEqual(["artifact_hash_mismatch"], result["reasonCodes"])

    def test_missing_ledger_does_not_claim_zero_calls_or_quality(self) -> None:
        self.run_fixture()
        (self.directory / "ledger.jsonl").unlink()
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("INCOMPLETE", result["status"])
        self.assertEqual({}, result["actual"])
        self.assertFalse(result["liveEvidenceEligible"])

    def test_runtime_identity_drift_is_not_hidden_by_good_scores(self) -> None:
        self.run_fixture()
        path = self.directory / "identity.json"
        identity = c19.read_json(path)
        identity["executionConfig"]["baseUrl"] = "https://example.test/changed"
        path.write_text(json.dumps(identity), encoding="utf-8")
        self.rewrite_completion()
        result = compiler.compile_run(ROOT, self.manifest, self.directory)
        self.assertEqual("INVALID", result["status"])
        self.assertFalse(result["comparisonEligible"])


if __name__ == "__main__":
    unittest.main()
