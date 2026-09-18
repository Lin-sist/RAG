# Tasks: C18 Generation Objective Evidence Baseline

## 0. 当前规划
- [x] C17阈值获批、三轮PASS、baseline接受与文件归档完成；未将被拒的commit写成成功。
- [x] 按原用户请求开始下一阶段规划，审计runner、RAGService、QueryEngine、generator及REST持久化。
- [x] 创建proposal/design（8条决策记录）/tasks/evaluation delta；C18唯一active change。
- [x] 当前仅规划，provider/backend calls=0、业务数据出站=false；C18规划提交责任于2026-09-11由用户授权更新为Agent本地提交。
- [x] 用户于2026-09-11明确要求“开始C18实现”，进入已规划W0/W1离线实施；真实canary/full仍须独立授权。

## 1. W0 调用与身份审计（零外调）
- [x] 从实际Java纯逻辑枚举canary/full初始和解释回退variants，输出ID-only预算；绑定代码/数据/配置hash，冻结精确E，重验11/451初始项。
- [x] 核对debug/ask检索、generation最坏调用数、runner/provider retries=0、答案缓存关闭、embedding cache计数、Router关闭及heuristic归因。
- [x] 核对prompt/model/endpoint/temperature/token budget/timeout安全fingerprint来源，不读取或输出secret。
- [x] 明确query count/history副作用和共享限流节奏，不修改既有业务语义。

## 2. W1 离线工具与验证
- [x] RED→GREEN：C18 manifest/schema和模式互斥；ID/order/repeat、预算或source hash漂移在login前拒绝。
- [x] RED→GREEN：query/generation HTTP调用前护栏，缓存/算法回退/provider fallback/自动retry分别统计；超限不发请求。
- [x] RED→GREEN：exact150 compiler、details/metadata/hash/channel status；missing/error/identity负例。
- [x] RED→GREEN：复用C9公式，不以debug contexts冒充ask provenance，不以成功子集补分母。
- [x] safe summary/schema、no-overwrite与敏感字段负例；C17 profile/reference字节保持不变。
- [x] Python全套；本轮无新增Java生产实现，Java聚焦测试不重复；真实结果和跳过项写AGENT_LOG。
- [x] canary/full plan-only：精确预算、模型、出站和副作用，前置缺失时fail closed。

## 3. W2 Canary（待单独授权）
- [x] 提交形成 clean HEAD：2026-09-18 初始 clean，护栏修复提交 a8c95cf；无用户未提交修改。
- [x] 持久化 embedding identity 后，HTTP preflight 只读验证 3 fixtures/50 chunks、c17g3/model identity；连续两次 `READY`，vector `50/50`，fixture `3/3`。
- [ ] 披露5 debug/5 ask/≤5 generation、精确E(canary)、fixed ID出站、timeout/retry/限流及query count/history写入，取得授权。
- [ ] 固定5条一次no-overwrite执行，judge/model rerank=0；失败保留并停止，无自动重跑或full。
- [ ] 验证canary预算/identity完整，不据小样本宣称质量达标。

## 4. W3 Full（待单独授权）
- [ ] canary clean后披露150 debug/150 ask/≤150 generation、精确E(full)及数据/副作用范围，取得独立授权。
- [ ] 一次完整150 run；raw全部保留，无自动retry、无拼接、无KB重建或历史清理。
- [ ] compiler COMPLETE、CLEAN/objective COMPLETE/judge SKIPPED；IDs/order/hash/errors/budget/provider归因完整。
- [ ] 分通道数值、分母和no-context bypass/调用计数；明确词法指标与单次run边界。

## 5. W4 收尾
- [ ] 用户验收真实baseline及边界；不激活objective/judge profile、不改C17阈值。
- [ ] approved delta接受进baseline，更新说明/债务/日志，验证exact suffix/链接/隐私。
- [ ] 用户授权归档后移动change并恢复IDLE；按明确提交责任处理commit，push/PR/deploy仍需单独授权。

## 2026-09-11 本地提交交接
- [x] 用户明确授权Agent本地提交并保持Git干净；C17收尾已提交，C18规划独立提交。
- [x] 本轮不开始C18实现或真实调用，保留上述尚未实施的任务与证据闸门。

## 2026-09-11 W0预算冻结与plan-only切片
- [x] 真实RAGServiceImpl.ask全空分支及每个命中提前退出点均经测试；复用实际QueryEngineImpl.explainQueryVariants，未在Python复制检索算法。
- [x] canary初始11、解释回退12，总query embedding上限34；full初始451、解释回退590，总上限1492。每条ID的各pass计数及源码hash已保存。
- [x] c18_budget_contract.py纯离线计划入口：源码/数据/完整ID/order/整数/合计校验、no-overwrite；PLAN_VALID不意味着executionReady或live授权。
- [x] 33项Java聚焦测试/0失败/0错误/0跳过；252项Python tests/OK；canary/full离线计划均通过。
- [x] W1在后续离线切片完成：manifest/schema、runner集成、请求前护栏、compiler和负例测试已通过；runtime fingerprint、真实canary/full及验收仍未完成。

## 2026-09-12 W1离线工具与证据编译
- [x] 新增C18独立manifest/schema与`c18_generation_contract.py`，绑定v2/150、W0审计hash、canary/full精确预算、runtime descriptor、zero retry、judge/router/cache开关、2.2秒节奏和safe allowlist。
- [x] 两个runner完成C18模式互斥、ID/order/repeat、raw no-overwrite、源码hash和runtime fingerprint前置校验；child请求前护栏超限不发送请求，父runner保留非零退出码。
- [x] 新增`compile_generation_objective_baseline.py`，验证exact selection、details/metadata/hash/channel status、ask返回citation provenance、no-answer bypass、调用事实和隐私边界；tracked摘要不包含raw文本、provider payload、凭据、数字KB ID、collection或绝对路径。
- [x] RED→GREEN：13项C18 W1聚焦测试通过；全套Python `unittest discover` 共265项通过；canary/full plan-only通过。
- [x] 本轮仅修改C18离线脚本/配置/schema、W0审计源码hash、OpenSpec/eval guide/日志；无Java生产实现、无frontend变更、无真实provider/backend/KB/SQL/Milvus调用。
- [ ] runtime fingerprint验证、5条canary、150条full、真实baseline验收和change归档仍按独立授权推进。

## 2026-09-18 整阶段执行与自动审批阻断
- [x] 收到用户整阶段及 Agent commit 授权，完成具体预算/出站/REST副作用披露。
- [x] 修复 direct runner 在 C18 样本错误后继续调用的缺口；266 Python tests PASS。
- [x] Docker 五项 healthy、后端启动、runtime allowlist 核验、只读 preflight READY。
- [ ] 真实 canary 命令被自动审批拒绝，尚未发出；需用户明确具体 NVIDIA 出站载荷及本地 history 写入，full/真实验收/归档因此仍未完成。

## 2026-09-18 具体出站授权后的 canary 结果
- [x] 用户明确NVIDIA endpoint载荷、既定canary/full预算和REST写入授权；审批已通过。
- [x] clean HEAD 65c2d3c，preflight READY，固定canary实际发出debug/ask各1，生成HTTP410后停止、retry=0，原始证据保留。
- [ ] 解决冻结模型HTTP410，并重新执行新身份canary；本次失败不拼接、不自动重跑，full未启动。
- [x] 修复真实runner暴露的compiler元数据缺口（268 tests PASS）：Windows路径、judgeConfig.mode、maxOutputTokens误脱敏、失败generation计数unknown语义；不得改raw凑COMPLETE。
- [ ] full/真实baseline验收/归档仍未完成，原因已从审批阻断变为provider失败及上述工具兼容性问题。

## r2 重冻结与执行
- [x] 官方确认旧Qwen免费端点Deprecated；新模型Nemotron3 Super合成HTTP200，manifest r2冻结，269 Python tests通过。
- [x] 进程CLI覆盖而不改.env.local；preflight READY，新canary只发首题后因HTTP503停止。
- [ ] provider持续可用性与新clean canary/full；r2另受并发工作区改动影响，metadata clean=false，不能接受为基线。
