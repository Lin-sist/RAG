## ADDED Requirements

### Requirement: 服务端确认的当前账号上下文
系统 SHALL 提供已认证只读 GET /api/session/context，从服务端 userId/tenantId 查询同时有效、未删除且归属一致的 user/tenant，返回 ApiResponse 的 userInfo 和 workspace。workspace.id SHALL 为字符串，名称 SHALL 来自数据库，响应 SHALL no-store；MUST NOT 返回其他组织列表、credential、内部 code、猜测角色或接受客户端选择租户。

#### Scenario: 正常读取和 selector
- GIVEN 用户属于组织 A
- WHEN 额外 header/query 声称组织 B 并读取上下文
- THEN 只返回服务端身份的 A 资料与字符串组织 ID，响应 no-store

#### Scenario: 无效身份
- GIVEN principal 无效、账号/组织失效或归属不一致
- WHEN 读取上下文
- THEN AUTH_001/401，不回退默认组织或披露其他身份

#### Scenario: 名称不可用
- GIVEN 身份有效但名称为空
- WHEN 读取上下文
- THEN SESSION_CONTEXT_UNAVAILABLE/503，不编造名称

### Requirement: 会话恢复和响应隔离
前端 SHALL 在会话建立、页面刷新及 Token 轮换后读取上下文，同一 generation 共用请求且只存内存。成功/错误/finally SHALL 校验 revision/generation。正常轮换 MUST NOT 清草稿或重建业务页；确认 userId/workspace.id 变化 MUST 清会话并卸载旧视图，要求重登。

#### Scenario: 刷新与共享
- GIVEN 已有 Token 而内存资料为空
- WHEN 组件需要归属
- THEN 共用一次读取恢复资料，路由切换不重复

#### Scenario: 轮换和自触发 refresh
- GIVEN 归属已确认且页面有草稿
- WHEN Token 轮换或上下文自己触发 refresh
- THEN 清旧名称并重验，同身份保留业务页，不循环等待或接受旧 generation

#### Scenario: 退出后迟到
- GIVEN A 请求尚未结束
- WHEN 退出或登录 B 后旧请求成功或失败
- THEN 不回填 B 状态，上一会话成功 HTTP 响应不得交给新会话使用

#### Scenario: 归属变化
- GIVEN 当前会话已确认归属
- WHEN 新上下文 userId/workspace.id 改变
- THEN 清会话，卸载旧知识库/历史/聊天/上传/轨迹并提示重登，不静默迁移

### Requirement: 渐进式归属展示
系统 SHALL 在侧栏品牌与导航间显示只读归属，个人资料展示全文；折叠/移动 SHALL 提供键盘/触屏入口。loading SHALL 稳定占位，error SHALL 固定说明与只读重试，长名 SHALL 可完整读取。MUST NOT 增加切换、安全通过徽章或虚构名称。

#### Scenario: 长名称和窄屏
- GIVEN 已取得名称
- WHEN 展开、折叠或窄屏查看
- THEN 最多两行且可打开资料读取全文，移动抽屉先关闭

#### Scenario: 加载失败
- GIVEN 正在读取或网络失败
- WHEN 查看或重试
- THEN 显示骨架/不可用，重试不触发 ask/上传/provider

### Requirement: 可见范围文案一致
列表、详情、创建/编辑 SHALL 统一“组织内公开／非公开”，公开只限组织内读取，非公开仍允许创建者与已授权用户。MUST 保留 isPublic 默认值、字段和读写授权，空资料 SHALL 与归属不可用分开。

#### Scenario: 可见性
- GIVEN isPublic 为 true 或 false
- WHEN 列表、详情或表单展示
- THEN 与既有授权一致，公开不表示可编辑

#### Scenario: 空资料
- GIVEN 组织有效但无可访问知识库
- WHEN 打开列表
- THEN 保留归属并显示暂无可访问资料，不提示未加入组织
