# F2剩余：正式登录页双翼动效接入

Report status：`CLIENT_VALIDATED_AWAITING_ACCEPTANCE`。2026-10-14用户明确“批准登录动效接入（推荐）”，按本包完成客户端实施与验证。F5/F1和登录主题修复已分别提交；沿用Agent本地提交授权。批准实施不等于验收、baseline接受或归档。

## 问题与结果

规划时正式/login使用旧LoginPixelArt网格轮换；用户已确认独立双翼曲面、拖尾与自然过渡。本轮已按批准方案将冻结效果接入AuthLayout，保留真实LoginView和唯一theme store，装饰失败静态降级。

## 范围

- 仅正式/login右侧品牌装饰及必要主题色变量/布局适配；基于2026-10-14已认可原型，保留标题、词标、真实用户名/密码与登录按钮。
- 细点→交叉→字符，6秒有效播放一段、18秒一轮，透明背景间隔与拖尾按已确认实现保留；无纹理/拖尾/暂停操作栏，不再优化曲面。
- 原生WebGL1、Vue组件生命周期、降低动态效果/隐藏/离屏停止、上下文丢失与静态SVG降级；全程不读凭据或业务数据。
- 合成登录成功/失败/等待、F1退出返回、主题跨路由和键盘表单的兼容验证；无真实auth/provider请求。

## 排除与能力分类

| 分类 | 本包事实 |
| --- | --- |
| confirmed | F2首批框架/主题/设置、F3/F4/F5客户端已接受；F1退出客户端已验证并提交；原型存在且独立38/38运行证据已记录 |
| confirmed（客户端） | 正式/login双翼组件/降级/生命周期与指定深浅三视口已验证，主题修复独立继承 |
| planned | 用户接受/baseline归档及其后F6客户端整体验收 |
| unknown | 物理GPU/手机/长时性能、真实认证撤销；新Vue WebGL软件浏览器证据不外推硬件 |
| out_of_scope | F4/F5真实闭环与清理、账号/注册/改密/权限、全量搜索、多轮、评测/MCP/工作台、后端/API/DTO/provider/C19/C20 |

## 授权与交付

用户本轮已授权Agent本地提交和继续工程；本包可提交规划。新Type C按工作流事前审查，动效实施在用户批准本包后进行；未获新接受/归档或push/PR/发布/部署授权。真实auth/backend/provider/ask/embedding/rerank/feedback/delete预算0，合成数据仅localhost内存，无出站或业务持久化。

完成标准为设计列出的正式build、行为/生命周期验证、六组深浅/三视口视觉与真实UI合成流程；分类和残余项分别报告。不将本包通过扩张为F6或完整真实Demo。

[design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md) · [总规划](../../../docs/roadmap/frontend-demo-backend-integration-plan.md)

## 实施收尾（2026-10-14）

102/102、含vue-tsc的正式build、完整41项浏览器复验及6正式/6参考/14补充文件已核验，真实业务调用0，详见[acceptance](acceptance.md)与[visual-evidence](visual-evidence.json)。沿用Agent本地提交授权，仍待用户接受，不自动接受baseline/归档或启动F6。
