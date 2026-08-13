# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`SHADOW_MIGRATION_FAILED_AWAITING_EXPLICIT_RETRY_AUTH`
- 目标：以固定 `rag-eval-dev-v2` 150×3 retrieval reference、严格身份/完整性 compiler、用户阈值审阅和 locked median reference，把首个 C10 retrieval profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进到可验收的 `ACTIVE / APPROVED`。

## Current Boundary

- proposal、design、tasks 与 `evaluation` spec delta 已获用户批准；offline tooling 与 W0 已完成。首次 canary 以 `VECTOR_INDEX_NOT_READY` 失败，修复后的 mutation-free preflight 已正确 BLOCKED；用户随后明确批准一次 zero-retry shadow migration，首次真实执行已失败并停止，仍不修改 profile 数值或 accepted baseline。
- W0 OTel 全仓时序债务已由独立 Type B commit `f2f0ec3` 关闭：聚焦 12 tests 与全仓 617 tests 均为 0 failures / 0 errors；OTel diff 未混入 C17 提交。
- offline implementation 的真实 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- 用户已明确批准固定 5-case canary；5 次 localhost debug retrieval 均在 embedding 前以 `VECTOR_INDEX_NOT_READY` / HTTP 503 失败，实际 query embedding/provider calls=0、retry/fallback/model rerank=0，未形成质量结论且未启动 full。
- TDD 修复后 preflight 通过只读 statistics/count fail closed，当前 `BLOCKED / VECTOR_READINESS_UNAVAILABLE`；只读盘点确认 legacy source collection 存在、50 vectors、dimension=2048，与 50 chunks 一致。
- 首次 shadow migration 在 source audit 50/50 后，因 legacy JSON 将等价 `kbId` 表示为浮点数、`MilvusVectorStore` 又使用字符串比较而在 shadow upsert 前误判 scope conflict；MySQL 已 fail closed 为 `AUDIT_FAILED`，active mapping 仍指向 source，source=50、shadow=0，未清理、未重试。
- 已用 TDD 将 scope marker 比较修复为精确数值等价：允许 `11.0 == 11`，同时拒绝 IEEE-754 舍入造成的伪相等；聚焦 tests 与最终全仓 619 tests 均为 0 failures / 0 errors。再次执行真实 migration 仍属于独立基础设施写入，必须取得新的明确 retry 授权；push、PR、deploy 仍未授权。
- 提交责任：`Agent 提交`（仅本地计划内 commit）；push、PR、deploy 未授权。
