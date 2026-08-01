# Active Task

## Status

`IDLE`

## Active Change

无。C16 `bounded-query-router` 已完成最终验收、baseline 接受与归档。

## Last Closeout

- C16 已将 `rag-system` 5 requirements / 17 scenarios 与 `evaluation` 4 requirements / 12 scenarios 精确接受进长期 baseline。
- 归档目录：`openspec/changes/archive/2026-08-01-bounded-query-router/`。
- deterministic 主证据保持 provider/model calls=0、business data outbound=false；live router ask/eval 仍为 `SKIPPED`。
- 能力边界仍为 default-off bounded `fact-intent-v1` / `fact-v1` / `evidence-no-answer-v1`；不宣称生产默认、真实 provider 质量、multi-hop/global/high-risk 或 Agentic RAG。
- 提交责任保持 `用户手动提交`；本轮 Agent 不暂存、不提交、不 push、不创建 PR、不部署。
