# Tasks: C16 Bounded Query Router

## 0. 事前闸门（实现前必须完成）

- [x] 用户已于 2026-07-30 审阅并批准统一 proposal 范围、非目标、default-off 策略与 C16 受限完成口径，并要求开始实现。
- [x] 用户确认 design 的 18 条决策，含 enabled 模式只注册 `fact-v1`、非 fact 返回 `UNSUPPORTED` 而非 legacy passthrough、确定性 classifier、deadline 保证边界、SSE wire 不变与 cache compatibility identity。
- [x] 用户批准 `rag-system` 5 requirements / 17 scenarios 与 `evaluation` 4 requirements / 12 scenarios spec delta 进入实现；baseline acceptance 仍等待最终验收。
- [x] 用户确认 `evidence-no-answer-v1`：no-answer 不是前置 route；insufficient evidence 不调用 generation；dependency error 不算 no-answer；fact answer 至少需要 validated citation evidence。
- [x] 实现授权已明确；提交责任保持 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 实现开始前复查完成：HEAD=`4d5fef5`，工作区与暂存区干净，唯一 active change=`bounded-query-router`，accepted baseline 未漂移。
- [x] 范围守卫确认：如实现需要新增/升级依赖、修改公开 request DTO/SSE wire/MCP schema、数据库 migration、前端或生产默认开关，停止并返回独立事前闸门，不在 C16 偷渡。

## 1. Router Contract And Default-Off Compatibility

- [x] RED：配置 tests 证明 `rag.router.enabled=false` 为默认，unknown classifier/policy/strategy、无效或超硬上限 budget fail startup。
- [x] 新增 immutable `QueryIntent`、`QueryRouteReason`、`QueryStrategyId`、`QueryExecutionBudget`、`QueryRoutePlan`、`QueryBudgetUsage`、`QueryFinalState` 与 `NoAnswerReason`。
- [x] 新增 `RouterProperties`，固定 classifier/policy/strategy identity 与 server-owned ceilings；禁止客户端 selector。
- [x] RED：disabled 模式对 sync/SSE/MCP/cache/provider 调用图与 C16 前兼容，不产生虚假 fact attribution。
- [x] 实现 `BoundedQueryRouter` 与 closed-world registry；首版只注册 `fact-v1`，不支持 reflection/dynamic class/plugin。
- [x] 确认值对象、异常与普通日志不保存 raw query、tenant/user/KB、context/answer、credential、provider body 或异常 message/stack。

## 2. Deterministic `fact-intent-v1` Classifier

- [x] RED：fact/definition allowlist、multi-hop/global/high-risk/规划/比较综合 unsupported、ambiguous、invalid/blank/overlength/control-char matrix。
- [x] RED：相同输入重复/并发执行的 intent/reason/version byte-stable，locale/time/randomness 不改变结果。
- [x] 实现 bounded normalization 与有限 cue priority；保守冲突规则输出 `UNSUPPORTED/AMBIGUOUS`。
- [x] 验证 classifier 不依赖 Spring context、database/cache/network/provider 或 eval artifact。
- [x] 验证 classifier provider/model calls=0，且 runtime source 不读取 sample ID/type、route sidecar、expected answer/source/context 或 fixture canary。
- [x] 用 property/fuzz-style tests 覆盖 Unicode、长文本、regex backtracking 与稳定运行时上限。

## 3. Budgeted Fact Retrieval Slice

- [x] RED：`fact-v1` 只能执行一次 retrieval orchestration，不进入 explanatory empty-result second pass。
- [x] 为 query variant builder 增加 server-owned max variants 与实际 count diagnostics；原始 query 占用预算并始终优先。
- [x] RED：variant ceiling 到达后不再调用 embedding/vector，实际 variant count 与 model/provider calls 可核对。
- [x] 将 query variants、retrieval pass、rerank call、generation call 与 elapsed facts 写入 usage ledger。
- [x] 将 candidate/context counts 与 estimated context/output tokens 补入 usage ledger，并覆盖 unknown diagnostics。
- [x] RED：budget 不足、unknown diagnostics 或 deadline 到期阻止下一 stage；不得 fallback、整链 replay 或写成功 cache。
- [x] 保持 server-derived `TenantVectorScope`、reserved filter、hybrid/BM25/RRF、heuristic default、Milvus degradation 与 unsupported adapter fail-closed contract。
- [x] 运行 QueryEngine/tenant/vector/rerank 聚焦 tests，记录 tests/failures/errors/skips 与 provider calls。

## 4. Unified Evidence/No-Answer And Sync Fact Execution

- [x] RED：empty/under-minScore evidence -> `NO_ANSWER/INSUFFICIENT_EVIDENCE` 且 generation calls=0。
- [x] RED：retrieval dependency error -> `ERROR`，不映射 no-answer；accepted keyword-only degradation 保留独立 diagnostics。
- [x] 新增 `EvidenceNoAnswerPolicy` 的 pre/post decision value objects，固定 `evidence-no-answer-v1` identity。
- [x] 抽取/实现 `FactQueryStrategyExecutor`，复用现有 QueryEngine/AnswerGenerator/CitationValidator，不改 provider/prompt/rerank/citation defaults。
- [x] RED：model refusal -> `NO_ANSWER/MODEL_REFUSAL`；无 validated citation 的 fact answer -> `NO_ANSWER/UNVALIDATED_EVIDENCE`；有证据回答 -> `ANSWER`。
- [x] 将 classifier/strategy/policy/final-state/budget usage 白名单加入 sync QA metadata，unknown facts不伪造成功/零调用。
- [x] 更新成功 cache identity，隔离 legacy/router、classifier、strategy、policy 与 budget profile。
- [x] 增加 budget exhausted/error/unsupported/invalid 不写普通成功 cache 的集成用例。
- [x] 运行 RAGService、AnswerGenerator、CitationValidator、cache 与 property tests，确认既有 retrieval/generation/citation/no-answer公式未改变。

## 5. SSE, MCP And Telemetry Integration

- [x] RED：相同 deterministic fixture 下 sync/SSE/MCP ask 的 effective strategy、policy、final state/reason 与 usage 语义一致。
- [x] 扩展 stream terminal signal/内部 diagnostics，保留 final state 与 usage；不新增 SSE wire event、不修改前端协议。
- [x] RED：SSE unsupported/no-answer/error 不保存正常成功 history，timeout/cancel 不缓存 partial output或重放 generation。
- [x] 让 C15 `rag.ask` 继续复用同一 `RAGService.ask` Router/policy；MCP Tool schema 不增加 route/budget/provider selector。
- [x] RED：既有 MCP raw Tool input validator 继续拒绝 strategy/classifier/budget/provider/model/timeout/retry/tenant selector，server plan 不接受客户端覆盖。
- [x] 在 GenAI telemetry 白名单中加入 bounded classifier/strategy/policy/final state/reason/budget outcome，不加入 query、identity、高基数数值或自由文本。
- [x] 运行 C15 MCP schema/guard/default-off/profile 相邻回归。
- [x] 在 C16 deterministic integration 中复核 MCP authoritative before/after 与 route terminal attribution。

## 6. Versioned Router Evaluation Release

- [x] RED：validator tests 覆盖 artifact 缺失、unsafe path、hash/bytes/order/count drift、duplicate/missing/unexpected ID、unknown enum/channel、dataset mismatch 与 sidecar coverage gap。
- [x] 新增 route expectation sidecar schema/artifact，只保存 sample ID、expected intent/required status，不复制 question/answer/context。
- [x] 新增 `bounded-query-router-eval-v1` manifest，固定 `rag-eval-dev-v2` identity、sidecar、classifier/strategy/policy/budget/evaluator version与 ordered samples。
- [x] 实现 Python 标准库 plan-only validator，在 backend/container/credential/provider 前 fail fast；正式 artifact no-overwrite。
- [x] RED：runtime classifier source/behavior 不读取 sidecar/sample labels；production规则与 eval artifact 物理/依赖隔离。
- [x] 记录 release count/distribution/hash 与 validator zero-call/zero-egress 证据。

## 7. Evaluator, Metrics And Status

- [x] RED：confusion/FACT precision/recall/coverage/unsupported leakage denominator 缺失或不一致时 evaluator fail closed。
- [x] 实现 classification、strategy execution、budget、retrieval、generation/citation、no-answer、errors 七通道独立聚合。
- [x] RED：unsupported/invalid case 的任一 retrieval/rerank/generation/provider call、unknown strategy、missing attribution 或 budget violation 阻止 PASS。
- [x] 按 effective strategy 切分既有 retrieval/generation/citation/no-answer指标，但不修改公式、dataset annotation、Report status 或历史报告。
- [x] 实现独立 Router `PASS/FAIL/NOT_EVALUABLE/INVALID` 与固定 exit code；required missing/error/skip/identity drift不能被成功子集掩盖。
- [x] 确保 summary/details/metadata 的 identity、counts、status、provider calls、egress 与 violation 完全一致。
- [x] 普通报告只输出 sample ID、enum/status/category、计数与安全数值；扫描 raw query/answer/context/citation/token/provider body/exception/absolute path 为 0。

## 8. Deterministic Integration Evidence

- [x] 建立/复用隔离 deterministic embedding/generation fixture；不得使用真实 credential、业务数据或常驻基础设施。
- [x] 执行 required fact/unsupported/invalid/no-answer/error/budget/deadline matrix，provider/model calls=0、business data outbound=false。
- [x] 对 sync/SSE/MCP/cache/telemetry 做相同 route identity 与 final-state traceability。
- [x] 记录 missing/unexpected/failed/errors/skipped、unsupported leakage、budget violations 与各通道 status；任一 required 非零缺口阻止验收。
- [x] live router ask/eval 未单独披露并授权时保持 `SKIPPED`，不把 deterministic evidence 写成真实 provider质量、费用或 SLA。

## 9. Full Gates And Closeout

- [x] 运行新增/受影响 Java focused tests，并按风险运行 Maven module/full suite；记录真实 reports/tests/failures/errors/skips。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`；确认既有 eval contract 与历史 baseline 未漂移。
- [x] 前端无改动时正式 build 记为 `SKIPPED`；若意外修改前端/API wire，停止并回到事前闸门。
- [x] 运行 SensitiveLogs、protected paths、credential/absolute path、eval-artifact leakage、Markdown links、no-overwrite 与 `git diff --check`。
- [x] 建立 C16 requirement/scenario -> test/case/evidence traceability，明确 deterministic/live、provider calls、egress 与受限结论。
- [x] 更新 tasks、`.ai/AGENT_LOG.md`、`openspec/project.md`、architecture/roadmap/optimization/eval guide 的当前事实。
- [ ] 用户最终验收后才将双 delta literal exact-copy 接受进 baseline、归档 change 并将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。
- [ ] 归档措辞只声明 default-off bounded `fact-v1` 与固定 evaluation profile；不宣称生产默认、真实 provider收益、multi-hop/global/high-risk或 Agentic RAG。

## 10. 外部调用与提交边界

- [x] 当前实现与 deterministic 单元/模块主证据真实 embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- [x] 如需 live router evaluation，先披露 sample/strategy分布、provider/model、最大调用/尝试、query/context 出站、费用/零费用依据、限流、timeout/retry与 artifact handling，并取得用户明确授权。（本轮未请求 live，状态保持 `SKIPPED`。）
- [x] 未获 `Agent 提交` 授权前不暂存、不提交；即使后续授权本地提交，也不自动 push、建 PR 或部署。
