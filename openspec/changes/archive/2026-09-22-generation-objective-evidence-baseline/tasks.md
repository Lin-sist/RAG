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
- [x] 已披露并获授权；最终r11 canary固定5条、零错误零重试、compiler v6 COMPLETE。
- [x] r3固定5条一次no-overwrite执行，judge/model rerank=0；CLEAN，旧失败保留，full按用户独立授权执行。
- [x] r3 canary compiler COMPLETE、clean HEAD、预算合规；不据小样本宣称质量达标。

## 4. W3 Full（待单独授权）
- [x] 已披露并获授权；r11从clean HEAD独立启动full。
- [x] 一次完整150 run；raw全部保留，无旧run拼接、无KB重建或历史清理；31次503按批准的C18请求边界恢复。
- [x] compiler v6 COMPLETE、CLEAN/objective COMPLETE/judge SKIPPED；IDs/order/hash/errors/budget/provider归因完整。
- [x] 分通道数值、分母和调用计数已记录于正式安全摘要及acceptance；明确词法指标与单次run边界。

## 5. W4 收尾
- [x] 用户已授权验收与归档；真实baseline及边界记录于acceptance，不激活objective/judge profile、不改C17阈值。
- [x] approved delta接受进baseline，更新说明/日志并验证exact suffix/链接/隐私。
- [x] 用户已授权归档；移动change并恢复IDLE，按Agent本地提交处理，push/PR/deploy仍未授权。

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
- [x] runtime fingerprint、5条canary、150条full、真实baseline验收和change归档均在后续授权下完成。

## 2026-09-18 整阶段执行与自动审批阻断
- [x] 收到用户整阶段及 Agent commit 授权，完成具体预算/出站/REST副作用披露。
- [x] 修复 direct runner 在 C18 样本错误后继续调用的缺口；266 Python tests PASS。
- [x] Docker 五项 healthy、后端启动、runtime allowlist 核验、只读 preflight READY。
- [x] 历史审批阻断后已取得具体NVIDIA出站与history写入授权，并由r11完成canary/full。

## 2026-09-18 具体出站授权后的 canary 结果
- [x] 用户明确NVIDIA endpoint载荷、既定canary/full预算和REST写入授权；审批已通过。
- [x] clean HEAD 65c2d3c，preflight READY，固定canary实际发出debug/ask各1，生成HTTP410后停止、retry=0，原始证据保留。
- [x] 旧Qwen 410由冻结Nemotron3 Super新身份解决；旧失败不拼接，r11独立完成。
- [x] 修复真实runner暴露的compiler元数据缺口（268 tests PASS）：Windows路径、judgeConfig.mode、maxOutputTokens误脱敏、失败generation计数unknown语义；不得改raw凑COMPLETE。
- [x] provider与工具兼容性问题修复后，r11 full/验收/归档完成。

## r2 重冻结与执行
- [x] 官方确认旧Qwen免费端点Deprecated；新模型Nemotron3 Super合成HTTP200，manifest r2冻结，269 Python tests通过。
- [x] 进程CLI覆盖而不改.env.local；preflight READY，新canary只发首题后因HTTP503停止。
- [x] r2保持历史失败；r11以独立clean identity完成canary/full。

## 2026-09-19 r3 当前验收事实
- [x] 隔离clean checkout与runtime/preflight；新canary5/5、error/retry0、compiler COMPLETE。
- [x] 用户已授权full150，按新身份发起；实际到第13条HTTP503即停，raw全部保留，compiler INCOMPLETE。
- [x] r3保持13/150历史失败；r11从头完成150/150，不拼接。
- [x] r11完整full后完成W4接受与归档。

## 2026-09-19 最新批准的重试切片
- r4 full2/150因503中断；用户已批准仅429/503最多3次重试，新身份r3-transient，预算canary136/full5968 embedding、20/600 generation。
- 重试实现和277项Python测试已通过；接续新clean canary/full，旧失败证据保留。

## 2026-09-19 r5 有界重试执行
- [x] 用户批准仅429/503最多3次重试，契约/manifest/schema/runner/compiler/测试同步；277 tests PASS，提交d3a4f43。
- [x] 新clean canary5/5 COMPLETE，3次503恢复；真实当前preflight READY。
- [x] 按用户full授权完整启动，115条观测、38次503恢复全部留存；第115条PrematureCloseException/network按不重试契约停止，compiler INCOMPLETE。
- [x] r5保持115/150历史失败；批准精确断连恢复后，r11从头完成150条。
- [x] r11正式full完整，W4 baseline接受/归档/IDLE完成。

## 2026-09-20 r6 精确提前断连恢复
- [x] 用户要求继续完成C18剩余内容；只将真实观测的 `network/PrematureCloseException` 纳入既有最多3次重试，不扩大timeout或其他network错误，预算不变。
- [x] compiler v3、新manifest/schema、runner ledger及负例测试完成；全套Python 280项通过，diff check通过。
- [x] r6保持403失败；凭据恢复后r11使用新身份完成clean canary与从头full，旧证据不拼接。
- [x] compiler v6 COMPLETE，通道数值与边界已呈交并完成验收、delta接受、归档/IDLE。

## 2026-09-20 代码质量复审加固（compiler v4）
- [x] 缺失/漂移generation model在请求边界立即停止，compiler不再用expected model补写observed identity。
- [x] compiler从逐样本明细重算安全聚合指标；畸形嵌套raw返回`INVALID`而非崩溃。
- [x] report/details/metadata路径必须互异；runner、metadata writer与compiler的no-overwrite采用独占创建。
- [x] compiler输入/输出路径锚定repo root并拒绝绝对路径、盘符和`..`逃逸。
- [x] Java embedding层直接记录逻辑调用、缓存命中、provider调用与fallback，并贯通debug/ask diagnostics；缺失观测或存在无法归因的HTTP重试时compiler v4降为`INCOMPLETE`。
- [x] 删除重复C18常量、重复metadata键和重复guide段落；保留分层契约验证，不做无关frontend或跨模块重构。
- [x] 历史403阻断已解除；r11完成新身份canary/full、baseline验收与归档。

## 2026-09-21 compiler v5 逐attempt embedding观测
- [x] NVIDIA鉴权恢复；models/embedding为200，chat由503恢复到200，mutation-free preflight仍为READY。
- [x] r7因启动脚本重新导入`.env.local`覆盖进程前置模型而实际调用旧Qwen并410，首条停止；raw保留，不续接。
- [x] r8正确使用Nemotron3 Super，5/5最终CLEAN但恢复6次503；compiler v4按设计判`INCOMPLETE`，full未启动。
- [x] 生成失败响应合并本次retrieval diagnostics；runner逐attempt记录embedding逻辑/cache/provider/fallback，compiler v5逐attempt校验并从ledger重算聚合。
- [x] W0样本与预算34/1492保持不变；Java聚焦测试通过，Python聚焦37项通过。
- [x] compiler v5已提交为`4b143b2`并形成clean HEAD；r9最终5/5且恢复1次503，但后端从本机仓库加载旧版`rag-core` JAR，失败attempt仍无embedding事实，compiler按契约为`INCOMPLETE`。
- [x] r9保持历史INCOMPLETE；安装当前模块后由r10/r11验证，最终r11完成W4。

## 2026-09-21 r10暂停状态
- [x] install当前内部模块；r10 clean canary 5/5，5次503均恢复，compiler v5 `COMPLETE`且逐attempt embedding事实完整。
- [x] r10 full从头启动并在38/150的`fact-018`耗尽4次503后停止；剩余112条未调用，旧run不拼接。
- [x] 修复compiler对失败ask无raw响应时`observed_model`未初始化的异常，新增回归测试；冻结为`nemotron3-super-r7-failure-safe-compiler`/compiler v6，失败证据返回`INCOMPLETE`而非异常。
- [x] Python全套290项通过；canary/full plan均`OFFLINE_VERIFIED`，预算136/5968及题目顺序不变；manifest/source hash自验证、diff检查通过。待本地提交形成clean HEAD。
- [x] r11 clean canary摘要先提交，随后从clean工作树完成full150与W4。

## 2026-09-22 r11 compiler v6执行
- [x] compiler v6提交并补录，install当前内部模块；Docker healthy、preflight READY。
- [x] clean canary固定5条全部完成，error/retry=0，compiler v6 `COMPLETE`；embedding事实`22/11/11/0`，安全摘要待本提交纳入Git。
- [x] r11 canary摘要提交形成clean HEAD；full preflight READY并从头完成150条，未复用r10的38条。
