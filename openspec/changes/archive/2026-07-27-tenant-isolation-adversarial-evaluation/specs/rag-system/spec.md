# RAG System Delta: C14 Tenant Isolation Evidence Gate

## ADDED Requirements

### Requirement: C14 Isolation Evidence Gate For Tenant Capability Claims

系统在 C13b data-plane enforcement 之后，仍 SHALL 通过独立 C14 adversarial evaluation 才能形成租户隔离完成结论。C14 evidence MUST 使用两个 synthetic tenant，覆盖 identity override、ID guessing、public/permission、reserved filter、cache/task/recovery、vector/keyword、sync/SSE/history/feedback、error 与 coarse timing disclosure，并同时验证 foreign content 不可见和 foreign state 不可修改。

在 C14 global `Report status` 不是完整 `PASS` 时，系统 MUST NOT 开放第二业务 tenant、tenant CRUD/switch、C15 MCP 或 C16 Router，也 MUST NOT 把 C13a identity、C13b unit/integration tests、mock、single-tenant happy path、`RETRIEVAL_ONLY` 或 `PARTIAL` evidence 描述为租户隔离成立。

#### Scenario: C13b 完成但 C14 非 PASS

- GIVEN C13b 已验收但 C14 为 `FAIL/NOT_EVALUABLE/INVALID` 或尚未执行
- WHEN 项目描述当前能力或决定是否开放后续 tenant/MCP/Router 能力
- THEN 只可声明 data-plane enforcement 已实现并通过指定测试
- AND 第二业务 tenant、tenant management、C15 与 C16 继续关闭

#### Scenario: C14 固定攻击矩阵通过

- GIVEN C14 versioned release、双 tenant synthetic fixture、全部 required cases 与 functional/content/error/timing channels 完整 PASS
- WHEN 形成阶段验收结论
- THEN 系统 MAY 声明 Milvus 支持配置和受测攻击矩阵下的租户隔离 evidence 通过
- AND 结论必须附带 Git/profile/release identity 与未覆盖边界

#### Scenario: C14 PASS 后请求自动开放能力

- GIVEN C14 evidence 已通过
- WHEN 请求直接启用 production tenant CRUD/switch、C15 MCP 或 C16 Router
- THEN 仍需独立 Type C change、权限契约、迁移/运行验证与明确授权
- AND C14 不自动改变生产默认开关、API surface 或部署状态

### Requirement: Supported Profile And Production Boundary After C14

C14 的隔离结论 SHALL 限定于当前已通过 tenant adapter contract 的 Milvus 配置、当前 SQL/Redis/task/RAG 路径和固定 synthetic attack matrix。Qdrant/Elasticsearch 在未通过等价 contract 与 C14 matrix 前 MUST 继续在 enforcement mode 下 fail startup；Milvus evidence MUST NOT 外推到未验证 adapter。

C14 PASS MUST NOT 被描述为真实 Milvus shadow migration 已执行、生产数据已验证、生产级多租户/合规认证、容量/DoS 安全、真实网关/跨区域 timing 安全或所有 side-channel 已消除。任何真实 collection copy/mapping/readiness switch、真实业务 tenant 启用或生产安全测试仍需单独披露范围、数据出站、超时/重试、回滚与风险并取得授权。

#### Scenario: 未验证 Adapter

- GIVEN C14 在 Milvus 支持配置下 PASS，但 Qdrant/Elasticsearch 没有等价 tenant contract/evidence
- WHEN 配置 enforcement mode 使用未验证 adapter
- THEN 应用继续 fail startup
- AND 不以 C14 Milvus report、接口兼容或空结果声称该 adapter 已隔离

#### Scenario: 真实 Shadow Migration 未授权

- GIVEN C14 使用 synthetic collection 完成 evaluation，但真实 Milvus collection 尚未 audit/copy/switch
- WHEN 形成 C14 结论或计划生产启用
- THEN 真实 maintenance 状态保持 `SKIPPED`，现有业务数据迁移不得写成已完成
- AND 任何真实写入、切换、重试或清理仍需单独授权

#### Scenario: 生产级安全声明

- GIVEN C14 本机 synthetic functional/error/timing evidence PASS
- WHEN 对外描述系统成熟度
- THEN 仍按工程原型表述，并列出真实拓扑、容量、合规、全 adapter、运维权限与网络侧信道未知项
- AND 不得使用“生产级多租户”“已通过渗透测试”或“无 timing side-channel”等超范围结论
