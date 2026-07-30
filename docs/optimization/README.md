# RAG 优化文档索引

> 状态日期：2026-07-30

本目录只保留三类内容：当前 v3 证据、已关闭的 v4 阶段证据、历史演进证据。阶段编号只在所属版本目录内解释。

## 当前真相源

### v3：检索质量工程

- `v3/summary.md`：v3 总结与指标入口。
- `v3/stage1-reproducible-eval.md`：固定评测 KB 与可复现 retrieval baseline。
- `v3/stage2-chunking.md`：分块矩阵与 `420/80` 决策。
- `v3/stage3-rerank-adapter.md`：HTTP model reranker adapter、健康检查与降级。

### v4：生成与引用质量（部分完成并关闭）

- `v4/plan.md`：已关闭的 v4 计划快照；Stage 3 已转入技术债 P1，不再从该文件继续执行。
- `v4/stage1-generation-citation.md`：两轮 CLEAN generation/citation/no-answer baseline。
- `v4/stage2-rerank-decision.md`：缺真实 provider 时的正式跳过结论。

### C7：当前 reranker A/B evidence

- `../eval/reports/c7-reranker-ab-full-2026-07-20.md`：固定 30 条开发样本、`R=3,W=3` 的 clean heuristic/NVIDIA 比较、provider coverage、质量 delta 与延迟边界；用户已验收，delta 已接受并归档。

### C8a：评测数据版本治理（已验收归档）

- `../eval/dataset-manifest.json`：首个 `rag-eval-dev-v1` release identity，固定 question/schema/annotation/corpus version 与当前 artifact hash。
- `../eval/schema/rag-eval-sample-v1.json`：allowed/required fields、类型、enum、ID pattern 与 answerable/no-answer 条件契约。
- `../eval/RAG_EVAL_GUIDE.md`：formal/UNVERSIONED 边界、零外调 plan、version bump matrix 与 drift recovery。4 个 requirements / 13 个 scenarios 已接受进长期 `evaluation` baseline，change 已归档。

### C8b：评测数据扩充与标注（已验收归档）

- `../eval/releases/rag-eval-dev-v2-manifest.json`：显式 v2 release，固定 150 条 exact quota、v1 seed identity、三份 fixture coverage、grounding/duplicate facts 和 review identity。
- `../eval/releases/rag-eval-dev-v2.jsonl`：前 30 条保持 v1 raw/object/order identity，追加 120 条 source-first 开发样本；默认 manifest 已切换为与显式 v2 manifest byte-identical。
- `../eval/review/rag-eval-dev-v2-review.jsonl`：150 条结构、grounding、duplicate 与语义复核事实，不复制完整 question、answer 或 fixture 正文。
- C8b 的 4 requirements / 12 scenarios 已接受进长期 baseline并归档；这不代表 C9/C10/C14 或任何质量收益完成。

### C9a：客观 Claim-Evidence 指标（已验收归档）

- archived change：`archive/2026-07-23-claim-evidence-objective-metrics`。
- direct runner 已新增固定 `claim-lexical-v1`：句子/列表 claim、provenance-valid returned citations、exact + `0.70` claim-token coverage、逐 claim attribution 与全 claim 分母。
- direct/reproducible plan、run metadata、Markdown 和 details JSON 固定 splitter/tokenizer/threshold/evidence policy identity；identity 漂移会在 backend/provider 调用前失败。
- 4 个 requirements / 12 个 scenarios 已接受进长期 `evaluation` baseline；当前只有合成离线测试和 plan-only 证据，真实 generation/provider 调用量为 0，仍不宣称 faithfulness、C9b judge calibration 或 C10 quality gate 完成。

### C9b：Judge 校准与状态语义（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-23-judge-calibration-and-status-semantics/`。
- `../eval/calibration/judge-calibration-v1-manifest.json` 固定 24 条 faithful×relevant 四象限人工 gold case，各象限 6 条；context 只解析 tracked fixture exact excerpt。
- normal/calibration runner 共用 `rag-judge-v1` prompt、strict score parser、score-derived pass 与脱敏 contract identity；normal report/details/console 分离 objective、judge、global status 和 comparison safety。
- canary 固定 4×1，full 固定 24×3；runner 无自动 retry，并要求显式 `--execute-live-judge`、本地 raw details 与 `--no-overwrite`。
- 4 requirements / 12 scenarios 已接受进长期 `evaluation` baseline；live calibration 未授权并按 `SKIPPED` 收口，真实 judge/provider 调用与数据出站为 0。尚无 agreement evidence，不能宣称 judge 已真实校准、默认开启或 C10 gate 已建立。

### C10：质量阈值门禁（离线框架已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-23-eval-quality-threshold-gates/`。
- `../eval/schema/rag-quality-gate-profile-v1.json` 与独立 evaluator 固定 profile/dataset/run/channel identity、`all/type/difficulty/answerability` 切片、hard/reference AND、minimum denominator、缺失/错误 fail-closed 语义以及 `0/3/4/2` 退出码。
- 首个 `rag-eval-dev-v2-retrieval-regression-v1` profile 保持 `DRAFT / PENDING_REFERENCE_EVIDENCE`，12 个 target 均未填写；reference calls 未授权并按 `SKIPPED` 收口，实际 backend/provider 调用和数据出站为 0。
- 4 requirements / 12 scenarios 已接受进长期 `evaluation` baseline；本次只确认 offline gate framework，不确认 ACTIVE quality gate、retrieval/generation/citation/judge 质量达标或 production readiness。

### C11：GenAI Tracing Core（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-23-genai-tracing-core/`。
- OTel 1.31 API/SDK 由 Spring Boot 3.2.1 BOM 管理，runtime 默认关闭且 fail-open；durable ingest 与 ask 使用分离 trace，并通过稳定 `ingestTaskId/documentId/chunkId` lineage 关联。
- 固定阶段 topology、W3C/custom context、MDC bridge、同步/流式终态、bounded lineage events 与隐私白名单已用 in-memory exporter/fake dependencies 验证。
- 4 requirements / 12 scenarios 已接受进长期 `rag-system` baseline；C11 不含 network exporter、metrics、alerts、dashboard、生产 sampling、retention、权限或部署，真实 provider/exporter 调用和数据出站为 0。

### C12：OTel Export And Metrics（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-26-otel-export-and-metrics/`。
- tracing/metrics/export 三个开关独立且默认关闭；OTLP gRPC exporter 使用有界 queue/batch/timeout 并保持业务 fail-open，低基数 operation/stage/provider/fallback/token metrics 独立于 trace sampling。
- 本机 reference stack 固定 Collector Contrib `0.157.0`、Tempo `2.10.7`、Prometheus `3.13.1`、Grafana OSS `13.1.1`；关键 trace 全保留、普通成功 trace 默认 10% tail sampling、metrics 不采样，retention 为 72h/7d，宿主只开放 localhost OTLP/Grafana 且 Grafana 禁止 anonymous。
- 4 requirements / 12 scenarios 已接受进长期 `rag-system` baseline；synthetic smoke 验证本机查询、采样、隐私、访问与恢复闭环，但不证明生产 HA、容量、合规 retention、租户权限、跨主机传输、通知或 SLA。

### C13a：Tenant Model, Context And Migration（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-26-tenant-model-context-and-migration/`。
- V10 创建唯一 `legacy-default` tenant，并按 nullable→全量回填→NOT NULL/index 顺序为既有 user 与 knowledge-base 建立持久化 tenant identity，保留主键、owner/public、逻辑删除状态与 `kb_permission` 关系。
- 数据库认证、access/refresh JWT、refresh reload 与 immutable `RequestIdentity(userId, tenantId)` 统一使用服务端 tenant 事实；旧无 tenant claim token fail closed，客户端 header/query/body/metadata 不能选择或覆盖 tenant。
- 4 requirements / 12 scenarios 已接受进长期 `rag-system` baseline；C13a 仍只证明 tenant model/context readiness。后续 C13b data-plane enforcement 已验收归档，但 C14 评测通过前仍不开放第二业务 tenant、C15/C16，也不宣称租户隔离成立。

### C13b：Tenant Data-Plane Enforcement（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-27-tenant-data-plane-enforcement/`。
- 已实现 SQL/API/permission、task/cache/history、RAG/keyword、Milvus tenant adapter contract，以及默认关闭的 shadow collection/readiness 维护路径；本地 Milvus 2.3.4 仅使用合成数据验证，真实模型调用为 0。
- 6 requirements / 18 scenarios 已接受进长期 `rag-system` baseline。真实 Milvus shadow copy/mapping switch 未获授权并 `SKIPPED`；全仓 Maven 唯一命中既有 OTel collector 时序波动且独立复跑通过，因此不记录为全仓 GREEN。C14 前不宣称租户隔离成立，不开放第二业务 tenant、tenant management、C15 或 C16。

### C14：Tenant Isolation Adversarial Evaluation（已验收归档）

- 固定 release `tenant-isolation-adversarial-v1` 在 Git HEAD `dc9e3e6` 上完成 26/26 required cases，functional/content/error/timing 四通道与 global report 均为 `PASS`；正式 summary/details/evidence 与 requirement traceability 位于 `../eval/reports/` 和 `../eval/isolation/`。
- 只使用自有 Testcontainers 与 deterministic test stub，provider/model calls=0、businessDataOutbound=false、真实 Milvus maintenance=`SKIPPED`。结论只适用于 Milvus 支持配置和固定 synthetic attack matrix，不外推 Qdrant/Elasticsearch、生产拓扑、真实迁移或所有 timing side-channel。
- archived change：`../../openspec/changes/archive/2026-07-27-tenant-isolation-adversarial-evaluation/`。`evaluation` 5 requirements / 15 scenarios 与 `rag-system` 2 requirements / 6 scenarios 已接受进 baseline；生产第二业务 tenant、tenant management、C15/C16 仍需独立 Type C change。

### C15：MCP Read-Only Service（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-30-mcp-readonly-service/`。已实现默认关闭、本机优先、sessionless Streamable HTTP `/mcp`，复用现有 JWT 与 server-derived tenant identity，提供三种 bounded Resource 和四个固定只读 Tool；search/ask 与 QA cache 均有独立 default-off 开关。
- 固定 MCP Java SDK `2.0.0`、spec `2025-11-25`、Tool schema SHA-256 和 conformance `0.1.15`；独立官方 Java SDK client、适用的五个 conformance generic scenarios、双 tenant MySQL/Redis/Milvus/MinIO synthetic integration 均通过。权威状态摘要前后一致，real provider/model calls=0、businessDataOutbound=false、真实 Milvus maintenance=`SKIPPED`。
- Git HEAD `45959672` 的 clean-HEAD driver evidence 已复跑，`workingTreeDirty=false`、权威状态摘要前后一致；7 requirements / 26 scenarios 已接受进 `rag-system` baseline并归档。该证据不外推 MCP OAuth、远程生产部署、真实 provider、Qdrant/Elasticsearch、生产第二业务 tenant、Router 或 Agentic RAG。

### C16：Bounded Query Router（实现完成，等待最终验收）

- active change：`../../openspec/changes/bounded-query-router/`。已实现 default-off `fact-intent-v1`、closed-world `fact-v1` executor、`evidence-no-answer-v1`、跨 stage budget/usage、sync/SSE/MCP read-only attribution 与版本化 cache identity。
- `../eval/router/bounded-query-router-eval-v1-manifest.json` 固定 v2 dataset identity、20 条 ID-only expectation sidecar、budget profile、七通道与四状态退出码；validator/evaluator 均为 Python 标准库、本地 fail-fast、formal no-overwrite。
- deterministic evidence 为 provider/model calls=0、businessDataOutbound=false，live router ask/eval 未授权并 `SKIPPED`。当前 delta 尚未接受进 baseline，change 未归档，生产默认仍关闭；不得外推 multi-hop/global/high-risk、真实 provider 质量、生产 SLA 或 Agentic RAG。

## 历史材料

`history/` 保存 v3 正式计划形成前的 hybrid、reranker abstraction 和 token chunker 演进记录。它们可以解释代码为何形成当前结构，但不得单独用于判断当前阶段、指标或待办。

## 使用规则

1. 先读当前代码，再读本索引。
2. 指标结论必须同时核对报告状态、error count、metadata 和 Git HEAD。
3. 新优化不得继续在本目录新增无版本号的 `stage1.md / stage2.md`。
4. 未完成的新 change 以 OpenSpec 为执行源；本目录只保存阶段完成后的长期结论。
