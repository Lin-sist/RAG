# Design: C9b Judge Calibration And Status Semantics

## 1. Current Gap

当前 judge 路径具备最小可运行能力：仅对成功 answerable ask 调用 OpenAI-compatible `/chat/completions`，读取 `faithfulnessScore`、`relevanceScore`、`pass` 和 `reason`，聚合平均分与 pass rate，并记录 judge error。它尚不是可比较的校准能力：

- prompt/rubric 内联在 runner，未形成版本和 hash；
- parser 接受多个宽松别名并 clamp 越界 score；
- provider 自报 `pass` 可直接成为规范结果，只有缺失时才从两个 `0.70` score 派生；
- 缺少独立 human-gold calibration corpus、identity validator、confusion/agreement 和 repeat stability；
- 聚合只对成功 judge 子集计算，judge 失败样本从 score 分母消失；
- `report_status` 只看 login/retrieve/ask，judge 全失败仍可能为 `CLEAN`；
- `metricsSafeForComparison` 在全局 `PARTIAL` 时只能说“retrieval metrics only”，无法表达 objective 完整而 judge 不完整。

## 2. Target Architecture

```text
tracked calibration manifest + cases + fixture refs
                    |
                    v
       local fail-fast calibration validator
                    |
                    v
        shared rag-judge-v1 contract
       /             |              \
normal eval      calibration       reproducible
runner           runner            runner metadata
   |                  |
   v                  v
objective/judge/   confusion + agreement
global statuses    + repeat stability
```

Normal eval 与 calibration 共享同一 prompt、strict parser、score threshold 和 identity builder。Calibration runner 不调用 backend，也不产生答案，只把人工复核的静态 question/answer/context case 送给 judge，从而把“judge 本身的行为”与 retrieval/generation 波动分离。

## 3. Judge Contract

规划中的 `judgeContractConfig` 至少包含：

```json
{
  "judgeContractVersion": "rag-judge-v1",
  "promptVersion": "rag-judge-prompt-v1",
  "promptSha256": "<tracked prompt hash>",
  "parserVersion": "strict-json-scores-v1",
  "faithfulnessThreshold": 0.70,
  "relevanceThreshold": 0.70,
  "jointPassRule": "both-scores-gte-threshold-v1",
  "maxContextChars": 6000,
  "model": "<runtime model>",
  "temperature": 0
}
```

API key、Authorization、完整环境变量和 raw provider response 不进入 config。Endpoint 只记录脱敏 provider/origin identity；如果 endpoint path 影响协议，记录协议版本而不是凭据或查询参数。

### Strict response schema

- response 必须能解析为一个 JSON object；Markdown fence 可以按固定规则剥离，但不能从任意自然语言猜测分数。
- `faithfulnessScore`、`relevanceScore` 必须同时存在、为有限 number 且处于 `[0,1]`。
- 任一 score 缺失、NaN/Infinity、字符串、越界或 schema 非法，整个 observation 为 `invalid_judge_payload`，不进入 score/agreement 分母。
- provider `pass` 若存在，必须是 boolean，保存为 `providerReportedPass`；规范 `judgePass` 始终由两个 score 和 tracked threshold 派生。
- provider pass 与 derived pass 不同，增加 `providerPassMismatch`，但 observation 仍可评分；这用于暴露 provider 没遵守 rubric 的事实。
- `reason` 不是规范指标输入；raw details 可在本地保留受限文本，aggregate/普通日志只记录是否存在和安全长度，不复制正文。

## 4. Calibration Corpus

### 4.1 Artifact layout

实现阶段计划新增：

- `docs/eval/calibration/judge-calibration-v1-manifest.json`
- `docs/eval/calibration/judge-calibration-v1-cases.jsonl`
- `docs/eval/schema/judge-calibration-case-v1.json`
- `scripts/run_judge_calibration.py`
- 必要时抽取 `scripts/rag_judge_contract.py` 供 normal/calibration runner 共用

Manifest 固定 schema/case/review version、case path/hash/bytes/count/order、四象限 quota、fixture paths/hash 和 prompt contract version。它不修改或成为 `rag-eval-dev-v2` 的组成部分。

### 4.2 Case contract

每个 case 至少包含：

- `id`：稳定、唯一；
- `language`：`zh` 或 `en`；
- `caseType`：`single_claim` 或 `multi_claim`；
- `question`：人工编写的静态问题；
- `answer`：人工编写的 synthetic answer；
- `contextRefs[]`：tracked fixture source + exact `contains`，运行时确定性解析；
- `goldFaithful`：boolean；
- `goldRelevant`：boolean；
- `goldJointPass`：必须等于二者逻辑与；
- `quadrant`：由两个 gold labels 确定性派生并与声明一致；
- `reviewStatus=approved`、`reviewVersion` 和不复制 fixture 正文的 review note/code。

24 cases 的 exact quota 为：

| Gold quadrant | Count | 代表性边界 |
|---|---:|---|
| faithful=true, relevant=true | 6 | 有证据且直接回答 |
| faithful=true, relevant=false | 6 | 句子受 context 支持，但没有回答当前问题 |
| faithful=false, relevant=true | 6 | 表面回应问题，但包含 context 不支持或矛盾断言 |
| faithful=false, relevant=false | 6 | 既未回答问题，也缺少证据支持 |

case 还应覆盖中英文、单/多 claim 和部分支持边界，但不为每个交叉维度建立硬 quota，避免 24 条被过细矩阵耗尽。任何 case 修改都必须产生新的 calibration artifact identity；live evidence 后不得原地改写 v1。

## 5. Calibration Execution

### 5.1 Plan and validation order

1. 解析命令参数，但不读取或打印 key。
2. 本地验证 manifest/schema/case/fixture/prompt identity。
3. 选择固定 canary 或 full case set 和 repeat indexes。
4. 输出 plan-only 的 case IDs、identity 与 estimated judge calls。
5. 非 plan-only 时才要求 judge credential 并调用 provider。

Invalid calibration identity 必须在任何 network/provider 调用前以稳定 error code 退出。

### 5.2 Call shapes

- Canary：四象限各按稳定顺序选择首个 case，4 cases × 1 repeat = 4 calls。
- Full：全部 24 cases × 3 repeats = 72 calls。
- 无自动 retry。HTTP、timeout、rate limit、parse error 都保留为该 observation 的失败；如未来允许 retry，必须新建 tracked execution policy 并把 attempts/cost 计入 identity。
- canary 和 full 使用独立 no-overwrite 输出，不把 canary observation 混入 full 分母。

### 5.3 Calibration metrics

只有 schema-valid observation 进入 judge quality confusion matrix，但 coverage 分母始终是批准的全部 case×repeat observations：

- `expectedObservationCount`、`validObservationCount`、`errorObservationCount`、`parseCoverage`；
- faithfulness TP/TN/FP/FN 与 exact agreement；
- relevance TP/TN/FP/FN 与 exact agreement；
- joint pass TP/TN/FP/FN 与 exact agreement；
- `providerPassMismatchCount`；
- 每个 case 三次 derived labels 是否完全一致；
- `repeatConsistentCaseCount/24` 与不一致 case IDs；
- error category histogram，但不包含 raw exception/provider body。

C9b 不设最低 agreement、最大 mismatch 或 consistency 门槛。只有 identity/coverage 失败会使 evidence 不完整；质量数值由用户审阅，C10 才定义可自动执行的阈值。

## 6. Status Model

### 6.1 Objective metric status

`objectiveMetricStatus` 只描述非 judge 通道：

| 状态 | 条件 |
|---|---|
| `FAILED` | login 失败，或既有 majority retrieval failure 条件成立 |
| `RETRIEVAL_ONLY` | `--skip-ask`，且未达到 FAILED |
| `PARTIAL` | 任一 retrieve/ask error，或成功 ask 的 objective claim channel 出现 `PARTIAL`/unavailable |
| `COMPLETE` | retrieval、ask 和适用的 objective generation/citation/no-answer/claim 通道完整 |

Objective score 高低不影响 completeness status；低 support rate 仍可以是 `COMPLETE`。

### 6.2 Judge metric status

Judge eligible sample 为 `should_answer=true` 且 ask 成功、answer 非空的样本：

| 状态 | 条件 |
|---|---|
| `SKIPPED` | `judge-mode=off`，或执行明确不请求 judge |
| `NOT_APPLICABLE` | judge 已开启，但 selection 中没有 eligible answerable sample |
| `PARTIAL` | judge 已开启且任一 eligible sample 调用失败、payload 非法、score 缺失或 coverage 不完整 |
| `COMPLETE` | judge 已开启且每个 eligible sample 均有一个 schema-valid observation |

Judge pass rate 或平均分高低不影响 completeness status；全部合法但全部判 fail 仍为 `COMPLETE`。

### 6.3 Global report status

全局 status 组合顺序：

1. objective `FAILED` → `FAILED`；
2. objective `RETRIEVAL_ONLY` → `RETRIEVAL_ONLY`；
3. objective `PARTIAL` → `PARTIAL`；
4. judge `PARTIAL` → `PARTIAL`；
5. objective `COMPLETE` 且 judge 为 `COMPLETE/SKIPPED/NOT_APPLICABLE` → `CLEAN`。

因此 judge all-error 会把 global status 从 `CLEAN` 降到 `PARTIAL`，但 objective status 仍保持 `COMPLETE`。

### 6.4 Comparison safety

新增结构化字段：

```json
{
  "metricChannels": {
    "objective": {"status": "COMPLETE", "comparisonSafety": "ELIGIBLE"},
    "judge": {"status": "PARTIAL", "comparisonSafety": "NOT_ELIGIBLE"}
  }
}
```

- objective `COMPLETE` 可标为 `ELIGIBLE`，`RETRIEVAL_ONLY` 只允许 retrieval subset，其他为 `NOT_ELIGIBLE`；实际两份报告比较仍必须匹配 dataset/config/Git/claim identity。
- judge 只有 `COMPLETE` 才可标为 `ELIGIBLE`；跨报告比较还必须匹配 judge contract/provider/model identity。`SKIPPED/NOT_APPLICABLE/PARTIAL` 都不能产生 judge quality delta。
- 旧 `metricsSafeForComparison` 保留兼容读取，但写入时根据 structured channel 生成不误导的描述；judge partial 不再降格成“retrieval metrics only”。

## 7. Normal Eval Integration

- `run_sample` 保留 objective 计算顺序；judge exception 继续不污染 objective metric values。
- `aggregate` 不再仅以成功子集暗示完整；新增 judge eligible/attempted/valid/error/invalid payload counts 和 channel status。
- Markdown、console、details JSON 分别输出 global/objective/judge status 与 per-channel safety。
- `--fail-on-judge-errors` 继续是显式技术错误退出选项，但 C9b 不新增基于 judge score/agreement 的默认非零退出码。
- Reproducible runner 的 child command 不传 API key，继续通过环境传递；plan 输出只含脱敏 identity 和 estimated calls。

## 8. Compatibility

- 新字段 additive；历史 JSON 缺 `judgeContractConfig` 或 `metricChannels` 时解释为 `unavailable`，不伪造当时 judge off/partial。
- 已有 `reportStatus` 值域不变，只有 judge-enabled error run 的组合规则修正。
- `judge_pass_rate`、`faithfulness_avg`、`relevance_avg` 字段保留；非法 payload 不进入数值分母，但必须进入 coverage/error 分母并令 judge status partial。
- C9a `claimMetricConfig` 和 objective claim formulas 不变。
- calibration report 使用独立 schema/status，不与 normal eval `Report status` 混为一谈。

## 9. Security And Privacy

- Tracked calibration case 只能使用人工 synthetic answer 和现有 tracked fixture，不使用用户/生产知识库。
- Plan、aggregate report、ordinary logs 不输出 question、answer、context、reason、raw response、key、Authorization、绝对路径或完整异常。
- Raw calibration details 默认输出到显式本地路径，使用 `--no-overwrite`；是否 tracked 必须在 live evidence 审阅时单独决定，默认不跟踪 raw provider body。
- 错误只输出稳定 category、case ID、repeat index、HTTP status/timeout/rate-limit 等安全事实。

## 10. Verification Strategy

### Offline TDD

- Contract/parser：strict JSON、score types/range、provider pass mismatch、identity drift。
- Corpus validator：path/hash/count/order、quota、fixture grounding、gold/review/schema drift。
- Calibration metrics：confusion matrix、coverage、repeat consistency、failed observation preservation。
- Status matrix：judge all-error、partial、off、not-applicable、objective partial/retrieval-only/failed。
- Compatibility/security：legacy fields、no key in command/metadata、ordinary output no raw text。

### Live evidence

- 先 4-call canary，确认 endpoint/auth/schema/identity 与四象限链路。
- 经 canary 复核后才执行 72-call full；不 retry、不换样本、不删失败 observation。
- 结果只解释为固定 calibration v1、judge contract、provider/model 和 Git HEAD 下的开发 evidence。

## 11. Rollback

- 回滚 status 组合时恢复旧 `report_status`，同时移除新增 structured channel fields；C9a objective calculations 不受影响。
- calibration artifacts/runner 可独立移除，不影响 v1/v2 dataset validator 或 normal eval plan。
- 已产生 evidence 按其原 identity 保留，不用新版 contract 回填或重算。

## 决策记录

### 决策 1：用独立 calibration corpus，不改 rag-eval-dev-v2
- **面临的选择**：直接给 v2 增加 judge gold；从 v2 抽样但另建 sidecar；建立完全独立的静态 calibration corpus。
- **选了哪个 + 为什么**：选择独立 corpus，因为 judge 校准需要固定 answer/context 和二维 gold，而 v2 固定的是 question/annotation/fixture 身份，混入会破坏已接受 release 并把 generation 波动带进 judge 校准。
- **放弃的代价**：改 v2 会要求新 dataset release；只做 v2 sidecar 仍缺固定生成答案，无法隔离 judge 与 generation；独立 corpus 的代价是它只能代表开发 rubric，必须限制外推。

### 决策 2：固定 24 条、四象限各 6 条
- **面临的选择**：只收集明显正反例；做 8 条最小 smoke；做 24 条 faithful×relevant 平衡开发集。
- **选了哪个 + 为什么**：选择 24 条平衡集，因为两个维度必须分别观察 false positive/negative，四象限能暴露“有证据但答非所问”和“相关但无证据”这两类关键混淆。
- **放弃的代价**：明显正反例会高估 judge；8 条对单个误判过于敏感；24 条仍不足以代表生产分布，因此不设生产门槛。

### 决策 3：context 通过 fixture 引用解析，不在 case 中复制全文
- **面临的选择**：在每条 case 复制完整 context；只存 fixture 路径和 exact contains；运行时调用 backend retrieval。
- **选了哪个 + 为什么**：选择 tracked fixture + exact contains，因为它可本地校验、减少重复正文，又不会引入 backend 索引和检索波动。
- **放弃的代价**：复制全文会产生漂移和审阅重复；backend retrieval 会让 calibration 同时依赖 KB 状态；引用解析要求 fixture 改动时明确 bump identity，这是可接受的保护。

### 决策 4：gold 分开标 faithfulness 和 relevance
- **面临的选择**：只标一个 pass；给两个连续人工分数；分别标两个 boolean 并派生 joint pass。
- **选了哪个 + 为什么**：选择两个 boolean，因为当前 judge 的职责就是两个维度，二值 gold 更容易人工一致复核，也能直接形成各自 confusion matrix。
- **放弃的代价**：单 pass 无法定位混淆来源；连续分数需要更重的标尺和多标注者一致性；二值标签牺牲细粒度，但更适合首版可审计校准。

### 决策 5：normal eval 与 calibration 共用唯一 judge contract
- **面临的选择**：calibration runner 复制 prompt/parser；只校准一个离线专用 prompt；抽取共享 contract 供两条路径复用。
- **选了哪个 + 为什么**：选择共享 contract，否则校准通过的 rubric 不是 normal eval 实际使用的 rubric，evidence 没有约束力。
- **放弃的代价**：复制实现会静默漂移；离线专用 prompt 无法证明正式 runner 行为；共享模块带来小范围重构风险，需要聚焦回归。

### 决策 6：严格拒绝非法 score，不再 clamp
- **面临的选择**：继续 clamp 越界值；尽量从字符串猜测；严格要求有限数值且位于 `[0,1]`。
- **选了哪个 + 为什么**：选择严格 fail closed，因为 clamp 会把 provider 协议错误伪装成极端质量分数，字符串宽松解析也会降低跨模型可比性。
- **放弃的代价**：clamp/猜测能提高表面 coverage，但污染指标；严格模式会增加 partial 报告，正是需要暴露的 contract 风险。

### 决策 7：规范 pass 由 tracked score 阈值派生
- **面临的选择**：完全相信 provider pass；只保存 score 不算 pass；以两个 score 派生 pass 并把 provider pass 当诊断。
- **选了哪个 + 为什么**：选择确定性派生，使同一 score 在不同 provider 输出下含义一致，并能统计 provider 是否遵守 rubric。
- **放弃的代价**：相信 provider pass 会出现分数相同但 pass 不同；不算 pass 无法与 joint gold 比较；派生规则依赖候选阈值，因此必须版本化且不自动调参。

### 决策 8：保留 0.70 为候选，不在 C9b 自动调阈值
- **面临的选择**：沿用 0.70 并只报告证据；在 24 条上自动网格搜索最优阈值；把阈值决策全部推给 C10。
- **选了哪个 + 为什么**：选择以现有 0.70 作为版本化 candidate 并报告 agreement，因为 C9b 要校准当前 contract，而小样本自动调参容易过拟合，C10 才负责门禁阈值。
- **放弃的代价**：自动搜索会得到看似更高但不可泛化的结果；完全不固定 candidate 又无法定义当前 pass；保留 0.70 可能暴露低 agreement，需用户据 evidence 决定是否新版本调整。

### 决策 9：full 使用 3 repeats，且不自动 retry
- **面临的选择**：每 case 只调一次；三次 measured repeats；失败时自动 retry 直到成功。
- **选了哪个 + 为什么**：选择三次且不 retry，因为 temperature=0 仍可能非确定，repeat consistency 是校准事实；不 retry 保留真实失败率和明确调用上限。
- **放弃的代价**：单次无法观察稳定性；自动 retry 会选择性隐藏 provider 问题并增加费用；三次将 full 调用上限提高到 72，必须单独授权。

### 决策 10：judge completeness 与 judge quality 分离
- **面临的选择**：低 pass rate 就标 PARTIAL；只报告平均分不设状态；状态只描述调用/解析/coverage 完整性。
- **选了哪个 + 为什么**：选择第三种，因为 `PARTIAL` 应说明证据缺失，不应把“完整测得但质量低”误成技术失败。
- **放弃的代价**：第一种混淆质量和执行；第二种会让全失败调用看似只是低分；分离后需要用户同时阅读 status 和 score/agreement，这是正确成本。

### 决策 11：judge partial 可降全局状态，但不污染 objective status
- **面临的选择**：judge error 永远不影响全局；judge error 把所有通道都标 partial；全局 partial 同时保留独立 objective complete。
- **选了哪个 + 为什么**：选择第三种，既修复 judge 全失败仍 CLEAN，又保留已完整获得的 retrieval/generation/citation/no-answer/C9a objective 证据。
- **放弃的代价**：完全忽略会误报 CLEAN；全通道降级会丢失可比较 objective evidence；分层状态增加字段，但语义最准确。

### 决策 12：judge 关闭仍允许 objective CLEAN
- **面临的选择**：没有 judge 就永远 PARTIAL；judge off 为 SKIPPED 且 objective 可 CLEAN；默认强制开启 judge。
- **选了哪个 + 为什么**：选择显式 SKIPPED，因为 accepted spec 已允许 judge off 时报告客观指标，且默认开启会引入费用、出站和可用性依赖。
- **放弃的代价**：永远 PARTIAL 会破坏既有 objective baseline；强制开启越过外调授权；SKIPPED 必须配合结论边界，不能宣称 faithfulness judge 已完成。

### 决策 13：新增 structured per-channel safety，保留 legacy 字段
- **面临的选择**：直接删除 `metricsSafeForComparison`；继续使用单字符串；新增结构化 channel 状态并由它生成兼容字符串。
- **选了哪个 + 为什么**：选择结构化字段加兼容字符串，既能表达 objective/judge 分离，也避免历史消费者立即失效。
- **放弃的代价**：删除字段会破坏兼容；单字符串无法准确表示一个通道完整另一个不完整；双写要求测试两者一致。

### 决策 14：C9b 不覆盖 no-answer、逐 claim judge 和 C10 gate
- **面临的选择**：一次校准 answerable/no-answer/逐 claim 并建立 CI 阈值；只完成 answerable whole-answer judge 与状态语义；完全不碰 judge 质量。
- **选了哪个 + 为什么**：选择中间范围，与当前 judge eligibility 和冻结路线图一致，能修复明确缺口又不把 calibration、claim entailment、拒答和门禁混成一个 change。
- **放弃的代价**：一次全做会让 gold schema、调用量和状态矩阵急剧膨胀；完全不校准则无法可信解释 judge；分阶段意味着 C9b 完成后仍需 C10 和未来专项。

### 决策 15：live calibration 必须另行授权
- **面临的选择**：规划批准即包含全部 judge 调用；offline 实现后自动跑 canary/full；把 live run 作为独立披露和授权闸门。
- **选了哪个 + 为什么**：选择独立闸门，因为最多 76 次调用会发送 tracked question/answer/context，provider/model/费用/限流在规划时仍是 unknown。
- **放弃的代价**：默认包含会违反 accepted 外调契约；自动执行无法让用户核对模型和数据出站；独立授权会多一次交互，但保留成本与隐私控制。

## Implementation Notes（2026-07-23，待验收）

- shared contract 落在 `scripts/rag_judge_contract.py`；normal 与 calibration 两条路径共用 system prompt、prompt builder、strict parser、threshold 和 secret-free config identity。
- calibration runner 以显式 `--execute-live-judge` 作为命令行误触保护，协作层仍必须另行取得外调授权；无 retry，任何失败 observation 保留在 coverage 分母。
- aggregate Markdown 不含 question/answer/context/reason/raw provider body/credential/绝对路径；本地 details 可保留 raw judge content，并由 `--no-overwrite` 保护。
- normal runner 保留 legacy `metricsSafeForComparison` 字段，但写值由 structured channel safety 生成；历史 details 缺新字段时不做回填。
- 实现未修改 v1/v2 release、C9a objective formula、Java/API、production defaults 或 C10 gate。
