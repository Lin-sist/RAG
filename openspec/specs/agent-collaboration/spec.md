# Agent Collaboration Specification

## Requirements

### Requirement: 唯一活动任务与挂起隔离

`.ai/ACTIVE_TASK.md` SHALL 是唯一活动任务指针，同时最多指向一个处于 `ACTIVE` 状态的未归档 OpenSpec change；因外部依赖或人工验收阻塞需暂存的任务 SHALL 记录在 `Parked` 槽位或证据债务表中，并保持文件整体精炼（不超过 25 行）。

#### Scenario: 开始重大变更

- WHEN Agent 准备实施重大变更
- THEN active task 状态为 `ACTIVE`
- AND 指向存在的 `openspec/changes/<change-id>/`
- AND 具体子任务进度仅在对应 `tasks.md` 中维护，不在 `ACTIVE_TASK.md` 重复抄写

#### Scenario: 外部阻塞时挂起切换

- WHEN 当前 change 因外部 API 限流、超时或等待人工验收而暂停，且用户授权切换任务
- THEN Agent 将该任务登记入 `Parked` 或移交证据债务
- AND 释放唯一 `ACTIVE` 写焦点后再启动下一任务

### Requirement: 重大变更先规格后实现

重大变更 MUST 在写代码前具备 proposal、design、tasks 和必要的 spec delta。

#### Scenario: 契约发生变化

- WHEN 变更 API、DTO、持久化、权限或 RAG 指标口径
- THEN 先更新并确认 OpenSpec artifacts
- AND 后续实现不得超出明确 scope

### Requirement: 精益执行证据与冷热分离

所有文件修改任务 SHALL 按定额格式将改动摘要（含改前/改后大白话）、验证与门禁结果、跳过原因、反直觉踩坑及剩余风险追加到 `.ai/AGENT_LOG.md` 滚动热窗口（随同次提交落盘，不维护手写 commit hash 补录），并在热窗口超过 200 行或阶段收口时分片归档至 `.ai/logs/`；具有跨会话复用价值的非显而易见工程陷阱 SHALL 提炼入 `.ai/GOTCHAS.md`。

#### Scenario: 验证受阻

- WHEN 测试因环境或凭据不可用而跳过
- THEN 日志记录具体阻塞和已完成的替代验证
- AND 不得写成验证通过

#### Scenario: 日志冷热分离

- WHEN `.ai/AGENT_LOG.md` 累计超过 200 行或完成 Wave 阶段收口
- THEN 历史记录无损迁入 `.ai/logs/` 下按阶段命名的归档文件
- AND `.ai/AGENT_LOG.md` 保持轻量热窗口以供后续会话直接读取

### Requirement: 费曼人话可读性

Agent 在事前闸门、完成输出、`design.md`（含设计主旨与决策记录）及 `.ai/AGENT_LOG.md` 中 SHALL 使用主谓宾完整、带直观场景或比喻的通透中文解释改动原因与取舍，MUST NOT 输出删掉因果逻辑与主谓宾的“机器电报体”缩写；事前闸门与完成输出开头 SHALL 先提供 `### 💡 30秒人话导读`。

#### Scenario: 汇报技术根因或设计决策

- WHEN Agent 向用户提交事前闸门计划、完成汇报或编写 `design.md` 决策记录
- THEN 先用通俗直觉或生活化比喻讲清“原来哪里卡住、核心怎么改、不选其他方案会踩什么坑”
- AND 随后再附上精确的文件路径、行号与测试门禁数据

### Requirement: 保护用户工作区

Agent MUST 保护与当前任务无关的已有改动，不得擅自回退、删除、暂存或提交。

#### Scenario: 工作区存在无关改动

- WHEN `git status` 显示与当前任务无关的修改
- THEN Agent 将其排除在 scope 外
- AND 无法安全隔离时向用户请示

### Requirement: 用户控制提交

Agent MUST NOT 自动 stage、commit 或 push，除非用户明确授权；提交信息 SHALL 使用中文 Conventional Commit，且执行日志随对应改动在同一次 commit 中提交，不产生独立的纯日志 hash 补录提交。

#### Scenario: 完成但未获提交授权

- WHEN 改动与验证完成
- THEN Agent 保留工作区改动并提供建议提交信息
