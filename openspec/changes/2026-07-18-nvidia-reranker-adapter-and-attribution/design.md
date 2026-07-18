# Design: C6 NVIDIA Reranker Adapter And Attribution

## Context

当前 query pipeline 在 RRF 后通过 `RerankerRegistry` 选择 `heuristic` 或通用 `model`。`Reranker` 只返回 `List<RetrievedContext>`，registry 无法把 requested/effective provider、调用次数、覆盖率与 fallback 原因作为 typed result 交给 `QueryEngineImpl`；失败信息只写日志。`RetrievalResult` 已有 diagnostics map，但正常路径使用 `complete(...)` 返回空 map，debug controller 又调用无 diagnostics 的 `retrieve(...)`。

NVIDIA NeMo Retriever Reranking NIM 官方文档（2026-07-02 更新）定义的 HTTP contract 是：

- `POST /v1/ranking`；
- request：`model`、`query: {text}`、`passages: [{text}]`、可选 `truncate=NONE|END`；
- response：`rankings: [{index, logit}]`，按 logit 降序；
- logit 是未归一化原始预测，不是概率；
- 单次最多 512 passages。

参考：`https://docs.nvidia.com/nim/nemo-retriever/text-reranking/latest/using-reranking.html`。base URL、model 和实际 endpoint deployment 仍由运行配置决定，tracked 文件不得写真实 key。

## Goals

- 新增显式 NVIDIA protocol adapter，而不破坏现有通用 `model` provider。
- 每次 rerank 生成单一、可序列化、可测试的 effective-provider outcome。
- fallback 不再只靠日志猜测；debug、QA 和评测 runner 使用同一 diagnostics 事实。
- 对 NVIDIA raw logit 保持语义诚实，不伪装成概率或跨样本可比 score。
- 默认 heuristic、无外调和既有 retrieval/generation 指标语义保持不变。
- 为 C7 提供判定“100% effective-model coverage”所需的逐样本证据，但不在 C6 做收益 A/B。

## Non-goals

- 修改默认 provider、自动 provider discovery 或跨 model provider failover。
- C7 A/B、质量阈值、P50/P95 比较或生产默认切换。
- SSE 结构化完成事件、统一 OTel tracing/metrics 或前端展示。
- 新依赖、数据库、migration、索引状态机或评测集改造。
- 批量真实 provider 调用或使用用户/知识库数据做 smoke。

## Proposed Architecture

```text
QueryEngineImpl
  -> candidates after vector/BM25/RRF
  -> RerankerRegistry.rerankWithDiagnostics(...)
       requested=heuristic -> HeuristicReranker -> outcome(effective=heuristic)
       requested=model     -> existing ModelReranker -> outcome(model or fallback)
       requested=nvidia    -> NvidiaReranker
          -> POST configured /v1/ranking
          -> validate complete rankings
          -> sort by logit
          -> outcome(effective=nvidia)
          -> any stable failure -> heuristic outcome + fallback facts
  -> merge rerank diagnostics with vector-route diagnostics
  -> RetrievalResult(contexts, diagnostics)
       -> synchronous QA metadata
       -> debug retrieval diagnostics
       -> Python per-sample details + aggregate attribution
```

`RerankerRegistry` 仍是 fallback policy 的唯一入口。adapter 只负责自己的配置、协议调用、响应验证和 provider-local execution facts；它不得自行悄悄返回原 contexts 并声称成功。

## Provider Identity And Compatibility

- 保留 `heuristic` 与既有 `model` provider id。
- 新增 `nvidia` provider id；requested/effective attribution 使用稳定 id，而不是笼统的“model enabled”。
- `provider=nvidia` 只有在 `enabled=true`、base URL、endpoint、API key 和 model 均完整时才可尝试调用。
- 缺配置时 model call count 为 0，effective provider 为 heuristic，fallback reason 为 `not_configured`。
- 不从 LLM/embedding provider 的成功状态推断 reranker 可用；即使用户选择复用同一账户 key，也必须通过 NVIDIA rerank 专用配置显式提供。

建议配置形状（待事前闸门批准）：

```yaml
retrieval:
  rerank:
    provider: heuristic
    top-n: 20
    top-k: 5
    nvidia:
      enabled: false
      base-url: ""
      endpoint-path: /v1/ranking
      api-key: ${NVIDIA_RERANK_API_KEY:}
      model: ""
      truncate: NONE
      timeout-millis: 3000
      health-check-enabled: false
      health-path: /v1/health/ready
      health-cache-millis: 60000
```

健康检查默认关闭是为了兼容不暴露 NIM readiness path 的托管 endpoint；自托管 NIM 可显式开启 `/v1/health/ready`。关闭健康检查不代表 provider 健康，只表示由真实 ranking request 决定成功或进入稳定 fallback。

## Typed NVIDIA Protocol

请求类型只包含协议所需字段：

- `model`：配置中的非空 model id；
- `query.text`：当前 query；
- `passages[].text`：按候选顺序映射的 context content；
- `truncate`：`NONE` 或 `END`，默认建议 `NONE`。

首版只支持 text query + text passages，不扩展 VLM image/data URL。候选数受现有 `top-n` 限制并额外校验不超过 512；空 query、空 passages、超限或无正文候选在外调前稳定失败/回退。

响应必须满足：

- `rankings` 非空；
- 每个 index 在 `[0, candidateCount)`；
- index 不重复；
- 所有候选恰好覆盖一次；
- logit 有限，不是 NaN/Infinity。

任何空、重复、越界、缺失或非法 logit 都按 `invalid_response` 或 `incomplete_rankings` 整次 fallback。首版不混合 provider 排序。

## Score Semantics

NVIDIA `logit` 只决定本次候选顺序：

- `RetrievedContext.relevanceScore` 保留进入 rerank 前的 retrieval/RRF score；
- context metadata 新增 `rerankProvider=nvidia`、`rerankLogit`、`rerankRank`、`originalRelevanceScore`；
- 不对 logit 做 sigmoid 后宣称 relevance probability；
- debug/runner 的排序指标继续使用返回顺序，原 retrieval score 只用于既有诊断。

这会造成“返回顺序不一定按 relevanceScore 数值递减”，因此 debug response 与文档必须明确：启用 NVIDIA 时 final order 由 `rerankRank/logit` 决定，`score` 仍是原 retrieval score。C7 若需要跨 run score 分析，应单独定义，不在 C6 发明口径。

## Rerank Outcome And Diagnostics

建议引入内部 immutable outcome（命名可在实现时按现有包风格调整）：

```text
RerankOutcome
  contexts
  requestedProvider
  effectiveProvider
  fallbackCount
  fallbackReason
  modelCallCount
  candidateCount
  scoredCount
  latencyMs
  model
  protocol
```

`RerankerRegistry` 负责把 provider outcome 转为 `Map<String,Object>` 并与既有 retrieval diagnostics 合并。建议公开字段：

| 字段 | 语义 |
|---|---|
| `rerankRequestedProvider` | 配置请求的稳定 provider id；禁用时为 `disabled` |
| `rerankEffectiveProvider` | 实际决定 final order 的 provider |
| `rerankFallbackCount` | 单次样本 0 或 1 |
| `rerankFallbackReason` | 稳定枚举；无 fallback 时省略或 `none` |
| `rerankModelCallCount` | 实际发出的 HTTP ranking 请求数，首版 0 或 1 |
| `rerankCandidateCount` | 进入 rerank 的候选数 |
| `rerankScoredCount` | provider 合法返回的唯一候选数；fallback 仍保留实际观察值 |
| `rerankCoverage` | `scoredCount/candidateCount`，无候选时定义为 0 |
| `rerankLatencyMs` | provider/heuristic execution wall time，不包含整体 retrieval |
| `rerankModel` | 非敏感 model id；未调用时可为空 |
| `rerankProtocol` | `heuristic-v1`、`generic-documents-v1` 或 `nvidia-ranking-v1` |

这些字段不包含 query、passages、content、source、API key、Authorization、base URL query、raw response、异常 message 或 stack trace。

## Stable Fallback Taxonomy

首版建议稳定枚举：

- `not_configured`：provider 配置不完整，调用数 0；
- `health_check_failed`：显式启用的 health probe 失败，调用数 0；
- `timeout`：ranking request 超时；
- `http_4xx`：provider 非成功 4xx；
- `http_5xx`：provider 5xx；
- `network_error`：连接/I/O 失败；
- `invalid_response`：2xx body malformed、空或非法字段；
- `incomplete_rankings`：合法结构但未完整唯一覆盖候选；
- `provider_unavailable`：其他可安全归类的 provider unavailable；
- `unknown`：只作为最后兜底，不附 raw exception。

C6 不引入自动 retry；每个样本 model call count 至多 1。重试与限流策略若未来要加入，应另建 change，因为会改变调用预算、延迟和 fallback 统计。

## Retrieval Diagnostics Merge

`QueryEngineImpl` 当前分别返回 `RetrievalResult.complete(...)` 或 `keywordOnly(...)`。C6 需要改成显式合并：

```text
route diagnostics (e.g. keyword_only / milvus degraded)
  + rerank diagnostics
  = one RetrievalResult.diagnostics map
```

Milvus degraded 与 rerank fallback 可以同时发生，二者必须同时保留，不能由后写 map 覆盖。`RetrievalResult.degraded()` 继续只表示既有 retrieval dependency degradation；rerank fallback 通过独立字段表达，不改变 cache/degradation 语义，除非后续 change 明确修改。

## Debug, QA And Eval Exposure

### Debug retrieval

`QAController.debugRetrieve` 改用 `queryEngine.retrieveWithDiagnostics(...)`，在现有 `RetrievalDebugResponse` 追加 `diagnostics` 字段。错误路径也返回稳定 diagnostics/warnings，不泄露 exception message。该字段是 additive API change，现有 contexts 与指标字段保持兼容。

### Synchronous QA

同步 `RAGServiceImpl` 已把 retrieval diagnostics 合入 `QAResponse.metadata`；C6 只需保证 rerank diagnostics 在正常、fallback 和 no-context 路径不丢失。若 explanatory retry 触发第二次 retrieval，必须明确采用最后实际生成 contexts 的 attribution，不能把两次调用数错误写成 1；实现时需要测试该边界。

### Eval runner

`run_rag_eval.py` 从 `debugRetrieveRawResponse.diagnostics` 提取每样本 attribution，写入 details 和 Markdown sample section，并聚合：

- requested/effective provider sample counts；
- total model calls；
- fallback count 与 reason histogram；
- effective-model sample coverage；
- candidate/scored coverage；
- rerank latency 分布原始值或基础汇总。

C6 不改变 `Report status`、Recall@3/5、MRR、Top1、generation、citation、no-answer 或 judge 计算。C7 再定义“干净 model 组”的比较门禁与 P50/P95 对比。

## Security And Privacy

- tracked config 只引用环境变量，不写 key；不修改 `.env.local`。
- diagnostics、client response、Markdown/JSON report 和普通日志禁止 API key、Authorization、query、passages、context、raw provider body、异常 message。
- log 只允许 provider id、model、endpoint path、timeout、稳定 error category、count 与 latency；不得记录 base URL 中的 query/userinfo。
- fake server 测试使用固定合成 marker，并通过 SensitiveLogs/定向断言确认 marker 不进入可观察输出。
- optional live smoke 只使用短合成文本，执行前再次披露并取得授权。

## Verification Strategy

1. RED：NVIDIA request/response contract、完整覆盖和 logit 排序测试。
2. GREEN：新增 config + `NvidiaReranker`，只通过本地 fake HTTP server。
3. RED：registry success/unavailable/failure/partial response 的 typed outcome 与 stable taxonomy。
4. GREEN：outcome + heuristic fallback，model call count 0/1 可断言。
5. RED：QueryEngine diagnostics merge、debug response、sync QA metadata 和 explanatory retry 边界。
6. GREEN：逐层传递 diagnostics，不改变既有 metrics/cache 语义。
7. RED/GREEN：Python per-sample extraction、aggregate coverage、report rendering 与敏感字段 redaction。
8. 聚焦 Java + `mvn -q test` + Python 33+ tests + SensitiveLogs + `git diff --check`。
9. 若用户授权：最多 1 次纯合成 live smoke；否则记录未执行和 protocol-tested 边界。

## Rollout And Compatibility

- 无数据库 migration、无新依赖。
- 默认 heuristic，因此部署后行为不变。
- `model` provider 与既有配置继续可用；`nvidia` 必须显式选择且完整配置。
- 新 debug response 字段与 QA metadata 均为 additive；旧客户端可忽略。
- 出现 provider 问题时可立即把 provider 改回 heuristic；无需数据回滚。

## 决策记录

### 决策 1：新增独立 `nvidia` provider，而不是覆盖通用 `model`
- **面临的选择**：直接把 `ModelReranker` 改成 NVIDIA 协议；新增独立 `NvidiaReranker`；一次性重构成多协议 client SPI。
- **选了哪个 + 为什么**：建议新增独立 `nvidia` provider并保留 `model`；provider identity 清楚、兼容既有配置且改动可控，待用户在事前闸门确认。
- **放弃的代价**：覆盖 `model` 会静默破坏现有通用协议；一次性 SPI 重构扩大 C6 范围并增加回归面。

### 决策 2：用 typed outcome 传递归因，而不是日志或 ThreadLocal
- **面临的选择**：继续只写日志；把状态塞进每个 context metadata；让 rerank 返回 typed contexts + diagnostics outcome。
- **选了哪个 + 为什么**：建议 typed outcome；它让 success/fallback/无候选都能有单一样本事实，并可直接单测和传给 runner，待用户在事前闸门确认。
- **放弃的代价**：日志无法形成评测证据；context metadata 在空结果/fallback/聚合时易丢失且会重复数据。

### 决策 3：同时向 debug retrieval 与同步 QA 暴露 diagnostics
- **面临的选择**：只改 QA metadata；只改 debug endpoint；两个入口都使用同一 `RetrievalResult.diagnostics`。
- **选了哪个 + 为什么**：建议两个入口共用 diagnostics；retrieval-only 与 generation run 才能使用同一 attribution，待用户在事前闸门确认。
- **放弃的代价**：只改 QA 会让 retrieval-only C7 无证据；只改 debug 会让线上同步问答无法解释 fallback。

### 决策 4：NVIDIA rankings 不完整时整次 fallback
- **面临的选择**：接受部分结果并把未评分项追加末尾；用 heuristic 补齐未评分项；整次回退 heuristic。
- **选了哪个 + 为什么**：建议整次 fallback；effective provider 保持单值且 C7 不会把混合排序当干净 model，待用户在事前闸门确认。
- **放弃的代价**：直接追加会产生不可解释次序；heuristic 补齐会形成污染样本且 coverage 很难被误用者正确解读。

### 决策 5：logit 只决定顺序，不伪装成概率
- **面临的选择**：raw logit 覆盖 `relevanceScore`；对 logit 做 sigmoid；保留原 retrieval score并把 logit/rank 放 metadata。
- **选了哪个 + 为什么**：建议保留原 score、metadata 记录 logit/rank；官方明确 logit 未归一化，不能凭空制造概率语义，待用户在事前闸门确认。
- **放弃的代价**：raw logit 会破坏 0..1 假设；sigmoid 看似概率但未经校准，会误导跨样本比较。

### 决策 6：health check 默认关闭、按部署显式开启
- **面临的选择**：所有 NVIDIA endpoint 强制 `/v1/health/ready`；完全删除 health；默认关闭但保留可配置 readiness probe。
- **选了哪个 + 为什么**：建议默认关闭并保留配置；兼容托管 endpoint 与自托管 NIM，同时让真实 ranking request 决定可用性，待用户在事前闸门确认。
- **放弃的代价**：强制 probe 可能误判托管服务；删除 health 会失去自托管部署的快速失败能力。

### 决策 7：`truncate=NONE` 作为显式、可比较的默认
- **面临的选择**：默认 `NONE`；默认 `END`；不发送 truncate 依赖 provider 默认。
- **选了哪个 + 为什么**：建议默认 `NONE` 并允许显式改为 `END`；当前 chunk 基线较短，超限应显式 fallback而不是静默截断，待用户在事前闸门确认。
- **放弃的代价**：默认 `END` 可能让不同样本发生不可见截断；省略字段会把行为交给 provider 版本默认值，降低复现性。

### 决策 8：使用稳定 fallback 枚举，不输出 raw exception
- **面临的选择**：直接写异常 message；只记录一个 `fallback=true`；定义稳定 reason taxonomy。
- **选了哪个 + 为什么**：建议稳定 taxonomy；既能聚合诊断又能守住隐私与跨版本可比性，待用户在事前闸门确认。
- **放弃的代价**：raw message 可能泄密且不稳定；单一布尔值无法区分配置、网络、HTTP 与响应契约问题。

### 决策 9：C6 不自动 retry，单样本 model call 至多一次
- **面临的选择**：内建指数重试；复用 LLM retry；C6 首版零自动 retry。
- **选了哪个 + 为什么**：建议零自动 retry；调用量、延迟和 fallback 统计保持确定，provider resilience 可另建 change，待用户在事前闸门确认。
- **放弃的代价**：内建或复用 retry 会扩大外调预算并污染 C7 latency；缺少独立契约时容易重复请求。

### 决策 10：真实 smoke 最多一次且使用纯合成文本
- **面临的选择**：完全不做 live smoke；直接跑 30 条评测；单独授权 1 次合成 smoke。
- **选了哪个 + 为什么**：建议保留一次合成 smoke 闸门；它能验证真实 endpoint/auth/schema，同时把数据与成本风险压到最小，待用户单独授权。
- **放弃的代价**：完全不做只能得到 protocol-tested 结论；直接跑 30 条属于 C7 批量 A/B且未经调用预算授权。

### 决策 11：C6 不做收益结论或默认 provider 切换
- **面临的选择**：接通后立即切默认；顺带跑 A/B并下结论；只完成 adapter + attribution。
- **选了哪个 + 为什么**：建议只完成 adapter + attribution；先证明模型真实生效，再由 C7 固定身份做可比较 A/B，待用户在事前闸门确认。
- **放弃的代价**：立即切默认没有收益与故障证据；把 A/B塞进 C6 会混合协议正确性和业务效果两个验收问题。

### 决策 12：不扩展 SSE、前端和统一 observability
- **面临的选择**：同步扩展 SSE structured event/前端展示/OTel；只扩 debug + sync QA + eval；完全只留内部日志。
- **选了哪个 + 为什么**：建议只扩 debug、同步 QA 与 eval；这足以支持 C6/C7证据链且不牵连独立架构债，待用户在事前闸门确认。
- **放弃的代价**：全栈扩展会把一个 provider change扩大成跨模块项目；只留内部日志又无法完成逐样本归因目标。
