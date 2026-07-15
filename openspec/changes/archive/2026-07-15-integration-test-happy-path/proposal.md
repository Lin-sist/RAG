# Proposal: C3 Integration Test Happy Path

## Why

当前仓库的单元测试、性质测试和 C2 MySQL migration 测试已经形成较好的局部证据，但还不能用一条确定性命令证明真实主链路可用：

- `application-test.yml` 仍使用 H2 且关闭 Flyway，不能代表真实 MySQL schema/migration；
- 现有 Testcontainers 只覆盖 MySQL migration，没有联合 Redis 与 Milvus；
- Redis 性质测试在 Redis 不可用时会在测试内部跳过，Maven 全绿不等于真实 Redis 已覆盖；
- embedding 目前只有单元测试 mock，没有可供完整 Spring 主链路使用的确定性 test provider；
- 仓库外常驻的 `rag-mysql`、`rag-redis`、`rag-milvus` 依赖本机状态，不能作为可重复测试前提。

C2 已完成数据库认证并归档。按冻结蓝图，C3 应在进入 C4 故障语义和 C5 索引恢复前，先建立真实 happy-path 集成测试基线。

## 用户故事（大白话）

改之前，开发者看到测试全绿，仍不能确定“真实数据库用户登录后上传一个文档，系统是否真的完成索引、能检索到内容并正确删除”；本机 Redis 或 Milvus 缺失时，部分测试还可能自行跳过。改之后，只需执行一条明确命令，测试就会启动隔离的真实依赖，走完登录 → 创建知识库 → 上传 → 索引 → 检索 → 删除链路；任何关键依赖或步骤失败都会让命令失败，而不是悄悄跳过。

## Current Status

### `confirmed`

- C2 `database-backed-authentication` 已验收、接受进 baseline 并归档，`ACTIVE_TASK` 在本 change 启动前为 `IDLE`。
- 当前 `main` 与 `origin/main` 一致，启动 C3 前工作区干净。
- 登录、知识库创建、文档上传、任务状态查询、检索调试、文档删除和知识库删除 API 已存在。
- 仓库已使用 Testcontainers 1.19.3 与 MySQL 8.0.36；当前 Docker Engine 可运行真实 MySQL Testcontainers 测试。
- 当前 HEAD 的完整 Maven 测试为 45 suites / 201 tests / 0 failures / 0 errors / 0 skipped；Python unittest 33 tests 通过。
- `/api/qa/debug/retrieve` 只执行 retrieval，不调用 LLM、不写问答历史，适合作为 C3 检索验收入口。

### `partial`

- MySQL 已有真实 Testcontainers migration 证据，但应用级测试仍主要使用 H2 且关闭 Flyway。
- Redis 有单元/性质测试，但不可用时存在测试内部降级，没有真实容器联合证据。
- Milvus adapter 与本地 Docker Compose 服务已存在，但没有应用级 Testcontainers happy-path。
- embedding 有 mock 和测试内存实现，但没有隔离的 Spring integration-test provider。

### `planned`

- 建立独立的 C3 Maven integration-test 入口，启动隔离的 MySQL、Redis、etcd、MinIO 与 Milvus。
- 通过动态属性接入随机 host ports，启用真实 Flyway，并使用 test-scope 合成 bootstrap 用户完成登录。
- 新增固定算法、固定维度的本地确定性 embedding provider，显式禁用全部真实 embedding providers。
- 通过 HTTP API 完整验证登录 → 创建知识库 → 上传 → 等待索引 → retrieval → 删除文档/知识库。
- 将容器启动失败、任务失败、轮询超时和 retrieval 缺失都作为明确测试失败，不使用 `disabledWithoutDocker` 或内部自跳过。

### `out_of_scope`

- LLM ask、SSE、generation、citation、no-answer 或 judge 质量验证。
- Redis/Milvus/LLM 故障注入与降级语义；分别留给 C4b/C4c/C4d。
- 索引输入持久化、孤儿任务协调、中断恢复；留给 C5a/C5b。
- 真实 embedding/rerank provider 的协议、归因或收益结论；留给后续 provider/A-B change。
- 修改 API、DTO、持久化模型、生产 provider 接口、检索参数、分块策略、prompt 或评测指标。
- 复用或停止本机常驻 `rag-*` Docker Compose 容器。

### `unknown`

- Milvus 多容器栈在不同开发机/CI 上的首次拉镜像时间、内存占用和稳定启动上限，需要在实现阶段用真实运行数据确定超时预算。
- Redis 的最终精确镜像版本需在实现前完成兼容性验证并固定；不得使用 `latest` 或只固定 major 的漂移标签作为验收终态。
- 当前仓库未定义 CI Docker 资源额度；本 change 只承诺本地一条命令可重复，CI 接入需在资源事实明确后决定。

## Scope

1. 在 `rag-admin` test scope 中建立独立的 C3 integration-test harness。
2. 使用 Testcontainers 启动隔离依赖：
   - MySQL 8.0.36；
   - 精确版本 Redis；
   - etcd + MinIO + Milvus 2.3.4 standalone。
3. 使用随机 host ports、独立 network alias、无固定 container name、无持久 volume，并由 Testcontainers 自动清理。
4. 新增 test-scope 确定性 embedding provider；真实 embedding providers 全部关闭。
5. 使用真实 Spring Security、JWT、Redis session、Flyway、MySQL mapper、异步索引、Milvus 与 BM25/RRF 检索链路。
6. 提供一条明确的 Maven 命令运行 C3 happy-path，并记录容器/任务超时与失败证据。

## Non-goals

- 不把测试 fake/local embedding 接入生产 profile 或生产 provider registry。
- 不为了测试修改生产检索、分块、rerank、拒答或权限行为。
- 不新增注册/用户管理 API，也不改变 C2 bootstrap 生产契约。
- 不把本 change 包装成完整生产 readiness、性能测试或高可用验证。
- 不因测试便利连接本机固定 `3306`、`6379`、`19530` 端口。

## Spec Delta Decision

本草案选择 **不创建长期 spec delta**。

理由：C3 只增加 test-scope harness、确定性 embedding fixture 与验证入口，不修改生产 provider 接口、正式运行语义、API/DTO、持久化模型或 accepted RAG 行为。现有 `rag-system` baseline 已要求成功索引后 chunk 可检索，并要求数据库用户认证；C3 负责为既有契约补真实联合证据，而不是新增产品能力。

如果实现阶段发现必须修改生产 provider 接口或正式运行语义，必须停止实现、补充 `specs/rag-system/spec.md` delta 并重新请用户审批，不能沿用本次“无 delta”决定直接扩 scope。

## External Calls

- embedding：预计 0 次业务外部调用；只使用 JVM 内 test provider。
- rerank：真实 model 调用 0 次；使用当前默认 heuristic。
- ask / LLM：0 次；检索验收使用 `/api/qa/debug/retrieve`。
- judge：0 次。
- 业务数据出站：0；只使用合成测试用户、合成 TXT fixture 和本机 Docker network。
- 模型、限流与费用：无外部模型、无 provider 限流、模型费用为 0，依据是实现设计不发起业务 provider 请求，而不是任何“免费模型”假设。
- 基础设施镜像：实现或首次运行可能从镜像仓库拉取已固定版本的 MySQL、Redis、etcd、MinIO、Milvus 镜像；只传输镜像层，不上传业务数据。实际拉取前按现有依赖解析授权执行，并记录镜像身份与结果。

## Acceptance Evidence

1. 专用 C3 命令从干净状态启动隔离容器并成功退出，重复运行不依赖常驻容器或固定 host ports。
2. integration test 不使用 `disabledWithoutDocker`，Docker 不可用时命令明确失败而非跳过。
3. Flyway 在 Testcontainers MySQL 上从空库迁移到最新版本；bootstrap 使用合成 test-scope 凭据创建可登录数据库用户。
4. HTTP 链路验证：
   - 登录成功并获得 token；
   - 创建私有知识库；
   - 上传合成 TXT fixture 并取得 `documentId`、`taskId`；
   - 在有界轮询内任务成功且文档状态为 `COMPLETED`；
   - debug retrieve 返回目标文档/chunk，确定性 embedding provider 确实被调用；
   - 删除文档后 retrieval 不再返回该 chunk；
   - 删除知识库后资源不可访问，相关向量 collection 被清理。
5. 真实 embedding/LLM/rerank model/judge 调用均为 0，测试日志和失败信息不包含 secret 或合成密码。
6. `mvn -q test`、Python unittest、SensitiveLogs、`git diff --check` 与 change 结构检查通过；未改前端则不重复运行前端 build，并明确记录原因。

## Risks

- Milvus standalone 需要 etcd 与 MinIO，多容器启动可能使本地测试变慢或受资源限制影响。
- 异步索引完成时间存在机器差异；轮询必须有固定上限、短间隔和失败诊断，不能无限等待。
- 使用常量向量会制造虚假检索结论；确定性 provider 必须根据 token 生成可区分、归一化向量，并用至少一个干扰 fixture 证明排序不是偶然。
- 测试配置若覆盖范围过大，可能污染现有 H2 单测；C3 应使用独立 Spring profile 和独立 Maven integration-test 入口。
- 直接复用 `docker-compose.yml` 的固定 container names/host ports 会与用户常驻容器冲突，因此禁止采用。

## Commit Responsibility

`用户手动提交`。Agent 不暂存、不提交、不 push；本规格阶段只生成草案、激活 change 并等待用户审批。
