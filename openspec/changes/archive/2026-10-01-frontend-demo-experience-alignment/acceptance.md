# 首批工程与视觉验收记录（用户已接受）

当前结论（2026-10-01）：首批工程、交互及指定 CSS 视口视觉复核完成；用户明确回复“接受首批并授权归档”，五项 delta 接受进 baseline，归档于 `openspec/changes/archive/2026-10-01-frontend-demo-experience-alignment/`。以下 2026-09-30 内容保留当时快照，最新视觉证据及限制见文末补充。本结论不代表完整 Demo 页面迁移完成。

日期：2026-09-30。用户指令“根据规划，开始前端demo的接入工程”授权五区域实施；未授权本轮提交、真实业务调用、baseline 接受或归档。本文件记录工程完成事实，不代替最终视觉验收。

## 基线与范围

- 实施起点及当前 HEAD：`e415068c1672a542dfe0d5c875a2e49b43f76c21`；包含 R5 `0024e924cd3b8c57de196ec0cd71c843f8b95359`，无暂存、提交、push 或分支切换。
- Demo 四个 blob 与 design 冻结值一致。原型未修改；未跟踪的 `docs/RAG_RETRIEVAL_OPTIONS_2026.md` 保留。
- 写入范围为 design 允许的 Shell/Sidebar/Settings/ChatPanel 首页与 composer 展示、主题消费/token/main、BrandMark/favicon、合成预览与两个新测试，以及本 change tasks/acceptance、总说明、活动指针和日志。
- 未修改 Java、API/DTO、auth/token、request client、SSE parser/useSSE/qaPresentation、依赖/锁文件、本地配置、accepted specs、C19/C20 或评测文件。

## 实现与能力状态

| 区域 | 状态 | 已确认行为 |
| --- | --- | --- |
| 主题 | confirmed（离线） | 唯一 Pinia store 管理 system/light/dark；挂载前应用、兼容旧值、重复初始化不叠加监听、可清理；storage 失败仍生效且不声称已保存 |
| 品牌 | confirmed（资源/DOM） | 复用本地箭刃 SVG 与金色细节，统一词标与 favicon；不引入网络字体 |
| 侧栏 | confirmed（合成/交互） | 固定新问答/知识库/历史；最近最多 20 条，加载/错误/空/成功分离，手动重试；代次、sessionRevision 与卸载保护旧结果 |
| 首页 | confirmed（合成/交互） | 问候、胶囊输入、范围 chip、四条建议卡片；建议仅填入；未选 KB 禁发；KB 失败可重试；长输入最高 180px 后滚动 |
| 设置 | confirmed（合成/交互） | 常规外观与 auth.userInfo 只读资料；缺失身份中性降级；移除 mock 密钥/资料/密码操作；旧备用界面分类兼容映射到常规 |
| 响应式与焦点 | confirmed（合成/交互） | 300px 桌面侧栏、折叠与窄屏抽屉；设置 dialog、初始焦点、Tab 约束、Escape、归还焦点和背景 inert/滚动恢复 |
| 严格视觉对齐 | partial | 状态截图已采集并复查；工具 DPI 缩放/留白异常尚未消除，不声明固定 100% 浏览器缩放下的严格视觉验收或像素匹配率 |
| 新真实业务证据、C19/C20 | out_of_scope | 本轮真实 backend/provider/embedding/rerank/ask/judge 调用为 0，不新增后端 query/history 数据 |

Settings Teleport 的实际深色计算样式为背景 `rgb(28, 28, 28)`、文字 `rgb(236, 236, 236)`。旧页面主题写入已移除，历史页仅改主题消费，不改分页/删除。窄屏焦点修复后，抽屉与设置关闭均回到“打开侧栏”按钮；设置打开时背景退出 AX 可操作树，关闭后恢复。关闭/卸载恢复滚动与 inert 有独立测试。

R5 请求和响应适配未修改，回归覆盖唯一有效 ANSWER、非答案/损坏/缺失 terminal、终态后文本与客户端 abort；不将原 R5 指定 ANSWER 的真实证据扩大到其他场景。同步默认、显式流式和历史只读仍保留。折叠/展开及开关设置后，同一条合成回答仍在页面。

## 验证

- Preflight：node 与已安装 node_modules 可用，npm 不在 PATH；未安装工具或依赖。
- `node --test tests/*.test.mjs`：47/47 通过。新测试覆盖主题四类、侧栏错误/重试/上限/旧请求、只读资料和设置锁释放；既有认证、QA/history、SSE、轮询回归通过。
- 正式等价构建：在 rag-frontend 内执行 `node node_modules/vue-tsc/bin/vue-tsc.js -b`，成功后执行 `node node_modules/vite/bin/vite.js build`；最终源码通过。保留既有 >500 kB chunk 警告。
- 合成预览：`node rag-frontend/tests/preview.mjs`，仅 127.0.0.1:5188，`configFile:false`、无真实 proxy。已知 login/ask 返回固定 fixture；upload/delete/refresh/stream 等未支持请求拒绝，未知 GET 返回 404。原型通过只读文件白名单提供参考，不改原稿。
- 浏览器覆盖：深浅主题下 /kb、/history、/chat/901、已有回答的 /chat、/login；知识库/最近记录加载、错误、空、20 条长标题；建议禁发、长输入、system 刷新、身份已知/缺失、菜单与抽屉键盘、设置焦点及消息保持。system 的 OS 变化、storage 失败、旧会话请求回填用合成逻辑测试；未修改真实 OS 设置。
- 文档 14 个相对链接、Demo 四个 blob、favicon 原样一致性、`node --check tests/preview.mjs` 和封闭预览探测通过：已知 KB GET 200、未知 GET 404、未知 POST/stream/DELETE 405。最终路径边界与 `git diff --check` 结果见 AGENT_LOG。

## 视觉证据及限制

截图位置在首次采集前于本轮工具中登记：

`C:/Users/Lin/.codex/visualizations/2026/09/30/01a0f1ae-a262-7e50-84be-8fef9fe1a180/`

`visual-evidence.json` 对应 24 张正式页：1440×900、1024×768、390×844 × dark/light × 首页无 KB、已选 KB、常规设置、个人资料。`demo-reference-evidence.json` 对应 12 张冻结 Demo 首页/设置参考；六张 `*-review.png` 为检查用联系表。`compatibility-evidence.json` 对应 10 张现有路由截图。均为合成资料，不包含真实用户或凭据，不写入 tracked files。

每帧记录实际 CSS 视口、DPR、visualViewport.scale 与横向溢出。最终标准帧 CSS/PNG 尺寸匹配、visualViewport.scale=1、无 DOM 横向溢出。DPR 在工具运行中出现 1 与 1.2；部分图像的内容区域仍被缩放并留下空白。文件尺寸和 DOM 断言不能证明图像内部的绝对尺度正确，因此 tasks 6.2 保持未完成。首次采集还发现上一帧残留，已改用稳定等待后的截图并重采；没有把失败或旧帧当成验收通过。

逐区域复查标注：

- 已对齐：本地箭刃/词标、黑白表面层次、圆角与字体栈、胶囊输入与建议卡片形态。
- 契约适配：同步/流式选择保留；最近区是真实 history id 而非 conversation；设置只留两类，窄屏改上下布局；问候不填默认 admin。
- 延后：工作台/搜索/强调色/聊天工作切换/语音等 Demo 入口；正文与引用重排、登录版式、账号后端与服务端 logout。
- 待复核：工具缩放问题排除后的固定 100% 浏览器缩放视觉一致性，不能以构建或上述 DOM 尺寸检查替代。

## 剩余项

工程切片完成，change 保持 ACTIVE。严格视觉收口、用户验收及获授权后的 baseline 接受/归档仍未完成。OpenSpec CLI 未发现，不安装；不宣称官方 validate 已通过。未修改 Java/评测，故未运行 Maven 或 Python 评测套件；未执行新的真实业务联调。

建议提交信息：`feat(前端): 接入Demo首批界面与统一主题`。Commit: pending。

## 2026-10-01 · 固定视口视觉复核

- 当前 HEAD：`cc8d98e18be5122be975af77cd171666621658c7`，工作区起始干净；用户此前提交已固定首批实现，包含 R5。原稿四个 blob 仍与 design 一致。本轮未修改前端源码、预览服务、依赖或后端。
- 仍使用 `tests/preview.mjs` 的 127.0.0.1:5188 封闭 fixture，无真实代理。Chrome 仅在该端口读取页面；真实 backend/provider/ask/embedding/rerank/judge 调用为 0。
- 旧浏览器连接不可用，IAB 重现缩放与留白；Chrome 的视口输入需按 DPR 1.25 换算，再以实际 CSS 尺寸核对。最终使用一个标签、固定对象绑定及连续一致截图；多标签/旧对象绑定产生的失败帧不纳入证据。
- 新证据目录：`C:/Users/Lin/.codex/visualizations/2026/10/01/01a0f557-b647-7452-a4b7-63041f461a4e/`。`visual-evidence.json` 仅指向 `verified-*.jpg` 的 24 张正式页，`demo-reference-evidence.json` 指向 12 张原型参考；均记录实际视口、DPR、scale、溢出、图像尺寸与 SHA-256。名称各自唯一，无真实身份或凭据。
- 正式页覆盖 1440×900、1024×768、390×844 × dark/light × 无 KB、已选 KB、常规设置、资料。最终身份缺失状态正确显示“已登录用户 / 资料暂不可用”；已知 fixture 身份的检查沿用原交互证据。桌面图像分别为 1440×900 / 1024×768，侧栏实际 300 CSS px；所有帧 scale=1，无页面横向溢出。窄屏原生 JPEG 为 390×843，底边少 1 像素是取整限制，未裁剪、补边或放大原图。
- 已实际查看六张 `*-final-review.png` 联系表的全部正式帧及 `demo-final-review.png` 的全部参考帧，并查看原尺寸桌面设置图。联系表仅用于浏览，不能作为原图的绝对尺度证据；尺寸断言也不代替上述查看。未发现此前的整体缩小留白或状态错帧；最初的失败图、错误标签图和测试图均排除在最终 manifest 外。

| 检查项 | 结论 | 适配或限制 |
| --- | --- | --- |
| 品牌、字体栈与表面层次 | 已对齐 | 本地箭刃、词标、黑白主题、边框和圆角；未引入网络资源 |
| 桌面侧栏与首页 | 已对齐 / 契约适配 | 300px 侧栏、760px 内容上限；保留同步/流式选择与真实单轮 history 导航 |
| 输入与建议 | 已对齐 / 契约适配 | 胶囊输入、范围 chip、四条通用建议；不复制指向虚构资料的建议或语音入口 |
| 设置深浅主题与资料 | 已对齐 / 契约适配 | 常规和只读资料，缺失身份中性显示；不复制原型未接功能 |
| 窄屏 | 契约适配 | 抽屉入口可见，设置上下布局；原型窄屏设置本身挤压成窄列，不照搬。图像底边 1px 取整保留 |
| 工作台、搜索、强调色、账号操作 | 延后 | 不属于本 change；正文和来源完整迁移留给 F3 |

工程结果沿用首批相同源码/依赖下的 47/47 测试、vue-tsc 与 Vite build；本轮仅更新验收与后续规划，不重复全套构建。Java/Python、真实业务矩阵和 C19/C20 未触及，未运行。视觉结论按实际 CSS 视口与原图尺度给出，不独立宣称读取到 Chrome UI 的缩放百分比，也不声明逐像素复刻。截图底边取整不能用于核验最后一行像素。

tasks 6.2 完成；6.6 保持未完成。ACTIVE_TASK 仍指向本 change，未接受 baseline、未归档、未暂存/提交或推送。F3 的四份规划已准备，待本 change 获用户接受并授权收口后，再进行其事前审查与实施。
