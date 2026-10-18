# C19 设计

## 当前事实与缺口

`scripts/run_judge_calibration.py` 已校验 corpus，并支持 4×1 / 24×3，调用共享 strict parser、无自动重试。本轮静态检查发现：身份只有 endpoint origin（缺 /v1 路径且 netloc 未排除 userinfo）；缺 clean HEAD/源码冻结；只在全部运行结束后写文件；无独立硬预算与真实 attempt ledger；聚合器即使 PARTIAL 仍返回成功子集 agreement。C19 不能直接用该结果宣称正式 evidence。

现有 raw details 含 reason/rawContent，只能留在本地 ignored 区域。normal evaluator 不因 C19 扩展改变口径；仅修正 calibration 缺测时抑制结论的行为，保留已接受状态名称及完整输入时的原公式。

## 数据与执行链

1. 本地 validate → plan → preflight：验证 manifest、数据输入原始bytes/hash、ordered case/repeat、Git LF blob源码hash（工作树LF规范化后须与blob一致）、clean HEAD、路径/no-overwrite、数值/预算/协议身份。纯本地，不探测 provider；credentials 仅在 live 阶段读取，绝不进入 artifact。
2. 阶段授权：离线批准与 live 授权分开；canary/full 各用唯一 runId 和新目录。provider/model 未冻结时 preflight=BLOCKED，不以 UNCONFIGURED 的 plan 称 live 就绪。
3. 串行执行：每次 POST 前预约一个预算并写 attempt-start，收到结果后立即写 outcome（flush）；保留 HTTP 状态、稳定 error taxonomy、耗时、attempt=1、retry=0。禁止静默重试、跨进程续跑、跟随跨 endpoint redirect。故障即停止，余项为 missing/not_attempted，不伪造 attempt。
4. 输出保全：run directory 以 exclusive-create 占用；ledger append-only；raw/details 不覆盖。异常/中断后保留已有 evidence；有 start 无 outcome 记 outcome_unknown，禁止自动重放或以0调用结算。
5. 本地 compiler：重新校验 raw hash、请求顺序、预算、每条 parsed score、derived pass、error/attempt、corpus/judge/runtime/source 身份。不得信任 details 自报 summary。金标从 tracked corpus 读取，不进 judge prompt。
6. 安全摘要：只写白名单版本/hash/ID/计数/状态/混淆矩阵/比率/错误 taxonomy，不含原始正文、reason、provider body、密钥、userinfo、query/fragment 或绝对路径。endpoint 身份只允许 canonical HTTPS host/port/path，拒绝凭据与query/fragment，不输出被拒 URL；compiler 不做网络调用。

实现补充：C19使用显式`--c19-manifest`入口，manifest为执行参数事实源；冲突的CLI/env配置会在请求前拒绝。`identity.json`先保存，`ledger.jsonl`逐事件flush/fsync，结束另写`completion.json`绑定前两文件hash；未知outcome不推断零费用。摘要区分reservation上限`providerAttemptsUpperBound`与已观察HTTP响应数，不把所有reservation声称为已送达的真实调用。源码未提交/模型未选定时本地preflight为BLOCKED；注入transport仅生成SYNTHETIC，`liveEvidenceEligible=false`。

## 状态与可比性

- runner 沿用 `COMPLETE / PARTIAL / NOT_COMPARABLE`；调用、parse或缺测为 PARTIAL；重复/意外 observation 为 NOT_COMPARABLE。
- compiler `COMPLETE / INCOMPLETE / NOT_COMPARABLE / INVALID` 分别退出 `0/4/3/2`，与 runner 历史退出码分开。
- COMPLETE 要求全部预期 observation/attempt、固定身份、无执行错误/缺口且 parse有效；低 agreement 仍可 COMPLETE。
- 非 COMPLETE 保留 coverage、error/缺测/重复计数。agreement 与 repeat consistency rate 为 N/A，不输出成功子集作为校准结论；诊断 confusion 若保留必须显式标记 partial 且 comparisonEligible=false。
- repeat consistency 是同 case 三次布尔预测一致性，不能表述为分数完全相等；canary只有一次，不作为 full repeat evidence。
- C19 自有 execution identity 补充 endpoint path/timeout/output cap；共享 `rag_judge_contract.py` 不改，避免静默改变 normal eval 的 comparison identity。

## 修改边界

允许：`scripts/run_judge_calibration.py`、`scripts/test_judge_calibration.py`；新增 `scripts/c19_judge_contract.py`、`scripts/compile_judge_calibration_evidence.py` 及对应 `test_*.py`；新增 `docs/eval/config/c19-judge-calibration-v1.json` 与对应schema。2026-10-18 full首错停止后，另允许准备显式候选`c19-judge-calibration-timeout120-v1.json`供审阅，原默认v1不改，新runtime调用须独立授权；C19 change、ACTIVE_TASK/只追加AGENT_LOG、指南/路线图/技术债和安全报告仍在范围内。

禁止：改 calibration corpus/schema/manifest 原件、共享 judge prompt/parser/threshold、normal runner、Java/前端、依赖/锁文件、C17/C18 artifacts、`.env.local`、`application-dev.yml`、`.agents/`、学习目录。若实现需要超出允许路径，先更新事前范围。

## 决策记录

### D1 执行入口
- **面临的选择**：直接运行 C9b runner；扩展既有 runner 并增加独立 C19 contract/compiler；复制一套完整 runner。
- **选了哪个 + 为什么**：扩展既有 runner，加独立 contract/compiler，复用同一 prompt/parser 且补齐执行证据边界。
- **放弃的代价**：直接运行会缺少身份/逐次保全；完整复制会使校准口径与共享实现逐渐漂移。

### D2 故障处理
- **面临的选择**：失败后自动重试；继续跑完所有余项；首个失败停止并保留缺测。
- **选了哪个 + 为什么**：首个失败停止、retry=0，以最小费用发现格式/限流问题，full只接受完整单次运行。
- **放弃的代价**：自动重试扩大预算且改变重复实验口径；继续余项可能在已知错误配置上耗尽72次预算。

### D3 模型与身份
- **面临的选择**：复用前端 generation 模型；独立选择 judge 模型并冻结；让脚本自动从模型列表挑选。
- **选了哪个 + 为什么**：独立选择并冻结，具体模型待live前确认；前端成功不证明judge格式或agreement可用。
- **放弃的代价**：直接继承会把业务配置变动混入校准；自动挑选产生隐式出站且实验身份不可控。

### D4 缺测结论
- **面临的选择**：返回成功子集 agreement；缺测计为错误预测；非完整输入抑制agreement/repeat率。
- **选了哪个 + 为什么**：非完整输入抑制结论，coverage/error单列，遵守accepted缺测不可比较契约。
- **放弃的代价**：成功子集会产生幸存者偏差；把调用失败计为预测错误会混淆provider可靠性与模型判断质量。

### D5 生产与门禁边界
- **面临的选择**：同时调阈值并启用judge门禁；只生成C19独立证据后移交C20。
- **选了哪个 + 为什么**：仅C19证据，测量完整性与质量批准分开；C20另立profile/version。
- **放弃的代价**：同步调阈值会用同一gold corpus拟合并放行，破坏已接受基线的可比较性。

### D6 本地文件保全
- **面临的选择**：运行末尾一次性覆盖输出；逐次持久化且独占新run目录；断点续跑合并成功记录。
- **选了哪个 + 为什么**：独占新目录、逐次保存，不自动续跑，避免中断丢失失败和重复计费。
- **放弃的代价**：末尾保存可能丢整轮证据；续跑合并会引入重复调用和不同运行状态的混合样本。

### D7 源码换行身份
- **面临的选择**：直接hash工作区源码bytes；冻结Git LF blob并核对工作树规范化一致；放宽或关闭源码hash验证。
- **选了哪个 + 为什么**：冻结Git LF blob且核对工作树一致，数据仍核对原始bytes，避免Windows换行造成假漂移，同时拒绝真实源码变动。
- **放弃的代价**：直接工作区hash跨平台不可比较；关闭验证会让实际执行逻辑变化逃过身份检查。

### D8 full超时后的恢复（待用户确认）
- **面临的选择**：保留60秒并独立重跑完整72条；保留旧证据，另建120秒配置先跑4条canary再独立申请full；保留partial并停止真实调用。
- **选了哪个 + 为什么**：待用户确认；建议独立120秒候选先canary，只扩大等待窗口、不改评分契约，且不覆盖旧manifest。已观察的失败是60秒超时，但上游根因未知，延长等待不保证修复。
- **放弃的代价**：60秒新full可能再次遇到同一等待上限；120秒会改变执行身份、增加最长等待并需新阶段授权；暂停则无法获得完整72条证据。所有分支都不能拼接旧partial或自动HTTP重试。
