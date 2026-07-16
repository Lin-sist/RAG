# Proposal: C5a Durable Index Inputs

## Why

C3 已证明健康态主链路，C4b/C4c/C4d 已锁定 LLM、Redis 与 Milvus 故障语义，但文档索引仍把上传内容写入系统临时文件，并把绝对 `Path` 捕获在当前进程的异步闭包中。任务结束时 finally 无条件删除该文件；如果进程在解析或写索引前中断，数据库只留下 PENDING/FAILED 文档和 Redis 任务状态，原始输入无法由新进程重新打开。

C5a 只解决“被系统接受的索引输入是否能跨进程存活、被完整校验并安全清理”。孤儿任务发现、抢占、自动重放和断点续跑留给 C5b，避免把存储边界和恢复状态机一次性耦合。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，用户上传成功后如果服务进程突然退出，上传文件可能随临时目录或 finally 清理一起消失，重启后即使能看到文档和任务记录也没有材料可重新索引；改之后，系统只有在输入已原子写入应用管理的持久位置并建立数据库关联后才接受任务，新进程可以用稳定 key 重新打开并校验同一份输入，且成功、删除与失败场景都有明确清理边界。

## Current Status

- `confirmed`：`DocumentIndexingServiceImpl` 使用 `Files.createTempFile`，异步 lambda 捕获 `Path`，`doIndex` finally 调用 `Files.deleteIfExists`。
- `confirmed`：`document.file_path` 已存在，但当前 submit/index/delete 生产路径均不写入或读取；`content_hash` 是解析后的内容去重事实，不能静默复用为上传字节校验。
- `confirmed`：当前没有应用级 MinIO/S3 文档存储 adapter；C3 的 MinIO 是 Milvus Testcontainers 依赖，不代表业务输入已进入对象存储。
- `confirmed`：C4c 将 Redis 任务状态锁定为当前事实源，C4d 禁止 outcome unknown mutation 自动重放。
- `partial`：数据库已有 document/async_task 表，但运行时任务管理器只使用 Redis；文档状态可表达 PENDING/PROCESSING/COMPLETED/FAILED，不能单独证明输入是否可读、完整或已清理。
- `planned`：建立稳定 storage key、原子 durable write、完整性元数据、按 key reopen、显式输入状态与安全清理。
- `out_of_scope`：孤儿扫描、lease/claim、自动重放、恢复调度、跨存储对账、对象存储 provider、公开 API/前端改造。
- `unknown`：durable root 的部署挂载方式、输入元数据采用现有字段扩展还是独立表、失败输入的最终保留期限，需在事前闸门确认。

## Scope

- 引入内部 `IndexInputStore` 契约，首版只提供应用管理的本地 durable filesystem 实现。
- 使用系统生成、不可猜测且与原文件名解耦的 opaque storage key；数据库不保存客户端绝对路径。
- 采用同一 durable root 内 staging write + atomic move；只有完整写入并可重新打开后，才允许建立已接受的文档/任务关联。
- 持久化输入大小、SHA-256 与状态，使读取时可区分 available、missing、corrupt、cleanup pending/cleaned。
- 异步索引按 storage key 打开新 stream，不捕获上传请求的 `MultipartFile`、原始 stream 或临时绝对路径。
- 健康任务完成后按明确状态清理；失败、中断和 outcome unknown 输入保留给 C5b；文档 canonical delete 时清理输入或显式进入 cleanup pending。
- 对旧 document 记录采用兼容读：没有 storage key 的历史已完成记录不报错；需要恢复但没有 durable input 的记录返回稳定 `INDEX_INPUT_UNAVAILABLE`，不伪造可恢复。
- 为路径穿越、符号链接逃逸、部分写入、校验失败、清理失败和重启 reopen 添加聚焦测试。

## Non-goals

- 不实现 C5b 的 orphan scanner、任务租约、抢占、自动 replay、resume endpoint 或恢复状态机。
- 不接入 S3/MinIO/OSS、共享文件系统、云 KMS、病毒扫描或内容审查服务。
- 不实现跨数据库、filesystem、Redis、Milvus 与 keyword index 的分布式事务。
- 不改变文档解析、`420/80` 分块、embedding、vector/keyword 写入、rerank、prompt、citation、no-answer 或评测指标。
- 不修改公开上传/任务 DTO shape，不重做前端。
- 不把客户端文件名、标题、正文、绝对路径或 storage root 写入普通日志、task error 或客户端错误。
- 本规格阶段不修改生产 Java、migration、POM、配置、测试或 C5a baseline。

## Spec Delta Decision

C5a 改变上传被接受的持久性保证、索引输入生命周期、失败结果和安全诊断，属于用户可观察的长期能力，必须提供 `rag-system` spec delta。用户批准规划不等于能力已实现；只有实现验收后才能接受进 baseline。

## External Calls And Authorization

| 调用类型 | 规划/实现验证调用量 | 数据出站 | 模型 | 限流风险 | 费用 | 授权状态 |
|---|---:|---|---|---|---|---|
| embedding | 0 真实调用 | 无；测试使用 stub 或停在解析前 | 无 | 无 | 0 | 不适用 |
| rerank | 0 | 无 | 无 | 无 | 0 | 不适用 |
| judge | 0 | 无 | 无 | 无 | 0 | 不适用 |
| ask/LLM | 0 | 无 | 无 | 无 | 0 | 不适用 |

首版建议不引入对象存储或新增网络服务。测试只使用临时测试根目录与合成文件，不上传业务数据。

## Acceptance Evidence

- 用户先审阅并批准 proposal、design、决策记录、tasks 与 spec delta，再允许修改生产代码、migration、配置或测试。
- 上传接受前输入已在 durable root 原子落盘、可 reopen，数据库只保存 opaque key 与完整性元数据。
- staging/写入/数据库关联/初始任务状态任一已知失败时不返回假 document/task acceptance，并清理可确定的未接受产物。
- 异步任务不依赖请求生命周期对象或系统临时绝对路径；模拟新实例可按 key 读取同一输入。
- missing/corrupt 输入产生稳定 `INDEX_INPUT_UNAVAILABLE` / `INDEX_INPUT_CORRUPT`，不进入解析、embedding 或 vector mutation。
- COMPLETED、FAILED/outcome unknown、document delete 与 cleanup failure 的保留/清理状态被测试锁定。
- 旧记录兼容、应用版本回滚安全忽略、root/path traversal/symlink 防护和敏感日志门禁通过。
- 聚焦 Java 测试、完整 Maven、Python、SensitiveLogs 与 `git diff --check` 通过；无前端改动时说明未运行前端 build。
- 真实 embedding/rerank/judge/ask/LLM 业务调用量均为 0。

## Risks

- 本地 durable filesystem 仍依赖部署正确挂载持久卷；未挂载时进程重启可读但容器重建可能丢失，必须用启动校验和部署说明诚实表达。
- filesystem 与数据库没有原子事务，崩溃窗口可能留下无主文件或无文件记录；C5a 应缩小窗口并表达状态，完整 reconciliation 留 C5b。
- 失败输入保留可提升恢复能力，但会增加磁盘与隐私风险；成功清理和容量拒绝必须在 C5a 锁定，失败输入的自动过期留 C5b。
- 新旧版本对 storage key/metadata 的理解不同；必须保证旧版本可忽略新增字段，且新版本不把旧记录伪装成可恢复。
- 若现在直接引入 MinIO/S3，会新增依赖、凭据、网络故障与部署面，扩大 C5a；因此首版建议只保留抽象边界。

## Commit Responsibility

`用户手动提交`。本轮仅启动规格草案；Agent 不暂存、不提交、不 push。
