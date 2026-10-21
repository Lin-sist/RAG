# C19 离线工具链与执行契约验收

2026-10-21。Report status：`ACCEPTED_OFFLINE_SCOPE_LIVE_DEFERRED`。

## 用户接受范围

用户明确批准将C19按已完成的零外调离线工具链/执行契约验收并归档；完整Live样本及正式校准结论移交[P1/W1-E1](../../../../docs/roadmap/technical-debt.md#w1-e1)。这是对工程验收范围的明确缩窄，不是把历史partial改为COMPLETE。适用设计D9。

## 已接受契约与证据

| Requirement | 实现及验证入口 |
| --- | --- |
| C19 Frozen Calibration Execution Identity | c19_judge_contract.py；test_c19_judge_contract.py覆盖manifest/endpoint/source/runtime/selection与纯本地plan/preflight |
| C19 Bounded Durable Judge Attempts | 独立4/72预算、请求前reserve、exclusive目录/逐次ledger、首错停止/零重试/无redirect；合成transport验证预算前拒绝、保全与gold不出站 |
| C19 Independently Compiled Safe Evidence | compile_judge_calibration_evidence.py及对应测试；缺失/重复/畸形/漂移抑制agreement，完整低值不篡改，输出脱敏且不覆盖 |
| C19 Offline Acceptance And Deferred Live Evidence | 本用户指令、proposal/design D9、P1/W1-E1与当前验收记录；归档不产生新Live授权 |

代码来源HEAD：`3f02225de582846c488a47a257ef35e34d1eec62`。复用同一源码和相关环境下已完成的全Python336 tests / OK、0 failures/0 errors/无跳过（2026-10-20，28.800s）。本次只改契约说明/归档治理，不因收尾重复全套测试；本地plan和文件级核查结果见AGENT_LOG。

## 明确保留的证据债务

- 最后一轮仍为24 attempts、23 valid、1 network error、48未调用、retry0；累计83 attempts/80 HTTP200/1 timeout/2 network errors。完整24×3校准尚未建立，正式agreement/repeat率N/A；错误根因、失败远端完成及费用未知。
- [最终安全review](../../../../docs/eval/reports/c19-final-full-review-v1.json)及所有旧raw/安全报告保留原状态与hash，不拼接、不续跑、不更新为本次归档结果。
- 未来补测须独立范围、当前身份核对及外调授权；本次新增backend/provider/embedding/rerank/ask/judge调用0，业务数据出站0。现有执行默认/模型/阈值未改，judge不升级为required门禁。

## 归档检查与边界

- 当前delta的4 requirements/9 scenarios逐字接受进evaluation baseline；历史执行任务在tasks中保留，未完成Live目标明确移交，不勾为已执行。
- OpenSpec CLI不可用，采用requirement/scenario精确比对、目录结构、受影响链接、Git范围、历史artifact保全与diff检查；不称官方validate。
- Java/前端/依赖/生产行为未变，跳过Maven/build/浏览器与真实业务验收；不push/PR/部署。归档仅证明已接受的离线工程范围。
