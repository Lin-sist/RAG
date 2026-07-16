# Design: C4d Milvus Failure Semantics

## 1. Context

C4d 是默认向量库 Milvus 的独立 Type C change。C3 已证明健康态主链路；C4b/C4c 已分别收口 LLM/Redis。C4d 只锁定 Milvus 不可用、collection 缺失和 mutation 回执未知时的业务语义，不提前实现 C5 的恢复与协调。

Milvus 在当前系统中不是单一“检索服务”，而是多类业务事实的外部载体：

```text
conditional retrieval route
  -> QueryEngineImpl dense search（仅 keyword route 健康时可部分降级）

index mutation
  -> knowledge-base create / document upsert

data lifecycle
  -> document vector delete / knowledge-base collection drop

response field source
  -> knowledge-base statistics vector count
```

因此故障处理必须落在 adapter 分类与 consumer boundary 两层：adapter 负责把 SDK status/exception 转成稳定安全事实，consumer 决定 degrade、fail-closed 或 outcome unknown。

## 2. Goals

- 为 Milvus connection refused、RPC timeout、non-success status、restart 窗口、collection missing 和 response lost 建立稳定分类。
- 在 BM25 真实可用时允许 dense route 明确降级，不把 keyword-only 包装成完整 hybrid。
- 在无法证明检索完整时禁止伪造 `no_result`，避免无上下文 generation。
- 让 create/upsert/delete/drop/count 的失败结果与副作用状态可观察，不报告假成功、假零值或安全重试。
- 保证客户端、异步 task error 与普通日志不泄露 SDK raw message 或用户/索引内容。
- 使用确定性 fault seam 与隔离 Milvus stop/start 锁定公开行为和 restart recovery。

## 3. Non-goals

- 不为 Qdrant/Elasticsearch 建立同等故障契约。
- 不做 Milvus HA、replica、backup/restore、服务发现、熔断、连接池或性能调优。
- 不做 durable index input、replay、orphan reconciliation、resume 或跨存储补偿事务。
- 不改 embedding/BM25/RRF/rerank/prompt/citation/no-answer/judge 的健康态算法和指标口径。
- 不改公开 DTO、数据库 schema、生产连接配置或前端。
- 不承诺 external mutation exactly-once。

## 4. 公共故障契约矩阵模板

| 维度 | 含义 |
|---|---|
| dependency / operation | Milvus 与具体 SDK/业务动作 |
| failure category | 稳定分类，不依赖原始 message |
| retry eligibility | 是否允许应用级重放 |
| attempt budget | 单个业务动作的硬上限 |
| client outcome | HTTP、QA metadata 或 task/document 可观察状态 |
| side effects | vector、SQL chunk/document、keyword index、cache/history |
| diagnostics | 允许记录的固定安全字段 |
| verification | mock response/exception 与隔离 stop/start |

### 4.1 C4d Milvus 矩阵

| operation / failure | criticality | retry | client outcome | side effects |
|---|---|---|---|---|
| dense search unavailable + keyword contexts available | conditional route | 无应用级 retry | `keyword_only` 明确降级 | 可继续 generation；不写普通成功 QA cache |
| dense search unavailable + keyword disabled/unavailable/empty | response source | 无应用级 retry | sync QA 保持外层兼容但 `metadata.status=error`；stream 进入稳定失败 | LLM 0 次；不伪造 `no_result` |
| collection confirmed missing on search | index consistency | 不自动创建 | `VECTOR_INDEX_UNAVAILABLE` | 不把 missing 当 empty，不触发重建 |
| knowledge-base collection create | lifecycle write | 无透明重放 | HTTP 503 或稳定业务失败 | KB create 不返回成功，SQL transaction 回滚/失败 |
| document vector upsert pre-operation failure | index write | 无应用级 retry | document/task `FAILED` + `VECTOR_STORE_UNAVAILABLE` | 不写 chunks/COMPLETED/document count |
| document vector upsert response lost | index write | 禁止自动重放 | document/task `FAILED` + `VECTOR_OPERATION_OUTCOME_UNKNOWN` | vector 可能已写；交 C5 对账 |
| document vector delete response lost/failure | data lifecycle | 禁止自动重放 | 删除请求不成功，稳定 unavailable/unknown | 不继续声称 SQL canonical delete 成功 |
| knowledge-base collection drop response lost/failure | data lifecycle | 禁止自动重放 | 删除请求不成功，稳定 unavailable/unknown | 不继续声称 KB 删除成功 |
| vector count failure | response field source | 无应用级 retry | HTTP 503 | 不返回 `vectorCount=0` |
| SDK non-success/raw exception | adapter boundary | 不由 adapter 重试 | stable exception + fixed diagnostics | raw message 不出边界 |

Milvus SDK 自身连接恢复不描述为业务 retry。当前文档索引的非 vector retry 兼容路径不在 C4d 重构范围；C4d 只保证 vector mutation exception 不被整段无差别自动重放。

## 5. Consumer Inventory

| consumer / adapter operation | 当前行为 | C4d 分类与计划语义 |
|---|---|---|
| `KnowledgeBaseServiceImpl.create` → `createCollection` | SQL insert 后创建 collection；失败包装 `KB_005` | lifecycle write；失败不返回 KB，稳定 503/安全 message |
| `DocumentIndexingServiceImpl` → `upsert` | 全部 runtime 最多 3 次整段重试；最终 document/task FAILED | index write；pre-operation unavailable 与 post-operation unknown 分离，vector exception 不自动重放 |
| `QueryEngineImpl` → `search` | vector 先执行；失败导致 keyword route 不执行 | keyword healthy+non-empty 时 degrade，否则稳定 retrieval error |
| `DocumentServiceImpl.delete` → `delete` | catch 后继续删 chunks/document | data lifecycle；不得吞失败后报告删除成功 |
| `KnowledgeBaseServiceImpl.delete` → `dropCollection` | catch 后继续删 KB | data lifecycle；不得吞失败后报告删除成功 |
| `KnowledgeBaseServiceImpl.getStatistics` → `count` | catch 后保留初始 `0` | response field source；503，不伪造零值 |
| `MilvusVectorStore.has/load/createIndex` | 多数检查 `R.status`，message 拼进 exception | adapter classification；稳定 category/code，禁止 raw message 出边界 |
| `MilvusVectorStore.deleteIfExists/releaseCollection` | SDK response 未统一检查 | adapter correctness；所有 mutation response 必须判定 success/unknown |
| `VectorStore.getById/getByIds` | 当前无 tracked 业务 consumer | adapter 一致性测试可覆盖；不新增用户能力 |
| `VectorStoreConfig` client creation | 仅 connect timeout，无 C4d 稳定分类 | 启动/首次调用差异需测试；不改生产地址或连接池 |

## 6. Contract Details

### 6.1 Stable dependency exception

Milvus adapter 产生统一安全 exception，至少携带：

- `dependency=milvus`；
- `subsystem`：`vector_search`、`vector_write`、`vector_lifecycle`、`vector_stats`；
- `operation`：固定枚举式动作名；
- `errorCategory`：`connection`、`timeout`、`rpc`、`index_missing`、`serialization`、`unknown`；
- `failMode`：`degraded`、`closed`、`outcome_unknown`；
- 可选安全 SDK status code 与 exception type。

分类只能依据 exception type、cause type、SDK status/code 和明确结构化字段。原始 message 可作为 cause 保留在内存异常链供调试工具使用，但不得进入普通日志、task error、QA metadata 或客户端 message。

### 6.2 Dense retrieval partial degradation

当前 `QueryEngine` 只返回 `List<RetrievedContext>`，无法可靠携带 route 状态。规划默认引入内部 retrieval result carrier（contexts + fixed diagnostics），保留公开 QA DTO shape；`RAGServiceImpl` 把固定字段写入现有 metadata map：

- `retrievalMode=hybrid` 或 `keyword_only`；
- `retrievalDegraded=true/false`；
- `degradedDependency=milvus`（仅降级时）；
- `vectorErrorCategory=<stable category>`（仅降级/失败时）。

当第一条 vector variant 遇到 dependency failure，立即停止剩余 vector variants；这不是把 query variant 当作 retry budget。keyword route 只执行既有一次调用：

- keyword 返回 contexts：继续 rerank/generation，并显式标记 keyword-only；
- keyword disabled、抛错或返回 empty：抛出稳定 retrieval failure；
- 不得把 dense unknown + keyword empty 转为 `no_result`；
- degraded answer 不写普通成功 QA cache，避免 Milvus 恢复后长期命中较弱答案；query count 仍只增加一次。

健康态 hybrid 排序、RRF 参数、keyword multiplier、reranker 和最终 topK 保持不变。

### 6.3 Collection missing

read/search 中确认 collection missing 表示数据库知识库记录与向量索引不一致，不是空 collection。系统返回 `VECTOR_INDEX_UNAVAILABLE`，不自动 create，也不生成 no-answer。write path 仅在既有 knowledge-base create/index contract 下创建 collection；自动重建、数据回填和 repair 属于 C5。

### 6.4 Create/upsert and outcome unknown

knowledge-base create 在 collection 未确认创建成功前不得返回成功。document indexing 的 vector upsert 有三个阶段：

1. `pre_operation`：连接/hasCollection/load 等在 mutation 前失败，已知未执行；
2. `operation_acknowledged`：SDK 明确 success；
3. `outcome_unknown`：insert/delete 等 mutation 可能已被服务端接受，但 response timeout/disconnect/non-structured failure 无法确认。

pre-operation failure 使用 `VECTOR_STORE_UNAVAILABLE`；post-operation unknown 使用 `VECTOR_OPERATION_OUTCOME_UNKNOWN`。两者都使 document/task 进入安全 `FAILED`，但不得向用户声称 vector 一定未写。C4d 不自动重放 vector mutation；现有非 vector retry 行为保持原边界，需测试证明 vector exception 会直接退出 retry loop。

向量未明确成功时不得持久化新的 DB chunks、contentHash、`COMPLETED`、document count 或 keyword success。若 vector 已成功而后续 SQL/keyword 失败，既有非 vector 行为和 C5 对账风险不在 C4d 扩写。

### 6.5 Delete/drop lifecycle

当前 delete/drop failure 被吞掉后继续删除 SQL 事实，可能留下仍可搜索或无法追踪的向量数据。C4d 默认 fail-closed：

- SDK 明确成功后才继续 canonical SQL delete；
- 已知 pre-operation unavailable 返回 `VECTOR_STORE_UNAVAILABLE`；
- mutation response lost 返回 `VECTOR_OPERATION_OUTCOME_UNKNOWN`；
- 不返回删除成功，不把 unknown 写成普通 warning 后继续；
- SQL rollback 只能说明数据库未提交，不能证明外部 mutation 未执行，客户端不得收到“可安全自动重试”的承诺。

durable tombstone、outbox、补偿任务和孤儿扫描属于 C5，不在 C4d 临时加入内存队列。

### 6.6 Statistics

`vectorCount` 是响应事实字段。Milvus count failure 时返回 HTTP 503 与稳定 dependency code，不使用初始值零作为故障占位。本 change 不新增 nullable/partial statistics DTO；若未来需要 partial response，另起 API contract change。

### 6.7 Safe task/client diagnostics

异步 task `error` 只保存稳定 code/message，不保存 `VectorStoreException.getMessage()` 或 SDK cause message。普通故障日志允许固定 dependency/subsystem/operation/category/failMode/traceId/exception type；禁止记录：

- raw SDK message、stack cause message、host/port/endpoint/credential；
- collection name、document/vector ID、query、content、metadata、filter；
- 文件名、标题、prompt/context/snippet；
- mutation request/response body。

## 7. Observable Mapping

| condition | observable result | stable code / metadata |
|---|---|---|
| vector search fail + keyword contexts | QA 可继续成功 | `retrievalMode=keyword_only`, `retrievalDegraded=true` |
| vector search fail + no usable keyword route | sync QA 外层兼容 error；stream stable failure | `VECTOR_STORE_UNAVAILABLE` |
| collection confirmed missing | retrieval/index management failure | `VECTOR_INDEX_UNAVAILABLE` |
| KB create/count/delete pre-operation failure | HTTP 503 | `VECTOR_STORE_UNAVAILABLE` |
| async index pre-operation failure | document/task `FAILED` | safe `VECTOR_STORE_UNAVAILABLE` |
| mutation acknowledgement unknown | HTTP 503 或 task `FAILED` | `VECTOR_OPERATION_OUTCOME_UNKNOWN` |
| normal healthy no-result | 保持现有 no-answer | `metadata.status=no_result`，不得与 dependency failure 混淆 |

同步 QA 当前使用 HTTP 200 外层兼容和 `QAResponse.metadata.status=error`；C4d 不借机重做该公开协议。管理类同步 endpoint 的 dependency failure 使用 HTTP 503。streaming 继续现有 error channel，不在 C4d 引入结构化 SSE 事件。

## 8. Implementation Shape

预计实现边界：

- `rag-core/vectorstore`：新增安全的 vector dependency exception/diagnostics；修正 Milvus response checking 与 raw message 边界；不改 Qdrant/Elasticsearch 行为。
- `rag-core/rag/query`：内部 retrieval result carrier、vector failure → keyword-only 分支、健康态 RRF 兼容。
- `rag-core/rag/service`：把 retrieval diagnostics 映射到现有 QA metadata，阻止 degraded answer 写普通 cache，保持 C4b generation failure 副作用。
- `rag-admin/kb`：KB create/delete/statistics 和 document index/delete 的 closed/unknown 语义；vector exception 不进入 blanket index retry。
- `rag-common/async`：仅在必要时提供安全 task error 映射，不改变 C4c Redis 状态事实源契约。
- `rag-admin` test profile：独立 `c4d-milvus-fault` Failsafe，复用 C3 固定 Milvus/etcd/MinIO 镜像与确定性 embedding。
- change artifacts、`.ai/ACTIVE_TASK.md` 与追加式 `.ai/AGENT_LOG.md`。

若实现需要公开 DTO/schema/新依赖/生产配置，必须停止并回到事前闸门补设计。

## 9. TDD And Verification

按 RED → GREEN → REFACTOR 推进：

1. adapter response/exception classification 与敏感 marker RED；
2. QueryEngine vector failure + keyword healthy/empty/disabled RED；
3. RAGService degraded metadata、LLM/cache/query-count 副作用 RED；
4. create/upsert pre-operation 与 post-operation unknown RED；
5. document/KB delete 和 statistics count RED；
6. async task safe error 与 vector retry budget RED；
7. 隔离 Milvus stop/start 公开 HTTP/task 验证；
8. 模块与全量门禁。

计划命令：

```powershell
mvn -q -pl rag-core -am test
mvn -q -pl rag-admin -am test
mvn -q -pl rag-admin -am verify -Pc4d-milvus-fault
mvn -q test
python -B -m unittest discover -s scripts -p 'test_*.py'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode SensitiveLogs
git diff --check
```

隔离 profile 可以在 Docker 不可用时 skip 编译验证，但不得把 skipped 记为 C4d 验收通过；最终必须取得真实 stop/start `0 skipped` 证据。测试只操作自己的 Milvus container id，不停止 etcd/MinIO 或用户常驻容器。Milvus host port 使用随机选取后固定映射，避免 Docker Desktop stop/start 后端口漂移。

## 10. External Calls And Cost

真实 embedding/rerank/judge/ask/LLM 调用量为 0。测试使用进程内确定性 embedding 和合成文档；本地 Docker 只承载固定 Milvus/etcd/MinIO 基础设施镜像，无业务数据出站和模型费用。

## 11. Alternatives

### A. 所有 Milvus failure 统一返回 503

拒绝作为默认。hybrid 模式下 BM25 可能仍有真实证据，统一失败会损失可用性；但只有在 keyword contexts 非空且明确标记时才允许降级。

### B. 所有 Milvus failure 都返回 keyword-only/no-result

拒绝。keyword disabled、失败或 empty 时无法证明知识库无答案，把 dense unknown 表达成 no-result 会制造错误拒答。

### C. mutation failure 继续自动重试三次

拒绝作为 vector 默认。response lost 时重复 mutation 可能扩大 delete/insert 副作用，且当前 `upsert` 实际为 delete + insert sequence；必须先区分 pre-operation 与 outcome unknown。

### D. 删除失败只记 warning 后继续 SQL delete

拒绝。它会留下孤儿向量或无法证明已删除的数据，且用户收到假成功；C4d 先 fail-closed，durable compensation 留 C5。

### E. 为 vectorCount 引入 nullable/partial DTO

暂不采用。它会扩成公开 statistics schema change；C4d 保持 DTO 并用 503 表达 unknown。

## 决策记录

### DR-1：C4d 覆盖 Milvus 还是所有 vector adapters
- **面临的选择**：同时统一 Milvus/Qdrant/Elasticsearch、只处理当前默认 Milvus，或修改共享接口但让所有 adapter 行为一起变化。
- **选了哪个 + 为什么**：规划默认只处理 Milvus，并把共享改动限制为不改变其他 adapter 行为，因为路线图和 C3 验收对象都是 Milvus；待用户在事前闸门确认。
- **放弃的代价**：Qdrant/Elasticsearch 仍没有同等级故障契约，未来启用前需要独立 change，不能宣称 vector store 全家族已完成韧性治理。

### DR-2：dense search 失败时整体失败还是 keyword-only
- **面临的选择**：所有请求整体失败、无条件返回 keyword-only，或仅在 keyword route 健康且返回 contexts 时降级。
- **选了哪个 + 为什么**：规划默认选择条件式 keyword-only，因为它保留可用性同时不把空关键词结果伪装成完整检索；待用户在事前闸门确认。
- **放弃的代价**：查询编排需要携带 route 状态和更多测试，降级答案的召回覆盖率也可能低于正常 hybrid。

### DR-3：如何向上层表达检索降级
- **面临的选择**：只写日志、给每个 context 塞隐式 marker，或引入内部 retrieval result carrier 并映射到现有 QA metadata。
- **选了哪个 + 为什么**：规划默认选择内部 result carrier，因为日志不能约束客户端可观察语义，context marker 容易在融合/重排中丢失，同时无需改公开 DTO；待用户在事前闸门确认。
- **放弃的代价**：`QueryEngine` 内部调用与测试需要调整，改动面大于局部 catch。

### DR-4：dense unknown + keyword empty 是否算 no-result
- **面临的选择**：返回正常 no-result、继续无上下文 generation，或返回稳定 retrieval error。
- **选了哪个 + 为什么**：规划默认返回 retrieval error，因为 dense 状态未知时不能证明知识库没有答案，也不能让 LLM 在无证据下作答；待用户在事前闸门确认。
- **放弃的代价**：Milvus outage 且关键词召回为空时可用性下降，但结果不会伪造为业务无答案。

### DR-5：degraded answer 是否进入普通 QA cache
- **面临的选择**：照常缓存、使用新短 TTL cache，或不写普通成功 cache。
- **选了哪个 + 为什么**：规划默认不写普通成功 cache，避免 Milvus 恢复后持续命中较弱 keyword-only 答案，同时不扩展 Redis key/TTL 契约；待用户在事前闸门确认。
- **放弃的代价**：outage 期间相同问题会重复执行 keyword retrieval 和 generation，延迟与 provider 压力上升。

### DR-6：Milvus mutation failure 是否自动重试
- **面临的选择**：沿用所有 runtime 三次重试、只重试确认未执行的 pre-operation failure，或 C4d 默认不做应用级 vector mutation retry。
- **选了哪个 + 为什么**：规划默认不做应用级 vector mutation retry，因为 delete + insert/drop 在回执丢失时无法证明安全重放，先锁定 unavailable/unknown 更诚实；待用户在事前闸门确认。
- **放弃的代价**：短暂抖动可能让一次索引或删除失败，需要用户稍后重新发起或等待 C5 协调能力。

### DR-7：删除失败后是否继续 SQL delete
- **面临的选择**：best-effort warning 后继续、fail-closed 并保留 SQL 事实，或本 change 引入 outbox/tombstone 补偿。
- **选了哪个 + 为什么**：规划默认 fail-closed，因为继续会产生孤儿向量和假成功；outbox/tombstone 需要持久状态机，属于 C5；待用户在事前闸门确认。
- **放弃的代价**：Milvus outage 期间文档/知识库删除不可用，且 outcome unknown 时客户端仍需查询当前状态。

### DR-8：statistics count unknown 如何表达
- **面临的选择**：继续返回零、修改 DTO 表达 partial/unknown，或保持 DTO 并返回 503。
- **选了哪个 + 为什么**：规划默认返回 503，因为零是业务事实，不是故障占位，同时避免 C4d 扩成 statistics API 重设计；待用户在事前闸门确认。
- **放弃的代价**：即使 document/query count 可读，整个 statistics endpoint 也会在 Milvus outage 时不可用。

### DR-9：collection missing 是否自动重建
- **面临的选择**：search 时自动建空 collection、当作 empty/no-result，或返回 index unavailable 并把重建留给 C5。
- **选了哪个 + 为什么**：规划默认返回 `VECTOR_INDEX_UNAVAILABLE`，因为创建空 collection 会掩盖数据丢失并产生假 no-result；待用户在事前闸门确认。
- **放弃的代价**：C4d 完成后仍不能自愈 collection 缺失，只能准确暴露并等待后续恢复流程。

### DR-10：错误分类使用 raw message 还是结构化事实
- **面临的选择**：继续匹配/返回 SDK message、只保留一个通用 unknown，或依据 exception/status 生成稳定 category 并屏蔽 raw message。
- **选了哪个 + 为什么**：规划默认使用结构化稳定分类，因为 raw message 不稳定且可能进入 task/client，单一 unknown 又不足以验证重启与 timeout；待用户在事前闸门确认。
- **放弃的代价**：需要维护 SDK exception/status 映射，未识别版本仍只能落到 `unknown`。

### DR-11：真实故障验证使用共享服务还是隔离容器
- **面临的选择**：mock-only、停止用户常驻 Milvus，或 mock 加测试自有 Milvus stop/start。
- **选了哪个 + 为什么**：规划默认 mock 加隔离 Testcontainers，以覆盖真实 RPC/reconnect 又不破坏用户环境；Milvus 固定 host port 避免 Docker Desktop 端口漂移；待用户在事前闸门确认。
- **放弃的代价**：验证资源占用和耗时较高，Docker 不可用时只能完成编译/单元测试而不能验收。

### DR-12：C4d 是否包含恢复、HA 与跨存储对账
- **面临的选择**：同时实现 repair/replay/HA，加入进程内临时补偿，或只锁定故障结果并把恢复留给 C5。
- **选了哪个 + 为什么**：规划默认只锁定故障结果，因为 durable input、orphan reconciliation 和 resume 已有 C5a/C5b 顺序，内存补偿在重启后也不可靠；待用户在事前闸门确认。
- **放弃的代价**：C4d 后仍需人工处理 outcome unknown，且不能自动重建缺失 collection 或清理孤儿向量。

### DR-13：提交责任
- **面临的选择**：沿用之前阶段的提交授权、由 Agent 自动提交，或把 C4d 作为新 change 重新确认。
- **选了哪个 + 为什么**：选择用户手动提交，因为当前请求只授权开始规划，仓库规则要求每个 change 单独明确提交责任。
- **放弃的代价**：规划和后续实现提交需要用户显式操作或重新授权 Agent。
