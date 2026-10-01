# F3 工程与本地验收记录

日期：2026-10-01。状态：工程、合成测试及指定视口视觉复核完成；用户授权提交及 F3 归档，按本记录验收范围接受三项 baseline delta，归档至 `openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/`，ACTIVE 恢复 IDLE。

用户明确“批准F3方案”，随后明确“接受首批并授权归档”。首批五项 delta 已逐字加入 `openspec/specs/rag-system/spec.md`，归档于 `openspec/changes/archive/2026-10-01-frontend-demo-experience-alignment/`，再切换唯一活动指针实施 F3。工程验收呈交后，用户进一步明确“可以为我提交commit，进行F3归档”，作为本次 F3 验收、baseline 接受、归档及提交授权；不包含 push。

## 基线、范围与能力

- 起点 `c74d8c40cecb357e89afcefd1372fbf39ced9126`，工作区干净；包含首批 `cc8d98e`、R5 `0024e92`。Demo 四个 blob 与 design 冻结值一致，prototype 未改。
- 正式 ChatPanel 沿用同一次 ask/SSE、原终态适配与历史事件；正文拆为 AnswerContent / AnswerSources，既有 MarkdownIt 集中到 answerMarkdown，clipboard 仅处理浏览器实际 Promise。无需新增依赖或服务。
- `confirmed`（本地合成/浏览器）：760px 上限、问题气泡、BrandMark、长 Markdown、代码语言栏和复制、局部横向滚动、独立来源展开、真实 Clipboard 成功/失败反馈、上翻暂停跟随、回底恢复、历史输入禁用、首批首页/设置兼容。
- `partial`：指定三种视口、深浅主题与所列状态已复核；不能扩张为全部设备、浏览器或所有内容的像素一致。
- `unknown`：真实 provider 的完整非答案、断流、取消矩阵和引用原文精确定位；本次无新增真实证据。
- `planned / out_of_scope`：F1 服务端 logout、F4 知识库/上传、F5 历史列表完整迁移、登录重设计、多轮/账号工作台、C19/C20、后端/模型改动。

## 对齐与适配

| 区域 | 结果 | 边界 |
| --- | --- | --- |
| 问题与正文 | 对齐气泡、标题/列表、间距、760px 内容上限；箭刃品牌替代旧 AI 圆标 | 正文 15px、1.85 行高及窄屏品牌留位为现有系统适配，未声明逐像素复制 |
| 代码/表格 | 对齐语言栏和复制入口；pre 与表格自身可横向阅读 | 不增加执行/下载/网络语法服务；反馈位于代码栏旁 |
| 动作 | 复制始终可见，可用 Enter/Space；实际成功才显示“已复制” | 未接的分享、重生成、消息评分按钮不迁入 |
| 来源 | 独立卡片、片段预览、原生 summary 键盘展开/收起全文；真实字段降级 | 检索相关度不是答案正确率；不迁入 Demo 行内编号、假原文定位或检索过程数据 |
| 长内容 | 接近底部 80px 内跟随，上翻暂停，回底入口恢复；发送新问题主动回底 | 无新请求/成功历史事件来源；客户端停止仅报告本地结果 |
| 主题/历史 | 深浅与窄屏可阅读，历史仍一问一答及现有反馈 | 不改 auth、history API、反馈持久化或列表迁移 |

## 合成与构建证据

- Preflight：Node、现有 node_modules、直接 vue-tsc/Vite 可用；npm 不在 PATH，未安装或升级工具。
- `node --test tests/*.test.mjs`：52/52 PASS，新增五项行为测试覆盖 HTML/语言/链接安全、代码空白与表格区域、Clipboard 实际完成/拒绝/不可用、来源字段与转义、阅读滚动。既有 R5、认证、QA/history、侧栏/设置、主题与轮询回归通过。
- 正式 build 等价执行：`node node_modules/vue-tsc/bin/vue-tsc.js -b` 成功后执行 `node node_modules/vite/bin/vite.js build`，均 PASS；保留既有 >500kB chunk 警告。最后 loading aria 状态变动重新构建，未改变逻辑，复用不受影响的 52/52 测试。
- `node --check rag-frontend/tests/preview.mjs` PASS。127.0.0.1:5188、configFile:false、无真实 proxy；指定 fixture 提供同步和 structured-v1 SSE。未知 GET 404、未知 POST/DELETE 405，未列入场景的 stream 405、登录/同步 ask 非 POST 405；已知 KB 200。允许 stream 200、唯一 terminal、原始 text event 验证通过。
- 浏览器验证五项非答案及非法/重复 terminal、终态后文本、未知事件、读取失败：均无来源或完整回答复制入口；缺 terminal 矩阵和客户端停止保持原提示。合成 CANCELLED 不证明真实服务端取消。
- Enter/Space 来源展开、回答 Space/代码 Enter 复制通过；只读取本次复制的合成文本，实际剪贴板与回答、code.textContent 完全一致。preview 仅对 clipboard-failure / clipboard-unavailable 页面注入合成拒绝/不可用，提示正确；不改变正式 runtime。
- 上翻证据：内容长度 528→1269、消息区高度 1286→3634，顶部保持 0；主动回底后 top=2980、clientHeight=654。表格 clientWidth=317 / scrollWidth=361、键盘横向 scrollLeft=32，页面横向溢出为 0。
- HTML、事件文本、javascript/file 链接不执行；浏览器回答区域 img/script/危险链接均为 0。长来源标题完整换行；缺失标题/score、空来源中性处理。加载状态可达，完成后正常展示。

## 视觉与交互证据

目录：`C:/Users/Lin/.codex/visualizations/2026/10/01/01a0f557-b647-7452-a4b7-63041f461a4e/f3/`。

- `visual-evidence.json`：59 张稳定图，名称唯一，逐帧 SHA-256 与图像/DOM 尺度核对通过。包括六组 36 张正式矩阵（短回答、长 Markdown/代码、来源折叠、展开、不完整流、历史只读），6 张冻结 Demo 正文参考，17 张补充交互/兼容图。
- CSS 1440×900、1024×768、390×844，深浅主题。桌面图像与 CSS 尺寸一致；窄屏图像 390×843，底边 1px 取整如实保留，scale=1、36 帧页面横向溢出均为 0。未独立读取浏览器 UI 缩放百分比，不声明像素匹配率。
- 六组联系表及正文、窄屏原图逐项查看；Demo 参考用于气泡、正文层级、代码栏和动作布局对照，来源与状态按上述适配边界审核。首页/设置补充复核深浅与窄屏，Tab 焦点仍在 dialog，Escape 后焦点回到“打开侧栏”。
- `interaction-evidence.json` 记录终态、滚动、Clipboard 和键盘结论。`loading-dynamic.jpg` 是刻意保存的动画加载帧，不属于稳定矩阵。
- 首轮控制脚本超时，元数据未持久化；不带 final- 的图像保留为探索记录并排除。后改为固定单标签、逐帧写清单与连续一致截图；代码反馈位置调整后重采六张代码状态，清单使用最终哈希。历史只读元数据选择 `.pill-input`，不误读反馈评论框。

## 真实证据、跳过与安全

接口/provider 契约未变，仅沿用 R5 已接受的 KB17 指定真实 ANSWER 证据；未再次请求或写入真实 query/history。真实 backend/provider/ask/embedding/rerank/judge/upload/delete 调用为 0。本轮不证明新的真实取消、断流或质量收益，不宣称完整 Demo 迁移或 C19/C20 完成。

Java/评测无改动，跳过 Maven/Python；OpenSpec CLI 未发现，未安装，文件结构/链接/精确 delta 检查不冒充官方 validate。触屏手势由本地跟随实现和滚动测试约束，未使用真实移动设备验证。

除获授权的首批及 F3 baseline 接受/归档和治理文档外，实施仅在 F3 允许路径；无后端/API/DTO/auth/token/request/useSSE/sseParser/qaPresentation、依赖/锁文件、prototype、本地配置、学习文件或其他页面业务修改。本轮授权 Agent 暂存和提交计划内文件，未 push/PR/发布/部署。受影响链接、精确路径与最终 diff 检查结果见执行日志及提交后核查输出。

剩余风险：窄屏底边 1px、既有大 chunk 警告、真实完整异常矩阵及后续页面迁移未完成。F3 baseline 接受与归档完成；归档不扩大前述证据范围。

[proposal](proposal.md) · [design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md) · [首批验收](../2026-10-01-frontend-demo-experience-alignment/acceptance.md)
