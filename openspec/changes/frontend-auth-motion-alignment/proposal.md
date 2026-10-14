# F2剩余：正式登录页双翼动效接入

Report status：`PROPOSAL_READY_AWAITING_APPROVAL`。2026-10-14用户要求提交F5/F1并继续剩余Demo接入。F5/F1已分别提交；已有契约内登录主题修复作为独立Type B先完成。此包为下一Type C事前审查，不代表双翼动效已迁入或整体Demo已验收。

## 问题与结果

正式/login仍使用旧LoginPixelArt网格轮换；用户已在独立prototype中确认双翼曲面、数据汇聚拖尾及细点/交叉/字符自然淡入淡出。拟把该冻结效果组件化接入AuthLayout，保留现有真实LoginView表单和统一theme store；装饰层失败不得阻断登录。

## 范围

- 仅正式/login右侧品牌装饰及必要主题色变量/布局适配；基于2026-10-14已认可原型，保留标题、词标、真实用户名/密码与登录按钮。
- 细点→交叉→字符，6秒有效播放一段、18秒一轮，透明背景间隔与拖尾按已确认实现保留；无纹理/拖尾/暂停操作栏，不再优化曲面。
- 原生WebGL1、Vue组件生命周期、降低动态效果/隐藏/离屏停止、上下文丢失与静态SVG降级；全程不读凭据或业务数据。
- 合成登录成功/失败/等待、F1退出返回、主题跨路由和键盘表单的兼容验证；无真实auth/provider请求。

## 排除与能力分类

| 分类 | 本包事实 |
| --- | --- |
| confirmed | F2首批框架/主题/设置、F3/F4/F5客户端已接受；F1退出客户端已验证并提交；原型存在且独立38/38运行证据已记录 |
| partial | 登录视觉已用旧Demo双列版，但仍是旧网格装饰；登录统一主题小修复独立完成，不证明动效接入 |
| planned | 双翼动效模块化、正式/login降级/生命周期与视觉矩阵、其后F6客户端整体验收 |
| unknown | 正式Vue中的WebGL/上下文恢复/性能及真实认证撤销；原型SwiftShader结果不外推新集成或物理设备 |
| out_of_scope | F4/F5真实闭环与清理、账号/注册/改密/权限、全量搜索、多轮、评测/MCP/工作台、后端/API/DTO/provider/C19/C20 |

## 授权与交付

用户本轮已授权Agent本地提交和继续工程；本包可提交规划。新Type C按工作流事前审查，动效实施在用户批准本包后进行；未获新接受/归档或push/PR/发布/部署授权。真实auth/backend/provider/ask/embedding/rerank/feedback/delete预算0，合成数据仅localhost内存，无出站或业务持久化。

完成标准为设计列出的正式build、行为/生命周期验证、六组深浅/三视口视觉与真实UI合成流程；分类和残余项分别报告。不将本包通过扩张为F6或完整真实Demo。

[design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md) · [总规划](../../../docs/roadmap/frontend-demo-backend-integration-plan.md)
