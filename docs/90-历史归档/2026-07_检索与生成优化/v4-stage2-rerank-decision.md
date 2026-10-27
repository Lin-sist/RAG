> 状态：历史参考（2026-10-27 归类）。以下日期、批准状态、授权和执行结论属于各自原始记录；归类不撤销历史批准，也不启动后续任务。
> 原位置：`docs/optimization/v4/stage2-rerank-decision.md`。本次保留正文与证据边界，仅调整可点击引用；现行资料从[规划与调研](../../01-规划与调研/)进入。

# Stage 2：真实 Rerank Provider 触发判定

## 状态

**跳过（2026-07-12，触发条件不满足）。**

`ModelReranker` HTTP adapter、健康检查、超时和 heuristic 降级已经存在，但当前默认配置仍为：

- `rag.rerank.provider=heuristic`
- model reranker endpoint 未配置
- model reranker API key 未配置
- 没有可用于真实 A/B 的 provider 凭据

根据 `docs/optimization/v4/plan.md` 的条件触发规则，本阶段不伪造 provider、不复用 LLM key 代替 rerank key，也不通过 mock 结果宣称业务收益，因此本轮正式跳过，不产生 rerank 代码改动或外部调用。

## 后续重新触发条件

取得真实 rerank provider、endpoint、model 和凭据后，再单独执行：

1. heuristic 与 model 使用同一 KB、fixture、topN/topK 和检索配置。
2. 各跑 retrieval-only 与 generation/citation 对比。
3. 记录 Recall@5、MRR、Top1、citation/no-answer、延迟、错误率和降级次数。
4. 只有在证据支持且用户确认后，才允许修改默认 provider。
