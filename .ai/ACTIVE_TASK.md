# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`retrieval-quality-gate-activation`
- 路径：`openspec/changes/retrieval-quality-gate-activation/`
- 阶段：`PREFLIGHT_READY_AWAITING_CANARY_DATA_EGRESS_AUTH`
- 目标：以固定 `rag-eval-dev-v2` 150×3 retrieval reference、严格身份/完整性 compiler、用户阈值审阅和 locked median reference，把首个 C10 retrieval profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进到可验收的 `ACTIVE / APPROVED`。

## Current Boundary

- proposal、design、tasks 与 `evaluation` spec delta 已获用户批准；offline tooling、W0 与 mutation-free preflight 均已完成，当前停止在 canary 具体数据出站授权前，仍不修改 profile 数值或 accepted baseline。
- W0 OTel 全仓时序债务已由独立 Type B commit `f2f0ec3` 关闭：聚焦 12 tests 与全仓 617 tests 均为 0 failures / 0 errors；OTel diff 未混入 C17 提交。
- offline implementation 的真实 backend/embedding/rerank/ask/generation/judge/LLM/provider calls=0、business data outbound=false。
- preflight=`READY`、3/3 fixtures=`COMPLETED`、provider calls=0、mutation=0；runtime embedding 为 NVIDIA hosted `nvidia/llama-nemotron-embed-1b-v2`，canary 具体出站范围已披露。
- 用户概括授权继续完成 C17，但外调安全门要求在具体披露后另行明确批准 5 条 tracked eval question 出站到 NVIDIA；canary 尚未执行，禁止绕过。push、PR、deploy 仍为独立授权闸门。
- 提交责任：`Agent 提交`（仅本地计划内 commit）；push、PR、deploy 未授权。
