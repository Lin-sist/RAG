# C19 full超时后的恢复审阅包

2026-10-18。Report status：`FULL_TIMEOUT120_INCOMPLETE_NETWORK_ERROR`。已按恢复指令独立执行120秒full：8个HTTP200有效响应、第9次judge_network_error、余63未调用，零重试/首错停止，compiler INCOMPLETE。见[本次full安全review](../../../docs/eval/reports/c19-full-timeout120-review-v1.json)；原[full计划](../../../docs/eval/reports/c19-full-timeout120-plan-v1.json)保留执行前授权快照，新run授权0，不自动补跑。

## 已保存的事实

- 原full从clean HEAD `14efadcfc108996a70323f81e398c29b8f5a99d0`独立运行；共42次尝试，41次HTTP200/有效解析，第42次timeout，余下30次未调用，retry0，首错停止生效。
- 失败为`cal-ff-rt-02 / repeat3`，elapsed60265ms、未记录HTTP状态。同case的repeat1/2分别HTTP200、7375/8390ms。根因仍未知，不能仅据此归因网络、provider排队或数据。
- compiler=`INCOMPLETE`、calibration=`PARTIAL`，coverage=41/72；agreement/repeat率均N/A。errorObservationCount31含1次实际timeout和30个missing，不是31次provider失败。
- 有若干已完成三轮的case出现预测变化；因整轮不完整，不产生总体一致性或agreement结论。延长timeout也不会被描述为解决judge质量问题。
- 原始证据完整保留ignored，独立重编译与原摘要一致；见[full安全review](../../../docs/eval/reports/c19-full-review-v1.json)。timeout的上游完成/费用未知，不保证provider已取消或零费用。

## 可审阅的恢复选择

| 选择 | 需要的新授权 | 身份与代价 |
| --- | --- | --- |
| 保留60秒，另跑完整full | 新增最多72次，24×3，约58KB合成请求体 | 仍用原manifest；不拼接旧41条，也不能保证不会再次超时。 |
| 独立120秒候选，建议先canary | 先新增最多4次canary，约3.3KB；成功后再独立提请72次full | 仅扩大等待窗口，模型/请求体/prompt/parser/阈值/源码均不变；执行身份改变，最长等待增加，不能保证修复。 |
| 保留partial，暂停真实调用 | 0次 | C19维持未完成，不接受baseline或归档。 |

剩余30个未使用名额不能在“首错停止、不可续跑/拼接”的旧run中补满，更不能当作新runtime的授权。任何新run都须明确授权，且使用全新ignored目录、零HTTP自动重试、首错停止。

## 已选择的120秒独立配置

- 原默认[60秒manifest](../../../docs/eval/config/c19-judge-calibration-v1.json)保留，canonical hash `486a649590bd06d617349436a8e94635fd536048ce0c855848a7a4ab1a69190a`。
- 新增独立[120秒候选](../../../docs/eval/config/c19-judge-calibration-timeout120-v1.json)，沿用v1格式/schema而非更改评分契约，canonical hash `2c1ac168c87df1e85e42508b7608f2d6d2772fac76a7554ebfefe769b4cae3ea`。逐对象比对只有`judge.timeoutSeconds:60→120`，原件未改。
- 候选调用必须显式指定该manifest及`--judge-timeout 120`；默认执行入口仍为60秒。纯本地validator/plan-only通过，canary预算4/实际0；121秒被现有validator拒绝，无新增依赖或执行源码修改。
- 本次只执行NVIDIA NIM / `nvidia/nemotron-3-super-120b-a12b`的固定4条canary，向integrate.api.nvidia.com发送合计3308bytes合成请求体；timeout120s、temperature0、max_tokens1024、context6000、间隔至少2s、retry0、首错停止。数据仅tracked合成问题/答案/fixture及system prompt，金标/ID/用户数据不出站；凭据只读进入进程，源文件/日志不写值。
- 费用依据仍为用户确认免费额度，具体账单/配额未独立核验；新请求继续消耗额度。先4条不含后续72次授权，不变更生产默认配置、metric或C20门禁。

design D8已由本次选择确认，仅授权该独立canary4次；执行前本地提交授权文档，再在clean实际HEAD下核查preflight并记录新run身份。此授权不含后续full72、额外查询、重试或补跑，不保证已修复根因。原full review及原阶段plan均保留。

## 独立canary实测与新full披露

- clean HEAD193e46cd82d9f46f70517d76de7b9ca13687dca3，run70f38f0e-74c4-43bb-bdda-33c230585564；4个HTTP200且解析有效，elapsed8155/5657/4202/5782ms，error/missing/unknown/retry均0，compiler COMPLETE。三组agreement1.0、mismatch0，仅4条单轮canary；不证明24个case三轮一致性或原60秒full超时修复。
- raw独立ignored保存，四份artifact hash绑定安全review，独立本地重编译与原safe-summary一致，额外调用0。凭据源未变；累计尝试50（旧canary4+旧full42+新canary4），HTTP200共49、旧timeout1，实际费用未知。
- 下一步只提请**新增最多72次full**，24个case各3次，同120秒manifest2c1ac168...、同模型/源码/prompt/parser/0.70阈值；发送固定合成请求体合计58191bytes约58KB到integrate.api.nvidia.com。timeout120s/max_tokens1024/context6000/temperature0/间隔至少2s/retry0/首错停止，无redirect/fallback/resume，仍可能超时/限流/解析失败。
- 沿用用户确认免费额度，继续消耗额度；具体账单/限流未独立核验。新完整run只写唯一ignored目录，不含backend/embedding/rerank/普通ask或业务写入。凭据本地只读进入进程、不写值；不拼接8条canary或旧41条成功记录，先前剩余30不充当本轮授权。本次恢复指令覆盖该已披露新full72，当前授权72/实际0，若全执行累计尝试上限122；不含额外查询、重试/补跑。
- 新full提交授权文档后使用其实际clean HEAD；完整72条才可由compiler出正式全轮指标，低agreement如实保留，不调阈值。仍需用户验收后方可归档，C20不启动。

## 恢复后的full实测与当前边界

- clean HEAD91dc5675ecfef3756de9b45e4ffcb103c46dea43、run e3db9573-ed8d-4860-a4fd-7d3e39fad2af、manifest2c1ac168...；9 starts/9 outcomes，8有效HTTP200，1个judge_network_error，63未调用，retry0。runner exit4/compiler INCOMPLETE，coverage8/72，正式agreement/repeat率N/A；errorObservationCount64是1网络错误+63missing，不是64次provider失败。
- 失败是cal-ft-rt-03/repeat3/attempt9，elapsed5032ms、无HTTP状态；同case前2次HTTP200、5266/17092ms。本地只读源码核查表明该类别对应URLError且其reason不是TimeoutError，但未保存底层reason；无法区分DNS/TLS/代理/连接或上游问题，不能认定429、认证失败或120秒timeout。远端是否接收/完成/扣费未知，5秒失败不证明延长等待有用。
- raw独立保留ignored，identity/ledger/completion/safe-summary四hash绑定安全review，独立重编译一致、额外provider请求0。本地凭据源前后内存比对未变；累计59次尝试/57个HTTP200/1旧timeout/1新网络错误，费用未独立核账。
- 原剩余30及本轮剩余63均不自动补跑或拼接，任何新run/诊断性网络请求须新的明确授权。可保留现状（0调用），或在用户另行授权后按同120秒manifest独立新full最多72次/约58KB；同模型、max_tokens1024、间隔至少2s、retry0/首错停止、唯一新目录，仍可能失败并继续消耗用户确认的免费额度。新full不含额外查询、backend/embedding或业务写入；当前新run授权0，不预填成功或承诺修复。
