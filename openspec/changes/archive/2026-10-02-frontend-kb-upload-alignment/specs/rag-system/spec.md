## ADDED Requirements

### Requirement: Demo-Aligned Knowledge Base Management

正式 `/kb` 与 `/kb/:id` SHALL 对齐冻结 Demo 的资料库层级、列表/网格、详情、表单与响应式体验，保留现有创建/编辑/统计/删除。筛选 MUST 明确仅作用于已加载数据；统计 MUST 使用当前 KB 实际 documentCount/vectorCount/queryCount，不假造大小/就绪/命中/趋势。公开/私有文案 MUST 保持 tenant 与服务端授权边界。

#### Scenario: 本地视图与筛选
- GIVEN 成功加载可访问知识库或当前 KB 文档
- WHEN 切换列表/网格或筛选名称/描述/文档标题和类型
- THEN 仅重排/筛选已加载数据，不新增全量搜索请求，空与筛选无结果可区分

#### Scenario: 编辑重开与失败
- GIVEN 关闭后重新编辑同一 KB
- WHEN 打开表单或提交失败
- THEN 按该 KB 回填并使用更新接口，失败保留输入，提交期间不重复写请求

#### Scenario: 统计与页面身份
- GIVEN KB 切换时有迟到响应或统计/文档局部失败
- WHEN 呈现详情与刷新结果
- THEN 只显示属于当前 KB 的数据，错误可独立重试，不抹掉确认详情、不当作零值

#### Scenario: 窄屏与真实字段
- GIVEN 深浅主题、长内容或指定窄屏
- WHEN 阅读列表/详情或操作弹窗
- THEN 控件可达、表格自身滚动、键盘关闭与焦点返回有效，缺失字段不补假值

### Requirement: Honest Upload Acceptance And Query Recovery

上传 SHALL 以 HTTP 202 和非空 taskId 识别接受，区别接受、索引完成、失败与接受未确认。已接受文件 MUST 移出后续队列；失败/未确认文件 SHALL 保留供用户处理，不自动重传。查询 SHALL 复用唯一 poller，读取错误恢复只查同一已知 taskId，不重放索引；无跨刷新任务关联接口时 MUST NOT 宣称恢复原任务。

#### Scenario: 混合上传结果
- GIVEN 多文件部分已接受、部分失败
- WHEN 查看结果并再次明确提交待选文件
- THEN 已接受文件不重复发送、失败项可见，无有效 taskId 不伪装成功或完成

#### Scenario: 接受结果不明
- GIVEN 响应丢失或 202 缺 taskId
- WHEN 呈现错误与后续操作
- THEN 明确接受状态无法确认，不声称后台未处理，不静默重发，提醒核查文档

#### Scenario: 查询暂停与恢复
- GIVEN 已知 taskId 查询失败
- WHEN 用户恢复查询
- THEN 仅查询原 taskId，同 id 不并行轮询、不再上传；COMPLETED/FAILED/CANCELLED 停止查询并保留结果

#### Scenario: 页面离开与需要核对
- GIVEN 切 KB/卸载或文档为 RECONCILIATION_REQUIRED
- WHEN 本地查询停止或重新读取文档
- THEN 迟到结果不污染新页面，不宣称服务端取消、不自动重放、不虚构任务 id/跨刷新恢复

### Requirement: Confirmed Deletion And Dependent Refresh

KB/文档删除 SHALL 确认目标、防重复，服务端成功后才移除或返回列表。文档删除与任务终态 SHALL 刷新相关文档/统计/KB 列表；成功删除后的局部刷新错误 MUST 区别于删除失败，不诱导重复删除。

#### Scenario: 删除取消或失败
- GIVEN 取消确认或服务端拒绝删除
- WHEN 更新页面
- THEN 取消不发 DELETE，失败不移除记录，明确错误，不模拟成功

#### Scenario: 删除成功后刷新失败
- GIVEN 删除成功而统计/列表刷新失败
- WHEN 呈现结果并重试
- THEN 区别确认删除与读取失败，恢复只重读失败数据，不重复 DELETE

### Requirement: Explicit Knowledge Base Handoff To New Question

详情 SHALL 提供指定 KB 新问答入口；路由 kbId MUST 经正整数与当前可访问列表校验才预选，失败不静默选其他 KB，不自动 ask。历史仍 SHALL 单条只读，同步默认、structured-v1终态、来源资格及成功历史事件语义不变。

#### Scenario: 可访问 KB 新问答
- GIVEN 详情点击开始提问且 KB 在可访问列表
- WHEN 新问答加载完成
- THEN 显示对应范围，等待用户输入/发送，无自动 provider 请求/history 写入

#### Scenario: 非法参数与历史兼容
- GIVEN kbId 非法/不可访问/列表失败或历史附带 kbId
- WHEN 加载页面
- THEN 新问答明确错误且不误选，历史忽略参数并保持只读/原来源契约
