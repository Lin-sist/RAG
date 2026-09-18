# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`generation-objective-evidence-baseline`
- 路径：`openspec/changes/generation-objective-evidence-baseline/`
- 阶段：`C18_RUNTIME_READY_LIVE_APPROVAL_REVIEW_BLOCKED`
- 目标：judge关闭下建立v2/150条一次完整真实generation/citation/objective claim/no-answer基线。

## Current Boundary

- C17文件验收归档已完成：4 requirements/12 scenarios已接受进baseline，ACTIVE profile/locked reference、原始三轮各12/12 PASS、Python244 tests通过。归档路径：`openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/`。
- C17已于2026-09-11本地提交：`9276d114058664a3cf33f26fecca046e68e566a4`。用户明确授权Agent本地提交并保持Git干净，原提交审批阻断已解除；C18规划与交接文档随后独立提交。
- 用户于2026-09-11明确要求开始C18实现；W0调用图审计及budget plan-only已完成，W1完整manifest/runner护栏/compiler和负例验证已完成；无任何真实业务调用。
- 当前已冻结canary/full query embedding上限34/1492，含解释回退12/590，ask/generation上限5/150；共享USER限流要求每请求至少2.2秒。33项Java聚焦与252项Python基线保持通过，W1全套Python为265项通过。执行前仍需runtime fingerprint和单独外调授权。
- REST ask会写query count/history；后续外调披露必须包含该本地副作用。2026-09-17已执行两次仅本机读取的 C18 canary preflight，均为 `READY`（vector 50/50、fixtures 3/3、chunk 11/14/25）；provider/embedding/rerank/ask/generation/judge calls=0，业务数据未出站、资源未mutation。
- 用户已授权将 `.env.local` 的 `NVIDIA_EMBED_MODEL` 对齐至现有 KB 15 的 `nvidia/nemotron-3-embed-1b`；普通 `start-backend.ps1` 启动日志与两次 preflight 均验证持久化 identity 生效。
- 本轮C18规划提交责任：`Agent 提交`（2026-09-11用户明确授权本地提交并保持Git干净）。用户已授权C18离线实现，本轮完成W1；未执行任何live调用。后续canary/full及验收/归档仍按对应范围授权；无push/PR/deploy授权。

## 2026-09-18 当前执行状态（覆盖上方旧授权待办）

- 用户明确授权一次性完成 C18 并由 Agent 提交；runtime allowlist 核验及真实只读 preflight READY，3 fixtures/50 vectors，后端启动成功。
- 补齐 C18 runner 失败即停护栏，Python 266 tests PASS；本地提交 `a8c95cf`。
- 真正执行 canary 前，自动审批拒绝命令：要求可信用户内容明确确认开发题目/变体/fixture contexts/prompt 至 NVIDIA endpoint 的具体出站及本地 history 写入，未接受整阶段授权覆盖该载荷。命令未启动，canary/full/provider calls=0。
- W2/W3/W4 未完成；不标 IDLE、不接受 baseline、不归档、不宣称真实 generation 质量。下一步只需解除该具体审批阻断，无需重复离线实现。
