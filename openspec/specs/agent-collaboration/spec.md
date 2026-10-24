# Agent Collaboration Specification

## Requirements

协作规则以仓库根目录 `AGENTS.md` 为准；本规格描述可观察的协作行为，不重复定义操作模板。

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

重大变更 MUST 按 `AGENTS.md` 的风险与契约条件判定，并在写代码前具备经确认的 proposal、design、tasks 和必要的 spec delta；MUST NOT 仅因拆成多个提交而升级任务类型。

#### Scenario: 契约发生变化

- WHEN 变更 API、DTO、持久化、权限或 RAG 指标口径
- THEN 先更新并确认 OpenSpec artifacts
- AND 后续实现不得超出明确 scope

#### Scenario: 执行明确的小范围维护

- WHEN 用户已明确要求既有契约内的 Type B 小修，且范围清楚、授权充分
- THEN Agent 简述范围后执行，不创建空 change 或额外等待计划确认
- AND 无提交授权时保留改动并给建议 message，不为选择提交方式再次打断

#### Scenario: 连续完成已批准的实现切片

- WHEN Type C 设计已批准，下一切片仍在同一授权范围内
- THEN Agent 继续实现与适用验证，不因切片、轮次或提交数量变化而重复确认
- AND 仅在范围不清、风险升级、授权不足或用户要求逐段审查时暂停受影响部分

### Requirement: 精益执行证据与冷热分离

文件修改任务 SHALL 按可验证切片在 `.ai/AGENT_LOG.md` 简短记录改动原因、验证结果、跳过原因和新增风险，不按对话轮次重复追加或为物理行数凑内容；获授权提交时与对应改动同次落盘，不维护 hash 补录。热窗口超过 200 行或阶段收口时 SHALL 无损归档；可复用的反直觉教训才写入 `.ai/GOTCHAS.md`。

#### Scenario: 验证受阻

- WHEN 测试因环境或凭据不可用而跳过
- THEN 日志记录具体阻塞和已完成的替代验证
- AND 不得写成验证通过

#### Scenario: 日志冷热分离

- WHEN `.ai/AGENT_LOG.md` 累计超过 200 行或完成 Wave 阶段收口
- THEN 历史记录无损迁入 `.ai/logs/` 下按阶段命名的归档文件
- AND `.ai/AGENT_LOG.md` 保持轻量热窗口以供后续会话直接读取

#### Scenario: 维护任务完成后记录证据

- WHEN 执行结果已记录在日志或专项验收文件
- THEN 其他文档链接引用证据，路线图只更新相关里程碑或债务状态
- AND 项目定位、能力边界与核心配置未变时，不为每次小修或测试计数更新 `project.md`

### Requirement: 费曼人话可读性

Agent SHALL 使用完整中文解释改动原因与取舍，MUST NOT 输出省略因果和主谓宾的缩写。Type C 的事前闸门与完成输出 SHALL 先提供 `### 💡 30秒人话导读`，Type B 可用简短段落；比喻按需使用，重要决策用三问，简单选择一句理由即可。

#### Scenario: 汇报技术根因或设计决策

- WHEN Agent 向用户提交事前闸门计划、完成汇报或编写 `design.md` 决策记录
- THEN 先讲清原因、改法和结果，涉及重要取舍时说明替代方案的代价
- AND 随后再附上精确的文件路径、行号与测试门禁数据

### Requirement: 保护用户工作区

Agent MUST 保护与当前任务无关的已有改动，不得擅自回退、删除、暂存或提交。

#### Scenario: 工作区存在无关改动

- WHEN `git status` 显示与当前任务无关的修改
- THEN Agent 将其排除在 scope 外
- AND 可以安全隔离时继续工作，不要求用户先清空工作区
- AND 无法安全隔离时向用户请示

#### Scenario: 当前任务需要回退

- WHEN 本轮修改出现问题
- THEN Agent 优先定向修复或撤销本轮改动，不把整体 reset/restore 作为默认操作
- AND 涉及用户改动或历史重写时，先明确影响并获授权；HEAD 锚点不视为未提交内容的备份

### Requirement: 用户控制提交

Agent MUST NOT 自动 stage、commit 或 push，除非用户明确授权；提交信息 SHALL 使用中文 Conventional Commit，且执行日志随对应改动在同一次 commit 中提交，不产生独立的纯日志 hash 补录提交。

#### Scenario: 完成但未获提交授权

- WHEN 改动与验证完成
- THEN Agent 保留工作区改动并提供建议提交信息
