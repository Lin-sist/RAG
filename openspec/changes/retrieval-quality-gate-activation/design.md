# Design: C17 Retrieval Quality Gate Activation

## 1. Design Objective

在不改变 retrieval 算法、生产默认或指标公式的前提下，把 C10 的 DRAFT retrieval profile 补成可审计的 evidence-to-policy 闭环：

`固定执行契约 → 5-case canary → 150×3 raw evidence → strict compiler → 脱敏审阅包 → 用户批准阈值 → ACTIVE profile → locked median reference → 离线重放验证`

本 change 的核心不是“跑出一组高分”，而是保证任何 ACTIVE 数值都能回答：数据是谁、运行身份是否一致、失败是否被保留、外调是否在预算内、阈值是谁批准的、reference 如何从三次 evidence 得出。

## 2. Existing Components And Gaps

### 2.1 Reused Components

- C8 dataset manifest validator：固定 release/question/schema/fixture hashes、count/order/quota/review identity。
- `run_reproducible_rag_eval.py`：支持既有 KB、repeat/run-index、独立 report/details/metadata、plan-only/preflight 与 no-overwrite。
- `run_rag_eval.py`：生成 per-sample retrieval facts、Report status、run counts、aggregate retrieval metrics 和 rerank attribution。
- C10 profile/evaluator：固定 slices/rules、identity validation、hard floor/reference AND、`PASS/FAIL/NOT_EVALUABLE/INVALID` 与安全输出。

### 2.2 Missing Bridge

- 非 C7 plan 不完整展示 query embedding 上限，也没有 C17 固定 repeat/provider/budget manifest。
- 三次 full details 尚无 strict compiler；手工读取 aggregate 会漏掉 sample/repeat/identity/error/fallback 完整性。
- C10 evaluator 的 `--reference` 假定已有绑定 ACTIVE profile hash 的 summary，但没有从 DRAFT evidence 到 human-approved ACTIVE reference 的生成流程。
- 当前 profile target 全为空，无法直接用 evaluator 计算 rule observed；需要复用同一 slice/metric 计算逻辑，而不是复制另一套公式。

## 3. Proposed Architecture

### 3.1 C17 Reference Manifest

新增 `c17-retrieval-reference-v1` schema 与 tracked manifest，至少绑定：

- dataset manifest path/hash/release 与 expected sample count=150；
- target profile id 和 draft/active canonical path；
- selection mode=`full`、measured repeats=3、expected run indexes=`1,2,3`；
- run identity：retrieval-only、topK=5、minScore=0.3、rerank enabled；
- expected rerank attribution：requested/effective heuristic、fallback/model call=0；
- error policy：retrieve/rate-limit/retry/missing/unexpected 全为 0；
- external-call upper bounds：debug retrieval=450、query embedding=450、external rerank/ask/generation/judge=0；
- raw artifact policy、compiler version 与 output allowlist identity。

runner 增加独立 C17 manifest 参数，不复用 C7 arm manifest。参数/manifest drift 必须在 login 或任何 backend/provider call 前失败。

### 3.2 Identity Layers

必须区分两层 identity：

1. **Reference repeat strict identity**：三次 reference 之间 Git HEAD、dataset/selection、fixture/document/KB、tracked config、retrieval config、metric contracts、provider attribution 和 repeat total 必须相同。该层决定 evidence 是否 `COMPLETE/NOT_COMPARABLE`。
2. **Future candidate compatibility identity**：candidate 与 locked reference 必须匹配 profile、dataset、selection、run/metric/slice identity；candidate Git HEAD 作为 provenance 保留但允许不同，否则代码变更永远无法与 reference 比较。

numeric KB id、vector collection 和绝对路径只用于本地 raw validation，不进入 tracked reference。严格 repeat 可通过安全 KB name/marker、document title/content hash/chunk count 与 fixture hash 建立身份。

### 3.3 Plan Preflight Canary And Full State Machine

- `PLAN_VALID`：纯本地 manifest/dataset/command/budget 校验通过，calls=0。
- `PREFLIGHT_READY`：本机 backend 登录、固定 KB/fixture/document readiness 通过，mutation/retrieval/provider calls=0。
- `CANARY_CLEAN`：固定 5 IDs 各 1 次；status/identity/provider/error/retry 全满足，仅证明环境可进入 full。
- `REFERENCE_COMPLETE`：三个 run 各 150 observations，strict identity、完整性与 zero-error policy 全通过。
- `PENDING_THRESHOLD_APPROVAL`：脱敏 pack 已生成，profile 仍 DRAFT。
- `ACTIVE_REFERENCE_LOCKED`：用户批准数值，profile ACTIVE，median reference 绑定最终 profile hash，三个 source repeats 均离线通过。

任何阶段失败都保留 safe status/reason 并停止；不得自动 retry、重建 KB、删 observation 或越级。

### 3.4 Reference Compiler

新增纯本地 compiler：

- 输入：tracked C17 manifest、canonical profile、三个 details/metadata paths；
- validation：JSON/schema、details hash、run index、sample IDs/order/count、Report/channel status、runCounts、dataset/fixture/KB/config/Git、rerank attribution、errors/retries；
- metric calculation：抽取/复用 C10 evaluator 的纯 slice/rule observed 计算，不复制 retrieval 公式；DRAFT target 为 null 时仍可计算 observed，但不能产生 PASS；
- output：manifest/compiler identity、三份 artifact hash、expected/actual counts、status/reasons、每 rule denominator、run values、min/median/max/spread、safe provider/call facts；
- safety：no-overwrite；ordinary output 不包含 raw sample text、contexts、provider body、credentials、numeric KB id、vector collection 或绝对路径。

compiler 对失败 evidence 不输出 median reference；只有 `REFERENCE_COMPLETE` 才生成 threshold review pack。

### 3.5 Threshold Approval And Reference Lock

threshold review 不由工具自动决定。用户看到 12 条规则的三次分布后，批准每条：

- `target` hard floor；
- `maxAbsoluteRegression`；
- 必要时拒绝激活并要求重新取得 clean evidence，而不是调 dataset/算法凑数。

激活步骤：

1. canonical profile `profileVersion: v1-draft -> v1`；
2. `status: DRAFT -> ACTIVE`；
3. `thresholdStatus: PENDING_REFERENCE_EVIDENCE -> APPROVED`；
4. 写入用户批准的 12 条 target/tolerance；
5. 计算最终 profile SHA-256；
6. compiler 用原始三次 details 与最终 profile 重放；
7. locked reference 每条 `observed` 使用三次 median，并绑定最终 profile/dataset/run identity；
8. 三次 source repeat 分别对 hard/reference rule 重放，全部 required rules PASS 才完成 lock。

median 只承担 reference central value；min/max/spread 继续留在 evidence pack，不能被 reference 单值隐藏。

## 4. External Call And Runtime Fingerprint

### 4.1 Approved-later Call Shape

- canary：5 debug retrieval + 最多 5 query embedding；
- full：450 debug retrieval + 最多 450 query embedding；
- 两阶段总上限：455 + 455；
- external rerank、ask、generation、judge：0；
- retry：0；任何 429/timeout/provider error 保留并停止。

### 4.2 Data Egress

debug retrieval 的 query 可能通过 backend 发送到实际 embedding provider。canary 为 5 条 tracked question，full 为 150 条 tracked question ×3。因为复用已有 KB，本 change 不发送 fixture 文档做重新 embedding；heuristic rerank 不把 retrieved passages 发送给外部 reranker；未启用 ask/judge，不发送 contexts 给 LLM/judge。

### 4.3 Runtime Fingerprint Gate

tracked default 不是 runtime proof。canary 前需记录不含 secret 的：

- embedding adapter/provider、model、endpoint host/path；
- dimension、timeout、fallback disabled/enabled；
- backend Git HEAD、tracked config hashes；
- 账户费用或零费用依据、速率/并发/配额；
- retry=0 与 raw artifact handling。

任何 runtime fingerprint 与批准内容不一致都需要重新授权。

## 5. W0 Boundary

C17 不修改 OTel tracing/exporter 代码。live canary/full 的前置条件是独立 W0 closeout 事实；当前聚焦 `GenAiTracingConfigurationTest` 9/9 只作诊断。

- 若 W0 只需既有契约内测试稳定性修复：单独按 Type B 处理和验证，不混入 C17 diff。
- 若 W0 需要改变 runtime semantics：停止 C17 实现推进，先建立独立 Type C change；同一时间不得有两个 active Type C change。
- 若当前 full-suite evidence 已能按 W0 验收标准证明稳定：在 C17 tasks/log 引用该独立证据，不重写成 C17 成果。

## 6. Artifact Policy

### 6.1 Untracked Raw Artifacts

- 路径：`tmp/eval/c17/`；
- 每个 canary/full run 使用独立 basename 与 `--no-overwrite`；
- 包含 raw details/metadata/report，仅供本地审计；
- 不进入 Git，不复制到普通日志或回复。

### 6.2 Tracked Safe Artifacts

- C17 manifest/schema；
- `REFERENCE_COMPLETE` 脱敏 evidence pack；
- 用户批准后的 ACTIVE profile；
- 绑定 profile hash 的 locked reference summary；
- traceability/guide/AGENT_LOG 的安全事实。

tracked output 仅含 allowlisted identity/hash/count/status/rule aggregates，不含 secret 或原始业务文本。

## 7. Implementation Slices

1. **Offline manifest + budget RED/GREEN**：schema/loader、参数 drift、plan call counts、canary/full identity。
2. **Strict compiler RED/GREEN**：450 pair completeness、repeat identity、provider attribution、error/retry、safe status。
3. **Rule distribution RED/GREEN**：复用 C10 metric calculation，输出 run values/min/median/max/spread，DRAFT 不产生 PASS。
4. **Safety/compatibility**：raw-vs-tracked boundary、no-overwrite、historical artifacts 不追认、existing C10 tests 回归。
5. **Plan/preflight**：零外调验证；W0 未关闭或 KB 不 READY 时停止。
6. **Canary/full evidence**：分别取得授权后执行，不自动重试。
7. **Threshold review/activation**：用户批准数值后更新 profile、锁定 median reference、离线重放。
8. **Acceptance/closeout**：全量 Python/static checks，用户验收后 baseline/archive/IDLE。

## 8. Verification Matrix

| 层级 | 验证 | 外调 |
|---|---|---:|
| schema/unit | manifest/profile/reference schema、invalid codes、call budget、identity drift、450 completeness、median/rounding、安全输出 | 0 |
| focused Python | runner/quality evaluator/compiler tests | 0 |
| full Python | `python -B -m unittest discover -s scripts -p 'test_*.py'` | 0 |
| plan-only | v2/full/repeat/config/call shape | 0 |
| preflight | local backend + existing KB readiness，无 mutation | 0 provider |
| canary | 5 fixed samples、heuristic attribution、zero retry/fallback/model rerank | 最多 5 embedding |
| full reference | 150×3、全部 observations 与 identity | 最多 450 embedding |
| activation replay | 三份 existing details + final profile/reference | 0 |
| static safety | SensitiveLogs、绝对路径/raw text、relative links、protected paths、`git diff --check` | 0 |

Java/POM/frontend/runtime/API 无改动时 Maven/frontend build 可记为 `SKIPPED`；但这不替代 C17 live 前独立 W0 closeout。

## 9. Failure And Recovery Semantics

- plan/schema invalid：修复 planning/offline artifact，仍为 0 calls。
- preflight not ready：停止；不自动建库或上传。
- canary failure：保留 raw artifact 与安全分类，不进入 full、不自动 retry。
- full partial/identity drift：reference status 不是 COMPLETE；不得删失败 run/sample 或补跑后拼接成功子集。若要重跑，废弃整个受影响 reference release，使用新 execution id 和重新授权的调用预算。
- threshold 未批准：profile 保持 DRAFT；evidence pack 可审阅但不可被 evaluator 当 active reference。
- activation replay 失败：撤回未接受的 ACTIVE edit，保留 DRAFT/证据事实并重新审阅；不得降低阈值来掩盖 contract error。
- accepted ACTIVE profile 后发现问题：新建 profile version/change，不原地改写已接受 profile/reference。

## 10. 决策记录

### 决策 1：C17 执行约束放在现有 profile 还是独立 reference manifest
- **面临的选择**：扩展通用 `rag-quality-gate-profile-v1`；复用 C7 arm manifest；新增 C17 reference manifest。
- **选了哪个 + 为什么**：选择新增 C17 reference manifest。profile 负责长期门禁 policy，C7 manifest 负责双 arm A/B，而 C17 需要三次单 arm reference、调用预算和 expected heuristic attribution，职责不同。
- **放弃的代价**：扩展 profile 会把一次性 evidence 运行细节污染长期 policy；复用 C7 会制造不存在的双 arm/model 语义；独立 manifest 的代价是多一个 schema/validator。

### 决策 2：preflight 不 READY 时如何处理 KB
- **面临的选择**：自动重建并上传 fixture；提示后继续用不完整 KB；强制 `--keep-existing` 并停止。
- **选了哪个 + 为什么**：选择强制复用既有固定 KB，任何缺失/不完整都 fail closed。C17 批准的 455 query embedding 预算不包含文档 indexing embedding 或数据 mutation。
- **放弃的代价**：自动重建会扩大调用、费用与持久化范围；继续使用不完整 KB 会生成伪低基线；fail closed 可能要求另开准备任务。

### 决策 3：是否需要 canary
- **面临的选择**：直接运行 150×3；复用旧报告当 canary；固定 5 类各 1 条的新 canary。
- **选了哪个 + 为什么**：选择固定 5-case canary，只用于发现环境/身份/provider 错误，并在 full 前设置独立授权点。
- **放弃的代价**：直接 full 可能在配置错误时浪费 450 calls；旧报告不能证明当前 runtime；canary 的代价是额外最多 5 次 retrieval/embedding。

### 决策 4：失败调用是否自动 retry
- **面临的选择**：对 timeout/429 自动重试；人工删掉失败 observation 后补跑；zero retry、整轮保留并停止。
- **选了哪个 + 为什么**：选择 zero retry。reference 要固定 150×3 identity，重试或成功子集会改变 exposure、调用量与错误分母。
- **放弃的代价**：自动重试会模糊真实稳定性和预算；补跑拼接会破坏 repeat identity；zero retry 可能需要重新授权整轮重跑。

### 决策 5：三次 reference 的严格身份包含什么
- **面临的选择**：只比 dataset/profile；再加 Git/config；固定 dataset、selection、fixture/KB/document、Git/config、run/metric、provider attribution 全集。
- **选了哪个 + 为什么**：选择完整 strict identity。否则 observed 变化无法区分算法波动、语料漂移和 provider/config 漂移。
- **放弃的代价**：只比少量字段容易形成伪可比；完整 identity 增加 preflight/metadata 校验，但换来可追溯性。

### 决策 6：未来 candidate 是否必须与 reference Git HEAD 相同
- **面临的选择**：candidate/reference Git 完全相同；完全忽略 Git；reference repeats 要同 HEAD，但 future candidate 允许不同 HEAD并保留 provenance。
- **选了哪个 + 为什么**：选择分层身份。reference repeats 必须同 HEAD；future candidate 本来就是用来评估新代码，不能要求同 HEAD，但必须匹配 profile/dataset/run/metric identity。
- **放弃的代价**：永远同 HEAD 会让 regression gate 无法评估变更；忽略 Git 会失去追责；分层规则需要 compiler/evaluator 清楚区分两类比较。

### 决策 7：三次 observed 如何形成 locked reference 单值
- **面临的选择**：取最好值；取最差值；取平均值；取 median 并保留 min/max/spread。
- **选了哪个 + 为什么**：选择 median 作为 locked central value，同时在 evidence pack 保留全部分布。median 不会被一个异常 run 拉动，也不会像最好值那样制造易抖动门禁。
- **放弃的代价**：最好值过于激进；最差值可能把异常低值合法化；平均值受异常影响；median 的代价是仍需额外查看 spread 才能理解波动。

### 决策 8：hard floor 和 tolerance 如何产生
- **面临的选择**：工具自动从 reference 学习；预置行业经验值；完整 evidence 后由用户批准 exact values。
- **选了哪个 + 为什么**：选择人审。当前项目没有足够外部代表性，自动学习会把现状缺陷固化，经验值也没有本项目证据。
- **放弃的代价**：自动方案快但不可审计且易过拟合；人审需要一个中间闸门，但责任和理由清楚。

### 决策 9：阈值审阅包展示什么
- **面临的选择**：只给 aggregate median；复制全部 raw details；给每 rule 三次值、denominator、min/median/max/spread 与 safe identity。
- **选了哪个 + 为什么**：选择逐规则分布包。它足够判断稳定性，又不复制问题、答案、上下文或 provider body。
- **放弃的代价**：只看 median 会隐藏波动；raw details 有泄露和审阅噪声；脱敏分布需要 compiler 与 allowlist。

### 决策 10：compiler 是否复制 C10 metric 公式
- **面临的选择**：在 compiler 重写 slice/metric 计算；调用 evaluator CLI 多次后解析输出；抽取并复用 evaluator 的纯 observed 计算。
- **选了哪个 + 为什么**：选择复用纯计算逻辑，保持 rule/slice/denominator 单一事实源，同时避免用 DRAFT profile 调 evaluator 时被 status/target 提前阻断。
- **放弃的代价**：复制公式会漂移；解析 CLI 输出脆弱；抽取纯函数会有小范围重构，但仍限于 evaluation tooling。

### 决策 11：draft profile 如何提升为 active
- **面临的选择**：不改 version 直接填数；保留 draft 文件并新建不同 profile id；沿用 canonical profile id/path，但显式 `v1-draft -> v1` 并重新绑定 hash。
- **选了哪个 + 为什么**：选择显式 version transition。当前 draft 本来就是首个 v1 的 scaffolding；保持 canonical 路径降低 consumer 漂移，version/hash 变化又能防止静默改数。
- **放弃的代价**：不改 version 会隐藏语义变化；新 profile id 会割裂既有 C10 target；version transition 要在激活时更新测试和文档引用。

### 决策 12：raw evidence 放在哪里
- **面临的选择**：全部提交 Git；只保留最终 aggregate、丢弃 raw；raw 放 ignored tmp，tracked 只存脱敏 pack/reference。
- **选了哪个 + 为什么**：选择 raw local + safe tracked。既保留本地可审计性，又避免把 question/context/provider payload 纳入长期仓库。
- **放弃的代价**：全提交扩大数据和隐私面；丢弃 raw 无法复核；本地 raw 的代价是需要 no-overwrite、保留期与手工备份纪律。

### 决策 13：W0 OTel 债务是否并入 C17
- **面临的选择**：顺手在 C17 修；忽略全仓非 GREEN；作为 live evidence 的外部硬前置，需改代码时独立处理。
- **选了哪个 + 为什么**：选择独立硬前置。OTel 与 retrieval gate 无合同交集，并入会污染 diff；忽略则违反蓝图 W0→W1 顺序。
- **放弃的代价**：顺手修会扩大 change；忽略会让阶段证据不可信；独立前置可能让 C17 等待，但保持范围清楚。

### 决策 14：C17 是否扩大到 generation/judge 或生产默认
- **面临的选择**：一次激活 retrieval/generation/judge；只激活 retrieval；同时切换默认 reranker/CI required gate。
- **选了哪个 + 为什么**：选择 retrieval-only，不改生产默认或 CI。generation evidence、judge calibration 和 profile 分别属于 C18–C20，当前没有授权和完整证据。
- **放弃的代价**：一次扩张会把未校准通道包装成成熟门禁；切默认会让 baseline 不可比较；窄切片的代价是后续仍需独立阶段。
