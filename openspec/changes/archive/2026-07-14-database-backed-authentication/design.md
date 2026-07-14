# Design: C2 Database-backed Authentication

## 1. 设计目标

将认证用户的唯一运行时事实从进程内默认账号切换到数据库持久化用户与角色，同时安全移除已知固定凭据。首版只解决“谁能登录、当前是否可用、拥有哪些角色、如何安全建立第一个管理员”，不扩展为用户管理平台。

## 2. 当前数据流与缺口

```text
应用启动
  └─ UserDetailsServiceImpl.initDefaultUsers()
       └─ 内存 admin/admin123 + user/user123

登录
  └─ DaoAuthenticationProvider
       └─ 内存 UserDetailsService
            └─ UserPrincipal → JWT → Redis session

refresh
  └─ JWT 中的 username
       └─ 内存 UserDetailsService 重载
            └─ 新 JWT
```

数据库虽然已有 `user`、`role`、`user_role`，但没有进入认证链路。`V3` 默认管理员当前不被登录使用；C2 一旦切到数据库，它会立即成为潜在固定凭据，因此数据源切换与种子隔离必须作为同一 change 交付。

## 3. 目标数据流

```text
Flyway 前向迁移
  └─ 精确识别 V3 known seed
       └─ 禁用 + 写入不可认证隔离标记，保留 user.id

应用启动（bootstrap 默认关闭）
  └─ enabled=false → 不创建用户
  └─ enabled=true
       ├─ 校验外部 username/password，不记录值
       ├─ 空用户库 → 创建用户 + 绑定 ADMIN（同一事务）
       ├─ 精确隔离种子 → 原 ID 安全接管 + 绑定 ADMIN（同一事务）
       ├─ 正常同名用户 → 幂等 no-op，绝不覆盖
       └─ 其他冲突状态 → fail-fast

登录 / refresh
  └─ DatabaseUserDetailsService
       └─ user + user_role + role
            └─ UserPrincipal → JWT → Redis session
```

## 4. 持久层边界

### 4.1 模块归属

认证用户 entity、mapper 和 repository 放在 `rag-auth`，因为 `rag-admin` 已依赖 `rag-auth`，反向依赖会造成模块循环。`rag-auth` 通过现有 `rag-common` 已可获得 MyBatis Plus 类型；不因 C2 新增持久层框架或升级依赖。

计划对象：

- `AuthUser`：映射 `user`，包含 id、username、passwordHash、email、enabled、deleted、version 与时间字段；
- `AuthRole`：至少映射 id、name、deleted；
- `AuthUserMapper` / `AuthRoleMapper`：数据库访问；
- repository/query service：封装“按 username 加载有效用户及角色”的认证查询，不让 Spring Security 服务拼接 SQL 细节。

权限表暂不进入 `GrantedAuthority`。现有安全规则使用 `ROLE_*`，C2 只从数据库角色生成 `UserPrincipal.roles`；permission 级授权属于后续独立能力。

### 4.2 有效用户定义

认证查询只接受：

- username 精确匹配；
- `deleted=0`；
- 用户记录存在；
- 登录时 `enabled=1`；
- 角色只加载 `role.deleted=0` 的关联记录。

用户不存在、已逻辑删除和密码错误对外保持统一的无凭据泄露失败语义；disabled 用户继续映射现有 `DisabledException` / `userDisabled` 语义。

角色集合允许为空，但只能访问不要求角色的已认证路径；`/admin/**` 仍要求 `ROLE_ADMIN`。角色名标准化规则沿用数据库值，进入 `UserPrincipal` 前拒绝 blank，不自动把任意 permission code 当角色。

## 5. 历史默认种子的安全迁移

### 5.1 不修改 V3

不得编辑 `V3__init_data.sql`，否则已执行环境会产生 Flyway checksum 差异。新增下一序号前向 migration，以 username 与 `V3` 中的精确 password hash 共同识别 known seed；只匹配 username 不足以证明它仍是默认凭据。

### 5.2 隔离而非删除

直接删除用户可能使现有 `knowledge_base.owner_id`、历史记录或未来补加的外键引用失去主体。前向 migration 对精确 known seed：

1. 设置 `enabled=0`；
2. 将 `password_hash` 替换为稳定、明确不可被 BCrypt 接受为用户密码的内部隔离标记；
3. 保留 `id`、username、业务数据和现有引用；
4. 不触碰已更改 password hash 的同名管理员；
5. 不把旧 hash、bootstrap 密码或其派生内容写入日志。

隔离标记只是状态哨兵，不是密码或 secret。最终字符串在实现前通过测试锁定，并只用于精确识别“可由 bootstrap 接管的历史种子”。

### 5.3 迁移验证矩阵

至少在真实 MySQL 上验证：

- 全新数据库执行 V1→最新：known seed 最终不可登录；
- 已执行到 V5 且仍为精确 known seed：升级后被隔离，ID 不变；
- 已执行到 V5 但 admin password hash 已修改：升级后记录、状态和 hash 不变；
- 重复 migrate：无二次副作用；
- Flyway validate 通过，历史 migration checksum 不变。

## 6. 一次性 bootstrap

### 6.1 配置契约

建议配置前缀为 `auth.bootstrap`：

- `enabled`：默认 `false`；
- `username`：启用时必填；
- `password`：启用时必填，只允许从外部注入，tracked config 不给可登录默认值；
- `email`：可选；
- 目标角色固定为既有 `ADMIN`，首版不开放任意角色配置。

启用后，blank、首尾空白、完整未解析占位符和仓库已知固定密码 `admin123/user123` 必须 fail-fast。错误只包含配置键与稳定错误类别，不包含用户名、密码、长度、hash 或片段。

### 6.2 执行时机与事务

bootstrap 必须依赖数据库初始化完成，并在应用被视为 ready 前完成。用户写入、密码 BCrypt 编码、ADMIN 角色查找与 `user_role` 关联在同一事务中；任何一步失败都不得留下半初始化用户。

bootstrap 不创建 role/permission 基础数据。若 `ADMIN` 角色缺失，说明 migration 状态异常，应 fail-fast。

### 6.3 状态机

| 当前数据库状态 | bootstrap 行为 |
|---|---|
| `enabled=false` | 不查询/创建 bootstrap 用户 |
| 启用且没有任何非删除用户 | 创建外部指定用户并绑定 ADMIN |
| 启用且目标 username 为精确隔离种子 | 保留 ID，写入外部密码的 BCrypt hash，启用并确保 ADMIN 关联 |
| 启用且目标 username 为正常 ADMIN 用户 | 幂等 no-op；不改密码、enabled、email 或角色 |
| 启用且目标 username 为正常非 ADMIN 用户 | fail-fast；不得静默提权或覆盖角色 |
| 启用、目标不存在，但已有其他非删除用户 | fail-fast，防止向已有系统意外注入管理员 |
| 启用但配置无效或 ADMIN 角色缺失 | fail-fast，无部分写入 |

“正常同名 ADMIN 用户 no-op”保证重复启动安全；同名非 ADMIN 用户必须 fail-fast，避免 bootstrap 静默提权。运维完成首次 bootstrap 后仍应关闭开关并撤去外部初始密码。C2 不尝试自动修改进程环境或 secret manager。

## 7. 登录、refresh 与 token 边界

### 7.1 登录

保留 `DaoAuthenticationProvider` 与 `BCryptPasswordEncoder`。`DatabaseUserDetailsService` 每次登录按 username 查询数据库，构造新的 `UserPrincipal`，不保留进程内可变用户 Map。

### 7.2 refresh

现有 `AuthServiceImpl.refreshToken` 已在会话校验后调用 `loadUserByUsername`。C2 保留顺序并以数据库结果签发新 token：

- 用户缺失、逻辑删除或禁用：拒绝 refresh；
- 角色变化：新 token 使用最新角色；
- 旧 refresh token 只有在新 token 成功签发路径上按现有规则失效。

### 7.3 access token

C2 不把每个 access token 请求改为数据库查询。已签发 access token 在到期或进入黑名单前仍携带签发时的用户/角色快照；实时撤权、用户版本 claim 或全会话吊销需单独 change。

## 8. 固定凭据清理

实现阶段扫描 tracked files，并按用途处理：

- 删除 `UserDetailsServiceImpl` 的 `admin123/user123` 与默认用户初始化；
- 删除登录页的固定账号提示；
- 使用/开发/评测文档改为“显式提供 bootstrap 或评测凭据”；
- `run_rag_eval.py`、`run_reproducible_rag_eval.py` 不再默认 `admin123`，缺失时在发起登录前明确报错；
- 测试代码中的合成密码允许保留，但必须只在 test scope，不能被描述为可登录默认账号；
- 不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。

## 9. 测试设计

### 9.1 TDD 切片

1. repository 查询：有效用户、deleted、disabled、角色集合与不存在；
2. migration：known seed、changed seed、重复迁移与 ID 保留；
3. bootstrap policy：默认关闭、配置拒绝、空库创建、隔离种子接管、正常用户 no-op、冲突 fail-fast、事务回滚；
4. 登录：正确/错误密码、disabled/deleted、重启后数据库读取；
5. refresh：用户禁用/删除拒绝、角色变化进入新 token；
6. 脚本与前端：缺失显式密码 preflight、固定凭据扫描、正式 build。

### 9.2 验证命令

实现阶段计划运行：

```powershell
mvn -q -pl rag-auth -am test
mvn -q -pl rag-admin -am test
mvn -q test
python -B -m unittest discover -s scripts -p 'test_*.py'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode SensitiveLogs
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode FrontendBuild
git diff --check
```

真实 MySQL migration 测试可使用仓库已声明的 Testcontainers MySQL；不要求 Redis/Milvus/LLM provider。若 Docker 不可用，必须记录为未验证风险，不能用 H2 冒充通过。

## 10. 备选方案

### 方案 A：继续保留内存用户，仅同步数据库

拒绝。会形成双写和双事实源，数据库禁用/角色变化仍不能可靠约束认证。

### 方案 B：直接删除 V3 默认 admin

拒绝。可能破坏 owner/history 引用，也无法安全区分已修改密码的真实管理员。

### 方案 C：修改 V3，移除 seed

拒绝。会破坏已执行环境的 Flyway checksum；必须新增前向 migration。

### 方案 D：bootstrap 每次启动都覆盖 admin 密码

拒绝。会让配置泄漏或误配置持续接管账号，也破坏用户已修改状态；bootstrap 只能创建、精确接管隔离种子或幂等 no-op。

### 方案 E：本次同时增加用户管理 API

拒绝。冻结蓝图已把该能力定义为旁支 Type C，不阻塞 C3，也不属于 C2。

## 11. 回滚

- 代码回滚必须同时恢复认证实现、bootstrap 配置、测试和未接受 spec delta，不能只恢复内存默认用户而保留数据库契约。
- 已执行的前向 migration 不通过编辑历史文件回退。若需要恢复被隔离的 seed，只能使用新的、审计明确的前向 migration 或显式 bootstrap 外部凭据；不得恢复固定已知密码。
- 回滚不得删除已创建的真实用户或破坏其业务数据引用。

## 12. 已接受的审查结论

1. 是否接受“精确隔离、保留 user.id”而非删除历史默认管理员；
2. 是否接受 bootstrap 默认关闭、空库创建、隔离种子接管、正常同名用户 no-op、其他冲突 fail-fast 的状态机；
3. 是否接受首版只加载角色，不把 permission code 映射为 `GrantedAuthority`；
4. 是否接受 C2 只保证登录与 refresh 重载数据库，access token 实时撤权留给后续 change；
5. 是否接受评测脚本移除固定密码 fallback，改为显式 CLI/env 输入。

用户已明确批准以上全部设计结论，允许按 tasks 的 TDD 切片进入实现。

## 13. 提交责任

`用户手动提交`。Agent 不暂存、不提交、不 push。
