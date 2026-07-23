#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest import mock

import run_rag_eval as runner


class RunRagEvalJudgeTest(unittest.TestCase):
    def test_parse_judge_content_rejects_out_of_range_scores(self) -> None:
        with self.assertRaisesRegex(runner.JudgePayloadError, "invalid_judge_payload"):
            runner.parse_judge_content(
                '{"faithfulnessScore": 1.2, "relevanceScore": 0.65, "pass": true}'
            )

    def test_parse_judge_content_derives_pass_and_reports_provider_disagreement(self) -> None:
        parsed = runner.parse_judge_content(
            '{"faithfulnessScore": 0.9, "relevanceScore": 0.6, "pass": true}'
        )

        self.assertFalse(parsed["pass"])
        self.assertTrue(parsed["providerReportedPass"])
        self.assertTrue(parsed["providerPassMismatch"])

    def test_extract_answer_claims_splits_sentences_and_list_items_deterministically(self) -> None:
        answer = "概念一。概念二；\n- First fact. Second fact!"

        first = runner.extract_answer_claims(answer)
        second = runner.extract_answer_claims(answer)

        self.assertEqual(["概念一", "概念二", "First fact", "Second fact"], [item["text"] for item in first])
        self.assertEqual([1, 2, 3, 4], [item["claimIndex"] for item in first])
        self.assertEqual(first, second)
        self.assertTrue(all(item["claimHash"] for item in first))

    def test_extract_answer_claims_ignores_structural_only_fragments(self) -> None:
        claims = runner.extract_answer_claims("###\n1.\n[1]\n---\n- 有效事实。")

        self.assertEqual(["有效事实"], [item["text"] for item in claims])

    def test_objective_claim_metrics_keep_unsupported_claims_in_denominator(self) -> None:
        result = runner.evaluate_objective_claim_metrics(
            "Spring Boot 使用自动配置。它会读取不存在的未来配置。",
            citations=[{"chunkId": "chunk-1", "snippet": "Spring Boot 使用自动配置。", "source": "spring.md"}],
            contexts=[{"source": "chunk-1", "content": "Spring Boot 使用自动配置。"}],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )

        self.assertEqual("COMPLETE", result["claimMetricStatus"])
        self.assertEqual(2, result["claimTotal"])
        self.assertEqual(1, result["supportedClaimCount"])
        self.assertEqual(1, result["unsupportedClaimCount"])
        self.assertEqual(0.5, result["objectiveClaimSupportRate"])
        self.assertEqual("exact", result["claims"][0]["support"])
        self.assertEqual("unsupported", result["claims"][1]["support"])

    def test_objective_claim_metrics_uses_fixed_token_overlap_boundary(self) -> None:
        claim = "alpha beta gamma delta epsilon zeta eta theta iota kappa"
        supported = runner.evaluate_objective_claim_metrics(
            claim,
            [{"chunkId": "c1", "snippet": "alpha beta gamma delta epsilon zeta eta"}],
            [{"source": "c1", "content": "alpha beta gamma delta epsilon zeta eta"}],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )
        unsupported = runner.evaluate_objective_claim_metrics(
            claim,
            [{"chunkId": "c1", "snippet": "alpha beta gamma delta epsilon zeta"}],
            [{"source": "c1", "content": "alpha beta gamma delta epsilon zeta"}],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )

        self.assertEqual("token_overlap", supported["claims"][0]["support"])
        self.assertEqual(0.7, supported["claims"][0]["bestEvidence"]["claimTokenCoverage"])
        self.assertEqual("unsupported", unsupported["claims"][0]["support"])
        self.assertEqual("below_lexical_threshold", unsupported["claims"][0]["reason"])

    def test_objective_claim_metrics_excludes_citation_without_context_provenance(self) -> None:
        result = runner.evaluate_objective_claim_metrics(
            "Spring Boot 使用自动配置。",
            [{"chunkId": "claimed", "snippet": "Spring Boot 使用自动配置。"}],
            [{"source": "actual", "content": "Spring Boot 使用自动配置。"}],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )

        self.assertEqual(0, result["eligibleEvidenceCount"])
        self.assertEqual("unsupported", result["claims"][0]["support"])
        self.assertEqual("no_eligible_evidence", result["claims"][0]["reason"])

    def test_objective_claim_metrics_prefers_exact_then_stable_citation_index(self) -> None:
        claim = "alpha beta gamma delta epsilon zeta eta theta iota kappa"
        result = runner.evaluate_objective_claim_metrics(
            claim,
            [
                {"chunkId": "c0", "snippet": "alpha beta gamma delta epsilon zeta eta"},
                {"chunkId": "c1", "snippet": claim},
                {"chunkId": "c2", "snippet": claim},
            ],
            [
                {"source": "c0", "content": "alpha beta gamma delta epsilon zeta eta"},
                {"source": "c1", "content": claim},
                {"source": "c2", "content": claim},
            ],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )

        self.assertEqual("exact", result["claims"][0]["support"])
        self.assertEqual(1, result["claims"][0]["bestEvidence"]["citationIndex"])
        self.assertEqual(3, result["claims"][0]["matchedEvidenceCount"])

    def test_objective_claim_metrics_marks_short_non_exact_claim_unsupported(self) -> None:
        result = runner.evaluate_objective_claim_metrics(
            "AI",
            [{"chunkId": "c1", "snippet": "artificial intelligence"}],
            [{"source": "c1", "content": "artificial intelligence"}],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )

        self.assertEqual("unsupported", result["claims"][0]["support"])
        self.assertEqual("insufficient_claim_tokens", result["claims"][0]["reason"])

    def test_objective_claim_metrics_distinguishes_skipped_not_applicable_and_partial(self) -> None:
        cases = [
            ({"should_answer": True, "skipped_ask": True, "ask_error": None, "answer": ""}, "SKIPPED", None),
            ({"should_answer": False, "skipped_ask": False, "ask_error": None, "answer": "拒答"}, "NOT_APPLICABLE", None),
            ({"should_answer": True, "skipped_ask": False, "ask_error": "timeout", "answer": ""}, "PARTIAL", "ask_error"),
            ({"should_answer": True, "skipped_ask": False, "ask_error": None, "answer": ""}, "PARTIAL", "empty_answer"),
            ({"should_answer": True, "skipped_ask": False, "ask_error": None, "answer": "###"}, "PARTIAL", "empty_claim_set"),
        ]

        for values, expected_status, expected_error in cases:
            with self.subTest(expected_status=expected_status, expected_error=expected_error):
                result = runner.evaluate_objective_claim_metrics(
                    values["answer"],
                    [],
                    [],
                    should_answer=values["should_answer"],
                    skipped_ask=values["skipped_ask"],
                    ask_error=values["ask_error"],
                )
                self.assertEqual(expected_status, result["claimMetricStatus"])
                self.assertEqual(expected_error, result["claimExtractionError"])

    def test_require_credentials_rejects_missing_values(self) -> None:
        args = argparse.Namespace(username="", password="")

        with self.assertRaisesRegex(RuntimeError, "explicit credentials"):
            runner.require_credentials(args)

    def test_parse_judge_content_accepts_fenced_strict_json(self) -> None:
        parsed = runner.parse_judge_content(
            """```json
            {"faithfulnessScore": 1.0, "relevanceScore": 0.75, "pass": true, "reason": "grounded"}
            ```"""
        )

        self.assertEqual(1.0, parsed["faithfulnessScore"])
        self.assertEqual(0.75, parsed["relevanceScore"])
        self.assertTrue(parsed["pass"])
        self.assertEqual("grounded", parsed["reason"])

    def test_parse_judge_content_derives_pass_when_missing(self) -> None:
        parsed = runner.parse_judge_content(
            '{"faithfulnessScore": 0.71, "relevanceScore": 0.70}'
        )

        self.assertTrue(parsed["pass"])

    def test_should_run_judge_requires_llm_mode_ask_and_answerable_sample(self) -> None:
        args = argparse.Namespace(judge_mode="llm")

        self.assertTrue(runner.should_run_judge(args, ask_evaluable=True, should_answer=True))
        self.assertFalse(runner.should_run_judge(args, ask_evaluable=False, should_answer=True))
        self.assertFalse(runner.should_run_judge(args, ask_evaluable=True, should_answer=False))

        args.judge_mode = "off"
        self.assertFalse(runner.should_run_judge(args, ask_evaluable=True, should_answer=True))

    def test_select_samples_filters_ids_then_applies_limit(self) -> None:
        samples = [
            {"id": "fact-001"},
            {"id": "definition-001"},
            {"id": "reasoning-001"},
        ]

        selected = runner.select_samples(samples, ["definition-001", "reasoning-001"], 1)

        self.assertEqual([{"id": "definition-001"}], selected)

    def test_eval_plan_estimates_ask_and_judge_calls(self) -> None:
        args = argparse.Namespace(
            skip_ask=False,
            judge_mode="llm",
            ask_timeout=12.0,
            retry_ask_timeouts=True,
            report="report.md",
            after_report="",
            details_json="details.json",
        )
        samples = [
            {"id": "fact-001", "should_answer": True},
            {"id": "no-answer-001", "should_answer": False},
        ]

        plan = runner.eval_plan(samples, args)

        self.assertEqual(2, plan["estimatedBackendCalls"]["debugRetrieve"])
        self.assertEqual(2, plan["estimatedBackendCalls"]["ask"])
        self.assertEqual(1, plan["estimatedBackendCalls"]["llmJudge"])
        self.assertEqual(12.0, plan["askTimeout"])
        self.assertTrue(plan["retryAskTimeouts"])

    def test_eval_plan_exposes_versioned_judge_contract_without_secret(self) -> None:
        args = argparse.Namespace(
            skip_ask=False,
            judge_mode="llm",
            judge_model="judge-model",
            judge_temperature=0.0,
            judge_max_context_chars=6000,
            judge_base_url="https://example.test/v1",
            judge_api_key="secret-value",
            ask_timeout=12.0,
            retry_ask_timeouts=True,
            report="report.md",
            after_report="",
            details_json="details.json",
        )

        config = runner.eval_plan([{"id": "fact-001"}], args)["judgeContractConfig"]

        self.assertEqual("rag-judge-v1", config["judgeContractVersion"])
        self.assertEqual("strict-json-scores-v1", config["parserVersion"])
        self.assertEqual(0.7, config["faithfulnessThreshold"])
        self.assertEqual("judge-model", config["model"])
        self.assertNotIn("apiKey", config)
        self.assertNotIn("secret-value", str(config))

    def test_normal_judge_prompt_delegates_to_shared_contract(self) -> None:
        sample = {
            "question": "What is RAG?",
            "expected_answer_points": ["retrieval", "generation"],
            "expected_keywords": ["RAG"],
        }
        answer = "RAG combines retrieval and generation."
        ask_response = {
            "contexts": [
                {"source": "fixture.md", "content": "RAG retrieves context before generation."}
            ]
        }

        expected = runner.judge_contract.build_judge_prompt(
            question=sample["question"],
            answer=answer,
            contexts=ask_response["contexts"],
            expected_points=sample["expected_answer_points"],
            expected_keywords=sample["expected_keywords"],
            max_context_chars=6000,
        )

        self.assertEqual(
            expected,
            runner.build_judge_prompt(sample, answer, ask_response, 6000),
        )

    def test_normal_judge_result_does_not_retain_raw_provider_content(self) -> None:
        args = argparse.Namespace(
            judge_model="judge-model",
            judge_api_key="test-key",
            judge_temperature=0.0,
            judge_base_url="https://example.test/v1",
            judge_timeout=12.0,
            judge_max_context_chars=6000,
        )
        response = {
            "choices": [
                {
                    "message": {
                        "content": '{"faithfulnessScore":0.8,"relevanceScore":0.9,"pass":true,"reason":"private"}'
                    }
                }
            ]
        }

        with mock.patch.object(runner, "call_json", return_value=response):
            result = runner.call_llm_judge(
                {"question": "q"},
                "answer",
                {"contexts": [{"source": "fixture.md", "content": "context"}]},
                args,
            )

        self.assertNotIn("rawContent", result)
        self.assertEqual("private", result["reason"])

    def test_run_metadata_rejects_judge_contract_identity_drift(self) -> None:
        args = argparse.Namespace(
            judge_model="judge-model",
            judge_temperature=0.0,
            judge_max_context_chars=6000,
            judge_base_url="https://example.test/v1",
        )
        metadata = {
            "judgeContractConfig": {
                **runner.judge_contract.contract_config(args),
                "parserVersion": "stale-parser",
            }
        }

        with self.assertRaisesRegex(
            runner.JudgeContractIdentityError,
            "judge_contract_identity_mismatch",
        ):
            runner.bind_judge_contract_identity(metadata, args)

    def test_should_retry_ask_timeout_can_be_disabled(self) -> None:
        error = TimeoutError("timed out")

        self.assertTrue(runner.should_retry_ask_error(error, retry_timeouts=True))
        self.assertFalse(runner.should_retry_ask_error(error, retry_timeouts=False))

    def test_is_no_answer_recognizes_context_missing_response(self) -> None:
        response = {"metadata": {}, "citations": []}
        answer = "提供的上下文未包含有关 Kubernetes 的信息，因此无法根据现有内容回答该问题。"

        self.assertTrue(runner.is_no_answer(response, answer))

    def test_format_error_metadata_includes_llm_diagnostics(self) -> None:
        metadata = {
            "status": "error",
            "errorCategory": "llm",
            "llmProvider": "openai",
            "llmEndpoint": "/chat/completions",
            "llmModel": "nvidia/test",
            "llmTimeoutSeconds": 120,
            "llmMaxRetries": 3,
            "llmErrorType": "TimeoutException",
            "llmErrorCategory": "timeout",
        }

        summary = runner.format_error_metadata(metadata)

        self.assertIn("category=llm", summary)
        self.assertIn("provider=openai", summary)
        self.assertIn("endpoint=/chat/completions", summary)
        self.assertIn("timeoutSeconds=120", summary)
        self.assertIn("llmErrorCategory=timeout", summary)

    def test_extract_rerank_attribution_keeps_only_stable_facts(self) -> None:
        debug_response = {
            "diagnostics": {
                "rerankRequestedProvider": "nvidia",
                "rerankEffectiveProvider": "heuristic",
                "rerankFallbackCount": 1,
                "rerankFallbackReason": "timeout",
                "rerankModelCallCount": 1,
                "rerankCandidateCount": 20,
                "rerankScoredCount": 20,
                "rerankCoverage": 1.0,
                "rerankLatencyMillis": 125,
                "rerankModel": "nvidia/test",
                "rerankProtocol": "nvidia-ranking-v1",
                "query": "must-not-leak",
                "Authorization": "must-not-leak",
            }
        }

        attribution = runner.extract_rerank_attribution(debug_response)

        self.assertEqual("nvidia", attribution["requestedProvider"])
        self.assertEqual("heuristic", attribution["effectiveProvider"])
        self.assertEqual("timeout", attribution["fallbackReason"])
        self.assertEqual(1, attribution["modelCallCount"])
        self.assertEqual(1.0, attribution["candidateCoverage"])
        self.assertNotIn("query", attribution)
        self.assertNotIn("Authorization", attribution)

    def test_aggregate_rerank_attributions_separates_effective_provider_and_fallback(self) -> None:
        summary = runner.aggregate_rerank_attributions([
            {
                "requestedProvider": "nvidia",
                "effectiveProvider": "nvidia",
                "fallbackCount": 0,
                "fallbackReason": "none",
                "modelCallCount": 1,
                "candidateCount": 10,
                "scoredCount": 10,
            },
            {
                "requestedProvider": "nvidia",
                "effectiveProvider": "heuristic",
                "fallbackCount": 1,
                "fallbackReason": "timeout",
                "modelCallCount": 1,
                "candidateCount": 20,
                "scoredCount": 20,
            },
        ])

        self.assertEqual({"nvidia": 1, "heuristic": 1}, summary["effectiveProviderCounts"])
        self.assertEqual(0.5, summary["modelCoverage"])
        self.assertEqual(1, summary["fallbackCount"])
        self.assertEqual({"timeout": 1}, summary["fallbackReasonHistogram"])
        self.assertEqual(2, summary["totalModelCalls"])
        self.assertEqual(1.0, summary["candidateCoverage"])

    def test_latency_summary_uses_nearest_rank_and_ignores_invalid_values(self) -> None:
        summary = runner.latency_summary([30, 10, float("nan"), -1, 20])

        self.assertEqual(
            {"count": 3, "min": 10.0, "p50": 20.0, "p95": 30.0, "max": 30.0},
            summary,
        )
        self.assertEqual(10.0, runner.latency_summary([10])["p95"])
        self.assertEqual(20.0, runner.latency_summary([10, 20, 30, 40])["p50"])
        self.assertEqual(
            {"count": 0, "min": None, "p50": None, "p95": None, "max": None},
            runner.latency_summary([]),
        )

    def test_aggregate_rerank_attributions_reports_latency_percentiles(self) -> None:
        summary = runner.aggregate_rerank_attributions([
            {"effectiveProvider": "nvidia", "latencyMillis": 10},
            {"effectiveProvider": "nvidia", "latencyMillis": 20},
            {"effectiveProvider": "nvidia", "latencyMillis": 40},
        ])

        self.assertEqual(
            {"count": 3, "min": 10.0, "p50": 20.0, "p95": 40.0, "max": 40.0},
            summary["latencyMillis"],
        )

    def test_run_sample_records_debug_retrieval_wall_clock_latency(self) -> None:
        args = argparse.Namespace(
            base_url="http://localhost:8080",
            kb_id=7,
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            timeout=60.0,
            skip_ask=True,
            judge_mode="off",
        )
        sample = {"id": "no-answer-001", "question": "q", "should_answer": False}

        with (
            mock.patch.object(runner.time, "monotonic", side_effect=[1.0, 1.125]),
            mock.patch.object(runner, "call_json", return_value={"data": {"status": "ok", "contexts": []}}),
        ):
            result = runner.run_sample(sample, args, "token")

        self.assertEqual(125.0, result.details["retrieveLatencyMillis"])

    def test_run_sample_records_objective_claim_metrics_in_details(self) -> None:
        args = argparse.Namespace(
            base_url="http://localhost:8080",
            kb_id=7,
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            timeout=60.0,
            skip_ask=False,
            judge_mode="off",
        )
        sample = {"id": "fact-001", "question": "q", "should_answer": True}
        ask_response = {
            "answer": "Spring Boot 使用自动配置。",
            "citations": [{"chunkId": "chunk-1", "snippet": "Spring Boot 使用自动配置。"}],
            "contexts": [{"source": "chunk-1", "content": "Spring Boot 使用自动配置。"}],
            "metadata": {"status": "ok"},
        }

        with (
            mock.patch.object(runner.time, "monotonic", side_effect=[1.0, 1.050]),
            mock.patch.object(
                runner,
                "call_json",
                return_value={"data": {"status": "ok", "contexts": [{"source": "chunk-1", "contentPreview": "Spring Boot 使用自动配置。"}]}},
            ),
            mock.patch.object(
                runner,
                "call_ask_with_retries",
                return_value=(ask_response, {"attempts": 1, "retries": 0, "rateLimitErrors": 0}),
            ),
        ):
            result = runner.run_sample(sample, args, "token")

        self.assertEqual("COMPLETE", result.details["objectiveClaimMetrics"]["claimMetricStatus"])
        self.assertEqual(1, result.details["objectiveClaimMetrics"]["supportedClaimCount"])

    def test_write_report_includes_claim_summary_and_algorithm_identity(self) -> None:
        args = argparse.Namespace(
            base_url="http://localhost:8080",
            kb_id=7,
            eval_set="eval.jsonl",
            sample_ids=[],
            sample_limit=0,
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            timeout=60.0,
            skip_ask=False,
            judge_mode="off",
            judge_model="",
            judge_base_url="https://example.test/v1",
            judge_temperature=0.0,
            judge_timeout=60.0,
            judge_max_context_chars=6000,
            ask_timeout=60.0,
            ask_delay_seconds=0.0,
            max_ask_retries=0,
            retry_backoff_seconds=0.0,
            retry_ask_timeouts=True,
        )
        sample = {"id": "fact-001", "question": "q", "type": "fact", "should_answer": True}
        ask_response = {
            "answer": "Spring Boot 使用自动配置。",
            "citations": [{"chunkId": "chunk-1", "snippet": "Spring Boot 使用自动配置。"}],
            "contexts": [{"source": "chunk-1", "content": "Spring Boot 使用自动配置。"}],
            "metadata": {"status": "ok"},
        }

        with (
            mock.patch.object(runner.time, "monotonic", side_effect=[1.0, 1.050]),
            mock.patch.object(
                runner,
                "call_json",
                return_value={"data": {"status": "ok", "contexts": [{"source": "chunk-1", "contentPreview": "Spring Boot 使用自动配置。"}]}},
            ),
            mock.patch.object(
                runner,
                "call_ask_with_retries",
                return_value=(ask_response, {"attempts": 1, "retries": 0, "rateLimitErrors": 0}),
            ),
        ):
            result = runner.run_sample(sample, args, "token")

        with tempfile.TemporaryDirectory() as tmp_dir:
            report = Path(tmp_dir) / "report.md"
            details_path = Path(tmp_dir) / "details.json"
            runner.write_report(
                report,
                args,
                [sample],
                [result],
                None,
                runner.time.time(),
                {"datasetValidation": "VALID"},
            )
            runner.write_details_json(
                details_path,
                args,
                [sample],
                [result],
                None,
                runner.time.time(),
                {"datasetValidation": "VALID"},
            )
            rendered = report.read_text(encoding="utf-8")
            details = json.loads(details_path.read_text(encoding="utf-8"))

        self.assertIn("Objective claim metric status", rendered)
        self.assertIn("Objective lexical claim support rate", rendered)
        self.assertIn("Objective metric status: `COMPLETE`", rendered)
        self.assertIn("Judge metric status: `SKIPPED`", rendered)
        self.assertIn("judgeContractConfig", rendered)
        self.assertIn("claim-lexical-v1", rendered)
        self.assertNotIn("Spring Boot 使用自动配置", rendered)
        self.assertEqual("COMPLETE", details["objectiveMetricStatus"])
        self.assertEqual("SKIPPED", details["judgeMetricStatus"])
        self.assertEqual("ELIGIBLE", details["metricChannels"]["objective"]["comparisonSafety"])
        self.assertEqual("NOT_ELIGIBLE", details["metricChannels"]["judge"]["comparisonSafety"])
        self.assertEqual("rag-judge-v1", details["judgeContractConfig"]["judgeContractVersion"])

    def test_run_sample_records_latency_when_debug_retrieval_fails(self) -> None:
        args = argparse.Namespace(
            base_url="http://localhost:8080",
            kb_id=7,
            top_k=5,
            min_score=0.3,
            enable_rerank=True,
            timeout=60.0,
            skip_ask=True,
            judge_mode="off",
        )
        sample = {"id": "no-answer-001", "question": "q", "should_answer": False}

        with (
            mock.patch.object(runner.time, "monotonic", side_effect=[1.0, 1.050]),
            mock.patch.object(runner, "call_json", side_effect=TimeoutError("timed out")),
        ):
            result = runner.run_sample(sample, args, "token")

        self.assertEqual(50.0, result.details["retrieveLatencyMillis"])
        self.assertIn("timed out", result.retrieval_error)

    def test_aggregate_reports_retrieval_latency_separately_from_rerank_latency(self) -> None:
        result = argparse.Namespace(
            sample={"should_answer": False},
            skipped_ask=True,
            ask_error=None,
            ask_response=None,
            skipped_judge=True,
            judge_error=None,
            judge_pass=None,
            faithfulness_score=None,
            relevance_score=None,
            recall_total=0,
            recall3_hits=0,
            recall5_hits=0,
            first_match_rank=None,
            top1_source_hit=None,
            keyword_hits=0,
            keyword_total=0,
            citation_hits=0,
            citation_total=0,
            citation_snippet_hits=0,
            citation_snippet_total=0,
            unsupported_citation_count=0,
            no_answer_citation_violation_count=0,
            no_answer_ok=None,
            details={
                "retrieveLatencyMillis": 250,
                "rerankAttribution": {"effectiveProvider": "heuristic", "latencyMillis": 15},
            },
        )

        summary = runner.aggregate([result])

        self.assertEqual(250.0, summary["retrieval_latency_millis"]["p50"])
        self.assertEqual(15.0, summary["rerank_attribution"]["latencyMillis"]["p50"])

    def test_judge_all_error_makes_global_partial_without_polluting_objective_status(self) -> None:
        result = argparse.Namespace(
            sample={"should_answer": True},
            retrieval_error=None,
            ask_error=None,
            skipped_ask=False,
            ask_response={"answer": "完整回答"},
            objective_claim_metrics=runner.empty_claim_metric_result("COMPLETE"),
            skipped_judge=False,
            judge_error="timeout",
            judge_pass=None,
            rate_limit_errors=0,
            ask_retry_count=0,
        )
        args = argparse.Namespace(skip_ask=False, judge_mode="llm")

        statuses = runner.metric_channel_statuses(args, [result], None)

        self.assertEqual("COMPLETE", statuses["objectiveMetricStatus"])
        self.assertEqual("PARTIAL", statuses["judgeMetricStatus"])
        self.assertEqual("PARTIAL", statuses["reportStatus"])

    def test_channel_safety_keeps_complete_objective_when_judge_is_partial(self) -> None:
        safety = runner.metric_channel_safety(
            {
                "objectiveMetricStatus": "COMPLETE",
                "judgeMetricStatus": "PARTIAL",
                "reportStatus": "PARTIAL",
            },
            dataset_validation="VALID",
        )

        self.assertEqual("ELIGIBLE", safety["objective"]["comparisonSafety"])
        self.assertEqual("NOT_ELIGIBLE", safety["judge"]["comparisonSafety"])

    def test_status_matrix_covers_judge_off_no_answer_only_and_retrieval_only(self) -> None:
        answerable = argparse.Namespace(
            sample={"should_answer": True},
            retrieval_error=None,
            ask_error=None,
            skipped_ask=False,
            ask_response={"answer": "answer"},
            objective_claim_metrics=runner.empty_claim_metric_result("COMPLETE"),
            skipped_judge=True,
            judge_error=None,
            judge_pass=None,
            rate_limit_errors=0,
            ask_retry_count=0,
        )
        no_answer = argparse.Namespace(
            **{
                **vars(answerable),
                "sample": {"should_answer": False},
                "ask_response": {"answer": "cannot answer"},
                "objective_claim_metrics": runner.empty_claim_metric_result("NOT_APPLICABLE"),
            }
        )

        off = runner.metric_channel_statuses(
            argparse.Namespace(skip_ask=False, judge_mode="off"),
            [answerable],
            None,
        )
        no_answer_only = runner.metric_channel_statuses(
            argparse.Namespace(skip_ask=False, judge_mode="llm"),
            [no_answer],
            None,
        )
        retrieval_only = runner.metric_channel_statuses(
            argparse.Namespace(skip_ask=True, judge_mode="off"),
            [argparse.Namespace(**{**vars(answerable), "skipped_ask": True})],
            None,
        )

        self.assertEqual(
            {"objectiveMetricStatus": "COMPLETE", "judgeMetricStatus": "SKIPPED", "reportStatus": "CLEAN"},
            off,
        )
        self.assertEqual("NOT_APPLICABLE", no_answer_only["judgeMetricStatus"])
        self.assertEqual("CLEAN", no_answer_only["reportStatus"])
        self.assertEqual("RETRIEVAL_ONLY", retrieval_only["reportStatus"])

    def test_aggregate_exposes_judge_coverage_counts(self) -> None:
        def result(*, judge_error: str | None, judge_pass: bool | None) -> argparse.Namespace:
            return argparse.Namespace(
                sample={"should_answer": True},
                skipped_ask=False,
                ask_error=None,
                ask_response={"answer": "answer"},
                skipped_judge=False,
                judge_error=judge_error,
                judge_pass=judge_pass,
                faithfulness_score=0.8 if judge_pass is not None else None,
                relevance_score=0.8 if judge_pass is not None else None,
                recall_total=0,
                recall3_hits=0,
                recall5_hits=0,
                first_match_rank=None,
                top1_source_hit=None,
                keyword_hits=0,
                keyword_total=0,
                citation_hits=0,
                citation_total=0,
                citation_snippet_hits=0,
                citation_snippet_total=0,
                unsupported_citation_count=0,
                no_answer_citation_violation_count=0,
                no_answer_ok=None,
                objective_claim_metrics=runner.empty_claim_metric_result("COMPLETE"),
                details={},
            )

        metrics = runner.aggregate([
            result(judge_error=None, judge_pass=True),
            result(judge_error="invalid_judge_payload", judge_pass=None),
        ])

        self.assertEqual(2, metrics["judge_eligible_samples"])
        self.assertEqual(2, metrics["judge_attempted_samples"])
        self.assertEqual(1, metrics["judge_valid_samples"])
        self.assertEqual(1, metrics["judge_error_samples"])
        self.assertEqual(1, metrics["judge_invalid_payload_samples"])

    def test_aggregate_claim_metrics_stays_partial_when_an_answerable_sample_fails(self) -> None:
        def sample_result(claim_metrics: dict[str, object], *, ask_error: str | None) -> argparse.Namespace:
            return argparse.Namespace(
                sample={"should_answer": True},
                skipped_ask=False,
                ask_error=ask_error,
                ask_response={} if ask_error is None else None,
                skipped_judge=True,
                judge_error=None,
                judge_pass=None,
                faithfulness_score=None,
                relevance_score=None,
                recall_total=0,
                recall3_hits=0,
                recall5_hits=0,
                first_match_rank=None,
                top1_source_hit=None,
                keyword_hits=0,
                keyword_total=0,
                citation_hits=0,
                citation_total=0,
                citation_snippet_hits=0,
                citation_snippet_total=0,
                unsupported_citation_count=0,
                no_answer_citation_violation_count=0,
                no_answer_ok=None,
                objective_claim_metrics=claim_metrics,
                details={"retrieveLatencyMillis": 10, "rerankAttribution": {}},
            )

        complete = runner.evaluate_objective_claim_metrics(
            "已支持事实。",
            [{"chunkId": "c1", "snippet": "已支持事实。"}],
            [{"source": "c1", "content": "已支持事实。"}],
            should_answer=True,
            skipped_ask=False,
            ask_error=None,
        )
        partial = runner.evaluate_objective_claim_metrics(
            "",
            [],
            [],
            should_answer=True,
            skipped_ask=False,
            ask_error="timeout",
        )

        summary = runner.aggregate([
            sample_result(complete, ask_error=None),
            sample_result(partial, ask_error="timeout"),
        ])

        self.assertEqual("PARTIAL", summary["claim_metric_status"])
        self.assertEqual(1, summary["claim_total"])
        self.assertEqual(1, summary["supported_claim_count"])
        self.assertEqual(1, summary["claim_partial_samples"])
        self.assertEqual(1, summary["answers_with_complete_claim_metrics"])
        self.assertEqual(0.5, summary["claim_complete_sample_rate"])

    def test_aggregate_claim_metrics_distinguishes_no_answer_from_retrieval_only(self) -> None:
        no_answer = argparse.Namespace(
            sample={"should_answer": False},
            objective_claim_metrics=runner.empty_claim_metric_result("NOT_APPLICABLE"),
        )
        retrieval_only = argparse.Namespace(
            sample={"should_answer": True},
            objective_claim_metrics=runner.empty_claim_metric_result("SKIPPED"),
        )

        no_answer_summary = runner.aggregate_objective_claim_metrics([no_answer])
        skipped_summary = runner.aggregate_objective_claim_metrics([retrieval_only])

        self.assertEqual("NOT_APPLICABLE", no_answer_summary["claim_metric_status"])
        self.assertEqual("N/A", no_answer_summary["claim_complete_sample_rate"])
        self.assertEqual("SKIPPED", skipped_summary["claim_metric_status"])
        self.assertEqual("skipped", skipped_summary["objective_claim_support_rate"])
        self.assertEqual("skipped", skipped_summary["claim_complete_sample_rate"])

    def test_legacy_result_without_claim_fields_is_unavailable_not_zero(self) -> None:
        legacy = argparse.Namespace(
            sample={"should_answer": True},
            skipped_ask=False,
            details={},
        )

        result = runner.objective_claim_metrics_for_result(legacy)

        self.assertEqual("PARTIAL", result["claimMetricStatus"])
        self.assertEqual("claim_metric_unavailable", result["claimExtractionError"])
        self.assertEqual("N/A", result["objectiveClaimSupportRate"])

    def test_latency_fact_rows_names_retrieval_and_rerank_separately(self) -> None:
        rows = runner.latency_fact_rows({
            "retrieval_latency_millis": {"count": 3, "min": 80.0, "p50": 90.0, "p95": 110.0, "max": 110.0},
            "rerank_attribution": {
                "latencyMillis": {"count": 3, "min": 10.0, "p50": 20.0, "p95": 40.0, "max": 40.0},
            },
        })

        rendered = "\n".join(rows)
        self.assertIn("Client-observed debug retrieval latency", rendered)
        self.assertIn("Server-side rerank stage latency", rendered)
        self.assertIn("count=3, min=80.0, P50=90.0, P95=110.0, max=110.0 ms", rendered)

    def test_main_rejects_unversioned_custom_eval_set_before_backend_calls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp = Path(tmp_dir)
            eval_set = tmp / "custom.jsonl"
            report = tmp / "report.md"
            eval_set.write_text('{"id":"fact-001","should_answer":true}\n', encoding="utf-8")
            argv = [
                "run_rag_eval.py",
                "--eval-set",
                str(eval_set),
                "--kb-id",
                "1",
                "--plan-only",
                "--report",
                str(report),
            ]

            with mock.patch("sys.argv", argv), mock.patch.object(runner, "login") as login:
                self.assertEqual(2, runner.main())

            login.assert_not_called()

    def test_main_rejects_drifted_release_before_backend_calls(self) -> None:
        error = runner.dataset_contract.DatasetContractError(
            "artifact_hash_mismatch",
            "docs/eval/rag_eval_set.jsonl",
            "bytes or sha256 drifted",
        )

        with (
            mock.patch("sys.argv", ["run_rag_eval.py", "--kb-id", "1", "--plan-only"]),
            mock.patch.object(runner, "validate_eval_dataset", side_effect=error),
            mock.patch.object(runner, "login") as login,
        ):
            self.assertEqual(2, runner.main())

        login.assert_not_called()

    def test_unversioned_results_are_never_safe_for_comparison(self) -> None:
        counts = {
            "askErrors": 0,
            "retrieveErrors": 0,
            "skippedAsk": 1,
            "judgeErrors": 0,
            "skippedJudge": 1,
            "rateLimitErrors": 0,
            "retryCount": 0,
        }

        result = runner.metrics_safe_for_comparison(
            "RETRIEVAL_ONLY",
            counts,
            dataset_validation="UNVERSIONED",
        )

        self.assertEqual("no; dataset is UNVERSIONED", result)

    def test_run_metadata_dataset_identity_mismatch_is_rejected(self) -> None:
        current = {
            "validationStatus": "VALID",
            "releaseVersion": "rag-eval-dev-v1",
            "manifestSha256": "current",
        }
        metadata = {
            "datasetValidation": "VALID",
            "datasetReleaseIdentity": {**current, "manifestSha256": "stale"},
        }

        with self.assertRaises(runner.dataset_contract.DatasetContractError) as raised:
            runner.bind_dataset_identity(metadata, current)

        self.assertEqual("release_identity_mismatch", raised.exception.code)

    def test_run_metadata_claim_metric_identity_mismatch_is_rejected(self) -> None:
        metadata = {
            "claimMetricConfig": {
                **runner.CLAIM_METRIC_CONFIG,
                "lexicalThreshold": 0.58,
            }
        }

        with self.assertRaisesRegex(ValueError, "claim_metric_identity_mismatch"):
            runner.bind_claim_metric_identity(metadata)

    def test_main_rejects_claim_metric_identity_drift_before_backend_calls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            metadata = Path(tmp_dir) / "metadata.json"
            metadata.write_text(
                '{"claimMetricConfig":{"claimMetricVersion":"older"}}',
                encoding="utf-8",
            )
            argv = [
                "run_rag_eval.py",
                "--kb-id",
                "1",
                "--plan-only",
                "--run-metadata-json",
                str(metadata),
            ]
            errors = io.StringIO()

            with (
                mock.patch("sys.argv", argv),
                mock.patch.object(
                    runner,
                    "validate_eval_dataset",
                    return_value={"validationStatus": "VALID", "manifestPath": "docs/eval/dataset-manifest.json"},
                ),
                mock.patch.object(runner, "login") as login,
                redirect_stderr(errors),
            ):
                self.assertEqual(2, runner.main())

            login.assert_not_called()
            self.assertIn("claim_metric_identity_mismatch", errors.getvalue())

    def test_versioned_plan_reports_release_identity_before_backend_calls(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            report = Path(tmp_dir) / "report.md"
            argv = [
                "run_rag_eval.py",
                "--eval-set",
                str(
                    Path(runner.__file__).resolve().parents[1]
                    / "docs/eval/releases/rag-eval-dev-v2.jsonl"
                ),
                "--kb-id",
                "1",
                "--plan-only",
                "--sample-limit",
                "1",
                "--report",
                str(report),
            ]
            output = io.StringIO()

            with mock.patch("sys.argv", argv), mock.patch.object(runner, "login") as login:
                with redirect_stdout(output):
                    self.assertEqual(0, runner.main())

            login.assert_not_called()
            self.assertIn('"validationStatus": "VALID"', output.getvalue())
            self.assertIn('"releaseVersion": "rag-eval-dev-v2"', output.getvalue())
            self.assertIn('"claimMetricVersion": "claim-lexical-v1"', output.getvalue())


if __name__ == "__main__":
    unittest.main()
