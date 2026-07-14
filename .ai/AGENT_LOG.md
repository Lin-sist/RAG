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
