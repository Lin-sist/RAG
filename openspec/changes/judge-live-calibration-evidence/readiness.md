# C19 事前 readiness（2026-10-18）

Report status：`FULL_TIMEOUT120_INCOMPLETE_NETWORK_ERROR`。已按用户恢复指令执行独立120秒full，clean HEAD91dc567下9次尝试：8个HTTP200有效响应、第9次judge_network_error、63未调用，retry0，compiler INCOMPLETE，正式率N/A。见[新full review](../../../docs/eval/reports/c19-full-timeout120-review-v1.json)及[执行记录](recovery-plan.md)。新run授权0，不续跑或拼接；默认配置/旧证据保留，C19未完成。

| 项目 | 分类 | 核查结果 |
| --- | --- | --- |
| 前端阶段 | confirmed（已接受范围） | [收口索引](../../../docs/roadmap/frontend-demo-closeout-2026-10-18.md)完成；原始archive/证据不动。 |
| corpus / shared contract | confirmed | 24 cases，四象限各6；manifest SHA-256 `54c891c45ac682e38373bab293893d22695a938b7fc5ac9fa205c7f620dedb55`，prompt `d98612cdc4f6681e126d3e6fd4dac81f5d76d14ec4bd596e904fa4843999df80`。 |
| canary / full 本地plan | confirmed（纯本地） | 4×1=4、24×3=72；validator=VALID；judge已冻结为用户提供的`nvidia/nemotron-3-super-120b-a12b`，不表示认证或live已获授权。 |
| calibration 聚焦测试 | confirmed（synthetic） | `python -B -m unittest discover -s scripts -p 'test_judge_calibration.py'`，9/9通过。 |
| normal eval 回归 | confirmed（synthetic） | `python -B -m unittest discover -s scripts -p 'test_run_rag_eval.py'`，47/47通过。 |
| 全部Python回归 | partial / 非GREEN | 模型配置后最新279 tests，1 failure / 4 errors，与前两阶段相同C18失败，含2组setUpClass错误。完整输出曾截断，紧凑unittest结果保全实跑计数/失败集合。 |
| C19实现 | confirmed（offline） | 运行冻结、预算、逐次ledger、compiler及缺测抑制已实现；最新judge聚焦29/29，含20项C19及9项既有calibration测试。 |
| live judge | partial（full incomplete） | 两轮canary各4/4；原full41有效/1timeout/30未尝试，新full8有效/1network error/63未调用，正式率N/A。累计59次尝试/57个HTTP200/2次失败，远端结果/费用未知，新run授权0。 |
| C20 | out_of_scope | 不改profile/阈值，不启用judge required gate。 |

## 已复现缺口

实现前：预期2条、只提供1条有效observation，聚合器输出PARTIAL、coverage=0.5但joint agreement仍1.0。实现后该负例agreement和repeat率均N/A；coverage/错误计数保留。源码/clean HEAD、完整endpoint、逐次ledger与硬预算均由C19显式入口实现，原无C19参数入口保留兼容；见[design](design.md)。

## 实现与CLI验证

- 最新120秒full：HEAD91dc567、run e3db9573-ed8d-4860-a4fd-7d3e39fad2af，第9次cal-ft-rt-03/repeat3在5032ms报judge_network_error且无HTTP状态；同case前2次5266/17092ms成功。当前raw未保存底层URLError reason，无法区分网络/代理/上游连接等根因；不是已确认的120秒超时或429。9次尝试/8有效/63missing、retry0，独立重编译逐对象一致，额外请求0，凭据源未变。正式agreement/repeat率N/A。

- 最新full：source HEAD14efadc、run693fcefd-94a2-4b2b-b919-bc7e9395a93d，42次尝试中41有效，第42次cal-ff-rt-02/repeat3在60265ms超时、无HTTP状态；同case前2次7375/8390ms成功。root cause未知，首错停止/不重试按契约生效。独立重编译与原摘要相同，无额外provider请求。
- 120秒恢复：manifest2c1ac168c87df1e85e42508b7608f2d6d2772fac76a7554ebfefe769b4cae3ea，与默认486a...仅timeout不同。D8已确认，canary4/4仅小样本；随后独立full尝试9次后网络错误停止，8有效/63未调用，不能证明原超时根因修复。默认/源码/评分规则未改，当前新run授权0。

- 真实canary：source HEAD823b81a、manifest486a6495...，4次HTTP200；latency3531/5171/3734/3702ms。三组agreement100%、mismatch0，单轮不建立三轮一致性。raw本地ignored，identity/ledger/completion/safe-summary hash写入安全报告；独立本地重编译逐对象相同，额外provider调用0。凭据源只读且未变。
- 原full计划：24×3=72请求、JSON请求体58191bytes、同模型/manifest/源码/runtime，canary不拼接。该run实际在clean HEAD14efadc尝试42次后停止，剩余30未调用；恢复不能续跑旧run。

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

本次执行恢复授权的独立120秒full9次，因网络错误首错停止；只保存证据/状态，不改代码/默认manifest/评分契约，复用29/29及全Python279的既有C18例外。跳过Maven/前端build/浏览器，OpenSpec为文件级检查。累计59次尝试/57个HTTP200/1旧timeout/1新network error，失败远端完成/费用未知，凭据源未变。backend/embedding/rerank/普通QA及业务写入仍0；未push/PR/部署，不自动补跑或诊断性外调。

受限Python写config失败，改apply_patch生成；首次因此出现16项缺manifest错误，不计通过。受限执行时Windows临时目录创建反复PermissionError，停止该测试进程后按既有验证授权提升执行，最终28/28及全量278的结果以上述数值为准；无平台自动审批拒绝，无真实HTTP请求。
