# Stage 3：工程硬伤修复

## 改动摘要

- 引入 `TokenCounter` / `TokenChunker` 抽象，默认使用保守 token 估算实现，保留原 SEMANTIC 分块骨架。
- 文档 chunk metadata 新增并保留 `headingPath`、`headingLevel`、`sourceFileName`、`tokenCount`，入库阶段继续补齐 `documentId`、`kbId`、`chunkIndex`、`startIndex`、`endIndex` 等已有字段。
- 文档入库新增最多 3 次指数退避重试；重试时如果 DB chunk 已存在则跳过重复持久化，避免重复 chunk 入库。
- `RedisAsyncTaskManager` 的 `CompletableFuture.supplyAsync` 显式使用 `asyncTaskExecutor`，修复配置线程池未生效的问题。
- 追加修复：过滤关键词索引 metadata 中的 `null` 值，避免 `headingLevel=null` 触发 `KeywordDocument` 的 `Map.copyOf` 空值异常，导致 BM25 索引写入失败。

## 评测与验证

### 运行时回归检查

使用 Stage 2 已完成索引的 `kbId=7`，在最新 Stage 3 代码下重跑 retrieval-only：

```powershell
python -B scripts/run_rag_eval.py --kb-id 7 --skip-ask --no-overwrite --report docs/eval/reports/stage3-existing-kb7-runtime-check.md --details-json docs/eval/reports/stage3-existing-kb7-runtime-check-details.json
```

结果与 Stage 2 持平：Recall@3 66.67%，Recall@5 68.63%，MRR 0.6821，Top1 source accuracy 96.30%，retrieveErrors 0。

### 新 token chunker 重新入库检查

新建 `kbId=8` 后上传同一组 `test-data` 文档：

- `springboot-basics.md`：19 个 chunk
- `java-interview-guide.md`：10 个 chunk
- `rag-technology-guide.md`：11 个 chunk

初次评测发现 retrieveErrors=1，原因不是文档未完成，而是 `headingLevel=null` 进入关键词索引 metadata 后触发空值异常，导致新 KB 的 BM25 索引未写入；修复后重启服务，`KeywordIndexBootstrap` 从 DB 重建 `kbId=8` 的 BM25 索引成功。

修复后评测命令：

```powershell
python -B scripts/run_rag_eval.py --kb-id 8 --skip-ask --no-overwrite --report docs/eval/reports/stage3-token-chunker-reindex-after-fix.md --details-json docs/eval/reports/stage3-token-chunker-reindex-after-fix-details.json
```

| 指标 | Stage 2 reranker abstraction | Stage 3 token reindex after fix |
| --- | ---: | ---: |
| Report status | RETRIEVAL_ONLY | RETRIEVAL_ONLY |
| Recall@3 | 66.67% | 62.75% |
| Recall@5 | 68.63% | 62.75% |
| MRR | 0.6821 | 0.6605 |
| Top1 source accuracy | 96.30% | 96.30% |
| retrieveErrors | 0 | 0 |

### 新上传链路检查

修复后再次新建 `kbId=9` 并上传三份 `test-data` 文档，三份均为 `COMPLETED`：

- `springboot-basics.md`：19 个 chunk
- `java-interview-guide.md`：10 个 chunk
- `rag-technology-guide.md`：11 个 chunk

后端日志 `logs/backend-stage3-fix-start.log` 显示该新 collection 三次 BM25 upsert 成功：

- `BM25 keyword index upserted: collection=kb_aaf99e02adb943c7, docs=10`
- `BM25 keyword index upserted: collection=kb_aaf99e02adb943c7, docs=11`
- `BM25 keyword index upserted: collection=kb_aaf99e02adb943c7, docs=19`

受平台用量限制，`kbId=9` 的最终 retrieval-only 评测命令被拦截，未产生报告；本阶段最终可比较指标采用 `kbId=8` 的修复后报告。

## 结论

Stage 3 工程修复已落地，主链路可启动、可上传、可重建关键词索引、可跑 retrieval-only 评测。最新代码跑旧索引 `kbId=7` 与 Stage 2 持平，说明检索执行链路未回退。

新 token chunker 重新入库后的指标相对 Stage 2 小幅回退，主要来自分块边界变化：`java-interview-guide.md` 从 11 个 chunk 变为 10 个 chunk，部分评测片段跨 chunk 边界后 Recall@5 下降。根据实施文档第 0 节，指标不是硬上涨门槛，本阶段不为评测样本特调分块策略；后续如继续优化，应在独立 Stage 中基于真实文档分布调整 token budget、overlap 和标题感知切分策略。

## 验证命令

- `mvn -pl rag-document,rag-admin -am "-Dtest=DocumentChunkerRegressionTest,DocumentProcessorPropertyTest,DocumentIndexingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `mvn -pl rag-common "-Dtest=RedisAsyncTaskManagerTest" test`：通过。
- `mvn -pl rag-admin -am "-Dtest=DocumentIndexingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `mvn -q test`：通过。
- `mvn -pl rag-admin -am install -DskipTests`：通过。
- 后端启动：通过，`logs/backend-stage3-fix-start.log` 显示 Tomcat 8080 started，并完成 `kbId=8` BM25 重建。
