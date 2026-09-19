# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`generation-objective-evidence-baseline`
- 路径：`openspec/changes/generation-objective-evidence-baseline/`
- 阶段：`C18_CANARY_COMPLETE_FULL_INCOMPLETE_NETWORK_CLOSE`
- 目标：judge关闭下建立v2/150条一次完整真实generation/citation/objective claim/no-answer基线。

## Current Boundary

- C17文件验收归档已完成：4 requirements/12 scenarios已接受进baseline，ACTIVE profile/locked reference、原始三轮各12/12 PASS、Python244 tests通过。归档路径：`openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/`。
- C17已于2026-09-11本地提交：`9276d114058664a3cf33f26fecca046e68e566a4`。用户明确授权Agent本地提交并保持Git干净，原提交审批阻断已解除；C18规划与交接文档随后独立提交。
- 用户于2026-09-11明确要求开始C18实现；W0调用图审计及budget plan-only已完成，W1完整manifest/runner护栏/compiler和负例验证已完成；无任何真实业务调用。
- 当前已冻结canary/full query embedding上限34/1492，含解释回退12/590，ask/generation上限5/150；共享USER限流要求每请求至少2.2秒。33项Java聚焦与252项Python基线保持通过，W1全套Python为265项通过。执行前仍需runtime fingerprint和单独外调授权。
- REST ask会写query count/history；后续外调披露必须包含该本地副作用。2026-09-17已执行两次仅本机读取的 C18 canary preflight，均为 `READY`（vector 50/50、fixtures 3/3、chunk 11/14/25）；provider/embedding/rerank/ask/generation/judge calls=0，业务数据未出站、资源未mutation。
- 用户已授权将 `.env.local` 的 `NVIDIA_EMBED_MODEL` 对齐至现有 KB 15 的 `nvidia/nemotron-3-embed-1b`；普通 `start-backend.ps1` 启动日志与两次 preflight 均验证持久化 identity 生效。
- 本轮C18规划提交责任：`Agent 提交`（2026-09-11用户明确授权本地提交并保持Git干净）。用户已授权C18离线实现，本轮完成W1；未执行任何live调用。后续canary/full及验收/归档仍按对应范围授权；无push/PR/deploy授权。

## 2026-09-18 当前执行状态（覆盖上方旧授权待办）

- 用户明确授权一次性完成 C18 并由 Agent 提交；runtime allowlist 核验及真实只读 preflight READY，3 fixtures/50 vectors，后端启动成功。
- 补齐 C18 runner 失败即停护栏，Python 266 tests PASS；本地提交 `a8c95cf`。
- 真正执行 canary 前，自动审批拒绝命令：要求可信用户内容明确确认开发题目/变体/fixture contexts/prompt 至 NVIDIA endpoint 的具体出站及本地 history 写入，未接受整阶段授权覆盖该载荷。命令未启动，canary/full/provider calls=0。
- W2/W3/W4 未完成；不标 IDLE、不接受 baseline、不归档、不宣称真实 generation 质量。下一步只需解除该具体审批阻断，无需重复离线实现。

## 2026-09-18 具体授权后真实执行（最新状态）

- 用户已明确具体NVIDIA出站和REST写入授权；自动审批通过，授权阻断解除。
- HEAD 65c2d3c clean 上执行canary，首题debug成功、生成HTTP 410 Gone，retry=0，护栏停止；实际debug/ask各1，后4条/full未执行。
- 原始证据保留，脱敏诊断见 docs/eval/reports/c18-canary-20260918-failure.md；compiler NOT_COMPARABLE，另发现Windows路径/judge字段/token脱敏三项元数据兼容性缺口及失败generation计数unknown语义问题。
- 当前不能接受baseline或归档：须先解决provider 410和工具元数据缺口，再以新身份验证；未擅改冻结模型，未重跑。

## 2026-09-18 本地兼容性修复验证

已修复Windows fixture路径规范化、judge模式从已验证的c18Execution读取、仅对完全匹配的公开runtime descriptor保留maxOutputTokens数值；任意同名字段仍脱敏。失败generation计数聚合现在保留null并单列unknown样本数，不再折算成0。真实runner的judge descriptor和sanitize函数已接入回归测试，全套Python 268 tests/OK，git diff --check PASS。仅同步相关源码hash，预算及模型未改。

上文描述的是原始失败run及原版compiler发现的问题；这些离线缺口已修复，但原始失败证据和原版摘要未改写、未重编成COMPLETE。下一步实际阻断仍为冻结生成模型HTTP410；没有追加provider请求，也没有执行full。

## 最新状态：r2 canary失败
- 原Qwen免费端点Deprecated已确认，C18模型契约改为nvidia/nemotron-3-super-120b-a12b；合成检查200、Java CLI身份核验、preflight READY。
- r2首题生成HTTP503，debug/ask各1、retry0；full未启动。详见 docs/eval/reports/c18-canary-r2-20260918-failure.md。
- 并发其他任务修改AGENTS.md/AGENT_LOG，r2 metadata clean=false；不得将本次作为clean基线。后续真实运行须隔离checkout或待用户工作区干净，不暂存其他任务修改。
- 269 tests通过，compiler兼容性修复生效；阶段保持ACTIVE，不归档。

## 2026-09-19 最新：r3 canary完成，full失败
- 隔离checkout固定6c26634，canary/full metadata均clean=true，模型/fixture/preflight核验通过。
- Canary 5/5，CLEAN/objective COMPLETE/judge SKIPPED，零错误零重试，compiler COMPLETE。
- Full实际13/150，第13条HTTP503，前12条成功，零重试；compiler INCOMPLETE，未拼接、未自动重跑。
- 证据：docs/eval/reports/c18-r3-20260919-execution.md；仅W2完成，W3完整基线/W4验收归档未完成。旧410/503证据保持。

## 2026-09-19 最新批准的重试切片
- r4 full2/150因503中断；用户已批准仅429/503最多3次重试，新身份r3-transient，预算canary136/full5968 embedding、20/600 generation。
- 重试实现和277项Python测试已通过；接续新clean canary/full，旧失败证据保留。

## 2026-09-19 r5 最终执行状态（覆盖上方旧状态）
- 用户批准仅429/503最多3次重试；实现提交d3a4f43，277 Python tests PASS；新身份r3-transient，compiler v2。
- clean隔离checkout：canary5/5 COMPLETE，3次503恢复，debug5/ask8。full115/150 INCOMPLETE，38次503恢复，debug115/ask153；第115条multi-hop-007因PrematureCloseException/network停止，无HTTP429/503状态，因此未重试。
- 已知generation HTTP152，另1次unknown；剩余35条未发出，无拼接。raw全部保留；脱敏报告见docs/eval/reports/c18-r5-20260919-execution.md。
- 正式full/接受/归档仍未完成，保持ACTIVE；需另行明确网络断连处理与新完整执行范围，不能扩大仅429/503授权。无push/PR/deploy。
