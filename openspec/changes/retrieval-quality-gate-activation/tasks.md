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
- [x] 在任何 C17 live call 前固定 clean Git HEAD=`82707068deaea038e96d842d4253cf50c40b3070`、v2/150 dataset、DRAFT profile、C17 manifest、既有 KB 的 3/3 COMPLETED fixtures、tracked config hash 与 ignored `tmp/eval/c17/` no-overwrite raw policy。

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
- [x] 原批准边界明确 C17 不修改 dataset、retrieval/rerank/embedding/metric 公式、production QA、默认 provider、generation/citation/no-answer answer quality、judge 或 CI 平台配置；其中 embedding provider/固定 KB generation 边界已由 2026-08-31 section 7A 修订取代，其余边界继续有效。

## 6. Offline Verification Before Any Live Call

- [x] 运行 compiler/runner/evaluator 聚焦 tests，记录 RED→GREEN 证据。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 运行 C17 full manifest 的 plan-only，确认 selected=150、repeat=3、debug retrieval=450、query embedding upper bound=450、其他外部通道=0。
- [x] 运行 fixed canary plan-only，确认 5 IDs、1 repeat、debug retrieval/query embedding upper bound=5。
- [x] 运行 SensitiveLogs、raw text/absolute path、Markdown links、protected paths、historical artifact 非覆盖与 `git diff --check`。
- [x] Java/POM/frontend/runtime/API 无改动时，将 Maven/frontend build 记为 `SKIPPED`；同时保留独立 W0 closeout 前置，不用此 SKIPPED 消除 W0 债务。

## 7. Preflight And Canary Authorization Gate

- [x] 以 `--preflight-only --keep-existing` 验证本机 backend 与固定 KB：首版只检查 document 状态而误报 `READY`；canary 暴露 legacy vector readiness 后已用 TDD 增加只读 statistics/count 门禁，复验为 `BLOCKED / VECTOR_READINESS_UNAVAILABLE`、expected vectors=50。
- [x] 修复后 preflight 不 READY，因此停止；未创建/删除 KB、未上传 fixture、未触发 indexing embedding。只读 Milvus 盘点确认 source collection 存在、vector count=50、dimension=2048。
- [x] runtime fingerprint：OpenAI-compatible NVIDIA hosted `nvidia/llama-nemotron-embed-1b-v2`、`integrate.api.nvidia.com/v1/embeddings`、dimension=2048、timeout=60000ms、fallback=false、proxy=`127.0.0.1:7897` reachable、Git=`8270706`、tracked config SHA-256=`d66479a5...a6575`、retry=0；官方 NIM FAQ 支持 Developer Program 原型/研发免费访问，但账户 quota/rate-limit 仍 unknown。
- [x] 已向用户披露 canary：5 debug retrieval、最多 5 query embedding、5 条 tracked eval question 经本机代理出站到 NVIDIA；external rerank/ask/generation/judge=0、retry=0、预期直接费用=0 但 quota 不可本地证明。
- [x] 用户在上述具体数据目的地与范围披露后明确批准 canary 出站。
- [x] 执行固定 5-case canary 并按门禁停止：5/5 local debug retrieval 均返回 `VECTOR_INDEX_NOT_READY` / HTTP 503，Report status=`FAILED`、retrieveErrors=5、rateLimit/retry/fallback/model rerank=0；失败发生在 embedding 前，不自动重试。
- [x] canary 仅为环境检查，未进入 reference aggregate、不形成质量结论；full 450/450 未启动。
- [x] 用户明确批准一次真实 zero-retry shadow migration：source 只读、创建 deterministic tenant-aware shadow、复制/审计 50 vectors、成功后原子切换 mapping/readiness；provider/embedding/rerank/LLM=0、数据不出站、不删除 source、不自动清理。
- [x] 首次真实执行在 source audit 50/50 后失败并停止：legacy JSON 的等价浮点 `kbId` 被 `MilvusVectorStore` 字符串比较误判为 scope conflict；MySQL=`AUDIT_FAILED`、source mapping 保持 active、source=50、shadow=0，未重试或清理。
- [x] TDD 修复既有 C13b contract bug：scope marker 使用精确数值等价，接受 `11.0 == 11` 且拒绝 IEEE-754 大整数舍入伪相等；Milvus/shadow 聚焦 tests 与最终全仓 Maven 均通过。
- [x] 在披露失败现场、修复证据、现存空 shadow 与 rollback 边界后，用户于 2026-08-31 重新明确授权一次 migration retry；该次 zero-retry 执行成功：`READY`、expected/observed/migrated=`50/50/50`、missing/mismatch=`0/0`，MySQL=`SHADOW_ACTIVE`，source 保留 50，tenant-scoped strong query 与 50 个预期 ID 读回均为 50。
- [x] migration 后重新启动 backend 并执行 mutation-free preflight：status/vector=`READY/READY`、vector count=`50/50`、fixtures matched/missing/incomplete=`3/0/0`；仅有 localhost 登录/只读 KB 请求，retrieval/provider/embedding/rerank/ask/generation/judge calls=0。
- [x] 用户独立授权一次 migration 后 fixed 5-case canary；执行前同一 HEAD/config 的 preflight 再次 `READY / 50/50 / fixtures 3/3`，输出使用新的 ignored no-overwrite 路径。
- [x] 执行并按门禁停止：5/5 debug retrieval 均在 NVIDIA query embedding 得到 HTTP 410 Gone，实际 provider/query embedding calls=5、retrieveErrors=5、rateLimit/retry/fallback/model rerank=0；canary=`FAILED`，未形成质量 evidence、未启动 full。
- [x] 只读核对 NVIDIA 官方模型页确认 `llama-nemotron-embed-1b-v2` hosted NIM endpoint 已 `Deprecated`；本地 TDD 证明 WebClient 保留 `/v1/embeddings`，因此未错误修改 URI。替代模型与旧向量不兼容，provider 切换/KB 重建超出 C17 原批准范围，等待独立决策与授权。

## 7A. Deprecated Provider Recovery And New Embedding Generation

- [x] 用户授权修订 C17 规划以迁移新 embedding model；本阶段仅允许 proposal/design/tasks/spec delta，provider/backend calls=0、business data outbound=0、KB mutation/rebuild=0，不修改代码、配置、secret、mapping 或 accepted baseline。
- [x] 只读调研 NVIDIA 官方模型页、Embedding NIM release notes/support matrix/API/pricing contract；用户确认选择 `nvidia/nemotron-3-embed-1b`：当前 Build 页面为 Free Endpoint，NIM 支持 text query/passage、OpenAI-compatible embeddings、native float dimension=2048、validated max sequence length=4096；Developer Program prototyping 的 NVIDIA API 直接费用依据=0。此选择不构成账户 entitlement、无限调用、生产免费、稳定 SLA 或质量收益证明。
- [x] 排除 `llama-nemotron-embed-1b-v2` 和 `llama-3.2-nemoretriever-300m-embed-v2` hosted endpoint，因为当前官方页面均为 `Deprecated`；自托管旧 model 不纳入 C17，避免增加 GPU/NIM 运维范围。
- [x] 修订 model-bound identity 和迁移边界：相同 2048 dimensions 不等于同一 embedding space；旧 50-vector collection 只读保留，新模型使用独立 deterministic collection generation，禁止新 query 搜旧 vectors 或新旧 passage vectors 混写。
- [x] 将未来执行拆为四个独立授权闸门：1 synthetic embedding item smoke；固定 3 fixtures / 50 passage items rebuild；rebuild READY 后的新 fixed 5-case canary；canary clean 后的 full 150×3。所有阶段 retry=0，授权互不继承。
- [x] 零外调审计当前 provider adapter/indexing pipeline：确认 request fields、response count/index/model/2048-finite validation、actual-model cache identity、zero-retry config、model-bound mapping/query fail-closed、new-generation rebuild 与 manifest/compiler identity 均存在实现缺口；backend/provider calls=0、KB/collection mutation=0。
- [x] 冻结 rebuild 预算与迁移 contract：固定 3 documents 的 chunk counts=`11/14/25`、exact passage items=50、adapter batch items<=5、HTTP request upper bound=11、retry=0；新 generation 独立命名，失败不复用；50/50/50 强读回与旧 identity CAS 原子切换后才 READY。
- [x] 用户已批准并完成 C17 model-migration offline implementation：adapter 精确请求/响应契约、actual-model cache identity、no-cache passage batch、retry=0、新模型 tracked config、V13 active/source/shadow model identity、query/preflight/compiler fail-closed、default-off 独立 50-item/11-request generation rebuild 与强读回 CAS 已用 TDD 落地；Java 聚焦 tests、Python 236 tests 与全仓 Maven 631 tests 均为 0 failures / 0 errors。本阶段 provider/backend calls=0、KB/Milvus/SQL mutation=0。
- [x] 用户已单独授权并完成 1-item synthetic endpoint smoke：`nvidia/nemotron-3-embed-1b`、`integrate.api.nvidia.com/v1/embeddings`、query/text/float/float/NONE、无 dimensions、proxy=`127.0.0.1:7897`、timeout=60s、attempts=1、retry=0；HTTP 200，response model 精确匹配，item/index=`1/0`、dimension=2048、all finite，latency=30472ms。仅发送非业务 synthetic 文本，KB/Milvus/SQL mutation=0；本次授权已消耗。
- [x] synthetic smoke clean 后，重新披露并取得固定 KB rebuild 授权：用户于 2026-09-06 明确授权 50 passage items、最多 11 requests、3 个 tracked fixture chunks 向 NVIDIA NIM 出站、retry=0、V13/new collection/source retain/CAS 语义；最终命令在 Maven 参数解析阶段失败，测试 JVM 未启动且 provider/mutation=0，本次一次性授权按 zero-retry 规则耗尽。
- [x] 用户重新授权相同 fixed rebuild；修正 PowerShell 参数传递后测试 JVM 已启动，但直接 `surefire:test` 使用旧本机 `rag-core` 而在 plan 前触发 `NoSuchMethodError`。provider/V13/KB/Milvus mutation=0；授权耗尽后未重跑。随后零外调安装当前 reactor、`javap` 核验所需方法、纯 mock rebuild tests 通过，classpath 已就绪。
- [x] 建立并验证两阶段执行入口：当前 reactor 编译通过，execute=false 的真实 MySQL/Milvus 预演=`READY`，确认 target identity、50 items、11-request bound 与 `c17g1` collection 不存在；真实执行在进程创建前因本次数据出站未获明确重新授权而被权限闸门拒绝，provider/V13/KB/Milvus mutation=0。
- [x] 用户明确重新授权后执行 `c17g1`：V13 成功应用；临时计数器在 adapter 内部拆批前误把 25 items 当作单 HTTP request，真实 provider calls/items=0，rebuild 以 `MODEL_REBUILD_HTTP_BUDGET_EXCEEDED` fail closed。旧 mapping/source 保留、target collection 未创建；`c17g1` 不复用。
- [x] 修正执行护栏为按 `ceil(documentItems/5)` 计算 HTTP batches；新 generation `c17g2` 零外调预演=`READY`，target identity/50 items/11-request bound/collection absence 均通过，等待独立授权。
- [x] 2026-09-08 离线补强 rebuild 读回审计：精确数值比较兼容 JSON 浮点 ID，同时拒绝精度丢失/错误 scope、额外/重复/缺失 ID、空条目、空 metadata、非有限向量及 model/contract/generation 漂移；聚焦 18 tests 通过，全仓 646 tests、0 failures/errors、21 skipped。V13 迁移测试断言已同步，真实 MySQL/Testcontainers 验证因 Docker 不可用跳过，不作为真实 rebuild 证据。
- [x] 2026-09-08 将 runner/manifest 的待执行 generation 同步为 c17g2，拒绝旧 c17g1 manifest、KB preflight identity 与整组三次 reference evidence；Python 238 tests 通过，canary/full plan-only 分别为 5/5 与 450/450，其他业务外调通道=0。此次为离线计划同步，未重跑真实 MySQL/Milvus preflight，未执行 rebuild。
- [x] 用户随后明确同意一次 c17g2 fixed rebuild；恢复 Docker 后真实只读预检发现 c17g2 target 已存在，SQL 的历史状态为 MODEL_REBUILD_FAILED/shadowGeneration=c17g2/50-50-50-0-50，错误类别 MODEL_REBUILD_READBACK_MISMATCH；本轮执行前停止，provider requests/items=0/0，未切换或清理失败代际。该真实状态取代此前未刷新的 target-absence 记录。
- [x] 保留旧 source=50 和失败 target=50；依既定失败代际不复用规则，将待执行 manifest/runner 同步为 c17g3，并完成当前源码 reactor/install/classpath/javac 与真实只读预演：V13、3 fixtures/50 chunks/source vectors、target absent、11 requests upper bound、batch<=5、retry=0。Python 238 tests/OK、canary/full plan-only=5/5 与 450/450；真实 c17g3 执行待重新授权。
- [x] 用户独立授权后，于 2026-09-08 在 c17g3 执行唯一一次 zero-retry rebuild：11/11 HTTP 200、50 passage items validated，expected/observed/read-back=`50/50/50`、missing/mismatch=`0/0`、exact ID set=50、finite 2048-d/model/request/generation 匹配，CAS 切换后 `MODEL_REBUILD_READY / SQL READY`。旧 source=50，未清理失败 c17g2；授权已消耗，未重跑。
- [x] 独立只读维护 verify 确认 SQL/Milvus 的新 model/request/endpoint/generation、fixtures=`3/3`、vectors/expected-ID read-back=`50/50`、旧 source=50，provider calls=0；证据保存在脱敏 c17-model-rebuild-c17g3-summary.json。
- [ ] rebuild 后通过应用 HTTP 的 --preflight-only --keep-existing 完成 mutation-free preflight 和 runtime fingerprint，确认新 model/request/collection generation、fixtures=`3/3`、vectors=`50/50`；provider calls=0。本轮 backend HTTP 未启动，独立维护 verify 不替代此项。
- [ ] 用户重新授权新模型 fixed 5-case canary；最多 5 debug retrieval + 5 query embedding、external rerank/ask/generation/judge=0、retry=0。只有 clean canary 才进入 section 8。
- [ ] 将旧模型 410 canary 保留为 deprecated-provider failure evidence；不得混入新 generation reference aggregate、阈值或质量结论。

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

## 2026-09-08 HTTP/canary 最新执行补充

- [x] 应用 HTTP preflight READY：fixtures3、vectors50/50、model/request/generation匹配；provider0，调度器/Flyway/bootstrap关闭。
- [x] 用户授权后执行唯一canary：5 retrieval、5 provider HTTP200、retrieveErrors3、retry/fallback/model rerank/ask/generation/judge0；FAILED，不进入full。
- [x] 离线实际QueryEngine核算：5 IDs变体数1/2/5/2/1，canary/full冷缓存embedding上限11/1353；原5/450预算存在gap。
- [ ] 修订查询变体预算契约并验证、披露变体文本出站和新上限、取得新执行授权；不得靠改检索算法或假设缓存命中绕过预算。

## 2026-09-08 查询变体预算修订（用户批准）

本节覆盖此前按每样本一次embedding的5/450预算。保留当前检索算法，按当前QueryEngine离线生成的全部query variants计算冷缓存上限：canary固定5个debug retrieval、最多11次query embedding（各样本1/2/5/2/1）；full固定450个debug retrieval、最多1353次query embedding（每repeat451）。缓存命中可减少实际调用，但不得缩减授权上限。tracked问题及其确定性变体发送至既定NVIDIA NIM endpoint；external rerank/ask/generation/judge仍为0。

用户明确说明当前NVIDIA NIM账户免费且未绑定支付方式；本任务按用户账户声明记录直接费用0，不再将费用作为反复请示原因，仍记录限流、超时及实际请求数。用户明确授权修订契约并持续诊断/修复、以新no-overwrite执行身份验证canary直至通过；每轮内部retry=0，失败保留完整证据，诊断后才能开始下一轮。该授权不扩大至KB重建/清理、baseline批准或发布。full仍须canary clean；此前full授权不自行扩大至1353新上限，本轮只推进canary。

- [x] 修订预算及query源码漂移门禁，239 Python tests/OK，提交9080b92。
- [x] r2 HTTP preflight READY、CANARY_CLEAN：5/5检索成功、6次新增provider HTTP200、缓存5、errors/retry/fallback/model rerank0。
- [ ] full新1353上限获授权后执行；canary不进入质量reference。
