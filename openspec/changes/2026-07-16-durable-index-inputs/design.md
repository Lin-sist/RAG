# Design: C5a Durable Index Inputs

## 1. Context

当前上传链路把请求内容复制到系统 temp，创建 document 后把 `Path` 捕获进本进程异步任务，最后无条件删除。这个模型只能支撑“同一进程尽快消费”，不能支撑进程中断后的重新打开。

```text
current
MultipartFile -> system temp Path -> in-memory task closure -> finally delete

proposed C5a
MultipartFile -> IndexInputStore atomic put -> opaque storage key + integrity facts
              -> document/task acceptance -> task reopens by key
              -> explicit retained / cleanup-pending / cleaned lifecycle
```

C5a 只建立可恢复输入事实，不决定哪个任务应在何时由谁恢复。C5b 才引入 orphan detection、lease/claim 与 replay state machine。

## 2. Goals

- 已接受索引任务的输入跨应用进程存活，并能由新实例按稳定 key 重新打开。
- 上传字节先完整、原子地进入应用管理存储，再对外返回可轮询 task。
- 输入身份、大小、SHA-256、可用性与清理状态可判定，不复用解析后 contentHash。
- 任务、删除与故障路径不再依赖系统 temp、请求 stream 或原始文件名路径。
- 旧记录、回滚版本和未挂载 durable root 有稳定且诚实的兼容/失败语义。
- 为后续 C5b 提供最小充分事实，但不提前实现恢复编排。

## 3. Non-goals

- 不实现任务扫描、租约、抢占、重放、断点或自动恢复。
- 不接入对象存储、共享存储或新增网络服务。
- 不重构整个异步任务框架，不把 Redis task status 迁移到数据库。
- 不实现跨 filesystem/DB/Redis/vector 的 exactly-once 或分布式事务。
- 不改公开 DTO、前端、解析/分块/检索/生成和评测口径。

## 4. Proposed Components

### 4.1 `IndexInputStore`

内部接口建议只暴露稳定动作：

- `put(staging input, expected metadata) -> StoredIndexInput`；
- `open(storageKey) -> InputStream`；
- `verify(storageKey, size, sha256)`；
- `delete(storageKey)`，结果区分 deleted / already_missing / failed；
- 固定安全 diagnostics，不暴露绝对 root 或客户端文件名。

filesystem 实现把 storage key 解析限制在 configured root 下，拒绝绝对路径、`..`、符号链接逃逸和非 regular file。写入先进入 root 内 staging 子目录，flush/close 后使用同文件系统 atomic move 发布；平台不支持 atomic move 时 fail closed，不退化成可见的部分文件。

### 4.2 Persistence facts

建议复用 `document.file_path` 存 opaque storage key，而不是绝对路径；新增独立字段保存：

- `input_size_bytes`；
- `input_sha256`；
- `input_state`：`AVAILABLE / CLEANUP_PENDING / CLEANED / MISSING / CORRUPT`。

解析后的 `content_hash` 保持原有 KB 内内容去重语义，不复用为上传字节哈希。migration 必须允许旧行字段为空；旧 COMPLETED 行不需要 durable input，旧 PENDING/FAILED 行缺输入时只能表达 unavailable。

### 4.3 Acceptance sequence

建议顺序：

1. 校验类型、大小上限与 durable root readiness；
2. 将请求流写入 root 内 staging，同时计算 byte size 与 SHA-256；
3. 原子发布为 opaque key；
4. 创建 document PENDING 并持久化 key/integrity/state；
5. 提交 Redis async task，确认初始 PENDING 状态已持久化；
6. 返回 documentId/taskId。

步骤 2–5 任一在客户端 acceptance 前发生已知失败时，不返回假 task。可确定未被接受的 staging/object 必须同步清理；若清理本身失败，记录固定 cleanup-pending 事实，不把原路径或文件名写入普通日志。filesystem 与 DB 的崩溃窗口由 C5b reconciliation 最终处理。

### 4.4 Task read and lifecycle

任务闭包只捕获 documentId/storageKey 等稳定标识，并在执行时从 store 重新 open + verify：

- missing -> `INDEX_INPUT_UNAVAILABLE`；
- size/hash mismatch -> `INDEX_INPUT_CORRUPT`；
- 读取错误 -> `INDEX_INPUT_READ_FAILED`；
- 上述失败均不得进入 parser 后续、embedding 或 vector mutation。

健康索引 `COMPLETED` 后把输入置为 `CLEANUP_PENDING`，删除成功后置为 `CLEANED`；清理失败不回滚已经完成的索引，但必须保留可协调状态。FAILED、进程中断和 vector outcome unknown 保持 `AVAILABLE`，供 C5b 决定是否重放。canonical document delete 同样触发输入清理；清理失败不伪装为已删除输入。

### 4.5 Capacity and deployment

首版 root 由配置显式指定，开发环境可使用项目外 `data/index-inputs`，生产 profile 若 root 缺失、不可写、位于系统 temp 或未通过 probe 则 fail fast。每次 put 在读取请求流时执行单文件大小上限；全局容量/配额只做可用空间低水位拒绝，不在 C5a 引入后台配额回收器。

## 5. Failure Matrix

| operation / failure | client/task outcome | durable facts | C5a action |
|---|---|---|---|
| staging write fails | upload not accepted | no published key | clean staging |
| atomic publish fails | upload not accepted | no available input | fail closed |
| DB association fails after publish | upload not accepted | possible unowned object | best-effort delete; cleanup evidence |
| initial Redis PENDING write fails | no fake taskId | document/input not runnable | deterministic cleanup or explicit retained fact |
| task open missing | task/document FAILED | input MISSING | no parser/embedding/vector call |
| size/hash mismatch | task/document FAILED | input CORRUPT | no downstream mutation |
| process stops with AVAILABLE input | no automatic claim in C5a | key remains reopenable | C5b handles orphan |
| index COMPLETED, delete succeeds | completed | CLEANED | no source retained |
| index COMPLETED, delete fails | completed + cleanup pending | CLEANUP_PENDING | C5b cleanup |
| index FAILED/outcome unknown | stable failed code | AVAILABLE | retain for C5b decision |

## 6. Verification Strategy

- unit tests：key validation、path traversal、symlink、atomic publish、size/hash、delete idempotency；
- service tests：acceptance ordering、initial task write failure、open/verify failure、COMPLETED/FAILED cleanup states；
- restart-style integration：实例 A put/associate 后关闭，实例 B 用同 root/key reopen 相同 bytes；不需要真实 embedding/provider；
- migration tests：全新 schema、旧 document 行、nullable compatibility、rollback reader 安全忽略；
- security：日志/task/client 不包含原文件名、标题、absolute root、storage key、正文或异常 raw message；
- regression：现有 C3 happy-path 与 C4c/C4d failure semantics 不被破坏。

## 7. Rollout And Rollback

- rollout：先 migration nullable 字段，再部署支持新字段的应用；只有新上传写 durable input。
- old rows：COMPLETED 可继续读取业务结果；PENDING/FAILED 且无 key 不自动恢复，返回稳定 unavailable。
- rollback：旧应用当前不读取 `file_path`，可忽略新增 nullable 字段；不得由旧版本清理新 storage root。回滚后新输入仍保留，待恢复新版或人工处置。
- C5a 不自动扫描历史 temp 或猜测文件归属。

## 8. 决策记录

### 决策 1：首版存储后端

1. **面临的选择**：A. 应用管理的本地 durable filesystem；B. 直接接 MinIO/S3；C. 继续使用 system temp 但延长删除时间。
2. **选了哪个 + 为什么**：建议选 A，待用户在事前闸门确认；当前没有业务对象存储 adapter，A 能以最小基础设施面建立 durable/reopen 契约，同时保留 `IndexInputStore` 扩展点。
3. **放弃的代价**：B 会引入 SDK、凭据、网络故障和部署依赖；C 仍无法保证容器重建、temp 清扫和跨实例读取，不满足 C5a。

### 决策 2：数据库保存路径还是逻辑 key

1. **面临的选择**：A. 保存客户端或系统绝对路径；B. 保存 root-relative opaque storage key；C. 把文件 bytes 直接存 BLOB。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；key 可跨 Windows/Linux 与部署 root 迁移，并能由 store 统一做路径安全校验。
3. **放弃的代价**：A 会泄露环境并绑定单机路径；C 会放大数据库、备份和事务压力，不符合当前模块边界。

### 决策 3：完整性事实

1. **面临的选择**：A. 只记录 key；B. key + byte size + SHA-256；C. 复用现有 `content_hash`。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；它能在解析前识别截断/错配，且不改变解析后内容去重语义。
3. **放弃的代价**：A 无法区分可读与完整；C 会混淆上传字节身份和标准化解析内容，可能破坏现有唯一约束与去重。

### 决策 4：发布原子性

1. **面临的选择**：A. 直接写最终文件；B. root 内 staging 后 atomic move；C. 写完后仅凭文件存在判断。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；任务只能看到完整发布的对象，崩溃留下的 staging 也可与可用输入区分。
3. **放弃的代价**：A/C 可能让消费者读取部分文件或把截断文件当成功输入。

### 决策 5：任务捕获内容

1. **面临的选择**：A. 继续捕获 `Path`/stream；B. 只捕获 documentId/storageKey 并执行时 reopen；C. 把整个文件放进 Redis task payload。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；稳定标识可跨进程，且不把用户内容塞进 Redis 或内存闭包。
3. **放弃的代价**：A 无法恢复且依赖请求进程；C 会增加 Redis 内存、序列化、隐私和大小限制风险。

### 决策 6：成功与失败后的保留

1. **面临的选择**：A. 所有任务结束即删；B. 成功后清理，失败/中断/outcome unknown 保留；C. 所有输入永久保留。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；它兼顾成功路径最小数据保留和 C5b 恢复所需输入。
3. **放弃的代价**：A 会再次丢失失败恢复材料；C 会造成无界磁盘与隐私保留风险。

### 决策 7：清理失败语义

1. **面临的选择**：A. 忽略清理失败；B. 回滚已完成索引；C. 保持业务结果并记录 `CLEANUP_PENDING`。
2. **选了哪个 + 为什么**：建议选 C，待用户确认；清理是后置副作用，不能伪装成功，也不应撤销已经可用的索引。
3. **放弃的代价**：A 会形成不可追踪数据保留；B 无法真实回滚 Milvus/SQL 等既有副作用并降低可用性。

### 决策 8：旧记录兼容

1. **面临的选择**：A. 启动时强制所有旧记录拥有输入；B. 按状态兼容，旧 COMPLETED 可无输入，需恢复但无输入则稳定 unavailable；C. 猜测 temp 目录并回填。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；它不伪造可恢复性，也不破坏已完成文档。
3. **放弃的代价**：A 会让升级被历史数据阻塞；C 无法证明文件归属与完整性，可能串错用户数据。

### 决策 9：生产 root 校验

1. **面临的选择**：A. 任意缺省到 system temp；B. production 显式 root + writable/non-temp probe；C. root 不可用时退回内存。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；durable 保证必须由部署显式承担，错误挂载应 fail fast。
3. **放弃的代价**：A 会把临时存储包装成持久化；C 在重启后必然丢失并违反已接受任务契约。

### 决策 10：C5a 与 C5b 边界

1. **面临的选择**：A. C5a 同时做扫描、租约与自动重放；B. C5a 只建立 durable input facts，C5b 再编排恢复；C. 只改路径不记录状态。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；先让恢复材料可信，再单独审查重放的幂等与 outcome unknown 风险。
3. **放弃的代价**：A 会把多个状态机和多个提交塞进一个 change；C 无法为 C5b 提供可判定输入事实。

### 决策 11：对象存储范围

1. **面临的选择**：A. 首版同步支持 filesystem + MinIO；B. 只定义 store 抽象并实现 filesystem；C. 直接依赖 C3 测试 MinIO。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；测试基础设施不等于业务 provider，首版应避免新增网络与凭据契约。
3. **放弃的代价**：A 会扩大验证矩阵；C 会错误复用 Milvus 内部依赖，缺少业务 bucket、权限和生命周期设计。

### 决策 12：刻意不做自动恢复

1. **面临的选择**：A. 检测到 AVAILABLE 就自动重跑；B. C5a 仅保留并暴露安全内部事实；C. 对所有 FAILED 提供客户端重试按钮。
2. **选了哪个 + 为什么**：建议选 B，待用户确认；C4d 已证明 mutation 可能 outcome unknown，未经 C5b 对账与租约设计不能安全重放。
3. **放弃的代价**：A/C 可能重复向量 mutation、重复 document count 或覆盖任务状态，制造新的不一致。
