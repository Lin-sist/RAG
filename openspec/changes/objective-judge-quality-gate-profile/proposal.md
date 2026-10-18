# C20：Objective草案与Judge辅助离线审阅

2026-10-18用户明确要求最后一轮C19如再次中断就保留结果并继续C20实现；该条件已满足（23有效/1网络错误/48未调用）。本切片已获直接实现授权，先记录设计再编码，不重复询问权限。

## 范围与验收

2026-10-18用户进一步要求“若不能归档则继续下一步实现”。整个change仍缺参考重复证据/数值审批/激活验收，故不归档；新增零外调参考证据编译器与阈值审阅包。此步只编译已有本地输入，不授权采集三轮真实调用、不批准数值或写ACTIVE。三轮候选必须完整且身份一致，缺测/策略不符/复用同一run时不从子集出分布。

- 原生C10 DRAFT profile，绑定v2全150样本、generation/citation运行身份；requiredChannels仅objective，judge全为辅助，target/tolerance均null，不自动批准或激活。
- 共享C10增加现有C9a字段的keyword/citation/no-answer映射，沿用原计数与分母，不改既有指标公式。optional judge通道不完整时不可从成功子集展示数值。
- 新review CLI只读本地：身份、顺序、通道完整性、逐规则观测/分母及错误策略分别展示；DRAFT始终NOT_EVALUABLE/exit4，不产生PASS。
- C19停放但不接受/归档，唯一活动指针转C20。C19补测、C20参考重复证据、数值批准和ACTIVE激活均为后续独立阶段。
- 验证：聚焦回归、全Python、C18接受报告hash绑定的本地原件烟测、输出脱敏/no-overwrite/链接/diff。C18 retry31照实显示，不因此放宽C20严格策略。

允许scripts/evaluate_quality_gate.py及测试、新review_objective_judge_profile.py及测试、C20配置/安全报告、当前change、ACTIVE_TASK/只追加AGENT_LOG、指南/路线图/技术债。禁止改历史C17/C18/C19 artifact、生产行为、依赖、数据集/公式/阈值及受保护配置。provider/backend调用0。本地提交沿用本任务授权，不push/PR/部署。

下一切片另允许scripts/compile_objective_gate_reference.py及对应测试；复用现有C20 review/共享计数，不修改它们来放宽策略。新报告为C20 intake，不回写C18接受报告或原始数据。
