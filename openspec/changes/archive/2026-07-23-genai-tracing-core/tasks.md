# Tasks: C11 GenAI Tracing Core

## 0. Approval And Boundary

- [x] 用户要求检查 C11 readiness，允许则直接开始规划。
- [x] readiness：启动前 HEAD=`d85d85a`，工作区干净，`main...origin/main [ahead 10]`，`ACTIVE_TASK=IDLE`，C10 已接受 baseline 并归档，无其他 active change。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 创建 proposal、design、tasks 与 `rag-system` spec delta，并激活 `.ai/ACTIVE_TASK.md`。
- [x] 规划阶段真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用与数据出站为 0。
- [x] 用户批准 proposal 的 C11/C12 边界、default-off、separate trace、lineage 与隐私方案。
- [x] 用户批准 design 的 14 条决策记录及 `rag-system` delta 的 4 requirements / 12 scenarios。
- [x] 用户明确授权新增既有 Boot BOM 管理下的 OTel API/SDK 依赖并进入 Java TDD implementation。

## 1. OTel Foundation And Safe Facade

- [x] RED：disabled/no-op、enabled SDK、fixed instrumentation scope、telemetry fail-open 行为被测试锁定。
- [x] RED：attribute/event allowlist 拒绝 raw content、credential、异常 message/stack 与动态 span name。
- [x] GREEN：按 common API / admin SDK / test exporter 分层添加依赖，不引入 runtime network exporter。
- [x] GREEN：实现 safe tracer facade、fixed names/keys/outcomes、safe error recorder 与配置开关。

## 2. Request Propagation MDC And Async Context

- [x] RED：W3C 优先、合法 custom pair fallback、非法/不完整 header 忽略、响应头/MDC 与 OTel id 一致。
- [x] RED：普通/async servlet scope、executor thread reuse 后 context/MDC 不泄漏。
- [x] GREEN：升级 `TraceFilter/TraceContext` bridge；disabled 时保持 legacy 行为。
- [x] GREEN：task submission 捕获可选 submission context，但 ingest 执行创建 independent root + link。

## 3. Ingest Trace And Durable Lineage

- [x] RED：submit、fresh execution、retry、failure、resume-without-parent 的 root/link/status/phase 行为正确。
- [x] RED：parse/chunk、batch embedding、vector upsert、keyword upsert、SQL finalize 只在真实执行时出现。
- [x] RED：`ingestTaskId/documentId/chunkId` metadata round-trip；旧 metadata 不触发业务失败。
- [x] GREEN：instrument durable indexing 主链路；不为每 chunk 建 span，不保存 OTel trace/span id。

## 4. Ask Trace Sync And Streaming

- [x] RED：sync cache hit/no-result/success/error topology 与 outcome 正确，未执行阶段无 span。
- [x] RED：retrieval route、query variant embedding/vector、keyword/fusion/rerank 与 provider/fallback diagnostics 一致。
- [x] RED：prompt/LLM/citation 阶段及 retry/fallback/real-vs-estimated token 语义正确。
- [x] RED：stream complete/error/cancel/timeout 各结束一次且覆盖真实 lifecycle；不逐 token 记录。
- [x] GREEN：instrument `RAGServiceImpl/QueryEngineImpl/AnswerGeneratorImpl` 与 SSE lifecycle integration。

## 5. Lineage Privacy And Cardinality

- [x] 最终 selected contexts 最多产生 final `topK` 个 `rag.lineage.context` events，rank/score/id 不进入 span name 或 metrics。
- [x] lineage 完整时为 `COMPLETE`；旧索引缺 task id 为 `PARTIAL`；完全缺失为 `MISSING`，三者均不改变 QA 结果。
- [x] sentinel 测试遍历 span name/attributes/events/status/log capture，确认 question/prompt/answer/context/snippet/content/file/title/user/credential/provider body/error message/stack 零泄漏。
- [x] telemetry helper 自身失败时业务继续，且只产生安全固定诊断。

## 6. Verification And Acceptance

- [x] 运行 common/core/admin 聚焦 tests，记录 suites/tests/failures/errors/skips。
- [x] 运行 `mvn -q test`。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`，确认评测工具无回归。
- [x] 运行 SensitiveLogs、changed Markdown links、secret/absolute-path、受保护路径与 `git diff --check`。
- [x] 前端无改动时正式 build 记为 `SKIPPED`；无 Docker/live dependency 行为时 Testcontainers/live backend 记为 `SKIPPED`。
- [x] 真实 provider/exporter 调用、外部传输与数据出站保持 0。
- [x] 用户验收实现与验证证据后，原文接受 delta、同步长期事实源、归档 change 并恢复 `ACTIVE_TASK=IDLE`。
