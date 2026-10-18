# 前端剩余接入：真实闭环分阶段执行包

初稿2026-10-17；补录2026-10-18。Report status：`USER_MANUAL_EVIDENCE_PARTIAL`。起点提交9801260；F1–F5客户端既定页面已接入，登录动效已归档，F6普通客户端路径已获用户手工确认、流式异常保留。用户自行使用真实资料的截图见[手工验收补录](frontend-user-manual-acceptance-2026-10-18.md)，不能替代本包固定合成目标L1/L2的响应与回读核验；L1/L2尚未由Agent执行，预算仍0。本包不扩大已归档契约，也不自动启动C19/C20。

## 当前条件与本轮产出

本轮提供零外调预检脚本、固定合成资料和以下执行边界。2026-10-17首次本机检查：5173/8080/3306/19530不可达，6379可达；受限会话及提升只读检查均未能连接Docker引擎。TCP可达只证明该端口有监听，不能据此证明Redis健康、身份、密码或数据正确。预检本身不启动服务、不创建业务数据、不调用真实provider；[原始预检快照](frontend-live-preflight-2026-10-17.json)保留当时状态。

后续恢复尝试：已启动本机原有Docker Desktop（未安装软件），Desktop/backend进程和Linux engine pipe已出现，但带15秒上限的只读docker ps仍超时，未取得容器清单。没有执行container start/up/restart/pull或任何volume操作；不能认定Docker引擎/RAG基础服务已恢复。用户需在Docker Desktop确认引擎正常后继续；不以无限等待、重建或删除数据绕过该环境条件。本轮真实业务/provider调用及业务写入仍0。

复查命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/frontend_live_preflight.ps1 -Json
```

脚本只检查本机默认端口、已有工具及合成资料hash，不读.env/受保护配置，不登录、不HTTP、不启动Docker、不写业务、不安装依赖。即使所有端口可达也只返回TCP_REACHABLE_ONLY，不能作为真实链路通过或执行授权。

## 先决条件与停止线

1. 执行前复核现有Docker/基础服务，保留原容器和数据卷，不创建替代数据库、不重置或删除数据。上述引擎API未响应为10月17日历史记录；10月18日用户手测已有业务结果，本轮未重新核验当前服务健康，不继续将旧故障作为当前阻塞事实。
2. 启动后端前先确认本地profile、durable input根目录和未完成task库存；已有resume/reconciliation不能越过新预算自动调用provider。默认resume-enabled:false不等于运行值已确认。
3. 使用用户已有的专用测试身份正常登录；账号与密码只在本地交互中使用，不写脚本、Git、日志或截图。没有专用身份就停止，不自动注册/赋权/改密。
4. 启动前端使用已有Vite配置：5173的/auth和/api代理到localhost:8080；不把5188封闭fixture当作真实后端。无需新依赖。
5. 复核实际生效的provider、endpoint、模型/向量维度、retry/timeout、router/rerank及KB模型身份。下表只描述仓库默认值；运行值不同或无法确认时停止，重新披露后再执行，不改生产默认来迎合此次资料。
6. 真实外部调用必须先得到覆盖本包目标/载荷/模型/预算/副作用的明确授权。当前实际预算仍为0，以下为待批准建议上限。执行中任何429、身份变化、写响应不明或异常扩展均停止后续写入，不自动重放。

## 载荷、provider与费用披露

| 项 | 待批准执行范围 |
| --- | --- |
| 专用目标 | 新建一个F4-LIVE-日期-短随机后缀知识库，名称执行时冻结；不得复用/修改用户既有KB，真实ID创建后记入本地证据。 |
| 文档 | [固定UTF-8 TXT](../../rag-frontend/tests/fixtures/frontend-live-source.txt)，不足1KiB，预检输出SHA-256；内容明确为合成资料。按现有semantic/420/80静态预期1个chunk，但必须在上传前用当前解析/分块代码离线预演确认；做不到则保持NOT_RUN，不能先上传后数chunk。策略不同或多于1则停止，不临时调chunker。 |
| 唯一问题 | `查询青禾资料站联络暗号`。不改prompt、检索、阈值或数据集以追求通过。当前查询变体代码静态预计只产生原问题1个变体，运行代码/策略不同必须重新核算。 |
| Embedding默认 | NVIDIA NIM，经OpenAI兼容adapter，`nvidia/nemotron-3-embed-1b`，2048维；默认endpoint为`https://integrate.api.nvidia.com/v1`。不是已确认运行值。 |
| Generation默认 | 同一默认endpoint，`qwen/qwen3.5-122b-a10b`；不是已确认运行值。 |
| 外部调用建议上限 | 文档embedding至多1次HTTP、问题embedding至多1次HTTP、generation至多1次HTTP；外部rerank=0、judge=0。缓存可能减少实际调用；如分块/变体/重试无法证明满足上限，停止，不先试后补授权。 |
| 出站数据 | 合成文档chunk、唯一问题及被召回合成片段会发往上述provider；不使用真实用户资料。API key只由已有本地安全配置提供，不输出。 |
| 费用与限流 | 当前实际费用0（未调用）。真实阶段费用未知，取决于该账户/模型计费与额度，不能保证免费；用户确认账户费用/预算后才执行。任一429停止，不轮换key、不额外探测或重试。 |
| timeout/retry | 前端现有HTTP timeout为30s；仓库LLM默认120s、embedding60s，二者max-retries默认0。前端超时不证明服务端终止，先GET核查原记录/任务，不能再次POST。向量/DB finalize现有最多3次内部重试不等于provider3次；必须保留真实retry归因。 |

上限不是新生产限流器，不承诺修改后端强制执行；它是此次一次性人工联调的停止条件。现有401 single-flight refresh/最多一次认证重发保留并计入HTTP记录；新鲜登录仍不能保证永不401，出现身份问题停止且不扩大provider预算。

## L1：F4最小真实闭环

- [ ] L1.0 前置环境/身份/provider/重试/费用及一次性预算明确确认，记录执行HEAD、实际配置指纹和开始时间；此时仍不读取或写出secret。
- [ ] L1.1 通过正式前端创建专用KB一次；确认真实ID/租户与模型身份。POST结果不明只GET核查，不再创建。
- [ ] L1.2 用户选取固定合成资料并显式开始上传一次；须HTTP202且taskId非空。未确认接受时保留原文件，不再次上传。
- [ ] L1.3 仅查询原taskId，读取上限30次/5分钟，以先到者为准；不取消任务、不重提索引、不改resume。COMPLETED后GET文档/统计，核对chunk数、documentId/taskId/KB归属。单次页面自动poller读取也计数，到上限离开详情页停止客户端轮询；服务端状态仍须单独报告。
- [ ] L1.4 在该KB正式聊天页只人工提交唯一问题一次（同步默认），不再发SSE或debug/retrieve补证。记录HTTP/envelope、metadata.status、answer、citations和traceId；HTTP200或界面有字都不等于成功答案。
- [ ] L1.5 GET分页历史并按真实kbId/问题/traceId定位本次记录，GET详情回读答案和来源。不得假设ask返回的字段就是historyId，也不得把旧记录冒充新写入。

建议人工写操作上限：login1、create KB1、upload1、ask1；已有401认证refresh至多1次及受保护请求最多一次认证重发必须单列。页面自动GET、任务轮询和恢复核查均计入证据，不以人工点击次数冒充HTTP总量。除task GET30次外，其他只读请求建议上限40次；即将到限时停止页面操作/离页，不延长预算。留存queryCount前后值；generation失败也可能增加queryCount，正常history仅完整成功答案才可能写入，实际历史保存仍需GET证明。

## L2：F5反馈读回（仅L1成功且另含该范围授权）

- [ ] 以L1真实historyId提交一次评分4、评论“合成联调：来源可核对；不代表课堂或业务效果”。POST成功按201/对应记录核对。
- [ ] GET该记录反馈、GET我的反馈，再刷新页面回读；核对feedbackId/qaId/当前身份与评分评论，证明持久化而非组件内存。
- [ ] 已知POST成功则只GET恢复；结果不明先GET，空结果不能排除迟到写入，不重复POST。

L2建议额外预算：反馈POST最多1次、只读GET最多10次，外部provider0。未人工验证真实失败/限流/权限/部分删除矩阵，不扩大为F5全面验收。

## 独立保留项

| 目标 | 当前分类与关闭条件 |
| --- | --- |
| F4-LIVE / F5-LIVE | partial：10月18日用户截图补充索引完成、同步答案/来源及反馈列表；L1/L2尚未由Agent执行，完整IDs/响应/回读链条未补齐。 |
| F4-DROP | unknown：须有真实拖入动作与文件进入列表的独立证据；手动选择、程序派发drop或FileList不替代。浏览器文件权限不足时不绕过；也不阻塞本次采用手动选择的L1。 |
| F4-CLEANUP / F5-CLEANUP | out_of_scope：本包不含DELETE或清空；执行后输出精确KB/document/history/feedback清单，由用户另行授权清理。 |
| F6客户端接受 | 普通路径已获用户手工确认，流式异常单列待查；上轮57张正式证据保留，不称全量流式通过。 |
| 全量搜索/多轮/账号/工作台/C19/C20 | out_of_scope，不能从“继续剩余接入”推导为统一授权。 |

## 执行报告约定

每阶段分别记`NOT_RUN / PARTIAL / PASS / FAILED`，包括HEAD、目标IDs、实际provider/model、请求次数、timeout/retry、error类别、出站与费用、截图/响应脱敏证据、query/history/task副作用。失败和初次超时保留，禁止只保留成功复跑。secret/凭据不进入文件；没有真实结果时只写NOT_RUN，不生成假的业务报告。

[总规划](frontend-demo-backend-integration-plan.md) · [F6客户端验收](frontend-f6-client-acceptance.md) · [技术债](technical-debt.md)
