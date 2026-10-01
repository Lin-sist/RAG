# F3 聊天正文与来源体验对齐

- Change ID：`frontend-chat-source-alignment`。
- 日期：2026-10-01；Type C；`PLANNED / 待事前审查`，未启用、未实施。
- 前置：首批 `frontend-demo-experience-alignment` 用户接受并授权归档；一次只启用一个 active change。

正式 `/chat` 已接同步问答与 structured-v1，但正文仍使用旧 AI 圆形标记和消息宽度，复制结果仅写控制台，来源片段截断且卡片没有实际展开操作。F3 对齐 Demo 的正文、代码块和来源阅读体验，继续使用现有接口与独立 citations，不能从布局变化推导新的业务或质量收益。

## 范围

1. 统一问题气泡、回答正文、列表/表格/代码块、动作区、来源区域和有消息时的输入区宽度、间距与深浅主题；保全首批首页与设置。
2. 支持代码块复制和整段回答复制，使用浏览器实际剪贴板结果显示成功/失败；没有可用剪贴板时给出手动选择提示，禁止模拟成功或新增第三方依赖。
3. 独立来源按返回字段显示标题、片段和可选检索相关度，支持键盘展开/收起完整片段；不建立行内编号与回答句子的对应关系，不新增原文定位、文档下载或额外查询。
4. 长回答/长代码/长标题可阅读，用户向上阅读时不强制拉回底部，提供“回到底部”入口；只改变本地滚动展示，不改变流式请求、终态或历史事件。
5. 同步默认、流式终态与 reason、非答案/断流/客户端停止、历史只读和现有历史反馈入口继续受既有契约约束。

## 边界与能力分类

- `confirmed`：当前 R5 accepted requirement 及指定真实 ANSWER 证据；现有独立 citations、同步请求与 history 适配。
- `partial`：正文排版、复制反馈、来源片段阅读、长内容滚动；现有代码不足以宣称 F3 完成。
- `planned`：上述 F3 迁移、合成浏览器矩阵与固定视口视觉验收。
- `unknown`：真实所有非答案/断流场景，引用原文精确定位。
- `out_of_scope`：F1 logout、F4 KB/上传、F5 历史列表/新搜索、登录页重设计、多轮 conversation、账号/工作台、C19/C20、模型和后端变更。

本包不构成实现、提交、真实联调、baseline 接受或归档授权。后续实施默认用户手动提交；预览须 configFile:false、独立 localhost 端口、未知业务 fail closed，无真实代理、provider 费用或业务数据出站。真实 provider/ask/upload/delete 等仍需专门的有效授权。

## 验收与关联

含 vue-tsc 的正式 build、受影响逻辑测试和 R5 回归，浏览器逐状态视觉/键盘/滚动检查；合成、沿用真实证据和未验证项分别报告。任何接口/DTO/认证/SSE parser/生产默认行为越界需求均停止并先修订范围。

[design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md) · [总规划](../../../docs/roadmap/frontend-demo-backend-integration-plan.md) · [首批验收](../frontend-demo-experience-alignment/acceptance.md)
