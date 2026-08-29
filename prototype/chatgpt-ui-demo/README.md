# 高仿 ChatGPT 界面 Demo（RAG 智能问答皮肤）

> 状态：静态原型（Type B 演示件），**未接入任何后端**，不改变现有 `rag-frontend` 的任何能力声明。
> 参考：`docs/开发文档/前端文档/UI-Reference/` 下的 ChatGPT 网页端截图（2026-08 版深色界面）。

## 打开方式

零依赖、可离线，任选其一：

```bash
# 方式一：直接双击打开
prototype/chatgpt-ui-demo/index.html

# 方式二：本地静态服务（推荐，剪贴板等 API 更稳定）
python -m http.server 8765 --directory prototype/chatgpt-ui-demo
# 浏览器访问 http://localhost:8765
```

## 包含的界面与交互

| 界面 | 对应参考截图 | 说明 |
| --- | --- | --- |
| 首页问候 | `chatgpt-empty.png` | 居中问候 + 胶囊输入框 + 轮换建议；顶部分段控件（聊天/工作） |
| 会话视图 | `chatgpt-chat.png` | 用户气泡、流式输出、`已检索 × 个片段`折叠行（对应"思考了 25s"）、代码块复制、消息操作 |
| 知识库视图 | `chatgpt-base.png` | 资料库式表格（名称/更新时间/文档/大小/状态）、搜索、筛选 chips、列表/网格切换 |
| 搜索弹窗 | `chatgpt-search.png` | `Ctrl+K` 呼出，搜历史对话 + 知识库，知识库可一键设为提问范围 |
| 设置弹窗 | `chatgpt-settings.png` | 常规页可用：外观（深/浅/系统）与强调色真实生效，其余为占位 |
| 用户菜单 | `chatgpt-user.png` | 左下角头像菜单，入口齐全 |

RAG 特色交互（区别于纯皮肤）：

- 输入框 `+` 菜单可选**知识库范围**，选中后输入框上方出现范围 chip；
- 回答前模拟检索状态，`已检索`行展开可看命中片段与相似度条；
- 引用角标可点开浮层：原文片段、相似度、来源 chunk。

## 边界声明

- 所有知识库名称、文档、对话、回答均为**演示样例数据**，界面常驻"演示数据"提示；
- 不发起任何网络请求，无外部 CDN / 字体 / 依赖；
- 不写入 `rag-frontend`，不作为生产实现依据；后续落地需按 `docs/roadmap/frontend-ux-iteration.md` 的条目定级（Type B/C）并立 OpenSpec change。
