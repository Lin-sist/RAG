# Tasks: C7 Reranker A/B Evaluation

## 0. Approval And Boundary

- [x] 用户要求建立 C7 规划文档，交付审阅后再授意执行。
- [x] readiness 复核：`main` 与 `origin/main` 同步、工作区干净、`ACTIVE_TASK=IDLE`、C6 delta 已接受并归档、C7 是冻结路线图下一阶段。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 本规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0。
- [ ] 用户批准 proposal 的 scope、non-goals、用户故事与 acceptance criteria。
- [ ] 用户批准 design 的 15 条决策记录。
- [ ] 用户批准 `evaluation` spec delta 的 4 个 requirements / 11 个 scenarios。
- [ ] 用户授权进入离线实现；该授权不包含任何真实 provider 调用。
- [ ] 用户选择正式 latency 预算：最低 `R=1,W=0`，或建议候选 `R=3,W=3`，或另行指定。
- [ ] 真实 canary 前单独披露并获批 provider/model、调用量、出站数据、限流、费用及零费用依据。
- [ ] full A/B 前基于 canary 结果再次确认调用预算；未获授权不得执行。

## 1. Per-sample Latency And Safe Aggregation

- [ ] RED：debug retrieval 成功/失败都记录非负有限 `retrieveLatencyMillis`，且使用 monotonic clock。
- [ ] RED：rerank/retrieval latency 的 nearest-rank P50/P95 在奇数、偶数、单元素、空集合和非法值输入下口径固定。
- [ ] 实现逐样本 retrieval wall-clock、latency count/min/P50/P95/max 聚合。
- [ ] 保留 `rerankLatencyMillis` 与 `retrieveLatencyMillis` 两套字段，禁止重命名或混算。
- [ ] 验证既有 Recall@3/5、MRR、Top1、generation、citation、no-answer、judge 和 Report status 公式不变。

## 2. Reproducible Arm Identity

- [ ] RED：metadata 必须包含 eval-set SHA-256、selected sample IDs/order、run index、measured repeats 与 warm-up policy。
- [ ] RED：缺少或包含非法字段的 arm manifest 在运行前失败，不联系 backend/provider。
- [ ] 定义 `c7-reranker-ab-v1` sanitized manifest schema 与 secret denylist。
- [ ] metadata 保存 manifest 白名单、manifest SHA-256、fixture/KB/config/Git HEAD 既有身份。
- [ ] observed requested/effective provider、model、protocol 与 manifest 不一致时生成 stable mismatch fact。
- [ ] 验证 manifest/metadata 不包含 API key、Authorization、password、完整环境变量或 raw provider body。

## 3. Offline Comparator

- [ ] RED：Git HEAD、eval-set、fixture、KB、config、sample order、run count 任一 strict identity 不一致时为 `NOT_COMPARABLE`。
- [ ] RED：heuristic arm requested/effective 污染、fallback 或 model call 时为 `NOT_COMPARABLE`。
- [ ] RED：model arm 任一 eligible observation fallback、provider mismatch、model call 不是 1 或 candidate coverage 不完整时为 `NOT_COMPARABLE`。
- [ ] RED：zero-candidate 仅在两 arm 同一 pair 都出现时为 not-applicable；单边出现时为 `zero_candidate_mismatch`。
- [ ] RED：缺少 run/sample pair、retrieve error、非 `RETRIEVAL_ONLY` arm 或 schema 损坏产生稳定状态与原因。
- [ ] 实现 strict identity diff、allowed provider differences 与 stable reason taxonomy。
- [ ] 实现 100% eligible model coverage 检查，禁止成功子集比较。
- [ ] 仅在 `COMPARABLE` 时计算 Recall@5/MRR/Top1、P50/P95 与逐样本 paired deltas。
- [ ] `NOT_COMPARABLE` 时保留 provider/fallback/missing-pair 诊断，但隐藏收益 delta。

## 4. Evidence And Documentation

- [ ] 输出 compact JSON：schema、identity、comparison status/reasons、arm summaries、per-sample facts、source file hashes。
- [ ] 输出 Markdown：范围、身份、调用预算、provider coverage、质量/延迟表、fallback 与结论边界。
- [ ] 验证 compact evidence 不包含 question、contexts、passages、raw response、异常 message 或 secret marker。
- [ ] raw repeat details 默认写入 `tmp/eval/`，tracked compact evidence 只记录 hash。
- [ ] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的 arm manifest、preflight、canary、full、compare 与恢复说明。
- [ ] 若用户批准 live evidence，记录每次 run 的 Report status、errors、retry、rate limit、metadata 与 Git HEAD，不以文件名代替结论。

## 5. Offline Verification

- [ ] 运行 comparator/latency/metadata 聚焦 Python tests。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 运行 `python -B scripts/run_reproducible_rag_eval.py --plan-only ...`，核对选中样本与估算调用量且真实调用为 0。
- [ ] 运行 SensitiveLogs 与定向 secret/user-content marker 扫描。
- [ ] 运行 `git diff --check`、断链、旧字段、受保护路径与 tracked secret 扫描。
- [ ] 运行 `mvn -q test` 做最终仓库回归，统计 reports/tests/failures/errors/skipped。
- [ ] 前端无改动时记录 build `SKIPPED`；若范围意外涉及前端则停止并回到事前闸门。
- [ ] 记录离线实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0。

## 6. Gated Live Canary

- [ ] 用户批准 canary 的精确 sample IDs、arm 次数、provider/model、出站、限流与费用说明。
- [ ] 确认 Docker/backend ready；运行 mutation-free `--preflight-only`，不得创建、上传、删除或重建 KB。
- [ ] heuristic canary 验证 requested/effective heuristic、fallback=0、model calls=0。
- [ ] model canary 验证 requested/effective model、fallback=0、model calls=1、candidate coverage=100%。
- [ ] canary 任一 mismatch/fallback/error 时停止，不自动进入 full run。
- [ ] 记录 canary 实际 embedding/rerank 调用量、timeout、retry、error category 与 rate limit；ask/judge/LLM generation=0。

## 7. Gated Full A/B And Closeout

- [ ] 用户基于 canary 明确批准 full budget 与 repeats/warm-up。
- [ ] 按批准顺序运行 heuristic/model arms，固定 KB/fixture/config/Git HEAD 与样本全集。
- [ ] 生成并验证 offline comparison；只有 `COMPARABLE` 才陈述质量/延迟 delta。
- [ ] 明确 30 条开发样本的外推限制、费用/限流事实和默认 provider 不变。
- [ ] 更新 proposal/design/tasks、`openspec/project.md`、架构/路线图/技术债与 `.ai/AGENT_LOG.md`。
- [ ] 用户验收实现、live evidence 与结论边界。
- [ ] 用户验收后接受 delta 进 `evaluation` baseline、恢复 `ACTIVE_TASK=IDLE` 并归档 change。

