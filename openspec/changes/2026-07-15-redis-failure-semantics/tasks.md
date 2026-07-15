# Tasks: C4c Redis Failure Semantics

## 0. Approval Gate

- [ ] 用户审阅并批准 proposal、design、决策记录与 spec delta。
- [ ] 用户确认限流 outage 采用 fail-closed 503，而不是当前 fail-open。
- [ ] 用户确认 auth session/blacklist outage 采用 fail-closed 503。
- [ ] 用户确认带幂等 key 的 post-operation 存储失败表达为 `outcome_unknown`，不建议自动重试。
- [ ] 用户确认 statistics query-count read 失败返回 503，不伪造零值且本 change 不改 DTO。
- [ ] 用户确认 task recovery/replay/orphan coordination 留给 C5a/C5b。
- [ ] 提交责任维持 `用户手动提交`，或用户另行明确授权 `Agent 提交`。

> 审批未完成前不得修改生产 Java、测试、配置、依赖或 baseline spec。

## 1. Consumer Inventory And RED Tests

- [ ] 建立所有 tracked Redis direct consumers 清单，逐项标记 optional/security-critical/side-effect-guard/state-source。
- [ ] 为 QA cache read/write/evict Redis 故障添加 RED 测试。
- [ ] 为 embedding cache read/write/evict Redis 故障添加 RED 测试。
- [ ] 为 query count increment/delete/statistics read 添加 RED 测试。
- [ ] 为 login/refresh/logout 与 blacklist lookup/write 添加 RED 测试。
- [ ] 为 rate limiter null/empty/exception 添加 RED 测试，断言 controller 未执行。
- [ ] 为 idempotency pre-operation 与 post-operation 写失败添加 RED 测试，记录 supplier 调用次数与事务结果。
- [ ] 为 task initial/status/progress/terminal/cancel Redis 故障添加 RED 测试。

## 2. Optional Paths

- [ ] 让 QA cache read/write Redis 故障 fail-open，不改变 canonical 问答结果或 C4b generation 副作用。
- [ ] 让 embedding cache read/write Redis 故障 fail-open，不中断 provider embedding 结果。
- [ ] 让 cache evict、query count increment 和知识库删除计数清理失败只产生安全诊断。
- [ ] 让 statistics query-count read 失败返回稳定 503，不返回 `queryCount=0`。

## 3. Security-Critical Paths

- [ ] login session hash 与 TTL 成功后才返回 token；Redis 故障返回 503。
- [ ] refresh session read/write 故障返回 503，不误报 invalid token、不签发新 token。
- [ ] blacklist lookup unknown 不建立认证上下文；blacklist write 失败不报告 logout 成功。
- [ ] rate-limit Redis null/empty/exception 与正常 429 分离，故障返回 503。
- [ ] 客户端和日志不包含 token/hash/session/key/value 或底层异常 message。

## 4. Idempotency Paths

- [ ] pre-operation Redis read/lock 故障返回 503，supplier 调用次数为 0。
- [ ] 正常 PROCESSING 冲突继续返回 409。
- [ ] post-operation completed-state 写失败返回 `IDEMPOTENCY_OUTCOME_UNKNOWN`，不声称回滚、不提示自动重试。
- [ ] 用 Spring AOP + transaction 集成测试记录真实切面顺序与数据库副作用结果。
- [ ] `required=false` 且未提供 key 时保持当前兼容路径。

## 5. Async Task Paths

- [ ] 初始 PENDING 状态持久化成功前不注册/启动 future。
- [ ] status/result read 故障返回 503，仅成功读到 absent 才返回 404。
- [ ] progress/terminal 状态写失败可观察，不能记录或返回假 COMPLETED。
- [ ] cancel 未能持久化 CANCELLED 时不得报告成功。
- [ ] 明确记录执行中断的不确定副作用由 C5 恢复/协调，不在 C4c 伪造 fallback。

## 6. Isolated Fault Integration

- [ ] 复用 C3 Testcontainers 结构或建立 `c4c-redis-fault` Failsafe profile。
- [ ] 对隔离 Redis 执行 stop/start，覆盖至少一个 optional fail-open、auth/rate-limit fail-closed、task status 503 场景。
- [ ] 断言未停止、复用、清理或改写任何用户常驻 `rag-*` Redis/容器/volume。
- [ ] 全程使用合成用户、token marker、key/value 与任务数据；真实 provider 调用量为 0。

## 7. Verification And Closeout

- [ ] 运行 `mvn -q -pl rag-common -am test`。
- [ ] 运行 `mvn -q -pl rag-auth -am test`。
- [ ] 运行 `mvn -q -pl rag-core -am test`。
- [ ] 运行 `mvn -q -pl rag-admin -am test`。
- [ ] 运行选定的隔离 Redis Failsafe verify。
- [ ] 运行 `mvn -q test`。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 运行 SensitiveLogs 门禁与 `git diff --check`。
- [ ] 更新本 tasks、`.ai/ACTIVE_TASK.md` 与追加式 `.ai/AGENT_LOG.md`。
- [ ] 用户完成实现验收后，才接受 spec delta、恢复 `IDLE` 并归档 change。

## Commit Responsibility

当前为 `用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布；若用户另行授权 Agent 提交，只限 C4c 计划内本地文件。
