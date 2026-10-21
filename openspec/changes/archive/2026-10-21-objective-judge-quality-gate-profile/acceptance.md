# C20 离线工具链与DRAFT契约验收

2026-10-21。Report status：`ACCEPTED_DRAFT_TOOLCHAIN_ACTIVATION_DEFERRED`。

## 用户接受范围

用户明确验收DRAFT profile、离线review和三轮参考intake/空阈值审阅包，并授权归档。真实参考移交[P1/W1-E2](../../../../docs/roadmap/technical-debt.md#w1-e2)，阈值/容差批准、新版本激活及judge升级required移交[P1/W1-E3](../../../../docs/roadmap/technical-debt.md#w1-e3)。适用设计D8。上述目标不再是本次工程归档前置，也没有被标为完成。

## 已接受契约与证据

| Requirement | 实现及验证入口 |
| --- | --- |
| C20 Draft Objective Profile And Independent Judge Advisory | c20-objective-judge-draft-v1.json、review_objective_judge_profile.py及test_c20_profile_review.py；7 objective required/3 judge advisory、空阈值与DRAFT边界 |
| C20 Existing Metric Reuse And Safe Local Review | evaluate_quality_gate.py既有计数/分母复用；身份/畸形计数/不同citation分母/partial judge/错误策略/安全exclusive输出测试 |
| C20 Independent Repeat Intake And Threshold Review Packet | compile_objective_gate_reference.py及9项测试；独立1/2/3、metadata/runtime/执行策略绑定、缺口槽位、完整才出FOR_REVIEW分布 |
| C20 Draft Toolchain Acceptance Without Activation | 本用户指令、proposal/design D8、W1-E2/E3与当前验收记录；归档不能推导PASS或激活授权 |

代码来源HEAD：`3f02225de582846c488a47a257ef35e34d1eec62`。复用2026-10-20同一源码/相关环境下全Python336 tests / OK、0 failures/0 errors/无跳过（28.800s），包括恢复执行的C18测试与临时目录故障回归。归档只改文档与baseline，不重复全套测试；文件级/状态核查结果见AGENT_LOG。

## 保持原样的状态与债务

- [profile](../../../../docs/eval/config/c20-objective-judge-draft-v1.json)保持v1-draft/DRAFT，targets/tolerances为null，requiredChannels仅objective，judge仅辅助，严格errorPolicy不变。
- [历史单轮intake](../../../../docs/eval/reports/c20-objective-reference-intake-v1.json)仍INCOMPLETE：缺repeat2/3、declared total1、retry31不符合策略；distribution为null、activationEligible/providerExecutionVerified为false。历史C18接受结果及C19 partial不回写。
- 即使三轮本地输入完整也只可FOR_REVIEW；DRAFT始终NOT_EVALUABLE/exit4，不是PASS。充分真实参考、数值批准、新version激活及独立接受仍必须另行完成。
- 本次新增backend/provider/embedding/rerank/ask/judge调用0、业务数据出站0；不采集三轮、不变阈值/指标/生产默认，不启动W2。

## 归档检查与边界

- 当前delta的4 requirements/10 scenarios逐字接受进evaluation baseline；历史未完成任务标明移交P1而非勾成Live/激活通过。
- OpenSpec CLI不可用，采用文件级requirement/scenario精确比对、目录/受影响链接、JSON状态/hash、历史artifact保全、Git范围及diff检查，不冒称官方validate。
- Java/前端/依赖/生产行为未变，跳过Maven/build/浏览器和Live评测；不push/PR/部署。Wave 1全员归档只表示各自已接受的工程范围收口。
