# Agent Collaboration Spec Delta

## ADDED Requirements

### Requirement: 统一治理入口

仓库 SHALL 使用 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md` 和 OpenSpec 分别承载协作规则、活动任务、执行证据和规格变更。

#### Scenario: Agent 开始新任务

- WHEN Agent 接收仓库任务
- THEN 先读取 `AGENTS.md` 和 `.ai/ACTIVE_TASK.md`
- AND 按任务分级决定是否需要 OpenSpec change

### Requirement: 单一 Agent 指令源

工具专属指令文件 SHALL 指向根目录 `AGENTS.md`，不得独立维护一套冲突的项目范围和开发规则。

#### Scenario: Copilot 参与项目

- WHEN GitHub Copilot 读取仓库指令
- THEN 它被要求遵守 `AGENTS.md`、active task 和 active OpenSpec change
