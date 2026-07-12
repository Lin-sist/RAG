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
