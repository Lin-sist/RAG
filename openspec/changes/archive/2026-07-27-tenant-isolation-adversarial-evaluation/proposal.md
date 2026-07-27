# Proposal: C14 Tenant Isolation Adversarial Evaluation

## Why

C13a 已建立服务端 tenant identity，C13b 已把 tenant scope 贯穿 SQL/API/permission、task/cache/history/feedback、RAG/keyword 与已验证的 Milvus adapter，并用双 tenant integration tests 证明指定数据面的 enforcement。但这些证据仍主要按实现切片组织：它们没有形成一个版本化、可重复、面向攻击路径的端到端评测，也没有统一回答错误响应、流式输出、缓存碰撞、恢复流程和粗粒度 timing oracle 是否会泄露另一个 tenant 的存在或内容。

因此项目当前只能描述为“C13b data-plane enforcement 已实现并通过指定测试”，不能把现有 unit/mock、单接口 happy path 或 retrieval-only 结果外推成受测范围内的完整租户隔离结论。C14 将建立独立的隔离与恶意样本评测门禁，用两个纯合成 tenant、固定攻击语料和隔离基础设施复核 C13b 的所有高风险入口。

## 用户故事：改前坏事 → 改后不同

- 改前坏事：测试分别证明 mapper、Redis key 或 Milvus filter 带了 tenantId，但无法证明攻击者组合伪造 header、猜测 ID、reserved filter 和流式请求时仍拿不到 tenant B 数据。
- 改后不同：固定的 adversarial corpus 会以 tenant A 身份对 tenant B 的 KB、document、history、feedback、task、cache、keyword、vector、sync ask 和 SSE 逐类攻击，并同时验证响应、持久化副作用和 canary 泄漏。
- 改前坏事：跨 tenant 404 与真实不存在 404 可能在 error body、字段、日志或耗时上形成存在性 oracle，却没有统一、可复现的证据口径。
- 改后不同：评测将 foreign-resource 与 matched nonexistent control 做响应 fingerprint 和预注册的本机粗粒度 timing 比较；证据不足时明确 `NOT_EVALUABLE`，不把噪声环境包装成通过。
- 改前坏事：某次手工 smoke 即使通过，也可能因为 corpus、fixture、driver 或 Git HEAD 漂移而无法复现。
- 改后不同：manifest 固定 case schema、case 顺序、fixture identity、driver version 和 hash；runner 在任何容器、backend 或 provider 调用前先本地 fail fast。

## Goals

1. 建立版本化 `tenant-isolation-adversarial-v1` manifest、case schema、JSONL case corpus 与纯标准库 validator，固定样本身份、类别配额、fixture/driver identity 和 artifact hash。
2. 新增独立 `c14-isolation-eval` Maven/Failsafe 入口，复用现有 Testcontainers MySQL、Redis、Milvus 与 test-scope deterministic embedding/generation stub；通过 test-only SQL fixture 建立两个 tenant，不开放 tenant CRUD/switch。
3. 覆盖 identity override、ID guessing、tenant-local public/permission、reserved filter、cache/idempotency、task/recovery、vector/keyword、sync ask、SSE、history/feedback、error disclosure 与 timing disclosure。
4. 为每个 case 同时校验 HTTP/stream 结果、稳定错误边界、tenant B canary 不可见、tenant B 状态不变，以及必要的 SQL/Redis/Milvus 后置条件；只看状态码不得算通过。
5. 分离 `functionalIsolationStatus`、`contentDisclosureStatus`、`errorDisclosureStatus`、`timingDisclosureStatus` 与全局 `Report status`；任一必需通道缺失、错误或不完整时 fail closed。
6. 生成脱敏、不可覆盖的机器可读 details 与 Markdown summary，记录 corpus identity、Git HEAD、基础设施镜像、case 数、attempts、错误类别、skips 和真实 provider 调用量。
7. 若评测发现违反已接受 C13b contract 的实现缺口，以该 adversarial case 先 RED，再做最小 contract-preserving 修复；不得借 C14 改写租户模型、权限语义或 RAG 质量口径。

## Non-Goals

- 不提供 tenant CRUD、邀请、membership、多 tenant switch、tenant selector、租户级 RBAC、跨租户管理员、SSO/provisioning、计费或前端管理界面。
- 不执行真实 Milvus shadow copy、mapping/readiness 切换、重试或清理；真实 maintenance 仍需单独披露 collection/record 数与风险并取得授权。
- 不为 Qdrant/Elasticsearch 补 tenant adapter；它们在 enforcement mode 下继续 fail startup，不能由 Milvus evidence 代替。
- 不评测通用 prompt injection、越狱、模型安全、答案质量、semantic faithfulness、reranker 收益或生产数据分布；恶意样本只服务于 tenant isolation 攻击面。
- 不调用真实 embedding、rerank、ask/generation、judge、LLM 或外部 provider，不发送真实知识库、用户或凭据数据。
- 不做互联网暴露的 penetration test、容量/DoS、密码学审计、合规认证、生产 SLA 或“所有 timing side-channel 已消除”的证明。
- 不自动开放第二业务 tenant、tenant management、C15 MCP 或 C16 Router；C14 通过只解除证据前置条件，后续启用仍需独立变更与授权。
- 不新增或升级依赖，不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。

## Capability Classification

- `confirmed`：C13a/C13b 已接受进 `rag-system` baseline；MySQL/Redis/Milvus Testcontainers、双 tenant fixtures、deterministic embedding、sync/SSE/controller、task recovery 和 Python 标准库治理模式均有可复用入口。
- `partial`：现有测试已覆盖多项跨 tenant 行为，但分散在 unit、service、controller 与 adapter suites；没有统一 adversarial release、端到端 case matrix、报告状态或 timing evidence。
- `planned`：versioned corpus/manifest/schema、local validator、dedicated Failsafe profile、test-only dual-tenant harness、case drivers、sanitized evidence、coarse timing profile 和 C14 claim gate。
- `out_of_scope`：真实业务 tenant 开放、tenant management、真实 shadow migration、Qdrant/Elasticsearch tenant support、通用 LLM/prompt 安全、生产渗透测试、C15/C16。
- `unknown`：真实生产拓扑与 tenant 数量、真实 Milvus 数据规模、网关/CDN timing、跨区域网络、生产日志与 SIEM 权限、组织级攻击模型；C14 不得臆造这些环境事实。

## Proposed Contract

1. 正式 C14 evidence 必须绑定唯一、已验证的 adversarial release；manifest/case/schema/fixture/driver 任一漂移都在启动容器或调用 backend 前失败。
2. 评测只使用两个 test-only synthetic tenant 和可识别 canary；不得复用用户数据库、常驻 Redis/Milvus、真实凭据或真实业务内容。
3. 每个攻击 case 必须声明 actor、target、driver、mutation、control、expected response fingerprint、forbidden canary 与后置条件；未知 driver、重复 ID、缺 control 或不安全 artifact path 使 release invalid。
4. 跨 tenant 请求必须同时满足：不返回 foreign content/metadata/identity，不修改 foreign SQL/Redis/vector/task state，不通过 public/permission、reserved filter、cache collision、stream 或 recovery 绕过 scope。
5. foreign-resource 与 matched nonexistent control 的 HTTP status、稳定 error code、响应 schema 和敏感字段 fingerprint 必须一致；报告与普通日志不得包含 foreign tenant/resource identity、canary content、token 或 credential。
6. timing 只作为预注册本机 synthetic coarse-oracle gate：固定 warmup、pair count、顺序 seed 和阈值。高噪声、样本不足或基础设施漂移必须是 `NOT_EVALUABLE`；通过不得外推为生产 timing side-channel 证明。
7. functional/content/error/timing 四个通道都完整 `PASS`，且所有必需 case 无 missing/error/skip，global `Report status` 才能为 `PASS`。`PARTIAL`、`RETRIEVAL_ONLY`、mock-only 或单 tenant 结果不得作为 C14 完成证据。
8. C14 runner 的真实 provider 调用量必须为 0；deterministic generation stub 只用于让 sync/SSE 输出可检查，不形成生成质量结论。
9. C14 发现的 C13b gap 只能按既有 contract 做最小修复并由对应 case 回归；新 API/DTO/schema/权限语义或 adapter 支持必须另立或扩展经批准的 Type C change。
10. C14 通过后，项目 MAY 表述为“Milvus 支持配置和受测攻击矩阵下的租户隔离证据通过”；仍不得表述为生产级多租户、全 adapter、合规认证或任意部署环境下无侧信道。

## Impact

- 预计新增 `docs/eval/isolation/` 下的 v1 manifest/cases、`docs/eval/schema/tenant-isolation-case-v1.json`、本地 validator/evaluator 及其 Python tests。
- `rag-admin` 预计新增 `c14-isolation-eval` Failsafe profile、端到端 integration harness、test-only fixture/driver/report writer；复用已声明依赖，不新增 production endpoint。
- 若 case 暴露 C13b contract breach，可能对现有 Java 实现做小范围修复；任何修复必须在 tasks 与 `.ai/AGENT_LOG.md` 单独列明。
- 规划阶段只新增 OpenSpec artifacts、更新 `.ai/ACTIVE_TASK.md` 并追加 `.ai/AGENT_LOG.md`。

## Risks And Mitigations

- 风险：只测 HTTP，遗漏 Redis/vector/recovery 内部旁路。缓解：case driver 分 HTTP/SSE 与 direct infrastructure/recovery 两层，但共享同一 corpus identity 和 global status。
- 风险：case corpus 针对当前实现写死而形成“为测试定制”。缓解：case 按攻击不变量和公开/已接受 contract 编写，不把具体 mapper/class 名或 observed 成功结果写入期望。
- 风险：timing gate 在本机抖动。缓解：固定 warmup、随机交错 pair、minimum sample、matched route control 和预注册粗阈值；环境不稳定时 `NOT_EVALUABLE`，不反复调阈值追求 PASS。
- 风险：deterministic generator 掩盖真实 LLM 行为。缓解：C14 只检查 scope 泄漏，断言 contexts/citations/history/SSE 不出现 foreign canary；明确不评估真实生成质量或 prompt injection。
- 风险：details/report 反而复制攻击载荷或 foreign canary。缓解：只输出 case ID、bounded category/status/error taxonomy、hash/计数和延迟聚合，禁止保存 token、完整 body、原始内容或凭据。
- 风险：评测为了完成 C14 顺手开放第二 tenant。缓解：只允许 test-only SQL fixtures；production tenant CRUD/switch 和第二业务 tenant 继续 out of scope。

## Acceptance Evidence

- proposal、design 决策、tasks、`evaluation` 与 `rag-system` spec delta 先经用户批准。
- manifest/schema/case validator 对 valid release、hash/order/count drift、duplicate ID、unknown driver、missing control、unsafe path 与 quota mismatch 有 RED→GREEN tests。
- `c14-isolation-eval` 在隔离 MySQL/Redis/Milvus 上完成全部必需 case；报告 case count 与 manifest exact match，missing/error/skip 为 0。
- ID guessing、public/permission、reserved filter、cache/task/recovery、vector/keyword、sync/SSE/history/feedback 都有 tenant B canary 不可见且后置状态不变的证据。
- error fingerprint 与 matched nonexistent control 一致；timing profile 满足预注册 completeness/threshold，或以非 PASS 明确阻断收口。
- 运行 `mvn -q test`、C14 专用 Failsafe、Python unit tests、SensitiveLogs、protected paths、credential/absolute path、Markdown links、artifact no-overwrite 与 `git diff --check`。
- 前端无改动时正式 build 记为 `SKIPPED`；若任何前端/DTO 发生变化，则运行包含 `vue-tsc` 的正式 build。
- 报告明确真实 provider/model calls=0、业务数据出站=0、真实 Milvus maintenance=`SKIPPED`，并限制结论范围。

## Approval Gate

本轮只批准启动 C14 规划，不代表批准 Java/Python 实现、容器运行、任何真实 provider 调用或真实 Milvus maintenance。用户需审阅并确认：v1 case matrix、test-only 双 tenant fixture、deterministic generation stub、报告四通道状态、timing profile、C13b gap 的最小修复边界，以及 design 中的决策记录。提交责任保持 `用户手动提交`。
