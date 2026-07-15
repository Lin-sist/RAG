# Active Task

## Status

`IDLE`

## Active Change

无。

## Last Completed

- Change：`2026-07-15-integration-test-happy-path`
- 位置：`openspec/changes/archive/2026-07-15-integration-test-happy-path/`
- 结果：新增独立 C3 Maven/Failsafe 入口，使用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope 确定性 embedding，真实验证登录、上传、索引、retrieval 与删除主链路。
- 实现提交：`7f94538`。
- 验收：用户已确认通过；本 change 不修改生产契约，因此无 spec delta、未修改 baseline。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
