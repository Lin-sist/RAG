## ADDED Requirements

### Requirement: Readable Demo-Aligned Answer Presentation

正式聊天页 SHALL 对齐冻结 Demo 的问题气泡、回答排版、代码块与动作区，在深浅主题及指定窄屏下可阅读。Markdown MUST 保持原始 HTML 不执行，代码与表格的横向滚动 MUST 限制在自身区域。复制 SHALL 以实际剪贴板结果提示成功或失败，不能模拟成功。

#### Scenario: 长内容与键盘复制
- GIVEN 回答包含长代码、表格或长标题
- WHEN 用户在窄屏阅读并用键盘复制
- THEN 页面不横向溢出，代码/表格自身可滚动，复制控件可达且结果真实

#### Scenario: 不可信回答内容
- GIVEN 回答含 HTML、事件文本或不安全协议链接
- WHEN 渲染回答
- THEN 原始 HTML 不执行，链接按安全协议校验，代码栏与动作不注入回答脚本

### Requirement: Independent Expandable Answer Sources

来源 SHALL 仅消费当前已适配的 citations，展示实际标题、片段及可选检索相关度，并允许键盘展开/收起完整片段。缺失字段 MUST 中性降级，不补假分数，不自行建立答案句子与来源编号对应关系，不为展开额外 ask 或检索。

#### Scenario: 来源展开与字段缺失
- GIVEN 返回长 snippet 或缺失标题/score
- WHEN 用户展开、收起来源
- THEN 只呈现实际片段，标题沿用已有降级，缺失score不展示假值，不发起业务请求

#### Scenario: 流式来源权限
- GIVEN 未获唯一有效 ANSWER terminal 或客户端已停止接收
- WHEN 呈现部分文本与终态提示
- THEN 保留现有 incomplete/非答案/本地停止语义，不展示来源或通知成功历史刷新

### Requirement: Reader-Controlled Answer Scrolling

消息区 SHALL 在用户接近底部时跟随新增内容，上翻阅读时暂停强制跟随，并提供可访问的回到底部入口。此展示行为 MUST 保留同步默认、structured-v1请求与适配、单轮历史只读及客户端停止的既有语义。

#### Scenario: 上翻阅读与恢复
- GIVEN 长回答正在增加内容且用户已向上滚动
- WHEN 收到新文本或点击回到底部
- THEN 新文本不强制拉回底部，主动回底恢复跟随，不新增网络请求或历史事件

#### Scenario: 历史与既有流程
- GIVEN 用户查看历史或显式选择流式
- WHEN 使用新的正文和来源界面
- THEN 历史仍禁止续聊，流式仍由原parser与唯一有效terminal判断，停止接收不宣称服务端取消或零费用
