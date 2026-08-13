# RAG 项目技术债清单

> 状态日期：2026-08-10
> 本文只登记当前未完成或仍需独立证据的债务，不是活动任务计划。重大改动必须进入独立 OpenSpec change；完成状态以 accepted spec、代码、正式 evidence 和 archive 为准。
> 长期顺序见 `docs/roadmap/iteration-blueprint.md` v6。

## P0：进入 C17 前

### 1. C17 retrieval profile reference readiness

- 当前 `rag-eval-dev-v2-retrieval-regression-v1` 仍为 `DRAFT / PENDING_REFERENCE_EVIDENCE`，12 条 target 均为 `null`。
- 正式 evidence 计划为固定身份 v2/150 × 3 repeats；最多 450 次 debug retrieval、450 次 query embedding。
- provider/model、KB/fixture、Git/config、数据出站、费用/零费用依据、限流、timeout/retry 与 raw artifact 策略尚未在新的执行闸门确认。
- 具体 hard floors 与 regression tolerances 必须在完整 evidence 产生后由用户审阅，不得提前猜测。

### 2. 开发态 JWT fallback 治理

- `application.yml` 的开发态 JWT fallback 不属于既有数据库认证 closeout。
- 需要独立评估本地易用性与误带生产环境的风险；不得顺手修改 `.env.local` 或 `application-dev.yml`。

## P1：真实质量证据与用户结果

### 1. Generation / citation / no-answer 真实基线

- C9a 已形成 deterministic objective lexical claim alignment，但真实 v2/150 generation evidence 尚未执行。
- 首轮应在 judge off 下固定 full v2/150、provider/model、KB/fixture、Git/config 与 raw artifact 边界，分开报告 retrieval、generation、citation、claim support、no-answer 和 errors。
- 词法 claim support 不能表述为语义蕴含或完整事实正确性。

### 2. Judge 真实校准

- C9b tooling、24-case 四象限 gold corpus 与状态语义已完成；live calibration 仍为 `SKIPPED`。
- accepted contract 固定 canary 4 × 1、full 24 × 3，最大 judge calls 为 4 + 72；任一 case/repeat 缺失不得从成功子集下 agreement 结论。
- 校准通过前 judge 不应成为 required release gate，也不得自动搜索阈值。

### 3. Objective / judge quality profile

- 当前首个 C10 profile 只覆盖 retrieval，不包含 generation/objective 或 judge gate。
- 需要在真实 evidence 充分后分别建立新 profile/version；retrieval、objective 与 judge 保持独立，judge 可先作为 advisory rule。

### 4. SSE 结构化 terminal result

- 当前流式路径仍以文本 chunk 为主，客户端不能稳定获得 citations、final state、reason、route/budget attribution 与 cancel/error 终态。
- 需要设计兼容 terminal event，并保证同步、SSE 与 MCP 的 final-state 语义一致；中断或 partial output 不得保存为正常成功历史。

### 5. 分块结构专项

- 标题感知、长代码块、长段落与父子块策略尚未形成独立、可比较 evidence。
- 保持 `420/80` 为稳定基线，只做单变量、可回滚实验；不得为固定评测题逐题定制。

### 6. 恢复与中断演练

- durable index ledger/finalize 已完成，但仍需按独立计划持续演练进程中断、输入损坏、长时间 outage 与恢复操作手册。
- 演练结果不得与生产 RTO/RPO 或 HA 声明混用。

## P2：有界策略、知识源与深度研究

### 1. Router 高级策略

- C16 仅完成 default-off `fact-intent-v1 / fact-v1 / evidence-no-answer-v1`。
- `multi-hop-v1 → compare-v1 → temporal-v1 → global-v1 → high-risk-v1` 需要逐项独立 classifier、预算、数据集、评测、门禁与 capability claim。
- 未支持或含糊输入继续 fail closed，不能回退 legacy 或伪装成 fact。

### 2. Knowledge Source Registry

- 尚无统一但权限不扁平化的 indexed document、SQL/API、Web、MCP、Graph、multimodal source contract。
- 需要补 source identity、tenant/permission、freshness/effective time、sensitivity、provenance、failure、cost 与 cache policy。
- 模型和客户端不得选择 tenant、collection、provider/model、任意 filter 或无限预算。

### 3. 多模态证据

- 表格、图片/OCR、页面坐标、音视频证据尚未进入 citation/evaluation contract。
- 顺序为表格与图片/OCR优先，音视频后置；所有结果仍需回连 artifact 与位置证据。

### 4. MCP 2026 migration

- C15 evidence 固定 MCP spec `2025-11-25`，不能直接作为 2026-07-28 验收。
- 需要独立处理协议 breaking changes、stateless core、Tasks、MRTR/input-required、trace context、authorization 与 compatibility。
- 远程 OAuth/TLS/proxy trust 未完成前保持 default-off/local-only。

### 5. Durable investigation task

- 尚无可恢复、可取消、带 checkpoint/预算/计划/claim-evidence artifact 的深度研究 runtime。
- 首版应采用单 durable task + 有界并行子查询，不从 multi-agent swarm 起步。
- 长期 memory 必须 tenant-scoped、可检查、可删除、有 TTL；聊天历史不默认持久化为 memory。

## P3：受控行动与生产化

### 1. 受控写操作与 A2A

- 写 Tool、dry-run、人工审批、幂等、补偿/不可逆风险和审计 contract 尚未实现。
- A2A 只在单 Agent durable task 已成熟、可恢复、可取消、可评测后进入；不替代内部领域模型或 MCP。

### 2. 真实租户与向量库迁移

- C14 的 26/26 PASS 只证明固定 synthetic dual-tenant matrix。
- 真实 Milvus shadow migration/readiness 切换、回滚、第二业务 tenant 与 tenant lifecycle 尚未完成。
- Qdrant/Elasticsearch 仍缺等价 tenant isolation evidence；在 enforcement mode 下必须继续 fail startup，不能静默降级。

### 3. MCP 远程生产边界

- MCP OAuth Authorization Profile、Protected Resource Metadata、audience/resource binding、scope、TLS、proxy trust、Origin、rate limit 与租户审计尚未完成。
- 真实 provider smoke、远程 rollout 与生产开放需要独立 change/evidence/授权。

### 4. 可观测性、容量与 SLA

- C12 只完成单机 synthetic reference stack；生产 HA、容量/费用、合规 retention、租户观测权限、跨主机传输、通知、备份恢复和 SLA 仍未验证。
- local sampling、单机 dashboard 或功能测试不得外推为生产 SLA。

### 5. 前端与反馈闭环

- 前端 design token、空态/错态/处理中态、可访问性、深度研究 task UI、approval UI 和 evidence inspection 仍需分阶段设计。
- production-like corpus、用户反馈治理、drift monitoring 与 release rollback 尚未闭环。

## 已完成、不得按“从零接入”重复立项

- 数据库认证、主链路 integration、LLM/Redis/Milvus 故障语义；
- durable index input、task ledger、恢复协调与幂等 finalize；
- BM25 + dense vector + RRF hybrid retrieval；
- reranker 接口、heuristic/NVIDIA adapter、fallback 与 provider attribution；
- v1/v2 dataset governance、claim objective metrics、judge contract、offline quality gate evaluator；
- GenAI tracing core 与单机 OTel reference export/metrics；
- tenant identity/enforcement 与固定 synthetic isolation matrix；
- default-off/local-only C15 read-only MCP；
- default-off、仅 fact 策略的 C16 bounded Router。

## 治理基础

- 根目录 `AGENTS.md`：协作、安全与验证规则；
- `.ai/ACTIVE_TASK.md`：唯一活动任务指针；
- `.ai/AGENT_LOG.md`：只追加执行证据；
- `openspec/specs/`：accepted 长期契约；
- `docs/roadmap/iteration-blueprint.md`：v6 长期方向，不替代 active change。
