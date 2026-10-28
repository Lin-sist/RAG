# MCP 接入页设计

## 💡 30秒人话设计主旨

用户希望点击接入按钮，选择自己常用的 AI 助手，让它使用知识库。页面首版把技术诊断放得太靠前，现在改为“接入 AI 助手 → Logo 选择 → 专属配置 → 在客户端验证”的主流程；原有协议诊断收进高级区域。页面会如实区分配置已准备、浏览器协议检查和外部客户端接入，不靠复制成功制造已连接状态。

状态：`APPROVED · REVIEW_READY`，2026-10-28。D6 为用户最新明确授权的界面修订，优先于第 1 节首版布局及 D4 的模板限制；协议与后端契约保持。D6 结果见[客户端接入记录](client-onboarding-review.md)，首版证据见[实施记录](implementation-review.md)。范围与授权见 [proposal](proposal.md)，验收契约见 [spec delta](specs/rag-system/spec.md)。

## 1. 页面结构与真实使用路径

选择用户已在示意中选中的「独立接入页」，路径 `/integrations/mcp`，挂在现有 AppShell；侧栏「历史记录」之后增加带 Plug 图标的「MCP 接入」。不替换设置浮层和个人资料，不重置问答 store、草稿或偏好。

```text
现有侧栏                    MCP 接入
新问答                      一句话说明 / 配置读取状态
知识库                      接入配置 | 工具与资源 | 连接检查
历史记录                    当前部分的内容
MCP 接入                    来源、检查时间与结果适用范围
```

### 接入配置

- 页面进入时仅读取一次元数据；失败显示重试，旧后端 404 显示“当前后端尚未提供接入信息”，不伪造开关值。
- 展示 MCP 配置是否启用、外部调用工具配置是否开放、本机限制与 JWT 认证说明。配置启用时文案是“已配置启用，尚未检查连接”，不能直接写“服务在线”。
- 分开显示实际协议路径 `/mcp`、当前站点检查路径，以及明确标注的本机开发地址例子 `http://127.0.0.1:8080/mcp`。不从 Host、Forwarded、X-Forwarded-* 拼出可信对外地址，也不保证浏览器 origin 就是外部客户端可达的后端地址。
- 通用模板只包含 `url` 与 `Authorization: Bearer <access-token>`。它是连接参数示意，不承诺匹配每一种客户端的配置文件格式。复制按钮旁保留“复制的是占位模板”；复制失败时展开可选中的文本供手动复制，成功反馈用 aria-live，不要求读取剪贴板。
- 实现首版不提供真实 Token 查看/导出、任意 endpoint 输入、客户端安装按钮或在线启停开关。用户在支持 HTTP 与自定义请求头的客户端中安全配置凭据；不要求把凭据粘回项目页面。

### 工具与资源

- 工具说明来自后端纯定义目录，保留 4 个工具的 input/output schema 与只读属性；默认先看中文用途和是否可能触发外部服务，参数、JSON Schema 与示例按需展开。
- `rag.search` / `rag.ask` 标明可能触发 embedding、rerank 或生成；`rag.get-citation` 是原文引用读取；`rag.compare-sources` 标明不做语义比较。所有示例 ID 为格式示例，不暗示当前账号拥有该资源。
- 页面始终区分实现目录、当前配置允许的名称集合、本次协议发现集合。关闭时可以查看静态说明，但实际开放集合必须为空；静态说明不是 `tools/list` 的协议结果。
- Resources 只展示三类 URI 模板与用途，不发 `resources/list` 或 `resources/read`，不列真实知识库数量、名称、文档或内容。没有“试运行工具”按钮。

### 连接检查

- 配置未读取、读取失败或 `enabled=false` 时不发送协议请求；服务关闭时说明需要服务端显式启用。
- 用户点击后依次展示“握手”“ping”“工具发现”“资源模板发现”，以及整体结果。默认、切页、切主题、展开说明与刷新元数据都不触发自动检查。
- 每次结果带完成时间和当前会话限定说明；它只证明本浏览器路径当次完成的步骤。完整通过也不证明知识库权限、工具执行、生成质量或外部客户端互操作。

## 2. 新只读元数据契约

新增 `GET /api/integrations/mcp`，使用现有 `ApiResponse`，返回 `Cache-Control: no-store`。接口必须使用当前认证 principal，经 `CurrentUserService.requireIdentity` 校验；不接收 tenant/user/owner/endpoint/provider 参数，无匿名能力，也不放宽 SecurityConfig。

接口即使 `rag.mcp.enabled=false` 也能返回配置说明，因此属性绑定与纯描述目录必须可在 transport 未注册时独立使用；不能为了读配置而实例化 SDK Server、Tool handler、QueryEngine 或 provider。实施时只做所需的局部定义提取，不跨模块重构现有业务服务。

数据字段固定如下，字段外的数据不透传：

| 字段 | 类型与含义 |
| --- | --- |
| `enabled` | boolean；当前进程绑定的 MCP 开关，属于配置事实，不是连通证据。 |
| `externalToolsConfigured` | boolean；当前进程外部调用工具开关的原值。 |
| `externalToolsEffective` | boolean；仅 `enabled && externalToolsConfigured` 为真。 |
| `localOnly` | boolean；当前 MCP 本机访问限制。 |
| `endpointPath` | 固定 `/mcp`；不得来自请求载荷或转发头。 |
| `transport` / `sessionMode` | 固定 `streamable-http` / `stateless`。 |
| `authMode` / `oauthSupported` | 固定 `project-access-jwt` / false，不能冒充 MCP OAuth。 |
| `targetProtocolVersion` | C15 已接受的目标版本 `2025-11-25`；不等于本次已协商的版本或 SDK 所有支持版本列表。 |
| `server` | `{name, version}`，复用注册时的 serverInfo 定义。 |
| `originPolicy` | 固定 `exact-allowlist`，只解释现有策略，不返回完整部署允许列表或自动新增 Origin。 |
| `implementedTools` | 最多 4 项纯定义，字段为 `name/title/description/inputSchema/outputSchema/readOnly/externalCallPossible`。 |
| `configuredToolNames` | 关闭时为空；启用且外调关闭为 get-citation/compare-sources；都启用为 4 个名称。 |
| `implementedResourceTemplates` | 最多 3 项 `name/title/description/uriTemplate/mimeType`，复用资源注册定义，不包含资源实例。 |
| `configuredResourceTemplateNames` | 关闭时为空，启用时为三个模板名称。 |

所有固定名称、schema 与模板从同一纯定义生成，注册逻辑与元数据共享这些定义；契约测试覆盖描述目录和实际 `tools/list` / `resources/templates/list`，防止维护两份名单。可调用的 handler 与身份校验仍由原 C15 边界负责。

不返回 Token、用户/租户 ID、数据库/向量库信息、provider 凭据、原始配置对象或完整允许来源列表；不探测网络、不执行资源/工具、不写查询次数/历史/任务、也不启用 QA cache。元数据响应上限为 128 KiB，超出时返回稳定的脱敏服务错误，不能截断 JSON 后当成功。

## 3. 浏览器协议检查的数据流与预算

```text
已登录页面 → GET /api/integrations/mcp → 展示配置与纯定义
用户明确点击 → 同源 POST /mcp → 既有 Origin/本机/认证过滤 → 官方 SDK
                                            ↓
                           只返回握手 / ping / 能力定义
```

不新增后端“代用户测试连接”接口。前端使用窄用途 MCP 协议适配器读取已有 auth store 中的当前 access token，仅发送给固定同源 `/mcp`；普通 REST 继续复用现有 request client。协议适配器不成为通用 URL 请求器，也不引入新的客户端 SDK 依赖或改写 MCP Server。

一次检查的上限是 5 个 HTTP POST，前一步失败就停止，不批量发送、不重试、不分页追取：

| 顺序 | JSON-RPC 方法 | 必须验证的结果 |
| --- | --- | --- |
| 1 | `initialize` | `jsonrpc=2.0`、匹配的请求 id、无 error、目标 protocolVersion、预期 serverInfo，以及只声明本期 Resources/Tools 的 capability 边界。 |
| 2 | `notifications/initialized` | 不带 id 的标准通知；接受 202 空响应，不当作一次 JSON-RPC result。若现有固定 SDK 对该通知行为不同，先记录不兼容，不能悄悄省略或改 Server。 |
| 3 | `ping` | 匹配请求 id、无 error 的标准成功结果。 |
| 4 | `tools/list` | 只包含固定目录允许的名称与有效定义；按当前配置比对 2/4 个工具，禁用外调时不得发现 search/ask。 |
| 5 | `resources/templates/list` | 返回三类 canonical 模板，不列资源实例。 |

请求使用 `Content-Type: application/json`、`Accept: application/json, text/event-stream` 和 Bearer header；浏览器协议适配器使用 `credentials: omit`、`cache: no-store` 和 `redirect: error`，不把 Cookie、缓存或重定向作为认证/恢复通道。initialize 后的请求带已验证的 `MCP-Protocol-Version`。首版只接受现有 C15 JSON POST 响应，不启动 GET/SSE server stream，不保存 MCP-Session-Id；意外返回 session、其他版本或 text/event-stream 时停止并提示不兼容，不能以 HTTP 200 直接判成功。

每个请求最多等待 5 秒，整次检查共用 15 秒截止时间，后续步骤仅用剩余时间；每个 JSON 响应在读取时限制为 128 KiB，超过即取消并返回 `RESPONSE_TOO_LARGE`。拒绝重定向，凭据不随 3xx 发往其他地址。若列表带非空 nextCursor，本期停止为 `DISCOVERY_INCOMPLETE`，不自动翻页或宣称完整通过。所有返回文本按文本渲染，不执行 schema/description 内的 HTML、URL 或脚本。

同一页面同时最多 1 次检查；按钮运行时变为可取消状态。取消、路由卸载、退出、sessionRevision 或 tokenRevision 改变时中止请求并废弃旧结果。响应还必须匹配本次 sequence 与身份版本，不能只依赖 AbortController。检查不刷新 Token、不使用自动认证重放；401 提示登录状态失效，用户完成现有登录流程后可手动重新检查。REST 元数据 GET 的已有刷新逻辑不变。

不发送 `tools/call`、`resources/list/read`、QA REST、embedding/rerank/generation 请求；实际 provider/model calls、业务数据出站、业务写入均为 0。固定协议元数据和认证 header 仍会到达当前站点服务端，不能把“无业务数据出站”写成完全没有网络请求。页面不轮询，也不在失败后自动恢复检查。

上述预算统计 MCP 功能新增的请求；AppShell 原有的会话上下文和侧栏最近历史读取保持既有行为，不能将它们算成新的 MCP 资源读取，也不能声称整个应用只发 MCP 请求。

## 4. Origin、代理与地址语义

现有 MCP 在 Origin 存在时执行精确 allowlist 检查，默认空允许列表会拒绝浏览器请求，即使浏览器与站点同源也不能绕过。`local-only` 检查的是 Servlet peer，不代表代理后真实客户端一定在本机。页面保留这些限制，不能为获得成功状态关闭它们。

实现时仅在 Vite 增加固定 `/mcp` → 本机后端的开发代理；保留浏览器 Origin，不删除、不伪造为允许值，也不设置 `*`。部署环境需自行把同源 `/mcp` 正确路由到服务，并显式配置允许的页面 Origin；不自动修改 nginx、生产 CORS、`.env.local` 或 `application-dev.yml`。

浏览器检查只有在允许 Origin 与既有本机边界都满足时才可能通过。默认配置下正确显示关闭/拒绝本身也是合格状态；验收不得要求把默认值改成开放。隔离测试可用测试属性配置精确本机 Origin；真实环境启用、重启或调允许项需要独立的环境授权。

## 5. 状态、错误与恢复

元数据和检查拥有独立状态，彼此不覆盖。元数据为 `loading / ready / unavailable`；步骤为 `idle / running / passed / failed / skipped`，整体另支持 `cancelled`。失败后未运行的步骤标为 skipped，保留本次已完成步骤，不能把部分通过写成完整连接成功。

| 条件 | 用户看到什么、怎样恢复 |
| --- | --- |
| metadata enabled=false | “服务端未启用 MCP”；检查按钮不可执行，提供现有使用说明。 |
| 元数据接口 404 | “当前后端尚未提供接入信息”；可以重试或查看静态帮助，不猜服务开关。 |
| `/mcp` 返回 404、HTML 或错误路径 | “协议入口不可用或代理未配置”；重新读取配置并核对路径，不强行推断关闭。 |
| 401 | “登录状态失效，请重新登录后检查”；不在协议步骤中刷新/重放。 |
| 403 / MCP_FORBIDDEN | “请求被访问策略拒绝，请核对 Origin 和本机限制”；当前统一错误不能准确区分两者，不编造具体原因。 |
| 405 / 406 / 415 | 分别说明方法/Accept/内容类型不兼容；检查客户端与代理，不把 GET 405 当 Server 宕机。 |
| 429 / 超时 / 网络中断 / 超限 | 给出固定类别和手动重试入口；结束本次，无自动重试。 |
| JSON-RPC error、版本/能力不符、列表不完整 | 标记相应步骤失败或发现未完成；保留已通过项，不显示整体成功。 |
| 取消或身份变化 | 主动取消显示取消；退出/换号清空结果并丢弃迟到内容。 |

配置可能在进程重启后变化。本次协议结果保留其时间与配置快照；如果实际目录与快照不符，提示刷新配置后重检，不能用静态目录覆盖真实差异。只展示受限的结果摘要、名称与固定错误类别，不展示原始请求头、异常正文、全量响应或敏感调试转储；不写 localStorage、历史或普通日志。

## 6. 布局、无障碍与已有功能兼容

本轮设计沿用已读取的 better-layout / better-ui 原则：优先用间距分组，让说明、配置和操作各有清晰位置，schema/模板采用有明确入口的渐进展开。使用现有 `--rag-*` token、Lucide 和主题 store，不引入设计框架、字体 CDN 或新依赖；普通文本不使用低对比度占位文字色。

桌面主内容最大宽度约 1040px；正文随容器回流，工具列表先两列、内容不足时一列。窄屏复用现有侧栏抽屉，栏目按钮允许换行，JSON/pre 允许折行；320px 或 200% 页面缩放下关键操作不被裁掉。长 schema 可局部滚动并有完整查看入口，不能只显示省略内容。

栏目切换使用具备选中语义与键盘行为的原生按钮组或合规 tabs；所有复制/检查/取消均有文本名称、可见焦点、合理触控区域。动态状态通过 aria-live 反馈，不只用颜色；无需 staged 入场动效，已有低强度反馈遵守 reduced-motion 与本地偏好。

页面状态为内存局部状态，不修改个人资料、偏好和问答记录；不清理或重建共享 auth/chat store。正式页面路由生命周期沿用 AppShell；离开原聊天页的既有行为不在本 change 重构。元数据不读取真实业务资源；能力定义也不构成知识库访问授权。

## 7. 实施位置、验证与回滚

后端预期新增 `McpIntegrationController`、白名单 DTO 和只读投影，局部整理 `McpProperties` 绑定及 MCP 纯定义；前端预期新增 MCP view、类型、REST API 与窄用途协议适配器，修改 router、现有正式 AppSidebar 和 Vite 代理。实际文件名可随项目约定微调，接口与失败契约不得静默改变。

验证必须覆盖关闭/开启及外调开关组合、未登录/非法 principal、元数据无敏感字段、共享定义与实际 registry 一致、无业务读取/写入/provider 调用；真实 Servlet + 官方 SDK + 生产访问过滤器验证浏览器方法序列、Origin 拒绝、通知、JSON/版本/id、无 session 和目录发现。协议检查的前端夹具必须覆盖超时、取消、超限、重定向、HTML/JSON-RPC error、部分通过、配置漂移和认证版本变化。

浏览器检查覆盖深浅主题、桌面/平板/320px/横屏、实际点击与键盘、复制成功/失败及局部披露。相称 Maven 与前端回归、含 vue-tsc 的正式 build、DocAndScope/SensitiveLogs 均需记录；缺失项明确 SKIPPED。旧 C15 conformance 证据仅沿用历史适用范围，不因为新页面通过就声称整套重验或远程客户端已验证。完整任务见 [tasks](tasks.md)。

回滚以本 change 文件差异为边界：移除导航/路由、新元数据接口、协议适配器与开发代理，再恢复本轮局部定义提取；没有迁移、业务写入或新依赖。保留原 MCP transport、工具、默认值和认证契约，不整体 reset/restore 用户改动。若实现需要放宽旧边界或扩展协议，先停止受影响切片并更新事前方案。

## 决策记录

### D6：先选择客户端，再显示专属接入；技术诊断放在次级入口

- **当时面临哪几个选择？** 可以沿用技术页，另做站内 Agent；也可以先满足用户更明确的意图，让已有 Codex、Claude Code、Cursor 使用我们的知识库。用户选择后者并授权实现，站内体验保留为后续候选。
- **最终选了谁、理由是什么？** 选择在原页面增加主接入按钮及原生可访问弹层，按 Logo/名称选择客户端后只展示对应步骤。Codex 给 HTTP 接入命令与 TOML；Claude Code 主推 `.mcp.json` 配置，避免跨 Shell JSON 命令转义不稳定；Cursor 给官方 deeplink 与 JSON 备选。命令、配置和链接只引用 `RAG_MCP_ACCESS_TOKEN`，有效的项目访问凭据由用户在客户端本地配置，页面不读写或导出它。官方依据：[Codex](https://learn.chatgpt.com/docs/extend/mcp?surface=cli)、[Claude Code](https://code.claude.com/docs/en/mcp)、[Cursor 安装链接](https://cursor.com/docs/mcp/install-links)与[Cursor HTTP 配置](https://cursor.com/docs/mcp)。
- **其他方案的代价是什么？** 站内 Agent 需要新增执行链和模型预算；本机自动检测/改配置需要桌面组件或扩展授权，普通网页不承诺这些能力。JWT 更新仍需用户在客户端处理，要做长期自动授权需独立后端方案，不能暗中扩大本次前端范围。

主页面先说明知识库用途，突出唯一的“接入 AI 助手”操作；原配置/工具/协议检查放在默认收起的“服务状态与高级诊断”。弹层有关闭、Escape、背景点击、焦点圈定与恢复、背景 inert、深浅主题和 320px 回流；三客户端用本地品牌 SVG 加文字，说明来源和许可，不加载远程图标或新增依赖。未选择时只显示三个客户端；选中后展示地址、凭据步骤、配置操作和客户端验证指引。

新增地址输入只用于配置生成，默认明确标注为本机开发示例。接受合法 http/https URL，拒绝 URL 用户名/密码、query、fragment、控制字符和 Shell 特殊字符；生成命令时地址始终引用。它不能传入协议适配器，也不能触发地址探测。地址改变或切换客户端后清空旧复制/打开状态；关闭、卸载或身份版本变化时销毁本次弹层输入与状态。不保存到 localStorage。

Cursor 安装链接使用固定 `cursor://anysphere.cursor-deeplink/mcp/install`，name 为 `enterprise-rag`，config 为只含 URL 与 `Authorization: Bearer ${env:RAG_MCP_ACCESS_TOKEN}` 的 UTF-8 Base64 JSON。只有用户明确点击原生链接时才交给客户端确认，页面不自动跳转、安装或宣称成功。测试拦截该动作并解码核对，禁止真的触发系统安装提示或修改真实配置。

本期没有客户端回执能力，所有客户端只可达到“配置已准备，待客户端验证”，不出现“已连接/已验证”的开关或伪造已安装标识。引导用户在客户端确认 `enterprise-rag` 工具可见；检索/问答示例明确需服务端开放外调并可能产生调用费用，点击复制不执行任务。现有浏览器五步检查也不能充当客户端接入结果。回滚仅撤去入口/弹层/静态资源与配置生成器，高级诊断恢复为首版，后端与已验协议不变。

### D1：使用独立接入页，为说明与检查留出清楚的位置

- **当时面临哪几个选择？** 可以把地址和模板放进设置浮层，也可以增加独立接入页，容纳能力说明与分步检查。
- **最终选了谁、理由是什么？** 选择用户在示意中选中的独立页。工具 schema 与检查失败原因都需要阅读空间，独立页能沿用现有布局，并让演示和返回入口更清楚。
- **其他方案的代价是什么？** 设置内轻接入适合只提供说明；同时加入工具目录和检查会让已有设置浮层过密。独立页需要增加一个导航项，但不必改动已接受的个人资料交互。

### D2：配置说明与协议检查分开，避免把说明当成运行事实

- **当时面临哪几个选择？** 可以硬编码“已支持 MCP”，可以让后端代发探测，也可以先读白名单元数据，再由浏览器访问真实 `/mcp`。
- **最终选了谁、理由是什么？** 选择第三种。配置接口可以解释默认关闭的状态；浏览器检查实际走 Origin、认证和代理路径，才知道当前页面是否接得上。
- **其他方案的代价是什么？** 硬编码无法反映开关；后端探测可能绕过浏览器路径限制，还会新增凭据转发或任意地址探测面。本方案需要一个小型受限协议适配器，接受默认 Origin 拒绝时如实失败。

### D3：首版只发现能力，不执行工具或浏览真实资源

- **当时面临哪几个选择？** 可以一次加入工具调试与资源树，也可以先把握手和能力发现做完整，再另立工具执行范围。
- **最终选了谁、理由是什么？** 选择后者。五个固定请求已足以解释接入链路，并且不会触发模型或读取知识库；避免把连接验证和生成质量混在一起。
- **其他方案的代价是什么？** 工具调试需要数据权限选择、结果呈现、外调预算与失败恢复，明显扩大首版。代价是本页暂时不能直接展示真实问答，应继续用现有聊天页演示该能力。

### D4：只复制占位模板，检查只发往固定同源路径

- **当时面临哪几个选择？** 可以提供任意服务器 URL 与 Token 输入，也可以自动导出登录 Token，还可以只给占位模板并复用当前会话检查固定路径。
- **最终选了谁、理由是什么？** 选择第三种，避免把凭据带到未知地址或长期留在剪贴板/文件。拒绝重定向和认证重放，身份变化就终止检查。
- **其他方案的代价是什么？** 任意 URL 会引入新的连接器与跨域安全范围；自动导出更方便，但容易泄露当前凭据。占位模板要求用户在外部客户端自行安全配置 Token，且不承诺第三方客户端都支持。

### D5：只复用纯定义，不为页面重写或放宽 MCP Server

沿用 C15 固定工具、模板、官方 SDK、只读边界与开关默认值；只提取必要的纯定义，让展示与注册同源生成，避免以后页面写着四个工具、实际却注册另一套。若必须改认证、Origin、协议或依赖，已经超出本 change，需独立评估。
