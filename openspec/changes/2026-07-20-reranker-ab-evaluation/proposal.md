# Proposal: C7 Reranker A/B Evaluation

## Status

- Change type：Type C 重大变更。
- 当前阶段：corrected-host canary 与 full `R=3,W=3` 均已完成。Full 的 heuristic/model 各 3 个 measured runs、每 arm 总计 3 次 warm-up，comparison=`COMPARABLE`；当前等待用户验收 live evidence 与结论边界，尚未接受 delta 或归档。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Summary

C6 已经解决“模型重排是否真的被调用、失败后是否 fallback、最终由哪个 provider 决定顺序”的归因问题，但尚未回答 NVIDIA model reranker 相比默认 heuristic 是否带来可复现收益。C7 将在同一评测身份下分别产生 heuristic arm 与 model arm 的 retrieval-only 结果，再通过离线 comparator 校验身份、provider 覆盖、样本配对和延迟口径，输出 Recall@5、MRR、Top1 与 P50/P95 的可比较差异。

C7 不修改默认 provider，也不把一次单样本 smoke 或含 fallback 的结果包装成收益证据。

## Why

当前具备以下基础：

- C6 已实现 NVIDIA ranking adapter、typed outcome、整样本 fallback 和逐样本 attribution；
- runner 已保存 requested/effective provider、fallback、model calls、candidate coverage 与 `rerankLatencyMillis`；
- 可复现 runner 已记录 KB、fixture、配置文件 hash 与 Git HEAD；
- 30 条评测样本和 3 份 fixture 可用于当前 retrieval-only 基线。

但当前仍缺少：

- eval-set 内容 hash 与 sanitized runtime rerank 配置身份；
- heuristic/model 两个 arm 的严格身份匹配与允许差异白名单；
- model arm 100% effective-model 覆盖的干净比较规则；
- P50/P95 与逐样本配对延迟；
- 对 fallback、样本缺失、retrieve error、重复次数不一致的 `NOT_COMPARABLE` 判定；
- 一份不依赖人工抄数的离线 A/B comparator 和紧凑证据产物。

## Current State Classification

### confirmed

- `main` 与 `origin/main` 同步，规划启动前工作区干净。
- `.ai/ACTIVE_TASK.md` 为 `IDLE`，无其他 active change。
- C6 的 4 个 requirements / 11 个 scenarios 已接受进 `rag-system` baseline 并归档。
- 归档后 1 次纯合成 NVIDIA hosted smoke 已验证当时的 endpoint/auth/schema 与 adapter 解析。
- 当前默认 reranker 仍为 heuristic。
- 当前 Python runner 能逐样本保存 rerank attribution 和 rerank stage latency。

### partial

- 可复现 metadata 已有 KB/fixture/config/Git HEAD，但 eval-set 只有路径、没有内容 hash，运行时 override 也没有安全快照。
- runner 有逐样本 rerank latency，但 aggregate 没有 P50/P95，也没有 debug retrieval 端到端 latency。
- runner 有单次评测报告，但没有 arm pairing、identity diff 和 comparison validity。

### planned

- 增加安全的 A/B arm manifest、身份签名和 observed attribution 交叉校验。
- 增加逐样本 latency、P50/P95 和离线 comparator。
- 在用户单独授权后，使用固定 KB/fixture/config/Git HEAD 执行 heuristic/model retrieval-only A/B。

### out_of_scope

- 不修改生产默认 heuristic provider，也不自动切换默认 provider。
- 不新增运行时“按请求选择 provider”的 API，不自动重启或部署后端。
- 不修改 embedding、chunking、hybrid/RRF、prompt、citation、no-answer 或 judge 口径。
- 不扩充或重新标注评测集；schema/versioning 留给 C8a，100~300 条扩充留给 C8b。
- 不做 claim-level 指标、judge 校准或通用质量阈值/退出码门禁；分别留给 C9/C10。
- 不运行 generation/citation A/B，不调用 `/api/qa/ask` 或 LLM judge。
- 不引入 provider retry、并发压测、生产 SLA 或成本基准。
- 不修改前端、SSE、数据库、索引状态机、Java provider/API 契约或依赖。

### unknown

- 当前真实评测 KB、backend 与 Docker 在正式执行时是否 ready，需在获批执行后用 mutation-free preflight 复核。
- 当前 NVIDIA 配额、限流与费用/零费用依据尚未在 C7 执行前确认。
- 正式 latency run 的重复次数与 warm-up 预算等待用户在事前闸门确认。

## 用户故事（大白话）

改之前，即使报告里写着“开启了 rerank”，也可能部分样本已经悄悄回退到 heuristic；两份报告还可能用了不同 KB、配置或代码，直接比较数字会得出假结论。改之后，系统先证明两组实验除了 reranker 外身份完全一致，再证明 model arm 的所有可重排样本确实由模型生效，最后才展示质量和延迟差异；任何 fallback、样本缺失或身份漂移都会把比较明确标成不可比较，而不是硬说模型更好。

## Goals

1. 固定并校验 A/B 的 eval-set、selected samples、fixture、KB、retrieval config、Git HEAD 和重复次数。
2. 让 heuristic/model 两个 arm 使用现有 retrieval-only runner 独立运行，不增加线上 provider 切换接口。
3. 对每个 `runIndex + sampleId` 记录 retrieval 与 rerank latency、provider attribution、候选覆盖和 retrieval 指标事实。
4. 只有 heuristic arm 干净且 model arm 对全部 rerank-eligible 样本 100% effective-model、0 fallback 时，comparison 才能为 `COMPARABLE`。
5. 输出 Recall@5、MRR、Top1、rerank P50/P95、retrieval P50/P95 和逐样本配对差异。
6. 产出紧凑、安全、可审查的 Markdown/JSON comparison evidence，不记录 secret 或 raw provider response。

## Proposed Scope

### Evaluation runner

- `scripts/run_rag_eval.py`
  - 测量每次 debug retrieval 的 wall-clock latency；
  - 保留现有 `rerankLatencyMillis`，新增明确区分的 `retrieveLatencyMillis`；
  - 聚合 latency count/P50/P95，并保持既有 retrieval/generation/citation/no-answer/judge 指标公式不变。
- `scripts/run_reproducible_rag_eval.py`
  - metadata 增加 eval-set SHA-256、selected sample IDs/order、repeat/run index 和 arm manifest hash；
  - 要求 C7 arm 提供不含凭据的 sanitized runtime rerank manifest；
  - 继续保持 `--preflight-only` mutation-free、`--keep-existing` reuse-only。
- 新增 `scripts/compare_reranker_ab.py`
  - 只读取两个 arm 的 metadata/details，不联系 backend/provider；
  - 校验 identity、run/sample pairing、report status、retrieve errors、provider coverage 与 fallback；
  - 输出 `COMPARABLE / NOT_COMPARABLE / FAILED`、原因列表、arm summaries 与 deltas。

### Tests and documentation

- 扩展 `scripts/test_run_rag_eval.py` 与 `scripts/test_run_reproducible_rag_eval.py`。
- 新增 comparator 单元测试，覆盖身份漂移、partial fallback、zero-candidate、样本缺失、延迟 percentile 和敏感字段过滤。
- 更新 `docs/eval/RAG_EVAL_GUIDE.md`，写清两 arm 启动、preflight、canary、full run、compare 与授权顺序。
- 实际 A/B 获批并完成后，再生成 C7 summary/compact JSON evidence，并同步长期说明。

## Comparison Validity

干净比较必须同时满足：

- 两个 arm 的 strict identity fields 完全一致；
- 两个 arm 的 run count、sample IDs、sample order 完全一致；
- 每个 arm 的 per-run report status 为 `RETRIEVAL_ONLY`，retrieve error 为 0；
- heuristic arm 的 rerank-eligible 样本 requested/effective provider 均为 heuristic、fallback=0、model calls=0；
- model arm 的 rerank-eligible 样本 requested/effective provider 均为批准的 model provider、fallback=0、model coverage=100%；
- zero-candidate 样本在两 arm 中一致并单列为 not-applicable，不用于抬高或压低 model coverage；
- 不删除、替换或只挑“模型成功”的样本。

任何身份不一致、部分 fallback、缺样本、重复次数不一致或 retrieve error 都必须输出 `NOT_COMPARABLE` 或 `FAILED`，但仍可保留降级诊断，不得宣称干净收益。

## External Call Gate

本规划阶段和离线实现阶段真实外部调用量均为 0。

真实执行必须再次向用户披露并单独获批。预算公式：

- `N`：样本数；当前为 30；
- `R`：每 arm 的 measured repeats；
- `W`：每 arm 不计入指标的 warm-up calls；
- debug retrieval 上限：`2 × N × R + 2 × W`；
- query embedding 上限：不超过 debug retrieval 上限；
- model rerank 上限：`N × R + W`；
- ask、judge、LLM generation：0。

最低功能比较 `R=1, W=0` 时，上限为 60 次 debug retrieval、60 次 query embedding、30 次 model rerank。用户已于 2026-07-20 选择 latency 证据方案 `R=3, W=3`，上限为 186 次 debug retrieval、186 次 query embedding、93 次 model rerank；NVIDIA NIM 费用依据为用户确认的免费使用，但速率与并发限制仍需通过串行 canary 观察。

出站数据包括 30 条固定评测问题；model arm 还会把固定 fixture 检索得到的候选 passages 发送给批准的 rerank provider。不得发送其他知识库、用户或生产数据。执行前还必须记录 provider、model、endpoint path、timeout、truncate、限流、费用及零费用依据、是否 retry；API key 和 Authorization 不得进入 tracked file、日志或回复。

首轮 canary 实际使用 12 次 debug retrieval、至多 12 次 query embedding、6 次 NVIDIA rerank，ask/judge/LLM generation 为 0，串行且无自动 retry。Heuristic 的 3 次 warm-up + 3 次 measured 均为 requested/effective heuristic、fallback=0、model calls=0；NVIDIA 的 3 次 warm-up + 3 次 measured 均 requested nvidia，但 effective heuristic、fallback=`http_4xx`、model calls=1，因此不能陈述收益。运行时使用了 `https://integrate.api.nvidia.com` 作为 rerank base URL，而 NVIDIA 当前官方模型 API reference 指向 `https://ai.api.nvidia.com`；这是高置信根因推断，仍须通过获批的 corrected-host model-only canary 证明，不能把推断写成已确认修复。

Corrected-host model-only canary 另使用 6 次 debug retrieval、至多 6 次 query embedding、6 次 NVIDIA rerank，ask/judge/LLM generation 为 0，串行且无自动 retry。Warm-up 与 measured 合计 6/6 requested/effective nvidia、fallback=0、model calls=1、candidate coverage=100%；clean pair comparator 为 `COMPARABLE`，证明 host 修正确实恢复了当前模型调用链。3 样本 canary 的 Recall@5/MRR/Top1 delta 均为 0，model rerank P50/P95 为 349/351ms；该小样本只用于 provider/身份闸门，不能替代 30 样本 full 结论。

Full A/B 在 Git HEAD `fb18b6bd5448db6e0985f98f44268da84195bb1b`、固定 KB 15、相同 fixture/config/eval-set 下按 `H1/N1、N2/H2、H3/N3` 执行。Heuristic measured 90/90 为 effective heuristic、model calls=0；NVIDIA measured 90/90 为 effective nvidia、model calls=1、coverage=100%；两 arm warm-up 各 3 次也 clean，fallback/retrieve error/missing pair 均为 0。Model 相对 heuristic 的 Recall@5 从 68.63% 升至 76.47%（+7.84pp），MRR 从 0.7346 升至 0.8241（+0.0895），Top1 从 96.30% 升至 100%（+3.70pp），三个 repeat 的质量指标完全一致。Model rerank stage P50/P95 为 363/688ms；overall retrieval P50 为 985ms，较 heuristic 797ms 增加 188ms。Heuristic H1 在 Docker 冷启动后出现 P95=14484ms 的异常，导致 aggregate heuristic/model retrieval P95 为 5203/2796ms；该 aggregate P95 不能解释为 model 尾延迟更快。

## Acceptance Criteria

### Planning gate

- proposal/design/tasks/evaluation spec delta 齐全且互相一致；
- design 含全部真实技术取舍的三行决策记录；
- `.ai/ACTIVE_TASK.md` 指向本 change；
- 本阶段真实 provider 调用量为 0；
- 用户明确批准 scope/non-goals、决策记录、spec delta 与实现授权后，才能改 runner。

### Implementation gate

- 所有新逻辑先有 RED，再有最小 GREEN；
- Python 全量单测、SensitiveLogs、`git diff --check` 通过；
- 若无 Java/POM 改动，前端 build 与 Java 聚焦测试可按范围跳过，但最终仍运行一次 `mvn -q test` 做仓库回归并如实记录 skip；
- plan-only、preflight-only 和离线 comparator 证明不会产生 provider 调用；
- 未获真实外调授权时，不执行 canary 或 full A/B。

### Live evidence gate

- 先通过实际 KB 的 mutation-free preflight；
- 先运行获批的小样本 canary，确认 requested/effective provider、fallback、model call 与候选覆盖；
- canary 通过且调用预算仍获批准后才运行 full arms；
- comparator 必须先给出 `COMPARABLE`，才能陈述观察到的质量/延迟差异；
- 即使观察到收益，也不在 C7 自动修改默认 provider或建立通用质量门禁。

## Risks

- 30 条开发样本规模小，结论只能适用于当前固定 fixture/问题集，不能外推为生产收益。
- provider、网络、JIT、连接与缓存会影响 wall-clock latency；因此 rerank stage latency 是主口径，整体 retrieval latency 只作辅助诊断。
- 运行时配置 manifest 由执行流程提供，仍可能与实际 backend 启动参数不一致；必须用 observed provider/model/protocol attribution 交叉校验，并在无法证明时判为不可比较。
- model arm partial fallback 会污染质量与延迟分布；C7 不做“剔除失败样本后继续比较”。
- C8a 尚未建立完整 dataset schema/versioning；C7 只补本次比较所需的 hash/identity，不提前扩张 C8a。

## Rollback

- 离线 runner/comparator 改动可独立回退，不影响 backend 数据与默认 provider。
- 真实 A/B 只复用固定 KB，不创建、上传、删除或重建资源。
- 若 model provider 不可用，停止 live run并保留 `NOT_COMPARABLE` 证据；无需回滚业务数据。
