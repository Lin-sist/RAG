# Agent Log

> 只追加执行证据，不改写历史。需求、契约和设计分别以 Active OpenSpec change 与 baseline spec 为准。

## 2026-07-12｜历史补录：仓库清理与文档迁移

- 范围：清理失效报告、旧脚本、Kiro 初始规格、旧维护计划和会话式交接稿；重组文档真相源。
- 已完成：
  - 第一批高置信度清理已提交：`34612b4 chore(仓库): 清理过期文档脚本与失效评测报告`。
  - 第二批将当前架构、技术债、前端现状、学习路线和优化文档迁入稳定目录，已提交：`e7014e8 docs(治理): 迁移旧规格并统一项目文档真相源`。
- 验证：旧路径扫描无残留；Markdown 相对链接无断链；Python 25 tests 通过；`git diff --check` 通过。
- 说明：这是对治理入口建立前工作的补录，不替代 Git 历史。

## 2026-07-12｜2026-07-12-repository-governance-bootstrap

- 类型：文档与治理。
- 范围：`AGENTS.md`、`.ai/`、`openspec/`、Copilot 指令入口及相关文档索引。
- 决策：
  - `AGENTS.md` 负责协作规则。
  - `.ai/ACTIVE_TASK.md` 只指向唯一活动 change。
  - `.ai/AGENT_LOG.md` 只追加执行证据。
  - OpenSpec baseline specs 负责已接受能力契约；重大变更进入独立 change。
  - 只读任务和小修不强制创建完整 OpenSpec change。
- 验证：目录契约、spec 标题层级、YAML 必需字段、引用、Markdown 链接与 `git diff --check` 均通过；本机未发现 `openspec` CLI，Python 环境也没有 PyYAML，因此未执行官方 CLI/schema validate。
- 业务代码：未修改。
- Commit：`pending`。

- 剩余风险：需要在第一次真实 change 中检验模板粒度，并根据实际协作成本微调规则。

## 2026-07-14｜迭代蓝图冻结与协作工作流固化

- 类型：治理 / 文档。
- 范围：`docs/roadmap/iteration-blueprint.md`、`docs/workflow/vibecoding-playbook.md`、`.ai/AGENT_LOG.md`。
- 事件一：迭代蓝图 v5 正式冻结，作为后续所有 change 的方向基线。
- 事件二：新增 `docs/workflow/vibecoding-playbook.md`，固化协作工作流（事前闸门 / git 锚点 / 禁止清单 / 报告事实验收 / bug 处理规程）。
- 验证：核对蓝图仅修改标题、状态日期与顶部收尾措辞；核对 Playbook 文件存在且正文完整；提交前检查工作区与暂存区范围。
- 跳过项：按用户明确边界未运行测试，未进行 provider 或网络外部调用。
- 范围安全：未创建、修改或归档任何 `openspec/changes` 目录；`.ai/ACTIVE_TASK.md` 保持 `IDLE`；未修改代码或 spec。
- 说明：后续将从 B0 `sensitive-log-redaction`（Type B）正式开工。
- 剩余风险：无；具体 B0 实施范围仍以开工时的聚焦检查为准。
- Commit：`pending`。

## 2026-07-14｜B0 sensitive-log-redaction

- 类型：Type B 小范围维护；未创建 OpenSpec change，`.ai/ACTIVE_TASK.md` 保持 `IDLE`。
- 范围与修改文件：
  - `rag-admin`：`AuthController.java`、`KnowledgeBaseController.java`、`QAController.java`、`DocumentIndexingServiceImpl.java`、`DocumentServiceImpl.java`、`KeywordIndexBootstrap.java`、`KnowledgeBaseServiceImpl.java`、`QAHistoryServiceImpl.java`、`application.yml`。
  - `rag-auth`：`AuthExceptionHandler.java`、`JwtAuthenticationFilter.java`、`JwtAccessDeniedHandler.java`、`JwtTokenProvider.java`、`TokenBlacklistService.java`、`AuthServiceImpl.java`、`UserDetailsServiceImpl.java`。
  - `rag-common`：`RedisAsyncTaskManager.java`、`DefaultDocumentIndexConsumer.java`、`GlobalExceptionHandler.java`、`IdempotencyExceptionHandler.java`、`RedisIdempotencyHandler.java`、`ApiResponseAdvice.java`、`RateLimitExceptionHandler.java`、`RateLimitInterceptor.java`、`SlidingWindowRateLimiter.java`、`RequestObservationFilter.java`。
  - `rag-core`：BGE/OpenAI/Qwen embedding provider、`EmbeddingServiceImpl.java`、`AnswerGeneratorImpl.java`、`PromptBuilder.java`、`QueryEngineImpl.java`、`ModelReranker.java`、`RerankerRegistry.java`、`RAGServiceImpl.java`、`MilvusVectorStore.java`。
  - 前端与脚本：`UserProfile.vue`、`SettingsModal.vue`、`run_rag_eval.py`、`run_reproducible_rag_eval.py`、`test_run_reproducible_rag_eval.py`。
- 已确认事实与关键决策：普通日志不再写入 question/query/query variants、prompt/context/snippet、知识库名、上传文件名、用户名、密码、API key、客户端幂等/限流 key、provider response body 或异常 message/throwable；保留 traceId、内部资源 ID、计数、耗时、score、provider/model/status/errorType。MyBatis mapper 与 Spring Security 日志定点降至 `INFO`，避免 DEBUG 隐式输出 SQL 参数和认证对象。
- 大白话：改前用户提问、文件名、密码或模型返回内容可能直接出现在普通日志里；改后普通日志只保留排障需要的内部 ID、统计和错误类别，不再泄露这些私密内容。
- 验证：
  - `python -B -m unittest discover -s scripts -p 'test_*.py'`：25 tests 通过。
  - 前端正式 build 按 `vue-tsc -b && vite build` 顺序执行，在既有 `TS5103: Invalid value for '--ignoreDeprecations'` 处失败，未进入 Vite；补充 `vue-tsc --noEmit --ignoreDeprecations 5.0` 通过。
  - 敏感日志静态扫描：未命中日志中的 `getResponseBodyAsString`、`getMessage()`、敏感对象 console 输出、评测 question/密码/文件名 stdout。
  - `git diff --check`：通过。
- 跳过项及原因：`mvn -q test` 因本地缺少 `spring-boot-starter-parent:3.2.1` 且下载需访问 Maven Central；遵守本任务“不做外部调用”边界，联网审批被拒后未继续执行。未运行任何 provider、embedding、rerank、ask 或 judge 调用。
- 范围安全：未修改接口、DTO、持久化模型、检索/生成逻辑、spec、`openspec/changes`、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；验证期间 pnpm 产生的 `.pnpm-store` 和 `node_modules/.ignored` 已清理并恢复原依赖目录。
- 剩余风险：Java 编译/测试仍待本地 Maven parent 可用后补跑；前端正式 build 的 TS5103 属既有 B1 债务，本次未越界修复。
- Commit：`pending`。

## 2026-07-14｜B0 sensitive-log-redaction 验证补充

- 用户在本地手工执行 `mvn -q clean compile` 与 `mvn -q test`，两条命令均完整通过，`LASTEXITCODE=0`。
- 结论：B0 原记录中“Java 编译/测试待补跑”的剩余风险已解除。
- Commit：`61a37472ecd41c8473306c0b2bdf9ab24a764a4b`。

## 2026-07-14｜B1 frontend-build-baseline-fix

- 类型：Type B 小范围维护；未创建 OpenSpec change，`.ai/ACTIVE_TASK.md` 保持 `IDLE`。
- 范围与修改文件：`rag-frontend/tsconfig.json`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md`、`docs/开发文档/前端文档/frontend-current-state.md`、`openspec/project.md`、`.ai/AGENT_LOG.md`。
- 已确认事实与关键决策：TypeScript 5.7.3 不接受面向 6.0 的 `ignoreDeprecations: "6.0"`；将其定点改为兼容值 `"5.0"`，不升级 TypeScript、Vue、Vite、vue-tsc 或其他依赖。
- 大白话：改前正式构建在类型检查阶段直接报 TS5103，产不出部署包；改后类型检查和 Vite 打包都能跑完并生成 `dist/`。
- 验证：按 `package.json` 的正式构建顺序执行 `vue-tsc -b` 与 `vite build`，退出码为 0；Vite 转换 3334 个模块并成功生成 `dist/`；`git diff --check` 通过。
- 跳过项及原因：未修改 Java 或 Python，故未重复运行相应测试；未执行 `npm install`，未调用 provider 或其他外部服务。
- 范围安全：未修改 UI、接口、业务逻辑、依赖版本、baseline spec 或 `openspec/changes`；部署和演示链接保持 out_of_scope。
- 剩余风险：Vite 报告单个压缩前 chunk 大于 500 kB 的性能警告，但不影响本次构建通过；代码分包优化不属于 B1。
- Commit：`pending`。

## 2026-07-14｜B1 frontend-build-baseline-fix 提交补录

- Commit：`596cefa045226496d2f7ee713f301557c0c1b0d4`。
- 结论：B1 已完成中文提交，工作区在后续只读复核时保持干净。

## 2026-07-14｜v4 剩余项关闭裁决

- 类型：纯文档收口；未创建 OpenSpec change，`.ai/ACTIVE_TASK.md` 保持 `IDLE`。
- 范围与修改文件：`docs/optimization/v4/plan.md`、`docs/optimization/README.md`、`docs/roadmap/technical-debt.md`、`.ai/AGENT_LOG.md`。
- 已确认事实与关键决策：v4 以“部分完成”关闭；Stage 1 已完成，Stage 2 条件跳过，Stage 4 已完成；未执行的 Stage 3 转入技术债 P1，未来独立立项；不再补写会暗示 v4 全部完成的最终总报告。
- 大白话：改前旧 v4 文档还像一张待继续执行的任务单，容易与冻结蓝图抢下一步；改后它只保留历史证据，后续统一按蓝图和 Active OpenSpec 推进。
- 验证：扫描 v4 索引、计划状态与技术债映射；运行 Markdown 相对链接检查和 `git diff --check`。
- 跳过项及原因：纯文档裁决，不运行代码测试或外部调用。
- 范围安全：未修改业务代码、baseline spec、`openspec/changes` 或 `.ai/ACTIVE_TASK.md`；未改写 v4 已有阶段结果和指标。
- 剩余风险：分块结构专项仍未执行，但已明确进入 P1 技术债，不阻塞冻结蓝图的 C1。
- Commit：`pending`。

## 2026-07-14｜v4 剩余项关闭裁决提交补录

- Commit：`0f47d8cbb17eeb38363d09e7b59edb156240b8b7`。
- 结论：v4 关闭裁决已完成中文提交，后续执行入口统一回到冻结蓝图和 Active OpenSpec。

## 2026-07-14｜local-quality-gates

- 类型：Type B 小范围维护；未创建 OpenSpec change，`.ai/ACTIVE_TASK.md` 保持 `IDLE`。
- 范围与修改文件：`.gitignore`、`scripts/check_sensitive_logs.py`、`scripts/test_check_sensitive_logs.py`、`scripts/run_local_quality_gates.ps1`、`.ai/AGENT_LOG.md`。
- 已确认事实与关键决策：新增敏感日志启发式扫描和含 `vue-tsc` 的正式前端构建门禁；工具链预检只报告 Git/Java/Maven/Python/Node/npm 与前端依赖可用性，不安装工具、不自动接入 CI 或 pre-commit。
- 大白话：改前每次都要临时拼扫描和构建命令，还容易把 PATH/依赖问题误判成代码问题；改后先跑一个预检，再按需运行两项可重复门禁。
- 验证：Python unittest 共 31 tests 通过；`Preflight` 正确报告 Git/Java/Maven/Python/Node 可用、npm 缺失及 direct vue-tsc/vite fallback 可用；`SensitiveLogs` 扫描 262 个运行代码文件通过；`FrontendBuild` 与聚合 `All` 均完成 `vue-tsc -b` 和 Vite 打包，转换 3334 个模块；`mvn -q test` 首次在沙箱内因 Maven Central 访问权限失败，按授权转到沙箱外后完整通过、退出码 0；`git diff --check` 通过。
- 跳过项及原因：不运行 provider、embedding、rerank、ask 或 judge；本任务不涉及真实业务外部调用。
- 范围安全：未修改业务逻辑、接口、依赖版本、baseline spec、`openspec/changes` 或 `.ai/ACTIVE_TASK.md`；门禁不自动阻塞 C1。
- 剩余风险：敏感日志扫描属于启发式回归防线，不能替代人工审计；Vite 既有大 chunk 警告仍不属于本任务。
- Commit：`pending`。

## 2026-07-14｜local-quality-gates 提交补录

- Commit：`a1bc63740ecf8c06fa6515d31b360eabdd2b934b`。
- 结论：本地工具链预检、敏感日志门禁和正式前端构建门禁已完成中文提交。

## 2026-07-14｜协作硬约定加固

- 类型：Type B 治理文档维护；未创建 OpenSpec change，`.ai/ACTIVE_TASK.md` 保持 `IDLE`。
- 范围与修改文件：`AGENTS.md`、`docs/workflow/vibecoding-playbook.md`、`.ai/AGENT_LOG.md`。
- 已确认事实与关键决策：提交责任必须在事前闸门二选一；未明确时默认用户手动提交；Agent 可直接执行现有 Maven/npm 验证及已声明依赖的正常解析，但不得借此新增依赖、发布、部署或执行 RAG 业务外部调用；AGENT_LOG 采用执行记录 `pending` + 后续只追加真实 hash 的两段式。
- 大白话：改前谁提交、谁跑 Maven/npm、什么时候补 commit hash 容易临时确认；改后这些动作在开工时就有默认答案，减少来回等待和日志不一致。
- 验证：扫描 AGENTS 与 Playbook 的规则一致性、Markdown 相对链接和 `git diff --check`；不重复运行已在上一切片通过的代码测试。
- 跳过项及原因：本切片只修改治理文档，不运行 provider 或其他业务外部调用。
- 范围安全：未修改业务代码、依赖、baseline spec、`openspec/changes` 或 `.ai/ACTIVE_TASK.md`。
- 剩余风险：当前治理记录在本提交内仍按规则写 `pending`，其真实 hash 将在下一次仓库写操作开始时追加。
- Commit：`pending`。

## 2026-07-14｜协作硬约定加固提交补录

- Commit：`f19065a810ae9039948a253ed666a1d9154ce094`。
- 结论：验证授权、事前提交责任和 AGENT_LOG 两段式记录规则已完成中文提交。

## 2026-07-14｜C1 jwt-secret-production-guard 启动与规格草案

- 类型：Type C 重大变更的规格阶段；change 已声明为 `ACTIVE`，业务代码尚未开始。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`、`openspec/changes/2026-07-14-jwt-secret-production-guard/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`。
- 已确认事实与关键决策：B0、B1、v4 关闭裁决与本地质量门禁已完成；当前 tracked JWT fallback 长度足以通过 JJWT，系统缺少 prod 已知默认值守卫；C1 只补精确的 default/blank/misconfiguration fail-fast，不重复 JJWT 基于 UTF-8 bytes 的 key-strength 校验。
- 大白话：改前生产环境忘配 `JWT_SECRET` 仍可能用公开默认密钥启动；改后 `prod` 命中默认、空白或明确误配置就直接启动失败，且错误不泄露 secret。
- 外部调用：embedding/rerank/judge/ask 预计与实际调用量均为 0；无业务数据出站、无模型、无限流风险、费用为 0，依据是未发生调用而非 NVIDIA 免费假设。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。
- 验证：待执行 change 目录契约、必需标题/字段、Markdown 相对链接、业务代码零改动与 `git diff --check` 检查。
- 跳过项及原因：按用户要求当前只交 proposal/design 审查，未运行 Maven 测试，未修改业务代码、配置或测试；实现与运行验证须在用户批准草案后进行。
- 范围安全：未修改 Java、Vue、数据库、依赖、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未执行 provider、部署、发布或网络业务调用。
- 剩余风险：prod 精确拒绝集合和 guard 接线方式仍待用户审定；用户批准前不得进入实现。
- Commit：`pending`。

## 2026-07-14｜C1 规格草案验证补充

- 验证结果：change 四个必需 artifact 均存在；proposal 的 Why/用户故事/Scope/Non-goals/Acceptance/外部调用/提交责任、design 的数据流与回滚、tasks 的切片、spec delta 的 requirement/scenario、ACTIVE 状态与 change 指针全部通过结构检查。
- 文档检查：Markdown 相对链接检查通过；`git diff --check` 通过；当前环境未发现 `openspec` CLI，因此未执行官方 CLI/schema validate。
- 范围检查：工作区仅包含 `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md` 与本 C1 change 的四个草案文件；业务代码、配置、测试、依赖均为零改动。
- 外部调用复核：embedding/rerank/judge/ask 实际调用量均为 0，无数据出站或费用。
- Commit：`pending`；提交责任仍为用户手动提交。

## 2026-07-14｜C1 规格草案提交补录

- Commit：`2f58c6a6e0921af701d93b890056862047310572`。
- 结论：C1 proposal、design、tasks、spec delta 与 ACTIVE_TASK 已由用户手动完成中文提交；用户随后明确批准草案，允许按 TDD 进入实现。

## 2026-07-14｜C1 jwt-secret-production-guard 实现与验证

- 类型：Type C 重大变更实现；change 保持 `ACTIVE`，等待用户验收确认。
- 范围与修改文件：`rag-auth` 新增 `JwtSecretProductionGuard.java`、两个 C1 测试类，调整 `JwtTokenProvider.java` 与两个既有直接构造测试；同步 `README.md`、C1 `design.md`/`tasks.md`、`.ai/ACTIVE_TASK.md` 与本日志。
- 已确认事实与关键决策：仅当 active profiles 包含精确 `prod` 时执行自定义守卫；精确拒绝 `blank`、tracked `known-default`、`surrounding-whitespace`、完整 `unresolved-placeholder`；合法值不 trim、不 normalize，原样以 UTF-8 bytes 交给 JJWT；自定义守卫不复制 JJWT key-strength 规则。
- TDD 证据：首个 RED 因 guard 不存在而编译失败；blank RED 暴露原行为为 NPE/WeakKeyException；首尾空白 RED 暴露原行为会放行；占位符 RED 暴露原行为只触发 JJWT 弱 key。每个切片加入最小实现后聚焦测试转 GREEN。
- 验证：`JwtSecretProductionGuardTest` 11 tests、`JwtSecretProductionGuardContextTest` 2 tests 全部通过；`mvn -q -pl rag-auth -am test` 通过；`mvn -q test` 通过，Surefire 汇总 41 reports / 168 tests / 0 failures / 0 errors / 0 skipped；敏感日志门禁扫描 263 个源码文件通过；构造器引用、职责边界、Markdown 相对链接和 `git diff --check` 检查通过。
- 跳过项及原因：未做真实部署环境的完整 `prod` 应用启动，因为它需要数据库/Redis/Milvus 等部署基础设施且不属于 C1；以只装载 JWT 配置、guard 和 provider 的最小 Spring context 覆盖启动拒绝/允许语义。完整测试日志中的 Redis unavailable 为既有属性测试的条件分支提示，不影响 Maven 成功结果。
- 外部调用：embedding/rerank/judge/ask 实际调用量均为 0；无业务数据出站、无模型、无限流风险、费用为 0。Maven 仅解析仓库已声明依赖并执行本地测试。
- 范围安全：未修改 API、DTO、数据库、认证数据源、token shape/过期策略、RAG pipeline、评测指标、依赖版本、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、部署或发布。
- 剩余风险：尚未在真实部署编排中验证 `prod` profile 与 `JWT_SECRET` 注入方式；secret manager 与轮换仍为明确 out_of_scope。需用户验收后才能将 ACTIVE_TASK 置为 `IDLE` 并归档 change。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-17｜C5b 规划批准与 TDD 实现启动

- 用户决策：用户明确批准 C5b proposal、design、13 条决策记录、tasks 与 `rag-system` spec delta；据此进入实现阶段。
- 已确认边界：MySQL `async_task` durable ledger、Redis 可重建投影、DB CAS lease/heartbeat、只恢复 SAFE_PRE_VECTOR 与 VECTOR_CONFIRMED 收尾、VECTOR_IN_FLIGHT/outcome unknown/legacy/mismatch 进入 `RECONCILIATION_REQUIRED`、新任务 deterministic IDs、cleanup-only reconciliation、auto resume 默认关闭。
- 执行方式：使用 `tdd` skill，按 durable ledger acceptance、lease/checkpoint、安全 resume、projection/legacy/cleanup 的垂直切片逐个 RED → GREEN → REFACTOR。
- 外部调用：实现与验证继续禁止真实 embedding、rerank、judge、ask/LLM；只允许确定性 stub、合成数据和隔离 Testcontainers。真实 provider 下批量 resume 仍需单独授权。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。Commit：`pending`。

## 2026-07-14｜C1 实现提交补录

- Commit：`528a2cb16e11a54539c1ff602c62c74670026578`。
- 结论：生产 JWT secret 启动守卫、TDD 测试、README 与 C1 实现证据已由用户手动完成中文提交；用户随后明确确认验收通过并要求收口 C1。

## 2026-07-14｜C1 jwt-secret-production-guard 验收收口与归档

- 类型：Type C 验收收口；用户已明确确认实现验收通过。
- 范围与修改文件：将 C1 delta 接受进 `openspec/specs/rag-system/spec.md`；将 change 移至 `openspec/changes/archive/2026-07-14-jwt-secret-production-guard/`；更新 `.ai/ACTIVE_TASK.md` 为 `IDLE`；补齐 archived `tasks.md` 与本日志。
- 已确认事实与关键决策：实现提交为 `528a2cb16e11a54539c1ff602c62c74670026578`；归档目录保持 change 原有日期前缀，不重复添加日期；accepted baseline 中的生产 JWT secret requirement/scenarios 与 archived delta 逐字一致。
- 验证：归档四个必需 artifact 均存在；活动 changes 目录无未归档 change；C1 tasks 无未勾选项；ACTIVE_TASK 为 `IDLE` 且 Last Completed 指向真实 archive 路径；Markdown 相对链接与 `git diff --check` 通过。
- 跳过项及原因：本轮仅做 spec 接受与治理归档，未修改 Java、配置或测试，因此未重复运行已在实现提交前通过的 `mvn -q test`（168 tests / 0 failures / 0 errors）与敏感日志门禁。
- 外部调用：embedding/rerank/judge/ask 实际调用量均为 0；无业务数据出站、模型、限流或费用。
- 范围安全：未修改业务代码、API、DTO、数据库、依赖、RAG pipeline、评测指标、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、部署或发布。
- 剩余风险：真实部署编排中的 `prod` profile 与 `JWT_SECRET` 注入仍需在未来部署验收中验证；secret manager 与轮换仍为 out_of_scope，不阻塞 C1 完成。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-14｜C2 database-backed-authentication 启动与规格草案

- 类型：Type C 重大变更的规格阶段；change 已声明为 `ACTIVE`，业务实现尚未开始。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`、`openspec/changes/2026-07-14-database-backed-authentication/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`。
- 已确认事实与关键决策：C1 已归档且 C2 顺序前置满足；当前认证仍使用内存 `admin/admin123` 与 `user/user123`；数据库已有 user/role/user_role 表和 V3 默认 admin 种子；历史 Flyway migration 不回改。草案建议用新前向 migration 精确隔离 known seed 并保留 user ID，bootstrap 默认关闭、外部注入、事务性、幂等且不得覆盖正常用户。
- 大白话：改前应用只认代码里写死的账号，数据库禁用和角色变化不生效；改后登录与刷新以数据库真实状态为准，没有显式 bootstrap 就不会出现可登录默认账号。
- 能力分类：`confirmed` 为 schema/种子/内存认证/refresh 重载入口；`partial` 为 H2 test profile 关闭 Flyway、缺真实 MySQL migration 证据；`planned` 为数据库认证、bootstrap、前向迁移和固定凭据清理；`out_of_scope` 为用户管理 API、实时 access token 撤权与 C3；`unknown` 为部署平台最终的 secret manager。
- 外部调用：embedding/rerank/judge/ask 预计与实际调用量均为 0；无业务数据出站、无模型、无限流风险、费用为 0，依据是不发生调用。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。
- 验证：待执行 change 四个 artifact、必需标题/字段、ACTIVE 指针、Markdown 相对链接、业务实现零改动、固定范围与 `git diff --check` 检查。
- 跳过项及原因：当前只启动规格草案，未修改 Java、SQL、Vue、配置或评测脚本，因此不运行 Maven/Python/前端测试；实现与真实 MySQL migration 验证须在用户批准草案后进行。
- 范围安全：未修改 API、DTO、数据库 migration、认证实现、依赖、RAG pipeline、评测指标、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未执行 provider、部署、发布或网络业务调用。
- 剩余风险：known seed 隔离标记、bootstrap 状态机、角色装配、access token 实时撤权边界和评测脚本显式凭据策略仍待用户审定；批准前不得实现。
- Commit：`pending`。

## 2026-07-14｜C2 规格草案验证补充

- 结构验证：proposal、design、tasks、`specs/rag-system/spec.md` 四个必需 artifact 均存在且非空；proposal 的 Why/用户故事/Current Status/Scope/Non-goals/External Calls/Acceptance/提交责任、spec delta 的 requirement/scenario、tasks 的审批闸门与 ACTIVE 指针均已检查。
- 文档验证：草案没有 Markdown 相对链接；六个计划内 Markdown 文件无行尾空白；`git diff --check` 通过。
- 工具说明：当前环境未发现 `openspec` CLI，因此未执行官方 CLI/schema validate；已使用目录契约、标题和场景结构检查替代，并明确保留该跳过项。
- 范围验证：工作区仅修改 `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md` 并新增本 C2 change 目录；Java、SQL、Vue、运行配置、依赖与评测脚本零改动。
- 外部调用复核：embedding/rerank/judge/ask 实际调用量均为 0，无业务数据出站或费用。
- Commit：`pending`；提交责任仍为用户手动提交。

## 2026-07-14｜C2 规格草案提交补录

- Commit：`2282d2a14e8e1d3d0f0a5154a5c332912617c05a`。
- 结论：C2 proposal、design、tasks、spec delta 与 ACTIVE_TASK 已由用户手动完成中文提交；用户随后明确批准草案及全部设计决策，允许按 TDD 进入实现。

## 2026-07-14｜C2 规格草案提交 hash 更正

- 更正：上一条补录中的 commit hash 录入错误；真实 commit 为 `2282d2ab7c40ffe5954c1e098cce3ba7f4f0d9b0`。
- 说明：按 append-only 规则保留原记录并追加更正，不回改历史文本。

## 2026-07-14｜C2 草案批准与实现启动

- 用户决策：用户明确批准 proposal、design、tasks、spec delta 及五项设计决策，授权按 TDD 开始 C2 实现。
- 当前切片：Phase 1 数据库用户/角色查询边界；先写一个公共行为测试形成 RED，再加入最小持久层实现转 GREEN，不并行铺开后续 phase。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。
- 外部调用：本切片 embedding/rerank/judge/ask 计划调用量均为 0；只运行本地 Maven 测试。
- Commit：`pending`。

## 2026-07-14｜C2 database-backed-authentication 实现与本地验证

- 类型：Type C 重大变更实现；change 保持 `ACTIVE`，等待真实 MySQL/Flyway 证据与用户验收。
- 范围与修改文件：`rag-auth` 新增 bootstrap、认证 user/role mapper 与 repository，替换 `UserDetailsServiceImpl` 的内存用户；`rag-admin` 新增 `V6__quarantine_known_admin_seed.sql`、数据库认证/bootstrap/H2 兼容性/Testcontainers MySQL 测试并增加 `auth.bootstrap` 配置；补 refresh 测试；清理登录页、评测脚本和正式文档中的固定凭据入口；同步 C2 tasks、ACTIVE_TASK、架构与技术债说明。
- 已确认事实与关键决策：登录和 refresh 以数据库未删除用户及有效角色为事实源；角色只映射 `ROLE_*`，不加载 permission code；V6 只按历史 username + 精确 hash 隔离 known seed，禁用并保留 ID；bootstrap 默认关闭，只允许空库创建、精确隔离种子接管、正常 ADMIN no-op，其他状态 fail-fast；外部密码仅以 BCrypt hash 入库，正常用户状态与凭据不被覆盖。
- TDD 证据：数据库用户测试先在旧内存实现上 RED，再由 mapper/repository/UserDetailsService 转 GREEN；V6 资源缺失先 RED，再由精确 migration 转 GREEN；bootstrap 从缺少类型、未实现创建、`user` 保留字、种子接管、幂等 no-op、known-default 放行等连续 RED 推进到 GREEN；两个 eval runner 的显式凭据 helper 均先 RED 后 GREEN；全量 Python 首轮暴露 preflight 测试缺少显式凭据，修正 test-scope fixture 后转 GREEN。
- 迁移事实：历史 V1→V5 未修改，Git blob hash 依次为 `b38c90e0fb367e729143403caff016436e2091ea`、`df30a10907a2243de2e23e7e157032f0f99fcb39`、`77c752db9ebb696a78a934d0628051b1b2c9a657`、`f591e0d72b016db64f32e28dd1c5d50f01717986`、`163b7a8fd7fd3e275e772b9bf99010e2198f36fc`；`git diff --exit-code` 确认这些文件相对 HEAD 无改动。
- 验证：`mvn -q -pl rag-auth -am test` 通过；C2 聚焦测试通过；最终 `mvn -q test` 通过，Surefire 汇总 45 suites / 200 tests / 0 failures / 0 errors / 2 skipped；`python -B -m unittest discover -s scripts -p 'test_*.py'` 33 tests 通过；SensitiveLogs 扫描 331 个源码文件通过；正式前端 `vue-tsc -b` 与 `vite build` 通过；change 四个 artifact、requirement/scenario 结构、10 个变更 Markdown 文件相对链接、固定凭据范围扫描和 `git diff --check` 通过。
- 跳过项及原因：本机 Docker daemon 不可用，因此 `KnownSeedMigrationMySqlTest` 的 2 个真实 MySQL 场景由 `disabledWithoutDocker` 明确 skipped；没有把 H2 兼容性测试当作 MySQL/Flyway 验收，也未勾选 Phase 2/6 的真实 MySQL 项。完整 Maven 日志中的 3 条 Redis unavailable 是既有属性测试内部条件降级；Maven 仍成功，但它们不构成真实 Redis 证据。
- 工具说明：bundled pnpm 与现有 npm 布局不兼容，首次前端尝试把依赖移入 ignored 目录并因沙箱网络失败；已恢复被忽略的本地 `node_modules` 布局，随后通过项目质量脚本的 direct Node fallback 完成正式 build，tracked files 未受该尝试污染。
- 外部调用：embedding/rerank/judge/ask 及其他业务 provider 实际调用量均为 0；无业务数据出站、无模型费用或限流风险。Maven 仅解析仓库已声明依赖；Docker/MySQL 容器未启动。
- 范围安全：未修改历史 V1/V3 migration、API/DTO/token shape、RAG pipeline、评测指标、依赖版本、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、部署或发布。
- 剩余风险：真实 MySQL 上的全新 V1→V6、V5 known seed 升级、changed-admin 不变、重复 migrate 与 Flyway validate 尚未执行；存量 access token 实时撤权仍按批准设计留在后续 change；前端 build 保留既有大 chunk 警告。完成 MySQL 证据并由用户确认验收前，不接受 baseline delta、不将 ACTIVE_TASK 置为 IDLE、不归档 C2。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C2 真实 MySQL/Flyway 最终技术验收

- 类型：Type C 最终技术验收；change 保持 `ACTIVE`，等待用户明确确认实现验收通过。
- 范围与修改文件：补强 `KnownSeedMigrationMySqlTest.java`，新增“V5 exact known seed 升级后原 ID 不变”的独立场景；同步 C2 `tasks.md`、`.ai/ACTIVE_TASK.md` 与本日志。未改动 V6 实现或其他业务逻辑。
- 已确认事实：Docker Desktop 4.47.0、Engine 28.4.0 可用；Testcontainers 使用 `mysql:8.0.36` 与合成数据库 `rag_c2_migration`，未连接或修改本机 MySQL80 数据。
- 真实 MySQL 证据：`KnownSeedMigrationMySqlTest` 3 tests / 0 failures / 0 errors / 0 skipped；覆盖全新数据库 V1→V6 后 exact seed 被隔离、重复 migrate 执行数为 0、V5 exact seed 升级保留原 user ID、V5 changed-admin 的 hash/enabled/version/ID 均不变；Flyway 9.22.3 对 6 migrations validate 成功。
- 完整验证：最终 `mvn -q test` 通过，Surefire 汇总 45 suites / 201 tests / 0 failures / 0 errors / 0 skipped；`python -B -m unittest discover -s scripts -p 'test_*.py'` 33 tests 通过；SensitiveLogs 门禁通过；正式前端 `vue-tsc -b` 与 `vite build` 通过并转换 3334 modules。
- 警告与降级：MySQL 8 对历史 V1/V2 中 integer display width 输出弃用警告，不影响 migration/validate，属于既有 schema 兼容性债务；完整 Maven 日志仍有 3 条既有 Redis unavailable 属性测试内部条件降级，但 Surefire skipped 为 0；前端保留既有大 chunk 警告。
- 外部调用：首次 Testcontainers 执行从 Docker Hub 拉取 `testcontainers/ryuk:0.5.1` 与 `mysql:8.0.36` 镜像；未发送业务数据、用户凭据或 secret。embedding/rerank/judge/ask 及其他业务 provider 调用量均为 0。
- 范围安全：未修改历史 V1→V5 migration、API/DTO/token shape、RAG pipeline、评测指标、依赖版本、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、部署或发布。
- 剩余步骤：等待用户明确确认实现验收通过；确认后才接受 delta 到 baseline、将 ACTIVE_TASK 恢复 `IDLE` 并归档 change。当前不提前执行治理收口。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C2 验收确认与提交授权

- 用户决策：用户明确回复“C2 验收通过”，并授权 Agent 为本 change 创建必要的本地中文 commit，可按范围拆分。
- 提交边界：授权仅包含计划内文件的 `git add` 与本地 `git commit`；不包含 push、PR、发布或部署。
- 提交计划：先提交 C2 实现、测试与技术验收证据；取得真实实现 hash 后，再以独立治理提交接受 spec delta、恢复 `IDLE` 并归档 change。
- Commit：`pending`。

## 2026-07-15｜C2 实现提交补录

- Commit：`9c63051d8863786f04d8c0ccdb9fd34743d6311e`。
- 结论：C2 数据库认证、known seed 前向迁移、管理员 bootstrap、固定凭据入口清理、真实 MySQL 测试及完整技术验收证据已完成本地中文提交。

## 2026-07-15｜C2 database-backed-authentication 验收收口与归档

- 类型：Type C 验收收口；用户已明确确认实现验收通过并授权 Agent 创建本地中文 commit。
- 范围与修改文件：将 C2 delta 接受进 `openspec/specs/rag-system/spec.md`；补齐 archived `tasks.md`；将 change 移至 `openspec/changes/archive/2026-07-14-database-backed-authentication/`；更新 `.ai/ACTIVE_TASK.md` 为 `IDLE`，并同步 `openspec/project.md`、技术债与本日志。
- 已确认事实：实现提交为 `9c63051d8863786f04d8c0ccdb9fd34743d6311e`；真实 MySQL、完整 Maven、Python、正式前端 build、敏感日志与范围门禁已通过；accepted baseline 使用已批准 delta 的 requirement/scenario 文本。
- 验证计划：归档后检查四个必需 artifact、tasks 无未勾选项、ACTIVE_TASK 为 `IDLE`、baseline 与 archived delta 一致、Markdown 相对链接和 `git diff --check`。
- 跳过项及原因：本轮只做验收治理收口，不重复运行刚刚通过的代码测试和 MySQL 容器测试。
- 外部调用：embedding/rerank/judge/ask 实际调用量均为 0；无业务数据出站或费用。
- 范围安全：不修改业务实现、历史 migration、依赖、受保护本地配置或 RAG 指标；不 push、不创建 PR、不部署或发布。
- 剩余风险：access token 实时撤权、JWT 开发态 fallback、Redis/Milvus 联合链路和前端大 chunk 仍是已声明后续债务，不阻塞 C2 关闭。
- Commit：`pending`。

## 2026-07-15｜C2 归档提交补录

- Commit：`3c212ae9e367174aa354d2d4252824262f7df969`。
- 结论：C2 delta 接受、ACTIVE_TASK 恢复 `IDLE`、change 归档与当前事实源同步已完成本地中文提交；本条为独立纯日志补录，不递归记录自身提交 hash。

## 2026-07-15｜C3 integration-test-happy-path 启动与规格草案

- 类型：Type C 重大变更的规格阶段；change 已声明为 `ACTIVE`，业务实现尚未开始。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`、`openspec/changes/2026-07-15-integration-test-happy-path/{proposal.md,design.md,tasks.md}`。
- 已确认事实与关键决策：用户已手动 push C1/C2 提交，`main` 与 `origin/main` 一致且启动前工作区干净；C2 已归档；现有应用测试仍以 H2/禁用 Flyway 为主，MySQL Testcontainers 仅覆盖 migration，Redis 性质测试存在内部降级，Milvus 与确定性 embedding 尚未进入同一 happy-path。草案选择独立 `c3-integration` Maven/Failsafe 入口，使用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope token-hash embedding；Docker/依赖不可用时专用命令失败而非 skip。
- 大白话：改前测试全绿仍不能证明数据库用户登录后真的能上传、完成索引、检索并删除；改后用一条命令启动隔离依赖并走完整链路，任何关键步骤失败都会明确报错。
- 能力分类：`confirmed` 为 C2/主链路 API/Testcontainers MySQL/Docker 基线；`partial` 为 H2 应用测试、Redis 内部降级、Milvus/embedding 局部测试；`planned` 为真实联合容器、确定性 embedding 与 HTTP happy-path；`out_of_scope` 为 LLM/citation/judge、故障语义、索引恢复、生产契约修改；`unknown` 为跨机器 Milvus 资源上限、Redis 精确镜像与 CI Docker 额度。
- Spec delta：当前不创建长期 delta，因为只增加 test-scope harness，不修改生产 provider 接口或正式运行语义；如果实现必须触及 production seam，停止实现、补 `rag-system` delta 并重新审批。
- 外部调用：embedding/rerank model/judge/ask/LLM 预计与实际业务调用量均为 0；无模型、无业务数据出站、无 provider 限流或费用。后续首次真实运行可能下载固定版本基础设施镜像，需记录镜像身份，不上传业务数据。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。
- 验证：待执行 change artifact、必需标题/字段、ACTIVE 指针、Markdown 相对链接、业务实现零改动、固定范围与 `git diff --check` 检查。
- 跳过项及原因：当前只启动规格草案，未修改 Java、POM、test resources、依赖、配置或测试，因此不运行 Maven/Python/前端测试；实现与真实联合容器验证须在用户批准草案后进行。
- 范围安全：未修改业务代码、API/DTO、Flyway migration、生产 provider、RAG pipeline、评测指标、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未执行 provider、容器启动、部署、发布或网络业务调用。
- 剩余风险：五容器拓扑的启动时间/资源上限、Redis 精确镜像、任务轮询预算和 cleanup 诊断仍待用户审定及实现期真实验证；批准前不得实现。
- Commit：`pending`。

## 2026-07-15｜C3 规格草案验证补充

- 结构验证：proposal、design、tasks 三个必需 artifact 均存在且非空；proposal 的 Why/用户故事/Current Status/Scope/Non-goals/Spec Delta Decision/External Calls/Acceptance/Risks/提交责任，design 的测试入口/容器拓扑/确定性 embedding/happy-path/验证链/重审条件/审查决策，以及 tasks 的审批闸门均已检查。
- Spec delta 复核：change 下没有 `specs/` 目录，符合当前“只增加 test-scope harness、不修改生产契约”的显式决定；proposal、design、tasks 和 ACTIVE_TASK 均写明触及 production seam 时必须停下补 delta 并重新审批。
- 文档与范围验证：三份新 Markdown 无相对链接；行尾空白扫描与 `git diff --check` 通过；变更范围仅为 `.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md` 和本 C3 change 三个文件，Java、POM、test resources、依赖、baseline spec 与生产配置零改动。
- 状态验证：`ACTIVE_TASK=ACTIVE` 且唯一指向 `2026-07-15-integration-test-happy-path`；用户审批任务仍未勾选，实现保持阻断。
- 外部调用复核：embedding/rerank model/judge/ask/LLM 实际调用量均为 0；未启动新容器、未拉取镜像、无业务数据出站或费用。
- 跳过项：本轮仅文档规格，无代码、依赖或测试实现改动，因此未运行 Maven、Python、前端 build 或真实联合容器；等待用户批准后按 tasks 进入 TDD 实现。
- Commit：`pending`；提交责任仍为用户手动提交。

## 2026-07-15｜C3 规格草案提交补录

- Commit：`ab1e6e5233dcd7ebb8fbd74e87c05390b7051592`。
- 结论：C3 proposal、design、tasks、ACTIVE_TASK 与规格阶段证据已由用户手动完成中文提交；当前本地 `main` 比 `origin/main` 多 1 个提交，后续实现不暂存、不提交、不 push。

## 2026-07-15｜C3 草案批准与实现启动

- 用户决策：用户确认 Docker 已打开，并明确要求条件具备后进入 C3 迭代；实现完成后先提供测试反馈，范围内问题可直接修复，Git 等用户审核后手动提交。
- 已批准设计：独立 `c3-integration` Maven/Failsafe 入口；真实 MySQL、Redis、etcd、MinIO、Milvus 隔离容器；test-scope 确定性 embedding；只验证 retrieval、不调用 LLM；Docker/关键容器不可用时专用命令失败而非 skip；当前无长期 spec delta。
- TDD 纪律：按公开 HTTP 接口进行 RED → GREEN → REFACTOR，每次只推进一个可观察行为；不通过内部 mock 或数据库直查代替主链路断言。
- Docker 事实：Client/Server 均为 28.4.0，Engine 连通；本 change 不读取、不停止、不复用常驻 `rag-*` 容器。
- 外部调用：embedding/rerank model/judge/ask/LLM 计划调用量均为 0；只允许本地 Testcontainers network。首次运行可能拉取固定版本基础设施镜像，不上传业务数据。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。
- Commit：`pending`。

## 2026-07-15｜C3 integration-test-happy-path 实现与技术验收

- 类型：Type C test-scope 主链路集成测试实现；change 保持 `ACTIVE`，等待用户审核并明确确认实现验收通过。
- 范围与修改文件：在 `rag-admin/pom.xml` 增加 test-scope Testcontainers 依赖和 `c3-integration` Failsafe profile；新增 `application-c3-integration.yml`、`HappyPathIT.java`、`DeterministicEmbeddingTestConfig.java`；同步本 change `tasks.md`、`.ai/ACTIVE_TASK.md` 与本日志。未修改 production Java、API/DTO、Flyway migration、baseline spec 或生产 profile。
- TDD 与修复：最小真实容器测试首次因 Milvus 无法使用自定义 MinIO access key 而失败，改为 Milvus standalone 固定镜像所期望的合成 `minioadmin` 凭据；随后知识库创建因无可用 embedding provider 明确失败，新增 test-scope `deterministic-test` provider 后转绿。完整 HTTP happy-path 在该边界上补齐登录、知识库创建、target/distractor 上传、异步任务轮询、文档状态、retrieval 排序、删除可见性和资源清理断言。
- 容器事实：Testcontainers 使用 `mysql:8.0.36`、Redis 7 Alpine 固定 digest、`quay.io/coreos/etcd:v3.5.5`、`minio/minio:RELEASE.2023-03-20T20-16-18Z`、`milvusdb/milvus:v2.3.4` 与 `testcontainers/ryuk:0.5.1`；随机 host ports、独立 network、无固定 name/volume/reuse。Docker Engine 28.4.0、总内存 7790 MB；C3 退出后仅原有常驻容器存活，未停止、复用或改写任何 `rag-*` 容器。
- 主链路证据：两次聚焦隔离运行分别 48.9 秒和 46.4 秒并通过；每次均重新生成端口、容器和知识库 collection。真实 MySQL/Flyway、Redis 异步任务、Milvus 向量、BM25/RRF 参与；两个文档均为 `COMPLETED` 且 chunkCount > 0，target 首位召回，删除 target 后只剩 distractor，最终知识库和 Milvus collection 清理成功。
- 完整验证：`mvn -q -pl rag-admin -am -Pc3-integration verify` 114.6 秒通过，47 个 XML report / 203 tests / 0 failures / 0 errors / 0 skipped，其中 `HappyPathIT` 1 test / 0 failures / 0 errors / 0 skipped、36.578 秒；独立 `mvn -q test` 78 秒通过，46 reports / 202 tests / 0 failures / 0 errors / 0 skipped；Python 33 tests 通过；SensitiveLogs 扫描 274 个源文件通过；`git diff --check` 与计划范围检查通过。
- 跳过项及原因：本轮没有前端改动，按计划跳过正式前端 build；没有 generation/citation/no-answer/judge/SSE 范围，因此未做 ask 或 provider smoke。
- 警告与降级：Testcontainers 停止 Redis 时 Lettuce 输出一次 connection closed/reconnect 告警，测试和资源清理仍成功；默认 Maven 日志保留 3 条既有 Redis unavailable 属性测试内部条件降级，但 Surefire skipped 为 0；MySQL 8 对历史 integer display width 的弃用告警仍存在。以上均未作为业务成功证据，也不阻塞 C3 retrieval happy-path。
- 外部调用：真实 embedding、rerank model、judge、ask/LLM 调用量均为 0；确定性 provider 调用覆盖两个文档索引与 query embedding。除固定基础设施镜像下载外无业务数据出站、provider 费用或限流风险。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、生产依赖版本、生产 provider、RAG pipeline、chunking、prompt、citation/no-answer 或评测指标；未暂存、提交、push、创建 PR、部署或发布。
- 剩余步骤：等待用户审核测试反馈并明确确认实现验收；确认后才将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE` 并归档 change。当前无长期 spec delta，不修改 baseline。
- 建议提交信息：`test(集成): 完成C3主链路真实依赖验证`。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C3 实现提交补录与验收收口

- 用户决策：用户明确要求完成 C3 主链路真实依赖验证，检查确认无误后授权 Agent 创建本地 commit；该授权不包含 push、PR、部署或发布。
- 实现提交：`7f94538`（`test(集成): 完成C3主链路真实依赖验证`）。
- 收口范围：勾选用户验收与归档任务；将 change 移入 `openspec/changes/archive/2026-07-15-integration-test-happy-path/`；将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE`；同步 `openspec/project.md`、技术债清单与本日志。
- Spec delta：本 change 只增加 test-scope 联合证据，没有修改生产契约；归档时不创建或接受 delta，不修改 `openspec/specs/` baseline。
- 验证依据：实现提交前 C3 专用 verify 为 203 tests / 0 failures / 0 errors / 0 skipped，默认 Maven 为 202 tests / 0 failures / 0 errors / 0 skipped，Python 33 tests 与 SensitiveLogs 门禁通过；本轮治理收口只改文档与 change 位置，不重复运行代码测试。
- 范围安全：未修改生产代码、API/DTO、Flyway migration、provider、RAG pipeline、评测指标或受保护本地配置；未 push、创建 PR、部署或发布。
- Commit：`pending`；本条将在治理提交中落盘，不递归记录该治理提交自身 hash。

## 2026-07-15｜C4b llm-provider-resilience 启动与规格草案

- 类型：Type C 重大变更的规格阶段；`ACTIVE_TASK` 已置为 `ACTIVE`，生产实现尚未开始。
- 用户决策：用户同意按状态扫描后的建议启动 C4b；本轮只生成 proposal、design、tasks 与 `rag-system` spec delta，等待用户审阅具体契约后再进入 TDD 实现。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md`、`openspec/changes/2026-07-15-llm-provider-resilience/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`。
- 已确认事实：C3 已归档，启动前 `main` 与 `origin/main` 一致且工作区干净；现有 LLM 同步/流式客户端已有 timeout、429/5xx retry filter 和安全诊断雏形，但 tracked 默认 `max-retries=0`，没有真实 HTTP 故障测试；完整 Flux retry 在首 chunk 后可能重订阅；同步失败当前以 HTTP 200 + `metadata.status=error` 返回，controller 仍会增加查询次数并保存失败历史。
- 能力分类：`confirmed` 为 C3 前置、现有 timeout/retry filter 与诊断字段；`partial` 为默认关闭的 retry、未锁定的同步/SSE 失败语义和失败历史；`planned` 为本地 429/503/timeout/4xx/malformed/stream 故障注入与副作用测试；`out_of_scope` 为 C4c/C4d、C5、跨 provider fallback、熔断器和结构化 SSE；`unknown` 为真实 provider 的 `Retry-After`/心跳/body 差异。
- 关键草案决策：公共故障矩阵直接落在 C4b，不创建 C4a；tracked 默认 `max-retries=0`，显式 `N` 时总尝试不超过 `1+N`；同步保留 HTTP 200 外层兼容并以 `metadata.status=error` 表达失败；SSE 仅首 chunk 前可重试，首 chunk 后不重放；query count 计一次，但失败/部分输出不写 cache/history；不做跨 provider failover。
- 大白话：改前模型抖动可能直接失败、流式重放或把错误/半截答案保存成正常历史；改后每类故障有明确预算、稳定提示和副作用边界，失败不再伪装成成功记录。
- Spec delta：新增“LLM Provider 有界重试与故障分类”“LLM Generation 失败响应与副作用”两个 requirements，当前仅为 change 草案，用户验收前不接受进 baseline。
- 外部调用：embedding/rerank/judge/ask/LLM 业务调用量均为 0；规格阶段未启动本地 HTTP server。后续测试只向 `127.0.0.1` 合成服务发送合成 prompt，无真实模型、业务数据出站、provider 限流或费用。部署态理论放大边界为默认 1 次，运维显式配置时最多 `1+N` 次。
- 验证：四个必需 artifact 均存在且非空；proposal 必需章节、spec delta 的 ADDED/Requirement/Scenario 结构、tasks 审批闸门、`ACTIVE_TASK=ACTIVE` 与唯一 change 指针检查通过；Markdown 相对链接检查为 `MARKDOWN_RELATIVE_LINKS_OK`；`git diff --check` 通过；业务代码、测试、配置、评测脚本与 baseline spec 零改动。
- 跳过项及原因：本轮仅创建规格草案，没有 Java/Python/前端/依赖改动，因此不重复运行 Maven、Python、前端 build、SensitiveLogs 或 C3 容器测试；上一只读扫描已在相同 HEAD 验证默认 Maven 202 tests、Python 33 tests 与 `git diff --check` 通过，但该结果不替代后续 C4b 实现验证。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、生产 Java、API/DTO、数据库 migration、RAG pipeline、评测指标或依赖；未执行真实 provider 调用、暂存、提交、push、PR、部署或发布。
- 剩余风险与审批点：用户仍需确认默认零重试、同步 HTTP 200 外层兼容、失败不保存 history、SSE 首 chunk 后不重试、无跨 provider fallback/熔断/结构化 SSE，以及本地合成验证边界；批准前不得开始实现。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C4b 草案批准与 Agent 提交授权

- 用户决策：用户确认 C4b proposal、design、决策记录和 `rag-system` spec delta 无误，批准按草案进入 TDD 实现，并授权 Agent 为本 change 创建必要的本地中文 commit。
- 提交边界：授权仅包含计划内文件的 `git add` 与本地 `git commit`；不包含 push、PR、部署、发布或真实 provider 业务调用。
- 已接受契约：默认 `max-retries=0`；显式 `N` 时总尝试不超过 `1+N`；同步 generation failure 保持 HTTP 200 外层与 `metadata.status=error`；失败计一次 query count 但不写 cache/history；SSE 仅首 chunk 前可重试，首 chunk 后不重放；不做跨 provider fallback、熔断器、精确 `Retry-After` 或结构化 SSE。
- TDD 纪律：按公开可观察行为逐个 RED→GREEN；先同步故障，再流式边界，再 controller 副作用；每个切片转绿后才进入下一行为。
- 外部调用：实现与验证只允许访问 `127.0.0.1` 合成 HTTP server；真实 embedding/rerank/judge/ask/LLM 调用量为 0。
- Commit：`pending`。

## 2026-07-15｜C4b 规格草案提交补录

- Commit：`22425c1861b33462ce9730fb2b05b5cd8b701b5c`。
- 结论：C4b proposal、design、严格三行决策记录、tasks、`rag-system` spec delta、ACTIVE_TASK 与启动证据已完成本地中文提交；本补录不改写历史记录。

## 2026-07-15｜C4b 批准检查点提交补录

- Commit：`95bed9ec7689838d8b4d5455868801451106e6fb`。
- 结论：C4b 规格批准状态、Agent 本地提交授权、ACTIVE_TASK 与决策记录已完成中文治理提交；不包含实现代码。

## 2026-07-15｜C4b LLM provider 韧性实现与技术验收

- 范围：实现同步 provider 重试计数与安全分类、SSE 首个可见内容前后重试闸门、稳定客户端降级、失败 cache/history 副作用约束和 emitter 取消；新增本地 `127.0.0.1` JDK `HttpServer` 故障测试，并更新 controller/RAG service 测试与 C4b tasks。
- 修改文件：`rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`、`LLMProperties.java`、`RAGServiceImpl.java`、`rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`、对应 3 个测试文件、`openspec/changes/2026-07-15-llm-provider-resilience/tasks.md` 与本日志。
- 已确认事实：`maxRetries=N` 的总尝试上限为 `1+N`，Java 与 tracked 配置默认均为 0；429、5xx、timeout、I/O/connect 才重试，401 与 malformed/empty 2xx 不重试；SSE 只在首个可见 chunk 前重试，`alpha` 后截断只订阅一次，完整流无可见内容按 `invalid_response` 结束；同步 failure 保持 HTTP 200 + `metadata.status=error`，失败或部分输出不写 cache/history，query count 仍为 1。
- 安全：异常消息和客户端提示不透传 provider body/message；diagnostics 仅保留固定 provider/endpoint/model/timeout/retry/category/status 等允许字段。合成 API key、provider body 和 prompt marker 均未出现在捕获日志、异常消息或 diagnostics。
- TDD 证据：连续 503 的 attempt diagnostics、malformed response 分类、首 chunk 后重复订阅、同步/流式失败 history 保存与 Java 默认重试值均先观察到 RED，再以最小实现转 GREEN；429 恢复行为在首个测试中已由现有实现满足。
- 验证：`mvn -q -pl rag-core -am test` 通过；`mvn -q -pl rag-admin -am test` 通过；最终 `mvn -q test` 通过，47 个 surefire report、213 tests、0 failures、0 errors、3 skipped；`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 33 tests / OK；SensitiveLogs 扫描 274 个源文件通过；`git diff --check` 通过。
- 跳过项：无前端改动，未运行前端 build；未调用真实 embedding、rerank、judge、ask/LLM，实际业务外调量为 0；未做 Redis/Milvus、跨 provider fallback、熔断、结构化 SSE、部署或 push。
- 既有内部降级信号：完整 Maven 中 Redis property tests 因本地 Redis 不可用按既有逻辑跳过，Spring 测试环境仍记录 Milvus/BGE/关键词索引降级日志；命令整体退出码为 0，未将这些日志误报为真实 provider 验收。
- 剩余风险：尚未进行用户实现验收；spec delta 未接受进 baseline，ACTIVE_TASK 仍为 C4b，change 未归档。流式客户端断连由取消订阅保证，但未引入真实 servlet 容器断连测试，留待后续端到端 transport 验证。
- Commit：`pending`。

## 2026-07-15｜C4b 实现提交补录

- Commit：`db8898a2edcaa96ba7c5a3bdec8a79049a774e10`。
- 结论：C4b LLM provider 有界重试、流式首内容闸门、安全降级、副作用约束及本地故障测试已完成本地中文提交。

## 2026-07-15｜C4b 用户验收与 OpenSpec 收口

- 用户决策：用户明确确认 C4b 实现验收通过，并要求收口后进入 C4c 规划。
- 范围：把已批准 C4b delta 原文接受进 `openspec/specs/rag-system/spec.md`，完成 exact-match 校验，更新 tasks，将 ACTIVE_TASK 恢复 `IDLE` 并归档 change。
- 验证依据：实现提交 `db8898a`；最终 Maven 213 tests / 0 failures / 0 errors / 3 skipped，Python 33 tests / OK，SensitiveLogs 274 个源文件通过，真实 provider 业务调用量为 0。
- 跳过项：本轮仅做治理收口，不修改 Java、配置、测试或前端，因此不重复运行 Maven、Python、前端 build 或业务外调。
- 范围安全：未修改 API/DTO、数据库 migration、Redis/Milvus、索引恢复、评测指标或受保护本地配置；未 push、创建 PR、部署或发布。
- 剩余风险：真实 servlet client disconnect 仍留作后续 transport 端到端验证，不阻塞已接受的 C4b provider 契约；C4c 必须作为新的唯一 active change 另行审阅。
- Commit：`pending`；本条将在 C4b 治理收口提交中落盘，不递归记录该提交自身 hash。

## 2026-07-15｜C4b 治理收口提交补录

- Commit：`df2f75b602f411ffd0a4b2342d464a678a9c0d5c`。
- 结论：C4b delta 已按原文接受进 `rag-system` baseline，exact match 为真，ACTIVE_TASK 已恢复 `IDLE`，change 已归档；本条为独立纯日志补录，不递归记录自身提交 hash。

## 2026-07-15｜C4c redis-failure-semantics 启动与规格草案

- 类型：Type C 重大变更规格阶段；C4b 已收口，`ACTIVE_TASK` 已唯一指向 `2026-07-15-redis-failure-semantics`，生产实现尚未开始。
- 用户决策：用户确认 C4b 实现验收通过，并要求收口后开始 C4c 规划；C4c 尚未获得实现批准或 Agent 提交授权，当前提交责任为用户手动提交。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md`、`openspec/changes/2026-07-15-redis-failure-semantics/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`；未修改业务代码、测试、配置、依赖或 baseline spec。
- 已确认事实：Redis 直接消费者不只包括 QA/embedding cache、登录 session 和异步任务状态，还包括 token blacklist、滑动窗口限流、Redis 幂等、query count/statistics 及知识库删除时的计数清理；当前异常处理同时存在 fail-open、传播底层异常和伪装 empty/zero/not-blacklisted 的不一致行为。
- 能力分类：`confirmed` 为 C4b 前置完成与 direct consumer 清单；`partial` 为 embedding cache read/query count increment/rate-limit/blacklist 的局部吞异常和 login/task/idempotency 的局部传播；`planned` 为 consumer criticality 矩阵、稳定 503、幂等 outcome unknown、task 状态事实源及隔离 Redis stop/start；`out_of_scope` 为 C4d、C5、Redis HA/集群、内存 fallback 与生产 retry；`unknown` 为幂等 post-operation Redis 写失败时真实事务/AOP 顺序和副作用状态。
- 关键草案决策：optional cache 与计数写/清理 fail-open；statistics read 不伪造零值而返回 503；auth session/blacklist、rate-limit、带 key 幂等 pre-operation、task status fail-closed；幂等 post-operation 写失败返回 `IDEMPOTENCY_OUTCOME_UNKNOWN`；task recovery 留给 C5；consumer boundary 分级，不在 `RedisUtil` 全局吞异常。
- 用户故事：改前 Redis 重启可能让缓存拖垮问答、让已注销 token/限流失效，或把 task unknown 误报成不存在；改后可选能力安全绕过，安全与状态关键能力明确拒绝，且不伪造成功、零值或不存在。
- Spec delta：新增“Redis 依赖分级与稳定故障结果”“Redis 幂等保护与不确定结果”“Redis 异步任务状态事实源”“Redis 故障安全诊断”四个 requirements；仅为 change 草案，用户实现验收前不接受进 baseline。
- 外部调用：embedding/rerank/judge/ask/LLM 实际调用量均为 0；规格阶段未启动容器。后续故障验证只操作隔离 Testcontainers Redis 与合成数据，不停止或复用用户常驻 Redis。
- 跳过项：本轮只创建规划文档，没有 Java/Python/前端/依赖改动，因此不运行 Maven、Python、前端 build、SensitiveLogs 或故障容器；完成结构与差异检查后再记录结果。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、生产 Java、API DTO、Flyway migration、评测指标或依赖；未执行真实 provider 调用、暂存、提交、push、PR、部署或发布。
- 剩余审批点：用户需确认 rate-limit/auth fail-closed、statistics read 503、idempotency outcome unknown、C5 边界、隔离 stop/start 验证及提交责任；批准前不得进入实现。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C4c 规格草案结构与范围验证补充

- 结构验证：proposal、design、tasks 与 `rag-system` spec delta 四个必需 artifact 均存在且非空；proposal 必需章节齐全；design 含 10 条决策记录，每条严格为“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”三行；spec delta 含 4 个 requirements 与 14 个 scenarios。
- 状态验证：`ACTIVE_TASK=ACTIVE` 且唯一指向现存 `2026-07-15-redis-failure-semantics`；实现审批任务均未勾选，提交责任为用户手动提交。
- 差异验证：相对 C4b 收口补录提交 `d3e86d8`，accepted `openspec/specs/` baseline、生产/测试 Java、POM/package、application 配置和受保护路径均零改动；`git diff --check` 通过。
- 跳过项：本轮仅规划文档，没有代码、测试、配置或依赖变更，因此未运行 Maven、Python、前端 build、SensitiveLogs 或容器故障测试；没有真实 provider 或外部业务调用。
- 工作区：仅 `.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md` 与新 C4c change artifacts 未提交；Agent 未暂存、未提交、未 push。
- Commit：`pending`；建议用户审核后使用 `docs(openspec): 启动C4c Redis故障语义规划`。

## 2026-07-15｜C4c 规格草案提交补录

- Commit：`1e40d2e35e1797b2c23f9ca7706eebe7af615d5e`。
- 结论：C4c proposal、design、严格三行决策记录、tasks、`rag-system` spec delta、ACTIVE_TASK 与规划验证证据已由用户手动完成中文提交。

## 2026-07-15｜C4c 实现批准与 TDD 启动

- 用户决策：用户明确确认进入 C4c 实现，等同批准当前 proposal、design、决策记录与 spec delta；提交责任继续为用户手动提交，Agent 不暂存、不提交、不 push。
- 范围：按已批准 tasks 依次推进 optional cache/counter、auth/blacklist/rate-limit、idempotency、async task 与隔离 Redis stop/start；不修改 baseline spec，不进入 C4d/C5。
- 验证纪律：使用 `tdd` skill，按公开可观察行为逐个 RED → GREEN → REFACTOR，不一次性铺开全部测试与实现。
- 外部调用：真实 embedding、rerank、judge、ask/LLM 业务调用量均为 0；只允许 mock boundary 与隔离 Testcontainers Redis 使用合成数据。
- 剩余风险：幂等 operation 后 Redis 写失败时事务/AOP 真实顺序仍需集成测试确认；任务执行中状态丢失的恢复与协调明确留给 C5。
- Commit：`pending`。

## 2026-07-15｜C4c Redis 分级故障语义实现与技术验证

- 范围与修改文件：在 `rag-common` 新增安全的 `RedisDependencyException`，实现限流、幂等、异步任务的稳定故障语义；在 `rag-auth` 收紧 session、token blacklist 与 JWT filter；在 `rag-core` 让 QA/embedding cache 全链路 fail-open；在 `rag-admin` 修正 query counter/statistics，并新增 `c4c-redis-fault` Failsafe profile、隔离 Redis stop/start 集成用例及对应单元/HTTP/事务测试；同步更新 C4c design inventory、tasks 与 ACTIVE_TASK。
- 已确认事实与关键决策：optional cache/counter 写清理只损失性能或计数；security-critical 与 state-source 在 unknown 时返回稳定 503；限流 null/empty/exception 均不执行 controller；幂等 pre-operation read/lock 故障 supplier 为 0 次，completed 写失败为 `IDEMPOTENCY_OUTCOME_UNKNOWN`；Spring AOP + transaction 测试先观察到 completed 写位于事务内的 RED，设置明确切面顺序后确认业务事务完成再写幂等结果；task PENDING 成功前不启动 operation，progress/terminal/cancel 写失败不报告假成功。
- TDD 证据：QA cache read/write、embedding cache write、blacklist lookup/write、login/refresh/logout session、rate-limit exception、幂等 read/lock/post-write、task initial/read/progress/completed/cancel 等关键分支均先观察到预期 RED，再以 consumer-boundary 最小实现转 GREEN；既有兼容路径和 null/empty/corrupt 值补充回归测试。
- HTTP 与安全：`REDIS_DEPENDENCY_UNAVAILABLE` 和 `IDEMPOTENCY_OUTCOME_UNKNOWN` 均映射 503；既有 PROCESSING 仍为 409、正常超限仍为 429；JWT filter 在 blacklist unknown 时清空认证并停止 filter chain。新增 Redis 诊断仅记录 dependency/subsystem/operation/errorCategory/failMode/exception type，不记录 Redis key/value、token/hash/session、幂等 key、question/prompt/context、任务 result/error 或底层异常 message。
- 验证：`mvn -q -pl rag-common -am test`、`rag-auth`、`rag-core`、`rag-admin` 均通过；聚焦 C4c 回归通过；最终 `mvn -q test` 通过，54 个 surefire reports、245 tests、0 failures、0 errors、3 skipped；Python 33 tests / OK；SensitiveLogs 扫描 275 个源文件通过；`git diff --check` 通过。
- 隔离故障入口：`mvn -q -pl rag-admin -am -Pc4c-redis-fault verify` 退出码 0且测试成功编译，但本机 Docker daemon 未运行，`RedisFailureSemanticsIT` 为 1 skipped，未真实执行 stop/start；不能记为集成通过。用例只调用其 Testcontainer 自身 container id，覆盖 optional embedding 200、auth/rate-limit 503且 controller 0 次、task status 503及 restart recovery，不枚举或操作用户常驻容器/volume。
- 跳过项：无前端改动，未运行前端 build；真实 embedding/rerank/judge/ask/LLM 业务调用量为 0；未修改 baseline spec、DTO/schema、依赖版本、生产 Redis 配置、`.env.local` 或 `application-dev.yml`；未进入 C4d/C5，未做 HA/retry/replay/orphan coordination。
- 剩余风险：Docker 可用后必须补跑 `-Pc4c-redis-fault verify` 并确认 stop/start 非 skipped；任务执行中 Redis 中断后的恢复、重放与孤儿协调仍明确留给 C5；用户尚未完成实现验收，因此 ACTIVE_TASK 保持 C4c，spec delta 未接受、change 未归档。
- 提交责任与范围安全：用户手动提交；Agent 未执行 `git add`、`git commit`、push、PR、部署或发布。Commit：`pending`。

## 2026-07-15｜C4c 隔离 Redis stop/start 补跑与端口漂移修正

- 环境与范围：为完成既定集成门禁，启动本机 Docker Desktop，daemon 版本为 28.4.0；仅操作测试创建的 Redis Testcontainer 和一次性端口探针容器，均使用镜像 `redis:7-alpine` 的既定固定 digest，未枚举、停止或修改用户常驻容器/volume。
- 首轮真实结果：optional embedding outage 保持 200、auth/rate-limit 返回稳定 503 且 controller 未执行、task status 返回稳定 503 均通过；恢复探针失败。诊断确认 Redis 容器内已 `PONG`，但 Docker Desktop 会在随机发布端口的容器 stop/start 后重新分配宿主端口，一次性探针实测由 63723 漂移到 63732，应用仍连接旧端口。
- 修正：隔离测试在启动前选择随机空闲宿主端口并固定映射，增加 stop/start 前后映射端口一致性断言；恢复等待以真实公开 `/auth/login` 200 为应用级完成条件，不引入生产 retry、fallback 或恢复逻辑。
- 最终验证：`mvn -q -pl rag-admin -am verify -Pc4c-redis-fault` 退出码 0；`RedisFailureSemanticsIT` 为 1 test / 0 failures / 0 errors / 0 skipped。真实验证覆盖健康登录与任务完成、outage 期间 optional fail-open、auth/rate-limit fail-closed 且 controller 0 次、task status 503，以及 Redis restart 后 Lettuce 重连和公开登录恢复 200。
- 数据与外调：全程使用合成用户、token marker、cache 内容与任务；embedding provider 为进程内 synthetic stub，真实 embedding、rerank、judge、ask/LLM 业务调用量均为 0。
- 剩余风险：任务恢复、重放与孤儿协调仍按批准边界留给 C5；用户尚未完成实现验收，因此 ACTIVE_TASK 保持 C4c，spec delta 未接受、change 未归档。Docker Desktop 为本次验证启动，验证后保持运行。
- 提交责任：用户手动提交；Agent 未暂存、未提交、未 push。Commit：`pending`。

## 2026-07-15｜C4c 实现提交补录

- Commit：`ae0fbd9d870370ac5f02f8c7e22bfd4fc5f29eb7`。
- 结论：C4c Redis 分级故障语义、TDD 回归、隔离 Redis stop/start 验证与实现证据已由用户手动完成中文提交。

## 2026-07-15｜C4c 用户验收与 OpenSpec 收口

- 用户决策：用户明确确认 C4c 实现验收通过，并授权归档 `2026-07-15-redis-failure-semantics` change。
- 范围与修改文件：将已批准 C4c `rag-system` delta 原文接受进 `openspec/specs/rag-system/spec.md`，勾选最终 closeout task，将 `.ai/ACTIVE_TASK.md` 恢复 `IDLE`，并把 change 移入 `openspec/changes/archive/`。
- 验证依据：实现提交 `ae0fbd9`；最终 Maven 245 tests / 0 failures / 0 errors，隔离 `RedisFailureSemanticsIT` 1 test / 0 failures / 0 errors / 0 skipped，Python 33 tests / OK，SensitiveLogs 275 个源文件通过，真实 provider 业务调用量为 0。
- 跳过项：本轮仅做治理收口，不修改 Java、测试、POM、前端或运行配置，因此不重复运行 Maven、Python、前端 build 或真实 provider/业务外调；执行 spec exact-match、旧 active 路径扫描与 `git diff --check`。
- 范围安全：未修改 API/DTO、数据库 schema、依赖版本、Redis/Milvus 生产配置、评测指标或受保护本地配置；未进入 C4d/C5，未暂存、未提交、未 push、未创建 PR、未部署或发布。
- 剩余风险：Redis outage 中断任务的恢复、重放与孤儿协调仍留给 C5；Milvus 故障语义仍留给 C4d，不影响 C4c 已接受契约。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C4c 治理收口提交补录

- Commit：`c63d9c209d82f6693ce699f93323cfa3e82559cf`。
- 结论：C4c delta 已接受进 `rag-system` baseline，ACTIVE_TASK 已恢复 `IDLE`，change 已归档并由用户手动完成中文提交。

## 2026-07-15｜C4d milvus-failure-semantics 启动与规格草案

- 类型与用户决策：Type C 重大变更规格阶段；用户在 readiness scan 结论为 GO 后明确要求开始 C4d 规划。当前只建立 OpenSpec 事前闸门，尚未批准生产实现或 Agent 提交。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md`、`openspec/changes/2026-07-15-milvus-failure-semantics/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`；未修改生产 Java、测试、POM、配置、依赖或 baseline spec。
- 已确认事实：C3 已用固定 Milvus 2.3.4 + etcd/MinIO + 确定性 embedding 验证健康主链路；当前 Milvus consumers 覆盖 KB create/drop/count、document upsert/delete 与 dense search。`MilvusVectorStore` 多数 status 失败拼接 raw message，部分 release/delete response 未检查；vector search failure 会阻止 keyword route；index 对所有 runtime 整段重试；delete/drop 吞异常后继续 SQL delete；count failure 伪装零值。
- 能力分类：`confirmed` 为 C3/C4b/C4c 前置和 tracked consumer 清单；`partial` 为 load-on-search、通用 VectorStoreException、FAILED 状态与未分类重试/吞异常；`planned` 为 stable categories、conditional keyword-only、mutation outcome unknown、lifecycle fail-closed、statistics 503、安全 task/log 与隔离 stop/start；`out_of_scope` 为其他 adapters、HA/容量、C5 恢复与公开 DTO/schema；`unknown` 为 SDK stop/start/timeout/status 与 post-mutation response lost 的真实表现。
- 规划默认与待审决策：仅 Milvus；keyword contexts 非空才降级并写现有 QA metadata，不写普通成功 cache；keyword 不可用/empty 时 stable error、LLM 0 次；vector mutation 不自动重放；delete/drop fail-closed；count unknown 503；collection missing 不自动重建；内部 retrieval result carrier；恢复/对账留 C5。所有默认项已在 tasks 事前闸门和 design 决策记录中标记待用户确认。
- Spec delta：草案新增“Milvus 依赖故障分类与稳定结果”“Milvus 检索部分降级”“Milvus 索引写入与生命周期一致性”“Milvus 统计与安全诊断”四组 requirements；仅为 change delta，用户实现验收前不得接受进 baseline。
- 外部调用：规划阶段未启动容器；真实 embedding/rerank/judge/ask/LLM 调用量均为 0。后续仅允许固定基础设施镜像、进程内确定性 embedding、合成数据与测试自有 container id。
- 跳过项：本轮只写规划文档，因此不运行 Maven、Python、前端 build、SensitiveLogs、Milvus stop/start 或 provider 调用；完成结构、决策记录、旧路径、baseline/代码零改动与 `git diff --check` 后再交付审阅。
- 范围安全：不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、Qdrant/Elasticsearch、API DTO、数据库 schema、评测指标或生产 Milvus 配置；不进入 C5/C6，不暂存、不提交、不 push、不部署。
- 剩余审批点：adapter scope、keyword-only 条件与可观察 metadata、degraded cache、mutation retry/outcome unknown、delete/drop、statistics 503、collection missing、隔离验证和提交责任。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-15｜C4d 规格草案结构与范围验证补充

- 结构验证：proposal、design、tasks 与 `rag-system` spec delta 四个必需 artifact 均存在；proposal 必需章节齐全；design 含 13 条真实决策记录，每条严格为“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”三行；spec delta 含 4 个 requirements 与 15 个 scenarios。
- 闸门验证：所有 implementation/approval tasks 均保持未勾选；`ACTIVE_TASK=ACTIVE` 且唯一指向现存 `2026-07-15-milvus-failure-semantics`；提交责任为用户手动提交。
- 差异验证：accepted `openspec/specs/` baseline、生产/测试 Java、POM/package、application 配置、Qdrant/Elasticsearch 和受保护路径均零改动；`git diff --check` 通过。
- 跳过项：本轮仅规划文档，没有代码、测试、配置、依赖或前端改动，因此未运行 Maven、Python、前端 build、SensitiveLogs、Docker/Milvus stop/start 或 provider 调用；没有业务数据出站和费用。
- 工作区：仅 `.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md` 与新 C4d change artifacts 未提交；Agent 未暂存、未提交、未 push。
- Commit：`pending`；建议用户审核后使用 `docs(openspec): 启动C4d Milvus故障语义规划`。

## 2026-07-16｜C4d 规格草案提交补录

- Commit：`3b6750f`。
- 结论：C4d proposal、design、严格三行决策记录、tasks、`rag-system` spec delta、ACTIVE_TASK 与规划验证证据已由用户手动完成中文提交。

## 2026-07-16｜C4d 实现批准与 TDD 启动

- 用户决策：用户明确要求启动 C4d 项目迭代，等同批准当前 proposal、design、全部决策记录与 `rag-system` spec delta；提交责任继续为用户手动提交，Agent 不暂存、不提交、不 push。
- 范围：按已批准 tasks 依次推进 Milvus adapter 稳定异常、条件式 keyword-only retrieval、mutation/lifecycle/statistics fail-closed 与隔离 Milvus stop/start；不修改 baseline spec，不进入 C5。
- 验证纪律：使用 `tdd` skill，按公开可观察行为逐个 RED → GREEN → REFACTOR，不一次性铺开全部测试与实现。
- 外部调用：真实 embedding、rerank、judge、ask/LLM 业务调用量均为 0；只允许 mock boundary、确定性 stub、合成数据与隔离 Testcontainers Milvus。
- 剩余风险：Milvus SDK 在真实 stop/start、RPC timeout 和 mutation 回执丢失时的 exception/status 仍需由聚焦测试与隔离集成验证确认；自动恢复、重放和跨存储对账继续留给 C5。
- Commit：`pending`。

## 2026-07-16｜C4d Milvus 故障语义实现与阶段验证

- 范围与修改文件：在 `rag-core` 新增 `VectorDependencyException` 与内部 `RetrievalResult`，收紧 `MilvusVectorStore` 全操作响应检查、检索降级和 QA metadata/cache；在 `rag-admin` 收紧索引重试、文档/知识库删除与 statistics，新增 `c4d-milvus-fault` profile 和隔离 stop/start 用例；在 `rag-common` 让异步 task 持久化稳定 BusinessException code/message；同步更新 C4d proposal/design/tasks、ACTIVE_TASK 和安全日志配置。
- 已确认事实与关键决策：Milvus search 首次依赖失败即停止剩余 query variants；只有 keyword contexts 非空才进入 `keyword_only`，固定写 `retrievalMode/retrievalDegraded/degradedDependency` 且不写普通成功 cache；keyword disabled/error/empty 保留稳定 vector error，不进入 no-result/LLM；confirmed missing collection 为 `VECTOR_INDEX_UNAVAILABLE`；thrown mutation 为 `VECTOR_OPERATION_OUTCOME_UNKNOWN` 且不进入索引 blanket retry；create/delete/drop/count 均 fail-closed。
- TDD 证据：adapter thrown exception、mutation response lost、collection missing、non-success status、null response和敏感 marker；QueryEngine keyword healthy/empty 与 vector call budget；QA metadata/cache；KB create/drop/count；document upsert retry budget均先观察预期 RED，再以最小实现转 GREEN。既有非 vector retry、hybrid/RRF、no-result 与 C4b generation failure 回归通过。
- 安全诊断：稳定 diagnostics 只含 dependency/subsystem/operation/errorCategory/failMode；客户端与 task 使用固定 code/message；Milvus adapter 普通日志不再记录 collection/host/port，并关闭会输出 SDK request 细节的 `io.milvus.client.AbstractMilvusGrpcClient` logger。SensitiveLogs 扫描 277 个源文件通过，人工扫描未发现 C4d raw marker 进入生产源。
- 隔离故障证据：Docker Desktop 28.4.0；`MilvusFailureSemanticsIT` 使用固定 `milvusdb/milvus:v2.3.4`、etcd 3.5.5、MinIO 固定版本、随机空闲后固定 Milvus host port及测试自有 container id；健康 create/upsert/search、仅 Milvus stop、稳定 unavailable、原 client restart search recovery、drop cleanup真实通过，1 test / 0 failures / 0 errors / 0 skipped，耗时 341.3 秒。未枚举或操作用户常驻容器/volume，也未停止测试 etcd/MinIO。
- 验证：聚焦 core/admin C4d 测试通过；`mvn -q test` 通过，55 个 surefire reports、255 tests、0 failures、0 errors、0 skipped；Python 33 tests / OK；SensitiveLogs 277 个源文件通过；`git diff --check` 通过。
- 外部调用：真实 embedding、rerank、judge、ask/LLM 业务调用量均为 0；集成测试只使用合成 collection、vector、content 和 metadata，无业务数据出站、无模型费用。
- 范围安全：accepted baseline spec、公开 DTO/schema、Flyway migration、Qdrant/Elasticsearch、`.env.local`、`application-dev.yml`、`.agents/` 和 `docs/学习文档/` 均零改动；未进入 C5，未暂存、未提交、未 push、未创建 PR、未部署或发布。
- 剩余风险与未完成项：真实 stop/start 已覆盖 adapter failure/recovery；keyword-only、no-keyword stable error 与 index task failure 当前由 mock/单元层覆盖，尚未在同一个真实 outage Failsafe 场景中全部串联，因此 tasks 6 对应组合项保持未勾选。Milvus SDK 2.3.4 在 outage 调用中执行较长内置 retry，单次隔离测试耗时约 5 分 41 秒；C4d 未新增生产 retry/timeout 配置。用户尚未验收，ACTIVE_TASK 保持 C4d，delta 未接受、change 未归档。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-16｜C4d 剩余组合故障验证闭环

- 范围与修改文件：仅增强 `MilvusFailureSemanticsIT` 的组合故障编排，并同步 C4d design、tasks、ACTIVE_TASK 与追加式执行证据；未修改生产 retry/timeout、公开 API、baseline 或归档状态。
- 真实故障编排：隔离测试 client 使用 SDK `withRetry(1)` 和 3 秒 RPC deadline；健康态完成 create/upsert/search 后，只 stop 测试自有 Milvus container id。真实 dense/no-keyword 请求得到稳定 `VECTOR_STORE_UNAVAILABLE`，同一已观测 outage 继续驱动 keyword-only diagnostics 和 document index task failure，断言 vector upsert 只调用一次、document 为 `FAILED` 且不保存 chunks；随后 start 同一 container id，固定 host port 不变，原 client 的应用级 search 恢复并完成 collection cleanup。
- Failsafe 结果：`MilvusFailureSemanticsIT` 1 test / 0 failures / 0 errors / 0 skipped，测试耗时 49.971 秒；相较上一版 341.3 秒，显式测试 retry/deadline 消除了 SDK 默认长重试造成的验收拖延。未枚举、停止或修改用户常驻容器/volume，也未停止测试 etcd/MinIO。
- 完整验证：`mvn -q test` 通过，55 个 surefire reports、257 tests、0 failures、0 errors、0 skipped；Python 33 tests / OK；SensitiveLogs 扫描 277 个源文件通过；`git diff --check` 通过。
- 外部调用与跳过项：真实 embedding、rerank、judge、ask/LLM 调用量均为 0；只使用进程内确定性 embedding、mock consumer seam、合成 collection/vector/content/metadata，无业务数据出站和模型费用。无前端改动，因此未运行前端 build。
- 范围安全：accepted baseline spec、公开 DTO/schema、Flyway migration、Qdrant/Elasticsearch、`.env.local`、`application-dev.yml`、`.agents/` 和 `docs/学习文档/` 均零改动；未进入 C5，未暂存、未提交、未 push、未创建 PR、未部署或发布。
- 剩余风险：Milvus SDK 的生产默认 retry/timeout 仍沿用既有配置，本 change 只保证稳定语义，不承诺 outage 延迟上界；outcome unknown 的自动恢复、重放和跨存储对账仍留给 C5。实现与必需验证已完成，当前只等待用户验收；delta 尚未接受、change 尚未归档。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-16｜C4d 用户验收与 OpenSpec 收口

- 用户决策：用户明确确认 C4d 实现验收通过，并要求完成收尾后启动 C5a。
- 范围与修改文件：将 C4d `rag-system` delta 原文接受进 `openspec/specs/rag-system/spec.md`，勾选最终 closeout task，把 change 移入 `openspec/changes/archive/2026-07-15-milvus-failure-semantics/`，并同步 `openspec/project.md`、`docs/roadmap/technical-debt.md` 与活动任务交接。
- 验证依据：实现提交 `545c8e7`；此前完整 Maven 257 tests / 0 failures / 0 errors / 0 skipped，隔离 `MilvusFailureSemanticsIT` 1 test / 0 failures / 0 errors / 0 skipped，Python 33 tests / OK，SensitiveLogs 277 个源文件通过，真实 provider 业务调用量为 0。
- 跳过项：本次 C4d 收口不修改 Java、测试、POM、前端或生产配置，因此不重复运行 Maven、Python、前端 build、Docker/Milvus 或 provider 调用；执行 delta exact-match、归档结构、旧 active 路径与 `git diff --check` 验证。
- 范围安全：未修改 API/DTO、数据库 schema、依赖、Qdrant/Elasticsearch、评测指标或受保护本地配置；未暂存、未提交、未 push、未创建 PR、未部署或发布。
- 剩余风险：生产 Milvus SDK 默认 retry/timeout 仍无 outage 延迟上界；索引输入持久化、自动恢复与对账按边界进入 C5a/C5b。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-16｜C5a durable-index-inputs 启动与规格草案

- 类型与用户决策：Type C 重大变更规格阶段；用户要求在 C4d 收口后启动 C5a。当前只建立 OpenSpec 事前闸门，尚未批准生产实现或 Agent 提交。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md`、`openspec/changes/2026-07-16-durable-index-inputs/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`，并同步 C4d 收口后的项目/技术债说明；未修改生产 Java、migration、POM、配置、测试或 baseline 中的 C5a 契约。
- 已确认事实：上传输入当前写入系统临时文件，异步闭包捕获绝对 `Path`，任务 finally 无条件删除；`document.file_path` 字段存在但生产代码未写入/读取；应用没有文档对象存储 adapter，C3 中 MinIO 仅为 Milvus 测试依赖；异步任务状态当前以 Redis 为事实源，自动协调与续跑属于 C5b。
- 规划建议：C5a 先引入 `IndexInputStore` 边界与应用管理的本地 durable filesystem 实现；数据库只保存 opaque storage key、大小、SHA-256 与输入状态；写入采用同目录 staging + atomic move，任务按 key 重新打开输入；成功后转入可清理状态，失败/中断保留输入供 C5b，删除文档时清理；全部建议待用户在 approval gate 确认。
- 边界：不实现 orphan scanner、lease/claim、自动 replay、resume API、跨存储补偿事务、S3/MinIO adapter、公开 DTO 或前端改造；不改变 embedding、分块、检索、prompt、citation、no-answer 或评测口径。
- 外部调用：规划阶段真实 embedding、rerank、judge、ask/LLM 调用量均为 0；未启动容器、未上传数据、无模型费用。
- 验证结果：C5a 四个必需 artifact 齐全；proposal 必需章节齐全；design 含 12 条决策记录，三类固定行各 12 条；spec delta 含 4 个 requirements / 14 个 scenarios；approval gate 0 项已勾选；唯一 active change 为 C5a。C4d delta 与 baseline exact match，四个 requirement 均只出现一次；旧 active 目录不存在、archive 存在；`git diff --check` 通过，OpenSpec CLI 当前不可用。
- 跳过项：本轮仅规划与治理文件，因此不运行 Maven、Python、前端 build、Docker 或真实 provider；没有业务数据出站和费用。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-16｜C5a 规划批准与 TDD 实现启动

- 用户决策：用户明确批准 C5a proposal、design、12 条决策记录、tasks 与 `rag-system` spec delta，并随后要求继续任务；据此进入实现阶段。
- 已确认边界：首版本地 durable filesystem；opaque key + byte size + SHA-256 + input state；root 内 staging + atomic move；任务按稳定 key reopen；成功清理、失败/中断/outcome unknown 保留；production root fail-fast；C5b 自动协调、对象存储与公开 DTO/前端均 out_of_scope。
- 执行方式：使用 `tdd` skill，按一个公开行为对应一个 RED → GREEN 切片推进，不批量预写全部测试。
- 提交责任：用户手动提交；Agent 不暂存、不提交、不 push。Commit：`pending`。

## 2026-07-16｜C5a durable index inputs 实现与验证完成

- 范围与修改文件：新增 `IndexInputStore`、filesystem 实现、稳定输入状态/异常/结果类型与 V7 migration；扩展 document 持久化字段和状态更新；把上传链路改为 durable put → document association → Redis task acceptance，异步任务按 opaque key reopen + size/SHA-256 verify；把健康完成、失败/outcome unknown、task acceptance failure 与 canonical document delete 的输入生命周期显式化；新增 storage/service/migration/restart 测试，同步 README 部署配置及 proposal/design/tasks、ACTIVE_TASK。未接受 delta 进 baseline，未启动 C5b。
- 已确认事实与关键决策：production 必须显式配置非 system-temp 的 `RAG_INDEX_INPUT_ROOT`；启动时执行可写/atomic move/可用空间 probe；store 自身执行 50MB 单文件上限和默认 100MB 可用空间低水位，不只依赖 multipart；storage key 限制在 root/objects 内并拒绝绝对路径、`.`/`..`、symlink/junction escape 和非 regular file；输入写入按 root 内 staging + atomic move 发布，保存原始 byte size/SHA-256；delete 明确区分 DELETED/ALREADY_MISSING/FAILED。
- 生命周期结果：健康 COMPLETED 后执行 `AVAILABLE -> CLEANUP_PENDING -> CLEANED`；清理失败保留 COMPLETED 与 CLEANUP_PENDING；一般 FAILED/进程中断/vector outcome unknown 保持 AVAILABLE；missing/corrupt 在 parser/embedding/vector 前终止并写 MISSING/CORRUPT；canonical document delete 若输入清理失败则不删除 SQL document 记录。C5a 未新增 scanner、scheduler、lease/claim、replay、resume endpoint 或自动恢复。
- TDD 证据：依次观察 store 类型缺失、上传持久事实缺失、normalized traversal 被接受、canonical delete 未注入 store、corrupt 未持久化状态、Windows junction escape 被误判为 corrupt、cleanup FAILED 结果缺失、store 大小/容量构造契约缺失、公开 Document JSON 泄露 storage key 等预期 RED；逐片最小实现后聚焦测试全部转 GREEN。新增测试覆盖原子发布/部分 staging 清理、跨实例 reopen、size/hash、防穿越/链接、幂等删除、production root、大小/容量拒绝、acceptance ordering、Redis 初始写失败、missing/corrupt 零下游调用、完成/失败/outcome unknown/清理失败、document delete 与内部输入字段序列化保护。
- migration 与集成：`KnownSeedMigrationMySqlTest#v6DocumentRowsRemainCompatibleAfterDurableInputMigration` 使用真实 MySQL 8.0.36 从 V1..V6 升至 V7并验证旧行新增字段为 nullable；C3 `HappyPathIT` 首轮暴露测试自动发现了 C4c 嵌套 TestApplication，改为显式 `RagQaApplication` 后通过，1 test / 0 failures / 0 errors / 0 skipped；C4c `RedisFailureSemanticsIT` 1/0/0/0；C4d `MilvusFailureSemanticsIT` 1/0/0/0。集成数据均为合成数据。
- 最终验证：C5a store/service 聚焦测试通过；`mvn -q test` 退出码 0，59 个 Surefire reports、278 tests、0 failures、0 errors、0 skipped；Python 33 tests / OK；SensitiveLogs 扫描 283 个源文件通过；`git diff --check` 通过，仅提示 3 个既有 CRLF 文件下次由 Git 转 LF；公开接口序列化、前端、解析/检索/评测生产模块、对象存储依赖和受保护路径扫描均无越界改动。
- 外部调用与跳过项：真实 embedding、rerank、judge、ask/LLM 业务调用量均为 0，无业务数据出站和模型费用；无前端改动，因此未运行前端 build；最后一轮容量保护只影响 filesystem store/config，并已重新运行聚焦测试和完整 Maven，未重复耗时的 C3/C4c/C4d Failsafe。
- 剩余风险：本地 filesystem 的跨容器持久性仍取决于部署方正确挂载 `RAG_INDEX_INPUT_ROOT`；filesystem/DB/Redis 间崩溃窗口、无主输入发现、失败输入保留期限、自动协调/重放与跨存储对账仍按批准边界留给 C5b。当前等待用户验收，ACTIVE_TASK 保持 C5a，spec delta 未接受、change 未归档。
- Commit：`pending`；提交责任为用户手动提交，Agent 未暂存、未提交、未 push、未创建 PR、未部署或发布。

## 2026-07-16｜C5a 用户验收、baseline 接受与归档收口

- 用户决策：用户明确确认 C5a 实现验收通过，并授权 Agent 接受 spec delta、恢复 `IDLE`、归档 change 与完成本地提交。
- 范围与修改文件：将 C5a `rag-system` delta 原文接受进 `openspec/specs/rag-system/spec.md`；勾选最终 closeout task；把 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE`；将 change 归档至 `openspec/changes/archive/2026-07-16-durable-index-inputs/`；未进入 C5b。
- 验证依据：C5a 最终 `mvn -q test` 为 59 个 Surefire reports、278 tests、0 failures、0 errors、0 skipped；Python 33 tests / OK；SensitiveLogs 283 个源文件通过；C3/C4c/C4d 相关集成与真实 MySQL V1-V7 兼容测试均已通过，真实 provider 业务调用量为 0。
- 本轮收口验证：执行 delta-to-baseline exact-match、archive 结构、全部 tasks 完成、`ACTIVE_TASK=IDLE`、无未归档 active change、受保护路径与 `git diff --check` 检查；提交前复核 staged diff 仅含 C5a 计划内实现、测试、文档、baseline 与归档文件。
- 跳过项：本轮只新增治理收口、baseline 接受与归档移动，不再修改 Java、migration、配置或测试，因此不重复运行 Maven、Python、前端 build、Docker/Failsafe 或 provider 调用；沿用同一工作区刚完成的最终验证证据。
- 范围安全：不启动 C5b，不修改 embedding、分块、检索、prompt、citation、no-answer 或评测口径；不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`；不 push、不创建 PR、不部署或发布。
- 剩余风险：production 仍需把 `RAG_INDEX_INPUT_ROOT` 挂载至真实持久卷；filesystem/DB/Redis 崩溃窗口、无主输入发现、失败输入保留期限、自动协调/重放与跨存储对账留给后续 C5b change。
- Commit：`pending`；用户已授权 Agent 完成本地提交。

## 2026-07-17｜C5a 实现与收口提交补录

- Commit：`b144bbb3c3d84b4dbec24dea04e22a7a87865d1d`。
- 结论：C5a durable input 实现、最终验证、baseline 接受、`ACTIVE_TASK=IDLE` 与 change 归档已由 Agent 按用户授权完成中文本地提交。

## 2026-07-17｜C5b readiness scan 与规格草案启动

- Readiness：`GO`。扫描开始时 `main...origin/main` 工作区干净、C5a 提交为 `b144bbb`、C5a delta 已接受进 baseline、change 已归档、`ACTIVE_TASK=IDLE` 且没有其他 active change；C5a durable input/完整性/输入状态与 C4c/C4d fail-closed 契约满足 C5b 前置。
- 类型与阶段：Type C 重大变更的 proposal 阶段；创建 `2026-07-17-index-task-reconciliation-and-resume` proposal、design、tasks 与 `rag-system` spec delta，并把 ACTIVE_TASK 指向该 change。当前只规划，用户批准前不修改生产 Java、migration、配置或测试。
- 范围与修改文件：`.ai/ACTIVE_TASK.md`、追加式 `.ai/AGENT_LOG.md`、`openspec/changes/2026-07-17-index-task-reconciliation-and-resume/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`；同步修正 C5a 后已过时的 `openspec/project.md`、`docs/roadmap/{iteration-blueprint.md,technical-debt.md}` 与 `docs/architecture/overview.md` 当前事实。
- 已确认事实：真实上传直接使用 `RedisAsyncTaskManager.submit`；Redis status TTL 为 24 小时，executor/future/closure 仅存在于当前 JVM。V2 虽有 `async_task` 表，但生产 Java 零读写；本地 MQ 模板未接入真实上传，默认 consumer 不执行索引。document/input 状态缺 taskId/phase/lease/attempt；chunk/vector IDs 当前为随机 UUID；C4d 禁止 outcome unknown 自动重放。
- 能力分类：`confirmed` 为 C5a durable input、Redis/JVM 生命周期、unused async_task/MQ 与 C4d mutation 边界；`partial` 为 document/input 候选事实和 VectorStore getByIds；`planned` 为 MySQL durable ledger、DB lease、phase checkpoint、deterministic new-task IDs、Redis projection fallback、安全 resume 和 cleanup reconciliation；`out_of_scope` 为 force resume、RabbitMQ/Kafka、exactly-once、非索引 task、对象存储与 provider failover；`unknown` 为默认开关/预算、lease 参数、ID contract 和 legacy 策略，等待事前闸门确认。
- 规划建议：MySQL `async_task` 为 durable source、Redis 为投影；acceptance 先 ledger 后 scheduling；DB CAS lease/heartbeat；只恢复 SAFE_PRE_VECTOR 和 VECTOR_CONFIRMED 收尾；VECTOR_IN_FLIGHT/outcome unknown/legacy/mismatch 进入 `RECONCILIATION_REQUIRED`；reconciliation 与 auto resume 分开，auto resume 默认关闭；CLEANUP_PENDING 只做幂等 delete。
- 规格草案：新增“文档索引任务的 durable ledger”“跨实例 claim 与孤儿任务协调”“Phase-aware safe resume”“Legacy 与 cleanup reconciliation”“恢复开关、调用预算与安全诊断”五组 requirements；仅为 change delta，用户实现验收前不得接受进 baseline。
- 外部调用：规划阶段真实 embedding、rerank、judge、ask/LLM 调用量均为 0，无数据出站、限流或费用。实现测试建议只使用确定性 embedding 与合成数据；真实 provider 下批量 resume 必须另行披露候选数/chunks/模型/数据出站/费用并获授权。
- 跳过项：本轮只写规划与当前事实文档，因此不运行 Maven、Python、前端 build、Docker/Failsafe 或 provider 调用；完成 artifact/决策记录/active pointer/baseline 零改动/受保护路径与 `git diff --check` 验证。
- 范围安全：不修改 accepted baseline、生产 Java、migration、POM/依赖、application 配置、API/DTO、前端、检索/生成/评测、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；不暂存、不提交、不 push、不创建 PR、不部署或发布。
- 剩余审批点：ledger/Redis 事实源、acceptance 顺序、lease 默认值、可恢复 phase、deterministic ID、vector ambiguity、resume 默认开关与 provider 预算、legacy 策略、cleanup 与公开入口边界。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-17｜C5b 规划批准与首轮 TDD 实现

- 用户决策与范围：用户批准 C5b proposal、design、13 条决策记录、tasks 与 `rag-system` spec delta；提交责任保持用户手动提交。本轮进入实现但不接受 delta、不恢复 IDLE、不归档、不暂存、不提交。
- 已实现：V8 将既有 `async_task` 前向扩展为 document index durable ledger；上传顺序改为 durable input/document → ledger → Redis projection/schedule → response，并由 ledger 生成稳定 taskId；Redis 初始投影失败会把未启动 task 收敛为稳定失败且不返回假 task。
- phase 与恢复：新增 ACCEPTED/SAFE_PRE_VECTOR/VECTOR_IN_FLIGHT/VECTOR_CONFIRMED/FINALIZING/TERMINAL checkpoint；新任务使用 `c5b-v1` deterministic chunk/vector ID，并持久化 contract/chunk config/content hash/chunk count。SAFE_PRE_VECTOR 可复用同一 taskId 重跑；VECTOR_IN_FLIGHT/outcome unknown 转 RECONCILIATION_REQUIRED 且不重放；VECTOR_CONFIRMED/FINALIZING 只重解析校验并完成 DB/keyword/input 收尾，embedding/vector 调用为 0。
- 协调：新增 MySQL 条件 UPDATE claim/lease/heartbeat/release，过期判断使用数据库 `CURRENT_TIMESTAMP(6)`；有界 scan 默认 batch 20、lease 300 秒、heartbeat 60 秒、maxAttempts 3；reconciliation 默认开启、auto resume 默认关闭。Redis 正常 miss/TTL 回源 ledger 并重建投影，ownerId 保留；Redis outage 仍按 C4c fail closed。CLEANUP_PENDING 只执行有界幂等 input delete。
- TDD 证据：依次观察 ledger 类型缺失、acceptance compensation 缺失、vector checkpoint 缺失、vector-confirmed 重复 upsert、deterministic identity 缺失、CAS claim 缺失、Redis durable fallback 缺失、安全 DTO 投影缺失、in-flight quarantine 缺失、confirmed/safe resume 缺失和 cleanup coordinator 缺失等 RED；逐片最小实现后全部转 GREEN。
- 验证：C5b 聚焦测试及 C4c/C4d 故障回归通过；首次 `mvn -q test` 因新 Mapper 放在非 `*.mapper` 包导致 26 个 context 连锁错误，移动至标准 mapper 包后重跑退出码 0；Python `33 tests / OK`；敏感词定向扫描无命中；`git diff --check` 无 whitespace error，仅 3 个既有 CRLF→LF 提示。真实 embedding/rerank/judge/ask/LLM/provider 调用量均为 0。
- 尚未完成/跳过：未运行真实 MySQL 8 的 V1/V7/legacy migration 与双 coordinator 并发验证；未完成 legacy 无 ledger document 标记、attempt exhausted/backoff 稳定终态和 DB finalize 事务/严格幂等；未做真实 crash-window/Redis restart 集成。Docker 型集成按现有环境条件自行跳过，未进行真实 provider resume（需另行预算授权）。
- 范围安全：无前端、API/DTO、retrieval/generation/eval、POM/新依赖、对象存储、MQ、force-resume 入口改动；未修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-17｜C5b 用户收口、已实现 delta 接受与归档准备

- 用户决策：用户确认 C5b 已提交，并明确要求归档 change、恢复空闲状态、等待下一阶段规划。实现提交为 `4c7e4a0 feat(索引): 实现C5b任务协调与安全续跑`；收口开始时工作区干净且 `main...origin/main` 无 ahead/behind。
- 事实校正：active tasks 仍有 legacy 无 ledger 隔离、持续 heartbeat/backoff/attempt exhausted、finalize document-count 严格幂等及真实 MySQL/并发/crash-window 集成验证等未完成项。收口未将这些项目伪装为完成；原 spec delta 收窄为提交中可证明的 durable ledger、DB claim 边界、保守 phase resume、Redis fallback、cleanup 与默认关闭 auto resume，并把剩余项登记到技术债。
- 收口范围：将已实现 delta 接受进 `openspec/specs/rag-system/spec.md`；同步 `.ai/ACTIVE_TASK.md`、`openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/{technical-debt.md,iteration-blueprint.md}` 与 tasks；待结构验证后归档至 `openspec/changes/archive/2026-07-17-index-task-reconciliation-and-resume/`。
- 沿用验证：实现提交前最近一次 `mvn -q test` 退出码 0；Python 33 tests / OK；`git diff --check` 无 whitespace error；真实 provider 业务调用量为 0。本轮只做治理/spec/docs 收口，不修改 Java、migration、配置或测试，因此不重复运行 Maven、Python、Docker/Failsafe 或 provider 调用。
- 范围安全：未修改 API/DTO、前端、POM/依赖、retrieval/generation/eval、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、未提交、未 push、未创建 PR、未部署或发布。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-17｜C5b 归档结构验证

- 结果：active change 目录已移至 `openspec/changes/archive/2026-07-17-index-task-reconciliation-and-resume/`，源目录不存在；`.ai/ACTIVE_TASK.md` 为 `IDLE`。
- 契约：归档 delta 的 4 个 requirements / 12 个 scenarios 与 `rag-system` baseline 接受内容逐行 exact match；未实现保证保留为未勾选 tasks，并已登记到技术债。
- 验证：`git diff --check` 无错误；收口 diff 仅含 OpenSpec、活动指针、架构/路线图与追加式日志，未修改业务实现或受保护本地配置。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-18｜C5 recovery debt closeout 启动

- 用户决策：用户要求检查 C5 阶段债务；若存在则直接修复收口，以便后续进入 C6 规划。审计确认 C5b 归档时登记的 legacy 隔离、持续 heartbeat/backoff/attempt exhausted、DB finalize 严格幂等与真实集成验证仍是当前代码事实。
- 类型与范围：Type C 重大变更 `2026-07-18-c5-recovery-debt-closeout`；不进入 C6，不修改 RAG 质量口径或公开 API。沿用 C5b outcome unknown 禁止重放、auto resume 默认关闭与真实 provider 调用量 0 的批准边界。
- 计划：按 TDD 依次闭环 legacy/稳定终态、有界 coordinator、transactional finalizer，再运行隔离 MySQL/Redis 与 crash-window 验证；只有验证通过后才接受 delta、移除技术债并归档。
- 外部调用：计划中的 embedding 使用 deterministic stub；真实 embedding、rerank、judge、ask/LLM/provider 调用量为 0，无业务数据出站或模型费用。
- 范围安全：当前只新增 OpenSpec 与活动指针/追加式日志；未修改 Java、migration、配置、测试、baseline 或受保护本地文件。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-18｜C5 recovery debt closeout 实现与验收准备

- 范围与修改：完成 legacy 无 ledger document 原子隔离；修复 contract mismatch 跨非终态 phase 的稳定 quarantine；协调器改为固定有界 executor、持续 heartbeat、mutation 前 lease guard、DB-time 指数 backoff 与 max-attempt `FAILED/TERMINAL`；新增独立 `IndexTaskSqlFinalizer`、document row lock、V9 status 扩容与 `(document_id, chunk_index)` 唯一约束。同步 change artifacts、project/architecture/roadmap 与测试。
- TDD 证据：依次观察 legacy 协调 API 缺失、contract mismatch 只允许 VECTOR_IN_FLIGHT、attempt exhausted/retry API 缺失、持续 heartbeat/lease guard 缺失、重复 finalize 无事务 bean 等 RED；每个切片最小实现后聚焦 GREEN。全量真实 MySQL 首轮进一步暴露 `document.status VARCHAR(20)` 无法保存 `RECONCILIATION_REQUIRED`，将 V9 扩为 VARCHAR(32) 后转 GREEN。
- MySQL 真实验证：Docker Desktop 28.4.0 + `mysql:8.0.36` Testcontainers；`C5RecoveryMySqlTest` 4 tests / 0 failures / 0 errors / 0 skipped。覆盖 fresh/V1/V7→V9、legacy generic task 与重复 chunks 兼容、legacy document 实际隔离、双 claimant、heartbeat owner/expiry、DB-time backoff、attempt exhausted、SQL finalize 重复幂等和同事务 rollback；Flyway 9 migrations validate 通过。
- Redis 真实验证：`redis:7-alpine` 隔离容器 stop/start；`RedisFailureSemanticsIT` 2 tests / 0 failures / 0 errors / 0 skipped。覆盖 outage 503、Lettuce 重连、restart 后 projection miss 从 durable store 回源并重建 owner/progress。
- 回归：最终 `mvn -q test` 退出码 0；Surefire 68 reports / 302 tests / 0 failures / 0 errors / 1 skipped，唯一 skip 为需要独立 Milvus 故障环境的 `MilvusFailureSemanticsIT`，C5 MySQL 4 tests 均真实执行且 0 skipped。Python 33 tests / OK；SensitiveLogs 扫描 301 source files / PASS；`git diff --check` 无 whitespace error，仅既有 CRLF→LF 提示。
- 跳过项：未运行前端 build，因为本 change 没有前端改动；未运行真实 embedding/rerank/judge/ask/LLM/provider 调用，实际调用量为 0，无业务数据出站、费用或限流风险；Milvus fault IT 不属于本 change，完整 Maven 中按其既有环境门禁跳过。
- 范围安全：baseline `openspec/specs/` 尚未修改；未修改 API/DTO、前端、retrieval/generation/evaluation、POM/依赖、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险与闸门：实现债务已清空；V9 部署会保留同一 document/chunk_index 的最早行并删除历史重复行，已在 design 决策记录和 V7→V9 真实迁移中验证。仍需用户验收后才能接受 delta、归档 change、恢复 `IDLE` 并进入 C6 规划。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(索引): 收口C5任务恢复与事务幂等债务`。

## 2026-07-18｜C5 recovery debt closeout 验收、baseline 接受与归档

- 用户决策：用户明确确认 C5 债务收口验收通过，并要求更新相关文档状态；本轮不规划 C6。
- 契约与治理：将 change 中 3 个 requirements / 7 个 scenarios 原文接受进 `openspec/specs/rag-system/spec.md`；完成 closeout tasks；同步 `openspec/project.md`、架构与路线图状态；恢复 `.ai/ACTIVE_TASK.md` 为 `IDLE`，并将 change 归档至 `openspec/changes/archive/2026-07-18-c5-recovery-debt-closeout/`。
- 验证依据：沿用同一工作区验收前最终证据——`mvn -q test` 302 tests / 0 failures / 0 errors / 1 个既有 Milvus 环境门禁 skip；C5 MySQL 4 tests 与 Redis 2 tests 均真实执行且 0 skipped；Python 33 tests / OK；SensitiveLogs 301 source files / PASS；真实 provider 调用量为 0。
- 本轮验证：检查归档源/目标、全部 tasks、`ACTIVE_TASK=IDLE`、无未归档 active change、delta-to-baseline exact match、受保护路径与 `git diff --check`。
- 跳过项：本轮只做已验收能力的 baseline 接受、文档状态同步与归档，不修改 Java、migration、配置或测试，因此不重复运行 Maven、Python、前端 build、Docker/Failsafe 或 provider 调用。
- 范围安全：未创建 C6 proposal/change，未改动 C6 规划；未修改 API/DTO、前端、retrieval/generation/evaluation、POM/依赖、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：C5 已登记实现债务清零；V9 部署仍会保留同一 document/chunk_index 的最早行并删除历史重复行，该行为已在 design 与 V7→V9 真实迁移中验证。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 验收归档C5恢复债务收口`。

## 2026-07-18｜C5 recovery debt closeout 实现提交补录

- Commit：`666dd9bb4e8185a8b56f86cc9178b01aa152b837`（`feat(索引): 收口C5任务恢复与事务幂等债务`）。
- 结论：C5 legacy 隔离、有界协调、lease/backoff/attempt 终态、事务幂等收尾及真实 MySQL/Redis 验证已由用户手动提交；本条只补录上一执行提交的真实 hash，不记录本次治理收口提交。

## 2026-07-18｜C6 NVIDIA reranker adapter 与 attribution 规划启动

- 用户决策与提交责任：用户确认 C5 债务已经收口，并明确允许开始 C6 阶段规划；本轮建立 Type C OpenSpec 事前闸门，不进入生产实现。提交责任为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 `main...origin/main [ahead 3]`、工作区干净，HEAD 为 `4fe45c0 docs(openspec): 验收归档C5恢复债务收口`；`.ai/ACTIVE_TASK.md=IDLE`，C5 recovery delta 已接受进 `rag-system` baseline，change 已归档，技术债明确标记 C5 已登记实现债务清零，允许进入 C6 规划。
- 范围与修改文件：新增 `openspec/changes/2026-07-18-nvidia-reranker-adapter-and-attribution/{proposal.md,design.md,tasks.md,specs/rag-system/spec.md}`，并把 `.ai/ACTIVE_TASK.md` 指向该 change；未修改 Java、Python runner、配置、测试、baseline spec、架构/路线图或受保护本地文件。
- 已确认事实：默认 reranker 仍是 heuristic；既有 `ModelReranker` 使用自建 `query + documents / results[].relevance_score` 协议，只有 fake server 单测；registry fallback 主要落日志，debug retrieval 丢失 `RetrievalResult.diagnostics`，因此现有 `enableRerank=true` 报告不能逐样本证明 requested/effective provider 与 fallback 覆盖。
- 规划决策：建议新增独立 `nvidia` provider并保留通用 `model`；使用 typed outcome 把 requested/effective provider、fallback taxonomy、model calls、candidate/scored coverage、latency、model/protocol 合入 retrieval diagnostics；NVIDIA partial/invalid rankings 整次 fallback；raw logit 只决定排序、不伪装概率；debug、同步 QA 和 runner 共用同一归因；C7 才做收益 A/B。12 条真实岔路口已按三行决策记录写入 design，均等待用户事前闸门确认。
- External calls：规划与离线实现测试的真实 embedding/rerank/judge/ask/LLM/provider 调用量均为 0；本轮未读取凭据、无数据出站、费用和限流风险。proposal 仅预留最多 1 次纯合成 NVIDIA ranking smoke 的独立授权闸门，当前未授权、未执行。
- 验证结果：四个必需 artifacts 齐全；唯一未归档 active change 为 C6；design 为 12 decisions / 12 choice / 12 selected / 12 tradeoff lines；spec delta 为 4 requirements / 11 scenarios；tasks 为 3 项已完成、33 项待批准/实现；`ACTIVE_TASK=ACTIVE` 且 change id/path 一致；旧 Gherkin 粘连与 trailing whitespace 扫描无命中；`git diff --check` 通过。OpenSpec CLI 当前不在 PATH，因此未声称 CLI validation 通过。
- 跳过项：本轮仅修改规划/治理文档，没有 Java、Python 或前端实现改动，未重复运行 Maven、Python、前端 build、Docker/Failsafe 或真实 provider smoke；最近一次已验收 C5 证据仍为 Maven 302 tests / 0 failures / 0 errors / 1 个既有 Milvus 环境 skip、Python 33 tests / OK、C5 MySQL/Redis 真实测试通过。
- 范围安全与剩余风险：未修改默认 heuristic、retrieval/generation/citation/no-answer/judge 指标、数据库、索引状态机、POM/依赖、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。proposal/design/tasks/spec delta 尚未获实现批准；实际 NVIDIA deployment/base URL/model/凭据与 live smoke 授权仍未知，未批准前不得写生产代码或产生外调。
- Commit：`pending`。建议用户手动提交：`docs(openspec): 启动C6 NVIDIA重排适配与归因规划`。

## 2026-07-18｜C6 规划批准与 TDD 实现启动

- 用户决策：用户批准 C6 proposal scope/non-goals、design 12 条决策记录、tasks 与 `rag-system` spec delta，并明确要求开始实现；C6/C7 边界保持不变。
- 提交责任：继续为`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外部调用：真实 NVIDIA live smoke 未获单独授权，当前 rerank/embedding/ask/judge/LLM/provider 真实调用预算均为 0；实现与验证仅使用本地合成 HTTP server。
- 实现顺序：按 TDD 小切片依次完成 NVIDIA `/v1/ranking` 协议适配、typed outcome 与整样本 fallback、retrieval/debug/同步 QA attribution、Python runner 逐样本与聚合归因。
- 范围安全：本条只同步批准状态与实现入口；未接受 baseline delta、未恢复 `IDLE`、未归档 change，未修改受保护本地配置。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-18｜C6 NVIDIA reranker adapter 与 attribution 实现完成、待验收

- 实现范围：新增默认关闭的独立 `nvidia` provider，按 `/v1/ranking` 发送 `model + query.text + passages[].text + truncate` 并解析完整唯一的 `rankings[].index/logit`；raw logit 仅决定顺序，原 retrieval score 保留，logit/rank 写独立 metadata。配置使用独立 `NVIDIA_RERANK_*` 变量，不覆盖既有通用 `model` adapter，默认 provider 仍为 heuristic。
- fallback 与归因：新增 typed `RerankOutcome/RerankDiagnostics/RerankProviderException`；registry 统一处理 not_configured、health_check_failed、timeout、http_4xx/http_5xx、network、invalid_response、incomplete_rankings、invalid_input 与 provider_failure。NVIDIA partial/invalid response 整次使用 heuristic，单次 retrieval 不自动 retry，model call count 为 0/1；diagnostics 合入 `RetrievalResult`，与 keyword-only/Milvus degradation 同时保留。
- API 与评测：debug retrieval 改用 `retrieveWithDiagnostics` 并白名单返回 diagnostics；同步 QA metadata 透传实际 contexts 的 attribution。explanatory retry 改为首个非空 fallback 即停止，采用该 retrieval 的 effective provider，并累计初始与 fallback 的真实 model calls、fallbacks、latency；该实现期新增决策 13 待用户验收确认。Python runner 新增逐样本 `rerankAttribution` 与 Markdown/JSON aggregate，不改变既有 Report status 或 retrieval/generation/citation/no-answer/judge 指标公式。
- TDD 证据：依次观察 NVIDIA 类型/配置缺失、typed outcome 缺失、retrieval diagnostics 丢失、debug response 无 diagnostics、runner 无归因提取/聚合、explanatory retry 使用首次 diagnostics 等 RED；最小实现后聚焦 GREEN。NVIDIA adapter 覆盖合法协议、未选择/禁用零调用、重复/越界/缺失/非有限 logit、候选上限、不完整响应整样本 fallback、health、timeout、network、4xx 与 5xx。
- 验证：最终 `mvn -q test` 退出码 0，72 个 XML reports / 318 tests / 0 failures / 0 errors / 1 skipped；唯一 skip 仍为既有 Milvus 独立故障环境门禁。`NvidiaRerankerTest` 8 tests / 0 failures / 0 errors / 0 skipped；Python 35 tests / OK；SensitiveLogs 扫描 305 source files / PASS；`git diff --check` 通过，受保护路径改动 0。
- 外部调用与跳过：真实 NVIDIA live smoke 未获单独授权，明确 `SKIPPED`；本轮真实 embedding/rerank/judge/ask/LLM/provider 调用量均为 0，无业务数据出站、模型费用或限流风险。当前结论仅为 official-schema + local contract tested，真实 endpoint/auth/deployment 未验证，不能宣称真实 NVIDIA 可用或优于 heuristic。
- 范围安全：未修改默认 heuristic、embedding、分块、hybrid/RRF、prompt、citation、no-answer、judge 指标、数据库/迁移、索引状态机、POM/依赖、前端、SSE、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、提交、push、创建 PR、部署或发布。
- 剩余闸门：用户需确认新增 design 决策 13，并决定 live smoke 是授权最多 1 次纯合成 ranking 请求还是接受 real-endpoint-unverified 边界；在用户验收前不接受 delta、不恢复 `IDLE`、不归档 change。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(检索): 实现C6 NVIDIA重排适配与逐样本归因`。

## 2026-07-19｜C6 用户验收、baseline 接受与归档

- 用户决策与提交责任：用户确认实现期新增决策 13，同意 explanatory fallback 首个非空结果即停止；本轮不授权真实 NVIDIA smoke，明确接受 protocol-tested/real-endpoint-unverified 边界。用户将提交责任改为 `Agent 提交`，授权计划内暂存与中文 commit，不包含 push、PR、部署或发布。
- 复验结果：C6 adapter/registry/query/sync QA/debug 聚焦回归退出码 0；最终 `mvn -q test` 退出码 0，72 个 XML reports / 318 tests / 0 failures / 0 errors / 9 skipped。当前 Docker 不可用，C5RecoveryMySqlTest 4 项与 KnownSeedMigrationMySqlTest 4 项按 `disabledWithoutDocker` 跳过；MilvusFailureSemanticsIT 1 项仍按既有独立故障环境门禁跳过。上述跳过均不涉及 C6，NVIDIA adapter 8 tests 全部真实执行通过。
- 其他门禁：Python `35 tests / OK`；SensitiveLogs 扫描 305 source files / PASS；真实 embedding/rerank/judge/ask/LLM/provider 调用量为 0，无数据出站、模型费用或限流风险。前端无改动，因此未运行 frontend build。
- 契约与治理：C6 delta 的 4 个 requirements / 11 个 scenarios 已原文接受进 `openspec/specs/rag-system/spec.md`，delta-to-baseline exact match；同步 `openspec/project.md`、架构、路线图、proposal/design/tasks 与活动指针，恢复 `ACTIVE_TASK=IDLE`，并将 change 归档至 `openspec/changes/archive/2026-07-18-nvidia-reranker-adapter-and-attribution/`。
- 能力边界：默认 provider 继续为 heuristic；本轮只证明 official schema、本地 HTTP contract、fallback 与 attribution 链路，不证明真实 NVIDIA endpoint/auth/deployment 可用，也不提供 NVIDIA 相对 heuristic 的收益结论；C7 A/B 仍需独立 change 与外调授权。
- 范围安全：未修改 embedding、分块、hybrid/RRF、prompt、citation、no-answer、judge 指标、数据库/迁移、索引状态机、POM/依赖、前端、SSE、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未 push、创建 PR、部署或发布。
- Commit：`pending`；提交责任为 Agent。计划提交信息：`feat(检索): 实现并验收C6 NVIDIA重排归因`。

## 2026-07-19｜C6 归档提交补录

- Commit：`33a1e26c999b2412164bd3e1359bc78009a51cba`（`feat(检索): 实现并验收C6 NVIDIA重排归因`）。本条只补录上一执行提交的真实 hash，不记录本次验证文档提交。

## 2026-07-19｜C6 真实 NVIDIA hosted rerank smoke 与依赖复验

- 范围与用户授权：用户独立授权 1 次 NVIDIA 纯合成 rerank smoke，并手动启动 Docker Desktop。本轮只验证 C6 真实 endpoint/auth/schema、项目 adapter 解析和依赖回归；不进入 C7 批量 A/B，不修改默认 provider。
- 外部调用：真实 rerank 请求严格为 1 次、无自动或测试重试；模型 `nvidia/llama-nemotron-rerank-1b-v2`，hosted endpoint 为模型专属 `/v1/retrieval/nvidia/llama-nemotron-rerank-1b-v2/reranking`，timeout 为 20000ms，truncate 为 `END`。出站数据仅为 1 条合成英文问题和 3 条短合成 passages，不含用户、知识库、凭据或业务数据；embedding/ask/judge/LLM 调用量均为 0。
- Smoke 结果：`NvidiaRerankerLiveSmokeTest` 1 test / 0 failures / 0 errors / 0 skipped，退出码 0；真实 response 完整覆盖 3 个候选，合成相关项排第一，requested/effective provider 均为 `nvidia`，fallback=0、model calls=1、coverage=100%，model/protocol 归因为预期值。无 4xx/5xx、timeout、network 或 provider failure；key 仅从 `.env.local` 读入进程环境，未输出或写入 tracked file。
- Docker 与全量回归：Docker Engine 28.4.0 可用；compose 的 MySQL、Redis、Milvus、etcd、MinIO 均为 running/healthy。随后 `mvn -q test` 退出码 0；仅统计本轮新写入的 67 份 Surefire XML reports，共 312 tests / 0 failures / 0 errors / 0 skipped。`C5RecoveryMySqlTest` 4 项与 `KnownSeedMigrationMySqlTest` 4 项均真实执行通过；7 月 17 日遗留的 Milvus skip XML 属于陈旧 build artifact，未计入本轮结果。
- 其他门禁：C6 聚焦 Java 49 tests / 0 failures / 0 errors / 0 skipped；Python 35 tests / OK；SensitiveLogs 扫描 305 source files / PASS；`git diff --check` 通过。临时 live smoke 测试文件已删除，未保留测试专用 endpoint、model 或调用脚手架。
- 跳过与边界：未启动 backend 或执行 debug/QA 业务入口，避免在只授权 1 次 rerank 的情况下额外触发 embedding/rerank/ask；未运行前端 build，因为无前端改动。单次合成 smoke 只确认当前 key、hosted endpoint、schema 与 adapter 可用，不证明生产 SLA、配额长期稳定或 NVIDIA 相对 heuristic 的收益。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、数据库/迁移、索引状态机、retrieval/generation/citation/no-answer/judge 指标或生产默认 provider；未暂存、提交、push、创建 PR、部署或发布。C7 仍须独立 OpenSpec change、固定 KB/fixture/config/Git HEAD 与批量外调授权。
- Commit：`pending`；本轮未获得新的 Agent 提交授权，默认由用户手动提交。建议：`docs(验收): 记录C6真实NVIDIA smoke结果`。

## 2026-07-20｜C7 reranker A/B evaluation 规划启动

- 用户决策与提交责任：用户要求建立 C7 规划文档，待其审阅后再授意执行。提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 `main` 与 `origin/main` 同步、工作区干净，`.ai/ACTIVE_TASK.md=IDLE`，C6 的 NVIDIA adapter/attribution 契约已接受并归档，归档后单次合成 hosted smoke 已验证 endpoint/auth/schema；默认 provider 仍为 heuristic，收益 A/B 尚未验证，C7 是冻结路线图下一阶段。
- 类型与范围：建立 Type C change `2026-07-20-reranker-ab-evaluation`，新增 proposal/design/tasks 与 `evaluation` spec delta，并激活活动任务指针。规划范围限定为 Python retrieval-only runner、可复现身份、sanitized arm manifest、离线 comparator、P50/P95、单元测试与评测文档；未进入实现。
- 已确认事实：现有 runner 已保存逐样本 requested/effective provider、fallback、model calls、candidate coverage、model/protocol 与 rerank latency；现有 aggregate 尚无 P50/P95，reproducible metadata 尚无 eval-set hash/runtime arm identity，也没有两 arm identity/coverage/pairing comparator。
- 规划决策：建议两个独立 arm + 离线 comparator；strict identity 只对白名单 provider 字段放行；model arm 对全部 rerank-eligible observations 要求 100% effective-model 与 0 fallback；zero-candidate 配对保留但不计 coverage；per-run 继续为 `RETRIEVAL_ONLY`，另设 comparison validity；rerank latency 为主、retrieval wall-clock 为辅；不删失败样本、不自动切默认 provider、不吞并 C8/C9/C10。design 共 15 条决策，均等待用户确认。
- 外部调用：本轮只写规划文档，真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0，无数据出站、费用或限流风险。最低执行候选 `R=1,W=0` 上限为 60 debug retrieval / 60 query embedding / 30 model rerank；建议 latency 候选 `R=3,W=3` 上限为 186 / 186 / 93，仅供审阅，均未获执行授权。
- 验证计划：规划文件结构、requirements/scenarios/decision 数量、活动指针、断链/旧字段/受保护路径与 `git diff --check`；本轮不重复 Maven/Python/前端测试，因为没有代码改动。上一只读 readiness 已验证 Python 35 tests / OK，Maven 312 tests / 0 failures / 0 errors / 8 Docker-unavailable skips，但不把该结果当作 C7 实现验证。
- 范围安全：未修改 baseline spec、Java、Python runner、测试、默认 provider、评测集、fixture、数据库、前端、POM/依赖、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未启动 backend、Docker 或真实评测。
- 剩余闸门：用户需审阅并批准 proposal scope/non-goals、15 条 design decisions、4 requirements / 11 scenarios、离线实现授权，以及后续 canary/full 的独立外调预算；批准前不得改 runner 或产生真实调用。
- Commit：`pending`。建议用户手动提交：`docs(openspec): 启动C7重排A-B评测规划`。

## 2026-07-20｜C7 规划文档验证完成、待用户审阅

- 验证范围：只验证 `2026-07-20-reranker-ab-evaluation` 的 proposal/design/tasks/evaluation spec delta、活动任务指针与追加式日志；未验证任何尚未实现的 runner/comparator 能力。
- 结构结果：4 个必需 artifacts 均存在；proposal 含唯一“用户故事（大白话）”；design 决策编号 1~15 连续，choice/selected/tradeoff 三行各 15 条且全部标为待用户确认；spec delta 为 4 requirements / 11 scenarios，WHEN/THEN 各 11 条；tasks 为 4 项已完成的规划事实、54 项待批准/实现/执行。
- 指针与范围结果：`.ai/ACTIVE_TASK.md` 精确为 `ACTIVE` 并指向本 change；`openspec/changes/` 下唯一未归档 change 为 C7；baseline `openspec/specs/` 无 diff；受保护路径改动 0。
- 文档门禁：`git diff --check` 通过；Git 仅输出用户级 ignore 文件无权限的既有 warning，不影响仓库 diff。OpenSpec CLI 当前不在 PATH，因此未声称 CLI validation 通过。
- 跳过项：本轮没有 Java、Python runner、前端、依赖或配置实现改动，因此未运行 Maven、Python、前端 build、Docker/Failsafe、backend preflight 或真实 A/B。真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0。
- 剩余风险：规划中的 runtime arm manifest 仍依赖实际启动流程提供，并需 observed attribution 交叉校验；30 条开发样本外推有限；正式 repeats/warm-up、provider 配额、费用与限流尚待用户审阅和执行前单独授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C7重排A-B评测规划`。

## 2026-07-20｜C7 规划提交补录

- Commit：`3a6b328f11bf2d04a65345897091c4a090232be7`（`docs(openspec): 启动C7重排A-B评测规划`）。
- 结论：C7 proposal/design/tasks/evaluation delta 与活动指针已由用户手动提交；本条只补录上一规划提交的真实 hash，不记录本次实现提交。

## 2026-07-20｜C7 规划批准与 TDD 实现启动

- 用户决策：用户批准 proposal scope/non-goals、15 条 design decisions、4 requirements / 11 scenarios，并要求按推荐方案执行；选择 `R=3,W=3`、arm 顺序交替、canary→full。遇到 fallback、429、身份漂移或调用量异常时停止并在对话中请示。
- 提交责任：继续为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 费用与限流：用户明确确认 NVIDIA NIM 免费、无需支付费用；该信息作为用户提供的外部依据记录，不包装成代码可证事实。速率/并发限制可能存在，因此实现与真实执行保持串行、C6 零自动 rerank retry，canary 先验证再进入 full。
- 实现范围：按 TDD 纵向切片依次完成 per-sample retrieval latency/P50/P95、C7 sanitized arm manifest与可复现 identity、离线 comparator 与 compact evidence、评测指南；不修改 Java/API、默认 heuristic、评测集或后续 C8/C9/C10 范围。
- 外部调用：当前仅启动离线实现，真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0。正式 `R=3,W=3` 上限仍为 186 debug retrieval / 186 query embedding / 93 model rerank；ask/judge/LLM generation=0，固定问题与 fixture passages 之外不出站。
- Commit：`pending`；提交责任为用户手动提交。

## 2026-07-20｜C7 离线实现与质量门禁完成

- 范围与修改：在 `run_rag_eval.py` 增加成功/失败均记录的 monotonic debug retrieval wall-clock，以及 retrieval/rerank 独立 nearest-rank count/min/P50/P95/max；在 reproducible runner 增加 exact-whitelist arm manifest、eval-set/sample/run/warm-up identity、独立 warm-up outputs 与可交替执行的 `--run-index/--skip-warmup`；新增严格离线 comparator、四份 canary/full 脱敏 manifest、单元测试和 C7 操作指南。未修改 Java/API、默认 heuristic、评测集、fixture、依赖或前端。
- TDD 证据：先后观察 latency helper/metadata/manifest/warm-up/comparator 模块缺失、identity 漂移未拒绝、fallback/zero-candidate/missing-pair 未拒绝、compact schema/source hash/跨 repeat paired median 缺失等 RED；最小实现后全部 GREEN。comparator 只在 `COMPARABLE` 时输出 Recall@5/MRR/Top1 delta，并在 `NOT_COMPARABLE` 时保留 provider/fallback/missing-pair 诊断但隐藏收益。
- 离线调用计划：plan-only 固定 30 条、`R=3,W=3`，heuristic/model 分别为 93 次 debug retrieval/query embedding 上限，model arm 另有 93 次 NVIDIA rerank 上限，合计 186/186/93；3 样本 canary 两 arm 合计为 12/12/6。ask/judge/generation 均为 0。上述均为计划计算，本轮真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0，无数据出站。
- 验证：聚焦 comparator/latency/metadata 测试通过；`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 59 tests / OK；SensitiveLogs 扫描 306 source files / PASS；`mvn -q test` 最终退出码 0，70 份 Surefire XML / 315 tests / 0 failures / 0 errors / 1 skipped，唯一 skip 为既有独立 Milvus 故障环境门禁。Maven 首次在沙箱内因本机依赖缓存权限失败，首次沙箱外运行仅因 120 秒工具超时终止，延长到 5 分钟后 108.8 秒通过。
- 文档与安全：4 requirements / 11 scenarios、15 条决策结构保持不变；`git diff --check` 通过；changed Markdown 无待解析相对链接；脱敏 manifests 的 sensitive key/field 扫描 0，受保护路径改动 0。compact evidence 单测确认不复制 question、contexts、passages 或 raw response。前端无改动，正式 build 按计划 `SKIPPED`。
- 执行口径：指南按已批准总预算将 warm-up 解释为每个 logical arm 总计 3 次，通过 `H1/N1、N2/H2、H3/N3` 和后续 `--skip-warmup` 保持 model 上限 93。若改为每次 backend 重启都做 3 次 warm-up，model 上限会升至 99，必须重新取得用户授权。
- 剩余闸门：正式 canary 不能在当前未提交实现上形成可靠 Git HEAD 证据。提交责任为用户手动提交，因此需用户先审阅并提交本轮 runner/comparator/manifests/docs；提交后再披露 canary 精确 provider/model/12 debug/12 embedding/6 rerank、固定 3 个问题与 fixture passages 出站、免费依据与限流风险，并执行 mutation-free preflight。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、数据库/迁移、索引状态机、embedding、分块、hybrid/RRF、prompt、citation、no-answer、judge、生产默认 provider；未暂存、提交、push、创建 PR、部署或发布。
- Commit：`pending`；建议用户手动提交 `feat(评测): 实现C7重排A-B离线评测工具`。

## 2026-07-20｜C7 离线实现复核补录与 canary 前置状态

- 复核补录：在上一条日志后新增 details 自一致性/retrieval error 与 unreadable-schema `FAILED` evidence 两条 comparator 回归，最终 Python 为 61 tests / OK；SensitiveLogs 仍为 306 source files / PASS，`git diff --check` 仍通过。上一条的 59 tests 是补测前的阶段结果，本条以 61 为当前最终值；Maven 315/0/0/1 结果未受纯 Python 补测影响。
- Readiness：沙箱外只读 `docker compose ps` 显示 MySQL、Redis、Milvus、etcd、MinIO 均已运行且 healthy；本机 8080 当前没有 backend listener。`.env.local` 存在，但本轮只检查存在性，没有输出或写入任何凭据；当前 Codex 进程环境未直接设置 eval username/password 或 NVIDIA key。
- 当前阻塞：工作区包含未提交的 C7 runner/comparator/manifest/docs 实现。由于提交责任为用户手动提交，必须先由用户提交并回复真实 hash，随后才能让 canary metadata 的 Git HEAD 锚定本次实现。真实外部调用量仍为 0。
- Commit：`pending`；建议 `feat(评测): 实现C7重排A-B离线评测工具`。

## 2026-07-20｜C7 canary 凭据存在性预检

- 只读结果：`.env.local` 中存在 `NVIDIA_API_KEY`，并将 embedding model 配置为 `nvidia/llama-nemotron-embed-1b-v2`；没有独立 `NVIDIA_RERANK_API_KEY`，后续启动时可在进程内将 rerank key 映射到已有 NVIDIA key，不写回文件。未读取、输出或记录任何 key 值。
- 阻塞项：`.env.local` 与当前进程均没有 `RAG_EVAL_USERNAME/RAG_EVAL_PASSWORD`。runner 按安全契约拒绝隐式或默认登录凭据；用户需在本地安全文件补齐这两个变量，不能在对话中粘贴密码。
- 外部调用：本预检只读取允许的非秘密配置值与 credential presence boolean，真实 embedding/rerank/ask/judge/LLM/provider 调用量仍为 0。

## 2026-07-20｜C7 离线实现提交补录

- Commit：`40f94068c28173f938b55ddfc9e54385c781270e`（`feat(评测): 实现C7重排A-B离线评测工具`）。本条只补录上一执行提交的真实 hash，不记录本次 canary 证据修正提交。

## 2026-07-20｜C7 首轮 3 样本 reranker A/B canary

- 授权与范围：用户确认凭据已补齐并批准 canary 上限 12 次 debug retrieval / 12 次 query embedding / 6 次 NVIDIA rerank；固定 `fact-001`、`fact-006`、`definition-001`，每 arm 3 次 warm-up + 3 次 measured。出站只含固定评测问题，NVIDIA arm 另含固定 fixture 检索 passages；ask/judge/LLM generation 为 0。
- Readiness：Docker 的 MySQL、Redis、Milvus、etcd、MinIO 均 running/healthy；mutation-free preflight 复用 KB 15、collection `kb_ff06e2ea3de24fb4`、3 documents / 50 chunks，未创建、上传、删除或重建资源。Git HEAD 为 `40f94068c28173f938b55ddfc9e54385c781270e`，eval-set SHA-256 为 `d17bde69db58848fe79069709a7b7c3c927da916661faa8caf1bd71efcd6d7fe`。
- Heuristic arm：warm-up 与 measured 合计 6/6 requested/effective heuristic、fallback=0、model calls=0、candidate coverage=100%；measured 为 `RETRIEVAL_ONLY`，retrieve errors=0、Recall@5=0.8333、MRR=1.0、Top1=1.0、retrieval P50=765ms、rerank P50=0ms。
- NVIDIA arm：模型 `nvidia/llama-nemotron-rerank-1b-v2`、protocol `nvidia-ranking-v1`、timeout 20000ms、truncate END、串行且 retry=0。warm-up 与 measured 合计 6/6 requested nvidia、effective heuristic、fallback=`http_4xx`、model calls=1、candidate coverage=100%；measured 为 `RETRIEVAL_ONLY`、retrieve errors=0，指标来自 fallback 后 heuristic，不是模型收益。
- Comparator：`NOT_COMPARABLE`，原因 `model_provider_mismatch`、`model_fallback_observed`、`model_coverage_incomplete`、`manifest_observation_mismatch`；收益 delta 被正确隐藏。raw evidence 保留在本地 `tmp/eval/`，compact summary 为 `docs/eval/reports/c7-canary-2026-07-20.md`，其中记录 5 个原始文件的 bytes 与 SHA-256。
- 实际调用：12 次 debug retrieval、至多 12 次 query embedding、6 次 NVIDIA rerank，ask/judge/LLM generation=0；无自动 retry，未超过授权。Runner 的 rate-limit counter 为 0，但不区分 rerank 4xx；当前安全归因只记录 `http_4xx`，因此精确状态（包括是否 429/402/422）未知。
- 停止与诊断：触发 fallback 停止条件后未进入 full A/B，并停止本轮 backend。首轮 runtime 使用 `https://integrate.api.nvidia.com` 作为 rerank base URL；NVIDIA 当前官方模型 API reference 指向 `https://ai.api.nvidia.com/v1/retrieval/nvidia/llama-nemotron-rerank-1b-v2/reranking`，官方 Retriever quickstart 也区分 embedding 的 integrate host 与 reranker 的 ai host。因此 host 漂移/误配为高置信推断，仍须获批的 corrected-host model-only canary 证明。
- 文档修正：将指南的 rerank base URL 改为当前官方 `https://ai.api.nvidia.com`，新增 `/tmp/eval/` Git ignore，避免 raw questions/passages/response evidence 被误提交；同步 proposal、tasks 与活动任务指针。本轮未修改 Java/provider/API、默认 heuristic、评测集、fixture、数据库、索引、embedding、prompt、citation、no-answer、judge、依赖或前端。
- 剩余闸门：由用户手动提交本次修正后，再单独授权相同 3 样本的 corrected-host model-only canary；建议新增上限 6 debug retrieval / 6 query embedding / 6 NVIDIA rerank，ask/judge/LLM generation=0、串行、无 retry、不覆盖首轮 raw evidence。Full A/B 继续禁止。
- Commit：`pending`；建议 `docs(评测): 记录C7 canary失败证据并修正NVIDIA主机`。

## 2026-07-20｜C7 首轮 canary 证据修正提交补录

- Commit：`8f297a818e26855d2873488abcdd2d780d03439c`（`docs(评测): 记录C7 canary失败证据并修正NVIDIA主机`）。本条只补录上一文档提交的真实 hash，不记录本次 corrected-host canary 证据提交。

## 2026-07-20｜C7 corrected-host model-only canary 通过

- 用户授权：用户批准以 `https://ai.api.nvidia.com` 重跑 model-only canary，新增上限为 6 次 debug retrieval / 6 次 query embedding / 6 次 NVIDIA rerank；不重复 heuristic，ask/judge/LLM generation=0、串行、无自动 retry，异常立即停止。
- 身份处理：主工作区在首轮证据提交后 HEAD 为 `8f297a818e26855d2873488abcdd2d780d03439c`，既有 heuristic arm 为 `40f94068c28173f938b55ddfc9e54385c781270e`。为不伪造 metadata 且保持 strict Git identity，从临时 detached worktree `40f9406` 启动 backend 与 runner；该 commit 与当前 HEAD 在 runner/Java/config 上无差异，执行后临时 worktree 已安全移除。
- Readiness：Docker 的 MySQL、Redis、Milvus、etcd、MinIO 均 running/healthy；mutation-free preflight 两次通过，复用 KB 15、collection `kb_ff06e2ea3de24fb4`、3 documents / 50 chunks，未创建、上传、删除或重建资源。第一次在沙箱内运行 runner 因临时 worktree ACL 在创建 `tmp/eval` 前失败，未进入 warm-up、未产生业务外调；随后获准在沙箱外执行。
- Model arm：模型 `nvidia/llama-nemotron-rerank-1b-v2`、protocol `nvidia-ranking-v1`、base URL `https://ai.api.nvidia.com`、模型专属 endpoint path、timeout 20000ms、truncate END。Warm-up 与 measured 合计 6/6 requested/effective nvidia、fallback=0、model calls=1、candidate coverage=100%；两轮 report status 均为 `RETRIEVAL_ONLY`、retrieve errors=0、retry=0。
- Comparator：既有 clean heuristic 与 corrected model arm 为 `COMPARABLE`，strict identity、pairing、provider coverage 均通过，无 missing/zero-candidate mismatch/fallback。3 样本 Recall@5=0.8333、MRR=1.0、Top1=1.0，三项 delta 均为 0；model retrieval P50/P95=1172/1187ms，rerank P50/P95=349/351ms。该 canary 只证明调用链与比较闸门，不代表 30 样本收益或生产 SLA。
- 调用事实：本次实际 6 次 debug retrieval、至多 6 次 query embedding、6 次 NVIDIA rerank；ask/judge/LLM generation=0、自动 retry=0。Backend 记录 6 次 debug retrieval 200，fallback warning=0、`RerankProviderException`=0、runtime 429 marker=0。全部 C7 canary 尝试累计为 18 次 debug retrieval、至多 18 次 query embedding、12 次 NVIDIA rerank，均在各自授权内。
- 证据与安全：corrected raw outputs 使用独立 `c7-canary-nvidia-ai-host*` / `c7-canary-ai-host-comparison*` 文件名，首轮失败证据未覆盖；8 份 raw evidence 的 bytes/SHA-256 已写入 `docs/eval/reports/c7-canary-corrected-host-2026-07-20.md`。Backend 已停止、8080 已释放；未修改 `.env.local`、application 配置、Java/provider/API、默认 heuristic、评测集、fixture、数据库、索引、embedding、prompt、citation、no-answer、judge、依赖或前端。
- 剩余闸门：Full `R=3,W=3` 仍需用户基于 canary 单独批准新增上限 186 debug retrieval / 186 query embedding / 93 NVIDIA rerank，以及相同出站、免费依据与限流风险；未获授权不得执行。
- Commit：`pending`；建议 `docs(评测): 记录C7 corrected-host canary通过证据`。

## 2026-07-20｜C7 corrected-host canary 证据提交补录

- Commit：`fb18b6bd5448db6e0985f98f44268da84195bb1b`（`docs(评测): 记录C7 corrected-host canary通过证据`）。本条只补录上一文档提交的真实 hash，不记录本次 full A/B evidence 提交。

## 2026-07-20｜C7 full `R=3,W=3` reranker A/B 完成

- 用户授权：用户批准 full 新增上限 186 次 debug retrieval / 186 次 query embedding / 93 次 NVIDIA rerank；固定 30 条问题与 fixture passages，ask/judge/LLM generation=0，串行、无自动 rerank retry，遇 fallback/429/身份漂移/超额立即停止。费用依据继续为用户确认的 NVIDIA NIM 免费，可能存在速率/配额限制。
- Readiness：首次继续时 Docker Desktop Engine 未运行，调用量为 0 并请用户启动；恢复后 MySQL、Redis、Milvus、etcd、MinIO 均 running/healthy。主工作区 HEAD `fb18b6bd5448db6e0985f98f44268da84195bb1b`、工作区干净、凭据存在、8080 空闲。四次 backend 启动后的 mutation-free preflight 均复用 KB 15、collection `kb_ff06e2ea3de24fb4`、3 documents / 50 chunks，未创建、上传、删除或重建资源。
- 执行顺序与覆盖：按 `H1/N1、N2/H2、H3/N3` 完成。Heuristic warm-up 3/3 与 measured 90/90 均 requested/effective heuristic、fallback=0、model calls=0；NVIDIA warm-up 3/3 与 measured 90/90 均 requested/effective nvidia、fallback=0、model calls=1、candidate coverage=100%。六个 measured reports 均 `RETRIEVAL_ONLY`、retrieve errors=0，missing pair/zero-candidate mismatch=0。
- Comparator：`COMPARABLE`，三次 repeat 均复现 heuristic Recall@5/MRR/Top1=`68.63%/0.7346/96.30%`，NVIDIA=`76.47%/0.8241/100%`；delta 为 +7.84pp/+0.0895/+3.70pp。样本级首次 run 中 Recall 改善 `reasoning-003/reasoning-006`，MRR 改善 `definition-003/fact-008/reasoning-003/reasoning-006`，Top1 改善 `reasoning-006`，answerable 样本未观察到对应回退。
- 延迟：90 observations 聚合的 heuristic/model retrieval P50/P95 为 797/5203ms 与 985/2796ms；model rerank stage P50/P95 为 363/688ms。H1 在 Docker 刚启动后即使 warm-up 3 次仍出现 P95=14484ms，而 H2/H3 为 2016/2031ms；因此 aggregate model P95 较低只作为冷启动诊断，不解释为 model 尾延迟收益。可信成本信号为 rerank stage 363/688ms 与 overall P50 +188ms。
- 实际调用：四个 backend 运行段分别记录 33、60、63、30 次 debug retrieval 200，总计 186；query embedding 不超过 186；NVIDIA model calls 精确为 93。Ask/judge/LLM generation=0、自动 retry=0、fallback warning=0、`RerankProviderException`=0，未观察到真实 HTTP 429。全部 C7 canary + full 累计 204/至多 204/105，均在分次授权内。
- 文档与证据：新增 `docs/eval/reports/c7-reranker-ab-full-2026-07-20.md`，同步 proposal/design/tasks、`openspec/project.md`、架构、优化索引与技术债；18 份 raw details/metadata/comparison 的 bytes/SHA-256 记录在 compact evidence，raw files 继续保留在 Git-ignored `tmp/eval/`。
- 安全与边界：四个 backend 均已停止，8080 已释放；未修改 `.env.local`、application 配置、Java/provider/API、默认 heuristic、评测集、fixture、数据库、索引、embedding、prompt、citation、no-answer、judge、依赖或前端。30 条开发样本不外推生产收益；C7 不自动切换默认 provider、不接受 delta、不归档。
- 剩余闸门：用户需验收 full evidence、延迟异常解释、默认 provider 不变与外推边界；确认后才能接受 evaluation delta、恢复 `ACTIVE_TASK=IDLE` 并归档 change。
- Commit：`pending`；建议 `docs(评测): 记录C7 full A-B可比较证据`。

## 2026-07-20｜C7 full evidence 提交补录

- Commit：`b56f22c8eeece0826499ec6851a1495b40e9650e`（`docs(评测): 记录C7 full A-B可比较证据`）。本条只补录上一 full evidence 提交的真实 hash，不记录本次验收归档提交。

## 2026-07-20｜C7 用户验收、evaluation baseline 接受与归档

- 用户决策：用户验收 C7 full evidence、质量/延迟结论与外推边界，接受 `evaluation` delta 并授权归档；默认 reranker 明确继续保持 heuristic，未来如需切换须另立 Type C change。
- 契约与治理：C7 delta 的 4 个 requirements / 11 个 scenarios 已原文接受进 `openspec/specs/evaluation/spec.md`；`2026-07-20-reranker-ab-evaluation` 已移入 `openspec/changes/archive/`，tasks 全部完成，`.ai/ACTIVE_TASK.md` 已恢复 `IDLE`，当前无未归档 change。同步 `openspec/project.md`、架构、技术债、优化索引与 full evidence 的 accepted 状态。
- 验证：delta-to-baseline exact match 通过（4 requirements / 11 scenarios）；archive structure、必需 artifacts、tasks 全勾选、`ACTIVE_TASK=IDLE`、无未归档 change 均通过；Python 全量为 61 tests / OK；SensitiveLogs 扫描 306 source files / PASS；`git diff --check` 通过；11 个 changed Markdown 文件的本地相对链接检查通过；当前事实源无“待用户验收/尚未接受”残留；8080 无监听。
- 跳过项：OpenSpec CLI 不在 PATH，因此只执行文件级 exact-match、结构与状态校验，未声称 CLI validation 通过。本轮仅接受契约与归档文档，无 Java、Python、前端、依赖或运行时配置改动，因此未重复运行 Maven、frontend build、Docker/Testcontainers 或 live provider；本轮真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0。
- 范围安全：默认配置仍为 `rag-admin/src/main/resources/application.yml` 中 `provider: heuristic`；未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、Java/provider/API、评测脚本、评测集、fixture、数据库/迁移、索引、embedding、分块、hybrid/RRF、prompt、citation、no-answer、judge、依赖或前端；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：30 条开发样本只支持当前固定身份下的 retrieval 观察结论；NVIDIA 长期速率、配额、并发与生产 SLA 未由 C7 证明。H1 冷启动污染 aggregate retrieval P95，不能据此宣称 model 尾延迟更快。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 验收并归档C7重排A-B评测`。

## 2026-07-21｜C8a eval dataset schema/versioning 规划启动

- 用户决策与提交责任：用户要求开启 C8a 规划。提交责任按仓库默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 `main` 与 `origin/main` 同步、工作区干净，`.ai/ACTIVE_TASK.md=IDLE`，C7 的 4 requirements / 11 scenarios 已接受进 `evaluation` baseline 并归档，当前无未归档 change；C8a 是冻结路线图下一项，C8b 继续串行等待。
- 范围与修改：建立 Type C change `2026-07-21-eval-dataset-schema-and-versioning` 的 proposal/design/tasks 与 `evaluation` spec delta，并把 `.ai/ACTIVE_TASK.md` 激活。规划只覆盖 dataset release/schema/annotation/fixture version、共享本地 validator、runner fail-fast/metadata、版本演进、测试与评测指南；未进入实现，未修改 baseline spec、runner、评测集、fixture、Java/API、配置、数据库、前端或依赖。
- 已确认事实：当前 eval-set 为 30 条，类型分布 fact 10 / definition 8 / reasoning 6 / multi_hop 3 / no_answer 3，SHA-256 为 `d17bde69db58848fe79069709a7b7c3c927da916661faa8caf1bd71efcd6d7fe`；现有 runner 已记录 eval/fixture/config/Git/KB identity，但只做 JSON/object 解析，缺少统一 schema/version/conditional semantics validator。
- 规划 artifacts：proposal 明确用户故事、scope/non-goals、零外调与验收门禁；design 提供 16 条待用户确认的真实岔路决策；delta 为 4 requirements / 13 scenarios；tasks 把用户批准与实现授权保留为未完成闸门。`.ai/ACTIVE_TASK.md` 只指向本 change。
- 验证：change 下 4 个必需文件齐全；decision/requirement/scenario 计数为 16/4/13；baseline spec diff 0、受保护路径 diff 0；`git diff --check` 通过；SensitiveLogs 扫描 306 source files / PASS；规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0、数据出站为 0。
- 跳过项：OpenSpec CLI 不在 PATH，因此未声称 CLI validation 通过；本轮为 doc-only 规划且未改 Python/Java/POM/前端/运行时配置，未运行 Python/Maven/frontend build、Docker/Testcontainers 或 live provider。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、`openspec/specs/`、评测集、fixture、业务代码、生产默认 provider 或历史报告；未创建 C8b 内容或提前声称 C8a 已实现。
- 剩余风险：用户尚未批准 proposal、16 条 design decisions 和 4/13 delta；version 命名、严格 unknown-field、逻辑 KB identity 与是否保留显式 `UNVERSIONED` 诊断模式仍在事前闸门等待确认。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C8a评测数据版本治理规划`。

## 2026-07-21｜C8a 规划提交补录

- Commit：`7b4542b261286025a1ab6fdd99e0f7e20ff0843f`（`docs(openspec): 启动C8a评测数据版本治理规划`）。本条只补录上一规划提交的真实 hash，不记录本次 C8a 实现提交。

## 2026-07-21｜C8a 规划批准与 TDD 实现启动

- 用户决策：用户批准 proposal scope/non-goals/version semantics、design 16 条决策和 `evaluation` delta 的 4 requirements / 13 scenarios，并明确授权进入 TDD 实现。
- 提交责任：继续为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 实现边界：按纵向 RED→GREEN 切片实现 tracked release manifest、项目级 sample schema、共享标准库 validator、direct/reproducible runner 前置 fail-fast 与兼容 metadata；当前 30 条 JSONL 和 3 份 fixture bytes 保持不变，不进入 C8b/C9/C10/C14。
- 外部调用：C8a 默认 acceptance 全部本地完成，真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0，数据出站为 0；不启动 backend/Docker/live provider。
- Commit：`pending`。

## 2026-07-21｜C8a 评测数据版本治理 TDD 实现完成（待验收）

- 范围与修改：新增 `docs/eval/dataset-manifest.json`、`docs/eval/schema/rag-eval-sample-v1.json`、`scripts/eval_dataset_contract.py` 与聚焦测试；修改 direct/reproducible runner 及测试，增加前置 dataset validation、release identity metadata/report 和显式 `UNVERSIONED` 降级；同步评测指南、proposal/design/tasks、项目上下文、架构、技术债、优化索引与活动任务指针。
- 已确认事实：首个 `rag-eval-dev-v1` 固定 30 条 question set、3 份 fixture、`rag-eval-sample-v1` schema、逻辑 KB contract 与 type/difficulty/shouldAnswer distribution。JSONL SHA-256 仍为 `d17bde69db58848fe79069709a7b7c3c927da916661faa8caf1bd71efcd6d7fe`；3 份 fixture SHA-256 仍为 `c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521`、`a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0`、`59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`，未为通过 schema 修改样本或 fixture。
- TDD 与行为：RED→GREEN 覆盖缺 version、unsafe/absolute path、缺失/hash drift artifact、非 object、missing/unknown/type/enum/ID pattern/duplicate、answerability、fixture source、context 和 distribution；两个 runner 的 invalid/drift 测试证明 login/KB stub call count=0。正式路径记录 `VALID` identity；custom 输入默认拒绝，仅显式 `--allow-unversioned-eval-set` 时标为 `UNVERSIONED`，且 `Metrics safe for comparison=no`。
- 验证：validator 18 tests / OK；direct + reproducible runner 49 tests / OK；Python 全量 `python -B -m unittest discover -s scripts -p 'test_*.py'` 为 86 tests / OK；direct/reproducible current-release plan 均返回 `VALID`、30 samples、3 fixtures、完整 version/hash/distribution，实际业务调用为 0；SensitiveLogs 扫描 307 source files / PASS；定向 secret pattern、受保护路径与 10 个 C8a 链接目标检查通过；`git diff --check` 通过。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；无 Java/POM/前端/依赖/运行时配置改动，因此 Maven、frontend build 与 Docker/Testcontainers 均 `SKIPPED`；C8a acceptance 为纯本地 contract 验证且用户未另行授权业务外调，因此 live backend/provider smoke `SKIPPED`。实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量均为 0，数据出站为 0。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、`openspec/specs/` baseline、Java/API、数据库/迁移、前端、依赖、生产默认 provider、retrieval/chunking/rerank/prompt/citation/no-answer/judge 公式或历史 C7 报告；未进入 C8b/C9/C10/C14，未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险与闸门：30 条开发数据仍不能外推生产分布或 SLA；Git 保证已提交 release 的历史不可变性，运行时 metadata mismatch 额外 fail closed，但正式 release 演进仍依赖评审时遵守 bump matrix。等待用户验收后才能把 4 requirements / 13 scenarios 接受进 baseline、归档并恢复 `ACTIVE_TASK=IDLE`。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(评测): 实现C8a评测数据版本治理`。

## 2026-07-22｜C8a TDD 实现提交补录

- Commit：`2065ed2`（`feat(评测): 实现C8a评测数据版本治理`）。本条只补录上一实现提交的真实 hash，不记录本次验收归档提交。

## 2026-07-22｜C8a 用户验收、evaluation baseline 接受、归档与 C8b readiness

- 用户决策：用户验收 C8a schema/manifest、version bump、validator、runner compatibility 与结论边界，并要求按流程完成 baseline 接受、归档和后续 C8b readiness 判断。
- 契约与治理：C8a delta 的 4 requirements / 13 scenarios 已原文接受进 `openspec/specs/evaluation/spec.md`；`2026-07-21-eval-dataset-schema-and-versioning` 已移入 `openspec/changes/archive/`，tasks 全部完成，`.ai/ACTIVE_TASK.md=IDLE`，当前无未归档 change。同步 `openspec/project.md`、架构、技术债与优化索引的 accepted 状态。
- 验证：delta-to-baseline ordinal exact match 通过（4 requirements / 13 scenarios）；archive structure、必需 artifacts、tasks 全勾选、`ACTIVE_TASK=IDLE`、无未归档 change 和当前事实源待验收残留检查通过；Python 全量为 86 tests / OK；current release reproducible plan 返回 `VALID`、30 samples、3 fixtures 和完整 version/hash/distribution，实际业务调用为 0；SensitiveLogs 扫描 307 source files / PASS；`git diff --check` 通过。
- C8b readiness 分类：`confirmed` 为 C8a 治理前置已接受、当前 release 可验证、路线图下一串行 change 明确为 `eval-dataset-expansion-and-annotation`；`partial` 为 C8b 目前只有“扩充至 100～300 条并明确分类配额”的意图卡；`planned` 为独立 Type C proposal/design/tasks/evaluation delta；`out_of_scope` 为 C9 claim/judge、C10 quality gate、C14 权限隔离/恶意文档样本以及默认 provider/指标公式变更；`unknown` 为目标总量、type/difficulty/answerability 配额、fixture 扩展策略、题目来源、标注复核与去重/泄漏规则。
- Readiness 结论：C8b **可以启动规划，但不能在本收口工作区直接开始实现**。唯一流程性前置是用户先手动提交本次 C8a 验收归档，使工作区恢复干净；提交后可创建独立 `2026-07-22-eval-dataset-expansion-and-annotation` change，先审 proposal、配额与数据/标注来源，再决定是否授权写入新 release。当前未知项是 C8b 设计输入，不阻塞规划启动。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；本轮仅 baseline/归档/长期文档改动，无 Java、Python 实现、POM、前端、依赖或运行时配置变化，因此 Maven、frontend build、Docker/Testcontainers 与 live provider smoke 均 `SKIPPED`。真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0，数据出站为 0。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、评测 JSONL/fixture/schema/manifest、Java/API、数据库、前端、依赖、生产默认 provider、retrieval/chunking/rerank/prompt/citation/no-answer/judge 公式或历史报告；未创建 C8b change、未新增/重写样本，未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：当前 30 条开发样本仍不能外推生产分布；C8b 若新增或修改 question/annotation/fixture，必须按 C8a baseline 同时 bump 对应 question/annotation/corpus 与 release version。若 C8b 使用外部 LLM 辅助生成或审核，必须另行披露调用量、出站内容、模型、费用与限流风险并获授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 验收归档C8a并确认C8b规划就绪`。

## 2026-07-22｜C8a 验收归档提交补录

- Commit：`83912d2`（`docs(openspec): 验收归档C8a并确认C8b规划就绪`）。本条只补录上一验收归档提交的真实 hash，不记录本次 C8b 规划提交。

## 2026-07-22｜C8b eval dataset expansion/annotation 规划启动

- 用户决策与提交责任：用户确认 C8a 验收完毕并要求开启 C8b 规划。提交责任按仓库默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD 为 `83912d2`、工作区干净、`.ai/ACTIVE_TASK.md=IDLE`、C8a 4 requirements / 13 scenarios 已接受进 `evaluation` baseline并归档、当前无未归档 change。冻结路线图下一串行 change 为 `eval-dataset-expansion-and-annotation`。
- 已确认数据事实：当前 release=`rag-eval-dev-v1`，30 samples / 3 fixtures，type=fact 10、definition 8、reasoning 6、multi_hop 3、no_answer 3；difficulty=easy 15、medium 12、hard 3；answerable/no-answer=27/3；source 引用 Java 12、RAG 11、Spring Boot 7；duplicate ID/question 均为 0。当前 type 与 difficulty 高度耦合，仍是开发数据而非生产分布。
- 规划范围：建立 Type C change `2026-07-22-eval-dataset-expansion-and-annotation` 的 proposal/design/tasks 与 `evaluation` spec delta，并激活 `.ai/ACTIVE_TASK.md`。草案建议 150 条总量、原 30 条 seed 不变、新增 120 条、五类×三难度 exact quota、现有 fixture coverage、grounding/review sidecar、v1/v2 共存和数据冻结边界。
- 规划 artifacts：change 下 proposal/design/tasks/spec delta 共 4 文件；design 18 条待用户确认的真实决策；delta 为 4 requirements / 12 scenarios。用户尚未批准总量、quota、corpus boundary、review/manifest v2 方案或实现授权。
- 外部调用与范围安全：规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0、数据出站为 0；未修改 baseline spec、eval JSONL、fixture、dataset manifest/schema、runner、Java/API、配置、数据库、前端、依赖、默认 provider 或历史报告，未进入 C9/C10/C14。
- 验证：启动提交与 clean-worktree readiness 通过；4 个必需 artifacts 齐全，decision/requirement/scenario 计数为 18/4/12；baseline spec 与数据/manifest/schema/fixture diff 为 0。其余 diff/link/SensitiveLogs 检查在规划收口时执行。
- 剩余风险：150 条与 quota 仍是建议值；仅用 3 份 fixture 可能造成题意重复，若扩 corpus 又会增加 fixture/KB/version 变量；annotation semantic review 不能仅靠结构校验。任何外部 LLM 辅助必须单独授权，不能从本规划自动推定。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C8b评测数据扩充规划`。

## 2026-07-22｜C8b 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 共 4 文件；18 条 design decisions 均满足三行决策结构；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C8b，当前只有一个未归档 change。
- 验证：baseline spec、eval JSONL、3 份 fixture、dataset manifest/schema 与两个 runner diff 均为 0；SensitiveLogs 扫描 307 source files / PASS；changed Markdown 本地链接、定向 secret value pattern 与规划受保护 artifact 检查通过；`git diff --check` 通过。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划仅修改 OpenSpec/ACTIVE_TASK/AGENT_LOG，未改 Python/Java/POM/前端/运行时配置，因此未运行 Python/Maven/frontend build、Docker/Testcontainers 或 live provider。
- 范围与闸门：未写入、删除、重排或重新标注任何评测样本，未创建 v2 release、review sidecar 或切换默认 manifest。下一步必须由用户先批准 proposal、18 条决策、4/12 delta 和实现授权；本轮真实业务外调与数据出站均为 0。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C8b评测数据扩充规划`。

## 2026-07-22｜C8b 规划提交补录

- Commit：`501222a`（`docs(openspec): 启动C8b评测数据扩充规划`）。本条只补录上一规划提交的真实 hash，不记录本次 C8b 实现提交。

## 2026-07-22｜C8b 决策批准并进入 TDD 实现

- 用户批准：proposal、18 条 design 决策、`evaluation` delta 的 4 requirements / 12 scenarios 与 TDD 实现授权全部通过；总量、exact quota、现有 3 份 fixture、review sidecar、manifest schema v2、v1/v2 共存及延迟切默认方案生效。
- 提交与外调边界：提交责任继续为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。未授权外部 LLM/provider 辅助，embedding/rerank/ask/judge/LLM/provider 调用量与数据出站保持 0。
- 当前阶段：先写失败测试，再实现 manifest v2、seed/quota/grounding/duplicate/review 门禁；expanded v2 完成后仍需用户验收，不能在本实现轮次接受 baseline、切默认 manifest 或归档。
- Commit：`pending`。

## 2026-07-22｜C8b expanded dataset 与 annotation TDD 实现完成（待验收）

- 范围与 artifacts：新增确定性本地构建器 `scripts/build_eval_dataset_v2.py`、显式 v1 release manifest、v2 question set/manifest 和 150 条 review sidecar；扩展共享 dataset validator 与聚焦测试；同步评测指南、OpenSpec change、项目上下文、架构、技术债、优化索引和活动任务指针。默认 `docs/eval/dataset-manifest.json` 保持 v1，未提前切换。
- 数据事实：`rag-eval-dev-v2`=150 条，前 30 条保持 v1 raw line bytes、解析对象、标注、ID 与顺序，追加 120 条。type=fact 35、definition 30、reasoning 40、multi_hop 25、no_answer 20；difficulty=easy 50、medium 65、hard 35；answerable/no-answer=130/20；批准的 15 格 type×difficulty matrix 全部 exact match。
- Grounding/review：三份 fixture answerable coverage 为 Java 49、RAG 43、Spring Boot 44，均在 [35, 45%] 边界内；103 条新增 answerable 的 242 个 context 全部 exact 命中；新增 multi-hop 至少两个独立 evidence points；150/150 review records 完整，17 条新增 no-answer 记录全 corpus 复核。normalized exact duplicate=0，阈值 0.82 下 near-duplicate candidate=0。
- Release identity：v1 manifest SHA-256=`91a03152ede5cd421650c5034158c1035248512bf38d1e2281079c6987a4a380`；v2 question SHA-256=`cdbcc42986f83f1b3bfe659828de38f7fc93f640a8ebaa375ef750074696a06d`；review SHA-256=`ef9a28b145aeb09bd40d10d789d03a221c1fd61348e6c3f647a9e64f54f75f86`；v2 manifest SHA-256=`404d896afc4bdacd54f5372d014b40b2a2779db42cdb966d4a2d869c9bb67b08`。构建器重复运行四份 artifact hash 不变；v1/v2 分别显式验证为 `VALID`。
- TDD 与回归：RED→GREEN 覆盖 manifest v2/review identity、fixture coverage、context grounding、normalized duplicate、version reuse、quota drift、seed drift、multi-hop evidence、review gap、near-duplicate review 及 schema/fixture version bump；Python 全量 `python -B -m unittest discover -s scripts -p 'test_*.py'` 为 98 tests / OK。direct/reproducible v2 plan-only 均返回 `VALID`，选取 1 条时 estimated debugRetrieve=1、ask=0、judge=0，但 plan-only 实际业务调用=0。
- 安全与文档验证：SensitiveLogs 扫描 308 source files / PASS；changed Markdown 10 个、missing local links=0；review 150 条均只有六个批准字段且不含 question key；定向 secret/private-key/absolute-user-path 扫描 PASS；default manifest 与显式 v1 manifest byte-identical；v1 question/schema、3 fixtures、baseline spec 及受保护配置 diff=0；`git diff --check` 通过。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；无 Java/POM/前端/依赖/运行时配置改动，因此 Maven、frontend build、Docker/Testcontainers 与 live backend/provider smoke 均 `SKIPPED`。未授权外部业务调用，实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0、数据出站为 0。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、v1 question/schema/fixture bytes、`openspec/specs/` baseline、Java/API、数据库、前端、依赖、默认 provider、retrieval/chunking/rerank/prompt/citation/no-answer/judge 公式或历史报告；未进入 C9/C10/C14，未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险与闸门：150 条仍来自 3 份 tracked fixture，是开发数据而非隐藏 benchmark、生产分布或论文级数据集；自动门禁不能替代用户对题意和标注的最终语义验收。用户验收前必须继续保持默认 manifest=v1，不接受 baseline、不归档；验收后才可切默认 v2、原文接受 4/12 delta、恢复 `ACTIVE_TASK=IDLE` 并归档。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(评测): 实现C8b评测数据扩充与标注`。

## 2026-07-23｜C8b 实现提交补录

- Commit：`55d9a34`（`feat(评测): 实现C8b评测数据扩充与标注`）。本条只补录上一实现提交的真实 hash，不记录本次验收归档提交。

## 2026-07-23｜C8b 用户验收、默认切换与归档

- 用户授权与结果：用户确认 C8b 验收完毕，并授权在完成度复核通过后直接归档。4 requirements / 12 scenarios 的 delta body 已原文接受进 `openspec/specs/evaluation/spec.md`；默认 manifest 和两个 runner 已切换到 `rag-eval-dev-v2`；change 已移动到 `openspec/changes/archive/2026-07-22-eval-dataset-expansion-and-annotation/`，`.ai/ACTIVE_TASK.md=IDLE`，当前无未归档 change。
- 归档范围：同步 proposal/design/tasks、项目上下文、架构、评测指南、技术债与优化索引；18 条已批准决策消除“待确认”残留；v1 显式 manifest/question set 继续可独立验证，C8b 不宣称 retrieval、generation、citation、no-answer、judge 质量收益或 C9/C10/C14 完成。
- 归档修复：默认切换后 runner 的默认 eval set 改为 v2，测试辅助和 v2 builder 改为显式读取 v1 seed manifest。额外发现 Windows `Path.write_text` 生成 CRLF、Git `eol=lf` 入库后会让 review/manifest raw hash 在新 checkout 漂移；先增加 LF 回归测试并观察 3 个预期失败，再改为 UTF-8 `write_bytes` 固定 LF。最终 v2 question SHA-256=`cdbcc42986f83f1b3bfe659828de38f7fc93f640a8ebaa375ef750074696a06d`，review SHA-256=`fb95b2c1c8947afff3dd7115e61b92daea462a9332f58732240b6e2fdecbe738`，默认与显式 v2 manifest byte-identical，SHA-256=`8fe7f88846436133592ddc27388701018884df4bc526504183e82f5cb5626b87`。
- 验证：`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 99 tests / OK；direct 与 reproducible 默认 plan-only 均返回 `VALID`、v2、150 samples，选取 1 条时 estimated debugRetrieve=1、ask=0、judge=0，plan-only 实际业务调用=0；v1/v2 并存验证、LF bytes、default=explicit-v2、question/review path/hash/bytes binding 全部通过。归档 4 个必需 artifacts 齐全、tasks 全勾选、delta body exact suffix、4/12 计数、无未归档 change 与 `ACTIVE_TASK=IDLE` 均通过。
- 安全与文档：SensitiveLogs 扫描 308 source files / PASS；12 个 changed Markdown 的本地相对链接 missing=0；`git diff --check` 通过；受保护的 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 改动为 0。未修改 v1 question/schema、3 份 fixture、Java/API、数据库、前端、依赖、provider、retrieval/chunking/rerank/prompt/citation/no-answer/judge 公式，未暂存、提交、push、创建 PR、部署或发布。
- 跳过项与外调：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；本轮无 Java/POM/前端/依赖/运行时服务改动，因此 Maven、frontend build、Docker/Testcontainers 与 live backend/provider smoke 均 `SKIPPED`。未获得外部业务调用授权，真实 embedding/rerank/ask/judge/LLM/provider 调用量与数据出站均为 0。
- 剩余风险与后续 readiness：150 条 v2 仍只来自 3 份 tracked fixture，是开发评测 release，不是生产分布、隐藏 benchmark 或论文级数据集。C9a `claim-evidence-objective-metrics` 的前置数据治理已满足，可以另立 Type C change 进入规划；claim 单位、evidence 对齐、指标分母和状态语义仍须在新 change 事前闸门决定。
- Commit：`pending`；提交责任为用户手动提交。建议 `chore(openspec): 验收并归档C8b评测数据扩充`。

## 2026-07-23｜C9a claim-evidence objective metrics 规划启动

- 用户决策与提交责任：用户要求检查当前项目状态，并在允许时直接开始 C9a 规划。提交责任按仓库默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD 为 `1577aab`、工作区干净、`.ai/ACTIVE_TASK.md=IDLE`、C8b 4 requirements / 12 scenarios 已接受进 `evaluation` baseline并归档、默认 v2 与显式 v2 manifest 已完成验收，当前无未归档 change。C8b closeout 已明确 C9a 的数据治理前置满足。
- 能力分类：`confirmed` 为 generation/citation/no-answer 客观指标、citation-context provenance、可选 judge 与 v1/v2 dataset identity；`partial` 为现有 details 已含 answer/citations/contexts 但没有 claim attribution；`planned` 为确定性 claim splitter、validated-citation-only evidence、exact/token lexical alignment、分母与局部状态；`out_of_scope` 为 C9b judge calibration、C10 quality gate、生产行为和 dataset 修改；`unknown` 为初始 0.70 threshold 在未来真实 150 条 generation evidence 上的分布。
- 规划范围与 artifacts：建立 Type C change `2026-07-23-claim-evidence-objective-metrics` 的 proposal/design/tasks 与 `evaluation` spec delta，并激活 `.ai/ACTIVE_TASK.md`。规划采用句子/列表 claim、只接受通过 provenance 的 returned citations、exact + 0.70 claim-token coverage、所有抽取 claim 进入分母、`COMPLETE/PARTIAL/SKIPPED/NOT_APPLICABLE` 局部状态；明确不称 entailment/faithfulness。
- 规划结构：change 下 proposal/design/tasks/spec delta 共 4 文件；design 12 条待用户确认的真实决策；delta 为 4 requirements / 12 scenarios。用户尚未批准 initial threshold、12 条决策、delta 或 TDD 实现授权。
- 外部调用与范围安全：规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站均为 0；未修改 baseline spec、Python runner/tests、eval JSONL/fixture/manifest/schema/review、Java/API、配置、数据库、前端、依赖、默认 provider、生产 prompt/citation/no-answer 或历史报告，未进入 C9b/C10。
- 跳过项：规划只修改 OpenSpec/ACTIVE_TASK/AGENT_LOG，不涉及实现，因此 Python、Maven、frontend build、Docker/Testcontainers 与 live backend/provider 均暂不运行；OpenSpec CLI 可用性将在规划验证中检查，未检查前不声称通过。
- 剩余风险：deterministic lexical alignment 存在同义 false negative 与共享术语 false positive；0.70 是待事前闸门批准的 v1 初始阈值，不是经验校准或质量门禁。未来一次 150 条 evidence run 的保守上限为 150 debug retrieval、150 ask、至多 300 query embedding、至多 150 generation、judge=0，当前未授权。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C9a客观claim证据指标规划`。

## 2026-07-23｜C9a 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 共 4 文件；12 条 design decisions 均满足“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”三行结构；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C9a，当前只有一个未归档 change。
- 验证：direct plan-only 首次因漏传必需的 `--kb-id` 在任何 backend/provider 调用前退出；补 `--kb-id 0` 后返回 dataset `VALID`、v2、150 samples，选取 1 条时仅估算 debugRetrieve=1、ask=1、judge=0，plan-only 实际业务调用为 0。reproducible plan-only 返回同一 `VALID` v2 identity，选取 1 条时估算 debugRetrieve=1、ask=0、judge=0，实际业务调用为 0。SensitiveLogs 扫描 308 source files / PASS；新规划文件无 trailing whitespace、均以 LF 结尾且无本地 Markdown 链接；定向 secret value scan无命中；`git diff --check` 通过。
- 范围检查：`openspec/specs/` baseline、`scripts/`、`docs/eval/`、fixture、`.env.local`、`application-dev.yml`、`.agents/` 与 `docs/学习文档/` tracked diff 为 0。当前工作区仅修改 `.ai/ACTIVE_TASK.md`、追加 `.ai/AGENT_LOG.md`，并新增 C9a change 目录；未修改业务代码、测试、数据或历史证据。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划无 Python 实现、Java/POM、前端、依赖或运行时服务改动，因此 Python 全量、Maven、frontend build、Docker/Testcontainers 与 live provider smoke 均 `SKIPPED`。真实 embedding/rerank/ask/judge/LLM/provider 调用量与数据出站均为 0。
- 下一闸门：用户需先审阅并批准 proposal、12 条决策、4/12 delta 与 TDD 实现授权；真实 generation evidence 仍需另行授权，不随实现批准自动放开。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C9a客观claim证据指标规划`。

## 2026-07-23｜C9a 规划提交补录

- Commit：`4afe586`（`docs(openspec): 启动C9a客观claim证据指标规划`）。本条只补录上一规划提交的真实 hash，不记录本次实现提交。

## 2026-07-23｜C9a 决策批准并进入离线 TDD 实现

- 用户批准：proposal、12 条 design 决策、`evaluation` delta 的 4 requirements / 12 scenarios 与 TDD 实现授权全部通过；句子/列表 claim、validated-citation-only evidence、exact + 0.70 claim-token coverage、全 claim 分母和局部状态方案生效。
- 提交与外调边界：提交责任继续为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。实现授权不包含真实 generation/judge/provider run，embedding/rerank/ask/judge/LLM/provider 调用量与数据出站保持 0。
- 当前阶段：按 `tdd` skill 进行纵向 RED→GREEN 切片，先从 deterministic splitter 的可观察行为开始，再逐步接入 eligible evidence、per-sample/aggregate metrics 与 report identity；不一次性批量写完测试。
- Commit：`pending`。

## 2026-07-23｜C9a objective claim-evidence 离线实现完成（待验收）

- 范围与修改：`scripts/run_rag_eval.py` 新增固定 `claim-lexical-v1` 的 splitter、eligible evidence、exact/token matcher、per-sample/aggregate/status、report/details/console 与 metadata identity drift gate；`scripts/run_reproducible_rag_eval.py` 复用 direct runner 的唯一 config 并写入 plan/run metadata；两份 runner tests 增加 TDD 行为覆盖。同步评测指南、active change proposal/design/tasks、`openspec/project.md`、架构、技术债、优化索引与活动任务指针。
- 已确认行为：成功 answerable answer 按段落/列表/中英文句末标点确定性拆分；只有通过既有 citation identity + snippet-to-returned-context provenance 的 citation 才进入 evidence。claim 先做 normalized exact，否则用 ASCII token/CJK bigram、claim-token denominator、固定 `0.70` 和最少 2 token；无 evidence、短 claim、低于阈值都保留在分母并输出稳定 reason。best evidence 按 exact、coverage、citationIndex 稳定排序。
- 指标与边界：per-sample 保存 claim text/hash/index 与 best evidence，aggregate/Markdown/console 只输出状态和计数，不复制 raw claim/snippet。局部状态为 `COMPLETE/PARTIAL/SKIPPED/NOT_APPLICABLE`，不改变现有全局 `CLEAN/PARTIAL/RETRIEVAL_ONLY/FAILED`、keyword、citation、no-answer 或 judge 公式。不同 `claimMetricConfig` 以 `claim_metric_identity_mismatch` 在 backend/provider 调用前失败；旧结果缺字段时解释为 unavailable/partial，不补算为 0。
- TDD 证据：逐个 RED→GREEN 覆盖 splitter 不存在、结构 marker、per-sample 缺字段、aggregate 缺状态、Markdown 缺摘要、metadata identity 缺口等 tracer bullets；补充 invalid provenance、0.70 boundary、短 claim、stable tie-break、no-answer/retrieval-only/partial 与旧结果兼容。聚焦 direct 为 36 tests / OK，reproducible 为 27 tests / OK；最终 `python -B -m unittest discover -s scripts -p 'test_*.py'` 为 114 tests / OK。
- Dataset/plan 验证：v1 direct plan-only=`VALID`/30 samples，v2 direct=`VALID`/150 samples，v2 reproducible=`VALID`/150 samples；三者选取 1 条均显示 `claim-lexical-v1` 与 threshold `0.7`。direct 只估算 debug=1/ask=1/judge=0，repro retrieval-only 估算 debug=1/ask=0/judge=0；plan-only 实际 backend/provider 调用为 0。
- 安全与文档验证：SensitiveLogs 308 source files / PASS；10 个 changed Markdown 的本地链接 missing=0；受保护路径、baseline spec、dataset release/review/schema/manifest、fixture、历史 reports/history diff 均为 0；`git diff --check` 通过。定向 secret scan 首次 PowerShell quoting 解析失败，修正为边界明确的 key pattern 后无命中；未把失败的首次命令算作通过。
- 跳过项与外调：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过。无 Java/POM/前端/依赖/运行时服务改动，因此 Maven、frontend build、Docker/Testcontainers 与 live backend/provider smoke 均 `SKIPPED`。真实 150 条 generation evidence 未授权、未执行；embedding/rerank/ask/judge/LLM/provider 实际调用量、数据出站与费用均为 0。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、`openspec/specs/` baseline、eval JSONL/fixture/review/schema/manifest、Java/API、数据库、前端、依赖、生产 prompt/citation/no-answer/provider、默认 reranker 或历史报告；未进入 C9b/C10，未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险与闸门：deterministic lexical metric 仍有同义 false negative 与共享术语 false positive，`0.70` 尚无真实 150 条 generation 分布校准；它不能证明 entailment/faithfulness。当前 change 保持 `ACTIVE` 等待用户验收；验收后才能接受 4/12 delta、归档并恢复 `IDLE`。真实 evidence 必须另行披露和授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(评测): 实现C9a客观claim证据指标`。

## 2026-07-23｜C9a 实现提交补录

- Commit：`26228aa`（`feat(评测): 实现C9a客观claim证据指标`）。本条只补录上一实现提交的真实 hash，不记录本次验收归档提交。

## 2026-07-23｜C9a 用户验收、baseline 接受与归档

- 用户授权与结果：用户确认 C9a 验收通过。4 requirements / 12 scenarios 的 delta body 已原文接受进 `openspec/specs/evaluation/spec.md`；change 已移动到 `openspec/changes/archive/2026-07-23-claim-evidence-objective-metrics/`，`.ai/ACTIVE_TASK.md=IDLE`，当前无未归档 change。
- 归档范围：同步 proposal/design/tasks、项目上下文、架构、技术债与优化索引；Optional Real Evidence Gate 因未获真实运行授权明确记为 `SKIPPED`，tasks 已全部闭环。C9a 只确认固定 `claim-lexical-v1` 的 objective lexical alignment，不确认 C9b judge calibration、semantic faithfulness、C10 quality gate 或真实 150 条 generation evidence。
- 验证：归档目录 4 个必需 artifacts 齐全，tasks 未勾选数为 0，delta body 为 baseline exact suffix，计数为 4 requirements / 12 scenarios，`ACTIVE_TASK=IDLE` 且未归档 change 数为 0；`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 114 tests / OK；direct/reproducible plan-only 均为 `VALID` v2/150 samples 并显示 `claim-lexical-v1` / `0.70`；SensitiveLogs 扫描 308 source files / PASS；7 个 changed Markdown 的本地链接 missing=0；`git diff --check` 通过。
- 跳过项与外调：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；无 Java/POM/前端/依赖/运行时服务改动，因此 Maven、frontend build、Docker/Testcontainers 与 live backend/provider smoke 均 `SKIPPED`。真实 embedding/rerank/ask/judge/LLM/provider 调用量、数据出站与费用均为 0。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、eval JSONL/fixture/review/schema/manifest、Java/API、数据库、前端、依赖、生产 prompt/citation/no-answer/provider、默认 reranker 或历史报告；未进入 C9b/C10，未暂存、提交、push、创建 PR、部署或发布。
- C9b readiness：治理前置已满足，且代码事实确认 judge 默认关闭、已有可选 judge/聚合字段，但 `report_status` 仍只看 retrieve/ask error，judge 全失败仍可能 `CLEAN`，也没有独立 objective/judge status 或校准 evidence。C9b 可在本归档提交后另立 `judge-calibration-and-status-semantics` Type C change 进入规划；实现和任何真实 judge 校准调用仍需分别通过事前闸门与外调授权。
- 剩余风险：C9a `0.70` 仍无真实 150 条 generation 分布证据；C9b 的 judge model/prompt、人工 gold、校准样本、agreement 指标、状态矩阵和错误降级语义尚未决策，不能从当前代码推定。
- Commit：`pending`；提交责任为用户手动提交。建议 `chore(openspec): 验收并归档C9a客观claim证据指标`。

## 2026-07-23｜C9a 验收归档提交补录

- Commit：`033ee01`（`chore(openspec): 验收并归档C9a客观claim证据指标`）。本条只补录上一验收归档提交的真实 hash，不记录本次 C9b 规划提交。

## 2026-07-23｜C9b judge calibration 与状态语义规划启动

- 用户决策与提交责任：用户要求现在开始 C9b 规划。提交责任按仓库默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD=`033ee01`、工作区干净、`.ai/ACTIVE_TASK.md=IDLE`、C9a 4 requirements / 12 scenarios 已接受进 `evaluation` baseline 并归档，当前无未归档 change，允许按冻结路线图启动 `judge-calibration-and-status-semantics` Type C change。
- 当前事实：可选 answerable-only judge、faithfulness/relevance/pass、计划调用估算、judge error count 和 `--fail-on-judge-errors` 已存在；但 inline prompt/parser/0.70 pass rule 未形成完整 contract identity，越界 score 会 clamp，provider pass 可成为规范结果，没有 calibration corpus/evidence，`report_status` 只看 retrieve/ask error，因此 judge 全失败仍可能 `CLEAN`。
- 能力分类：`confirmed` 为既有 optional judge 与 C9a objective status；`partial` 为未版本化 judge contract、成功子集聚合和单一 comparison safety；`planned` 为 24 条四象限 human-gold calibration v1、strict parser、shared contract、canary/full agreement/repeat metrics、objective/judge/global status 分离；`out_of_scope` 为 C10、生产行为、默认开启 judge、no-answer/逐 claim judge和 dataset v2 修改；`unknown` 为 live provider/model/费用/限流与实际 agreement。
- 规划 artifacts：创建 change `2026-07-23-judge-calibration-and-status-semantics` 的 proposal/design/tasks 与 `evaluation` spec delta，并激活 `.ai/ACTIVE_TASK.md`。规划固定 24 cases（四象限各 6）、full 3 repeats、strict score schema、score-derived pass、`objectiveMetricStatus`/`judgeMetricStatus`/global composition 与 per-channel comparison safety；design 包含 15 条真实决策记录，delta 为 4 requirements / 12 scenarios。
- 外部调用与范围：规划阶段 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用量和数据出站均为 0；live calibration 预算被锁在独立闸门，canary 最多 4 judge calls、full 最多 72、总计最多 76，执行前必须另行披露和授权。
- 未修改范围：本轮不修改 baseline spec、Python runner/tests、calibration artifact、eval JSONL/fixture/manifest/schema/review、Java/API、配置、数据库、前端、依赖、默认 provider、生产 prompt/citation/no-answer 或历史报告；未进入 C10。
- 剩余风险与下一闸门：24 条只能代表开发 rubric；0.70 只是当前 candidate，规划不自动调参或建立门禁。用户需先批准 proposal 的 corpus/repeat/status/external-call 方案、15 条决策、4/12 delta 和 offline TDD 实现授权；实现授权默认仍不包含 live judge 调用。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C9b judge校准与状态语义规划`。

## 2026-07-23｜C9b 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 共 4 文件；15 条 design decisions 均完整包含“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C9b，当前只有一个未归档 change。
- Plan-only：direct 与 reproducible runner 均返回 dataset `VALID`、`rag-eval-dev-v2`、150 samples。各选取 1 条时 direct 仅估算 debugRetrieve=1、ask=1、judge=0，reproducible 估算 debugRetrieve=1、ask=0、judge=0；plan-only 实际 backend/provider 调用和数据出站均为 0。
- 安全与文档：SensitiveLogs 扫描 308 source files / PASS；6 个 changed/untracked Markdown 的本地链接 missing=0、trailing whitespace=0、CRLF=0；5 个本轮规划目标文件的 secret value / Authorization token / `C:\\Users\\` 绝对路径定向扫描为 0；`git diff --check` 通过。
- 范围检查：`openspec/specs/` baseline、`scripts/`、`docs/eval/`、eval data/fixture、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、Java/POM 和前端 tracked diff 均为 0；当前只修改 ACTIVE_TASK、追加 AGENT_LOG 并新增 C9b change 目录。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划阶段没有 Python/Java/前端实现改动，因此 Python 全量、Maven、frontend build、Docker/Testcontainers、live backend 与 live judge/provider 均 `SKIPPED`。
- 下一闸门：用户需审阅并批准 24-case 四象限校准集、3 repeats、strict parser、score-derived pass、objective/judge/global status matrix、15 条决策、4/12 delta 与 offline TDD 实现授权。即使批准实现，live canary/full judge 调用仍必须另行披露和授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C9b judge校准与状态语义规划`。

## 2026-07-23｜C9b 规划提交补录

- Commit：`f9c2d10`（`docs(openspec): 启动C9b judge校准与状态语义规划`）。本条只补录上一规划提交的真实 hash，不记录本次方案批准状态同步。

## 2026-07-23｜C9b 方案审阅批准

- 用户批准：proposal 的 24-case faithful×relevant 四象限校准集、full 3 repeats、strict parser、score-derived pass、objective/judge/global status matrix、external-call gate 与 non-goals；design 的 15 条决策记录及 `evaluation` delta 的 4 requirements / 12 scenarios 均通过事前规划闸门。
- 当前边界：本次表述确认方案，不自动解释为 offline TDD 实现授权；`tasks.md` 的 implementation authorization 继续未勾选，runner/tests/calibration artifacts/guide/baseline 均不修改。
- 外调边界：live canary 最多 4 judge calls、full 最多 72、合计最多 76 的调用仍未授权；embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 实际调用量与数据出站保持 0。
- 下一闸门：等待用户明确授权进入 offline TDD implementation；即使获得实现授权，live judge calibration 仍需后续单独披露 provider/model、出站、费用/限流和 raw artifact 策略并授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 确认C9b方案审阅通过`。

## 2026-07-23｜C9b 方案批准提交补录

- Commit：`dc7a99d`（`docs(openspec): 确认C9b方案审阅通过`）。本条只补录上一方案批准状态提交的真实 hash，不记录本次 offline implementation 提交。

## 2026-07-23｜C9b 获准进入 offline TDD 实现

- 用户授权：明确授权进入 C9b offline TDD implementation；批准范围为共享 judge contract/strict parser、calibration schema/corpus/validator/runner、objective/judge/global status、per-channel comparison safety、测试与文档同步。
- TDD 方法：按 `tdd` skill 使用纵向 RED→GREEN tracer bullets，一次锁定一个 public behavior，再做最小实现；不先批量写完全部测试。
- 提交边界：继续由用户手动提交；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外调边界：本授权不包含 live judge calibration。canary 4 calls、full 72 calls、总计最多 76 仍未授权；本轮 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 实际调用和数据出站必须保持 0。
- Closeout 边界：实现完成后 change 继续保持 `ACTIVE` 等待 offline implementation 验收和后续 live calibration 决策；不提前接受 baseline、不归档、不恢复 `IDLE`。
- Commit：`pending`。

## 2026-07-23｜C9b 方案批准状态同步验证

- 状态验证：proposal 与 design/delta 两项批准任务均已勾选，offline TDD implementation authorization 仍未勾选；`.ai/ACTIVE_TASK.md` 明确记录“规划已获批准，等待实现授权”，没有提前进入实现或接受 baseline。
- 文档与范围验证：4 个 changed Markdown 本地链接 missing=0，`git diff --check` 通过；`openspec/specs/` baseline、`scripts/`、`docs/eval/`、eval data/fixture、受保护本地配置、Java/POM 和前端 tracked diff 均为 0。
- 跳过项与外调：仅同步批准状态，未修改代码/数据，因此 Python、Maven、frontend build、Docker/backend 均 `SKIPPED`；live judge/provider 未授权，实际业务调用和数据出站均为 0。
- 剩余风险：实现尚未开始，calibration corpus、strict parser、status matrix 和 runner 均仍是 planned；只有后续 offline TDD 验证完成后才能称实现 ready，只有另行授权并完成 live calibration 后才能称 judge 获得校准 evidence。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 确认C9b方案审阅通过`。

## 2026-07-23｜C9b offline TDD implementation 完成（待验收）

- 用户授权与提交责任：用户明确授权 C9b offline TDD implementation；提交责任继续为 `用户手动提交`，Agent 未暂存、提交、push、创建 PR、部署或发布。Live canary/full judge calibration 未获授权。
- 实现范围：新增 shared `scripts/rag_judge_contract.py`，固定 `rag-judge-v1` prompt/parser/双 `0.70` threshold/score-derived pass/secret-free identity；direct/reproducible runner 绑定同一 contract。Normal eval 新增 objective/judge/global status、per-channel comparison safety、judge eligible/attempted/valid/error/invalid-payload/provider-pass-mismatch coverage，judge error 不污染 objective 通道。
- Calibration：新增 `judge-calibration-v1` schema、manifest 与 24 条人工复核静态 case，faithful×relevant 四象限各 6 条；validator 在外调前校验 path/hash/bytes/count/order、quota、fixture exact grounding、gold/review consistency。独立 runner 固定 canary 4×1、full 24×3，无 retry，保留失败 observation，输出 coverage/confusion/agreement/provider-pass mismatch/repeat consistency、脱敏 Markdown 与 `--no-overwrite` 本地 raw details；live 边界还需显式 `--execute-live-judge`。
- TDD 证据：按纵向 RED→GREEN 依次锁定 out-of-range/strict score、provider pass disagreement、contract drift、shared prompt、reproducible metadata、judge all-error/channel safety、details JSON、coverage counts、calibration corpus/plan/missing observation/fake execution/no-overwrite/live HTTP boundary 和绝对路径脱敏；外部 boundary 均由 fake/mock 替代。
- 验证：direct/reproducible/calibration 聚焦 suites 通过；最终 `python -B -m unittest discover -s scripts -p 'test_*.py'` 为 132 tests / OK。Direct v2、direct v1、reproducible v2 plan-only 均验证对应 release，选 1 条时实际业务调用为 0；calibration canary/full plan-only 分别返回 `VALID`、4×1=4 与 24×3=72 的预算，实际 judge 调用为 0。v1 首次误传不存在的 question-set path，validator 在任何调用前以 `unversioned_eval_set` 退出；改用 manifest 固定的 `docs/eval/rag_eval_set.jsonl` 后通过。
- 安全与文档：SensitiveLogs 扫描 310 source files / PASS；10 个 changed/untracked Markdown 本地链接 missing=0；定向扫描只命中单测中的 `secret-value`/`unused-by-fake` 假值，无真实 credential 或新增 `C:\Users\` 路径；受保护配置、accepted baseline、v1/v2 release、fixture、历史 reports/history diff 均为 0；`git diff --check` 通过。OpenSpec CLI 当前不可用，未声称 CLI validation 通过。
- 跳过项与外调：Java/POM/前端/依赖/生产配置均未修改，因此 Maven、frontend build、Docker/Testcontainers、live backend 均 `SKIPPED`。真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、数据出站、费用与限流事件均为 0。
- 范围与剩余风险：未修改 production QA、默认 judge、C9a formula、v1/v2 dataset、Java/API、prompt/citation/retrieval/rerank/no-answer、baseline spec 或 C10 gate。Change 保持 `ACTIVE`；当前只能声明 offline implementation ready，尚无 live agreement/repeat evidence，不能声明 judge 已校准、通用可靠、production-ready 或可自动 gate。
- Commit：`pending`；建议用户手动提交 `feat(评测): 完成C9b离线judge校准与状态语义`。

## 2026-07-23｜C9b offline implementation 最终复核补录

- 在上一条后补充 duplicate/unexpected calibration observation identity 回归：两类漂移现在明确产生 `NOT_COMPARABLE`，不会因保留首个成功 observation 而误报 `COMPLETE`；脱敏报告同时展示 missing/duplicate/unexpected counts。
- 最终 Python 全量更新为 133 tests / OK；calibration manifest 仍为 `VALID`，canary/full plan-only 预算仍为 4/72，实际业务调用与数据出站仍为 0；SensitiveLogs 仍为 310 source files / PASS。
- 上一条的 132 tests 是补测前的阶段结果，本条以 133 为当前最终值；其余范围、跳过项、剩余风险与 `Commit: pending` 不变。

## 2026-07-23｜C9b offline implementation 提交补录

- Commit：`d827f18`（`feat(评测): 完成C9b离线judge校准与状态语义`）。本条只补录上一执行提交的真实 hash，不记录本次验收归档改动。

## 2026-07-23｜C9b 用户验收、baseline 接受与归档

- 用户授权与结果：用户确认 C9b 验收完成并要求项目归档。4 requirements / 12 scenarios 的 delta body 已原文接受进 `openspec/specs/evaluation/spec.md`；change 已移动到 `openspec/changes/archive/2026-07-23-judge-calibration-and-status-semantics/`，`.ai/ACTIVE_TASK.md=IDLE`，当前无未归档 change。
- 验收范围：接受 shared `rag-judge-v1` contract/strict parser、24 条四象限静态 calibration corpus/validator/runner、objective/judge/global status、per-channel comparison safety、兼容性与安全边界。实现提交为 `d827f18`。
- Live gate：canary 4 calls、full 72 calls、合计最多 76 的真实 judge calibration 从未单独授权或执行，5 项 live gate 均以 `SKIPPED` 收口；没有 provider/model/endpoint、HTTP/rate-limit/timeout、repeat agreement/confusion 或 raw provider evidence。归档不确认真实 judge agreement、production faithfulness、通用 judge 可靠性、默认开启 judge 或 C10 quality gate。
- 验证：delta-to-baseline exact suffix 通过（4 requirements / 12 scenarios，首 requirement 在 baseline 仅出现 1 次）；archive 4 个必需 artifacts 齐全、tasks 未勾选数 0、未归档 change 数 0、`ACTIVE_TASK=IDLE`。`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 133 tests / OK；SensitiveLogs 扫描 310 source files / PASS。
- Plan-only：direct/reproducible 均验证 `rag-eval-dev-v2`，各选择 1 条且实际业务调用为 0；calibration manifest=`VALID`，canary/full 仅报告 4/72 调用预算，实际 judge/provider 调用和数据出站为 0。
- 跳过项：本轮仅做 OpenSpec baseline/archive 与事实文档收口，没有 Java/POM/前端/依赖/生产配置改动，因此 Maven、frontend build、Docker/Testcontainers、live backend 和 live provider 均 `SKIPPED`。OpenSpec CLI 当前不可用，未声称 CLI validation 通过。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、v1/v2 release/fixture/review、Java/API、数据库、前端、生产 prompt/citation/retrieval/rerank/no-answer/default judge 或历史报告；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：静态 24-case corpus 只代表已接受的开发 rubric；`0.70` 仍是 contract candidate，没有 live agreement、成本、限流或稳定性证据。未来真实校准、阈值调整或 C10 gate 必须新建 Type C change 并重新取得外调授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `chore(openspec): 验收并归档C9b judge校准与状态语义`。

## 2026-07-23｜C9b 归档文档门禁补录

- 首次 changed-Markdown 链接命令把已移动的 active 路径删除项也当作现存文件读取，产生 4 组本地 `Get-Content` 诊断；该命令不作为通过证据。改用 `--diff-filter=AMR` 并合并 untracked archive 后，12 个现存 changed Markdown 的本地链接 missing=0。
- 当前事实源旧 active 路径/待验收表述扫描为 0，受保护路径 diff=0，`git diff --check` 通过；归档仍保持 `Commit: pending`。

## 2026-07-23｜C10 quality threshold gates readiness 与规划启动

- 用户决策与提交责任：用户要求检查项目状况，若允许则开始 C10 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD=`c246929`，工作区干净，`main...origin/main [ahead 7]`，`.ai/ACTIVE_TASK.md=IDLE`；C9a/C9b 均已接受进 `evaluation` baseline 并归档，当前无未归档 change。路线图下一顺序项明确为 C10 `eval-quality-threshold-gates`，结论为 `GO`。
- 当前事实：direct details 已有 versioned dataset identity、run metadata、global/objective/judge status、per-channel comparison safety、aggregate metrics 和逐样本 calculation details；现有 `--fail-on-ask-errors/--fail-on-judge-errors` 只覆盖执行错误，没有版本化 quality profile、type/difficulty slices、threshold/tolerance 或质量退出码。
- 能力分类：`confirmed` 为 C8 v2 release 与 C9 channel/status contract；`partial` 为仅有 aggregate metrics/error exit；`planned` 为 tracked profile、offline evaluator、fixed slices、hard/reference rules、fail-closed missing/error 和 `0/3/4/2` exit codes；`out_of_scope` 为算法/生产默认/CI/C11+/C14；`unknown` 为 v2 reference 指标、provider/费用/限流，未在 planning 中猜测。
- 规划 artifacts：创建 `2026-07-23-eval-quality-threshold-gates` 的 proposal/design/tasks 与 `evaluation` spec delta，激活 `.ai/ACTIVE_TASK.md`。Design 包含 15 条真实决策记录，delta 为 4 requirements / 12 scenarios。
- 两道闸：offline implementation 只使用合成/静态 evidence、业务调用和数据出站为 0；首个 retrieval profile 先为 `DRAFT`。若后续激活，推荐另行授权 v2/150×3 repeats，最多 450 debug retrieval、可能最多 450 query embedding，heuristic 下 external rerank/ask/generation/judge=0；具体 provider/model/出站/费用/限流/timeout/retry/raw artifact 和阈值仍待事前闸门确认。
- 范围安全：本轮不修改 baseline spec、scripts、tests、eval dataset/fixture/schema/manifest/review、历史 report、Java/API、数据库、前端、依赖、生产配置、prompt/citation/no-answer 或默认 provider；未触发任何 backend/provider 调用。
- 剩余风险与下一闸门：当前没有正式 v2/150 reference evidence，不能用 C7 30 条历史值或 C9 离线 corpus 直接激活数值 gate。用户需先批准 proposal、15 条 decisions、4/12 delta 与 offline TDD 实现授权；reference evidence 和 profile ACTIVE 仍需后续单独授权/验收。
- Commit：`pending`。建议用户手动提交 `docs(openspec): 启动C10质量阈值门禁规划`。

## 2026-07-23｜C10 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 共 4 个必需 artifacts；design 的 15 条 decisions 均完整包含“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C10，未归档 active change 数为 1。
- Plan-only：direct runner 使用默认 v2 manifest，返回 `VALID` / `rag-eval-dev-v2` / 150 samples；选 1 条仅估算 debugRetrieve=1、ask=1、judge=0。Reproducible runner 返回 retrieval-only、同一 `VALID` v2 identity；选 1 条仅估算 debugRetrieve=1、ask=0、judge=0。两次均为 plan-only，实际 backend/provider 调用、数据出站与费用为 0。
- 文档与安全：SensitiveLogs 扫描 310 source files / PASS；6 个 changed/untracked Markdown 的本地链接 missing=0；trailing whitespace=0；规划目标新增内容的 secret value / Authorization token / `C:\Users\` 绝对路径命中为 0；受保护路径 diff=0；`git diff --check` 通过。
- 范围检查：`openspec/specs/` baseline、`scripts/`、`docs/eval/`、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、Java/POM、前端与历史 reports tracked diff 均为 0。当前只修改 ACTIVE_TASK、追加 AGENT_LOG 并新增 C10 change 目录。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划阶段没有 Python/Java/前端实现改动，因此 Python 全量、Maven、frontend build、Docker/Testcontainers、live backend 与 live provider 均 `SKIPPED`。
- 下一闸门：等待用户审阅并批准两道闸、initial retrieval-only profile、`PASS/FAIL/NOT_EVALUABLE/INVALID` 与 `0/3/4/2`、15 条 decisions、4/12 delta 和 offline TDD 实现授权。批准 offline implementation 仍不包含 v2/150×3 reference calls。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C10质量阈值门禁规划`。

## 2026-07-23｜C10 规划提交补录

- Commit：`76331fa`（`docs(openspec): 启动C10质量阈值门禁规划`）。本条只补录上一规划提交的真实 hash，不记录本次 offline implementation 改动。

## 2026-07-23｜C10 规划批准并获准进入 offline TDD

- 用户批准：proposal 的两道闸、profile/status/exit-code 语义、initial retrieval-only 边界与 non-goals；design 的 15 条 decisions 与 `evaluation` delta 的 4 requirements / 12 scenarios 均通过事前门禁。
- 实现授权：用户明确批准进入 offline TDD；提交责任继续为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外调边界：用户明确不授权 reference calls。本轮只允许 synthetic/static/local evidence；v2/150×3 reference、debug retrieval、query embedding、rerank、ask、generation、judge 或其他 provider 调用均不得执行，数据出站必须为 0。
- Closeout 边界：offline implementation 完成后 change 继续保持 `ACTIVE` 等待用户验收和 reference gate 决策；DRAFT profile 不切为 ACTIVE，不接受 baseline、不归档、不恢复 IDLE。
- Commit：`pending`。

## 2026-07-23｜C10 offline TDD implementation 完成

- 实现范围：新增 `scripts/evaluate_quality_gate.py` 与 21 条 evaluator 回归，交付 `rag-quality-gate-profile-v1` schema 和 `rag-eval-dev-v2-retrieval-regression-v1` DRAFT profile；支持 versioned dataset/run/channel identity、固定 `all/type/difficulty/answerability` slices、retrieval/objective/judge 指标、hard threshold、reference regression AND、minimum denominator、fail-closed missing/error、`PASS/FAIL/NOT_EVALUABLE/INVALID=0/3/4/2`、脱敏 JSON/Markdown 与 `--no-overwrite`。
- TDD 证据：按纵向 RED→GREEN 锁定 ACTIVE pass、DRAFT fail-closed、invalid contract、固定切片与分母、required missing/error budget、hard+reference、objective/judge channel、no-answer completeness、selection/dataset/reference identity、maxInclusive、CLI output/exit code 与 no-overwrite；最终自审另以失败用例复现并修复“缺 judge score 抛 KeyError”“DRAFT 不保留预期规则”“metric 可错误绑定 channel”及“reference rule identity 未校验”四项边界。
- 文档与 profile：`docs/eval/RAG_EVAL_GUIDE.md` 已补两步运行、profile lifecycle/versioning、稳定退出码、CI 示例、raw artifact 与 external-call boundary；首个 12-rule retrieval profile 保持 `DRAFT / PENDING_REFERENCE_EVIDENCE`，12 个 target 均为 `null`，未猜测阈值或宣称质量结论。
- 验证：evaluator 聚焦为 21 tests / OK；dataset/direct/reproducible 关联 suites 为 105 tests / OK；最终 `python -B -m unittest discover -s scripts -p 'test_*.py'` 为 154 tests / OK。Direct 与 reproducible v2 plan-only 均为 `VALID`、各选 1 条，实际业务调用为 0。SensitiveLogs 扫描 311 source files / PASS；4 个 changed Markdown 本地链接 missing=0；C10 新增 guide section 的 secret/absolute-path 命中为 0；8 个 changed/untracked 文件中受保护或越界路径为 0；`git diff --check` 通过。
- 跳过项与外调：用户未授权 reference calls；未执行 v2/150×3 reference、debug retrieval、query embedding、rerank、ask、generation、judge、LLM/provider、backend 或任何数据出站，实际调用与费用均为 0。Java/POM/前端/依赖/生产配置无改动，因此 Maven、frontend build、Docker/Testcontainers、live backend 均 `SKIPPED`。OpenSpec CLI 当前不可用，未声称 CLI validation 通过。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、accepted baseline、v1/v2 release/fixture/review、历史 reports/history、Java/API、数据库、前端、production prompt/citation/retrieval/rerank/no-answer/default judge/provider；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：offline evaluator 已 ready，但没有正式 v2 reference evidence、具体阈值或重复运行稳定性证据；因此不能声明 ACTIVE quality gate、质量达标、production-ready 或 C10 完整归档。Change 保持 `ACTIVE`，等待用户验收和 reference gate 决策；不接受 baseline、不归档、不恢复 IDLE。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(评测): 实现C10离线质量阈值门禁`。
