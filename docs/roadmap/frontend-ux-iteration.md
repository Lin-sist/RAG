# 前端 UX 迭代建议（Frontend UX Iteration）

> 文档性质：前端体验迭代的建议池与优先级排序，Type B 建议文档；不是可执行 OpenSpec change，不改变任何既有能力声明。
> 状态日期：2026-08-29
> 事实基线：基于 2026-08-29 的 `main` 分支代码与 `http://localhost:5173/chat` 实际页面审查；已核实的代码事实见 §2，未逐项核实的均标注"待确认"。
> 授权边界：本文件落盘不等于实现授权。每个条目落地前按 §7 定级；涉及新用户可见能力或后端接口的必须先立 OpenSpec change。
> 方向对齐：`docs/roadmap/iteration-blueprint.md` v6 的第一产品表面"面向员工的可信知识工作台"；与 `docs/roadmap/technical-debt.md` P1.4（SSE 结构化 terminal result）存在依赖关系，见 §5。

## 1. 总体判断

当前 `/chat` 主页面已具备 ChatGPT 布局范式（深色侧栏 + 欢迎页 + pill 输入框 + 消息流），`variables.css` 已建立完整的 ChatGPT 风格设计 token（V2），骨架方向正确。

差距集中在两点：

1. **细节打磨未完成**：输入框仍是单行 `input`、侧栏列表无管理能力、部分交互按钮是空壳（点了两下只 `console.log`）。
2. **RAG 产品自己的特色未做出来**：引用来源是本产品区别于通用聊天工具的核心资产，但目前引用卡片不可点击、示例问题与知识库无关、知识库选择状态表达薄弱。只模仿 ChatGPT 的形，没有放大"依据是什么、来自哪里"的产品承诺。

## 2. 现状事实快照（2026-08-29，已核实）

- 路由 `/chat`、`/chat/:id` 等均直接渲染 `components/chat/ChatPanel.vue`（`router/index.ts`）。
- **疑似死代码簇**：`RagChatInterface.vue`（1539 行）全仓无引用；`ChatView.vue`（566 行）无路由或组件引用；`ChatMessage.vue`、`RetrievedContextList.vue`、`CitationList.vue` 仅被 `ChatView.vue` 间接引用，形成互相引用的死簇（删除前需按 §7 复核）。
- `ChatPanel.vue`（990 行）单文件承载欢迎页 + 消息流 + 输入区 + KB 下拉；模板内 9 处内联 `style`（KB 下拉与选中 chip）。
- Markdown 渲染：`new MarkdownIt({ html: false, linkify: true, typographer: true, breaks: true })`；`html: false` 关闭原生 HTML 注入，当前无 XSS 敞口；`highlight.js` 已在 `package.json` 声明但**未接入** md 实例，代码块无高亮。
- 消息操作：`thumbUp/thumbDown` 仅 `console.log`，未接任何后端；`FeedbackDialog.vue` 组件存在但未被 `ChatPanel.vue` 使用。
- `composables/useSSE.ts` 已有 `AbortController` 中止能力（`abort()`），但 UI 未暴露"停止生成"。
- 输入区：单行 `<input>`；`mic-btn` 无任何 click 处理；placeholder 为英文 `Ask about company knowledge...`。
- 侧栏（`components/layout/AppSidebar.vue`，891 行）：知识库列表无过滤框、无长名截断，评测用 KB（`codex-stage2-*` 等）与业务 KB 混排；历史对话无日期分组、无删除/重命名操作。
- 欢迎页示例问题为写死的 4 条通用问题，与所选知识库无关。
- 引用卡片显示文件名 / snippet / `{{ score*100 }}% Match`，点击无响应；score 语义为检索相似度，"Match"表述有误导。
- 设计 token：主色 `#10A37F`（ChatGPT 绿）；dark mode 变量齐全（`#212121` / `#171717`），同时支持 `prefers-color-scheme` 与 `html.dark` 手动类；字体栈 `Inter` 无中文字形，中文实际回落系统字体。dark mode 是否有手动切换 UI 入口：**待确认**。
- 侧栏 collapse 后的行为（icon rail 还是完全收起）：**待确认**。

## 3. 条目清单

状态取值：`proposed / planned / in_progress / done / deferred`。定级含义见 §7。

| ID | 条目 | 分组 | 优先级 | 定级 | 状态 |
|----|------|------|--------|------|------|
| F-01 | 输入框改自动增高 textarea | A 体验硬伤 | P0 | Type B | proposed |
| F-02 | 侧栏知识库列表治理 | A 体验硬伤 | P0 | B / C 拆分 | proposed |
| F-03 | 历史对话管理（分组 + 删除/重命名） | A 体验硬伤 | P0 | 定级待确认 | proposed |
| F-04 | 清理聊天死代码簇 | A 体验硬伤 | P0 | Type B | proposed |
| F-05 | 引用点击预览原文片段 | B RAG 特色 | P1 | Type C | proposed |
| F-06 | 欢迎页示例问题接知识库 | B RAG 特色 | P1 | B / C 拆分 | proposed |
| F-07 | KB 选择 chip / 下拉组件化 | B RAG 特色 | P1 | Type B | proposed |
| F-08 | 引用分数语义修正 | B RAG 特色 | P1 | Type B | proposed |
| F-09 | 暴露"停止生成" | C 交互完整性 | P1 | Type B | proposed |
| F-10 | 点赞点踩接真实反馈链路 | C 交互完整性 | P1 | 定级待确认 | proposed |
| F-11 | 消息操作 hover 显隐 + 代码块复制 | C 交互完整性 | P2 | Type B | proposed |
| F-12 | 品牌色替换 ChatGPT 绿 | D 视觉与设计系统 | P2 | Type B | proposed |
| F-13 | 中文字体栈补齐 | D 视觉与设计系统 | P2 | Type B | proposed |
| F-14 | 代码块 highlight 接入 | D 视觉与设计系统 | P2 | Type B | proposed |
| F-15 | placeholder 中文化 + 移除未实现 mic 按钮 | D 视觉与设计系统 | P0 | Type B | proposed |
| F-16 | dark mode 手动切换入口确认 | D 视觉与设计系统 | P2 | Type B | proposed |

### A. 体验硬伤（P0）

- **F-01 输入框改自动增高 textarea**：`pill-input` 换为 `textarea`，1~8 行自动增高；Enter 发送、Shift+Enter 换行。这是与 ChatGPT 日常体验差距最大的一项。涉及 `ChatPanel.vue`。
- **F-02 侧栏知识库列表治理**：最小做法（Type B）是加过滤输入框、长名 ellipsis + title、评测 KB 置底或前端标记隐藏；若要引入"归档"这类持久化语义（后端加字段），属 Type C，不得顺手做。
- **F-03 历史对话管理**：按今天/昨天/7 天内/更早分组，hover 出现删除/重命名。后端是否已有对应 history 接口**待确认**；接口已在既有契约内则 Type B，需要新接口则 Type C。
- **F-04 清理聊天死代码簇**：删除 §2 所列 5 个文件前，先全仓 grep 复核 + `vue-tsc` build 验证通过后再删。动机：每改一处 UI 都要在两处确认，是迭代效率的隐性税。

### B. RAG 特色（P1）

- **F-05 引用点击预览原文片段**：点击引用卡片展开/定位原文并高亮对应 chunk。这是 RAG 产品体验的王牌。是否已有可复用的 chunk 原文接口**待确认**；需要新接口则必须先立 OpenSpec change（Type C）。
- **F-06 欢迎页示例问题接知识库**：两档实现——低档（Type B）：未选 KB 时展示最近使用的知识库入口，选中后显示该 KB 名称与文档数；高档（Type C）：从 KB 文档生成示例问题，需要后端内容接口。
- **F-07 KB 选择 chip / 下拉组件化**：把内联 style 的下拉与选中 chip 抽成独立组件，支持搜索切换；该状态是 RAG 界面最重要的上下文提示，值得正式组件化。
- **F-08 引用分数语义修正**：`xx% Match` 改为"相关度 87%"之类中性表述，或把 score 弱化为次要信息/tooltip，避免被误读为"答案匹配度"。

### C. 交互完整性（P1–P2）

- **F-09 暴露"停止生成"**：`useSSE` 已具备 abort 能力，UI 在流式期间把发送键切换为停止键。与债务 P1.4 的关系见 §5：完整终态展示依赖 SSE 结构化 terminal result，纯前端 abort 可先行，不与之冲突。
- **F-10 点赞点踩接真实反馈链路**：现状是空壳。`FeedbackDialog.vue` 已存在，后端 feedback 接口是否覆盖消息级反馈**待确认**；若需新契约则 Type C。在接通前，按钮至少应禁用或移除，不得保留假交互。
- **F-11 消息操作 hover 显隐 + 代码块复制**：复制/重新生成/反馈按钮改为 hover 时出现；markdown 代码块增加复制按钮。

### D. 视觉与设计系统（P2）

- **F-12 品牌色替换**：`#10A37F` 直接沿用 ChatGPT 绿，整体观感"高仿"。在 `variables.css` 集中替换为本项目品牌色，并同步 Element Plus override 与引用高亮色。高亮、来源标签借此获得专属色。
- **F-13 中文字体栈补齐**：`Inter` 无中文字形，中文回落系统默认导致混排基线与观感不可控；补 `"PingFang SC", "Microsoft YaHei"` 等，并考虑中文优先字体（如 MiSans/HarmonyOS Sans）。
- **F-14 代码块 highlight 接入**：`highlight.js` 已声明依赖，接入 md 实例的 highlight 选项即可，属低成本高感知项。
- **F-15 placeholder 中文化 + 移除 mic 按钮**：全中文界面统一 placeholder 文案；语音输入未实现，先移除，避免假交互。
- **F-16 dark mode 手动切换入口确认**：变量层已支持 `html.dark` 手动类，确认 UI 上是否存在切换开关，缺失则补一个（设置弹窗或侧栏）。

## 4. 建议推进切片

一次一个切片，每个切片独立验证、独立提交；不跨切片顺手重构。

1. **切片 1（工程地基，全 Type B）**：F-04 → F-01 → F-15。先清死代码再动 `ChatPanel.vue`，避免在两处重复改。
2. **切片 2（输入与选择，Type B 为主）**：F-07、F-02（低档）、F-03（若接口在既有契约内）。
3. **切片 3（RAG 特色，含 Type C）**：F-08、F-09、F-10、F-05、F-06（高档）。凡涉及后端新接口的条目，先立 OpenSpec change，再写代码。
4. **切片 4（视觉，全 Type B）**：F-12、F-13、F-14、F-11、F-16。

## 5. 与既有文档和债务的关系

- `docs/roadmap/technical-debt.md` P1.4（SSE 结构化 terminal result）：F-09 的**纯前端中止**可独立先行；但流式中断后的终态语义（citations、final state、中断历史标记）以该债务的收敛为准，前端不得抢先把 partial output 当作正常成功渲染保存。
- `docs/roadmap/iteration-blueprint.md` v6：本清单只服务第一产品表面（员工可信知识工作台）的体验补课；蓝图中的引用回看、来源比较、时序判断等长期能力不在本清单范围内，后续按蓝图节奏独立立项。
- `docs/roadmap/technical-debt.md` 未登记前端债务；本文件落盘后，后续如产生确证的_frontend debt_（如死代码未清、假交互），可回链本文件条目 ID，不重复建档。

## 6. 验证基线（所有条目通用）

- 前端改动必须运行包含 `vue-tsc` 的正式 build（`npm run build`）；不得用单独 `vite build` 冒充通过。
- 涉及 markdown 渲染或引用内容的条目，人工核对 XSS 面；当前安全基线为 `html: false`，引入任何渲染增强（高亮、链接预览）时不得打开原生 HTML。
- 涉及 SSE/历史/反馈的条目，记录是否产生真实后端调用与 provider 调用；纯前端改动应为 provider calls=0。

## 7. 条目落地流程

1. 每个条目动手前先定级：
   - **Type B**：纯前端、无 API/DTO/持久化变化 → 聚焦验证 + 追加 `.ai/AGENT_LOG.md`，不建 change。
   - **Type C**：新增用户可见能力、新后端接口、修改既有契约 → 先完成 proposal / design / tasks / spec delta，经用户批准后实施。
2. "定级待确认"的条目（F-03、F-10）先做只读核实（后端是否已有对应接口），再按上面规则定级。
3. 标注"待确认"的代码事实（§2）在对应条目启动时一并核实并回写本文件。

## 8. 迭代记录

- 2026-08-29：初版落盘。来源为当日前端 `/chat` 页面审查对话；全部条目初始状态 `proposed`，未经实现授权。
