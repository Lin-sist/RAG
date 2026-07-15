# Design: C3 Integration Test Happy Path

## 1. Context

C3 的目标不是新增产品功能，而是为 accepted 的认证、索引和检索契约建立真实依赖联合证据。现有测试存在三类断层：

1. 应用级 `test` profile 使用 H2 并关闭 Flyway；
2. Redis 不可用时部分性质测试在内部降级；
3. Milvus 与 embedding 没有进入同一个 Spring happy-path。

设计必须同时满足：真实数据基础设施、零业务 provider 外调、重复运行隔离、Docker 缺失时明确失败、与默认快速单测解耦。

## 2. Goals

- 用隔离容器验证数据库认证、Redis session/任务状态、异步索引、Milvus 与 hybrid retrieval 的 happy path。
- 用 JVM 内确定性 embedding 消除真实 provider、网络、限流和费用变量。
- 保留生产 API、DTO、provider 接口、检索行为和分块配置不变。
- 提供一条清晰命令和稳定失败信号，便于开发者本地复跑。

## 3. Non-goals

- 不验证 LLM generation、citation、no-answer、judge 或 SSE。
- 不定义依赖故障时的产品降级语义。
- 不验证并发、吞吐、性能或生产容量。
- 不修改现有 Docker Compose 容器状态。
- 不把 C3 fixture 指标当成真实 retrieval baseline 或模型收益证据。

## 4. Test Entry and Lifecycle

### 4.1 独立 Maven 入口

计划在 `rag-admin` 增加 `c3-integration` Maven profile，并通过 Maven Failsafe 运行 `*IT`：

```powershell
mvn -q -pl rag-admin -am -Pc3-integration verify
```

选择独立 profile，而不是把五容器测试塞进默认 `mvn test`，原因是：

- 默认单元测试仍应在无 Docker 环境快速运行；
- C3 命令一旦显式执行，就必须要求 Docker 并失败可见，不能 self-skip；
- Failsafe 的 `integration-test` / `verify` 生命周期更适合应用启动、资源清理和集成测试失败报告。

`mvn -q test` 仍作为回归门禁运行，但它不替代专用 C3 命令。

### 4.2 Spring test 形态

主测试使用：

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`；
- 独立 `c3-integration` Spring profile；
- HTTP client 调用真实 controller/security/filter/service 链路；
- `@Testcontainers`，不设置 `disabledWithoutDocker=true`；
- 单个顺序 happy-path 测试，避免多个测试共享异步状态后产生顺序不确定性。

测试失败或 teardown 时仍由 Testcontainers/Ryuk 清理资源。不得通过 catch/assumption 把 Docker、Redis、Milvus、Flyway 或索引失败变成 skip。

## 5. Container Topology

### 5.1 拓扑

```text
Spring Boot test JVM
├─ MySQLContainer (random host port)
├─ Redis GenericContainer (random host port)
└─ isolated Testcontainers Network
   ├─ etcd (network alias: etcd)
   ├─ MinIO (network alias: minio)
   └─ Milvus standalone
      ├─ ETCD_ENDPOINTS=etcd:2379
      ├─ MINIO_ADDRESS=minio:9000
      └─ mapped gRPC/health ports
```

### 5.2 镜像与隔离

- MySQL 固定为仓库已验证的 `mysql:8.0.36`。
- Milvus 固定为当前 adapter 对齐的 `milvusdb/milvus:v2.3.4`。
- etcd 与 MinIO 初始候选沿用当前 Compose 的精确版本，但实现时须记录兼容性验证。
- Redis 在实现阶段选择并固定精确 patch tag；验收终态禁止 `latest` 或只固定 major 的漂移标签。
- 不声明固定 container name，不绑定固定 host ports，不挂载命名 volume，不启用 container reuse。
- 只在 Milvus 内部依赖间使用 network alias；Spring 通过 mapped host/port 访问 MySQL、Redis、Milvus。

本机已运行的 `rag-mysql`、`rag-redis`、`rag-etcd`、`rag-minio`、`rag-milvus` 不属于测试生命周期，不读取、不停止、不复用。

### 5.3 Readiness

- MySQL 使用 JDBC 可连接等待策略。
- Redis 使用 `PING` 或监听端口后再由 Spring health/readiness 验证。
- etcd 使用 endpoint health。
- MinIO 使用 `/minio/health/live`。
- Milvus 使用 `/healthz`，并在 Spring context 创建前完成 readiness。

容器启动和索引轮询都必须设置有界超时；失败报告至少包含依赖名称、镜像、阶段、耗时和稳定错误类别，不记录密码或 token。

## 6. Dynamic Configuration

通过 `@DynamicPropertySource` 或等价 Testcontainers service connection 映射：

- `spring.datasource.url/username/password`；
- `spring.data.redis.host/port/password`；
- `rag.vectorstore.type=milvus`；
- `rag.vectorstore.milvus.host/port/database`；
- `spring.flyway.enabled=true`；
- `auth.bootstrap.enabled=true` 与仅用于 test scope 的合成 username/password；
- 测试 JWT secret；
- `rag.embedding.openai.enabled=false`、`qwen.enabled=false`、`bge.enabled=false`；
- reranker 保持 heuristic，model adapter 关闭。

不改写现有 `application-test.yml` 的 H2 行为。新增的 `application-c3-integration.yml` 只保存非 secret、稳定的 test defaults；容器端口与合成凭据在测试进程内注入。

Flyway 必须从空 MySQL 数据库执行当前全部 migration。C3 不回改 V1→V6，也不重复 C2 migration 专项矩阵；它只证明应用 happy-path 使用的 schema 来自真实迁移。

## 7. Deterministic Embedding Boundary

### 7.1 Test provider

在 `rag-admin` test source 中通过 `@TestConfiguration` 注册唯一 `EmbeddingProvider`：

- provider/model name 固定为 `deterministic-test`；
- 固定维度，例如 64；
- 对 UTF-8 文本做稳定 normalization/tokenization；
- 每个 token 通过稳定 hash 映射到 bucket 与 sign，累加后做 L2 normalization；
- 相同输入跨进程得到相同向量，不使用随机数、时钟、网络或本机模型；
- 暴露仅供测试断言的调用计数，证明索引和查询均走到该 provider。

不能使用“所有文本都返回同一个常量向量”，否则检索排序没有证明力。

### 7.2 Fixture

至少上传两个合成 TXT 文档：

- target fixture：包含唯一 token 与目标事实；
- distractor fixture：长度相近但不包含唯一 token。

查询复用目标唯一 token。断言 target chunk 排在结果中且关联正确 `documentId`；同时断言 provider 调用计数覆盖文档索引和 query embedding。

该 fixture 只证明 plumbing 与确定性排序，不报告 Recall/MRR，不进入正式 evaluation dataset。

## 8. Happy-path Sequence

单个测试按以下顺序执行：

1. Spring 启动时 Flyway 建库，C2 bootstrap 用合成凭据创建数据库 ADMIN。
2. `POST /auth/login`，断言 access/refresh token 非空且响应不泄露密码。
3. 携带 Bearer token 调用 `POST /api/knowledge-bases` 创建私有知识库。
4. 两次调用 `POST /api/knowledge-bases/{id}/documents` 上传 target/distractor TXT，记录 `documentId` 与 `taskId`。
5. 有界轮询 `GET /api/tasks/{taskId}`：
   - `COMPLETED`：继续；
   - `FAILED/CANCELLED`：立即失败并输出稳定诊断；
   - 超时：失败，报告最后状态与耗时。
6. `GET /api/knowledge-bases/{id}/documents` 断言文档为 `COMPLETED` 且 chunkCount 大于 0。
7. `POST /api/qa/debug/retrieve`，断言：
   - `resultCount > 0`；
   - target document/chunk 存在并优先于 distractor；
   - 没有 `retrieve_failed`；
   - 确定性 provider 已参与索引和查询。
8. `DELETE /api/knowledge-bases/{kbId}/documents/{docId}` 删除 target；再次 retrieval 不得返回 target chunk。
9. 删除剩余文档和知识库；断言知识库不可访问，并验证向量 collection 被删除或不存在。

测试不调用 `/api/qa/ask`，因此不触发 LLM、generation、citation 或 QA history。

## 9. Failure Semantics of the Test Harness

C3 只定义“测试如何失败”，不定义产品在依赖故障下如何降级：

| 失败位置 | C3 测试行为 |
|---|---|
| Docker/镜像/容器 readiness | integration command 失败，不 skip |
| Flyway/bootstrap/登录 | 立即失败，输出阶段与错误类别 |
| Redis task status 不可读 | 立即失败，不 fallback 到进程内状态 |
| embedding 或 Milvus 写入失败 | 任务必须进入失败状态，测试失败 |
| 任务长期非终态 | 有界超时后失败 |
| retrieval 为空或返回错误文档 | 断言失败 |
| cleanup 失败 | 测试失败或在 teardown 记录 suppressed failure，不能掩盖主失败 |

Redis/Milvus 真正的业务降级合同仍属于 C4c/C4d，不在 C3 接受进 baseline。

## 10. Planned File Boundary

实现阶段预计只修改/新增：

- `rag-admin/pom.xml`：test-scope Testcontainers/Failsafe profile；
- `rag-admin/src/test/resources/application-c3-integration.yml`；
- `rag-admin/src/test/java/.../HappyPathIT.java`；
- 必要的 `rag-admin/src/test/java/.../DeterministicEmbeddingTestConfig.java` 与容器 helper；
- 本 change 的 `tasks.md`、`.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`；
- 若验证结果形成稳定事实，再同步 `docs/roadmap/technical-debt.md`。

不计划修改 production Java、Flyway migration、API/DTO、前端、评测脚本或 baseline spec。

如果为了测试必须修改 production seam，必须先解释现有 seam 为什么不足，判断是否触发 spec delta，并重新经过用户审批。

## 11. Verification Chain

实现阶段按以下顺序验证：

```powershell
# C3 专用真实依赖链路
mvn -q -pl rag-admin -am -Pc3-integration verify

# 既有 Java 回归
mvn -q test

# Python 评测脚本回归
python -B -m unittest discover -s scripts -p 'test_*.py'

# 敏感日志与文档范围
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode SensitiveLogs
git diff --check
```

没有前端改动时不重复运行前端 build，并在 AGENT_LOG 明确记录。若实现意外触及前端，则必须运行包含 `vue-tsc` 的正式 build。

验证记录必须包含：容器镜像、Docker/Testcontainers 版本、启动耗时、任务轮询耗时、测试数、failures/errors/skipped、Redis 内部降级日志、外部 provider 实际调用量和 Git HEAD。

## 12. Spec Delta Decision

无长期 spec delta。C3 test harness 只验证 accepted `rag-system` requirements，不新增正式运行契约。

触发重审的条件：

- 修改 `EmbeddingProvider` 或 `VectorStore` 生产接口；
- 修改正式 profile/provider selection；
- 修改 API/DTO、数据库 schema、任务状态机、权限或检索行为；
- 把 deterministic provider 暴露到非 test profile。

任一条件发生时，停止实现，新增 `rag-system` delta 并重新审批。

## 13. Rollback

- 删除 C3 Maven profile、integration-test test sources 和 test resource，即可恢复实现前测试结构。
- rollback 不修改生产数据或 migration；容器无持久 volume，由 Testcontainers 清理。
- 若测试执行中断，后续通过 Testcontainers/Ryuk 或明确的 label-scoped cleanup 清理本 change 容器；不得停止用户常驻 `rag-*` 容器。

## 14. Review Decisions Required

1. 是否接受 C3 使用独立 `-Pc3-integration verify` 命令，而不让默认 `mvn test` 强制依赖 Docker？
2. 是否接受真实 MySQL + Redis + Milvus 多容器，但 embedding 使用 test-scope token-hash 确定性 provider？
3. 是否接受 C3 只验证 `/api/qa/debug/retrieve`，不调用 LLM ask/generation/citation？
4. 是否接受 Docker/容器不可用时专用命令失败而非 skip？
5. 是否接受本 change 无长期 spec delta；若触及 production seam 再停下补 delta 并重审？

用户批准以上设计结论后，才按 `tasks.md` 进入实现。

## 15. Commit Responsibility

`用户手动提交`。Agent 不暂存、不提交、不 push。
