# Enterprise RAG QA System｜项目上下文

## 项目身份

- 企业知识库 RAG 问答工程原型。
- 当前主链路：认证 → 知识库 → 文档异步索引 → 混合检索 → LLM 生成 → 引用校验 → 历史与反馈。
- 当前目标：先完成可信质量工程、生产化基础和个人掌握闭环，再增量进入 MCP 或 Agentic RAG。

## 当前代码事实

- 后端是 Java 17 + Spring Boot 3.2.1 Maven 多模块。
- 前端是 Vue 3 + TypeScript + Vite + Element Plus。
- 默认向量库是 Milvus；另有 Qdrant、Elasticsearch adapter。
- 默认检索是 dense vector + BM25 + RRF。
- 默认 reranker 仍是 heuristic；除既有通用 HTTP model adapter 外，C6 已实现默认关闭的 NVIDIA ranking adapter、整样本 heuristic fallback 与逐次 requested/effective provider 归因。C7 已在固定 30 条开发样本上完成 clean `R=3,W=3` A/B：NVIDIA Recall@5/MRR/Top1 为 76.47%/0.8241/100%，相对 heuristic 提升 7.84pp/0.0895/3.70pp；该 evidence 已获用户验收并接受进 `evaluation` baseline，不自动修改默认 provider。
- 默认分块为 `420/80`。
- 同步问答返回答案、contexts、citations、metadata；SSE 当前主要输出文本 chunk。
- 当前默认评测 release 是已验收的 150 条 `rag-eval-dev-v2`（30 条 immutable seed + 120 条新增），具备 quota、grounding、duplicate 与 review sidecar 门禁；30 条 v1 仍可显式复现。两者都不是生产数据集或隐藏 benchmark。
- `c3-integration` Maven/Failsafe 入口可用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope 确定性 embedding 重复验证登录、上传、索引、retrieval 与删除主链路。
- LLM、Redis 与默认 Milvus 的故障语义已分别被测试锁定；Milvus dense route 仅在关键词证据可用时显式降级，mutation outcome unknown、删除和统计均不伪造成功。
- 文档索引已具备 C5a durable input、MySQL durable task ledger 与 C5 恢复债务收口实现：新任务使用稳定 taskId、phase checkpoint 与 deterministic chunk/vector IDs；Redis 是可重建状态投影。legacy 无 ledger 只隔离不合成任务；协调器使用有界 concurrency、持续 heartbeat、DB-time backoff 与 attempt 终态；SQL finalize 以 document row lock 和单一事务保证 chunks/document count/task completion 幂等。provider auto resume 默认继续关闭；相关契约已接受进 `rag-system` baseline，change 已归档。
- C6 rerank diagnostics 已通过显式 outcome 合入 `RetrievalResult`，同步问答 metadata、debug retrieval 与 Python eval details/report 可区分 requested/effective provider、fallback taxonomy、model calls、候选覆盖与延迟；不记录 query/passages/raw body/凭据。
- C6 的 4 个 requirements / 11 个 scenarios 已接受进 `rag-system` baseline 并归档；归档后用户独立授权的 1 次纯合成 NVIDIA hosted rerank smoke 已通过，无重试且未使用知识库/用户数据。该结果只确认当前 endpoint/auth/protocol 可用，不替代 C7 的固定身份收益 A/B。
- C7 full 六个 measured runs 的 strict identity、pairing 与 provider coverage 已通过，comparison=`COMPARABLE`；model 90/90 effective nvidia、fallback=0、coverage=100%。Server-side rerank P50/P95 为 363/688ms；overall latency 受 H1 冷启动异常影响，不能据 aggregate P95 宣称模型更快。
- C8a 已验收归档：首个 `rag-eval-dev-v1` dataset manifest、`rag-eval-sample-v1` contract 和两个 runner 共用的本地 fail-fast validator 已落地；正式路径固定当前 30 条 question set 与 3 份 fixture，custom 输入只能显式降级为 `UNVERSIONED`。4 个 requirements / 13 个 scenarios 已接受进 `evaluation` baseline。
- C8b 已验收归档：默认与显式 v2 manifest byte-identical，固定 150 条 exact quota、v1 seed raw/object/order identity、103 条新增 answerable 的 242 个 fixture contexts、150 条 review records 与三份 fixture coverage 49/43/44；4 requirements / 12 scenarios 已接受进 `evaluation` baseline。
- C9a 已验收归档：direct runner 使用固定 `claim-lexical-v1` 将成功 answerable 输出按句子/列表拆分，并只对 provenance-valid returned citation snippets 做 exact 或 `0.70` claim-token coverage；逐 claim attribution、aggregate support rate 与 `COMPLETE/PARTIAL/SKIPPED/NOT_APPLICABLE` 局部状态已接入 direct/reproducible report/metadata。4 个 requirements / 12 个 scenarios 已接受进 `evaluation` baseline；真实 150 条 generation evidence 尚未授权和执行。
- C9b 已验收归档：normal/calibration runner 共用版本化 `rag-judge-v1` contract 与 strict parser；独立 `judge-calibration-v1` 固定 24 条四象限人工 gold case；normal eval 分离 objective/judge/global status 与 per-channel comparison safety。4 requirements / 12 scenarios 已接受进 `evaluation` baseline；live calibration 未授权并按 `SKIPPED` 收口，不能声称已有真实 judge agreement 或生产 faithfulness 结论。
- C10 已验收归档：新增 `rag-quality-gate-profile-v1`、独立离线 evaluator、固定切片、hard/reference AND、fail-closed completeness、脱敏 summary 与 `PASS/FAIL/NOT_EVALUABLE/INVALID=0/3/4/2`。4 requirements / 12 scenarios 已接受进 `evaluation` baseline；reference calls 未授权并按 `SKIPPED` 收口，首个 v2 retrieval profile 仍为 `DRAFT`，不能声称 active quality gate 或项目质量达标。
- C11 已验收归档：基于 Spring Boot 3.2.1 BOM 管理的 OTel 1.31 API/SDK 建立默认关闭、fail-open 的进程内 GenAI tracing core；ingest/ask 使用分离 trace，以稳定 `ingestTaskId/documentId/chunkId` lineage 关联，覆盖同步/流式终态、W3C/custom context、MDC bridge 与隐私白名单。4 requirements / 12 scenarios 已接受进 `rag-system` baseline；runtime 不含 network exporter，真实 provider/exporter 调用与数据出站为 0。

## 当前边界

- 不是生产级多租户系统。
- 登录与 refresh 已使用数据库用户、状态和角色；bootstrap 默认关闭，运行时不提供固定默认账号。
- LLM judge 默认关闭；C9b 已接受的是离线校准工具、静态 corpus 与状态语义，尚无 live provider evidence，不能声称 judge 已真实校准或逐 claim faithfulness 已成立。
- C10 已接受的是离线门禁 contract/evaluator 与 DRAFT profile，不包含正式 v2 reference evidence、具体阈值或 ACTIVE profile；后续激活仍须单独披露并授权 reference calls。
- C7 真实 model reranker A/B 已验收归档；默认 provider 继续保持 heuristic。标题感知长块专项仍未完成。C11 只完成默认关闭的进程内 tracing contract；C12 exporter、metrics、alerts、sampling、retention、权限与部署仍未完成。

## 长期规格

- `openspec/specs/rag-system/spec.md`
- `openspec/specs/evaluation/spec.md`
- `openspec/specs/agent-collaboration/spec.md`

## 参考事实源

- `docs/architecture/overview.md`
- `docs/roadmap/technical-debt.md`
- `docs/optimization/README.md`
- `docs/eval/RAG_EVAL_GUIDE.md`

如果参考文档与当前代码或 accepted spec 冲突，先记录差异并请示，不得自行选择对自己实现最方便的版本。
