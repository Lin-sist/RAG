# Proposal: C13a Tenant Model, Context And Migration

## Why

当前系统已有数据库用户认证、owner/public/用户级知识库授权，但没有 tenant 领域模型。`UserPrincipal`、JWT、`CurrentUserService`、`knowledge_base` 和既有 Flyway schema 都只有 user/owner 身份；因此现状不能安全地被描述为多租户系统，也无法为后续 SQL、Milvus、cache、task、history 的强制租户过滤提供可信 tenant identity。

C13 必须拆分推进。C13a 先建立可迁移、可验证、服务端推导的 tenant identity，并保持现有单租户行为兼容；C13b 再逐数据面启用强制隔离。若直接在一个 change 中同时修改身份、schema、全部数据面和向量 adapter，范围过大且无法清楚判断迁移错误、身份错误或过滤遗漏来自哪一层。

## 用户故事：改前坏事 → 改后不同

- 改前坏事：认证主体只有 `userId`，客户端若将来提交 tenant metadata，服务端没有独立可信事实可校验；各数据面只能各自猜测租户，容易形成不一致或遗漏。
- 改后不同：每个已认证用户和知识库都有持久化 tenant identity，请求上下文只接受数据库加载并由服务端签发的 tenant；后续 C13b 可以复用同一身份源实施过滤。
- 改前坏事：直接给旧库增加非空 tenant 字段可能导致迁移失败、用户 ID/知识库 ID 改写或现有登录与知识库主链路中断。
- 改后不同：新的向前 Flyway migration 创建稳定的 legacy tenant，原地回填旧用户和知识库并锁定非空/索引约束，既有 ID 与 owner/public/KB 权限关系保持不变。
- 改前坏事：只增加字段就宣称“多租户隔离完成”，会掩盖 SQL、向量、缓存、任务和历史仍未强制隔离的事实。
- 改后不同：C13a 明确为暗铺设；只允许 legacy single-tenant 兼容状态，不提供 tenant CRUD/切换，不把本 change 的测试当作隔离证明。

## Goals

1. 新增最小 tenant 持久化模型，并给 `user` 与 `knowledge_base` 建立非空 tenant 归属。
2. 通过新的前向 Flyway migration 把所有既有用户和知识库映射到一个稳定的 legacy tenant，保持业务主键和现有授权关系不变。
3. 扩展数据库认证事实、`UserPrincipal`、JWT access/refresh claims 与统一服务端身份读取入口，使 tenant identity 可被后续数据面显式传递。
4. 对缺失、格式非法或与数据库事实不一致的 tenant identity fail closed；客户端输入不得覆盖服务端身份。
5. 用 migration/auth/context 聚焦测试证明 fresh install、V9 升级、登录、refresh 和旧 token 边界，不产生真实 provider 调用或数据出站。

## Non-Goals

- 不在本 change 启用或证明跨租户隔离。
- 不实现 tenant 创建、删除、邀请、成员管理、租户切换、跨租户管理员或计费。
- 不修改现有 username/email 的全局唯一性，也不改变登录请求 DTO。
- 不在 SQL/API、Milvus、Qdrant、Elasticsearch、Redis/cache、async task、QA history/feedback 上实施 tenant filter 或 namespace。
- 不改变 owner/public/`kb_permission` 的现有授权语义；`is_public` 在 C13b 前仍不能被解释为 tenant-public 契约。
- 不执行 C14 隔离评测，不开放 C15 MCP，不实现 C16 Router。
- 不新增依赖，不调用 embedding、rerank、ask、generation、judge、LLM 或任何外部 provider。

## Capability Classification

- `confirmed`：数据库用户认证、JWT access/refresh、refresh 时重载用户、统一 `CurrentUserService`、owner/public/用户级 KB 授权、Flyway V1-V9 与 MySQL migration test 入口。
- `partial`：用户/KB 已有 ownership 关系，但没有 tenant 表、tenant column、tenant-aware principal 或统一 tenant context。
- `planned`：legacy tenant、`user.tenant_id`、`knowledge_base.tenant_id`、服务端签发 tenant claim、显式 immutable tenant identity、旧 token fail-closed 与迁移兼容测试。
- `out_of_scope`：C13b 跨数据面强制隔离、tenant 管理/切换、多 membership、租户级 RBAC、C14 隔离评测、Qdrant/Elasticsearch tenant mode、MCP/Router。
- `unknown`：真实组织层级、一个用户是否需要加入多个租户、生产 tenant provisioning/SSO、跨租户运维角色与数据保留政策；这些信息不得在 C13a 中臆造。

## Proposed Contract

1. 每个运行时用户和知识库必须关联一个 tenant；现有数据统一迁入稳定的 legacy tenant。
2. tenant identity 来自数据库认证记录，并进入服务端签发的 access/refresh token 与 `UserPrincipal`；请求不得通过任何客户端字段选择或覆盖 tenant。
3. 新 token 缺失/非法 tenant claim 时认证失败；部署前签发且没有 tenant claim 的 token 需要重新登录，不做隐式默认租户回退。
4. refresh 必须按现有规则重载用户，同时重新取得 tenant identity；token 内旧 tenant 不得覆盖数据库事实。
5. C13a 完成后系统仍保持单租户兼容状态；只有 C13b 完成全部启用数据面的服务端过滤并经 C14 隔离评测后，才可宣称租户隔离成立。

## Impact

- 预计新增 `V10__tenant_model_and_legacy_backfill.sql`，并扩展 auth persistence、principal/JWT/context 及相关单元和 MySQL migration tests。
- `knowledge_base` 将持久化 tenant identity，但 C13a 不改变现有 KB 查询/授权行为。
- 旧 access/refresh token 在部署后需重新登录；这是一次显式兼容边界，不改变登录 API 形状。
- 规划阶段只新增 OpenSpec artifacts、更新 `.ai/ACTIVE_TASK.md` 并追加 `.ai/AGENT_LOG.md`。

## Risks And Mitigations

- 风险：迁移中错误回填或约束顺序导致旧库升级失败。缓解：分别验证 fresh install、V9→V10 有数据升级、ID/关系保持、重复 validate。
- 风险：JWT tenant claim 被误当作客户端可选路由。缓解：只由数据库 principal 签发，统一 context 读取，不支持 tenant header/body/query/metadata。
- 风险：旧 token 没有 tenant claim。缓解：明确 fail closed 并要求重新登录，不使用不安全的默认 tenant 猜测。
- 风险：C13a 被误报为完成隔离。缓解：spec、任务与长期文档均把 C13b/C14 设为必要后续门禁。
- 风险：未来需要多 tenant membership。缓解：C13a 明确一名用户一个 tenant 的原型边界；真实需求出现后另立 Type C change，不在本轮预构建管理平台。

## Acceptance Evidence

- OpenSpec 事前闸门批准 proposal、design 决策与 spec delta。
- MySQL Flyway fresh install 与 V9→V10 compatibility tests 通过，旧 user/KB ID、owner/public/permission 关系不变。
- auth/JWT/context 聚焦测试覆盖 login、refresh、缺失/非法 tenant claim、客户端伪造输入不生效。
- 既有认证与知识库聚焦回归通过；高风险时运行 `mvn -q test`。
- Python 评测脚本全量测试通过；前端无改动时正式 build 记为 `SKIPPED`。
- SensitiveLogs、protected paths、migration 范围、文档链接与 `git diff --check` 通过。
- 真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站和费用均为 0。

## Approval Gate

当前只批准启动规划，不代表批准 schema/Java 实现。用户需审阅并确认：单用户单 tenant、legacy tenant 回填、旧 token 要求重新登录、C13a 暗铺设边界、C13b/C14 后置门禁，以及 design 中的决策记录。提交责任保持 `用户手动提交`。
