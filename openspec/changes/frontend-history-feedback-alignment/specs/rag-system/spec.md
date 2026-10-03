## ADDED Requirements

### Requirement: Explicit History Paging And Scoped Find

正式历史页 SHALL 使用既有page/size/kbId显式分页，默认20条，保留真实时间分组及单轮详情导航。关键词 MUST 明确仅过滤本页已加载问题/KB ID，不新增全量关键词请求；total MUST NOT 当作关键词匹配总数。页面 SHALL 区分loading/error/empty/filtered-empty/ready，迟到结果 MUST 校验目标、会话及生命周期。

#### Scenario: 分页与本页查找
- GIVEN 当前成功加载一页历史
- WHEN 输入关键词、翻页或切换kbId范围
- THEN 关键词只过滤本页，翻页只GET目标页，范围切换回第1页，无匹配不宣称全量无结果

#### Scenario: 读取归属与失败
- GIVEN 请求未结束时切页、范围、会话或卸载
- WHEN 旧请求返回或当前读取失败
- THEN 旧结果不污染当前页面，当前错误可重试且不当作空历史

#### Scenario: 可达操作与窄屏
- GIVEN 深浅主题、长问题或窄屏
- WHEN 键盘或触屏打开记录及删除
- THEN 两控件独立可达、不依赖hover，详情仍单轮只读且缺失字段不伪造

### Requirement: Confirmed History Deletion And Partial Recovery

历史删除 SHALL 复用逐条DELETE、确认目标并防重复。批量 MUST 由用户启动完整读取/去重快照并确认实际数量和全部知识库范围；快照失败/取消 MUST NOT DELETE。系统 SHALL 分别报告确认成功、失败或不明、未执行数；限流/写响应不明/身份变化 MUST 停余项，不自动重放。成功后刷新失败 MUST 区别删除失败，仅只读恢复，不承诺原子清空或关联反馈清理。

#### Scenario: 快照准备与取消
- GIVEN 用户启动批量删除准备
- WHEN 枚举失败、身份变化或取消确认
- THEN 不DELETE，不用total或当前页数量伪造完整快照

#### Scenario: 部分成功与停止
- GIVEN 用户确认去重ID快照
- WHEN 部分失败、429、写响应不明或身份变化
- THEN 保留成功及失败/未执行数，停止条件后不继续写入、不自动重试或称全清空

#### Scenario: 删除成功但刷新失败
- GIVEN 服务端已确认删除且派发成功事件
- WHEN 列表/最近记录读取失败或旧请求返回
- THEN 已删项不被旧结果恢复，提示刷新失败，恢复只GET不再次DELETE

### Requirement: Record-Bound Feedback And Uncertain Submission

反馈 SHALL 绑定已成功读取的真实historyId，1–5整数评分/可选评论，失败保留、防重复。结果 MUST 校验记录/会话/生命周期；已知POST成功 SHALL 保存锁定、读取失败只GET恢复。写响应不明 SHALL 明确待确认并先核查原记录，不自动POST；GET为空 MUST NOT 解释为原POST不会迟到成功。详情仍 SHALL 单轮只读并保全F3/R5来源资格。

#### Scenario: 成功后读取失败
- GIVEN POST返回对应记录的成功反馈
- WHEN 随后GET失败
- THEN 保留成功、禁止重复提交，恢复只读取原historyId

#### Scenario: 写入不明与重复
- GIVEN 写已发无确定结果或收到既有重复错误
- WHEN 用户恢复反馈状态
- THEN 先GET原记录不自动重发，核查失败保持不明，空结果仍说明迟到风险并需用户主动确认再次提交

#### Scenario: 记录及会话切换
- GIVEN 反馈或详情请求尚未结束
- WHEN 切记录、会话或卸载后旧请求返回
- THEN 旧反馈/输入/答案不污染新页面，未成功读取记录不展示可提交反馈

### Requirement: Truthful My Feedback Presentation

历史页 SHALL 提供只读我的反馈区域，复用当前身份getMyFeedbacks、区分loading/error/empty/ready。评分/评论/日期/qaId SHALL 来自真实返回；题目仅复用同会话已加载历史，缺失用中性ID，不逐条补题或伪造KB/质量数据。点击记录 SHALL 经既有详情读取授权，失败不伪造答案。

#### Scenario: 无标题与按需读取
- GIVEN 反馈有qaId但题目未加载
- WHEN 进入我的反馈
- THEN 展示真实反馈及记录ID，仅按需读取列表，无逐条补标题请求

#### Scenario: 错误、空与失效记录
- GIVEN 反馈读取失败、为空或目标详情不可访问
- WHEN 用户查看或重试
- THEN 状态明确、恢复只读，不加mock或绕授权，迟到旧会话结果不回填
