+# RAG System Spec Delta: C5b Index Task Reconciliation And Safe Resume

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

### Requirement: 数据库任务 claim 边界

系统 SHALL 通过数据库条件更新竞争 document index task lease，并使用数据库时间判断 lease 与 next-attempt 是否到期。只有条件更新成功的 worker MAY 执行后续分类或恢复；失败竞争者 MUST 跳过该任务。

lease 过期只表示任务可被重新 claim 和分类，不表示任何外部 mutation 未发生。VECTOR_IN_FLIGHT 任务即使 lease 过期也 MUST NOT 因此自动重放 vector mutation。

#### Scenario: 两个 worker 竞争同一任务

- GIVEN 两个 worker 尝试 claim 同一个可协调 task
- WHEN 两者执行 durable compare-and-set
- THEN 至多一个条件更新成功
- AND 失败竞争者不执行恢复或外部副作用

#### Scenario: Lease 过期后的重新分类

- GIVEN task 非终态且 lease 已按数据库时间过期
- WHEN 新 worker 成功 claim
- THEN 系统仍按 durable phase 决定后续动作
- AND 不把 lease 过期解释为 vector mutation 未执行

### Requirement: 保守的 phase-aware resume

系统 SHALL 在文档索引副作用边界持久化 ACCEPTED、SAFE_PRE_VECTOR、VECTOR_IN_FLIGHT、VECTOR_CONFIRMED、FINALIZING 与终态 phase。新 C5b task SHALL 使用 versioned、可重现的 chunk/vector identity，并持久化 content、chunk config 与 chunk count 的安全 contract facts。

只有 SAFE_PRE_VECTOR，或已明确 VECTOR_CONFIRMED 且不再执行 vector upsert 的收尾路径，MAY 在显式启用 resume 后执行。VECTOR_IN_FLIGHT 或 `VECTOR_OPERATION_OUTCOME_UNKNOWN` MUST 转为 `RECONCILIATION_REQUIRED`，MUST NOT 自动 vector replay。恢复时 contract 或重新解析 facts 不一致 MUST fail closed。

#### Scenario: Pre-vector 中断后安全续跑

- GIVEN task phase 为 SAFE_PRE_VECTOR、input AVAILABLE 且恢复开关已显式启用
- AND persisted contract 与当前 runtime 一致
- WHEN worker 恢复该 task
- THEN 系统沿用相同 taskId 与 deterministic vector IDs
- AND 可重新 parse/embed 并继续索引

#### Scenario: Vector in-flight 中断

- GIVEN task phase 为 VECTOR_IN_FLIGHT 或 failure 为 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- WHEN worker 协调该 task
- THEN task 进入 `RECONCILIATION_REQUIRED`
- AND vector mutation 调用次数为 0
- AND 系统不声称原 mutation 未执行、已回滚或可以安全自动重试

#### Scenario: Vector confirmed 后收尾

- GIVEN ledger 明确记录 VECTOR_CONFIRMED
- WHEN worker 按相同 contract 恢复
- THEN 系统重新解析并校验 content hash 与 chunk count
- AND 只执行 DB、keyword 与 input cleanup 收尾
- AND vector upsert 调用次数为 0

#### Scenario: Resume contract 不一致

- GIVEN chunk config/version、content hash 或 chunk count 与 ledger 不一致
- WHEN worker评估恢复
- THEN 恢复 fail closed
- AND embedding/vector mutation 调用次数为 0
- AND 客户端与普通日志不暴露不一致的原始内容

### Requirement: Cleanup reconciliation 与恢复开关

document 已 COMPLETED 且 input state 为 CLEANUP_PENDING 时，系统 MAY 有界扫描并重试幂等 input delete；该路径 MUST NOT 调用 parser、embedding、vector mutation、rerank 或 generation。delete 返回 DELETED 或 ALREADY_MISSING 时系统 SHALL 标记 CLEANED；失败时 SHALL 保持 CLEANUP_PENDING。

reconciliation classification 与会触发 provider 的 auto resume SHALL 使用独立开关。会重新 embedding 的 auto resume 默认 MUST 为关闭；只有显式启用且调用预算获授权后才能运行。

恢复 diagnostics MUST NOT 记录 storage root/key、文件名、标题、正文、chunks、embedding input、vector payload、provider raw message、credential、Redis value 或 task DB payload。

#### Scenario: Cleanup pending 重试成功

- GIVEN document 已 COMPLETED 且 input state 为 CLEANUP_PENDING
- WHEN coordinator 幂等删除 input 得到 DELETED 或 ALREADY_MISSING
- THEN input state 变为 CLEANED
- AND parser、embedding 与 vector mutation 调用次数为 0

#### Scenario: Cleanup pending 重试失败

- GIVEN input delete 返回 FAILED 或稳定 cleanup error
- WHEN coordinator 处理该结果
- THEN input state 保持 CLEANUP_PENDING
- AND 不把失败伪装成 CLEANED

#### Scenario: Auto resume 未启用

- GIVEN reconciliation scan 已启用但 resume 开关关闭
- WHEN 发现 SAFE_PRE_VECTOR task
- THEN embedding/vector mutation 调用次数为 0
- AND task 不被伪装成已恢复完成
