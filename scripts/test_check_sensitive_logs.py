import unittest

from check_sensitive_logs import scan_text


class SensitiveLogGateTest(unittest.TestCase):

    def test_detects_java_question_value(self):
        findings = scan_text('log.info("question={}", question);', "Example.java")
        self.assertEqual(1, len(findings))
        self.assertEqual("sensitive-identifier", findings[0].rule)

    def test_detects_exception_message(self):
        findings = scan_text('log.error("request failed: {}", e.getMessage());', "Example.java")
        self.assertEqual(1, len(findings))
        self.assertEqual("exception-or-response-body", findings[0].rule)

    def test_detects_python_f_string_interpolation(self):
        findings = scan_text('print(f"question={question}")', "example.py")
        self.assertEqual(1, len(findings))

    def test_allows_safe_diagnostic_fields(self):
        findings = scan_text(
            'log.info("request completed, traceId={}, count={}", traceId, count);',
            "Example.java",
        )
        self.assertEqual([], findings)

    def test_allows_sensitive_words_without_values(self):
        findings = scan_text('console.info("API key copied")', "example.ts")
        self.assertEqual([], findings)

    def test_allows_safe_query_variant_metrics(self):
        findings = scan_text(
            'log.debug("merged {} variants, weight={}", queryVariants.size(), queryVariant.weight());',
            "Example.java",
        )
        self.assertEqual([], findings)


if __name__ == "__main__":
    unittest.main()
