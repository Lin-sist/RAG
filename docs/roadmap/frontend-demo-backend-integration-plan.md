# 前端 Demo 接入总说明与分阶段推进规划

## 现行推进规则（2026-09-29 修订）

本节是后续前端接入的现行入口。下方原第 1–10 节保留为早期规划，涉及 C18 活跃、等待 C21、未修复契约缺口和历史授权的描述不得直接作为当前启动条件；第 11–15 节为历史执行证据。当前事实仍按 AGENTS.md、ACTIVE_TASK、active change、accepted specs 和代码核验。

### A. 采用总说明与 OpenSpec 分工推进

采用第二种方案：本文维护目标、现状、顺序与验收边界；Type B 独立小修复直接按仓库规则实施；Type C 先完成 proposal、design、tasks 和相关 spec delta，再进入实现。本文不替代 active change，也不构成所有后续功能、业务调用、提交或发布的授权。

整体 Demo 对齐涉及多个页面、共享样式和多个独立提交，推荐作为 Type C 规划，不能拆成多个 Type B 来绕过重大变更流程。独立的局部样式、文案和已有契约内小 bugfix 仍可按 Type B 推进，不必每次新建 change。

2026-09-30：已按用户“根据规划，开始前端demo的接入工程”指令实施 [frontend-demo-experience-alignment](../../openspec/changes/frontend-demo-experience-alignment/proposal.md) 的侧栏、品牌、主题、首页与设置。工程与交互验证完成：47/47 合成测试、vue-tsc 与 Vite build 通过；24 张首批正式页与 12 张 Demo 参考已采集，截图工具 DPI 缩放/留白仍需严格复核，不能据此宣布视觉完全验收。详见 [工程验收记录](../../openspec/changes/frontend-demo-experience-alignment/acceptance.md)。design 中的决策记录保持原规划，不在本文或 AGENT_LOG 重复保存设计决策。

本地提交整理补充：R5 已固定为 `0024e92`；下文未提交状态是规划时快照。规划阶段的本地提交授权不包含新规划的业务实现或 push；本次实现授权来自上述新指令，提交责任为用户手动提交。未执行真实业务调用、baseline 接受、归档或推送。

### B. 与 C19/C20 的顺序

用户当前优先级为：C21/R5 已完成范围 → 前端 Demo 对齐和业务闭环 → C19 judge 校准 → C20 门禁。该顺序与当前 ACTIVE_TASK 一致。

- C19/C20 未完成不阻塞已有 REST 和 structured-v1 契约内的视觉、知识库、历史、反馈接入。
- 前端验收不能代替 generation、citation、no-answer 的质量评测；不得宣称 C19/C20 或质量门禁已通过。
- 一次只推进一个 active change。若 C19/C20 已启动，应先协调顺序；不接管其任务。独立 Type B 维护仅在职责、文件及工作区隔离成立时进行。
- 不改评测数据集、阈值、profile、provider/model、检索或 prompt 默认行为；需要跨越边界时单独明确 Type C 范围。

### C. 当前模块快照

以下为 2026-09-29 工作区事实；分类仅针对所述范围，不代表模块所有场景均通过。

| 模块 | 分类 | 已完成与剩余项 |
| --- | --- | --- |
| 登录/请求基础层 | partial | typed error、single-flight refresh、token rotate 已有实现及合成测试，有真实登录记录；正式侧栏退出仅清本地态，未调用现有 logout API |
| 应用框架/侧栏/主题 | partial | 正式路由、真实 KB/最近历史、深浅切换已存在；仍是旧布局，弹窗主题不统一，侧栏错误与空态区分不足 |
| 知识库列表/详情 | partial | 已接 CRUD、统计、文档、本地筛选与局部表格视觉；未完整对齐 Demo 资料库布局 |
| 上传/任务 | partial | 已接上传和任务轮询，专用 KB 上传解析已有真实证据；删除、失败恢复及完整浏览器路径不能据此外推通过 |
| 同步问答 | confirmed | 指定合成问题的真实回答、独立 citation、history 写入与回读通过，不代表整体生成质量 |
| R5 结构化 SSE | partial | Chrome 单题 ANSWER、terminal citation 与 history 回读通过；39/39 合成测试及正式 build 通过；真实非答案/断连矩阵未完整验证 |
| 历史记录 | partial | 列表、分组、筛选、详情和删除接线存在，真实成功记录可回读；仍为单轮 history |
| 历史反馈 | confirmed | 真实评分/评论提交、回读及刷新后保留已验证 |
| 设置 | partial | mock 资料、保存空函数、改密码仅清输入、API 密钥占位；不能算作账号管理已接入 |
| 完整外观/搜索体验 | planned | 正式版未完整迁移；已加载列表筛选和全量服务端搜索必须区分 |
| 多轮 conversation/重命名 | out_of_scope | 本轮不新增会话实体，不以 history id 伪装 conversation id |
| 引用原文精确定位 | unknown | 可展示现有 citation 片段；原文定位需另核查接口 |
| 评测/MCP/可观测/知识源/研究任务工作台 | out_of_scope | 不在当前迁移范围，Demo 样例不等于真实管理接口 |

证据入口：[正式路由](../../rag-frontend/src/router/index.ts)、[正式聊天页](../../rag-frontend/src/components/chat/ChatPanel.vue)、[设置](../../rag-frontend/src/components/settings/SettingsModal.vue)、[反馈](../../rag-frontend/src/components/history/HistoryFeedback.vue)、[R5 验收](../../openspec/changes/archive/2026-09-29-frontend-structured-stream-r5/acceptance.md)、[执行日志](../../.ai/AGENT_LOG.md)。本轮只读核查这些事实，没有新增真实联调。

### D. 迁移目标

唯一正式落点为 `rag-frontend/`。保留 `prototype/chatgpt-ui-demo/` 原型作为参考，不给原型新增真实 API，也不建立第二套正式前端。

迁移应覆盖品牌、配色、字体层级、间距、侧栏、主区宽度、输入框、消息、来源、资料库、弹窗与响应式布局。仅接真实列表或换背景色不能算作视觉迁移完成。

- `/login`：在 design 明确采用哪个 Demo 登录版本；不新增注册。
- shell/侧栏：首页导航、最近记录和品牌对齐 Demo；未接工作台不伪装成可用入口。
- `/chat`：迁移输入、范围 chip、建议、消息和来源交互；同步仍是当前默认，改变默认须纳入设计和验收。
- `/kb`、`/kb/:id`：对齐资料库和详情布局，不补假大小、假进度或本地假归档。
- `/history`、`/chat/:id`：统一历史及反馈体验，保持一条记录一问一答。
- 设置：展示真实身份与实际生效的外观项；未接密码/API 密钥管理不能模拟成功。隐藏或删除既有入口涉及能力变化时纳入 Type C。
- 引用：来源来自实际返回字段，不自行建立答案句子与引用的对应关系；检索相关度不是正确率。

Demo 网格切换、搜索、强调色等在 proposal 逐项确定保留、适配或延后。新增用户可见能力按 Type C 定义。截图中的暗色页面/浅色弹窗须做浏览器计算样式定位，尚不能把特定 CSS 选择器认定为根因。

### E. 推荐切片与顺序

F1–F6 为 R5 后的新阶段，不替换历史 R 编号，也不是自动实施授权。

| 阶段 | 范围与产出 | 流程与验收 |
| --- | --- | --- |
| F1 独立缺口修复 | logout 接线、侧栏错误/空态等已有契约内缺陷；登记设置占位问题 | 独立小修复可 Type B；隐藏/删除能力入口纳入 F2 Type C，不顺手重构 shell |
| F2 应用框架与视觉系统 | 品牌、主题变量、侧栏、共享控件/弹窗、首页和设置适配 | 推荐下一个 Type C；先逐页差异表和保留/适配/延后清单，再按区域实现；验收深浅主题和窄屏 |
| F3 聊天与来源 | 输入区、消息排版、代码块操作、长回答滚动、来源展开及终态提示 | 整体迁移按 Type C；保留 R5 parser 和单一网络入口；覆盖非答案、断流、非法 terminal 和 abort |
| F4 知识库与上传闭环 | 列表/详情/编辑/统计/上传任务一致性及失败恢复 | 多页迁移按 Type C；专用数据验证创建→上传→解析→问答→来源/历史，真实调用和删除按授权范围 |
| F5 历史、反馈与查找 | 侧栏/历史页刷新、分页浏览、反馈和查找体验 | 新搜索等能力按 Type C；反馈绑定真实 history id；不把局部过滤宣传为全量搜索 |
| F6 整体验收与分流 | 对纳入范围的页面作视觉、交互和业务状态验收，登记延后功能 | 每个 change 独立收口；之后推进 C19/C20；多轮/账号管理/工作台另立规划 |

已创建 change：`frontend-demo-experience-alignment`，首批 F2 覆盖侧栏、品牌、主题、首页和设置，已获授权并完成工程接入，严格视觉收口仍待复核。F3 正文/引用迁移不在本 change 内；F1 logout 接线修复单独保留。精确范围以该 change 为准，不能实施中扩张为所有前端愿望。

### F. 启动、隔离与授权复用

1. 每次先核对 AGENTS.md、Git、ACTIVE_TASK 和相关代码，不默认加载全部后端历史材料。
2. 明确切片目标、允许路径、排除项、依赖契约、测试和停止条件。复用已有明确授权，不逐轮重复询问同一范围。
3. 本次 main 快照领先 origin/main 4 个提交且包含 R5 等未提交增量。不能覆盖或混入，也不能擅自提交以获得 clean 状态。
4. 优先复用合适隔离工作区；新 checkout 必须确认包含 R5 代码和 accepted spec，不能从缺少未提交 R5 的旧 HEAD 开始却声称继承当前能力。
5. Type B 不接管活动指针；Type C 需无其他 active change，先建立完整规划和活动指针并完成事前审查，再写业务代码。
6. 不新增第二套 HTTP client、SSE parser、task poller 或正式聊天页。重复组件先查引用图，再决定是否删除。

### G. 验收与完成标准

- 工程：前端改动运行含 `vue-tsc -b` 的 `npm run build`，逻辑改动运行受影响测试；纯样式不编写镜像实现的测试。保持 Markdown `html: false`，核查来源/链接渲染边界。
- 视觉：design 固定 Demo 参考版本、视口、主题及数据状态；逐页对比桌面/窄屏、深色/浅色、正常/长内容及适用的 loading/empty/error/中断状态。记录“已对齐 / 契约要求的适配 / 延后”，build 不能替代视觉验收。
- 交互：覆盖焦点、键盘关闭、弹窗/下拉菜单、滚动、截断、按钮状态，不能只验收首页。
- 真实链路：静态/合成、本地真实 API、真实 provider、质量评测分层报告。复用不受改动影响的现有证据，只重跑受影响且获授权路径。
- 调用边界：披露 provider/模型、最大调用量、数据出站、费用/限流、timeout/retry 和 KB/query/history 副作用；同范围有效授权可复用，一次性授权不能延长。cleanup/删除不默认包含。
- SSE：不为来源重发 ask；客户端 abort 只证明停止接收，不承诺服务端取消、未写历史或零费用。
- 文档：检查受影响链接、旧路径和 `git diff --check`；日志只追加。既有相同代码/依赖/环境的成功验证可复用。
- 收口：完整 change 验收后，按覆盖该动作的用户授权接受 spec/归档；单片完成不清空整个 change 指针。未授权 Agent 提交时不暂存、不提交；push/PR/部署另行授权。

---

## 早期规划与执行记录（历史材料）

以下内容保留当时规划及执行语境。后续实施先使用上面的现行推进规则，再按需要查询历史，不沿用已过时的启动条件。

> 文档性质：前端并行实施规划与边界；不构成 Type C、真实后端联调、provider 调用、提交、发布或部署授权。
> 状态日期：2026-09-22。
> 当前事实基线：前端规划初始提交 `24f1be2`；C18已完成正式full、验收和归档，`.ai/ACTIVE_TASK.md`恢复`IDLE`。下文关于“C18活跃期间”的隔离规则保留为当时实施边界，不再表示当前阻断。
> 唯一正式落点：`rag-frontend/`。`prototype/chatgpt-ui-demo/` 仅作为视觉、信息架构和交互参考，不改造成第二套生产前端。
> 启动时授权：用户要求在新对话直接开始既有契约内、零外调的前端 Type B 首切片；该次授权不覆盖 Type C、真实 ask/upload/provider 调用、暂存、commit、push、PR、发布或部署。后续 R3b/R5 外调与 R5 归档分别取得授权，见下方执行记录。

## 最新状态（2026-09-29）

上述表格与阶段说明保留当时规划语境。R1-R4 已分别推进；C21 后端 `structured-v1` 契约已在本地确定性范围内验收归档。R5 正式 `/chat` 结构化文本、终态与有效来源接入已完成，合成测试、正式构建及 Chrome 单题真实 `ANSWER` 联调通过；用户授权后已接受 baseline 并归档，`.ai/ACTIVE_TASK.md` 为 `IDLE`。首次代理连接失败与成功复验均保留证据。浏览器停止接收不证明服务端已取消或无费用。R5 的当前事实以归档 change、代码和执行日志为准。

## 1. 目标与边界

本规划回答三个问题：

1. Demo 中哪些页面和交互可以映射到当前真实后端；
2. 每个正式页面必须覆盖哪些加载、空、成功、错误和中断状态；
3. 哪些既有契约内 Type B 能在 C18 活跃期间隔离实施，哪些必须等待 C18 `IDLE`、C21 或另立 Type C change。

本规划修订本身不做：

- 不修改 Vue、TypeScript、Java、配置或数据库迁移；
- 不启动前后端，不请求登录、知识库、问答、embedding、generation 或 judge；
- 不创建第二个 active OpenSpec change，不修改 `.ai/ACTIVE_TASK.md`；
- 不把 Demo 的 mock conversation、行内 `{{cite:N}}`、pipeline timing 或五个静态工作台视图包装成已实现能力；
- 不决定最终视觉稿，不新增依赖，不调整生产默认行为。

## 2. 当前能力分类

| 能力 | 状态 | 规划结论 |
| --- | --- | --- |
| 登录、refresh、logout | `confirmed` | 复用现有 `/auth/**`；refresh 必须 single-flight |
| 知识库 CRUD、统计、文档列表 | `confirmed` | 可先做前端类型/UI/合成测试；真实上传、删除或联调须走独立数据与外调边界 |
| 文档上传与任务轮询 | `confirmed` | 上传按 `202 + taskId`，不得按同步完成处理 |
| 同步问答与独立 citations | `confirmed` | 作为第一条真实问答主链；不生成 Demo 行内引用标记 |
| 扁平历史与 feedback | `confirmed` | 一条 history 是一轮问答，不冒充 conversation |
| 文本 SSE | `partial` | 仅纯文本 delta、`[DONE]`、`[ERROR]`；无结构化终态和流式 citations |
| 停止生成 | `partial` | 客户端可 abort，但当前不能可靠声明服务端 `CANCELLED` 或历史未保存 |
| 结构化 SSE terminal | `planned` | 等待 C21；禁止前端先发明字段或终态 |
| 多轮 conversation、重命名 | `out_of_scope` | 当前无后端实体；不得用 history id 伪装会话 id |
| 引用原文定位接口 | `unknown` | 现有 citation/context 可展示片段；若需新接口则另立 Type C |
| 评测/MCP/可观测/知识源/研究任务 REST | `out_of_scope` | 保留为 Demo 或从正式导航裁剪 |

## 3. 工程结构原则

### 3.1 单一实现原则

- `prototype/chatgpt-ui-demo/`：只保留静态设计参考和历史调研，不增加真实 API client。
- `rag-frontend/`：唯一正式路由、状态、API、错误处理和构建入口。
- 现有 `rag-frontend/src/api/` 是唯一 HTTP 边界；页面和组件不直接散落 `fetch/axios`。
- SSE 继续通过一个 composable 管理，不在不同聊天组件中复制解析器。
- 任务轮询继续通过一个 composable 管理，不在详情页和上传组件各写一套计时器。

### 3.2 数据模型原则

- 后端 DTO 是事实源；Demo mock shape 只能适配到 view model，不能反向要求后端匹配。
- 同步问答与流式问答共享“消息展示模型”，但保留不同的能力标记：同步可带 citations/contexts，当前流式不可。
- history 与 conversation 分开命名：正式代码使用“历史问答记录”，不得创建虚假的 `conversationId`。
- UI 可以定义本地显示状态，但不能提前定义 C21 尚未接受的 transport 字段。

### 3.3 状态所有权

| 状态 | 唯一所有者 | 页面职责 |
| --- | --- | --- |
| access/refresh token、userInfo | auth store | 只消费登录态，不自行操作 storage |
| KB 列表、当前 KB、统计 | knowledgeBase store | 展示与触发动作，不复制缓存 |
| 当前消息、选中 KB、发送状态 | chat store | 组件只派发状态转换 |
| HTTP 错误标准化、401 refresh | request 层 | 页面仅展示标准错误，不解析多种 envelope |
| SSE reader/abort/transport outcome | SSE composable | ChatPanel 不自行解析 `data:` 行 |
| 上传任务轮询 | task polling composable | 页面只维护任务卡片 view model |

### 3.4 C18 与前端的工作树隔离

- 前端实现必须使用独立分支/worktree；建议分支名 `codex/frontend-demo-integration`，起点至少包含 `b53c298` 与 `24f1be2`。
- C18 的正式 canary/full 只能在固定、clean、与 compiler v4 匹配的独立 checkout 执行；不得在前端分支上运行。
- “并行”表示两条工作轨道可以同时存在，不表示两个 Agent 可以同时编辑同一个工作树或共享日志。
- 当前主工作树存在未提交的 `AGENTS.md` 与 `.ai/AGENT_LOG.md` 增量；新 worktree 不会自动继承。新对话必须重新读取其所在 worktree 的规则，并遵守本规划的附加隔离边界。
- 前端任务不得借用 C18 的提交或外调授权；提交责任、真实联调和外部调用分别按前端切片确认。

前端轨道默认允许修改：

- `rag-frontend/**`；
- `docs/roadmap/frontend-demo-backend-integration-plan.md`；
- `.ai/AGENT_LOG.md` 中本前端切片自己的追加段，且只能精确暂存该 hunk。

前端轨道默认禁止修改：

- `.ai/ACTIVE_TASK.md`；
- `openspec/changes/archive/2026-09-22-generation-objective-evidence-baseline/**`；
- C18 的 eval config/schema/report、runner、compiler、数据集、fixture、C17 profile/reference；
- Java 后端、`.env.local`、provider/model/retrieval/prompt/citation/no-answer 生产语义；
- C18 使用的账号、KB、history/query-count 和原始证据。

一旦前端切片需要越过以上禁止路径或改变 API/DTO/持久化/状态语义，立即停止并按 Type C 报告；C18 活跃期间不得创建第二个 active change。

## 4. 信息架构与页面去留

| Demo/现有入口 | 正式落点 | 处理方式 | 依赖 |
| --- | --- | --- | --- |
| 登录页 | `/login` | 保留正式页，吸收 Demo 视觉语言 | 现有 auth REST |
| 新问答/欢迎页 | `/chat` | 作为主入口；先同步问答，后补降级 SSE | 现有 QA REST |
| Demo 多轮会话 | `/chat/:id` | 仅展示单条 history 的问答详情；不称多轮会话 | 现有 history REST |
| 知识库列表 | `/kb` | 保留正式页，吸收筛选和空态设计 | 现有 KB REST |
| 知识库详情/上传 | `/kb/:id` | 保留正式页，展示统计、文档和异步任务 | KB/task REST |
| 历史列表 | `/history` | 保留扁平记录分组、筛选、删除 | history REST |
| 我的反馈 | 暂不设独立主导航 | 可在历史详情内展示/提交；独立页另评估 | feedback REST |
| 设置/主题 | 正式 shell 内本地设置 | 仅保留真实生效项 | localStorage；非业务数据 |
| 全局搜索 | 延后 | 当前只可本地过滤已加载 KB/history；无服务端搜索 | 需要产品决策 |
| 评测/MCP/可观测/知识源/研究任务 | Demo 保留或正式版裁剪 | 不接假接口、不放正式可点击入口 | 无现成 REST |

现有 `/chat-v2`、`/history-v2`、旧知识库路径等兼容入口在实施前单独核查访问来源；未确认无引用前不删除。

## 5. 页面状态设计

### 5.1 全局应用壳

| 状态 | 进入条件 | 页面表现 | 可用操作 |
| --- | --- | --- | --- |
| `AUTH_CHECKING` | 首次加载且本地有 token | 保持稳定骨架，避免先闪登录页 | 无业务操作 |
| `AUTHENTICATED` | token 可用 | 进入目标路由 | 正常操作 |
| `REFRESHING` | 收到 401 且 refresh 可用 | 原请求排队；只发一个 refresh | 不重复弹错 |
| `UNAUTHENTICATED` | 无 token 或 refresh 失败 | 清空认证态，跳转 `/login` | 重新登录 |
| `DEPENDENCY_UNAVAILABLE` | 503/Redis 等依赖失败 | 全局错误提示，可手动重试 | 不自动无限重试 |
| `RATE_LIMITED` | HTTP 429 | 展示限流提示和可用的 `Retry-After` | 到期后手动重试 |

禁止把业务 `ApiResponse.code` 当作唯一成功依据；先以 HTTP status 判定，再兼容 `ApiResponse.message` 与 `ErrorResponse.errorCode`。

### 5.2 登录页

状态：`IDLE → SUBMITTING → SUCCESS`，失败分为 `INVALID_CREDENTIALS / RATE_LIMITED / DEPENDENCY_UNAVAILABLE / NETWORK_ERROR`。

- `SUBMITTING` 时禁用重复提交；
- 成功后存储 access/refresh token 和 userInfo，再跳转原目标路由；
- 失败保留用户名、清空或保留密码由安全评审决定，不显示后端堆栈；
- 当前无注册 API，正式页面不显示可用的“注册”入口。

### 5.3 知识库列表

| 状态 | 表现 |
| --- | --- |
| `LOADING` | 卡片骨架，不显示“暂无知识库” |
| `READY_WITH_ITEMS` | 列表、前端本地过滤、创建入口 |
| `READY_EMPTY` | 明确空态和创建入口 |
| `ERROR` | 保留页面结构，展示重试，不把错误当空列表 |
| `MUTATING` | 对具体创建/编辑/删除操作局部禁用 |

评测 KB 的隐藏或置底只能是明确的前端显示规则；不能把“归档”写成本地假状态。

### 5.4 知识库详情与文档上传

详情状态：`LOADING / READY / NOT_FOUND / FORBIDDEN / ERROR`。

文档状态：`PENDING / PROCESSING / COMPLETED / FAILED / RECONCILIATION_REQUIRED`。当前 TypeScript 联合类型缺少 `RECONCILIATION_REQUIRED`，实施前先修正契约模型。

上传任务状态：

```text
SELECTED → UPLOADING → ACCEPTED(taskId) → POLLING
                                     ├─ COMPLETED → 刷新文档与统计
                                     ├─ FAILED → 保留原因，可重新选择文件
                                     └─ CANCELLED → 明确取消，不显示为失败或成功
```

- HTTP 202 只表示已接受，不表示索引完成；
- 轮询异常停止后提供手动恢复，不并行启动重复 timer；
- KB 向量身份未就绪时应显示后端错误，不自动重建、不切换模型；
- 删除文档或 KB 都需要明确确认，成功后再更新本地列表。

### 5.5 同步问答主链

```text
NO_KB
  └─ select KB → READY
READY
  └─ submit → REQUESTING
REQUESTING
  ├─ HTTP success + answer → ANSWERED
  ├─ HTTP success + explicit no-answer metadata → NO_ANSWER
  ├─ 401 → REFRESHING → REQUESTING | AUTH_REQUIRED
  ├─ 403/404/429/5xx → FAILED
  └─ network error → INTERRUPTED
```

页面规则：

- 未选择 KB 时禁止发送，并解释原因；
- 一次发送只创建一条 user message 和一条 pending assistant message；
- 成功响应以独立 `citations[]` 渲染来源卡片，不从 answer 文本解析 `{{cite:N}}`；
- citations 为空时显示“本回答未返回可展示来源”，不能生成虚假来源；
- score 表述为“检索相关度”，不能写成“答案正确率”；
- `contexts` 是调试/解释材料，不默认等价于有效 citation；
- 失败消息可重试，但重试是新的用户动作，不在前端静默重复真实 ask。

### 5.6 当前 SSE 的降级状态

在 C21 前仅支持以下本地 transport 状态：

| 状态 | 可确认事实 | 不得声称 |
| --- | --- | --- |
| `CONNECTING` | POST 已发起，等待响应 | provider 已开始生成 |
| `STREAMING_TEXT` | 收到纯文本 delta | 已有 citations 或最终业务状态 |
| `DONE_TEXT_ONLY` | 收到 `[DONE]`/流结束且无已知错误 | `ANSWER`、`NO_ANSWER` 等结构化终态 |
| `STREAM_ERROR` | 收到 `[ERROR]` 或读取异常 | HTTP status 一定失败 |
| `CLIENT_ABORTED` | AbortController 已触发 | 服务端已取消、未写历史 |

规划要求：

- 当前 `useSSE` 对 `[ERROR]` 只会当普通文本，对 `AbortError` 也可能返回 `completed=true`；实施前必须先用测试锁定并修正，但本轮不修改代码；
- stream message 不显示同步问答 citations；不为 citations 再偷偷调用一次 `/ask`；
- client abort 后的 partial text 明确标记“已中断”，不能保存/渲染成正常成功；
- C21 接受前不定义 `ANSWER/NO_ANSWER/UNSUPPORTED/ERROR/CANCELLED` transport DTO。

### 5.7 历史与反馈

历史列表状态：`LOADING / READY_WITH_ITEMS / READY_EMPTY / FILTERED_EMPTY / ERROR / DELETING`。

- 按日期分组属于前端 view model，不改变后端记录；
- `/chat/:id` 只加载一条 `QAHistoryDTO`，展示一问一答；
- 无 rename API，不显示可用的重命名按钮；
- feedback 提交必须绑定真实 history id；重复反馈错误单独提示；
- “我的反馈”如需问题标题，必须回查 history 或使用已加载缓存，不假造标题。

## 6. 接口契约映射

| 用户动作 | 方法与路径 | 请求 | 成功数据 | 关键状态/约束 | 实施阶段 |
| --- | --- | --- | --- | --- | --- |
| 登录 | `POST /auth/login` | username/password | `AuthResponse` | 401、429、503 | R1 |
| 刷新 | `POST /auth/refresh` | refreshToken | 新 `AuthResponse` | rotate；single-flight | R1 |
| 退出 | `POST /auth/logout` | Bearer | 无 data | 本地态始终清理 | R1 |
| KB 列表 | `GET /api/knowledge-bases` | — | `KnowledgeBaseDTO[]` | 不能把错误当空数组 | R2 |
| KB 创建 | `POST /api/knowledge-bases` | name/description/isPublic | KB DTO | 201、限流 | R2 |
| KB 详情/更新/删除 | `GET/PUT/DELETE /api/knowledge-bases/{id}` | 对应 DTO | KB DTO/无 data | 403/404 分开 | R2 |
| KB 统计 | `GET /api/knowledge-bases/{id}/statistics` | — | statistics | 统计失败不抹掉详情 | R2 |
| 上传文档 | `POST /api/knowledge-bases/{id}/documents` | multipart file/title | upload response | 202、taskId、幂等头 | R2 |
| 文档列表/删除 | `GET/DELETE .../documents` | — | documents/无 data | 状态五态 | R2 |
| 任务状态 | `GET /api/tasks/{taskId}` | — | task status | 五种终态/进行态 | R2 |
| 同步问答 | `POST /api/qa/ask` | `AskRequest` | `QAResponse` | 真实调用、副作用、限流 | R3 |
| 文本流 | `POST /api/qa/ask/stream` | `AskRequest` | SSE text | POST fetch reader；非 EventSource | R4/C21前降级 |
| 历史分页 | `GET /api/history` | page/size/kbId? | page result | page 从1开始，size≤100 | R3 |
| 历史详情/删除 | `GET/DELETE /api/history/{id}` | — | history/无 data | 用户所有权 | R3 |
| 提交/读取反馈 | `POST/GET /api/history/{id}/feedback` | rating/comment | feedback | rating 1–5、重复错误 | R3 |

`POST /api/qa/debug/retrieve` 不进入普通用户默认问答链。若以后作为 evidence inspection 暴露，需单独产品设计和权限/成本评审。

## 7. 实施前必须修正的契约缺口

这些是当前代码与后端契约之间已经确认的差异，本规划只登记、不修复：

1. 前端 `KnowledgeBaseDTO` 类型声明缺少后端既有的七个向量身份字段；详情页不能继续硬编码模型、维度或 collection 状态。只有逐字段对齐现有后端响应且不改变后端 DTO/运行语义时，才可按 Type B bugfix 实施。
2. 前端 `DocumentStatus` 声明缺少后端既有的 `RECONCILIATION_REQUIRED`；同样只能做声明对齐，不修改后端状态机。
3. 前端 `Citation` 声明仅含 `source/snippet/startIndex/endIndex`，缺少后端既有的 `sourceFileName/documentTitle/documentId/chunkId/score`；只能对齐现有响应，不新增 citation 契约。
4. `ApiResponse<T>.data` 被定义为必填，但删除/退出等成功响应可能没有 `data`；错误响应还有另一种 `ErrorResponse` 结构。
5. request 层的错误规范化尚未形成统一 typed error；页面仍可能重复弹 toast。
6. `useSSE` 尚未把流内 `[ERROR]` 与客户端 abort 可靠映射到 `STREAM_ERROR/CLIENT_ABORTED`。
7. 知识库详情页已有自写轮询，同时仓库存在 `useTaskPolling`，实施时必须合并为单一机制，不能继续两套 timer。
8. `/chat` 与 `/chat-v2` 当前指向同一 `ChatPanel`；迁移前需决定保留兼容别名还是移除，不能复制第二套页面。
9. 仓库存在疑似重复/死代码页面和组件；先做引用图与构建验证，再删除，不能边迁移边保留两个实现。

## 8. 分阶段实施策略

以下阶段只是建议顺序，不构成本轮实施授权。

### P0：规划与并行边界（已完成）

- 冻结页面状态和接口契约映射；
- 选择 Demo 中要迁移的视觉模块；
- 明确 Type B 可在独立 worktree 并行，Type C 必须等待 C18 `IDLE`；
- 不做真实后端联调或 provider 调用。

### R0：实施启动前复核

- 重新检查 Git、active task、C18/C21 状态和后端 Controller/DTO；
- 创建或确认前端独立分支/worktree，检查起点、`AGENTS.md`、日志和其他任务改动已隔离；
- 输出本切片精确允许/禁止文件清单，确认不修改 C18、Java 后端或共享配置；
- 为涉及真实问答的联调披露 provider、模型、数据出站、调用量和 history/query-count 副作用；
- 若范围只复用既有契约且不新增用户能力/接口/状态语义，按小范围 Type B 前端切片处理；否则停止，等待 C18 `IDLE` 后再建立 Type C change。

### R1：认证与错误地基

- 本阶段是当前获准直接开始的首个候选切片；先用测试锁定现状，再做最小实现，不顺手进入 R2/R3；
- 统一 success/error envelope 和 typed error；
- 验证 single-flight refresh、refresh rotate、失败退出；
- 页面只消费标准错误状态，不各自解析响应体。

### R2：知识库、文档与任务

- R2a（可并行 Type B）：仅对齐前端类型声明、迁移列表/详情视觉、合并任务轮询并使用合成 fixture 验证；
- R2b（真实联调）：上传/删除会修改 KB 并可能触发 embedding，须单独授权，只能使用非 C18 账号、KB 和数据，且避开 C18 执行窗口；
- 合并任务轮询实现；
- 不假定历史 KB 可用，不清理或重建 C18 评测 KB。

### R3：同步问答、来源、历史与反馈

- R3a 可先用 mock/fixture 完成前端适配与状态测试；R3b 真实 `/ask` 会写 query count/history 并触发 provider，须单独披露和授权；
- 真实联调必须使用非 C18 账号、KB 和数据，并在 C18 live 窗口暂停；
- 使用独立 citations 数组，不实现行内 citation mock；
- 历史保持一问一答，反馈绑定 history id；
- 完成后再考虑是否需要新的 conversation 能力。

### R4：降级文本流

- 先修复并测试 `[ERROR]`、abort、分帧、尾 buffer；
- 明确 text-only 标签，不展示虚假 citations/terminal state；
- 若产品不能接受降级语义，则整个 R4 延后至 C21。

### R5：C21 后的流式收口

- 以接受后的 terminal contract 为唯一事实源；
- 统一同步/SSE/MCP 的 final state、reason、citations、usage 与 history 行为；
- 再开放完整停止生成、流式来源和结构化终态 UI。

## 9. 防止 Bug 与冗余代码的验收规则

每个实施切片必须满足：

1. 只改一个能力域；不顺手重构其他页面。
2. API、store/composable、page/component 三层职责不重复。
3. 不新增第二套 HTTP client、SSE parser、task poller 或聊天页面。
4. 先补状态和契约测试，再接视觉组件；错误态、空态、中断态与成功态同等验收。
5. 前端必须运行 `npm run build`，其中包含 `vue-tsc -b`；不得只跑 `vite build`。
6. 涉及 markdown/citation 时保持 `html: false`，复核链接与 snippet 的 XSS 边界。
7. 涉及真实 ask/SSE 时记录 provider 调用、timeout、retry、错误类别和本地持久化副作用。
8. 暂存只包含本切片精确路径，不混入 C18、`AGENTS.md`、`.env.local` 或其他任务日志。
9. 不以 mock、静态 Demo 或前端合成状态宣称真实后端能力通过。
10. 前端 worktree 内不得运行正式 C18 canary/full；C18 checkout 内不得合入或试跑未验收的前端修改。
11. `.ai/AGENT_LOG.md` 若含其他任务未提交增量，只精确暂存本切片追加 hunk，不得整文件暂存。

## 10. 实施启动闸门

### 10.1 并行 Type B 启动闸门

- C18 保持 ACTIVE，前端任务明确不接管、不关闭、不归档该 change；
- 前端独立 worktree/分支已建立，起点和工作树状态已记录；
- 当前其他任务修改已提交或与该 worktree 物理隔离；
- 用户确认首个实施切片；提交责任单独记录，未授权时默认用户手动提交，不阻塞实现，但 Agent 不得暂存或 commit；
- 本规划中的 DTO/错误/SSE 已知缺口仍经当前代码复核成立；
- 首切片不新增用户能力，不改变 API/DTO/持久化/状态语义，不修改 Java/C18 路径，不产生真实后端/provider调用；
- 精确文件清单、测试命令和停止条件已经写明。

用户已明确要求新对话直接开始前端对接实现，因而首个 R0/R1 Type B 切片的实现授权可沿用；暂存/commit、真实联调、外调、Type C、push/PR/deploy 未随之授权。

### 10.2 Type C 与真实联调闸门

- 新增用户可见能力，或修改 API、后端 DTO、持久化模型、状态机、权限/provider 契约时，必须等待 C18 正式 full、用户验收、归档且 `.ai/ACTIVE_TASK.md` 为 `IDLE`，再建立独立 Type C change；
- 上传、删除、真实 ask/SSE 等联调先明确测试账号、非 C18 KB/数据、provider/模型、调用量、数据出站、费用/限流、timeout/retry 与 history/query-count 副作用并取得授权；
- C21 未接受前，不实现或宣称完整 structured terminal、流式 citations 或可靠 cancel/history 语义。

R1、R2a、R3a 不依赖 C21，也不要求 C18 先归档；R2b、R3b受真实联调边界约束。R4 是否提前取决于用户是否接受 text-only 降级。完整流式产品体验必须等待 C21，不以 C18 完成代替 C21。

## 11. R0/R1 首切片执行记录（2026-09-20）

- 状态：`confirmed`（离线认证请求测试与正式构建）；真实认证联调 `unknown`，R2/R3/R4 `planned`，真实 provider 调用 `out_of_scope`。
- 隔离分支：`codex/frontend-demo-integration`，起点 `708bd7c5021d6f490dd5c2ab22637a94dc6e148e`；独立 worktree，不改主工作树或 C18 活动指针。
- 已实现：单一 request client 的 typed error；HTTP status 优先并兼容两种错误 envelope；无 data 成功类型；single-flight refresh、双 token 轮换、所有等待者失败收敛、一次重发上限、登录/退出端点不触发刷新；Pinia/storage 同步更新；客户端会话版本阻止跨登录重发；登录防重复提交和单处错误提示。
- 原始 `ErrorResponse` 消费者仅发现反馈弹窗一处，已将错误读取对齐到标准 `message`，不改变反馈业务。
- 验证：`rag-frontend/` 下 `npm test` 14/14 PASS；`npm run build`（`vue-tsc -b && vite build`）PASS；`git diff --check` PASS。无新增/升级依赖。
- 验证边界：全部请求使用合成 Axios adapter，真实 backend/provider 调用均为 0；未运行浏览器端到端或真实登录/登出验收，不以合成测试宣称后端联调通过。
- 剩余项：构建有大于 500 kB chunk 警告；其他业务页面的完整状态适配留在各自阶段。未暂存、提交、合并或发布，提交责任为用户手动提交。

## 12. R3a 离线适配执行记录（2026-09-21）

- 状态：同步问答、独立来源卡片和扁平历史前端适配为 `confirmed`（合成契约测试与正式构建）；真实 `/ask`、真实 history 写入及 provider 效果仍为 `unknown`，R3b 未执行。
- 正式 `/chat` 主链改为单次 `POST /api/qa/ask`，失败不静默重试；不再由正式聊天页默认进入 text-only SSE，也不为补来源额外调用 `/ask`。
- `Citation` 前端类型对齐后端已有 `sourceFileName`、`documentTitle`、`documentId`、`chunkId`、`score` 等字段；来源标题按真实返回字段降级，缺失 score 时不展示假相关度，空 citations 明示未返回可展示来源。
- `/chat/:id` 仅构造一条问题和一条回答，并禁用继续提问，明确不是 conversation；历史列表具备 loading、error/retry、empty、filtered-empty 和 deleting 状态，删除前确认。
- 验证：`node --test tests/*.test.mjs` 26/26 PASS；`vue-tsc -b` PASS；`vite build` PASS；`git diff --check` PASS。构建仍有既有的 >500 kB chunk 警告。
- 验证边界：全部新增问答/历史测试使用合成 request fixture；backend/provider/embedding/rerank/ask/generation/judge 调用均为 0，业务数据出站为 false，query count/history 写入为 0。
- 未进入：R3b 真实联调、反馈闭环、R4 文本流修复、C21 structured terminal/citations/cancel-history；未修改 Java、C18、OpenSpec、`.env.local`，未暂存、提交、push、PR、发布或部署。

## 13. R3b 单题真实联调记录（2026-09-26）

- 用户明确授权固定范围：使用现有唯一启用的 `admin` 账号，在专用知识库 `frontend-r3b-smoke-20260926` 上传一份无敏感信息的合成文本，执行一次同步问答；模型为 `nvidia/nemotron-3-embed-1b` 与 `qwen/qwen3.5-122b-a10b`，timeout 120 秒、retry 0，不清理、不删除、不提交。
- 创建专用 KB `17`、文档 `60`，解析任务完成；问答前确认 document/vector/query/history 为 `1/1/0/0`。上传首次仅因本地沙箱拒绝读取文件而未发出请求，确认服务端仍为 0 文档后才重发一次上传；该重发不是 provider 重试。
- 通过前端 Vite `/auth`、`/api` 代理完成真实登录与唯一一次 `POST /api/qa/ask`。检索链成功：查询变体完成 embedding、每路返回 1 个结果，合并/rerank 后保留 1 个 context；随后 chat provider `/chat/completions` 返回 HTTP 410 Gone，响应为 HTTP 200 envelope 内 `metadata.status=error`，trace id 为 `001a0dbe92c3b181c3f62f6108a71437`。
- 失败即停，没有重试或替换模型。问答后 query count `0→1`，history `0→0`，专用 KB vector count 保持 1；因此真实成功回答、citations 展示和 history 回读仍未验收。
- 根据真实响应修正前端错误适配：`metadata.status=error` 现在进入错误态并清空 citations/contexts，不再把 provider 失败文案渲染为普通成功答案；增加合成回归测试。
- 浏览器 UI 自动化未执行：Playwright 前置检查发现当前 shell 无可用 `npx`，且不能读取现有 Node 安装目录；未安装工具。联调证据来自真实 Vite 代理 HTTP 链路、后端 trace 与持久化计数。
- 范围安全：未修改 `.env.local`、Java/C18/OpenSpec、KB15 或其他业务数据；未 cleanup、暂存、commit、push、PR、发布或部署。下一次真实问答必须单独授权模型修正和新调用，不能复用本次已耗尽的一次性授权。
- 本地验证：`node --test tests/*.test.mjs` 27/27 PASS，`vue-tsc -b`、`vite build` 与 `git diff --check` 均 PASS；构建仍有既有 >500 kB chunk 警告。

## 14. R3b 模型修正与成功复验（2026-09-28）

- 用户针对上一轮停止点明确授权修正 chat 模型、重启后端并执行一次新的真实合成问答；继续使用 KB `17`、文档 `60`、同一问题、`topK=5`、`enableCache=false`、timeout 120 秒、retry 0，失败即停。
- 诊断先只改变一个变量：本次后端进程覆盖为已通过 C18 验收的 `nvidia/nemotron-3-super-120b-a12b`，embedding 仍为 `nvidia/nemotron-3-embed-1b`。单题成功后，依据用户对模型配置修正的明确授权，将 `.env.local` 的唯一 `NVIDIA_CHAT_MODEL` 从已 Deprecated 的旧 Qwen 持久化为同一已验证模型，避免普通启动脚本重新加载旧值；未修改密钥或其他配置。Docker 五项依赖重新创建并健康，当前内部模块安装后启动后端，Vite 代理启动于 5173。
- 唯一一次 `POST /api/qa/ask` 成功：HTTP/envelope 均为 200，耗时约 4.38 秒，回答“北星令牌的代号是 CEDAR-47。”，citations/contexts 均为 1，trace `001a0e56f612d503665e01ffe78b8aa1`。后端 trace 显示两个查询变体 embedding、1 个合并/rerank context 和一次成功 generation；没有重试或额外模型探测。
- 持久化结果：document/vector 保持 `1/1`，query count `1→2`，history `0→1`；新 history id `646`，回读得到同一问题、答案、trace 及 1 条 citation，标题/文件名均为“Frontend R3b 合成联调资料”。这确认同步回答、独立 citation 与 flat history 的真实后端链路通过。
- 根因结论：在账号、KB、文档、问题和请求参数不变时，仅替换已 Deprecated 的 Qwen 模型标识即可消除 410；因此上一轮 generation 失败由旧模型端点退役直接导致，而非检索、Vite 代理或 history 保存故障。
- 浏览器 UI E2E 仍为 `SKIPPED`：Codex in-app browser 的本地桥不可用，未绕过、未安装工具，也未从界面再次发送问答。当前结论是接口级真实前后端联调通过，不扩张为浏览器视觉验收。
- 本轮无前端/Java业务代码修改，仅追加本记录与 AGENT_LOG；复用 2026-09-26 的 27/27 前端测试及正式 build 结果，收尾执行 `git diff --check`。未 cleanup、暂存、commit、push、PR、发布或部署。

## 15. R5 结构化流式前端联调（2026-09-29）

- C21 后端 `structured-v1` 已验收；R5 在正式 `/chat` 显式流式模式使用同一次请求展示文本、完整终态及 terminal 引用，同步问答仍为默认。离线 39/39 合成测试与包含 `vue-tsc` 的正式 build 通过。
- 首次 Chrome 页面发送时 Vite 代理对后端 `ECONNREFUSED`，返回 HTTP 500，前端显示 incomplete；根因是本轮误判监听并再次重启后端，造成短暂不可用。KB17 queryCount 仍为 3，未见新 history。失败证据保留。
- 用户授权继续测试后，只读确认 KB17、文档 60 和模型身份，再从 Chrome 正式 `/chat` 页面发送同一合成问题一次。页面收到 `ANSWER`、文本 `CEDAR-47` 与文档 60 的一条来源。后端 trace `001a0edbb414b4a9da4ef99e3d556ba6` 记录 `POST /api/qa/ask/stream` HTTP 200、检索 contextCount=1、流交付完成及保存 history id 648；无业务自动重试。
- 后置只读核对：KB17 document/vectorCount 保持 `1/1`、queryCount `3→4`，全局 history `647→648`；history 648 的问题、答案、kbId 与 citation 的 documentId/snippet 均与页面一致。真实 `ANSWER` 主链为 `confirmed`；实际非答案和断流仍只由合成测试覆盖。
- 本轮未改 Java、provider、评测、`.env.local` 或其他业务数据；未暂存、提交、push、PR、发布或部署。R5 详细边界与剩余风险见 change 的 `acceptance.md`。
- 用户已明确授权将 R5 spec delta 接受进 baseline 并归档；`Frontend Structured SSE Presentation` 已逐字加入 `openspec/specs/rag-system/spec.md`，change 归档于 `openspec/changes/archive/2026-09-29-frontend-structured-stream-r5/`，活动指针置为 `IDLE`。
