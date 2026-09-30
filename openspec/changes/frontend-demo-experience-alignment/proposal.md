# 前端 Demo 首批体验对齐

## 状态与问题

- Change ID：`frontend-demo-experience-alignment`。
- 日期：2026-09-30；Type C；本轮仅规划，实施待事前审查。
- 用户指定首批：侧栏、品牌、主题、首页、设置。

正式前端已具备同步问答和 R5 结构化流式主链，但应用外层仍与 Demo 有明显差异：侧栏全量知识库挤占历史区域、品牌不同、主题多处管理，设置含 mock 资料和未接通操作。本 change 迁移这五个区域的视觉与交互，不将核心接口接通等同于完整 Demo 迁移。

## 范围

1. 侧栏采用固定新问答/知识库/历史入口及最近 20 条真实历史；移除全量 KB 展开，保留 `/kb` 和输入框选库。
2. 复用 Demo 本地箭刃 SVG、词标和 favicon，不重新设计品牌或引入依赖。
3. 单一主题状态支持深色/浅色/跟随系统，兼容现有偏好，统一菜单与设置弹窗。
4. 首页迁移问候、胶囊输入、范围 chip 和紧凑建议；保留同步默认、显式流式、未选 KB 禁止发送和历史只读。
5. 设置提供常规外观和只读真实资料；移除 mock 及未接通的资料编辑、头像、密码、API 密钥操作。
6. 合成状态测试、正式构建和逐区域浏览器视觉验收；共享样式对其他现有页面做兼容检查。

## 非目标与能力边界

- `confirmed`：既有路由、KB 选择、同步问答、R5 与反馈在原验收记录的范围内成立。
- `partial`：正式视觉与 Demo 对齐、设置主题和身份显示。
- `planned`：本 change 五个区域的迁移及离线验收。
- `unknown`：设置暗色异常的具体运行时根因，实施前检查计算样式。
- `out_of_scope`：聊天正文/引用重设计、知识库/历史整页重排、登录页重设计、logout 接线修复、搜索、强调色、工作模式、多轮会话、账号管理后端和高级工作台。

向 rag-system 增加五项前端体验 requirement，不修改后端 API/DTO/认证语义或 R5 `Frontend Structured SSE Presentation`。C19/C20 不阻塞本 change，也不能由本 change 验收替代；前端先行后继续独立校准与门禁。

## 调用、数据与授权

本次仅完成规划，不实施业务代码、不提交。拟议首批使用独立本地端口的封闭合成预览，未知业务请求失败关闭，无真实代理、业务数据出站或 provider 费用，不新增/升级依赖。

真实登录/logout/upload/delete/ask/SSE/embedding/rerank/judge 不包含在本次授权内。后续如需联调，按已有授权的实际范围和预算核查，必要时单独披露。push、PR、部署、baseline 接受与归档均未随规划授权。

## 风险与验收

- R5 尚有未提交增量，实施前保全并确认继承，不能从缺失 R5 的旧 HEAD 开始。
- 共享主题/composer 会影响非首页路径，必须做兼容回归。
- 导航与无效设置入口移除纳入本 Type C 事前审查，不作为无须说明的小修复。
- 完成证据包括含 vue-tsc 的正式构建、逻辑回归、design 固定视口/状态的截图与键盘操作检查。缺少视觉证据不能仅凭 build 收口。
- 不宣称新增真实 provider 场景、生成质量或 C19/C20 门禁通过。

## 关联

[设计](design.md) · [任务](tasks.md) · [spec delta](specs/rag-system/spec.md) · [总说明](../../../docs/roadmap/frontend-demo-backend-integration-plan.md) · [R5 验收](../archive/2026-09-29-frontend-structured-stream-r5/acceptance.md)
