# C19：Judge 真实校准证据

## 2026-10-21 用户批准的归档范围

用户明确验收已实现的零外调离线工具链与执行契约，并授权归档；完整Live样本/正式agreement与repeat一致性移交`technical-debt.md`的P1证据债务W1-E1。本次接受manifest/身份冻结、本地plan/preflight、请求前预算/逐次ledger、失败保全及独立安全compiler；不接受partial为完整Live校准，不新增外调或恢复旧预算。验收见[acceptance](acceptance.md)。

本节取代下文关于“完整72条才可归档”的原工程收口条件；完整72条仍是未来正式Live证据的必要条件。以下2026-10-18记录保留历史执行语境。

## 问题与目标

C9b 已接受 `rag-judge-v1`、24 条 `judge-calibration-v1` 和离线 runner，真实 judge 仍为 SKIPPED。C19 为固定四象限 corpus 建立独立 judge agreement / repeat consistency 证据，不从前端或 C18 objective 结果推断 judge 已校准。

2026-10-18 用户批准设计并授权本地提交；原60秒full41有效/1timeout，独立120秒canary4/4。用户恢复指令后独立120秒full尝试9次首错停止：8有效/1网络错误/63未调用，见[新full review](../../../../docs/eval/reports/c19-full-timeout120-review-v1.json)。compiler INCOMPLETE，正式率N/A，不续跑或拼接；新run授权0。change保持ACTIVE，不归档或推进C20；本地提交授权继续，不含push/PR/部署。

## 范围

1. 冻结独立 C19 manifest：corpus/schema/fixture、共享 prompt/parser/threshold/context、provider/model/完整协议路径、temperature、timeout、输出上限、retry=0、case/repeat 顺序、源码 hash 与 clean Git HEAD。
2. 加固已有 runner：请求前硬预算、逐次落盘、不覆盖、失败即停止且保留缺测、无自动 retry/resume；金标不进入 provider payload。
3. 新增本地 compiler：从完整 observation 与 attempt ledger 重算 coverage/confusion/agreement/mismatch/repeat consistency，缺失/重复/意外/畸形或身份漂移时抑制 agreement 结论，不能用成功子集补齐。
4. 建立安全审阅摘要及独立 canary → full 执行闸门，记录真实 provider 错误、HTTP、耗时、retry 和调用事实。

## 不做

不修改 corpus/gold/fixture、共享 rubric/prompt/parser/0.70 阈值、normal eval 公式、C17/C18 evidence、生产模型/检索/分块/SSE；不自动搜索阈值、不创建或激活 C20 profile、不做后端/KB/前端业务调用。

## 预算与验收

- 离线实现：真实 HTTP/provider calls=0，数据出站=false。
- canary：固定 4 case × 1 repeat，最多 4 次 judge POST；完整 schema/ledger/identity 无误后才可提请 full 授权。agreement 低值保留，不作为自动调参或补跑理由。
- full：24 case × 3 repeats，最多 72 次 judge POST，独立授权；不得把 canary 拼成 full。
- provider采用NVIDIA NIM，judge固定为`nvidia/nemotron-3-super-120b-a12b`；累计59次尝试/57个HTTP200/1旧timeout/1新网络错误。凭据仅本地只读加载到进程，不落tracked文件/日志；两轮full均不完整，不从成功子集给agreement。60秒默认保留，120秒新full按首错停止，新run授权0，见[recovery-plan](recovery-plan.md)。
- 出站仅 tracked 合成 question/answer、fixture excerpt 和 judge prompt，不包含 gold labels、用户资料或 KB 内容。费用/零费用依据与账号配额待执行前确认；不得用旧账号声明当作本轮事实。串行、retry=0、拟 timeout=60s、间隔至少2s、输出上限1024 token、context 上限6000字符，均在 manifest 中冻结并呈交 live 授权。
- 离线验收：聚焦回归及 `python -B -m unittest discover -s scripts -p 'test_*.py'`；plan/preflight/compiler 为纯本地；负例证明请求前拒绝及脱敏。
- 最终接受：一份完整正式 full 的全部72 observation、parse coverage、三组 confusion/agreement、provider-pass mismatch、24个case repeat consistency、错误/重试/身份均可审计。COMPLETE 只证明测量完整，不表示 agreement 达标；接受/归档由用户确认。

## 事实源

[路线图 C19](../../../../docs/roadmap/iteration-blueprint.md#c19-judge-live-calibration-evidence) · [accepted evaluation](../../../specs/evaluation/spec.md) · [design](design.md) · [tasks](tasks.md) · [readiness](readiness.md)
