# C19 full 执行包（2026-10-18）

Report status：`FULL_INCOMPLETE_TIMEOUT`。本次在clean HEAD14efadc执行42次后按首错停止：41有效/HTTP200、1次timeout，余30未调用、retry0。compiler INCOMPLETE，完整agreement/repeat率N/A。原[full计划](../../../docs/eval/reports/c19-full-plan-v1.json)保留执行前授权状态，实际见[安全review](../../../docs/eval/reports/c19-full-review-v1.json)。新run未授权，见[恢复审阅包](recovery-plan.md)。

此页专属原60秒full，保留原状态。后续独立120秒canary已4/4完成，见[新review](../../../docs/eval/reports/c19-canary-timeout120-review-v1.json)；下一轮[120秒full计划](../../../docs/eval/reports/c19-full-timeout120-plan-v1.json)授权/实际0，不拼接本页partial或任一canary。

## 本轮执行约束与实际

- 本run获授权最多72次judge POST，24×3；实际尝试42、剩余30未调用，不能续跑或拼接旧41条。前次4条canary独立保留；两阶段已尝试总计46，收到45个HTTP200，1个timeout未记录HTTP状态，不能推断未计费。
- provider/model/endpoint保持NVIDIA NIM / `nvidia/nemotron-3-super-120b-a12b` / `https://integrate.api.nvidia.com/v1/chat/completions`。
- manifest canonical SHA-256保持`486a649590bd06d617349436a8e94635fd536048ce0c855848a7a4ab1a69190a`；共享prompt/parser/0.70阈值、执行源码hash与canary相同。full记录运行前clean HEAD14efadc，已执行42条绑定同一身份，其余30条未执行。
- temperature0、timeout60s、max_tokens1024/次、context上限6000字符、串行间隔至少2s、retry0、首错停止、不redirect/fallback/resume/补跑。若72次全执行，间隔等待共142秒，另计模型处理时间。
- 出站为tracked合成question/answer、fixture excerpts/source和固定system prompt；72条JSON请求体合计**58191字节，约58KB**，不含HTTP头/传输开销；金标/ID/用户业务数据不发送。
- 原计划配置的输出token上限合计73728，不等同实际token用量或计费承诺。费用依据沿用用户确认的免费额度，账号账单/具体限流未独立核验；后续新请求会继续消耗额度，可能遇到429、认证、超时或解析失败，须另行授权。
- 凭据仅live时从同一本地项目环境源只读映射到子进程，不写入源文件/命令/日志/回复。新建ignored唯一run目录保留identity/ledger/completion、安全摘要；不调用backend、embedding、rerank、普通ask或KB业务。

## 正式验收

1. full必须完整72个case/repeat observation；任何缺失/错误不得用成功子集或canary补齐，未知outcome不重放。
2. compiler独立重算parse coverage、三组confusion/agreement、provider-pass mismatch及24个case三轮布尔预测一致性；完整低agreement如实保留，不调阈值。
3. 完成后形成安全审阅包，用户接受前不归档、不合入baseline、不设置IDLE；C20 profile/硬门禁仍在范围外。

精确ID顺序/72-key指纹与源码hash仍见原计划；本次已经按错误策略停止，不因名额未用完而补跑，不转C20。恢复分支需新授权；原60秒manifest及partial证据保全。
