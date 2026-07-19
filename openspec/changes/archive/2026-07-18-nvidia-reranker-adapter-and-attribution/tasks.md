# Tasks: C6 NVIDIA Reranker Adapter And Attribution

## 0. Approval And Boundary

- [x] 用户确认 C5 债务已收口，并授权启动 C6 规划草案。
- [x] readiness 复核：工作区干净、`ACTIVE_TASK=IDLE`、C5 delta 已接受、change 已归档、技术债已标记完成。
- [x] 提交责任为 `用户手动提交`；本 change 不暂存、不提交、不 push、不部署。
- [x] 用户于 2026-07-19 将收口提交责任改为 `Agent 提交`；授权仅含计划内暂存与中文 commit，不含 push、PR、部署或发布。
- [x] 用户批准 proposal scope/non-goals 与 C6/C7 边界。
- [x] 用户批准 design 中 12 条决策记录。
- [x] 用户批准 `rag-system` spec delta 的 4 个 requirements / 11 个 scenarios。
- [x] 用户验收实现时确认新增决策 13：explanatory fallback 首个非空结果即停止，以保持 effective provider 单值归因。
- [x] 用户决定 live smoke：本轮不授权真实调用，明确接受 protocol-tested/real-endpoint-unverified 边界。

## 1. NVIDIA Protocol Adapter

- [x] RED：固定合成 query/passages，断言请求为 `model + query.text + passages[].text + truncate` 且 endpoint 为配置的 `/v1/ranking`。
- [x] RED：合法 `rankings[].index/logit` 按 logit 排序，同时保留原 retrieval score并写 rerank logit/rank metadata。
- [x] RED：空、重复、越界、缺失 index、NaN/Infinity logit 和超过候选上限都稳定拒绝。
- [x] 实现独立 `nvidia` provider、typed request/response 与默认关闭的配置，不覆盖既有 `model` adapter。
- [x] 验证配置不完整、provider 未选择或 rerank disabled 时真实 HTTP 调用为 0。

## 2. Outcome, Fallback And Diagnostics

- [x] RED：heuristic、generic model、NVIDIA success 均返回 requested/effective provider、candidate/scored/model-call/latency facts。
- [x] RED：not configured、health failed、timeout、4xx、5xx、network、invalid response、incomplete rankings 映射稳定 fallback taxonomy。
- [x] RED：NVIDIA partial/invalid response 整次使用 heuristic，不产生混合 effective provider。
- [x] 实现 typed rerank outcome 与 registry 单一 fallback policy；首版不自动 retry，单次 retrieval model call count 至多 1。
- [x] 将 rerank diagnostics 与 Milvus/keyword route diagnostics 无覆盖合并进 `RetrievalResult`。
- [x] 验证 diagnostics/log/client 不包含 key、Authorization、query、passages、context、raw body 或异常 message。

## 3. Debug, QA And Eval Attribution

- [x] RED：debug retrieval 使用 `retrieveWithDiagnostics`，正常/fallback/失败路径均返回 sanitized diagnostics。
- [x] RED：同步 QA metadata 保留本次实际生成 contexts 的 rerank attribution；explanatory retry 不误报调用次数/provider。
- [x] 实现 additive `RetrievalDebugResponse.diagnostics` 与同步 QA diagnostics 传递。
- [x] RED：Python runner 逐样本提取 requested/effective provider、fallback、model calls、coverage 与 latency。
- [x] 实现 Markdown/JSON aggregate：effective provider counts、model coverage、fallback reason histogram、total model calls、candidate coverage。
- [x] 验证既有 Report status、Recall@3/5、MRR、Top1、generation/citation/no-answer/judge 指标计算不变。

## 4. Offline Verification

- [x] 运行 NVIDIA adapter/registry/query/debug/QA 聚焦 Java tests。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 运行 `mvn -q test` 并记录 reports/tests/failures/errors/skipped。
- [x] 运行 SensitiveLogs 与定向 secret/user-content marker 扫描。
- [x] 运行 `git diff --check`、旧配置前缀/断链与受保护路径扫描。
- [x] 记录真实 embedding/rerank/judge/ask/LLM/provider 调用量为 0。

## 5. Optional Live Smoke And Closeout

- [x] `N/A`：用户未授权 live smoke，因此无需执行前调用披露；真实调用预算保持 0。
- [x] `N/A`：用户明确接受 real-endpoint-unverified 边界，未执行真实 NVIDIA ranking smoke。
- [x] 若未授权或 provider 不可用，明确记录 `SKIPPED` 与 protocol-tested、real-endpoint-unverified 边界。
- [x] 更新 proposal/design/tasks、长期说明和 `.ai/AGENT_LOG.md` 的实现/验证/跳过/风险证据。
- [x] 用户验收实现和 protocol-tested/real-endpoint-unverified 真实能力边界。
- [x] 用户验收后已接受 delta 进 baseline、恢复 `ACTIVE_TASK=IDLE` 并归档 change。
