# Tasks: C13a Tenant Model, Context And Migration

## 0. Approval And Boundary

- [x] 用户要求检查 C13 readiness，允许则直接开始规划。
- [x] readiness：启动前 HEAD=`250a5c3`，工作区干净，`main...origin/main [ahead 3]`，`ACTIVE_TASK=IDLE`，C12 已接受 baseline 并归档，无其他 active change。
- [x] 冻结蓝图确认 C13 必须拆分；本 change 只启动 C13a，不并入 C13b/C14/C15/C16。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 创建 proposal、design、tasks 与 `rag-system` spec delta，并激活 `.ai/ACTIVE_TASK.md`。
- [x] 规划阶段真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件为 0。
- [x] 用户批准单用户单 tenant、legacy tenant 回填、旧 token 重新登录、immutable context 与暗铺设边界。
- [x] 用户批准 design 的决策记录及 `rag-system` delta，允许进入 migration/auth/context TDD。

## 1. Tenant Schema And Forward Migration

- [x] RED：fresh install 与 V9 fixture 升级测试证明当前没有 tenant model，并锁定旧 user/KB/permission identity。
- [x] RED：覆盖正常、禁用、逻辑删除 user，public/private KB 与现有 `kb_permission` 的回填场景。
- [x] GREEN：新增前向 V10 migration，创建唯一 `legacy-default` tenant，并给 `user`、`knowledge_base` 增加 tenant_id。
- [x] GREEN：按先可空回填、再非空/索引约束的顺序完成迁移；不得修改 V1-V9。
- [x] 验证 fresh install、V9→latest migrate、Flyway validate、重复启动，以及 user/KB 主键、owner/public/permission 关系不变。

## 2. Auth Persistence Principal And JWT

- [x] RED：auth repository/account/principal 必须携带数据库 tenantId，缺失或无效时不得完成认证。
- [x] RED：access/refresh token 都包含服务端 tenant claim；缺失、非整数、非正数或错误类型 fail closed。
- [x] RED：refresh 重载 fresh user tenant，旧 token tenant 不能覆盖数据库事实。
- [x] GREEN：扩展 auth entity/repository、`UserPrincipal` 与 JWT provider，保持 username/password/role 与 tokenType 现有契约。
- [x] GREEN：登录、validate、refresh 和认证 filter 使用统一 tenant claim 规则；错误响应和日志不泄露 token/tenant 内部值。

## 3. Immutable Server-Derived Request Identity

- [x] RED：统一 context 对非 `UserPrincipal`、null userId/tenantId 与非法 tenant identity 返回稳定未认证边界。
- [x] RED：client header、query、body 或 metadata 中的 tenant 值不能创建、切换或覆盖 context。
- [x] GREEN：新增 immutable `RequestIdentity(userId, tenantId)` 或等价值对象，并由统一 service 从 principal 构造。
- [x] 保持显式传播设计，不新增跨请求 ThreadLocal；为后续 async/Reactor/task handoff 留下明确接口。
- [x] C13a 不把 identity 接入 SQL/Milvus/cache/task/history filter，不新增 tenant CRUD/switch API。

## 4. Compatibility And Scope Tests

- [x] 更新数据库认证、bootstrap、JWT property/filter 与 `CurrentUserService` 聚焦测试。
- [x] 更新 HappyPath/知识库测试 fixture，使 legacy tenant 下现有登录、创建 KB、上传、索引、检索和删除语义不变。
- [x] 证明旧无 tenant claim token 被拒绝并需要重新登录，不做 fixed tenant 或客户端 fallback。
- [x] 证明只有一个内部 legacy tenant 可用，不产生第二租户隔离成功的误导性测试。
- [x] 扫描确认 Qdrant/Elasticsearch、vector metadata、Redis keys、task/history 查询未被本 change 静默部分修改。

## 5. Verification

- [x] 运行 auth/admin/migration 聚焦 tests，记录 suites/tests/failures/errors/skips。
- [x] 运行 `mvn -q test`。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 运行 SensitiveLogs、secret/absolute-path、protected paths、migration ordering、Markdown links 与 `git diff --check`。
- [x] 前端无改动时正式 build 记为 `SKIPPED`；Docker-dependent MySQL test 若环境不可用，明确记录而不伪称通过。
- [x] 确认真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、业务数据出站、费用与限流事件为 0。

## 6. Acceptance And Closeout

- [ ] 用户验收 migration、auth/context evidence、旧 token 边界与 C13a 非隔离声明。
- [ ] 原文接受 delta 到 `openspec/specs/rag-system/spec.md`。
- [ ] 同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`。
- [ ] 归档 change，恢复 `.ai/ACTIVE_TASK.md=IDLE`，验证 archive structure 与 baseline exact match。
- [ ] 明确下一阶段仍是 C13b 服务端跨数据面强制隔离；C14 通过前不开放 C15/C16。
