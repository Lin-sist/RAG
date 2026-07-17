# RAG System Spec Delta: C5b Index Task Reconciliation And Safe Resume

## ADDED Requirements

### Requirement: 文档索引任务的 durable ledger

系统 SHALL 为每个新接受的文档索引任务持久化稳定 taskId、document/owner 关联、执行 status/phase、attempt、lease 与安全 failure facts。MySQL ledger SHALL 是 document index task 的 durable source of truth；Redis MAY 作为低延迟状态投影，但 TTL、miss 或进程退出 MUST NOT 抹掉已接受任务身份。

客户端获得 taskId 前，系统 SHALL 按 durable input/document、DB ledger、Redis initial projection、local scheduling 的顺序建立 acceptance。任一已知失败 MUST NOT 返回可轮询的假任务，task operation 调用次数 MUST 为 0。

durable task payload/result MUST NOT 保存 input bytes、storage key、绝对路径、原文件名、标题、正文、chunks、embedding/vector payload、provider raw message 或凭据。

#### Scenario: 新任务完成 durable acceptance

- GIVEN 用户上传受支持文档且 C5a input 已原子发布
- WHEN 系统返回 documentId 与 taskId
- THEN DB ledger 已保存稳定 task identity、owner 与 ACCEPTED phase
- AND Redis 已保存可查询的 initial projection
- AND taskId 在后续 resume attempts 中保持不变

#### Scenario: Ledger 或 initial projection 失败

- GIVEN durable input/document 已建立
- WHEN DB ledger 或 Redis initial PENDING projection 写入失败
- THEN 系统不返回假 taskId
- AND task operation 调用次数为 0
- AND document/input/ledger 留下可清理或可协调的明确事实

#### Scenario: Redis projection TTL 或 miss

- GIVEN taskId 已由系统接受且 DB ledger 仍存在
- AND Redis 正常但 projection 已过期或缺失
- WHEN owner 查询 task status
- THEN 系统从 DB ledger 返回 sanitized durable status
- AND best-effort 重建 Redis projection
- AND 不把任务误报为 404

### Requirement: 跨实例 claim 与孤儿任务协调

系统 SHALL 使用 durable compare-and-set lease 保证同一 document index task 在同一时刻至多一个 active owner。claim、heartbeat、lease expiry、attempt 与 next-attempt SHALL 可持久化判定，并 SHALL 使用数据库时间避免应用节点时钟漂移决定归属。

协调扫描 SHALL 有 batch、concurrency、lease、backoff 与 max-attempt 上限。lease 未过期的任务 MUST NOT 被其他实例执行；lease 过期只表示可重新分类/claim，不表示任何外部 mutation 未发生。

#### Scenario: 两个实例竞争同一任务

- GIVEN 两个 coordinator 同时扫描到同一可恢复 task
- WHEN 两者尝试 claim
- THEN 只有一个 compare-and-set lease 成功
- AND task operation 总调用次数为 1
- AND 失败竞争者不修改 phase、attempt 或外部副作用

#### Scenario: Active worker 正常 heartbeat

- GIVEN worker 持有未过期 lease 并持续执行安全 phase
- WHEN coordinator 再次扫描
- THEN 该 task 不被其他实例接管
- AND heartbeat 只能由当前 lease owner 更新

#### Scenario: Worker 消失且 lease 过期

- GIVEN task 非终态且原 owner 不再 heartbeat
- WHEN 数据库 lease 到期
- THEN 其他实例可原子 claim 并增加 attempt
- AND 后续动作仍必须按 durable phase 分类

### Requirement: Phase-aware safe resume

系统 SHALL 在文档索引副作用边界持久化至少 ACCEPTED、SAFE_PRE_VECTOR、VECTOR_IN_FLIGHT、VECTOR_CONFIRMED、FINALIZING 与终态 phase。只有可证明未进入 vector mutation 的 SAFE_PRE_VECTOR，或已明确 VECTOR_CONFIRMED 且只需收尾的 task，MAY 自动续跑。

新 C5b task SHALL 使用 versioned、可重现的 chunk/vector identity，并持久化足以验证 content、chunk config 与 chunk count 的安全 contract facts。恢复时 contract、input、content hash、chunk count 或 deterministic IDs 不一致 MUST fail closed，MUST NOT 继续 embedding 或 vector mutation。

#### Scenario: Pre-vector 中断后安全续跑

- GIVEN task phase 为 SAFE_PRE_VECTOR、input AVAILABLE 且 lease 已过期
- AND persisted index contract 与重新解析 facts 一致
- WHEN 新实例 claim 该 task
- THEN 系统可重新 parse/embed 并继续索引
- AND 沿用相同 taskId 与 deterministic vector IDs
- AND attempt 有界增加

#### Scenario: Vector in-flight 中断

- GIVEN task phase 为 VECTOR_IN_FLIGHT 或 failure 为 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- WHEN lease 过期或新实例协调该 task
- THEN task 进入 `RECONCILIATION_REQUIRED`
- AND vector mutation 调用次数为 0
- AND 系统不声称原 mutation 未执行、已回滚或可以安全自动重试

#### Scenario: Vector confirmed 后中断

- GIVEN ledger 明确记录 VECTOR_CONFIRMED
- AND document DB finalize 尚未明确完成
- WHEN 新实例按相同 contract 恢复
- THEN 系统只重建/校验 deterministic facts并完成 DB/keyword/input 收尾
- AND vector upsert 调用次数为 0
- AND document count/chunks/contentHash 不重复提交

#### Scenario: Resume contract 不一致

- GIVEN input、content hash、chunk config/version、chunk count 或 deterministic ID 与 ledger 不一致
- WHEN coordinator 评估恢复
- THEN task 进入 `RECONCILIATION_REQUIRED`
- AND embedding/vector mutation 调用次数为 0
- AND 客户端与日志不暴露不一致的原始内容

### Requirement: Legacy 与 cleanup reconciliation

C5b 上线前没有 durable ledger/phase 的 PENDING/FAILED document MUST NOT 仅因 input AVAILABLE 就自动恢复。系统 SHALL 把无法证明 mutation 安全边界的 legacy task 表达为稳定协调状态，并保留人工判断证据。

document 已 COMPLETED 且 input state 为 CLEANUP_PENDING 时，系统 MAY 有界重试幂等 input delete；该路径 MUST NOT 调用 parser、embedding、vector mutation、rerank 或 generation。cleanup 失败 SHALL 保持 CLEANUP_PENDING，不得报告 CLEANED。

#### Scenario: Legacy 未完成任务没有 ledger

- GIVEN 升级前 document 为 PENDING/FAILED、input AVAILABLE 但没有 durable phase
- WHEN C5b 扫描该记录
- THEN 系统标记 `RECONCILIATION_REQUIRED`
- AND 不自动 parse/embed/vector replay
- AND 不删除仍可用于人工判断的 input

#### Scenario: Cleanup pending 重试成功

- GIVEN document 已 COMPLETED 且 input state 为 CLEANUP_PENDING
- WHEN coordinator 幂等删除 input 得到 DELETED 或 ALREADY_MISSING
- THEN input state 变为 CLEANED
- AND parser、embedding 与 vector mutation 调用次数为 0

#### Scenario: Cleanup pending 重试失败

- GIVEN input delete 返回 FAILED 或稳定 cleanup error
- WHEN coordinator 处理该结果
- THEN input state 保持 CLEANUP_PENDING
- AND 按有界 backoff 等待后续协调
- AND 不把失败伪装成 CLEANED

### Requirement: 恢复开关、调用预算与安全诊断

reconciliation classification 与会触发 provider 的 auto resume SHALL 使用独立开关。系统 SHALL 支持在不产生 provider 调用的情况下扫描、claim 分类和 cleanup；会重新 embedding 的 auto resume 默认 MUST 为关闭，只有显式启用且调用预算获授权后才能运行。

恢复 diagnostics MAY 包含安全 task/document 标识、phase、attempt、lease category、stable failure code、dependency、operation、errorType 与 traceId。系统 MUST NOT 记录 storage root/key、文件名、标题、正文、chunks、embedding input、vector payload、provider raw message、credential、Redis value 或 task DB payload。

#### Scenario: Auto resume 未启用

- GIVEN reconciliation scan 已启用但 resume 开关关闭
- WHEN 发现 SAFE_PRE_VECTOR orphan task
- THEN 系统可 claim/classify 或报告候选
- AND embedding/vector mutation 调用次数为 0
- AND task 不被伪装成已恢复完成

#### Scenario: 有界 resume 预算

- GIVEN auto resume 已显式启用并获调用授权
- WHEN coordinator 执行一个扫描周期
- THEN 候选数、并发、attempt 与 backoff 不超过配置上限
- AND 每个真实 provider 调用可归属到安全 taskId/attempt 诊断
- AND 不把限流/费用失败变成无界重试循环

#### Scenario: 故障内容包含敏感 marker

- GIVEN ledger payload、input facts、provider error 或 Redis value 包含合成敏感 marker
- WHEN 系统生成客户端状态、metrics 与普通日志
- THEN 输出只包含允许的固定安全字段
- AND 不包含上述 marker 或底层 raw message
