# Stage 2：分块参数专项调参

## 当前状态

本阶段已完成分块参数 live eval 矩阵。结论：在不改动 RRF、rerank、检索排序逻辑的前提下，`document.chunking.chunk-size=420`、`document.chunking.chunk-overlap=80` 是本轮最佳参数。

该参数将 fixture chunk 数从 Stage 1 的 40 提升到 50，Recall@3/Recall@5 从 62.75% 恢复到 68.63%，MRR 从 0.6605 提升到 0.7346。更大的 chunk 参数会显著降低召回。

## 已完成改动

- 新增 `DocumentChunkingProperties`，通过 `document.chunking.*` 绑定索引链路默认分块参数。
- `DocumentProcessorImpl#process(DocumentInput)` 改为使用运行时分块配置；显式传入 `ChunkConfig` 的调用路径保持不变。
- `application.yml` 默认分块参数调整为 `chunk-size=420`、`chunk-overlap=80`、`strategy=semantic`。
- `start-backend.ps1` 在启动 `rag-admin` 前安装内部依赖模块，避免本地 live eval 误用 Maven 本地仓库里的旧 `rag-document` jar。
- `start-backend.ps1` 对 8080 端口进程瞬时退出场景做容错，降低串行 live eval 的端口竞态。

## 验证命令

```powershell
mvn -pl rag-document "-Dtest=DocumentChunkingPropertiesTest,DocumentChunkerRegressionTest" test
mvn -q test
```

用户明确授权 4 组 chunk 参数批量 live eval 后，使用可复现评测 runner 分别重建评测 KB 并运行 retrieval-only eval。

## Live Eval 矩阵

| 参数 | KB | chunkCount | Recall@3 | Recall@5 | MRR | Top1 source accuracy | 状态 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| 500 / 50 | `codex-stage2-chunk-500-50` | 40 | 62.75% | 62.75% | 0.6605 | 96.30% | `RETRIEVAL_ONLY` |
| 640 / 96 | `codex-stage2-fill-chunk-640-96` | 31 | 50.98% | 54.90% | 0.5833 | 96.30% | `RETRIEVAL_ONLY` |
| 700 / 100 | `codex-stage2-chunk-700-100` | 28 | 43.14% | 47.06% | 0.4352 | 96.30% | `RETRIEVAL_ONLY` |
| 420 / 80 | `codex-stage2-fill-chunk-420-80` | 50 | 68.63% | 68.63% | 0.7346 | 96.30% | `RETRIEVAL_ONLY` |

各组 `askErrors=0`、`retrieveErrors=0`、`skippedAsk=30`。当前 runner 仍处于 retrieval-only 模式，generation/citation 指标未纳入本阶段结论。

## 分文档 Chunk 数

| 参数 | springboot-basics.md | java-interview-guide.md | rag-technology-guide.md | 总数 |
| --- | ---: | ---: | ---: | ---: |
| 500 / 50 | 19 | 10 | 11 | 40 |
| 640 / 96 | 16 | 7 | 8 | 31 |
| 700 / 100 | 14 | 6 | 8 | 28 |
| 420 / 80 | 25 | 11 | 14 | 50 |

## 证据文件

- `docs/eval/reports/stage2-chunk-500-50.md`
- `docs/eval/reports/stage2-chunk-500-50-details.json`
- `docs/eval/reports/stage2-chunk-500-50-metadata.json`
- `docs/eval/reports/stage2-chunk-640-96.md`
- `docs/eval/reports/stage2-chunk-640-96-details.json`
- `docs/eval/reports/stage2-chunk-640-96-metadata.json`
- `docs/eval/reports/stage2-chunk-700-100.md`
- `docs/eval/reports/stage2-chunk-700-100-details.json`
- `docs/eval/reports/stage2-chunk-700-100-metadata.json`
- `docs/eval/reports/stage2-chunk-420-80.md`
- `docs/eval/reports/stage2-chunk-420-80-details.json`
- `docs/eval/reports/stage2-chunk-420-80-metadata.json`

## 结论

本阶段接受 `420 / 80` 作为新的索引默认分块参数。它恢复到 v3 文档提到的 68.63% 召回水平，并明显优于 Stage 1 可复现基线 62.75%。本阶段未实现“超过 68.63%”，后续若要继续突破，应进入检索召回策略或 rerank 专项，而不是继续单纯放大 chunk。
