# C19 canary 执行包（2026-10-18）

Report status：`PLAN_READY_AWAITING_LIVE_AUTHORIZATION`。离线实现已提交为`e107b2cf001006a56faad36765753f8cd0845564`；用户随后提供模型信息，现将该模型独立冻结为C19 judge。此文件与模型配置提交后，执行时再读取实际clean HEAD，不把准备过程中的dirty HEAD称为正式运行身份。

## 固定目标及载荷

| 项目 | 值 |
| --- | --- |
| provider / endpoint | NVIDIA NIM，`https://integrate.api.nvidia.com/v1/chat/completions` |
| judge model | `nvidia/nemotron-3-super-120b-a12b`，按用户本次提供信息选定；不是从生产配置隐式继承 |
| manifest | [c19-judge-calibration-v1.json](../../../docs/eval/config/c19-judge-calibration-v1.json) |
| manifest canonical SHA-256 | `486a649590bd06d617349436a8e94635fd536048ce0c855848a7a4ab1a69190a` |
| corpus / contract | 既有24条`judge-calibration-v1` / `rag-judge-v1`；canary每象限第一条 |
| selection | `cal-ft-rt-01`、`cal-ft-rf-01`、`cal-ff-rt-01`、`cal-ff-rf-01`，顺序固定，各1次 |
| 最大请求数 | 4次judge POST；首错停止，无重试、redirect、fallback或补跑 |
| runtime | temperature=0、timeout=60s、间隔至少2s、context上限6000字符、`max_tokens=1024`/次 |
| 请求体大小 | 四条JSON请求体分别871/897/779/761字节，合计3308 UTF-8字节；不含HTTP头/传输开销 |
| 数据出站 | tracked合成问题/答案、fixture excerpt/source及固定system prompt；金标/ID/用户资料/KB数据不发送 |
| 本地副作用 | 新建ignored `tmp/eval/`唯一run目录，写identity/ledger/completion及安全摘要；不访问后端、数据库或修改KB |

四次请求配置的输出token上限合计4096，实际用量与provider计费仍须观察，不能据配置承诺费用。共享prompt/parser/0.70阈值、语料/fixture及生产默认行为均不变。

## 授权前置条件

1. 本地配置已选模型，source hash与clean提交HEAD一致；重新执行本地preflight。READY只证明本地身份，不证明provider认证/模型可用。
2. 用户确认本次账号费用/免费额度及配额，再明确授权“最多4次canary”；目前该信息未知，调用授权仍0。密钥存在不等于调用预算授权。
3. 凭据只在本地配置`RAG_EVAL_JUDGE_API_KEY`，不放入tracked文件、命令参数、日志或回复。执行前由本地读取；本准备阶段未读取或验证凭据。聊天中提供的凭据不复制到任何文件；建议用户在控制台换新后本地配置。
4. 本包不含GET模型列表、72次full、embedding、rerank、普通ask/generation、KB上传/删除/清理或服务启动。full需canary完整后另行授权。

若已在同一运行环境配置`NVIDIA_API_KEY`，可在该进程内映射到`RAG_EVAL_JUDGE_API_KEY`，不把值放进命令或文件；后端专用配置不会自动导入此独立脚本。这里仅说明本地凭据绑定，没有执行读取或映射。

## 执行与停止

只有上述条件满足后，运行C19显式入口，profile固定canary，新目录且不可覆盖/续跑。每次请求前保存reservation/start，结束保存outcome；HTTP/网络/限流/超时/解析错误首错停止。中断未知outcome保持unknown，不推断零费用，不重放。

独立compiler复核4条完整证据；失败/缺测时保留coverage/error并抑制agreement。即便完整，canary也不证明24×3 repeat consistency或生产质量；低agreement保留，不调阈值。安全报告只含身份/hash/计数/状态，raw只留本地ignored区域。

## 本轮本地检查

- 已选模型后judge聚焦测试29/29通过；新增一项证明“模型已选择+clean源码”只能返回本地READY，providerAuthentication仍NOT_CHECKED。原缺模型/拒绝live测试改用独立UNCONFIGURED测试副本，不依赖正式模型始终为空。
- 完整Python最新279 tests、1 failure/4 errors；失败集合与先前一致，均为C18旧audit，不重写历史基线或宣称全仓GREEN。
- 生成两个profile的本地计划，预算仍canary4/full72；没有provider请求或模型列表查询。
- 费用/配额、当前模型访问权限和真实认证unknown；不会把旧前端成功或本次提供模型信息当作已校准。
- 准备时只检查环境变量名称：运行环境中没有`RAG_EVAL_JUDGE_API_KEY`或`NVIDIA_API_KEY`；没有读取变量值或后端私有配置。live前仍需完成本地凭据配置，聊天中的值不注入工具或文件。

[任务](tasks.md) · [readiness](readiness.md) · [安全计划摘要](../../../docs/eval/reports/c19-canary-plan-v1.json)
