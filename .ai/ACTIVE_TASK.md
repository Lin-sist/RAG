# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`MODEL_MIGRATION_OFFLINE_AUDIT_COMPLETE_AWAITING_IMPLEMENTATION_APPROVAL`
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
- 下一步不是 synthetic smoke：必须先由用户批准 model-migration offline implementation；该阶段仍 provider/backend calls=0、KB mutation=0。实现与离线 tests clean 后，才可另行申请 1-item synthetic smoke。full 450/450、阈值批准、baseline acceptance/archive、push、PR、deploy 仍未授权。
- 提交责任：`Agent 提交`（仅本地计划内 commit）；push、PR、deploy 未授权。
