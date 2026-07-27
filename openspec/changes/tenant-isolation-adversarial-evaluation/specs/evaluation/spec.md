# Evaluation Delta: C14 Tenant Isolation Adversarial Evaluation

## ADDED Requirements

### Requirement: Versioned Tenant Isolation Adversarial Release

系统 SHALL 为 C14 维护独立、版本化的 tenant isolation adversarial release。manifest SHALL 固定 case schema、case artifact、ordered case IDs、case/category/channel distribution、synthetic fixture identity、driver/profile version、timing policy 与 artifact bytes/SHA-256。validator MUST 只使用本地 artifact，并在启动 container、登录 backend 或调用 provider 前 fail fast。

case SHALL 使用稳定 ID 和有限枚举声明 actor、target、driver、mutation、matched control、expected response/state invariants 与 required status。绝对路径、父目录逃逸、重复 ID、未知 driver、缺失 control、hash/order/count/quota drift 或 schema 之外字段 MUST 使 release `INVALID`；不得降级为 unversioned formal evidence。

#### Scenario: 固定 Release 通过本地验证

- GIVEN v1 manifest、schema、cases、fixture descriptor 和 driver registry 均存在
- WHEN validator 在零 backend/provider 调用下验证 release
- THEN artifact path/hash/bytes、ordered IDs、count/distribution 与 profile identity 全部 exact match
- AND release identity 可写入后续 details 与 summary

#### Scenario: Case 或 Driver 身份漂移

- GIVEN case 内容、顺序、数量、schema、fixture 或 driver version 任一漂移
- WHEN 尝试形成正式 C14 evidence
- THEN 在容器、backend、数据库、Redis、Milvus 与 provider 调用前以稳定 category 失败
- AND 不复用旧 manifest、旧 report 或成功子集继续执行

#### Scenario: 非法 Case Contract

- GIVEN case 使用 duplicate ID、unknown driver/enum、不安全路径、缺 required invariant 或 timing case 缺 matched control
- WHEN validator 解析 adversarial corpus
- THEN release 标记为 `INVALID`
- AND details 不复制原始攻击 payload、credential 或环境绝对路径

### Requirement: Isolated Dual-Tenant Synthetic Evaluation Fixture

C14 正式执行 SHALL 使用测试进程创建并独占的 MySQL、Redis、Milvus、临时 durable input 和随机本机 application endpoint。系统 SHALL 通过 test-only fixture 建立 tenant A/B、用户、private/public KB、permission、document/chunk、history/feedback、task、cache 与 vector canary；MUST NOT 开放 production tenant CRUD/switch 或复用用户常驻服务与真实数据。

Embedding 与 generation SHALL 使用 test-scope deterministic stub，真实 embedding、rerank、generation、judge、LLM/provider calls MUST 为 0。Docker 或任一必需基础设施不可用、fixture identity 不完整或 container ownership 不可证明时，global result MUST NOT 为 `PASS`。

#### Scenario: 隔离 Fixture 完整执行

- GIVEN C14 profile 创建自己拥有的 MySQL、Redis、Milvus、network、volume 与 temp path
- WHEN fixture 建立两个 synthetic tenant 和 collision/canary 数据
- THEN 所有 case 使用当前 Flyway schema 与正式 scope boundary 执行
- AND 用户常驻容器、数据库、collection、volume 与业务文件不被枚举、停止、修改或清理

#### Scenario: Deterministic Sync And Stream

- GIVEN sync/SSE case 需要走完整 retrieval-to-output 链
- WHEN test-scope deterministic embedding/generation stub 执行
- THEN 输出只由当前 tenant pipeline 返回的 contexts 决定
- AND provider/model calls、费用、限流事件与业务数据出站均为 0

#### Scenario: 基础设施不可评

- GIVEN Docker、必需 image、container health、时钟或 fixture completeness 不满足 profile
- WHEN 运行正式 C14 evaluation
- THEN result 为 `NOT_EVALUABLE` 或 `INVALID`，取决于环境还是 identity 问题
- AND skip/mock/single-tenant 结果不得转换为 `PASS`

### Requirement: Surface-Complete Isolation And Malicious Case Invariants

C14 SHALL 覆盖 identity override、resource ID guessing、tenant-local public/permission、reserved filter、cache/idempotency、task/recovery、durable input、Milvus vector、keyword fallback、debug retrieval、sync ask、SSE、history/feedback。每个 required case SHALL 同时验证传输结果、稳定错误、foreign canary 不可见和 tenant B 的 SQL/Redis/vector/task/file 后置状态不变；仅状态码、空结果或 unit/mock MUST NOT 构成 case PASS。

恶意输入 SHALL 只覆盖可能选择、扩大或泄漏 tenant scope 的 header/query/body/cookie/metadata/filter/ID/key/recovery 变体。C14 MUST NOT 以少量 case 宣称通用 prompt injection、越狱、模型安全、DoS 或生产 penetration testing 已完成。

#### Scenario: 组合攻击不泄漏或修改 Foreign Tenant

- GIVEN tenant A actor 知道或猜测 tenant B 的资源 ID、key pattern、vector ID 或 canary term
- WHEN case 组合 selector override、public/permission、reserved filter、cache/task/vector/stream/recovery 路径
- THEN response、contexts、citations、metadata、SSE、history 与 report 均不含 tenant B canary/identity
- AND tenant B 的 SQL、Redis、Milvus、task 与 durable input 状态前后不变

#### Scenario: Vector 故障转 Keyword 路径

- GIVEN tenant A/B 都有相同关键词且 tenant B 内容含唯一 canary
- WHEN Milvus 不可用使受测请求进入 keyword-only fallback
- THEN fallback 保持 tenant A 的 immutable scope
- AND sync/debug/SSE/history 均看不到 tenant B canary

#### Scenario: C13b Contract Breach 被发现

- GIVEN 固定 adversarial case 证明当前实现违反已接受 tenant enforcement contract
- WHEN C14 进入修复
- THEN case identity 与 expected invariant 保持不变并先作为稳定 RED evidence
- AND 只允许最小 contract-preserving 修复；新 API/schema/权限/adapter 语义必须返回 OpenSpec 事前闸门

### Requirement: Error And Coarse Timing Disclosure Evidence

foreign-resource case SHALL 与同 route、同 actor、同 request shape 的 nonexistent control 比较 HTTP status、稳定 error code、response schema fingerprint 与敏感字段。两者 MUST NOT 通过 foreign tenant/resource identity、owner/name、canary、内部 mapper/vector/cache detail 或不同错误类别暴露资源存在性；普通日志与 report 亦 SHALL 遵守同一禁止项。

C14 SHALL 使用 manifest 预注册的本机 synthetic timing profile：固定 warmup、minimum matched pairs、interleaving seed、median/P95 delta threshold 和 completeness rule。timing profile 只用于发现粗粒度可重复 oracle；高噪声、样本不足、错误或 identity drift SHALL 为 `NOT_EVALUABLE/INVALID`，不得通过事后调整阈值或挑选 run 获得 PASS。

#### Scenario: Foreign 与 Nonexistent Fingerprint 一致

- GIVEN tenant A 请求 tenant B resource 和同 route nonexistent control
- WHEN 比较 status、stable error code、schema fingerprint 与 forbidden fields
- THEN 两者的存在性边界一致
- AND response/log/report 不含 foreign identity、canary、token、credential 或原始内部错误

#### Scenario: Coarse Timing Profile 完整

- GIVEN warmup、matched pair count、seed、clock 与 container health 满足固定 profile
- WHEN 随机交错测量 foreign/control 的 route duration
- THEN median 与 P95 absolute delta 都不超过预注册阈值时 timing channel 才可 `PASS`
- AND 报告明确该结果不代表生产网络或所有 timing side-channel 已消除

#### Scenario: Timing Evidence 不完整

- GIVEN pair 缺失、请求错误、资源抖动、时钟异常或 profile identity 漂移
- WHEN evaluator 聚合 timing channel
- THEN channel 为 `NOT_EVALUABLE` 或 `INVALID`
- AND functional 成功子集不得把 global status 提升为 `PASS`

### Requirement: C14 Status, Evidence And Claim Boundary

C14 report SHALL 分离 `functionalIsolationStatus`、`contentDisclosureStatus`、`errorDisclosureStatus`、`timingDisclosureStatus` 与 global `Report status`。通道 SHALL 使用 `PASS/FAIL/NOT_EVALUABLE/INVALID`；只有全部 required cases 完整、无 missing/error/skip 且四通道都 `PASS`，global status 才 SHALL 为 `PASS`。退出码 SHALL 固定为 `PASS=0`、`INVALID=2`、`FAIL=3`、`NOT_EVALUABLE=4`。

Markdown summary 与 machine-readable details SHALL exact 对齐 release/profile/Git HEAD、image versions、case totals、channel status、errors、skips、timing aggregate 与 provider calls=0，并采用 no-overwrite。`PARTIAL`、`RETRIEVAL_ONLY`、mock-only、single-tenant 或缺 timing evidence MUST NOT 被解释为 C14 完成。

#### Scenario: C14 完整 PASS

- GIVEN versioned release、isolated fixture、全部 required cases 和四个 evidence channel 完整
- WHEN evaluator 校验 report identity、case coverage、invariants、errors/skips 与 timing profile
- THEN global `Report status=PASS` 且退出码为 0
- AND summary 只声明 Milvus 支持配置和固定 synthetic attack matrix 下的隔离 evidence

#### Scenario: 成功子集掩盖 Missing 或 Failure

- GIVEN 部分 case 成功但存在 missing、duplicate、unexpected、error、required skip、channel failure 或 timing 不可评
- WHEN evaluator 聚合 global status
- THEN 按失败类型输出 `FAIL/NOT_EVALUABLE/INVALID` 和非零退出码
- AND 不使用成功率、平均值或已有 C13b unit tests 抵消缺口

#### Scenario: Evidence 输出安全

- GIVEN case runtime 内存中存在 synthetic token、foreign canary、请求与响应
- WHEN 写入 details、summary、console 或普通日志
- THEN 只输出 case ID、bounded taxonomy、hash/计数、聚合 timing 与必要版本事实
- AND 不落盘 token、credential、完整 body、原始 content、用户路径或可复用 secret
