# 前端Demo用户手工验收补录

2026-10-18。Report status：`USER_MANUAL_CONFIRMED_STREAM_SUCCESS_OBSERVED`。初次核对起点HEAD：`047c87336837ffa60fe4edee49161f32128ec178`；后续10:36成功补录见文末，原失败仍保留。

用户表示已手动验证，基本功能可以正常实现，流式问答存在失败。本记录接受普通客户端路径的用户手工确认，将流式异常单列待查；不把截图扩大为全部业务场景或生成质量通过。没有新Type C，也不自动启动C19/C20。

## 证据及适用范围

| 附件 | Agent从用户截图观察到的事实 | 仍不能证明 |
| --- | --- | --- |
| 图1 | 同步答案、引用文件与片段展开已显示。 | 回答准确性、生成质量指标及对应历史持久化。 |
| 图2 | 知识库详情显示文档已完成、分块/向量统计与索引信息。 | 上传HTTP202及非空taskId、完整任务状态流转、实际拖放动作。 |
| 图3 | “我的反馈”显示评分、评论和记录时间。 | 反馈POST响应、刷新后的持久化、权限/限流/迟到写入矩阵。 |
| 图4 | 流式界面显示未返回答文本、失败原因为`STREAM_FAILED`，没有将失败结果呈现为完整答案。 | 流式成功、异常根因、服务端已取消或没有产生provider费用。 |

用户手工确认与截图观察共同补充真实使用证据；Agent未独立重放操作。截图原件只保存在本线程本地证据目录，不进入Git；tracked记录不保存知识库名称/ID、文档内容、问题、回答或评论原文。

本地证据目录：`C:\Users\Lin\.codex\visualizations\2026\10\16\01a14286-d48b-7ba2-b889-5192b44db56e\manual-2026-10-18`。

| 文件 | SHA-256 |
| --- | --- |
| manual-01.png | 2992ca7ab44cab848f1f59880ade2f3e02d39f8fc9de014b344816a0fb0638a5 |
| manual-02.png | 08af3f447546e011cc84651c562467face36cf56111de3ba9cc277a9b8453af1 |
| manual-03.png | efbf8da287df06605518fde3ec40e2899b71ec624b8861ad28e9cdc8d87039b2 |
| manual-04.png | 7823a11930289849c0de068a459ac0fe52359fd00c0a7dfcb14c8b1edddbb874 |

## 流式异常的只读核对

- [QAController](../../rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java)的`streamErrorReason`将已识别的超时映射为`TIMEOUT`，其余异常兜底为`STREAM_FAILED`。因此该字段不足以定位文档、检索、provider、网络或其他链路原因，也不能排除未被该函数识别的超时。
- [RAGServiceImpl](../../rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java)的无检索结果分支走`NO_ANSWER / NO_RESULT`并给出无相关信息提示，与通用异常路径不同。不能把本次异常直接归为“文档没有答案”。
- [qaPresentation](../../rag-frontend/src/utils/qaPresentation.ts)按有效答案终态决定是否展示来源；失败结果不展示完整答案来源符合既有客户端规则，不代表流式链路成功。
- 已检查的本地日志目录没有找到能对应本次操作的近期记录，未用历史错误猜测本次根因。下一定位点是同次请求的traceId、后端异常类型/堆栈及provider错误类别和HTTP状态；任何日志使用前均需去除凭据及业务正文。

## 当前结论与剩余项

| 分类 | 结论 |
| --- | --- |
| confirmed | 普通客户端路径获得用户手工确认；截图中的同步答案/来源、文档完成状态、反馈列表可见。既有F6客户端工程/视觉验证仍复用原记录。 |
| partial | F4/F5真实使用证据增加，但专用执行包L1/L2的HTTP/任务/历史/刷新回读链条尚未补齐，不标为PASS。 |
| unknown | 本次流式失败根因及修复、真实拖放/删除清理、认证撤销与权限/限流、物理设备及长时性能。 |
| planned | 优先定位本次流式异常；根据对应请求证据决定是否需契约内修复或新Type C，再进行受影响验收。 |
| out_of_scope | 全量搜索、多轮、账号/工作台和C19/C20；不因普通功能手工确认自动扩入。 |

10月17日预检和Docker故障是历史快照。用户截图显示手工操作时已有可用页面与业务结果，不能继续用旧预检断言当前环境未就绪；本轮也未独立核验当前容器/服务健康。

本轮Agent新增HTTP/业务/provider调用、业务写入及费用均为0；用户自行操作的实际provider、模型、请求次数、timeout/retry与费用未知，不记为0。未运行新的build、Maven、Python评测或浏览器矩阵，因为仅更新验收文档、未修改实现；文档链接、范围与diff另作聚焦验证。原始57张F6证据及10月17日预检快照不变。

[F6客户端验收](frontend-f6-client-acceptance.md) · [真实闭环执行包](frontend-live-integration-execution.md) · [总规划](frontend-demo-backend-integration-plan.md) · [技术债](technical-debt.md)

## 同日后续诊断补录

用户随后要求定位流式失败。已从运行后端的控制台找到09:43:37的一条失败：检索与生成contextCount=5、实际模型已确认，但正文chunk和长度均0，耗时52909ms；失败路径定位至生成流结束后的正文提取/空结果校验。6项同版本合成诊断已通过，真实上游为何没有可见正文仍未定。详见[只读诊断](frontend-stream-failure-diagnosis-2026-10-18.md)。上文“未找到近期日志”是初次文件日志检查结果，本次直接读取控制台补充了证据；未重发真实问答、未修改产品代码。

## 新诊断生效后的用户流式成功补录

用户再次手测并提供完整答案/来源截图。10:36后端日志确认新增诊断已生效：HTTP200、sse、finishReason=stop、attempt1/retry0；推理239事件/3578字符，正文9事件/93字符，清洗后9块/92字符。usage记录1972输入/946输出token，其中reasoningTokens857；总耗时18878ms，首正文18440ms。出现历史insert日志，未独立GET回读。完整脱敏字段及原图hash见[诊断报告最新节](frontend-stream-failure-diagnosis-2026-10-18.md)。本次成功confirmed，前次失败根因unknown，不因成功宣称诊断代码修复了原问题或全部稳定性通过。Agent未主动查询业务数据、重发问答或重新调用模型。
