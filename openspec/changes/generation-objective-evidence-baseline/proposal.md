# Proposal: C18 Generation Objective Evidence Baseline

## 目标与用户故事

C17已有可重放的retrieval开发回归门禁，但检索成功不等于回答正确。C18在judge关闭的条件下，为已冻结的 `rag-eval-dev-v2` 150条问题建立首个完整真实generation/citation/objective claim support/no-answer基线，分别报告执行完整性和质量数值，为C20提供evidence，不在本阶段设新质量阈值。

改前：v2没有完整真实生成证据，历史v1/30条和C17 retrieval-only不能代替。改后：一个固定身份的150条完整run，可追溯observation、分母、provider/model、错误和实际预算，失败不被成功子集掩盖。

## 启动事实与能力分类（2026-09-10）

- `confirmed`：C17的4 requirements/12 scenarios已接受进evaluation baseline，归档于 `openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/`。profile=v1/ACTIVE/APPROVED，三轮各12/12离线PASS，Python244 tests通过。
- `confirmed`：Git HEAD=`38c5d1fed4ffac96444118848396b9fa4173d6df`；C17收尾23个文件已暂存，本地commit被自动审批拒绝，尚无新提交hash、工作树非clean。允许文档规划，不允许以此工作树生成clean-HEAD live baseline。
- `confirmed`：runner支持include-ask、judge-mode=off、keep-existing、plan/preflight-only、no-overwrite和max-ask-retries=0；ask显式enableCache=false。C9a词法指标与C9b分通道状态已接受。
- `partial`：通用estimate_live_calls只报告debugRetrieve/ask/llmJudge，缺完整embedding/generation上限；C17 manifest锁retrieval-only，不能复用为C18。
- `partial`：ask再次检索；无context的解释类问题可能进入retryExplanatoryRetrieval，对fallback queries再次生成variants。这是既有算法回退，不是HTTP自动重试，也不能漏算或为了预算而禁用。
- `confirmed`：REST ask增加query count，并按既有成功条件写历史。enableCache=false不等于零持久化；需要在live授权中披露专用fixture KB上的这些写入。
- `planned`：C18独立manifest、预算审计、fail-fast计划、调用护栏、完整性compiler和脱敏summary。
- `unknown`：runtime LLM endpoint/model/auth/配额、实际timeout、解释回退后精确embedding最坏上限及实际计数覆盖。tracked默认不等于live fingerprint。
- `out_of_scope`：修改题目/标注/fixtures、检索/分块/rerank/prompt/citation/no-answer公式、开启Router、judge校准、新objective profile、其他KB重建/清理和生产部署。

## 实施切片

1. **W0 零外调审计与冻结**：按实际Java逻辑枚举初始variants和所有可达解释回退variants，核对debug/ask调用图、generation重试及history副作用，冻结数据/代码/配置hash、预算与safe identity。
2. **W1 离线工具**：独立c18-generation-objective-v1 manifest/schema、runner参数与parent退出码、provider调用前预算护栏、compiler和必要测试。契约冲突在backend调用前失败。
3. **W2 真实canary**：实现获批、离线验证通过后披露runtime及精确预算，独立授权固定5条；干净完成后才申请full。
4. **W3 正式evidence**：新no-overwrite身份，一次完整150条run，judge=0、retry=0；失败保留并停止，后续新尝试需明确授权，不拼接canary或旧成功样本。
5. **W4 验收**：compiler完整性/身份通过，分别报告各通道；用户验收后接受baseline并归档。C18数值不成为C20阈值。

## 调用与数据边界

| 阶段 | debug retrieval | ask | generation上限 | query embedding上限 | judge/model rerank |
| --- | ---: | ---: | ---: | --- | ---: |
| 当前规划、W0/W1审计/测试、compiler | 0 | 0 | 0 | 0 | 0 |
| 后续mutation-free HTTP preflight | 0 | 0 | 0 | 0 | 0 |
| canary，5条×1 | 5 | 5 | 5 | W0冻结：2×11 + E_fallback(canary) | 0 |
| full，150条×1 | 150 | 150 | 150 | W0冻结：2×451 + E_fallback(full) | 0 |

11/451来自C17相同查询代码的初始variants审计，是待重验的组成项；22/902不是最终授权上限。E_fallback为集合中所有可达解释fallback query的完整variant数之和，保守计入最坏分支，不按热缓存扣预算。缺精确整数或源码hash漂移时禁止live。无context时允许既有确定性no-answer而不调用模型，必须记明原因，不能伪称150次LLM均执行。

出站仅限tracked开发问题、确定性变体、用于生成的fixture contexts与固定prompt，到经核验的NVIDIA endpoint，禁止真实业务数据。NIM费用沿用用户“免费且无支付方式”声明，直接费用按0记录；不推断无限配额/SLA，账户与模型适用性在live前核实。preflight可登录/刷新会话但不修改KB/文档；正式ask授权须包含query count/history写入，默认不清理历史、不重建KB。

## 风险与验收

- 预算低估：W0覆盖ask嵌套检索和解释回退；请求发出前限额，缓存命中与实际HTTP分别计数。
- 部分成功误判：exact150 ID/order、details/metadata hash/identity、zero errors、objectiveMetricStatus=COMPLETE、judgeMetricStatus=SKIPPED、Report status=CLEAN共同约束；完整低分是有效测量，不是执行失败。
- 状态混淆：no-answer回答正确性单列；claim-lexical-v1只证明词法对齐，不是语义蕴含。保留各分母和局部NOT_APPLICABLE，judge未运行不记0分。
- 运行污染：提交形成clean HEAD后才live；C17 profile/reference与历史raw保持不变。

## 授权与提交责任

当前仅C18规划，零业务外调、零运行时修改。proposal/design/tasks/spec delta待审阅；W0/W1实现、canary、full、最终验收/归档分别按范围授权，C17外调授权不继承。

C18规划提交责任：**Agent 提交**（2026-09-11用户明确授权本地提交并保持Git干净）。本轮提交规划及交接文档，用户另行开始实现；push/PR/部署未授权。提交信息：`docs(openspec): 规划C18生成与客观质量基线`。

## 2026-09-11 提交交接更新

上方2026-09-10启动盘点保留为当时事实。C17已提交为`9276d114058664a3cf33f26fecca046e68e566a4`；提交阻断已解除，C18规划将独立本地提交。当前仍没有C18实现或真实生成evidence；精确嵌套预算/runtime fingerprint与live授权要求不变。
