# RAG System Spec Delta: C2 Database-backed Authentication

## ADDED Requirements

### Requirement: 数据库用户认证

系统 SHALL 以数据库中持久化的用户、密码 hash、enabled/deleted 状态和角色作为登录认证事实源，MUST NOT 在运行时创建或依赖进程内固定默认账号。密码 MUST 以 BCrypt hash 验证，普通日志、错误响应和 tracked files MUST NOT 包含明文密码或 bootstrap 凭据。

#### Scenario: 持久化用户成功登录

- GIVEN 数据库存在未删除、已启用且密码 hash 有效的用户
- AND 用户关联一个或多个未删除角色
- WHEN 用户提交正确的 username 与 password
- THEN 系统从数据库加载用户与角色并完成认证
- AND 签发的 token 使用该数据库用户 ID 与当前角色
- AND 应用重启后仍可使用同一持久化用户登录

#### Scenario: 无效用户不能登录

- GIVEN username 对应的用户不存在、已逻辑删除、已禁用或密码不匹配
- WHEN 发起登录
- THEN 系统拒绝认证
- AND 响应不暴露用户是否存在、password hash 或凭据内容

#### Scenario: 运行时不存在固定默认账号

- GIVEN bootstrap 未显式启用
- WHEN 应用完成启动
- THEN 系统不创建进程内或数据库默认用户
- AND `admin123`、`user123` 等仓库已知固定凭据不能用于登录

### Requirement: Refresh 时重载用户状态

系统在使用 refresh token 签发新 token 前 SHALL 从数据库重新加载用户与角色。用户不存在、已逻辑删除或已禁用时 MUST 拒绝 refresh；角色变化 SHALL 反映到新 token。C2 不要求每次 access token 请求都查询数据库，存量 access token 继续受现有过期与黑名单规则约束。

#### Scenario: 用户被禁用后不能刷新

- GIVEN refresh token 的签发用户已在数据库中被禁用、逻辑删除或移除
- WHEN 客户端请求 refresh
- THEN 系统拒绝签发新的 access token 与 refresh token
- AND 不使用 refresh token 内的旧用户状态绕过数据库状态

#### Scenario: 角色变化进入新 token

- GIVEN 用户的数据库角色在原 token 签发后发生变化
- AND 用户仍存在且已启用
- WHEN refresh 会话与 token 校验通过
- THEN 系统使用数据库中的最新角色签发新 token
- AND 不继续复制旧 token 中的角色快照

### Requirement: 显式一次性管理员 Bootstrap

系统 SHALL 提供默认关闭的一次性管理员 bootstrap。bootstrap 仅在显式启用且用户名、密码由外部配置完整提供时执行；tracked config MUST NOT 提供可登录密码 fallback。用户写入、BCrypt 编码后的密码持久化与 ADMIN 角色关联 MUST 在同一事务中完成。

bootstrap MUST 幂等，MUST NOT 覆盖正常同名用户的密码、enabled 状态、email 或角色。数据库已有其他非删除用户而目标 username 不存在、ADMIN 角色缺失或配置无效时，应用 MUST fail-fast 且不得留下部分数据。

#### Scenario: Bootstrap 默认关闭

- GIVEN bootstrap 配置未设置或 `enabled=false`
- WHEN 应用启动
- THEN 系统不创建或修改任何用户与角色关联
- AND 缺少 bootstrap 凭据不会产生默认账号

#### Scenario: 空用户库创建首个管理员

- GIVEN bootstrap 显式启用
- AND 外部提供合法 username 与 password
- AND 数据库没有非删除用户且存在 ADMIN 角色
- WHEN 应用完成数据库初始化
- THEN 系统创建一个已启用的持久化用户
- AND 只持久化 password 的 BCrypt hash
- AND 在同一事务中建立 ADMIN 角色关联
- AND 应用在 bootstrap 完成前不被视为 ready

#### Scenario: 重复启动不覆盖正常管理员

- GIVEN bootstrap 目标 username 已对应正常数据库用户
- AND 该用户已有 ADMIN 角色
- WHEN 使用相同或不同的外部 bootstrap 配置再次启动
- THEN bootstrap 幂等结束
- AND 不修改该用户的密码、状态、email 或角色

#### Scenario: 同名非管理员不能被静默提权

- GIVEN bootstrap 目标 username 已对应正常数据库用户
- AND 该用户没有 ADMIN 角色
- WHEN 应用启动
- THEN 应用 fail-fast
- AND 不修改该用户的密码、状态、email 或角色

#### Scenario: 已有其他用户时拒绝注入管理员

- GIVEN bootstrap 显式启用
- AND 目标 username 不存在
- AND 数据库已存在其他非删除用户
- WHEN 应用启动
- THEN 应用 fail-fast
- AND 不创建新用户或部分角色关联
- AND 错误不回显 username、password、长度、hash 或内容片段

### Requirement: 已知默认管理员种子隔离

系统 SHALL 通过新的前向 Flyway migration 精确识别历史 migration 中 username 与 password hash 均匹配的已知默认管理员种子，并使该固定凭据不可认证。系统 MUST NOT 修改已执行的历史 migration，MUST NOT 仅按 username 删除用户，且 SHALL 保留可能被业务数据引用的用户 ID。

显式 bootstrap MAY 接管带精确隔离标记的历史种子，使用外部密码的 BCrypt hash 重新启用同一用户 ID 并确保 ADMIN 关联；它 MUST NOT 把相同逻辑应用到已修改密码的正常同名用户。

#### Scenario: 精确 known seed 被隔离

- GIVEN 数据库仍包含 username 与 password hash 均匹配历史 known seed 的用户
- WHEN 执行 C2 前向 migration
- THEN 该用户被禁用并写入不可认证的隔离标记
- AND 用户 ID 与业务数据引用保持不变
- AND 已知固定密码不能登录

#### Scenario: 已修改密码的同名用户不受影响

- GIVEN 数据库存在相同 username 但 password hash 已不同于历史 known seed 的用户
- WHEN 执行 C2 前向 migration
- THEN migration 不修改该用户的 password hash、enabled 状态、角色或 ID

#### Scenario: Bootstrap 接管隔离种子

- GIVEN bootstrap 显式启用
- AND 目标 username 对应精确隔离标记的历史种子
- AND 外部凭据合法且 ADMIN 角色存在
- WHEN bootstrap 执行
- THEN 系统保留原用户 ID
- AND 用外部密码的 BCrypt hash 替换隔离标记并启用用户
- AND ADMIN 角色关联存在且无重复记录
