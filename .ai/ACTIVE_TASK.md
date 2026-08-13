# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`OFFLINE_IMPLEMENTED_AWAITING_W0_PREFLIGHT`
- 目标：以固定 `rag-eval-dev-v2` 150×3 retrieval reference、严格身份/完整性 compiler、用户阈值审阅和 locked median reference，把首个 C10 retrieval profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进到可验收的 `ACTIVE / APPROVED`。

## Current Boundary

- proposal、design、tasks 与 `evaluation` spec delta 已获用户批准；offline manifest/schema、runner plan budget、reference compiler、tests 与指南已实现，当前停止在 W0/preflight/live authorization 前，不修改 profile 数值或 accepted baseline。
- W0 OTel 全仓时序债务是 C17 live canary/full 的独立硬前置；聚焦测试通过不能替代 closeout。
- offline implementation 的真实 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- canary、full reference、阈值批准、baseline acceptance/archive、commit、push、PR、deploy 均为独立授权闸门。
- 提交责任：`Agent 提交`（仅本地计划内 commit）；push、PR、deploy 未授权。
