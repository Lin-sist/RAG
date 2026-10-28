# 个人资料编辑实施记录

> 2026-10-28 用户反馈后的最新结论见[保存修复记录](save-fix-review.md)：已复现并修复生产 MyBatis-Plus 的头像参数异常，移除用户手动草稿核对流程，200 保存后头像与昵称立即更新。下文首版验证保留为历史证据，不作为修复前真实可用的声明。

日期：2026-10-27，收尾复验：2026-10-28。状态：`REVIEW_READY · ACCEPTANCE_PENDING`。用户已确认方案并授权实现与本地验证，尚未接受、提交、部署或归档；原设置任务仍待验收。

## 💡 30秒人话导读

原来的账户栏目只能看资料，现在可以更换或移除头像、修改显示昵称和邮箱。修改先留在草稿，点击保存后才一起写入账号；取消会恢复原资料。保存遇到断线时会保留草稿并读取服务器资料供核对，不会自行重复提交或把预览说成保存成功。登录用户名与工作空间仍然只读。

## 实现与范围

- 后端新增 [ProfileController](../../../rag-admin/src/main/java/com/enterprise/rag/admin/controller/ProfileController.java)、资料服务、严格载荷校验、图片规范化和独立 mapper，以及 [V14 迁移](../../../rag-admin/src/main/resources/db/migration/V14__account_profile.sql)。接口从当前认证身份确定目标，检查用户与组织有效状态；资料投影只读取头像是否存在，认证查询、登录/刷新 DTO 和 JWT 未添加头像。
- PUT 使用 JSON 与可选图片的 multipart，拒绝未知/重复 part、未知/重复字段与尾随 JSON。保留现有邮箱唯一索引；用 version 检查并在一个事务更新昵称、邮箱、头像。冲突返回 409，不泄露其他账号信息。
- JPG/PNG 原图上限 2 MiB，最长边 4096、像素总数 1600 万；服务端先读取尺寸再解码，重新生成有上限的 256×256 PNG。浏览器按图片方向居中裁剪，预览与提交使用同一份 PNG。头像通过认证请求读取，成功响应 no-store/nosniff，不将 Token 放到 URL 或将头像保存到 localStorage。
- 前端新增 [AccountProfilePanel](../../../rag-frontend/src/components/settings/AccountProfilePanel.vue)、头像组件、内存头像仓库与 API；修改现有浮层、侧栏、账号资料版本合并。保存不改变 sessionRevision，不重建聊天组件；旧版本/旧账号响应无法覆盖新资料。关闭、背景、Escape、换栏目及退出有草稿保护，保存期间防止重复操作。
- 字段错误、保存结果未知、服务器读回与旧后端只读降级分别展示。用户可以核对后明确结束草稿，也可以保留草稿，以最新版本继续编辑并手动再次保存；这不被标记为原写入成功。邮箱只作资料邮箱，不承担已验证、登录或找回密码功能。本地偏好重置保持资料不变。

## 验证证据

| 检查 | 当前结果与边界 |
| --- | --- |
| 全仓 Maven | `mvn -q test` 退出码 0；本次运行开始后更新的 133 个 Surefire XML 合计 703 tests，0 failures、0 errors、22 skipped。后续最终 JSON 校验与重复文件用例的聚焦复验也通过。 |
| 新增资料与上下文聚焦测试 | 13 tests，12 通过、1 MySQL 容器测试跳过。H2/Spring 事务验证冲突/回滚/头像读回；MockMvc 验证身份、白名单、缓存与二进制响应；图片测试验证格式、尺寸与解码前边界。 |
| 前端回归 | 在 rag-frontend 目录运行全量测试，最终使用 `node --test --test-concurrency=1 tests/*.test.mjs`，146/146。新增覆盖资料版本合并、不重放 PUT，以及退出后迟到头像解码不能回填且释放 Blob URL。 |
| 正式构建 | 仓库 FrontendBuild 门禁执行 `vue-tsc -b` 与 Vite build，通过；仅保留既有大包体警告。未安装或升级依赖。 |
| 个人资料浏览器 | [脚本](../../../rag-frontend/tests/profile-browser.mjs) 在本机 Chrome 中验证 21 组场景、22 张截图：深浅主题、1440×900、1024×768、390×844、320×640、844×390，真实文件 input、预览/取消/移除/保存、冲突/断线核对、401 不重放、离开确认、旧后端、资料刷新和偏好重置。EXIF=6 JPEG 通过红蓝方向像素断言；资料保存期间同一个 SSE 请求和聊天节点持续存在。 |
| 原设置与下拉回归 | 原设置脚本 17 组 / 50 图，菜单专项 34 检查 / 36 图，通过。合成请求被限制在本机预览，未连接实际业务服务。 |

详细运行输出及截图保存在仓库忽略目录 `.tmp/account-profile-editing/`：`maven.txt`、`focused.txt`、`frontend-tests.txt`、`build.txt`、`browser/evidence.json`、`settings-regression/browser-evidence.json` 和 `dropdown-regression/`。此目录只保存本地证据，提交时不携带合成账号的截图包。预览 `configFile:false`、无后端代理；浏览器路由仅为新增接口提供内存合成数据。浏览器 Skill 的原生桥在本会话不可用，因此沿用仓库现有的本机 Chrome/Playwright 脚本验证路径。

中间失败已修复后复跑：MyBatis 将裸 byte[] 返回值误当集合，现使用明确 Blob 行对象；继续编辑和保存后被禁用按钮丢失焦点，现恢复到合理输入框；SSR 测试补齐 profile API 隔离与浏览器 location。另一次前端测试从仓库根执行导致 Vite 根路径错误，已从正式前端目录重跑通过，未把该失败归因于业务代码。

2026-10-28 补齐冲突后保留草稿、按最新版本继续编辑的明确操作，并验证只有用户再次点击保存才产生第二次 PUT。恢复会话时本地预览进程已停止，重启隔离预览后 21 组浏览器检查通过。一次与构建并行的全量前端测试为 145/146，既有 task-polling 测试在固定 100 ms 内未等到终态；原断言及轮询代码未改，构建完成后完整串行复跑为 146/146。该失败保存在 `frontend-parallel-timing-failure.txt`，仍需留意重负载下的定时测试稳定性。

## UI 检查

本轮参考 better-ui Skill，查看了深浅主题截图，并检查普通、悬停、焦点、加载、错误、空头像与禁用状态的代码与浏览器交互。

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| HIGH（已修复） | SettingsModal / AccountProfilePanel | 关闭确认或保存后，焦点可能落到页面 body | 继续编辑回到昵称框，邮箱冲突回到邮箱框，保存后保留浮层内焦点 | 确保键盘用户继续操作与 Escape 关闭 |
| LOW（已修复） | AccountProfilePanel | 窄屏按钮沿用桌面 36 px 高度 | 窄屏最小 44 px，头像有低强度轮廓 | 改善触控命中与头像边界 |

Approve：仅针对上述已检查的桌面浏览器范围。实体触屏、读屏器、系统级缩放和跨浏览器不在此通过结论内。

## 未验证与后续边界

- Docker 不可用，`ProfileMigrationMySqlTest` 明确跳过；V13→V14 的真实 MySQL 迁移、MySQL 事务/唯一索引/Blob 行为仍待隔离环境验证。H2 和内存 HTTP 证据不能替代它。
- 没有重启实际服务、执行实际数据库迁移或修改真实账号；因此现有运行中的旧后端不会自动获得编辑能力。部署到目标环境前需要备份并应用迁移，按批准环境联调。
- 没有真实 provider/ask/embedding/rerank/judge、外部图床、出站业务数据或新依赖。浏览器问答均为合成预览，不证明生成质量。
- 未验证真实手机、读屏、跨浏览器以及实际账号重新登录/跨设备同步；当前刷新证据来自合成服务，账号持久化证据来自隔离 H2。
- OpenSpec CLI 未安装，未运行其校验；已检查提案、设计、任务与规格文件结构及相对链接。
- 尚未接受或归档，不同步长期 baseline。Agent 未暂存、提交、push 或发布。

## 收尾门禁

DocAndScope PASS，差异格式和受保护路径正常，相对链接检查通过；SensitiveLogs PASS（440 份 Java 源文件）。它们只证明对应静态门禁，不替代上述运行或真实环境证据。日志和任务勾选后再次运行 DocAndScope。

建议提交信息：`feat(账户): 支持头像昵称邮箱编辑与事务保存`。
