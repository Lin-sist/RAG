# AGENTS.md

## 1. 项目与语言

- 项目：Enterprise RAG QA System。
- 默认使用简体中文与用户沟通，代码标识符、命令和协议字段保留原文。
- 当前定位：可运行、可评测的模块化 RAG 工程原型，不得包装成生产级多租户系统或已完成的 Agentic RAG 平台。

## 2. 必读顺序

开始任何仓库任务前，按顺序读取：

1. `AGENTS.md`
2. `.ai/ACTIVE_TASK.md`
3. 若存在 active change：读取对应 `openspec/changes/<change-id>/` 下的 proposal、design、tasks 和 spec delta
4. `openspec/project.md` 与相关 baseline spec
5. 当前任务直接涉及的代码、配置和测试
6. 必要时再读 `docs/architecture/overview.md`、`docs/roadmap/technical-debt.md`、`docs/optimization/README.md`

不要先读历史报告或 `docs/optimization/history/` 再判断当前状态。

## 3. 事实源优先级

- `AGENTS.md`：协作与安全规则。
- `.ai/ACTIVE_TASK.md`：唯一活动任务指针，不承载长期设计。
- Active OpenSpec change：本轮范围、契约、设计与验收标准。
- `openspec/specs/`：已接受的长期能力契约。
- 当前代码和配置：已实现事实；若与 spec 冲突，应记录为 gap，不得悄悄改写 spec 或伪称已实现。
- `docs/architecture/`、`docs/roadmap/`、`docs/optimization/`：当前说明、债务库存和完成证据。
- `docs/optimization/history/`：仅供历史解释。

## 4. 任务分级

### A. 只读任务

代码审查、现状扫描、解释、诊断和建议不要求创建 OpenSpec change，也不修改 `.ai/ACTIVE_TASK.md`。只有产生值得长期保存的验证结论时才追加 AGENT_LOG。

### B. 小范围维护

文案、注释、文档链接、低风险配置修正、已有契约内的小 bugfix，可以不创建 OpenSpec change，但必须：

- 明确范围；
- 做聚焦验证；
- 将文件改动、验证和剩余风险追加到 `.ai/AGENT_LOG.md`。

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
2. 从 `.ai/ACTIVE_TASK.md` 确认当前是否有 active change。
3. 将相关能力分类为 `confirmed / partial / planned / out_of_scope / unknown`。
4. 事前闸门必须明确提交责任：`Agent 提交`或`用户手动提交`；未明确时默认 Agent 不暂存、不提交。
5. 重大变更先完善 proposal、design、tasks 和 spec delta，再写代码。
6. 一次只推进一个可验证切片，不顺手重构。
7. 运行与风险相称的测试；不能验证时写清原因，不得假装通过。
8. 更新 tasks 和 `.ai/AGENT_LOG.md`。
9. 完成后将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`；OpenSpec change 经用户确认后再归档。

## 6. RAG 专项规则

- 必须区分 retrieval、generation、citation、no-answer 和 judge 指标。
- 报告文件名不等于结论；必须检查 `Report status`、error count、retry、metadata 和 Git HEAD。
- `RETRIEVAL_ONLY` 不能证明生成质量，`PARTIAL` 不能当作干净 generation baseline。
- 不得为了评测集定制 prompt、切分或拒答规则。
- `--preflight-only` 只检查；`--keep-existing` 只复用；不得恢复为隐式建库行为。
- 批量 ask、judge、embedding、rerank 或其他外部调用前，说明调用量、模型、数据出站和费用/限流风险，并取得用户授权。
- 真实 provider 不可用时明确降级或跳过，不用 mock 结果宣称业务收益。

## 7. 安全与范围

- API key、JWT secret、数据库密码和用户数据不得写入 tracked files、日志或回复。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 等本地内容，除非用户明确授权。
- 不覆盖、回退或混入与当前任务无关的工作区改动。
- 每个任务必须在事前闸门明确提交责任；用户授权 `Agent 提交` 后，Agent 才可暂存和提交计划内文件。未明确时默认由用户手动提交，Agent 只给建议 message。
- `push`、创建 PR、发布或部署始终需要单独明确授权，不因 `Agent 提交` 一并放开。
- 提交信息使用中文 Conventional Commit，例如：`docs(治理): 建立Agent协作与OpenSpec入口`。

## 8. 验证要求

- Java 改动：聚焦测试；风险较高时运行 `mvn -q test`。
- Python 评测脚本：运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- 前端改动：必须运行包含 `vue-tsc` 的正式 build；不能用单独 `vite build` 冒充通过。
- 文档/治理改动：扫描旧路径与断链，运行 `git diff --check`。
- 外部集成：记录 provider、模型、超时、重试、错误类别和是否产生真实调用。
- Agent 已获授权直接运行现有项目的 Maven/npm 验证命令，包括 compile、test、package、build、lint，以及解析 `pom.xml`、`package-lock.json` 等已声明版本所需的正常依赖下载；无需逐次请示。
- 上述授权不包含新增或升级依赖、`npm publish`、部署、push，也不包含 provider、embedding、rerank、ask、judge 等业务外部调用；这些动作仍按原规则单独授权。
- 工具链不明时先运行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode Preflight`；它只报告环境，不安装工具、不作为阻塞门禁。

## 9. AGENT_LOG 规则

`.ai/AGENT_LOG.md` 只追加，不改写历史。每条记录至少包含：

- 日期与任务/change id；
- 范围与修改文件；
- 已确认事实和关键决策；
- 验证命令与结果；
- 跳过项及原因；
- 剩余风险；
- 执行记录随实现写入，尚未提交时写 `Commit: pending`；
- 提交完成后，在下一次仓库写操作开始时只追加一条提交补录，写入上一执行提交的真实 hash，不回改历史记录；
- 若要求立即闭环，可创建独立的纯日志补录提交；该补录提交只承载上一提交的 hash，不递归记录自身 hash。

日志是执行证据，不是需求或设计事实源。

## 10. 完成输出

每轮有文件改动时，最终回复必须包含：

- 修改了什么；
- 验证结果；
- 跳过的验证及原因；
- 范围安全检查；
- 剩余风险；
- 建议的中文提交信息。
