# C18 r6 执行记录：canary 在 embedding 鉴权阶段停止

日期：2026-09-20。执行来源：`4730abdbae84fd692aaea138496bf1a3b2ce4d0e`，隔离 checkout，metadata `gitClean=true`。manifest：`rag-eval-dev-v2-generation-objective-nemotron3-super-r4-network-close`，compiler v3。

## 结果

- 只读 preflight：`READY`，vector `50/50`、fixtures `3/3`、chunk `11/14/25`，`mutationFree=true`。
- 正式 canary：第1条 `fact-001` 的 debug retrieval 在 NVIDIA embedding 请求收到 HTTP 403 后停止；实际最终观测 `1/5`，ask/generation `0`，后4条与 full 均未调用。
- compiler：`INCOMPLETE`；不得依据 preflight、已有向量或失败样本推断 generation/citation/objective 质量。
- 两个不含业务数据的最小合成探针分别调用 embedding 与 chat endpoint，均为 HTTP 403；随后模型目录鉴权读取同为403。当前凭据存在、格式正常，本地 `.env.local` 自2026-09-17未修改，但服务端拒绝该凭据。不能把故障归因于单一模型、C18题目或本次重试实现。

## 调用与边界

- 正式评测实际发生：debug REST 1次；NVIDIA query embedding 1次并返回403；ask/generation/judge/model rerank均为0。没有成功问答history写入，不清理已有query count/history。
- 诊断额外发生：synthetic embedding 1次、synthetic chat 1次、models目录读取1次，均403；不含题目、fixture context、prompt或业务数据。
- `network/PrematureCloseException` 精确重试逻辑尚未在真实运行中触发；403不在允许重试集合，runner按契约停止。
- 没有KB重建/清理，没有修改 `.env.local`、题目、fixture、prompt、C17门禁或业务配置；旧r5 115/150及所有raw保持不变。

## 证据与下一步

- [canary安全摘要](c18-canary-r6-20260920-incomplete-summary.json)
- raw report/details/metadata保存在 ignored `tmp/eval/c18/`，复制前后SHA-256逐文件一致。
- 下一步需要用户在本机 `.env.local` 更新可用的 `NVIDIA_API_KEY`。更新后应先重新执行两个不含业务数据的endpoint探针与mutation-free preflight，再以新no-overwrite身份从canary开始；不得续接本次失败样本或直接启动full。

C18保持 `ACTIVE`，不能接受baseline、归档或进入C19。
