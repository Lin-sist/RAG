# 流式问答失败：只读定位

2026-10-18。Report status：`FAILURE_PATH_CONFIRMED_UPSTREAM_CAUSE_UNCONFIRMED`。起点HEAD：`6f18f36`。仅诊断，未修改产品实现、配置或SSE契约，未重新发送真实问答。

## 已定位的失败路径

从运行中的后端控制台读到一条与用户截图症状一致的失败请求。截图本身没有traceId，故不声称已从截图直接核验请求身份。

| 观察项 | 已有控制台证据 |
| --- | --- |
| 错误时间 / traceId | `2026-10-18 09:43:37.304` / `001a14cacefa1d092d644434e4ec2fdd` |
| 检索与生成上下文 | 检索完成；生成contextCount=5，estimatedContextTokens=1254，removedByDedup=0，removedByBudget=0。 |
| 生成模型 | `nvidia/nemotron-3-super-120b-a12b`，由stream_prompt_ready日志确认；openai为兼容adapter名称，不等同于实际服务厂商。 |
| 模型请求路径 | `/chat/completions(stream)`。 |
| 最终结果 | totalLatencyMs=52909，firstChunkLatencyMs=-1，chunkCount=0，answerLength=0，errorType=LLMException。 |
| 前置解析日志 | `Failed to parse OpenAI stream chunk: errorType=NullPointerException`。日志未保留该事件原文。 |

因此，这条请求已进入模型生成，不能解释为“文档未被检索到”。5个片段被送入生成也不证明这些片段与问题充分相关。

用户切换到后端终端后，另通过app终端只读快照交叉核对：09:42:46.198检索完成，09:42:46.199生成准备完成，09:43:37.299解析日志、随后LLMException和同一traceId失败。两种读取渠道一致；快照仍未包含上游finish_reason或正文/推理字段统计，不能进一步确认上游原因。仅保存字段白名单到本地terminal-observations.json。

[AnswerGeneratorImpl](../../rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java)的callOpenAIStream只将delta.content作为正文，再经过sanitizeStream。上游正常结束但没有剩余正文时，switchIfEmpty抛出invalid_response类别的LLMException；[QAController](../../rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java)将其映射为STREAM_FAILED。当前控制台错误类型、零正文及本地复现与此路径一致，而非已观察到的HTTP错误或已识别的超时路径。

## 同版本代码的本地验证

使用后端进程的既有classpath，实际执行同一安装JAR中的AnswerGeneratorImpl；该类字节与rag-core/target/classes一致，SHA-256为`45e3557dfa96276cfb8855d9b5cdcbc6cd9c8b556eca4b66a18c72f74226adbf`。通过WebClient的内存ExchangeFunction提供合成SSE，未发HTTP、未使用业务资料或真实凭据。

| 合成响应 | 结果 |
| --- | --- |
| 正常delta.content | 成功，有正文。 |
| 正常正文后跟choices为空的usage事件 | 成功，有正文；空choices会产生同类解析日志，但不单独导致整体失败。 |
| reasoning_content后跟正常正文 | 成功，仅正文可见。 |
| 只有reasoning_content、finish_reason=length及空choices usage事件 | 零正文，LLMException / invalid_response，与观察到的失败路径一致。 |
| 以message.content代替delta.content的事件 | 零正文，同类失败。 |
| 损坏的JSON事件 | 零正文，同类失败。 |

6项预期均通过，每项1次内存exchange、retry=0。这证明多种上游情况会汇聚为同一个错误，不能把合成样本当作真实provider原始响应，也不能把解析空指针直接认定为唯一根因。

## 尚未确定及下一定位点

优先假设为模型只返回推理内容或正文在清洗后为空；其次为实际事件格式与解析器不兼容。现有日志未记录原始帧、finish_reason、正文/推理事件计数，无法在这两类原因之间定论。实际max_tokens、HTTP状态、timeout/retry及provider使用量也未从本次控制台核验，不以默认值冒充运行值。

后续若需继续到确定根因，建议先设计只记录结构与计数的诊断：HTTP状态/content-type、事件JSON是否可解析、choices数量、delta字段是否存在、正文/推理字符数、finish_reason、usage及重试次数，不记录prompt、片段、答案、推理正文或Authorization。再由用户手动复现，或在明确目标、模型、出站载荷和费用预算后授权Agent一次受控调用。不能将reasoning_content作为最终答案，也不应先靠扩大token预算、改模型或重试掩盖原因。当前未实施这些代码或配置改动。

本轮Agent新增真实业务HTTP/provider/embedding/ask/rerank/judge调用与业务写入均0，真实新增费用0。既有失败请求的实际provider次数/费用未知；本地结果不证明真实流式已恢复。没有代码修改，不重跑正式build、Maven或Python评测；上述合成验证为临时诊断程序，不冒充正式测试套件。

本地证据目录：`C:\Users\Lin\.codex\visualizations\2026\10\16\01a14286-d48b-7ba2-b889-5192b44db56e\stream-diagnosis-2026-10-18`。保留脱敏diagnosis-summary.json、synthetic-results.json、StreamEmptyDiagnostic.java和合成输出；未保存真实请求/响应正文。进程classpath仅存本地，不进入Git。

[用户手工验收](frontend-user-manual-acceptance-2026-10-18.md) · [F6客户端验收](frontend-f6-client-acceptance.md)

## 后续实施：脱敏结构与计数诊断

实施状态：`LOCAL_VALIDATED_NOT_LIVE_VERIFIED`。用户明确要求继续增加诊断，按Type B观测维护实施，前面的只读调查为此前事实。产品行为仍使用既有解析、清洗、retrieve错误处理、重试/超时和空结果判定；不将推理正文作为答案，不改变API/DTO/终态/历史或provider请求字段。仅覆盖本次涉及的OpenAI兼容流式路径，Qwen原生路径不扩入。

[StreamResponseDiagnostics](../../rag-core/src/main/java/com/enterprise/rag/core/rag/generator/StreamResponseDiagnostics.java)使用固定大小的标量计数器，每个订阅有独立streamId，每次尝试重新计数；在完成、错误或取消时输出一条INFO级`llm_stream_attempt`汇总。traceId在异步响应前捕获，仅接受16–32位十六进制值，否则写missing；不会修改MDC。重试各尝试共用streamId并有独立attempt/retryCount。已经观察到错误后由重试操作符触发的取消仍记error，避免误判用户取消。

| 字段组 | 含义及边界 |
| --- | --- |
| httpStatus / contentType | 实际响应状态；响应前取消为-1。类型只输出sse/json/other/missing，不输出原始header及参数。 |
| events / jsonEvents / jsonParseFailures / extractionFailures / doneEvents | String解码器交付的块数、readTree成功次数、JSON解析失败、已解析后字段提取失败、DONE数量。events不等于网络包数或原始SSE帧数，尤其响应类型异常时。 |
| missingChoicesEvents / emptyChoicesEvents / maxChoices / deltaEvents / messageEvents | 固定JSON位置的结构计数；只观察与既有解析器一致的第一个choice，不记录字段名列表或任意值。 |
| contentEvents / contentChars / reasoningEvents / reasoningChars | 非空文本content、reasoning_content/reasoning的事件及字符数；推理计数为两个已知推理字段的长度之和，不记录或留存正文。字符数不是token或计费值。 |
| visibleChunks / visibleChars | 清洗后真正流向下游的块和字符数，可与清洗前正文计数区分。 |
| finishReason | 仅stop/length/tool_calls/function_call/content_filter/other/missing；未知上游字符串映射other，不截断后输出原值。仅反映首个choice最后观察到的非空结束原因，不代表业务终态。 |
| usageEvents / promptTokens / completionTokens / totalTokens / reasoningTokens | 仅接收固定位置的非负long整数，缺失或非法值为-1；多个usage事件保留最后有效值，不求和，不把缺失当0，不用于费用结论。 |
| maxOutputTokens / timeoutSeconds / maxRetries / retryCount / elapsedMs / outcome | 本次请求预算/配置与实际尝试次序、尝试耗时、complete/error/cancel结果。outcome=complete仅表示该生成流完成，不证明引用校验、SSE交付或历史保存成功。 |

下一次人工复现可按traceId和`llm_stream_attempt`定位：reasoningChars>0、contentChars=0且finishReason=length支持“收到推理但没有正文并到达长度限制”；jsonParseFailures>0或messageEvents>0支持格式/解析问题；contentChars>0且visibleChars=0说明正文在清洗后没有可见输出。均须结合实际响应/终态判断，单项不自动认定根因。

### 验证与生效条件

- `mvn -q -pl rag-core -am "-Dtest=AnswerGeneratorStreamDiagnosticsTest,AnswerGeneratorImplResilienceTest,AnswerGeneratorImplSanitizerTest,AnswerGeneratorTelemetryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：25/25、失败0/错误0/跳过0；含10项新诊断测试及15项既有回归。测试仅内存响应或loopback合成服务，无真实provider调用。
- 覆盖纯推理/length/usage、正常正文加空choices、推理后正文、非法JSON与message形状、恶意finish/header/usage值、503不重试、重试隔离、取消、异步trace及错误后取消。敏感问题/正文/推理/凭据/错误体均断言不进入日志。
- 对受影响两个产品源码运行既有敏感日志扫描器，0发现；文档链接、日志只追加、精确文件范围及diff检查另做聚焦核验。初始9项因缺诊断日志预期失败；实现后发现测试辅助函数不接受空断言列表，修正后24/24通过；追加错误后取消专项先失败，再修正诊断结果顺序，最终25/25通过。受限Maven依赖访问和PowerShell未引用-D参数失败不计通过，提升并引用参数后执行成功。
- 未改前端/Python评测，跳过前端build及Python评测；使用聚焦Java回归，不声称全仓Maven测试通过。依赖/lock/模型/默认配置/受保护文件未改。
- 当前运行后端没有重启，未向本地Maven仓库安装新JAR，因此旧进程不会自动获得日志。需要下一次按既有流程重新构建并启动后端后再由用户手动复现。Agent本轮真实业务/provider调用及业务写入仍0，不把本地通过称为原故障已修复。
