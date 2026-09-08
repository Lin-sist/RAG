# C17 完整 reference 与阈值审阅

日期：2026-09-08。Evidence：`COMPLETE`；profile：`DRAFT / PENDING_REFERENCE_EVIDENCE`；具体阈值：**待批准**。

## 已验证结果

- full3 的 Git HEAD：`082b03037fbe38122e72b1f8e5bb6493643da522`。
- `rag-eval-dev-v2` 全部150条×3，450/450 observations，三轮均为 `RETRIEVAL_ONLY`。
- retrieve/rate-limit/retry/fallback/model rerank/ask/generation/judge均为0（ask/generation/judge未执行）。
- 三轮Git/config/dataset/KB/model generation与metadata一致；details哈希、顺序、exact sample set与compiler schema检查通过。
- 新模型为 `nvidia/nemotron-3-embed-1b`，2048维、c17g3；保持heuristic rerank、topK=5/minScore=0.3。
- full3实际新增provider请求0，复用热embedding缓存，但执行了全部450次真实检索。每次检索前等待1.2秒，不计入单次检索延迟；这不是冷缓存性能基准。按用户免费账户声明直接费用0，预算已归零。

原始数据保留在ignored输出中；[完整脱敏分布](c17-retrieval-reference-review-v1.json)绑定6份原始文件的哈希；[执行摘要](c17-full-c17g3-summary.json)记录真实调用和运行条件。

## 12项规则与具体候选方案

三轮每项数值完全一致，因此下表每行的run1/run2/run3、min、median、max相等，spread均为0。精确值及每轮denominator保留在JSON。比例指标以0–1为单位；0.01表示绝对下降1个百分点，MRR为绝对下降0.01。

| 规则 | 每轮分母 | 三轮各自observed | 候选hard floor | 候选maxAbsoluteRegression |
| --- | ---: | ---: | ---: | ---: |
| overall-recall5 | 293 | 0.474402730375 | 0.45 | 0.01 |
| overall-mrr | 130 | 0.526153846154 | 0.50 | 0.01 |
| overall-top1 | 130 | 0.923076923077 | 0.90 | 0.01 |
| fact-recall5 | 76 | 0.394736842105 | 0.38 | 0.01 |
| definition-recall5 | 45 | 0.644444444444 | 0.60 | 0.01 |
| reasoning-recall5 | 88 | 0.534090909091 | 0.50 | 0.01 |
| multi-hop-recall5 | 84 | 0.392857142857 | 0.38 | 0.01 |
| no-answer-retrieve-success | 20 | 1.000000000000 | 1.00 | 0 |
| easy-recall5 | 76 | 0.500000000000 | 0.48 | 0.01 |
| medium-recall5 | 133 | 0.488721804511 | 0.47 | 0.01 |
| hard-recall5 | 84 | 0.428571428571 | 0.40 | 0.01 |
| answerable-recall5 | 293 | 0.474402730375 | 0.45 | 0.01 |

这是一项提交审阅的开发态回归方案，不是已批准策略。hard floor给出可读的绝对下限，reference tolerance同时约束相对当前median的退化，二者必须同时满足。1个百分点是提议的工程容忍度，不是从三次相同结果估计出的统计置信区间；20条no-answer要求检索执行全部成功，不容许退化。

Recall分母293是预期context条目数，不是150条问题；MRR/Top1分母为130条answerable。no-answer检索成功率100%只表示20次检索无执行错误，不能当作拒答正确率。总体Recall@5仅47.44%，fact/multi-hop约39%，本方案即使批准，也只能防止当前开发基线继续退化，不能证明生产质量足够、语义检索全面正确或generation/citation/judge达标。三轮热缓存一致也不证明跨时间、跨provider版本稳定性。

## 故障与修复证据

1. Docker启动在`dockerInference`失效socket处失败。只将含两个0字节socket的run目录保留为备份并重建空目录；五个原容器恢复healthy。没有factory reset、prune、容器/镜像/卷删除或WSL磁盘变更。复发的更深层原因未证实。
2. full1第二轮遇本地60次/60秒用户限流，96个HTTP429，第三轮未启动。新增432次NVIDIA请求均200。旧runner漏计retrieval429，raw未修改。
3. `73566c9`补充1.2秒节奏与429计数。full2完成450/450但公开指标契约被误脱敏，compiler正确拒绝为NOT_COMPARABLE。
4. `082b030`仅允许完整精确匹配的公开metric descriptor保留；任意同名token/secret仍脱敏。244项离线测试通过，包含serializer→compiler与泄露负例。full3重新完整执行并获得COMPLETE。

[失败尝试摘要](c17-full-attempts-c17g3-summary.json)与成功full3分开保存，不拼接成功子集、不回填旧raw。

## 归档前剩余步骤

- 人工批准上述或另行指定的12项hard floor/tolerance。具体数值未批准前不改变canonical profile。
- 将profile提升为v1/ACTIVE/APPROVED，生成绑定其最终SHA-256的locked median reference。
- 原始三轮离线重放全部required rules PASS，完成fail-closed负例与隐私/链接验证。
- 最终验收后同步accepted baseline、项目说明与归档状态。C17尚未归档，也未开始C18。

本次已经执行244项Python测试；Java/frontend代码未变，未重复Maven/frontend build。未进行push/PR/deploy。
