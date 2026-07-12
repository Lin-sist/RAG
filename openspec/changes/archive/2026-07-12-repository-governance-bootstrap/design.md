# Design: Repository Governance Bootstrap

## 决策

### 1. 分离四类职责

- `AGENTS.md`：稳定协作规则。
- `.ai/ACTIVE_TASK.md`：唯一活动 change 指针。
- `.ai/AGENT_LOG.md`：append-only 执行证据。
- OpenSpec：长期契约与重大变更 artifacts。

这样可避免把任务状态、设计、日志和 Agent 提示全部塞进同一个文件。

### 2. OpenSpec 只用于重大变更

只读任务和小修不强制创建 change，避免治理成本超过修改本身。涉及能力、契约、数据模型、指标口径、外部服务或跨模块实现时必须使用 change。

### 3. Code 与 Spec 冲突按 gap 处理

Accepted spec 表示目标契约，代码表示实现事实。冲突时记录差异并决定修代码或显式修 spec，不允许无声选择。

### 4. 手工初始化

当前环境未发现 `openspec` CLI。本次使用标准目录和 Markdown/YAML 手工初始化；CLI 安装属于单独任务。

## 风险

- 初始规则可能偏严格：通过首个真实 change 复盘并小步调整。
- 没有 CLI schema validation：使用引用扫描、结构检查和人工规范检查兜底。
- AGENT_LOG 可能膨胀：只记录文件修改任务和重要验证，普通问答不记账。
