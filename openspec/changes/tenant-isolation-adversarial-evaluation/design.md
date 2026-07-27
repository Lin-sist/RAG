# Design: C14 Tenant Isolation Adversarial Evaluation

## 1. Context

C13b 已把服务端 `RequestIdentity` 和 tenant scope 落到主要数据面，并接受 6 requirements / 18 scenarios。现有证据证明各实现切片具备 enforcement，但还缺少一个固定攻击语料驱动的端到端门禁，尤其缺少组合攻击、SSE、跨存储副作用、错误存在性 oracle 和可解释的 timing 证据。

C14 的职责是评测既有隔离 contract，而不是发明新的租户管理能力。实现中若发现 breach，只允许先用固定 adversarial case 复现，再做最小 contract-preserving 修复。

## 2. Artifact Layout And Identity

计划新增：

```text
docs/eval/isolation/
├─ tenant-isolation-adversarial-v1-manifest.json
└─ tenant-isolation-adversarial-v1-cases.jsonl
docs/eval/schema/
└─ tenant-isolation-case-v1.json
scripts/
├─ tenant_isolation_eval_contract.py
├─ evaluate_tenant_isolation.py
├─ test_tenant_isolation_eval_contract.py
└─ test_evaluate_tenant_isolation.py
```

manifest 至少固定：

- release/schema/driver/profile version；
- case/schema artifact 的 repo-relative path、bytes、SHA-256；
- ordered case IDs hash、总数与 category/channel/required 配额；
- synthetic fixture descriptor identity；
- timing profile（warmup、pair count、seed、threshold）；
- evidence policy 与允许的 status/exit code。

validator 只用 Python 标准库。它在 Docker、backend、数据库、Redis、Milvus 或 provider 调用前执行；绝对路径、`..`、hash/count/order drift、duplicate case ID、unknown enum/driver、缺 matched control 或 quota mismatch 都返回稳定 invalid category。

## 3. Case Model

每条 case 只描述攻击意图和可审计不变量，不写 Java class/method：

- `id`：稳定 ID；
- `category`：`identity_override`、`id_guessing`、`public_permission`、`reserved_filter`、`cache_idempotency`、`task_recovery`、`vector_keyword`、`rag_sync`、`rag_stream`、`history_feedback`、`error_disclosure`、`timing_disclosure`；
- `driver`：有限白名单 driver ID；
- `actor` / `target`：fixture 中的逻辑身份，不保存密码/token；
- `mutation`：header/query/body/metadata/filter/ID/key/outage/recovery 等有限操作；
- `controlCaseId`：timing/error 需要的同 route nonexistent control；
- `expected`：status/error fingerprint、forbidden canary classes、允许的 response keys 与后置 invariant；
- `required`：是否影响 global PASS。

case 不包含真实 host、用户名、密码、API key 或用户目录绝对路径。fixture 在测试进程中解析逻辑 actor，凭据只存在于 test runtime memory。

## 4. Synthetic Dual-Tenant Fixture

专用 `c14-isolation-eval` Failsafe profile 复用已声明的 Testcontainers：

- MySQL：执行当前 Flyway 后，以 test-only SQL 建立 tenant A/B、owner/reader、private/public KB、permission、document/chunk、history/feedback 与 task rows；
- Redis：为 A/B 制造相同 query/content/idempotency/task pattern，但使用 tenant v2 namespace；
- Milvus：建立同一物理 collection 下的两个 `TenantVectorScope`，包含相同和不同 vector ID、唯一 canary content；
- RAG：使用 test-scope deterministic embedding 和 deterministic generation stub，后者只回显受测 pipeline 返回的 allowed contexts，用于让 sync/SSE 泄漏可观察；
- durable input/recovery：使用测试临时目录和 tenant-scoped ledger/projection，不读取 workspace 外业务文件。

所有 fixture 都是纯合成数据。profile 必须连接自己创建的 container ID、network、volume 与临时目录；Docker 不可用时专用命令失败或 `NOT_EVALUABLE`，不得 skip 后声称通过。

## 5. Driver Architecture

runner 分成三类 driver，但共享一个 manifest、case set 与 report：

1. **HTTP/SSE driver**：通过随机本机端口访问真实 Spring Security/controller/filter/serializer 链，覆盖登录后 selector override、资源 ID、permission/public、sync ask、debug retrieval、SSE、history/feedback 和 task API。
2. **Infrastructure driver**：通过应用正式 service/adapter boundary 触发 Redis cache/idempotency、Milvus search/get/delete/count/drop 与 keyword route；不得直接绕开正式 scope object 构造“必然通过”的假测试。
3. **Recovery driver**：模拟 Redis projection miss、claim/recovery/finalize 与 durable input 重建，验证 tenant 从 ledger 恢复且 foreign state 不变。

driver registry 必须版本化且白名单化；case 文件不能通过反射指定任意 class/method 或文件路径。

## 6. Attack Matrix

### 6.1 Identity And Resource Enumeration

- header/query/body/cookie/metadata 中的 tenant selector 与大小写/命名别名；
- 缺失、错误类型、非正数或被篡改 tenant claim；
- KB/document/history/feedback/task 的 foreign ID、nonexistent ID 与 same-tenant forbidden control；
- list/statistics/delete/cancel/result 等读写入口均需覆盖，不能只测 detail GET。

### 6.2 Public, Permission And Reserved Filters

- tenant B 的 public KB 对 tenant A 仍不可见；
- tenant B permission 不能授予或扩大 tenant A scope；
- reserved tenant/KB/collection aliases、case-fold、snake/camel/hyphen 与表达式注入被稳定拒绝；
- 普通 filter 只能收窄 tenant A 结果。

### 6.3 Cache, Task, Recovery And Storage

- A/B 使用相同 query hash、content hash、idempotency key 和 task ID pattern 时不互相命中、覆盖、清理或重建；
- projection miss 后只从同 tenant durable ledger 回源；
- claim/heartbeat/phase/finalize/input cleanup 全程保持同一 tenant；
- mismatch/null/cross-parent 进入稳定 fail-closed terminal path，不继续 vector/SQL mutation。

### 6.4 Vector, Keyword, Sync And Stream

- 同物理 collection 下验证 search/get/getByIds/delete/count/drop 与 marker mismatch；
- vector 故障转 keyword-only 时仍使用相同 tenant scope；
- debug retrieval、sync response 的 contexts/citations/metadata、SSE chunk 和最终 history 均不得包含 tenant B canary；
- deterministic stub 只用于使泄漏显性化，不计算生成、citation、judge 或 no-answer 质量指标。

## 7. Response And State Invariants

每条 required case 至少验证：

1. 传输结果：HTTP status/SSE terminal state 符合期望；
2. 稳定错误：error code/schema 与 matched control 一致，不回显 foreign ID/name/tenant/canary；
3. 内容保密：response、contexts、citations、metadata、stream、history 与 report 无 foreign canary；
4. 完整性：tenant B 的 SQL rows、Redis values、Milvus count/content、task phase 和 durable input hash 前后不变；
5. 审计安全：普通日志与 details 只含 case ID、bounded category/status，不含 token、凭据、完整请求/响应或原文。

只验证状态码、只验证空结果或只验证 service mock 均不足以使 case `PASS`。

## 8. Timing Disclosure Profile

timing 仅验证本机 synthetic 环境中“foreign resource”与“matched nonexistent resource”是否形成粗粒度、可重复的 route-level oracle：

- 每个 timing group 先 warmup 10 对请求，再测量 40 对；
- 使用固定 seed 随机交错 foreign/control 顺序，记录单调时钟 duration；
- status、error code、response schema fingerprint 必须先完全一致；
- median absolute delta 阈值为 `max(10ms, 50% × control median)`；P95 absolute delta 阈值为 `max(25ms, 50% × control P95)`；任一超限使该 group `FAIL`；
- pair 数不足、错误率非零、容器资源明显抖动、时钟异常或 profile identity 漂移使通道 `NOT_EVALUABLE`；不得通过重复运行或改阈值挑选 PASS。

该阈值只发现粗粒度本机 oracle。即使通过，也不能证明网关、跨区域网络、生产数据库、真实负载或共享基础设施不存在 timing side-channel。

## 9. Status And Report Semantics

独立通道：

- `functionalIsolationStatus`
- `contentDisclosureStatus`
- `errorDisclosureStatus`
- `timingDisclosureStatus`

每个通道值为 `PASS / FAIL / NOT_EVALUABLE / INVALID`。global `Report status` 规则：

- 所有 required cases 完整、四通道均 `PASS` → `PASS`；
- 任一已执行 invariant 失败 → `FAIL`；
- corpus/profile 合法但依赖不可用、timing 不可评或 required evidence 缺失 → `NOT_EVALUABLE`；
- manifest/schema/hash/driver/report identity 无效 → `INVALID`。

退出码固定为 `PASS=0`、`INVALID=2`、`FAIL=3`、`NOT_EVALUABLE=4`。Markdown 和 details JSON 必须同时记录 release、driver/profile version、Git HEAD、image versions、case totals、channel status、error taxonomy、timing aggregate、skips 和 provider calls=0；raw token/body/content 不落盘。`--no-overwrite` 为正式 evidence 必需。

## 10. Gap Handling

若 case 失败：

1. 保留原 case 与 identity，不根据 observed output 改期望；
2. 先确认是 harness/fixture 无效、环境 `NOT_EVALUABLE`，还是生产实现 contract breach；
3. breach 必须先有稳定 RED，再做最小修复并运行相邻数据面回归；
4. 若修复需要新 API/DTO/schema/权限语义、依赖升级或新增 adapter 支持，暂停并回到 OpenSpec 事前闸门，不能在 C14 偷渡；
5. 报告保留失败类别和修复后的新 run identity，不覆盖旧 evidence。

## 11. Verification

- Python：contract/evaluator unit tests + `python -B -m unittest discover -s scripts -p 'test_*.py'`；
- Java：case driver 聚焦 tests、`mvn -q -pl rag-admin -am -Pc14-isolation-eval verify`、`mvn -q test`；
- 现有 Milvus/Redis/MySQL suites：按 C14 触及面聚焦复跑，不能用 mock 代替容器 evidence；
- 静态：SensitiveLogs、credentials、absolute paths、protected paths、未知 driver、未版本化 case、raw token/body/canary report、Markdown links、`git diff --check`；
- 前端：无改动则 `SKIPPED`，有改动必须运行包含 `vue-tsc` 的正式 build；
- 外调：embedding/rerank/generation/judge/provider calls 必须为 0，真实 Milvus maintenance 必须 `SKIPPED`。

## 12. Rollout And Claim Boundary

1. 先提交并审阅 OpenSpec 规划，不写实现。
2. 批准后按 corpus contract → harness → functional matrix → RAG/vector/stream → disclosure/timing → report/full gates 的顺序做 TDD。
3. C14 不修改生产默认开关，不开放 tenant CRUD/switch，也不执行真实 shadow migration。
4. 验收时把两个 delta body 分别接受进 `evaluation` 与 `rag-system` baseline，再归档 change 并恢复 `ACTIVE_TASK=IDLE`。
5. C14 PASS 只支持“Milvus 支持配置 + 固定 synthetic attack matrix 下通过”的结论。生产级多租户、全 adapter、真实数据迁移、合规和任意网络环境 timing 安全仍需后续独立证据。

## 决策记录

### 决策 1：C14 使用现有普通 RAG 评测集还是独立 adversarial release
- **面临的选择**：扩展 150 条质量评测集；建立独立 tenant isolation release；只写 JUnit 方法不做数据 release。
- **选了哪个 + 为什么**：选择独立 versioned release，因为隔离 case 的 actor、mutation、control 和后置条件与问答质量样本不是同一 schema，也必须独立冻结身份。
- **放弃的代价**：混入质量集会污染既有 baseline 可比性；只写 JUnit 会缺少 case identity、配额、review 和报告复现能力。

### 决策 2：双 tenant 从哪里创建
- **面临的选择**：开放 production tenant CRUD；复用真实数据库 tenant；用 Testcontainers 内 test-only SQL fixture。
- **选了哪个 + 为什么**：选择 test-only SQL fixture，能覆盖真实 schema/SQL，又不在 C14 前开放业务能力或触碰用户数据。
- **放弃的代价**：开放 CRUD 会把评测 change 扩成产品能力；复用真实库会带来数据破坏、凭据与隐私风险。

### 决策 3：是否调用真实模型验证 sync/SSE
- **面临的选择**：调用真实 embedding/LLM；只测 retrieval 不测 stream；使用 test-scope deterministic embedding/generation stub。
- **选了哪个 + 为什么**：选择 deterministic stub，让 scope 泄漏在 contexts、citations、输出和 history 中可观察，同时保持零外调、可复现和零费用。
- **放弃的代价**：真实模型会引入数据出站、费用和不可重复输出；只测 retrieval 会遗漏 sync/SSE/history 传播链。

### 决策 4：runner 只走 HTTP 还是同时覆盖内部基础设施边界
- **面临的选择**：全量 HTTP 黑盒；全量直接 service/adapter；HTTP/SSE 与 infrastructure/recovery driver 组合。
- **选了哪个 + 为什么**：选择组合 driver，因为用户入口需要真实安全链，cache/vector/recovery 的跨 tenant 副作用又无法全部由公开 API 精确触发。
- **放弃的代价**：只走 HTTP 会漏掉内部恢复和 destructive adapter 操作；只走 service 会绕过认证、filter、serializer 和 SSE。

### 决策 5：case 只断言响应还是同时断言后置状态
- **面临的选择**：只看 404/空结果；只扫 foreign canary；同时检查响应、内容和 SQL/Redis/Milvus/task 后置状态。
- **选了哪个 + 为什么**：选择多层 invariant，因为删除、清理、cancel、evict 等攻击可能返回安全错误却已经产生跨 tenant 副作用。
- **放弃的代价**：只看状态码会漏 silent corruption；只看 canary 会漏无内容的完整性破坏。

### 决策 6：跨 tenant 与不存在的错误边界如何比较
- **面临的选择**：只要求都为 404；比较完整 raw body；比较稳定 status/error/schema fingerprint 并禁止敏感字段。
- **选了哪个 + 为什么**：选择稳定 fingerprint，既能发现存在性差异，又避免把时间戳、traceId 等非安全字段造成的字节差异当成失败。
- **放弃的代价**：只看 404 会漏 error code/schema 泄漏；比较 raw bytes 会被合法动态字段制造高噪声。

### 决策 7：timing disclosure 是硬门禁还是只记录诊断
- **面临的选择**：完全不测；只记录不阻断；用预注册 coarse profile 作为必需通道，证据不足时 `NOT_EVALUABLE`。
- **选了哪个 + 为什么**：选择必需 coarse 通道，因为 C13b 明确把 timing disclosure 交给 C14；同时用 `NOT_EVALUABLE` 防止噪声环境伪装 PASS。
- **放弃的代价**：不测或只记录无法满足 handoff；把一次延迟差直接判安全或不安全会造成严重误报。

### 决策 8：timing 阈值何时确定
- **面临的选择**：运行后按结果调阈值；不设阈值只给图；在 planning 中预注册固定 warmup/pairs/seed 与粗阈值。
- **选了哪个 + 为什么**：选择事前预注册，避免为了让当前实现通过而调整口径，并让后续 evidence 可复现。
- **放弃的代价**：事后调参会污染结论；没有阈值只能形成观察，不能形成门禁。

### 决策 9：timing profile 采用什么粒度
- **面临的选择**：微基准纳秒级比较；生产网络压测；本机 route-level matched-pair median/P95 粗门禁。
- **选了哪个 + 为什么**：选择本机 matched-pair 粗门禁，它与当前工程原型和隔离 Testcontainers 证据范围匹配，能发现明显 oracle 而不冒充生产证明。
- **放弃的代价**：微基准不能代表完整请求路径；生产压测需要真实拓扑、容量和单独授权，超出 C14。

### 决策 10：C14 发现 C13b bug 后是否允许修复
- **面临的选择**：只报告不修；在 C14 内做既有 contract 的最小修复；借机重做租户架构。
- **选了哪个 + 为什么**：选择先 RED 后最小 contract-preserving 修复，使 C14 能形成闭环，又不改变已接受语义。
- **放弃的代价**：只报告会让阶段无法通过；架构重做会扩大范围并让评测与新设计纠缠。

### 决策 11：C14 是否为 Qdrant/Elasticsearch 产生隔离结论
- **面临的选择**：用 Milvus 结果代替三个 adapter；在 C14 补齐三个 adapter；只评估当前受支持 Milvus profile，其他 adapter 继续 fail startup。
- **选了哪个 + 为什么**：选择 Milvus-only supported profile，因为 C13b 已明确其他 adapter 无完整 contract 时不能启用。
- **放弃的代价**：外推会伪造证据；同 change 补齐 adapter 会引入高风险实现与依赖范围。

### 决策 12：报告是否保存原始请求响应
- **面临的选择**：保存完整 raw payload 便于调试；完全不保存 details；只保存 case ID、bounded taxonomy、hash/计数和聚合。
- **选了哪个 + 为什么**：选择脱敏结构化 details，在保留复现线索的同时避免报告本身成为 token、canary 或 foreign content 泄漏源。
- **放弃的代价**：raw payload 会扩大敏感数据面；完全无 details 无法审计 missing/error/timing completeness。

### 决策 13：C14 通过后允许什么结论
- **面临的选择**：直接宣称生产级多租户完成；只说评测工具可用；声明受支持配置和固定攻击矩阵下证据通过，并保留生产边界。
- **选了哪个 + 为什么**：选择受限结论，既反映 C14 的实际证据，又不越过真实迁移、全 adapter、网络拓扑、合规与容量未知项。
- **放弃的代价**：生产级宣称会过度外推；只说工具可用又无法表达完整 PASS 的价值。

### 决策 14：C14 是否自动开放第二业务 tenant、C15 或 C16
- **面临的选择**：PASS 后自动启用；在同 change 增加 tenant management；只解除证据前置条件，启用能力另立 change。
- **选了哪个 + 为什么**：选择只解除前置条件，因为 tenant CRUD/switch、MCP 和 Router 都有独立 API、权限和运行风险。
- **放弃的代价**：自动启用会把测试结论变成未经审查的生产状态变更；同 change 实现会混合多个重大能力。

### 决策 15：真实 Milvus shadow migration 是否纳入 C14
- **面临的选择**：先对真实 collection 写入并切换；只做只读盘点；C14 完全使用 synthetic collection，真实 maintenance 继续单独授权。
- **选了哪个 + 为什么**：选择 synthetic collection，C14 的目标是攻击矩阵证据，真实迁移涉及业务数据、容量、额外空间和回滚窗口。
- **放弃的代价**：真实写入会产生不可逆风险并超出当前授权；只读盘点也不能证明 shadow copy/switch 成功，不能作为 C14 完成条件。

### 决策 16：通用 prompt injection 是否属于“恶意样本”
- **面临的选择**：把所有 LLM 安全一次纳入；加入少量 prompt injection 并宣称安全；只覆盖可能扩大 tenant scope 或泄漏 foreign canary 的输入攻击。
- **选了哪个 + 为什么**：选择 tenant-isolation 攻击面，保持 C14 与 C13 handoff 一致；通用模型安全需要独立 threat model、provider 和评测契约。
- **放弃的代价**：全量纳入会把阶段扩大成无限安全项目；少量样本会制造“已防 prompt injection”的误导。
