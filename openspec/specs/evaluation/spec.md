# Evaluation Specification

## Requirements

### Requirement: 可复现评测身份

正式 baseline SHALL 固定评测集、fixture、知识库身份、配置快照和 Git HEAD。

#### Scenario: 复跑 baseline

- GIVEN 相同 fixture、配置和代码
- WHEN 连续运行可复现评测
- THEN 报告记录相同的评测身份信息
- AND 客观 retrieval 指标应可直接比较

### Requirement: 只读复用语义

`--preflight-only` MUST 只检查现有资源；`--keep-existing` MUST 只复用现有知识库。目标不存在时 MUST 失败，不得隐式创建空知识库。

#### Scenario: 目标知识库缺失

- WHEN 使用 `--keep-existing` 指向不存在的固定知识库
- THEN runner 非零退出并给出恢复提示
- AND 不创建知识库或上传 fixture

### Requirement: 报告状态

评测报告 SHALL 区分 `CLEAN`、`PARTIAL`、`RETRIEVAL_ONLY` 和 `FAILED`，并记录 retrieve/ask/judge error、retry、rate limit 和 skipped 项。

#### Scenario: Ask 部分失败

- WHEN retrieval 完整但部分 ask 失败
- THEN 报告状态为 `PARTIAL`
- AND generation/citation 指标不得被表述为干净 baseline

### Requirement: 指标边界

Retrieval、generation、citation、no-answer 与 LLM judge 指标 MUST 分开解释。Citation snippet hit MUST NOT 被当作逐 claim faithfulness 的替代。

#### Scenario: Judge 关闭

- GIVEN `judge-mode=off`
- WHEN 评测完成
- THEN 客观指标可以报告
- BUT 不得宣称独立 faithfulness/relevance judge 已完成

### Requirement: 外部调用安全闸

批量 ask、judge、embedding 或 rerank 调用前 MUST 获得用户授权，并说明预计调用量、数据出站、模型、费用和限流风险。

#### Scenario: 未获授权

- WHEN 任务需要新的批量外部调用但尚未获得授权
- THEN 只允许 plan、静态检查、单元测试或不出站的预检
