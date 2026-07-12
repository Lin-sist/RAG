# OpenSpec Changes

重大变更使用 kebab-case change id：

```text
openspec/changes/<change-id>/
├─ proposal.md
├─ design.md
├─ tasks.md
└─ specs/
   └─ <capability>/spec.md
```

## 生命周期

1. 创建 change，并写 proposal 的问题、范围、非目标和验收证据。
2. 在 design 中记录契约、数据流、替代方案、回滚和风险。
3. tasks 拆成小步可验证切片。
4. spec delta 使用 `ADDED / MODIFIED / REMOVED Requirements` 表达规范变化。
5. `.ai/ACTIVE_TASK.md` 指向该 change 后才开始重大实现。
6. 完成、验证并经用户确认后，移动到 `openspec/changes/archive/YYYY-MM-DD-<change-id>/`。

普通只读任务或已有契约内的小修不需要创建空 change；按 `AGENTS.md` 记录必要证据即可。
