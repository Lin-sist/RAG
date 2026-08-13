# Tasks: C17 Retrieval Quality Gate Activation

## 0. Approval And Boundary

- [x] 用户要求在 readiness 通过后开始阶段规划。
- [x] 启动事实：HEAD=`701ade3`，工作树干净，`main...origin/main [ahead 1]`，`ACTIVE_TASK=IDLE`，无其他 active change，C1–C16 已归档。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta，并将 `.ai/ACTIVE_TASK.md` 指向唯一 C17 change。
- [x] 规划阶段提交责任为 `用户手动提交`；规划获批后用户已授权本 change 的本地 Agent commit，仍不包含 push、PR 或部署。
- [x] 本规划阶段真实 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- [x] 用户审阅并批准 proposal、design 14 条决策记录与 `evaluation` delta 的 4 requirements / 12 scenarios。
- [x] 用户明确授权进入 offline TDD implementation，并授权计划内本地 commit；该授权不包含 canary/full provider calls、阈值批准、baseline acceptance、archive、push、PR 或部署。
- [x] 用户在 W0 伪改动/时序问题处理完成后，授权继续完成 C17 剩余内容；本地 commit 仍不包含 push、PR 或部署，live call 仍须先完成 runtime 披露并遵守 canary/full 固定预算。

## 1. W0 And Runtime Readiness Prerequisite

- [x] 引用独立 W0 closeout commit `f2f0ec3`：聚焦 12 tests 与全仓 617 tests 均为 0 failures / 0 errors，不再以原聚焦 9/9 覆盖全仓 unavailable-collector 时序债务。
- [x] W0 确认为 logging 全局状态重置后的测试稳定性问题，已按独立 Type B 修复/验证/提交；OTel diff 未混入 C17 提交。
- [x] W0 不需要改变 runtime semantics，因此无需停止 C17 或创建第二个 Type C change。
- [ ] 在任何 C17 live call 前固定 clean Git HEAD、dataset/profile/manifest、KB/fixture/document、tracked config 与 raw artifact policy。

## 2. C17 Manifest And Plan Budget

- [x] RED：manifest schema/version、dataset/profile identity、selection、repeat、run config、expected provider、error/retry policy 或调用预算缺失/漂移时，在 backend call 前返回稳定 invalid code。
- [x] RED：full 计划不是 150×3、canary IDs/数量漂移、`--keep-existing` 缺失、include-ask/judge/model rerank 非零时 fail closed。
- [x] GREEN：新增 `c17-retrieval-reference-v1` schema、tracked manifest、loader 与 canonical hash。
- [x] GREEN：runner plan 对 canary/full 显示 debug retrieval、query embedding、external rerank、ask、generation、judge 的精确上限。
- [x] GREEN：C17 manifest 与 C7 arm manifest 职责隔离，禁止同时使用或互相冒充。

## 3. Strict Reference Compiler

- [x] RED：三个 run 的 Git/config/fixture/KB/document/dataset/selection/retrieval/metric/repeat identity 任一漂移均为 `NOT_COMPARABLE`。
- [x] RED：missing/unexpected run/sample、Report status 非 `RETRIEVAL_ONLY`、retrieve/rate-limit/retry/fallback/model rerank call 任一非零均阻止 `COMPLETE`。
- [x] RED：compiler 不得删除失败 observation、缩小 denominator、从成功子集计算或把缺失填 0。
- [x] GREEN：新增纯本地 compiler，按 `runIndex + sampleId` 校验 exact 450 observations 与三份 details/metadata hash。
- [x] GREEN：status 固定为 `COMPLETE / INCOMPLETE / NOT_COMPARABLE / INVALID`，保留 expected/actual counts 和 safe reasons。

## 4. Rule Distribution And Reference Lock

- [x] RED：DRAFT target 为 null 时仍能用 C10 同一纯计算逻辑得到 12 条 rule observed，但不能产生 gate PASS 或 locked reference。
- [x] RED：三次 observed 的 min/median/max/spread 与手算 fixture 一致，偶数/奇数、有限数、rounding 语义稳定。
- [x] GREEN：抽取并复用 evaluator 的 slice/rule observed 计算，避免复制指标公式。
- [x] GREEN：生成脱敏 threshold review pack，包含每次 denominator/observed 与 min/median/max/spread，不含 raw sample/provider 内容。
- [x] RED：ACTIVE profile hash/version/dataset/run/rule identity 不匹配时不能生成 locked reference。
- [x] GREEN：用户批准阈值后，以 median 生成 `reference.rules[].observed`，绑定最终 ACTIVE profile SHA-256。
- [ ] GREEN：三个 source reference repeat 对最终 hard floor/reference tolerance 分别重放且 required rules 全部 PASS。

## 5. Safety Compatibility And Documentation

- [x] raw report/details/metadata 只写 `tmp/eval/c17/` 且 `--no-overwrite`；tracked artifact 使用 schema + allowlist。
- [x] 普通/跟踪输出不包含 question、expected answer/context、retrieved context、provider body、secret、Authorization、numeric KB id/vector collection 或绝对路径。
- [x] C7/C8/C9/C10 历史 artifacts 保持原样，不能按文件名或 aggregate 追认为 C17 reference。
- [x] 当前 DRAFT profile 在阈值批准前继续得到 `NOT_EVALUABLE/4`；existing C10 evaluator/status/exit-code tests 保持兼容。
- [x] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的 plan/preflight/canary/full/compiler/review/activation/no-overwrite 与 external-call boundary。
- [x] 明确 C17 不修改 dataset、retrieval/rerank/embedding/metric 公式、production QA、默认 provider、generation/citation/no-answer answer quality、judge 或 CI 平台配置。

## 6. Offline Verification Before Any Live Call

- [x] 运行 compiler/runner/evaluator 聚焦 tests，记录 RED→GREEN 证据。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 运行 C17 full manifest 的 plan-only，确认 selected=150、repeat=3、debug retrieval=450、query embedding upper bound=450、其他外部通道=0。
- [x] 运行 fixed canary plan-only，确认 5 IDs、1 repeat、debug retrieval/query embedding upper bound=5。
- [x] 运行 SensitiveLogs、raw text/absolute path、Markdown links、protected paths、historical artifact 非覆盖与 `git diff --check`。
- [x] Java/POM/frontend/runtime/API 无改动时，将 Maven/frontend build 记为 `SKIPPED`；同时保留独立 W0 closeout 前置，不用此 SKIPPED 消除 W0 债务。

## 7. Preflight And Canary Authorization Gate

- [ ] 以 `--preflight-only --keep-existing` 验证本机 backend、固定 KB、三份 fixture/document 状态；provider calls=0、mutation=0。
- [ ] 若 preflight 不 READY，停止；不创建/删除 KB、不上传 fixture、不触发 indexing embedding。
- [ ] 记录不含 secret 的 runtime embedding provider/model/endpoint host/path/dimension/timeout/fallback、Git/config hash、费用或零费用依据、限流/配额与 retry=0。
- [ ] 向用户披露 canary：5 debug retrieval、最多 5 query embedding、5 条 tracked question 可能出站；取得单独授权。
- [ ] 执行固定 5-case canary；任何 auth/429/timeout/retrieve error、identity drift、fallback/model rerank call 均停止且不自动重试。
- [ ] 证明 canary 仅为环境检查，不进入 reference aggregate、不形成质量结论。

## 8. Full Reference Authorization And Execution

- [ ] canary clean 后重新披露 full：450 debug retrieval、最多 450 query embedding、external rerank/ask/generation/judge=0、retry=0、数据出站与 raw artifact 策略。
- [ ] 用户单独授权 full reference calls；canary 授权不得自动扩展为 full。
- [ ] 使用同一 clean HEAD/config/KB/fixture identity 执行 150×3，并为每个 run 使用独立 no-overwrite output。
- [ ] compiler 验证 exact 450 observations、3 run indexes、150 sample IDs/order、zero errors/retries/fallback/model rerank calls。
- [ ] 生成 `REFERENCE_COMPLETE / PENDING_THRESHOLD_APPROVAL` 脱敏 evidence pack；若非 COMPLETE，保持 DRAFT 并停止。

## 9. Threshold Review And Activation

- [ ] 向用户提交 12 条 rules 的 denominator、三次 observed、min/median/max/spread 与适用边界，不预先填值。
- [ ] 用户批准每条 hard floor 与 `maxAbsoluteRegression`；若拒绝或证据不足，profile 继续 DRAFT。
- [ ] 将 canonical profile 显式从 `v1-draft / DRAFT / PENDING_REFERENCE_EVIDENCE` 提升为 `v1 / ACTIVE / APPROVED`，写入批准数值。
- [ ] 生成绑定最终 profile hash 的 locked median reference，并离线重放三个 source repeats。
- [ ] 确认 ACTIVE profile/reference 可使完整兼容 evidence 得到稳定 PASS/FAIL，同时 missing/identity/error evidence 仍 fail closed。

## 10. Acceptance And Closeout

- [ ] 汇总 offline、W0、preflight、canary、full、compiler、threshold approval、activation replay 的证据与全部 skipped 边界。
- [ ] 用户最终验收 C17 的 4 requirements / 12 scenarios、ACTIVE profile、locked reference 和结论边界。
- [ ] 将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`，验证 exact suffix/无重复 requirement title。
- [ ] 同步 project/architecture/roadmap/optimization/eval guide 与 append-only `.ai/AGENT_LOG.md`。
- [ ] 将 change 归档到 `openspec/changes/archive/<date>-retrieval-quality-gate-activation/` 并恢复 `.ai/ACTIVE_TASK.md=IDLE`。
- [ ] 提交责任为本 change 计划内 `Agent 提交`；baseline acceptance、archive、push、PR、deploy 均不从实现或外调授权自动继承。
