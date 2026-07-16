# RAG System Spec Delta: C4d Milvus Failure Semantics

## ADDED Requirements

### Requirement: Milvus 依赖故障分类与稳定结果

系统 SHALL 在 Milvus adapter boundary 把连接拒绝、RPC timeout、SDK non-success status、collection/index 缺失、序列化失败和未知异常转换为稳定、安全且机器可判定的结果。系统 MUST NOT 依赖 SDK 原始 message 作为客户端契约，也 MUST NOT 把 dependency unknown 表达为 empty result、success 或零值。

已知在 operation 前失败的请求 SHALL 使用 `VECTOR_STORE_UNAVAILABLE`；确认 collection/index 缺失 SHALL 使用 `VECTOR_INDEX_UNAVAILABLE`；mutation 可能已被服务端接受但回执无法确认时 SHALL 使用 `VECTOR_OPERATION_OUTCOME_UNKNOWN`。系统 MUST NOT 声称 outcome unknown 已回滚或可以安全自动重试。

#### Scenario: SDK 在 operation 前连接失败

- GIVEN Milvus SDK 在 search/create/upsert/delete/drop/count 的 operation 前抛出连接异常
- WHEN consumer 处理该调用
- THEN 系统产生稳定 `VECTOR_STORE_UNAVAILABLE`
- AND 不把调用结果表达为成功、empty 或 zero
- AND 不向客户端或普通日志输出 SDK 原始 message

#### Scenario: Collection 已确认缺失

- GIVEN 数据库中的知识库指向一个已确认不存在的 Milvus collection
- WHEN 系统执行 search/read
- THEN 系统返回 `VECTOR_INDEX_UNAVAILABLE`
- AND 不把结果表达为 `no_result`
- AND 不在 read/search 路径自动创建空 collection

#### Scenario: Mutation 回执未知

- GIVEN Milvus mutation 已发送且服务端可能已经接受
- WHEN response timeout、disconnect 或非结构化失败使执行结果无法确认
- THEN 系统返回 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- AND 不声称 mutation 未执行或已回滚
- AND 不自动重放该 mutation

### Requirement: Milvus 检索部分降级

hybrid retrieval 中 Milvus dense route 不可用时，系统 MAY 仅在 keyword route 健康且返回非空 contexts 时继续生成，并 MUST 明确标记 `retrievalMode=keyword_only`、`retrievalDegraded=true` 和 `degradedDependency=milvus`。该响应 MUST NOT 被写入普通成功 QA cache，且 MUST NOT 被描述为完整 hybrid retrieval。

当 keyword route 被禁用、调用失败或返回空 contexts 时，系统 MUST 返回稳定 retrieval error，MUST NOT 表达 `metadata.status=no_result`，也 MUST NOT 调用 LLM generation。一次 vector dependency failure 后系统 MUST 停止剩余 query-variant vector calls，不得把 query variants 当作隐式 retry budget。

#### Scenario: Dense 失败但关键词证据可用

- GIVEN hybrid retrieval 已启用
- AND Milvus dense search 发生依赖故障
- AND keyword route 成功返回非空 contexts
- WHEN 用户发起问答
- THEN 系统使用 keyword contexts 继续既有 rerank/generation
- AND QA metadata 明确标记 keyword-only degradation
- AND 不写普通成功 QA cache
- AND query count 只增加一次

#### Scenario: Dense 失败且关键词路线被禁用

- GIVEN Milvus dense search 发生依赖故障
- AND keyword route 未启用
- WHEN 用户发起问答
- THEN 系统返回稳定 `VECTOR_STORE_UNAVAILABLE`
- AND 不返回 `no_result`
- AND LLM 调用次数为 0
- AND 不写成功 cache/history

#### Scenario: Dense 失败且关键词结果为空

- GIVEN Milvus dense search 发生依赖故障
- AND keyword route 成功但返回空 contexts
- WHEN 用户发起问答
- THEN 系统返回稳定 retrieval error
- AND 不把 dense unknown 解释为知识库没有答案
- AND LLM 调用次数为 0

#### Scenario: 健康态 hybrid 行为保持不变

- GIVEN Milvus 与 keyword route 均健康
- WHEN 用户发起 hybrid retrieval
- THEN 系统继续使用现有 query variants、BM25、RRF、rerank 和 final topK
- AND 不写入 degradation 标记

### Requirement: Milvus 索引写入与生命周期一致性

知识库 collection create 未明确成功前，系统 MUST NOT 返回知识库创建成功。文档 vector upsert 未明确成功时，document/task MUST NOT 进入 `COMPLETED`，系统 MUST NOT 持久化新的成功 chunks、contentHash 或 document count。Milvus vector mutation failure MUST NOT 进入无差别应用级自动重放。

文档 vector delete 或知识库 collection drop 未确认成功时，系统 MUST NOT 报告 canonical delete 成功，也 MUST NOT 仅记录 warning 后继续删除 SQL 事实。SQL rollback MUST NOT 被描述为外部 mutation 回滚。durable compensation、orphan reconciliation、rebuild 和 replay 属于后续 C5 change。

#### Scenario: Knowledge-base collection 创建失败

- GIVEN 数据库知识库创建流程尚未向客户端返回
- WHEN Milvus collection create 在 operation 前失败
- THEN 系统返回 HTTP 503 与 `VECTOR_STORE_UNAVAILABLE`
- AND 不返回可用知识库
- AND 不把 collection 初始化失败表达为创建成功

#### Scenario: Document upsert 在 mutation 前失败

- GIVEN 文档已进入异步索引
- WHEN Milvus upsert 在 mutation 前发生依赖故障
- THEN document 与 task 进入安全 `FAILED`
- AND task error 只包含稳定 code/message
- AND 不写新的 chunks、contentHash、`COMPLETED` 或 document count
- AND vector operation 不被自动重放

#### Scenario: Document upsert 结果未知

- GIVEN 文档 vector mutation 已发送
- WHEN 系统无法确认服务端是否完成写入
- THEN document 与 task 进入 `FAILED`
- AND error code 为 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- AND 响应不声称 vector 未写或建议安全自动重试

#### Scenario: Document vector 删除未确认

- GIVEN 文档存在 SQL chunk 和 Milvus vector
- WHEN vector delete 未明确成功
- THEN 系统不得报告文档删除成功
- AND 不把异常吞掉后继续提交 canonical SQL delete
- AND outcome unknown 时明确可能存在部分外部副作用

#### Scenario: Knowledge-base collection drop 未确认

- GIVEN 知识库删除流程正在执行
- WHEN Milvus collection drop 未明确成功
- THEN 系统不得报告知识库删除成功
- AND 不把 drop failure 仅记录为 warning 后继续提交 canonical SQL delete

### Requirement: Milvus 统计与安全诊断

Milvus vector count 是 statistics 响应的事实字段。count 读取失败时系统 MUST 返回 HTTP 503 与稳定 dependency code，MUST NOT 返回伪造的 `vectorCount=0`。本 change 不要求修改 statistics DTO shape。

Milvus failure 响应、异步 task error 和普通日志 MAY 记录 dependency、固定 subsystem、固定 operation、稳定 errorCategory、failMode、traceId、安全 SDK status code 和 exception type。系统 MUST NOT 记录 SDK raw message、host/port/endpoint/credential、collection、document/vector ID、query、content、metadata/filter、文件名、标题、prompt/context/snippet 或 mutation body。

#### Scenario: Vector count 读取失败

- GIVEN statistics endpoint 无法从 Milvus 读取 vector count
- WHEN 客户端请求知识库统计
- THEN 系统返回 HTTP 503 与 `VECTOR_STORE_UNAVAILABLE`
- AND 不返回 `vectorCount=0`

#### Scenario: 故障内容包含敏感 marker

- GIVEN SDK message、collection、query、content、metadata 或 endpoint 包含合成敏感 marker
- WHEN 系统生成客户端错误、task error、diagnostics 与普通日志
- THEN 输出只包含允许的固定安全字段和稳定类别
- AND 不包含上述 marker 或原始 SDK 内容

#### Scenario: 隔离 Milvus 重启恢复

- GIVEN 测试自有 Milvus container 已完成健康 search
- WHEN 仅该 Milvus container 被 stop 后再 start
- THEN outage 期间公开入口符合 keyword-only 或 stable failure 契约
- AND restart 后应用级 search 在有界等待内恢复
- AND 测试不枚举或操作用户常驻容器、volume、etcd 或 MinIO
