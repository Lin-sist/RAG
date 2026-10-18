# C19 full 执行包（2026-10-18）

Report status：`FULL_PLAN_AWAITING_AUTHORIZATION`。真实canary已在clean HEAD `823b81a373d9cbda5c6b0e9a0d31a77125397430`完成4/4、全部HTTP200、parse coverage100%、错误/重试0；三组agreement均100%，仅限4条/1轮。见[真实安全摘要](../../../docs/eval/reports/c19-canary-review-v1.json)。这不建立full repeat consistency或生产质量证据。

## 需独立授权的下一阶段

- **新增最多72次**judge POST：既有24个case×3 repeats；前次4条canary不拼接进full，总计最多76次（含已完成4次）。当前full授权0、实际0。
- provider/model/endpoint保持NVIDIA NIM / `nvidia/nemotron-3-super-120b-a12b` / `https://integrate.api.nvidia.com/v1/chat/completions`。
- manifest canonical SHA-256保持`486a649590bd06d617349436a8e94635fd536048ce0c855848a7a4ab1a69190a`；共享prompt/parser/0.70阈值、执行源码hash与canary相同。新提交仅保存证据/授权文档；full实际读取并记录其运行前clean HEAD，72条在同一运行身份下执行。
- temperature0、timeout60s、max_tokens1024/次、context上限6000字符、串行间隔至少2s、retry0、首错停止、不redirect/fallback/resume/补跑。若72次全执行，间隔等待共142秒，另计模型处理时间。
- 出站为tracked合成question/answer、fixture excerpts/source和固定system prompt；72条JSON请求体合计**58191字节，约58KB**，不含HTTP头/传输开销；金标/ID/用户业务数据不发送。
- 配置的输出token上限合计73728，不等同实际token用量或计费承诺。费用依据沿用用户确认的免费额度，账号账单/具体限流未独立核验；新72次会继续消耗额度，可能遇到429、认证、超时或解析失败。
- 凭据仅live时从同一本地项目环境源只读映射到子进程，不写入源文件/命令/日志/回复。新建ignored唯一run目录保留identity/ledger/completion、安全摘要；不调用backend、embedding、rerank、普通ask或KB业务。

## 正式验收

1. full必须完整72个case/repeat observation；任何缺失/错误不得用成功子集或canary补齐，未知outcome不重放。
2. compiler独立重算parse coverage、三组confusion/agreement、provider-pass mismatch及24个case三轮布尔预测一致性；完整低agreement如实保留，不调阈值。
3. 完成后形成安全审阅包，用户接受前不归档、不合入baseline、不设置IDLE；C20 profile/硬门禁仍在范围外。

精确ID顺序、72-key指纹、24个case载荷大小及源码hash见[full计划](../../../docs/eval/reports/c19-full-plan-v1.json)。本包只是准备，不能替代新增72次调用授权。
