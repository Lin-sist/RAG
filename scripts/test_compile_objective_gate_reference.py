from __future__ import annotations

import contextlib
import copy
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import compile_objective_gate_reference as compiler
import review_objective_judge_profile as c20
import test_c20_profile_review as helpers


class ObjectiveReferenceCompileTest(unittest.TestCase):
    def setUp(self):
        helper = helpers.C20ProfileReviewTest(); helper.setUp()
        self.root = helper.root
        self.details = []
        self.metadata = []
        for index in (1, 2, 3):
            d = copy.deepcopy(helper.details)
            for sample, annotation in zip(d["samples"], helper.helper.samples, strict=True):
                if annotation["should_answer"]:
                    sample["metricCalculationDetails"]["keywordHits"] = index - 1
            m = {"repeat": {"index": index, "total": 3},
                "generatedAt": f"2026-10-18T00:00:0{index}+00:00",
                "datasetValidation": "VALID", "datasetReleaseIdentity": d["datasetReleaseIdentity"],
                "sampleSelection": {"ids": [s["id"] for s in d["samples"]], "count": 150},
                "topK": 5, "minScore": 0.3, "enableRerank": True,
                "git": {"head": "1" * 40, "clean": True},
                "evaluationSchema": "synthetic-c20-repeat-v1", "evalSetIdentity": {
                    k: d["datasetReleaseIdentity"]["questionSet"][k] for k in ("path", "bytes", "sha256")},
                "configSnapshot": {"fixtureSha256": "b" * 64},
                "knowledgeBase": {"privateName": "PRIVATE RAW"},
                "fixtures": [{"sha256": "c" * 64}],
                "claimMetricConfig": {"version": "synthetic"},
                "judgeContractConfig": {"version": "synthetic"},
                "runtimeFingerprint": {"provider": "synthetic", "model": "test"},
                "execution": {"timeout": 120, "retries": 0},
                "executionManifest": {"id": "synthetic", "sha256": "d" * 64}}
            d.update(claimMetricConfig=m["claimMetricConfig"], judgeContractConfig=m["judgeContractConfig"],
                     execution=copy.deepcopy(m["execution"]), runMetadata=copy.deepcopy(m))
            self.details.append(d); self.metadata.append(m)

    def compile(self, details=None, metadata=None):
        details = self.details if details is None else details
        metadata = self.metadata if metadata is None else metadata
        with tempfile.TemporaryDirectory(dir=self.root / "tmp") as folder:
            dp, mp = [], []
            for index, d in enumerate(details):
                p = Path(folder) / f"d{index}.json"; p.write_text(json.dumps(d), encoding="utf-8"); dp.append(p)
            for index, m in enumerate(metadata):
                p = Path(folder) / f"m{index}.json"; p.write_text(json.dumps(m), encoding="utf-8"); mp.append(p)
            return compiler.compile_reference(self.root, c20.DEFAULT_PROFILE, dp, mp)

    def rebind(self, index):
        self.details[index]["runMetadata"] = copy.deepcopy(self.metadata[index])

    def assert_no_distributions(self, result):
        self.assertTrue(all(x["distribution"] is None for x in result["rules"]))
        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertFalse(result["activationEligible"])

    def test_complete_synthetic_repeats_form_review_packet_without_activation(self):
        with patch("urllib.request.urlopen", side_effect=AssertionError("network forbidden")):
            r = self.compile()
        self.assertEqual(("FOR_REVIEW", 4), (r["status"], r["exitCode"]))
        self.assertEqual([], r["reasonCodes"])
        self.assertEqual([1, 2, 3], r["suppliedRepeatIndexes"])
        self.assertEqual(0, r["providerCalls"])
        self.assertFalse(r["providerExecutionVerified"])
        rule = next(x for x in r["rules"] if x["metric"] == "answer_keyword_hit_rate")
        self.assertEqual([0, 0.5, 1], [x["observed"] for x in rule["repeats"]])
        self.assertEqual([260] * 3, [x["denominator"] for x in rule["repeats"]])
        self.assertEqual({"minimum": 0, "median": 0.5, "maximum": 1, "spread": 1}, rule["distribution"])
        self.assertTrue(all(x["target"] is None and x["maxAbsoluteRegression"] is None
                            and x["approvalStatus"] == "PENDING" for x in r["rules"]))
        self.assertNotIn("PRIVATE RAW", json.dumps(r))
        self.assertNotIn(str(self.root), json.dumps(r))
        for index in range(3):
            self.metadata[index]["evalSetIdentity"]["path"] = str(
                self.root / self.metadata[index]["evalSetIdentity"]["path"])
            self.rebind(index)
        canonical = self.compile()
        self.assertEqual("FOR_REVIEW", canonical["status"])
        self.assertEqual(r["runs"][0]["runtimeIdentitySha256"],
                         canonical["runs"][0]["runtimeIdentitySha256"])

    def test_empty_and_missing_repeat_preserve_null_slots(self):
        for count in (0, 1, 2):
            with self.subTest(count=count):
                r = self.compile(self.details[:count], self.metadata[:count])
                self.assertEqual("INCOMPLETE", r["status"])
                self.assertEqual(list(range(count + 1, 4)), r["missingRepeatIndexes"])
                self.assert_no_distributions(r)
                self.assertEqual(3, len(r["rules"][0]["repeats"]))

    def test_single_run_total_one_and_retries_cannot_be_reference(self):
        self.metadata[0]["c18Execution"] = self.metadata[0].pop("execution")
        self.metadata[0]["c18Manifest"] = self.metadata[0].pop("executionManifest")
        self.details[0].pop("execution")
        self.details[0]["c18Execution"] = {"counts": {"ask": 150}, "attempts": []}
        self.metadata[0]["repeat"]["total"] = 1; self.rebind(0)
        self.details[0]["runCounts"]["retryCount"] = 31
        r = self.compile(self.details[:1], self.metadata[:1])
        self.assertEqual("INCOMPLETE", r["status"])
        self.assertIn("repeat_total_incompatible", r["reasonCodes"])
        self.assertIn("candidate_ineligible", r["reasonCodes"])
        self.assertEqual("retry_count_exceeded", r["runs"][0]["policyBlocker"])
        self.assert_no_distributions(r)

    def test_duplicate_artifacts_and_timestamps_are_not_independent(self):
        for mode in ("artifact", "timestamp", "index"):
            d, m = copy.deepcopy(self.details), copy.deepcopy(self.metadata)
            if mode == "artifact": d[1] = copy.deepcopy(d[0]); m[1] = copy.deepcopy(m[0])
            elif mode == "timestamp": m[1]["generatedAt"] = m[0]["generatedAt"]; d[1]["runMetadata"] = copy.deepcopy(m[1])
            else: m[1]["repeat"]["index"] = 1; d[1]["runMetadata"] = copy.deepcopy(m[1])
            r = self.compile(d, m)
            self.assertEqual("NOT_COMPARABLE", r["status"])
            self.assertIn("duplicate_run", r["reasonCodes"])
            self.assert_no_distributions(r)

    def test_incompatible_provider_model_or_config_does_not_mix_distributions(self):
        for mode in ("model", "execution"):
            d, m = copy.deepcopy(self.details), copy.deepcopy(self.metadata)
            if mode == "model": m[1]["runtimeFingerprint"]["model"] = "other"
            else: m[1]["execution"]["timeout"] = 10; d[1]["execution"] = copy.deepcopy(m[1]["execution"])
            d[1]["runMetadata"] = copy.deepcopy(m[1])
            r = self.compile(d, m)
            self.assertEqual("NOT_COMPARABLE", r["status"])
            self.assertIn("repeat_runtime_identity_drift", r["reasonCodes"])
            self.assert_no_distributions(r)

    def test_metadata_binding_selection_metric_and_dirty_git_fail_closed(self):
        for mode in ("binding", "selection", "metric", "git", "question_set"):
            d, m = copy.deepcopy(self.details), copy.deepcopy(self.metadata)
            if mode == "binding": m[0]["configSnapshot"]["fixtureSha256"] = "different"
            elif mode == "selection": m[0]["sampleSelection"]["ids"].reverse(); d[0]["runMetadata"] = copy.deepcopy(m[0])
            elif mode == "metric": d[0]["claimMetricConfig"] = {"version": "other"}
            elif mode == "question_set": m[0]["evalSetIdentity"]["sha256"] = "a" * 64; d[0]["runMetadata"] = copy.deepcopy(m[0])
            else: m[0]["git"]["clean"] = False; d[0]["runMetadata"] = copy.deepcopy(m[0])
            r = self.compile(d, m)
            self.assertEqual("NOT_COMPARABLE", r["status"])
            self.assert_no_distributions(r)

    def test_failed_observation_or_error_policy_prevents_complete_pack(self):
        for mode in ("metric", "policy", "missing"):
            d = copy.deepcopy(self.details)
            if mode == "metric": d[2]["samples"][0]["metricCalculationDetails"]["keywordHits"] = -1
            elif mode == "policy": d[2]["runCounts"]["retryCount"] = 1
            else: d[2]["samples"].pop()
            r = self.compile(d)
            self.assertNotEqual("FOR_REVIEW", r["status"])
            self.assert_no_distributions(r)

    def test_malformed_pairs_indexes_timestamp_and_json_are_safe_invalid(self):
        r = self.compile(self.details[:1], [])
        self.assertEqual("INVALID", r["status"])
        for value in (True, 0, 4, "PRIVATE"):
            m = copy.deepcopy(self.metadata); m[0]["repeat"]["index"] = value
            r = self.compile(metadata=m)
            self.assertEqual("INVALID", r["status"])
            self.assertNotIn("PRIVATE", json.dumps(r))
        self.metadata[0]["generatedAt"] = "PRIVATE timestamp"
        r = self.compile(); self.assertEqual("INVALID", r["status"])
        self.assertNotIn("PRIVATE", json.dumps(r))

    def test_cli_is_exclusive_and_refuses_input_alias_and_outside_root(self):
        with tempfile.TemporaryDirectory(dir=self.root / "tmp") as folder:
            dp, mp, out = Path(folder) / "d.json", Path(folder) / "m.json", Path(folder) / "out.json"
            dp.write_text(json.dumps(self.details[0])); mp.write_text(json.dumps(self.metadata[0]))
            before = dp.read_bytes()
            args = ["--details", str(dp), "--metadata", str(mp), "--output", str(out)]
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(4, compiler.main(args))
                saved = out.read_bytes()
                self.assertEqual(2, compiler.main(args))
                self.assertEqual(2, compiler.main(["--details", str(dp), "--metadata", str(mp), "--output", str(dp)]))
                self.assertEqual(2, compiler.main(["--profile", str(self.root.parent / "PRIVATE.json")]))
            self.assertEqual(before, dp.read_bytes()); self.assertEqual(saved, out.read_bytes())


if __name__ == "__main__":
    unittest.main()
