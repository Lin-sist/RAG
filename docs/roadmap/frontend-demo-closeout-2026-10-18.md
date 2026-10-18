# 前端 Demo 接入工程收口索引

2026-10-18。Report status：`ACCEPTED_WITH_TRACKED_DEBT`。核查起点：`e2b077b`，main 工作区干净。用户明确确认接入工程完成，授权检查并归档，再进入 C19。本记录是整体阶段收口，不新增产品契约，也不把部分真实使用证据提升为完整业务验收。

## 已归档切片

| 范围 | 正式归档/验收入口 | 本次核查 |
| --- | --- | --- |
| R4 纯文本流历史切片 | [R4](../../openspec/changes/archive/2026-09-29-frontend-text-stream-r4/acceptance.md) | 已归档、任务完成；当前正式聊天页采用 R5。R4 原 requirement 当前 baseline 不逐字存在，保留历史文件，不重新追加旧契约。 |
| R5 结构化流 | [R5](../../openspec/changes/archive/2026-09-29-frontend-structured-stream-r5/acceptance.md) | 1 requirement / 3 scenarios 与当前 baseline 逐字一致。 |
| F2 首批导航、品牌、主题、首页、设置 | [首批](../../openspec/changes/archive/2026-10-01-frontend-demo-experience-alignment/acceptance.md) | 5 requirements / 14 scenarios 与 baseline 逐字一致。 |
| F3 聊天正文与来源 | [F3](../../openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/acceptance.md) | 3 requirements / 6 scenarios 与 baseline 逐字一致。 |
| F4 知识库与上传客户端 | [F4](../../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/acceptance.md) | 4 requirements / 12 scenarios 与 baseline 逐字一致；D1–D3 为明确移交的债务。 |
| F5 历史与反馈客户端 | [F5](../../openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/acceptance.md) | 4 requirements / 11 scenarios 与 baseline 逐字一致；R1–R2 为真实验证移交项。 |
| F2 剩余登录动效 | [登录动效](../../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/acceptance.md) | 2 requirements / 6 scenarios 与 baseline 逐字一致。 |
| F1 退出、F6 客户端总验收 | [F1](frontend-f1-logout-acceptance.md)、[F6](frontend-f6-client-acceptance.md) | Type B，无独立 Type C 可搬迁；本次接受并关闭该阶段，证据原位保留。 |

两个旧非 archive 目录 `frontend-demo-experience-alignment`、`frontend-chat-source-alignment` 仅剩空的 specs/rag-system 文件夹，无待归档文件；本次不删除目录、不重复归档或复制 baseline。

## 证据及分流

- confirmed：本次重新核对上述六个现行 delta，共 19 requirements / 52 scenarios；F6 清单全部 77 张图片 hash（57 正式、20 排除）与三个正式源码指纹均一致。复用原正式 build、聚焦测试、视觉与键盘结果，不称本轮重新执行浏览器矩阵。
- confirmed：用户普通路径手工确认及 10:36 流式成功单例；详见[用户验收](frontend-user-manual-acceptance-2026-10-18.md)、[脱敏诊断](frontend-stream-failure-diagnosis-2026-10-18.md)。不外推质量或长期稳定性。
- partial / unknown：[技术债](technical-debt.md#f6-客户端整体验收后的证据分流2026-10-16)继续保留 F4-LIVE/DROP/CLEANUP、F5-LIVE/CLEANUP、F6-DEVICE/LIVE/STREAM/SIZE；前次上游失败原因、真实权限/撤销、完整持久化、删除清理不关闭。[L1/L2 执行包](frontend-live-integration-execution.md)未由 Agent 执行。
- planned：下一独立阶段为 [C19](../../openspec/changes/judge-live-calibration-evidence/proposal.md)；C20 门禁仍延后。
- out_of_scope：全量搜索、多轮会话、账号/工作台、生产化与质量达标声明。

本次不搬动已有 archive、不修改原始证据 JSON/图片或 accepted specs。OpenSpec CLI 不可用，文件级比对不是官方 validate。Agent 新增真实业务/provider 调用与数据出站均为 0。本阶段接受不授予 C19 本地提交或真实 judge 调用权限。
