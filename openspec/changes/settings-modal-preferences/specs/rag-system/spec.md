## ADDED Requirements

### Requirement: Expanded Accessible Settings Overlay
前端 SHALL 在同一 dialog 提供五类栏目、旧入口映射、深浅窄屏、背景 inert、键盘循环及焦点归还。重置 SHALL 使用行内确认。

#### Scenario: 入口与窄屏
- GIVEN 从设置/资料/归属入口打开
- WHEN 使用桌面或 320 CSS px 窄屏
- THEN 对应栏目、关闭、末行均可达，无页面横向溢出

#### Scenario: 重置与 Escape
- GIVEN 正在行内重置确认
- WHEN 依次按 Escape 两次
- THEN 先取消并回重置按钮，再关闭并归还背景焦点

### Requirement: Local Display And Interaction Preferences
系统 SHALL 保持 theme 兼容，并用白名单 schema 保存字号/动效/发送键/来源，默认 standard/system/enter/collapsed。无效字段独立回默认，存储失败内存仍生效且不误报保存。重置只复位两个自有 key。

#### Scenario: 损坏与存储失败
- GIVEN 坏 JSON、无效字段或浏览器读写抛错
- WHEN 初始化或选择偏好
- THEN 安全默认/有效字段生效，写失败明确说明，不输出原始存储

#### Scenario: 刷新与精确重置
- GIVEN 非默认偏好与无关存储哨兵
- WHEN 刷新或确认重置
- THEN 刷新恢复选择，重置恢复默认且凭据/草稿/业务/无关存储保持，部分失败如实提示

### Requirement: Reading And Reduced Motion Presentation
字号 SHALL 只改变回答正文和相对标题/表格，代码/输入不变。系统或本地任一减少时 SHALL 停止连续粒子与非必要位移，监听有唯一归属并清理。

#### Scenario: 字号与有效动效
- GIVEN 大字号或系统/本地减少
- WHEN 跨路由、挂载粒子或切换偏好
- THEN 字号限于阅读区，系统减少优先，无重复监听，静态回退/暂停保持

### Requirement: Safe Send Keys And Initial Source Expansion
发送 SHALL 支持 Enter 与 modifier-enter，Shift+Enter 换行；组合输入/结束边界/重复键不得误发。来源挂载时读取偏好，人工操作不被设置覆盖。默认同步、显式流式、终态、草稿、历史契约保持。

#### Scenario: 输入法与请求中
- GIVEN 组合文字、刚结束组合、长按或请求中
- WHEN 按发送键
- THEN 不误发/重复；明确发送最多一次且输入提示一致

#### Scenario: 来源人工状态与终态
- GIVEN 展开偏好和人工收起卡片，或流式无有效 ANSWER
- WHEN 改设置或接收文本
- THEN 人工状态保持，受限来源仍不可见，不额外 ask/检索

### Requirement: Truthful Settings Account And Help Actions
设置 SHALL 复用真实身份/工作空间/共享退出，提供精确重置与历史导航。帮助/前端版本来自静态说明和真实构建元数据，不提供未接通的账户/组织/模型操作。

#### Scenario: 账户状态与退出
- GIVEN 资料加载/失败/长名或退出未确认
- WHEN 查看或退出
- THEN 沿用现有上下文与退出提示，不虚构字段、不重复请求或冒充服务端确认

#### Scenario: 本地操作与业务保持
- GIVEN 生成中或存在草稿
- WHEN 改偏好、看帮助、关闭设置
- THEN 不卸载业务、不重连/重发/清草稿，版本仅说明前端

### Requirement: Theme Aligned Settings Selection Menus

设置内五处选择 SHALL 使用已有组件库提供一致的深浅主题菜单，选中态包含勾号与文字，保留原选项/默认值/保存规则。菜单 MUST 位于当前 dialog 的非 inert 子树，按浮层及视口空间换向和限高，不遮断可操作项；关闭/卸载清理附加监听。

#### Scenario: 指针与键盘选择
- GIVEN 设置内选择菜单打开
- WHEN 点击任一选项或使用方向键/Home/End/Enter
- THEN 值与选中标记一致，业务副作用仍按原偏好规则

#### Scenario: 分层关闭与焦点
- GIVEN 焦点在选择控件中
- WHEN 按 Escape 或 Tab
- THEN Escape 先关菜单，菜单已关时再关闭设置；Tab 在设置内移动且菜单不残留

#### Scenario: 窄屏滚动与缩窗
- GIVEN 在窄屏或矮窗口展开菜单
- WHEN 滚动内容或调整窗口
- THEN 菜单在浮层与视口交集中可见，必要时换向/内部滚动，关闭设置后无残留菜单
