# Stage 1：可复现评测闭环

## 当前状态

本阶段已完成可复现 retrieval-only 评测闭环，并连续两次跑通同一 KB 的 live eval。

结论：两次报告均为 `RETRIEVAL_ONLY`，`retrieveErrors=0`，Recall@5/MRR 完全一致。当前真实 Stage 2 起点为 Recall@5 62.75%、MRR 0.6605。

## 已完成改动

- 新增 `scripts/run_reproducible_rag_eval.py`，负责固定名称评测 KB 的创建/清理、fixture 上传、任务与文档状态轮询、metadata 生成，并委托 `scripts/run_rag_eval.py` 产出 retrieval-only 报告。
- 扩展 `scripts/run_rag_eval.py`，新增 `--run-metadata-json`，把 KB、fixture、配置快照、Git HEAD 等信息写入报告头与 details JSON。
- 未改动 Recall@3/Recall@5/MRR/Top1 source accuracy 等指标定义。

## 固定评测资产

- Eval set：`docs/eval/rag_eval_set.jsonl`
- Fixture：
  - `test-data/springboot-basics.md`
  - `test-data/java-interview-guide.md`
  - `test-data/rag-technology-guide.md`
- 默认 KB 名称：`codex-stage1-repro-eval`
- 本次 KB：
  - kbId：`7`
  - vectorCollection：`kb_b336dd162e314e30`
  - documentCount：`3`
  - chunkCount：`40`
- 本次报告：
  - `docs/eval/reports/stage1-reproducible-eval-run1.md`
  - `docs/eval/reports/stage1-reproducible-eval-details-run1.json`
  - `docs/eval/reports/stage1-reproducible-eval-metadata-run1.json`
  - `docs/eval/reports/stage1-reproducible-eval-run2.md`
  - `docs/eval/reports/stage1-reproducible-eval-details-run2.json`
  - `docs/eval/reports/stage1-reproducible-eval-metadata-run2.json`

## 已验证项

- `python -B -m py_compile scripts/run_rag_eval.py scripts/run_reproducible_rag_eval.py`：通过。
- `python -B scripts/run_reproducible_rag_eval.py --help`：通过。
- `python -B scripts/run_rag_eval.py --help`：通过，确认 `--run-metadata-json` 已接入。
- 离线失败路径验证：使用 `http://127.0.0.1:9` 作为不可达 base URL，runner 可生成 `FAILED` 报告与 details JSON，并写入 `Eval KB name`、`Fixture files`、`Git HEAD`、`runMetadata`。
- `mvn -q test`：通过。
- Docker compose 依赖：MySQL、Redis、MinIO、etcd、Milvus 均已启动。
- 后端启动：通过，日志 `logs/stage1-live-backend.out.log` 显示 Tomcat started on port 8080。
- 登录 smoke：`POST /auth/login` 返回 200。

## Live eval 命令

用户已明确授权外部 embedding/provider 调用后运行：

```powershell
python -B scripts/run_reproducible_rag_eval.py --repeat 2 --no-overwrite --report docs/eval/reports/stage1-reproducible-eval.md --details-json docs/eval/reports/stage1-reproducible-eval-details.json --metadata-json docs/eval/reports/stage1-reproducible-eval-metadata.json
```

## 两次运行结果

| 指标 | Run 1 | Run 2 |
| --- | ---: | ---: |
| Report status | `RETRIEVAL_ONLY` | `RETRIEVAL_ONLY` |
| retrieveErrors | 0 | 0 |
| askErrors | 0 | 0 |
| skippedAsk | 30 | 30 |
| Recall@3 | 62.75% | 62.75% |
| Recall@5 | 62.75% | 62.75% |
| MRR | 0.6605 | 0.6605 |
| Top1 source accuracy | 96.30% | 96.30% |
| KB id | 7 | 7 |
| chunkCount | 40 | 40 |

## Stage 2 起点

当前 token chunker 重新入库后的真实 retrieval-only 基线为：

- Recall@5：62.75%
- MRR：0.6605
- Top1 source accuracy：96.30%
- fixture chunk 数：40

这确认了 v3 文档中提到的 token chunker 回退仍然存在，Stage 2 应以该 KB/fixture 闭环为基准做分块参数实验。

## 提交状态

准备按 Stage 1 提交：`test(评测): 脚本化可复现评测闭环，固定评测KB身份消除kbId漂移`。
