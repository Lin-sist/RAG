# RAG 第一轮最小诊断闭环

## 1. 目标

- 目的：为 `docs/开发文档/plan.md` 的阶段零、阶段一和阶段二提供一套可重复执行的最小诊断起点。
- 范围：只覆盖首批 3 条样本，不做自动评分平台，不引入新的后端接口。
- 接口：统一使用同步 `POST /api/qa/ask`。
- 默认知识库：`kbId=2`，若本地环境不同，可通过脚本参数覆盖。

## 2. 首批样本与验收口径

| 编号 | 问题 | 期望要点 | 可接受回答标准 |
|------|------|------|------|
| Q1 | 什么是RAG | 说明 RAG = 检索 + 生成；回答基于外部知识而非纯模型记忆；能点出“先检索再生成”的流程 | 允许表述不同，但不能把 RAG 解释成单纯向量库或单纯大模型调用 |
| Q2 | 为什么需要向量化 | 说明文本向量化是为了做语义检索；能支持相似度匹配、召回相关 chunk；最好点出比关键词匹配更适合语义问题 | 允许不展开数学细节，但不能只回答“为了存数据库” |
| Q3 | 知识库没有该内容时应如何回答 | 明确拒答或保守回答策略；说明应告知未检索到足够依据，避免编造 | 允许措辞不同，但不能把“无结果”回答成确定事实 |

## 3. 固定采集指标

每轮至少记录以下字段：

- `contextCount`
- `removedByBudget`
- `citationCount`
- `answerLength`
- `retrievedTopScore`
- `retrievedAvgScore`

建议同时保留：

- `retrievedContextCount`
- `status`
- 最终 answer 文本
- citations 和 contexts 原始 JSON

## 4. 推荐执行方式

### 4.1 首轮快照

PowerShell:

```powershell
.\scripts\phase0_qa_snapshot.ps1 -KbId 2
```

Shell:

```bash
./scripts/phase0_qa_snapshot.sh
```

说明：

- 默认问题集来自 [test-data/qa-phase0-questions.txt](/C:/_01_Code/RAG/test-data/qa-phase0-questions.txt)
- 输出目录为 `diagnostics/qa-snapshots/<timestamp>/`
- 每个问题会生成一份原始 JSON，以及一份 `summary.csv`

### 4.2 参数网格实验

```powershell
.\scripts\qa_grid_experiment.ps1 -KbId 2 -TopKList "4,6,8" -MinScoreList "0.20,0.30,0.40"
```

说明：

- 输出目录为 `diagnostics/qa-grid/<timestamp>/`
- 每组参数都会生成一份明细 CSV
- 汇总文件为 `grid-summary.csv`

## 5. 结果记录模板

- 模板文件：[qa-first-round-record-template.md](/C:/_01_Code/RAG/diagnostics/templates/qa-first-round-record-template.md)
- 推荐做法：每次快照后复制一份模板，按本轮日期补全观察结论

## 6. 复用规则

- 第二轮开始前，优先复用同一批问题，避免样本漂移影响判断。
- 若知识库内容变动较大，可新增样本，但不要替换首批 3 条基础问题。
- 任何参数实验都应同时保留“参数组合 + 结果目录 + 主观结论”三项记录。

## 7. 第一轮落地说明（2026-04-22）

- 已确认样本文件存在：[qa-phase0-questions.txt](/C:/_01_Code/RAG/test-data/qa-phase0-questions.txt)
- 已确认快照脚本默认使用 `kbId=2`：[phase0_qa_snapshot.ps1](/C:/_01_Code/RAG/scripts/phase0_qa_snapshot.ps1)
- 已将网格实验脚本默认知识库修正为 `kbId=2`：[qa_grid_experiment.ps1](/C:/_01_Code/RAG/scripts/qa_grid_experiment.ps1)
- 当前闭环目标是“稳定采集 + 人工复盘”，暂不做自动评分
