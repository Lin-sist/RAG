# Active Task

## Status

`IDLE`

- 当前无 active change。C21 后端契约已在本地确定性范围内验收并归档；未执行真实 provider 调用。
- 推进顺序：C21 后端契约与验证 → 前端接入和联调 → C19 校准 → C20 门禁。后续阶段分别验收。

## Last Completed Change

- Change ID：`structured-sse-terminal-contract`（C21）
- 归档路径：`openspec/changes/archive/2026-09-29-structured-sse-terminal-contract/`
- 阶段：C21 结构化 SSE 后端契约与本地确定性验收完成；详见归档 `acceptance.md`。

## Current Boundary

- 下一阶段前端 R5 可接入已接受的 `structured-v1` wire；真实前后端联调及 C19/C20 均需按各自契约与外调授权推进。客户端断流仍只可判定 incomplete，不能宣称服务端取消或无 provider 费用。
