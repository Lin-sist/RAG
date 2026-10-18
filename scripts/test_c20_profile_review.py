from __future__ import annotations

import contextlib
import copy
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import evaluate_quality_gate as gate
import review_objective_judge_profile as c20
import test_evaluate_quality_gate as existing


class C20ProfileReviewTest(unittest.TestCase):
    def setUp(self) -> None:
        self.root = Path(__file__).resolve().parents[1]
        self.helper = existing.EvaluateQualityGateTest()
        self.helper.setUp()
        self.profile = json.loads((self.root / c20.DEFAULT_PROFILE).read_text())
        self.details = self.helper.complete_objective_details()
        for sample, annotation in zip(self.details["samples"], self.helper.samples, strict=True):
            m = sample["metricCalculationDetails"]
            m.update(keywordHits=1 if annotation["should_answer"] else 0,
                     keywordTotal=2 if annotation["should_answer"] else 0,
                     unsupportedCitationCount=0, noAnswerCitationViolationCount=0)

    def run_review(self, details=None, profile=None):
        # These files are synthetic and remain in the ignored workspace.
        with tempfile.TemporaryDirectory(dir=self.root / "tmp") as folder:
            pp, dp = Path(folder) / "profile.json", Path(folder) / "details.json"
            pp.write_text(json.dumps(profile or self.profile), encoding="utf-8")
            dp.write_text(json.dumps(details or self.details), encoding="utf-8")
            return c20.review(self.root, pp, dp)

    def test_draft_observations_do_not_activate_and_judge_is_unavailable(self):
        with patch("urllib.request.urlopen", side_effect=AssertionError("network forbidden")):
            r = self.run_review()
        self.assertEqual(("NOT_EVALUABLE", 4), (r["gateStatus"], r["exitCode"]))
        self.assertTrue(r["objectiveMeasurementComplete"])
        self.assertFalse(r["activationPerformed"])
        self.assertEqual(0, r["providerCalls"])
        values = {x["metric"]: x for x in r["observations"]}
        self.assertEqual((0.5, 260), (values["answer_keyword_hit_rate"]["observed"],
                                     values["answer_keyword_hit_rate"]["denominator"]))
        self.assertEqual((1.0, 20), (values["no_answer_accuracy"]["observed"],
                                   values["no_answer_accuracy"]["denominator"]))
        self.assertEqual(150, values["unsupported_citation_count"]["denominator"])
        self.assertEqual(130, values["objective_claim_support_rate"]["denominator"])
        self.assertIsNone(values["faithfulness_avg"]["observed"])
        self.assertEqual("SKIPPED", values["judge_pass_rate"]["result"])
        self.assertTrue(all(x["target"] is None for x in r["observations"]))

    def test_retry_policy_does_not_hide_objective_measurement_or_approve_gate(self):
        self.details["runCounts"]["retryCount"] = 31
        r = self.run_review()
        self.assertEqual("retry_count_exceeded", r["policyBlocker"])
        self.assertTrue(r["objectiveMeasurementComplete"])
        self.assertEqual("NOT_EVALUABLE", r["gateStatus"])

    def test_citation_source_and_snippet_preserve_different_c9a_scopes(self):
        index = next(i for i, s in enumerate(self.helper.samples) if not s["should_answer"])
        self.details["samples"][index]["metricCalculationDetails"].update(
            citationHits=0, citationTotal=7, citationSnippetHits=2, citationSnippetTotal=2)
        values = {x["metric"]: x for x in self.run_review()["observations"]}
        self.assertEqual((1.0, 130), (values["citation_source_hit_rate"]["observed"],
                                     values["citation_source_hit_rate"]["denominator"]))
        self.assertEqual((1.0, 132), (values["citation_snippet_hit_rate"]["observed"],
                                     values["citation_snippet_hit_rate"]["denominator"]))

    def test_complete_judge_advisory_requires_all_three_valid_fields(self):
        self.details["judgeMetricStatus"] = "COMPLETE"
        self.details["metricChannels"]["judge"] = {"status": "COMPLETE", "comparisonSafety": "ELIGIBLE"}
        for s in self.details["samples"]:
            s["metricCalculationDetails"].update(judgePass=True, faithfulnessScore=0.8, relevanceScore=0.9)
        r = self.run_review()
        self.assertEqual("AVAILABLE", r["judgeAdvisoryStatus"])
        first = next(s for a, s in zip(self.helper.samples, self.details["samples"])
                     if a["should_answer"])
        first["metricCalculationDetails"]["faithfulnessScore"] = 2
        r = self.run_review()
        self.assertEqual("UNAVAILABLE", r["judgeAdvisoryStatus"])
        self.assertTrue(all(x["observed"] is None for x in r["observations"] if x["channel"] == "judge"))
        self.assertTrue(r["objectiveMeasurementComplete"])

    def test_outside_repository_and_broken_json_are_safe_invalid(self):
        r = c20.review(self.root, self.root.parent / "private.json", self.root / "missing.json")
        self.assertEqual("path_outside_repository", r["reason"])
        self.assertNotIn("private", json.dumps(r))
        with tempfile.TemporaryDirectory(dir=self.root / "tmp") as folder:
            dp = Path(folder) / "bad.json"; dp.write_text('not json PRIVATE')
            r = c20.review(self.root, c20.DEFAULT_PROFILE, dp)
            self.assertEqual("INVALID", r["gateStatus"])
            self.assertNotIn("PRIVATE", json.dumps(r))

    def test_missing_order_and_runtime_never_emit_observations(self):
        for mutation in ("missing", "order", "runtime"):
            d = copy.deepcopy(self.details)
            if mutation == "missing": d["samples"].pop()
            elif mutation == "order": d["samples"].reverse()
            else: d["topK"] = 99
            with self.subTest(mutation=mutation):
                r = self.run_review(d)
                self.assertEqual([], r["observations"])
                self.assertNotEqual("PASS", r["gateStatus"])

    def test_partial_judge_cannot_poison_complete_objective_or_leak_values(self):
        self.details["judgeMetricStatus"] = "PARTIAL"
        self.details["metricChannels"]["judge"] = {"status": "PARTIAL", "comparisonSafety": "NOT_ELIGIBLE"}
        for s in self.details["samples"]:
            s["metricCalculationDetails"].update(judgePass=True, faithfulnessScore=1, relevanceScore=1)
        r = self.run_review()
        self.assertTrue(r["objectiveMeasurementComplete"])
        self.assertTrue(all(x["observed"] is None for x in r["observations"] if x["channel"] == "judge"))

    def test_active_objective_gate_skips_partial_optional_judge(self):
        p = copy.deepcopy(self.profile)
        p.update(status="ACTIVE", thresholdStatus="APPROVED")
        for rule in p["rules"]: rule["target"] = 999 if rule["operator"] == "maxInclusive" else 0
        self.details["metricChannels"]["judge"] = {"status": "PARTIAL", "comparisonSafety": "NOT_ELIGIBLE"}
        with tempfile.TemporaryDirectory(dir=self.root / "tmp") as folder:
            pp, dp = Path(folder) / "p.json", Path(folder) / "d.json"
            pp.write_text(json.dumps(p)); dp.write_text(json.dumps(self.details))
            r = gate.evaluate_gate(self.root, pp, dp)
        self.assertEqual("PASS", r["gateStatus"])  # synthetic thresholds only
        self.assertTrue(all(x["result"] == "SKIPPED" and x["observed"] is None
                            for x in r["rules"] if x["channel"] == "judge"))
        self.assertEqual("INVALID", self.run_review(profile=p)["gateStatus"])

    def test_malformed_counts_do_not_become_successful_subset(self):
        sample_index = next(i for i, s in enumerate(self.helper.samples) if s["should_answer"])
        for hits, total in ((True, 2), (-1, 2), (3, 2), ("1", 2), (1, float("nan"))):
            d = copy.deepcopy(self.details)
            d["samples"][sample_index]["metricCalculationDetails"].update(keywordHits=hits, keywordTotal=total)
            r = self.run_review(d)
            x = next(x for x in r["observations"] if x["metric"] == "answer_keyword_hit_rate")
            self.assertIsNone(x["observed"])
            self.assertEqual("INCOMPLETE", x["status"])

    def test_sample_ask_error_blocks_metrics_even_if_channel_claims_complete(self):
        self.details["samples"][0]["errors"]["ask"] = "private-message"
        r = self.run_review()
        self.assertTrue(all(x["observed"] is None for x in r["observations"] if x["channel"] == "objective"))
        self.assertNotIn("private-message", json.dumps(r))

    def test_zero_denominator_and_bad_claim_or_judge_are_unavailable(self):
        for sample in self.details["samples"]:
            sample["metricCalculationDetails"].update(keywordHits=0, keywordTotal=0)
        r = self.run_review()
        item = next(x for x in r["observations"] if x["metric"] == "answer_keyword_hit_rate")
        self.assertIsNone(item["observed"])
        annotated = list(zip(self.helper.samples, self.details["samples"], strict=True))
        first = next(s for a, s in annotated if a["should_answer"])
        first["objectiveClaimMetrics"]["supportedClaimCount"] = 999
        rule = next(x for x in self.profile["rules"] if x["metric"] == "objective_claim_support_rate")
        self.assertEqual("INCOMPLETE", gate.calculate_rule_observation(rule, self.profile["slices"][0], annotated)["status"])
        first["metricCalculationDetails"]["faithfulnessScore"] = 8
        rule = next(x for x in self.profile["rules"] if x["metric"] == "faithfulness_avg")
        self.assertEqual("INCOMPLETE", gate.calculate_rule_observation(rule, self.profile["slices"][0], annotated)["status"])

    def test_schema_and_privacy_and_negative_counters(self):
        self.details["question"] = self.details["samples"][0]["question"] = "PRIVATE RAW"
        r = self.run_review()
        self.assertNotIn("PRIVATE RAW", json.dumps(r))
        self.details["runCounts"]["retryCount"] = -1
        self.assertEqual("INVALID", self.run_review()["gateStatus"])
        p = copy.deepcopy(self.profile); p["rules"][0]["target"] = 0.01
        self.assertEqual("INVALID", self.run_review(profile=p)["gateStatus"])

    def test_malformed_nested_judge_payload_is_unavailable_without_exception(self):
        self.details["judgeMetricStatus"] = "COMPLETE"
        self.details["metricChannels"]["judge"] = {"status": "COMPLETE", "comparisonSafety": "ELIGIBLE"}
        self.details["samples"][0]["metricCalculationDetails"] = ["PRIVATE MALFORMED"]
        r = self.run_review()
        self.assertEqual("UNAVAILABLE", r["judgeAdvisoryStatus"])
        self.assertFalse(r["objectiveMeasurementComplete"])
        self.assertNotIn("PRIVATE MALFORMED", json.dumps(r))

    def test_output_is_exclusive_and_input_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory(dir=self.root / "tmp") as folder:
            dp, out = Path(folder) / "d.json", Path(folder) / "out.json"
            dp.write_text(json.dumps(self.details)); before = dp.read_bytes()
            args = ["--details", str(dp), "--output", str(out)]
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(4, c20.main(args))
                original = out.read_bytes()
                self.assertEqual(2, c20.main(args))
                self.assertEqual(2, c20.main(["--details", str(dp), "--output", str(dp)]))
            self.assertEqual(original, out.read_bytes()); self.assertEqual(before, dp.read_bytes())


if __name__ == "__main__":
    unittest.main()
