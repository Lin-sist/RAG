#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

import run_judge_calibration as calibration


class JudgeCalibrationTest(unittest.TestCase):
    def test_default_calibration_release_is_valid_and_balanced(self) -> None:
        identity = calibration.validate_calibration_release(
            Path(__file__).resolve().parents[1],
            calibration.DEFAULT_MANIFEST,
        )

        self.assertEqual("VALID", identity["validationStatus"])
        self.assertEqual("judge-calibration-v1", identity["releaseVersion"])
        self.assertEqual(24, identity["caseCount"])
        self.assertEqual(
            {"ff_rf": 6, "ff_rt": 6, "ft_rf": 6, "ft_rt": 6},
            identity["quadrantCounts"],
        )

    def test_canary_plan_selects_one_case_per_quadrant_without_calls(self) -> None:
        plan = calibration.calibration_plan(
            Path(__file__).resolve().parents[1],
            calibration.DEFAULT_MANIFEST,
            profile="canary",
            judge_args=argparse.Namespace(
                judge_base_url="https://example.test/v1",
                judge_model="judge-model",
                judge_temperature=0.0,
                judge_max_context_chars=6000,
            ),
        )

        self.assertEqual(4, plan["selectedCaseCount"])
        self.assertEqual(1, plan["repeatCount"])
        self.assertEqual(4, plan["estimatedJudgeCalls"])
        self.assertEqual({"ff_rf", "ff_rt", "ft_rf", "ft_rt"}, set(plan["selectedQuadrants"]))

    def test_plan_only_cli_returns_safe_call_budget_without_case_text(self) -> None:
        stdout = io.StringIO()
        with (
            mock.patch(
                "sys.argv",
                [
                    "run_judge_calibration.py",
                    "--plan-only",
                    "--profile",
                    "canary",
                    "--judge-model",
                    "judge-model",
                ],
            ),
            redirect_stdout(stdout),
        ):
            exit_code = calibration.main()

        plan = json.loads(stdout.getvalue())
        self.assertEqual(0, exit_code)
        self.assertEqual(4, plan["estimatedJudgeCalls"])
        self.assertNotIn("question", plan)
        self.assertNotIn("answer", plan)

    def test_absolute_manifest_path_is_rejected_without_echoing_local_path(self) -> None:
        with self.assertRaises(calibration.CalibrationContractError) as raised:
            calibration.validate_calibration_release(
                Path(__file__).resolve().parents[1],
                Path("C:/Users/private/calibration.json"),
            )

        self.assertEqual("invalid_artifact_path", raised.exception.code)
        self.assertEqual("<invalid-path>", raised.exception.artifact)
        self.assertNotIn("Users", str(raised.exception))

    def test_calibration_summary_preserves_missing_observation_in_coverage(self) -> None:
        cases = [
            {"id": "positive", "goldFaithful": True, "goldRelevant": True, "goldJointPass": True},
            {"id": "negative", "goldFaithful": False, "goldRelevant": False, "goldJointPass": False},
        ]
        observations = [
            {
                "caseId": "positive",
                "repeatIndex": 1,
                "faithfulnessScore": 0.9,
                "relevanceScore": 0.9,
                "judgePass": True,
                "providerPassMismatch": False,
                "errorCategory": None,
            }
        ]

        summary = calibration.aggregate_calibration(cases, observations, repeat_count=1)

        self.assertEqual("PARTIAL", summary["calibrationStatus"])
        self.assertEqual(2, summary["expectedObservationCount"])
        self.assertEqual(1, summary["validObservationCount"])
        self.assertEqual(0.5, summary["parseCoverage"])

    def test_duplicate_or_unexpected_observation_is_not_comparable(self) -> None:
        cases = [
            {"id": "positive", "goldFaithful": True, "goldRelevant": True, "goldJointPass": True},
        ]
        valid = {
            "caseId": "positive",
            "repeatIndex": 1,
            "faithfulnessScore": 0.9,
            "relevanceScore": 0.9,
            "judgePass": True,
            "providerPassMismatch": False,
            "errorCategory": None,
        }

        summary = calibration.aggregate_calibration(
            cases,
            [valid, dict(valid), {**valid, "caseId": "unknown"}],
            repeat_count=1,
        )

        self.assertEqual("NOT_COMPARABLE", summary["calibrationStatus"])
        self.assertEqual(1, summary["duplicateObservationCount"])
        self.assertEqual(1, summary["unexpectedObservationCount"])

    def test_canary_execution_uses_static_cases_and_shared_strict_parser(self) -> None:
        repo_root = Path(__file__).resolve().parents[1]
        args = argparse.Namespace(
            judge_base_url="https://example.test/v1",
            judge_api_key="unused-by-fake",
            judge_model="judge-model",
            judge_temperature=0.0,
            judge_timeout=60.0,
            judge_max_context_chars=6000,
        )
        calls: list[str] = []

        def fake_judge(case: dict[str, object], contexts: list[dict[str, str]], _: object) -> str:
            calls.append(str(case["id"]))
            self.assertTrue(contexts)
            self.assertIn("source", contexts[0])
            return json.dumps(
                {
                    "faithfulnessScore": 0.9 if case["goldFaithful"] else 0.1,
                    "relevanceScore": 0.9 if case["goldRelevant"] else 0.1,
                    "pass": bool(case["goldJointPass"]),
                    "reason": "fixture-only fake",
                }
            )

        result = calibration.execute_calibration(
            repo_root,
            calibration.DEFAULT_MANIFEST,
            profile="canary",
            judge_args=args,
            judge_call=fake_judge,
        )

        self.assertEqual(4, len(calls))
        self.assertEqual("COMPLETE", result["summary"]["calibrationStatus"])
        self.assertEqual(1.0, result["summary"]["jointPass"]["agreement"])
        self.assertEqual(4, len(result["observations"]))
        self.assertTrue(all(item["attemptCount"] == 1 for item in result["observations"]))

    def test_outputs_keep_aggregate_report_safe_and_refuse_overwrite(self) -> None:
        result = {
            "plan": {
                "profile": "canary",
                "selectedCaseCount": 1,
                "repeatCount": 1,
                "estimatedJudgeCalls": 1,
                "calibrationReleaseIdentity": {
                    "releaseVersion": "judge-calibration-v1",
                    "manifestSha256": "abc",
                },
                "judgeContractConfig": {"judgeContractVersion": "rag-judge-v1"},
            },
            "summary": {
                "calibrationStatus": "COMPLETE",
                "expectedObservationCount": 1,
                "validObservationCount": 1,
                "parseCoverage": 1.0,
                "faithfulness": {"tp": 1, "tn": 0, "fp": 0, "fn": 0, "agreement": 1.0},
                "relevance": {"tp": 1, "tn": 0, "fp": 0, "fn": 0, "agreement": 1.0},
                "jointPass": {"tp": 1, "tn": 0, "fp": 0, "fn": 0, "agreement": 1.0},
                "providerPassMismatchCount": 0,
                "repeatConsistentCaseCount": 1,
                "repeatConsistentCaseRate": 1.0,
                "inconsistentCaseIds": [],
            },
            "observations": [
                {
                    "caseId": "case-1",
                    "rawContent": "private-provider-reason",
                    "reason": "private-provider-reason",
                    "errorCategory": None,
                }
            ],
        }

        with tempfile.TemporaryDirectory() as tmp_dir:
            report = Path(tmp_dir) / "report.md"
            details = Path(tmp_dir) / "details.json"
            calibration.write_calibration_outputs(
                report,
                details,
                result,
                no_overwrite=True,
            )
            rendered_report = report.read_text(encoding="utf-8")
            rendered_details = details.read_text(encoding="utf-8")
            with self.assertRaises(calibration.CalibrationContractError) as raised:
                calibration.write_calibration_outputs(
                    report,
                    details,
                    result,
                    no_overwrite=True,
                )

        self.assertNotIn("private-provider-reason", rendered_report)
        self.assertIn("private-provider-reason", rendered_details)
        self.assertEqual("output_exists", raised.exception.code)

    def test_live_boundary_uses_shared_contract_prompt_without_retry(self) -> None:
        class FakeResponse:
            def __enter__(self) -> "FakeResponse":
                return self

            def __exit__(self, *_: object) -> None:
                return None

            def read(self) -> bytes:
                return json.dumps({
                    "choices": [{"message": {"content": '{"faithfulnessScore":0.8,"relevanceScore":0.9,"pass":true}'}}]
                }).encode("utf-8")

        args = argparse.Namespace(
            judge_base_url="https://example.test/v1",
            judge_api_key="secret-value",
            judge_model="judge-model",
            judge_temperature=0.0,
            judge_timeout=12.0,
            judge_max_context_chars=6000,
        )
        case = {
            "question": "Question?",
            "answer": "Answer.",
        }
        contexts = [{"source": "fixture.md", "content": "Tracked context."}]

        with mock.patch.object(calibration.urllib.request, "urlopen", return_value=FakeResponse()) as request:
            content = calibration.call_live_judge(case, contexts, args)

        sent_request = request.call_args.args[0]
        payload = json.loads(sent_request.data.decode("utf-8"))
        self.assertIn("Tracked context.", payload["messages"][1]["content"])
        self.assertEqual(calibration.judge_contract.JUDGE_SYSTEM_PROMPT, payload["messages"][0]["content"])
        self.assertEqual("Bearer secret-value", sent_request.headers["Authorization"])
        self.assertIn("faithfulnessScore", content)


if __name__ == "__main__":
    unittest.main()
