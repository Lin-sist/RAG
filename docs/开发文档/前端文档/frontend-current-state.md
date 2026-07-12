# RAG 前端当前状态与维护边界

> 状态日期：2026-07-12  
> 本文合并旧前端脚手架计划、V2 视觉提案和长期维护文档中仍符合当前代码的内容。

## 1. 技术栈

- Vue 3.5、TypeScript 5.7、Vite 6。
- Element Plus、Pinia、Vue Router、Axios。
- Markdown 渲染使用 `markdown-it` 与 `highlight.js`。

标准验证命令是 `npm run build`，其含义为 `vue-tsc -b && vite build`。当前 `tsconfig.json` 的 `ignoreDeprecations: "6.0"` 会让 TypeScript 5.7.3 报 `TS5103`，这是当前 P0，而不是通过单独运行 Vite 绕过的问题。

## 2. 当前路由

| 路径 | 当前组件/行为 |
|---|---|
| `/login` | `LoginView.vue` |
| `/chat`、`/chat/:id` | `ChatPanel.vue` |
| `/chat-v2` | 同样指向 `ChatPanel.vue`，属于兼容入口 |
| `/kb` | `KnowledgeBaseList.vue` |
| `/kb/:id` | `KnowledgeBaseDetail.vue` |
| `/history` | `ChatHistory.vue` |
| `/history-v2`、`/knowledge-base*`、`/profile` | 兼容重定向 |

`RagChatInterface.vue` 包含 mock data，但当前正式路由没有引用它；后续代码清理应在确认无动态引用后单独删除。`ChatView.vue` 也不是当前 `/chat` 的正式入口。

## 3. 当前能力

- 登录态由 `stores/auth.ts` 管理，路由守卫负责未登录跳转。
- 知识库列表、详情、上传和任务状态已接真实 API。
- `ChatPanel.vue` 支持历史加载、文本流式问答和已有历史 citations 展示。
- 同步回答类型已包含 `citations / contexts / metadata`。
- 历史页面能够展示已保存的 citations。

## 4. 后端能力边界

- 同步 `/api/qa/ask` 可以返回结构化答案、contexts、citations 和 metadata。
- SSE `/api/qa/ask/stream` 当前输出文本 chunk；流式完成后不能假设实时拿到完整 citations/metadata。
- 后端保存流式历史时 citations 为空，界面应明确表达“当前记录未保存来源”，不能制造伪引用。
- 前端不得自行推断知识库权限、置信度或生成质量。

## 5. 当前维护优先级

### P0

1. 修复正式 TypeScript build。
2. 删除或收敛未路由的 mock/兼容组件和重复聊天实现。
3. 保证同步来源完整、流式来源降级的表达不被后续 UI 改造破坏。

### P1

1. 统一颜色、间距、圆角、阴影等 design token，减少硬编码。
2. 统一空态、错误态、权限态、处理中态。
3. 强化文档处理任务进度和失败恢复入口。
4. 让知识库、聊天和历史页形成一致的信息架构。

### P2

1. 响应式与键盘可访问性。
2. 前端性能和关键交互观测。
3. 在后端提供结构化 SSE 完成事件后，再增强实时 citation/context 体验。

## 6. 文档维护规则

- 本文只描述当前实现和稳定维护边界，不记录每次 change 的详细任务。
- 具体改造应进入 OpenSpec change；完成后只在必要时更新本文的“当前状态”。
- 视觉探索稿不能冒充已实现能力。
