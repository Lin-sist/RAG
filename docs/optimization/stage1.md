# Stage 1：混合检索（BM25 + 向量 + RRF）

## 改动摘要

- 在 `rag-core` 增加 `KeywordIndex` / `KeywordDocument` 抽象，以及内置 `InMemoryBm25KeywordIndex`。
- `QueryEngineImpl` 在 `retrieval.hybrid.enabled=true` 时同时执行向量召回与 BM25 关键词召回，并使用 RRF 融合排序。
- 在 `rag-admin` 文档入库成功后同步写入关键词索引，删除文档时清理关键词索引。
- 增加 `KeywordIndexBootstrap`，应用启动后从已持久化的 `document_chunk` 记录重建 BM25 索引；本次 KB=6 启动重建出 41 个 chunk。
- 在 `application.yml` 增加 `retrieval.hybrid.enabled`、`retrieval.hybrid.rrf-k`、`retrieval.hybrid.keyword-top-k-multiplier`、`retrieval.keyword.enabled` 配置。

## 方案选择

本阶段选择仓库内置轻量 BM25，而不是 Lucene 本地索引。

原因：
- 当前本机 Maven 缓存未发现 Lucene 依赖，直接引入 Lucene 会把 Stage 1 验证风险转移到外部依赖下载与版本兼容。
- 本轮核心目标是形成可验证的关键词 BM25 召回路线，并与现有向量召回通过 RRF 融合；内置 BM25 可以在不改变主框架、不更换向量库的前提下完成。
- 索引生命周期由应用内服务控制：启动从 DB 重建、入库 upsert、删除清理；若关键词索引为空或失败，查询自动退回向量结果，不破坏主链路。

## 索引生命周期

- 创建：应用启动时 `KeywordIndexBootstrap` 按知识库 collection 重建索引；新文档入库成功后由 `DocumentIndexingServiceImpl` 写入该 collection。
- 更新：重复 vectorId 的 chunk 会覆盖旧关键词文档。
- 删除：删除文档时，`DocumentServiceImpl` 使用该文档的 vectorId 列表清理关键词索引。
- 重建：服务重启后从 MySQL 中已完成文档的 `document_chunk` 记录重建。
- 降级：BM25 索引为空、检索失败或禁用时，`QueryEngineImpl` 保留纯向量召回结果。

## 评测结果

评测命令：

```powershell
python -B scripts/run_rag_eval.py --kb-id 6 --skip-ask --no-overwrite --report docs/eval/reports/stage1-hybrid-retrieval.md --details-json docs/eval/reports/stage1-hybrid-retrieval-details.json
```

| 指标 | 优化前 baseline-goal-pre-stage1 | Stage 1 hybrid |
| --- | ---: | ---: |
| Report status | RETRIEVAL_ONLY | RETRIEVAL_ONLY |
| Recall@3 | 54.90% | 66.67% |
| Recall@5 | 54.90% | 68.63% |
| MRR | 0.6667 | 0.6821 |
| Top1 source accuracy | 100.00% | 96.30% |
| retrieveErrors | 0 | 0 |

## 结论

Recall@3、Recall@5、MRR 均提升，说明关键词 BM25 路线补足了部分向量召回遗漏。Top1 source accuracy 小幅回退 3.70 个百分点，原因是 RRF 会把关键词强命中的 chunk 推到更靠前位置，个别样本 top1 来源不再命中 expected source；由于 Recall@5 与 MRR 均提升，暂不回滚，后续 Stage 2 的 Reranker 抽象正好用于收敛 top1 排序质量。

## 验证

- `mvn -pl rag-core "-Dtest=QueryEngineImplTest,InMemoryBm25KeywordIndexTest" test`：通过。
- `mvn -pl rag-admin -am "-Dtest=DocumentIndexingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `mvn -q test`：通过。
- 后端启动：通过，`logs/backend-stage1-start2.log` 显示 Tomcat 8080 started，KB=6 BM25 索引重建 41 个 chunk。
