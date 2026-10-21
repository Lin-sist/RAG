# Enterprise RAG QA System｜项目上下文

## 1. 项目身份与当前阶段（2026-10-21）

- **定位**：可运行、可评测的模块化企业知识库 RAG 问答工程原型（尚未达到生产级多租户 SLA 或完整 Agentic RAG 平台）。
- **主链路**：数据库认证与服务端租户身份 → 知识库与持久化异步索引（MySQL task ledger + Redis 投影）→ 混合检索（Dense + BM25 + RRF）→ 可归因 Reranker → 有界路由（默认关闭的 `fact-v1`）→ LLM 生成（同步 + 结构化 SSE `structured-v1`）→ 历史与反馈。
- **阶段坐标**：基础基线（C1–C16）、Wave 1（C17–C21）及前端 Demo（R4/R5/F1–F6）均已按各自接受范围归档，`.ai/ACTIVE_TASK.md = IDLE`。

## 2. 技术栈与核心默认配置

- **后端**：Java 17 + Spring Boot 3.2.1 Maven 多模块（`rag-common`、`rag-auth`、`rag-core`、`rag-admin`），MySQL + Redis + Milvus（默认向量库；Qdrant/ES adapter 在租户强制模式下 fail startup）。
- **前端**：Vue 3 + TypeScript 5.7 + Vite + Element Plus（已接入 R5 结构化流式终态、知识库上传、历史反馈与双翼登录动效）。
- **检索与分块**：默认分块 `420/80`；默认检索 `Dense + BM25 + RRF`；默认 reranker 为 `heuristic`（已具备默认关闭的 NVIDIA hosted reranker adapter 与 fallback 归因）。
- **评测体系**：Python 离线/在线评测与门禁工具链（`scripts/`，336 项单测 GREEN）；默认开发评测集为版本化 150 条 `rag-eval-dev-v2`（兼容 30 条 `v1`）。

## 3. 已接受能力与当前边界矩阵

| 领域 | 已确认能力（Confirmed） | 当前边界与未闭环债务（Partial / Default-Off / Planned） |
| :--- | :--- | :--- |
| **认证与多租户 (C1, C13–C14)** | 数据库认证、服务端不可变 `RequestIdentity(userId, tenantId)`、26-case 合成双租户隔离矩阵 PASS | 仅证合成矩阵与 Milvus 路径；开发态 JWT fallback、真实 Milvus shadow 切换与第二业务租户见 [技术债](../docs/roadmap/technical-debt.md) |
| **持久化索引与故障语义 (C3–C5)** | 确定性 chunk/vector ID、MySQL task ledger、心跳恢复与事务幂等 finalize；LLM/Redis/Milvus 显式故障降级 | 进程中断与长时 outage 恢复演练仍需持续进行；provider auto resume 默认关闭 |
| **评测与质量门禁 (C6–C10, C17–C20)** | **C17 Retrieval Gate 已 `ACTIVE`**（Recall@5=47.44% 防退化）；C18 单轮 full150 生成基线、C19 校准工具链、C20 DRAFT profile 与三轮 intake 编译器已归档 | C19 完整 24×3 Judge Live、C20 三轮独立 full150 参考证据及阈值激活仍为未闭环 P1 债务（`W1-E1~E3`）；当前源码相对旧 C18 冻结审计存在预期 `budget_source_drift` |
| **可观测性 (C11–C12)** | 默认关闭的进程内 GenAI tracing core、单机 OTel Collector/Tempo/Prometheus/Grafana 参考栈 | 全仓并发下 `GenAiTracingConfigurationTest` 偶发时序抖动（W0 债务）；生产 HA/容量/SLA 未验证 |
| **只读 MCP 与有界路由 (C15–C16, C21)** | 默认关闭的本机 sessionless `/mcp`（3 Resources / 4 Tools）；默认关闭的 `fact-v1` 路由与 `evidence-no-answer-v1`；SSE `structured-v1` 终态契约 | MCP 2026-07-28 迁移与远程 OAuth 未开始；Wave 2 高级路由（`multi-hop / compare / temporal / global / high-risk`）尚未启动 |

## 4. 长期规格与参考入口

- **Baseline Specs**：[`rag-system`](specs/rag-system/spec.md)、[`evaluation`](specs/evaluation/spec.md)、[`agent-collaboration`](specs/agent-collaboration/spec.md)
- **协作与避坑**：[`AGENTS.md`](../AGENTS.md)、[`.ai/ACTIVE_TASK.md`](../.ai/ACTIVE_TASK.md)、[`.ai/GOTCHAS.md`](../.ai/GOTCHAS.md)、[`.ai/AGENT_LOG.md`](../.ai/AGENT_LOG.md)
- **架构与路线**：[`docs/architecture/overview.md`](../docs/architecture/overview.md)、[`docs/roadmap/iteration-blueprint.md`](../docs/roadmap/iteration-blueprint.md)、[`docs/roadmap/technical-debt.md`](../docs/roadmap/technical-debt.md)、[`docs/eval/RAG_EVAL_GUIDE.md`](../docs/eval/RAG_EVAL_GUIDE.md)
