# MCP 接入页实施记录

> 最新页面已按用户明确的客户端接入意图修订（D6）：主接入按钮、三客户端 Logo 与专属配置见[客户端接入记录](client-onboarding-review.md)。本文保留首版后端与协议验证证据；旧的页面结构说明不代表最新主界面。

日期：2026-10-28。状态：`REVIEW_READY · ACCEPTANCE_PENDING`。用户“可以开始实现”授权本方案实现及本地隔离验证；尚未授权实际服务检查、业务写入、真实 provider 调用、Agent 提交、发布或正式归档。

## 💡 30秒人话导读

此前 MCP 只能翻代码与文档接入，现在侧栏有独立的「MCP 接入」页，可以查看服务进程的配置、复制带占位符的连接模板，并由用户主动检查握手、Ping 和能力发现。配置开放与真实连接结果分别展示；检查不会执行任何工具、读取真实资源或调用模型，失败后不会自动重试。

## 改动与契约

- 后端新增 [McpIntegrationController](../../../rag-admin/src/main/java/com/enterprise/rag/admin/controller/McpIntegrationController.java)、[McpIntegrationService](../../../rag-admin/src/main/java/com/enterprise/rag/admin/mcp/McpIntegrationService.java)及独立属性绑定配置。GET `/api/integrations/mcp` 校验当前服务端 principal，只返回白名单且 no-store；整体序列化结果超过 128 KiB 时失败，不截断为成功。关闭 MCP 时仍可查看实现说明，但不注册 transport 或执行 handler。
- [McpCatalog](../../../rag-admin/src/main/java/com/enterprise/rag/admin/mcp/McpCatalog.java)提供工具、schema、资源模板和 serverInfo 的纯定义。页面投影与原 SDK 注册复用它，保留原四工具身份、schema hash、三模板和全部默认开关/授权边界（D2、D5）。
- 前端增加 [McpAccessView](../../../rag-frontend/src/views/integrations/McpAccessView.vue)、API/类型及[窄用途协议适配器](../../../rag-frontend/src/utils/mcpCheck.ts)，挂在既有 AppShell 的受保护 `/integrations/mcp` 路由。正式侧栏增加入口；接入配置、目录/schema 与连接检查分区，深浅主题、窄屏和键盘沿用现有体系（D1）。
- 复制只包含本机开发地址示例与 `<access-token>`；不读取或导出真实凭据。剪贴板失败显示只读文本并转移焦点供手动复制，资源栏目不浏览真实知识库（D3、D4）。
- 检查仅主动触发，固定同源 `/mcp`，最多五次 POST，单次 5 秒、整体 15 秒，每个响应读取上限 128 KiB；拒绝重定向、自动认证重放、重试和列表分页。核对版本、id、serverInfo、能力、无 session、工具 schema 和模板；部分失败保留已通过步骤。取消、卸载、退出或 Token 版本变化会中止并通过序号/身份版本丢弃旧响应。
- Vite 仅新增固定本机 `/mcp` 代理，不删除或伪造 Origin，不放宽默认空允许列表和 local-only。普通 AppShell 会话上下文与侧栏历史读取保持原行为，不属于新增 MCP 资源读取。

## 验证结果

| 检查 | 结果与适用范围 |
| --- | --- |
| 聚焦后端 | 新增 [McpIntegrationTest](../../../rag-admin/src/test/java/com/enterprise/rag/admin/mcp/McpIntegrationTest.java) 4 项与 [McpConsoleProtocolTest](../../../rag-admin/src/test/java/com/enterprise/rag/admin/mcp/McpConsoleProtocolTest.java) 2 项通过；同时运行原 schema identity 与配置测试。覆盖开关矩阵、白名单/无认证、disabled 不创建 transport、大小上限和实际 registry/schema 一致性。 |
| 真实隔离协议 | 真实 Tomcat + 官方 SDK 2.0.0 + 生产 McpOriginAndExposureFilter，local-only=true，测试属性精确允许本机 Origin；五个请求顺序通过，通知返回 202 空响应，无 session，外调关闭仅发现两工具。知识库/文档/历史/QueryEngine/RAGService mocks 全部 verifyNoInteractions，错误 Origin 仍为 403。principal 来自测试过滤器，不冒充真实账号 JWT 烟测。 |
| 完整 Maven | `mvn -q test` 退出 0；本次运行起始后更新的 136 个 Surefire XML 合计 712 tests、0 failures、0 errors、22 skipped。原 C15 独立客户端、认证/Origin/关闭/外调开关等现有回归随全仓执行；没有重跑需独立环境的全部 conformance suite。 |
| 前端回归 | `node --test --test-concurrency=1 tests/*.test.mjs`，155/155。随后补强工具 schema 比对并加入相应漂移用例，MCP 专项 9/9 再次通过；无需重复无关前端回归。 |
| 正式构建 | 最后一次 FrontendBuild 执行 `vue-tsc -b` + Vite，退出 0；MCP 为独立懒加载页面。保留既有主包大于 500 kB 警告，没有新增依赖。 |
| 布局矩阵 | Ego 浏览器中两主题 × 1440×900、1024×768、768×1024、390×844、320×640、844×390，共 12 组。三栏目实际切换、关键控件/pre 的水平边界和 shell/document 横向溢出检查通过；切栏目没有自动协议请求。保存了 36 张工具截图，其中截图工具输出为缩放/可见区域，不当作完整像素画布验收。 |
| 交互矩阵 | 11 组通过：模板复制成功/失败恢复、键盘展开、精确五请求成功、403/401/目录漂移、关闭零请求、旧后端降级、取消、Token 轮换、退出与匿名路由。使用 Ego 原生点击/键盘，复制由本地剪贴板替身验证；API 与协议由独立合成预览提供，不能代替上一行真实 SDK 证据。 |
| 静态门禁 | SensitiveLogs PASS（448 Java 源文件）；DocAndScope PASS。最终文档/日志更新后再次运行，结果见本轮热日志。 |

前端测试服务为新增 [mcp-preview.mjs](../../../rag-frontend/tests/mcp-preview.mjs)，只监听本机 5194，无真实后端代理，未知业务 API 失败关闭。测试数据来自 [mcp-fixtures](../../../rag-frontend/tests/mcp-fixtures.mjs)；[协议专项](../../../rag-frontend/tests/mcp-check.test.mjs)可独立重跑。浏览器完整布局、11 项交互与原生缩放跳过记录保存在本聊天可视化目录的 `mcp-evidence/`，摘要及源文件指纹见仓库忽略目录 `.tmp/mcp-access-console/evidence.json`。

## 修正与未验证项

- 首次受限 Maven 无法读取/解析依赖，按项目本地测试授权使用批准的 Maven 执行通道；随后修正 PowerShell 对带点 `-D` 参数的引用，完成聚焦及全仓测试。首次构建发现 ES2020 没有 `Object.hasOwn`，改用现有基线支持的 hasOwnProperty，没有升级 TypeScript 或 target。
- 静态复核发现路由初稿插在登录布局，已在浏览器验证前改到受认证 AppShell，并增加路由归属检查。发现目录测试最初共享同一份可变 fixture，造成改了响应也改了预期，已独立克隆响应并验证名称/schema/模板漂移。
- Ego 自动滚动不能可靠滚动嵌套内容区；复核实际边界后显式滚动可见，再用真实指针操作。浏览器测试等待函数曾在元素尚未出现时误判完成，已要求元素存在并重新完整通过 11 组，未修改产品逻辑绕过验证。
- 原生 200% 页面缩放快捷键未由 Ego 转发，前后 viewport/DPR 未变化，因此标记 SKIPPED，不用 CDP 尺寸模拟替代。额外完整画布截图遇到浏览器窗口最小化/target 不可用，未取得，不重复包装已有可见区域截图；实体触屏、读屏、跨浏览器也未验证。
- 未重启实际后端、启用实际 MCP、修改真实 Origin/保护配置、读取实际知识库或写实际数据库。没有运行真实 provider/embedding/rerank/generation/judge；新增功能模型调用、业务出站和业务写入为 0。既有全仓测试中的 deterministic tools 调用只在合成测试上下文内发生，不是本页执行工具。
- OpenSpec CLI 未在 PATH 发现；结构、场景和链接做本地检查，不声称官方 CLI 校验。个人资料已接受但正式归档仍待处理；其他挂起项保持原状态。

## 状态与提交

实现和本地验证完成，等待用户审阅；本 change 尚未接受、正式归档或同步长期 baseline。保留用户原有未提交文档，未暂存、提交、push、创建 PR 或部署。

建议提交信息：`feat(MCP): 增加接入页面与有界协议检查`。
