# RAG 后端优化总报告

## 阶段与提交

| 阶段 | 提交 | 说明 |
| --- | --- | --- |
| Baseline | - | 写业务代码前运行 retrieval-only 基线报告 |
| Stage 1 | `142d569` | 新增 BM25 关键词召回并用 RRF 融合，实现真混合检索 |
| Stage 2 | `9184578` | 抽象 Reranker 接口，保留模型重排扩展点，默认 heuristic 兜底 |
| Stage 3.3 | `d63f71c` | 修复 `supplyAsync` 未使用配置线程池 |
| Stage 3.1 | `cfce764` | 引入 token chunker 抽象并补充标题层级 metadata |
| Stage 3.2 | `dd6cc46` | 增加入库重试和幂等防重复 chunk 持久化 |
| Stage 3 回归修复 | `76e9c58` | 过滤空 metadata，避免关键词索引写入失败 |

## 检索指标对比

所有可比较指标均来自 `scripts/run_rag_eval.py --skip-ask --no-overwrite` 的 retrieval-only 报告，未修改评测集与指标口径。

| 阶段 | 报告 | KB | Recall@3 | Recall@5 | MRR | Top1 source accuracy | retrieveErrors |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline | `baseline-goal-pre-stage1.md` | 6 | 54.90% | 54.90% | 0.6667 | 100.00% | 0 |
| Stage 1 | `stage1-hybrid-retrieval.md` | 6 | 66.67% | 68.63% | 0.6821 | 96.30% | 0 |
| Stage 2 | `stage2-reranker-abstraction.md` | 7 | 66.67% | 68.63% | 0.6821 | 96.30% | 0 |
| Stage 3 runtime check | `stage3-existing-kb7-runtime-check.md` | 7 | 66.67% | 68.63% | 0.6821 | 96.30% | 0 |
| Stage 3 token reindex | `stage3-token-chunker-reindex-after-fix.md` | 8 | 62.75% | 62.75% | 0.6605 | 96.30% | 0 |

## 结论

- Stage 1 明确提升 Recall@3 / Recall@5 / MRR，代价是 Top1 source accuracy 从 100.00% 降到 96.30%，属于 RRF 融合后排名分布变化。
- Stage 2 指标与 Stage 1 持平，符合预期：本阶段目标是 Reranker 抽象，不强行伪造真实模型重排能力。
- Stage 3 在旧索引 `kbId=7` 上与 Stage 2 持平，说明检索执行链路未回退。
- Stage 3 在新 token chunker 重新入库后 Recall@5 回退到 62.75%，但 retrieveErrors 为 0，上传、索引、BM25 重建和评测链路可用。该回退来自分块边界变化，不在本轮通过评测样本特调修正。

## 验证闭环

- 每个代码阶段提交前均运行 focused tests 和 `mvn -q test`。
- 后端以最新代码启动成功，`logs/backend-stage3-fix-start.log` 显示 Tomcat 8080 started。
- `kbId=9` 新上传链路完成，三份 `test-data` 文档均为 `COMPLETED`，日志显示 BM25 keyword index upsert 成功。
- `kbId=9` 的最终 eval 因平台用量限制被拦截，未生成报告；最终 Stage 3 指标采用同一测试资料、同一评测脚本、修复后可比较的 `kbId=8` 报告。
