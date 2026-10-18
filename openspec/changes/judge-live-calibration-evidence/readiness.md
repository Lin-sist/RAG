# C19 事前 readiness（2026-10-18）

Report status：`CANARY_PLAN_READY_AWAITING_AUTHORIZATION`。离线实现已提交`e107b2cf001006a56faad36765753f8cd0845564`；随后按用户提供信息独立固定judge模型，执行时须读取模型配置提交后的clean HEAD。本轮仍无live quality evidence，见[canary执行包](canary-execution.md)。原起点为`e2b077b`，旧占位模型阶段结果按下文日期/阶段保留。

| 项目 | 分类 | 核查结果 |
| --- | --- | --- |
| 前端阶段 | confirmed（已接受范围） | [收口索引](../../../docs/roadmap/frontend-demo-closeout-2026-10-18.md)完成；原始archive/证据不动。 |
| corpus / shared contract | confirmed | 24 cases，四象限各6；manifest SHA-256 `54c891c45ac682e38373bab293893d22695a938b7fc5ac9fa205c7f620dedb55`，prompt `d98612cdc4f6681e126d3e6fd4dac81f5d76d14ec4bd596e904fa4843999df80`。 |
| canary / full 本地plan | confirmed（纯本地） | 4×1=4、24×3=72；validator=VALID；judge已冻结为用户提供的`nvidia/nemotron-3-super-120b-a12b`，不表示认证或live已获授权。 |
| calibration 聚焦测试 | confirmed（synthetic） | `python -B -m unittest discover -s scripts -p 'test_judge_calibration.py'`，9/9通过。 |
| normal eval 回归 | confirmed（synthetic） | `python -B -m unittest discover -s scripts -p 'test_run_rag_eval.py'`，47/47通过。 |
| 全部Python回归 | partial / 非GREEN | 模型配置后最新279 tests，1 failure / 4 errors，与前两阶段相同C18失败，含2组setUpClass错误。完整输出曾截断，紧凑unittest结果保全实跑计数/失败集合。 |
| C19实现 | confirmed（offline） | 运行冻结、预算、逐次ledger、compiler及缺测抑制已实现；最新judge聚焦29/29，含20项C19及9项既有calibration测试。 |
| live judge | unknown / NOT_RUN | 模型身份已固定，账号费用/配额、模型访问权限/认证仍未知；真实预算0，未读真实凭据、未查询模型列表或provider。 |
| C20 | out_of_scope | 不改profile/阈值，不启用judge required gate。 |

## 已复现缺口

实现前：预期2条、只提供1条有效observation，聚合器输出PARTIAL、coverage=0.5但joint agreement仍1.0。实现后该负例agreement和repeat率均N/A；coverage/错误计数保留。源码/clean HEAD、完整endpoint、逐次ledger与硬预算均由C19显式入口实现，原无C19参数入口保留兼容；见[design](design.md)。

## 实现与CLI验证

- 最新模型准备：29/29通过；新增测试证明已选模型与clean源码只能得到本地READY，providerAuthentication=NOT_CHECKED。缺模型负例改用独立UNCONFIGURED测试副本，不依赖正式manifest为空。本次只改模型配置及测试，四个执行源码hash与e107b2c一致。
- 模型配置提交前的本地preflight仅因`clean_frozen_source_required`为BLOCKED，原`model_not_selected`已消除；提交后只读复核实际clean HEAD。准备时仅检查变量名，当前环境未配置judge/NVIDIA凭据变量；未读取凭据值或后端私有配置，live前需本地配置。
- 以下28/28、占位模型BLOCKED和合成CLI为首次离线实现时证据；原[offline report](../../../docs/eval/reports/c19-offline-readiness-v1.json)不改写。新的模型/预算/载荷摘要见[canary plan](../../../docs/eval/reports/c19-canary-plan-v1.json)。

- `python -B -m unittest discover -s scripts -p 'test_*judge*.py'`：28/28通过，纯内存/合成transport。
- C19 canary/full `--plan-only`退出0，计划4/72，实际调用0；`--preflight-only`退出4，reasons为`model_not_selected`、`clean_frozen_source_required`。未选模型和未提交工作区均不冒称live就绪。
- 独立compiler CLI读取一轮4条合成canary：退出0、measurement COMPLETE、evidenceKind=SYNTHETIC、liveEvidenceEligible=false；同名输出重写退出2。raw留在ignored tmp/eval，无真实provider或质量结论。
- 原安全审阅摘要的hash绑定当次SYNTHETIC CLI烟测，不将其当作新模型的真实agreement。

## 全量回归失败的界限

5个失败条目共同触发C18 `budget_source_drift`（含2个setUpClass），在C19实现前已存在；C19只改离线Python，未改Java。8个原始源码hash不符：其中5个仅Windows CRLF与Git LF差异；`QAController.java`、`AnswerGeneratorImpl.java`、`RAGServiceImpl.java` 连当前Git blob也不同于C18冻结hash。不能改写C18 audit/历史报告来让当前测试通过，也不据此撤销旧HEAD的历史验收。

C19独立冻结，不复用旧call-graph audit；源码采用Git LF blob且核对工作树规范化一致，数据仍按原bytes/hash验证。C18回放兼容性维修须另定范围，全仓非GREEN在后续继续披露。

## 工具与跳过项

Preflight：Git/Java/Maven/Python/Node可用，npm缺失、已有直接vue-tsc/Vite入口，无安装/升级。OpenSpec CLI不可用，文件级检查不冒称官方validate。

仅改Python离线评测及文档，跳过Maven、前端build和新浏览器矩阵；前端源码/图片hash一致，复用原证据。真实backend/auth/upload/ask/embedding/rerank/generation/judge/feedback/delete、业务写入及数据出站0；未启动服务、清理业务数据、暂存、commit、push或PR。单元测试自行清理其临时合成目录，CLI烟测raw保留。

受限Python写config失败，改apply_patch生成；首次因此出现16项缺manifest错误，不计通过。受限执行时Windows临时目录创建反复PermissionError，停止该测试进程后按既有验证授权提升执行，最终28/28及全量278的结果以上述数值为准；无平台自动审批拒绝，无真实HTTP请求。
