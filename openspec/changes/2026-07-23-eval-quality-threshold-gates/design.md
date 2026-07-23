# Design: C10 Eval Quality Threshold Gates

## Context

当前 runner 已能区分 dataset validation、global/objective/judge status、comparison safety 和各类 aggregate metrics，但“报告完整且可比较”与“质量达到政策目标”仍是两件没有机器化连接的事。现有 `--fail-on-ask-errors` / `--fail-on-judge-errors` 只覆盖执行错误，不能表达 profile、类别阈值、容差、缺失值或质量失败。

C10 必须在不改指标公式、不触发真实 provider 的前提下建立一个可审计的 policy layer。它消费既有 details evidence，不把 report Markdown 当数据源，也不从当前 candidate 自动学习门槛。

## Proposed Architecture

### Artifacts

- `docs/eval/schema/rag-quality-gate-profile-v1.json`：profile JSON Schema。
- `docs/eval/gates/*.json`：tracked profile；初始 retrieval profile 保持 `DRAFT`，直到 reference evidence 与阈值获批。
- `scripts/evaluate_quality_gate.py`：离线 evaluator/CLI。
- `scripts/test_evaluate_quality_gate.py`：contract、slice、threshold、status、exit code 与安全输出测试。
- `docs/eval/RAG_EVAL_GUIDE.md`：两步运行、退出码、CI 示例、外调与 raw artifact 边界。

### Processing Flow

1. 加载 profile raw bytes，校验 schema、受支持 version、status 与 hash。
2. 加载 details JSON，只读取 allowlisted identity/status/metrics/sample calculation fields。
3. 校验 dataset release、sample selection、run/metric identity、required channel completeness 与 profile compatibility。
4. 通过 profile 声明的固定 slice axes 将 sample id 与 versioned dataset annotation 连接，确定性计算 denominator 和 metric。
5. 先执行 evidence completeness/error rules；不完整则 `NOT_EVALUABLE`。
6. 对完整 evidence 执行 hard thresholds 与 reference regression tolerance；任一 required quality rule 不通过则 `FAIL`，全部通过才 `PASS`。
7. 写脱敏 JSON/Markdown gate summary，并按结果返回稳定退出码。

### Profile Shape

profile 至少包含：

- `schemaVersion/profileId/profileVersion/status`；
- `datasetReleaseId/datasetManifestSha256/expectedSampleCount/selectionMode`；
- `runIdentity`（mode、topK、minScore、rerank、必要的 metric/claim/judge contract identity）；
- `requiredChannels` 与各通道允许的 completeness/comparison 状态；
- `slices[]`：`all/type/difficulty/answerability`、value、minimum denominator；
- `rules[]`：channel、metric、operator、target、required、可选 reference/tolerance；
- `errorPolicy/missingPolicy`；
- `profileSha256` 或由 validator 计算并在 result 中绑定的 canonical identity。

### Gate Result

gate summary 固定包含：

- profile/dataset/run/evidence identity；
- `gateStatus: PASS | FAIL | NOT_EVALUABLE | INVALID`；
- 每条 rule 的 slice、channel、metric、denominator、observed、target、tolerance、result、safe reason code；
- error/missing/incompatible counts；
- exit code 与 evaluator version。

它不复制 raw question、answer、expected content、context、claim、citation、provider body、secret、Authorization 或绝对路径。

## Status And Exit Code Semantics

- `PASS / 0`：profile 为 ACTIVE，evidence 完整、身份兼容、所有 required rules 通过。
- `FAIL / 3`：evidence 完整且可比较，但至少一条 required quality rule 低于 hard threshold 或超过允许 regression tolerance。
- `NOT_EVALUABLE / 4`：DRAFT profile、required channel/status 不完整、error 超预算、缺 metric、denominator 不足、selection 不完整或 evidence 不可比较。
- `INVALID / 2`：profile/schema/hash/operator/input JSON 等 contract 无效。
- `1`：未分类的 evaluator 运行异常；不得吞掉后伪装为质量 FAIL。

## Threshold And Tolerance Semantics

- rate/score 使用 `[0,1]` 有限数；count/latency 使用非负有限数。
- hard rule 只支持 `minInclusive` / `maxInclusive`；安全计数默认 zero tolerance。
- reference rule 以锁定 reference summary 为输入，支持 `maxAbsoluteRegression`；candidate 与 reference 的 profile/dataset/run/metric identity 必须匹配。
- tolerance 只能作用于 quality regression，不得豁免 status、error、missing、identity 或 denominator completeness。
- hard rule 与 reference rule并存时是 AND；不允许只靠 tolerance 穿过 hard floor。

## Initial Profile Strategy

先实现通用 evaluator 和一个 `DRAFT` retrieval-only v2 profile shape。它覆盖 overall、五类 `type`、三档 `difficulty` 与 answerable retrieval slices；不要求 judge，不读取 generation/judge quality。

具体 hard floors 和 tolerances 不在缺少 v2 reference evidence 时猜测。推荐在后续独立授权下对固定 v2/150、heuristic reranker、同一 KB/config/Git identity 做 3 个 measured repeats；reference complete 后再由用户审阅候选阈值。Generation/objective 与 judge gate 留待各自真实 evidence 充分后以新 profile/version 激活，不阻塞 retrieval profile。

## Verification Strategy

- TDD：profile drift/invalid、slice denominator、hard threshold、reference tolerance、missing/error/status、exit code、安全输出逐切片 RED→GREEN。
- 合成 fixture：覆盖 PASS、质量 FAIL、证据 NOT_EVALUABLE、contract INVALID 与历史缺字段兼容性。
- 全量：`python -B -m unittest discover -s scripts -p 'test_*.py'`。
- 静态：SensitiveLogs、Markdown 链接、secret/absolute-path、protected paths、历史 report 非覆盖、`git diff --check`。
- planning/offline implementation 不运行 Maven、frontend build、Docker/backend/provider；无对应改动时明确 `SKIPPED`。

## 决策记录

### 决策 1：门禁作为独立 evaluator 还是直接塞进 direct runner

1. **面临的选择**：独立 evaluator 消费 details JSON；直接在 `run_rag_eval.py` 末尾判定；由 CI 自己解析 Markdown。
2. **选了哪个 + 为什么**：选择独立 evaluator。它把“产生 evidence”和“应用 policy”解耦，可对同一不可变 evidence 重放不同 profile，也避免触发 backend/provider 才能测试门禁。
3. **放弃的代价**：直接塞 runner 会把运行错误、质量失败和外调生命周期耦合；CI 解析 Markdown 脆弱且会复制算法。

### 决策 2：profile 使用 tracked JSON + schema 还是命令行散装阈值

1. **面临的选择**：tracked JSON + JSON Schema；大量 CLI flags；Python 常量。
2. **选了哪个 + 为什么**：选择 tracked JSON + schema，以 raw/canonical identity 固定政策、适用 release 与阈值，便于 review、版本化和复现。
3. **放弃的代价**：CLI flags 难以审计且容易漏参数；Python 常量把 policy 与实现耦合，变更阈值必须改代码。

### 决策 3：首个 profile 直接激活还是先保持 DRAFT

1. **面临的选择**：按经验填写数值并 ACTIVE；从 C7 30 条历史报告追认；先 DRAFT，等 v2 reference evidence 与用户审阅。
2. **选了哪个 + 为什么**：选择先 DRAFT。当前没有正式 v2/150 retrieval evidence，C7 历史报告也不能追认为 C10 gate 输入。
3. **放弃的代价**：经验值会制造伪精确；追认 C7 会违反 dataset versioning/历史 evidence 边界；DRAFT 的代价是 C10 需要后续 evidence 闸门才能完全激活。

### 决策 4：初始 active 能力覆盖哪些指标通道

1. **面临的选择**：一次激活 retrieval/objective/judge；先 retrieval，后续 profile 扩展；只做 execution error gate。
2. **选了哪个 + 为什么**：选择先 retrieval profile，framework 同时支持 objective/judge channel。retrieval 已有成熟客观公式，而 generation v2 evidence 与 live judge calibration 尚未完成。
3. **放弃的代价**：一次激活三通道会把未验证 judge/generation 当作成熟政策；只做 error gate 又达不到 C10 的质量阈值目标。

### 决策 5：类别切片允许任意表达式还是固定 axes

1. **面临的选择**：任意 Python/JSONPath 表达式；固定 `all/type/difficulty/answerability`；只做 overall。
2. **选了哪个 + 为什么**：选择固定 axes，覆盖路线图要求且保持 schema 可验证、计算可复现、输出易审计。
3. **放弃的代价**：任意表达式存在执行/歧义风险；只做 overall 会掩盖 multi-hop/no-answer 或 hard category 回退。

### 决策 6：类别 annotation 从 details 读取还是从 versioned dataset 连接

1. **面临的选择**：扩写 details 复制全部 annotation；按 sample id 连接 profile 固定的 versioned question set；仅依赖 aggregate metrics。
2. **选了哪个 + 为什么**：选择按 sample id 连接 versioned dataset，并验证完整 selection。这复用 C8 identity，避免历史 details 缺 `difficulty` 时被错误补 0。
3. **放弃的代价**：复制 annotation 增加漂移面；只用 aggregate 无法做类别切片。

### 决策 7：证据不完整算 FAIL 还是 NOT_EVALUABLE

1. **面临的选择**：统一 FAIL；统一 PASS 并警告；区分 `NOT_EVALUABLE` 与完整 evidence 下的 `FAIL`。
2. **选了哪个 + 为什么**：选择区分。质量低与执行/身份/分母不完整具有不同处置方式，且 C9b 已建立 channel completeness 语义。
3. **放弃的代价**：统一 FAIL 会误导根因；PASS+警告会让缺失 evidence 穿过门禁。

### 决策 8：缺失 metric 如何处理

1. **面临的选择**：填 0；从成功子集计算；required rule 变 `NOT_EVALUABLE`，optional rule 明确 `SKIPPED`。
2. **选了哪个 + 为什么**：选择 required fail closed、optional explicit skip，不改变 denominator，也不把 unavailable 当 0。
3. **放弃的代价**：填 0 混淆缺失与低质量；成功子集会产生虚高指标。

### 决策 9：hard floor 与 regression tolerance 的关系

1. **面临的选择**：只有固定 floor；只有相对 reference；两者 AND 且 tolerance 不豁免 safety rules。
2. **选了哪个 + 为什么**：选择两者 AND。hard floor 防止基线本身太差，相对 tolerance 检测回退，二者职责不同。
3. **放弃的代价**：只有 floor 对小回退不敏感；只有 reference 会把差基线永久合法化。

### 决策 10：退出码是否复用 1/2

1. **面临的选择**：所有非通过都返回 1；`0/1/2`；保留 contract/runtime 语义并新增 `3=FAIL`、`4=NOT_EVALUABLE`。
2. **选了哪个 + 为什么**：选择 `0/3/4/2`，未分类异常保留 1，使 CI 和人工排障能区分质量失败、证据不足与输入无效。
3. **放弃的代价**：统一 1 丢失根因；复用 1/2 会和现有 runtime/validation 语义冲突。

### 决策 11：reference evidence 用一次还是重复运行

1. **面临的选择**：一次 150 条；三次固定身份 150 条；沿用 C7 30 条。
2. **选了哪个 + 为什么**：推荐三次固定身份 v2/150，用完整 repeat 分布观察波动；具体调用与是否执行待用户在 evidence 事前闸门确认。
3. **放弃的代价**：一次无法区分偶然波动；C7 30 条不具 v2 正式 gate identity；三次的代价是最多 450 次 retrieval/query embedding 调用。

### 决策 12：阈值是否自动从 reference 学习

1. **面临的选择**：自动取当前值/分位数；内置经验阈值；evidence 后生成候选、由用户审阅确认。
2. **选了哪个 + 为什么**：选择 evidence 后给候选并由用户确认，具体 hard floor 与 tolerance 当前标记为待用户在事前闸门确认。
3. **放弃的代价**：自动学习易把当前缺陷固化并诱发过拟合；经验阈值缺少本项目证据。

### 决策 13：普通 gate output 是否保留 raw sample 内容

1. **面临的选择**：复制 details 便于调试；只输出 sample id 和 allowlisted rule evidence；完全不输出逐规则信息。
2. **选了哪个 + 为什么**：选择 allowlisted identity/metric/denominator/reason，不复制原文；既可排障又遵守 raw evidence 边界。
3. **放弃的代价**：复制原文有日志泄露风险；完全无逐规则信息难以定位失败类别。

### 决策 14：是否在本 change 修改生产行为或 CI 配置

1. **面临的选择**：同时切生产默认/CI required check；只交付 evaluator/profile/文档；顺带修改 prompt/rerank 让 gate 通过。
2. **选了哪个 + 为什么**：选择只交付 evaluation policy layer；生产默认、CI 平台和算法优化分别立项。
3. **放弃的代价**：同时修改会让 baseline 不可比较并扩大授权面；顺带调算法会形成 benchmark overfitting。

### 决策 15：judge 缺失是否阻塞 retrieval profile

1. **面临的选择**：任何 judge SKIPPED 都阻塞；profile 只要求声明的 channel；把静态 calibration 当 judge PASS。
2. **选了哪个 + 为什么**：选择 profile 只要求声明的 channel。retrieval profile 可在 judge off 下评估，但不得产生 judge quality 结论。
3. **放弃的代价**：全局阻塞会让独立客观通道不可用；静态 corpus 冒充 live judge evidence 会越过 C9b 结论边界。

