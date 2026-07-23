#!/usr/bin/env python3
from __future__ import annotations

import json
import hashlib
import tempfile
import unittest
from pathlib import Path

import eval_dataset_contract
import evaluate_quality_gate


class EvaluateQualityGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[1]
        self.dataset_identity = eval_dataset_contract.validate_versioned_release(
            self.repo_root,
            Path("docs/eval/dataset-manifest.json"),
        )
        question_set_path = self.repo_root / self.dataset_identity["questionSet"]["path"]
        self.samples = [
            json.loads(line)
            for line in question_set_path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]

    def test_active_retrieval_profile_passes_complete_evidence(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])
        details = self.complete_retrieval_details()

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)

            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                details_path,
            )

        self.assertEqual("PASS", result["gateStatus"])
        self.assertEqual(0, result["exitCode"])
        self.assertEqual("PASS", result["rules"][0]["result"])
        self.assertEqual(1.0, result["rules"][0]["observed"])

    def test_draft_profile_is_not_evaluable_even_with_complete_evidence(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": None,
                "required": True,
            }
        ])
        profile["status"] = "DRAFT"
        profile["thresholdStatus"] = "PENDING_REFERENCE_EVIDENCE"

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())

            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                details_path,
            )

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual("profile_draft", result["reason"])
        self.assertEqual(1, len(result["rules"]))
        self.assertEqual("profile_draft", result["rules"][0]["reason"])

    def test_invalid_profile_returns_invalid_contract_result(self) -> None:
        profile = self.active_profile([])
        profile["schemaVersion"] = "unknown-profile-schema"

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())

            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                details_path,
            )

        self.assertEqual("INVALID", result["gateStatus"])
        self.assertEqual(2, result["exitCode"])
        self.assertEqual("profile_schema_version_invalid", result["reason"])

    def test_metric_must_belong_to_declared_channel(self) -> None:
        profile = self.active_profile([
            {
                "id": "misbound-judge-rate",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "judge_pass_rate",
                "operator": "minInclusive",
                "target": 0.9,
                "required": True,
            }
        ])

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("INVALID", result["gateStatus"])
        self.assertEqual("profile_metric_channel_invalid", result["reason"])

    def test_fixed_slice_axes_produce_deterministic_metrics_and_denominators(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            },
            {
                "id": "multi-hop-recall3",
                "channel": "retrieval",
                "slice": "multi-hop",
                "metric": "recall_at_3",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            },
            {
                "id": "hard-mrr",
                "channel": "retrieval",
                "slice": "hard",
                "metric": "mrr",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            },
            {
                "id": "answerable-top1",
                "channel": "retrieval",
                "slice": "answerable",
                "metric": "top1_source_accuracy",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            },
        ])
        profile["slices"] = [
            {"id": "overall", "axis": "all", "value": "all", "minimumDenominator": 1},
            {"id": "multi-hop", "axis": "type", "value": "multi_hop", "minimumDenominator": 1},
            {"id": "hard", "axis": "difficulty", "value": "hard", "minimumDenominator": 1},
            {"id": "answerable", "axis": "answerability", "value": True, "minimumDenominator": 1},
        ]

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        by_id = {rule["id"]: rule for rule in result["rules"]}
        expected_multi_hop_contexts = sum(
            len(sample["expected_contexts"])
            for sample in self.samples
            if sample["type"] == "multi_hop"
        )
        expected_hard_answerable = sum(
            1
            for sample in self.samples
            if sample["difficulty"] == "hard" and sample["should_answer"]
        )
        self.assertEqual("PASS", result["gateStatus"])
        self.assertEqual(expected_multi_hop_contexts, by_id["multi-hop-recall3"]["denominator"])
        self.assertEqual(expected_hard_answerable, by_id["hard-mrr"]["denominator"])
        self.assertEqual(130, by_id["answerable-top1"]["denominator"])
        self.assertTrue(all(rule["observed"] == 1.0 for rule in result["rules"]))

    def test_required_metric_missing_is_not_evaluable_not_quality_failure(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])
        details = self.complete_retrieval_details()
        first_answerable = next(
            sample
            for sample in details["samples"]
            if sample["metricCalculationDetails"]["recallTotal"] > 0
        )
        del first_answerable["metricCalculationDetails"]["recall5Hits"]

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual("NOT_EVALUABLE", result["rules"][0]["result"])
        self.assertEqual("required_metric_missing", result["rules"][0]["reason"])

    def test_retrieval_error_budget_exceeded_is_not_evaluable_not_quality_failure(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])
        details = self.complete_retrieval_details()
        details["runCounts"]["retrieveErrors"] = 1
        details["reportStatus"] = "PARTIAL"
        details["metricChannels"]["objective"] = {
            "status": "PARTIAL",
            "comparisonSafety": "NOT_ELIGIBLE",
        }

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual("retrieve_errors_exceeded", result["reason"])
        self.assertEqual("NOT_EVALUABLE", result["rules"][0]["result"])

    def test_reference_regression_fails_even_when_hard_floor_passes(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.5,
                "maxAbsoluteRegression": 0.0,
                "required": True,
            }
        ])
        reference_details = self.complete_retrieval_details()
        candidate_details = self.complete_retrieval_details()
        first_answerable = next(
            sample
            for sample in candidate_details["samples"]
            if sample["metricCalculationDetails"]["recallTotal"] > 0
        )
        first_answerable["metricCalculationDetails"]["recall5Hits"] -= 1

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            reference_path = Path(tmp_dir) / "reference-gate.json"
            candidate_path = Path(tmp_dir) / "candidate-details.json"
            self.write_json(profile_path, profile)
            reference = {
                "profile": {
                    "id": profile["profileId"],
                    "version": profile["profileVersion"],
                    "sha256": hashlib.sha256(profile_path.read_bytes()).hexdigest(),
                },
                "dataset": {
                    "releaseVersion": self.dataset_identity["releaseVersion"],
                    "manifestSha256": self.dataset_identity["manifestSha256"],
                },
                "runIdentity": profile["runIdentity"],
                "rules": [
                    {
                        "id": "overall-recall5",
                        "channel": "retrieval",
                        "slice": "overall",
                        "metric": "recall_at_5",
                        "operator": "minInclusive",
                        "observed": 1.0,
                    }
                ],
            }
            self.write_json(reference_path, reference)
            self.write_json(candidate_path, candidate_details)

            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                candidate_path,
                reference_path,
            )

        self.assertEqual("FAIL", result["gateStatus"])
        self.assertEqual(3, result["exitCode"])
        self.assertTrue(result["rules"][0]["hardThresholdPassed"])
        self.assertFalse(result["rules"][0]["referenceThresholdPassed"])
        self.assertEqual("reference_regression_exceeded", result["rules"][0]["reason"])

    def test_objective_profile_passes_when_judge_is_skipped(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-claim-support",
                "channel": "objective",
                "slice": "overall",
                "metric": "objective_claim_support_rate",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            }
        ])
        profile["runIdentity"]["mode"] = "generation/citation"
        profile["requiredChannels"] = ["objective"]
        details = self.complete_objective_details()

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("PASS", result["gateStatus"])
        self.assertEqual("PASS", result["rules"][0]["result"])
        self.assertEqual(1.0, result["rules"][0]["observed"])
        self.assertEqual("SKIPPED", details["judgeMetricStatus"])

    def test_judge_profile_requires_complete_judge_channel(self) -> None:
        profile = self.active_profile([
            {
                "id": "judge-pass-rate",
                "channel": "judge",
                "slice": "answerable",
                "metric": "judge_pass_rate",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            }
        ])
        profile["runIdentity"]["mode"] = "generation/citation"
        profile["requiredChannels"] = ["judge"]
        profile["slices"] = [
            {
                "id": "answerable",
                "axis": "answerability",
                "value": True,
                "minimumDenominator": 130,
            }
        ]
        details = self.complete_objective_details()
        details["judgeMetricStatus"] = "COMPLETE"
        details["metricChannels"]["judge"] = {
            "status": "COMPLETE",
            "comparisonSafety": "ELIGIBLE",
        }
        for sample, annotation in zip(details["samples"], self.samples, strict=True):
            if annotation["should_answer"]:
                sample["metricCalculationDetails"].update(
                    {"judgePass": True, "faithfulnessScore": 1.0, "relevanceScore": 1.0}
                )

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("PASS", result["gateStatus"])
        self.assertEqual(130, result["rules"][0]["denominator"])
        self.assertEqual(1.0, result["rules"][0]["observed"])

    def test_missing_required_judge_score_is_not_evaluable(self) -> None:
        profile = self.active_profile([
            {
                "id": "faithfulness-average",
                "channel": "judge",
                "slice": "answerable",
                "metric": "faithfulness_avg",
                "operator": "minInclusive",
                "target": 0.9,
                "required": True,
            }
        ])
        profile["runIdentity"]["mode"] = "generation/citation"
        profile["requiredChannels"] = ["judge"]
        profile["slices"] = [
            {
                "id": "answerable",
                "axis": "answerability",
                "value": True,
                "minimumDenominator": 130,
            }
        ]
        details = self.complete_objective_details()
        details["judgeMetricStatus"] = "COMPLETE"
        details["metricChannels"]["judge"] = {
            "status": "COMPLETE",
            "comparisonSafety": "ELIGIBLE",
        }
        for sample, annotation in zip(details["samples"], self.samples, strict=True):
            if annotation["should_answer"]:
                sample["metricCalculationDetails"].update(
                    {"judgePass": True, "faithfulnessScore": 1.0, "relevanceScore": 1.0}
                )
        first_answerable = next(
            sample
            for sample, annotation in zip(details["samples"], self.samples, strict=True)
            if annotation["should_answer"]
        )
        del first_answerable["metricCalculationDetails"]["faithfulnessScore"]

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual("required_metric_missing", result["rules"][0]["reason"])
        self.assertIsNone(result["rules"][0]["observed"])

    def test_cli_returns_quality_exit_code_and_writes_safe_outputs(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            }
        ])
        details = self.complete_retrieval_details()
        first_answerable = next(
            sample
            for sample in details["samples"]
            if sample["metricCalculationDetails"]["recallTotal"] > 0
        )
        first_answerable["metricCalculationDetails"]["recall5Hits"] -= 1
        first_answerable["question"] = "RAW-QUESTION-MUST-NOT-LEAK"
        first_answerable["askRawResponse"] = {"answer": "RAW-ANSWER-MUST-NOT-LEAK"}
        details["Authorization"] = "Bearer RAW-SECRET-MUST-NOT-LEAK"
        details["evalSet"] = "C:\\Users\\example\\private-eval.jsonl"

        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            profile_path = root / "profile.json"
            details_path = root / "details.json"
            output_json = root / "gate.json"
            output_markdown = root / "gate.md"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)

            exit_code = evaluate_quality_gate.main(
                [
                    "--repo-root",
                    str(self.repo_root),
                    "--profile",
                    str(profile_path),
                    "--details",
                    str(details_path),
                    "--output-json",
                    str(output_json),
                    "--output-markdown",
                    str(output_markdown),
                ]
            )
            json_text = output_json.read_text(encoding="utf-8")
            markdown_text = output_markdown.read_text(encoding="utf-8")

        self.assertEqual(3, exit_code)
        self.assertEqual("FAIL", json.loads(json_text)["gateStatus"])
        for forbidden in (
            "RAW-QUESTION-MUST-NOT-LEAK",
            "RAW-ANSWER-MUST-NOT-LEAK",
            "RAW-SECRET-MUST-NOT-LEAK",
            "C:\\Users\\example",
        ):
            self.assertNotIn(forbidden, json_text)
            self.assertNotIn(forbidden, markdown_text)

    def test_tracked_retrieval_profile_stays_draft_until_reference_evidence(self) -> None:
        schema_path = self.repo_root / "docs/eval/schema/rag-quality-gate-profile-v1.json"
        profile_path = self.repo_root / "docs/eval/gates/rag-eval-dev-v2-retrieval-regression-v1.json"
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        profile = json.loads(profile_path.read_text(encoding="utf-8"))
        self.assertEqual("rag-quality-gate-profile-v1", schema["$id"])
        self.assertEqual("DRAFT", profile["status"])
        self.assertEqual("PENDING_REFERENCE_EVIDENCE", profile["thresholdStatus"])
        self.assertTrue(profile["rules"])
        self.assertTrue(all(rule["target"] is None for rule in profile["rules"]))

        with tempfile.TemporaryDirectory() as tmp_dir:
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(details_path, self.complete_retrieval_details())
            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                details_path,
            )

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual("profile_draft", result["reason"])

    def test_no_answer_type_can_gate_retrieval_execution_completeness(self) -> None:
        profile = self.active_profile([
            {
                "id": "no-answer-retrieve-success",
                "channel": "retrieval",
                "slice": "type-no-answer",
                "metric": "retrieve_success_rate",
                "operator": "minInclusive",
                "target": 1.0,
                "required": True,
            }
        ])
        profile["slices"] = [
            {
                "id": "type-no-answer",
                "axis": "type",
                "value": "no_answer",
                "minimumDenominator": 20,
            }
        ]

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("PASS", result["gateStatus"])
        self.assertEqual(20, result["rules"][0]["denominator"])
        self.assertEqual(1.0, result["rules"][0]["observed"])

    def test_profile_rejects_unknown_contract_fields(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])
        profile["unexpectedPolicy"] = "silently-ignore-me"

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("INVALID", result["gateStatus"])
        self.assertEqual("profile_unknown_field", result["reason"])

    def test_profile_rejects_rule_that_references_unknown_slice(self) -> None:
        profile = self.active_profile([
            {
                "id": "orphan-rule",
                "channel": "retrieval",
                "slice": "missing-slice",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("INVALID", result["gateStatus"])
        self.assertEqual("profile_rule_slice_unknown", result["reason"])

    def test_reference_identity_mismatch_is_not_evaluable(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.5,
                "maxAbsoluteRegression": 0.02,
                "required": True,
            }
        ])

        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            profile_path = root / "profile.json"
            details_path = root / "details.json"
            reference_path = root / "reference.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            self.write_json(
                reference_path,
                {
                    "profile": {
                        "id": profile["profileId"],
                        "version": profile["profileVersion"],
                        "sha256": "0" * 64,
                    },
                    "dataset": {
                        "releaseVersion": self.dataset_identity["releaseVersion"],
                        "manifestSha256": self.dataset_identity["manifestSha256"],
                    },
                    "runIdentity": profile["runIdentity"],
                    "rules": [{"id": "overall-recall5", "observed": 1.0}],
                },
            )
            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                details_path,
                reference_path,
            )

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual("reference_identity_mismatch", result["reason"])
        self.assertEqual("NOT_EVALUABLE", result["rules"][0]["result"])

    def test_reference_rule_identity_must_match_profile(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.5,
                "maxAbsoluteRegression": 0.02,
                "required": True,
            }
        ])

        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            profile_path = root / "profile.json"
            details_path = root / "details.json"
            reference_path = root / "reference.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            self.write_json(
                reference_path,
                {
                    "profile": {
                        "id": profile["profileId"],
                        "version": profile["profileVersion"],
                        "sha256": hashlib.sha256(profile_path.read_bytes()).hexdigest(),
                    },
                    "dataset": {
                        "releaseVersion": self.dataset_identity["releaseVersion"],
                        "manifestSha256": self.dataset_identity["manifestSha256"],
                    },
                    "runIdentity": profile["runIdentity"],
                    "rules": [
                        {
                            "id": "overall-recall5",
                            "channel": "retrieval",
                            "slice": "overall",
                            "metric": "mrr",
                            "operator": "minInclusive",
                            "observed": 1.0,
                        }
                    ],
                },
            )
            result = evaluate_quality_gate.evaluate_gate(
                self.repo_root,
                profile_path,
                details_path,
                reference_path,
            )

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual("reference_rule_identity_mismatch", result["reason"])

    def test_unversioned_evidence_is_not_evaluable(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])
        details = self.complete_retrieval_details()
        details["datasetValidation"] = "UNVERSIONED"

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("NOT_EVALUABLE", result["gateStatus"])
        self.assertEqual(4, result["exitCode"])
        self.assertEqual("evidence_dataset_invalid", result["reason"])

    def test_max_inclusive_latency_threshold_produces_quality_failure(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-retrieval-p95",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "retrieval_latency_p95",
                "operator": "maxInclusive",
                "target": 50.0,
                "required": True,
            }
        ])
        details = self.complete_retrieval_details()
        for sample in details["samples"]:
            sample["retrieveLatencyMillis"] = 100.0

        with tempfile.TemporaryDirectory() as tmp_dir:
            profile_path = Path(tmp_dir) / "profile.json"
            details_path = Path(tmp_dir) / "details.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, details)
            result = evaluate_quality_gate.evaluate_gate(self.repo_root, profile_path, details_path)

        self.assertEqual("FAIL", result["gateStatus"])
        self.assertEqual(3, result["exitCode"])
        self.assertEqual(100.0, result["rules"][0]["observed"])
        self.assertEqual("hard_threshold_failed", result["rules"][0]["reason"])

    def test_cli_no_overwrite_preserves_existing_output(self) -> None:
        profile = self.active_profile([
            {
                "id": "overall-recall5",
                "channel": "retrieval",
                "slice": "overall",
                "metric": "recall_at_5",
                "operator": "minInclusive",
                "target": 0.95,
                "required": True,
            }
        ])
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            profile_path = root / "profile.json"
            details_path = root / "details.json"
            output_path = root / "gate.json"
            self.write_json(profile_path, profile)
            self.write_json(details_path, self.complete_retrieval_details())
            output_path.write_text("KEEP-ME", encoding="utf-8")

            exit_code = evaluate_quality_gate.main(
                [
                    "--repo-root",
                    str(self.repo_root),
                    "--profile",
                    str(profile_path),
                    "--details",
                    str(details_path),
                    "--output-json",
                    str(output_path),
                    "--no-overwrite",
                ]
            )

            self.assertEqual("KEEP-ME", output_path.read_text(encoding="utf-8"))

        self.assertEqual(2, exit_code)

    def active_profile(self, rules: list[dict[str, object]]) -> dict[str, object]:
        return {
            "schemaVersion": "rag-quality-gate-profile-v1",
            "profileId": "test-retrieval-gate",
            "profileVersion": "v1",
            "status": "ACTIVE",
            "thresholdStatus": "APPROVED",
            "dataset": {
                "manifestPath": "docs/eval/dataset-manifest.json",
                "manifestSha256": self.dataset_identity["manifestSha256"],
                "releaseVersion": self.dataset_identity["releaseVersion"],
                "expectedSampleCount": len(self.samples),
                "selectionMode": "full",
            },
            "runIdentity": {
                "mode": "retrieval-only",
                "topK": 5,
                "minScore": 0.3,
                "enableRerank": True,
            },
            "requiredChannels": ["retrieval"],
            "errorPolicy": {
                "retrieveErrorsMax": 0,
                "rateLimitErrorsMax": 0,
                "retryCountMax": 0,
            },
            "missingPolicy": "NOT_EVALUABLE",
            "slices": [
                {
                    "id": "overall",
                    "axis": "all",
                    "value": "all",
                    "minimumDenominator": 1,
                }
            ],
            "rules": rules,
        }

    def complete_retrieval_details(self) -> dict[str, object]:
        details_samples = []
        for sample in self.samples:
            should_answer = bool(sample["should_answer"])
            recall_total = len(sample["expected_contexts"]) if should_answer else 0
            details_samples.append(
                {
                    "id": sample["id"],
                    "errors": {"retrieval": None, "ask": None},
                    "metricCalculationDetails": {
                        "recall3Hits": recall_total,
                        "recall5Hits": recall_total,
                        "recallTotal": recall_total,
                        "firstMatchRank": 1 if should_answer else None,
                        "top1SourceHit": True if should_answer else None,
                        "retrievalError": None,
                    },
                }
            )
        return {
            "reportStatus": "RETRIEVAL_ONLY",
            "objectiveMetricStatus": "RETRIEVAL_ONLY",
            "judgeMetricStatus": "SKIPPED",
            "metricChannels": {
                "objective": {
                    "status": "RETRIEVAL_ONLY",
                    "comparisonSafety": "RETRIEVAL_ONLY",
                },
                "judge": {"status": "SKIPPED", "comparisonSafety": "NOT_ELIGIBLE"},
            },
            "runCounts": {
                "retrieveErrors": 0,
                "askErrors": 0,
                "judgeErrors": 0,
                "rateLimitErrors": 0,
                "retryCount": 0,
            },
            "datasetValidation": "VALID",
            "datasetReleaseIdentity": self.dataset_identity,
            "sampleCount": len(details_samples),
            "topK": 5,
            "minScore": 0.3,
            "enableRerank": True,
            "skipAsk": True,
            "samples": details_samples,
        }

    def complete_objective_details(self) -> dict[str, object]:
        details = self.complete_retrieval_details()
        details["reportStatus"] = "CLEAN"
        details["objectiveMetricStatus"] = "COMPLETE"
        details["skipAsk"] = False
        details["metricChannels"]["objective"] = {
            "status": "COMPLETE",
            "comparisonSafety": "ELIGIBLE",
        }
        for sample, annotation in zip(details["samples"], self.samples, strict=True):
            calculation = sample["metricCalculationDetails"]
            if annotation["should_answer"]:
                calculation.update(
                    {
                        "citationHits": 1,
                        "citationTotal": 1,
                        "citationSnippetHits": 1,
                        "citationSnippetTotal": 1,
                        "noAnswerOk": None,
                    }
                )
                sample["objectiveClaimMetrics"] = {
                    "claimMetricStatus": "COMPLETE",
                    "claimTotal": 1,
                    "supportedClaimCount": 1,
                }
            else:
                calculation.update(
                    {
                        "citationHits": 0,
                        "citationTotal": 0,
                        "citationSnippetHits": 0,
                        "citationSnippetTotal": 0,
                        "noAnswerOk": True,
                    }
                )
                sample["objectiveClaimMetrics"] = {
                    "claimMetricStatus": "NOT_APPLICABLE",
                    "claimTotal": 0,
                    "supportedClaimCount": 0,
                }
        return details

    @staticmethod
    def write_json(path: Path, payload: dict[str, object]) -> None:
        path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
