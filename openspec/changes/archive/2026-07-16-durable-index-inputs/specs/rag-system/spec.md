# RAG System Spec Delta: C5a Durable Index Inputs

## ADDED Requirements

### Requirement: 已接受索引任务的输入持久性

系统 SHALL 在返回已接受的 documentId/taskId 前，把上传输入完整写入应用管理的 durable storage，并持久化可由新进程解析的 opaque storage key、byte size、SHA-256 与 input state。系统 MUST NOT 把请求生命周期 stream、内存闭包或 system temp absolute path 作为已接受任务的唯一输入事实。

durable input 发布 SHALL 使用同一存储根内的 staging + atomic publish。发布、数据库关联或初始 task PENDING 状态任一在 acceptance 前发生已知失败时，系统 MUST NOT 返回可轮询的假任务，并 SHALL 清理可确定未被接受的 staging/object；清理失败 MUST 留下可协调事实，不得伪装成功。

#### Scenario: 输入持久化后任务被接受

- GIVEN 用户上传受支持的文档
- WHEN 系统返回 documentId 与 taskId
- THEN 对应输入已通过 atomic publish 完整存在于 durable storage
- AND 数据库保存 opaque key、byte size、SHA-256 与可用状态
- AND 新应用进程可不依赖原请求或原进程 temp 重新打开同一输入

#### Scenario: Atomic publish 失败

- GIVEN 上传流只写入 staging 或 atomic publish 失败
- WHEN 系统处理该上传
- THEN 不返回已接受 documentId/taskId
- AND 不让任务读取部分最终文件
- AND 清理可确定的 staging 数据或记录 cleanup pending

#### Scenario: 初始任务状态写入失败

- GIVEN durable input 与 document 关联已建立
- WHEN Redis 无法持久化初始 task PENDING 状态
- THEN 系统不返回假 taskId 且 task operation 不启动
- AND 对未接受的 document/input 执行确定性清理或保留明确可协调状态

### Requirement: 索引输入身份与完整性

系统 SHALL 使用与客户端文件名、绝对路径和解析后 `content_hash` 分离的 opaque storage key。任务读取输入时 MUST 验证 regular-file/root confinement、byte size 与 SHA-256；missing、路径逃逸、非普通文件或校验不一致 MUST 在 parser、embedding 和 vector mutation 前失败。

missing input SHALL 表达为稳定 `INDEX_INPUT_UNAVAILABLE`，完整性不一致 SHALL 表达为稳定 `INDEX_INPUT_CORRUPT`。客户端、task error 与普通日志 MUST NOT 暴露 storage root、storage key、绝对路径、原始文件名、标题、正文或底层异常原始 message。

#### Scenario: 新进程重新打开输入

- GIVEN 实例 A 已原子发布输入并持久化 key 与完整性事实
- AND 实例 A 已停止
- WHEN 实例 B 使用相同 durable root 处理该 document
- THEN 实例 B 按 key 打开与校验相同 bytes
- AND 不依赖实例 A 的闭包、stream 或 temp path

#### Scenario: Durable input 缺失

- GIVEN document 记录声明输入 AVAILABLE
- WHEN store 无法找到对应 regular file
- THEN document/task 返回 `INDEX_INPUT_UNAVAILABLE`
- AND parser、embedding 与 vector mutation 调用次数为 0
- AND 不把缺失输入表达为文档无内容或索引完成

#### Scenario: Durable input 被截断或替换

- GIVEN store 中 bytes 的 size 或 SHA-256 与持久化事实不一致
- WHEN task 尝试读取输入
- THEN document/task 返回 `INDEX_INPUT_CORRUPT`
- AND 不继续解析或写入任何索引

#### Scenario: Storage key 尝试逃逸 root

- GIVEN storage key 是绝对路径、包含 traversal 或解析到 root 外的符号链接
- WHEN store 解析该 key
- THEN 操作 fail closed
- AND 不读取、覆盖或删除 configured root 外文件

### Requirement: 索引输入生命周期与清理事实

系统 SHALL 明确区分 `AVAILABLE`、`CLEANUP_PENDING`、`CLEANED`、`MISSING` 与 `CORRUPT` 输入状态。健康索引完成后系统 SHALL 尝试最小化保留原始输入；清理成功后标记 CLEANED，清理失败时保持业务索引结果并标记 CLEANUP_PENDING，不得报告输入已删除。

索引 FAILED、进程中断或 vector mutation outcome unknown 时，系统 MUST NOT 无条件删除仍可用于后续协调的 AVAILABLE 输入。C5a MUST NOT 因输入可用而自动重放任务；orphan detection、lease/claim、replay 与 resume 属于 C5b。

#### Scenario: 健康索引完成并清理

- GIVEN 输入 AVAILABLE 且文档索引明确 COMPLETED
- WHEN durable input 删除成功
- THEN input state 变为 CLEANED
- AND 后续不把该输入表达为可恢复

#### Scenario: 完成后的清理失败

- GIVEN 文档索引已明确 COMPLETED
- WHEN durable input 删除失败
- THEN 文档索引结果保持 COMPLETED
- AND input state 为 CLEANUP_PENDING
- AND 系统不声称原始输入已删除

#### Scenario: 索引失败或 outcome unknown

- GIVEN task FAILED、进程中断或 vector mutation outcome unknown
- WHEN C5a 处理任务终态或中断窗口
- THEN 仍可校验的输入保持 AVAILABLE
- AND 系统不自动 replay vector mutation
- AND 后续协调由 C5b 决定

#### Scenario: Canonical document delete

- GIVEN document 仍关联 AVAILABLE 或 CLEANUP_PENDING 输入
- WHEN canonical document delete 执行
- THEN 系统尝试清理 durable input
- AND 清理失败不得被记录为输入已删除

### Requirement: 旧记录与部署边界

新增输入字段 SHALL 对旧 document 行保持 nullable compatibility。旧 COMPLETED document 没有 durable input 时系统 MAY 继续提供既有已索引结果；旧 PENDING/FAILED document 没有 durable input 时系统 MUST 返回稳定 unavailable，MUST NOT 猜测 system temp 文件、伪造可恢复性或自动重跑。

production profile SHALL 显式配置可写、非 system temp 的 durable root，并在启动时验证；root 缺失、不可写或不满足持久化约束时系统 MUST fail fast，不得静默退回 system temp 或 memory。

#### Scenario: 旧已完成文档没有输入

- GIVEN 升级前 document 已 COMPLETED 且没有 storage key
- WHEN 新版本读取该文档或执行检索
- THEN 已有索引结果保持可用
- AND 系统不把该行标记为可恢复输入

#### Scenario: 旧未完成文档没有输入

- GIVEN 升级前 document 为 PENDING/FAILED 且没有 storage key
- WHEN 系统评估其输入
- THEN 返回 `INDEX_INPUT_UNAVAILABLE`
- AND 不搜索或猜测 system temp 文件
- AND 不自动启动索引任务

#### Scenario: Production durable root 无效

- GIVEN production profile 的 durable root 缺失、不可写或位于 system temp
- WHEN 应用启动
- THEN 启动 fail fast 并给出不含绝对路径的稳定配置错误
- AND 不退回 system temp 或 memory storage
