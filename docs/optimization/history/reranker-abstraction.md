# Stage 2：Reranker 抽象与模型重排扩展点

## 改动摘要

- 新增 `Reranker` 接口，统一暴露 `provider()`、`available()`、`rerank(...)` 契约。
- 新增 `HeuristicReranker`，把原 `QueryEngineImpl` 内部关键词启发式重排迁移为独立实现，作为默认兜底。
- 新增 `ModelReranker` 扩展点，但在未接入具体 API 适配器前始终不可用，避免把占位实现误判为真实模型重排。
- 新增 `RerankerRegistry`，按 `retrieval.rerank.provider` 选择 provider；请求的 provider 不可用时自动降级到 `heuristic` 并记录告警。
- `QueryEngineImpl` 改为通过 registry 执行重排，并新增 `rerank.top-n` 与最终 `top-k` 截断配置。
- 在 `application.yml` 增加 `retrieval.rerank.provider`、`top-n`、`top-k`、`model.*` 配置。

## Provider 策略

默认 provider 是 `heuristic`。它保持 Stage 1 已有关键词启发式重排行为，因此在没有真实 rerank 服务时主链路不依赖外部服务。

`model` provider 目前只保留扩展点，不伪装成可用 provider：

- 即使配置了 `enabled/base-url/api-key/model`，在具体 API adapter 未实现前 `available()` 仍返回 `false`。
- 当 `retrieval.rerank.provider=model` 时，registry 会告警并自动降级到 `heuristic`。
- 后续接入 cross-encoder、本地 bge-reranker 或在线 rerank API 时，应在 `ModelReranker` 内补齐真实调用与错误降级，再把 `available()` 改为基于配置和健康检查返回。

## 评测结果

原 Stage 1 报告使用 `kbId=6`，但当前本地库中 `kbId=6` 已漂移为《时光回序》文档，不再是 eval 测试知识库。为避免错误证据，本阶段重新创建 `kbId=7`，上传同一组 `test-data` 文档：

- `springboot-basics.md`：19 个 chunk
- `java-interview-guide.md`：11 个 chunk
- `rag-technology-guide.md`：11 个 chunk

评测命令：

```powershell
python -B scripts/run_rag_eval.py --kb-id 7 --skip-ask --no-overwrite --report docs/eval/reports/stage2-reranker-abstraction.md --details-json docs/eval/reports/stage2-reranker-abstraction-details.json
```

| 指标 | Stage 1 hybrid | Stage 2 reranker abstraction |
| --- | ---: | ---: |
| Report status | RETRIEVAL_ONLY | RETRIEVAL_ONLY |
| Recall@3 | 66.67% | 66.67% |
| Recall@5 | 68.63% | 68.63% |
| MRR | 0.6821 | 0.6821 |
| Top1 source accuracy | 96.30% | 96.30% |
| retrieveErrors | 0 | 0 |

## 结论

Stage 2 指标与 Stage 1 持平，符合预期：本阶段默认仍使用 `HeuristicReranker`，目标是把重排能力从 `QueryEngineImpl` 中抽象出来，并留下真实模型 provider 的安全扩展点。

真实模型重排未验证，原因是当前没有已实现的 rerank API adapter；本阶段不强行调用不可控外部服务，也不把未实现能力写成已完成能力。

## 验证

- `mvn -pl rag-core "-Dtest=QueryEngineImplTest,InMemoryBm25KeywordIndexTest,ModelRerankerTest" test`：通过。
- `mvn -q test`：通过。
- 后端启动：通过，`logs/backend-stage2-start2.log` 显示 Tomcat 8080 started；上传后通过文档列表 API 轮询确认 `kbId=7` 三份 eval 文档均为 `COMPLETED`。
- `python -B scripts/run_rag_eval.py --kb-id 7 --skip-ask --no-overwrite --report docs/eval/reports/stage2-reranker-abstraction.md --details-json docs/eval/reports/stage2-reranker-abstraction-details.json`：通过，报告状态 `RETRIEVAL_ONLY`。
