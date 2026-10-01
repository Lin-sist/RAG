## ADDED Requirements

### Requirement: Frontend Demo-Aligned Navigation And Brand

正式前端 SHALL 使用本地 Demo 箭刃与一致词标，提供新问答、知识库、历史固定入口和最多 20 条真实最近记录。移除侧栏全量 KB 展开后 SHALL 保留列表和输入框选库能力。侧栏 MUST 区分加载、错误、空和成功，不暴露未接工作台或搜索入口。

#### Scenario: 记录与知识库访问
- GIVEN 用户已登录且列表读取成功
- WHEN 查看最近记录或进入知识库
- THEN 使用真实 history id 打开单轮详情，KB 经原路由及输入框访问，不冒充多轮 conversation

#### Scenario: 读取失败
- GIVEN 最近请求失败
- WHEN 侧栏呈现
- THEN 显示错误和手动重试而非空记录，旧会话结果不得覆盖新会话

#### Scenario: 窄屏和折叠
- GIVEN 用户处于窄屏或桌面折叠布局
- WHEN 打开关闭侧栏
- THEN 入口可识别可操作，无横向溢出，不因展开折叠清空当前消息

### Requirement: Unified Frontend Theme Preference

前端 SHALL 由唯一状态所有者管理 system/light/dark，兼容 theme=light/dark，缺失/无效按 system。首批区域及浮层 SHALL 使用相同有效主题；存储失败 MUST NOT 阻断页面，外观持久化 MUST NOT 含身份、凭据或业务数据。

#### Scenario: 旧值与手动选择
- GIVEN 已保存 dark 或 light
- WHEN 启动、跨路由或打开设置
- THEN 首次挂载及后续区域主题一致，系统变化不覆盖手动选择

#### Scenario: 跟随系统
- GIVEN 选择 system 或没有有效偏好
- WHEN 系统主题变化
- THEN 页面和 Teleport 弹窗同步更新，重复挂载不叠加监听

#### Scenario: 存储失败
- GIVEN 浏览器不能读写外观偏好
- WHEN 用户选择主题
- THEN 本次选择在内存生效且页面可用，不模拟持久化成功

### Requirement: Demo-Aligned Home With Preserved QA Behavior

首页 SHALL 对齐 Demo 问候、胶囊输入、范围 chip 和紧凑建议，保持同步默认及显式流式。建议 SHALL 仅填入文本，未选 KB MUST NOT 发问。迁移 MUST NOT 改变 structured-v1 终态、来源、停止接收或单轮历史只读语义。

#### Scenario: 建议与未选范围
- GIVEN 未选 KB
- WHEN 点击建议
- THEN 填入但不发送，提示选择 KB

#### Scenario: KB 读取失败
- GIVEN 范围列表加载失败
- WHEN 打开选择器
- THEN 显示可重试错误，不冒充空列表

#### Scenario: 既有问答行为
- GIVEN 用户选定 KB
- WHEN 保持默认或显式选择流式
- THEN 分别沿用同步或 structured-v1，不为来源额外 ask，不把停止接收解释为服务端取消
- AND 历史详情仍禁止续聊

### Requirement: Truthful And Accessible Frontend Settings

设置 SHALL 提供常规外观与只读资料，仅使用现有认证状态真实字段，缺失时中性降级。系统 MUST NOT 显示 mock 身份/密钥或提供未接通的资料保存、头像、改密、密钥生成操作。设置 SHALL 支持一致主题、窄屏和键盘焦点管理。

#### Scenario: 身份缺失
- GIVEN 刷新后 userInfo 不可用
- WHEN 查看资料
- THEN 显示资料暂不可用，不猜测身份、不解析 token 或新增个人资料持久化补值

#### Scenario: 只读资料
- GIVEN auth store 有资料
- WHEN 打开设置
- THEN 只展示真实字段，无编辑保存/密码/API 密钥占位操作

#### Scenario: 键盘与主题
- GIVEN 用户打开设置
- WHEN 使用 Tab、Escape 或切换外观
- THEN 焦点在弹窗内，关闭后归还触发控件，主题一致且窄屏无横向溢出

### Requirement: Frontend Experience Acceptance Evidence

体验迁移 SHALL 分别提供工程、视觉与交互证据，按 design 固定版本/视口/主题/状态逐项标注对齐、适配或延后。合成数据 MUST NOT 被表述为真实后端/provider 或质量门禁证据。

#### Scenario: 逐区域验收
- GIVEN 首批实现完成
- WHEN 审核交付
- THEN 提供含 vue-tsc 构建、受影响测试、指定截图及键盘/窄屏检查，缺少视觉证据不能仅凭 build 收口

#### Scenario: 证据边界
- GIVEN 使用封闭合成预览
- WHEN 展示状态
- THEN 明示合成范围，未知业务请求失败关闭，无真实代理，不宣称 C19/C20 或新增真实 provider 场景通过
