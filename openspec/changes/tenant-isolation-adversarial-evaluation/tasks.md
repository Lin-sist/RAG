# Tasks: C14 Tenant Isolation Adversarial Evaluation

## 0. 事前闸门（实现前必须完成）

- [ ] 用户审阅并批准 proposal 的范围、非目标、受限结论与 C14 完成口径。
- [ ] 用户确认 design 的 16 条决策，重点确认独立 adversarial release、test-only 双 tenant、deterministic stub、组合 driver、后置状态、timing profile、gap 最小修复和 Milvus-only claim。
- [ ] 用户审阅并批准 `evaluation` 与 `rag-system` spec delta；确认 C14 PASS 不自动开放 tenant management/C15/C16，也不等于生产级多租户认证。
- [ ] 明确提交责任；当前默认 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [ ] 实现开始前复查 `git status --short --branch`，保护用户改动并补录上一规划提交 hash（若用户已提交）。

## 1. Adversarial Release Contract

- [ ] RED：validator tests 覆盖 artifact 缺失、hash/bytes/order/count drift、duplicate ID、unknown driver/enum、unsafe path、missing control 与 quota mismatch。
- [ ] 新增 `tenant-isolation-case-v1` schema，固定 actor/target/driver/mutation/control/expected/required 字段与有限枚举。
- [ ] 新增 v1 JSONL cases，覆盖 identity override、ID guessing、public/permission、reserved filter、cache/idempotency、task/recovery、vector/keyword、sync/SSE、history/feedback、error/timing disclosure。
- [ ] 新增 manifest，固定 artifact identity、ordered IDs、distribution、fixture/driver/profile version、timing policy 与 status/exit code。
- [ ] 实现纯标准库 validator，在 backend/container/provider 前 fail fast；增加 `--plan-only`/validate-only 证据且业务调用为 0。
- [ ] 运行 Python 聚焦 tests，记录 case 总数、类别配额和结果到 `.ai/AGENT_LOG.md`。

## 2. Isolated Harness And Fixture

- [ ] RED：profile/harness tests 证明未创建两个 synthetic tenant、fixture mismatch、Docker 缺失或 image drift 时不能形成 PASS。
- [ ] 新增 `c14-isolation-eval` Maven/Failsafe profile，复用已声明 Testcontainers，不新增/升级依赖。
- [ ] 建立隔离 MySQL/Redis/Milvus、随机本机端口、临时 durable input，以及 test-only A/B tenant/user/KB/document/history/feedback/task fixtures。
- [ ] 注入 test-scope deterministic embedding/generation stub；断言真实 provider/model calls=0。
- [ ] driver registry 使用有限白名单和版本，不允许 case 反射任意 class/method/path。
- [ ] harness 只操作自有 container/network/volume/temp path，不枚举、停止、清理或复用用户常驻基础设施。

## 3. Identity, SQL, API And Permission Matrix

- [ ] RED：header/query/body/cookie/metadata selector、缺失/非法 tenant claim 和 foreign IDs 取得预期失败。
- [ ] 覆盖 KB detail/list/update/delete/statistics、document list/delete/upload、history/feedback read/write/delete、task status/result/cancel/exists/completed。
- [ ] 覆盖 tenant-local public、owner 与 permission grant/read/write/admin；foreign public/permission 不扩大 scope。
- [ ] 对每个 foreign ID 使用 matched nonexistent control，比较 status/error/schema fingerprint 且禁止 foreign identity/content。
- [ ] 对写操作验证 tenant B SQL rows、版本、删除标记和关联关系前后不变。
- [ ] 若发现既有 C13b contract breach，先保留 RED case，再做最小修复和相邻回归；新语义返回事前闸门。

## 4. Cache, Task, Recovery And Durable Input Matrix

- [ ] RED：A/B 相同 query/content/idempotency/task pattern 的命中、覆盖、evict、clear、rebuild 攻击失败。
- [ ] 覆盖 auth session、QA/embedding cache、idempotency、task projection/payload mismatch 与旧无 tenant key 不回退。
- [ ] 覆盖 Redis projection miss、system scan、claim/heartbeat/phase/finalize/recovery/cancel，确认 scope 从 durable ledger 重建。
- [ ] 覆盖 input open/delete/cleanup、业务 lock 与 path traversal/symlink 组合，tenant B 文件 hash 和存在性不变。
- [ ] 验证 token blacklist/global IP rate limit 保持其 global security 语义，但不被当作 tenant business cache evidence。

## 5. Vector, Keyword, Sync And SSE Matrix

- [ ] RED：reserved tenant/KB/collection aliases、case-fold、snake/camel/hyphen 与 expression-like injection 均不能覆盖服务端 scope。
- [ ] 覆盖 Milvus 同物理 collection 的 search/get/getByIds/delete/count/drop、相同 vector ID、marker mismatch 与 foreign destructive operation。
- [ ] 覆盖 vector failure→keyword-only fallback，确认相同 tenant scope 且 tenant B canary 不可见。
- [ ] 覆盖 debug retrieval、sync ask 的 contexts/citations/metadata、SSE chunks/terminal state 和最终 history；禁止 foreign canary。
- [ ] 断言 deterministic stub 只用于 scope evidence，不输出 generation/citation/no-answer/judge 质量结论。
- [ ] Qdrant/Elasticsearch 只复核 enforcement-mode fail startup，不把 Milvus case 外推为其隔离 evidence。

## 6. Error And Timing Disclosure

- [ ] RED：foreign 与 nonexistent control 的 status/error/schema 不一致、响应/日志含 foreign identity/canary 时稳定失败。
- [ ] 实现 response fingerprint allowlist，忽略时间戳/traceId 等非安全动态字段，但不忽略 status/error/schema/敏感业务字段。
- [ ] details/summary/log 只输出 case ID、bounded taxonomy、hash/计数与聚合；扫描 raw token/body/content/canary 泄漏。
- [ ] 实现预注册 timing profile：10 warmup、40 fixed-seed interleaved pairs、median/P95 absolute-delta 阈值。
- [ ] 样本不足、错误、时钟异常、容器资源抖动或 profile drift 标为 `NOT_EVALUABLE`；禁止事后调阈值或挑 run。
- [ ] 记录 timing gate 只证明本机 synthetic coarse oracle，不外推生产网络或所有 side-channel。

## 7. Evaluator, Report And Status Semantics

- [ ] RED：missing/duplicate/unexpected case、channel error、required skip、timing incomplete、identity drift 与 overwrite 不能被成功子集掩盖。
- [ ] 实现 functional/content/error/timing 四通道与 global `PASS/FAIL/NOT_EVALUABLE/INVALID` 聚合。
- [ ] 固定退出码 `0/3/4/2`，并让 Markdown/details JSON 的 identity、counts、status、errors、skips 完全一致。
- [ ] 正式 evidence 要求 `--no-overwrite`；report metadata 固定 release、driver/profile version、Git HEAD、image versions 和 provider calls=0。
- [ ] validator/evaluator 继续只用 Python 标准库，不新增依赖或隐式网络调用。

## 8. Full Gates And Closeout

- [ ] 运行 case driver 与 gap-fix 聚焦 tests；记录 tests/failures/errors/skips。
- [ ] 运行 `mvn -q -pl rag-admin -am -Pc14-isolation-eval verify`；Docker/容器不可用不得以 skip 作为 PASS。
- [ ] 运行 `mvn -q test`；若既有 OTel collector 时序波动再次出现，按当前债务独立复跑并如实记录全仓非 GREEN，不在 C14 顺手改观测实现。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`；确认既有 evaluation contract/quality baseline 未漂移。
- [ ] 前端无改动时正式 build 记为 `SKIPPED`；若有前端/DTO 改动，运行包含 `vue-tsc` 的正式 build。
- [ ] 运行 SensitiveLogs、protected paths、credential/absolute path、raw payload/canary report、unknown driver、Markdown links 与 `git diff --check` 门禁。
- [ ] 将每个 requirement/scenario 映射到 case/test/evidence；mock-only、single-tenant、`PARTIAL`、`RETRIEVAL_ONLY` 不得替代 C14 PASS。
- [ ] 更新 tasks、`.ai/AGENT_LOG.md`、`openspec/project.md` 与相关 architecture/roadmap/optimization/eval guide 文档。
- [ ] 用户验收后才把两个 delta body 原文接受进对应 baseline、归档 change 并将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。
- [ ] 收口措辞限定为“Milvus 支持配置和固定 synthetic attack matrix 下 C14 evidence 通过”；不宣称生产级多租户、全 adapter、真实迁移或所有 timing side-channel 已验证。
