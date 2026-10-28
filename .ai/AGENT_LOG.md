# Agent Log（滚动热窗口）

> **维护规则**：本文件只保留近期热记录（控制在 **200 行以内**，历史记录只追加不改写），供新会话快速读取；具体要求见 [`AGENTS.md`](../AGENTS.md) 第 9 节。
> - **同次提交、零补录**：日志随当次改动在同一个 commit 中提交；不再写 `Commit: pending` 或追加手写 SHA 补录（Commit SHA 以 `git log` 为准）。
> - **简短记录**：每个可验证切片记一次改动原因、验证与跳过原因、新增风险；已有专项证据直接链接，可复用教训才提炼入 [`.ai/GOTCHAS.md`](GOTCHAS.md)，不为行数凑内容。
> - **历史冷归档**：Wave 0 ～ Wave 1 收口（2026-07-12 至 2026-10-21，共 3,804 行）已无损封存至 [`.ai/logs/2026-07-to-10-wave1.md`](logs/2026-07-to-10-wave1.md)。

> - 2026-10-21 的早期治理与验证记录原文迁入 [上下文治理冷归档](logs/2026-10-context-governance.md)。

> - 设置精修前的阶段记录原文迁入[阶段冷归档](logs/2026-10-settings-refinement-earlier.md)。

## 2026-10-27 · workspace-context-design（Type B：设计规划与 Skill 试用）

- **改动与大白话**：新增工作空间归属规划及两份独立示意文件，更新文档索引；此前前端难以看出组织归属和公开范围，现在用侧栏、个人资料与统一文案形成可审阅方案，正式接口和业务源码待独立 Type C 接入。
- **Skill 试用**：按用户指定仓库固定版本安装 landing-page-design、design-to-motion 到用户级 skills；采用前者的信息层级与完整状态方法，后者仅安装，详见[规划](../docs/01-规划与调研/工作空间归属与可见范围-设计规划.md)。
- **验证与门禁**：示意稿 JavaScript 语法、HTML ID/标签引用、无网络/存储引用及安装文件检查通过；DocAndScope PASS。未改正式业务代码，未跑 Maven、Python 业务测试或正式前端构建。
- **边界与风险**：浏览器桥不可用且备用通道拒绝 file 协议，未绕过；视觉/点击/窄屏均 SKIPPED，真实业务调用 0；示例不证明隔离效果，活动指针和 OpenSpec 保持。未暂存或提交，建议 `docs(前端): 规划工作空间归属与资料可见范围`。

## 2026-10-27 · workspace-context-visibility（Type C：正式接入）

- **改动与大白话**：新增服务端只读上下文并接入侧栏/资料，统一知识库可见范围；此前看不出组织且刷新丢资料，现在显示真实归属并区分加载/失败/长名，Token 轮换不清草稿，归属变化清会话重登（D1–D4）。
- **验证与门禁**：聚焦接口、完整 Maven/前端回归、含 vue-tsc 正式构建、DocAndScope 与 SensitiveLogs 均通过，结果和合成浏览器 13 项检查见[实施记录](../openspec/changes/workspace-context-visibility/implementation-review.md)。未改 Python 评测，未跑无关业务回归；OpenSpec CLI 不可用。
- **踩坑与风险**：单独注册 Mapper 的 SQL 测试未发现包路径漏出既有扫描范围，首次全仓失败后按原约定移包并完整重跑通过；窗口最小化使截图未取得，真实数据库/后端启动、provider、实体设备与逐像素外观未验，真实业务调用 0。旧热记录以原文块无损归档。
- **状态与提交**：按用户确认登记执行轨迹 USER_ACCEPTED，归档待处理；本 change 为 REVIEW_READY，未暂存、提交、push、部署或接受/归档，建议 `feat(工作空间): 展示账号归属并明确知识库可见范围`。

## 2026-10-27 · settings-modal-design（Type B：设置浮层规划）

- **改动与大白话**：新增两份规划/检查和三组 SVG/PNG，更新 docs 导航；现有设置只有主题与只读资料，现在把五类栏目、默认值、即时保存、存储失败和分步验收写成可审阅方案，尚未实现功能（D1–D5）。
- **Skill 与范围**：采用 better-layout 的分组/回流及 better-accessibility 的原生控件/焦点/减少动效规则；沿用浮层，保留默认同步、引用终态与工作空间上下文。活动任务仍待审，不建立第二活动 change。
- **验证与门禁**：三组静态图格式/尺寸/文字画布范围、实际查看、五组静态对比度、三份 Markdown 的 36 个相对链接与尾空白通过，DocAndScope PASS；结果见[规划检查](../docs/01-规划与调研/设置浮层扩展-规划检查.md)。
- **边界与提交**：初版缺字图标已改为向量并重新查看；静态稿不证明浏览器/IME/读屏/设备验收。仅设计工件，未跑 Maven/Python 业务测试或前端构建，真实业务调用 0；未暂存或提交，建议 `docs(前端): 规划五类设置浮层与本地偏好行为`。

## 2026-10-27 · settings-modal-preferences（Type C：五类设置正式接入）

- **改动与大白话**：接入 16 份前端生产/配置文件和 7 份测试/浏览器脚本；原来只能换主题，现在可以调整阅读、动效、发送键和来源初始展示，浏览器保存失败也能本次使用，重置只恢复自有偏好（D1–D5）。
- **验证与门禁**：最终前端 143/143、含 vue-tsc 正式构建、DocAndScope 与 SensitiveLogs PASS；26 组封闭浏览器、55 张截图及深浅实测对比度通过，六张审阅图/结果/指纹见[实施记录](../openspec/changes/settings-modal-preferences/implementation-review.md)。
- **修正与风险**：修复重复导入/旧 SSR 夹具，校正移动与失败状态的测试定位；新增控件边界 token，不借用浅色占位文字。主包大小警告保留；实体设备/读屏/真实页面缩放与业务未验，200% 文字回流不冒充页面缩放。
- **边界与提交**：5 次合成 ask、真实业务/出站 0，无后端或依赖变更，未跑无关 Maven/Python；按用户新实现请求切换活动项，前项工作空间保留待验收。本项 REVIEW_READY，未暂存、提交或接受/归档，建议 `feat(设置): 扩展五类浮层并接入本地阅读与问答偏好`。

## 2026-10-27 · settings-modal-preferences（Type B：下拉框视觉精修）

- **改动与大白话**：按用户截图反馈调整 4 份设置生产文件并适配 5 份检查脚本；原生菜单此前显示系统蓝条，现在复用既有 ElSelect，五处选择都有圆角、悬停底色、勾号与主题一致的菜单，保留原值/保存行为（D6）。
- **验证与门禁**：前端 143/143、含 vue-tsc 正式构建、34 项菜单专项和原 26 组设置回归通过，DocAndScope PASS；新增 36 张菜单截图，结果、边界与指纹见[精修记录](../openspec/changes/settings-modal-preferences/dropdown-review.md)。
- **修正与风险**：明确菜单挂在 dialog 内、按浮层空间换向限高，验证 Escape/Tab/缩窗/滚动；测试改点真实面板，不强点内部微小输入，区分同名 combobox/listbox。旧日志原文块无损归档；实体触屏/读屏/真实页面缩放仍未验证，既有包体警告保留。
- **边界与提交**：无新依赖/API/后端，真实业务/出站 0，跳过无关 Maven/Python；保留首版证据和所有既有改动，未暂存/提交/接受/归档，建议 `style(设置): 统一下拉菜单主题与选中交互`。

## 2026-10-27 · account-profile-editing（Type C：资料编辑事前提案）

- **改动与大白话**：新增 proposal/design/tasks/spec delta 四份提案并补导航；当前资料只读、旧页面仅演示，现按用户选择把头像、昵称和邮箱设计为有草稿/取消/真实保存的账号能力，登录名与组织保持只读（D1–D5）。
- **契约与范围**：规划自助身份接口、单事务/版本保存、受限图片与认证读取，头像存数据库且不进入认证查询；新接口和迁移尚未实现，现设置活动指针保留待审，未建立第二活动实现。
- **验证与门禁**：源码事实、四份提案结构/字段范围/相对链接及旧场景保留检查，DocAndScope PASS；仅文档，没有运行无关 Maven/Python/前端 build，真实业务/出站/实际数据库写入均为 0。
- **剩余与提交**：具体方案待用户确认，真实 MySQL 迁移/图片方向/设备与保存回读均未验证；邮箱不是已验证恢复凭据，未暂存、提交或接受/归档，建议 `docs(账户): 规划头像昵称邮箱编辑与保存契约`。

## 2026-10-27 · account-profile-editing（Type C：确认后实现）

- **改动与大白话**：用户“确认方案”后推进资料接口、身份上下文及设置浮层三个模块（D1–D5）；原来只能查看资料，现在头像、昵称、邮箱先预览，保存时用版本检查和事务一起写入，断线时保留草稿读回核对。原设置任务保留待验收。
- **验证与门禁**：完整 Maven 133 suites / 703 tests，0 失败、0 错误、22 跳过；最终聚焦 12 通过、1 MySQL 跳过；前端 146/146、含 vue-tsc build、资料浏览器 21 组、原设置 17 组和菜单 34 检查通过；DocAndScope 与 SensitiveLogs PASS（440 Java）。详见[实施记录](../openspec/changes/account-profile-editing/implementation-review.md)。
- **修正与边界**：裸 byte[] 的 MyBatis 返回值会误当集合，已使用 Blob 行对象并验证读回；修复保存/继续编辑后的焦点恢复，验证 JPEG 方向、SSE 持续及迟到头像清理。Docker 不可用，真实 MySQL 迁移与 Blob/事务未验；无实际账号/数据库写入、业务出站或 provider 调用，实体设备/跨浏览器未验。
- **状态与提交**：REVIEW_READY，未接受/归档；未暂存、提交或发布，建议 `feat(账户): 支持头像昵称邮箱编辑与事务保存`。本地证据位于忽略目录 `.tmp/account-profile-editing/`，不以合成刷新冒充真实跨设备持久化。

## 2026-10-28 · account-profile-editing（Type C：冲突草稿续编与收尾复验）

- **改动与大白话**：在既有结果核对区补齐“保留草稿，按最新版本继续编辑”；用户不必丢掉冲突草稿，也不会在核对后被自动重复提交。
- **验证与门禁**：资料浏览器 21 组、含 vue-tsc build、前端完整串行 146/146 通过；确认冲突前只有一次 PUT，用户手动保存后才出现第二次；DocAndScope/SensitiveLogs PASS。证据与一次既有轮询定时测试失败说明见[实施记录](../openspec/changes/account-profile-editing/implementation-review.md)。
- **边界与提交**：并行构建时的固定 100 ms 测试未等到终态，保留失败记录，未改轮询代码或放宽断言；MySQL/真实账号/设备缺口保持原记录。仍为 REVIEW_READY，未暂存、提交或发布。

## 2026-10-28 · account-profile-editing（Type B：头像保存失败与即时生效）

- **改动与大白话**：修复头像 mapper 参数、共享头像与账户浮层；原来生产 MyBatis-Plus 把 byte[] 当对象数组转换，导致整次保存 500，现用值对象绑定 BLOB。200 确认后头像与昵称立即更新，失败保留输入并简短提示重试，移除用户手动“草稿核对”流程（D6）。
- **复现与验证**：先在真实 Tomcat/生产配置/隔离 H2 复现 500，再验证修复；新增 HTTP 3 项通过，全仓 134 suites / 706 tests，0 失败/错误、22 跳过；前端 146/146、23 组 Chrome、含 vue-tsc build、SensitiveLogs/DocAndScope PASS。详见[保存修复记录](../openspec/changes/account-profile-editing/save-fix-review.md)。
- **陷阱与边界**：此前普通 MyBatis 与合成接口绕过生产参数处理器，不能证明真实保存；已补正确测试入口并记入 GOTCHAS。MySQL 容器仍不可用，未重启实际后端或修改实际账号；无业务出站。
- **提交状态**：保留原未提交改动，未暂存、提交或部署；建议 `fix(账户): 修复头像保存异常并让资料保存后立即生效`。

## 2026-10-28 · mcp-access-console（Type C：用户接受衔接与事前规划）

- **改动与大白话**：新增 MCP 四份 OpenSpec 文档与个人资料接受记录，同步旧状态、文档入口和唯一活动指针；原来 MCP 只能翻文档接入，现在先把独立页面、真实配置和分步协议检查写成可审阅方案，业务实现尚未开始（D1–D5）。
- **范围与授权**：按用户原话登记个人资料 USER_ACCEPTED，保留 MySQL/真实账号/设备缺口及待归档状态；新方案待确认，不执行工具或读取真实资源，不改默认关闭、Origin、本机限制、JWT 或 provider 契约。
- **验证与门禁**：8 requirements / 21 scenarios 的结构与场景覆盖、5 份新增 Markdown 的 16 个相对链接检查通过，DocAndScope PASS；活动指针 24 行、热日志 79 行。OpenSpec CLI 未在 PATH 发现，官方 CLI 校验 SKIPPED。纯文档，未跑无关 Maven/Python 业务测试或正式前端 build。
- **风险与提交**：协议检查拟限制为五请求/15 秒，默认 Origin 可能拒绝浏览器，真实链路与外部客户端尚未验证；本轮业务请求/数据库写入/provider/业务出站均为 0。未暂存、提交或发布，建议 `docs(MCP): 规划独立接入页与有界连接检查`。

## 2026-10-28 · mcp-access-console（Type C：接入页与协议检查实现）

- **改动与大白话**：用户确认后完成后端白名单配置接口、共用能力目录、独立接入页与协议适配器（D1–D5）；原来只能翻文档接入，现在可以看真实配置、复制占位模板并主动检查连接，配置开放和当次连接结果分开，失败不自动重试。
- **验证与门禁**：全仓 Maven 136 suites / 712 tests，0 失败/错误、22 跳过；前端 155/155、补强后 MCP 9/9、最终含 vue-tsc build、两主题十二尺寸及十一组浏览器交互通过，SensitiveLogs（448 Java）/DocAndScope PASS。证据与环境见[实施记录](../openspec/changes/mcp-access-console/implementation-review.md)。
- **修正与边界**：修正 ES2020 hasOwn 兼容、路由初稿布局和共享 fixture；真实 Tomcat/SDK/生产访问过滤器验证五步序列，业务依赖零调用。Ego 嵌套滚动/等待和额外截图问题如实记录；原生 200% 缩放、实体设备/读屏/跨浏览器未验。未改默认开关、Origin/local-only、JWT、provider 或依赖。
- **状态与提交**：REVIEW_READY，用户尚未接受；本轮实际服务检查、真实数据库写入/provider/业务出站均为 0，未跑无关 Python 业务测试或完整 C15 conformance，CLI 不可用。保留原文档改动，未暂存/提交/发布，建议 `feat(MCP): 增加接入页面与有界协议检查`。

## 2026-10-28 · mcp-access-console（Type C：客户端选择与专属接入 D6）

- **改动与大白话**：按用户新授权调整 3 份前端生产文件并加入 3 个本地 Logo 与许可；原来进页面先看技术诊断，现在先点接入、选择客户端，再复制专属配置或打开 Cursor 添加确认，原诊断收在高级区域，凭据仍只用本地环境变量。
- **验证与门禁**：前端完整 163/163、聚焦 17/17、最后一次含 vue-tsc 正式构建与 18 组浏览器通过，60 图已审阅代表图；DocAndScope PASS。截图/指纹、测试脚本和未验项见[客户端接入记录](../openspec/changes/mcp-access-console/client-onboarding-review.md)。后端未变，复用首版 Maven/协议证据，不跑无关 Maven/Python。
- **修正与边界**：暗色 Logo 的 scoped :global 初稿误让整页反色，审图后收窄完整选择器，并以 html filter=none/Logo 局部 filter 回归；移动夹具改等挂载而非隐藏侧栏可见。Ego 最小化无法截图后用现有隔离 Chrome；原生缩放/设备/读屏与实际客户端接入未验。
- **状态与提交**：REVIEW_READY，复制/打开只记待客户端验证，Cursor 安装意图在测试中被拦截；实际客户端安装、真实服务/model/provider/数据库写入/业务出站为 0，无新依赖或后端改动。保留先前改动，未暂存/提交/发布，建议 `feat(MCP): 增加客户端选择与专属接入引导`。
