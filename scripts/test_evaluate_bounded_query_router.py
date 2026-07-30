#!/usr/bin/env python3
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

import evaluate_bounded_query_router as evaluator
import router_eval_contract as contract


class EvaluateBoundedQueryRouterTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[1]
        self.manifest_path = Path(
            "docs/eval/router/bounded-query-router-eval-v1-manifest.json"
        )
        self.identity = contract.validate_release(self.repo_root, self.manifest_path)
        sidecar = self.repo_root / "docs/eval/router/bounded-query-router-eval-v1-expectations.jsonl"
        self.expectations = [json.loads(line) for line in sidecar.read_text(encoding="utf-8").splitlines()]

    def test_complete_evidence_passes_seven_channels_with_stable_metrics(self) -> None:
        result = evaluator.evaluate_evidence(
            self.repo_root, self.manifest_path, self.complete_evidence()
        )

        self.assertEqual("PASS", result["reportStatus"])
        self.assertEqual(0, result["exitCode"])
        self.assertEqual({channel: "PASS" for channel in contract.REQUIRED_CHANNELS},
                         result["channelStatuses"])
        self.assertEqual(1.0, result["classificationMetrics"]["factPrecision"])
        self.assertEqual(1.0, result["classificationMetrics"]["factRecall"])
        self.assertEqual(1.0, result["classificationMetrics"]["coverage"])
        self.assertEqual(0.0, result["classificationMetrics"]["unsupportedLeakage"])

    def test_missing_declared_denominator_is_invalid(self) -> None:
        evidence = self.complete_evidence()
        del evidence["classification"]["factObserved"]

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("INVALID", result["reportStatus"])
        self.assertEqual("classification_denominator_invalid", result["reason"])

    def test_inconsistent_declared_denominator_is_invalid(self) -> None:
        evidence = self.complete_evidence()
        evidence["classification"]["factExpected"] = 9

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("INVALID", result["reportStatus"])
        self.assertEqual("classification_denominator_mismatch", result["reason"])

    def test_classification_mismatch_fails_without_hiding_other_counts(self) -> None:
        evidence = self.complete_evidence()
        case = evidence["cases"][0]
        case["observedIntent"] = "UNSUPPORTED"
        case["effectiveStrategy"] = "NONE"
        case["finalState"] = "UNSUPPORTED"
        case["channels"]["classification"] = "FAIL"
        case["channels"]["strategy_execution"] = "PASS"
        for field in (
            "queryVariants", "retrievalPasses", "rerankCalls", "generationCalls",
            "candidateCount", "contextCount", "estimatedContextTokens",
            "estimatedOutputTokens",
        ):
            case["usage"][field] = 0
        evidence["classification"]["factObserved"] -= 1

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("FAIL", result["reportStatus"])
        self.assertEqual(1, result["caseCounts"]["failed"])
        self.assertLess(result["classificationMetrics"]["factRecall"], 1.0)

    def test_unsupported_execution_leakage_fails_strategy_and_budget(self) -> None:
        evidence = self.complete_evidence()
        case = next(item for item in evidence["cases"] if item["observedIntent"] == "UNSUPPORTED")
        case["usage"]["retrievalPasses"] = 1
        case["channels"]["strategy_execution"] = "FAIL"
        case["channels"]["budget"] = "FAIL"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("FAIL", result["reportStatus"])
        self.assertEqual(0.1, result["classificationMetrics"]["unsupportedLeakage"])
        self.assertEqual("FAIL", result["channelStatuses"]["strategy_execution"])

    def test_unknown_strategy_is_invalid(self) -> None:
        evidence = self.complete_evidence()
        evidence["cases"][0]["effectiveStrategy"] = "agentic-v1"

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("INVALID", result["reportStatus"])
        self.assertEqual("unknown_strategy", result["reason"])

    def test_missing_required_case_is_not_evaluable(self) -> None:
        evidence = self.complete_evidence()
        removed = evidence["cases"].pop()
        if removed["observedIntent"] == "FACT":
            evidence["classification"]["factObserved"] -= 1
        evidence["classification"]["observedCount"] -= 1

        result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

        self.assertEqual("NOT_EVALUABLE", result["reportStatus"])
        self.assertEqual(1, result["caseCounts"]["missing"])

    def test_error_or_skip_cannot_be_hidden_by_pass_channels(self) -> None:
        for outcome in ("ERROR", "SKIPPED"):
            with self.subTest(outcome=outcome):
                evidence = self.complete_evidence()
                evidence["cases"][0]["outcome"] = outcome
                evidence["cases"][0]["channels"]["errors"] = "NOT_EVALUABLE"

                result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)

                self.assertEqual("NOT_EVALUABLE", result["reportStatus"])

    def test_identity_drift_and_duplicate_case_are_invalid(self) -> None:
        with self.subTest("identity"):
            evidence = self.complete_evidence()
            evidence["policyVersion"] = "policy-v2"
            result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)
            self.assertEqual("INVALID", result["reportStatus"])
            self.assertEqual("evidence_identity_mismatch", result["reason"])
        with self.subTest("duplicate"):
            evidence = self.complete_evidence()
            evidence["cases"].append(dict(evidence["cases"][0]))
            evidence["classification"]["observedCount"] += 1
            evidence["classification"]["factObserved"] += 1
            result = evaluator.evaluate_evidence(self.repo_root, self.manifest_path, evidence)
            self.assertEqual("INVALID", result["reportStatus"])
            self.assertEqual("duplicate_case_result", result["reason"])

    def test_formal_output_is_no_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "details.json"
            evaluator.write_new(output, "first\n")
            with self.assertRaises(FileExistsError):
                evaluator.write_new(output, "second\n")
            self.assertEqual("first\n", output.read_text(encoding="utf-8"))

    def complete_evidence(self) -> dict:
        cases = []
        for expectation in self.expectations:
            fact = expectation["expectedIntent"] == "FACT"
            channels = {
                "classification": "PASS",
                "strategy_execution": "PASS",
                "budget": "PASS",
                "retrieval": "PASS" if fact else "NOT_APPLICABLE",
                "generation_citation": "PASS" if fact else "NOT_APPLICABLE",
                "no_answer": "PASS" if fact else "NOT_APPLICABLE",
                "errors": "PASS",
            }
            cases.append({
                "sampleId": expectation["sampleId"],
                "outcome": "PASS",
                "observedIntent": expectation["expectedIntent"],
                "routeReason": "DEFINITION_CUE" if fact else "MULTI_HOP_CUE",
                "effectiveStrategy": "fact-v1" if fact else "NONE",
                "finalState": "ANSWER" if fact else "UNSUPPORTED",
                "noAnswerReason": "NONE",
                "usage": {
                    "queryVariants": 1 if fact else 0,
                    "retrievalPasses": 1 if fact else 0,
                    "rerankCalls": 1 if fact else 0,
                    "generationCalls": 1 if fact else 0,
                    "candidateCount": 3 if fact else 0,
                    "contextCount": 2 if fact else 0,
                    "estimatedContextTokens": 100 if fact else 0,
                    "estimatedOutputTokens": 40 if fact else 0,
                    "elapsedMillis": 10,
                    "budgetOutcome": "WITHIN_BUDGET",
                    "providerCalls": 0,
                },
                "channels": channels,
            })
        return {
            "schemaVersion": "bounded-query-router-evidence-v1",
            "releaseVersion": self.identity["releaseVersion"],
            "manifestSha256": self.identity["manifestSha256"],
            "evaluatorVersion": self.identity["evaluatorVersion"],
            "classifierVersion": self.identity["classifierVersion"],
            "strategyVersion": self.identity["strategyVersion"],
            "policyVersion": self.identity["policyVersion"],
            "budgetProfileId": self.identity["budgetProfileId"],
            "gitHead": "a" * 40,
            "providerCallCount": 0,
            "businessDataOutbound": False,
            "liveEvaluationStatus": "SKIPPED",
            "classification": {
                "expectedCount": 20,
                "observedCount": 20,
                "factExpected": 10,
                "factObserved": 10,
            },
            "cases": cases,
        }


if __name__ == "__main__":
    unittest.main()
