# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`PLANNING_REVIEW`
- 目标：以固定 `rag-eval-dev-v2` 150×3 retrieval reference、严格身份/完整性 compiler、用户阈值审阅和 locked median reference，把首个 C10 retrieval profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进到可验收的 `ACTIVE / APPROVED`。

## Current Boundary

- 当前只完成 proposal、design、tasks 与 `evaluation` spec delta；用户批准前不修改业务代码、evaluation tooling、profile 数值或 accepted baseline。
- W0 OTel 全仓时序债务是 C17 live canary/full 的独立硬前置；聚焦测试通过不能替代 closeout。
- planning/offline 当前真实 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- canary、full reference、阈值批准、baseline acceptance/archive、commit、push、PR、deploy 均为独立授权闸门。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交。
