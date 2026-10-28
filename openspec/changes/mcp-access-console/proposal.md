# MCP 接入页与有界连接检查

> 日期：2026-10-28。分级：Type C。状态：`APPROVED · REVIEW_READY`。用户明确需要“接入按钮 → Agent Logo → 客户端专属接入”并授权后，D6 已完成实现与本地验证，见[客户端接入记录](client-onboarding-review.md)。首版证据仍见[实施记录](implementation-review.md)。本 change 尚未接受，无实际客户端安装、真实服务/模型调用、提交或部署授权。

## 为什么改

项目已经实现只读 MCP Server，但接入页首版主要服务开发排障，普通用户仍不知道如何让自己的 AI 助手使用知识库。现将主入口改为“接入 AI 助手”：点击后选择 Codex、Claude Code 或 Cursor，取得对应配置与下一步验证指引；真实配置、工具目录和有界协议检查保留在高级诊断中。

## 本次范围

- 在现有 AppShell 与侧栏内增加受登录保护的 `/integrations/mcp` 页面，包含接入配置、工具与资源、连接检查三部分。
- 新增 `GET /api/integrations/mcp`，只返回白名单配置与静态能力说明；服务关闭时也能解释关闭原因，但不注册或调用 MCP transport。
- 工具目录复用服务端定义，区分“代码已实现”“按当前配置开放”和“本次协议实际发现”；只显示资源模板，不浏览真实知识库、文档或 chunk。
- 提供带 `<access-token>` 占位符的通用连接模板、复制失败恢复与 JWT/访问限制说明；不读取、展示或导出真实 Token。
- 增加显式「检查连接」。一次最多向同源 `/mcp` 顺序发送 5 个 HTTP 请求：initialize、initialized 通知、ping、tools/list、resources/templates/list。不执行任何 Tool，不查询资源实例；模型/provider 调用、业务数据出站和业务写入预算为 0。
- 仅为开发期增加固定 `/mcp` Vite 代理；保留 Origin、本机访问及认证边界，不新增网络探测代理或任意目标 URL。
- D6：新增 Logo 客户端选择弹层；Codex 使用命令/TOML，Claude Code 使用项目 `.mcp.json` 环境变量配置，Cursor 使用官方安装链接并提供 JSON 备选。所有模板只引用 `RAG_MCP_ACCESS_TOKEN`，不导出真实 Token。用户可修改仅用于生成配置的 HTTP(S) 地址，该值绝不改变本页固定同源协议检查的目标。
- D6：弹层只显示“配置已准备/待客户端验证”，不扫描安装状态、不自动写本机配置、不自动执行命令、不以复制或点击链接判为接入成功。验证第一步是客户端发现工具；模型任务仅作为外调开放后的说明，不在本页执行。站内 Agent 体验保留为后续候选，不在本次实现。

## 已核对事实与能力分级

下表保留首版规划时的源码坐标，非当前进度表；当前已实现项与仍未验证项分别以首版实施记录和 D6 客户端接入记录为准。

| 分类 | 本轮核对结论 |
| --- | --- |
| `confirmed` | [C15 配置](../../../rag-admin/src/main/java/com/enterprise/rag/admin/mcp/McpProperties.java)、[注册](../../../rag-admin/src/main/java/com/enterprise/rag/admin/mcp/McpServerConfiguration.java)、[4 个工具定义](../../../rag-admin/src/main/java/com/enterprise/rag/admin/mcp/McpToolSpecifications.java)和[3 类资源模板](../../../rag-admin/src/main/java/com/enterprise/rag/admin/mcp/McpResourceTemplates.java)已存在；默认关闭、无会话、JWT、独立外调开关等契约已进入 [rag-system baseline](../../specs/rag-system/spec.md)。 |
| `partial` | 当前具备后端协议实现及既有隔离测试，尚未接到浏览器页面；[Vite](../../../rag-frontend/vite.config.ts)只代理 `/api` 和 `/auth`，仅新增页面不足以形成 `/mcp` 检查链路。 |
| `planned` | 本 change 的配置接口、独立页面、只含占位符的复制模板和有界浏览器协议检查。 |
| `unknown` | 当前实际进程的 MCP 开关、Origin 允许项、代理拓扑、真实账号可访问性和外部客户端互操作；本轮没有发请求证明这些状态。 |
| `out_of_scope` | 在线改开关、工具执行、真实资源浏览、OAuth、API Key/Token 管理、客户端程序安装/更新、自动扫描/写客户端配置、真实客户端接入验收、远程部署、指标大屏、写工具、MCP 协议升级；Cursor 官方安装链接及客户端专属模板已由 D6 新授权纳入。 |

`rag.compare-sources` 仅确定性并排返回来源，不做语义比较；`rag.search` / `rag.ask` 可能触发 embedding、rerank、生成。目录文案必须保留这些差异，不能把只读等同于没有调用成本。

## 影响、风险与验收

预期影响 `rag-admin` 的只读元数据投影及 MCP 纯定义复用，`rag-frontend` 的导航、页面、API/协议适配器和 Vite 代理，以及相关测试与使用说明。不改数据库、检索/生成/路由指标、provider 默认行为、MCP 开关默认值、认证机制或依赖版本。

主要风险是配置快照被误认为连通证据、Origin 默认空列表使浏览器拒绝、开发代理地址被误写为外部客户端地址、Token 被复制或随重定向发送，以及退出后旧结果回填。具体防护、错误状态、预算和回滚见 [design](design.md)，可观察契约见 [spec delta](specs/rag-system/spec.md)，实施切片见 [tasks](tasks.md)。

实现验收需要配置开关矩阵与严格白名单测试、真实 Servlet/SDK 的协议及 Origin 检查、前端状态/取消/换号测试、包含实际指针与键盘的深浅宽窄浏览器检查，以及正式 FrontendBuild、SensitiveLogs 和 DocAndScope。合成浏览器只能证明交互，不能代替真实 MCP transport 证据；不以本页面重做 C15 全套协议认证。

## 授权与推进边界

个人资料已按用户指令登记[接受](../account-profile-editing/acceptance.md)，保留待正式归档和已有证据缺口。本 change 占用唯一活动槽位进行规划，不自动接受其他挂起项。

2026-10-28 用户确认开始实现，授权本文实现与本地隔离验证，包含上述每次最多 5 个固定协议请求的本地合成验证，不包含对实际运行服务的烟测。真实业务请求、数据库写入、模型/provider 调用和业务出站均为 0。实际环境检查需在已披露环境和授权范围内执行。未获得 Agent 暂存、提交、push、PR、发布或部署授权。
