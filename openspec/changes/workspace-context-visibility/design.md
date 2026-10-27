# 工作空间上下文接入设计

## 💡 30秒人话设计主旨

在用户继续问答时，让他看清资料属于哪个组织、公开会让谁看到。新增只读接口取得服务端确认的账号和组织，再显示在现有侧栏与个人资料中。读取失败就说明不可用；旧账号的慢响应不能改回新账号的界面，实际归属变更时要求重新登录。

## 接口与查询

新增 GET /api/session/context，沿用 Bearer 认证与 ApiResponse。成功数据为 `{ userInfo: { id: number, username: string, email: string | null }, workspace: { id: string, displayName: string } }`，响应 `Cache-Control: no-store`。workspace.id 仅供比较，不展示，也不能回传作为租户选择凭据。

Controller 经 CurrentUserService 获取 RequestIdentity。SQL 同时约束 user.id、user.tenant_id、tenant.id，用户/租户均 enabled=1、deleted=0；客户端 header/query/body 不参与身份选择。无有效 principal、没有一致有效记录均返回既有 AUTH_001/401；名称空缺返回固定 SESSION_CONTEXT_UNAVAILABLE/503。数据库异常沿用脱敏处理，不记录 SQL、账号或名称。不改 migration、login/refresh DTO、JWT 或既有授权。

## 前端生命周期

auth store 在内存保存 workspace、contextStatus（idle/loading/ready/error）、内部已确认 identity 和 tokenRevision；名称不持久化。单独状态函数共用未结束请求，API 层通过既有 request 读取，避免 store 与 request 循环导入。

1. AppShell 在会话建立/页面刷新时读取；路由切换不重复，Sidebar/Settings 共享同一状态。手动重试共用当前未结束请求。
2. 每次读取捕获 sessionRevision 和 generation；成功、失败和 finally 只能写当前状态。退出或换号立即清空归属，旧成功/错误均不能回填。
3. 正常 Token 轮换递增独立 tokenRevision，不改变 sessionRevision，以保留草稿与业务页；清空展示的 workspace 并重新验证，内部 identity 仅用于比较。
4. 上下文自己收到 401 并触发 refresh 时，新 generation 可替代旧读取；save 回调不等待新读取，避免循环等待。过时 generation 自然结束但不写状态。
5. 同一会话内最新 userId/workspace.id 不同于已确认 identity，或不同于登录返回的 userId 时，立即清除会话并提示重新登录。AppShell 不渲染失效会话的侧栏/路由，以 sessionRevision 为 key 卸载旧业务视图，复用既有 store 清理。
6. HTTP 客户端拒绝上一 sessionRevision 的成功响应，补齐与既有失败重试相同的迟到保护；不增加重试次数。普通网络/5xx 或无效响应只让资料不可用，不重发业务写入。

## 信息层级与状态

侧栏品牌与主导航之间显示只读工作空间：标题 12/16 px、名称 14/20 px，最多两行；可聚焦“查看归属”提供完整 accessible name，资料弹窗全文换行。折叠时保留图标入口，移动时先关闭既有抽屉再打开资料。无切换箭头、安全通过徽章或虚构名称。

个人资料在用户名/邮箱之后显示归属。loading 使用等高骨架，error 使用固定说明与只读重试，ready 显示真实值；不把失败显示成无组织。列表为空显示暂无可访问知识库，并保留归属。

列表、详情和创建/编辑统一“组织内公开／非公开”，公开只限组织内读取，非公开仍允许创建者和已授权用户；保留 isPublic 默认值、接口字段及读写权限。现有字体、主题 tokens、Lucide 和布局继续沿用，新增控件提供 hover/pressed/focus-visible，不只用颜色区分状态；减少动态效果时不位移。不套用营销 Hero/FAQ/滚动标语，不改聊天输入与轨迹源码。

## 验证与回滚

H2 合成双组织测试验证真实 SQL 条件及 disabled/deleted/mismatched 身份，HTTP 测试验证认证/JSON/no-store/selector 无效；前端验证共享请求、恢复、轮换、退出/换号竞态、归属变化和组件状态。运行前端回归、包含 vue-tsc 的正式 build、相称 Maven、DocAndScope/SensitiveLogs。浏览器未执行时不能用 SSR 代替视觉验收；真实业务调用 0。

回滚仅定向处理本 change 文件与接线，保留既有规划、示意和轨迹；老后端没有接口则显示不可用，不创建第二业务租户或修改真实数据。

## 决策记录

### D1：独立接口恢复归属

- **当时面临哪几个选择？** 扩展登录响应、从 JWT 推断，或单独读取当前上下文。
- **最终选了谁及理由？** 选独立接口，能恢复刷新丢失的资料并读取真实组织名；/api 路由复用现有 Token refresh。
- **其他方案的代价是什么？** 登录扩展仍需恢复流程；JWT 没有名称且可能陈旧。新接口增加一次本地读取，需要并发与失败处理。

### D2：Token 轮换与换号使用不同序号

- **当时面临哪几个选择？** 每次轮换都重建业务页，或保留会话序号并单独重验归属。
- **最终选了谁及理由？** 选后者，正常 refresh 不应丢草稿或中断流，确认变更才清理重登。
- **其他方案的代价是什么？** 重建更简单但改变问答体验；独立序号必须验证迟到响应和上下文自己触发 refresh 的竞态。

### D3：归属变更先重新登录

沿用一个账号一个租户，归属变化清会话并卸载旧业务视图。完整切换需要成员关系及业务状态迁移契约，仍在范围外。

### D4：按用户限定范围使用 Skill

仅采用 landing-page-design 的信息层级、文案与状态补全；既有视觉和依赖继续沿用，租户 CRUD、邀请、切换、计费和新角色均不纳入本次实现。
