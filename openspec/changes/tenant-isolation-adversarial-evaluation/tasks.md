# Tasks: C14 Tenant Isolation Adversarial Evaluation

## 0. 事前闸门（实现前必须完成）

- [x] 用户审阅并批准 proposal 的范围、非目标、受限结论与 C14 完成口径。
- [x] 用户确认 design 的 16 条决策，重点确认独立 adversarial release、test-only 双 tenant、deterministic stub、组合 driver、后置状态、timing profile、gap 最小修复和 Milvus-only claim。
- [x] 用户审阅并批准 `evaluation` 与 `rag-system` spec delta；确认 C14 PASS 不自动开放 tenant management/C15/C16，也不等于生产级多租户认证。
- [x] 明确提交责任；用户已授权 `Agent 提交` 当前 C14 计划内文件，仍不 push、不创建 PR、不部署。
- [x] 实现开始前复查 `git status --short --branch`；HEAD=`bb9d0a4`、工作区干净，规划提交已识别并按 append-only 规则补录。

## 1. Adversarial Release Contract

- [x] RED：validator tests 覆盖 artifact 缺失、hash/bytes/order/count drift、duplicate ID、unknown driver/enum、unsafe path、missing control 与 quota mismatch。
- [x] 新增 `tenant-isolation-case-v1` schema，固定 actor/target/driver/mutation/control/expected/required 字段与有限枚举。
- [x] 新增 v1 JSONL cases，覆盖 identity override、ID guessing、public/permission、reserved filter、cache/idempotency、task/recovery、vector/keyword、sync/SSE、history/feedback、error/timing disclosure。
- [x] 新增 manifest，固定 artifact identity、ordered IDs、distribution、fixture/driver/profile version、timing policy 与 status/exit code。
- [x] 实现纯标准库 validator，在 backend/container/provider 前 fail fast；增加 `--plan-only`/validate-only 证据且业务调用为 0。
- [x] 运行 Python 聚焦 tests，记录 case 总数、类别配额和结果到 `.ai/AGENT_LOG.md`。

## 2. Isolated Harness And Fixture

- [x] RED：profile/harness tests 证明 evidence map/driver identity、双 tenant fixture、Docker/image/health evidence 缺失或漂移时不能形成 PASS。
- [x] 新增 `c14-isolation-eval` Maven/Failsafe profile，复用已声明 Testcontainers，不新增/升级依赖。
- [x] 建立隔离 MySQL/Redis/Milvus、随机本机端口、临时 durable input，以及 test-only A/B tenant/user/KB/document/history/feedback/task fixtures。
- [x] 注入 test-scope deterministic embedding/generation stub；stub 为 synthetic 索引、sync/SSE control 提供可复现输出，真实 provider 调用数为 0。
- [x] driver registry 使用有限白名单和版本，不允许 case 反射任意 class/method/path。
- [x] harness 只操作自有 container/network/volume/temp path，不枚举、停止、清理或复用用户常驻基础设施。

## 3. Identity, SQL, API And Permission Matrix

- [ ] RED：header/query/body/cookie/metadata selector、缺失/非法 tenant claim 和 foreign IDs 取得预期失败。
- [x] 覆盖 KB detail/list/update/delete/statistics、document list/delete/upload、history/feedback read/write/delete、task status/result/cancel/exists/completed。
- [x] 覆盖 tenant-local owner/reader permission read 与 foreign public/permission；foreign public/permission 不扩大 scope。
- [x] 当前 HTTP 切片已对 KB detail/statistics/document-list 使用 matched nonexistent control，比较 status/error/schema fingerprint 且禁止 foreign identity/content；其余 foreign ID 继续保留未完成项约束。
- [x] 当前 HTTP 切片已对跨 tenant KB update/delete 验证 tenant B SQL row、版本和删除标记前后不变；其余写入口继续保留未完成项约束。
- [x] 若发现既有 C13b contract breach，先保留 RED case，再做最小修复和相邻回归；本轮修复 Milvus null metadata scoped upsert NPE 与 task not-found 指纹，均先 RED 后 GREEN。

## 4. Cache, Task, Recovery And Durable Input Matrix

- [x] RED：A/B 相同 query/content/idempotency/task pattern 的命中、覆盖与 projection rebuild 攻击失败。
- [ ] 覆盖 auth session、QA/embedding cache、idempotency、task projection/payload mismatch 与旧无 tenant key 不回退。
- [x] 覆盖 Redis projection miss、system scan、claim/heartbeat/phase/finalize/recovery/cancel，确认 scope 从 durable ledger 重建。
- [ ] 覆盖 input open/delete/cleanup、业务 lock 与 path traversal/symlink 组合，tenant B 文件 hash 和存在性不变。
- [ ] 验证 token blacklist/global IP rate limit 保持其 global security 语义，但不被当作 tenant business cache evidence。

## 5. Vector, Keyword, Sync And SSE Matrix

- [x] RED：reserved tenant/KB/collection aliases、case-fold、snake/camel/hyphen 与 expression-like injection 均不能覆盖服务端 scope。
- [x] 复用并纳入专用 profile 的 `MilvusFailureSemanticsIT` 覆盖同物理 collection 的 search/get/getByIds/delete/count/drop、相同 vector ID、marker mismatch 与 foreign destructive operation。
- [x] 复用并纳入专用 profile 的 `MilvusFailureSemanticsIT` 覆盖 vector failure→keyword-only fallback 及 tenant scope。
- [x] 覆盖 debug retrieval reserved-scope、sync ask 的 response/context pipeline、SSE chunks/terminal state 和最终 history；禁止 foreign canary。
- [x] 断言 deterministic stub 只用于 scope evidence，不输出 generation/citation/no-answer/judge 质量结论。
- [ ] Qdrant/Elasticsearch 只复核 enforcement-mode fail startup，不把 Milvus case 外推为其隔离 evidence。

## 6. Error And Timing Disclosure

- [x] RED：foreign 与 nonexistent control 的 status/error/schema 不一致、响应含 foreign identity/canary 时稳定失败。
- [x] 当前 HTTP 切片实现 response fingerprint allowlist，忽略 traceId 等动态值但保留 status/error/schema，并扫描 foreign canary/tenant/user identity。
- [ ] details/summary/log 只输出 case ID、bounded taxonomy、hash/计数与聚合；扫描 raw token/body/content/canary 泄漏。
- [x] 当前 KB route 实现预注册 timing profile：10 warmup、40 fixed-seed interleaved pairs、median/P95 absolute-delta 阈值。
- [x] 样本不足、请求错误或 profile drift 标为 `NOT_EVALUABLE`；冻结 10/40/14001 与 median/P95 阈值，禁止事后调阈值或挑 run。
- [x] 记录 timing gate 只证明本机 synthetic coarse oracle，不外推生产网络或所有 side-channel。

## 7. Evaluator, Report And Status Semantics

- [x] RED：missing/duplicate/unexpected case、channel error、required skip、timing incomplete、identity drift 与 overwrite 不能被成功子集掩盖。
- [x] 实现 functional/content/error/timing 四通道与 global `PASS/FAIL/NOT_EVALUABLE/INVALID` 聚合。
- [x] 固定退出码 `0/3/4/2`，并让 Markdown/details JSON 的 identity、counts、status、errors、skips完全一致。
- [x] 正式 evidence 要求 `--no-overwrite`；report metadata 固定 release、driver/profile version、Git HEAD、image versions和 provider calls=0。
- [x] validator/evaluator 继续只用 Python 标准库，不新增依赖或隐式网络调用。

## 8. Full Gates And Closeout

- [ ] 运行 case driver 与 gap-fix 聚焦 tests；记录 tests/failures/errors/skips。
- [x] 运行 `mvn -q -pl rag-admin -am -Pc14-isolation-eval verify`；默认命令先被既有 OTel 普通单测时序波动拦截，随后以空普通单测 selector 运行同一 profile 的 Failsafe 门禁并通过 5/0/0/0，Docker/容器未 skip。
- [x] 运行 `mvn -q test`；全仓在既有 OTel collector 时序用例 217 tests / 1 failure / 2 skipped，失败用例独立复跑通过；如实保持全仓非 GREEN，不在 C14 修改观测实现。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`；176 tests 通过，既有 evaluation contract/quality baseline 未漂移。
- [ ] 前端无改动时正式 build 记为 `SKIPPED`；若有前端/DTO 改动，运行包含 `vue-tsc` 的正式 build。
- [ ] 运行 SensitiveLogs、protected paths、credential/absolute path、raw payload/canary report、unknown driver、Markdown links 与 `git diff --check` 门禁。
- [ ] 将每个 requirement/scenario 映射到 case/test/evidence；mock-only、single-tenant、`PARTIAL`、`RETRIEVAL_ONLY` 不得替代 C14 PASS。
- [ ] 更新 tasks、`.ai/AGENT_LOG.md`、`openspec/project.md` 与相关 architecture/roadmap/optimization/eval guide 文档。
- [ ] 用户验收后才把两个 delta body 原文接受进对应 baseline、归档 change 并将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。
- [ ] 收口措辞限定为“Milvus 支持配置和固定 synthetic attack matrix 下 C14 evidence 通过”；不宣称生产级多租户、全 adapter、真实迁移或所有 timing side-channel 已验证。
