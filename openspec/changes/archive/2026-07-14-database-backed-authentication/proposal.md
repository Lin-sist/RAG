# Proposal: C2 Database-backed Authentication

## Why

当前 `UserDetailsServiceImpl` 在进程内初始化 `admin/admin123` 与 `user/user123`，登录认证并不读取数据库；与此同时，Flyway `V1` 已创建 `user`、`role`、`user_role` 等表，`V3` 又写入了带已知固定凭据的默认 `admin`。这造成两套互相脱节的用户事实：运行时依赖内存假账号，数据库用户、禁用状态与角色变化不能成为认证依据；一旦直接切换到数据库，历史默认种子又会变成固定可登录凭据。

C2 将认证数据源切换为数据库用户和角色，并增加默认关闭、显式启用的一次性 bootstrap。历史 `V3` 不回改，而由新的前向迁移精确隔离已知默认凭据；正常用户、已变更密码的管理员和可能引用该用户 ID 的业务数据不得被误删或覆盖。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，不管数据库里用户是否被禁用、改了角色或根本不存在，应用仍只认代码里写死的账号；固定的 `admin/admin123` 还散落在界面、脚本和文档中。改之后，登录和刷新都以数据库里的真实用户状态与角色为准，系统默认不会创建任何可登录账号；只有管理员显式提供外部 bootstrap 凭据时，才会安全、幂等地建立首个管理员，并且不会覆盖已有正常用户。

## Current Status

- `confirmed`：C1 已实现、验收并归档，`.ai/ACTIVE_TASK.md` 在本 change 启动前为 `IDLE`。
- `confirmed`：`V1` 已存在 `user`、`role`、`user_role`、`permission`、`role_permission` 表。
- `confirmed`：`V3` 写入已知默认 `admin` 行及 ADMIN 角色关联；历史 Flyway migration 不能直接修改。
- `confirmed`：当前登录通过内存 `UserDetailsServiceImpl`，内置 `admin/admin123` 与 `user/user123`。
- `confirmed`：refresh 流程已经调用 `UserDetailsService.loadUserByUsername` 重载用户，切换数据库实现后可复用该边界。
- `partial`：当前测试 profile 使用 H2 MySQL mode，但关闭 Flyway；尚无从历史 migration 升级到 C2 migration 的真实 MySQL 证据。
- `planned`：新增认证 entity/mapper/repository、数据库 `UserDetailsService`、一次性 bootstrap、前向迁移和固定凭据清理。
- `out_of_scope`：注册、用户管理 API、密码修改/找回 API、租户模型、权限管理 UI、C3 全链路联合集成测试。
- `unknown`：不同部署平台最终用哪种 secret manager 注入 bootstrap 密码；C2 只定义配置与安全边界，不绑定平台。

## Scope

- 在 `rag-auth` 中建立数据库用户、角色查询所需的 entity/mapper/repository 边界。
- 用数据库实现替换进程内 `ConcurrentHashMap` 用户和 `ApplicationReadyEvent` 默认账号初始化。
- 登录时读取未逻辑删除的数据库用户、密码 hash、enabled 状态和角色集合。
- refresh 签发新 token 前重新读取数据库用户与角色；用户不存在、已删除或禁用时拒绝刷新。
- 增加默认关闭的一次性 bootstrap：
  - 仅显式启用时执行；
  - 用户名和密码必须从外部配置注入，tracked config 不提供可登录 fallback；
  - 密码只以 BCrypt hash 入库，不写日志、响应或 tracked files；
  - 首次创建/接管已隔离种子与 ADMIN 角色赋予在同一事务中完成；
  - 重复启动幂等，不覆盖正常用户的密码、状态或角色；
  - 数据库已有其他用户但目标用户名不存在时 fail-fast，避免意外注入新管理员。
- 新增前向 Flyway migration，精确匹配 `V3` 中已知 username + password hash，将该凭据隔离为不可登录状态；不修改 `V3` checksum，不删除可能被业务数据引用的用户 ID。
- 清除运行时代码、登录界面、正式使用/评测入口中的固定默认凭据；评测脚本改为要求显式参数或环境变量。
- 增加聚焦单元测试、持久层测试、bootstrap 测试和真实 MySQL/Flyway 迁移验证。

## Non-goals

- 不新增注册、用户 CRUD、角色/权限管理或密码修改/找回 API。
- 不新增 `must_change_password` 等无法由本 change 完成闭环的密码状态机。
- 不改变 login/refresh/logout HTTP 路径、DTO shape、JWT claim shape 或 token 过期策略。
- 不要求每次 access token 鉴权都查询数据库；C2 明确保证登录与 refresh 的数据库重载，存量 access token 生命周期仍由现有 JWT/黑名单规则控制。
- 不删除或重写历史 `V1`、`V3` migration，不按用户名粗暴删除数据库用户。
- 不把 C3 的登录→上传→索引→检索→删除联合链路纳入本 change。
- 不修改 retrieval、embedding、rerank、generation、citation、no-answer 或 judge 行为及指标。
- 本草案阶段不修改 Java、SQL、Vue、配置或评测脚本实现。

## External Calls And Authorization

本 change 的规格、实现和验证不需要 RAG 业务外部调用：

| 调用类型 | 预计调用量 | 数据出站 | 模型 | 限流风险 | 费用与零费用依据 | 授权状态 |
|---|---:|---|---|---|---|---|
| embedding | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |
| rerank | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |
| judge | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |
| ask | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |

Maven、前端 build、Python unittest 和本地/Testcontainers MySQL 验证只处理仓库代码与合成测试数据，不发送问题、文档、prompt、context、密码或 secret 给业务 provider。若实现计划出现任何业务外调，必须先更新 proposal 并重新取得用户授权。

## Risks

- 直接删除 `V3` 默认管理员可能使 `knowledge_base.owner_id` 等历史引用失去主体：本 change 采用精确隔离并保留 ID，不做粗暴删除。
- bootstrap 规则过宽可能覆盖真实管理员：只允许创建空库首个用户或接管带精确隔离标记的历史种子；正常同名用户永不覆盖。
- H2 无法证明 MySQL/Flyway 语义：C2 必须提供真实 MySQL migration 路径证据，不能仅用 context load 代替。
- 删除脚本默认密码会改变本地评测启动方式：通过明确的 CLI/env preflight 错误和文档迁移说明控制影响。
- access token 在到期前仍携带签发时角色：这是现有 JWT 边界；C2 只保证 refresh 重载，实时吊销留给后续单独契约。

## Acceptance Evidence

- 用户先审阅并明确批准 proposal、design 与 spec delta，之后才进入实现。
- 数据库用户使用正确密码可以登录；错误密码、缺失、逻辑删除或 disabled 用户不能登录。
- 应用重启后用户仍可从数据库认证，不再初始化内存默认用户。
- refresh 前重载数据库用户与角色；禁用/删除用户不能获得新 token，角色变化反映到新 token。
- bootstrap 默认关闭且不创建用户；显式启用并提供外部凭据时能事务性建立首个 ADMIN 用户。
- bootstrap 重复执行幂等；正常同名用户的密码、状态和角色不被覆盖；冲突状态 fail-fast 且不泄露凭据。
- 真实 MySQL 从现有 migration 链升级后，精确已知默认凭据不可登录，已修改密码的同名用户不受影响，历史用户 ID 得到保留。
- tracked runtime、界面和正式操作入口不再提供 `admin123/user123` 固定凭据；测试 fixture 必须明确限定在测试代码。
- 聚焦测试、`mvn -q test`、Python 评测脚本 unittest、包含 `vue-tsc` 的正式前端 build、敏感日志门禁与 `git diff --check` 通过。
- 不发生 embedding、rerank、judge、ask 或其他业务 provider 调用。

## Commit Responsibility

`用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、发布或部署；每个可验证切片结束后只提供建议的中文 Conventional Commit message。
