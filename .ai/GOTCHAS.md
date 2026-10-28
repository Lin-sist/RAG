# Repository Gotchas（非显而易见陷阱备忘录）

> **用途**：记录本仓库反直觉的工程陷阱與隐性契约，供新会话按需快速扫读，避免重复踩坑。
> **维护原则**：仅当某类问题无法从代码表面直接推断、或曾导致重复返工时才收录；保持在 100 行以内，不抄写常规规范。

## 1. Python 评测与 Windows 沙箱临时目录

- **陷阱**：在受限沙箱中，工作区下的 `tmp/` 或 `.tmp/` 可能无写权限（`WinError 5 / PermissionError`）；而 Python 标准库 `tempfile.mkdtemp` 在 `os.access` 报告可写但 `os.mkdir` 报 `PermissionError` 时会死循环重试，导致测试假死 12 秒以上。
- **对策**：评测单元测试创建临时目录统一使用 [`scripts/eval_test_support.py`](../scripts/eval_test_support.py)（优先使用系统临时目录，且单次创建失败立即抛错，不死循环重试）。

## 2. C18 预算审计源码冻结锁（`budget_source_drift`）

- **陷阱**：历史已归档的 C18 预算配置锁定了 `QAController.java`、`AnswerGeneratorImpl.java`、`RAGServiceImpl.java` 的冻结 Git LF SHA。后续 C21 与流式诊断已更新这 3 个文件，因此对当前工作树直接运行正式 C18 预算 CLI 返回 `exit 2 / BLOCKED / budget_source_drift` **属于预期保护**，不是 bug。
- **对策**：单元测试必须通过 [`scripts/eval_test_support.py`](../scripts/eval_test_support.py) 构造合成审计夹具隔离验证；**严禁**为了跑通测试或新任务而回改历史 `c18-*` 审计/manifest JSON。未来若基于新源码发起真实评测，须独立立项新版本审计。

## 3. 前端正式构建与无全局 `npm` 环境回退

- **陷阱**：部分本地或 Agent 会话环境中 `npm.cmd` 不在 `PATH` 上，但 `node` 与 `rag-frontend/node_modules` 存在；此外 TypeScript 5.7.3 不支持 `ignoreDeprecations: "6.0"`（会报 `TS5103`）。
- **对策**：运行前端验证时优先调用 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode FrontendBuild`（脚本会自动回退至 direct `vue-tsc -b` + `vite build`）；切勿单独用 `vite build` 跳过类型检查。

## 4. Java / OTel 全仓并发测试时序抖动（W0.1 已修复）

- **陷阱**：exporter 的 100ms 超时不等于后台线程和回调一定能在 100ms 内完成；原来的 2 秒测试等待在全仓高负载时可能误报。共用的等待函数还会让只启用 trace 的测试白等未启用的 metric。
- **对策**：2026-10-21 已改为在共同的 10 秒宽限期内等待 flush 完成及预期信号的失败计数，条件满足就立即继续；只启用 trace 时只等 trace。全仓 Maven 已通过，生产 fail-open 语义没有改变；以后仍不能用独立复跑通过掩盖全仓失败。

## 5. Wave 1 质量门禁的真实状态边界

- **陷阱**：看到 C19（Judge 校准）和 C20（Objective/Judge 门禁 Profile）在 `openspec/changes/archive/` 中归档，容易误以为质量门禁已全部启用。
- **对策**：目前仅 **C17（Retrieval Gate）** 为 `ACTIVE`；C19 与 C20 仅归档了**零外调离线工具链与 `DRAFT` 空阈值配置**。此外 C18 的单轮生成基线含 31 次 503 重试恢复，故在 C20 严格策略下仍为 `NOT_EVALUABLE`。完整 Live 证据与阈值激活属于未闭环的 P1 债务（`W1-E1` ～ `W1-E3`）。

## 6. 敏感日志门禁与路径安全

- **陷阱**：排障或写评测脚本时顺手打印 `e.getMessage()`、上游 HTTP response body、用户 `question/query/snippet`、知识库名或本地绝对路径（如 `C:\...`），会触发回归门禁失败或隐私泄露。
- **对策**：普通日志只允许记录 `traceId`、内部资源 ID、计数、耗时、状态码和固定错误枚举；提交前运行 `scripts/run_local_quality_gates.ps1 -Mode SensitiveLogs` 与 `-Mode DocAndScope`。

## 7. 流式终态与阶段收尾顺序

- **陷阱与对策**：Reactor 的下游完成回调可能先于上游 doFinally 执行，因此不能在那里才关闭供界面使用的阶段，否则 terminal 前的快照仍会显示 RUNNING。轨迹投影在实际 doOnComplete/doOnError 收尾，既有 OTel/metrics 保留 doFinally；异步 finalization 必须恢复订阅时捕获的 Context。

## 8. 隐藏单选框与失焦关闭弹层

- **陷阱与对策**：1px 透明 radio 的 label 文字被鼠标点下时，旧输入可能先产生 relatedTarget=null 的 focusout，弹层在 label 激活前就被销毁。原生 radio 应覆盖整行点击区；回归必须实际点击文字/说明/空白，直接调用 radio.check() 会绕过该故障。

## 9. 菜单 max-height 不等于视口内可见

- **陷阱与对策**：固定向上展开的菜单即使设了 max-height，仍可能在矮窗口越过顶部。应按锚点与实际视口的上下空间选方向、限高，并在窗口/visualViewport/输入尺寸变化后更新；回归同时覆盖长列表和打开后的缩窗。

## 10. MyBatis-Plus 顶层基本类型数组参数

- **陷阱与对策**：将 byte[] 直接放入 mapper 的 @Param 参数映射，普通 MyBatis 测试可能通过，但 MyBatis-Plus 的 MybatisParameterHandler 会把它误当批量数组转成 Object[]，导致保存返回 500。使用标量值对象包装 bytes 并绑定其属性；回归须使用生产 MybatisConfiguration/MybatisSqlSessionFactoryBuilder 与插件，不能只用普通 SqlSessionFactoryBuilder。

## 11. Vue scoped 样式中的 :global 选择器范围

- **陷阱与对策**：当前编译器会将 `:global(html.dark) .icon` 替换为整个 `html.dark` 选择器，局部 filter 会误作用整页。将完整限定选择器放进 `:global(html.dark .component .icon)`；浏览器回归还需检查祖先 filter，单测子元素背景色无法发现整页反色。
