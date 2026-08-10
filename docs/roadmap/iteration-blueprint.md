# Enterprise RAG QA System｜迭代蓝图（Iteration Blueprint）· v6（已冻结）

> 文档性质：长期迭代的方向母文档，不是可执行的 OpenSpec change，也不改变当前能力声明。
> 状态日期：2026-08-10
> 审批状态：用户已一次性批准 D01–D36，并授权本版蓝图落盘。
> 当前起点：C1–C16 已完成范围验收、baseline 接受与归档；`.ai/ACTIVE_TASK.md=IDLE`。

## 0. 执行与事实边界

1. 本文件只规定方向、顺序和长期边界；`AGENTS.md`、`.ai/ACTIVE_TASK.md`、active OpenSpec change、accepted specs 和当前代码仍按既有优先级构成事实源。
2. v6 的长期目标不改变项目当前定位：在真实生产证据形成前，项目仍是“可运行、可评测的模块化 RAG 工程原型”，不得包装成生产级多租户系统或已完成的 Agentic RAG 平台。
3. 每个 Type C change 正式启动前，必须独立完成 readiness，并建立 proposal、design、tasks 和 spec delta；一次只允许一个 active change。
4. 蓝图获批不等于实现授权、profile 数值批准、外部调用授权、baseline acceptance、归档、push、PR、部署或发布授权。
5. 批量 embedding、rerank、ask、generation、judge 或其他 provider 调用，必须在每个执行闸门重新披露固定身份、最大调用量、数据出站、费用或零费用依据、限流、timeout/retry 和 raw artifact 策略，并单独获得用户授权。
6. 所有阶段都必须区分 `confirmed / partial / planned / out_of_scope / unknown`，并把 offline、synthetic、local、live、production evidence 分层表述。
7. 时间窗口用于排序而非进度承诺；若前置 evidence 不成立，后续阶段不得以日期压力绕过门禁。

## 1. 最终产品定义

项目长期定位为：

**企业证据型知识运行时（Enterprise Evidence & Knowledge Runtime，EKR）**。

它不是单一聊天页面，而是同一个可信知识核心的三个产品表面：

1. **面向员工的可信知识工作台**：快速问答、引用回看、来源比较、时序判断与深度研究。
2. **面向业务系统和其他 Agent 的权限感知知识服务**：通过 REST、SSE、MCP，后期再通过 A2A 提供有界能力。
3. **面向治理者的评测与运行控制面**：管理数据集、策略、预算、质量门禁、trace、风险、审批和发布状态。

核心产品承诺是：对每个重要结论都能回答“依据是什么、来自哪里、是否过期、当前用户是否有权查看、为什么回答或拒答”。

服务对象的优先级固定为：企业员工可信问答 → 业务系统/其他 Agent 的知识服务 → 自动执行型 Agent。不得为了展示 Agent 概念而牺牲第一层的确定性和可信度。

## 2. 北极星指标与证据原则

### 2.1 北极星指标

- evidence-backed answer rate；
- validated citation / claim support rate；
- no-answer correctness 与 unsupported leakage；
- durable research task completion / cancel / recovery rate；
- human takeover / approval / rejection rate；
- latency、token、provider call 与预算遵守率；
- identity、tenant、policy 与 evidence completeness。

聊天次数、生成字数、Agent 步数和“自动化程度”不作为核心成功指标。

### 2.2 不可混合的指标通道

- retrieval；
- generation；
- citation / objective claim support；
- no-answer；
- judge；
- task outcome；
- security / tenant isolation；
- latency / cost / budget。

任何一个通道的 PASS 不得替代另一个通道；`PARTIAL`、`RETRIEVAL_ONLY`、`SKIPPED`、synthetic/local evidence 不得改写为干净业务结论。

### 2.3 门禁方法

- 只使用版本化、固定身份且完整的 evidence；
- hard floor 与 reference regression tolerance 并存时必须同时通过；
- 缺失、错误、身份漂移、分母不足或 required channel 不完整时 fail closed 为 `NOT_EVALUABLE` 或 `INVALID`；
- 不从成功子集推断结论；
- 不自动学习阈值，也不为了让门禁通过而修改评测题目、标注、fixture、指标公式或生产行为；
- 数值阈值只在真实 evidence 产生后由用户审阅批准。

## 3. 当前起点：C1–C16 已完成基础

截至 2026-08-10，以下基础已经按各自批准范围进入 accepted baseline：

- 数据库认证、凭据治理、真实依赖 happy path、LLM/Redis/Milvus 故障语义；
- durable index input、task ledger、恢复协调与幂等 finalize；
- reranker provider attribution，以及固定 30 条开发样本上的 clean NVIDIA/heuristic A/B；
- 150 条 `rag-eval-dev-v2` 数据治理、objective claim alignment、judge contract 和离线 quality gate contract；
- GenAI tracing core、本机 OTel reference export/metrics；
- tenant identity、跨数据面 enforcement 与固定 26-case synthetic isolation matrix；
- default-off/local-only 的 C15 read-only MCP；
- default-off、closed-world、仅 `fact-v1` 的 C16 bounded Router 与 `evidence-no-answer-v1`。

这些完成事实不证明以下能力：

- 真实 v2/150 reference quality gate 已激活；
- 真实 generation/citation/no-answer 与 live judge 已形成可信结论；
- Router 已支持 multi-hop/global/high-risk 或已默认开启；
- MCP 已迁移至 2026-07-28、具备远程 OAuth 或生产开放条件；
- 真实 Milvus shadow migration、第二业务 tenant、全 adapter 隔离或生产 SLA 已完成；
- durable research、tool loop、长期 memory、受控写操作、A2A 或 Agentic RAG 已完成。

## 4. 2026–2027 演进总图

严格主线为：

`W0 基线稳定 → W1 真实质量证据与门禁 → W2 有界策略扩展 → W3 权限感知知识源联邦 → W4 可恢复深度研究 → W5 受控行动与 Agent 互操作 → W6 生产化与规模验证`

建议窗口：

| 时间视角 | Wave | 目标结果 |
|---|---|---|
| 2026 Q3 | W0–W1 | 首个 ACTIVE retrieval gate、真实 objective/judge evidence 与结构化 SSE 终态 |
| 2026 Q4 | W2 | multi-hop、compare、temporal、global 等策略逐项形成独立 evidence |
| 2027 H1 | W3–W4 | 权限感知知识源联邦与可恢复深度研究任务 |
| 2027 H2 | W5–W6 | 受控行动、A2A 互操作和真实生产化验证 |

## 5. W0：基线稳定

### 5.1 OTel 测试时序债务

- 目标：稳定 `GenAiTracingConfigurationTest` 中 unavailable collector 的全仓并发时序断言。
- 边界：独立复跑通过不能把全仓失败改写为 GREEN；不得借维护切片改变 tracing/exporter 契约。
- 分级：readiness 若确认只是既有契约内测试稳定性修复，可按 Type B；若需要改变运行语义，升级 Type C。

### 5.2 Provider 与 evidence readiness

- 固定 Git HEAD、dataset、KB/fixture、provider/model、endpoint、temperature、timeout/retry、runtime config 与 raw artifact 策略。
- 先运行本地 validator、plan-only 和 preflight；未授权前 provider calls=0、business data outbound=false。
- preflight 只证明环境与身份准备，不构成 quality evidence。

## 6. W1：真实质量证据与门禁

### C17. `retrieval-quality-gate-activation`

**目标**：把首个 `rag-eval-dev-v2` retrieval-only profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进为有完整 evidence、具体阈值和用户批准的 `ACTIVE` profile。

**固定正式 evidence**：

- `rag-eval-dev-v2` full 150 条；
- 3 个 fixed-identity measured repeats；
- `topK=5`、`minScore=0.3`、rerank enabled 且默认 heuristic；
- 最多 450 次 debug retrieval、最多 450 次 query embedding；
- heuristic 条件下 external rerank、ask、generation、judge 为 0；
- 三次 run 的 sample、KB/fixture、Git/config、provider attribution 或 repeat identity 任一漂移即不可比较。

小规模 canary 只用于发现环境错误，不能替代正式 evidence。完整 reference 产生后才能提出 hard floors 和 regression tolerances；具体数值仍需用户在 C17 中间闸门审阅，未批准前 profile 保持 DRAFT。

**非目标**：不切换默认 reranker，不宣称 generation/citation/no-answer/judge 已达标，不使用生产数据，不改变指标公式。

### C18. `generation-objective-evidence-baseline`

**目标**：在 judge 关闭的条件下，为 v2 建立真实 generation、citation、objective claim support 与 no-answer 基线，并为未来独立 objective profile 固定 evidence identity。

**首轮正式边界**：

- v2 full 150 条，先执行 1 个完整 baseline run；
- 最多 150 次 debug retrieval、150 次 ask、300 次 query embedding、150 次 generation；
- judge calls=0；
- 只使用 tracked 开发问题和 fixture，不使用真实业务数据；
- retrieval、generation、citation、claim support、no-answer、error/retry 分开报告。

如果未来要建立 objective release gate，必须创建新的 profile/version，并用独立批准的重复 evidence 决定阈值；不得用一次 baseline 或 retrieval profile 冒充。

**非目标**：不根据失败题逐题修改 prompt、分块、路由、题目或标注；不把词法 claim alignment 表述为语义蕴含或完整事实正确性。

### C19. `judge-live-calibration-evidence`

**目标**：在既有 `rag-judge-v1` 与 `judge-calibration-v1` contract 上形成真实 judge agreement 与 repeat consistency evidence。

**固定调用形态**：

- canary：4 case × 1 repeat，最多 4 次 judge 调用；
- full：24 case × 3 repeats，最多 72 次 judge 调用；
- 固定 provider/model、temperature、rubric/prompt/parser/threshold、context truncation 与 endpoint identity；
- 记录 parse coverage、faithfulness/relevance/joint confusion、agreement、provider-pass mismatch 和 per-case repeat consistency；
- 任一预期 case/repeat 缺失时为 `PARTIAL / NOT_COMPARABLE`，不得从成功子集产生 agreement 结论。

**非目标**：不自动优化 score threshold，不在 calibration 通过前把 judge 设为硬发布门禁，不让 judge 质量值污染 objective completeness。

### C20. `objective-judge-quality-gate-profile`

**目标**：在 C18/C19 evidence 充分后，以新 profile/version 建立 objective 与 judge 的独立门禁。

- retrieval、objective、judge profile 保持独立；
- judge 可先作为 advisory rule，再经过独立证据和批准升级为 required rule；
- `PASS / FAIL / NOT_EVALUABLE / INVALID` 与既有稳定退出码保持兼容；
- 不因 judge 跳过或失败把完整 objective 通道降级为 retrieval-only；
- 不因门禁难以通过而修改 metric、dataset 或生产默认行为。

### C21. `structured-sse-terminal-contract`

**目标**：在保留文本 chunk 兼容性的同时，为 SSE 增加可验证的结构化 terminal event。

terminal event 至少表达 final state、reason、citations、必要 metadata、classifier/strategy/policy identity、budget outcome 与实际 usage。`ANSWER / NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED` 必须可区分；中断、取消、timeout 或 partial output 不得保存为正常成功历史。同步、SSE 与 MCP 对同一执行结果保持语义一致。

**非目标**：不借 SSE change 扩展 Router 策略、provider 选择、前端大改或 Agent task runtime。

## 7. W2：有界检索策略扩展

策略按以下顺序逐项独立推进：

1. `router-multihop` / `multi-hop-v1`；
2. `router-compare` / `compare-v1`；
3. `router-temporal` / `temporal-v1`；
4. `router-global` / `global-v1`；
5. `router-high-risk` / `high-risk-v1`。

每个策略必须拥有独立 classifier contract、route plan、预算、数据集、per-strategy metrics、unsupported leakage、failure/no-answer taxonomy、trace attribution 与启用门禁。不能可靠分类的请求继续 fail closed，不得回退 legacy 或伪装成已支持策略。

### 7.1 Multi-hop

只允许有限子问题、有限检索轮次、有限 generation 和固定 overall deadline；首版禁止开放式 reflection/self-loop。每个最终 claim 必须能回连到一个或多个可审核证据点。

### 7.2 Compare

输出共同点、差异、冲突和各自来源；不得把多个来源融合成无归属结论。来源权限、版本和时间不一致必须显式表达。

### 7.3 Temporal

引入文档时间、有效期、观察时点和“截至何时”的明确语义。缺少可靠时间事实时拒绝给出“最新”“当前有效”等结论。

### 7.4 Global 与 Graph

global 面向全库主题、趋势和汇总，可采用有界 map-reduce 或 GraphRAG 路径。GraphRAG 只作为特定 multi-hop/global 策略的可选知识结构，不替换现有 hybrid retrieval，也不得因使用图结构就宣称推理正确。

### 7.5 High-risk

高风险能力必须独立定义风险分类、人工确认、批准/拒绝/过期/取消状态和审计边界。它不是普通 retrieval 策略的自然升级，也不在 W2 中开放写 Tool。

## 8. W3：权限感知知识源联邦

建立版本化 Knowledge Source Registry，统一描述但不抹平下列来源：

- indexed documents；
- SQL / business API；
- Web / freshness-sensitive source；
- MCP Resource / Tool；
- Graph；
- table、image/OCR 与其他 multimodal artifact。

每个来源必须携带独立的 identity、tenant/permission、freshness/effective time、sensitivity、provenance、failure、cost 和 cache policy。tenant、collection、filter、provider、model、timeout 与 budget 必须由服务端 policy 推导，客户端和模型不得任意覆盖。

多模态顺序固定为：先支持可引用的表格、图片/OCR 和页面坐标证据，再评估音视频。多模态答案仍必须回连原始 artifact 与位置证据。

## 9. MCP 2026 迁移边界

在 W1 质量门禁完成后、任何远程 MCP 开放或 durable research task 对外暴露前，独立推进 MCP 2026-07-28 migration。

- C15 的 MCP `2025-11-25` conformance/client evidence 不直接复用为新协议验收；
- 显式评估 stateless core、Tasks、MRTR/input-required approval、trace context、authorization 和 compatibility；
- 先完成协议迁移、Tasks/approval/auth 边界，再考虑远程生产开放；
- read-only Tool 保持 default-off/local-only，写 Tool 继续不存在或 fail closed；
- remote TLS/proxy trust、OAuth Protected Resource Metadata、audience/resource binding、scope 和 rate limit 必须独立验证。

## 10. W4：可恢复深度研究

首个 Agent runtime 采用单个 durable investigation task，不采用 multi-agent swarm。

必须支持：

- versioned plan 与 bounded subqueries；
- checkpoint、resume、cancel、timeout 与 retry policy；
- 有界并发、总步骤、token、provider calls 和 deadline；
- claim-evidence graph、来源冲突和不确定性；
- 可保存、可引用、可复查的 report/artifact；
- human input required、approval 与 rejection；
- task outcome eval，而不只评估某一条最终文本。

长期 memory 只允许保存 tenant-scoped、可检查、可删除、有 TTL 的任务事实或用户明确保存的内容。全部聊天历史不得默认成为长期记忆。

## 11. W5：受控行动与 Agent 互操作

### 11.1 写操作

所有写 Tool 必须：

- default-off；
- least privilege；
- dry-run / preview；
- 人工批准与明确拒绝路径；
- idempotency key；
- 执行前后状态、结果和审计证据；
- timeout、cancel、compensation 或不可逆风险说明。

模型不得自行扩大 scope、切换 tenant、改变 provider/model/budget，或把读取授权推导成写入授权。

### 11.2 A2A

只有在单 Agent durable task 已成熟、可恢复、可取消、可评测之后才引入 A2A。A2A 用于独立 Agent 的发现、委派、任务和 artifact 交换，不替代内部领域模型、MCP 或核心 orchestration。

## 12. W6：生产化与规模验证

生产化必须由独立证据证明：

- 真实 Milvus shadow migration/readiness 切换与回滚；
- 第二业务 tenant 与真实 tenant lifecycle；
- Qdrant/Elasticsearch 等价隔离或在 enforcement mode 下继续 fail startup；
- MCP OAuth、TLS、proxy trust、Origin、audience/resource binding 和租户审计；
- HA、容量、费用、合规 retention、告警、备份恢复、跨主机传输和 SLA；
- production-like corpus、反馈闭环、drift monitoring 和 release rollback。

任何新 Router、MCP、Tasks、Graph、memory、高风险或写能力都保持 default-off；只有经过 shadow/canary、质量 gate、权限验证与回滚演练后才逐项开启。

## 13. 长期禁区

以下方向不进入本蓝图：

- 没有明确任务边界的通用 multi-agent swarm；
- 模型自行选择 tenant、provider、model、任意 SQL/filter 或无限预算；
- 用长上下文替代检索、权限、引用和 no-answer；
- 把 GraphRAG 应用于全部问题；
- 把全部聊天历史默认为长期记忆；
- 因 30/150 条开发集单次结果自动切换默认 provider；
- 把 synthetic/local PASS 包装成生产 SLA；
- 为评测集逐题定制 prompt、分块、路由或拒答；
- 让 Agent 框架侵入认证、索引、权限、citation 等领域核心；
- 在真实质量门禁、durable task 和受控行动证据形成前宣称 Agentic RAG 已完成。

## 14. 启动下一 change 的固定闸门

每个后续 change 启动前必须再次确认：

1. Git 与用户已有改动；
2. `.ai/ACTIVE_TASK.md=IDLE`；
3. 前置 evidence 与 capability claim；
4. `confirmed / partial / planned / out_of_scope / unknown`；
5. proposal 用户故事、design 决策记录、tasks 和 spec delta；
6. 外部调用是否存在及其单独授权；
7. 验证矩阵、跳过项与真实环境边界；
8. 提交责任；
9. baseline acceptance、archive、push、PR 与部署的独立授权。

本版冻结不自动启动 C17。下一次实施讨论应从 C17 readiness 和外部调用 plan-only 预算开始。
