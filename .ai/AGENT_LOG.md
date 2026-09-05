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

## 2026-07-23｜C10 offline implementation 提交补录

- Commit：`4e74b70d394d42c7dd3f1cae72e2a0e58228caf8`（`feat(评测): 实现C10离线质量阈值门禁`）。本条只补录上一执行提交的真实 hash，不记录本次验收归档改动。

## 2026-07-23｜C10 用户验收、baseline 接受与归档

- 用户授权与结果：用户确认 C10 offline implementation 验收完成并要求检查后归档。4 requirements / 12 scenarios 的 delta body 已原文接受进 `openspec/specs/evaluation/spec.md`；change 已移动到 `openspec/changes/archive/2026-07-23-eval-quality-threshold-gates/`，`.ai/ACTIVE_TASK.md=IDLE`，当前无未归档 change。
- Reference gate：用户此前明确不授权 reference calls，且本轮没有新的外调授权；v2/150×3 reference、最多 450 debug retrieval/可能 450 query embedding、locked reference summary、hard floors/tolerances 与 profile activation 全部按 `SKIPPED` 收口。首个 retrieval profile 保持 `DRAFT / PENDING_REFERENCE_EVIDENCE`、12 个 target 均为 `null`；本次归档只确认 offline gate framework，不确认 ACTIVE quality gate 或任何 retrieval/generation/citation/judge 质量达标。
- 长期事实源：同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`，明确 C10 已接受的能力与未完成的 reference/activation 边界。
- 验证：archive 4 个必需 artifacts 齐全、tasks 未勾选数 0、未归档 change 数 0、`ACTIVE_TASK=IDLE`；delta body 是 `evaluation` baseline exact suffix，计数为 4 requirements / 12 scenarios。`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 154 tests / OK；SensitiveLogs 扫描 311 source files / PASS。
- 跳过项与外调：本轮只有 OpenSpec baseline/archive 与长期文档收口，没有 Java、Python 实现、POM、前端、依赖或运行时配置改动，因此 Maven、frontend build、Docker/Testcontainers、live backend 均 `SKIPPED`。Reference/provider 调用、数据出站、费用与限流事件均为 0；OpenSpec CLI 当前不可用，未声称 CLI validation 通过。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、v1/v2 release/fixture/review、历史 reports/history、Java/API、数据库、前端、production prompt/citation/retrieval/rerank/no-answer/default judge/provider；未暂存、提交、push、创建 PR、部署或发布。
- 后续 readiness：冻结蓝图的下一主线候选为 C11 `genai-tracing-core`。其前置 C10 离线契约已接受，结论为“归档提交完成后可进入独立规划”；为避免把 C10 closeout 与 C11 proposal 混入同一提交，本轮不创建 C11 active change。用户手动提交本次归档并恢复干净工作区后，可启动 C11 proposal/design/tasks/spec delta 事前闸门。
- Commit：`pending`；提交责任为用户手动提交。建议 `chore(openspec): 验收并归档C10离线质量门禁`。

## 2026-07-23｜C11 GenAI tracing core readiness 与规划启动

- 用户决策与提交责任：用户要求检查项目状况，允许则直接开始 C11 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD=`d85d85a`，工作区干净，`main...origin/main [ahead 10]`，`.ai/ACTIVE_TASK.md=IDLE`；C10 4 requirements / 12 scenarios 已接受进 `evaluation` baseline 并归档，当前无未归档 change。冻结蓝图下一顺序项明确为 C11 `genai-tracing-core`，结论为 `GO`。
- 当前事实：Spring Boot 3.2.1 已管理 OTel BOM `1.31.0`，但仓库没有 OTel/Micrometer tracing runtime dependency；现有 `TraceFilter/TraceContext` 只有 custom header + MDC。`RedisAsyncTaskManager` 用 `CompletableFuture.supplyAsync` 且未传播 context；ingest 已有稳定 task/document/deterministic chunk identity，ask 已有 provider/fallback/citation diagnostics 和 sync/SSE 两条入口。
- 能力分类：`confirmed` 为 request MDC、durable task ledger、vector metadata、sync/stream QA 与 existing diagnostics；`partial` 为只有请求日志/诊断字段、无 OTel span/async propagation；`planned` 为 API/SDK 分层、default-off SDK、W3C/custom bridge、separate ingest/ask topology、stable lineage、安全 allowlist 与 in-memory verification；`out_of_scope` 为 C12 exporter/metrics/alerts/deployment/sampling、Java agent、生产 SLA/租户/质量激活；`unknown` 为真实 backend 吞吐、费用、retention、权限与采样。
- 规划 artifacts：创建 `2026-07-23-genai-tracing-core` 的 proposal/design/tasks 与 `rag-system` spec delta，激活 `.ai/ACTIVE_TASK.md`。Design 包含 14 条真实决策记录，delta 为 4 requirements / 12 scenarios。
- 关键边界：ingest durable task 使用 independent root + optional submission link，ask 与 ingest 不建 parent/child；跨时间只通过 `ingestTaskId/documentId/chunkId` lineage 关联，不持久化 OTel trace/span id。stream span 绑定 complete/error/cancel/timeout；raw question/prompt/answer/context/snippet/file/user/credential/provider body/error message/stack 全部禁止进入 telemetry；C11 runtime 不注册 network exporter。
- 外调与范围安全：规划阶段真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、外部传输和数据出站均为 0；未修改 baseline spec、POM/Java/test/config、数据库/API/DTO、评测资产、前端、默认 provider 或历史报告，未进入 C12+。
- 跳过项：规划只修改 OpenSpec/ACTIVE_TASK/AGENT_LOG，因此尚未运行 Maven、Python、frontend build、Docker/Testcontainers、live backend/provider 或 exporter；OpenSpec CLI 可用性和文档/安全门禁将在规划收口验证中记录。
- 剩余风险与下一闸门：default-off、W3C/custom bridge、lineage metadata、stream lifecycle 与 14 条决策仍需用户批准；用户还需明确授权新增 Boot BOM 已管理的 OTel API/SDK 依赖并进入 Java TDD。实现授权不包含任何 exporter、外部 provider 或数据出站。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C11 GenAI追踪核心规划`。

## 2026-07-23｜C11 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 共 4 个必需 artifacts；design 的 14 条 decisions 均完整包含“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C11，未归档 active change 数为 1。
- 文档与安全：SensitiveLogs 扫描 311 source files / PASS；6 个 changed/untracked Markdown 的本地相对链接 missing=0，当前规划文件没有本地链接；新增 change artifacts 的 credential value / Authorization token / `C:\Users\` 绝对路径命中为 0；CRLF 文件数 0；`git diff --check` 通过。
- 范围检查：`openspec/specs/` baseline、`scripts/`、`docs/eval/`、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、Java/POM、前端与历史 reports tracked diff 均为 0。当前只修改 ACTIVE_TASK、追加 AGENT_LOG 并新增 C11 change 目录。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划阶段没有 Java/POM/Python/前端实现改动，因此 Maven、Python 全量、frontend build、Docker/Testcontainers、live backend/provider 与 network exporter 均 `SKIPPED`。
- 外调与下一闸门：真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、数据出站、费用与限流事件均为 0。等待用户审阅并批准 default-off、separate trace、stable lineage、stream lifecycle、privacy contract、14 条 decisions、4/12 delta，并授权新增 OTel 依赖与进入 Java TDD。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C11 GenAI追踪核心规划`。

## 2026-07-23｜C11 规划提交补录

- Commit：`3e02074`（`docs(openspec): 启动C11 GenAI追踪核心规划`）。本条只补录上一规划提交的真实 hash，不记录本次 C11 实现改动。

## 2026-07-23｜C11 规划批准并获准进入 Java TDD

- 用户批准：proposal 的 C11/C12 边界、default-off、separate ingest/ask trace、stable lineage、stream lifecycle 与 privacy contract；design 的 14 条 decisions 和 `rag-system` delta 的 4 requirements / 12 scenarios 全部通过事前闸门。
- 实现授权：用户明确授权新增 Spring Boot 3.2.1 BOM 已管理的 OpenTelemetry API/SDK/test 依赖并进入 Java TDD；提交责任继续为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外调边界：授权不包含 OTLP/Zipkin/Jaeger exporter、metrics、告警、部署、真实 embedding/rerank/ask/generation/judge/LLM/provider 调用或数据出站；实现与测试只使用 no-op/in-memory SDK 和 fake dependencies。
- 执行方式：按 `tdd` skill 进行纵向 RED→GREEN，从 public tracing facade/context behavior 开始，再推进 request/async、ingest lineage、sync/stream ask；不一次性横向写完全部测试。
- Commit：`pending`。

## 2026-07-23｜C11 GenAI tracing core Java TDD implementation 完成

- 实现范围：按 Spring Boot 3.2.1 BOM 管理的 OTel `1.31.0` 分层新增 common API、admin SDK 与 test-only in-memory exporter；新增 default-off/no-op wiring、固定 instrumentation scope、安全 tracer facade、span/attribute/event allowlist、fail-open error recorder，以及 W3C 优先/custom pair 回退的 request/MDC bridge。Runtime 未注册 OTLP/Zipkin/Jaeger exporter。
- Ingest：`DocumentIndexingServiceImpl` 在 durable task 执行时创建 independent `rag.ingest` root，fresh submission 只以 optional link 关联，resume 无 parent/link；真实执行阶段覆盖 input open、parse/chunk、batch embedding、vector upsert、keyword upsert、SQL finalize，retry 不复制 root。Vector/keyword metadata 共享 `ingestTaskId/documentId/确定性 chunkId`，不持久化 OTel trace/span id。
- Ask：`RAGServiceImpl/QueryEngineImpl/AnswerGeneratorImpl` 已接入 cache/retrieval/query embedding/vector/keyword/fusion/rerank/generation/prompt/LLM/citation 固定拓扑；复用既有 requested/effective provider、fallback、attempt/retry diagnostics，真实 provider usage 与 prompt estimated token 分离。Sync cache-hit/no-result/success/error 与 stream complete/error/cancel/timeout 均按真实生命周期结束一次；`QAController` 通过 Reactor context terminal signal 将 SseEmitter timeout 与主动断连区分。
- Lineage 与隐私：只对最终 `topK` contexts 记录 bounded `rag.lineage.context` event，支持 `COMPLETE/PARTIAL/MISSING`；question/prompt/answer/context/source/file/title/credential/provider body/raw error message/stack 不进入普通 telemetry，不调用 `recordException`，动态 span name 与非 allowlisted attribute 被拒绝。Telemetry helper 自身失败降级 no-op，不改变业务异常、retry、fallback、持久化或响应语义。
- TDD 与验证：所有切片均先得到预期 RED，再做最小 GREEN。最终 `mvn -q test` 退出码 0，77 个 Surefire reports / 336 tests / 0 failures / 0 errors / 9 skipped；9 个 skip 均为需要 Docker 的 8 个 MySQL migration/recovery tests 与 1 个独立 Milvus stop/start test。Python 全量 `154 tests / OK`；SensitiveLogs 扫描 313 source files / PASS；OTel dependency tree 仅含 API/SDK/SDK-testing 及 SDK 自身模块，无 network exporter；3 个 changed Markdown 本地链接 missing=0；新增内容 secret/private-key/`C:\Users\` 命中 0；受保护路径与 accepted baseline diff 均为 0；`git diff --check` 通过，仅有既有 CRLF→LF 提示。
- 跳过项与外调：前端无改动，正式 frontend build `SKIPPED`。本轮不执行 Docker/Testcontainers/live backend；真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、外部 telemetry 传输、业务数据出站、模型费用与限流事件均为 0。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、accepted baseline、评测 release/fixture/history、数据库 schema、API/DTO、production provider/prompt/retrieval/rerank/citation/no-answer 默认行为；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：C11 只验证进程内 tracing contract，默认仍关闭；生产 exporter、metrics、alerts、sampling、retention、权限与部署属于 C12。Docker MySQL/Milvus live suites 本轮未执行。Change 保持 `ACTIVE`，等待用户验收；验收前不接受 delta、不归档、不恢复 `IDLE`。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(观测): 实现C11 GenAI追踪核心`。

## 2026-07-23｜C11 implementation 提交补录

- Commit：`e1aa401`（`feat(观测): 实现C11 GenAI追踪核心`）。本条只补录上一执行提交的真实 hash，不记录本次验收归档改动。

## 2026-07-23｜C11 验收与归档

- 用户验收：用户明确确认 C11 验收通过并要求归档；提交责任继续为 `用户手动提交`，Agent 未暂存、未提交、未 push、未创建 PR、未部署。
- 规格收口：`2026-07-23-genai-tracing-core` delta body 已原文接受为 `openspec/specs/rag-system/spec.md` 的 exact suffix，共 4 requirements / 12 scenarios；change 的 proposal、design、tasks 与 delta 已移入 `openspec/changes/archive/2026-07-23-genai-tracing-core/`，tasks unchecked=0，未归档 active change=0，`.ai/ACTIVE_TASK.md=IDLE`。
- 长期事实源：同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`，记录 default-off/fail-open OTel 1.31 tracing core、分离 ingest/ask trace、稳定 lineage、固定 topology、context/MDC、流式终态和隐私边界；C12 exporter、metrics、alerts、sampling、retention、权限与部署仍明确未完成。
- 本轮验证：delta exact suffix=`true`；4/12 计数正确；archive artifacts=4；11 个归档相关 Markdown 本地链接 missing=0；Python 全量 `154 tests / OK`；SensitiveLogs 扫描 313 source files / PASS；受保护路径 diff=0。新增文档定向扫描仅命中 AGENT_LOG 既有历史说明，无新增真实 credential 或用户目录绝对路径。
- 复用与跳过：本轮只做规格/文档/状态归档，未修改 Java/POM/前端/数据库 schema；因此未重跑 Maven、frontend build、Docker/Testcontainers/live backend。实现提交 `e1aa401` 已有同一代码状态的 `mvn -q test` 77 reports / 336 tests / 0 failures / 0 errors / 9 Docker-related skipped 证据。OpenSpec CLI 不在 PATH，未声称 CLI validation 通过。
- 外调与范围：真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、外部 telemetry 传输、业务数据出站、模型费用与限流事件均为 0；未触碰 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、评测 release/fixture/history 或 migration。
- 剩余风险：C11 仍是默认关闭的进程内 tracing contract，不代表生产观测栈或 production readiness；Docker MySQL/Milvus live suites 本轮未执行。后续 C12 必须另立 Type C change 并重新确认外部传输、费用、采样、retention、权限和部署边界。
- Commit：`pending`；建议 `chore(openspec): 验收并归档C11 GenAI追踪核心`。

## 2026-07-26｜C12 OTel export 与 metrics readiness 及规划启动

- 用户决策与提交责任：用户要求检查项目状况，若允许则直接开始 C12 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD=`5cdddd2`，工作区干净，`main...origin/main`，`.ai/ACTIVE_TASK.md=IDLE`；C11 4 requirements / 12 scenarios 已接受进 `rag-system` baseline 并归档，当前无未归档 change。冻结蓝图下一顺序项明确为 C12 `otel-export-and-metrics`，结论为 `GO`。
- 当前事实：C11 已有 OTel 1.31 API/SDK、default-off/fail-open tracing、固定 ask/ingest topology、stable lineage、context/MDC 与隐私 allowlist；runtime 尚无 network exporter、meter provider、Collector/Tempo/Prometheus/Grafana、dashboard、rules、sampling、retention 或观测访问控制。
- 能力分类：`confirmed` 为 C11 trace contract 与无 exporter 默认路径；`partial` 为 spans 有安全阶段事实但无低基数聚合；`planned` 为 OTLP gRPC trace/metric export、bounded batching、固定 metrics、Collector tail sampling、本机 reference stack、72h/7d retention、Grafana auth、dashboard/rules 与 synthetic smoke；`out_of_scope` 为 SaaS/公网/跨主机 telemetry、外部通知、生产 SLA/HA/容量/成本、租户观测权限与日志聚合；`unknown` 为真实生产流量、合规保留期、组织权限和生产阈值。
- 规划 artifacts：创建 `2026-07-26-otel-export-and-metrics` 的 proposal、design、tasks 与 `rag-system` spec delta，激活 `.ai/ACTIVE_TASK.md`。Design 包含 15 条真实决策记录，delta 为 4 requirements / 12 scenarios。
- 关键方案：三个 signal/export 开关独立且默认关闭；trace 与 metrics 都通过 localhost OTLP gRPC 到 Collector。Collector 将 error/timeout/cancel/fallback trace 全保留、普通 success 默认 tail-sample 10%，metrics 不采样；Tempo/Prometheus reference retention 为 72h/7d。只有 localhost OTLP/Grafana 暴露，Grafana 禁止 anonymous，credential 只从未跟踪 env/secret 注入。
- 指标与告警边界：固定 operation/stage/provider/fallback/actual-token instruments，仅允许 bounded labels；lineage/id/score/user/content/model 自由文本/error detail 不得进入 labels。Dashboard 覆盖 traffic/outcome/latency/provider/fallback/token/export；本地 rules 覆盖有最小流量门槛的 ask error、fallback 与 collector failure，不定义 latency SLA，不部署通知渠道。
- 外调与范围安全：规划阶段真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、Docker image 下载、telemetry 出站、费用与限流事件均为 0；未修改 baseline spec、POM/Java/test/runtime config、默认 compose、数据库/API/DTO、评测资产、前端或 provider 默认值。
- 跳过项：规划只修改 OpenSpec/ACTIVE_TASK/AGENT_LOG，尚未运行 Maven、Python、frontend build、Docker/Collector/Tempo/Prometheus/Grafana、live backend/provider 或 exporter。进入实现前需用户批准新增 OTel exporter/metrics SDK 依赖、固定 images 下载与本机 synthetic smoke；OpenSpec CLI 可用性和文档/安全门禁将在本轮规划验证中记录。
- 剩余风险与下一闸门：本机 backend、10% success sampling、72h/7d retention、15 条 decisions、4/12 delta 与 non-SLA alert 边界仍需用户批准；image 兼容版本须在实施前从官方发行源核对并固定。规划不代表生产观测栈、SLA、容量或长期费用已成立。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C12遥测导出与指标规划`。

## 2026-07-26｜C12 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 4 个必需 artifacts 齐全；design 的 15 条 decisions 均完整包含“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C12，未归档 active change 数为 1。
- 文档与安全：SensitiveLogs 扫描 313 source files / PASS；6 个 changed/untracked Markdown 本地相对链接 missing=0；新增 change/ACTIVE_TASK 定向 credential/value/用户目录扫描只命中 password/secret 规则说明，无真实值或用户目录绝对路径；相关 Markdown CRLF 文件数 0；`git diff --check` 通过。
- 范围检查：`openspec/specs/` accepted baseline、Java/POM/runtime YAML、默认 `docker-compose.yml`、前端、`.env.local`、`application-dev.yml`、`.agents/` 与 `docs/学习文档/` tracked diff 均为 0。当前只修改 ACTIVE_TASK、追加 AGENT_LOG 并新增 C12 change 目录。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划阶段没有 Java/POM/Python/前端/部署实现改动，因此 Maven、Python 全量、frontend build、Docker/Collector/Tempo/Prometheus/Grafana、live backend/provider 与 network exporter 均 `SKIPPED`。
- 外调与下一闸门：真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、镜像下载、telemetry 出站、费用与限流事件均为 0。等待用户审阅并批准本机 backend、default-off OTLP、metrics cardinality、tail sampling、72h/7d retention、Grafana access、dashboard/rules、15 条 decisions、4/12 delta，并授权新增依赖/固定 images/synthetic smoke 后才能进入实现。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C12遥测导出与指标规划`。

## 2026-07-26｜C12 规划提交补录

- Commit：`8f36b9d`（`docs(openspec): 启动C12遥测导出与指标规划`）。本条只补录上一规划提交的真实 hash，不记录本次 implementation 改动。

## 2026-07-26｜C12 规划批准并获准进入 Java/config TDD

- 用户批准：proposal 的本机自托管 backend、default-off OTLP、metrics、tail sampling、72h/7d retention、Grafana access、dashboard/local rules 与 non-goals；design 的 15 条 decisions 和 `rag-system` delta 的 4 requirements / 12 scenarios 全部通过事前门禁。
- 实现授权：用户明确要求开始实现，授权新增 OTel OTLP exporter/metrics SDK 依赖、下载固定 Collector/Tempo/Prometheus/Grafana images，并执行仅含 synthetic telemetry 的本机 smoke；提交责任继续为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外调边界：授权不包含真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider、SaaS backend、付费服务或公网/跨主机 telemetry；业务数据出站必须为 0。本机 reference stack 只接收 synthetic smoke 与显式启用后的本机应用 telemetry。
- 执行方式：按 `tdd` skill 采用纵向 RED→GREEN，先锁 default-off/config/export fail-open，再推进低基数 metrics、业务 lifecycle、reference stack、dashboard/rules 和 synthetic smoke；不一次性横向写完测试。
- Commit：`pending`。

## 2026-07-26｜C12 OTel export、低基数 metrics 与本机参考栈 implementation 完成

- 实现范围与文件：`rag-common/GenAiTelemetry` 在 C11 固定 lifecycle 上增加 operation count/duration/in-flight、stage duration、actual provider calls、fallback、actual token usage/coverage；`rag-admin` 新增 typed `GenAiObservabilityProperties`、OTLP gRPC trace/metric exporter、固定 histogram views、bounded batch/queue/timeout、`GenAiExportDiagnostics` 安全成功/失败计数与 endpoint/exception 日志脱敏，`application.yml` 的 tracing/metrics/export 三个开关继续默认关闭。新增 common/admin unit tests 与环境门控 synthetic/recovery smoke，`rag-admin/pom.xml` 只增加 BOM 管理的 `opentelemetry-exporter-otlp`。
- 参考栈：新增独立 `deploy/observability/`，固定官方发行版本 Collector Contrib `0.157.0`、Tempo `2.10.7`、Prometheus `3.13.1`、Grafana OSS `13.1.1`；Collector 使用 memory limiter/resource/tail sampling/batch，ERROR/TIMEOUT/CANCELLED/fallback 100% 保留、普通 success 10%，metrics pipeline 不采样；Tempo retention=72h，Prometheus retention=7d。宿主只绑定 `127.0.0.1:4317` 与 `127.0.0.1:3000`，Grafana anonymous=false，credential 只从未跟踪 env 注入。
- Dashboard/rules：provision Prometheus/Tempo datasource 与 `Enterprise RAG Observability` dashboard，覆盖 traffic/outcome/in-flight、operation/stage P50/P95/P99、provider/fallback、actual token coverage、Collector failure 和 TraceQL；Prometheus 成功加载 3 条 `non_sla_reference` rules：ask 10m error ratio、provider 15m fallback ratio、Collector receive/export/refuse failure。未加入 latency SLA、Alertmanager 或外部通知。
- TDD：default-off、metrics-only、trace-only/local export、远程/null endpoint 禁用、数值 clamp、固定 buckets、unreachable/timeout、queue pressure、export/flush/shutdown 安全事实均先得到预期 RED 再转 GREEN。5000-span / queue=64 / batch=1 的 blocking-exporter 压力测试在 5 秒边界内完成业务 lifecycle；OTLP 原始 warning 被收敛为固定 `OTLP export failed`，不含 endpoint、header、credential、异常 message/stack。
- Synthetic 闭环：只向 `127.0.0.1:4317` 发送 synthetic telemetry。Prometheus 查询得到 operation/stage/provider/fallback=`1/1/1/1`、actual token=`18`、coverage=`1`；扩展 smoke 的 101 个 operation metrics 全量可见，100 个普通 success traces 在 Tempo 保留 6 个，2 次 error/timeout smoke 均保留。exported trace sentinel 与禁止的 metric labels 均 0 命中。停止 Collector 后同一 JVM 先记录 trace/metric failure facts，12 秒后恢复 Collector 又记录两个 signal 的 success facts，无需重启 SDK。
- Backend/access 证据：compose render、Collector `validate`、Prometheus `promtool check config/rules`、dashboard JSON 与 provisioning parse 全部通过；Tempo `/ready=ready`，Grafana authenticated API 返回 dashboard uid/title，anonymous dashboard API 返回 401。实际 `HostConfig.PortBindings`：Collector 仅 localhost 4317、Grafana 仅 localhost 3000、Tempo/Prometheus 为空。稳定运行日志无 deprecated OTLP alias、插件下载或 provisioning error。
- 回归结果：最终 `mvn -q test` 退出码 0，81 个 Surefire reports / 352 tests / 0 failures / 0 errors / 3 skipped；skip 为两个默认关闭的 synthetic/recovery smoke 与既有 Milvus live IT，两个 smoke 已在明确环境门控下分别真实执行通过。Python 全量 `159 tests / OK`；reference 静态契约 `5 tests / OK`；SensitiveLogs 扫描 315 source files / PASS；受保护路径与 tracked workspace 绝对路径命中 0；官方 release links 已逐项打开验证；`git diff --check` 通过，仅保留既有 POM CRLF→LF 提示。
- 外调与异常处理：真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider/SaaS 调用、业务数据出站、模型费用与限流事件均为 0。授权内下载了四个固定 Docker images。首次 Grafana 13.1.1 启动发现默认预装插件会额外下载公网插件；随即增加 `GF_PLUGINS_PREINSTALL_DISABLED=true`、禁用 plugin admin/auto-update/public-key retrieval，并用全新 project/volume 重建验证不再下载。首次验证的 Docker volumes 未删除，避免未经授权的数据删除。
- 跳过项：前端无改动，正式 frontend build `SKIPPED`。未执行真实业务 provider、SaaS、公网 telemetry、Alertmanager、push、PR、部署或生产容量/SLA 验证。
- 范围安全：未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、accepted baseline、数据库 schema、API/DTO、production provider/prompt/retrieval/rerank/citation/no-answer 默认行为；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：本实现只证明单机 reference 闭环；6/100 success sampling 是本次 deterministic synthetic 结果，不是统计 SLA/容量结论。Docker images 与 named volumes 保留在本机，生产 HA、容量、合规 retention、租户权限、跨主机传输和通知仍 out of scope。Change 保持 `ACTIVE`，等待用户验收；验收前不接受 4 requirements / 12 scenarios delta、不归档、不恢复 `IDLE`。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(观测): 实现C12遥测导出与低基数指标`。

## 2026-07-26｜C12 implementation 提交补录

- Commit：`ac48cd0`（`feat(观测): 实现C12遥测导出与低基数指标`）。本条只补录上一执行提交的真实 hash，不记录本次验收归档改动。

## 2026-07-26｜C12 验收与归档

- 用户验收：用户明确确认 C12 验收通过并要求归档；提交责任继续为 `用户手动提交`，Agent 未暂存、未提交、未 push、未创建 PR、未部署。
- 规格收口：`2026-07-26-otel-export-and-metrics` delta body 已原文接受为 `openspec/specs/rag-system/spec.md` 的 exact suffix，共 4 requirements / 12 scenarios；change 的 proposal、design、tasks 与 delta 已移入 `openspec/changes/archive/2026-07-26-otel-export-and-metrics/`，tasks unchecked=0，未归档 active change=0，`.ai/ACTIVE_TASK.md=IDLE`。
- 长期事实源：同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`，记录 default-off/fail-open OTLP gRPC export、低基数 metrics、本机 Collector/Tempo/Prometheus/Grafana、关键 trace 全保留、普通成功 trace 10% tail sampling、72h/7d retention、认证访问及 non-SLA rules；生产 HA、容量、合规 retention、租户权限、跨主机传输、通知与 SLA 仍保持未完成或 out of scope。
- 本轮验证：delta exact suffix=`true`；4 requirements / 12 scenarios；archive artifacts=4；tasks unchecked=0；未归档 active change=0；`.ai/ACTIVE_TASK.md=IDLE`。Python 全量 `159 tests / OK`；SensitiveLogs 扫描 315 source files / PASS；11 个归档相关 Markdown 本地链接 missing=0；受保护路径 diff=0；预期范围外改动=0；新增内容用户目录绝对路径与 secret value pattern 命中均为 0；`git diff --check` 通过。OpenSpec CLI 不在 PATH，未声称 CLI validation 通过。
- 复用与跳过：本轮只做规格/文档/状态归档，未修改 Java/POM/前端/数据库 schema；因此不重跑 Maven、frontend build、Docker/Testcontainers/reference stack/live backend。实现提交 `ac48cd0` 已有同一代码状态的 `mvn -q test` 81 reports / 352 tests / 0 failures / 0 errors / 3 skipped，以及完整本机 synthetic reference 闭环证据。OpenSpec CLI 若仍不在 PATH，则不声称 CLI validation 通过。
- 外调与范围：本轮真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter/SaaS 调用、telemetry 出站、业务数据出站、费用与限流事件均为 0；未触碰 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、评测 release/fixture/history 或 migration。
- 剩余风险：C12 只证明默认关闭的单机 reference observability 闭环；6/100 success sampling 是 synthetic 结果，不是 SLA/容量结论。Docker images 与 named volumes 仍保留在本机；生产 HA、容量/费用、合规 retention、租户权限、跨主机传输、通知与 SLA 仍需独立 change。
- Commit：`pending`；建议 `chore(openspec): 验收并归档C12遥测导出与指标`。

## 2026-07-26｜C13a tenant model/context/migration readiness 及规划启动

- 用户决策与提交责任：用户要求检查项目状况，若允许则直接开始 C13 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD=`250a5c3`，工作区干净，`main...origin/main [ahead 3]`，`.ai/ACTIVE_TASK.md=IDLE`；C12 的 4 requirements / 12 scenarios 已接受进 `rag-system` baseline 并归档，tasks unchecked=0，当前无未归档 change。冻结蓝图下一顺序项为拆分后的 C13a，结论为 `GO`。
- 当前事实：已有数据库用户认证、JWT access/refresh、refresh 时重载用户、`CurrentUserService`、owner/public/用户级 KB 授权与 Flyway V1-V9；`user`、`knowledge_base`、`UserPrincipal`、JWT 和 request context 均没有 tenant identity，不能宣称多租户隔离。
- 能力分类：`confirmed` 为 user/owner/permission 与 migration test 入口；`partial` 为已有用户级授权但没有 tenant 根边界；`planned` 为 legacy tenant、user/KB tenant_id、服务端 JWT tenant claim、immutable request identity、旧 token fail-closed 与 fresh/V9 migration tests；`out_of_scope` 为 C13b SQL/vector/cache/task/history enforcement、tenant CRUD/switch、多 membership、C14 隔离评测、C15 MCP 与 C16 Router；`unknown` 为真实组织层级、SSO/provisioning、跨租户管理员与合规政策。
- 规划 artifacts：创建 `2026-07-26-tenant-model-context-and-migration` 的 proposal、design、tasks 与 `rag-system` spec delta，激活 `.ai/ACTIVE_TASK.md`。Design 包含 12 条真实决策记录，delta 为 4 requirements / 12 scenarios。
- 关键方案：C13 继续拆分；C13a 只建立单用户单 tenant、稳定 `legacy-default` 回填、`user/knowledge_base.tenant_id`、数据库 principal→服务端 JWT→immutable context 的身份链。部署前无 tenant claim token fail closed 并要求重新登录；不信任 header/query/body/metadata，不使用全局 tenant ThreadLocal。
- 范围边界：C13a 只做暗铺设，不开放 tenant CRUD/membership/switch 或第二业务 tenant，不修改 SQL/API/Milvus/Qdrant/Elasticsearch/cache/task/history 的强制过滤，不把字段/JWT 就绪描述为隔离完成。C13b 实现且 C14 评测通过前不得开放 C15/C16。
- 外调与范围安全：规划阶段真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0；未修改 accepted baseline、Java/POM/test/runtime config、migration、API/DTO、数据库实际 schema、评测资产、前端或 provider 默认值。
- 跳过项：规划只修改 OpenSpec/ACTIVE_TASK/AGENT_LOG，因此尚未运行 Maven、Python、frontend build、Docker/Testcontainers、live database/backend/provider。OpenSpec CLI 可用性和文档/安全门禁将在本轮规划验证中记录。
- 剩余风险与下一闸门：单用户单 tenant、legacy 回填、旧 token 重新登录、无物理外键、12 条 decisions 与 4/12 delta 仍需用户批准；规划不等于批准 migration/Java 实现，也不证明跨租户隔离。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C13a租户模型与上下文规划`。

## 2026-07-26｜C13a 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 4 个必需 artifacts 齐全；design 的 12 条 decisions 均完整包含“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”；delta 为 4 requirements / 12 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C13a，未归档 active change 数为 1。
- 文档与安全：SensitiveLogs 扫描 315 source files / PASS；5 个本轮核心 Markdown 的本地相对链接 missing=0、trailing whitespace=0、CRLF files=0；新增规划内容的 credential value、Authorization token 与用户目录绝对路径命中为 0；`git diff --check` 通过。
- 范围检查：当前状态只包含 `.ai/ACTIVE_TASK.md`、append-only `.ai/AGENT_LOG.md` 与新增 C13a change 目录；accepted `openspec/specs/`、Java/POM/test/runtime config、migration、docs、评测资产、前端、`.env.local`、`application-dev.yml`、`.agents/` 与 `docs/学习文档/` tracked diff 均为 0。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划阶段没有实现改动，因此 Maven、Python 全量、frontend build、Docker/Testcontainers、live database/backend/provider 均 `SKIPPED`。
- 外调与下一闸门：真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0。等待用户审阅并批准 proposal、12 条 decisions、4/12 delta、旧 token 重新登录和 C13a/C13b 边界后，才能进入 migration/auth/context TDD。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C13a租户模型与上下文规划`。

## 2026-07-26｜C13a 规划提交补录

- Commit：`15036c8`（`docs(openspec): 启动C13a租户模型与上下文规划`）。本条只补录上一规划提交的真实 hash，不记录本次 C13a 实现改动。

## 2026-07-26｜C13a 规划批准并进入 migration/auth/context TDD

- 用户批准：proposal 的单用户单 tenant、稳定 legacy tenant 回填、旧 token 无 tenant claim 时重新登录、服务端签发 identity、immutable context 与 C13a 暗铺设边界；design 的 12 条决策和 `rag-system` delta 的 4 requirements / 12 scenarios 通过事前门禁。
- 实现授权：开始实现 schema migration、auth persistence/principal/JWT 与 request identity，并执行聚焦及全量本地验证；不新增依赖。提交责任继续为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- TDD 方式：遵循 `tdd` skill，使用 migration → auth/JWT → request identity 的纵向 RED→GREEN；一次只锁定一个可观察行为，不先横向写完全部测试。
- 范围边界：不实施 C13b SQL/vector/cache/task/history 强制隔离，不开放 tenant CRUD/membership/switch 或第二业务 tenant，不执行 C14 隔离评测，不进入 C15/C16。
- 外调边界：真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件必须保持 0。
- Commit：`pending`。

## 2026-07-26｜C13a tenant migration、认证身份与 immutable context implementation 完成

- 实现范围与文件：新增 `V10__tenant_model_and_legacy_backfill.sql`，创建唯一 `legacy-default` tenant，按 nullable→全量回填→NOT NULL/index 顺序为 `user`、`knowledge_base` 建立 tenant 归属；新增真实 MySQL 8.0.36 migration test。扩展 `AuthUser`、repository/account、`UserPrincipal`、`UserDetailsServiceImpl`、JWT provider 与 bootstrap tenant lookup；新增 immutable `RequestIdentity(userId, tenantId)`，由 `CurrentUserService` 统一构造，并在知识库创建链路显式写入服务端 tenant。同步 auth/bootstrap/JWT/context/KB/property/C5 recovery/HappyPath fixtures 与测试；未改变 API 请求/响应 DTO 形状。
- TDD 证据：migration 首轮 RED 为 `tenant` 表不存在；auth persistence RED 为 `tenantId` 未进入 account/principal，禁用 tenant 用户仍可加载；JWT RED 分别证明 tenant claim 未 round-trip、旧无 tenant claim token 被接受、缺 tenant principal 仍可签发；context/KB RED 证明尚无 `RequestIdentity` 且 create 仍只接收 ownerId。逐项 GREEN 后，8 个聚焦 suites 共 67 tests / 0 failures / 0 errors / 0 skipped。
- 迁移与兼容：fresh install、V9→V10 有数据升级、Flyway validate、重复 migrate 均通过；正常/禁用/逻辑删除 user、public/private/逻辑删除 KB 全部回填，user/KB 主键、owner、public 与 `kb_permission` 关系不变。C5 恢复夹具同步 latest=V10 和知识库 tenant 归属后，`C5RecoveryMySqlTest` 通过。V1-V9 tracked diff=0，migration versions 1..10 唯一且连续。
- 认证与失败语义：数据库登录只接受存在且 enabled/not-deleted tenant 的 user；access/refresh token 都由服务端签发正整数 tenant claim，缺失、零/负数、非整数或错误类型 fail closed，缺 tenant principal 不得签发 token；refresh 仍重载 fresh database principal 并使用 fresh tenant。旧无 tenant claim token 被拒绝，需要重新登录，不提供 fixed/header/query/body/metadata fallback；日志和错误不回显 tenant 原始输入或 token。
- 请求身份与端到端：`RequestIdentity` 同时要求正数 userId/tenantId，不使用 ThreadLocal。知识库 create 从认证 principal 派生 owner/tenant；`HappyPathIT` 同时伪造 header/query/body/metadata tenant 值，数据库仍保存 owner user 的 tenant，并完成登录、建库、上传、异步索引、Milvus retrieval、删除和资源清理。正常 reactor 生命周期下 `TenantModelMigrationMySqlTest` 2/0/0/0、`HappyPathIT` 1/0/0/0，命令 59.4 秒退出码 0；全部数据均为合成数据，embedding 使用 test-scope deterministic provider。
- 全量验证：最终 `mvn -q test` 111.7 秒退出码 0，79 个 Surefire reports / 360 tests / 0 failures / 0 errors / 2 skipped；两个 skip 分别因未设置 `RAG_OBSERVABILITY_SMOKE` 与 `RAG_OBSERVABILITY_RECOVERY_SMOKE`，不属于 C13a。首次全量运行中 `GenAiTracingConfigurationTest` 出现一次 OTLP logger 时序断言波动，单独复跑通过，随后完整全量重跑干净通过。Python 全量 159 tests / OK；SensitiveLogs 扫描 317 source files / PASS。
- 命令诊断：第一次完整 `-Pc3-integration verify` 暴露并修正两处历史 C5 fixture（latest V9 写死、手工 KB insert 缺 tenant_id）；修正后 C5 单测通过。随后一次完整 C3 命令在 HappyPath XML 已为 1/0/0/0 后被 184 秒工具超时终止；一次绕过 reactor 的直接 Failsafe 诊断因错误发现全部 4 个 IT 且缺 reactor classpath 被弃用。最终使用正常 `-am ... verify` 生命周期并限定 `-Dit.test=HappyPathIT` 获得退出码 0，不把无效命令当成产品失败或通过证据。
- 静态与范围门禁：30 个 changed files 中 protected paths=0、C13b vector/Qdrant/Elasticsearch/cache/Redis/task/history production surfaces=0、ThreadLocal=0、V1-V9 migration=0；新增内容 credential/Authorization/用户目录绝对路径命中 0；3 个 changed Markdown relative links missing=0；`git diff --check` 通过，仅有既有 CRLF→LF 和用户级 git ignore 权限 warning。前端无改动，正式 frontend build `SKIPPED`。
- 外调与范围安全：真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、模型费用与限流事件均为 0；未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、accepted baseline、生产 provider/prompt/retrieval/rerank/citation/no-answer 默认行为；未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：C13a 仍是单一 legacy tenant 暗铺设，不证明跨租户隔离；SQL/API/permission、所有 vector adapters、cache/task/history 强制 tenant filtering 仍属于 C13b，C14 隔离评测通过前不得开放 C15/C16。Change 保持 `ACTIVE`，等待用户验收；验收前不接受 4 requirements / 12 scenarios delta、不归档、不恢复 `IDLE`。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(租户): 实现C13a租户模型与身份上下文`。

## 2026-07-26｜C13a implementation 提交补录

- Commit：`a864020`（`feat(租户): 实现C13a租户模型与身份上下文`）。本条只补录上一执行提交的真实 hash，不记录本次验收归档改动。

## 2026-07-26｜C13a 验收与归档

- 用户验收：用户明确确认 C13a migration、auth/context evidence、旧 token 边界与非隔离声明验收通过，并要求满足条件时直接收口；提交责任继续为 `用户手动提交`，Agent 未暂存、未提交、未 push、未创建 PR、未部署。
- 规格收口：`2026-07-26-tenant-model-context-and-migration` delta body 已原文接受为 `openspec/specs/rag-system/spec.md` 的 exact suffix，共 4 requirements / 12 scenarios；change 的 proposal、design、tasks 与 delta 已移入 `openspec/changes/archive/2026-07-26-tenant-model-context-and-migration/`，archive files=4、tasks unchecked=0、未归档 active change=0，`.ai/ACTIVE_TASK.md=IDLE`。
- 长期事实源：同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`，记录 V10 唯一 legacy tenant、user/knowledge-base 非空 tenant identity、数据库认证与 JWT tenant claim、refresh reload、immutable `RequestIdentity`、旧 token fail-closed 和客户端 selector 不生效；同时明确 C13a 只证明 tenant model/context readiness，不证明跨租户隔离。
- 下一阶段边界：下一轮是独立 Type C change C13b，必须从服务端 `RequestIdentity` 覆盖 SQL/API/permission、所有启用 vector adapters、cache/task/history/feedback；不支持的 adapter 必须 fail closed。C13b 完成且 C14 隔离与恶意样本评测通过前，不开放 C15 MCP/C16 Router，也不宣称租户隔离成立。
- 本轮验证：baseline exact suffix=`true`；4 requirements / 12 scenarios；archive files=4；tasks unchecked=0；未归档 active change=0；`.ai/ACTIVE_TASK.md=IDLE`。Python 全量 159 tests / OK；SensitiveLogs 扫描 317 source files / PASS；11 个收口 Markdown 的本地相对链接 missing=0；代码/配置范围外改动=0；新增内容用户目录绝对路径与 credential value pattern 命中均为 0；`git diff --check` 通过。OpenSpec CLI 不在 PATH，未声称 CLI validation 通过。
- 复用与跳过：本轮只做规格、文档和状态归档，未修改 Java/POM/前端/runtime config/migration，因此不重跑 Maven、frontend build、Docker/Testcontainers/C3 HappyPath。实现提交 `a864020` 已有同一代码状态的 `mvn -q test` 79 reports / 360 tests / 0 failures / 0 errors / 2 environment-gated OTLP smoke skips，MySQL migration 2/0/0/0 与 HappyPath 1/0/0/0 证据。
- 外调与范围安全：本轮真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0；未触碰 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、评测 release/fixture/history、production provider/prompt/retrieval/rerank/citation/no-answer 默认行为或 V1-V10 migration。
- 剩余风险：当前仍只有单一 legacy tenant，SQL/vector/cache/task/history 等数据面未做 tenant enforcement，不能创建第二业务 tenant或作多租户隔离承诺。真实组织层级、多 membership、provisioning/SSO、跨租户管理员、数据保留与生产迁移回退策略仍需后续独立决策。
- Commit：`pending`；建议用户手动提交 `chore(openspec): 验收并归档C13a租户模型与上下文`。

## 2026-07-26｜C13b tenant data-plane enforcement readiness 及规划启动

- 用户决策与提交责任：用户要求检查当前项目状态，满足条件则直接开始 C13b 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动前 HEAD=`69cd868`，工作区干净，`main...origin/main [ahead 6]`，`.ai/ACTIVE_TASK.md=IDLE`；C13a 的 4 requirements / 12 scenarios 已接受进 `rag-system` baseline 并归档，tasks unchecked=0，当前无未归档 change。C13a 已建立 V10 tenant/user/KB、JWT tenant claim、refresh reload 与 immutable `RequestIdentity`，冻结下一项明确为 C13b，结论为 `GO`。
- 当前事实：多数 controller/service/authorization 仍只传裸 userId/resource ID；document/chunk/kb_permission/history/feedback/task 没有 tenant column；vector/RAG/keyword 接口接受裸 collection/filter，三个 adapter 的 get/delete/count/lifecycle 没有 tenant contract；QA/embedding/session/idempotency/task Redis key、durable recovery/input path 未统一 tenant namespace。因此当前不能开放第二业务 tenant或宣称租户隔离成立。
- 能力分类：`confirmed` 为 C13a identity、单 KB vector collection、MySQL/Redis/Milvus integration 入口与 durable ledger；`partial` 为 KB tenant root、少数 RequestIdentity 创建路径及普通 vector filter；`planned` 为 V11 child tenant backfill、显式 SQL/API/permission、tenant-local public/permission、Milvus contract、unsupported adapter fail startup、legacy vector maintenance/readiness、Redis/task/input/keyword 隔离；`out_of_scope` 为 tenant CRUD/switch/membership、tenant RBAC、C14/C15/C16；`unknown` 为真实组织/容量、真实向量规模、生产 Qdrant/Elasticsearch 使用情况与合规政策。
- 规划 artifacts：创建 active change `tenant-data-plane-enforcement` 的 proposal、design、tasks 与 `rag-system` spec delta，并将 `.ai/ACTIVE_TASK.md` 置为 `ACTIVE`。Design 包含 15 条真实决策记录，delta 为 6 requirements / 18 scenarios，tasks 为 62 个未执行项。
- 关键方案：用户请求路径显式传播 `RequestIdentity`，六类 child/business table 冗余非空 tenantId；跨 tenant not-found、同 tenant无权限 forbidden，public/permission 只在 tenant 内生效。Milvus 是最小已验证 adapter，Qdrant/Elasticsearch 未通过完整 contract 时启动失败。旧向量由默认关闭的 maintenance mode 复用现有 vector 原位补 marker，未 READY 的 KB fail closed，不允许 legacy-default fallback。
- 缓存与异步边界：session/QA/embedding/idempotency/task projection 使用 v2 tenant key；旧业务 key 不双读，session 重新登录、cache 重算、task projection 从 tenant-scoped durable ledger 重建。token blacklist 与 global IP rate limit 保持明确 global security scope。后台恢复从 ledger tenantId 构造 execution scope，不使用 ThreadLocal/SecurityContext 推断。
- 外调与范围安全：规划阶段真实 Milvus/Qdrant/Elasticsearch maintenance、embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0；未修改 accepted baseline、Java/POM/test/runtime config、migration、数据库实际 schema、评测资产、前端或 provider 默认值。
- 剩余风险与下一闸门：child tenant 冗余、404/403 语义、Milvus-first、legacy vector readiness、Redis v2 冷启动和 15 条 decisions / 6/18 delta 仍需用户批准。规划不等于 schema/Java/Redis/Milvus 实现授权，也不证明真实向量已迁移或租户隔离成立。
- Commit：`pending`；建议用户手动提交 `docs(openspec): 启动C13b租户数据面隔离规划`。

## 2026-07-26｜C13b 规划门禁验证

- 结构与状态：proposal/design/tasks/spec delta 4 个必需 artifacts 齐全；design 的 15 条 decisions 均完整包含“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”；delta 为 6 requirements / 18 scenarios；`.ai/ACTIVE_TASK.md=ACTIVE` 且只指向 C13b，未归档 active change 数为 1。
- 文档与安全：SensitiveLogs 扫描 317 source files / PASS；5 个本轮核心 Markdown 的本地相对链接为 0、missing=0，新增内容 trailing whitespace=0；credential value、Authorization token 与用户目录绝对路径定向扫描无命中；`git diff --check` 通过，仅有用户级 git ignore 权限 warning。
- 范围检查：规划目标仅为 `.ai/ACTIVE_TASK.md`、append-only `.ai/AGENT_LOG.md` 与新增 C13b change 目录；accepted `openspec/specs/`、Java/POM/test/runtime config、V1-V10 migration、architecture/roadmap/optimization、评测资产、前端、`.env.local`、`application-dev.yml`、`.agents/` 与 `docs/学习文档/` 不在计划改动范围。
- 跳过项：OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；规划阶段没有实现改动，因此 Maven、Python、frontend build、Docker/Testcontainers、live database/backend/vector adapter/maintenance/provider 均 `SKIPPED`。
- 外调与下一闸门：真实 vector maintenance、embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0。等待用户审阅并批准 proposal、15 条 decisions、6/18 delta 与 tasks 后，才能从 V11 migration RED 开始实现；任何真实 legacy vector audit/backfill 仍需单独授权。
- Commit：`pending`；提交责任为用户手动提交。建议 `docs(openspec): 启动C13b租户数据面隔离规划`。

## 2026-07-26｜C13b 规划提交补录

- Commit：`17093d2`（`docs(openspec): 启动C13b租户数据面隔离规划`）。本条只补录上一规划提交的真实 hash，不记录本次 C13b 实现改动。

## 2026-07-26｜C13b 规划批准并进入 data-plane enforcement TDD

- 用户批准：proposal 的 V11 child tenant backfill、显式 `RequestIdentity`、tenant-local public/permission、跨 tenant not-found、Milvus-first、unsupported adapter fail startup、legacy vector maintenance/readiness、Redis v2 冷启动及 C14 后置边界；design 的 15 条决策和 `rag-system` delta 的 6 requirements / 18 scenarios 通过事前门禁。
- 实现授权：开始按 `tasks.md` 实现 SQL/API/permission、task/cache/history、RAG/keyword/Milvus tenant enforcement，并执行聚焦与全量本地验证；不新增或升级依赖。提交责任继续为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- TDD 方式：遵循 `tdd` skill，以 migration→SQL/API/permission→task/cache→RAG/vector→maintenance/readiness 的纵向 tracer bullet 逐项 RED→GREEN；测试通过公共接口验证行为，不先横向写完全部测试，不在 RED 状态顺手重构。
- 外调边界：本次授权不包含真实 legacy vector audit/backfill，不调用真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider。Testcontainers 中的合成 MySQL/Redis/Milvus integration 允许执行，且必须记录模型调用为 0。
- 完成边界：C13b 完成时只可报告 data-plane enforcement evidence；C14 通过前不开放第二业务 tenant、tenant CRUD/switch、C15 MCP 或 C16 Router，也不宣称生产租户隔离成立。
- Commit：`pending`。

## 2026-07-26｜C13b V11 tenant data-plane migration（TDD）

- 范围：新增 `V11__tenant_data_plane_enforcement.sql`，为 `document`、`document_chunk`、`kb_permission`、`qa_history`、`qa_feedback`、`async_task` 增加非空 `tenant_id`；补六类 entity 映射与 REST 隐藏；修正 `KnownSeedMigrationMySqlTest`、`C5RecoveryMySqlTest` 中原本没有可信 user/KB/owner 父事实的历史 fixture。未修改 V1-V10、未清表、未新增依赖。
- 迁移门禁：MySQL DDL 非整份事务化，因此 V11 在任何永久 DDL 前用 temporary table + 命名 CHECK 校验 root tenant、KB owner、document uploader、chunk parent、permission/history/feedback user/parent 与 task owner/document；回填后再做 non-null postflight。ownerless legacy task、missing root、跨 tenant uploader/permission 均在永久列创建前失败；不回填 `legacy-default` 猜归属。
- RED 证据：首次 happy fixture 因目标表无 `tenant_id` 报 `Unknown column 'tenant_id'`；收紧反例后，旧实现会在失败前留下已提交的 V11 列（expected 0, actual 1）；六类 entity 测试首次因缺少 `get/setTenantId` 在 testCompile 失败；历史 V6 document fixture 首次由 `chk_c13b_document_parent` 正确拒绝。
- GREEN：`mvn -q -pl rag-admin -am '-Dtest=TenantDataPlaneMigrationMySqlTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`，MySQL `8.0.36`、Flyway `9.22.3`，Tests `8` / Failures `0` / Errors `0` / Skipped `0`，覆盖 tenant A/B、逻辑删除、nullable-KB history、owner-only task、字段保持、fresh V1→V11、repeat migrate、validate 与 index `SEQ_IN_INDEX`。
- 兼容验证：`KnownSeedMigrationMySqlTest#v6DocumentRowsRemainCompatibleAfterDurableInputMigration` Tests `1/0/0/0`；`C5RecoveryMySqlTest#v7LegacyLedgerAndDuplicateChunksRemainCompatible` Tests `1/0/0/0`；`TenantDataPlaneEntityTest,DocumentSerializationTest` Tests `2/0/0/0`。均使用合成 Testcontainers 数据。
- 跳过与风险：尚未运行完整 migration/auth/admin/full Maven；V11 使用 `CREATE TEMPORARY TABLE`，部署 migration account 需对应权限；preflight 可挡已知数据冲突，但索引/ALTER 的容量、锁或环境失败仍需上线前备份与恢复预案。应用新写入的一致性由后续 SQL/task service 切片继续收口。
- 外调：真实 provider、embedding、rerank、ask/generation/judge/LLM、vector maintenance 调用均为 `0`；真实 legacy vector audit/backfill `SKIPPED`（未获单独授权）。
- Commit：`pending`；提交责任为用户手动提交。建议 `feat(租户): 增加V11数据面租户迁移门禁`。

## 2026-07-26｜C13b Milvus legacy schema 实现期阻断发现

- 已确认事实：根 `pom.xml` 固定 `milvus-sdk-java 2.3.4`；当前 `MilvusVectorStore` 把 `metadata` 建为 `VarChar`，既有 collection 没有独立 `tenant_id` / `kb_id` 标量字段；当前仓库没有 KB vector readiness 的 SQL 字段/表。
- 影响：已批准决策 8 的“既有 collection 原位补 marker”不能由当前已证明的 schema/API 能力直接实现；把 marker 继续塞进 VarChar 或只给 search 拼过滤不能覆盖 get/getByIds/delete/count/drop，不得作为完成证据。
- 处理：在 `design.md` 新增待用户确认的决策 16，并在 tasks 增加实现期事前闸门。确认前 legacy collection 保持非 READY，maintenance/runtime fail closed；migration、SQL/API、task/cache、reserved filter 与新 collection scope 等独立切片继续推进。
- 选择待定：tenant-aware shadow collection + 复制既有 vector/content + SQL mapping/readiness 切换；或另立依赖升级与 schema-evolution contract。Agent 未替用户选择，未新增/升级依赖。
- 外调：真实 Milvus audit/backfill、provider、embedding、rerank、LLM 调用均为 `0`；真实 maintenance 为 `SKIPPED`。
- Commit：`pending`。

## 2026-07-26｜C13b 实现暂停检查点

- 暂停决定：用户明确要求暂停并收尾，明日再继续开发。所有并行 Agent 已停止，当前没有运行中的测试或后台命令；`.ai/ACTIVE_TASK.md` 保持 `ACTIVE`，阶段改为“暂停检查点”，没有把部分实现误置为 `IDLE` 或 C13b 完成。
- 已完成且已有聚焦 GREEN：V11 migration 与六类 entity tenant 根（证据见上一条 migration 日志）；KB detail 的 controller→`RequestIdentity`→tenant-scoped KB/permission/document count tracer（相关 27 tests / 0 failures / 0 errors / 0 skipped）；Redis v2 key contract、auth session v2（auth 17 tests 通过）与 idempotency v2（10 个聚焦测试通过，`rag-common` 全量和 `rag-auth -am` 全量退出码 0）；reserved tenant/KB/collection filter、immutable `TenantVectorScope` 与 Qdrant/Elasticsearch enforcement-mode startup guard 的聚焦测试通过。
- 本次收尾完成：QA history 的 ask/stream/history controller 统一取得 `RequestIdentity`，save/get/page/delete 持久化或查询 `tenant_id + authenticated user_id`，跨 tenant/非 owner 以 scoped lookup 隐藏；旧 `QAHistoryServiceImpl` 裸入口 fail closed。`QAHistoryTenantEnforcementTest,QAControllerTest,AuthorizationServiceTest` 共 25 tests / 0 failures / 0 errors / 0 skipped；此前独立 `QAHistoryTenantEnforcementTest` 为 3/0/0/0。
- task ledger 当前进度：create/find/claim/release/heartbeat/phase/retry/reconciliation/completion 已显式传播 tenantId，mapper 条件包含 `tenant_id`，system-wide `scanClaimable` 对缺失 tenant 的记录 fail closed；document indexing 与 reconciliation coordinator 已传播 durable tenant。`mvn -q -pl rag-admin -am -DskipTests test` 在暂停前和收尾后均退出码 0，证明生产与测试源码可编译；新增 ledger 行为测试与 C5 真 SQL transition 的最终 GREEN 运行被暂停指令中止，不能宣称已通过。`completeFinalization/isCompleted`、`IndexTaskSqlFinalizer`、task projection 仍是明确 gap。
- 留给明日的有效起点：`KnowledgeBaseListTenantEnforcementTest` 已写入但生产 list 链仍是 `requireUserId -> getAccessibleByUserId(userId)`，且尚未执行到行为断言；明日应先运行它取得干净 RED，再实现 tenant-scoped owner/public/permission/final list 和 document count。随后先补跑 ledger 聚焦测试，不跨过失败继续扩展。
- 实现期新决策闸门：Milvus SDK 2.3.4 的既有 VarChar metadata schema 尚无已证明的原位标量字段演进路径；design 决策 16 仍待用户确认 shadow collection + SQL mapping/readiness，或另立依赖升级/schema-evolution contract。确认前 legacy vector/readiness 必须 fail closed；真实 vector audit/backfill 仍未获授权。
- 暂停前验证：`mvn -q -pl rag-admin -am -DskipTests test` 退出码 0；上述 QA history/controller/authorization 25 tests 通过；`git diff --check` 退出码 0；worktree 共有 74 个 modified/untracked entries，受保护路径 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 命中 0。分支仍为 `main...origin/main [ahead 7]`。
- 跳过项：因用户要求暂停，未运行 ledger 最终行为测试、KB list RED、完整 SQL/API/permission、完整 Redis/cache、Milvus contract/Testcontainers、`mvn -q test`、Python 全量、前端 build、SensitiveLogs/credential/ThreadLocal/裸 mapper 全套收口门禁。真实 Redis 不可用时既有 property tests 按原规则跳过；前端无改动。所有跳过项均不得视为通过。
- 外调与范围安全：真实 Milvus/Qdrant/Elasticsearch maintenance、embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0；未修改受保护本地配置、accepted baseline、V1-V10 或依赖，未暂存、提交、push、创建 PR、部署或发布。
- 剩余风险：当前是可编译的中途检查点，不是完整回归检查点；KB list、update/delete/statistics、document、permission write、feedback、QA/embedding/task cache、task projection/finalizer/recovery/input、RAG/keyword/Milvus contract 和 legacy readiness 均未收口。C14 通过前仍不得开放第二业务 tenant、MCP/Router 或宣称租户隔离成立。
- Commit：`pending`；提交责任为用户手动提交。本次暂停不建议把 74 个条目压成单一“C13b 完成”提交；明日聚焦 GREEN 后再按 migration、SQL/API、task/cache、RAG/vector 分组给出中文提交建议。

## 2026-07-27｜C13b SQL/API/permission 与 task/recovery/input/projection 续作

- 恢复与范围：从 2026-07-26 暂停检查点恢复，提交责任保持 `用户手动提交`；未暂存、未提交、未 push、未创建 PR、未部署。先补跑 ledger 检查点，再按 TDD 完成 KB/document list/update/delete/statistics/upload、permission、history/feedback、SQL finalizer、恢复 scope、持久化输入与 task projection 小切片。
- SQL/API/permission：controller 用户路径统一传播 `RequestIdentity`；KB owner/public/permission、document lookup/list/delete/count、permission target user、history/feedback save/read/list/delete/duplicate 均增加 tenant predicate 与 mapper-returned mismatch 防御。QA query count 使用 `kb:query:count:v2:{tenantId}:{kbId}`，source/citation title enrich 使用相同 tenant identity；跨 tenant 资源以 scoped lookup 隐藏。
- Task/recovery/finalize：ledger create/find/claim/heartbeat/phase/retry/finalize 全部携带 tenantId；recovery 从 durable record 构造 execution scope并核对 task/document/KB/owner；`IndexTaskSqlFinalizer` 同事务锁定 tenant-scoped task/document，chunk insert 与 KB document count 更新均带 tenant predicate。
- Durable input：`IndexInputStore` 新增 tenant-aware put/open/delete，文件键固定为 `objects/v2/{tenantId}/{uuid}.bin`；旧无 tenant 入口和旧 `objects/{uuid}` key fail closed，不双读。跨 tenant open/delete、路径穿越、符号链接、完整性校验与补偿清理均有测试；cleanup coordinator 从 document durable tenant 执行。
- Task projection/message：`TaskStatus`、Redis payload、durable fallback 与 `DocumentIndexMessage` 增加 tenantId；Redis key 使用 `task:status:v2:{tenantId}:{taskId}`，payload tenant/task mismatch fail closed，内存 future map 同样按 v2 key 分桶。TaskController 的 status/result/cancel/exists/completed 全部从 `RequestIdentity` 使用 tenant-scoped manager，旧 manager/status-service 入口 fail closed。
- Keyword SQL bootstrap：system-wide KB 启动扫描后，每个 KB 必须有合法 tenant；document/chunk 通过 tenant-scoped service 加载并再次核对 tenant，内部 metadata 写入 tenantId。当前只证明 SQL bootstrap scope；`KeywordIndex` 内存 map 仍以 collection name 分桶，属于后续 RAG/keyword contract gap。
- 聚焦验证：task/recovery/input/projection 组合命令共 68 tests / 0 failures / 0 errors / 0 skipped；其中 ledger 8、reconciliation 6、SQL finalizer 2、input store 12、indexing 16、task controller 3（随后扩展为 5）、task Redis/key/failure 等均 GREEN。QA controller、KnowledgeBase service、keyword bootstrap 与 telemetry 聚焦回归均退出码 0。ThreadLocal/SecurityContext 扫描在 task/recovery/input main source 无命中；旧 task key main-source 使用只剩 deprecated 常量声明，无运行时调用。
- 全仓验证：两次 `mvn -q test` 均在 `rag-admin` 报 207 tests / 1 failure / 0 errors / 21 skipped，唯一失败为既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` 的不可用 OTLP collector 时序断言；该用例单独复跑退出码 0。因完整命令仍非零，不记录为全仓通过。`C5RecoveryMySqlTest` 在本环境 Docker 不可用时 5/5 skipped，不能作为真实 MySQL 恢复通过证据。
- 静态与范围：`git diff --check` 退出码 0；当前 worktree 115 个 modified/untracked entries，均为 C13b 累积实现/测试/治理文件，受保护 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 命中 0。V1-V10、accepted baseline、依赖与前端未修改；CRLF 提示和用户级 git ignore permission warning 不影响 diff check 结果。
- 跳过与外调：Python 全量、frontend build、真实 Redis/Testcontainers Milvus contract、legacy vector maintenance/readiness 与真实 provider 调用尚未执行。真实 Milvus/Qdrant/Elasticsearch maintenance、embedding/rerank/debug retrieval/ask/generation/judge/LLM 调用、业务数据出站、费用与限流事件均为 0。
- 剩余风险：QA/embedding cache v2、RAG immutable scope 贯通、KeywordIndex tenant map、Milvus 新 collection adapter contract 与 get/delete/count/drop 仍未完成；部分 legacy service 裸入口仍存在但用户 controller 主路径已 scoped，不能勾选全量裸 mapper 门禁。Design 决策 16 未获确认，legacy collection 必须保持非 READY；C13b/C14 均未完成，不能开放第二业务 tenant、MCP/Router 或宣称租户隔离成立。
- Commit：`pending`；提交责任为用户手动提交。建议按阶段拆分：`feat(租户): 收紧SQL权限与问答数据路径`、`feat(任务): 增加租户化恢复输入与状态投影`。

## 2026-07-27｜C13b V11 migration 提交补录

- Commit：`f0bde8a`（`feat(租户): 增加V11数据面租户迁移门禁`）。本条只补录上一执行提交的真实 hash，不记录后续 C13b 实现改动。

## 2026-07-27｜C13b Redis identity 与 query scope 提交补录

- Commit：`59944fe`（`feat(租户): 建立Redis身份与查询范围基础`）。本条只补录上一执行提交的真实 hash，不记录后续 C13b 实现改动。

## 2026-07-27｜C13b SQL/API 与异步数据面提交补录

- Commit：`255c72f`（`feat(租户): 收紧SQL权限与异步任务数据面`）。本条只补录上一执行提交的真实 hash，不记录后续 C13b 实现改动。

## 2026-07-27｜C13b 分段提交与 Docker 恢复验证检查点

- 用户授权与提交范围：用户明确授权 Agent 将当前 C13b 阶段实现分段提交。已创建 `f0bde8a`（V11 migration/entity）、`59944fe`（Redis identity/query scope 基础）、`255c72f`（SQL/API/permission 与 task/recovery/input 数据面）三段实现提交；未 push、未创建 PR、未部署。
- Docker/MySQL 证据：Docker Desktop `28.4.0` 可用；执行 `mvn -q -pl rag-admin -am '-Dtest=TenantDataPlaneMigrationMySqlTest,C5RecoveryMySqlTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`，退出码 0。MySQL `8.0.36` 下 migration 为 8 tests / 0 failures / 0 errors / 0 skipped，C5 recovery 为 5/0/0/0；此前 Docker 不可用导致的 5 个 recovery skips 已消除。
- 编译与静态门禁：`mvn -q -pl rag-admin -am -DskipTests test` 在标准本机 Maven 缓存环境退出码 0；首次 sandbox 隔离环境因无法访问本机 Maven 缓存/远端而失败，不属于代码失败。SensitiveLogs 扫描 323 source files / PASS；`git diff --check` 通过；受保护路径、V1-V10、accepted baseline、依赖与前端改动均为 0。
- 已知未通过项：此前两次全仓 `mvn -q test` 都只有既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` 的 collector 时序断言失败，rag-admin 207 tests / 1 failure / 0 errors / 21 skipped；该测试独立复跑通过，因此仍不把全仓门禁记为 GREEN。
- 跳过与外调：本轮未运行 Python 全量（无 evaluation/Python 改动），前端 build 因无前端改动记为 `SKIPPED`；Milvus tenant adapter contract、legacy readiness/maintenance、QA/embedding cache v2 与完整 RAG/keyword scope 尚未实现。真实 vector maintenance、embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0。
- 剩余风险与决策：design 决策 16 仍需用户在 shadow collection + SQL mapping/readiness 切换与另立依赖升级/schema-evolution contract 之间确认；确认前 legacy collection/runtime/maintenance 保持非 READY/fail closed。Qdrant/Elasticsearch 继续只具备 enforcement-mode startup guard，C14 前不开放第二业务 tenant、MCP/Router，也不宣称租户隔离成立。
- Commit：`pending`；本条与 ACTIVE_TASK/tasks 检查点建议独立提交 `docs(openspec): 记录C13b阶段实现检查点`。

## 2026-07-27｜C13b 阶段检查点提交补录

- Commit：`f0a4ffd`（`docs(openspec): 记录C13b阶段实现检查点`）。本条只补录上一治理提交的真实 hash，不记录后续 C13b 实现改动。

## 2026-07-27｜C13b 决策 16=A 与执行边界确认

- 用户确认：D16 选择方案 A，采用 tenant-aware shadow collection；维护实现复用既有 vector/content，完成 expected/observed/migrated/missing/mismatch 全量审计后，才允许原子切换 SQL active collection mapping/readiness。既有 source collection 不原位改 schema、不删除，部分失败保持原 mapping 与非 READY。
- 授权边界：真实 Milvus 当前只授权只读盘点；任何真实 collection 创建、vector 复制、mapping/readiness 切换、重试或清理均未授权，必须另行披露范围与风险并取得授权。合成 unit/Testcontainers 可用于实现验证，真实 maintenance write/switch 继续 `SKIPPED`。
- 范围决策：既有 `GenAiTracingConfigurationTest` 的 OTel collector 时序波动不扩入 C13b；若完整门禁再次仅命中该项，记录全仓非 GREEN 与独立复跑事实，必要时另立维护任务，不在 tenant change 内顺手修改观测实现。
- 实现顺序：先完成 QA/embedding cache v2，再贯通 RAG/Keyword tenant scope、Milvus adapter contract 与 shadow collection/readiness，最后执行完整门禁。遵循纵向 TDD，每个行为先 RED 后最小 GREEN。
- 外调与安全：本条仅修改 design/tasks/append-only AGENT_LOG；真实 Milvus 写入、provider、embedding、rerank、ask/generation/judge/LLM 调用、数据出站、push、PR、部署均为 0。
- Commit：`pending`；提交责任为 Agent，建议 `docs(openspec): 确认C13b影子集合迁移决策`。

## 2026-07-27｜C13b 影子集合迁移决策提交补录

- Commit：`dc20355`（`docs(openspec): 确认C13b影子集合迁移决策`）。本条只补录上一治理提交的真实 hash，不记录后续 C13b 实现改动。

## 2026-07-27｜C13b tenant data-plane enforcement 实现提交补录

- Commit：`fbe8e17`（`feat(rag): 强制租户数据面与影子向量就绪`）。本条只补录上一执行提交的真实 hash，不记录后续 duplicate/partial fixture 补测。

## 2026-07-27｜C13b shadow 异常夹具提交补录

- Commit：`9f0dc68`（`test(rag): 补全影子迁移异常夹具`）。本条只补录上一执行提交的真实 hash，不记录本次治理收口改动。

## 2026-07-27｜C13b data-plane enforcement 实现与指定测试收口

- 范围与修改：QA cache 使用 `qa:cache:v2:{tenantId}:{kbId}:queryHash:optionHash` 并校验 payload scope，embedding cache 使用 tenant/effective provider/model/content hash v2 namespace；两者只允许 tenant-local evict/clear，Redis 异常仅降级 miss/重算。`QARequest`、`RetrieveOptions`、`RAGService`、`QueryEngine`、`KeywordIndex` 贯通 immutable `TenantVectorScope`，keyword-only fallback 保持相同 scope；无 tenant 的 KB/document service lookup 兼容签名改为 fail closed。
- Milvus contract：新 schema 增加独立 Int64 `tenant_id/kb_id`；upsert 强制服务端 marker 并拒绝冲突，search 将 scope 与普通 metadata filter 做 AND，get/getByIds/delete/count/drop 都使用 tenant scope。跨 tenant ID 或 marker mismatch 抛稳定 scope error，canonical drop 前以强一致 foreign-row count 防止删除含其他 scope 的集合。runtime `VectorStore` 已移除无租户 get/getByIds surface，legacy 读取仅由独立只读 `LegacyVectorSourceReader` 提供。
- Shadow/readiness：V12 增加 KB readiness、source/shadow mapping 与 expected/observed/migrated/missing/mismatch/error 审计字段。新 KB 使用 canonical tenant-aware collection 且仅在创建成功后 READY；问答、索引、恢复、删除、统计与 keyword bootstrap 均要求 tenant-scoped READY。默认关闭、非 REST 的 maintenance 从 tenant-scoped SQL 读取 vector identity，只读 source vector/content/metadata 与 schema dimension，复制到 shadow 后全量回读审计，再以单条条件 SQL 切换 mapping/READY；source 不修改不删除，失败保持原 mapping 与非 READY。
- TDD：QA/embedding payload mismatch、RAG/keyword scope、Milvus marker/predicate、runtime readiness、shadow missing/mismatch/duplicate/partial/empty/success 均先取得预期 RED 再转 GREEN。最后补测发现 duplicate SQL 行的 expected 应按唯一 vector ID 计数，已在 `9f0dc68` 修正并由 `VectorShadowMigrationServiceTest` 6/0/0/0 证明。
- 聚焦与容器验证：core/admin cache-query-readiness 组合命令退出码 0；最终 scoped-only vector/service/shadow 聚焦 suites 退出码 0。`mvn -q -pl rag-admin -Pc4d-milvus-fault failsafe:integration-test failsafe:verify` 使用 Docker Desktop 28.4.0、Milvus `2.3.4`、etcd `3.5.5`、MinIO `RELEASE.2023-03-20T20-16-18Z`，Tests 2 / Failures 0 / Errors 0 / Skipped 0；覆盖同物理集合双 scope create/has/upsert/search/get/getByIds/delete/count/drop、marker mismatch、真实容器 stop/start、keyword-only degradation 与恢复，全部数据为合成数据，真实模型调用 0。
- 全仓 Java：最终 `mvn -q test` 退出码 1；`rag-admin` 为 214 tests / 1 failure / 0 errors / 2 skipped，唯一失败是已确认越界的 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` collector 时序断言。该用例随后独立复跑退出码 0；因此只记录“其他 C13b suites 未见回归 + OTel 独立通过”，不把全仓门禁写成 GREEN。该波动不在本 change 修改，必要时另立维护任务。
- 其他门禁：`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 159 tests / OK，evaluation contract 未修改；SensitiveLogs 扫描 326 source files / PASS；7 个 changed Markdown 本地相对链接 missing=0；protected paths=0、runtime unscoped vector reads=0、bare tenant-bypass mapper hits=0、legacy QA/embedding cache runtime uses=0；task/recovery main source 无 `SecurityContextHolder` 或 tenant ThreadLocal（仅通用 trace ID 使用 `ThreadLocalRandom`）；`git diff --check` 通过。前端无改动，正式 build `SKIPPED`。
- 真实环境与外调：没有连接或盘点真实 Milvus，也没有创建 collection、复制 vector、切换 mapping/readiness、重试或清理；真实 maintenance write/switch 明确 `SKIPPED`，mock/unit/Testcontainers 不代表现有数据已迁移。真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0。
- 范围安全：未修改 V1-V10、accepted baseline、依赖、前端、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未 push、未创建 PR、未部署。OTel 时序波动没有混入 C13b 修复。
- 剩余风险：真实 Milvus collection/record 数、容量、超时/重试、shadow 额外空间和回滚窗口仍未知；执行任何真实 shadow copy/switch 前必须重新披露并取得授权。Qdrant/Elasticsearch 尚未通过同等 tenant adapter contract，只能在 enforcement mode 下 fail startup。当前只可表述“C13b data-plane enforcement 已实现并通过指定测试”；C14 通过前不得宣称租户隔离成立，也不得开放第二业务 tenant、C15 MCP 或 C16 Router。
- Commit：`pending`；提交责任为 Agent，建议 `docs(openspec): 收口C13b实现与验证证据`。用户验收前不接受 delta、不归档 change、不将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。

## 2026-07-27｜C13b 实现与验证收口提交补录

- Commit：`2f18827`（`docs(openspec): 收口C13b实现与验证证据`）。本条只补录上一治理提交的真实 hash；本补录提交不递归记录自身 hash。

## 2026-07-27｜C13b 验收与归档

- 用户验收与提交责任：用户明确确认 C13b 验收通过并要求归档；沿用已授权的 `Agent 提交`，只提交计划内归档与长期事实源文件，不 push、不创建 PR、不部署。
- 规格收口：`tenant-data-plane-enforcement` delta body 已按行原文接受为 `openspec/specs/rag-system/spec.md` 的 exact suffix，共 6 requirements / 18 scenarios；proposal、design、tasks 与 delta 已移入 `openspec/changes/archive/2026-07-27-tenant-data-plane-enforcement/`，archive files=4、tasks unchecked=0、未归档 active change=0，`.ai/ACTIVE_TASK.md=IDLE`。
- 长期事实源：同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`，记录 C13b data-plane enforcement 已验收归档及指定测试边界；真实 Milvus shadow copy/mapping/readiness switch 继续 `SKIPPED`，Qdrant/Elasticsearch 继续在 enforcement mode 下 fail startup。
- 本轮验证：baseline exact line suffix=`true`；6 requirements / 18 scenarios；archive files=4；tasks unchecked=0；未归档 active change=0；`.ai/ACTIVE_TASK.md=IDLE`。Python 全量 `159 tests / OK`；SensitiveLogs 扫描 326 source files / PASS；11 个现存 changed Markdown 的本地相对链接 missing=0；protected paths=0、unexpected paths=0、Java/migration/frontend changes=0；当前事实源的 C13b 待验收残留=0；`git diff --check` 通过。OpenSpec CLI 不在 PATH，未声称 CLI validation 通过。
- 复用与跳过：本轮只做规格、文档、任务状态与 change 位置归档，未修改 Java/POM/前端/runtime config/migration，因此不重跑 Maven、frontend build、Docker/Testcontainers 或 C3。实现收口已记录 Milvus 2.3.4 合成 contract 2/0/0/0 与聚焦 suites 通过；此前全仓 `mvn -q test` 仍因唯一既有 OTel collector 时序断言保持非 GREEN，该用例独立复跑通过，本轮不改写该事实。
- 外调与范围安全：没有连接、盘点或写入真实 Milvus，没有创建 collection、复制 vector、切换 mapping/readiness、重试或清理；真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0。未触碰 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`，未 push、未创建 PR、未部署。
- 剩余风险与下一阶段：真实 Milvus collection/record 数、容量、超时/重试、shadow 额外空间和回滚窗口仍未知，任何真实写入或切换必须另行授权；OTel collector 时序波动保留为独立维护债务。下一项重大变更必须另立 C14 隔离与恶意样本评测 change；C14 通过前不得宣称租户隔离成立，也不得开放第二业务 tenant、tenant management、C15 MCP 或 C16 Router。
- Commit：`pending`；建议 `chore(openspec): 验收并归档C13b数据面约束`。

## 2026-07-27｜C13b 验收归档提交补录

- Commit：`243860a`（`chore(openspec): 验收并归档C13b数据面约束`）。本条只补录上一归档提交的真实 hash；本纯日志补录提交不递归记录自身 hash。

## 2026-07-27｜C14 tenant isolation adversarial evaluation readiness 及规划启动

- 用户决策与提交责任：用户要求检查项目状况，若允许则直接开始 C14 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动 HEAD=`d5e07b2`，分支 `main`，工作区与暂存区均干净，`.ai/ACTIVE_TASK.md=IDLE`；C13b archive files=4、tasks unchecked=0、未归档 active change=0，delta body 与 `rag-system` baseline exact suffix。C13b 的 6 requirements / 18 scenarios 已验收归档，长期 project/architecture/roadmap/optimization 均把 C14 定义为下一独立 Type C 门禁，因此结论为 `GO`。
- 能力分类：`confirmed` 为 C13a/C13b accepted contract、MySQL/Redis/Milvus Testcontainers、双 tenant fixtures、deterministic embedding、sync/SSE/task recovery 与 Python 标准库治理入口；`partial` 为现有跨 tenant tests 分散且无统一 adversarial release/report/timing evidence；`planned` 为 versioned corpus、dedicated Failsafe harness、functional/content/error/timing 四通道与受限 claim gate；`out_of_scope` 为 tenant management、真实 shadow migration、Qdrant/Elasticsearch tenant support、通用 prompt/LLM 安全、生产渗透测试、C15/C16；`unknown` 为真实生产拓扑、数据规模、网关 timing、组织攻击模型与合规要求。
- 规划产物：创建 `openspec/changes/tenant-isolation-adversarial-evaluation/`，包含 proposal、design、tasks、`evaluation` delta 与 `rag-system` delta；design 记录 16 条真实取舍。更新 `.ai/ACTIVE_TASK.md` 为 `ACTIVE / C14 规划待审`。
- 关键边界：规划采用独立 `tenant-isolation-adversarial-v1`、test-only 双 tenant、deterministic embedding/generation stub、HTTP/SSE + infrastructure/recovery 组合 driver、响应与后置状态双重 invariant、预注册本机 coarse timing profile。C14 PASS 只支持 Milvus 配置和固定 synthetic attack matrix 下的隔离 evidence，不自动开放第二业务 tenant/tenant management/C15/C16，也不构成生产级多租户、全 adapter、真实迁移或所有 side-channel 证明。
- 验证：五个必需 artifact 均存在；design 的 16 条决策均具备“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”三行；`evaluation` delta 为 5 requirements / 15 scenarios，`rag-system` delta 为 2 requirements / 6 scenarios；未归档 active change=1 且 ACTIVE_TASK 唯一指向 C14。Markdown relative links=`OK`；accepted baseline、protected paths、Java、Python、POM、frontend 改动均为 0；`git diff --check` 通过。
- 跳过项及原因：本轮只做规划，未修改 Java/Python runner、POM、migration、frontend 或 runtime config，因此 Maven、Python、frontend build、Docker/Testcontainers 与 C14 evaluation 均 `SKIPPED`；用户批准规划前不得实现。
- 外调与范围安全：真实 Milvus maintenance、embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0。未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、accepted baseline、Java、migration、POM、前端或历史报告。
- 剩余风险与下一闸门：用户需审阅并批准 v1 case matrix、16 条 decisions、双 spec delta、timing profile 与 gap 最小修复边界；批准前不进入实现。C14 完整 PASS 前仍不得宣称租户隔离成立或开放第二业务 tenant、tenant management、C15/C16。
- Commit：`pending`；建议 `docs(openspec): 启动C14租户隔离对抗评测规划`。

## 2026-07-27｜C14 规划提交补录

- Commit：`bb9d0a4`（`docs(openspec): 启动C14租户隔离对抗评测规划`）。本条只补录上一规划提交的真实 hash，不记录后续 C14 实现改动。

## 2026-07-27｜C14 实现授权与 TDD 启动

- 用户授权与提交责任：用户明确验收 C14 规划并要求开始实现；proposal、16 条 design decisions、tasks 与双 spec delta 的事前闸门通过。提交责任继续为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 启动状态：HEAD=`bb9d0a4`，分支 `main...origin/main [ahead 1]`，工作区与暂存区干净；active change 唯一指向 `tenant-isolation-adversarial-evaluation`。
- 实现方法：使用 `tdd` skill，按 public behavior 的纵向 RED→GREEN 小切片推进；先完成 versioned adversarial release contract，再进入隔离 harness 和攻击矩阵，不先横向铺完全部测试。
- 授权边界：只允许合成 Testcontainers 与 deterministic test stub；真实 embedding/rerank/ask/generation/judge/LLM/provider 调用和真实 Milvus maintenance 继续禁止，业务数据出站为 0。
- Commit：`pending`。

## 2026-07-27｜C14 release contract、evaluator 与首个容器对抗切片

- 范围与修改：新增 `tenant-isolation-case-v1` schema、26 条固定 JSONL attack cases、manifest、标准库 validator/`--plan-only`、四通道 evaluator/no-overwrite report；新增 `c14-isolation-eval` Failsafe profile、deterministic generation stub、真实双 tenant HTTP/Testcontainers 对抗 IT 与 profile contract test。修正既有 `RedisFailureSemanticsIT` 的 test-only probe，使其使用 C13b 已要求的 `embed(TENANT_ID, text)`，未修改生产语义。
- TDD 证据：contract 从 module missing、artifact drift、duplicate/unknown driver、schema drift、unsafe path、missing control、quota/count/order drift 等 RED 逐项转 GREEN，最终 13 tests / OK；evaluator 从 module missing、missing/duplicate/unexpected、case ERROR/required SKIP、channel incomplete、timing incomplete、identity drift 与 overwrite 等 RED 转 GREEN，最终 11 tests / OK。`--plan-only` 验证 release=`tenant-isolation-adversarial-v1`、caseCount=26、12 categories、providerCallCount=0、businessDataOutbound=false、executionStarted=false。
- 容器 RED/GREEN：首次 C14 IT 因 etcd/MinIO 等待端口未声明而在业务执行前失败，补充 test-only exposed ports 后同命令通过。完整 `mvn -q -pl rag-admin -am -Pc14-isolation-eval '-Dtest=NoSuchC14UnitTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dfailsafe.failIfNoSpecifiedTests=false' verify` 最终退出码 0，5 tests / 0 failures / 0 errors / 0 skipped；包括 C14 双 tenant 1、Milvus tenant-scope 2、Redis failure/recovery 2。Docker Desktop 28.4.0，MySQL 8.0.36、固定 digest Redis、etcd 3.5.5、MinIO 固定 release、Milvus 2.3.4 均为本轮自有合成容器。
- 当前对抗证据：HTTP 切片验证 header/query/body/cookie/metadata tenant selector 不覆盖服务端 identity；tenant B public KB 对 A 的 list/detail/statistics/document-list 不可见；foreign/nonexistent 的 status/error/schema fingerprint 一致且不含 B canary/tenant/user identity；A 对 B update/delete 后 SQL tenant/name/description/public/deleted/version 不变；10 warmup + 40 fixed-seed interleaved pairs 的 median/P95 coarse timing gate 通过。测试内 deterministic embedding/generation invocation 均为 0。
- 相邻基础设施证据：既有 Redis IT 首次独立复跑因无 scope probe 得到 500，修正 test-only 调用后 2/0/0/0；既有 Milvus IT 独立复跑 2/0/0/0，覆盖同物理 collection tenant-scope search/get/getByIds/delete/count/drop、marker mismatch、foreign destructive rejection、真实容器 stop/start 与 keyword-only fallback。
- 全量门禁：`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 176 tests / OK；随后新增 evaluator/contract cases 的聚焦结果分别为 11/0/0 与 13/0/0。`mvn -q test` 退出码 1，rag-admin 217 tests / 1 failure / 0 errors / 2 skipped，唯一失败仍为既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` collector 时序断言；该用例本轮独立复跑退出码 0，因此不把全仓门禁记为 GREEN，也不在 C14 修改观测实现。SensitiveLogs 扫描 328 source files / PASS，新增 artifact 未发现凭据、用户绝对路径或私钥模式，`git diff --check` 通过。
- 跳过与剩余实现：前端无改动，正式 build `SKIPPED`。当前尚未形成 26/26 case-level 正式 evidence，task/history/feedback、reserved filter、cache/idempotency、sync/SSE、durable input 与统一 driver→details/report 映射仍待实现；因此 C14 继续 `ACTIVE`，不接受 delta、不归档、不宣称 C14 PASS。
- 外调与范围安全：真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用、业务数据出站、费用和限流事件均为 0；真实 Milvus maintenance、collection copy、mapping/readiness switch、重试与清理均 `SKIPPED`。未修改 migration、API/DTO、生产 schema/权限语义、依赖、前端、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或 accepted baseline；未暂存、未提交、未 push、未创建 PR、未部署。
- Commit：`pending`；提交责任保持用户手动提交。当前切片建议 `test(隔离): 建立C14对抗评测契约与容器门禁`。

## 2026-07-27｜C14 evaluator 边界补测

- 在上述检查点后继续补齐 evaluator 的 unexpected case、case ERROR、required SKIP、error channel incomplete 与 evidence identity drift，以及 release case count/order drift 测试；最终 contract 聚焦 13 tests / OK、evaluator 聚焦 11 tests / OK。
- 再次运行 Python 全量为 183 tests / OK；`git diff --check` 继续通过。没有新增外调、真实 maintenance、暂存、提交、push、PR 或部署。
- Commit：`pending`。

## 2026-07-27｜C14 实现检查点提交授权与归档审计

- 用户授权：用户明确确认当前交付验收通过，并授权 Agent 提交相关 commit、检查归档条件；本授权不包含 push、PR、部署、真实 provider 调用或真实 Milvus maintenance。
- 提交范围：只包含当前工作区内 C14 release/schema/validator/evaluator、Python tests、C14 Maven profile/Testcontainers tests、Redis test-only tenant scope 修正及对应 ACTIVE_TASK/tasks/append-only AGENT_LOG；未发现用户无关改动。
- 归档审计：`tasks.md` 仍有 26 个 unchecked items；尚未形成 26/26 case-level 正式 evidence，task/history/feedback、reserved filter、cache/idempotency、sync/SSE、durable input 与 requirement→case/test/evidence 映射未闭环；全仓 Maven 仍因既有 OTel collector 时序波动保持非 GREEN。因此归档结论为 `NO-GO`，不得接受双 delta、移动 change 或把 ACTIVE_TASK 置为 IDLE。
- 状态决策：本轮只提交可验证实现检查点；change 继续 `ACTIVE / C14 实现中`。后续完成剩余 attack matrix 和正式 evidence 后，再重新执行归档审计。
- Commit：`pending`。

## 2026-07-27｜C14 隔离对抗评测实现检查点提交补录

- Commit：`3965c60`（`feat(评测): 建立C14隔离对抗评测实现检查点`）。本条只补录上一执行提交的真实 hash；归档审计仍为 `NO-GO`，change 保持 ACTIVE。

## 2026-07-27｜C14 证据映射、完整双租户驱动与最小 gap 修复

- 范围与修改：新增版本化 `tenant-isolation-evidence-map-v1`，把 26 条冻结 case 映射到具体 Surefire/Failsafe testcase selector；release validator 校验 map hash、identity、exact case set、有限 report type、安全 class/test prefix 与 timing evidence key。新增纯标准库 evidence assembler，从 JUnit XML 和受限 driver JSON 逐 selector 组装证据，missing/ambiguous test、driver/Git HEAD 漂移、外调边界或 overwrite 均 fail closed。
- 双租户驱动：`C14IsolationAdversarialIT` 现在建立 A owner/A reader/B owner、同租户 READ permission、A/B KB/document/index task 与 B history；真实经过 KB/detail/list/update/delete/statistics、document list/delete/upload、task status/result/cancel/exists/completed、reserved filters、sync ask、SSE、history/feedback，以及 owner/reader 两组 10 warmup + 40 fixed-seed interleaved timing。SQL document/task/history 与 foreign KB 后置快照保持不变；响应与 matched nonexistent control 比较稳定 status/error/schema，禁止 B canary/tenant/user identity。
- TDD gap 1：真实索引先因 `MilvusVectorStore.withServerScope` 对允许 null 的 metadata 使用 `Map.copyOf` 抛 NPE；新增 `scopedUpsertShouldIgnoreNullMetadataValuesBeforeAddingServerScope` 先稳定 RED，再只过滤 null key/value 后添加服务端 tenant/kb marker，`MilvusVectorStoreFailureSemanticsTest` 全类通过。
- TDD gap 2：foreign/nonexistent task 指纹均为 `400/TASK_001`，与冻结 release 的 `404/TASK_NOT_FOUND` 不符；先收紧 `TaskControllerTest` 取得 RED，再让两个 not-found 分支共用 `HttpStatus.NOT_FOUND`，owner mismatch 的 `AUTH_004` 不变，TaskController 5/0/0/0。
- 真实容器迭代：首次扩展 IT 因上述 Milvus NPE失败；修复后因索引前 KB version 快照误报，移动为正常 fixture 建成后的攻击前快照；随后发现 task 400 gap 并修复；SSE control 又暴露共享 USER rate-limit 测试干扰，改用同租户 reader + READ permission，不关闭生产限流；负向 SSE 增加 text/event-stream/application-json 双 Accept 以确保请求进入鉴权。最终 `C14IsolationAdversarialIT` 为 1/0/0/0，Docker Desktop 28.4.0、自有 MySQL 8.0.36、固定 digest Redis、etcd 3.5.5、固定 MinIO 与 Milvus 2.3.4 全部运行。
- 聚焦验证：证据映射 contract 15 tests / OK，assembler 4 tests / OK；mapped Surefire 组合退出码 0，覆盖 permission、reserved filter、QA/embedding cache、idempotency、task ledger/finalizer、history/feedback、TaskController 与 Milvus null metadata 相邻回归。Python 全量 189 tests / OK；`git diff --check` 通过。
- 外调与范围安全：driver 只记录 bounded timing、镜像、health、Git HEAD 与零外调边界；真实 provider/model calls=0、业务数据出站=false、真实 Milvus maintenance=`SKIPPED`。未修改 migration、DTO、依赖、前端、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或 accepted baseline；未 push、未创建 PR、未部署。
- 剩余项：需在干净提交 HEAD 上重跑 mapped Surefire + C14 完整 Failsafe，生成不可覆盖的 26/26 evidence/details/summary，再跑全量/静态门禁并同步 closeout 文档。前端无改动，正式 build待记录 `SKIPPED`。
- Commit：`pending`；提交责任为 Agent，建议 `feat(隔离): 补全C14双租户证据驱动与错误边界`。

## 2026-07-27｜C14 双租户证据驱动提交补录与参数化映射修正

- Commit 补录：`612ecbe2e8082c6507ce432e01854561d5d8d7fc`（`feat(隔离): 补全C14双租户证据驱动与错误边界`）。本条只补录上一执行提交的真实 hash。
- 干净 HEAD 验证：在 `612ecbe2e8082c6507ce432e01854561d5d8d7fc` 上重跑 mapped Surefire 组合退出码 0；完整 `c14-isolation-eval` Failsafe profile 退出码 0、约 131 秒，合成 Testcontainers 正常完成，provider/model calls=0、businessDataOutbound=false、realMaintenanceStatus=`SKIPPED`。
- 组装器严格检查发现 `ReservedScopeFilterTest` 的两个 selector 分别对应 9 组与 3 组参数化调用，原先“每 selector 必须恰好一条”会错误报 `mapped_test_ambiguous`。按 TDD 先新增参数化聚合 RED，再引入受限 `expectedMatches`（1..100）并要求命中数完全一致、任一 ERROR/FAIL/SKIPPED 向 case 聚合；映射明确为 9/9 与 3/3，不放宽 missing/extra 防线。
- 聚焦验证：assembler 5 tests / OK；release contract 15 tests / OK。manifest 已同步 evidence map 新 bytes/hash；正式 26/26 evidence 尚未生成，需在本修正提交后的干净 HEAD 重跑容器评测并绑定新 Git HEAD。
- 范围安全：仅修改 C14 evidence map/manifest、标准库 validator/assembler tests 与本追加日志；未修改生产 Java、migration、依赖、前端、accepted baseline 或受保护路径，未外调、未 push、未创建 PR、未部署。
- Commit：`pending`；建议 `fix(评测): 严格聚合参数化隔离证据`。

## 2026-07-27｜C14 参数化证据修正提交补录与正式 evidence 收口

- Commit 补录：`dc9e3e6`（`fix(评测): 严格聚合参数化隔离证据`）。本条只补录上一执行提交的真实 hash。
- 正式执行：在干净 Git HEAD `dc9e3e6ed1434989a646b36389d6d9eeeea4ea83` 上，mapped Surefire 71 tests / 0 failures / 0 errors / 0 skipped；`c14-isolation-eval` Failsafe 5/0/0/0，约 129 秒。assembler 严格读取 mapped JUnit 与 driver JSON，生成 26 条 case evidence；evaluator 退出码 0，`Report status=PASS`，expected/observed=26/26，missing/unexpected/failed/errors/skipped 全为 0，functional/content/error/timing 四通道均为 `PASS`。
- 证据输出：新增 `docs/eval/reports/c14-tenant-isolation-evidence-v1.json`、`c14-tenant-isolation-details-v1.json`、`c14-tenant-isolation-report-v1.md`，均采用 no-overwrite；新增 `docs/eval/isolation/tenant-isolation-adversarial-v1-traceability.md`，逐 requirement/scenario 映射 release/case/test/evidence。
- 相邻边界：claim/session、global token blacklist/IP rate-limit、durable input 跨租户/open/delete/cleanup/traversal/symlink、Qdrant/Elasticsearch enforcement-mode fail-startup 聚焦 58/0/0/0。它们用于确认边界，不冒充 26-case tenant business evidence。
- 全量门禁：Python 190 tests / OK。全仓 `mvn -q test` 仍在 `rag-admin` 217 tests 中只有既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` 1 failure / 0 errors / 2 skipped；该 OTel collector 时序用例独立复跑退出码 0，因此如实保持全仓非 GREEN，不扩入 C14。
- 静态门禁：SensitiveLogs 扫描 329 source files / PASS；release `--plan-only` 为 `VALID`、26 cases、12 categories、executionStarted=false；protected paths=0、frontend changes=0；正式报告 raw token/body/content/canary/credential/用户绝对路径扫描 0 命中。Markdown links 与 `git diff --check` 在最终文档同步后再次执行。
- 状态与边界：tasks 除“用户验收后接受双 delta 并归档”外均闭环；ACTIVE_TASK 标为“实现与证据闭环完成，待用户最终验收/归档”。同步 project、architecture、roadmap、optimization 与 eval guide，结论限定为 Milvus 支持配置和固定 synthetic matrix；不开放生产第二业务 tenant、tenant management、C15/C16，不声称生产级多租户、全 adapter、真实迁移、渗透测试或所有 timing side-channel 已验证。
- 外调与跳过：provider/model calls=0、businessDataOutbound=false、真实 Milvus maintenance=`SKIPPED`；前端/DTO 无改动，正式 build=`SKIPPED`。未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`，未 push、未创建 PR、未部署。
- Commit：`pending`；建议 `docs(评测): 收口C14正式隔离证据`。

## 2026-07-27｜C14 最终静态门禁补充

- 最终 changed Markdown 10 files、relative link missing=0；protected paths=0、frontend changes=0、report sensitive matches=0；details 为 `PASS`、cases=26/26、missing/failed/errors/skipped 全为 0；`git diff --check` 通过。
- `tasks.md` unchecked=1，唯一未完成项是“用户验收后接受双 delta、归档 change、ACTIVE_TASK 置为 IDLE”，符合归档前状态；本条随 C14 正式证据收口提交，不额外扩大范围。

## 2026-07-27｜C14 正式证据收口提交补录与验收归档

- Commit 补录：`e9e9700`（`docs(评测): 收口C14正式隔离证据`）。本条只补录上一执行提交的真实 hash。
- 用户验收与提交责任：用户明确要求归档 C14 并提交 commit；继续按 `Agent 提交` 执行，仅包含 baseline 接受、change 归档、ACTIVE_TASK/长期文档/日志同步，不 push、不创建 PR、不部署。
- Baseline 接受：将 C14 delta body 原文接受到 `openspec/specs/evaluation/spec.md` 与 `openspec/specs/rag-system/spec.md`；前者 exact suffix 134 lines、5 requirements / 15 scenarios，后者 exact suffix 53 lines、2 requirements / 6 scenarios，目标 requirement 无重复。
- 归档状态：change 移至 `openspec/changes/archive/2026-07-27-tenant-isolation-adversarial-evaluation/`，archive files=5、tasks unchecked=0、未归档 active change=0；`.ai/ACTIVE_TASK.md=IDLE`。同步 project、architecture、roadmap、optimization、eval guide 与 traceability，当前事实源的待验收/未归档残留=0。
- 正式证据复核：details 仍为 `PASS`，cases=26/26，missing/failed/errors/skipped 全为 0，provider calls=0、businessDataOutbound=false、realMaintenanceStatus=`SKIPPED`；归档不改写 evidence identity 或扩大结论。
- 静态验证：SensitiveLogs 329 source files / PASS；14 个 changed Markdown relative links missing=0；protected paths=0、code/dependency paths=0；`git diff --check` 通过。OpenSpec CLI 不在 PATH，未声称 CLI validation 通过。
- 跳过项：本轮只变更 OpenSpec baseline/archive 与治理文档，未修改 Java/Python/POM/前端/runtime config，因此不重跑 Maven、Python、Docker/Testcontainers 或 frontend build；复用归档前已提交的 C14 71/0/0/0 Surefire、5/0/0/0 Failsafe、58/0/0/0 相邻边界、Python 190 tests / OK 正式证据。全仓 Maven 的既有 OTel collector 时序债务保持原记录。
- 外调与范围安全：真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件均为 0；未连接或写入真实 Milvus，没有 collection copy、mapping/readiness switch、重试或清理。未触碰 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`，未修改代码、依赖、migration、DTO 或前端。
- 剩余风险：C14 只证明 Milvus 支持配置和固定 synthetic attack matrix；真实 shadow migration、生产拓扑/容量/合规/网关 timing、Qdrant/Elasticsearch 等价 evidence、生产第二业务 tenant/tenant management、C15/C16 仍需独立 Type C change 与授权。
- Commit：`pending`；建议 `chore(openspec): 验收并归档C14租户隔离评测`。

## 2026-07-28｜C14 验收归档提交补录

- Commit：`cab9939`（`chore(openspec): 验收并归档C14租户隔离评测`）。本条只补录上一归档提交的真实 hash，不记录 C15 规划。

## 2026-07-28｜C15 只读 MCP 服务 readiness 与规划启动

- 用户目标与提交责任：用户要求检查项目状态，若允许则直接进入 C15 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动 HEAD=`cab9939`，分支 `main...origin/main [ahead 7]`，工作区与暂存区均干净，`.ai/ACTIVE_TASK.md=IDLE`；C14 archive files=5、tasks unchecked=0、未归档 active change=0，正式 evidence 为 26/26 且 functional/content/error/timing 四通道均 `PASS`。长期 project/architecture/roadmap 均将 C15 定义为 C14 之后的独立 Type C change，因此结论为 `GO`。
- 能力分类：`confirmed` 为 C13/C14 已接受的服务端 JWT identity、tenant-scoped SQL/Redis/Milvus 数据面、RAG retrieval/generation/citation 与固定 C14 隔离证据；`partial` 为现有 `RAGService.ask` 可复用但 REST controller 还会写 query count/history、现有 rate limiter 不能直接区分单一 `/mcp` endpoint 下的 tool；`planned` 为认证的只读 MCP Resources/Tools、独立只读 facade、bounded result/error/conformance 证据；`out_of_scope` 为 C16 Router、写工具、文档历史版本、完整 MCP OAuth authorization server、stateful session/stdio/旧 SSE transport、框架升级与生产远程暴露；`unknown` 为官方 Java SDK 与当前 Boot 3.2.1/Jackson/Reactor/Servlet 组合的实测兼容性及真实客户端互操作差异。
- 规划产物：创建 `openspec/changes/mcp-readonly-service/`，包含 proposal、design、tasks 与 `rag-system` spec delta；design 记录 18 条真实取舍，delta 为 7 requirements / 26 scenarios。更新 `.ai/ACTIVE_TASK.md` 为 `ACTIVE / C15 规划待审`。
- 关键决策：目标协议固定 MCP `2025-11-25` 的 sessionless Streamable HTTP，单端点 `/mcp`、default-off、local-only；优先做官方 Java SDK core/Servlet + Jackson 2 兼容 spike，若与当前基线不兼容则 hard-stop 并另立 runtime foundation change，不在 C15 偷渡框架升级。首版沿用每请求现有 JWT/`RequestIdentity` 的部署型认证并明确不宣称完整 MCP OAuth profile；tenant 不接受 client selector。
- 只读与外调边界：Resources 只暴露 tenant-filtered KB/document/chunk；Tools 固定 `rag.search`、`rag.ask`、`rag.get-citation`、`rag.compare-sources`。`rag.ask` 通过 read-only facade 复用 RAG 能力，不写 QA history/query count；`rag.compare-sources` 只做确定性并排来源，不调用 LLM。search/ask 受独立 default-off 外调开关约束，cache/metrics/rate-limit 只允许 bounded 非权威技术写入。
- 规划验证：四个必需 artifact 均存在；design 的 18 条决策均具备“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”三行；delta 为 7 requirements / 26 scenarios，GIVEN/WHEN/THEN/AND 格式已扫描；ACTIVE_TASK 唯一指向 C15。OpenSpec CLI 不在 PATH，未声称 CLI validation 通过；最终 Markdown 链接、受保护路径、unexpected paths 与 `git diff --check` 结果在本条后续静态门禁中复核。
- 跳过项及原因：本轮只做 Type C 事前规划，未修改 Java/POM/config/migration/frontend，因此 Maven、Python、frontend build、Docker/Testcontainers、MCP conformance/client smoke 与 provider-capable Tool 均 `SKIPPED`；用户批准规划和兼容 spike 门禁前不得进入实现。
- 外部调研与安全：只读取 MCP、官方 Java SDK 与 Spring AI 的公开官方文档；没有向外发送业务数据。真实 embedding/rerank/search/ask/generation/judge/LLM/provider 调用、费用和限流事件均为 0；真实 Milvus maintenance 继续 `SKIPPED`。
- 范围安全：未修改 accepted baseline、Java、Python、POM、migration、frontend、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或历史报告；未暂存、未提交、未 push、未创建 PR、未部署。
- 剩余风险与下一闸门：用户需审阅并批准 18 条 decisions、7 requirements / 26 scenarios、现有 JWT 非 OAuth-conformant 的明确边界、local-only/default-off 暴露策略、deterministic compare 与 SDK hard-stop 策略。批准后先只做依赖解析/编译级兼容 spike；通过后才允许进入 MCP adapter 的 TDD 实现。
- Commit：`pending`；建议 `docs(openspec): 启动C15只读MCP服务规划`。

## 2026-07-28｜C15 规划最终静态门禁

- 结构与规格：active change=1 且唯一为 `mcp-readonly-service`；artifact files=4，tasks unchecked=69（均为待事前闸门批准后的实现/验收项）；delta=7 requirements / 26 scenarios，和 accepted `rag-system` baseline 的重复 requirement title=0；GIVEN/WHEN/THEN/AND malformed=0。
- 决策与文档：design decisions=18，三行字段分别为 18/18/18；changed planning Markdown=4、missing relative links=0、trailing whitespace=0、敏感凭据模式=0、用户绝对路径=0、TODO/TBD/FIXME/待确认占位=0。
- 范围与 Git：最终工作区只有 `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md` 与 `openspec/changes/mcp-readonly-service/**` 变更；protected/implementation path matches=0，`git diff --check`=`PASS`。分支保持 `main...origin/main [ahead 7]`，未暂存、未提交、未 push。
- 工具边界：OpenSpec CLI=`ABSENT`，因此只记录文件级结构/链接/规格校验，不宣称 CLI validation 通过。Java/Python/POM/runtime/frontend 均无改动，Maven、Python、Docker/Testcontainers、frontend build、MCP conformance/client smoke 与任何 provider 调用继续 `SKIPPED`。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `docs(openspec): 启动C15只读MCP服务规划`。

## 2026-07-28｜C15 规划提交补录

- Commit：`c864e1c`（`docs(openspec): 启动C15只读MCP服务规划`）。本条只补录上一规划提交的真实 hash，不记录后续实现改动。

## 2026-07-28｜C15 实现授权与 SDK compatibility tracer bullet 启动

- 用户授权与提交责任：用户明确要求“启动C15规划实现”，据此通过 proposal、18 条 design decisions、tasks 与 7 requirements / 26 scenarios delta 的事前闸门，并授权规划内固定版本官方 MCP Java SDK / conformance 工具的加入与下载。提交责任继续为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 启动状态：HEAD=`c864e1c`，分支 `main...origin/main [ahead 8]`，工作区与暂存区干净；active change 唯一为 `mcp-readonly-service`。
- TDD 顺序：先以一个 public transport behavior test 建立 RED，再完成 SDK 2.0.0 dependency/compile/transport 最小 GREEN；不横向预写 Resources/Tools。若实际解析需要升级 Spring Boot/Spring Framework/Jackson/Reactor、手写协议或退回未批准 spec，则按 design hard-stop。
- 外调与安全：新增依赖只允许从公开 Maven 仓库解析；真实 embedding/rerank/search/ask/generation/judge/LLM/provider 调用、业务数据出站、真实 Milvus maintenance 仍未授权且保持 0 / `SKIPPED`。
- Commit：`pending`。

## 2026-07-28｜C15 SDK 2.0.0 compatibility 与最小 transport GREEN

- TDD RED→GREEN：新增 `McpProtocolMvcTest`，首次执行在 testCompile 仅因 `McpServerConfiguration` 不存在失败；随后只加入官方 SDK 与最小 conditional Servlet adapter，使真实随机端口 HTTP initialize 协商 `2025-11-25`、serverInfo=`enterprise-rag-readonly/c15-v1`、Resources/Tools capability、GET=405、无 `MCP-Session-Id` 全部通过。新增 default-off context test 与 Jackson 2 JSON Schema 2020-12 valid/invalid runtime test。
- 依赖与来源：固定 `io.modelcontextprotocol.sdk:mcp-bom/mcp-core/mcp-json-jackson2=2.0.0`，官方 release commit=`f56d038`、license=`MIT`；conformance suite 固定为官方仓库当前使用的 `@modelcontextprotocol/conformance=0.1.15`，尚未执行。下载来源为本机 Maven settings 指向的公开 alimaven mirror；本地 SHA-256：core=`56C1F99CC5E9932FCEBB57D4D82F89D0B641F90275FC19D0B132F6CE7D1EAB2C`，jackson2=`A7BDF467FB59B1675A3E4191B49EB0700869C82CCF2AD85EF70D962AE59E8C9C`，BOM POM=`35CC06E4560435ED0E98A1DB041F148E499E417C2F506AEA7ED95DCD3B766B9E`。
- 实际基线：dependency tree 显示 Boot 3.2.1 继续管理 Spring Framework `6.1.2`、Reactor `3.6.1`（SDK 上游声明 3.7.0）、Jackson `2.15.3`（上游声明 annotations 2.20 / databind 2.20.1）、SLF4J `2.0.9`；runtime 使用 Tomcat `10.1.17` / Servlet `6.0`。`mvn -q -pl rag-admin -am -DskipTests compile` 通过，initialize 与 schema validator 均未出现 linkage/serialization error，因此本切片 compatibility gate=`PASS`，无需升级框架或另立 foundation change。
- 验证：C15 组合聚焦测试 `McpServerConfigurationTest,McpProtocolMvcTest,McpSdkCompatibilityTest` 退出码 0；加入 `JwtAuthenticationFilterFailureTest,CurrentUserServiceTest,QAControllerTest` 的相邻回归命令也退出码 0。Maven Enforcer `dependencyConvergence` 退出码 1，但输出只命中既有 Milvus/Qdrant protobuf/guava/gRPC、PDFBox/Flexmark、annotations/collections 冲突，未出现 MCP artifact；如实记录为全树既有非 GREEN，不在 C15 顺手修复。
- 代码范围：新增 `McpProperties`、`McpServerConfiguration` 和 3 个 focused tests；只修改 root/rag-admin POM、ACTIVE_TASK/tasks/append-only log。尚未实现 JWT/Origin/identity、Resources、Tools、side-effect、Testcontainers 或 conformance；`/mcp` 仍默认关闭。
- 外调与安全：只下载公开 Maven artifact/读取官方 SDK 文档；真实 provider/model calls=0、business data outbound=false、真实 Milvus maintenance=`SKIPPED`。未修改 Spring/Jackson/Reactor/Servlet 版本、migration、前端、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或 accepted baseline。
- 剩余风险：SDK 以较新 Jackson/Reactor/Servlet 编译而由 Boot 管理到旧版本，当前 initialize + JSON Schema 路径已通过但尚未覆盖完整 Resources/Tools/conformance；后续每个纵向切片仍必须以 runtime HTTP test 继续证明兼容，任一 LinkageError/NoSuchMethodError 立即 hard-stop。
- Commit：`pending`；当前兼容切片建议 `feat(mcp): 建立C15协议兼容与默认关闭基础`。

## 2026-07-28｜C15 authentication / Origin / request identity 安全 checkpoint

- 范围与实现：在 SDK transport 前增加 `/mcp` 专用 `McpOriginAndExposureFilter`，固定 exact Origin allowlist、`*` fail startup、默认 local-only 且只信任 literal loopback peer；body 在 JSON-RPC 解析前按默认 `131072` bytes（配置硬上限 `1048576`）有界缓存，因此缺失 `Content-Length` 也不能绕过。POST 只接受显式 `application/json`，并要求 `Accept` 同时显式包含 JSON 与 SSE；固定返回脱敏的 403/413/415/406 类别。
- 身份边界：新增唯一 `McpRequestIdentityResolver`，每个 transport request 只从 Spring Security `Authentication -> UserPrincipal` 调用现有 `CurrentUserService.requireIdentity`；SDK `McpTransportContext` 只写入 server-resolved immutable `RequestIdentity`。handler context 缺该专用 key 时以 `AUTH_001` fail closed，query/header/body 中的 tenant/user selector 均不参与身份解析。
- JWT 与 usage：真实 Spring Security filter chain 证明 initialize、resources/templates/list、resources/list/read、tools/list/call 在无 token 时均先返回 `401/AUTH_001`；expired token、Cookie/query token 也不进入协议处理且响应不回显 token。`application.yml` 保持 `rag.mcp.enabled=false`、`local-only=true`；README 与 `docs/architecture/mcp-readonly-service.md` 明确手工 Bearer header、部署型 JWT 非 MCP OAuth Profile、无 metadata/discovery/audience/scopes 以及禁止直接远程暴露。
- TDD 证据：identity resolver 首先因类不存在 testCompile RED；无 Content-Length request-size 首先因 property 不存在 RED；Content-Type/Accept 首先以错误 200 放行 RED；transport-context handler identity 首先因方法不存在 RED；`application/*` 又先暴露兼容匹配误放行 RED。均只补最小生产实现后转 GREEN；Origin、local-only 与 endpoint filter registration 的行为 RED/GREEN 保留在对应 focused tests。
- 聚焦验证：`McpServerConfigurationTest,McpProtocolMvcTest,McpSdkCompatibilityTest,McpOriginAndExposureFilterTest,McpAuthenticationMvcTest,McpRequestIdentityResolverTest` 当前 Surefire 汇总为 23 tests / 0 failures / 0 errors / 0 skipped；加入 `JwtAuthenticationFilterFailureTest,CurrentUserServiceTest,QAControllerTest` 的组合命令退出码 0。真实随机端口 initialize 继续协商 `2025-11-25`、无 session id，request body wrapper 未破坏 SDK transport。
- 全仓验证：`mvn -q test` 退出码 1；`rag-admin` 为 239 tests / 1 failure / 0 errors / 21 skipped，唯一失败仍是既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` collector 时序用例。该用例独立复跑退出码 0，因此如实保持全仓非 GREEN，不把观测债务扩入 C15。最终静态门禁将在本条后复核；Commons Logging discovery warning 仍来自既有依赖路径，未作为 MCP 回归处理。
- 跳过项与范围安全：Resources/Tools、resource-not-found / tool error mapping、side-effect、双 tenant、conformance、独立 client、Python、前端 build 与 Docker/Testcontainers 尚未进入本切片。真实 embedding/rerank/search/ask/generation/judge/LLM/provider calls=0，business data outbound=false，真实 Milvus maintenance=`SKIPPED`。未修改 accepted baseline、migration、DTO、前端、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`，未暂存、未提交、未 push、未创建 PR、未部署。
- 剩余风险：Section 2 当前 5/6；稳定 not-found、JSON-RPC 与 tool execution error 必须随 Resources/Tools 落地后闭环。SDK 在较旧 Boot-managed Jackson/Reactor 上尚未经过完整 Resource/Tool/conformance 路径；下一切片从 `McpResourceUri` 恶意输入 RED 开始，出现 linkage/serialization drift 仍立即 hard-stop。
- Commit：`pending`；提交责任为用户手动提交，建议 `feat(mcp): 建立C15协议与认证安全基础`。

## 2026-07-28｜C15 协议与认证安全基础提交补录

- Commit：`e17adca`（`feat(mcp): 建立C15协议与认证安全基础`）。本条只补录上一实现 checkpoint 的真实 hash；用户已确认该 checkpoint 验收通过，C15 active change 继续进入 Read-Only Resources 切片，不代表 C15 整体验收或归档。

## 2026-07-28｜C15 Read-Only Resources URI parser checkpoint

- 范围与修改：新增 `McpResourceUri` 与 `McpResourceUriTest`，只实现 KB、document、chunk 三种 Resource URI 的纯解析/格式化值对象；同步 active design/spec delta/tasks 与 `.ai/ACTIVE_TASK.md`。未接数据库、权限或 SDK Resource handler，不横向进入 Tools。
- 公共契约：`McpResourceUri.parse(String)` 只接受小写 `rag://knowledge-bases/...` 的三种 exact grammar；kbId/documentId 为无前导零的 canonical 正 long，chunkIndex 为 `0` 或无前导零正 int。解析结果分别为强类型 immutable record，并由 `uri()` 产生 canonical 字符串；构造与解析均 fail closed。
- TDD 证据：KB、document、chunk 各自以一个 public round-trip test 启动，前三轮 RED 分别只因 `McpResourceUri`、`Document`、`Chunk` 不存在而 testCompile 失败，随后只补当前类型的最小 GREEN。再加入 28 组 invalid matrix，覆盖 null/empty、query、fragment、userinfo、port、percent/double-encoded slash、`..`、空/额外 segment、正负号、零/前导零/非法/溢出 ID、大小写变体、version path 与前置空格；有限 grammar 已使该矩阵直接保持 GREEN。
- 脱敏边界：所有无效输入统一抛 `InvalidResourceUriException("Invalid MCP resource URI")`，不把 raw URI、ID、query、tenant selector 或内部解析异常写入消息。foreign ID 与 document/KB mismatch 不能由纯 parser 判断，继续留给下一 Resource service 查询/授权切片，相关 task 未提前勾选。
- 验证：`McpResourceUriTest` 为 31 tests / 0 failures / 0 errors / 0 skipped；C15 全部 MCP focused reports 汇总为 54/0/0/0，真实随机端口 transport/JWT/Origin/request identity 回归继续通过；`git diff --check` 通过。
- 跳过项：本切片未改持久化、service、controller、provider、Python、前端或 infrastructure，因此不重跑全仓 Maven、Python、frontend build、Docker/Testcontainers、conformance 或独立 client；上一 checkpoint 已记录全仓唯一既有 OTel 时序失败及独立复跑通过。本轮真实 embedding/rerank/search/ask/generation/judge/LLM/provider calls=0，business data outbound=false，真实 Milvus maintenance=`SKIPPED`。
- 范围安全：未修改 accepted baseline、migration、DTO、现有 REST/RAG/provider 语义、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、未提交、未 push、未创建 PR、未部署。
- 剩余风险与下一步：URI parser 只证明语法与 canonical identity；尚未证明 accessible KB pagination、cursor 重授权、templates/list、KB/document/chunk whitelist、foreign/nonexistent fingerprint 或 timing。下一 tracer bullet 应进入 authenticated `resources/templates/list` 与 `resources/list`，再实现 read/authorization。
- Commit：`pending`；提交责任为用户手动提交，建议 `feat(mcp): 实现C15资源URI严格解析`。

## 2026-07-28｜C15 Resource URI parser 提交补录

- Commit：`74c9355`（`feat(mcp): 实现C15资源URI严格解析`）。本条只补录上一实现 checkpoint 的真实 hash；C15 active change 继续进入 authenticated Resource discovery 切片，不代表 Resource read、Tools、互操作证据或 C15 整体验收完成。

## 2026-07-28｜C15 authenticated Resource discovery checkpoint

- 范围与实现：新增固定三条 `resources/templates/list`，只包含 KB/document/chunk canonical template，不声明 subscription/listChanged 或 document version；新增 `McpKnowledgeResourceService`，把现有 `KnowledgeBaseService.getAccessibleByIdentity` 的当次授权结果按 `kbId` 升序映射为 KB Resource，默认每页 50、配置硬上限 100。列表只输出 URI、name/title 与 `application/json`，不输出 description、ownerId、vectorCollection、tenant/user 或自定义 metadata。
- Cursor 与逐请求授权：cursor 固定为无 padding canonical base64url 的 `v1:lastSeenKbId`，不含 tenantId、userId 或资源名称；非法/非 canonical cursor 与 tenant/limit 等 extra params 使用 JSON-RPC `-32602/MCP_INVALID_ARGUMENT` 且不回显输入。每个 `resources/list` 从当次 SDK transport context 重新取得 immutable `RequestIdentity` 并重新执行 accessible query；真实 HTTP 测试用 tenant A cursor 切换 tenant B 后只返回 B 的 `4,6`，不返回 A 的 next canary。
- SDK 实测决策：官方 SDK 2.0.0 的高层 `resources(...)` 实测是进程级静态 registry，不能安全表达按请求身份变化的列表。新增 design 决策 19，选择只在公开 `McpStatelessServerTransport#setMcpHandler` / `McpStatelessServerHandler` 边界窄装饰 `resources/list`；initialize、templates、read、tools、notification、JSON 解析/序列化与 HTTP transport 继续委托 SDK。明确禁止按请求全局 add/remove Resource，避免并发身份竞态与泄漏；本决策随 checkpoint 待用户复核，复核前不进入 Resource read。
- TDD 证据：模板 wire test 先以实际空列表 RED，再注册恰好三模板转 GREEN；SDK registry 不保证模板迭代顺序，因此测试只锁定 exact set/数量而不制造未批准顺序契约。Resource service 首先因类不存在 testCompile RED，再让当前 identity owner/public/permission + foreign fixture 只返回本 tenant `7,19,42` 并升序；51 条分页随后以 expected 50 / actual 51 RED，再补最小 cursor 分页转 GREEN。page-size 配置先因 getter/setter 不存在 RED；wire identity test先以 resources 空列表 RED，再接 request-scoped handler 转 GREEN。
- 聚焦验证：`McpServerConfigurationTest,McpProtocolMvcTest,McpSdkCompatibilityTest,McpOriginAndExposureFilterTest,McpAuthenticationMvcTest,McpRequestIdentityResolverTest,McpResourceUriTest,McpKnowledgeResourceServiceTest,KnowledgeBaseListTenantEnforcementTest` 汇总 62 tests / 0 failures / 0 errors / 0 skipped。真实随机端口 initialize、sessionless、JWT fresh authentication、Origin/request-size、三模板、分页、跨身份 cursor 与 invalid-argument 路径全部通过，未出现 SDK linkage/serialization drift。
- 全仓门禁：`mvn -q test` 退出码 1；`rag-admin` 278 tests / 1 failure / 0 errors / 21 skipped，唯一失败仍为既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` collector 时序断言；该用例独立复跑退出码 0，因此如实保持全仓非 GREEN，不扩入 C15。SensitiveLogs 扫描 337 source files / PASS；changed files=16、protected paths=0、frontend=0、migration/dependency paths=0、changed Markdown relative-link missing=0、`git diff --check`=PASS；design 为 19/19/19/19 决策三行结构。
- 跳过项：本切片未进入 KB/document/chunk `resources/read`、foreign/nonexistent matched controls、Tools、side-effect snapshot、双 tenant Testcontainers、conformance 或独立 client；Python/evaluation 与前端均无改动，因此 Python tests 和含 `vue-tsc` 的正式 frontend build 记为 `SKIPPED`。未运行 Docker/Testcontainers、真实 provider smoke 或真实 Milvus maintenance。
- 外调与范围安全：真实 embedding/rerank/search/ask/generation/judge/LLM/provider calls=0，business data outbound=false，费用/限流事件=0，真实 Milvus maintenance=`SKIPPED`。未修改 accepted baseline、migration、POM/依赖、现有 REST/RAG/provider 语义、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未暂存、未提交、未 push、未创建 PR、未部署。
- 剩余风险与下一步：当前分页结果有 wire 与现有 tenant service 聚焦证据，但尚无 MySQL 双 tenant integration、matched foreign/nonexistent timing、完整 result-byte limit 或 conformance evidence；`KnowledgeBaseService.getAccessibleByIdentity` 仍先读取当前身份全部可访问 KB 再由 MCP 层切页，不构成生产容量证明。用户复核决策 19 并手动提交后，下一 tracer bullet 才进入 KB Resource JSON whitelist/read authorization，不横向进入 document/chunk read 或 Tools。
- Commit：`pending`；提交责任为用户手动提交，建议 `feat(mcp): 实现C15资源发现与分页授权`。

## 2026-07-29｜C15 authenticated Resource discovery 提交补录

- Commit：`a701446`（`feat(mcp): 实现C15资源发现与分页授权`）。本条只补录上一实现 checkpoint 的真实 hash；用户已确认验收并批准 design 决策 19，C15 active change 继续进入 KB Resource read 切片，不代表 document/chunk Resource、Tools、互操作证据或 C15 整体验收完成。

## 2026-07-29｜C15 KB Resource read checkpoint

- 用户授权与提交责任：用户在验收 Resource discovery checkpoint 后明确要求进入下一阶段实现，据此确认 design 决策 19 并只推进 KB Resource read；提交责任保持 `用户手动提交`，Agent 未暂存、未提交、未 push、未创建 PR、未部署。
- 范围与实现：`McpKnowledgeResourceService.read` 每次从当次 transport context 取得 immutable `RequestIdentity`，调用现有 `AuthorizationService.requireKnowledgeBaseReadAccess` 做 tenant-scoped lookup 与 owner/public/READ authorization；KB JSON 只含 `id/name/description/documentCount/isPublic/createdAt/updatedAt`，排除 tenantId、ownerId、vectorCollection、storage/vector/internal fields。document/chunk template 在本切片继续 `MCP_RESOURCE_NOT_FOUND`，未横向进入其读取实现或 Tools。
- 错误与输入边界：foreign tenant KB 与 matched nonexistent KB 的 JSON-RPC fingerprint 均为 `-32603/MCP_RESOURCE_NOT_FOUND`；同 tenant 私有且无权限为 `MCP_FORBIDDEN`；未分类下游异常为 `MCP_INTERNAL_ERROR`，不回显原异常。SDK 2.0.0 实测会静默忽略 `resources/read` extra 参数，因此同一公开 stateless handler decorator 只在 SDK dispatch 前执行 `uri` / `_meta` exact allowlist，合法 read 仍完整委托 SDK；该取舍记录为待用户复核的 design 决策 20。
- TDD RED→GREEN：成功读取用例先得到 placeholder `MCP_RESOURCE_NOT_FOUND`（8 tests / 1 failure）；foreign/nonexistent 与 forbidden 用例先分别回显 `知识库不存在: id` / `无权访问该知识库`（10 / 2）；extra `tenantId` 先被 SDK 静默接受（11 / 1）；dependency canary 先被原样回显（12 / 1）。每轮只补对应 whitelist、error mapper、raw-param validator 或异常脱敏后转 GREEN；non-canonical URI 与 document/chunk fail-closed wire controls 也已锁定。
- 聚焦验证：`McpServerConfigurationTest,McpProtocolMvcTest,McpSdkCompatibilityTest,McpOriginAndExposureFilterTest,McpAuthenticationMvcTest,McpRequestIdentityResolverTest,McpResourceUriTest,McpKnowledgeResourceServiceTest,KnowledgeBaseListTenantEnforcementTest,AuthorizationServiceTest` 汇总 79 tests / 0 failures / 0 errors / 0 skipped。service test 使用真实 `KnowledgeBaseServiceImpl` 与 `AuthorizationService`、mock DB/provider boundary，验证查询固定携带当前 tenant、七字段 exact whitelist、document count tenant scope，以及 VectorStore/EmbeddingService interactions=0。
- 全仓门禁：`mvn -q test` 退出码 1；`rag-admin` 为 286 tests / 1 failure / 0 errors / 21 skipped，唯一失败仍是既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` collector 时序断言；该用例独立复跑退出码 0，因此如实保持全仓非 GREEN，不把观测债务扩入 C15。
- 静态门禁：SensitiveLogs 扫描 337 source files / PASS；changed files=12、protected paths=0、changed Markdown=5、missing relative links=0、`git diff --check`=PASS；design 为 20/20/20/20 决策三行结构。
- 跳过项及原因：本切片未改 Python/evaluation、frontend、migration、POM/依赖或 infrastructure，因此 Python tests、含 `vue-tsc` 的 frontend build、Docker/Testcontainers、official conformance 和独立 client 继续 `SKIPPED`。真实 embedding/rerank/search/ask/generation/judge/LLM/provider calls=0，business data outbound=false，费用/限流事件=0，真实 Milvus maintenance=`SKIPPED`。
- 范围安全：未修改 accepted baseline、migration、POM、现有 REST/RAG/provider 语义、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`；未覆盖或混入用户无关改动。
- 剩余风险与下一步：当前只有 KB wire matched fingerprint，尚无 document/chunk Resource、matched timing、MySQL 双 tenant integration、总 result byte limit、conformance 或独立 client evidence；决策 20 需用户复核。复核并手动提交后，下一 tracer bullet 进入 document Resource JSON whitelist 与 tenantId + documentId + kbId 一致性检查，不进入 chunk 或 Tools。
- Commit：`pending`；建议 `feat(mcp): 实现C15知识库资源读取授权`。

## 2026-07-30｜C15 KB Resource read 提交补录

- Commit：`e812e3e`（`feat(mcp): 实现C15知识库资源读取授权`）。本条只补录上一实现 checkpoint 的真实 hash；用户已确认验收并批准 design 决策 20，C15 active change 继续进入 document Resource read，并授权在既有安全边界内分纵向切片完成其余 C15 内容；不包含真实 provider 调用、push、PR 或部署。

## 2026-07-30｜C15 剩余 Resources、固定只读 Tools 与互操作证据闭环

- 用户授权与提交责任：用户在验收 KB Resource checkpoint 后要求进入下一切片，并明确允许按实际情况完成剩余 C15。提交责任继续为 `用户手动提交`；Agent 未暂存、未提交、未 push、未创建 PR、未部署。用户没有授权真实 provider/model smoke。
- Resource 范围：document read 只返回 `id/kbId/title/fileType/status/chunkCount/createdAt/updatedAt`，并按 tenantId + documentId + kbId 校验；chunk read 按 tenantId + documentId + chunkIndex 精确查询，输出 bounded UTF-8 text，截断不破坏 code point。citation 另按 tenantId + documentId + vectorId 精确读取。foreign/nonexistent/mismatched KB/document/chunk 统一使用脱敏 matched boundary。
- Tool 范围：固定 `rag.search`、`rag.ask`、`rag.get-citation`、`rag.compare-sources`，JSON Schema 2020-12 + `additionalProperties=false` + read-only/non-destructive/idempotent/closed-world annotations；公开 stateless handler 在 SDK dispatch 前校验 raw `tools/call`，拒绝 tenant/collection/filter/provider/model/retry/timeout/cache selector、空/超长文本、越界 topK/minScore、非法 citation identity 与正 long 溢出。最终 canonical schema SHA-256=`44ce4cc851551057bbabfbdf3735b964de300ff691b2e73a612856e9fa64213f`。
- 执行与结果边界：search 复用 KB read authorization + READY tenant vector scope + `QueryEngine`；ask 直接调用 `RAGService` 的 read-only service boundary，不经过 REST controller，因此不写 QA history/query count。get-citation/compare 两侧逐一授权，compare 固定 `semanticComparisonStatus=NOT_PERFORMED` 且 provider calls=0。`McpResultMapper` 验证 output schema，保持 structured/TextContent 语义一致，加入 canonical ResourceLink，并把双份 JSON + link 计入总 byte budget；source filename 去除 slash/backslash/drive/scheme 与控制字符，避免绝对路径披露。
- 运行保护：MCP/external/cache 继续分别 default-off；external tools 在 list 与 direct call 两侧双重关闭。所有 Tool 使用 tenant:user:tool security rate key，search+ask 共享 per-caller concurrency=2；SynchronousQueue bounded executor 拒绝排队膨胀，read/search/ask 分别使用 5s/30s/120s deadline，timeout cancel 当前 worker且不重放。Redis rate security state 异常 fail closed；QA cache 开启时也只传 server-owned tenant/KB scope。
- implementation-discovered 决策：用户放行剩余 C15即确认决策 20；design 新增决策 21，选择在公开 handler 边界统一校验 Tool raw input；新增决策 22，固定 conformance `0.1.15` 只执行适用于 C15 capability、无需 suite-specific fixture 的五个 generic scenarios。Windows 系统 Node 24.14.0 曾在 scenario 已报告 success 后出现 libuv teardown assertion，未忽略非零退出，改用固定 Node 22.17.0 后进程 clean exit。
- TDD：document/chunk、四 Tool、result/schema/ResourceLink、external disabled、cache on、rate/concurrency/timeout、独立 client 与双 tenant driver 均按 RED→GREEN 小步推进。最终静态复核另发现正整数 ID 缺 Java long 上限；新增 BigInteger overflow 用例先得到 `expected INVALID but was VALID`，补 `maximum=Long.MAX_VALUE` 后 validator 与新 schema hash 测试转 GREEN。
- 聚焦验证：最终 MCP + 相邻 RAG 命令为 22 reports / 132 tests / 0 failures / 0 errors / 0 skipped，覆盖全部 `Mcp*Test`、独立官方 Java SDK client、Document tenant guard、RAG service/cache/query engine 与 C15 profile contract。
- C15 组合证据：`c15-mcp-readonly` Failsafe 为 2 tests / 0 failures / 0 errors / 0 skipped；`C15McpConformanceIT` 用时 20.92s，`C15McpReadOnlyIT` 用时 48.00s。官方 client 覆盖 initialize、分页 Resources、三 template、三类 read、四 Tool 与 unknown write Tool；conformance 运行 `server-initialize/ping/tools-list/resources-list/dns-rebinding-protection`，suite-specific echo/add/long-running/Prompt/Task 场景明确不适用，未宣称整套 active scenarios 全部通过。
- 双 tenant 与副作用：自有 MySQL 8.0.36、Redis 7-alpine digest、etcd 3.5.5、MinIO 固定 release、Milvus 2.3.4 全部 healthy。driver 的 authoritative before/after SHA-256 均为 `4c5643ecad9793eeb797a560c6aa93d15420a35262f1634bf33c19549e434878`；real provider/model calls=0、business data outbound=false、deterministic embedding/generation invocations=5/2、QA cache disabled、真实 Milvus maintenance=`SKIPPED`。foreign/control timing 使用 seed=15001、5 warm-up、20 measured pairs、request errors=0；该粗门禁只代表本机 synthetic run。
- 全仓与脚本：最终 `mvn -q test` 退出码 1，rag-admin 320 tests / 1 failure / 0 errors / 2 skipped；唯一失败仍为既有 `GenAiTracingConfigurationTest#unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` collector 时序断言，立即独立复跑 1/0/0/0，因此全仓仍不记为 GREEN且未在 C15修改观测实现。Python 全量 190 tests / OK；SensitiveLogs 346 source files / PASS。
- 依赖与静态边界：dependency convergence 仍命中既有 PDFBox/Flexmark、Milvus/Qdrant protobuf/gRPC/Guava、annotations/collections 冲突，输出没有 MCP artifact 冲突，未顺手扩修。changed Markdown 本地相对链接 missing=0、protected paths=0、frontend/migration/accepted baseline changes=0、staged files=0、新增 credential/private-key/Bearer value/用户目录绝对路径命中=0；`git diff --check` 在最终文档同步后复核。前端无改动，含 `vue-tsc` 的正式 build=`SKIPPED`。
- 文档与状态：新增 `docs/eval/mcp/c15-mcp-readonly-traceability.md`，逐项映射 7 requirements / 26 scenarios；同步 usage、project、architecture overview、roadmap、optimization、design/tasks 与 ACTIVE_TASK。当前 driver evidence 生成于 HEAD `e812e3e` 的 dirty implementation tree，只是实现期可复现证据；用户手动提交后须在新 HEAD clean worktree 重跑，再由用户最终验收后接受 delta、归档 change并置 `IDLE`。
- 跳过项与剩余风险：live MCP provider smoke 未授权并 `SKIPPED`；没有真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、费用或业务数据出站。远程 TLS/proxy trust、MCP OAuth Authorization Profile、生产 rollout、真实 provider、真实 Milvus migration/maintenance、Qdrant/Elasticsearch、生产第二业务 tenant、Router 与 Agentic RAG 均未完成。Docker Desktop 为本轮 Testcontainers 验证启动，C15 容器由 Testcontainers 回收，Desktop 进程仍运行。
- Commit：`pending`；建议 `feat(mcp): 完成C15只读服务与互操作证据闭环`。

## 2026-07-30｜C15 最终静态门禁补录

- 静态与内容安全：SensitiveLogs 扫描 346 source files / PASS；新增及未跟踪文件的 private-key、AKIA、Bearer value 与用户目录绝对路径模式命中均为 0；新增 Java logger call=0，因此没有新增 raw query/content/metadata/provider body 日志面。Resource/Tool exact whitelist 与 result schema 仍由 focused/profile tests 覆盖。
- 文档与结构：changed files=47、changed Markdown=12、missing local relative links=0；design decisions=22，三行决策字段分别为 22/22/22；delta=7 requirements / 26 scenarios。OpenSpec CLI=`ABSENT`，因此不宣称 CLI validation 通过。
- 范围与 Git：protected paths=0，frontend/migration/accepted baseline changes=0，staged files=0；canonical Tool schema 旧 hash 命中=0，新 hash 在 4 个实现/测试/文档证据点一致。dependency convergence 仍只命中已记录的既有非 MCP 债务；未扩修。
- 最终文件写入后复核：`git diff --check`=`PASS`（只有既有文件的行尾转换 warning，没有 whitespace error）；Markdown relative links missing=0；tasks unchecked=2；Git status 为 26 个 tracked modified + 21 个 untracked、staged=0。本 change 只保留“用户最终验收/接受 delta/归档/置 IDLE”及归档后能力声明边界两项，不提前勾选用户闸门。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `feat(mcp): 完成C15只读服务与互操作证据闭环`。

## 2026-07-30｜C15 剩余实现提交补录

- Commit：`4595967`（`feat(mcp): 完成C15只读服务与互操作证据闭环`）。本条只补录上一轮剩余 Resources、固定只读 Tools、运行保护与互操作证据实现提交的真实 hash；不记录本轮 baseline acceptance/archive 改动。

## 2026-07-30｜C15 最终验收、Baseline Acceptance 与 OpenSpec 归档

- 用户授权与范围：用户明确要求归档 C15，据此视为最终验收，只执行 `mcp-readonly-service` 的 clean-HEAD 复跑、delta acceptance、长期文档同步、archive 与 `ACTIVE_TASK=IDLE`；提交责任保持 `用户手动提交`，未暂存、未提交、未 push、未创建 PR、未部署。
- clean-HEAD 证据：实现提交为 `45959672ec64ec72c05bcbe17fe52204555f1098`。固定 `c15-mcp-readonly` 命令最终退出码 0，Failsafe 为 2 tests / 0 failures / 0 errors / 0 skipped；`C15McpConformanceIT` 用时 27.52s，`C15McpReadOnlyIT` 用时 46.05s。driver evidence 为 `workingTreeDirty=false`、SDK `2.0.0`、spec `2025-11-25`、conformance `0.1.15`、Tool schema SHA-256=`44ce4cc851551057bbabfbdf3735b964de300ff691b2e73a612856e9fa64213f`。
- 状态与隔离：authoritative before/after SHA-256 均为 `8261a84b7344e813a7dec0dca80e34e1642d4a96b869444066d93b5fe2a5a419`，`authoritativeStateUnchanged=true`；deterministic embedding/generation invocations=5/2，real provider/model calls=0、business data outbound=false、QA cache=false、真实 Milvus maintenance=`SKIPPED`。timing seed=15001、warm-up=5、measured pairs=20、request errors=0，仅作为本机粗粒度 synthetic evidence。
- 环境诊断：首次沙箱内 Maven 解析因网络权限失败；沙箱外首次因本机 PATH 中带错误引号的 Tomcat 条目使 Testcontainers path probe 失败；过滤仅本次子进程的非法条目后发现 Docker daemon 未运行。隐藏启动 Docker Desktop 并确认 Server `28.4.0` 后，完整 profile 通过。以上均为环境前置失败，不计入最终 C15 测试结果；Testcontainers 容器由 Ryuk 回收，Docker Desktop 仍运行。
- Baseline 与归档：`rag-system` delta 的 7 requirements / 26 scenarios 已 literal exact-copy 到 `openspec/specs/rag-system/spec.md`，归档前 exact suffix=`true`。tasks unchecked=0；change 已移动到 `openspec/changes/archive/2026-07-30-mcp-readonly-service/`，archive files=4；`.ai/ACTIVE_TASK.md` 已置为 `IDLE`。
- 文档同步：更新 README、`openspec/project.md`、MCP architecture/overview、technical debt、iteration blueprint、optimization index 与 C15 traceability，统一改为“已验收归档”，并保留 default-off/local-only、非 OAuth profile、非生产部署与无真实 provider evidence 的边界。
- 跳过项与既有风险：本轮未重跑全仓 Maven/Python/前端，因为实现内容已提交且归档轮只改治理文档；实现轮全仓 Maven 仍因既有 OTel collector 时序断言保持非 GREEN，Python 190 tests / OK，前端无改动。OpenSpec CLI=`ABSENT`，不宣称 CLI validation；live MCP provider smoke 未授权并继续 `SKIPPED`。
- 范围安全：未修改业务代码、配置、migration、前端、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或历史报告。未开放 MCP 默认开关，也未扩大为 OAuth、远程生产、真实 tenant rollout、Qdrant/Elasticsearch、C16 Router 或 Agentic RAG。
- Commit：`pending`；建议 `docs(openspec): 接受契约并归档C15只读MCP服务`。

## 2026-07-30｜C15 归档最终静态门禁补录

- OpenSpec 文件级验证：baseline exact suffix=`true`；archive files=4、tasks unchecked=0、delta=7 requirements / 26 scenarios、active change dirs=0、`ACTIVE_TASK=IDLE`。OpenSpec CLI=`ABSENT`，未宣称 CLI validation。
- 文档与安全：changed paths=19、unexpected paths=0、existing changed Markdown=15、missing relative links=0、trailing whitespace=0；SensitiveLogs 扫描 346 source files / PASS；新增及归档文件的 private-key、AKIA、Bearer value 与用户目录绝对路径模式命中均为 0；C15 stale active/pending/dirty 状态命中=0。
- Git 与清理：`git diff --check`=`PASS`（只有 `iteration-blueprint.md` 的既有行尾转换 warning，没有 whitespace error）；staged files=0，分支保持 `main...origin/main [ahead 13]`。Testcontainers label 残留容器=0；Docker Desktop 仍运行。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `docs(openspec): 接受契约并归档C15只读MCP服务`。

## 2026-07-30｜C15 归档提交补录

- Commit：`3b2f06e`（`docs(openspec): 接受契约并归档C15只读MCP服务`）。本条只补录上一归档提交的真实 hash，不记录 C16 规划。

## 2026-07-30｜C16 Bounded Query Router Readiness 与规划启动

- 用户目标与提交责任：用户要求查看项目状况，若允许则直接进入 C16 规划。提交责任按默认保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- Readiness：启动 HEAD=`3b2f06e`，分支 `main...origin/main [ahead 14]`，工作区与暂存区均干净，`.ai/ACTIVE_TASK.md=IDLE`；未归档 active change=0，C15 archive 存在且 tasks unchecked=0。C14 正式 26/26 required cases 与 functional/content/error/timing/global `PASS` 已解除 C16 前置隔离门禁；路线图把 C16 定义为 C15/MCP 不捆绑的独立 Type C change，因此结论为 `GO`。
- 能力分类：`confirmed` 为 query normalization/query variants、hybrid/BM25/RRF、rerank/provider attribution、context token budget、sync/SSE/MCP read-only ask、C14 tenant gate 与 v2 eval types；`partial` 为已有阶段级 timeout/diagnostics 但没有统一 route plan/跨阶段 budget，no-answer 分散在 empty retrieval 与生成文本判断，sync/SSE/eval attribution 不一致；`planned` 为 `fact-intent-v1`、唯一 `fact-v1`、usage ledger、`evidence-no-answer-v1`、cache/telemetry compatibility 与 versioned router eval；`out_of_scope` 为 multi-hop/global/high-risk、Agent loop、LLM classifier、生产默认切换、live provider evidence、API/SSE/MCP schema/数据库/前端变更；`unknown` 为真实 fact coverage、生产 deadline/token ceiling、客户端 route metadata 需求和真实 provider latency/cost。
- 规划产物：创建 `openspec/changes/bounded-query-router/`，包含 proposal、design、tasks，以及 `rag-system` / `evaluation` 双 spec delta；design 记录 18 条真实取舍。更新 `.ai/ACTIVE_TASK.md` 为 `ACTIVE / C16 规划待审`，accepted baseline 未修改。
- 关键边界：Router 默认关闭；enabled 模式 classifier 为纯本地确定性 allowlist，只有 `fact-v1`，ambiguous/multi-hop/global/high-risk 返回 `UNSUPPORTED` 且 provider calls=0，不偷跑 legacy。fact strategy 只允许一次 retrieval orchestration，不沿用 explanation second pass；budget ledger 约束 query variants、rerank/generation、token 与 stage 间 deadline。
- No-answer 与评测：`evidence-no-answer-v1` 在 retrieval 后判断 evidence，insufficient evidence generation calls=0，dependency error 不映射 no-answer，model refusal/citation不足保留独立 reason。新增独立 router status 与 classification/strategy/budget/quality/no-answer/error 通道，不替代既有 Report status 或修改既有指标公式。
- 外调与范围安全：本轮只读取仓库事实并写 OpenSpec/治理文档；真实 embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false、费用/限流事件=0、真实 Milvus maintenance=`SKIPPED`。未修改 Java、Python、POM、runtime config、migration、frontend、accepted baseline、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或历史报告。
- 跳过项及原因：本轮是 Type C 事前规划，Maven、Python、frontend build、Docker/Testcontainers、deterministic integration 与 live provider evaluation 均 `SKIPPED`；规划获批前不得进入实现。OpenSpec CLI 可用性与最终结构/链接/敏感信息/`git diff --check` 在后续静态门禁复核。
- 剩余风险与下一闸门：用户需审阅并批准 18 条 decisions、双 delta、enabled 非 fact unsupported、fact success 最低 citation evidence、deadline 非强制抢占 I/O、SSE wire 不变与 deterministic evidence 完成口径。批准后应从 default-off config + classifier/registry 的纯 unit/property RED 开始，不横向进入高级策略。
- Commit：`pending`；建议 `docs(openspec): 启动C16有界查询路由规划`。

## 2026-07-30｜C16 规划最终静态门禁

- OpenSpec 结构：未归档 active change=1 且唯一为 `bounded-query-router`；change artifacts=5。`rag-system` delta=5 requirements / 17 scenarios，`evaluation` delta=4 requirements / 12 scenarios；每个 scenario 的 GIVEN/WHEN/THEN 数量 exact match，malformed=0，与 accepted baseline 重复 requirement title=0。
- 决策与任务：design decisions=18，三行字段“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”均为 18/18/18；tasks unchecked=70，全部属于用户事前批准、实现、验证和验收后 closeout，未提前勾选。C15 archive tasks unchecked=0。
- 文档与安全：本轮 changed/new Markdown=7，missing relative links=0、trailing whitespace=0、TODO/TBD/FIXME/待确认占位=0；private-key、AKIA、Bearer value 与用户目录绝对路径模式命中=0。OpenSpec CLI=`ABSENT`，因此只记录文件级结构/格式检查，不宣称 CLI validation 通过。
- 范围与 Git：最终变更只包含 `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md` 与 `openspec/changes/bounded-query-router/**`；implementation/protected/accepted baseline paths=0，staged files=0，`git diff --check`=`PASS`。分支保持 `main...origin/main [ahead 14]`，未暂存、未提交、未 push。
- 跳过项：Java/Python/POM/runtime/frontend 均无改动，Maven、Python、Docker/Testcontainers、frontend build、deterministic integration 与任何 provider 调用继续 `SKIPPED`；规划获批前不进入实现。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `docs(openspec): 启动C16有界查询路由规划`。

## 2026-07-30｜C16 规划提交补录与统一实现授权

- 规划提交补录：Commit=`4d5fef5`（`docs(openspec): 启动C16有界查询路由规划`）。本条只补录上一规划提交的真实 hash，不回改历史记录。
- 用户授权与闸门：用户要求“统一规划，开始C16实现”，据此确认 proposal、design 18 条决策、`rag-system` 5 requirements / 17 scenarios 与 `evaluation` 4 requirements / 12 scenarios，授权在该 active change 内按 TDD 实现。该授权不包含新增/升级依赖、真实 provider/model 调用、baseline acceptance/archive、Git 提交、push、PR 或部署。
- 启动状态：HEAD=`4d5fef5`，分支 `main...origin/main`，工作区与暂存区干净，唯一 active change=`bounded-query-router`。提交责任保持 `用户手动提交`。
- 实现顺序：先完成 default-off Router/classifier/closed-world registry 的公共接口 tracer，再实现 budgeted fact retrieval、`evidence-no-answer-v1`、cache/telemetry、sync/SSE/MCP 与 versioned evaluation；每个行为保持 RED→GREEN，不先铺满横向测试。
- 外调与范围：实现主证据保持真实 embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false；不修改公开 request DTO、SSE wire、MCP schema、database migration、frontend、`.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。
- Commit：`pending`。

## 2026-07-30｜C16 第一实现切片：Bounded Fact Router 主链路

- 范围与修改：新增 default-off `BoundedQueryRouter`、`RouterProperties`、`fact-intent-v1` 确定性分类器、route/budget/final-state 值对象与 `QueryBudgetLedger`；为 `QueryEngineImpl` 增加 server-owned query variant ceiling/实际 count diagnostics；将 Router 接入同步 ask、SSE unsupported/no-evidence、cache identity 与 GenAI telemetry。配置只新增 `rag.router.*` 且默认 `false`，未修改 request DTO、SSE wire、MCP Tool schema、数据库或前端。
- TDD 事实：逐项观察到构造器缺失、分类理由错误、变体超额、legacy/router 缓存复用、预算 metadata 缺失、SSE terminal state 缺失、telemetry 属性缺失与未验证 citation 被误判 ANSWER 等 RED；最小实现后对应聚焦测试转 GREEN。fact 路径只执行一次 retrieval orchestration，insufficient evidence 不调用 generation，dependency error 保持 ERROR，无 CitationValidator `validCitations>0` 的回答稳定为 `NO_ANSWER/UNVALIDATED_EVIDENCE`。
- 验证：`mvn -q -pl rag-core -am test` 退出码 0；随后 C15 MCP schema、request validator、execution guard、server config、result mapper、external-tools default-off 与 profile contract 相邻命令退出码 0。最近 55 份 Surefire reports 汇总 246 tests / 0 failures / 0 errors / 0 skipped。`git diff --check` 无 whitespace error，仅有既有 CRLF/LF warning。
- 外调与安全：全部验证使用 mock/property/deterministic fixtures；真实 embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false、费用事件=0。没有新增依赖，未修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`、accepted baseline 或历史报告；未暂存、未提交、未 push、未创建 PR、未部署。
- 跳过与剩余风险：versioned router eval/validator/evaluator、candidate/context/token usage、SSE generation 完成/取消后的 citation/final-state、完整 MCP authoritative before/after 与全仓 `mvn -q test` 尚未执行；live router ask/eval 未授权并保持 `SKIPPED`。当前只是第一实现切片，不构成 C16 完成、baseline acceptance、生产默认或真实 provider 质量/SLA 证明。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `feat(rag): 实现C16有界事实路由第一切片`。

## 2026-07-30｜C16 第一实现切片提交补录

- Commit：`32d085cb2a1479aaca2a4b0e47a5efa561eefee7`（`feat(rag): 实现C16有界事实路由第一切片`）。本条只补录上一执行提交的真实 hash；本轮继续实现仍为 `Commit: pending`。

## 2026-07-30｜C16 剩余实现与 Deterministic Evidence 收口

- 范围与实现：补齐 `QueryBudgetUsage/QueryBudgetLedger` 的 candidate/context、estimated context/output token 与 deadline facts；新增 `GenerationBudget` 并把 server-owned context/output ceiling 传入 prompt 与 provider request。SSE 首版在内部缓冲后复用 citation validation，只有 validated answer 才转发原 chunks；terminal signal 记录 transport outcome、classifier/strategy/policy/final state/no-answer reason 与完整 usage，取消/timeout/error 不形成 partial success。
- Router 结构：`RouterProperties` 增加固定 `strategy-version=fact-v1`，unknown classifier/policy/strategy 与所有 budget hard bounds 在构造期 fail closed；新增 closed-world `QueryStrategyRegistry`、`FactQueryStrategyExecutor` 与 `EvidenceAdmission`，把 fact generation、stream finalize 和 pre/post evidence policy 从主服务中显式编排。分类器只增加通用“是指什么”事实后缀；冻结 20 条 sidecar 为 FACT=10 / UNSUPPORTED=10，intent 全匹配。
- MCP 与兼容：新增 deterministic `C16McpRouterIntegrationTest`，确认 MCP `rag.ask` 复用同一 enabled `RAGService.ask`、同一 fact route/final state，且 Tool diagnostics 不暴露 route/budget/provider selector。全部 MCP 相邻回归为 18 classes / 102 tests / 0 failures / 0 errors / 0 skipped；公开 request DTO、SSE wire、MCP Tool schema、数据库与前端均未修改。
- 评测 release：新增 `docs/eval/router/` 下 manifest、ID-only expectation schema/sidecar、固定 budget 与 traceability；dataset 继续固定 `rag-eval-dev-v2` 150 条 identity。新增 Python 标准库 `router_eval_contract.py` 与 `evaluate_bounded_query_router.py`，覆盖安全相对路径、hash/bytes/order/count/distribution、sidecar coverage、七通道、confusion/FACT precision/recall/coverage/unsupported leakage、四状态退出码与 formal no-overwrite。plan-only=`VALID`，manifest SHA-256=`099a35a11cb6301592cb8cf5b41812671caf9b905924676dcb81916b057d8553`。
- TDD 与聚焦验证：validator 11 tests、evaluator 10 tests 均先观察 module missing RED 后转 GREEN；Router 配置/矩阵/并发/locale/timezone/10,000-case Unicode fuzz、release isolation、evidence policy、budget、generation ceiling、RAG sync/SSE 与 cache 聚焦测试全部通过。C16 所在 `rag-core` 全量为 168 tests / 0 failures / 0 errors / 0 skipped。
- 全量验证：`python -B -m unittest discover -s scripts -p 'test_*.py'` 为 211 tests / OK。`mvn -q test` 实际为 616 tests / 1 failure / 0 errors / 2 skipped；唯一 failure 是既有 `GenAiTracingConfigurationTest.unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts` 在全仓执行时捕获到 OTel 原始连接日志，隔离重跑该类 9/9 通过。该债务已在 roadmap 登记且与 C16 无代码交集，因此不越界修改，也不把全仓结果声明为 GREEN。首次并行全量命令触发工具超时并留下测试 JVM，已只终止本轮启动的四个 JDK 17 测试进程，未触碰 IDE Java 进程。
- 安全与静态门禁：SensitiveLogs 扫描 369 source files / PASS；protected/frontend/migration/accepted-baseline changes=0；Router production source 的 `docs/eval`、sample ID、sidecar label 引用=0；新增评测普通输出路径的用户绝对路径与 credential value 命中=0（`AuthorizationService` 类名为非 secret 误报）；release artifacts 固定 LF；`git diff --check` 无 whitespace error，仅有既有 CRLF/LF warning。评测 validator、evaluator 和本轮全部业务测试真实 embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- 文档与状态：更新 `.ai/ACTIVE_TASK.md`、tasks、project、architecture、roadmap、optimization index、eval guide 与 C16 traceability。tasks 只剩用户最终验收后的 baseline exact-copy/archive/`IDLE` 和归档措辞两项，不提前执行。
- 跳过与剩余风险：前端 build=`SKIPPED`（无前端/API/SSE wire 改动）；live router ask/eval、真实 provider/model、费用/限流与业务数据出站均未授权并 `SKIPPED`。当前证据只支持 default-off `fact-intent-v1/fact-v1/evidence-no-answer-v1` 与固定 deterministic profile，不支持生产默认、真实质量/SLA、multi-hop/global/high-risk 或 Agentic RAG。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `feat(rag): 完成C16有界路由与评测闭环`。

## 2026-08-01｜C16 最终验收、baseline 接受与归档

- 用户明确要求归档 C16；按 `用户手动提交` 责任执行收口，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 规格收口：`openspec/changes/bounded-query-router/specs/rag-system/spec.md` 的 5 requirements / 17 scenarios 与 `evaluation/spec.md` 的 4 requirements / 12 scenarios 已分别 literal exact-copy 接受为两个长期 baseline 的 suffix；对比结果 `exact_suffix=true`。
- 状态收口：C16 tasks 71 项实现/验证任务与 2 项最终收口任务均已勾选；active change 已移至 `openspec/changes/archive/2026-08-01-bounded-query-router/`；`.ai/ACTIVE_TASK.md=IDLE`；未归档 active change=0。
- 当前证据：`python -B scripts/router_eval_contract.py --plan-only` 返回 `VALID`，selection=20、required channels=7、providerCallCount=0、businessDataOutbound=false、liveEvaluationStatus=`SKIPPED`；既有 C16 focused/MCP/Python 证据保持上一执行记录，Maven 全仓既有 OTel 日志捕获时序失败与 2 个 Redis 环境 skip 仍不声明 GREEN。
- 同步文档：`openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md`、`docs/optimization/README.md`、`docs/eval/RAG_EVAL_GUIDE.md` 与 C16 traceability 已更新为已验收归档状态；生产默认仍关闭，未扩大为真实 provider 质量、multi-hop/global/high-risk 或 Agentic RAG。
- 跳过项及原因：前端正式 build 因无前端改动记为 `SKIPPED`；live router ask/eval、真实 provider/model、业务数据出站、真实 Milvus maintenance、部署与远程发布均未授权并保持 `SKIPPED`；OpenSpec CLI 不在 PATH，使用文件级 exact-match/结构检查替代，未声称 CLI validation 通过。
- 剩余风险：C10 仍为 DRAFT/PENDING_REFERENCE_EVIDENCE；真实生成/citation/judge/provider 质量、生产 Router 默认开启、Qdrant/Elasticsearch 等价隔离、真实 Milvus shadow migration 与高级 Router 策略仍需独立 change/evidence/授权。
- Commit：`pending`；建议 `docs(openspec): 验收并归档C16有界查询路由`。

## 2026-08-01｜C16 归档后静态验证补录

- 验证：两个 baseline 与归档 delta 均 `exact_suffix=True`；`rag-system` delta=5 requirements / 17 scenarios，`evaluation` delta=4 requirements / 12 scenarios；baseline requirement duplicate titles=0。
- 验证：`python -B scripts/router_eval_contract.py --plan-only`=`VALID`；`python -B -m unittest discover -s scripts -p 'test_*.py'`=`211 tests / OK`；changed Markdown=15、missing relative links=0；C16 旧 active 路径/等待验收表述扫描=0；`git diff --check`=`PASS`。
- 范围：source active directory 不存在、archive files=5、archive tasks unchecked=0、active non-archive change dirs=0、`ACTIVE_TASK=IDLE`、unexpected changed paths=0；仅发生计划内 baseline/status/docs/archive 变更。
- Commit：`pending`；本条为 C16 归档验证补录，不执行暂存或提交。

## 2026-08-10｜企业证据型知识运行时迭代蓝图 v6 冻结

- 用户授权与提交责任：用户一次性批准 D01–D36，并明确回复“全部接受”；本轮只执行路线图冻结、技术债重排、静态验证和一次 Agent 中文短提交。该授权不包含 C17/OpenSpec 启动、profile 数值批准、真实 provider 调用、push、PR、部署或发布。
- 范围与修改：将 `docs/roadmap/iteration-blueprint.md` 从已完成 C1–C16 的 v5 更新为 v6，冻结“企业证据型知识运行时”定位、W0–W6 主线、C17–C21 近端切片、有界策略、知识源联邦、MCP 2026 migration、durable research、受控行动/A2A 与生产化边界；重排 `docs/roadmap/technical-debt.md`，保留并显式列出 OTel 时序、开发态 JWT fallback、C10 DRAFT profile、generation/judge、SSE、分块、恢复演练、真实 Milvus/tenant、远程 MCP、生产观测和前端/反馈债务。
- 关键决策：下一主线先激活 fixed-identity v2/150 × 3 retrieval reference evidence，再推进 generation objective baseline、judge 4-call canary/72-call full calibration、objective/judge profile 与结构化 SSE；高级 Router 顺序为 multi-hop → compare → temporal → global → high-risk。首个 Agent runtime 固定为单 durable investigation task，不采用 multi-agent swarm；所有新能力继续 default-off、server-owned policy、evidence-first。
- 验证：`git diff --check`=`PASS`；v6 关键术语/阶段/调用上限扫描全部命中；v5 日期/冻结后首项/C16 readiness 等 stale 表述在两份当前文档中命中=0；临时 `*.v6.tmp` 文件=0；首次范围检查只有两份 roadmap 文档，追加本日志后计划内文件为 3 个。
- 跳过项及原因：本轮无 Java/Python/frontend/runtime/spec 变更，Maven、Python tests、frontend build、Docker/Testcontainers 均 `SKIPPED`；真实 embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false、费用与限流事件=0。
- 范围安全：未修改 `.ai/ACTIVE_TASK.md`、active/archive OpenSpec、accepted baseline、业务代码、配置、migration、前端、`.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或历史评测报告；`ACTIVE_TASK` 继续为 `IDLE`，C17 未启动。
- 剩余风险：C17 的 provider/model/KB/config 与正式外调预算仍须独立 readiness 和执行授权；具体 hard floors/regression tolerances 必须在完整 reference evidence 后由用户审阅，不能由本蓝图预先填值；蓝图时间窗口不是进度或生产承诺。
- Commit：`pending`；建议 `docs(路线图): 冻结企业证据型知识运行时蓝图v6`。

## 2026-08-12｜C17 Retrieval Quality Gate Activation 规划启动

- 用户授权与提交责任：用户在只读 readiness 判定为“规划有条件 GO”后明确要求“现在开始规划”。本轮按 Type C 事前闸门创建单一 change；提交责任默认并明确为 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 范围与修改：创建 `openspec/changes/retrieval-quality-gate-activation/` 下 proposal、design、tasks 与 `evaluation` spec delta；将 `.ai/ACTIVE_TASK.md` 置为唯一 `ACTIVE / PLANNING_REVIEW`。accepted baseline、业务代码、Python tooling、profile 数值、runtime config 与历史 evidence 均未修改。
- 已确认事实：启动时 HEAD=`701ade3`、`main...origin/main [ahead 1]`、工作树干净、`ACTIVE_TASK=IDLE`、无其他 active change；C10 profile 仍为 `v1-draft / DRAFT / PENDING_REFERENCE_EVIDENCE`，full v2/150、12 条 rules 的 target 全为空。tracked embedding/rerank 默认不等于 runtime identity，费用/配额与 KB readiness 仍为 `unknown`。
- 规划决策：新增独立 C17 reference manifest，不复用 C7 arm；固定 5-case canary 与 150×3 full、zero retry、existing-KB fail closed；三次 evidence strict identity/完整性不允许成功子集；locked reference 使用 median 并保留 min/max/spread；hard floors/tolerances 只在完整 evidence 后由用户批准；canonical profile 显式 `v1-draft -> v1`；raw details 留在 ignored tmp，tracked artifact 仅保存脱敏 allowlist。
- W0 与外调边界：OTel 全仓时序债务作为 live canary/full 的独立硬前置，不混入 C17 diff。规划/offline/plan-only calls=0；建议 canary 上限 5 retrieval + 5 query embedding，full 上限 450 + 450，external rerank/ask/generation/judge=0；两阶段均需单独授权。KB 重建、fixture upload 和 indexing embedding 不在本 change 预算，preflight 不 READY 时停止。
- 规格与任务：delta 共 4 requirements / 12 scenarios；design 共 14 条三行决策记录；tasks 当前 5 项 planning facts 已勾选、57 项用户批准/实现/W0/外调/阈值/验收与 closeout 保持未勾选。规划批准不等于 offline implementation、live calls、阈值批准或归档授权。
- 跳过项及原因：本轮仅规划文档与治理指针，Maven、Python full tests、frontend build、Docker/Testcontainers、backend preflight、canary/full provider evidence 均 `SKIPPED`；未获得实现或外调授权。真实 provider calls=0、business data outbound=false、费用/限流事件=0。
- 剩余风险与下一闸门：用户需审阅 proposal、14 条 design decisions 与 4 requirements / 12 scenarios；重点确认 5-case canary 的额外 5 calls、median reference、三次 source repeat 必须通过最终 profile、canonical `v1-draft -> v1`、W0 独立前置和 raw/tracked artifact 边界。批准后仍需用户明确授权 offline TDD implementation。
- Commit：`pending`；建议 `docs(openspec): 启动C17检索质量门禁规划`。

## 2026-08-12｜C17 规划静态门禁

- OpenSpec 结构：未归档 active change=1，唯一为 `retrieval-quality-gate-activation`；change artifacts=4。`evaluation` delta=4 requirements / 12 scenarios，GIVEN/WHEN/THEN=12/12/12，与 accepted baseline 重复 requirement title=0。
- 决策与任务：design decisions=14，三行字段“面临的选择 / 选了哪个 + 为什么 / 放弃的代价”均为 14/14/14；tasks checked=5，仅为用户启动规划、启动事实、规划产物/指针、提交责任和零外调边界；unchecked=57，覆盖待审、W0、实现、外调、阈值、验收与 closeout。
- 文档与安全：change Markdown missing relative links=0、trailing whitespace=0、TODO/TBD/FIXME/待确认/待补充占位=0；private-key、AKIA、Bearer value、`sk-` credential pattern 命中=0。OpenSpec CLI=`ABSENT`，因此只记录文件级结构/格式检查，不宣称 CLI validation 通过。
- 范围与 Git：最终 changed/new paths=6，且只包含 `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md` 与 `openspec/changes/retrieval-quality-gate-activation/**`；unexpected paths=0、accepted baseline diffs=0、business/script/config/frontend diffs=0、staged files=0；`git diff --check`=`PASS`。
- 跳过项：本轮没有实现代码、profile 数值或 runtime 变更，Maven、Python full tests、frontend build、Docker/Testcontainers、backend preflight 和任何 live/provider evidence 均 `SKIPPED`。planning calls=0、business data outbound=false。
- Commit：`pending`；提交责任保持 `用户手动提交`，建议 `docs(openspec): 启动C17检索质量门禁规划`。

## 2026-08-12｜C17 规划提交补录与 Offline Implementation 授权

- 规划提交补录：Commit=`9181897`（`docs(openspec): 启动C17检索质量门禁规划`）。本条只补录上一规划提交的真实 hash，不回改历史记录。
- 用户授权：用户明确表示“授权你 commit 权限，规划通过，现在开始实现阶段”，据此批准 proposal、design 14 条决策与 `evaluation` delta 的 4 requirements / 12 scenarios，并授权 C17 计划内 offline TDD implementation 和本地 Agent commit。
- 授权边界：本轮可修改 C17 manifest/schema、Python evaluation tooling/tests、eval guide、active tasks 与 AGENT_LOG；不包含 W0 OTel 修改、canary/full backend/provider 调用、profile 阈值批准、baseline acceptance/archive、push、PR、部署或发布。
- 实现顺序：先用 RED tests 固定 manifest/参数 fail-fast、canary/full 调用预算、三次 strict identity/completeness 与 rule distribution；再实现 runner/纯本地 compiler，最后运行聚焦和全量 Python/static gates。
- 外调状态：实现阶段继续保持 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false、费用/限流事件=0。
- Commit：`pending`。

## 2026-08-12｜C17 Offline Reference Tooling Implementation

- 范围与修改文件：新增 `docs/eval/config/c17-retrieval-reference-v1.json`、执行/脱敏 evidence schemas、`scripts/compile_retrieval_reference.py` 与 tests；更新 reproducible runner、C10 evaluator 纯 observed 入口、eval guide、active tasks/指针。未修改 dataset/fixture、Java/POM/frontend、retrieval/chunking/embedding/rerank/metric 公式、production QA/default provider、accepted specs 或历史 artifacts。
- 已确认事实与关键决策：C17/C7 manifest 互斥；C17 只允许 existing KB、ignored `tmp/eval/c17/`、no-overwrite、zero retry、heuristic attribution；full 计划固定 150×3=450 debug retrieval / 最多 450 query embedding，canary 固定 5 IDs/1 repeat=5/5，external rerank/ask/generation/judge=0。compiler 只本地读取三份 details/metadata，严格校验 repeat/sample/dataset/fixture/document/KB/config/Git/run/metric/provider identity，复用 C10 rule observed 计算并输出 COMPLETE/INCOMPLETE/NOT_COMPARABLE/INVALID；DRAFT COMPLETE 仍只 `PENDING_THRESHOLD_APPROVAL`。
- RED→GREEN：RED 先得到 `ModuleNotFoundError: compile_retrieval_reference`、runner 缺 `load_reference_manifest`、evaluator 缺 `calculate_rule_observation`；GREEN 聚焦 `python -B -m unittest test_compile_retrieval_reference test_run_reproducible_rag_eval test_evaluate_quality_gate`=`63 tests PASS`。
- 全量验证：`python -B -m unittest discover -s scripts -p 'test_*.py'`=`226 tests PASS`；C17 manifest 与 synthetic COMPLETE pack 均通过 `jsonschema`；full/canary plan-only=`PASS` 且 actual calls=0；`python -B scripts/check_sensitive_logs.py --root .`=`PASS (370 source files)`；3 个 C17 JSON 可解析、`tmp/eval/c17` 被 `.gitignore` 命中、protected/accepted/history paths diff=0、`git diff --check`=`PASS`。
- 跳过项：W0 OTel closeout、backend preflight、真实 KB/MySQL/Milvus、canary/full embedding/provider、ACTIVE profile/reference replay、baseline acceptance/archive 均未授权或仍有前置，记 `SKIPPED`；Java/POM/frontend/runtime/API 无改动，因此 Maven/frontend build 也 `SKIPPED`。本轮 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- 剩余风险：当前只是 offline tooling GREEN，不是 reference evidence 或 active gate；profile 仍 `DRAFT / PENDING_REFERENCE_EVIDENCE`。W0 与 preflight 未完成，canary/full 仍需分别披露 runtime fingerprint、费用/限流/数据出站并取得授权；编译器的 ACTIVE replay 路径目前以 synthetic unit test 覆盖，须在用户批准 12 条 target/tolerance 后以真实三次 source evidence 重放。
- Commit：`pending`；用户已授权本 change 计划内本地 Agent commit，建议 `feat(eval): 实现C17检索参考证据工具链`；push/PR/deploy 未授权。

## 2026-08-13｜C17 Offline Tooling 提交补录

- Commit：`c9d86d5`（`feat(eval): 实现C17检索参考证据工具链`）。本条只补录上一执行提交的真实 hash，不回改历史记录。

## 2026-08-13｜W0 OTel unavailable-collector 时序债务收口

- 范围与修改文件：作为 C17 live 前的独立 Type B 前置，修改 `GenAiExportDiagnostics.java` 与对应测试，使安全 JUL filter 在被外部 logging 初始化重置后可幂等重装；从 `docs/roadmap/technical-debt.md` 移除已关闭的 W0 债务。不修改 exporter、timeout、queue、fail-open、指标语义、C17 evaluation tooling 或 accepted specs。
- 已确认事实与关键决策：修复前全仓 `mvn -q test` 稳定复现唯一失败，`GenAiTracingConfigurationTest` 第 188 行捕获到原始 endpoint；相同 9-test 类独立运行通过。根因是 Spring 全仓前序 logging 状态清除了 logger filter，而进程级 `AtomicBoolean` 阻止后续 context 重装，属于既有安全契约内测试/全局状态稳定性问题，不需要改变 runtime semantics 或新建 Type C change。
- 验证：聚焦 `mvn -q -pl rag-admin -am '-Dtest=GenAiExportDiagnosticsTest,GenAiTracingConfigurationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`=`12 tests PASS`；全仓 `mvn -q test`=`PASS`。按本次运行时间筛选 Surefire XML：617 tests、0 failures、0 errors、21 skipped，其中 `rag-admin` 322 tests 全通过；OTLP 日志只保留固定安全消息。
- 跳过项及原因：21 项为既有外部环境 smoke/MySQL migration/recovery 条件跳过；本次不启动本地 collector 或 MySQL，不把这些跳过项包装为真实集成通过。frontend、C17 backend preflight 与 provider calls 均未执行；business data outbound=false、费用/限流事件=0。
- 范围安全与剩余风险：`EmbeddingServiceImpl.java` 的规范化 worktree hash 与 HEAD blob 均为 `765ee8801b3e6923cf540068249e075962e3e064`，路径级索引刷新后 staged/worktree diff 均为空；W0 已可供 C17 引用，但 C17 仍需独立 preflight、canary/full evidence、阈值批准与 activation replay。
- Commit：`pending`；提交责任为 `Agent 提交`，建议 `fix(observability): 稳定OTel安全日志过滤器重装`；push/PR/deploy 未授权。

## 2026-08-13｜W0 OTel 修复提交补录

- Commit：`f2f0ec3`（`fix(observability): 稳定OTel安全日志过滤器重装`）。本条只补录上一执行提交的真实 hash，不回改历史记录。

## 2026-08-13｜C17 W0 引用提交补录

- Commit：`e3af60e`（`docs(eval): 记录C17的W0前置关闭`）。本条只补录上一治理提交的真实 hash，不回改历史记录；本次采用独立纯日志补录提交，不递归记录该补录提交自身 hash。

## 2026-08-13｜C17 W0 后 mutation-free preflight

- 范围与启动事实：先确认 `EmbeddingServiceImpl.java` 的规范化 worktree hash 与 HEAD blob 完全一致并通过路径级索引刷新清除 CRLF/LF 伪改动；随后独立 W0 commits=`f2f0ec3`/`e3af60e`/纯日志 `8270706`，当前 clean HEAD=`82707068deaea038e96d842d4253cf50c40b3070`。恢复既有 Docker Desktop 与 5 个 compose 容器，MySQL/Redis/etcd/MinIO/Milvus 均 healthy；未修改 `.env.local`、`application-dev.yml` 或 tracked runtime config。
- Preflight：使用 C17 full manifest 参数运行 `--preflight-only --keep-existing`，status=`READY`、mutationFree=true、expected/matched fixture=3/3、missing/incomplete=0/0、document states 全为 `COMPLETED`、chunk counts=11/14/25。只执行本地登录与 KB/document 读取；未创建/删除 KB、未上传 fixture、未触发 indexing embedding，provider/embedding/rerank/ask/generation/judge calls=0。
- Runtime fingerprint：OpenAI-compatible adapter + NVIDIA hosted `nvidia/llama-nemotron-embed-1b-v2`，endpoint host/path=`integrate.api.nvidia.com/v1/embeddings`，dimension=2048、timeout=60000ms、fallback=false、API key present（值未输出）、local proxy `127.0.0.1:7897` reachable；Git=`8270706`、`application.yml` SHA-256=`d66479a5ae5c8f92d37a1d5fbe933d5e32e0134c7844c35587d95486d5ba6575`、heuristic rerank、eval retry=0。
- 费用/限流与数据出站：通过 agent-reach/Exa 核对 NVIDIA 官方 NIM FAQ/model catalog；Developer Program hosted endpoints 用于原型/研发并提供免费访问，因此 canary 预期直接费用=0，但账户剩余 quota/rate-limit 无本地/官方固定数值证据，保持 unknown。已披露 5 条 tracked eval question 将经本机代理出站到 NVIDIA，external rerank/ask/generation/judge=0。
- 阻断与跳过：在执行前，外调安全门拒绝把概括的“完成 C17”授权解释为这 5 条具体问题向具体 NVIDIA endpoint 出站的知情授权；命令未启动、canary files 均不存在、canary/provider calls=0、business data outbound=false。必须由用户在上述披露后明确批准，禁止绕过；full 450/450、threshold approval、activation/archive 继续 `SKIPPED`。
- 剩余风险：NVIDIA hosted endpoint/API catalog、账户 entitlement/quota 可能变化；canary 获批后任何 auth/429/timeout/provider drift/fallback/model rerank 均停止且不重试。canary clean 也不自动授权 full。
- Commit：`pending`；提交责任为 `Agent 提交`，建议 `docs(eval): 记录C17预检就绪状态`；push/PR/deploy 未授权。

## 2026-08-13｜C17 Preflight 提交补录

- Commit：`f5ffbbd`（`docs(eval): 记录C17预检就绪状态`）。本条只补录上一执行提交的真实 hash，不回改历史记录；本次采用独立纯日志补录提交，不递归记录该补录提交自身 hash。

## 2026-08-13｜C17 Canary 失败与 Vector Readiness 门禁修复

- 授权与实际调用：用户在具体披露后明确批准固定 5 条 tracked eval question 向 NVIDIA embeddings endpoint 出站，预算为 5 debug retrieval / 最多 5 query embedding、external rerank/ask/generation/judge=0、retry=0。实际发起 5 次 localhost debug retrieval；五次均在 embedding 前被 `VECTOR_INDEX_NOT_READY` / HTTP 503 拦截，因此实际 query embedding/provider calls=0、business data outbound=false、retry/fallback/model rerank=0、预期费用=0。
- Canary 证据：runner invocation 退出码虽为 0，但 raw details 的 Report status=`FAILED`、objective status=`FAILED`、retrieveErrors=5、rateLimitErrors=0、sampleCount=5、heuristic attribution=unknown；按报告字段判定 canary `FAILED`，不以进程退出码冒充 clean。raw report/details/metadata 仅位于 ignored `tmp/eval/c17/`，未进入 reference aggregate，未形成质量结论，也未启动 full。
- 根因与只读盘点：runtime 在 `KnowledgeBaseServiceImpl.requireReadyVectorScope` 检查 SQL `vector_readiness` 时 fail closed；V12 对 legacy KB 的默认状态为 `LEGACY_PENDING`。只读 Milvus inventory 确认 source collection exists=true、vectorCount=50、dimension=2048，与三份 COMPLETED documents 的 11+14+25=50 chunks 一致；无需重新 embedding，正确恢复路径是既有 C13b tenant-aware shadow migration。
- TDD 工具修复：`run_reproducible_rag_eval.py` 的 preflight 新增只读 `/statistics` probe，vector statistics 不可读或 count 与 expected chunks 不一致时 `BLOCKED`；显示输出移除数字 KB ID/collection。C17 live child details 非 `RETRIEVAL_ONLY`、error/retry 非零、sample identity 或 heuristic/fallback/model-call attribution 漂移时，父 runner 现在非零失败。对应测试按逐项 RED→GREEN 增补。
- 验证：runner 聚焦 `42 tests PASS`；阶段中间全量 Python=`231 tests PASS`，待本条最终改动后再复跑；SensitiveLogs=`PASS (370 source files)`、真实修复后 preflight=`BLOCKED / VECTOR_READINESS_UNAVAILABLE`、expected vectors=50、matched fixtures=3。未修改 dataset、retrieval/rerank/embedding/metric 公式、production QA/provider、Java/POM/frontend、accepted specs 或历史 artifacts。
- 跳过与剩余风险：真实 shadow collection 创建、50 vector 复制、全量审计、SQL mapping/readiness 原子切换、失败后的清理/重试均未授权，保持 `SKIPPED`；full 450/450、compiler、threshold approval、ACTIVE replay、baseline acceptance/archive 同样 `SKIPPED`。shadow migration 不出站、不调用 embedding/rerank/LLM，但会新增本地 collection 并写 MySQL readiness；source collection 保留用于回滚，任何清理不在授权建议内。
- Commit：`pending`；提交责任为 `Agent 提交`，建议 `fix(eval): 让C17预检与单次运行失败关闭`；push/PR/deploy 未授权。

## 2026-08-13｜C17 Canary 门禁修复最终静态复验

- 最终验证：`python -B -m unittest test_run_reproducible_rag_eval`=`42 tests PASS`；`python -B -m unittest discover -s scripts -p 'test_*.py'`=`235 tests PASS`；`python -B scripts/check_sensitive_logs.py --root .`=`PASS (370 source files)`；`git diff --check`=`PASS`。
- 范围复核：tracked diff 仅为 C17 runner/tests、eval guide、active tasks/指针和 append-only log；dataset/release/fixtures、accepted specs、archive/history、Java/POM/frontend/runtime/provider diffs=0。三份 canary raw artifacts 均由 `/tmp/eval/` ignore 规则覆盖。
- 结论边界：上述 GREEN 只证明 fail-closed tooling 修复，不把 `FAILED` canary 改写为通过。真实 shadow migration、canary rerun 和 full reference 仍未执行；Commit=`pending`。

## 2026-08-13｜C17 Canary 门禁修复提交补录

- Commit：`abd3c70`（`fix(eval): 让C17预检与单次运行失败关闭`）。本条只补录上一执行提交的真实 hash，不回改历史记录。

## 2026-08-13｜C17 首次 Shadow Migration 失败与 Scope Marker 修复

- 授权与执行边界：用户明确批准一次真实 zero-retry shadow migration。执行前复核 MySQL 为 `LEGACY_PENDING`、3/3 documents COMPLETED、50 chunks/50 vector IDs，Milvus source exists=true/count=50/dimension=2048、deterministic shadow exists=false；本地后端先停止以冻结写入，迁移使用无 Web、无业务调度器、禁用 Flyway 的最小 MyBatis 上下文。授权范围内 provider/embedding/rerank/ask/generation/judge/LLM calls=0、business data outbound=false，不删除 source、不自动清理或重试。
- 首次真实结果：source audit 已读到 50/50，但 shadow upsert 前抛出稳定 `Vector metadata scope conflicts with server scope`。根因是 legacy metadata 经 Gson 读取后把等价 `kbId` 表示为浮点数，而 `MilvusVectorStore` 使用字符串比较，导致 `10.0` 与服务端 `10` 被误判冲突。迁移按合同停止；MySQL=`AUDIT_FAILED`、expected/observed/migrated/missing/mismatch=`50/50/0/0/0`、last error=`maintenance_failure`、active mapping=`SOURCE_ACTIVE`，Milvus source=50、shadow exists=true/count=0。没有第二次迁移、cleanup 或 mapping switch。
- TDD 与修改文件：先补 `MilvusVectorStoreFailureSemanticsTest` 的等价 legacy 数值用例，RED 稳定复现原异常；最小实现改为数值等价后 GREEN。随后增加 IEEE-754 大整数舍入安全用例，RED 证明直接 `double == long` 会误接收，再以 `BigDecimal` 精确比较收口；真实冲突、非整数和不可解析值继续 fail closed。修改 `MilvusVectorStore.java`、对应测试、C17 tasks/active pointer 与本 append-only 日志；未修改 migration contract、SQL schema、API、provider/config、dataset/profile/accepted specs。
- 验证：等价数值单测 RED=`IllegalArgumentException`、GREEN=`PASS`；大整数舍入单测 RED=`expected IllegalArgumentException but reached vector dependency`、GREEN=`PASS`；`MilvusVectorStoreFailureSemanticsTest` + `VectorShadowMigrationServiceTest`=`18 tests / 0 failures / 0 errors`；最终 `mvn -q test` exit=0，按本次 Surefire XML 汇总=`619 tests / 0 failures / 0 errors / 2 skipped`。
- 跳过项与剩余风险：原授权明确 zero retry，因此修复后真实 migration retry=`SKIPPED`，现存空 shadow 未清理，backend 保持停止；mutation-free preflight、canary rerun、full 450/450、compiler、threshold approval、ACTIVE replay、baseline acceptance/archive 均未执行。下一步必须先重新披露现存 `AUDIT_FAILED`/空 shadow 状态并取得一次新的 migration retry 授权；即使迁移成功，canary 仍需新的独立外调授权。
- Commit：`pending`；提交责任为 `Agent 提交`，建议 `fix(vector): 兼容legacy scope数值标记`；push/PR/deploy 未授权。

## 2026-08-29｜前端 UX 迭代建议文档落盘

- 任务类型：Type B 文档任务；不指向 active change，`.ai/ACTIVE_TASK.md`（C17 retrieval-quality-gate-activation）未改动、未受影响。
- 范围与修改文件：新增 `docs/roadmap/frontend-ux-iteration.md`（前端体验迭代建议池：总体判断、2026-08-29 代码事实快照、F-01~F-16 条目清单含优先级/定级/状态、4 个推进切片、与既有 roadmap/债务的关系、验证基线、落地流程、迭代记录）；追加本日志。未修改任何代码、配置、spec 或既有文档。
- 已确认事实：路由 `/chat` 仅用 `ChatPanel.vue`；`RagChatInterface.vue`/`ChatView.vue` 无引用，`ChatMessage/RetrievedContextList/CitationList` 仅被 `ChatView.vue` 间接引用（死代码簇结论已按引用核实）；`thumbUp/thumbDown` 仅 `console.log`；`useSSE.ts` 已有 AbortController；`highlight.js` 已声明未接入 md 实例；markdown-it `html:false` 无 XSS 敞口；dark mode 变量齐全。侧栏 collapse 行为与 history/feedback 后端接口覆盖情况在文档中标注"待确认"，未臆断。
- 验证：`git diff --check` = PASS；文档引用的 9 个文件路径逐一经 `ls` 核实存在。文档类改动无需构建类验证。
- 跳过项：未运行前端 build（本任务无代码改动）；死代码删除、任何条目实现均未启动，文档落盘不等于实现授权。
- 剩余风险：§2 快照基于当日 main 分支，代码演进后需回写；F-05/F-06 高档与 F-10 等条目定级依赖后端接口核实结果。
- Commit：`pending`；提交责任为用户手动提交（本轮未获 Agent 提交授权），建议 `docs(roadmap): 新增前端UX迭代建议文档`；push/PR/deploy 未授权。

## 2026-08-29｜高仿 ChatGPT 界面静态 Demo（独立原型）

- 任务类型：前端演示原型（未触碰 `rag-frontend` 生产代码与 C17 active change；`.ai/ACTIVE_TASK.md` 未改动）。
- 范围与修改文件：仅新增 `prototype/chatgpt-ui-demo/`（`index.html`、`css/app.css`、`js/app.js`、`README.md`）；追加本日志。设计基线为 `docs/开发文档/前端文档/UI-Reference/` 下 7 张 ChatGPT 网页端截图（2026-08 深色界面）。
- 已确认事实与关键决策：纯静态零依赖（内联 SVG 图标、系统字体、无 CDN），双击或任意静态服务可开；品牌沿用"RAG 智能问答"，界面结构复刻 ChatGPT（侧栏/问候页/气泡/资料库表格/搜索弹窗/设置弹窗/用户菜单）；RAG 特色为知识库范围 chip、`已检索 × 个片段`折叠行、引用角标浮层（相似度+chunk）、评测库"评测"徽标；全部数据为本地 mock，界面常驻"演示数据"提示，无任何网络请求。
- 验证：`node --check js/app.js`=PASS；`git diff --check`=PASS；经本地 `http.server` + 内置浏览器在 1600×900 逐视图截图验证：首页（暗/亮主题）、会话流式输出（含代码块复制按钮、消息操作）、检索展开、引用浮层、知识库列表/网格视图、搜索弹窗、设置弹窗（外观/强调色真实生效）、用户菜单、侧栏收起、聊天/工作分段。浏览器自动化验证中发现并修复 5 个问题：`topbar-right` 缺 `margin-left:auto`、composer 在 flex 容器内被压缩、`data-icon` 未水合导致 +/mic/筛选/视图切换/关闭按钮无图标、`pickQA` 误读 `CONVS[0]`（被新建对话占用）致发送抛错、`renderConv` 与流式轮重复渲染。
- 跳过项：未运行 `rag-frontend` 的 vue-tsc build（demo 不在其构建体系内，未改任何前端源码）；键盘 Enter 发送在本自动化环境中无法注入按键，已用页面内调用等效验证，真实键盘路径为标准 `keydown` 监听；移动端仅做了 ≤900px 的 CSS 适配，未逐机型截图。
- 剩余风险：demo 仅证明视觉与交互方向，不代表任何已实现能力；若后续要把该皮肤落入 `rag-frontend`，需按 `docs/roadmap/frontend-ux-iteration.md` 条目定级（Type B/C）并立 OpenSpec change；`localStorage` 持久化仅存主题/强调色，无用户数据。
- Commit：`pending`；提交责任为用户手动提交（本轮未获 Agent 提交授权），建议 `demo(frontend): 新增高仿ChatGPT界面静态原型`；push/PR/deploy 未授权。

## 2026-08-29｜高仿 ChatGPT Demo 用户反馈修复（+图标缺失 / 移除右缘悬浮按钮）

- 范围与修改文件：仅 `prototype/chatgpt-ui-demo/` 内 3 个文件。`index.html` 删除右缘悬浮分享按钮节点；`js/app.js` 将 `initComposer()`（模板克隆）调整到 `initStaticIcons()`（`data-icon` 水合）之前，修复 `#plusBtn`/`#micBtn` 图标不显示（根因：水合时模板节点尚未入 DOM）；同步移除 `floatShare` 相关 JS（图标 map、`show()` 显隐、`float-share` 菜单 case）；`css/app.css` 删除 `.float-share` 样式块及媒体查询引用。
- 验证：`node --check js/app.js`=PASS；`grep` 确认无 `float-share/floatShare` 残留；浏览器重载后检查 `plusSvg=true`、`micSvg=true`、`floatGone=true`，截图确认输入框左侧 `+`、右侧听写图标正常渲染、右缘无悬浮按钮；顶栏「分享/更多」菜单（copy-link/export-md）未受影响。
- 跳过项与剩余风险：无新增；其余边界同上一条 Demo 记录。Commit：`pending`；提交责任为用户手动提交，建议并入 `demo(frontend): 新增高仿ChatGPT界面静态原型`；push/PR/deploy 未授权。

## 2026-08-31｜C17 Shadow Migration Retry 成功与迁移后 Preflight

- 授权与范围：用户明确回复“授权执行一次 C17 shadow migration retry”。本次授权仅允许恢复本地 Docker 依赖、只读 source、复用 deterministic 空 shadow、复制并审计 50 vectors、成功后原子切换 MySQL mapping/readiness；provider/embedding/rerank/ask/generation/judge/LLM calls=0、business data outbound=false，不删除 source、不自动清理或第二次重试。提交责任沿用 C17 的 `Agent 提交`；push/PR/deploy 未授权。
- 事前门禁：当前 HEAD=`46bd90a` 且包含 scope fix commit `a0c4e1f`；重新构建当前后端 jar 成功，embedded `rag-core` 与模块 jar SHA-256 一致。后端 8080 未监听；MySQL=`AUDIT_FAILED / SOURCE_ACTIVE`、expected/observed/migrated/missing/mismatch=`50/50/0/0/0`、source=`50×2048`、现存 shadow=0。前端 demo 与本日志已有未提交改动均原样保留，未混入迁移代码。
- 唯一一次 retry：无 Web、无调度器、禁用 Flyway 的最小 MyBatis 上下文调用既有 `VectorShadowMigrationService`；结果=`READY`、expected/observed/migrated=`50/50/50`、missing/mismatch=`0/0`。MySQL 事后=`READY / SHADOW_ACTIVE / SOURCE_RETAINED`，error category=`NONE`。
- 事后核验：source exists/count=`true/50`；Milvus collection statistics 对新 shadow 一度返回 0，但迁移使用的 tenant-scoped STRONG query、50 个预期 vector ID 读回均为 50，dimension=2048。随后启动真实本地 backend，startup BM25 按 active shadow 重建 50 chunks；mutation-free C17 preflight=`READY`、vector readiness=`READY`、vector count=`50/50`、fixtures matched/missing/incomplete=`3/0/0`。因此不把 eventually-consistent collection statistics 单次 0 误判为数据丢失。
- 构建与验证：沙箱内 Maven 首次因 parent POM 访问权限失败；按仓库既有授权在本机执行 `mvn -q -pl rag-admin -am package -DskipTests`=`PASS`。后端启动时 Flyway 验证 12 migrations、schema v12 无待执行 migration；未运行新的全仓测试，因为本轮未修改 Java/Python 实现，使用既有 fix 的 619-test 证据并补充真实 migration/preflight 证据。
- 剩余门禁：本次 retry 授权已消耗。fixed 5-case canary 仍需重新披露并取得独立授权；当前 runtime 为 OpenAI-compatible NVIDIA embedding、`nvidia/llama-nemotron-embed-1b-v2`、`integrate.api.nvidia.com/v1/embeddings`、dimension=2048、timeout=60000ms、fallback=false、proxy=false、retry=0。full 450/450、compiler、阈值批准、ACTIVE replay、baseline acceptance/archive 继续未授权。
- Commit：`pending`；建议 `docs(eval): 记录C17迁移就绪与预检通过`。

## 2026-08-31｜C17 Migration 后 Canary 失败：NVIDIA Hosted Endpoint 已弃用

- 授权与调用边界：用户明确回复“授权执行一次 C17 fixed 5-case canary”。执行前同一 HEAD=`46bd90a`、tracked config SHA-256=`d66479a5...a6575`、runtime provider/model/endpoint/dimension/timeout/fallback/proxy/retry=`OpenAI-compatible NVIDIA / nvidia/llama-nemotron-embed-1b-v2 / integrate.api.nvidia.com/v1/embeddings / 2048 / 60000ms / false / false / 0`；mutation-free preflight 再次 `READY`、vector count=`50/50`、fixtures=`3/3`。
- 实际结果：固定 5 条 tracked question 各执行一次 localhost debug retrieval，并分别进入一次 NVIDIA query embedding；5/5 provider response 均为 HTTP 410 Gone。runner=`FAILED`、retrieveErrors=5、rateLimitErrors=0、retry=0、fallback/model rerank=0，ask/generation/judge/external rerank=0。raw artifacts 写入 ignored `tmp/eval/c17/canary-retry-20260831*` 且 no-overwrite；canary 授权已消耗，未自动重试、未进入 aggregate、未启动 full。
- 根因核对：使用 agent-reach 的 Exa + Jina Reader 读取 NVIDIA 官方模型页；页面于 2026-08-31 明确标记该模型 `Deprecated`，并写明 “This NIM Endpoint has been deprecated”。本地新增临时 HTTP-path test 证明现有 WebClient 对 base URL `/v1` 与 `uri("/embeddings")` 的实际路径仍为 `/v1/embeddings`，路径假设被推翻；临时测试随后删除，Java 生产/测试代码最终 diff=0。
- 边界判断：官方候选 `nemotron-3-embed-1b` 同为 2048 维，但不同模型产生不同 embedding 空间；仅维度相同不能让现有 50 条旧向量与新 query vector 可比较。C17 proposal/non-goal 明确排除默认 embedding/provider 切换、KB rebuild/indexing embedding，因此本轮不修改 `.env.local`、provider config、dataset/profile/metric，不重建 KB，也不申请 full。
- 验证与跳过：本地 URI 聚焦 test=`PASS`（用于否定错误路径假设，文件已删除）；官方模型页读取=`PASS`；`agent-reach check-update` 因与当前诊断无关且网络副作用未获授权而被审批拒绝，按要求 `SKIPPED`，未绕过。后端已停止；Docker 本地依赖保持运行。
- 剩余风险与决策门：若要继续 C17，需独立选择并批准其一：A) 自托管 deprecated 旧模型 NIM，保留向量空间但引入 GPU/容器/容量依赖；B) 选择当前 hosted 新模型，重建固定 KB 的 50 chunks 并重置 provider/KB/reference identity。两者都超出当前 C17 原边界，不能由本次 canary 授权推导。
- Commit：`pending`；建议 `docs(eval): 记录C17 provider端点弃用阻断`。

## 2026-08-31｜C17 新模型 Adapter/Indexing 零外调审计

- 授权与范围：用户要求“先完成零外调的 adapter/indexing 离线审计”。本轮只读取当前 Java/config/schema/tests、C17 OpenSpec 与 ignored C17 safe metadata counts，并更新 C17 design/tasks/active pointer/append-only log；backend/provider/embedding/rerank/ask/generation/judge calls=0、business data outbound=false、KB/collection/SQL mutation=0，未修改 `.env.local`、Java、schema、runtime config、dataset/profile/reference 或前端文件。
- 审计结论：当前实现尚不可进入 synthetic smoke。阻断包括：adapter 缺 `modality/embedding_type/truncate`；未验证 response model/count/index/order/exact-2048/finite；`getModelName()` 固定为 `openai` 导致新旧模型 cache identity 冲突；embedding 默认 `maxRetries=3` 与 C17 retry=0 冲突；普通 indexing 未检查 batch result count/order；既有 shadow service 只复制旧 vectors、READY 时直接返回且固定 `shadow_v1`；KB mapping/query/preflight/manifest/compiler 均未持久化并强制 model/request/collection generation identity。
- 冻结 contract：目标仍为 `nvidia/nemotron-3-embed-1b`；固定 KB 三文档 chunk counts=`11/14/25`，exact passage items=50；adapter 每 HTTP batch items<=5，沿用 document grouping 的保守 HTTP request upper bound=`3+3+5=11`，automatic retry=0。新模型需独立 model-rebuild workflow、actual-model+request+dimension cache identity且 rebuild bypass cache、显式新 generation collection、50/50/50 强读回、missing/mismatch=0，以及旧 collection+identity CAS 原子切换；失败保留旧 mapping/source 且不自动补跑/清理/复用 generation。
- 验证：纯静态 `rg/Get-Content` 交叉核对 adapter、embedding cache、indexing、Milvus、shadow mapper/service、application config、C17 manifest/compiler 与既有 tests；`git diff --check` 待本轮文档完成后执行。未运行 Maven/Python/frontend build：本轮未修改代码或 executable tooling，且审计目标是冻结 implementation 前 RED gaps。
- 剩余门禁：需用户另行批准 model-migration offline implementation；该阶段仍为零外调/零 KB mutation。实现与离线 contract tests clean 后才可重新披露并申请 1-item synthetic smoke；旧 retry/canary 授权均已消耗且不得复用。
- Commit：`pending`；本轮未收到新的提交指令，建议 `docs(eval): 完成C17新模型离线审计`；push/PR/deploy 未授权。

### C17 新模型离线审计验证补录

- `git diff --check`=`PASS`；手工 OpenSpec structure/freeze-token/task-gate 检查=`PASS`。本机未安装 `openspec` CLI，`openspec validate <change> --strict`=`SKIPPED (CLI_NOT_FOUND)`；未以该跳过项冒充严格 CLI 验证。最终确认 provider/backend calls=0、KB mutations=0。
