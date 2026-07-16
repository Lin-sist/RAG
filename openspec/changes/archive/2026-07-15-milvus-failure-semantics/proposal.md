# Proposal: C4d Milvus Failure Semantics

## Why

C3 已用隔离 Milvus 2.3.4 证明登录、上传、异步索引、混合检索、删除和资源清理的 happy-path 可重复运行；C4b、C4c 已分别接受 LLM 与 Redis 故障契约。下一步需要锁定默认向量库 Milvus 在连接拒绝、RPC timeout、重启窗口、collection 缺失和 mutation 回执不确定时的业务语义。

当前 Milvus 同时参与知识库创建、文档索引、dense retrieval、文档删除、知识库删除和统计计数。现有行为不一致：dense search 失败会在 BM25 可用时仍中断整条检索；索引服务会对所有 `RuntimeException` 统一重试；文档/知识库删除会吞掉向量删除失败后继续删除 SQL 事实；statistics 读取失败会把 `vectorCount` 伪装成零；SDK 原始 message 还可能进入异步任务 error 或客户端响应。若只在 `MilvusVectorStore` 中统一吞异常，会进一步把检索可降级、mutation 结果未知和生命周期一致性混为一谈。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，Milvus 短暂重启可能让本来还能靠 BM25 回答的问题整体失败，也可能把向量删除失败误报成删除成功、把统计故障显示成零，或把 SDK 原始错误暴露给用户；改之后，dense route 失败时只有在关键词证据真实可用时才明确降级，其余入口返回稳定且安全的失败/结果未知语义，索引、删除和统计不再伪造完成、空结果或零值。

## Current Status

- `confirmed`：`main` 与 `origin/main` 同步、工作区干净，`ACTIVE_TASK=IDLE`；C3、C4b、C4c 已归档，C4c delta 已接受进 `rag-system` baseline。
- `confirmed`：默认 vector store 为 Milvus；tracked 业务消费者包括知识库 create/drop/count、文档 upsert/delete 和 `QueryEngineImpl` dense search。
- `confirmed`：C3 `c3-integration` 使用固定 `milvusdb/milvus:v2.3.4`、etcd、MinIO 与确定性 embedding，已证明健康态主链路，不证明故障语义。
- `partial`：`MilvusVectorStore` 会检查多数 `R` status，并在 search 前调用 `loadCollection`；但异常只有通用 `VectorStoreException`，分类依赖原始 message，部分 release/delete 响应未检查。
- `partial`：索引异常最终会把 document/task 标为 `FAILED`，但所有 runtime failure 共用三次重试，Milvus mutation 回执未知时可能重复执行；task error 仍可能保存底层 message。
- `partial`：hybrid retrieval 已有 BM25 + dense + RRF，但 vector failure 会在 keyword route 执行前直接传播；没有 `keyword_only` 降级标记，也无法区分“完整检索无结果”和“dense 状态未知”。
- `partial`：文档和知识库删除吞掉 Milvus failure 后继续删除 SQL 数据；statistics count failure 返回 `vectorCount=0`。
- `planned`：建立 Milvus operation matrix、稳定错误类别、keyword-only 明确降级、mutation outcome unknown、生命周期 fail-closed、安全诊断及隔离 stop/start 验证。
- `out_of_scope`：Qdrant/Elasticsearch 故障契约、Milvus HA/replica/backup、索引输入持久化、孤儿任务协调/续跑、容量与性能调优。
- `unknown`：Milvus Java SDK 在 stop/start、RPC timeout 与服务端已接受 mutation 但回执丢失时的实际 exception/status；现有 SQL transaction 与外部 mutation 的可观察边界需用测试确认。

## Scope

- 复用 C4 公共故障契约矩阵，仅覆盖当前默认 Milvus adapter 及其 tracked consumers。
- 将 Milvus failure 分类为 `connection / timeout / rpc / index_missing / serialization / unknown`，以稳定安全字段表达，不依赖原始 message 作为客户端契约。
- dense search 失败且 keyword route 健康并返回证据时，允许显式 `keyword_only` 降级；不得把降级伪装成完整 hybrid retrieval。
- dense search 状态未知且 keyword route 不可用、被禁用或无结果时，返回稳定 retrieval error，不得伪装成 `no_result`，也不得进入 LLM generation。
- degraded keyword-only 响应必须携带现有 metadata map 可表达的固定降级字段，并不得写入普通成功 QA cache；query count 仍按一次已接受请求计数。
- collection 已确认缺失时表达 `VECTOR_INDEX_UNAVAILABLE`，search/read 不自动创建 collection；重建与恢复留给 C5。
- knowledge-base create、document upsert、document delete、knowledge-base drop 和 vector count 建立明确的 `closed / outcome_unknown` 语义。
- Milvus mutation failure 不走当前无差别应用级重试；保留非 vector failure 的既有兼容行为，不在 C4d 重构整个索引事务。
- 文档/知识库向量删除或 drop 未确认成功时不得继续报告删除成功；SQL 与外部向量状态不一致的恢复/对账留给 C5。
- statistics vector count 读取失败返回稳定 503，不返回伪造的 `vectorCount=0`，本 change 不改 DTO shape。
- 使用 mock/fake SDK seam 与隔离 Milvus Testcontainer stop/start 覆盖公开可观察结果；只操作测试自有 container id 和合成数据。

## Non-goals

- 不修改 Qdrant、Elasticsearch adapter 的故障契约；二者保持可编译但不作为 C4d 验收对象。
- 不实现 Milvus Cluster、replica failover、backup/restore、服务发现、熔断器、连接池或容量调优。
- 不做索引输入持久化、任务重放、孤儿任务扫描、断点续跑或跨存储对账；这些属于 C5a/C5b。
- 不承诺 Milvus mutation exactly-once；post-operation 回执未知只表达 outcome unknown。
- 不修改 embedding、BM25 算法、RRF、reranker、prompt、citation、no-answer 或 judge 指标口径。
- 不修改公开 API DTO、数据库 schema、Flyway migration、前端交互或生产 Milvus 地址。
- 不为了故障测试定制评测集或改变正常健康态 retrieval 排名。
- 本规格阶段不修改生产 Java、测试、POM、配置、依赖或 baseline spec。

## Spec Delta Decision

本 change 会改变 hybrid retrieval、知识库/文档生命周期、statistics 和异步索引失败时的用户可见语义，因此必须提供 `rag-system` spec delta。delta 只在用户实现验收后接受进 baseline；规划审批不等于 spec 已接受或能力已实现。

## External Calls And Authorization

| 调用类型 | 规划/实现验证调用量 | 数据出站 | 模型 | 限流风险 | 费用 | 授权状态 |
|---|---:|---|---|---|---|---|
| embedding | 0 真实调用 | 无；测试使用进程内确定性 stub | 无 | 无 | 0 | 不适用 |
| rerank | 0 | 无 | 无 | 无 | 0 | 不适用 |
| judge | 0 | 无 | 无 | 无 | 0 | 不适用 |
| ask/LLM | 0 | 无 | 无 | 无 | 0 | 不适用 |

规划阶段不启动容器。实现验证只允许使用固定基础设施镜像、合成文档与测试自有 Testcontainers network；可能下载已声明镜像，但不上传业务数据，不调用模型 provider。

## Acceptance Evidence

- 用户先审阅并批准 proposal、design、决策记录、tasks 与 spec delta，再允许修改生产代码、测试或 POM。
- consumer inventory 覆盖 create、upsert、search、delete、drop、count 及 adapter 内部 has/load/index/query response handling。
- vector search failure + healthy keyword evidence 产生明确 `keyword_only` 降级；不产生假 hybrid 标记，不写普通成功 QA cache。
- vector search failure + keyword disabled/unavailable/empty 返回稳定 error；不返回 `no_result`，LLM 调用次数为 0。
- collection missing 与 connection/timeout 分类分离，read/search 不自动建 collection。
- create/upsert pre-operation failure 与 post-operation outcome unknown 均使 document/task 不进入假 `COMPLETED`；vector mutation 不被无差别自动重放。
- document delete、knowledge-base drop 未确认成功时不报告删除成功；statistics count failure 不返回假零值。
- 客户端、task error 和普通日志不包含 SDK raw message、collection、query、content、metadata、文件名、凭据或 endpoint 细节。
- mock fault tests 与隔离 Milvus stop/start 集成测试通过；隔离测试只操作自身 container id，restart 后应用级 search 恢复。
- `mvn -q -pl rag-core -am test`、`mvn -q -pl rag-admin -am test`、选定 Failsafe、完整 Maven、Python、SensitiveLogs 与 `git diff --check` 通过。
- 真实 embedding/rerank/judge/ask/LLM 业务调用量均为 0。

## Risks

- keyword-only 降级会降低召回覆盖率；如果不显式标记或仍写普通缓存，可能把较弱答案包装成完整 hybrid 结果。
- Milvus insert/delete/drop 的响应丢失无法仅靠客户端证明服务端是否已执行；C4d 只能诚实表达 outcome unknown，长期对账属于 C5。
- 删除 fail-closed 会降低 Milvus outage 期间的数据管理可用性，但继续删除 SQL 事实会留下不可追踪向量和数据保留风险。
- 当前索引重试覆盖整个 vector + SQL/keyword sequence；只排除 vector mutation retry 需要聚焦测试，避免破坏既有非 vector retry 兼容路径。
- Testcontainers Milvus 依赖 etcd/MinIO 且资源占用高；stop/start 必须固定测试 Milvus host port，避免 Docker Desktop 随机端口漂移让恢复测试失真。
- `technical-debt.md` 仍把 Redis 与 Milvus 一并列为剩余债务；accepted C4c baseline 优先级更高，该文档陈旧不阻塞本 change，也不在规划阶段顺手改写。

## Commit Responsibility

`用户手动提交`。用户已批准 C4d 规划并授权进入实现，但未授权 Agent 暂存、提交或 push。
