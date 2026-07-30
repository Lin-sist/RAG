# C16 Bounded Query Router 实现与验证追踪

> 状态日期：2026-07-30  
> 当前状态：实现完成、等待用户验收；尚未接受进 baseline 或归档。

## 固定身份

- Release：`bounded-query-router-eval-v1`
- Dataset：`rag-eval-dev-v2`，150 条，保持原 artifact/hash/order 不变
- Route sidecar：20 条，仅包含 sample ID、expected intent、required status
- 分布：FACT=10、UNSUPPORTED=10、INVALID=0、required=20
- Classifier / strategy / policy：`fact-intent-v1` / `fact-v1` / `evidence-no-answer-v1`
- Budget：`fact-v1-default-budget`
- Live evaluation：`SKIPPED`
- 真实 provider/model calls：0
- Business data outbound：false

## Requirement / scenario 追踪

| 能力 | 主要证据 | 结论边界 |
|---|---|---|
| default-off 与配置 fail-fast | `BoundedQueryRouterTest` | 覆盖 unknown classifier/policy/strategy 与全部 budget hard bounds；不改变生产默认开关 |
| 确定性分类 | `BoundedQueryRouterTest`、`RouterEvaluationClassifierContractTest` | 20 条冻结 sidecar intent 全匹配；重复、并发、locale/timezone、Unicode fuzz 均为本地零外调 |
| closed-world strategy | `QueryStrategyRegistry`、`FactQueryStrategyExecutor` 与 Router tests | 只注册 `fact-v1`；没有 reflection、dynamic class 或 plugin |
| budget 与 diagnostics | `QueryBudgetLedgerTest`、`RAGServiceImplTest`、`AnswerGeneratorTelemetryTest` | 覆盖一次 retrieval/generation、variant/rerank、candidate/context、context/output token、deadline 与 unknown diagnostics fail-closed |
| evidence/no-answer | `EvidenceNoAnswerPolicyTest`、`RAGServiceImplTest` | 区分 insufficient evidence、model refusal、unvalidated evidence 与 dependency error |
| sync / SSE | `RAGServiceImplTest.enabledFactSyncAndStreamShareRoutePolicyFinalStateAndUsageSemantics` | 相同 fixture 的 classifier/strategy/policy/final state/reason/usage 对齐；SSE wire 未增加事件 |
| MCP read-only ask | `C16McpRouterIntegrationTest` 与既有 C15 schema/guard tests | MCP 复用同一 `RAGService.ask`；不新增 client selector 或 Tool schema 字段 |
| release validator | `test_router_eval_contract.py` | 11 个用例覆盖 path/hash/bytes/order/count/ID/enum/channel/dataset/coverage 漂移；plan-only、零外调 |
| evaluator/status | `test_evaluate_bounded_query_router.py` | 10 个用例覆盖七通道、confusion/precision/recall/coverage/leakage、denominator、四状态/退出码与 no-overwrite |
| runtime/eval 隔离 | `RouterEvaluationClassifierContractTest` 与 source scan | production classifier 不读取 `docs/eval`、sidecar、sample ID 或 label |

## 本轮验证事实

- Python：`python -B -m unittest discover -s scripts -p 'test_*.py'`，211 tests，0 failures/errors/skips。
- Java focused：C16 Router/executor/evidence/budget/generation/SSE/MCP 聚焦测试均通过。
- Maven full：616 tests，1 failure，0 errors，2 skips；唯一 failure 为既有 `GenAiTracingConfigurationTest` 不可用 collector 日志捕获时序，隔离重跑该类 9/9 通过。由于全仓命令并非全绿，本轮不把 Maven full 写成 `PASS`。
- 前端：`SKIPPED`，本 change 未修改前端或公开 SSE wire。
- Live router ask/eval：`SKIPPED`，未获真实 embedding/rerank/generation/judge/provider 调用授权。

## 受限结论

本证据只支持 default-off 的单轮事实型 `fact-v1`、固定预算与 deterministic evaluation contract。它不证明真实 provider 质量或费用、生产 SLA、multi-hop/global/high-risk strategy、生产默认开启、客户端可选路由或 Agentic RAG。
