## 一、文档应该包含的内容

**1. 项目概述与技术栈选型**
- Vue 3 + TypeScript + Vite
- UI 框架选择（Element Plus / Naive UI / Ant Design Vue）
- 状态管理（Pinia）、路由（Vue Router）、HTTP 客户端（Axios）

**2. 后端 API 对接契约**
- 统一响应格式：`{ code, message, data, traceId, timestamp }`
- 认证方式：JWT Bearer Token，accessToken + refreshToken 双 token 机制
- 后端 CORS 已全开（`allowedOriginPatterns: *`），前端开发时走代理到 `localhost:8080`
- SSE 流式接口（`/api/qa/ask/stream`）的特殊处理

**3. 路由与页面规划**（后端有 5 个 Controller 模块）
- 登录页（`/auth`系列）
- 知识库管理页（`/api/knowledge-bases` CRUD + 统计）
- 文档管理页（上传、列表、状态轮询 `/api/tasks/{taskId}`）
- 问答对话页（同步问答 + 流式问答 SSE）
- 历史记录页（分页查询 + 反馈提交）

**4. 类型定义（TypeScript 接口）**
- 对应后端所有 DTO/Record：`LoginRequest`, `AuthResponse`, `KnowledgeBaseDTO`, `QAResponse`, `Citation`, `PageResult<T>`, `TaskStatusResponse` 等

**5. 核心业务流程说明**
- 登录 → Token 存储 → 自动刷新 → 登出黑名单
- 创建知识库 → 上传文档 → 轮询任务状态（PENDING→PROCESSING→COMPLETED/FAILED）→ 提问
- 流式问答的 SSE EventSource 处理

**6. 开发规范**
- 目录/文件命名约定、组件拆分规则、API 封装方式、错误处理统一策略

---

## 二、推荐的前端项目结构

```
rag-frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts                  # Vite 配置（含 proxy 代理到 8080）
├── .env.development                # VITE_API_BASE_URL=http://localhost:8080
├── .env.production
│
├── public/
│   └── favicon.ico
│
└── src/
    ├── main.ts                     # 入口
    ├── App.vue                     # 根组件
    │
    ├── router/
    │   └── index.ts                # 路由定义 + 导航守卫（Token 检查）
    │
    ├── stores/                     # Pinia 状态管理
    │   ├── auth.ts                 # 用户登录态、Token 管理
    │   ├── knowledgeBase.ts        # 知识库列表/当前选中
    │   └── chat.ts                 # 问答会话状态
    │
    ├── api/                        # API 层（一个后端 Controller 对应一个文件）
    │   ├── request.ts              # Axios 实例（拦截器：自动加Token、401刷新、错误提示）
    │   ├── auth.ts                 # login / logout / refresh
    │   ├── knowledgeBase.ts        # CRUD + statistics + uploadDocument + listDocuments
    │   ├── qa.ts                   # ask / askStream(SSE) / askSimple
    │   ├── history.ts              # getPage / getById / delete / submitFeedback
    │   └── task.ts                 # getStatus / getResult / cancel
    │
    ├── types/                      # TypeScript 类型定义（对齐后端 DTO）
    │   ├── api.d.ts                # ApiResponse<T>、PageResult<T>
    │   ├── auth.d.ts               # LoginRequest、AuthResponse、UserInfo
    │   ├── knowledgeBase.d.ts      # KnowledgeBaseDTO、CreateKBRequest、Statistics
    │   ├── document.d.ts           # Document、DocumentUploadResponse、DocumentStatus
    │   ├── qa.d.ts                 # AskRequest、QAResponse、Citation、RetrievedContext
    │   ├── history.d.ts            # QAHistoryDTO、QAFeedbackDTO、FeedbackRequest
    │   └── task.d.ts               # TaskStatusResponse
    │
    ├── views/                      # 页面级组件
    │   ├── login/
    │   │   └── LoginView.vue
    │   ├── knowledge-base/
    │   │   ├── KBListView.vue      # 知识库列表（卡片/表格）
    │   │   └── KBDetailView.vue    # 知识库详情 + 文档管理
    │   ├── chat/
    │   │   └── ChatView.vue        # 问答主界面（选知识库 → 提问 → 流式回答）
    │   └── history/
    │       └── HistoryView.vue     # 问答历史 + 反馈
    │
    ├── components/                 # 可复用组件
    │   ├── common/
    │   │   ├── AppHeader.vue       # 顶部导航栏
    │   │   ├── AppSidebar.vue      # 侧边栏
    │   │   └── LoadingSpinner.vue
    │   ├── knowledge-base/
    │   │   ├── KBCard.vue          # 知识库卡片
    │   │   ├── KBCreateDialog.vue  # 创建/编辑弹窗
    │   │   └── KBStatsPanel.vue    # 统计面板
    │   ├── document/
    │   │   ├── DocUploader.vue     # 文件上传（支持 PDF/MD/Word/代码）
    │   │   ├── DocList.vue         # 文档列表 + 状态标签
    │   │   └── DocProgress.vue     # 处理进度条（轮询 task 接口）
    │   ├── chat/
    │   │   ├── ChatInput.vue       # 输入框
    │   │   ├── ChatMessage.vue     # 消息气泡（支持 Markdown 渲染）
    │   │   └── CitationList.vue    # 引用来源展示
    │   └── history/
    │       ├── HistoryItem.vue     # 历史条目
    │       └── FeedbackDialog.vue  # 反馈弹窗（1-5星 + 评论）
    │
    ├── composables/                # 组合式函数（可复用逻辑）
    │   ├── useAuth.ts              # Token 存取、自动刷新
    │   ├── useSSE.ts               # SSE 流式连接封装
    │   └── useTaskPolling.ts       # 任务状态轮询封装
    │
    ├── layouts/
    │   ├── DefaultLayout.vue       # 登录后布局（Header + Sidebar + Content）
    │   └── AuthLayout.vue          # 登录页布局（居中卡片）
    │
    ├── styles/
    │   ├── variables.css           # CSS 变量 / 主题色
    │   └── global.css
    │
    └── utils/
        ├── storage.ts              # localStorage 封装（Token 存取）
        └── format.ts               # 日期格式化、文件大小格式化等
```

**核心设计思路**：

| 层 | 职责 | 对应后端 |
|---|---|---|
| `api/` | 一个文件对一个 Controller，纯 HTTP 调用 | 5 个 Controller |
| `types/` | 1:1 映射后端所有 DTO/Record | 所有 Request/Response/DTO |
| `stores/` | 管全局状态（登录态、当前选中知识库） | — |
| `composables/` | 封装复杂逻辑（SSE、轮询） | 流式问答 + 异步任务 |
| `views/` | 页面，4 个核心页面 | 对应 4 个业务模块 |
| `components/` | 按业务域拆组件 | — |

---

开发顺序要遵循**依赖链**——后端的业务流程是 `登录 → 创建知识库 → 上传文档 → 等处理完成 → 提问 → 看历史`，前端开发顺序就该跟着这条链走。

---

## 推荐开发顺序（共 8 个阶段）

### 阶段 1：项目脚手架搭建
**做什么**：用 Vite 创建 Vue3 + TS 项目，装好所有依赖，配好基础配置
- `npm create vite@latest rag-frontend -- --template vue-ts`
- 安装核心依赖：vue-router、pinia、axios、element-plus（或你选的 UI 框架）
- 配置 `vite.config.ts` 中的代理（proxy 到 `localhost:8080`）
- 搭好 `src/` 下的目录骨架（api/、types/、views/、components/、stores/、composables/、layouts/）
- 配好全局样式、CSS 变量

**产出**：空项目能跑起来，访问 `localhost:5173` 看到空白页

---

### 阶段 2：API 基础层 + TypeScript 类型定义
**做什么**：这是地基，后面所有模块都依赖它
- **`src/types/`**：把后端所有 DTO 翻译成 TS 接口（`ApiResponse<T>`、`PageResult<T>`、`LoginRequest`、`AuthResponse`、`KnowledgeBaseDTO` 等）
- **`src/api/request.ts`**：创建 Axios 实例，配置：
  - 请求拦截器：自动从 localStorage 读 Token 加到 Header
  - 响应拦截器：统一处理错误码（401 跳登录、业务错误弹提示）
- **`src/utils/storage.ts`**：封装 Token 的存/取/删

**产出**：类型安全的 HTTP 客户端就绪

---

### 阶段 3：认证模块（登录/登出/Token 刷新）
**为什么排第一个业务模块**：所有 `/api/*` 接口都需要认证，没登录什么都调不了
- **`src/api/auth.ts`**：封装 `login()`、`logout()`、`refresh()` 三个函数
- **`src/stores/auth.ts`**：Pinia store 管理用户状态（isLoggedIn、userInfo、token）
- **`src/views/login/LoginView.vue`**：登录表单页
- **`src/router/index.ts`**：路由定义 + **导航守卫**（未登录跳 `/login`，已登录跳主页）
- **`src/layouts/AuthLayout.vue`**：登录页布局（居中卡片）
- **`src/layouts/DefaultLayout.vue`**：登录后布局（先做空壳，有 Header + 侧边栏占位）

**产出**：能登录、Token 存到 localStorage、刷新页面不丢登录态、401 自动跳回登录页

**验证方法**：用 `admin / admin123` 登录，看控制台能拿到 accessToken

---

### 阶段 4：整体布局框架
**做什么**：登录后看到的主框架
- **`src/components/common/AppHeader.vue`**：顶栏（Logo、用户名、登出按钮）
- **`src/components/common/AppSidebar.vue`**：左侧导航（知识库、问答、历史）
- 完善 `DefaultLayout.vue`：Header + Sidebar + `<router-view />`
- 路由配置所有页面的路径（先放空页面占位）

**产出**：登录后能看到完整布局，点侧栏能切换页面（虽然页面还是空的）

---

### 阶段 5：知识库管理（CRUD）
**为什么排这里**：这是所有业务的入口，后续上传文档、提问都依赖"先有知识库"
- **`src/api/knowledgeBase.ts`**：封装 create / list / getById / update / delete / getStatistics
- **`src/stores/knowledgeBase.ts`**：管理知识库列表、当前选中的知识库
- **`src/views/knowledge-base/KBListView.vue`**：知识库列表页（卡片或表格展示）
- **`src/components/knowledge-base/KBCard.vue`**：单个知识库卡片（名称、描述、文档数、公开/私有）
- **`src/components/knowledge-base/KBCreateDialog.vue`**：创建/编辑弹窗
- **`src/components/knowledge-base/KBStatsPanel.vue`**：统计面板（文档数、向量数、查询次数）
- **`src/views/knowledge-base/KBDetailView.vue`**：知识库详情页（先做基础信息，文档列表下一阶段加）

**产出**：能创建、查看、编辑、删除知识库

---

### 阶段 6：文档管理 + 异步任务轮询
**为什么排这里**：知识库里要有文档才能提问
- **`src/api/task.ts`**：封装 getStatus / getResult / cancel
- **`src/composables/useTaskPolling.ts`**：任务轮询组合式函数（每 2 秒查一次状态，COMPLETED/FAILED 时停止）
- **`src/components/document/DocUploader.vue`**：文件上传组件（支持 PDF/MD/Word/代码，用 `multipart/form-data`）
- **`src/components/document/DocList.vue`**：文档列表（显示标题、类型、状态标签 PENDING/PROCESSING/COMPLETED/FAILED）
- **`src/components/document/DocProgress.vue`**：处理进度条（对接 TaskStatusResponse 的 progress + message）
- 在 `KBDetailView.vue` 中集成文档上传和文档列表

**产出**：能上传文档，看到实时处理进度，处理完成后文档状态变为 COMPLETED

**验证方法**：上传一个 Markdown 文件，看进度从 10% 走到 100%

---

### 阶段 7：问答对话（核心功能）
**为什么排这里**：知识库有了文档之后才能问答
- **`src/api/qa.ts`**：封装 ask（同步）、askStream（SSE 流式）
- **`src/composables/useSSE.ts`**：SSE 连接封装（建立 EventSource、带 Token、逐字接收、错误处理、关闭连接）
- **`src/stores/chat.ts`**：问答会话状态（消息列表、当前选中知识库、加载中状态）
- **`src/views/chat/ChatView.vue`**：问答主界面（左边选知识库，右边对话区）
- **`src/components/chat/ChatInput.vue`**：输入框（回车发送，支持设置 topK）
- **`src/components/chat/ChatMessage.vue`**：消息气泡（用户提问 + AI 回答，回答支持 **Markdown 渲染**）
- **`src/components/chat/CitationList.vue`**：引用来源展示（显示 source + snippet）

**产出**：能选知识库 → 输入问题 → 看到 AI 逐字回答（流式）+ 引用来源

**这是项目最出彩的页面，面试重点展示**

---

### 阶段 8：历史记录 + 反馈
**为什么排最后**：需要先有问答数据才有历史
- **`src/api/history.ts`**：封装 getPage / getById / delete / submitFeedback / getMyFeedbacks
- **`src/views/history/HistoryView.vue`**：历史列表页（分页，按时间倒序）
- **`src/components/history/HistoryItem.vue`**：历史条目（问题摘要、知识库名、时间、延迟）
- **`src/components/history/FeedbackDialog.vue`**：反馈弹窗（1-5 星评分 + 文字评论）

**产出**：能查看问答历史详情、删除记录、提交反馈

---

## 一张图总结依赖关系

```
阶段1 脚手架 ──→ 阶段2 API层+类型 ──→ 阶段3 认证模块 ──→ 阶段4 布局框架
                                                                    │
         ┌──────────────────────────────────────────────────────────┘
         ▼
   阶段5 知识库CRUD ──→ 阶段6 文档上传+轮询 ──→ 阶段7 问答对话 ──→ 阶段8 历史+反馈
```

每个阶段完成后都是**可运行、可验证**的，不会出现做到一半什么功能都用不了的情况。你想从阶段 1 开始吗？