# Tasks: C3 Integration Test Happy Path

## Phase 0：启动与规格草案

- [x] 按顺序读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、`openspec/project.md`、相关 baseline spec、冻结蓝图和当前测试/配置事实。
- [x] 确认 C2 已归档、`main` 与 `origin/main` 一致、工作区无未提交改动，C3 顺序前置满足。
- [x] 将能力状态分类为 `confirmed / partial / planned / out_of_scope / unknown`。
- [x] 创建 proposal，包含“改前坏事 → 改后不同”的用户故事。
- [x] 创建 design 与本 tasks，锁定真实依赖 + test-scope 确定性 embedding 边界。
- [x] 明确本 change 不修改生产契约，因此当前无需长期 spec delta；若实现触及 production seam 必须停下补 delta 并重审。
- [x] 声明 embedding/rerank/judge/ask 业务外部调用量均为 0，草案不授权业务外调。
- [x] 明确提交责任为“用户手动提交”。
- [x] 将 `.ai/ACTIVE_TASK.md` 指向本 change。
- [x] 完成 change 结构、必需标题/字段、Markdown 链接、范围和 `git diff --check` 验证。
- [x] 用户审阅并明确批准 proposal、design 与无 spec delta 决定。

## Phase 1：集成测试运行骨架

- [x] RED：增加最小 `*IT`，证明没有专用 profile/harness 时无法启动真实联合依赖链路。
- [x] 在 `rag-admin` 增加独立 `c3-integration` Maven/Failsafe 入口；默认 `mvn test` 不强制依赖 Docker。
- [x] 建立 MySQL、Redis、etcd、MinIO、Milvus Testcontainers 拓扑，使用随机 host ports、隔离 network、无固定 container name/volume/reuse。
- [x] 固定全部容器精确镜像版本并记录选择依据；不得使用 `latest` 或漂移 major tag 作为验收终态。
- [x] 配置 readiness 与有界 startup timeout；Docker/容器不可用时命令失败而非 skip。
- [x] 新增独立 `c3-integration` Spring profile，通过动态属性接入容器，启用真实 Flyway。
- [x] 验证不读取、不停止、不复用本机常驻 `rag-*` 容器。
- [x] 运行最小 C3 命令并记录容器身份、启动耗时和真实结果。
- [x] 更新本 tasks 与 `.ai/AGENT_LOG.md`，建议中文提交但不代用户提交。

## Phase 2：确定性 embedding 边界（TDD）

- [x] RED：无业务 provider 凭据时，完整 Spring 索引/检索链路不能稳定选择可用 embedding provider。
- [x] 在 test source 注册唯一 `deterministic-test` provider，固定 token normalization/hash/vector dimension/L2 normalization 算法。
- [x] 显式关闭 OpenAI/Qwen/BGE 等真实 providers，保持 model reranker 关闭、heuristic 启用。
- [x] 使用 target + distractor fixtures 验证向量可区分；禁止所有文本返回同一常量向量。
- [x] 断言 provider 调用覆盖文档索引和 query embedding，且真实 provider 调用量为 0。
- [x] 确认 provider 只存在于 `c3-integration` test scope，不进入 production jar/profile。
- [x] 更新本 tasks 与 `.ai/AGENT_LOG.md`。

## Phase 3：HTTP happy-path（TDD）

- [x] RED：先写登录 → 创建知识库 → 上传 → 任务轮询 → retrieval → 删除的端到端断言，并确认旧基线无法提供该证据。
- [x] 使用真实 Flyway schema 和 C2 合成 bootstrap 用户完成 `/auth/login`。
- [x] 创建私有知识库并上传 target/distractor TXT fixtures，记录 `documentId` 与 `taskId`。
- [x] 有界轮询任务状态；FAILED/CANCELLED/timeout 必须明确失败，不无限等待或 self-skip。
- [x] 断言文档 `COMPLETED`、chunkCount > 0，MySQL、Redis、Milvus 与 BM25/RRF 链路均实际参与。
- [x] 调用 `/api/qa/debug/retrieve`，断言 target document/chunk 可检索且排序不是常量向量偶然结果。
- [x] 删除 target 后断言 retrieval 不再返回其 chunk；删除剩余资源并确认知识库与向量 collection 清理。
- [x] 断言未调用 `/api/qa/ask`，没有 LLM/generation/citation/history 外部路径。
- [x] 扫描测试日志和失败信息，不出现 JWT secret、数据库/Redis 密码、bootstrap 密码或 token。
- [x] 更新本 tasks 与 `.ai/AGENT_LOG.md`。

## Phase 4：完整验证与验收

- [x] `mvn -q -pl rag-admin -am -Pc3-integration verify` 通过，记录 integration tests/failures/errors/skipped、容器与耗时。
- [x] 重复运行隔离 C3 happy-path 通过，证明不依赖固定端口、常驻容器或上一次测试数据。
- [x] `mvn -q test` 通过，并记录 suites/tests/failures/errors/skipped 及 Redis 内部降级信息。
- [x] `python -B -m unittest discover -s scripts -p 'test_*.py'` 通过。
- [x] SensitiveLogs 门禁通过。
- [x] 无前端改动时明确跳过前端 build；若触及前端则运行包含 `vue-tsc` 的正式 build。
- [x] `git diff --check`、change 结构、Markdown 相对链接和计划文件范围检查通过。
- [x] 确认 embedding、rerank model、judge、ask/LLM 业务外部调用量均为 0。
- [ ] 用户明确确认实现验收通过。
- [ ] 无 spec delta 时不修改 baseline；将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE` 并经用户确认后归档 change。

## Guardrails

- 用户已批准草案；实现必须保持 proposal、design 与“无长期 spec delta”的既定边界。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。
- 不修改 production API、DTO、Flyway migration、provider 接口、RAG pipeline、chunking、prompt、citation/no-answer 或评测指标。
- 不连接本机固定 `3306/6379/19530` 作为验收前提，不停止或复用常驻 `rag-*` 容器。
- 不使用 `disabledWithoutDocker`、JUnit assumptions 或 catch-and-return 把 C3 专用命令降级为 skip。
- 不为 fixture 定制生产 prompt、检索、分块、rerank 或拒答规则。
- 未取得单独授权不得执行业务外部调用、push、PR、发布或部署。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push。
