## Plan: RAG回答质量优化（修订版）

先做根因诊断，再做基线、调参、代码精修和回归护栏。核心原则是先定位损失层，避免在错误层面反复调参。

## 当前状态（2026-04-22）

| 阶段 | 状态 | 当前结论 |
|------|------|------|
| 阶段零：根因快速诊断 | `PARTIAL` | 首批样本、同步 `ask` 诊断方式和默认 `kbId=2` 已固定，但完整根因快照还需继续积累 |
| 阶段一：建立可量化基线 | `PARTIAL` | 关键元数据字段已在接口和脚本中可采集，首轮样本与记录模板已补齐 |
| 阶段二：低风险参数调优 | `PARTIAL` | `qa_grid_experiment.ps1` 已可复用，但参数实验结论还未沉淀 |
| 阶段三：小规模代码优化 | `PARTIAL` | 已完成文档去重语义修复、聊天页流式来源降级与上下文展示，引用提取和分块边界优化待后续继续 |
| 阶段四：质量护栏与回归 | `TODO` | 还没有正式回归门禁与阈值阻断机制 |

## 第一轮已落地项（2026-04-22）

- 文档处理去重语义已统一为“同知识库重复上传返回 `isNew=false`、空 `chunks`、保留 `contentHash`”，并恢复后端测试基线。
- 聊天页已区分同步回答和流式回答；同步回答继续展示 `citations + contexts`，流式回答在无结构化来源时展示显式降级提示。
- 第一轮最小诊断闭环已补齐：
  - 样本文件：[qa-phase0-questions.txt](/C:/_01_Code/RAG/test-data/qa-phase0-questions.txt)
  - 快照脚本：[phase0_qa_snapshot.ps1](/C:/_01_Code/RAG/scripts/phase0_qa_snapshot.ps1)
  - 网格实验脚本：[qa_grid_experiment.ps1](/C:/_01_Code/RAG/scripts/qa_grid_experiment.ps1)
  - 诊断说明：[qa-first-round-diagnostics.md](/C:/_01_Code/RAG/docs/开发文档/qa-first-round-diagnostics.md)
  - 结果模板：[qa-first-round-record-template.md](/C:/_01_Code/RAG/diagnostics/templates/qa-first-round-record-template.md)

**Steps**
1. 阶段零：根因快速诊断（0.5天）
2. 选2-3个低质量问答样例，逐层保留链路快照：原始问题、检索chunk及分数、最终Prompt、模型原始输出、前端最终渲染。
3. 逐层判定首个失真点：检索侧、提示侧、生成侧或流式渲染侧，并据此确定阶段二和阶段三的优先次序。
4. 产出根因诊断记录，明确主要损失层与次要损失层，作为后续改动排序依据。*后续步骤依赖本步*
5. 阶段一：建立可量化基线（0.5天）
6. 构建10-20条分层评测集，覆盖事实性、推理性、多跳和边缘拒答问题，每条样本记录期望要点、贴原文要求、可接受长度。
7. 固化评分口径：事实一致性、原文贴合度、回答完整度、可读性，保证人工评分可重复。
8. 采集辅助指标：contextCount、removedByBudget、citationCount、answerLength、avgChunkScore，用于判断损失发生层。
9. 阶段二：低风险参数调优（1天）
10. 先审查Query预处理（同义词扩展、复合问题拆分改写）并按阶段零结论决定优先级。
11. 开展检索参数网格实验：minScore（0.45/0.55/0.65）与topK（4/6/8），记录噪声率、漏召回率与指标变化。*依赖步骤6-8*
12. 开展提示与生成参数实验：context budget（1200/1600/1800）、max tokens（2048/3000/3500）、temperature（0.2/0.4/0.6），并同步审查Prompt措辞是否明确要求完整回答。*可与步骤11并行*
13. 收敛第一版生产参数，以不断句、不跑偏、可读性提升且事实一致性不下降为目标。*依赖步骤11-12*
14. 阶段三：小规模代码优化（2-3天）
15. 先做流式拼接鲁棒性优化，统一data行解析、空片段、结束信号与异常处理，完成连续压测。*最高优先级*
16. 再做引用提取优化，将关键词硬匹配升级为句级相似+多候选，提升引用覆盖与贴题度。
17. 最后做分块边界调整，采用语义边界优先和overlap策略，避免长句/表格/代码块中间截断；该项需独立上线并重建索引。*高风险项*
18. 阶段四：质量护栏与回归（1天）
19. 建立固定评测集回归脚本，每次改动后输出四项评分变化和退化项。
20. 建立合入阈值：事实一致性与完整度低于基线阈值时阻止合入。
21. 记录参数版本快照和评分结果，保留最近3个稳定版本，支持快速回滚。

**Relevant files**
- c:/_01_Code/RAG/rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java — 问答入口与辅助指标采集点
- c:/_01_Code/RAG/rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGService.java — 检索到生成主链路与参数透传
- c:/_01_Code/RAG/rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java — 上下文预算与Prompt措辞
- c:/_01_Code/RAG/rag-core/src/main/java/com/enterprise/rag/core/rag/model/RetrieveOptions.java — 检索阈值与topK默认值
- c:/_01_Code/RAG/rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java — 引用提取与流式解析
- c:/_01_Code/RAG/rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunker.java — 分块边界策略
- c:/_01_Code/RAG/rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java — Query预处理
- c:/_01_Code/RAG/rag-admin/src/main/resources/application.yml — 生成与检索关键参数
- c:/_01_Code/RAG/rag-frontend/src/composables/useSSE.ts — 前端流式拼接
- c:/_01_Code/RAG/docs/开发文档/qa-first-round-diagnostics.md — 第一轮诊断样本、执行方式与记录口径
- c:/_01_Code/RAG/diagnostics/templates/qa-first-round-record-template.md — 诊断结果记录模板

**Verification**
1. 阶段零完成标准：至少2条问题链路快照齐全，并明确首个失真层。
2. 阶段一完成标准：评测集、评分口径、辅助指标三件套齐备。
3. 阶段二完成标准：完成参数实验记录并明确推荐参数组合。
4. 阶段三完成标准：流式、引用、分块改动均通过专项回归且无关键指标退化。
5. 阶段四完成标准：形成可重复执行的回归与阈值门禁机制。

**Decisions**
- 优先目标：提升可读性，不牺牲准确性与原文贴合。
- 可接受轻微延迟上升换取更高质量。
- 范围包括：配置调优与小规模代码优化。
- 根因诊断前置为强制步骤。
- Query预处理与Prompt措辞审查纳入第二阶段。
- 暂不纳入：更换向量库、训练新模型、重写架构。

- 阶段零首轮样本已确定："什么是RAG"、"为什么需要向量化"、"知识库没有该内容时应如何回答"。
- 阶段零首轮接口模式：先使用同步 ask 接口做主诊断。
- 阶段零首轮目标知识库：ID=2，名称=RAG知识库。

**Further Considerations**
1. 若阶段二后仍不稳定，再评估引入reranker作为后续阶段。
