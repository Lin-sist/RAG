# RAG v3 优化总报告

## 阶段与提交

| 阶段 | 提交 | 说明 |
| --- | --- | --- |
| Stage 1 | `2ba2970` | 脚本化可复现评测闭环，固定评测 KB 身份并记录 metadata |
| Stage 2 | `902071b` | 参数化索引分块，live eval 矩阵选定 `420 / 80` |
| Stage 3 | `58d6f2a` | `ModelReranker` 接入默认关闭的 HTTP adapter，补健康检查、超时与 heuristic 降级 |

## 核心结论

- Stage 1 消除了评测 KB 漂移问题：同一 KB 连续两次 retrieval-only eval 结果一致，Recall@5 均为 62.75%，MRR 均为 0.6605。
- Stage 2 在不改 RRF/rerank 逻辑的前提下完成 4 组分块参数 live eval，最终选择 `chunk-size=420`、`chunk-overlap=80`，Recall@5 恢复到 68.63%，MRR 提升到 0.7346。
- Stage 3 完成真实 rerank provider 的接入能力，但默认安全关闭；本环境未配置真实 provider，因此没有宣称真实 rerank 指标提升。已验证 HTTP adapter、健康检查、超时配置、默认不可用、异常降级到 heuristic。

## 可比较指标

所有指标均来自 retrieval-only 报告，未修改评测集和指标定义。

| 阶段 | 报告 | KB | chunkCount | Recall@3 | Recall@5 | MRR | Top1 source accuracy | retrieveErrors |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Stage 1 Run 1 | `stage1-reproducible-eval-run1.md` | `codex-stage1-repro-eval` | 40 | 62.75% | 62.75% | 0.6605 | 96.30% | 0 |
| Stage 1 Run 2 | `stage1-reproducible-eval-run2.md` | `codex-stage1-repro-eval` | 40 | 62.75% | 62.75% | 0.6605 | 96.30% | 0 |
| Stage 2 500 / 50 | `stage2-chunk-500-50.md` | `codex-stage2-chunk-500-50` | 40 | 62.75% | 62.75% | 0.6605 | 96.30% | 0 |
| Stage 2 640 / 96 | `stage2-chunk-640-96.md` | `codex-stage2-fill-chunk-640-96` | 31 | 50.98% | 54.90% | 0.5833 | 96.30% | 0 |
| Stage 2 700 / 100 | `stage2-chunk-700-100.md` | `codex-stage2-chunk-700-100` | 28 | 43.14% | 47.06% | 0.4352 | 96.30% | 0 |
| Stage 2 420 / 80 | `stage2-chunk-420-80.md` | `codex-stage2-fill-chunk-420-80` | 50 | 68.63% | 68.63% | 0.7346 | 96.30% | 0 |

## 验证闭环

本轮已执行并通过：

```powershell
python -B -m py_compile scripts/run_rag_eval.py scripts/run_reproducible_rag_eval.py
python -B scripts/run_reproducible_rag_eval.py --help
python -B scripts/run_rag_eval.py --help
python -B scripts/test_run_reproducible_rag_eval.py
mvn -pl rag-document "-Dtest=DocumentChunkingPropertiesTest,DocumentChunkerRegressionTest" test
mvn -pl rag-core "-Dtest=ModelRerankerTest,RerankerRegistryTest,QueryEngineImplTest" test
mvn -q test
```

运行时验证：

- Stage 1 live eval：用户授权外部 embedding/provider 调用后，连续两次生成可复现报告。
- Stage 2 live eval：用户明确批准 4 组 chunk 参数批量 live eval 后，生成 4 组报告和 metadata。
- Stage 3 后端 smoke：`start-backend.ps1` 启动后，`POST /auth/login` 返回成功；随后停止 8080 上的 Java 后端进程。

## 范围边界

- 本轮未改鉴权、前端、向量库替换、关键词索引生命周期重构。
- Stage 2 未改 RRF 与 rerank 逻辑，指标变化只归因于索引分块参数。
- Stage 3 未配置真实外部 rerank provider，真实 rerank 指标待后续凭据和服务可用后再评测。
- 未跟踪目录 `docs/后端优化文档/` 不属于本轮提交范围，未纳入任何阶段 commit。

## 后续建议

1. 配置真实 rerank provider 后，用 Stage 1 的可复现 eval runner 对比 Stage 2 终值。
2. 若继续追求超过 68.63%，优先做真实 rerank 或标题感知切分专项，不再盲目放大 chunk。
3. 如需清理评测环境，可后续单独处理 live eval 过程中创建的 `codex-stage*` 评测 KB。
