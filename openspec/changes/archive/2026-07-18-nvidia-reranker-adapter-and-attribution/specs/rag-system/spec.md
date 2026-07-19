# RAG System Spec Delta: C6 NVIDIA Reranker Adapter And Attribution

## ADDED Requirements

### Requirement: NVIDIA Ranking 协议适配

系统 SHALL 在显式选择且完整配置 `nvidia` reranker 时，使用 typed NVIDIA ranking contract 调用配置的 HTTP endpoint。请求 MUST 包含 model、`query.text`、按候选顺序构造的 `passages[].text` 与显式 truncate policy；响应 MUST 按 `rankings[].index/logit` 解释。系统 MUST NOT 把 NVIDIA raw logit 表述为已校准概率或跨样本可直接比较的 relevance probability。

NVIDIA adapter MUST 验证 rankings 非空、index 唯一且不越界、完整覆盖本次候选并且 logit 为有限数值。候选数量 MUST NOT 超过配置与 provider contract 的上限。首版仅支持 text query 与 text passages。

#### Scenario: 合法 NVIDIA rankings

- GIVEN `provider=nvidia` 且配置完整
- AND 本次候选均为非空 text passages
- WHEN provider 返回完整唯一的 `rankings[].index/logit`
- THEN 系统按 logit 降序决定 final rerank order
- AND 每个 index 精确映射回原候选
- AND 原 retrieval score 保留，raw logit 与 rerank rank 作为独立 metadata
- AND 系统不把 logit 命名或展示为概率

#### Scenario: 非法或不完整 rankings

- WHEN provider 返回空 rankings、重复/越界/缺失 index、非法 logit 或未完整覆盖候选
- THEN 本次 NVIDIA 结果不得部分生效
- AND 系统整次使用 heuristic fallback 或返回稳定失败
- AND diagnostics 使用稳定 `invalid_response` 或 `incomplete_rankings`，不包含 raw body

#### Scenario: 外调前输入不满足协议

- WHEN query/passages 为空、候选超过 provider 上限或 NVIDIA 配置不完整
- THEN 系统不发送 ranking HTTP 请求
- AND model call count 为 0
- AND 使用 heuristic fallback 或稳定返回无候选结果

### Requirement: Reranker Requested 与 Effective Provider 归因

每次启用 rerank 的 retrieval SHALL 产生结构化归因，至少包含 requested provider、effective provider、fallback count/reason、实际 model call count、candidate count、scored count、coverage、rerank latency、model 与 protocol。requested provider 表示配置意图，effective provider 表示实际决定 final order 的唯一 provider；两者 MUST NOT 因 fallback 被混为同一事实。

归因 MUST 使用稳定字段与枚举，MUST NOT 包含 API key、Authorization、query、passages/context、raw provider response、原始异常 message 或 stack trace。单次样本的 fallback count SHALL 为 0 或 1；C6 首版对 NVIDIA 的 model call count SHALL 为 0 或 1，不自动 retry。

#### Scenario: NVIDIA 成功生效

- GIVEN requested provider 为 `nvidia`
- WHEN 一个合法 ranking 请求成功且完整覆盖候选
- THEN effective provider 为 `nvidia`
- AND fallback count 为 0
- AND model call count 为 1
- AND candidate/scored coverage 为 100%
- AND protocol 标识为稳定 NVIDIA ranking 版本

#### Scenario: 调用前不可用

- GIVEN requested provider 为 `nvidia`
- WHEN 配置不完整或显式 health check 失败
- THEN effective provider 为 `heuristic`
- AND fallback count 为 1
- AND fallback reason 为 `not_configured` 或 `health_check_failed`
- AND model call count 为 0

#### Scenario: 调用后失败

- GIVEN requested provider 为 `nvidia`
- WHEN ranking request timeout、网络失败、HTTP 4xx/5xx 或响应无效
- THEN effective provider 为 `heuristic`
- AND fallback count 为 1
- AND model call count 为 1
- AND fallback reason 使用稳定分类而非原始异常内容

#### Scenario: 默认 heuristic

- GIVEN tracked 默认配置未被覆盖
- WHEN 系统执行 rerank
- THEN requested 与 effective provider 均为 `heuristic`
- AND model call count 与 fallback count 均为 0
- AND 系统不访问 NVIDIA endpoint

### Requirement: Rerank Fallback 与 Retrieval Diagnostics 合并

Rerank outcome SHALL 通过显式返回值进入 `RetrievalResult.diagnostics`，不得只依赖普通日志、ThreadLocal 或单个 context metadata。rerank diagnostics SHALL 与 vector/keyword route diagnostics 合并；当 Milvus degradation 与 rerank fallback 同时发生时，两类事实 MUST 同时保留且不得互相覆盖。

Rerank fallback 本身 MUST NOT 改写既有 retrieval degradation/cache 语义，除非后续独立 change 明确修改。provider 返回部分 rankings 时首版 MUST 整次 fallback，不得形成无法单值归因的 model/heuristic 混合排序。

#### Scenario: Milvus 降级且 NVIDIA fallback

- GIVEN dense route 不可用但 keyword evidence 可用
- AND requested reranker 为 `nvidia`
- WHEN NVIDIA ranking 同时失败
- THEN returned contexts 使用 keyword route 与 heuristic rerank
- AND diagnostics 同时包含 `retrievalMode=keyword_only`、Milvus degradation 与 NVIDIA fallback facts
- AND effective rerank provider 仅为 `heuristic`

#### Scenario: Rerank 未启用

- GIVEN request 明确 `enableRerank=false`
- WHEN retrieval 返回 contexts
- THEN diagnostics 标明 rerank disabled 或不产生 provider 调用
- AND model call count 为 0
- AND contexts 保持既有 retrieval order

### Requirement: Debug、同步问答与评测逐样本归因

同步 QA response metadata 与 debug retrieval response SHALL 暴露同一套 sanitized reranker attribution。debug retrieval MUST 使用包含 diagnostics 的检索入口，使 retrieval-only 运行也能证明 requested/effective provider 与 fallback；同步 QA MUST 报告实际用于生成 contexts 的 attribution。

评测 runner SHALL 逐样本保存 reranker attribution，并聚合 effective provider sample count、model coverage、fallback count/reason、model call count 与 candidate coverage。C6 attribution SHALL NOT 改变既有 Report status、Recall@3/5、MRR、Top1、generation、citation、no-answer 或 judge 指标语义，也 SHALL NOT 自行宣称 NVIDIA 相对 heuristic 的业务收益。

#### Scenario: Retrieval-only 报告包含 provider coverage

- GIVEN 评测以 `skipAsk=true` 运行多个样本
- WHEN 部分样本 NVIDIA 成功、部分样本 fallback
- THEN 每个样本记录 requested/effective provider 与调用/fallback facts
- AND 报告分别聚合 effective NVIDIA 与 heuristic 样本数
- AND 部分 fallback 不得被描述为 100% model coverage
- AND retrieval metrics 仍按既有顺序与公式计算

#### Scenario: 同步问答透传安全归因

- GIVEN 同步问答完成 retrieval 并生成答案
- WHEN 构造 `QAResponse.metadata`
- THEN metadata 包含本次实际 contexts 的 sanitized rerank attribution
- AND 不包含 query、passages、provider raw body、异常 message、API key 或 Authorization
