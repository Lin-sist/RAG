# C19：Judge 真实校准证据

## 问题与目标

C9b 已接受 `rag-judge-v1`、24 条 `judge-calibration-v1` 和离线 runner，真实 judge 仍为 SKIPPED。C19 为固定四象限 corpus 建立独立 judge agreement / repeat consistency 证据，不从前端或 C18 objective 结果推断 judge 已校准。

2026-10-18 用户批准具体零外调设计，随后授权Agent本地提交并继续；前端按[收口索引](../../../docs/roadmap/frontend-demo-closeout-2026-10-18.md)接受，C19离线实现提交e107b2c。用户确认免费额度并授权canary，实际4/4成功、0错误/重试，见[安全摘要](../../../docs/eval/reports/c19-canary-review-v1.json)。用户随后独立授权新增最多72次full，当前等待正式结果及验收；change保持ACTIVE，本地提交授权继续，不含push/PR/部署。

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
- provider采用NVIDIA NIM OpenAI-compatible，judge独立固定为`nvidia/nemotron-3-super-120b-a12b`；canary验证4条HTTP200，凭据仅从本地只读加载到进程，不落tracked文件/日志。未查模型列表，full已独立授权72但尚未执行；canary不证明完整校准，见[canary](canary-execution.md)和[full执行包](full-execution.md)。
- 出站仅 tracked 合成 question/answer、fixture excerpt 和 judge prompt，不包含 gold labels、用户资料或 KB 内容。费用/零费用依据与账号配额待执行前确认；不得用旧账号声明当作本轮事实。串行、retry=0、拟 timeout=60s、间隔至少2s、输出上限1024 token、context 上限6000字符，均在 manifest 中冻结并呈交 live 授权。
- 离线验收：聚焦回归及 `python -B -m unittest discover -s scripts -p 'test_*.py'`；plan/preflight/compiler 为纯本地；负例证明请求前拒绝及脱敏。
- 最终接受：一份完整正式 full 的全部72 observation、parse coverage、三组 confusion/agreement、provider-pass mismatch、24个case repeat consistency、错误/重试/身份均可审计。COMPLETE 只证明测量完整，不表示 agreement 达标；接受/归档由用户确认。

## 事实源

[路线图 C19](../../../docs/roadmap/iteration-blueprint.md#c19-judge-live-calibration-evidence) · [accepted evaluation](../../specs/evaluation/spec.md) · [design](design.md) · [tasks](tasks.md) · [readiness](readiness.md)
