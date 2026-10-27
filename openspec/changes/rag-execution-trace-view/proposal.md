# 单次问答轨迹：把执行过程变成安静、可检查的时间线

## 为什么改

正式聊天页能展示回答、来源和流式终态，但用户仍不知道时间花在哪里、实际执行了哪些步骤，以及降级时发生了什么。旧静态 Demo 的检索步骤和可观测页面使用演示数据，不能代表真实问答轨迹。

用户希望借鉴所附 DeepSeek Harness 截图的时间总览和轨迹入口，同时减少密集文字。本设计只参考用户提供的截图，没有研究或声称复现该产品的内部协议。

## 目标与范围

- 在现有 ChatPanel 内提供“对话 / 轨迹”切换，保留侧栏、主题、输入框和当前回答状态。
- 以“准备、检索、生成、校验”四类分组压缩实际阶段，默认只显示名称、状态、时间条和耗时；选中后再查看子阶段与安全事实。
- 规划同步问答最终快照，以及流式问答经显式协商传递的阶段事件。首版覆盖当前页面内的单次问答，不新增轨迹查询服务或数据库。
- 轨迹来自真实生命周期。缓存命中、无答案、降级、失败和连接中断均有明确呈现，缺失数据不补造。
- 本轮仅交付 OpenSpec 设计和静态视觉稿；业务代码实施须经设计确认。

## 现状与能力分类

| 分类 | 事实 |
| --- | --- |
| confirmed | /chat 与 /chat/:id 挂载 ChatPanel；已有 text/terminal 结构化流、引用展示和深浅主题。后端已有实际阶段的 OTel 生命周期入口。 |
| partial | prototype/chatgpt-ui-demo 有模拟检索管道和可观测配置页，尚未形成真实产品轨迹。OTel 本身受开关、采样和导出策略影响。 |
| planned | 本 change 的时间线、详情交互、独立于 OTel 导出采样的阶段投影，以及可选 trace-v1 响应扩展。 |
| unknown | 实际浏览器表现、真实事件完整性、开销和设备表现，待实施后验证。 |
| out_of_scope | 上传索引轨迹、跨请求 lineage 浏览、刷新或历史页恢复、轨迹搜索后台、数据库/Collector 新部署、多轮会话、模型内部思维文本和生产容量声明。 |

现状依据：[路由](../../../rag-frontend/src/router/index.ts)、[ChatPanel](../../../rag-frontend/src/components/chat/ChatPanel.vue)、[流解析器](../../../rag-frontend/src/composables/sseParser.ts)、[阶段埋点](../../../rag-common/src/main/java/com/enterprise/rag/common/trace/GenAiTelemetry.java)、[问答服务](../../../rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java)。

## 交付与验收

- [设计](design.md)明确默认视图、交互、协议、失败语义、预算和真实取舍。
- [视觉稿](visuals/README.md)提供浅色总览、深色选中详情、窄屏三张静态稿；数值全部标注为模拟数据。
- [任务](tasks.md)按设计、视觉、真实数据、前端接线与验收拆分，实施项保持未完成。
- [规格增量](specs/rag-system/spec.md)定义可观察行为，不提前修改 accepted baseline。
- 实施后的验收分别提供聚焦测试、含 vue-tsc 正式 build、浏览器交互/可访问检查，以及经独立授权的真实业务证据。静态稿和合成测试不能代替真实链路验收。

## 授权、成本与活动指针

2026-10-26 用户明确确认登录页已经验收并提交，并要求按 OpenSpec 规划本界面。开工工作区干净，Git HEAD 为 50d0036；登录页 change 的历史接受/归档条目尚未同步，本轮在活动索引保留这一事项，不搬迁旧 change 或修改 baseline。

本轮授权覆盖规划文件、原创静态设计稿、活动指针及必要日志，不包含业务实现、暂存/提交、push、PR、部署或新依赖。真实 provider、ask、embedding、rerank、judge 调用预算为 0，无业务数据出站或新增费用。后续本地合成验收不授予真实模型调用权限。

## 风险

阶段分组可能隐藏细节，因此详情保留真实子阶段与时间。未协商就向现有流塞入新事件会触发协议错误，因此保留原 structured-v1 行为。采集失败只影响轨迹可用性，不改变问答结果或历史规则，详见设计 D2–D5。
