# 2026-10 上下文治理与阶段记录冷归档

旧日志按原始字节封存在下方原文块中；其中的相对链接基于当时的 .ai/AGENT_LOG.md 位置，不作为本目录导航。现行入口：[协作规则](../../AGENTS.md)、[协作手册](../../docs/02-使用与维护/AI协作开发手册.md)、[活动任务](../ACTIVE_TASK.md)。

```markdown
## 2026-10-21 · wave1-offline-closeout（阶段里程碑锚点）

- **改动与大白话**：完成 `eval_test_support.py` 测试隔离修复及 C19/C20 零外调离线工具链归档（`3f02225`、`2536fb5`），清理 2 个旧前端空壳目录，`.ai/ACTIVE_TASK.md` 恢复 `IDLE`。改前全仓 Python 测试受旧 C18 源码漂移阻塞且 Wave 1 挂起项挤占活动指针；改后 Wave 1（C17–C21）全员归档，未完 Live 采证转入 P1 证据债务 `W1-E1~E3`。
- **验证与门禁**：`python -B -m unittest discover -s scripts -p 'test_*.py'` 336 tests GREEN（0 failure/error）；C19 plan-only 与 C20 空输入/C18 回放烟测全部符合契约。
- **踩坑与风险**：定位并隔离了 Windows 沙箱 `tempfile.mkdtemp` 权限重试卡顿与 C18 冻结审计 SHA 漂移（已收录入 `.ai/GOTCHAS.md`）；真实 Judge 校准与三轮 full150 参考仍待后续独立授权。

## 2026-10-21 · workflow-v2-context-lean-governance（Type B 治理升级）

- **改动与大白话**：升级 [`AGENTS.md`](../AGENTS.md)、[`vibecoding-playbook.md`](../docs/workflow/vibecoding-playbook.md)、[`agent-collaboration/spec.md`](../openspec/specs/agent-collaboration/spec.md)、[`ACTIVE_TASK.md`](ACTIVE_TASK.md)、[`project.md`](../openspec/project.md) 與 [`run_local_quality_gates.ps1`](../scripts/run_local_quality_gates.ps1)，新增 [`GOTCHAS.md`](GOTCHAS.md) 并将原 3,804 行日志封存入 [`.ai/logs/2026-07-to-10-wave1.md`](logs/2026-07-to-10-wave1.md)。改前 730KB 单文件日志只写不读、每次提交需额外补录 SHA 且多处文档重复抄写进度；改后实行“≤200 行滚动热窗口 + 6 行定额 + 同次提交零补录 + `Parked` 挂起表 + `DocAndScope` 自动门禁”。
- **验证与门禁**：运行 `scripts/run_local_quality_gates.ps1 -Mode DocAndScope` 与 `-Mode SensitiveLogs` 均 PASS；核对冷归档文件与原日志 3,804 行字节完全一致；未改业务代码，跳过 Maven/前端构建/外部调用。
- **踩坑与风险**：无新陷阱（首批已从历史日志提炼 6 条核心避坑指南入 `GOTCHAS.md`）；后续在 W0 OTel 时序修复及 Wave 2 首个 Change 中实测检验 v2.0 模板顺滑度。

## 2026-10-21 · workflow-v2-plain-language-patch（Type B 治理微调）

- **改动与大白话**：更新 [`AGENTS.md`](../AGENTS.md)、[`design-decision-record.md`](../docs/workflow/prompt-snippets/design-decision-record.md)、[`vibecoding-playbook.md`](../docs/workflow/vibecoding-playbook.md) 与 [`agent-collaboration/spec.md`](../openspec/specs/agent-collaboration/spec.md)。改前只要求“简短/半句话”，导致 Agent 为了省字数删掉主谓宾和因果逻辑，写出天书般的机器电报体；改后强制每次事前闸门、完成汇报和 `design.md` 开头先写 `💡 30秒人话导读`，并植入“❌ 电报黑话 vs ✅ 通透人话”正反例对照表，让主人 10 秒看懂每次改动。
- **验证与门禁**：运行 `scripts/run_local_quality_gates.ps1 -Mode DocAndScope` 门禁 PASS；纯治理文案与模板升级，跳过 Maven/Python/前端构建及外部调用。
- **踩坑与风险**：发现给大模型下达“用半句话说清”指令极易诱发无主谓宾的名词堆砌，必须用正反面 Few-Shot 示例约束文风；紧接着在 W0.1 OTel 修复任务中检验 Codex 汇报效果。

## 2026-10-21 · W0.1（Type B：稳定 OTel 测试等待）

- **改动与大白话**：修改 1 个 Java 测试文件和 4 个说明文件。原来的秒表只给后台线程 2 秒，全仓繁忙时容易误报；现在统一给最多 10 秒，等到导出结束和失败计数出现就继续，只开 trace 的测试也不再白等没开的 metric。
- **验证与门禁**：两类聚焦测试连续三轮通过，每轮 12 项；全仓 `mvn -q test` 退出码为 0，682 项测试没有失败或错误，21 项跳过，目标测试类 9 项全部通过。DocAndScope 已通过，差异格式、受保护路径、Markdown 链接和热日志行数检查均合格。
- **跳过原因**：19 项 Docker 测试因环境不可用而跳过，2 项 OTel 烟测因未开启而跳过；本次没有修改 Python 或前端，因此没有重跑它们的验证。
- **踩坑与风险**：已将“导出超时不等于后台回调完成时间”的区别写入 GOTCHAS 第 4 节；生产导出配置、业务耗时断言和 fail-open 行为保持原样，本轮通过不能代替真实 Collector 烟测或生产容量验证。

```
