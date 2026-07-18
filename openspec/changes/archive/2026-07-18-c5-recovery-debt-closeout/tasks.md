# Tasks: C5 Recovery Debt Closeout

## 0. Approval And Boundary

- [x] 用户明确要求检查并修复 C5 阶段债务，完成收口后再进入 C6 规划。
- [x] 沿用 C5b 已批准安全边界：outcome unknown 不重放、auto resume 默认关闭、真实 provider 调用为 0。
- [x] 提交责任为 `用户手动提交`。

## 1. Legacy And Stable Failure States

- [x] RED：legacy PENDING/FAILED + AVAILABLE + no ledger 被隔离且下游调用为 0。
- [x] 实现 bounded legacy scan 与 `RECONCILIATION_REQUIRED` document 状态，不合成 task、不删除 input。
- [x] RED：contract mismatch 能稳定转 reconciliation，而不是因 phase 条件更新失败。
- [x] RED：attempt exhausted 进入 FAILED/TERMINAL 且不再被扫描。

## 2. Bounded Coordinator

- [x] RED：两个 worker 竞争只有一个 claim 成功。
- [x] RED：恢复期间按 heartbeat interval 续租，非 owner 与过期 lease 不能 heartbeat。
- [x] 实现固定有界 concurrency executor 与可关闭生命周期。
- [x] 实现 DB-time exponential backoff、stable failure code 与 max-attempt terminality。
- [x] lease 丢失后在新的 vector mutation 前 fail closed。

## 3. Transactional Finalization

- [x] RED：VECTOR_CONFIRMED 重复 finalize 不重复 chunks/document count。
- [x] 抽取独立 transactional finalizer，锁 document 行并原子更新 SQL 事实与 ledger completion。
- [x] keyword upsert/input cleanup 保持事务外幂等，不把外部副作用伪装成 SQL rollback。
- [x] migration 增加必要的 chunk uniqueness/ledger 索引且兼容旧数据。

## 4. Real Integration Verification

- [x] MySQL 8.0.36：V1→最新、V7→最新、legacy async_task migration/Flyway validate。
- [x] MySQL 8.0.36：双 claimant、heartbeat owner/expiry、backoff、attempt exhausted。
- [x] Redis restart：outage 503，restart 后 miss 回源 durable ledger 并重建 owner projection。
- [x] crash-window：SAFE_PRE_VECTOR 可恢复；VECTOR_IN_FLIGHT 隔离；VECTOR_CONFIRMED 收尾 vector calls=0。

## 5. Closeout

- [x] 聚焦测试与 `mvn -q test`。
- [x] `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] SensitiveLogs、受保护路径、公开 DTO/前端/retrieval/eval 与 `git diff --check`。
- [x] 更新 project/architecture/roadmap/AGENT_LOG，将已完成 C5 实现债务改为待验收治理闸门。
- [x] 用户验收后接受 spec delta 进入 baseline。
- [x] 用户验收后归档 change、恢复 `IDLE`。

## Commit Responsibility

当前为 `用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布。
