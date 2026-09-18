# C18 2026-09-18 Canary 失败记录

## 结论

C18 未完成。固定 canary 在第一个样本生成时收到 NVIDIA HTTP 410 Gone，runner 立即停止并以非零退出；full 未启动。不能将此记录作为 generation/citation/objective baseline，也不能据 410 单独断言模型已下线（provider 内部原因未验证）。

## 授权与运行身份

用户明确授权开发题目、变体、fixture 上下文与 prompt 发往 NVIDIA integrate.api.nvidia.com，允许既定 canary/full 预算及本地 query count/history，并授权收尾和本地 commit。出站审批已通过，本次不再存在授权待办；当前阻断是 provider HTTP 错误。

Git source HEAD：65c2d3c；运行时工作区 clean。生成模型 qwen/qwen3.5-122b-a10b，OpenAI-compatible adapter，endpoint=https://integrate.api.nvidia.com/v1/chat/completions，temperature=0.2、maxOutputTokens=2048、timeout=120s、maxRetries=0。embedding 模型 nvidia/nemotron-3-embed-1b，2048维，timeout=60000ms、retry=0。judge/model rerank=0。

## 观测事实与边界

- mutation-free preflight：READY，3 fixtures/50 vectors。
- canary 计划5条，实际执行1条；debug retrieval HTTP=1，ask HTTP=1，retrieval error=0，ask error=1，automatic retry=0，full=0。
- 应用日志记录1次生成发起及1次 provider HTTP 410 Gone；没有成功生成。不能用 compiler 中 generationCalls=0 推断真实 provider 请求数为0：失败响应导致该字段为unknown，当前聚合将unknown折算为0。
- debug/ask各经历一次query embedding逻辑，ask阶段明确命中embedding cache；未单独捕获底层embedding HTTP计数，因此不把源码推导当作直接HTTP观测。
- ask依照QAController失败路径计入query count；失败答案不满足isSuccess，不写正常问答历史。未另做SQL差量核查，不声称数据库级独立审计。
- 报告PARTIAL、objective PARTIAL、judge SKIPPED。原始report/details/metadata在ignored目录保留，没有覆盖或拼接。

## 离线 compiler 发现的兼容性缺口

原始产物经现有compiler返回NOT_COMPARABLE，除失败/缺样本外，还报告三个可复现的元数据缺口：Windows fixture路径反斜杠与manifest斜杠逐字比较；compiler要求judgeContractConfig.mode但现有contract_config不输出该字段；通用脱敏将runtime的maxOutputTokens数值替换成REDACTED，导致details内metadata与外置metadata不一致。这些是工具链问题，不是模型质量结论。保留原始产物与原始compiler结果，不通过手工补字段伪造COMPLETE。

## 后续所需

先解决冻结生成模型的HTTP 410可用性；若更换模型，须显式修订模型身份/manifest并重新冻结，不能沿用旧模型身份。还需修复上述compiler/runner元数据兼容性及失败generation计数的unknown语义，使用真实runner产物形状补回归测试。固定失败即停契约下本次canary不自动重试；full预算未消耗，仍以新canary clean为前提。

## 原始产物SHA-256

- -report.md: `ca1b97aa56293feb0af6d51853c34f65ce1b08dd2fbc92b8d4a65bf3f7bcf777`
- -details.json: `12c128d6fac26328865bb79dad552951ad77bdac943f120103a847618972c4d5`
- -metadata.json: `40dc3bfcd1b9ae53efeeb7d0ee49270ee7bded1ccae24385954fa15c25d8d28e`

## 2026-09-18 本地兼容性修复验证

已修复Windows fixture路径规范化、judge模式从已验证的c18Execution读取、仅对完全匹配的公开runtime descriptor保留maxOutputTokens数值；任意同名字段仍脱敏。失败generation计数聚合现在保留null并单列unknown样本数，不再折算成0。真实runner的judge descriptor和sanitize函数已接入回归测试，全套Python 268 tests/OK，git diff --check PASS。仅同步相关源码hash，预算及模型未改。

上文描述的是原始失败run及原版compiler发现的问题；这些离线缺口已修复，但原始失败证据和原版摘要未改写、未重编成COMPLETE。下一步实际阻断仍为冻结生成模型HTTP410；没有追加provider请求，也没有执行full。
