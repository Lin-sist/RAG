# F3 执行任务

## 0. 规划与启动闸门

- [x] 0.1 核查正式 ChatPanel、路由、renderer、sources、R5 baseline 与冻结 Demo。
- [x] 0.2 准备 proposal、design（含真实取舍）、tasks、spec delta 和允许路径。
- [x] 0.3 2026-10-01 用户明确接受首批并授权 baseline 接受/归档。
- [x] 0.4 用户明确批准 F3 方案；首批归档后将唯一 active 指针切到本 change，再写业务代码。提交默认用户手动，外调未授权。

## 1. 展示实施

- [x] 1.1 起点 c74d8c4、工作区干净、四项 Demo blob 一致；Preflight 已完成。截图登记于本轮 visualization 目录的 f3 子目录。
- [x] 1.2 气泡和 760px 内容上限、BrandMark、Markdown/表格/代码块与可见动作区完成，首页/composer 保全。
- [x] 1.3 代码/回答复制仅在真实 Clipboard Promise 成功后反馈；拒绝及不可用提示手动复制，代码栏旁提示；键盘与窄屏检查通过，无新依赖。
- [x] 1.4 独立真实字段、原生 details/summary 键盘展开，标题/score 缺失中性降级，片段仅文本展示。
- [x] 1.5 80px 接近底部跟随、上翻暂停与主动回底完成；同步默认、status/reason、停止及历史只读沿用。

## 2. 验证与收口

- [x] 2.1 封闭 fixture 覆盖 ANSWER、五项非答案、缺/重复/非法 terminal、终态后文本、未知事件、读取失败及停止；未知 API/非允许写入 fail closed。剪贴板拒绝/不可用仅注入特定 fixture 页面。
- [x] 2.2 52/52 合成测试、R5 回归、vue-tsc + Vite 正式等价 build 通过；HTML/链接边界、精确路径检查见 acceptance 与日志。
- [x] 2.3 六组 36 张最终矩阵、6 张冻结 Demo 参考、17 张交互/兼容截图完成；逐帧 DOM/图像尺度和 SHA-256 校验，键盘/滚动、首页/设置复核完成。
- [x] 2.4 已形成 acceptance，分别报告合成、视觉、沿用真实与未知项；追加日志。收尾文档链接与 diff 检查结果见执行记录。
- [x] 2.5 用户授权提交及 F3 归档，按已完成验收范围接受三项 baseline delta，归档至 `2026-10-01-frontend-chat-source-alignment` 并恢复 IDLE。Agent 提交本轮文件；push/PR/发布/部署未授权。

本 change 已验收归档，前述提交责任为实施时快照，2.5 记录最新授权。OpenSpec CLI 不可用，文件/链接/精确 delta 检查不冒充官方 validate；合成、视觉与真实证据边界见 acceptance。
