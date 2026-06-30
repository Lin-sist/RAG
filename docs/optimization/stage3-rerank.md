# Stage 3：ModelReranker 真实接入能力

## 当前状态

本阶段已把 `ModelReranker` 从“不可用扩展点”推进为默认关闭的 HTTP rerank adapter。当前仓库默认配置仍为 `retrieval.rerank.provider=heuristic`，`retrieval.rerank.model.enabled=false`，因此正常启动不会访问外部 rerank 服务。

本环境未配置真实 rerank provider 凭据或服务地址，本阶段不宣称真实重排线上指标已验证。已验证范围为：HTTP 请求适配、健康检查、超时配置、默认关闭、配置不完整不可用、provider 失败时自动降级到 `HeuristicReranker`。

## 已完成改动

- `ModelReranker` 支持通用 HTTP rerank 请求：`model`、`query`、`documents`。
- `ModelReranker` 支持读取 provider 响应中的 `results[].index` 与 `results[].relevance_score` / `score`，并按模型分数重排候选。
- rerank 后的 `RetrievedContext` 保留 `originalRelevanceScore`，新增 `rerankScore`、`rerankProvider=model` 元数据。
- `RetrievalProperties.ModelReranker` 新增：
  - `endpointPath`
  - `timeoutMillis`
  - `healthCheckEnabled`
  - `healthPath`
  - `healthCacheMillis`
- `application.yml` 保持默认安全关闭，并补齐脱敏配置占位。
- `RerankerRegistry` 新增统一 `rerank` 入口：请求 provider 不可用时回退 heuristic；provider 调用异常时也回退 heuristic。

## 默认配置摘要

```yaml
retrieval:
  rerank:
    provider: heuristic
    top-n: 20
    top-k: 5
    model:
      enabled: false
      base-url: ""
      endpoint-path: /rerank
      api-key: ""
      model: ""
      timeout-millis: 3000
      health-check-enabled: true
      health-path: /health
      health-cache-millis: 60000
```

## 验证结果

已运行：

```powershell
mvn -pl rag-core "-Dtest=ModelRerankerTest,RerankerRegistryTest,QueryEngineImplTest" test
mvn -q test
```

结果：

- 聚焦测试通过，13 tests，0 failures，0 errors。
- 全量测试通过。
- 后端启动 smoke 通过：`start-backend.ps1` 启动后，`POST /auth/login` 返回成功；随后已停止 8080 上的 Java 后端进程。

覆盖项：

- 默认配置下 `ModelReranker.available()` 为 `false`。
- 本地 fake HTTP server `/health` 返回 200 时，model reranker 可用。
- 本地 fake HTTP server `/health` 返回 503 时，model reranker 不可用。
- 本地 fake HTTP server `/rerank` 返回模型分数时，候选按 `relevance_score` 重排。
- 请求 payload 包含 `model`、`query`、`documents`，请求头使用 `Authorization: Bearer <apiKey>`。
- `/rerank` 返回 500 时，`ModelReranker` 抛出异常，由 `RerankerRegistry` 降级到 `HeuristicReranker`。
- `QueryEngineImpl` 仍能在 `provider=model` 但 model 不可用时回退 heuristic。

## 指标说明

Stage 2 终值：

- Recall@3：68.63%
- Recall@5：68.63%
- MRR：0.7346
- Top1 source accuracy：96.30%

Stage 3 未接入真实外部 rerank provider，因此没有新的真实 rerank 指标可与 Stage 2 对比。本阶段对检索质量的结论是“默认 heuristic 链路不变，真实 provider 待配置后再评测”，不是“真实 rerank 已提升指标”。

## 后续验证条件

若要完成真实 provider 指标验证，需要显式配置：

- `retrieval.rerank.provider=model`
- `retrieval.rerank.model.enabled=true`
- `retrieval.rerank.model.base-url`
- `retrieval.rerank.model.api-key`
- `retrieval.rerank.model.model`

配置完成后再用 Stage 1 的可复现 eval runner 对比 Stage 2 终值，报告 Top1 source accuracy、Recall@5、MRR 的涨/平/跌。
