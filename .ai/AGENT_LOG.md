# Agent Log（滚动热窗口）

> **维护规则**：本文件只保留近期热记录（控制在 **200 行以内**，历史记录只追加不改写），供新会话快速读取；具体要求见 [`AGENTS.md`](../AGENTS.md) 第 9 节。
> - **同次提交、零补录**：日志随当次改动在同一个 commit 中提交；不再写 `Commit: pending` 或追加手写 SHA 补录（Commit SHA 以 `git log` 为准）。
> - **简短记录**：每个可验证切片记一次改动原因、验证与跳过原因、新增风险；已有专项证据直接链接，可复用教训才提炼入 [`.ai/GOTCHAS.md`](GOTCHAS.md)，不为行数凑内容。
> - **历史冷归档**：Wave 0 ～ Wave 1 收口（2026-07-12 至 2026-10-21，共 3,804 行）已无损封存至 [`.ai/logs/2026-07-to-10-wave1.md`](logs/2026-07-to-10-wave1.md)。

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

## 2026-10-22 · workflow-v2.1-risk-based-maintenance（Type B：精简协作流程）

- **改动与大白话**：同步 5 个治理文件；原来小修也可能等确认、同一结果要抄多份，现在已授权的小修直接执行，重大变更按风险分级，同一证据链接引用，设计和汇报按复杂度展开，回退优先保护已有改动。
- **验证与门禁**：DocAndScope PASS；协作规格 6 条要求/12 个场景的结构检查通过，旧规则冲突检索与 diff 复核完成；范围外 6 个文件的 SHA-256 与开工快照一致，历史日志及原有 W0.1 记录逐字保留。
- **跳过原因**：本轮仅改治理文档，未重跑 Maven/Python/前端构建或业务外调；OpenSpec CLI 不在 PATH，未运行官方 validate，文件级检查不替代官方验证。
- **边界与风险**：Type C 设计审查、外调与发布授权仍保留；DocAndScope 不自动判断任务范围，日志超限仍需执行归档。活动指针不变；本轮未暂存、未提交。

## 2026-10-23 · F6-SIZE（Type B：清理图标全量注册）

- **改动与大白话**：仅修改 1 个实现文件 `rag-frontend/src/main.ts`，删除图标命名空间导入与全量注册循环；原先把未用图标一起装进入口包，现在由已有局部导入提供所需图标，Element Plus 组件注册、样式与交互代码保留。另追加本日志并更新技术债条目，共 2 个治理文件。
- **引用核查**：覆盖 76 个源码文件、39 个 Vue 组件；排除原生元素及 Vue 内置组件后，145 处组件标签均有局部绑定，5 处图标参数已有导入，2 处动态组件用于路由；未发现按图标名称字符串解析的依赖，无需补充导入。
- **重建对照**：两次成功构建均使用同一 FrontendBuild 门禁（Node 24.14.0、TypeScript 5.7.3、vue-tsc 2.2.12、Vite 6.4.1，实际执行 npm run build）。入口 JS 原始/gzip 为 1,170,217/376,428 → 1,022,081/337,473 B；全部 21 个 JS 合计为 1,428,747/486,653 → 1,280,611/447,676 B。gzip 用相同 Node 24.19.0 的 gzipSync(level=6) 逐文件压缩求和；CSS 文件及 SHA-256 均一致，旧 dist 未作基线。
- **验证与门禁**：修改前后正式构建均含 vue-tsc 且通过；现有前端测试 102/102 通过，无失败或跳过；DocAndScope PASS，并复核本轮 diff 范围。未改后端或评测代码，因此不运行 Maven/Python 测试。
- **浏览器与边界**：现有 127.0.0.1:5188 封闭合成预览中检查聊天、知识库列表/详情、历史页及上传区展开；统计区 4 个 SVG、上传图标正常，检查时未出现空 el-icon 或控制台 warn/error。这是局部浏览器烟测，未完成全页面/全设备视觉矩阵；真实 backend/provider 调用为 0。
- **风险与流程**：主包仍有 >500kB 警告，F6-SIZE 保持 partial。初次沙箱构建因清空 dist 的 EPERM 失败，内置浏览器桥接不可用；通过自动权限审批重跑构建并使用 ego-browser 完成烟测，人工确认 0 次。已有 9 个改动保留（原日志仅追加），活动指针不变；未暂存、提交或 push。

## 2026-10-23 · rag-learning-and-ownership-roadmap（Type B：讨论路线落盘）

- **改动与大白话**：新增 [项目定位、学习与协作路线 v0.1](../docs/rag-learning-and-ownership-roadmap.md)，另追加本日志，共 2 个文件；原先目标与协作建议散落在对话中，现在统一保存已确认选择、场景假设、阶段停止标准、Java 学习主线和六步协作方法，便于后续试行调整。
- **验证与门禁**：DocAndScope PASS；新文档的相对链接检查通过，学习任务与工程能力的完成状态分别表述；本轮差异仅为新文档与日志追加。
- **跳过原因**：仅整理讨论文档，未运行 Maven/Python/前端测试或真实 provider 调用；文中学习任务、场景验证和效果评测均未执行。
- **边界与风险**：v0.1 为讨论初稿，不替代现行规则和能力契约，不自动启动旧蓝图后续阶段；活动指针保持 IDLE，已有工作区改动保留，未暂存或提交。

## 2026-10-26 · document-and-fixture-cleanup（Type B：收敛文档与目录视图）

- **改动与大白话**：修正 3 份过时说明、收敛 2 处规划/索引入口，并整理 6 份手工样本；原先旧状态混在现行说明里、测试文件散在根目录，现在以[文档导航](../docs/README.md)进入现行资料，完整旧规划单独保留，样本集中到 test-data/manual，另提供可选 VS Code 清爽工作区。
- **验证与门禁**：DocAndScope PASS；88 份非接受归档/冷日志 Markdown 的 292 个相对链接目标有效，6 份迁移样本 SHA-256 不变；历史规划全文及链接目标等价，范围外 tracked 文件哈希不变，三份正式评测语料、accepted archive、specs 与原始报告均保留。
- **踩坑与边界**：源码确认 legacy 流不传结构化引用不等于历史引用必为空，已纠正报告；tmp 含评测原件，data/index-inputs 是持久输入，不能按目录名批量删除。仅新增视图配置，没有删除目录或修改现有 IDE/本地 Skill 配置。
- **跳过与提交**：仅文档、样本路径与目录视图配置变化，未重跑 Maven/Python 业务测试或前端构建，真实 provider 调用为 0；工作区 JSON 与 Git 忽略规则另行核验，视图未在 VS Code 实际打开验证。已有日志内容保留，活动指针不变，未暂存、提交或 push。

## 2026-10-26 · antigravity-explorer-cleanup（Type B：按实际 IDE 收敛目录视图）

- **改动与大白话**：新增 1 份项目级隐藏配置，更新 README、文档导航与忽略规则，移除扩展推荐和此前新增的工作区文件；原先要单独打开 VS Code 工作区才会隐藏杂项，现在 Antigravity IDE 直接打开项目即可采用相同规则，.vscode 自身也隐藏。
- **验证与门禁**：本机已安装 IDE 源码含 files.exclude 支持；实际目录面板已显示源码、docs、openspec 等入口，.m2repo、.idea、.agents、.vscode、data、logs 以及前端 dist/node_modules 已隐藏。JSON、跟踪例外、文档链接与 DocAndScope 均通过。
- **边界与风险**：只改变 IDE 展示，没有删除运行数据、评测原件、依赖或源码；.tmp/.codex 由用户事先删除。隐藏设置可能影响 IDE 搜索，需查原件时按文档说明恢复显示或按路径检索。
- **跳过与提交**：复用前一切片的样本哈希与历史规划保全验证，未重跑业务测试/构建或真实调用；本轮获本地 commit 授权，整理改动与对应日志同次提交，开工前已有的两条学习 Skill 日志不混入暂存，push 未授权。
