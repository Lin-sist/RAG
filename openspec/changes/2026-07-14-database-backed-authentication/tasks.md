# Tasks: C2 Database-backed Authentication

## Phase 0：启动与规格草案（当前阶段）

- [x] 按顺序读取 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、`openspec/project.md`、`rag-system` baseline spec、冻结蓝图和当前认证/迁移事实。
- [x] 确认 C1 已归档、工作树无未提交改动，C2 顺序前置满足。
- [x] 将能力状态分类为 `confirmed / partial / planned / out_of_scope / unknown`。
- [x] 创建 proposal，包含“改前坏事 → 改后不同”的用户故事。
- [x] 创建 design、tasks 与 `rag-system` spec delta 草案。
- [x] 声明 embedding/rerank/judge/ask 调用量均为 0，草案不授权业务外调。
- [x] 明确提交责任为“用户手动提交”。
- [x] 将 `.ai/ACTIVE_TASK.md` 指向本 change。
- [ ] 用户审阅并明确批准 proposal、design 与 spec delta。

## Phase 1：数据库查询边界（TDD）

- [ ] RED：为有效用户、缺失用户、逻辑删除、disabled 与角色集合编写失败测试。
- [ ] GREEN：在 `rag-auth` 新增最小 user/role entity、mapper 与 repository/query service。
- [ ] GREEN：实现数据库 `UserDetailsService`，删除内存 Map 和默认用户初始化。
- [ ] REFACTOR：统一 username 查询与角色装配，确保错误和日志不泄露用户名或密码。
- [ ] 运行 `mvn -q -pl rag-auth -am test` 并记录结果。
- [ ] 更新本 tasks 与 `.ai/AGENT_LOG.md`，建议中文提交但不代用户提交。

## Phase 2：known seed 前向迁移（TDD）

- [ ] 记录现有 V1→V5 checksum/版本事实，禁止修改历史 migration。
- [ ] RED：真实 MySQL 覆盖精确 known seed、已改密码同名用户、ID 保留与重复 migrate。
- [ ] GREEN：新增下一序号 Flyway migration，精确隔离 known seed，不删除用户 ID。
- [ ] 验证全新数据库与 V5 升级数据库两条 migration 路径。
- [ ] 验证 Flyway validate 通过、历史 checksum 不变。
- [ ] 更新本 tasks 与 `.ai/AGENT_LOG.md`。

## Phase 3：一次性 bootstrap（TDD）

- [ ] RED：默认关闭、blank/占位符/known-default、空库创建、隔离种子接管、正常用户 no-op、已有其他用户冲突、ADMIN 缺失和事务回滚。
- [ ] GREEN：实现 `auth.bootstrap` 配置绑定与无 secret 泄露的 policy 校验。
- [ ] GREEN：实现数据库初始化完成后、ready 前执行的事务性 bootstrap。
- [ ] GREEN：保证 BCrypt 入库、ADMIN 关联幂等、正常用户永不覆盖。
- [ ] 扫描日志与异常，不出现用户名、密码、长度、hash 或片段。
- [ ] 更新本 tasks 与 `.ai/AGENT_LOG.md`。

## Phase 4：登录与 refresh 契约（TDD）

- [ ] RED：数据库正确/错误密码、disabled/deleted、重启后持久登录。
- [ ] RED：refresh 时用户缺失/禁用/删除被拒绝，角色变化进入新 token。
- [ ] GREEN：完成 Spring Security 接线，保留 login/refresh/logout API 与 DTO shape。
- [ ] 回归 Redis session 与 refresh blacklist 既有行为。
- [ ] 运行聚焦模块测试并记录真实结果。
- [ ] 更新本 tasks 与 `.ai/AGENT_LOG.md`。

## Phase 5：固定凭据与操作入口清理

- [ ] 删除登录页固定账号提示。
- [ ] 将评测脚本默认密码改为显式 CLI/env 输入，并补 preflight 单测。
- [ ] 更新正式使用/开发/评测文档，不再宣称固定默认账号。
- [ ] 扫描 tracked runtime/界面/正式入口中的 `admin123/user123`；仅允许明确 test-scope fixture。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 运行包含 `vue-tsc` 的正式前端 build。
- [ ] 更新本 tasks 与 `.ai/AGENT_LOG.md`。

## Phase 6：完整验证与验收

- [ ] `mvn -q test` 通过，并记录 suites/tests/failures/errors/skipped 及测试内部降级。
- [ ] 真实 MySQL/Flyway migration 验证通过；不能用 H2 代替。
- [ ] 敏感日志门禁通过。
- [ ] Python unittest 与正式前端 build 通过。
- [ ] `git diff --check`、change 结构、Markdown 相对链接和范围扫描通过。
- [ ] 确认没有 embedding、rerank、judge、ask 或其他业务 provider 调用。
- [ ] 用户明确确认实现验收通过。
- [ ] 将 delta 接受进 `openspec/specs/rag-system/spec.md`。
- [ ] 将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE`，经用户确认后归档 change。

## Guardrails

- 用户批准草案前不得修改 Java、SQL、Vue、运行配置或评测脚本实现。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。
- 不修改历史 `V1`/`V3` migration，不粗暴删除 username=`admin` 的记录。
- 不新增注册/用户管理/密码修改 API，不顺手实现 C3。
- 不为测试定制 RAG prompt、切分、检索或拒答行为。
- 未取得单独授权不得执行业务外部调用、push、PR、发布或部署。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交。
