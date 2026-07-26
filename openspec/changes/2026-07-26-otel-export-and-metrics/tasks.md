# Tasks: C12 OTel Export And Metrics

## 0. Approval And Boundary

- [x] 用户要求检查 C12 readiness，允许则直接开始规划。
- [x] readiness：启动前 HEAD=`5cdddd2`，工作区干净，`main...origin/main`，`ACTIVE_TASK=IDLE`，C11 已接受 baseline 并归档，无其他 active change。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 创建 proposal、design、tasks 与 `rag-system` spec delta，并激活 `.ai/ACTIVE_TASK.md`。
- [x] 规划阶段真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、镜像下载、telemetry 出站和费用为 0。
- [ ] 用户批准 proposal 的本机自托管 backend、default-off、OTLP push、metrics、sampling、retention、权限、dashboard/alerts 与 non-goals。
- [ ] 用户批准 design 的 15 条决策记录及 `rag-system` delta 的 4 requirements / 12 scenarios。
- [ ] 用户明确授权新增 OTel OTLP exporter/metrics SDK 依赖、下载固定 Collector/Tempo/Prometheus/Grafana images 并进入 Java/config TDD。

## 1. Export Configuration And Lifecycle

- [ ] RED：默认开关下无 exporter/metric reader/network connection，C11 tracing disabled/no-op 行为不变。
- [ ] RED：tracing/metrics/export 三个开关可独立组合，invalid/out-of-range properties 被安全处理。
- [ ] RED：unreachable backend、queue full、timeout 与 shutdown flush failure 均业务 fail-open，不泄漏 endpoint/header/exception 原文。
- [ ] GREEN：按 common facade / admin SDK wiring 分层增加 OTLP gRPC trace/metric export 与 typed properties。
- [ ] GREEN：实现 bounded batch/queue/timeout、resource identity、safe internal export state 与有界 shutdown。

## 2. Low-Cardinality Metrics

- [ ] RED：固定 instrument name/type/unit/bucket 与 operation/stage/provider/fallback/token lifecycle 被测试锁定。
- [ ] RED：metrics 与 trace sampling 解耦；unsampled trace 对应 operation/stage metrics 仍完整。
- [ ] RED：未知 provider/outcome/route/fallback 归一为 `unknown/other`，随机高基数输入不导致 series 线性增长。
- [ ] RED：question/prompt/answer/context/snippet/file/user/task/document/chunk/trace/span id、rank/score、model 自由文本、endpoint/credential/error 原文无法成为 labels。
- [ ] GREEN：在 C11 真实 lifecycle 上记录 count/duration/in-flight/stage/provider/fallback/actual-token/coverage，不重复计数 retry 或未执行阶段。

## 3. Collector Tempo Prometheus Grafana Reference Stack

- [ ] 从官方发行源选择并记录相互兼容的固定 images/version 或 digest；禁止 `latest`。
- [ ] 新增独立 `deploy/observability/docker-compose.observability.yml` 与 `.env.example`，不改变默认 compose 启动行为。
- [ ] 配置 Collector memory limiter/batch/tail sampling、Tempo local retention=72h、Prometheus retention=7d。
- [ ] 仅向宿主 localhost 映射 OTLP 4317 与 Grafana 3000；Tempo/Prometheus/Collector internal ports 不公开。
- [ ] Grafana 禁止 anonymous，admin credential 只从未跟踪 env/secret 注入；tracked files 无真实 secret。
- [ ] Collector 不使用 logging/debug exporter 输出业务 telemetry；metrics pipeline 不采样。

## 4. Dashboard And Local Alert Rules

- [ ] Provision Tempo/Prometheus datasources 与 RAG overview dashboard。
- [ ] Dashboard 覆盖 traffic、outcomes、in-flight、stage latency、provider/fallback、actual token coverage、Collector/export 状态与 trace 查询入口。
- [ ] Prometheus rules 覆盖 ask error ratio（10m、>5%、minimum 20）、provider fallback ratio（15m、>10%、minimum 20）与 Collector receive/export/drop failure。
- [ ] Rule annotations 明确 `non_sla_reference`；不创建 latency SLA rule，不部署 Alertmanager/外部 notification。
- [ ] 验证 dashboard variables、legend 与 alert labels 不包含 lineage、user、document、model 自由文本或其他高基数值。

## 5. Verification And Synthetic Smoke

- [ ] 运行 common/admin/core 聚焦 tests，记录 suites/tests/failures/errors/skips。
- [ ] 运行 `mvn -q test`。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`，确认评测工具无回归。
- [ ] 运行 compose render、Collector config validation、Prometheus rule check、dashboard/datasource JSON/YAML parse。
- [ ] 启动本机 reference stack，只发送 synthetic spans/metrics；验证 Tempo/Prometheus/Grafana 查询闭环与 sampling/retention/access boundary。
- [ ] 停止/恢复 Collector，验证 exporter fail-open、bounded drop 与后续恢复；不调用业务 provider。
- [ ] 对 exported traces/metrics 执行 sensitive sentinel 与 cardinality 检查；运行 SensitiveLogs、secret/absolute-path、localhost port、protected path、Markdown links 与 `git diff --check`。
- [ ] 前端无改动时正式 build 记为 `SKIPPED`；真实 embedding/rerank/ask/generation/judge/LLM/provider/SaaS 调用与公网 telemetry 出站保持 0。

## 6. Acceptance And Closeout

- [ ] 用户验收实现、依赖/镜像清单、synthetic evidence、sampling/retention/access 与剩余风险。
- [ ] 原文接受 4 requirements / 12 scenarios delta 到 `openspec/specs/rag-system/spec.md`。
- [ ] 同步 `openspec/project.md`、`docs/architecture/overview.md`、`docs/roadmap/technical-debt.md` 与 `docs/optimization/README.md`。
- [ ] 归档 change，恢复 `.ai/ACTIVE_TASK.md=IDLE`，验证 archive structure 与 baseline exact match。

