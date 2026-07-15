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
