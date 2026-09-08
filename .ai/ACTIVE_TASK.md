# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`C17G3_FULL_PREFLIGHT_READY_AWAITING_EXPLICIT_EGRESS_APPROVAL`
- 目标：以固定 `rag-eval-dev-v2` 150×3 retrieval reference、严格身份/完整性 compiler、用户阈值审阅和 locked median reference，把首个 C10 retrieval profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进到可验收的 `ACTIVE / APPROVED`。

## Current Boundary

- proposal、design、tasks 与 `evaluation` spec delta 已获用户批准；offline tooling 与 W0 已完成。首次 canary 以 `VECTOR_INDEX_NOT_READY` 失败，修复后的 mutation-free preflight 已正确 BLOCKED；用户随后明确批准一次 zero-retry shadow migration，首次真实执行已失败并停止，仍不修改 profile 数值或 accepted baseline。
- W0 OTel 全仓时序债务已由独立 Type B commit `f2f0ec3` 关闭：聚焦 12 tests 与全仓 617 tests 均为 0 failures / 0 errors；OTel diff 未混入 C17 提交。
- offline implementation 的真实 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- 用户已明确批准固定 5-case canary；5 次 localhost debug retrieval 均在 embedding 前以 `VECTOR_INDEX_NOT_READY` / HTTP 503 失败，实际 query embedding/provider calls=0、retry/fallback/model rerank=0，未形成质量结论且未启动 full。
- TDD 修复后 preflight 通过只读 statistics/count fail closed，当前 `BLOCKED / VECTOR_READINESS_UNAVAILABLE`；只读盘点确认 legacy source collection 存在、50 vectors、dimension=2048，与 50 chunks 一致。
- 首次 shadow migration 在 source audit 50/50 后，因 legacy JSON 将等价 `kbId` 表示为浮点数、`MilvusVectorStore` 又使用字符串比较而在 shadow upsert 前误判 scope conflict；该次执行按 zero-retry 合同停在 `AUDIT_FAILED / SOURCE_ACTIVE / shadow=0`。
- 已用 TDD 将 scope marker 比较修复为精确数值等价：允许 `11.0 == 11`，同时拒绝 IEEE-754 舍入造成的伪相等；聚焦 tests 与最终全仓 619 tests 均为 0 failures / 0 errors。
- 用户于 2026-08-31 重新明确授权一次 retry；该次执行已成功返回 `READY`，expected/observed/migrated=`50/50/50`、missing/mismatch=`0/0`，MySQL 已原子切换到 shadow，source 仍保留 50。tenant-scoped strong query 与 50 个预期 vector ID 读回均为 50；迁移后的 mutation-free preflight=`READY`、vector count=`50/50`、fixtures=`3/3`。
- 本次 retry 授权已消耗。用户随后独立授权一次 fixed 5-case canary；执行前 preflight 再次 `READY / 50/50 / fixtures 3/3`，5 次 localhost debug retrieval 均进入 NVIDIA query embedding，但实际 provider 统一返回 HTTP 410 Gone。结果=`FAILED`、retrieveErrors=5、rateLimit/retry/fallback/model rerank=0，full 未启动。
- 2026-08-31 只读核对 NVIDIA 官方模型页确认 `llama-nemotron-embed-1b-v2` 与其 hosted NIM endpoint 已标记 `Deprecated`；本地路径 TDD 证明请求实际为正确的 `/v1/embeddings`，因此未修改 provider 代码。替代模型即使同为 2048 维也属于不同 embedding 空间，不能复用现有 50 条向量。
- 用户已确认迁移到 `nvidia/nemotron-3-embed-1b` 并批准 doc-only 规划修订；对应提交为 `288a0f8`。本轮零外调 adapter/indexing 审计已完成：冻结 50 passage items、每 HTTP request 最多 5 items、request upper bound=11、retry=0，并确认 adapter request/response validation、actual-model cache、model-bound persistence/query fail-closed、独立 rebuild workflow 与 compiler identity 尚需实现。
- model-migration offline implementation 已完成：adapter/cache/config、V13 model-bound persistence、query/preflight/compiler fail-closed、default-off 独立 rebuild workflow 均已落地并通过离线 tests；本阶段 provider/backend calls=0、KB/Milvus/SQL mutation=0。
- 用户于 2026-09-05 单独授权并完成 1-item synthetic smoke：唯一 NVIDIA 请求 HTTP 200，model 精确匹配 `nvidia/nemotron-3-embed-1b`，item/index=`1/0`、dimension=2048、all finite、latency=30472ms、attempts=1、retry=0；仅发送非业务 synthetic 文本，KB/Milvus/SQL mutation=0。本次授权已消耗，不得复用。
- 用户于 2026-09-06 明确授权固定 50 passage items model-bound rebuild 及 tracked fixture chunks 向 NVIDIA NIM 出站；最终执行命令在 Maven 参数解析阶段因 PowerShell 展开 `$surefire` 而以 `Unknown lifecycle phase` 退出，测试 JVM 未启动。V13/provider/KB/Milvus mutations=`0/0/0/0`，Flyway 仍为 V12，旧 KB mapping 仍为 `READY / 50/50/50`。按一次性 zero-retry 规则，本次授权已消耗，未修正后重跑。
- 用户随后重新授权相同范围；本次改用字面量参数数组后测试 JVM 正常启动，但直接 `surefire:test` 解析到本机 Maven 仓库中的旧 `rag-core`，在任何 plan/Flyway/provider/rebuild 操作前以 `NoSuchMethodError: EmbeddingService.getActiveModelIdentity()` 停止。V13/provider/KB/Milvus mutations 仍为 `0/0/0/0`，本次授权同样已消耗，未重跑。
- 已在零外调范围内执行当前 reactor `maven.test.skip=true install`，并以 `javap` 确认安装后的 `rag-core` 含 `getActiveModelIdentity/getMaxBatchSize/embedBatchUncached`；纯 mock `VectorModelRebuildServiceTest` 已通过，classpath 阻断已消除。
- 已将执行改为两阶段入口并用当前 reactor 重新编译；零外调预演已在真实 MySQL/Milvus 上通过：provider identity=`nvidia/nemotron-3-embed-1b`、expected=50、maxRequests=11、目标 `tenant_1_kb_15_emb_nemotron3_83acf73b5765_gc17g1` 不存在、execute=false。随后真实执行因缺少本次 50 chunks 数据出站的明确重新授权而在进程创建前被权限闸门拒绝，calls/mutations=0。
- 用户明确重新授权 `c17g1` 后，真实执行成功应用 V13，但临时预算计数器在 adapter 内部拆批前错误地把首个 25-item document batch 当作单个 HTTP request，因而在 NVIDIA 请求前抛出 `MODEL_REBUILD_HTTP_BUDGET_EXCEEDED`。实际 provider HTTP requests/items=`0/0`；MySQL=`MODEL_REBUILD_FAILED`、shadow generation=`c17g1`、observed/migrated/missing/mismatch=`0/0/50/0`，旧 active/source mapping 保留，Milvus target 未创建。
- 已修正计数器按 `ceil(documentItems/5)` 统计 adapter HTTP batches，并用新 generation `c17g2` 完成零外调预演：expected=50、maxRequests=11、model/dimension 匹配、目标 `tenant_1_kb_15_emb_nemotron3_83acf73b5765_gc17g2` 不存在、execute=false。失败 generation `c17g1` 不复用；`c17g2` 真实执行等待独立授权。
- 2026-09-08 已离线补强 rebuild exact ID set/metadata 数值审计；聚焦 18/18、全仓 646 tests/0 failures/0 errors/21 skipped，相关 MySQL/Testcontainers 因 Docker 不可用跳过。runner 与 tracked manifest 已同步为计划代际 `c17g2`，旧 `c17g1` 不可再作为新 reference identity。本轮未重新运行真实基础设施预演，既有预演状态需在真实执行前刷新。
- 2026-09-08 用户同意一次 c17g2 rebuild 后，Docker 因遗留 dockerInference runtime socket 报 Error 1920 而无法启动；仅备份 Docker/run（两个 0 字节 socket）并重建该运行目录后恢复，原有五个容器均 healthy，未清理容器/镜像/卷或修改 WSL 数据盘。
- 当前真实盘点取代此前目标不存在的旧记录：SQL 自 `2026-09-06T15:18:46` 即为 `MODEL_REBUILD_FAILED / shadowGeneration=c17g2 / MODEL_REBUILD_READBACK_MISMATCH`，expected/observed/migrated/missing/mismatch=`50/50/50/0/50`。旧 source active 且强读回=50；c17g2 target 已存在且 count/expected IDs/new-model-content match=`50/50/50`。此事实不表示 CAS 已切换或质量验收通过。本轮在 provider/SQL 写入前停止，实际 HTTP attempts/items=0/0，未复用/覆盖/删除或切换失败 generation。
- 依照失败 generation 不复用契约，已将 runner/manifest 的待执行身份同步为 c17g3，并完成当前源码重新编译及真实 MySQL/Milvus 只读预演：V13、fixtures=3、chunks/source vectors=50、target absent、max requests=11、max batch=5、timeout=60000ms、retry=0；provider calls=0。Python 238 tests 与 canary/full plan-only 通过。维护源码与默认只读 PowerShell 入口保留在 ignored tmp/eval/c17，供后续从当前源码重新编译验证。
- 用户随后明确授权一次 c17g3 fixed rebuild；2026-09-08 在 HEAD=c5dd7b3 刷新 reactor/javac 和真实只读 preflight 后完成唯一一次执行：NVIDIA nvidia/nemotron-3-embed-1b 的 11/11 HTTP requests 均 200、passage items=50/50、query/rerank/ask/generation/judge=0、retry/fallback=0。SQL 已 CAS 切换为 READY/activeGeneration=c17g3，expected/observed/migrated/read-back=50/50/50/50、missing/mismatch=0/0，旧 source 保留50，未清理失败 c17g2。V13 本轮只验证、不重复迁移；该次真实重建授权已消耗。
- 独立无出站 verify 进程再次确认 SQL READY、model/request/endpoint/dimension/generation 匹配、3 fixtures/50 chunks、强读回 exact ID set=50、finite 2048-d vectors、source=50；provider HTTP attempts/items=0/0。脱敏重建证据见 docs/eval/reports/c17-model-rebuild-c17g3-summary.json。此为维护入口 SQL/Milvus postflight，不冒充应用 HTTP preflight 或 retrieval 质量 evidence。
- 下一步：先完成本机应用 backend 的 --preflight-only --keep-existing 和 runtime fingerprint 核验，再单独授权新模型 fixed 5-case canary。当前未启动 backend HTTP 服务，不能用维护入口 verify 代替端到端 HTTP readiness。full 450/450、阈值批准、baseline acceptance/archive、push、PR、deploy 仍未授权。
- 提交责任：`Agent 提交`（仅本地计划内 commit）；push、PR、deploy 未授权。

- 2026-09-08 最新执行覆盖此前待授权状态：用户明确授权 HTTP preflight/canary/full。应用 HTTP preflight READY（fixtures3、vectors50/50、新模型身份匹配），provider0。唯一 canary 5 retrieval、5 provider HTTP200、retrieveErrors3；预算护栏拒绝超额请求。离线当前代码核算 variants=1/2/5/2/1，canary/full冷缓存embedding上限11/1353，原5/450契约低估。full因canary失败和预算漂移未启动；须修订预算并重新授权，不改检索算法绕过，不复用失败canary。profile仍DRAFT。

- 最新：2026-09-08用户批准预算修订并验证canary直至通过，声明NIM免费且无支付方式。9080b92修订11/1353冷缓存上限；Python239 tests/OK。r2 HTTP preflight READY，canary5/5成功、RETRIEVAL_ONLY、errors/retry/fallback/model rerank0；provider新增6次HTTP200、缓存5。原进程复用且Java源码未变；新进程端口占用退出，预算已归零。证据c17-canary-c17g3-r2-summary.json。full新1353上限待授权，profile仍DRAFT。

- 2026-09-08 用户授权完成归档前任务后，在 HEAD=04c8b88 刷新 full plan-only（450 retrieval / 1353 query embedding upper bound）及 HTTP preflight（READY、fixtures=3/3、vectors=50/50）。新启动后端因端口占用退出，原 canary 进程仍在且业务源码未变。本轮 full 命令在进程创建前被自动审批拒绝：要求明确授权固定开发问题及查询变体向 NVIDIA 出站。实际新增 provider calls=0，累计 counter 保持11、预算文件=0，无 full artifacts；未执行重试。等待具体数据/目的地出站授权，随后刷新 clean HEAD/preflight 再执行；profile 保持 DRAFT，阈值审阅与归档未完成。
