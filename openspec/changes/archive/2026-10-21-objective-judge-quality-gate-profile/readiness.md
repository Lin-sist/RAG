# C20零外调切片执行结果

> 2026-10-21当前状态：用户已接受DRAFT契约及离线review/intake工具链，按`ACCEPTED_DRAFT_TOOLCHAIN_ACTIVATION_DEFERRED`归档，见[验收](acceptance.md)。真实参考转P1/W1-E2，阈值/激活/judge升级转W1-E3；同一源码复用2026-10-20全Python336项通过。下方“不归档/保持ACTIVE”和302项失败为历史记录，不再表示当前工程状态；profile仍DRAFT，历史intake仍INCOMPLETE。

2026-10-18新增参考intake切片已实现，归档仍未就绪：真实三轮参考证据、逐规则数值/容差批准、ACTIVE激活验收仍未完成。见[单轮intake安全报告](../../../../docs/eval/reports/c20-objective-reference-intake-v1.json)。C19仍停放，未新增外调。

2026-10-18。Report status：`OFFLINE_SLICE_IMPLEMENTED_PROFILE_DRAFT`。用户授权的首切片已实现；整个change保持ACTIVE，数值批准/参考重复证据/ACTIVE激活/验收归档尚未完成。C19 live未完任务停放，不接受或拼接其partial。

## 已确认

- [DRAFT配置](../../../../docs/eval/config/c20-objective-judge-draft-v1.json)为原生C10格式，full150、generation/citation、requiredChannels仅objective；7项objective/3项judge辅助，target/tolerance全null。
- 共享C10支持既有keyword/citation/no-answer计数，严格拒绝bool/负数/不可能计数/无效值；来源citation只应答样本，snippet及unsupported为全成功ask，和C9a分母一致。optional judge通道不完整或三字段缺测时SKIPPED，不改变objective完整性；required judge旧错误原因保留。
- [离线review](../../../../scripts/review_objective_judge_profile.py)只读本地，校验dataset/selection/run身份；身份错不出观测。测量完整性、错误策略及DRAFT状态独立，输出受限/no-overwrite，异常不带原始消息/路径。
- [安全烟测报告](../../../../docs/eval/reports/c20-objective-judge-draft-review-v1.json)：C18原件hash匹配接受报告，150样本7项指标逐值相同；objective观测完整，judge辅助UNAVAILABLE，gate=NOT_EVALUABLE/exit4、policyBlocker=retry_count_exceeded（历史31次），未自动改策略或批准阈值。当前C18 compiler未重验通过，不混淆历史接受与当前源码审计。

## 验证与限制

- `python -B -m unittest discover -s scripts -p 'test_*quality_gate.py'`：22/22通过。
- `python -B -m unittest discover -s scripts -p 'test_c20_profile_review.py'`：14/14通过，含身份/分母/畸形/partial judge/安全输出/输入不可覆盖等。
- `python -B -m unittest discover -s scripts -p 'test_*.py'`：293项，1 failure/4 errors，均原有C18 budget_source_drift集合（含2个setUpClass）；无新增失败，不称全仓GREEN。raw输出仅ignored tmp/eval/c20-full-unittest.log。
- 初次新增judge全字段检查误改required规则错误原因，聚焦测试检出；已收窄到optional judge后22/22通过。来源citation与snippet分母差异通过非对称合成样本验证。
- C20阶段真实provider/backend调用0。Java/前端/依赖/生产默认未改，跳过Maven/正式前端build/浏览器；OpenSpec CLI不可用，采用文件级检查，不冒称官方validate。无push/PR/部署，无受保护本地配置或凭据改动。

## 可复现命令

```powershell
python -B scripts/review_objective_judge_profile.py --details tmp/eval/c18/20260922-r11-full-live-details.json
```

预期exit4是DRAFT未激活，并非执行故障；用--output指定全新仓库内文件可保存安全JSON，存在或与输入同路径则拒绝。输入必须是完整普通评测details，不能使用C19校准摘要或将历史raw回写为新baseline。

## 下一切片：三轮参考intake与阈值审阅包

- [编译器](../../../../scripts/compile_objective_gate_reference.py)只读本地profile与0至3对details/metadata；检查实际dataset/questionSet、完整顺序、metadata与embedded绑定、三轮index/total、不同文件hash/运行时间、干净Git来源、provider/执行manifest/config/KB/metric身份及原严格策略。只有全三轮完整且可比时出min/median/max/spread，其余保留槽位/分母、distribution=null。
- FOR_REVIEW只表示输入编译完整，不代表provider执行已独立核验或可激活；providerExecutionVerified/activationEligible恒false，target/tolerance始终null，gateStatus NOT_EVALUABLE/exit4。非法输入exit2，重复/漂移NOT_COMPARABLE，缺轮/策略不合格INCOMPLETE；不覆盖输入或旧输出。
- 新聚焦`test_compile_objective_gate_reference.py` 9/9通过，含完整synthetic、缺轮、复用、重复时间、模型/执行策略漂移、source/metadata/dataset不符、畸形输入、安全保全；最终全Python302项仍既有C18 1 failure/4 errors，无新增失败。
- C18单轮details和metadata hash均匹配接受报告；intake为INCOMPLETE，缺repeat2/3、total1及retry31策略不符。7项只保留单轮诊断值，其余槽位null，无质量分布/数值批准；当前C18 compiler未重验，不伪造或拼接三轮。
- 本地原件核查修正了兼容边界：questionSet路径按仓库内解析结果规范化（hash/bytes仍严格相等）；metadata.c18Execution是策略而details.c18Execution是attempt账本，策略通过完整runMetadata绑定，不把两者错误比较。各类负例及实际C18 intake均通过。

```powershell
python -B scripts/compile_objective_gate_reference.py --details tmp/eval/c18/20260922-r11-full-live-details.json --metadata tmp/eval/c18/20260922-r11-full-live-metadata.json
```

预期exit4/INCOMPLETE。真正三轮候选使用三对--details/--metadata，必须来自独立且身份相容的完整运行；本切片不采集这些运行或发起外调。现有draft profile及首切片安全report未改，Java/前端/依赖/生产行为未改，跳过Maven/build/浏览器；官方OpenSpec CLI仍不可用。C20保持ACTIVE，不归档/置IDLE。
