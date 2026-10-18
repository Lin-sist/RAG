# C19 full超时后的恢复审阅包

2026-10-18。Report status：`RECOVERY_CANARY_AUTHORIZED_PENDING_RUN`。用户明确选择“授权120秒候选最多4次canary（建议）”，本轮新增授权4/实际0；独立新目录，不续跑旧full。成功后仍须单独授权新full72。

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
