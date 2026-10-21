# Agent Log（滚动热窗口）

> **维护规则（v2.0）**：本文件只保留近期热记录（控制在 **200 行以内**，只追加不改写），供新会话快速读取以延续上下文。
> - **同次提交、零补录**：日志随当次改动在同一个 commit 中提交；不再写 `Commit: pending` 或追加手写 SHA 补录（Commit SHA 以 `git log` 为准）。
> - **单条 6 行定额**：只记改动与大白话、核心验证与 `DocAndScope` 门禁结果、反直觉踩坑（同步提炼入 [`.ai/GOTCHAS.md`](GOTCHAS.md)）及剩余风险。
> - **历史冷归档**：Wave 0 ～ Wave 1 收口（2026-07-12 至 2026-10-21，共 3,804 行）已无损封存至 [`.ai/logs/2026-07-to-10-wave1.md`](logs/2026-07-to-10-wave1.md)。

## 2026-10-21 · wave1-offline-closeout（阶段里程碑锚点）

- **改动与大白话**：完成 `eval_test_support.py` 测试隔离修复及 C19/C20 零外调离线工具链归档（`3f02225`、`2536fb5`），清理 2 个旧前端空壳目录，`.ai/ACTIVE_TASK.md` 恢复 `IDLE`。改前全仓 Python 测试受旧 C18 源码漂移阻塞且 Wave 1 挂起项挤占活动指针；改后 Wave 1（C17–C21）全员归档，未完 Live 采证转入 P1 证据债务 `W1-E1~E3`。
- **验证与门禁**：`python -B -m unittest discover -s scripts -p 'test_*.py'` 336 tests GREEN（0 failure/error）；C19 plan-only 与 C20 空输入/C18 回放烟测全部符合契约。
- **踩坑与风险**：定位并隔离了 Windows 沙箱 `tempfile.mkdtemp` 权限重试卡顿与 C18 冻结审计 SHA 漂移（已收录入 `.ai/GOTCHAS.md`）；真实 Judge 校准与三轮 full150 参考仍待后续独立授权。

## 2026-10-21 · workflow-v2-context-lean-governance（Type B 治理升级）

- **改动与大白话**：升级 [`AGENTS.md`](../AGENTS.md)、[`vibecoding-playbook.md`](../docs/workflow/vibecoding-playbook.md)、[`agent-collaboration/spec.md`](../openspec/specs/agent-collaboration/spec.md)、[`ACTIVE_TASK.md`](ACTIVE_TASK.md)、[`project.md`](../openspec/project.md) 與 [`run_local_quality_gates.ps1`](../scripts/run_local_quality_gates.ps1)，新增 [`GOTCHAS.md`](GOTCHAS.md) 并将原 3,804 行日志封存入 [`.ai/logs/2026-07-to-10-wave1.md`](logs/2026-07-to-10-wave1.md)。改前 730KB 单文件日志只写不读、每次提交需额外补录 SHA 且多处文档重复抄写进度；改后实行“≤200 行滚动热窗口 + 6 行定额 + 同次提交零补录 + `Parked` 挂起表 + `DocAndScope` 自动门禁”。
- **验证与门禁**：运行 `scripts/run_local_quality_gates.ps1 -Mode DocAndScope` 与 `-Mode SensitiveLogs` 均 PASS；核对冷归档文件与原日志 3,804 行字节完全一致；未改业务代码，跳过 Maven/前端构建/外部调用。
- **踩坑与风险**：无新陷阱（首批已从历史日志提炼 6 条核心避坑指南入 `GOTCHAS.md`）；后续在 W0 OTel 时序修复及 Wave 2 首个 Change 中实测检验 v2.0 模板顺滑度。
