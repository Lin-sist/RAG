# Proposal: C6 NVIDIA Reranker Adapter And Attribution

## Why

当前仓库已有 `Reranker` 抽象、默认 heuristic、通用 HTTP `ModelReranker` 和失败后 heuristic fallback，但现有 adapter 使用自建的 `query + documents` 请求和 `results[].relevance_score/score` 响应，只由本地 fake server 单测验证。NVIDIA NeMo Retriever Reranking NIM 当前公开的 OpenAI HTTP 契约是 `POST /v1/ranking`，请求为 `query: {text}`、`passages: [{text}]` 与可选 `truncate`，响应为 `rankings[].index + logit`；现有 adapter 不能直接声称已适配该协议。

同时，`RerankerRegistry` 在 provider 未配置、健康检查失败或调用异常时会回退 heuristic，但归因主要停留在日志。成功 model rerank 只在部分 context metadata 写 `rerankProvider=model`；debug retrieval 使用 `queryEngine.retrieve(...)`，会丢失 `RetrievalResult.diagnostics`。因此现有评测报告即使写着 `enableRerank=true`，也无法逐样本证明真正请求了哪个 provider、实际由哪个 provider 排序、fallback 几次、调用覆盖了多少样本，更不能作为 C7 model A/B 的干净输入。

## Readiness Verdict

`GO`，允许进入 C6 规划阶段：

- C5 recovery debt closeout 已由提交 `666dd9b` 实现、提交 `4fe45c0` 验收归档；delta 已接受进 `rag-system` baseline。
- `.ai/ACTIVE_TASK.md` 在启动前为 `IDLE`，工作区干净，没有其他 active change。
- `docs/roadmap/technical-debt.md` 已明确 C5 登记实现债务清零，C6 不再受前置 P0 债务阻断。
- 默认 heuristic、通用 model adapter、fallback、`RetrievalResult.diagnostics`、同步 QA metadata 与可复现 runner 已存在，C6 可在这些边界上增量实现。
- 当前阻断项是待批准的 provider/diagnostics 契约，不阻止创建 proposal；未获事前闸门批准前不得修改生产实现。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，评测命令明明开着 rerank，但 provider 没配好或请求失败时系统会悄悄改用 heuristic；报告仍然只有 `enableRerank=true`，维护者无法判断所谓 model 组到底有多少样本真的经过 NVIDIA。改之后，每个样本都会明确记录“请求了谁、最后谁生效、是否 fallback、为什么、真实调用几次、覆盖多少候选、耗时多久”；只有真实有效覆盖的样本才能在下一阶段进入 model A/B，fallback 不再被包装成模型收益。

## Current Status

- `confirmed`：`retrieval.rerank.provider` 默认是 `heuristic`，model adapter 默认关闭，不会因正常启动产生外部 rerank 调用。
- `confirmed`：现有 `ModelReranker` 发送 `model/query/documents`，解析 `results[].index` 与 `relevance_score/score`，并非 NVIDIA `/v1/ranking` typed contract。
- `confirmed`：`RerankerRegistry` 在 requested provider unavailable 或调用异常时回退 heuristic，但只在日志记录 provider/error type。
- `confirmed`：model 成功时 context metadata 含 `originalRelevanceScore`、`rerankScore`、`rerankProvider=model`；fallback 后没有样本级 requested/effective/fallback diagnostics。
- `confirmed`：`RetrievalResult.diagnostics` 已承载 Milvus keyword-only 降级事实，同步 QA 会把它复制到 response metadata。
- `confirmed`：debug retrieval 当前调用 `queryEngine.retrieve(...)`，响应没有 request-level diagnostics；retrieval-only runner 因此无法可靠归因 reranker。
- `confirmed`：评测 runner 会保存 `debugRetrieveRawResponse` 与 `askRawResponse`，但当前不提取 reranker coverage/fallback 聚合。
- `partial`：既有通用 model adapter、健康检查、timeout 与 fallback 可复用设计经验，但协议、provider identity 和 logit 语义不满足 NVIDIA C6 契约。
- `planned`：新增显式 `nvidia` provider、typed ranking client、typed rerank outcome、稳定 fallback taxonomy、debug/QA attribution 与 runner 聚合。
- `out_of_scope`：真实收益 A/B、默认 provider 切换、批量真实调用、SSE 结构化事件、数据库/前端/索引恢复改动、跨 provider model failover。
- `unknown`：最终使用的 NVIDIA deployment/base URL、model、凭据可用性及是否批准一次纯合成 live smoke，等待事前闸门或 closeout 前确认。

## Scope

- 保留现有 `model` adapter 兼容边界，新增 provider id 为 `nvidia` 的独立 typed adapter；默认仍为 `heuristic`。
- 配置化 NVIDIA base URL、`/v1/ranking` endpoint、API key、model、truncate、timeout 和可选 readiness health path；tracked config 不包含真实凭据。
- 按 NVIDIA ranking schema 发送 `query.text` 与 `passages[].text`，解析 `rankings[].index/logit`；不把 logit 伪装成校准概率。
- 建立 typed rerank outcome，使 registry 一次返回 contexts 与 sanitized diagnostics，而不是通过日志或 ThreadLocal 旁路推断。
- 每次 rerank 记录稳定字段：requested/effective provider、fallback count/reason、model call count、candidate/scored count、coverage、latency、model 与 protocol。
- provider 返回空、重复、越界或未完整覆盖候选的 ranking 时，整次样本回退 heuristic，不混合“部分 model + 部分 heuristic”排序。
- 将 diagnostics 与现有 retrieval degradation facts 合并进 `RetrievalResult.diagnostics`；同步 QA metadata 与 debug retrieval 均可观察。
- 扩展 Python runner：逐样本保存 reranker attribution，并聚合 effective provider coverage、fallback count/reason 与 model call count；不改变既有 Recall/MRR/Top1、report status 或 generation/citation 指标语义。
- 使用本地 fake HTTP server 和合成文本验证成功、未配置、health unavailable、timeout、HTTP failure、malformed/partial response、fallback 与脱敏。
- 预留最多 1 次真实 NVIDIA ranking smoke 的独立授权闸门：只使用 1 个合成 query 与 3 个合成 passages，不发送知识库、用户问题、文档或凭据到报告。

## Non-goals

- 不删除或静默改写既有 `model` provider，不把其自建协议重新命名成 NVIDIA。
- 不修改默认 `heuristic`，不自动复用 LLM/embedding 的 provider 状态来宣称 rerank 已配置。
- 不在 C6 对 heuristic 与 NVIDIA 的 Recall@5、MRR、Top1、P50/P95 做收益判断；C7 才进行固定 KB/fixture/config/Git HEAD 的 A/B。
- 不为了评测集定制 query、chunk、topN/topK、fallback 或拒答规则。
- 不引入新 HTTP SDK、重试库、observability SDK 或其他依赖；沿用 Spring `WebClient` 与现有测试工具。
- 不新增数据库表、migration、公开强制 provider API、管理 UI 或前端页面。
- 不改 SSE 文本流的结构化完成事件；该能力仍属于独立技术债/后续 change。
- 不进行批量 rerank、ask、judge、embedding、LLM 或其他业务外部调用。

## Spec Delta Decision

C6 修改 reranker provider identity、真实 NVIDIA 协议、fallback 可观察语义以及 debug/QA/eval 的逐样本归因，属于长期用户可观察契约，必须提供 `rag-system` spec delta。C6 不修改 retrieval/generation/citation/no-answer/judge 指标口径，因此本 change 不新增 `evaluation` spec delta；runner 只暴露归因证据，为 C7 的可比较性规则准备输入。

## External Calls And Authorization

| 调用类型 | 规划阶段 | 离线实现/测试 | 可选 live smoke | 数据出站 | 费用/限流 | 授权状态 |
|---|---:|---:|---:|---|---|---|
| rerank | 0 | 0 真实调用；仅 `127.0.0.1` fake server | 最多 1 次 ranking 请求，1 个合成 query + 3 个合成 passages | 仅纯合成英文短句；不发送项目/用户数据 | 取决于届时 provider/model/账户条款；调用前重新披露 | 未授权；需用户单独批准 |
| embedding | 0 | 0 | 0 | 无 | 0 | 不适用 |
| ask/LLM | 0 | 0 | 0 | 无 | 0 | 不适用 |
| judge | 0 | 0 | 0 | 无 | 0 | 不适用 |

规划与离线实现测试不得读取 `.env.local` 中的凭据或调用真实 provider。若用户不批准 live smoke，C6 closeout 必须明确写成“official-schema + local contract tested，真实 endpoint 未验证”，不得宣称真实 NVIDIA 调用成功；若批准，则在执行前记录 provider、model、endpoint path、timeout、预计 1 次调用、合成出站内容、限流和费用依据。

## Acceptance Evidence

- 默认配置启动时 effective provider 为 heuristic、model call count 为 0，且不访问 NVIDIA。
- `provider=nvidia` 且配置完整时，请求精确符合 `query.text + passages[].text + truncate`，响应按 `rankings[].index/logit` 重排。
- NVIDIA 成功结果只用于排序；原 retrieval score 保留，raw logit 与 rerank rank 进入 metadata，不把 logit 表述成概率。
- 未配置、health unavailable、timeout、HTTP 4xx/5xx、malformed、空、重复、越界或不完整 rankings 均产生稳定 fallback reason，并整次使用 heuristic。
- 每次 debug retrieve 与同步 QA 都能观察 requested/effective provider、fallback/model-call/candidate/scored/coverage/latency；不包含 API key、Authorization、query、passages、raw body 或异常 message。
- retrieval-only runner 的每个样本保存 attribution，报告聚合 effective provider coverage 与 fallback；既有 Recall/MRR/Top1 和 report status 计算保持不变。
- focused Java、Python runner tests、完整 Maven、SensitiveLogs 与 `git diff --check` 通过；前端无改动时可明确跳过 build。
- live smoke 只有在用户批准后才执行；无批准时真实 provider 调用量保持 0。

## Risks

- NVIDIA 自托管 NIM 与托管 endpoint 的 health/readiness 能力可能不同；强制 health probe 会把可调用 provider 误判为 unavailable。
- `logit` 是未归一化原始分数，若直接覆盖 `relevanceScore` 或做未经校准的 sigmoid，会制造错误的跨模型/跨样本可比性。
- debug retrieval 是 runner 的 retrieval-only 事实源；若只改同步 QA metadata，C7 的 model 组仍无法证明逐样本有效覆盖。
- fallback diagnostics 若携带 raw exception/body/query/passages，可能泄露 provider 或用户数据；必须只暴露固定枚举与计数。
- 局部 rankings 若与 heuristic 混合会让 effective provider 无法单值归因；首版应整次 fallback，代价是 provider 偶发残缺时模型结果完全不采用。
- 新增 `nvidia` provider 会增加配置面；必须保持 `model` 兼容、默认 heuristic 与 tracked secret 为空。
- 本地 schema contract test 不能替代真实 endpoint smoke；若 live smoke 延期，closeout 结论必须保持该限制。

## Approval Gate

当前仅获准创建规划草案。进入生产实现前，用户需要确认：

1. proposal 的 scope/non-goals 与 C6/C7 边界；
2. design 中 12 条决策记录；
3. `rag-system` spec delta 的 4 个 requirements / 11 个 scenarios；
4. 提交责任继续为 `用户手动提交`；
5. live smoke 是授权最多 1 次纯合成调用，还是明确延期并保留 protocol-tested 边界。

## Commit Responsibility

`用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布。
