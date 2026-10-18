# RAG 项目技术债清单

> 状态日期：2026-09-22
> 本文只登记当前未完成或仍需独立证据的债务，不是活动任务计划。重大改动必须进入独立 OpenSpec change；完成状态以 accepted spec、代码、正式 evidence 和 archive 为准。
> 长期顺序见 `docs/roadmap/iteration-blueprint.md` v6。

## P0：当前运行治理

### 1. C17 readiness 已关闭，保留运行风险

- 2026-09-10 C17阈值获批、ACTIVE profile/locked reference完成；三轮离线12/12 PASS，已验收归档。
- 固定开发集Recall@5仅47.44%；这是防退化门禁，不能外推生产质量。C18单次generation/objective基线已归档，但judge及objective/judge profile仍属C19–C20。
- Docker dockerInference socket复发根因未证实；旧source与失败c17g2按原授权保留，清理必须独立授权。
- 证据见[批准与激活记录](../eval/references/c17-activation-approval-v1.json)。

### 2. 开发态 JWT fallback 治理

- `application.yml` 的开发态 JWT fallback 不属于既有数据库认证 closeout。
- 需要独立评估本地易用性与误带生产环境的风险；不得顺手修改 `.env.local` 或 `application-dev.yml`。

## P1：真实质量证据与用户结果

### 1. Generation / citation / no-answer 重复证据与门禁

- C18已在judge off下完成一次真实v2/150基线：generation keyword hit=79.74%、citation source hit=91.91%、lexical claim support=5.22%、no-answer=95%；31次503均恢复。
- 该结果只有单次固定开发态身份，不是objective release gate；后续仍需独立profile/version和批准的重复evidence决定阈值。
- 词法 claim support不能表述为语义蕴含或完整事实正确性；1次no-answer citation violation与低claim support必须保持可见，不得调题或调prompt掩盖。

### 2. Judge 真实校准

- C9b tooling、24-case 四象限 gold corpus 与状态语义已完成；live calibration 仍为 `SKIPPED`。
- accepted contract 固定 canary 4 × 1、full 24 × 3，最大 judge calls 为 4 + 72；任一 case/repeat 缺失不得从成功子集下 agreement 结论。
- 校准通过前 judge 不应成为 required release gate，也不得自动搜索阈值。

### 3. Objective / judge quality profile

- 当前首个 C10 profile 只覆盖 retrieval，不包含 generation/objective 或 judge gate。
- 需要在真实 evidence 充分后分别建立新 profile/version；retrieval、objective 与 judge 保持独立，judge 可先作为 advisory rule。

### 4. SSE 结构化 terminal result

- 当前流式路径仍以文本 chunk 为主，客户端不能稳定获得 citations、final state、reason、route/budget attribution 与 cancel/error 终态。
- 需要设计兼容 terminal event，并保证同步、SSE 与 MCP 的 final-state 语义一致；中断或 partial output 不得保存为正常成功历史。

### 5. 分块结构专项

- 标题感知、长代码块、长段落与父子块策略尚未形成独立、可比较 evidence。
- 保持 `420/80` 为稳定基线，只做单变量、可回滚实验；不得为固定评测题逐题定制。

### 6. 恢复与中断演练

- durable index ledger/finalize 已完成，但仍需按独立计划持续演练进程中断、输入损坏、长时间 outage 与恢复操作手册。
- 演练结果不得与生产 RTO/RPO 或 HA 声明混用。

## P2：有界策略、知识源与深度研究

### 1. Router 高级策略

- C16 仅完成 default-off `fact-intent-v1 / fact-v1 / evidence-no-answer-v1`。
- `multi-hop-v1 → compare-v1 → temporal-v1 → global-v1 → high-risk-v1` 需要逐项独立 classifier、预算、数据集、评测、门禁与 capability claim。
- 未支持或含糊输入继续 fail closed，不能回退 legacy 或伪装成 fact。

### 2. Knowledge Source Registry

- 尚无统一但权限不扁平化的 indexed document、SQL/API、Web、MCP、Graph、multimodal source contract。
- 需要补 source identity、tenant/permission、freshness/effective time、sensitivity、provenance、failure、cost 与 cache policy。
- 模型和客户端不得选择 tenant、collection、provider/model、任意 filter 或无限预算。

### 3. 多模态证据

- 表格、图片/OCR、页面坐标、音视频证据尚未进入 citation/evaluation contract。
- 顺序为表格与图片/OCR优先，音视频后置；所有结果仍需回连 artifact 与位置证据。

### 4. MCP 2026 migration

- C15 evidence 固定 MCP spec `2025-11-25`，不能直接作为 2026-07-28 验收。
- 需要独立处理协议 breaking changes、stateless core、Tasks、MRTR/input-required、trace context、authorization 与 compatibility。
- 远程 OAuth/TLS/proxy trust 未完成前保持 default-off/local-only。

### 5. Durable investigation task

- 尚无可恢复、可取消、带 checkpoint/预算/计划/claim-evidence artifact 的深度研究 runtime。
- 首版应采用单 durable task + 有界并行子查询，不从 multi-agent swarm 起步。
- 长期 memory 必须 tenant-scoped、可检查、可删除、有 TTL；聊天历史不默认持久化为 memory。

## P3：受控行动与生产化

### 1. 受控写操作与 A2A

- 写 Tool、dry-run、人工审批、幂等、补偿/不可逆风险和审计 contract 尚未实现。
- A2A 只在单 Agent durable task 已成熟、可恢复、可取消、可评测后进入；不替代内部领域模型或 MCP。

### 2. 真实租户与向量库迁移

- C14 的 26/26 PASS 只证明固定 synthetic dual-tenant matrix。
- 真实 Milvus shadow migration/readiness 切换、回滚、第二业务 tenant 与 tenant lifecycle 尚未完成。
- Qdrant/Elasticsearch 仍缺等价 tenant isolation evidence；在 enforcement mode 下必须继续 fail startup，不能静默降级。

### 3. MCP 远程生产边界

- MCP OAuth Authorization Profile、Protected Resource Metadata、audience/resource binding、scope、TLS、proxy trust、Origin、rate limit 与租户审计尚未完成。
- 真实 provider smoke、远程 rollout 与生产开放需要独立 change/evidence/授权。

### 4. 可观测性、容量与 SLA

- C12 只完成单机 synthetic reference stack；生产 HA、容量/费用、合规 retention、租户观测权限、跨主机传输、通知、备份恢复和 SLA 仍未验证。
- local sampling、单机 dashboard 或功能测试不得外推为生产 SLA。

### 5. 前端与反馈闭环

- 前端 design token、空态/错态/处理中态、可访问性、深度研究 task UI、approval UI 和 evidence inspection 仍需分阶段设计。
- production-like corpus、用户反馈治理、drift monitoring 与 release rollback 尚未闭环。

## F4 客户端归档后的证据债务（2026-10-02）

本节仅登记F4归档移交的未验证目标，不重验或改写其他既有债务。F4客户端工程、合成、视觉与上传接受/查询恢复已接受；这不代表完整真实业务闭环。追踪来源：[F4归档验收](../../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/acceptance.md)与[延后任务](../../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/tasks.md)。

- **F4-LIVE（partial，原4.1–4.2 / D1，2026-10-18补录）**：[用户手测截图](frontend-user-manual-acceptance-2026-10-18.md)显示索引完成/统计、同步答案与来源；尚缺上传HTTP202/taskId、任务状态及历史回读的完整链条。[分阶段执行包](frontend-live-integration-execution.md)L1/L2尚未由Agent执行，10月17日环境故障只作历史快照。Agent新增真实预算0；实际provider/模型、timeout/retry、出站/费用/限流等须在Agent执行前确认并授权，不把用户手测当作重跑授权，不宣称生成质量收益。
- **F4-DROP（unknown，原3.4部分 / D2）**：用户手动选文件后的多文件与混合上传已验证，但“已选择”无法独立证明拖放。Chrome自动选文件仍被扩展文件权限阻止，不绕过安全策略；后续取得真实拖入上传区的动作与结果证据后才能关闭，不能改勾为通过。
- **F4-CLEANUP（conditional / D3，原4.3）**：本轮真实数据写入0，无需清理。后续真实验收产生的专用KB/document/history按明确目标单独授权清理；不默认删除既有用户数据。

## 已完成、不得按“从零接入”重复立项

- 数据库认证、主链路 integration、LLM/Redis/Milvus 故障语义；
- durable index input、task ledger、恢复协调与幂等 finalize；
- BM25 + dense vector + RRF hybrid retrieval；
- reranker 接口、heuristic/NVIDIA adapter、fallback 与 provider attribution；
- v1/v2 dataset governance、claim objective metrics、judge contract、offline quality gate evaluator；
- GenAI tracing core 与单机 OTel reference export/metrics；
- tenant identity/enforcement 与固定 synthetic isolation matrix；
- default-off/local-only C15 read-only MCP；
- default-off、仅 fact 策略的 C16 bounded Router。

## 治理基础

- 根目录 `AGENTS.md`：协作、安全与验证规则；
- `.ai/ACTIVE_TASK.md`：唯一活动任务指针；
- `.ai/AGENT_LOG.md`：只追加执行证据；
- `openspec/specs/`：accepted 长期契约；
- `docs/roadmap/iteration-blueprint.md`：v6 长期方向，不替代 active change。

## F5 客户端归档后的证据债务（2026-10-14）

F5按[客户端验收](../../openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/acceptance.md)接受归档，合成验证不等于真实业务闭环。

- F5-LIVE（partial，2026-10-18补录）：用户确认普通功能可用且截图显示我的反馈评分/评论列表；尚缺同次POST响应与刷新回读、专用历史ID关联、真实权限/限流等完整证据。Agent新增真实执行仍需另披露目标、请求预算及副作用并授权。
- F5-CLEANUP（unknown）：真实单删/批量部分失败及删除后反馈关联行为未验证；删除清理需另授权，不能推断原子清空或关联反馈已删除。
- 保留offset快照并发漂移、逐删限流、反馈空GET不能排除迟到写入、我的反馈一次列表/题目ID降级、既有大chunk及窄屏底边取整限制。F4债务/F6/C19/C20独立保留，当前真实调用预算0。


## F6 客户端整体验收后的证据分流（2026-10-16）

[F6客户端验证](frontend-f6-client-acceptance.md)完成，2026-10-18普通路径已获用户手工确认，流式异常单列待查；两类窄屏遮挡已在3条CSS内修复，不能据此认定全部真实闭环完成。F4-LIVE/F5-LIVE补充为partial，其余债务继续保留原边界。

- F6-DEVICE（unknown）：指定桌面浏览器CSS视口矩阵不能证明原生触屏、GPU、Safari/Firefox、320px/200%缩放、完整无障碍或长时功耗；没有新设备证据。
- F6-LIVE（partial）：用户手测补充同步答案/来源、索引完成和反馈列表；真实auth撤销、权限、完整持久化及删除清理链条未补齐。Agent新增真实调用0，用户实际调用次数/费用未知；合成成功不替代真实证据。
- F6-STREAM（unknown，2026-10-18）：用户流式操作出现STREAM_FAILED。代码只证明它是已识别超时之外的异常兜底；无检索结果另走NO_ANSWER/NO_RESULT。未找到对应本次操作的近期本地日志，根因未定，不能直接归因文档；应先取得同次traceId与后端异常/provider错误信息，再决定修复及验收范围，不改变既有SSE契约或默认模型。
- F6-SIZE（partial）：既有>500kB主bundle警告保留，未为审计引入拆包/依赖升级或性能重构；C19/C20质量口径与外部评测另立阶段。
