# Tasks: C5a Durable Index Inputs

## 0. Approval Gate

- [ ] 用户审阅并批准 proposal、design、全部决策记录与 `rag-system` spec delta。
- [ ] 用户确认首版只实现应用管理的本地 durable filesystem，MinIO/S3 为 out_of_scope。
- [ ] 用户确认数据库保存 opaque storage key、byte size、SHA-256 与 input state，不保存绝对路径且不复用 `content_hash`。
- [ ] 用户确认 root 内 staging + atomic move；平台不支持原子发布时 fail closed。
- [ ] 用户确认任务只捕获稳定 ID/key，执行时 reopen + verify，不把文件 bytes 放入 Redis payload。
- [ ] 用户确认成功后清理，FAILED/中断/outcome unknown 保留；清理失败进入 `CLEANUP_PENDING`。
- [ ] 用户确认旧 COMPLETED 行允许无 input，旧 PENDING/FAILED 无 input 返回稳定 unavailable，不猜测 temp 文件。
- [ ] 用户确认 production durable root 必须显式、可写且非 system temp，不可用时 fail fast。
- [ ] 用户确认 C5a 不做 orphan scanner、lease/claim、自动 replay、resume API 或跨存储对账。
- [ ] 用户确认提交责任维持 `用户手动提交`，或另行明确授权 `Agent 提交`。

> 当前仅为规划草案。approval gate 全部确认前，不得修改生产 Java、migration、配置、POM 或测试。

## 1. Inventory And RED Tests

- [ ] 固化 upload → temp → async closure → finally delete 的当前 consumer/lifecycle inventory。
- [ ] 固化 `document.file_path/content_hash/status`、Redis task status 与 document delete 的当前事实边界。
- [ ] 为 atomic put、reopen、size/hash verify、idempotent delete 添加 store RED 测试。
- [ ] 为 traversal、absolute key、symlink escape、partial staging 和 raw path marker 添加安全 RED 测试。
- [ ] 为 acceptance ordering 与 initial Redis PENDING write failure 添加 service RED 测试。
- [ ] 为 task missing/corrupt/read failure 添加 RED 测试，断言 parser/embedding/vector 调用次数为 0。
- [ ] 为 COMPLETED、FAILED/outcome unknown、document delete 与 cleanup failure 添加 lifecycle RED 测试。
- [ ] 为旧 row compatibility 与应用重启 reopen 添加 migration/integration RED 测试。

## 2. Storage Boundary

- [ ] 新增 `IndexInputStore`、稳定结果类型与安全异常，不泄露 root/key/文件名/raw message。
- [ ] 实现 configured-root filesystem store：opaque key、root confinement、regular-file 与 symlink 防护。
- [ ] 实现 root 内 staging、flush/close、atomic publish；失败不暴露部分最终文件。
- [ ] 写入时计算 byte size 与 SHA-256；open 时按持久化事实校验。
- [ ] 实现 delete 的 deleted/already-missing/failed 结果，禁止把失败伪装成 cleaned。
- [ ] production root 缺失、不可写、位于 system temp 或 probe 失败时 fail fast。

## 3. Persistence And Acceptance

- [ ] 新增前向 migration，兼容旧行，保存 storage key、input size、input SHA-256 与 input state。
- [ ] 保持解析后 `content_hash` 及其 KB 内去重语义不变。
- [ ] 上传按 durable put → document association → initial task PENDING → response 排序。
- [ ] acceptance 前已知失败不返回假 document/task；清理可确定的 staging/unowned object。
- [ ] task 闭包只捕获 documentId/storageKey 等稳定标识，执行时 reopen 新 stream。
- [ ] missing/corrupt/read failure 使用稳定 code，并在 parser/embedding/vector 前终止。

## 4. Input Lifecycle

- [ ] 健康 COMPLETED 后执行 `AVAILABLE -> CLEANUP_PENDING -> CLEANED`。
- [ ] cleanup failure 保持 COMPLETED 业务结果但留下 `CLEANUP_PENDING`，不记录敏感路径。
- [ ] FAILED、进程中断和 `VECTOR_OPERATION_OUTCOME_UNKNOWN` 保持 AVAILABLE，供 C5b 决策。
- [ ] canonical document delete 清理 durable input；失败不伪造输入已删除。
- [ ] 旧 COMPLETED 无 input 正常兼容；旧 PENDING/FAILED 无 input 返回 `INDEX_INPUT_UNAVAILABLE`。
- [ ] 不新增 scanner、scheduler、lease、claim、replay、resume endpoint 或自动恢复。

## 5. Verification And Closeout

- [ ] 运行 C5a store/service/migration 聚焦测试。
- [ ] 运行 restart-style integration，证明新实例可按同 root/key reopen 相同 bytes。
- [ ] 运行现有 C3 happy-path 与 C4c/C4d 相关回归。
- [ ] 运行 `mvn -q test`。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 运行 SensitiveLogs 门禁与 `git diff --check`。
- [ ] 扫描公开 DTO、前端、解析/检索/评测、对象存储依赖和受保护路径无越界改动。
- [ ] 更新 tasks、`.ai/ACTIVE_TASK.md` 与追加式 `.ai/AGENT_LOG.md`。
- [ ] 用户完成实现验收后，才接受 spec delta、恢复 `IDLE` 并归档 change。

## Commit Responsibility

当前为 `用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布。
