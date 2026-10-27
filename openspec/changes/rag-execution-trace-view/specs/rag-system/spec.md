## ADDED Requirements

### Requirement: 单次问答的渐进式轨迹界面

系统 SHALL 在现有聊天上下文提供“对话 / 轨迹”。默认界面 SHALL 只突出业务状态、服务端耗时和实际阶段分布；子阶段、调用/重试及诊断 SHALL 在选中阶段后展示。阶段 SHALL 投影为准备、检索、生成、校验四类；未知实际阶段 MUST 可见并明确分类边界。

#### Scenario: 默认总览控制文字密度

- GIVEN 当前问答已收到合法轨迹
- WHEN 用户第一次打开轨迹
- THEN 一级行只有名称、状态、时间条和耗时，详情关闭
- AND 不自动铺出问题/回答全文、参数或原始 JSON

#### Scenario: 手动选择不被更新打断

- GIVEN 当前 run 正接收更新
- WHEN 用户选择一个阶段
- THEN 展示实际子阶段与白名单事实
- AND 后续更新不抢选择、键盘焦点或滚动位置

### Requirement: 轨迹必须反映实际执行与时间

阶段 SHALL 来自真实 lifecycle，时间 SHALL 使用同一起点的服务端单调偏移。父子重叠 MUST 保留，不得简单累加为串行耗时。未执行 MUST NOT 生成假成功条；缺少真实计数/usage 时 MUST NOT 填造零值。

#### Scenario: 缓存命中

- GIVEN 缓存实际命中且下游未执行
- WHEN 显示轨迹
- THEN 只画实际准备/缓存记录并解释命中
- AND 检索、生成和校验不出现成功条

#### Scenario: 父子时间重叠

- GIVEN retrieval 与子记录存在真实重叠
- WHEN 显示一级耗时和展开后的子项
- THEN 一级使用自身区间，子项按原偏移布局
- AND 不把子耗时之和当作总耗时

### Requirement: 应用轨迹独立于遥测导出采样

系统 SHALL 使用可关闭且有界的 observer 投影实际阶段，与 OTel/exporter 开关和采样解耦。采集内部失败 MUST NOT 改变问答、重试/降级、引用或历史语义，也 MUST NOT 新增模型调用。

#### Scenario: OTel 关闭

- GIVEN 应用轨迹开启而 OTel tracing/exporter 关闭
- WHEN 问答实际执行
- THEN 仍取得受限阶段投影
- AND 不启动 Collector 或增加 provider 请求

#### Scenario: Observer 内部失败

- GIVEN 业务可正常执行而 observer 异常
- WHEN 交付问答
- THEN 回答沿用原业务和历史规则
- AND 轨迹明确缺失或 PARTIAL，不生成完整成功过程

### Requirement: 显式轨迹协商与原流协议兼容

系统 SHALL 仅对 X-RAG-Trace-Contract: trace-v1 且能力可用的请求返回新增轨迹。同步 SHALL 用 metadata.executionTrace；流式 SHALL 同时保留 structured-v1 协商，只有新模式增加 trace 命名事件。原 text/terminal 字段与业务语义 MUST 保持兼容。最终快照 SHALL 在 terminal 前，terminal 后 MUST NOT 发新事件。

#### Scenario: 原客户端不请求轨迹

- GIVEN 客户端仅请求 structured-v1
- WHEN 服务端执行问答
- THEN 响应仍只有原协议允许的 text/terminal
- AND 原 parser 无需放宽未知事件检查

#### Scenario: 坏轨迹不污染有效业务终态

- GIVEN 已协商 trace-v1 且 text/terminal 有效
- WHEN trace 格式不合法或超额
- THEN 轨迹降为 PARTIAL，并停止接受超额细节
- AND 有效问答终态不改判失败

#### Scenario: 业务协议损坏仍严格失败

- GIVEN 已协商 trace-v1
- WHEN 重复 terminal、terminal 后 text 或 terminal 格式破损
- THEN 沿用原业务协议失败语义
- AND 不因轨迹显示完成而合成 ANSWER

### Requirement: 业务结果、采集完整性与连接状态分开

系统 SHALL 分别表达业务 final state、采集 COMPLETE/PARTIAL 和客户端连接状态。COMPLETE MUST NOT 等于业务成功或质量通过。断开 MUST NOT 自动证明服务端取消；正常无答案/不支持 MUST NOT 被当作系统故障。

#### Scenario: 降级后成功

- GIVEN 业务最终 ANSWER，实际发生一次降级
- WHEN 显示摘要
- THEN 保留“已完成”并提供单个降级数量入口
- AND 展开后显示安全原因与实际替代路线

#### Scenario: 客户端停止或断网

- GIVEN 收到部分阶段但没有有效业务 terminal
- WHEN 用户停止或连接断开
- THEN 说明服务端最终状态未确认
- AND 不擅自把运行阶段改为 CANCELLED 或成功

#### Scenario: 未提供轨迹

- GIVEN 问答响应可用但没有可用轨迹
- WHEN 用户打开轨迹
- THEN 显示“本次未提供轨迹”或“轨迹不完整”
- AND 回答/来源按原规则展示，不播放预设成功步骤

### Requirement: 轨迹更新有序、有界且仅在当前页面保留

轨迹 SHALL 用服务器生成的 runId、stageId 和单 run 单调 seq 幂等更新。旧更新 MUST NOT 覆盖新状态；seq 缺口 SHALL 标 PARTIAL，只有合法自足的最终完整快照才 MAY 恢复 COMPLETE。每 run SHALL 限制为 128 阶段、256 条增量、每条 2 KiB、最终快照 64 KiB、总净荷 576 KiB，包含开始/最终记录预留额度。

浏览器 SHALL 只保留最近 20 条已结束记录及当前运行记录，不承诺刷新或历史恢复。退出、身份变化、组件释放 MUST 清理数据及迟到回调；淘汰 MUST NOT 触发 ask 自动重放。

#### Scenario: 乱序和缺口

- GIVEN 同一 run 新状态已生效
- WHEN 收到旧 seq 或发现缺口
- THEN 旧记录不覆盖新状态，缺口使采集 PARTIAL
- AND 仅通过校验的最终自足快照可恢复完整显示

#### Scenario: 超限与淘汰

- GIVEN 阶段/事件/净荷或页面保留数量达到边界
- WHEN 又有细节到达
- THEN 有界截断/淘汰并明确不完整或已释放
- AND 不阻塞业务、不写浏览器持久存储、不重发请求

### Requirement: 轨迹只包含同一授权响应的安全事实

轨迹 SHALL 沿通过身份/KB 权限校验的本次响应传输，runId/traceId MUST NOT 作为独立读取凭证。净荷 SHALL 只含固定枚举、合法关联 ID、非负时间和受限计数。问题、prompt、answer、context、snippet、路径/文件标题、用户/租户身份、凭据、原始异常和 provider body MUST NOT 进入 trace 净荷或普通遥测。

#### Scenario: 私密哨兵不进入投影

- GIVEN 输入、引用、provider 错误和身份字段含私密哨兵
- WHEN 生成快照、trace 事件及可复制技术信息
- THEN 投影不含哨兵或原始 JSON 转储
- AND 问题/引用正文只从原授权数据流显示

#### Scenario: 身份变化后的迟到更新

- GIVEN 账号退出或切换身份
- WHEN 原 run 更新随后到达
- THEN 丢弃更新并保持旧记录/选中详情已清理
- AND 不出现在新身份视图

### Requirement: 轨迹响应式、可访问且不干扰问答

轨迹 SHALL 沿用深浅主题，提供可读列表、键盘选择和非颜色状态信息。详情 SHALL 按可用内容宽度使用右侧面板、行内展开或全宽 sheet。tabs 切换 MUST NOT 重发/取消 ask、清空输入或改变终态。减少动态效果时 SHALL 静态显示，不逐 token 播报。

#### Scenario: 窄屏与键盘

- GIVEN 390×844 视口或仅键盘操作
- WHEN 用户切换轨迹、打开并关闭详情
- THEN 无页面横向溢出，名称/状态/耗时可读，焦点回到原按钮
- AND 信息不依赖 hover、颜色或动画

#### Scenario: 切换不扰动运行

- GIVEN ask 运行中且输入框有未发送内容
- WHEN 来回切换对话/轨迹
- THEN 请求继续、输入保留
- AND 不新增模型调用，不取消流或改变历史规则
