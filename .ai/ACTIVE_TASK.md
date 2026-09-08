# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`C17G2_OFFLINE_HARDENED_AWAITING_REBUILD_AUTHORIZATION`
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
- 下一步仍需重新明确授权固定 rebuild；范围保持 3 fixtures/50 chunks、最多 11 HTTP requests、每次最多 5 items、retry=0、新 `c17g2` collection、50/50/50 强读回与 CAS 切换。V13 已由此前执行应用，本轮不重复迁移；执行前必须确认 schema/旧 source mapping/target absence。5-case canary、full 450/450、阈值批准、baseline acceptance/archive、push、PR、deploy 仍未授权。
- 提交责任：`Agent 提交`（仅本地计划内 commit）；push、PR、deploy 未授权。
