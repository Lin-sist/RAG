import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OBSERVABILITY = ROOT / "deploy" / "observability"


class ObservabilityReferenceTest(unittest.TestCase):

    def read(self, relative_path: str) -> str:
        path = OBSERVABILITY / relative_path
        self.assertTrue(path.is_file(), f"missing reference file: {relative_path}")
        return path.read_text(encoding="utf-8")

    def test_compose_pins_images_and_only_publishes_loopback_entrypoints(self):
        compose = self.read("docker-compose.observability.yml")
        self.assertIn("otel/opentelemetry-collector-contrib:0.157.0", compose)
        self.assertIn("grafana/tempo:2.10.7", compose)
        self.assertIn("prom/prometheus:v3.13.1", compose)
        self.assertIn("grafana/grafana:13.1.1", compose)
        self.assertNotIn(":latest", compose)
        published = re.findall(r'-\s*"([^"]+:[0-9]+:[0-9]+)"', compose)
        self.assertEqual(
            ["127.0.0.1:4317:4317", "127.0.0.1:3000:3000"],
            published,
        )
        self.assertIn("GRAFANA_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:?", compose)
        self.assertIn("GF_AUTH_ANONYMOUS_ENABLED: \"false\"", compose)
        self.assertIn("GF_PLUGINS_PREINSTALL_DISABLED: \"true\"", compose)
        self.assertIn("GF_PLUGINS_PLUGIN_ADMIN_ENABLED: \"false\"", compose)

    def test_collector_has_bounded_trace_pipeline_and_unsampled_metrics(self):
        collector = self.read("collector/config.yaml")
        self.assertIn("memory_limiter:", collector)
        self.assertIn("tail_sampling:", collector)
        self.assertIn("decision_wait: 5s", collector)
        self.assertIn("num_traces: 5000", collector)
        self.assertIn("expected_new_traces_per_sec: 50", collector)
        self.assertIn("sampling_percentage: 10", collector)
        self.assertIn("key: rag.outcome", collector)
        self.assertIn("values: [timeout, cancelled, TIMEOUT, CANCELLED]", collector)
        self.assertIn("key: rag.fallback.count", collector)
        self.assertIn("otlp_grpc/tempo:", collector)
        self.assertNotIn("otlp/tempo:", collector)
        self.assertRegex(collector, r"traces:\s+receivers: \[otlp\]\s+processors: \[memory_limiter, resource, tail_sampling, batch\]")
        self.assertRegex(collector, r"metrics:\s+receivers: \[otlp\]\s+processors: \[memory_limiter, resource, batch\]")
        self.assertNotRegex(collector, r"(?m)^\s*(logging|debug)(/[^:]*)?:")

    def test_retention_auth_and_local_only_boundaries_are_explicit(self):
        tempo = self.read("tempo/tempo.yaml")
        prometheus = self.read("prometheus/prometheus.yml")
        compose = self.read("docker-compose.observability.yml")
        env_example = self.read(".env.example")
        self.assertIn("block_retention: 72h", tempo)
        self.assertIn("--storage.tsdb.retention.time=7d", compose)
        self.assertIn("rule_files:", prometheus)
        self.assertIn("alerts.yml", prometheus)
        self.assertEqual("GRAFANA_ADMIN_PASSWORD=replace-with-a-local-secret\n", env_example)
        self.assertNotIn("GF_AUTH_ANONYMOUS_ENABLED: \"true\"", compose)

    def test_rules_are_non_sla_and_exclude_latency_alerts(self):
        rules = self.read("prometheus/alerts.yml")
        self.assertIn("RagAskErrorRatioHigh", rules)
        self.assertIn("RagProviderFallbackRatioHigh", rules)
        self.assertIn("OtelCollectorTelemetryFailure", rules)
        self.assertGreaterEqual(rules.count("non_sla_reference"), 3)
        self.assertNotRegex(rules.lower(), r"alert:\s*.*latency")
        for forbidden in ("user", "document", "chunk", "trace_id", "span_id", "rag_model"):
            self.assertNotIn(forbidden, rules.lower())

    def test_dashboard_json_has_required_low_cardinality_views(self):
        dashboard = json.loads(self.read("grafana/dashboards/rag-overview.json"))
        titles = {panel["title"] for panel in dashboard["panels"]}
        required = {
            "Operation traffic",
            "Operation outcomes",
            "In-flight operations",
            "Operation latency",
            "Stage latency",
            "Provider calls",
            "Fallback ratio",
            "Token usage coverage",
            "Collector failures",
            "Trace search",
        }
        self.assertTrue(required.issubset(titles))
        serialized = json.dumps(dashboard, ensure_ascii=False).lower()
        for forbidden in ("rag.user", "rag.document", "rag.chunk", "rag.model"):
            self.assertNotIn(forbidden, serialized)
        datasource = self.read("grafana/provisioning/datasources/datasources.yml")
        self.assertIn("uid: prometheus", datasource)
        self.assertIn("uid: tempo", datasource)


if __name__ == "__main__":
    unittest.main()
