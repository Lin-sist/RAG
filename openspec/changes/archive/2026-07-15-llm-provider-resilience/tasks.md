# Tasks: C4b LLM Provider Resilience

## Phase 0：启动与规格草案

- [x] 按顺序读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、`openspec/project.md`、相关 baseline spec、冻结蓝图和当前 LLM/QA 代码与测试。
- [x] 确认 C3 已归档、`main` 与 `origin/main` 一致、工作区无未提交改动，C4b 顺序前置满足。
- [x] 将能力状态分类为 `confirmed / partial / planned / out_of_scope / unknown`。
- [x] 创建 proposal，包含“改前坏事 → 改后不同”的用户故事。
- [x] 创建 design、tasks 与 `rag-system` spec delta 草案。
- [x] 把公共故障契约矩阵模板应用到 C4b，不创建独立 C4a change。
- [x] 声明规格/实现验证中的 embedding/rerank/judge/ask/LLM 业务调用量均为 0，并披露 runtime `1+N` 理论放大边界。
- [x] 启动草案时提交责任为“用户手动提交”；规格批准后用户已追加授权为“Agent 提交”。
- [x] 将 `.ai/ACTIVE_TASK.md` 置为 `ACTIVE` 并指向本 change。
- [x] 完成 change 结构、必需标题/字段、Markdown 链接、范围和 `git diff --check` 验证。
- [x] 用户审阅并明确批准 proposal、design 与 spec delta；批准后按 TDD 进入实现。

## Phase 1：同步 provider 故障契约（TDD）

- [x] RED：使用本地 `HttpServer` 添加 429→成功、连续 503、timeout、非重试型 4xx、malformed 2xx 测试，证明当前 attempt/diagnostics 契约未被锁定。
- [x] 固定 `max-retries=N` 表示总尝试上限 `1+N`，tracked 默认保持 0。
- [x] 只允许 429、5xx、timeout、I/O/connect 进入 retry；其他 4xx、invalid response 不重试。
- [x] 补齐 `attemptCount`、`retryCount`、`retryExhausted` 和稳定 `errorCategory`，不暴露原始 provider message/body。
- [x] 保持 OpenAI-compatible 与 Qwen 成功解析行为不变。
- [x] 运行 `rag-core` 聚焦测试并更新 tasks/AGENT_LOG。

## Phase 2：流式首 chunk 边界（TDD）

- [x] RED：添加首 chunk 前 transient failure 可重试测试。
- [x] RED：添加已输出 `alpha` 后 provider failure 测试，断言当前完整 Flux retry 会造成或可能造成重复订阅风险。
- [x] 实现 `BEFORE_FIRST_CONTENT / AFTER_FIRST_CONTENT` gate；首 chunk 后禁止重订阅。
- [x] 耗尽或首 chunk 后失败时只发送稳定 `[ERROR]` 与 `[DONE]`，不泄露 provider body/prompt/context。
- [x] client disconnect、emitter send failure 和 timeout 取消订阅，不触发 provider retry。
- [x] 运行 `rag-core` 流式聚焦测试并更新 tasks/AGENT_LOG。

## Phase 3：API 与副作用（TDD）

- [x] RED：同步 generation failure 保持 HTTP 200 外层但 `metadata.status=error`，citations/contexts 为空且 machine-readable diagnostics 完整。
- [x] RED：同步失败不保存 QA history、不写 cache，query count 只增加一次。
- [x] RED：SSE 失败或部分输出后失败不保存 history，成功 complete 才保存完整 answer。
- [x] `RAGServiceImpl` 按 diagnostics category 映射稳定客户端提示，不再依赖原始 LLM message 猜测 429/timeout。
- [x] 保持 retrieval/no-answer/citation 成功路径和评测口径不变。
- [x] 运行 `rag-admin` 聚焦测试并更新 tasks/AGENT_LOG。

## Phase 4：安全与完整验证

- [x] 用合成 API key、Authorization、provider body、prompt/context marker 验证响应、metadata 和日志均不泄露。
- [x] `mvn -q -pl rag-core -am test` 通过。
- [x] `mvn -q -pl rag-admin -am test` 通过。
- [x] `mvn -q test` 通过，记录 suites/tests/failures/errors/skipped 与既有内部降级日志。
- [x] `python -B -m unittest discover -s scripts -p 'test_*.py'` 通过。
- [x] SensitiveLogs 门禁通过。
- [x] 无前端改动，已明确跳过正式前端 build。
- [x] `git diff --check`、change 结构、Markdown 相对链接和计划文件范围检查通过。
- [x] 确认真实 embedding、rerank、judge、ask/LLM 业务调用量均为 0。

## Phase 5：验收与收口

- [x] 更新本 tasks 的真实完成状态、验证结果、跳过原因与剩余风险。
- [x] 将修改文件、关键决策、验证结果和 `Commit: pending` 追加到 `.ai/AGENT_LOG.md`。
- [x] 用户完成 review 并明确确认 C4b 实现验收通过。
- [x] 把已批准 delta 按原文接受进 `openspec/specs/rag-system/spec.md` 并验证 exact match。
- [x] 将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE`，经用户确认后归档 change。
- [x] 为实现提交准备中文 Conventional Commit；提交后等待用户确认，不 push。

## Guardrails

- proposal/design/spec delta 未获用户明确批准前，不修改生产 Java、配置、测试或评测脚本。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。
- 不新增/升级依赖，不连接真实 provider，不执行批量 ask、judge、embedding 或 rerank。
- 不修改 API DTO shape、数据库 migration、认证、权限、retrieval、chunking、rerank、prompt、citation/no-answer 或评测指标。
- 不实现跨 provider fallback、熔断器、结构化 SSE、Redis/Milvus 故障或索引恢复。
- 不为故障测试定制生产 prompt 或 provider response parser 的成功语义。
- 未取得单独授权不得 push、创建 PR、部署、发布或真实业务外调。
- 提交责任：`Agent 提交`；用户已授权本 change 计划内文件的本地暂存与中文提交，不包含 push。
