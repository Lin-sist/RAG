# RAG 项目优化实施文档 v4（已关闭的计划快照）

> 关闭状态（2026-07-14）：v4 以“部分完成”关闭。Stage 1 已完成并形成两轮 CLEAN objective baseline；Stage 2 因无真实 rerank provider/凭据按条件规则跳过；Stage 4 文档真相源清理已完成；Stage 3 转入 `docs/roadmap/technical-debt.md` 的 P1 分块结构专项，未来另行立项；不再补写 v4 最终总报告。本文件仅保留历史计划与决策证据，不再作为执行入口。

## 0. 全局约束（最高优先级，违反即视为失败）

1. **承接现状**：Modular RAG 已成型，v3 检索质量工程第一轮已完成。已落地：
   BM25 keyword route + vector route + RRF 混合检索(`QueryEngineImpl.java:112`)，默认开启
   hybrid/keyword(`application.yml:144`)，分块 chunk-size=420 / overlap=80(`application.yml:167`)，
   Stage 2 最优 retrieval-only 指标 Recall@5 68.63% / MRR 0.7346 / Top1 96.30%
   (`docs/optimization/v3/stage2-chunking.md:26`)；Reranker 工程接入完成(ModelReranker 为 HTTP
   adapter，含健康检查/超时/降级，`RerankerRegistry.java:22`)，但**默认仍是 heuristic，真实
   provider 未配置，业务收益未验证**(`docs/optimization/v3/stage3-rerank-adapter.md:3`)。
2. **本轮核心缺口**：检索侧已被量化得很干净，但**生成/引用侧仍是黑盒**——答案忠实度、引用命中率、
   no-answer 稳定性都没有对等的评测尺子。本轮首要目标就是补齐这条链路的可评测性。
3. **指标是证据，不是保证**：所有指标（检索类 + 新增生成/引用类）仅用于举证，**不得**作为
   "必须上涨"的硬门槛。每阶段如实报告 提升/持平/回退；若回退，分析原因并决定是否回滚。
   **严禁为迎合文档措辞而过拟合评测集**（针对样本特调、把 fixture 写进 prompt/切分规则等一律禁止）。
4. **不准破坏现有主链路与既有收益**：上传→解析→切分→Embedding→Milvus→检索(hybrid+RRF)→
   rerank 抽象→LLM(SSE) 必须始终可用；本轮**不准改动分块参数(420/80)与 RRF 融合逻辑**，
   以免污染已稳定的检索指标。每阶段结束保证 `mvn test` 通过、服务能启动。
5. **范围按能力边界锁定**：本轮只允许围绕"生成/引用质量评测、真实 rerank 对比验证、分块质量专项、
   文档真相源清理"四块改动。为实现这些**允许**新增评测脚本/评测集字段/配置类/测试类、
   按需改 pom 与 application.yml。
   **严禁碰**：鉴权(SecurityConfig/JWT)、前端、向量库替换、关键词索引生命周期重构、
   已稳定的分块参数与 RRF 逻辑、与本轮无关的重命名或重构。任何"顺手优化"一律不做。
6. **每阶段中文提交 + 异常请示**（本轮授权按 /goal 自动推进）：每完成一个 Stage 执行一次
   git commit，**commit message 用中文**，格式见第 5 节。必须遵守安全闸：
   - 提交**前**检查工作区是否有与本任务无关的未提交改动；若有，**停下来向用户请示**，不得混入提交。
   - 每次提交**只 add 与当前 Stage 相关的文件**，禁止 `git add .` 全量提交。
   - 每次提交都必须可编译、可运行、`mvn test` 通过，禁止提交半坏代码。
   - 遇任何异常（评测无法出数、LLM-judge 不稳定、依赖/凭据缺失、改动可能超范围、指标大幅回退
     且无法解释），**暂停并向用户请示**，不要自行扩大范围或硬推。
7. **诚实标注 + 改动留痕**：环境限制无法验证的项明确写"未验证/待验证"；每阶段在
   `docs/optimization/` 追加说明（改了什么、为什么、验收数字、是否回滚）。
   **复用 v3 的可复现评测闭环**（固定评测 KB 身份、记录 kbId/chunk 数/时间戳），禁止 kbId 漂移复发。
8. **执行前事实核验（必须先做，不得跳过）**：
   - 先执行 `git status --short --branch`，记录当前分支与 HEAD。
     若出现与当前任务无关的未提交改动，暂停并向用户请示。
   - 重新核对关键事实源：`docs/optimization/v3/summary.md`、`v3/stage1-reproducible-eval.md`、
     `v3/stage2-chunking.md`、`v3/stage3-rerank-adapter.md`、`scripts/run_rag_eval.py`、`scripts/run_reproducible_rag_eval.py`，
     以及 `QueryEngineImpl`、`RerankerRegistry`、`ModelReranker`、`DocumentChunker`、`application.yml`。
     若本文档与代码/已提交报告不一致，以代码和 `docs/optimization/` 为准，先记录差异并请示。
   - 不得信任旧 `docs/后端优化文档/`、README 或历史 audit 的阶段判断；它们只能作为历史材料。
   - 不得只看报告文件名下结论；比较前必须读取报告头的 `Report status`、`retrieveErrors`、
     `askErrors`、`rateLimitErrors`、run metadata/details JSON。
9. **v4 文档归属**：本文件位于 `docs/optimization/v4/plan.md`，是已关闭的 v4 阶段计划快照；当前目录索引见
   `docs/optimization/README.md`。新的未完成 change 必须进入 OpenSpec，不得从本文件继续执行或新增平行总计划。

## 1. 本轮优化目标（明确、不可跑偏）

给"生成/引用"这条尚未量化的链路建立与检索侧对等的干净评测闭环，让项目第一次拥有
**"检索 + 生成"双可量化质量线**；在具备外部凭据时验证真实 rerank 的业务收益；
限时精进分块质量；并清理会误导后续 agent 的过时文档真相源。

**本轮做以下四件事，按顺序执行（Stage 2 为条件触发）：**
- Stage 1：generation / citation 质量评测闭环（本轮主线，必做）
- Stage 2：真实 rerank provider 接入并跑对比评测（**有凭据才触发，无凭据自动跳过并标注**）
- Stage 3：分块质量专项（标题感知切分 + 长代码块/长段落处理，**限时、见好就收**）
- Stage 4：清理文档真相源（整理 `docs/后端优化文档/`，消除过时结论）

**明确不做**：Agentic 循环、换向量库、关键词索引生命周期重构、前端 SSE 来源体验、nDCG、动鉴权、
改动已稳定的 420/80 分块参数与 RRF 融合逻辑。

## 2. Stage 1：generation / citation 质量评测闭环（主线，必做）

### 现状
当前评测脚本 `scripts/run_rag_eval.py` 主要产出**检索类**指标(Recall@k/MRR/Top1 source accuracy)；
答案忠实度、引用命中、no-answer 稳定性**没有对等评测**。评测集 `docs/eval/rag_eval_set.jsonl`
已覆盖 fact/definition/reasoning/multi_hop/no_answer 五类，正好复用。

### 目标
在现有评测闭环基础上扩出 **answer 维度评测**，让"答案质量"可量化、可复现、可回归。

### 实施约束
1. 在现有脚本体系内扩展一条 **generation 评测通道**（调用 `/api/qa/ask` 或等价问答接口，
   拿到答案 + 引用来源），与 retrieval-only 通道并存、互不干扰。**不准修改 retrieval-only
   的指标口径与既有评测集内容**；如需新增字段（如 expected_answer / must_cite_source），
   以**向后兼容**方式追加，旧字段与旧报告仍可跑。
2. 至少产出以下三类指标，并写清定义：
   - **引用命中率(citation hit)**：答案引用的 source 是否落在该问题 ground-truth 命中集合内。
   - **答案质量(faithfulness / relevance)**：用 **LLM-as-judge**（复用现有 DeepSeek/OpenAI-compatible
     调用风格），judge 用固定 prompt + 固定温度(尽量低)，判定答案是否忠实于检索到的上下文、是否相关。
   - **no-answer 正确率**：no_answer 类问题上，系统是否正确拒答/未编造。
3. **LLM-judge 可复现性**：固定 judge 模型、温度、prompt 模板；报告头记录 judge 配置。
   judge 结果要可抽样人工复核（保留每题的 judge 原始判定，便于核对），**严禁把 judge 调成
   "总是通过"或针对样本特调**。judge 服务不可用时，明确降级为"仅出 citation/no-answer 客观指标 +
   标注 faithfulness 待验证"，不得伪造分数。
4. **外部调用安全闸**：LLM-judge、`/api/qa/ask` 批量评测、真实 provider 调用都可能触发网络、限流或成本。
   未经用户确认前，只允许先做 CLI wiring、单元测试和小样本 smoke；批量 judge 前必须说明预计调用量、
   使用模型、是否会产生费用，并获得用户确认。报告必须记录 `askErrors`、`rateLimitErrors`、
   `judgeErrors`、retry 次数和跳过原因。
5. 沿用 v3 可复现闭环：固定评测 KB、记录 kbId/chunk 数/时间戳/配置快照。

### 验收标准
- 同一配置连续跑两次，客观指标(citation hit / no-answer)一致；LLM-judge 类指标差异在可解释浮动内，
  在 `docs/optimization/v4/stage1-generation-citation.md` 贴出两次结果 + judge 配置 + 若干抽样判定。
- 产出当前系统的**首份生成/引用质量基线**（citation hit、faithfulness、no-answer 正确率），
  如实报告，不与任何目标数字挂钩。

### ✅ Stage 1 完成后提交（先过安全闸，勿混入无关改动）：
`git commit -m "test(评测): 新增生成与引用质量评测通道，产出忠实度/引用命中/拒答首份基线"`

## 3. Stage 2：真实 rerank provider 接入并对比（条件触发）

### 触发条件
**仅当具备可用的 rerank 服务/凭据（在线 rerank API 或本地 BGE reranker 服务）时执行本阶段；
若无凭据或服务不可达，跳过本阶段，在 `docs/optimization/v4/stage2-rerank-decision.md` 明确写
"真实 rerank 待验证：缺 provider/凭据"，并继续 Stage 3。**

### 现状
`ModelReranker` 已是含健康检查/超时/降级的 HTTP adapter，但默认 heuristic，真实收益未验证
(`docs/optimization/v3/stage3-rerank-adapter.md:3`、`RerankerRegistry.java:22`)。

### 目标
配好真实 rerank provider，量化 heuristic vs 真实 rerank 的收益（检索 + 生成双维度）。

### 实施约束
1. 通过 `retrieval.rerank.provider` 显式配置凭据后启用 ModelReranker，候选 topN=20、最终 topK=5
   （沿用现值）；**不准改分块参数与 RRF 融合逻辑**，保证变量只有 rerank。
2. 保留 heuristic 兜底与降级链路；provider 异常必须降级不崩、日志告警。
3. 跑对比：heuristic vs 真实 rerank，各出一次 retrieval-only（Recall@5/MRR/Top1）+ Stage 1 的
   generation/citation 指标。

### 验收标准
- 在 `v4/stage2-rerank-decision.md` 如实报告两组对比与涨/平/跌；若真实 rerank 未带来收益甚至回退，
  照实写并给出"是否默认启用/维持 heuristic"的建议，**暂停并向用户请示**是否切换默认 provider。

### ✅ Stage 2 完成后提交（若被跳过则不产生本提交，仅在后续阶段文档或总报告记录跳过原因）：
`git commit -m "test(检索): 接入真实rerank provider并跑heuristic对比，如实记录收益"`

## 4. Stage 3：分块质量专项（限时、见好就收）

### 关闭裁决

本阶段未在 v4 内执行，已转入 `docs/roadmap/technical-debt.md` 的 P1“分块结构专项”。后续如排入执行，应按冻结蓝图和 `AGENTS.md` 重新分级、建立独立范围与验收，不得继续占用已关闭的 v4 计划。

### 现状
420/80 是 Stage 2 参数矩阵最优，但尚未做标题感知切分、长代码块/长段落处理，也未用更接近真实
业务分布的文档评测。

### 目标
在**不推翻已稳定参数**的前提下，限时探索能否通过更好的切分策略（而非单纯调大 chunk）进一步提质。

### 实施约束
1. 实验方向限定：**标题感知切分**（沿 headingPath 边界优先切）、**长代码块/长段落的专门处理**、
   用更贴近真实业务的 fixture 评测。单变量对照，每组出报告。
2. **限时约束**：这是边际收益递减区，设定明确的实验轮数上限（如每个方向≤3 组参数），
   到达上限即收敛，不得无限调参。禁止过拟合评测集。
3. Stage 3 默认只做实验、报告和可回滚的候选实现。除非新策略显著优于当前基线并经用户确认，
   **不得修改生产默认配置**，不得改变 `document.chunking.chunk-size=420`、`chunk-overlap=80`，
   不得影响既有 RRF/hybrid 链路。
4. 若新策略未超过当前 Recall@5 68.63%，**保持现状不改默认**，只在文档记录实验与结论。

### 验收标准
- 在 `docs/optimization/v4/stage3-chunking-plus.md` 给出实验矩阵、最终是否采纳、涨/平/跌与原因。

### ✅ Stage 3 完成后提交：
`git commit -m "perf(切分): 标题感知与长块处理专项实验，如实记录是否采纳"`
（描述按真实结果如实改写，禁止虚标数字）

## 5. Stage 4：清理文档真相源

### 状态

**已完成（2026-07-12）。**

已删除与当前实现矛盾的旧 audit、Kiro 初始规格、旧维护计划、Phase 0 诊断资料和会话式交接稿；有效内容已迁入当前架构、技术债、前端现状和学习路线文档。

### 目标
消除过时真相源，建立清晰的"历史材料 vs 新版策略文档"结构。

### 已落实

1. `docs/optimization/` 已按 `v3 / v4 / history` 重组，并新增 `README.md` 索引。
2. 当前架构与技术债分别归入 `docs/architecture/`、`docs/roadmap/`。
3. 前端旧规格合并为 `frontend-current-state.md`。
4. 个人学习路线移入 `docs/learning/`。
5. 本阶段未修改 Java/Vue 业务逻辑。

### 验收标准
- 仓库内不再存在与当前实现矛盾的过时结论；`docs/optimization/` 有清晰的最新索引。

### ✅ Stage 4 完成后提交：
`git commit -m "docs(优化): 归档过时后端优化文档，统一以optimization目录为权威真相源"`

## 6. 提交规范（强制）

- 每个已执行 Stage 至少一次中文 commit，遵循 `类型(范围): 描述`，类型用 feat/fix/perf/refactor/docs/test/chore。
  条件触发但被明确跳过的 Stage（如无凭据的 Stage 2）不要求单独 commit，只需在后续阶段文档或总报告中记录。
- 提交前执行第 0 节安全闸：检查无关未提交改动→有则请示；只 add 本 Stage 相关文件；
  确保能编译、`mvn test` 通过、服务能启动。
- 涉及评测结论的 Stage，提交附带 `docs/optimization/` 下对应说明文件。
- 关闭裁决：因 Stage 3 未在 v4 内执行，且其范围已转入技术债，不再补写会暗示“v4 全部完成”的最终总报告；各阶段事实以现有阶段文档为准。

## 7. 关闭状态

已完成：事实核验 → Stage 1 生成/引用评测 → Stage 2 条件判定并跳过 → Stage 4 文档清理。

未在 v4 内执行：Stage 3 分块专项；已转入技术债 P1，等待未来独立立项。

不再执行：v4 最终总报告。

后续唯一执行路线以 `docs/roadmap/iteration-blueprint.md` 和 Active OpenSpec change 为准；本文件不再承载下一步任务。
