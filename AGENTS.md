# AGENTS.md

## 1. 项目与语言

- 项目：Enterprise RAG QA System。
- 默认使用简体中文与用户沟通，代码标识符、命令和协议字段保留原文。
- 当前定位：可运行、可评测的模块化 RAG 工程原型，不得包装成生产级多租户系统或已完成的 Agentic RAG 平台。

## 2. 必读顺序

按当前任务需要渐进式读取，不因工作目录在仓库内而加载全部文档：

1. 先读 `AGENTS.md`；同一会话已读且内容未变时复用。
2. 涉及仓库开发状态、代码修改、测试执行或变更隔离时，读 `.ai/ACTIVE_TASK.md` 与 `.ai/GOTCHAS.md`（非显而易见陷阱备忘）。
3. 仅当任务涉及 active change 时，读其 proposal、design、tasks 和相关 spec delta；无关审查不加载整套 change。
4. 需要承接近期执行上下文时，扫读 `.ai/AGENT_LOG.md`（滚动热窗口，≤200 行）；不默认读取 `.ai/logs/` 下的历史冷归档。
5. 仅当需要项目背景或能力契约时，读 `openspec/project.md` 与相关 baseline spec。
6. 读取当前任务直接涉及的代码、配置和测试；必要时再读相关架构或技术债文档，不默认遍历历史报告。

纯教学、学习验收和模拟面试不自动进入开发工作流；资料读取范围遵循对应 Skill。审查 Skill 文件本身不等于启用其中的工作流。

## 3. 事实源优先级

- `AGENTS.md`：协作与安全规则。
- `.ai/ACTIVE_TASK.md`：唯一活动任务指针与挂起（Parked）索引，保持在 25 行以内，不重复抄写 `tasks.md` 进度。
- Active OpenSpec change：本轮范围、契约、设计与 `tasks.md` 进度唯一事实源。
- `openspec/specs/`：已接受的长期能力契约。
- 当前代码和配置：已实现事实；若与 spec 冲突，应记录为 gap，不得悄悄改写 spec 或伪称已实现。
- `.ai/GOTCHAS.md` 与 `.ai/AGENT_LOG.md`：跨会话避坑备忘与近期执行证据（历史分片封存于 `.ai/logs/`）。
- `docs/architecture/`、`docs/roadmap/`、`docs/optimization/`：当前说明、债务库存和完成证据；`history/` 仅供历史解释。

## 4. 任务分级

### A. 只读任务

代码审查、现状扫描、解释、诊断和建议不要求创建 OpenSpec change，也不修改 `.ai/ACTIVE_TASK.md`。只有产生值得长期保存的验证结论且用户未禁止写文件时才更新 `.ai/AGENT_LOG.md` 或 `.ai/GOTCHAS.md`；纯只读请求不因日志要求转为写入任务。

### B. 小范围维护

文案、注释、文档链接、低风险配置修正、已有契约内的小 bugfix 或治理维护，可以不创建 OpenSpec change，但必须：

- 明确范围；
- 做聚焦验证与 `DocAndScope` 门禁检查；
- 按第 9 节定额格式追加 `.ai/AGENT_LOG.md`（随同次修改一并提交）。

### C. 重大变更

满足任一条件时必须先创建 OpenSpec change，并将 `.ai/ACTIVE_TASK.md` 指向它：

- 新增用户可见能力或删除已有能力；
- 修改 API、DTO、持久化模型、状态机、权限语义或 provider 契约；
- 修改检索、分块、rerank、prompt、citation、no-answer 的指标口径或生产默认行为；
- 引入新基础设施、依赖、外部服务或付费调用；
- 跨模块重构或预计包含多个独立提交；
- 变更会使既有 baseline 不再可比较。

## 5. 工作流

1. 执行 `git status --short --branch`，保护用户已有改动。
2. 按第 2 节读取 `.ai/ACTIVE_TASK.md` 与 `.ai/GOTCHAS.md`，不得接管无关 active change。
3. 涉及能力或验收判断时，将相关能力分类为 `confirmed / partial / planned / out_of_scope / unknown`。
4. 提交责任与授权复用统一按第 7 节执行。
5. 重大变更先完善 proposal、design、tasks 和 spec delta，再写代码。
6. **写单焦点 + 读可并发**：一次只推进一个可验证的写切片，不顺手重构；在跨模块检索、长日志排障或事前探路时，可使用只读 Subagent 并发收集证据，主会话仅接收精炼结论。
7. 运行与风险相称的测试；不能验证时写清原因，不得假装通过。
8. 写入任务按第 9 节更新 `.ai/AGENT_LOG.md`（必要时提炼新陷阱入 `.ai/GOTCHAS.md`）；涉及 active change 时仅在对应 `tasks.md` 勾选进度，不在多处文档重复抄写。
9. 仅当整个 active change 验收完成（或经用户授权转入 `Parked` 挂起槽位 / 阶段性归档）后，才释放或切换 `.ai/ACTIVE_TASK.md` 活动指针。

## 6. RAG 专项规则

- 必须区分 retrieval、generation、citation、no-answer 和 judge 指标。
- 报告文件名不等于结论；必须检查 `Report status`、error count、retry、metadata 和 Git HEAD。
- `RETRIEVAL_ONLY` 不能证明生成质量，`PARTIAL` 不能当作干净 generation baseline。
- 不得为了评测集定制 prompt、切分或拒答规则。
- `--preflight-only` 只检查；`--keep-existing` 只复用；不得恢复为隐式建库行为。
- 批量 ask、judge、embedding、rerank 或其他外部调用前，说明调用量、模型、数据出站和费用/限流风险，并取得用户授权。同一已披露目标、载荷、模型、预算和本地副作用范围内复用已有授权；仅范围扩大、授权耗尽或平台审批明确要求时重新确认。
- 真实 provider 不可用时明确降级或跳过，不用 mock 结果宣称业务收益。

## 7. 安全与范围

- API key、JWT secret、数据库密码和用户数据不得写入 tracked files、日志或回复。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 等本地内容，除非用户明确授权。
- 不覆盖、回退或混入与当前任务无关的工作区改动。
- 沿用当前任务已有的提交责任和授权；用户授权 `Agent 提交` 后，才可暂存和提交计划内文件。未授权则默认用户手动提交，Agent 不暂存、不提交，只给建议 message。
- `push`、创建 PR、发布或部署始终需要单独明确授权，不因 `Agent 提交` 一并放开。
- 提交信息使用中文 Conventional Commit，例如：`docs(治理): 升级Vibecoding工作流至v2.0`。

## 8. 验证要求

- Java 改动：聚焦测试；风险较高时运行 `mvn -q test`。
- Python 评测脚本：运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- 前端改动：必须运行包含 `vue-tsc` 的正式 build；不能用单独 `vite build` 冒充通过。
- 文档/治理与收尾检查：运行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode DocAndScope`（自动执行 `git diff --check`、受保护路径检查、改动 Markdown 相对链接校验及热日志行数检查）。
- 外部集成：记录 provider、模型、超时、重试、错误类别和是否产生真实调用。
- Agent 已获授权直接运行现有项目的 Maven/npm/Python/PowerShell 本地验证与门禁命令（含正常依赖解析下载），无需逐次请示；该授权不包含新增/升级依赖、部署、push 或业务外部调用。
- 同一代码、配置、依赖和环境状态下已有的成功验证可复用；仅新增改动、失败或新风险触发重跑。
- 工具链不明时先运行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode Preflight`。

## 9. AGENT_LOG 与 GOTCHAS 规则

- **冷热分离（≤200 行滚动窗口）**：`.ai/AGENT_LOG.md` 只保留近期热记录（控制在 200 行以内，只追加不改写）。超过 200 行或完成大阶段收口时，将旧记录原封不动迁入 `.ai/logs/YYYY-MM-<stage>.md` 冷归档。
- **同次提交、零 Hash 补录**：日志随当次代码/文档改动在**同一个 commit** 中提交；**不写 `Commit: pending`，不追加“提交补录”章节，也不创建纯日志补录 commit**（Commit SHA 统一由 `git log` 承载）。
- **单条 6 行定额制**：禁止在日志中堆砌自然语言免责套话（由 `DocAndScope` 门禁替代），每条记录严格控制在 **5～8 行**，只包含：
  1. 标题：`## YYYY-MM-DD · <task-or-change-id>（类型/切片）`
  2. **改动与大白话**：修改的核心模块/文件数 + 一句话“改前坏事 → 改后不同”（及适用 `design.md` 决策号如 `D1–D3`）；
  3. **验证与门禁**：核心测试结果、`DocAndScope` 门禁输出，以及跳过项原因；
  4. **踩坑与风险**：本轮发现的反直觉陷阱（若有复用价值，同步提炼 1～2 行入 `.ai/GOTCHAS.md`）与剩余边界风险。
- 日志是执行证据，不是需求或设计事实源。

## 10. design.md 决策记录规则

每个 Type C change 的 `design.md` 必须包含一节 `## 决策记录`，在事前闸门随 design 一并呈交审查：

- 为本 change 中每一个存在真实岔路口的技术决策（含 `out_of_scope` 边界决策）各写一条；纯唯一解细节不必记。
- 每条固定三行结构，用大白话，禁止贴大段代码：
  1. **面临的选择**：列出当时真实存在的 2-3 个候选方案（含被否决的）。
  2. **选了哪个 + 为什么**：选定项与决定性理由（职责边界 / 一致性 / 风险 / 与既有基线兼容等）。
  3. **放弃的代价**：被否决方案各自的具体代价，即"若选它会发生什么糟糕的事"。
- 本节只写进 `design.md`，不写进 `.ai/AGENT_LOG.md`。完整模板见 `docs/workflow/prompt-snippets/design-decision-record.md`。

## 11. 完成输出

每轮有文件改动时，最终回复必须包含：

- 修改了什么；
- 验证与门禁结果（含 `DocAndScope` 状态）；
- 跳过的验证及原因；
- 剩余风险；
- 本轮提交状态或建议的中文提交信息。
