# Tasks: C1 JWT Secret Production Guard

## Phase 0：启动与规格草案（当前切片）

- [x] 核对 `AGENTS.md`、`.ai/ACTIVE_TASK.md`、冻结蓝图、baseline spec 与当前 JWT 代码/配置/测试。
- [x] 确认 B0、B1、v4 关闭裁决与本地质量门禁已完成，C1 顺序前置满足。
- [x] 将能力状态分类为 `confirmed / partial / planned / out_of_scope / unknown`。
- [x] 创建 proposal，包含“改前坏事 → 改后不同”的用户故事。
- [x] 创建 design、tasks 与 `rag-system` spec delta 草案。
- [x] 声明 embedding/rerank/judge/ask 调用量均为 0，无数据出站、模型、限流与费用风险。
- [x] 明确提交责任为“用户手动提交”。
- [x] 将 `.ai/ACTIVE_TASK.md` 置为 `ACTIVE` 并指向本 change。
- [ ] 用户审阅并明确批准 proposal、design 与 spec delta。

## Phase 1：聚焦实现（须在用户批准后开始）

- [ ] 确认 prod 精确拒绝集合及稳定错误类别。
- [ ] 在 `rag-auth` 新增可纯函数测试的 JWT production secret guard/policy。
- [ ] 通过显式依赖保证 guard 在 JJWT key 构造前执行。
- [ ] 保持非 prod fallback 与现有 UTF-8/JJWT key-strength 职责不变。
- [ ] 如需更新生产配置说明，只修改 tracked 文档；不修改 `.env.local` 或任何真实 secret。

## Phase 2：测试与验证

- [ ] 添加 prod known default、blank、首尾空白、未解析占位符和合法 secret 单元测试。
- [ ] 添加错误消息不泄露 secret 的断言。
- [ ] 添加非 prod 兼容与 JJWT UTF-8 byte-length 职责回归测试。
- [ ] 添加最小 Spring context 的 prod fail-fast / valid-secret 测试，不启动业务外部依赖。
- [ ] 运行 `mvn -q -pl rag-auth -am test`。
- [ ] 运行 `mvn -q test`。
- [ ] 运行 Markdown 相对链接/旧路径扫描与 `git diff --check`。
- [ ] 确认未发生 embedding、rerank、judge、ask 或其他 provider 业务调用。

## Phase 3：收口（实现完成后）

- [ ] 更新本 tasks 的真实完成状态与跳过原因。
- [ ] 将修改文件、验证结果、剩余风险和 `Commit: pending` 追加到 `.ai/AGENT_LOG.md`。
- [ ] 请用户完成真实 review；未经用户确认不归档 change。
- [ ] 用户确认完成后将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE`，再按治理流程归档。
- [ ] 提供中文 Conventional Commit 建议；由用户手动暂存和提交。

## Commit Responsibility

`用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、发布或部署。
