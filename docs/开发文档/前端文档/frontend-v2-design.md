你正在根据 frontend-v2-design.md 进行重构。
请严格遵循设计系统规范。
不要引入新的 UI 库。
优先修改现有组件结构。
所有颜色使用 CSS 变量。
代码保持简洁。
这份文档目标很明确：

> 建立统一设计系统 + 重构聊天核心界面 + 强化 RAG 产品特性
> 而不是简单改 CSS

---

# 📘 Enterprise RAG Frontend V2 设计规范

---

# 1. 项目目标

## 1.1 升级目标

将当前基于 Element Plus 的后台式界面，升级为：

* 统一设计语言
* ChatGPT 风格聊天体验
* 企业级 AI 产品视觉风格
* 强调 RAG 特性（引用来源 / 可信度 / 知识库融合）

---

## 1.2 不做的事情

* 不引入复杂前端框架重构
* 不改动核心业务逻辑
* 不进行大规模组件重写
* 不引入 Puppeteer 抓取外部设计

---

# 2. 设计系统（Design System）

> 所有组件必须遵循以下视觉 Token 规范

---

## 2.1 颜色体系

Primary: `#4F46E5`
Primary Hover: `#4338CA`
Secondary Accent: `#0EA5E9`

Background Page: `#F8FAFC`
Surface: `#FFFFFF`
Border: `#E2E8F0`

Text Primary: `#0F172A`
Text Secondary: `#64748B`

AI Message Background: `#FFFFFF`
User Message Background: `#F1F5F9`

Dark Mode 预留：
使用 `prefers-color-scheme: dark` 实现变量切换

---

## 2.2 圆角规范

| 类型  | Radius |
| --- | ------ |
| 按钮  | 8px    |
| 卡片  | 12px   |
| 输入框 | 24px   |
| 大容器 | 16px   |

---

## 2.3 阴影层级

Level 1（卡片）:

```
0 4px 12px rgba(0, 0, 0, 0.05)
```

Level 2（悬浮）:

```
0 8px 24px rgba(0, 0, 0, 0.08)
```

---

## 2.4 字体层级

Heading 1: 28px / 600
Heading 2: 22px / 600
Body: 14px / 400
Small: 12px / 400

行高统一 1.6

---

## 2.5 Spacing System

统一使用 4 的倍数：

4 / 8 / 16 / 24 / 32 / 48

禁止出现随意 margin。

---

# 3. 聊天界面重构（核心改造）

> 优先级 P0

---

## 3.1 布局结构

```
-----------------------------------
| Sidebar |      Chat Area        |
-----------------------------------
```

Chat Area 内部：

```
-----------------------------------
| Message List (scrollable)      |
-----------------------------------
| Floating Input Area            |
-----------------------------------
```

---

## 3.2 消息展示规范（替换传统气泡布局）

### AI 消息

* 全宽布局
* 左侧头像
* 白底
* Markdown 渲染
* hover 显示操作按钮（复制 / 重新生成）

结构：

```
[Avatar]  内容区域（最大宽度 768px）
```

---

### 用户消息

* 右侧对齐
* 浅灰背景
* 不使用蓝色气泡

---

## 3.3 Markdown 渲染增强

引入：

* `highlight.js` 实现代码高亮
* 代码块带复制按钮
* 表格样式统一

---

## 3.4 RAG 专属增强

在 AI 消息下方增加：

### 1. 引用来源卡片

样式：

* 浅灰背景
* 小号字体
* 点击可展开详情

显示内容：

* 文档名称
* 相似度分数
* 来源知识库

---

### 2. 置信度显示

用 mini progress bar 展示 AI 置信度（如有返回）

---

### 3. 流式输出优化

* 打字 cursor 动画
* loading skeleton
* AI 回复中禁止重复触发请求

---

# 4. 输入框重构

---

## 4.1 结构

* 居中悬浮
* 最大宽度 768px
* 圆角 24px
* 内嵌发送按钮

---

## 4.2 工具条

输入框上方增加：

* TopK 设置
* 是否流式输出开关
* 当前知识库标签

---

## 4.3 底部免责声明

```
AI 可能产生不准确内容，请核对引用来源。
```

---

# 5. 登录页升级

---

## 5.1 背景

* 抽象渐变背景
* 半透明遮罩
* 毛玻璃卡片

---

## 5.2 卡片规范

* 背景 rgba(255,255,255,0.8)
* border 1px rgba(255,255,255,0.3)
* backdrop-filter: blur(20px)

---

# 6. 知识库卡片升级

---

## 6.1 卡片增强

* 顶部渐变装饰条（统一品牌渐变）
* 大号圆形图标背景
* 文档数量 mini badge

---

## 6.2 Hover 动效

* 上移 2px
* 阴影升级为 Level 2

---

# 7. 侧边栏优化

* 品牌 Logo 区域
* 当前菜单高亮左侧 4px 指示条
* 新建对话按钮置顶

---

# 8. 动画规范

统一使用：

```
transition: all 0.2s ease;
```

页面切换：

```
ease-in-out
```

---

# 9. 响应式规范

断点：

```
< 768px
```

规则：

* Sidebar 自动折叠
* 聊天区全宽
* 输入框自适应宽度

---

# 10. 文件改造清单

AuthLayout.vue
AppHeader.vue
AppSidebar.vue
ChatView.vue
ChatMessage.vue
ChatInput.vue
KBCard.vue
variables.css

---

# 11. 开发原则

* 不新增无意义组件
* 优先重构现有结构
* 保持代码简洁
* 所有样式使用变量
* 不允许硬编码颜色

---

# 12. 升级顺序

1. ChatMessage 重构
2. ChatInput 重构
3. Markdown + highlight
4. 引用来源卡片
5. 登录页
6. 卡片美化
7. 响应式

---

# 13. 成功标准

* UI 统一
* 聊天体验接近 ChatGPT
* RAG 特征明显
* 暗色模式可切换
* 移动端可用

---

# ✅ 给 Copilot 的指令模板

你可以在 VSCode 顶部加一段：

```
你正在根据 frontend-v2-design.md 进行重构。
请严格遵循设计系统规范。
不要引入新的 UI 库。
优先修改现有组件结构。
所有颜色使用 CSS 变量。
代码保持简洁。
```

---

你现在这套文档已经不是“改改 CSS”，而是一次系统升级。

如果你愿意，我可以再帮你做一个：

> 🎨 极简科技风版本
> 🌑 暗黑 AI 专业风版本

两个不同方向的视觉策略文档。

你想往哪种产品气质走？
“清爽科技感” 还是 “暗黑极客感”？
