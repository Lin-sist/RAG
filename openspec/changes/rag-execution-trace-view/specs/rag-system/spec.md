## ADDED Requirements

### Requirement: 单次问答的渐进式轨迹界面

系统 SHALL 在现有聊天上下文提供“对话 / 轨迹”。默认界面 SHALL 突出业务状态、服务端耗时、类型轨道与简短操作列表；调用/重试及诊断 SHALL 在选中操作后展示。四阶段 SHALL 保留作流程分类和筛选，未知实际阶段 MUST 可见并明确边界。

#### Scenario: 默认总览控制文字密度

- GIVEN 当前问答已收到合法轨迹
- WHEN 用户第一次打开轨迹
- THEN 总览为类型轨道，列表只含类型、名称、一句说明、状态与耗时，详情关闭
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

正式接入 MUST 保留现有输入区模板、scoped CSS 和组件身份，不移植设计稿 composer。轨迹局部样式 MUST NOT 修改 .input-wrapper/.input-container/.pill-input 或其按钮的现有样式。

#### Scenario: 正式输入框保持原样

- GIVEN 当前正式 ChatPanel 输入区已有可用样式和草稿
- WHEN 接入轨迹并切换对话/轨迹或操作详情
- THEN 输入区模板与原 scoped CSS 指纹保持一致，原 textarea 节点及草稿保留
- AND 发送、停止接收、知识库选择及窄屏输入布局仍沿原规则工作

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

### Requirement: 三色操作类型与独立执行状态

界面 SHALL 至少以三种可辨识颜色分别表示工具、助手、上下文，在时间轨道、操作标签和详情保持一致。类型 MUST 从真实 stage 的有版本映射得到；工具仅表示实际 RAG 组件调用，不得据此宣称已具备 Agent 自主编排。失败/降级/取消 SHALL 使用独立图标与文字，MUST NOT 以状态色抹掉类型身份。

#### Scenario: 三类操作保持同一颜色身份

- GIVEN 同一 run 实际包含工具、LLM 请求和证据组织记录
- WHEN 用户查看轨道、列表和对应详情
- THEN 三类至少使用琥珀、紫、青绿三种语义颜色及类型文字
- AND 选中时通过描边/侧线强调，不把所有类型统一改色

#### Scenario: 调用说明来自真实模板

- GIVEN 一条实际工具操作仅有受限 stage 和计数
- WHEN 显示该操作的一句说明
- THEN 使用对应固定模板和实际可用计数，缺失值明确未提供
- AND 不通过新增模型调用编造执行理由或输出长参数 JSON

#### Scenario: 工具失败但问答降级成功

- GIVEN 向量组件失败而关键词路线维持问答最终 ANSWER
- WHEN 显示该工具与整体摘要
- THEN 工具保留类型色并叠加失败/降级标记，摘要保留业务成功
- AND 操作数量不冒充模型调用次数

### Requirement: 每个操作有分类详情与双向选择关联

每个真实操作 SHALL 可从片段或列表选择，并同步选中另一处。工具详情 SHALL 按实际字段提供概览/参数/结果，助手 SHALL 提供概览/回答/计时，上下文 SHALL 提供概览/来源；没有可用字段时 SHALL 明确未提供。详情 MUST NOT 以原始 prompt、系统指令、模型内部思维或任意 JSON 作为默认内容。

#### Scenario: 点选短操作

- GIVEN 总览存在耗时很短的真实工具片段
- WHEN 用户点选扩大命中区或对应列表项
- THEN 两处关联到同一 run/stageId 并打开该操作详情
- AND 扩大命中区不改变真实时间比例或捏造持续时间

#### Scenario: 助手详情复用当前回答

- GIVEN 本次已收到回答文本和有效业务终态
- WHEN 用户选择助手操作并打开回答/计时
- THEN 正文复用同一 run 的现有授权展示，计时仅使用实际数据
- AND 缺少首 Token/usage 时显示未提供，不披露内部思维

#### Scenario: 上下文来源尚未授权交付

- GIVEN 流式问答尚未收到允许展示来源的有效 ANSWER terminal
- WHEN 用户打开上下文来源页签
- THEN 只显示已有计数与来源尚未提供的提示
- AND 不从 trace 泄露片段、不额外回查知识库；完成后仅复用同一 run 的已授权来源

### Requirement: 可拖动时间选区及可撤销本页筛选

时间总览 SHALL 支持正反向拖选、端点调整和内部平移，并以真实区间相交规则筛选本页操作；零耗时记录 SHALL 以时刻落在闭区间判断。选区 SHALL 显示起止、跨度及匹配数，与类型/阶段筛选取交集。所有操作 MUST 保留原服务端时间、业务状态和调用次数，MUST NOT 因拖动触发 ask/provider 或新增读取。

选区边界 SHALL 有键盘 slider 与触屏替代，最小区间为 1 ms，拖动取消 SHALL 恢复原区间。选区激活时展示域 SHALL 冻结，新事件不能推动选区；清除后才恢复最新全长。

#### Scenario: 正反向拖选与恢复全部时间

- GIVEN 当前 run 包含多个时间区间
- WHEN 用户向任一方向拖选并随后选择全部时间
- THEN 正反拖得到相同范围/匹配项，清除只恢复时间范围并保留类型/阶段筛选
- AND 不重发请求或改写耗时

#### Scenario: 端点与平移不越界

- GIVEN 已有合法选区
- WHEN 用户拖动端点、平移或用键盘移动边界
- THEN 范围限制于展示域、宽度至少 1 ms，点记录可按时间命中
- AND pointercancel 恢复原范围，触屏纵向滚动仍可用

#### Scenario: 选区与当前详情不再相交

- GIVEN 已选操作的详情打开
- WHEN 用户主动调整选区使该操作不再匹配
- THEN 关闭范围外详情并简短播报，详情内焦点才需返回总览
- AND 新事件自身不抢选择或滚动位置

#### Scenario: 运行中选区不漂移

- GIVEN 用户正在检查一个已选时间范围
- WHEN 新阶段事件到达并扩大完整执行时长
- THEN 保持该展示域和选区不动，并提示有新操作
- AND 清除范围后才回到最新全长，业务流始终继续
