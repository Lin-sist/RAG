import argparse
import sys
import unittest
from pathlib import Path
from unittest import mock

import run_rag_eval as direct
import run_reproducible_rag_eval as reproducible
import test_run_reproducible_rag_eval as helpers


class RetrievalPacingTest(unittest.TestCase):
    def test_pacing_precedes_timed_retrieval_and_429_is_counted_once(self):
        args = argparse.Namespace(base_url='http://localhost:8080', kb_id=7,
            top_k=5, min_score=0.3, enable_rerank=True, timeout=60,
            skip_ask=True, judge_mode='off', retrieval_delay_seconds=1.1)
        events = []
        def request(*unused):
            events.append('request')
            raise direct.ApiCallError('HTTP 429', http_status=429)
        with mock.patch.object(direct.time, 'sleep', side_effect=lambda n: events.append(('sleep', n))), \
             mock.patch.object(direct.time, 'monotonic', side_effect=[1, 1.125]), \
             mock.patch.object(direct, 'call_json', side_effect=request):
            result = direct.run_sample({'id': 'x', 'question': 'synthetic', 'should_answer': False}, args, 'token')
        self.assertEqual([('sleep', 1.1), 'request'], events)
        self.assertEqual(125, result.details['retrieveLatencyMillis'])
        self.assertEqual(1, result.rate_limit_errors)
        self.assertEqual(0, result.ask_retry_count)

    def test_retrieval_only_parent_passes_pacing_to_child(self):
        args = helpers.ReproducibleRagEvalTest.eval_command_args(include_ask=False)
        args.retrieval_delay_seconds = 1.1
        cmd = reproducible.build_eval_command(args, 7, Path('r'), Path('d'), Path('m'))
        self.assertEqual('1.1', cmd[cmd.index('--retrieval-delay-seconds') + 1])

    def test_invalid_pacing_rejected_before_execution(self):
        for module in (direct, reproducible):
            for invalid in ('-1', 'nan', 'inf'):
                with self.subTest(module=module.__name__, invalid=invalid), \
                     mock.patch.object(sys, 'argv', ['runner', '--retrieval-delay-seconds', invalid] + (['--kb-id', '7'] if module is direct else [])), \
                     mock.patch('sys.stderr'), self.assertRaises(SystemExit):
                    module.parse_args()


if __name__ == '__main__':
    unittest.main()
