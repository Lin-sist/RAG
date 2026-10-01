# 首批前端体验设计

## 1. 事实与参考基线

规划于 2026-09-29 核查、2026-09-30 落盘。HEAD 为 `2a69509deb525a2f08a222d641e5394ec9e84313`，工作区包含 R5 未提交代码、测试、baseline 和归档材料，不能只用 HEAD 代表本次基线。

2026-09-30 提交整理补充：用户授权本地提交后，R5 已固定为 `0024e924cd3b8c57de196ec0cd71c843f8b95359`。上述未提交状态为规划时快照；后续实施以包含该提交的 checkout 为最低基线，再核对其他工作区改动。此授权不包含本 change 实现或推送。

正式入口为 AppShell → components/layout/AppSidebar → ChatPanel；设置使用 Teleport。auth store 的 userInfo 仅在内存中，刷新后可能为空。AppSidebar/ChatHistory 分别写主题；variables.css 同时存在系统媒体查询与 html.dark。设置主题异常尚未做运行时定位，不预写根因。

冻结参考为 `prototype/chatgpt-ui-demo/` 当前内容，以下是 Git blob 标识：

| 文件 | blob |
| --- | --- |
| index.html | `260a1268f34c22a08c64d76777218c0bf17d1f05` |
| css/app.css | `d5bc03b6abe2cf78511a51e7b7a96f364da3a225` |
| js/app.js | `4be498e0d644d5a2f8287a8f990e810e16b38f8d` |
| favicon.svg | `9b46e403dbe3c0803b25488f42c58ec7a473b63f` |

实施前重验；参考有变化时记录差异再决定，不自动追随并发修改扩大范围。

## 2. 页面与交互映射

| 区域 | 迁移 | 契约适配与延后 |
| --- | --- | --- |
| 侧栏 | 词标、固定导航、最近区、用户菜单 | 新问答/知识库/历史使用现有路由；最近最多 20 条；不显示搜索和五类未接工作台 |
| 品牌 | 本地箭刃、词标、favicon | 新增共享 BrandMark，显示名称“RAG 智能问答”；不重做登录版式 |
| 主题 | 黑白层次、输入框/菜单/弹窗、hover 与边框 | 映射既有 --rag-* 和 Element Plus 变量；强调色选择延后 |
| 首页 | 问候、胶囊输入、范围 chip、紧凑建议 | 保留四条通用建议，点击仅填入；保留同步/流式选择，不迁聊天/工作切换 |
| 设置 | 左侧分类/右侧内容 | 仅常规与只读个人资料；移除密码/API 密钥/换头像/保存等无效入口 |

侧栏不再展开所有 KB，用户经 `/kb` 或 composer 选择范围。展开/折叠/设置不得清空当前消息；“新问答”沿用既有重置行为。桌面折叠保留可识别导航，窄屏默认关闭抽屉。

首页只改空态和共享输入区，不重排回答正文/citation；KB 加载失败与空列表分开并可手动重试；未选 KB 不发送，建议不自动提交。保持同步默认、显式 structured-v1、停止接收和历史详情只读。

## 3. 状态与数据流

### 主题

新增 Pinia theme store，维护 preference=`system|light|dark` 与 effectiveTheme。保留 localStorage key `theme`，兼容已有 dark/light；缺失/无效按 system。只持久化外观，不写身份或业务数据。

挂载前应用有效主题，保留 html 的 dark/light class 兼容旧页面，并设置 color-scheme。system 订阅 matchMedia，手动选择不随系统变化；重复挂载不叠加 listener。storage 失败时本次内存选择仍生效，不声称持久化成功。

AppSidebar、SettingsModal、ChatHistory 统一消费 store；ChatHistory 只改主题接线，不改分页/删除。Teleport 通过根主题和变量生效，不依赖父组件 scoped 穿透；用计算样式检查实际背景/文字颜色。

### 导航与身份

最近记录沿用 getHistoryPage(1,20) 和 history-updated 事件，分离 loading/error/empty/ready，失败提供手动重试；请求代次或等效机制阻止旧会话结果回填，不全量预加载。

设置只读当前 auth store.userInfo；缺失显示“已登录用户”“资料暂不可用”，不默认填 admin/Lin/邮箱，不解析 JWT 补身份、不新增 /me 或身份持久化。退出维持既有行为，服务端 logout 接线作为独立 F1 缺口保留，不能宣称本轮修复。

### 弹窗与响应式

设置具有 dialog 语义与名称、初始焦点、Tab 约束、Escape/关闭按钮和关闭后焦点归还。用户菜单与窄屏抽屉均有可访问按钮和遮罩关闭；打开时避免背景误操作与双重滚动。窄屏设置使用上下或自适应布局，不能固定宽度导致横向溢出。

## 4. 视觉验收基线

桌面展开侧栏目标 300px（Demo --sb-w），首页内容最大宽 760px；窄屏侧栏不超过 min(300px, viewport - 32px)。采用 Demo 原生字体栈、黑白表面与边框层次，不引入网络字体。

固定视口为 CSS 像素 1440×900、1024×768、390×844，缩放 100%；每个覆盖 dark/light，共六组。每组保存首页无 KB、已选 KB、设置常规、设置资料四状态，至少 24 张正式页证据，并保存可比 Demo 参考。原型多出的工作台/模式标注延后，不列为未完成迁移。

额外覆盖长标题/20 条历史、loading/error/empty、折叠/抽屉、长输入、身份缺失、system 切换及刷新。对 `/kb`、`/history`、`/chat/:id`、有回答的 `/chat`、`/login` 做深浅主题兼容截图，不要求这些页面完整复刻。

逐项检查品牌、布局、字号、间距、颜色、边框、hover/focus、截断、响应式，标注“已对齐/契约适配/延后”；不编造像素匹配率，不以主观美观或 build 通过代替视觉证据。

## 5. 文件边界

本轮仅写本 change 四份文档、ACTIVE_TASK、总说明及 AGENT_LOG 追加段。

事前批准后的实现允许路径：

- `rag-frontend/src/layouts/AppShell.vue`、`components/layout/AppSidebar.vue`、`components/settings/SettingsModal.vue`。
- `rag-frontend/src/components/chat/ChatPanel.vue`：限首页、共享 composer 展示和 KB 加载状态，不改 ask/SSE 请求与响应适配。
- `rag-frontend/src/styles/variables.css`、`styles/global.css`、`main.ts`；新增 `stores/theme.ts`、`components/common/BrandMark.vue`。
- `rag-frontend/src/views/history/ChatHistory.vue`：仅统一主题；`rag-frontend/index.html`、`public/rag-mark.svg`：品牌。
- `rag-frontend/tests/preview.mjs`、新增 `theme.test.mjs`、`shell-settings.test.mjs`；`qa-history.test.mjs` 仅必要首页回归。
- 本 change tasks/后续 acceptance、总说明及日志。截图位置实施前登记，不把真实用户/凭据截图写入 tracked files。

禁止修改 prototype 原稿、Java、API/DTO、auth token 存储、request client、SSE parser/useSSE/qaPresentation、accepted specs、C19/C20/评测材料、依赖/锁文件及本地配置；不清理重复页面。越界需求先修订规划。

## 6. 验证、隔离和回滚

顺序为基线保全→主题/品牌→侧栏→首页→设置→综合回归。每片聚焦验证；最后完整前端测试与正式 build（vue-tsc + vite），相同状态下成功结果可复用。

已运行 Preflight：node 可用，npm 不在 PATH，node_modules 及直接 vue-tsc/vite 入口存在；openspec CLI 未在 PATH 和常用用户 npm 入口发现。本轮不安装工具。实现时先定位既有 npm，不可用时分别运行声明版本 vue-tsc 与 vite并如实记录等价命令，不只执行 vite。

合成预览沿用 tests/preview.mjs 的独立端口及 configFile:false，未知业务请求 fail closed，不加真实 proxy。主题测试覆盖旧值、system、手动覆盖、listener 清理、storage 失败；交互测试覆盖身份缺失、无假操作、导航失败、建议填入和未选 KB 禁发。现有 R5 合成回归保护 terminal/citation/abort。

实施前保存本轮与 R5 可区分的文件哈希/补丁，或在获得相应提交授权后固定基线；不得擅自提交用户文件。优先复用可用隔离 checkout，但它必须完整包含 R5 未提交实现与 baseline，不能从旧 HEAD 直接视为就绪。

回滚仅撤销本 change 补丁和新增资源，保留旧 theme=dark/light 可读性；不能整文件还原带 R5 增量的 ChatPanel/测试/共享日志，不 reset/clean 用户工作区，不改后端数据。

## 决策记录

以下为规划选定方案，随本包事前审查，不表示实现已获批准。

### 决策 1：正式落点
- **面临的选择**：在现有 Vue 前端迁移；把静态 Demo 接成第二套真实前端。
- **选了哪个 + 为什么**：迁移现有 Vue 页面，复用已验证的路由、认证和 R5。
- **放弃的代价**：第二套前端会重复网络/状态实现，既有回归不能直接保护。

### 决策 2：侧栏结构
- **面临的选择**：保留全量 KB 和历史；固定导航加最近历史、KB 留在列表与输入框。
- **选了哪个 + 为什么**：采用后者，接近 Demo 且避免 KB 挤占最近记录。
- **放弃的代价**：保留全量 KB 会持续冗长；本方案接受单个 KB 详情多一步访问，以 composer 保留快速选库。

### 决策 3：主题架构
- **面临的选择**：各组件维护；统一 store 并兼容旧值；直接复制 Demo data-theme 全局 CSS。
- **选了哪个 + 为什么**：统一 store 与 token 映射，兼顾一致性和旧页面兼容。
- **放弃的代价**：独立状态会互相覆盖；照搬全局 CSS 易破坏 Element Plus 和未迁移页面。

### 决策 4：设置与身份
- **面临的选择**：保留假表单注明占位；移除无效操作并只读现有身份；新增账号后端与身份持久化。
- **选了哪个 + 为什么**：移除无效操作、缺失身份中性降级，符合首批范围。
- **放弃的代价**：占位表单仍易误导；新增后端/身份存储扩展权限、安全和持久化契约。

### 决策 5：聊天改动边界
- **面临的选择**：重写整套聊天与引用；只迁首页和共享输入外观。
- **选了哪个 + 为什么**：只做首批范围，保护尚未提交的 R5。
- **放弃的代价**：整套重写扩大终态/来源回归；本方案接受正文暂时仍与 Demo 有差异。

### 决策 6：高级能力与质量阶段
- **面临的选择**：开放 Demo 全部导航并等待 C19/C20；先五区域、其他阶段分开。
- **选了哪个 + 为什么**：先五区域，视觉不依赖 judge 校准或工作台新接口。
- **放弃的代价**：全量开放会引入假入口或大量新契约并延后收口；本方案明确保留高级能力缺口。

### 决策 7：验收数据
- **面临的选择**：真实业务全量重跑；封闭合成预览并复用未受影响真实证据。
- **选了哪个 + 为什么**：采用后者，首批视觉可确定性验证且无业务副作用。
- **放弃的代价**：全量重跑增加费用和 history/query 写入；合成结果不能证明新增真实 provider 场景。

### 决策 8：附带修复范围
- **面临的选择**：同时补 logout、登录页和强调色；只做兼容检查，分别后续处理。
- **选了哪个 + 为什么**：后续处理，避免认证副作用与额外产品能力进入五区域。
- **放弃的代价**：一起实现需扩充认证和视觉验收；当前方案留下服务端 logout 接线缺口，不宣称已修复。
