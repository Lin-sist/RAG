#!/usr/bin/env python3
from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path

import compile_retrieval_reference as compiler
import eval_dataset_contract
import run_reproducible_rag_eval as runner


class CompileRetrievalReferenceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[1]
        self.manifest_path = self.repo_root / "docs/eval/config/c17-retrieval-reference-v1.json"
        self.profile_path = (
            self.repo_root
            / "docs/eval/gates/rag-eval-dev-v2-retrieval-regression-v1.json"
        )
        self.reference_manifest = runner.load_reference_manifest(self.manifest_path)
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

    def test_complete_draft_evidence_produces_review_distribution_without_pass(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))

            result = compiler.compile_reference(
                self.repo_root,
                self.manifest_path,
                self.profile_path,
                details_paths,
                metadata_paths,
            )

        self.assertEqual("COMPLETE", result["status"])
        self.assertEqual("PENDING_THRESHOLD_APPROVAL", result["activationStatus"])
        self.assertEqual(450, result["actual"]["observationCount"])
        self.assertEqual(12, len(result["rules"]))
        first = result["rules"][0]
        self.assertEqual([1.0, 1.0, 1.0], [run["observed"] for run in first["runs"]])
        self.assertEqual(1.0, first["minimum"])
        self.assertEqual(1.0, first["median"])
        self.assertEqual(1.0, first["maximum"])
        self.assertEqual(0.0, first["spread"])
        self.assertNotIn("gateStatus", result)
        self.assertNotIn("lockedReference", result)

    def test_distribution_median_and_rounding_are_deterministic(self) -> None:
        self.assertEqual(
            {"minimum": 0.1, "median": 0.2, "maximum": 0.3, "spread": 0.2},
            compiler.summarize_observations([0.3, 0.1, 0.2]),
        )
        self.assertEqual(0.25, compiler.summarize_observations([0.1, 0.2, 0.3, 0.4])["median"])
        self.assertEqual(
            0.0,
            compiler.summarize_observations([0.1 + 0.2, 0.3, 0.30000000000000004])["spread"],
        )
        with self.assertRaisesRegex(compiler.ReferenceCompileError, "distribution_value_invalid"):
            compiler.summarize_observations([float("nan")])

    def test_missing_sample_is_incomplete_and_does_not_emit_distribution(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))
            details = json.loads(details_paths[1].read_text(encoding="utf-8"))
            details["samples"].pop()
            details["sampleCount"] -= 1
            details_paths[1].write_text(json.dumps(details), encoding="utf-8")

            result = compiler.compile_reference(
                self.repo_root,
                self.manifest_path,
                self.profile_path,
                details_paths,
                metadata_paths,
            )

        self.assertEqual("INCOMPLETE", result["status"])
        self.assertIn("sample_selection_incomplete", result["reasonCodes"])
        self.assertEqual([], result["rules"])
        self.assertNotIn("lockedReference", result)

    def test_repeat_git_identity_drift_is_not_comparable(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))
            metadata = json.loads(metadata_paths[2].read_text(encoding="utf-8"))
            metadata["git"]["head"] = "f" * 40
            metadata_paths[2].write_text(json.dumps(metadata), encoding="utf-8")

            result = compiler.compile_reference(
                self.repo_root,
                self.manifest_path,
                self.profile_path,
                details_paths,
                metadata_paths,
            )

        self.assertEqual("NOT_COMPARABLE", result["status"])
        self.assertIn("repeat_identity_drift", result["reasonCodes"])
        self.assertEqual([], result["rules"])

    def test_retired_generation_cannot_be_compiled_as_new_reference(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))
            for details_path, metadata_path in zip(details_paths, metadata_paths):
                metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
                metadata["knowledgeBase"]["embeddingGeneration"]["generation"] = "c17g2"
                metadata_path.write_text(json.dumps(metadata), encoding="utf-8")
                details = json.loads(details_path.read_text(encoding="utf-8"))
                details["runMetadata"] = metadata
                details_path.write_text(json.dumps(details), encoding="utf-8")

            result = compiler.compile_reference(
                self.repo_root, self.manifest_path, self.profile_path, details_paths, metadata_paths,
            )

        self.assertEqual("NOT_COMPARABLE", result["status"])
        self.assertEqual([], result["rules"])
        self.assertNotIn("lockedReference", result)

    def test_provider_fallback_is_not_comparable(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))
            details = json.loads(details_paths[0].read_text(encoding="utf-8"))
            details["samples"][0]["rerankAttribution"]["fallbackCount"] = 1
            details_paths[0].write_text(json.dumps(details), encoding="utf-8")

            result = compiler.compile_reference(
                self.repo_root,
                self.manifest_path,
                self.profile_path,
                details_paths,
                metadata_paths,
            )

        self.assertEqual("NOT_COMPARABLE", result["status"])
        self.assertIn("provider_attribution_mismatch", result["reasonCodes"])
        self.assertEqual([], result["rules"])

    def test_fixture_document_identity_drift_is_not_comparable(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))
            for details_path, metadata_path in zip(details_paths, metadata_paths, strict=True):
                metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
                metadata["knowledgeBase"]["documents"][0]["title"] = "unexpected-document.md"
                metadata_path.write_text(json.dumps(metadata), encoding="utf-8")
                details = json.loads(details_path.read_text(encoding="utf-8"))
                details["runMetadata"] = metadata
                details_path.write_text(json.dumps(details), encoding="utf-8")

            result = compiler.compile_reference(
                self.repo_root,
                self.manifest_path,
                self.profile_path,
                details_paths,
                metadata_paths,
            )

        self.assertEqual("NOT_COMPARABLE", result["status"])
        self.assertIn("fixture_document_identity_mismatch", result["reasonCodes"])
        self.assertEqual([], result["rules"])

    def test_safe_pack_does_not_copy_raw_or_local_identity(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            details_paths, metadata_paths = self.write_reference_runs(Path(tmp_dir))
            result = compiler.compile_reference(
                self.repo_root,
                self.manifest_path,
                self.profile_path,
                details_paths,
                metadata_paths,
            )

        payload = json.dumps(result, ensure_ascii=False)
        self.assertNotIn("question", payload)
        self.assertNotIn("debugRetrieveRawResponse", payload)
        self.assertNotIn("vectorCollection", payload)
        self.assertNotIn("kb-test-vector", payload)
        self.assertNotIn(str(self.repo_root), payload)
        self.assertNotIn('"id": 77', payload)

    def test_active_profile_builds_median_locked_reference_bound_to_final_hash(self) -> None:
        profile = json.loads(self.profile_path.read_text(encoding="utf-8"))
        profile["profileVersion"] = "v1"
        profile["status"] = "ACTIVE"
        profile["thresholdStatus"] = "APPROVED"
        for rule in profile["rules"]:
            rule["target"] = 0.8
            rule["maxAbsoluteRegression"] = 0.1
        distributions = [
            {
                "id": rule["id"],
                "channel": rule["channel"],
                "slice": rule["slice"],
                "metric": rule["metric"],
                "operator": rule["operator"],
                "median": 0.9,
            }
            for rule in profile["rules"]
        ]

        locked = compiler.build_locked_reference(profile, "a" * 64, distributions)

        self.assertEqual("v1", locked["profile"]["version"])
        self.assertEqual("a" * 64, locked["profile"]["sha256"])
        self.assertTrue(all(rule["observed"] == 0.9 for rule in locked["rules"]))

    def write_reference_runs(self, root: Path) -> tuple[list[Path], list[Path]]:
        details_paths: list[Path] = []
        metadata_paths: list[Path] = []
        base_metadata = self.complete_metadata(1)
        for run_index in (1, 2, 3):
            metadata = copy.deepcopy(base_metadata)
            metadata["repeat"] = {"index": run_index, "total": 3}
            details = self.complete_details(metadata)
            details_path = root / f"details-run{run_index}.json"
            metadata_path = root / f"metadata-run{run_index}.json"
            details_path.write_text(json.dumps(details), encoding="utf-8")
            metadata_path.write_text(json.dumps(metadata), encoding="utf-8")
            details_paths.append(details_path)
            metadata_paths.append(metadata_path)
        return details_paths, metadata_paths

    def complete_metadata(self, run_index: int) -> dict[str, object]:
        fixture_entries = self.dataset_identity["fixtures"]
        return {
            "evaluationSchema": runner.C17_REFERENCE_SCHEMA,
            "datasetValidation": "VALID",
            "datasetReleaseIdentity": self.dataset_identity,
            "evalSetIdentity": self.dataset_identity["questionSet"],
            "sampleSelection": {
                "ids": [sample["id"] for sample in self.samples],
                "count": len(self.samples),
            },
            "topK": 5,
            "minScore": 0.3,
            "enableRerank": True,
            "knowledgeBase": {
                "id": 77,
                "name": "codex-stage1-repro-eval",
                "description": "codex reproducible retrieval-only eval fixture",
                "vectorCollection": "kb-test-vector",
                "documentCount": 3,
                "chunkCount": 50,
                "embeddingGeneration": {
                    **self.reference_manifest["embeddingGeneration"]["identity"],
                    "fingerprint": "d" * 64,
                },
                "documents": [
                    {
                        "id": index,
                        "title": Path(item["path"]).name,
                        "status": "COMPLETED",
                        "chunkCount": [11, 14, 25][index - 1],
                        "contentHash": item["sha256"],
                    }
                    for index, item in enumerate(fixture_entries, start=1)
                ],
            },
            "fixtures": [
                {
                    "path": str(self.repo_root / item["path"]),
                    "name": Path(item["path"]).name,
                    "sha256": item["sha256"],
                    "bytes": item["bytes"],
                }
                for item in fixture_entries
            ],
            "configSnapshot": {
                str(self.repo_root / "rag-admin/src/main/resources/application.yml"): {
                    "sha256": "b" * 64,
                    "bytes": 100,
                }
            },
            "git": {"head": "c" * 40},
            "claimMetricConfig": {"version": "test"},
            "judgeContractConfig": {"version": "test"},
            "armManifest": None,
            "referenceManifest": {
                "referenceId": self.reference_manifest["referenceId"],
                "sha256": self.reference_manifest["sha256"],
                "mode": "full",
            },
            "repeat": {"index": run_index, "total": 3},
            "warmup": {"calls": 0},
        }

    def complete_details(self, metadata: dict[str, object]) -> dict[str, object]:
        evidence_samples = []
        for sample in self.samples:
            should_answer = bool(sample["should_answer"])
            recall_total = len(sample["expected_contexts"]) if should_answer else 0
            evidence_samples.append(
                {
                    "id": sample["id"],
                    "question": "must-not-enter-safe-pack",
                    "debugRetrieveRawResponse": {"secret": "raw-provider-body"},
                    "errors": {"retrieval": None, "ask": None},
                    "rerankAttribution": {
                        "requestedProvider": "heuristic",
                        "effectiveProvider": "heuristic",
                        "fallbackCount": 0,
                        "modelCallCount": 0,
                    },
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
            "claimMetricConfig": metadata["claimMetricConfig"],
            "judgeContractConfig": metadata["judgeContractConfig"],
            "sampleCount": len(evidence_samples),
            "topK": 5,
            "minScore": 0.3,
            "enableRerank": True,
            "skipAsk": True,
            "runMetadata": metadata,
            "samples": evidence_samples,
        }


if __name__ == "__main__":
    unittest.main()
