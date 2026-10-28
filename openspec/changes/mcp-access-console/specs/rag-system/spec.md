## ADDED Requirements

### Requirement: Client First MCP Onboarding Dialog

主页面 SHALL 以“接入 AI 助手”为主操作，点击后在可访问弹层中显示 Codex、Claude Code、Cursor 的本地 Logo 与名称；选择后只展示该客户端的步骤。既有配置、工具目录与五步协议检查 SHALL 保留在默认收起的高级诊断中，不修改后端、服务开关、认证或调用预算。弹层 MUST 支持键盘、Escape、背景关闭、焦点圈定/恢复、背景 inert、深浅主题和 320px 回流。

#### Scenario: 选择与关闭客户端引导
- GIVEN 用户进入 MCP 接入页
- WHEN 点击接入按钮并选择某客户端
- THEN 显示对应接入操作，其他客户端可切换，页面不扫描本机安装情况或调用 MCP
- WHEN 关闭、退出、身份版本变化或路由卸载
- THEN 清理本次输入与准备状态，释放背景锁，并在可用时恢复原触发器焦点

### Requirement: Credential Free Client Specific Configuration

系统 SHALL 为 Codex 提供 HTTP 命令/TOML，为 Claude Code 提供项目 `.mcp.json`，为 Cursor 提供官方安装链接与备选 JSON；载荷 MUST 只引用专用 `RAG_MCP_ACCESS_TOKEN`，不读写/复制/导出真实 Token，不修改本机客户端配置。地址输入 MUST 仅用于生成配置，接受受限 http/https URL，拒绝凭据/query/fragment/控制和 Shell 注入字符，不改变固定同源检查目标。Cursor 链接 MUST 使用固定官方 scheme/host/path，并只编码当前客户端配置。

#### Scenario: 复制和打开配置
- GIVEN 用户选择客户端并填写合法地址
- WHEN 复制命令或配置，或明确点击 Cursor 安装入口
- THEN 对应格式与地址正确，凭据只有环境变量引用；Cursor 的 Base64 解码只含 URL/header，不含当前会话凭据
- AND 安装动作交给客户端确认，网页不写本机文件或自动安装，复制失败提供可选中的文本

#### Scenario: 恶意地址或切换输入
- GIVEN 地址含非 HTTP(S) scheme、用户信息、query/fragment、控制字符或可执行 Shell 字符
- WHEN 尝试生成或打开配置
- THEN 阻止操作并显示可恢复字段错误，不生成可执行命令或链接
- WHEN 修改地址或切换客户端
- THEN 清空旧复制/打开反馈，不沿用先前配置已准备的状态，不发网络探测

### Requirement: Honest External Client Handoff And Verification Guidance

客户端引导 SHALL 清楚说明项目 access JWT 的本地配置与有效期边界、客户端查看工具的方法，以及检索/问答需外调工具开放的条件。本期 MUST 只标记“配置已准备/待客户端验证”；复制成功、打开安装链接和浏览器协议检查均 MUST NOT 当作外部客户端接入成功。站内 Agent、OAuth、凭据签发/自动刷新、本机检测/更新和真实外部客户端互操作不属于本期。

#### Scenario: 完成模板准备
- GIVEN 用户复制了客户端配置或点击 Cursor 打开入口
- WHEN 页面反馈操作结果
- THEN 仅提示配置已准备或已发起打开、待客户端确认与验证，不显示已连接或已安装
- AND 首先引导客户端工具发现；复制示例任务不执行模型或工具，检索/问答示例标明外调前提


### Requirement: Authenticated Read Only MCP Integration Metadata

系统 SHALL 提供 `GET /api/integrations/mcp`，从当前 authenticated principal 验证服务端身份，以 `ApiResponse` 和 `Cache-Control: no-store` 返回设计约定的配置与纯能力描述白名单。该接口 MUST 在 MCP 关闭时仍可返回说明，但 MUST NOT 注册 transport、实例化执行 handler、探测网络、读取业务资源、执行工具、调用 provider 或写业务事实。响应 MUST 有 128 KiB 上限；超过时返回稳定脱敏错误，不得截断后报告成功。

#### Scenario: 默认关闭仍可解释配置
- GIVEN 已认证用户且 `rag.mcp.enabled=false`
- WHEN 读取接入元数据
- THEN 返回 enabled=false、externalToolsEffective=false 和空的配置开放名称集合，静态实现目录明确为说明
- AND `/mcp` 保持未注册，业务读取/写入/provider 调用为 0，不把目录说明作为协议发现

#### Scenario: 身份与敏感字段边界
- GIVEN 无认证、无效 principal 或客户端试图指定 tenant/user/endpoint/provider
- WHEN 请求接入信息
- THEN 未认证/非法身份被拒绝，参数不能选择身份、配置或目标地址
- AND 接口只返回固定白名单，不含 Token、用户/租户 ID、完整允许来源、原始配置、数据库或 provider 凭据

#### Scenario: 关闭服务但配置了外调开关
- GIVEN enabled=false 且 externalToolsConfigured=true
- WHEN 读取元数据
- THEN 保留配置原值但 externalToolsEffective=false，开放集合为空，不标记 search/ask 可调用

### Requirement: Canonical MCP Capability Description And Discovery Separation

MCP 说明接口与既有 Server 注册 SHALL 共享工具 schema、资源模板和 serverInfo 的纯定义。页面 MUST 区分代码实现目录、配置允许集合与本次协议发现集合；工具显示 MUST 保留外部调用可能性、只读不等于零成本、compare-sources 不做语义比较等边界。资源栏目 MUST 只展示模板，不浏览资源实例。

#### Scenario: 外调关闭时目录与发现不同
- GIVEN MCP 启用且 external-tools-enabled=false
- WHEN 查看实现目录并完成工具发现
- THEN 目录可解释四个已实现工具，但配置开放和实际发现只允许 get-citation/compare-sources
- AND search/ask 明确标记未开放，不能通过页面执行

#### Scenario: 共享定义保持一致
- GIVEN 相同代码与配置的元数据和真实 SDK registry
- WHEN 比较工具 schema、模板及 serverInfo
- THEN 名称与定义一致；未启用服务的静态说明不被当作可调用 registry

#### Scenario: 实际发现与快照不符
- GIVEN 检查使用的配置快照与实际工具/模板不一致，或列表返回非空 nextCursor
- WHEN 接收发现响应
- THEN 显示目录变化或发现未完成，提示刷新后手动重检，不追取分页、不覆盖差异、不报告整体通过

### Requirement: Accessible MCP Access Page And Placeholder Connection Template

前端 SHALL 在 AppShell 内提供受登录保护的 `/integrations/mcp` 独立页和侧栏入口，包含接入配置、工具与资源、连接检查。页面 SHALL 沿用既有主题、侧栏抽屉和偏好，不修改个人资料、授权、问答记录或共享会话状态。模板 MUST 仅含明确标注的 endpoint 示例和 Bearer 占位符，不读取/展示/导出真实 Token，不宣称通用参数示例适配全部客户端配置格式。

#### Scenario: 复制成功或失败
- GIVEN 用户查看连接参数模板
- WHEN 点击复制
- THEN 只复制 URL 与 `<access-token>` 占位模板；成功有可感知反馈，失败可手动选取完整文本
- AND 不读取 Token、剪贴板内容或写入本地持久化，不发协议或业务请求

#### Scenario: 地址语义与旧后端降级
- GIVEN 当前站点通过开发代理访问后端，或元数据接口返回 404
- WHEN 页面加载
- THEN 将同源检查路径与本机开发地址示例分开说明，不把浏览器 origin/转发头当成可信外部接入地址
- AND 接口缺失时显示不可用与恢复入口，不猜测 MCP 开关或生成成功状态

#### Scenario: 主题、窄屏与键盘
- GIVEN 深浅主题、320px 窄屏、200% 页面缩放或减少动效偏好
- WHEN 操作栏目、复制、披露、检查和取消
- THEN 文本与关键操作完整可达，控件有名称/焦点/键盘支持，状态不只依赖颜色或动画，长内容可完整读取

### Requirement: Explicit Bounded Same Origin MCP Protocol Check

连接检查 MUST 仅由已读取 enabled=true 配置后的用户明确动作触发，使用固定同源 `/mcp` 与当前会话 Bearer header。一次检查 SHALL 最多顺序发送 initialize、notifications/initialized、ping、tools/list、resources/templates/list 五个 HTTP POST，失败即停止；MUST NOT 自动重试、轮询、翻页、刷新/重放认证、跟随重定向、访问任意地址、执行 tools/call 或读取资源实例。每个请求超时 MUST 不超过 5 秒，整次共享截止时间 MUST 不超过 15 秒，每个 JSON 响应在读取时 MUST 限制为 128 KiB。

#### Scenario: 用户主动检查固定方法
- GIVEN 元数据配置启用且用户尚未点击检查
- WHEN 页面进入、切栏目、切主题或刷新元数据
- THEN 协议请求数为 0
- WHEN 用户明确点击检查且各步骤通过
- THEN 最多顺序发五次固定请求，通知使用无 id 与 202 空响应语义，不重复发送任何步骤

#### Scenario: 超时、超限或重定向
- GIVEN 协议步骤超过单次/整体期限、响应超过 128 KiB 或返回 3xx
- WHEN 接收或等待响应
- THEN 取消当前读取并返回稳定失败类别，后续步骤跳过，不自动重试或跟随重定向发送凭据

#### Scenario: 服务关闭或重复点击
- GIVEN 配置未就绪、enabled=false 或已有检查运行
- WHEN 用户尝试启动检查
- THEN 不发送新一轮协议请求；运行中只允许取消，始终最多一轮检查

### Requirement: Truthful MCP Protocol Result And Recovery Semantics

前端 SHALL 分开保存元数据与逐步协议状态，核对 JSON-RPC 版本/id/error、目标协议版本、serverInfo、能力、JSON 内容类型和无 session 边界。initialize 后请求 SHALL 携带已验证的 MCP-Protocol-Version。配置启用、HTTP 200、部分步骤通过或历史配置 MUST NOT 独立构成连接成功；完整通过只证明当次浏览器路径的握手/ping/能力发现。结果 SHALL 带时间、会话边界与固定错误恢复说明，不回显原始头、异常正文或敏感转储。

#### Scenario: HTTP 成功但协议不符
- GIVEN HTTP 200 的响应为 HTML、JSON-RPC error、错误 id/版本/serverInfo、意外 session 或非本期 JSON transport
- WHEN 检查该响应
- THEN 相应步骤失败，之后步骤跳过，不用 HTTP 状态或静态目录冒充通过

#### Scenario: 部分通过与手动恢复
- GIVEN initialize 通过但 ping/发现失败、429、网络中断或 401
- WHEN 本次结束
- THEN 保留当次已通过步骤并显示未完成及恢复入口，不展示整体成功；401 不在协议流程内刷新重放

#### Scenario: 检查成功的适用范围
- GIVEN 固定步骤全部通过
- WHEN 显示结果
- THEN 只声明本浏览器路径本次协议检查通过，不声明知识库授权、工具执行、生成/citation/no-answer 质量或外部客户端互操作已验证

### Requirement: Preserved MCP Origin Exposure And Authentication Boundary

页面接入 MUST 保留现有 MCP 的 Origin 精确 allowlist、local-only、逐请求 JWT 与服务端身份边界。开发代理 SHALL 只映射固定本机目标并保留 Origin，不增加通用探测代理、不删除/伪造 Origin、不设置通配允许、不改保护配置或 MCP 默认值。MCP 与元数据接口均不得接收客户端选择的 tenant/user/provider 或认证 query/cookie。

#### Scenario: 浏览器 Origin 不在允许列表
- GIVEN MCP 启用但 Origin 不在显式允许列表，包括默认空列表
- WHEN 从页面发起协议检查
- THEN 既有 filter 拒绝，页面提示核对 Origin 与本机限制，不自动放宽配置，也不伪称能区分统一 403 的具体原因

#### Scenario: 代理路径缺失或本机限制不满足
- GIVEN `/mcp` 未正确代理或请求不满足既有访问边界
- WHEN 用户检查连接
- THEN 如实显示入口不可用/策略拒绝，保留默认保护；同源代理的存在不能被描述为真实客户端位于本机或远程部署已验证

### Requirement: MCP Console Session Isolation And Zero Business Side Effects

页面检查与展示 MUST 仅持有内存状态；取消、离开、退出或 sessionRevision/tokenRevision 改变时 SHALL 中止本次检查并以请求序号和身份版本丢弃迟到结果。任何配置读取、复制、目录查看和连接检查 MUST NOT 写问答历史、query count、业务任务、QA cache 或持久化结果，MUST NOT 调用 embedding/rerank/generation/judge/provider 或发出业务数据。

#### Scenario: 退出与迟到响应
- GIVEN 用户检查中退出、换号或 Token 版本改变
- WHEN 旧请求稍后返回
- THEN 中止旧流程并废弃结果，不展示给新会话，不恢复旧凭据或向后续步骤发送请求

#### Scenario: 只做接入检查
- GIVEN 用户使用本期全部页面操作
- WHEN 统计请求和服务调用
- THEN 本期 MCP 功能新增的请求只有元数据 GET 和明确触发的固定协议方法；MCP 资源实例读取、工具执行、业务写入、provider/model calls 与业务数据出站均为 0，AppShell 既有会话上下文与最近历史读取保持原行为
- AND 不因 search/ask 在发现结果内而执行它们，不把无业务数据出站写成没有任何网络请求

### Requirement: Evidence Scoped MCP Console Acceptance

本 change 验收 SHALL 同时提供白名单/开关矩阵、共享定义一致性、真实 Servlet/官方 SDK/生产访问过滤器的隔离协议链路、前端故障/身份清理与浏览器交互证据，以及相称 Maven/前端回归、含 vue-tsc build、SensitiveLogs 和 DocAndScope。无法完成的检查 MUST 明确 SKIPPED；合成前端或历史 C15 证据不得替代本期真实协议证据，真实服务和模型调用不得由规划或本地隔离授权推定。

#### Scenario: 合成页面通过但真实协议缺失
- GIVEN 浏览器夹具通过而 Servlet/SDK 或 Origin 路径没有证据
- WHEN 评估本 change 完成情况
- THEN 将协议链路标为未验证，不能据此宣布完整验收或第三方客户端可接入

#### Scenario: 没有真实环境或模型授权
- GIVEN 仅授权规划或本地隔离验证
- WHEN 执行/记录验收
- THEN 不探测实际运行服务、不改服务开关、不调用真实 provider，不补造远程/设备结果；未授权项保持 SKIPPED
