# Tasks: C4d Milvus Failure Semantics

## 0. Approval Gate

- [x] 用户审阅并批准 proposal、design、全部决策记录与 `rag-system` spec delta。
- [x] 用户确认 C4d 只覆盖默认 Milvus，Qdrant/Elasticsearch 故障契约为 out_of_scope。
- [x] 用户确认 dense failure 仅在 keyword route 健康且返回 contexts 时降级为 `keyword_only`。
- [x] 用户确认 dense unknown + keyword disabled/unavailable/empty 返回稳定 error，不表达 `no_result`，LLM 调用次数为 0。
- [x] 用户确认降级结果写入现有 QA metadata，但不写普通成功 QA cache，也不修改公开 DTO。
- [x] 用户确认 Milvus mutation 不走应用级自动重试，post-operation 回执未知表达 `VECTOR_OPERATION_OUTCOME_UNKNOWN`。
- [x] 用户确认文档/知识库向量删除未确认成功时 fail-closed，不继续报告 SQL canonical delete 成功。
- [x] 用户确认 statistics vector count unknown 返回 503，不伪造零值且不改 DTO。
- [x] 用户确认 collection missing 返回 `VECTOR_INDEX_UNAVAILABLE`，自动重建与对账留给 C5。
- [x] 用户确认提交责任维持 `用户手动提交`，或另行明确授权 `Agent 提交`。

> 用户已于 2026-07-16 批准进入实现；实现验收前不得修改 baseline spec 或归档 change。

## 1. Inventory And RED Tests

- [x] 固化 tracked Milvus consumer inventory：create/upsert/search/delete/drop/count 与 adapter 内部 has/load/index/query。
- [x] 为 SDK thrown exception、non-success status、null/malformed response 和敏感 marker 添加 adapter RED 测试。
- [x] 为 vector search failure + keyword healthy/empty/disabled/error 添加 QueryEngine RED 测试。
- [x] 为 sync/stream QA retrieval degradation/error 添加公开可观察 RED 测试，断言 LLM/cache/query-count 副作用。
- [x] 为 KB create 与 document upsert 的 pre-operation unavailable/post-operation unknown 添加 RED 测试。
- [x] 为 document delete、KB drop 和 statistics count failure 添加 RED 测试。
- [x] 为 async task safe error 与 vector mutation retry budget 添加 RED 测试。

## 2. Adapter Boundary

- [x] 新增稳定安全的 vector/Milvus dependency exception 与 diagnostics。
- [x] 按 exception/status 结构分类 `connection / timeout / rpc / index_missing / serialization / unknown`。
- [x] 检查 create/index/load/has/insert/delete/query/drop/count/release 的 SDK response，不把 raw message 当契约。
- [x] 区分 pre-operation unavailable 与 post-mutation `outcome_unknown`。
- [x] 客户端、task error 和普通日志不包含 raw message、host/port、collection、query/content/metadata、文件名或凭据。
- [x] Qdrant/Elasticsearch 行为与配置保持不变。

## 3. Retrieval Semantics

- [x] 引入内部 retrieval result/diagnostics carrier，保留公开 QA DTO shape。
- [x] 第一条 vector variant 发生 Milvus dependency failure 后停止剩余 vector variants，keyword route 仅执行既有一次。
- [x] keyword contexts 非空时返回明确 `keyword_only`，保留健康态 rerank/finalTopK。
- [x] keyword disabled/unavailable/empty 时返回 stable retrieval error，不执行 LLM，不表达 `no_result`。
- [x] collection missing 返回 `VECTOR_INDEX_UNAVAILABLE`，search/read 不自动建 collection。
- [x] sync QA metadata 写入固定 retrieval degradation 字段；stream 使用稳定失败语义。
- [x] degraded answer 不写普通成功 QA cache；一次接受请求的 query count 不因 route failure/variant 重复增加。
- [x] 健康态 hybrid + RRF 排名、no-result、C4b generation failure 行为保持回归通过。

## 4. Index Mutation Semantics

- [x] KB collection create 未明确成功时返回稳定 503/安全错误，不返回已创建 KB。
- [x] document upsert pre-operation failure 使 document/task `FAILED`，不写 chunks/contentHash/COMPLETED/document count。
- [x] document upsert response lost 表达 `VECTOR_OPERATION_OUTCOME_UNKNOWN`，不声称 vector 未写。
- [x] vector dependency exception 直接退出当前 blanket index retry；既有非 vector retry 兼容路径保持测试锁定。
- [x] task error 只持久化稳定 code/message，不保存 SDK/cause raw message。

## 5. Lifecycle And Statistics

- [x] document vector delete 未确认成功时不继续报告 document 删除成功。
- [x] knowledge-base collection drop 未确认成功时不继续报告 KB 删除成功。
- [x] pre-operation unavailable 与 mutation outcome unknown 映射为不同稳定结果。
- [x] SQL rollback 不被描述成外部 mutation 回滚；响应不建议安全自动重试。
- [x] vector count failure 返回 503，不返回 `vectorCount=0`。
- [x] 不引入 outbox/tombstone/orphan scanner/rebuild/replay；C5 边界保持明确。

## 6. Isolated Milvus Fault Integration

- [x] 建立 `c4d-milvus-fault` Failsafe profile，复用 C3 固定 Milvus/etcd/MinIO 镜像；本 adapter 级用例不触发 embedding。
- [x] 为测试自有 Milvus 选择随机空闲后固定 host port，避免 stop/start 后端口漂移。
- [x] 对隔离 Milvus 执行 stop/start，覆盖至少 keyword-only degradation、no-keyword stable error、index task failure 和 restart search recovery。
- [x] stop/start 只接受该 Milvus Testcontainer 自身 container id，不枚举或操作用户常驻容器/volume，也不停止测试 etcd/MinIO。
- [x] 全程使用合成 KB/document/query/vector/metadata；真实 provider 业务调用量为 0。
- [x] Docker 不可用时允许编译/skip，但最终验收必须取得 0 skipped 的真实运行证据。

## 7. Verification And Closeout

- [x] 运行 `mvn -q -pl rag-core -am test`。
- [x] 运行 `mvn -q -pl rag-admin -am test`。
- [x] 运行聚焦 C4d adapter/query/index/lifecycle 回归。
- [x] 运行 `mvn -q -pl rag-admin -am verify -Pc4d-milvus-fault` 并确认非 skipped。
- [x] 运行 `mvn -q test`。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 运行 SensitiveLogs 门禁与 `git diff --check`。
- [x] 扫描 baseline spec、公开 DTO/schema、Qdrant/Elasticsearch、生产配置和受保护路径无越界改动。
- [x] 更新本 tasks、`.ai/ACTIVE_TASK.md` 与追加式 `.ai/AGENT_LOG.md`。
- [ ] 用户完成实现验收后，才接受 spec delta、恢复 `IDLE` 并归档 change。

## Commit Responsibility

当前为 `用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布；如用户后续授权 Agent 提交，只限 C4d 计划内本地文件。
