# F3 设计与事前审查包

## 事实基线

2026-10-01 核查 HEAD `cc8d98e18be5122be975af77cd171666621658c7`，包含 R5 `0024e92` 与首批工程。正式 `/chat`、`/chat-v2`、`/chat/:id` 均落到 `components/chat/ChatPanel.vue`；其他 ChatMessage/CitationList 并非该正式路由主链，不在此顺手清理。

ChatPanel 使用 MarkdownIt(html:false)，保留 applySyncResponse/applyStructuredResponse；仅 ANSWER 展示流式 sources。当前正文 800px、composer 760px，复制只处理成功并写控制台，来源为两行截断的 div，scrollToBottom 会在接收文本时调用。QA/history 与 SSE 已有合成回归，不能以迁移名义重写契约适配。

Demo 冻结版本沿用首批四个 blob：index.html `260a1268f34c22a08c64d76777218c0bf17d1f05`；css/app.css `d5bc03b6abe2cf78511a51e7b7a96f364da3a225`；js/app.js `4be498e0d644d5a2f8287a8f990e810e16b38f8d`；favicon.svg `9b46e403dbe3c0803b25488f42c58ec7a473b63f`。实施前重验，不自动追随原稿变化。

## 映射与实现

| 区域 | 对齐 / 适配 | 延后 |
| --- | --- | --- |
| 问题 / 回答 | Demo 气泡与正文层级，共享 760px 内容上限，本地 BrandMark 替换旧 AI 字符标记 | 模型切换、重新生成、编辑问题 |
| Markdown / 代码 | 表格和代码自身横向滚动，代码块语言栏与复制按钮；保持 HTML 转义 | 下载代码、执行代码、网络语法服务 |
| 动作区 | 复制回答可聚焦、窄屏可见，结果反馈以真实 Clipboard Promise 为准 | 新消息评分持久化、分享 |
| 来源 | 独立来源列表、可展开完整 snippet，标题按既有 label 降级；score 缺失不补值 | 行内 cite mock、原文定位、下载 |
| 滚动 | 接近底部时跟随；上翻后暂停跟随，回到底部按钮恢复；发送新问题主动到底 | 修改 stream callback、取消或历史写入契约 |
| 状态 / 历史 | 当前 status/reason 继续展示，异常和部分文本明确；历史仍一问一答及现有反馈 | 多轮实体、历史列表迁移 |

展示组件可拆出 `AnswerContent.vue` 与 `AnswerSources.vue`，它们只消费已适配数据，不调用 API。Markdown 仅有一个 renderer（可置于 `utils/answerMarkdown.ts`）；语言栏/代码的文本必须转义，复制读取已渲染 code 的 textContent，不将回答作为事件脚本或 HTML。Markdown 链接保持安全协议校验；新窗口链接使用适当 rel。禁止把 citation snippet 作为 v-html。

本地滚动状态属于 ChatPanel，流式网络调用和响应处理保持原样；替换 scrollToBottom 的展示实现时，区分发送、接收和用户主动回底，不新增 history 刷新来源。展开状态按消息/来源定位，在新消息或历史路由切换后清理；旧请求结果和 terminal 判断继续沿用 R5。

## 文件与验证边界

批准后允许：ChatPanel.vue（正文/展示/滚动）、新增 AnswerContent.vue、AnswerSources.vue、utils/answerMarkdown.ts、utils/clipboard.ts；`tests/preview.mjs` 的封闭 fixture、`tests/chat-experience.test.mjs`、必要的 qa-history.test.mjs 展示回归；本 change 文档、ACTIVE_TASK、总规划和 AGENT_LOG 追加段。优先局部样式，若确需改 global.css/variables.css，先登记影响并覆盖首批/KB/history/login。

禁止：prototype、Java、API/DTO、auth/token/request、useSSE/sseParser/qaPresentation、后端配置、模型/检索/prompt、accepted specs（未授权接受前）、依赖/锁文件、C19/C20、学习/本地配置、无关旧组件清理。不新增第二套 client/parser/poller 或正式页面。

preview 的正常同步响应和指定 SSE 协议流全部是合成 fixture；按白名单提供 ANSWER、各非答案、缺 terminal、重复/非法 terminal、终态后文本、读取失败及停止场景，未知请求失败关闭；只读取最小请求字段以选择 fixture，不记录 token/正文到日志。未支持的真实业务接口仍拒绝。不以 fixture 宣称后端终态、取消或质量收益。

验收视口为实际 CSS 1440×900、1024×768、390×844，dark/light 六组。每组至少短回答、长 Markdown/代码、来源折叠、来源展开、非完整回答、历史只读六态（至少 36 张）；按适用状态保存冻结 Demo 参考。另检查加载、缺失标题/score、空来源、特殊字符/XSS、复制失败、上翻阅读/回底、焦点可达及首批首页/设置兼容。截图须核对图像与 DOM 尺度、状态和稳定帧；失败帧排除但保留原因，不声明像素匹配率。

运行 `node --test tests/*.test.mjs` 与 `npm run build`（含 vue-tsc），工具链不可用则定位现有入口，不安装升级。文档/资源/边界与 git diff --check 通过后才形成 acceptance。现有真实证据仅在契约未改变时沿用；新增真实场景另授权。

## 决策记录

### 决策 1：正式落点
- **面临的选择**：修改正式 ChatPanel；迁移到旧 ChatMessage 页面；创建第二套 Demo 前端。
- **选了哪个 + 为什么**：沿用 ChatPanel，少量纯展示组件保护已经接通的请求与 R5 适配。
- **放弃的代价**：旧页面迁移会重复接线与回归；第二套前端会产生新的 client 和状态分叉。

### 决策 2：来源阅读
- **面临的选择**：独立来源展开；复制原型行内 citation 编号和原文弹窗。
- **选了哪个 + 为什么**：独立展开真实 snippet，当前接口没有逐句对应或原文精确定位保证。
- **放弃的代价**：行内编号会暗示不存在的逐句证据映射；原文弹窗需新增接口或伪造定位。

### 决策 3：复制与 Markdown
- **面临的选择**：保留当前 renderer 加局部安全展示；引入新的 Markdown/代码依赖；直接插入 Demo HTML。
- **选了哪个 + 为什么**：复用现有 MarkdownIt并保持 html:false，集中 renderer，实际剪贴板成功后才提示。
- **放弃的代价**：新依赖扩大维护和安全面；Demo HTML 直接插入会使回答携带事件或伪造操作。

### 决策 4：滚动与网络
- **面临的选择**：仅调整本地跟随策略；为了排版重写 stream 回调与 parser。
- **选了哪个 + 为什么**：本地跟随和回底入口，不触碰已接受的 structured-v1 契约。
- **放弃的代价**：重写流解析扩大终态与引用错误风险，视觉回归不能证明协议仍正确。

### 决策 5：本轮范围
- **面临的选择**：同时迁移 KB、历史列表、logout 和高级工作台；只做 F3 正文/来源。
- **选了哪个 + 为什么**：只做 F3，其他切片分别规划、验收和授权。
- **放弃的代价**：全量同时做会混入认证和写数据副作用，无法隔离视觉与业务失败。

### 决策 6：验收证据
- **面临的选择**：封闭 fixture 与沿用真实证据；再跑全量真实业务调用。
- **选了哪个 + 为什么**：封闭 fixture 足以验证展示分支，真实接口和 provider 没有因布局变化而需要重跑。
- **放弃的代价**：真实重跑增加费用和 query/history 副作用；本方案不新增真实非答案或取消证据。
