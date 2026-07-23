# Design: C9a Claim Evidence Objective Metrics

## 1. Context And Boundary

现有 runner 已有三条彼此不同的 generation/citation 事实：

1. answer keyword hit：生成答案包含多少 expected keywords；
2. citation source/snippet provenance：returned citation 是否命中 expected source、snippet 是否能回连 returned context；
3. 可选 LLM judge：对 answer/context 做 faithfulness/relevance 评分。

C9a 新增第四条独立通道：把生成答案拆成 claim，并检查每个 claim 是否能与“本轮已经过 provenance 校验的 returned citation evidence”形成确定性词法对齐。它不替代前三条，也不修改其公式。

## 2. Data Flow

```text
successful answerable ask
  -> deterministic claim splitter
  -> ordered claim units

returned citations + returned contexts
  -> existing citation identity/snippet provenance check
  -> eligible citation evidence

claims x eligible evidence
  -> exact containment
  -> claim-token coverage fallback
  -> per-claim best attribution
  -> per-sample and aggregate objective metrics
```

direct runner 是唯一计算入口。reproducible runner 只负责固定 dataset/KB/config/Git identity、估算调用与拼接 child command，不复制 splitter 或 matcher。

## 3. Claim Extraction Contract

### Input eligibility

- `should_answer=true`；
- ask 未跳过；
- ask 无错误且 response 存在；
- answer 去空白后非空。

不满足时不伪造 claim：no-answer 为 `NOT_APPLICABLE`，retrieval-only 为 `SKIPPED`，ask/answer/extractor 缺口进入 `PARTIAL` 计数。

### Deterministic splitting

1. 标准化 CRLF/LF 与 Unicode 空白，但不改写可见文字。
2. 按空行、换行列表项拆 block；识别 `-`、`*`、`•`、`1.`、`1)`、`（一）` 等常见 marker。
3. block 内按 `。！？；.!?;` 切 sentence，保留分隔符前文本。
4. 去掉两端空白与孤立 list/citation marker。
5. 丢弃空 claim、纯编号、纯标点、只有 Markdown heading marker 的单元；不做摘要、合并或语义重写。
6. 保持原出现顺序并赋 `claimIndex`（从 1 开始）。

claim hash 使用 normalized claim 的 SHA-256 短前缀，仅用于同一 details artifact 内定位；它不替代原 claim 文本，也不作为跨算法版本的永久身份。

### Extractor completeness

eligible answer 非空却抽取出 0 个 claim 时，该样本记 `claimExtractionError=empty_claim_set`，claim metric status 至少为 `PARTIAL`。不能把它当成 0/0 后报告 `COMPLETE`。

## 4. Eligible Evidence Contract

每条 returned citation 先复用既有 `citation_supported_by_context`：

- 优先以 chunkId 对齐；缺失时以 documentId；再缺失时才使用 normalized source candidates；
- citation snippet 必须能在对应 returned context 中 normalized exact 命中，或通过既有 snippet-token overlap；
- citation 未通过 provenance 时继续计入既有 unsupported citation 指标，但不得进入 C9a eligible evidence。

eligible evidence 最小字段：`citationIndex`、安全 source identity、chunk/document identity、snippet、matched context identity。aggregate 不复制 snippet 或 answer；per-sample details 沿用现有 raw evidence 文件的访问边界。

不直接把全部 retrieved contexts 当作 claim evidence。原因是 C9a 衡量“答案 claim 是否有显式且有效的 citation evidence”，不是“检索结果里是否碰巧存在相似文字”。

## 5. Objective Lexical Alignment

对每个 claim 与每条 eligible evidence 计算：

1. `exact`：normalized claim 非空且完整包含于 normalized eligible citation snippet；
2. `token_overlap`：exact 未命中时，复用现有 tokenizer（ASCII 连续字母数字 token + CJK bigram），以 claim token 为分母计算 `|claimTokens ∩ evidenceTokens| / |claimTokens|`；至少 2 个 claim token，coverage `>=0.70`；
3. `unsupported`：没有 eligible evidence，或所有 evidence 都不满足前两级。

若多条 evidence 命中，选择顺序固定为：`exact` 优先于 `token_overlap`；同方法选择 coverage 高者；再以 citationIndex 小者稳定 tie-break。保留 matched evidence 数量，避免 best match 隐藏一对多事实。

`0.70` 是 C9a v1 初始、可审阅的 lexical threshold，比现有 citation-snippet provenance 的 `0.58` 更严格，因为前者试图覆盖 claim 内容，后者只确认 snippet 来源。该阈值进入 `claimMetricConfig`，不得由环境变量静默改变。它仍不是语义置信度。

## 6. Result Model

### Per-claim details

建议 additive 结构：

```json
{
  "claimIndex": 1,
  "claimHash": "sha256-prefix",
  "text": "原 claim 文本",
  "support": "exact | token_overlap | unsupported",
  "bestEvidence": {
    "citationIndex": 0,
    "method": "exact",
    "claimTokenCoverage": 1.0
  },
  "matchedEvidenceCount": 1,
  "reason": null
}
```

unsupported 时 `bestEvidence=null`，reason 只允许稳定枚举，例如 `no_eligible_evidence`、`below_lexical_threshold`、`insufficient_claim_tokens`，不写原始异常 message。

### Per-sample facts

- `claimMetricStatus`；
- `claimTotal`；
- `supportedClaimCount`；
- `unsupportedClaimCount`；
- `exactSupportCount`；
- `tokenOverlapSupportCount`；
- `objectiveClaimSupportRate`；
- `eligibleEvidenceCount`；
- `claimExtractionError`；
- `claims[]`。

### Aggregate facts

- `claim_metric_status`；
- `claim_evaluable_samples` / `claim_partial_samples` / `claim_not_applicable_samples`；
- `claim_total`；
- `supported_claim_count` / `unsupported_claim_count`；
- `exact_supported_claim_count` / `token_overlap_supported_claim_count`；
- `objective_claim_support_rate`；
- `answers_with_complete_claim_metrics`。

所有字段使用 additive JSON/report 变更，旧 consumer 缺字段时按“C9a unavailable”处理，不推断为 0。

## 7. Denominator And Status Semantics

`objective_claim_support_rate = supported_claim_count / claim_total`，其中 claim_total 是所有成功抽取的 answerable claims。以下规则防止分母漂移：

- 无 eligible citation 的 claim 仍进入分母并计 unsupported；
- token 太短无法可靠比较的 claim 仍进入分母并计 unsupported，同时 reason=`insufficient_claim_tokens`；
- ask 失败样本没有虚构 claim，但增加 partial sample count，使 aggregate status 为 `PARTIAL`；
- no-answer 不进入分母，保留既有 no-answer accuracy/citation violation；
- 全部为 retrieval-only 时为 `SKIPPED`；
- 已运行 ask 但选集没有 answerable 样本时为 `NOT_APPLICABLE`；
- 只有当全部 eligible answerable ask 样本成功抽取并完成 alignment 时才为 `COMPLETE`。

C9a 不修改全局 `Report status`：现有 CLEAN/PARTIAL/RETRIEVAL_ONLY/FAILED 继续描述 retrieve/ask 主通道。judge error 仍不在本 change 修复；C9b 再定义 objective/judge 的全局组合状态。

## 8. Algorithm Identity And Compatibility

details/report metadata 新增：

- `claimMetricVersion=claim-lexical-v1`；
- `claimSplitterVersion=sentence-list-v1`；
- `tokenizerVersion=ascii-cjk-bigram-v1`；
- `lexicalThreshold=0.70`；
- `minClaimTokens=2`；
- `evidencePolicy=validated-returned-citations-only-v1`。

正式比较 claim 指标时这些字段必须一致。旧报告无这些字段时，只能说明 C9a 未计算，不能补写或追认。C9a 不变更 dataset release，因为 question/annotation/schema/fixture bytes 均不变；算法身份属于 run metadata。

## 9. Error And Privacy Handling

- splitter/matcher 是纯函数，不应抛出原始 provider exception；异常转换为稳定 `claim_metric_internal_error`。
- 普通日志不输出 answer、claim、citation snippet、context、question 或原始异常 message。
- Markdown aggregate 只输出 count/rate/status/config；per-sample details 可沿用现有本地 raw evidence 边界保存文本，不新增 tracked report。
- secret、Authorization、API key、绝对本机路径不得进入 claim attribution。

## 10. TDD And Verification Strategy

### Unit RED/GREEN

- 中英文句末、分号、Markdown 列表、编号、heading/citation marker、空白与稳定顺序；
- exact、token overlap、threshold 边界、minimum token、stable tie-break；
- invalid citation 不进入 evidence；无 citation 计 unsupported；
- no-answer/retrieval-only/ask failure/empty answer 的 status 与 denominator；
- aggregate 不因 partial 样本静默变成 COMPLETE；
- details/report additive schema 与算法 identity；
- ordinary output/error 不泄露 secret、绝对路径或原始异常。

### Offline regression

- `python -B -m unittest discover -s scripts -p 'test_*.py'`；
- v1/v2 dataset contract validation 与 direct/reproducible plan-only；
- SensitiveLogs 和定向 user-content/secret/path scan；
- `git diff --check`、change 结构、断链、受保护路径与历史 report 非覆盖检查。

Maven、frontend build、Docker/live provider 在无 Java/前端/运行时改动时跳过并记录原因。

## 11. Rollout And Evidence Boundary

规划批准后先完成离线实现和合成测试，不运行真实 ask。真实 150 条 generation evidence 必须独立授权，并固定 dataset release、KB/doc identity、Git HEAD、tracked config、LLM provider/model、claimMetricConfig、timeout/retry 与 raw artifact no-overwrite policy。

首次真实 evidence 只能描述当前固定身份下的 objective lexical alignment。若 observed 分布显示 threshold 或 splitter 需要调整，应产生新的 algorithm version/change 后重新跑，不能覆盖原 evidence。

## 12. Implementation Notes（待验收）

- splitter 与 matcher 作为 direct runner 内的纯函数实现；reproducible runner 只引用 direct runner 的配置常量，不复制算法。
- `claimMetricConfig` 同时进入 direct/repro plan、repro run metadata、Markdown header、details JSON、per-sample 和 aggregate；外部 metadata 声明不同 identity 时 fail closed。
- unsupported reason 固定为 `no_eligible_evidence`、`insufficient_claim_tokens`、`below_lexical_threshold`；ask/抽取缺口固定为 `ask_error`、`empty_answer`、`empty_claim_set`，不透传原始 provider exception。
- aggregate `claim_metric_status` 独立于全局 Report status；no-answer 为 `NOT_APPLICABLE`，retrieval-only 为 `SKIPPED`，缺口为 `PARTIAL`，完整 answerable alignment 为 `COMPLETE`。
- 实现没有增加依赖、环境变量、Java/API、生产配置或 dataset schema/release；真实 generation evidence 继续由独立授权闸门控制。

## 决策记录

### 决策 1：claim 拆分使用本地确定性规则
- **面临的选择**：用 LLM 拆 claim；用 NLP/第三方模型；用内置段落、列表和句末规则。
- **选了哪个 + 为什么**：选择内置确定性规则，因为 C9a 要建立可复现的客观通道，必须零 provider 依赖、可单测且同输入稳定。
- **放弃的代价**：LLM/NLP 方案会引入模型版本、费用、出站和非确定性；内置规则会牺牲部分语义原子性，因此结果只能称句法 claim 单元。

### 决策 2：claim 来自生成答案而不是 expected answer points
- **面临的选择**：拆生成答案；把 expected answer points 直接当 claim；比较两者后只保留命中项。
- **选了哪个 + 为什么**：选择拆生成答案，因为目标是审计系统实际说出的内容是否有证据，而 expected answer points 已由 keyword/人工检查通道承担另一职责。
- **放弃的代价**：直接用 expected points 会漏掉模型额外编造的句子；只保留命中项会把最需要发现的 unsupported claim 从分母删掉。

### 决策 3：以句子和列表项作为 v1 claim 单位
- **面临的选择**：整段一个 claim；句子/列表项；进一步按连词做语义原子化。
- **选了哪个 + 为什么**：选择句子/列表项，因为它比整段更可定位，又能保持规则简单稳定；连词原子化在中英文中歧义较大。
- **放弃的代价**：整段会让一个受支持片段掩盖同段其他内容；连词切分容易把条件、否定和固定术语拆坏。

### 决策 4：evidence 只接受通过 provenance 的 returned citations
- **面临的选择**：全部 retrieved contexts；全部 returned citations；仅通过 citation-context 校验的 returned citations。
- **选了哪个 + 为什么**：选择最后一种，因为 C9a 要测显式 claim-evidence 链，不能把未引用的检索文本或来源不明 citation 当支持。
- **放弃的代价**：全部 contexts 会高估支持率；全部 citations 会让伪造或漂移 snippet 污染结果；严格策略会对“答案正确但没引用”的 claim 记 unsupported，这是有意暴露的 citation 缺口。

### 决策 5：采用 exact + claim-token coverage 的两级匹配
- **面临的选择**：只做 exact；exact 加确定性 token overlap；embedding/NLI/LLM entailment。
- **选了哪个 + 为什么**：选择 exact 加 token overlap，兼顾可解释的轻微改写与纯本地复现，并与现有 ASCII/CJK tokenizer 复用。
- **放弃的代价**：只 exact 对改写过严；语义模型会产生 provider/模型身份和校准问题；token overlap 仍有同义 false negative 与术语 false positive，所以不得称 entailment。

### 决策 6：v1 阈值固定 0.70 且最少 2 个 claim token
- **面临的选择**：复用 provenance 的 0.58；固定更严格的 0.70；运行时可配置阈值。
- **选了哪个 + 为什么**：选择 tracked 的 0.70 与最少 2 token，因为 claim 支持比 snippet 来源校验要求更强，且固定配置才能保证报告可比较。
- **放弃的代价**：0.58 容易把弱共享术语算支持；运行时自由配置会造成静默不可比；0.70 尚未经验校准，因此必须在 metadata 暴露且不形成通用质量门禁。

### 决策 7：所有抽取出的 claim 都进入分母
- **面临的选择**：只算有 citation 的 claim；只算成功匹配的 claim；所有抽取 claim 都算并把无证据记 unsupported。
- **选了哪个 + 为什么**：选择全部进入分母，因为删除无 citation/未匹配 claim 会系统性抬高 support rate。
- **放弃的代价**：只算有 citation 会隐藏 citation coverage 缺口；只算命中项会把指标退化成恒高值；全量分母会暴露格式性短 claim，需要稳定 reason 解释。

### 决策 8：no-answer 不进入 claim support 分母
- **面临的选择**：把拒答句也拆 claim；把 no-answer 当 0 claim；继续走独立 no-answer 指标。
- **选了哪个 + 为什么**：选择独立 no-answer 指标，因为拒答正确性与事实 claim 支持不是同一问题，混入会扭曲分母。
- **放弃的代价**：拆拒答句会把“无法回答”误当知识 claim；0 claim 混入 aggregate 会造成分母和状态含义不清。

### 决策 9：claim 通道有独立局部状态但不改全局 Report status
- **面临的选择**：直接改 CLEAN/PARTIAL；完全不记 claim 完整性；新增 claimMetricStatus。
- **选了哪个 + 为什么**：选择新增局部状态，因为 C9a 必须暴露自己的 partial/skipped，而 C9b 才负责 objective/judge 全局状态组合。
- **放弃的代价**：现在改全局状态会吞并 C9b；不记局部状态会让 ask/extractor 缺口被聚合率掩盖。

### 决策 10：算法身份写入 run metadata，不 bump dataset release
- **面临的选择**：修改 v2 manifest/schema；只写代码不记录版本；在 run metadata 固定 claim 算法身份。
- **选了哪个 + 为什么**：选择 run metadata，因为 C9a 没改变 question/annotation/fixture，算法配置却必须随报告审计和比较。
- **放弃的代价**：bump dataset 会混淆数据与指标版本；不记录版本会让不同 threshold/splitter 的结果看似可比。

### 决策 11：aggregate 不复制 raw claim/evidence，details 保留审计文本
- **面临的选择**：所有报告都输出全文；全部只输出 hash/count；aggregate 脱敏而本地 details 保留既有 raw evidence。
- **选了哪个 + 为什么**：选择分层输出，既能逐 claim 复核，又避免摘要报告和普通输出扩大用户内容暴露面。
- **放弃的代价**：全文 aggregate 不利于安全分享；全 hash 无法判断 matcher 是否误判；details 仍需按 raw artifact 管理而不能随意 tracked。

### 决策 12：C9a 不运行真实 baseline、不校准 judge、不建质量门禁
- **面临的选择**：一次完成 C9a/C9b/C10；规划后直接跑 150 条；先完成离线客观指标契约。
- **选了哪个 + 为什么**：选择先完成离线 C9a，因为 judge 与 quality gate 有独立状态、成本和校准风险，真实 generation 也需要单独外调授权。
- **放弃的代价**：一次完成会让客观算法、judge 失败和退出码难以归因；直接跑会在 contract 未验收前产生昂贵且不可比较的 evidence；分阶段意味着 C9a 完成后仍不能宣称完整 faithfulness 或质量门禁。
