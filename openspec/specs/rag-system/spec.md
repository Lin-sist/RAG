# RAG System Specification

## Requirements

### Requirement: 文档索引链路

系统 SHALL 将上传文档解析、分块、向量化并写入配置的向量库，同时持久化文档/chunk 状态；失败 SHALL 以可观察的失败状态结束，不得伪装成功。

#### Scenario: 成功索引

- GIVEN 用户对知识库具有写权限
- WHEN 上传受支持的文档并完成异步任务
- THEN 文档状态为 `COMPLETED`
- AND chunk 可被该知识库检索

#### Scenario: 外部依赖失败

- WHEN Embedding 或向量库调用失败
- THEN 任务或文档状态明确失败
- AND 错误不包含 secret

### Requirement: 混合检索

系统 SHALL 支持 dense vector 与 BM25 keyword 双路召回并通过 RRF 融合；任一非关键路线不可用时 SHALL 以明确降级方式维持主链路或返回可诊断失败。

#### Scenario: 默认查询

- WHEN 用户在已完成索引的知识库中提问
- THEN 系统使用当前启用的 hybrid 配置检索
- AND 返回结果携带可用于诊断的来源与分数信息

### Requirement: Reranker 边界

系统 MUST 默认使用已验证可用的 reranker。真实 model provider 未配置、不可用或失败时 MUST 降级到 heuristic，且不得宣称 model rerank 收益已经验证。

#### Scenario: Model provider 不可用

- GIVEN 配置请求 model reranker
- WHEN provider 健康检查失败或调用异常
- THEN 查询使用 heuristic fallback 或返回明确失败
- AND 记录 provider 与降级原因

### Requirement: 生成与引用

系统 SHALL 只基于检索上下文生成知识库回答。Citation MUST 回连到本轮 returned contexts；无法验证的 citation SHALL 被丢弃或标记为 unsupported。

#### Scenario: 有足够上下文

- WHEN 检索上下文足以回答问题
- THEN 响应包含答案及可验证 citations
- AND citation snippet 能回连到 returned contexts

#### Scenario: 无足够上下文

- WHEN 知识库没有足够信息
- THEN 系统明确拒答
- AND `metadata.status=no_result`
- AND citations 为空

### Requirement: Secret 安全

API key、JWT secret、数据库密码和用户私密内容 MUST NOT 写入 tracked files、诊断报告或普通日志。

#### Scenario: Provider 失败诊断

- WHEN 外部 provider 调用失败
- THEN 可以记录 provider、endpoint、model、timeout、retry 和错误类别
- BUT MUST NOT 记录 API key 或认证 header
