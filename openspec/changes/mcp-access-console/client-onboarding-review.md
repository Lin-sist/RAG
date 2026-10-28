# MCP 客户端选择与接入引导记录

日期：2026-10-28。状态：`REVIEW_READY · ACCEPTANCE_PENDING`。用户在明确选择“接入按钮 → Codex/Claude Code/Cursor Logo → 专属接入”后授权设计与实现；本轮属于同一 change 的 D6。没有实际写入客户端配置、打开安装提示、启用服务、探测真实后端、数据库写入或模型调用。

## 💡 30秒人话导读

首版页面把开发者的连接诊断放在最前面，用户进来不知道如何让自己的助手使用知识库。现在先展示“接入 AI 助手”，点击后选择客户端 Logo，再按对应步骤添加配置并到客户端确认工具。原来的服务配置与五步检查保留在默认折叠的高级诊断中，已有后端和协议逻辑继续复用。

## 当前行为与范围

- [McpAccessView](../../../rag-frontend/src/views/integrations/McpAccessView.vue)增加主接入入口与三步说明，旧三栏目收进“服务状态与高级诊断”。路由与现有侧栏名称不变。
- 新增 [McpConnectDialog](../../../rag-frontend/src/components/mcp/McpConnectDialog.vue)：打开先显示三种本地 Logo 和名称；选中后才出现该客户端的地址、凭据、添加方式和验证指引。支持 Escape、背景关闭、Tab 圈定、初始焦点、关闭恢复、背景 inert、滚动隔离与身份变化关闭。
- 新增[配置生成器](../../../rag-frontend/src/utils/mcpOnboarding.ts)：Codex 提供 HTTP 命令/TOML，Claude Code 提供项目 `.mcp.json`，Cursor 提供官方 `cursor://anysphere.cursor-deeplink/mcp/install` 与 JSON 备选。配置内只有 `RAG_MCP_ACCESS_TOKEN` 的客户端环境变量引用，页面不读出真实 Token；JWT 有效期与客户端进程环境由用户在本机处理，不冒充新授权体系。
- 地址输入只用于生成配置：限制 http/https、受限主机与以 `/mcp` 结尾的路径，拒绝用户名/密码、query/fragment、控制字符与 Shell 注入字符。地址不会进入协议检查器或触发探测。切换客户端、修改地址、关闭或身份变化时清理旧反馈，迟到的复制结果不能覆盖新选项。
- 复制仅反馈“配置已准备，待客户端添加与验证”；Cursor 明确点击后只提示已发起打开、仍待验证。没有客户端回执能力，不显示已安装、已连接或已验证。首个示例是发现工具，检索示例说明外调开关与潜在费用，不自动执行。
- 三个 Logo 来自 [LobeHub Icons](https://github.com/lobehub/lobe-icons) 的现有 SVG，保存在[本地资产目录](../../../rag-frontend/src/assets/mcp-clients/README.md)，包含原 MIT 许可；未新增 npm 依赖、远程 CDN 或自行生成品牌图案。Cursor 单色 Logo 仅在暗色主题局部反色。

## 验证与证据

| 检查 | 结果与边界 |
| --- | --- |
| 前端完整回归 | `node --test --test-concurrency=1 tests/*.test.mjs`，163/163；新增[客户端配置测试](../../../rag-frontend/tests/mcp-onboarding.test.mjs) 8 项，覆盖恶意地址拒绝、三种格式、Cursor 链接解码/凭据引用、SSR 初始语义及 SVG/许可。原 MCP 专项仍为 9 项，合计聚焦 17/17。 |
| 正式构建 | 最终 `FrontendBuild` 的 vue-tsc + Vite 通过；本轮 MCP 页面资源仍懒加载，主包既有大于 500 kB 警告保留。最后一处暗色 CSS 修正后重跑构建与完整浏览器矩阵。 |
| 隔离 Chrome | 新增[浏览器脚本](../../../rag-frontend/tests/mcp-onboarding-browser.mjs)，18 组通过：两主题 × 六个视口的三客户端布局/截图，键盘与圈定、三格式复制与拦截 Cursor、失败恢复/非法地址/重开清理、迟到复制与 Token/退出清理、关闭/旧后端降级、原诊断精确五请求。 |
| 视口与视觉 | 1440×900、1024×768、768×1024、390×844、320×640、844×390；保存 60 张全尺寸截图。检查对话框边界、控件水平可达、滚动后主动作可见、图片成功加载、实际主题 class/表面色、html filter=none 与 Cursor 局部 filter；已实际查看主页面、Logo 选择和窄/横屏截图。 |
| 副作用隔离 | 浏览器非本地出站 0、页面异常 0、实际客户端安装 0、真实 provider/model/业务写入 0。Cursor click 在 DOM 默认动作前被测试捕获并阻止，但保留应用处理器，既验证 payload/反馈也避免真正唤起客户端；没有点击真实系统安装确认。 |
| 本地门禁 | DocAndScope 通过；补录日志与任务后复跑。没有 Java/Python 业务代码、依赖或配置改变，沿用[首版后端验证](implementation-review.md)，不重复 Maven/Python 或真实 C15 conformance。 |

独立本机 [mcp-preview](../../../rag-frontend/tests/mcp-preview.mjs)继续提供合成 API/协议，未连真实后端。浏览器结果和 60 张图保存在本聊天可视化目录 `mcp-onboarding-evidence/`；摘要、源文件与截图指纹另保存在仓库忽略目录 `.tmp/mcp-access-console/client-onboarding-evidence.json`。这只证明页面引导和载荷准备，不证明三个实际客户端的认证/工具调用已经成功。

## 发现、修正与未验证项

- 审图发现早期局部暗色规则 `:global(html.dark) .cursor-logo` 被当前 Vue scoped 编译器替换为整个 `html.dark` 选择器，造成整页反色；仅断言背景色无法发现祖先 filter。已把完整限定选择器放进 :global，编译结果及最终浏览器断言确认 html filter=none，只有 Cursor Logo 反色；最终暗色图已重新查看。该陷阱已记入 GOTCHAS。
- 浏览器夹具最初等待移动侧栏中的工作空间名“可见”，在窄屏正常隐藏时超时。修正为等待节点挂载与元数据完成，不修改产品侧栏绕过测试。期间还修正了模板编辑初稿的高级区域闭合位置，并验证旧协议入口完整保留。
- 按默认浏览器 Skill 先用 Ego 验证了主按钮、三 Logo 初始焦点和 Codex 选择，因窗口最小化无法截图，随后使用项目已有 Playwright/独立 headless Chrome 验证路径，未安装新工具或使用真实浏览器资料。原生 200% 页面缩放、实体触控、读屏与跨浏览器仍未验证。
- 不扫描本机软件、不升级客户端、不自动写配置；实际 Codex/Claude Code/Cursor 接入与 JWT 更新、目标部署的可达地址/Origin/local-only 均未验证。站内 Agent 体验、长期 MCP 授权/独立凭据继续留待独立范围，前者仅保留为候选。
- OpenSpec CLI 仍未在 PATH 发现；本地检查结构、场景与相对链接，不冒充官方 CLI 校验。未触碰 `.agents/`、本地敏感配置或个人学习资料。

## 状态与提交

D6 实现与本地验证完成，等待用户审阅；不接受或归档整个 change，不改长期 baseline。保留此前所有未提交改动，未暂存、提交、push 或部署。

建议提交信息：`feat(MCP): 增加客户端选择与专属接入引导`。
