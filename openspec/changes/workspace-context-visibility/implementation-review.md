# 工作空间归属正式接入记录（2026-10-27）

## 💡 30秒人话导读

此前前端看不到组织归属，刷新还会丢失用户资料。现在后端有只读上下文接口，前端会读取真实账号与组织名称，显示在侧栏及个人资料，并明确知识库的组织内公开范围。加载、读取失败、长名称和空资料有各自状态；旧账号的迟到响应不会回填新会话。

本记录证明源码接入、本地 SQL/组件测试及封闭浏览器交互，不证明真实数据库或 provider 联调。用户已授权实现，尚未接受本 change；不自动提交、归档或部署。

## 实现范围

- [Controller](../../../rag-admin/src/main/java/com/enterprise/rag/admin/controller/SessionContextController.java) 提供 GET /api/session/context，成功 no-store；[查询](../../../rag-admin/src/main/java/com/enterprise/rag/admin/session/mapper/SessionContextMapper.java)只使用服务端 userId/tenantId，并约束用户/组织有效和未删除。[服务](../../../rag-admin/src/main/java/com/enterprise/rag/admin/session/SessionContextService.java)对无效身份返回 AUTH_001/401，名称空缺返回固定 503；不接受客户端选择租户。
- [上下文状态](../../../rag-frontend/src/utils/sessionContext.ts)实现共用读取、generation 与会话/Token 序号保护；[认证状态](../../../rag-frontend/src/stores/auth.ts)只在内存保存归属，正常轮换重读且不重建页面。归属变化触发重新登录；[AppShell](../../../rag-frontend/src/layouts/AppShell.vue)卸载旧会话视图并清理知识库与聊天状态。
- [共享请求客户端](../../../rag-frontend/src/api/requestClient.ts)拒绝上一会话的成功响应，补齐原有失败重试保护；不增加重试次数。
- [归属组件](../../../rag-frontend/src/components/common/WorkspaceContext.vue)共用于侧栏和个人资料，含稳定骨架、失败重试、两行名称、全文入口、折叠按钮与可见焦点。移动抽屉先关闭再打开设置，桌面焦点回到实际入口。
- 知识库列表、详情、创建/编辑统一“组织内公开／非公开”，保留 isPublic 默认值、API 与读写授权。无数据与加载失败仍分开显示。
- 不修改 ChatPanel、轨迹组件、检索/生成协议、数据库 migration、真实配置和依赖。Skill 仅用于信息层级、文案与状态补全，保留原字体、Lucide 和主题。

## 自动验证

| 检查 | 本轮结果 | 能证明的范围 |
| --- | --- | --- |
| 新接口聚焦 Java | SessionContextTest 5 项通过 | 合成 H2 双组织真实 SQL 条件、失效/删除/不一致身份、长 ID 字符串精度、名称缺失、HTTP/no-store/客户端 selector 无效。 |
| 全仓 Maven | 最终 mvn -q test 退出码 0；131 suites，695 tests，0 failure、0 error、21 skipped | 仅统计本次完整运行开始后更新的 Surefire XML，不混入目录中旧 C15/C17 的失败报告。 |
| 完整前端测试 | 135/135，通过 node --test tests/*.test.mjs | 上下文恢复/共用、失败重试、轮换自触发 refresh、退出/换号迟到成功和错误、身份变化退出、组件状态、知识库清理及原有回归。 |
| 正式 FrontendBuild | PASS，包含 vue-tsc -b 和 Vite | 类型与打包，不替代真实业务联调。既有主包大于 500 kB 警告仍保留。 |
| 文档与敏感日志门禁 | DocAndScope PASS；SensitiveLogs PASS（425 份源码） | 差异格式、受保护路径、相对链接与源码日志扫描；不替代授权范围核对。 |

21 项跳过属于 MySQL migration/recovery 的 Docker 环境测试，以及未开启的 OTLP smoke。未修改 Python 评测脚本，不运行其业务回归。OpenSpec CLI 不在 PATH，规格按结构与链接检查，不能称官方 validate 通过。

### 发现与修复

首次全仓 Maven 出现 29 个 Spring context 初始化错误：新增 Mapper 没放在既有 `com.enterprise.rag.**.mapper` 扫描范围，单独手动注册 Mapper 的 H2 测试未暴露这一接线问题。已将本轮 Mapper 移入 session.mapper，不扩大扫描或改全局配置；随后完整 Maven 重跑通过，不能用聚焦测试替代这次全仓结果。

新增清理回归曾插入到模块加载之前，并出现重复声明；已经调整为加载后仅执行一次。其余旧测试断言未放宽，最终完整前端回归通过。

## 封闭浏览器交互

使用既有 [preview.mjs](../../../rag-frontend/tests/preview.mjs) 增加工作空间夹具；未知 API 仍失败关闭，不代理真实后端。组织、账号和资料均为合成内容。本轮通过 Ego 浏览器在同一个任务空间实际点击与读取 DOM，共 13 项断言通过：

- 宽屏个人资料与侧栏组织一致；深色沿用共享主题。
- Shift+Tab 留在设置内；Escape 回到实际工作空间入口。
- 76 px 折叠侧栏保留归属入口，关闭资料后回到该按钮。
- 读取失败后点击重试恢复归属；loading 有可访问状态。
- 移动抽屉在资料打开前关闭；完整 100 字名称保留。
- 窄屏资料弹窗在视口内；关闭后焦点回到打开侧栏按钮。
- 归属浏览没有发送 ask 或上传，最终窄屏页面无横向溢出。

宿主缩放会影响请求的模拟尺寸，测试按实际 innerWidth/innerHeight 校准到 1440×900 和 390×844，未把初次请求值直接当证据。长名称断言最初误设为“超过 100 字”，而夹具恰好 100 字；观察实际全文后改为与完整夹具逐字相等，再完成回归。

浏览器证据为当前聊天可视化目录的 workspace-browser-evidence.json 与 workspace-browser-05.mjs。只证明合成应用的 DOM、尺寸和交互，不证明真实租户数据。窗口最小化使截图无法取得，因此逐像素外观审阅和截图证据仍未完成；实体触屏、屏幕阅读器、200% 缩放及其他视口未验。

## 启动、范围与剩余事项

真实后端未重启、真实数据库未读取，provider/ask/embedding/rerank/judge 调用 0。新接口随下一次正常后端启动生效；若前端先更新而后端仍旧版，归属显示不可用，可在后端更新后重试，不伪造默认组织。

本 change 不创建第二业务租户，不提供组织切换、邀请、成员管理、计费或新角色。接口只是显示信息，授权仍由现有后端执行；组织名不代表安全验收标志。没有生产多租户 SLA 或真实双组织业务验收结论。

若需回滚，只定向撤销本 change 新接口/状态/展示接线，不覆盖已有规划和轨迹。用户确认过的轨迹登记 USER_ACCEPTED，真实 provider 边界和归档待办保留。本 change 当前待用户审阅，未暂存、提交、push、部署或接受/归档；建议 `feat(工作空间): 展示账号归属并明确知识库可见范围`。
