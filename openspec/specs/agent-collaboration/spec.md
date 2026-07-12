# Agent Collaboration Specification

## Requirements

### Requirement: 唯一活动任务

`.ai/ACTIVE_TASK.md` SHALL 是唯一活动任务指针，同时最多指向一个未归档 OpenSpec change。

#### Scenario: 开始重大变更

- WHEN Agent 准备实施重大变更
- THEN active task 状态为 `ACTIVE`
- AND 指向存在的 `openspec/changes/<change-id>/`

### Requirement: 重大变更先规格后实现

重大变更 MUST 在写代码前具备 proposal、design、tasks 和必要的 spec delta。

#### Scenario: 契约发生变化

- WHEN 变更 API、DTO、持久化、权限或 RAG 指标口径
- THEN 先更新并确认 OpenSpec artifacts
- AND 后续实现不得超出明确 scope

### Requirement: 执行证据

所有文件修改任务 SHALL 将范围、修改、验证、跳过原因、风险和 commit 状态追加到 `.ai/AGENT_LOG.md`。

#### Scenario: 验证受阻

- WHEN 测试因环境或凭据不可用而跳过
- THEN 日志记录具体阻塞和已完成的替代验证
- AND 不得写成验证通过

### Requirement: 保护用户工作区

Agent MUST 保护与当前任务无关的已有改动，不得擅自回退、删除、暂存或提交。

#### Scenario: 工作区存在无关改动

- WHEN `git status` 显示与当前任务无关的修改
- THEN Agent 将其排除在 scope 外
- AND 无法安全隔离时向用户请示

### Requirement: 用户控制提交

Agent MUST NOT 自动 stage、commit 或 push，除非用户明确授权；提交信息 SHALL 使用中文 Conventional Commit。

#### Scenario: 完成但未获提交授权

- WHEN 改动与验证完成
- THEN Agent 保留工作区改动并提供建议提交信息
