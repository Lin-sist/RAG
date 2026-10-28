## ADDED Requirements

### Requirement: Self Scoped Editable Account Profile

系统 SHALL 允许当前用户修改自己的显示昵称、资料邮箱与头像；服务端从认证身份确定用户/租户，验证双方有效状态。用户名、租户、角色、密码与权限 MUST NOT 可由资料更新载荷修改；未知字段拒绝。

#### Scenario: 当前账号保存
- GIVEN 当前账号与租户有效
- WHEN 使用合法昵称、邮箱和头像动作保存
- THEN 只更新本人资料，返回真实版本后的完整资料，不改变认证身份或组织归属

#### Scenario: 伪造目标或受保护字段
- GIVEN 缺少认证、身份失效或载荷包含目标用户/租户/角色
- WHEN 尝试读取或保存
- THEN 拒绝访问或拒绝载荷，不读取/修改其他账号资料

### Requirement: Atomic Versioned Profile Persistence

资料保存 SHALL 校验 expectedRevision，在同一事务更新昵称/邮箱/头像并递增既有 version；邮箱空白转 null，保留唯一约束。失败 MUST NOT 留下部分字段或孤儿头像。

#### Scenario: 旧版本或邮箱冲突
- GIVEN 资料版本已改变或邮箱不可用
- WHEN 提交旧草稿
- THEN 返回明确 409，保留服务端原资料和头像，不暴露其他用户身份

#### Scenario: 头像或写入失败
- GIVEN 头像校验或事务中任一步失败
- WHEN 执行保存
- THEN 不提交其他字段，不报告完整成功

### Requirement: Bounded Authenticated Avatar Content

头像 SHALL 接受受限 JPG/PNG，独立校验字节/尺寸/像素并规范为有上限的 256×256 PNG；不接收脚本格式或外部 URL。头像只通过当前用户的认证二进制接口读取，no-store/nosniff，不进入登录投影、Token、普通日志或 localStorage。

#### Scenario: 非法或超限图片
- GIVEN 非支持格式、无效字节、超 2 MiB 或超尺寸/像素图片
- WHEN 上传头像
- THEN 返回明确错误，资料和头像不变

#### Scenario: 头像回读与替换
- GIVEN 当前用户已保存规范头像
- WHEN 读取、替换或移除
- THEN 只返回本人头像/正确的不存在状态，重编码不保留原元数据，其他账号不能通过 ID 获取

### Requirement: Explicit Profile Draft And Honest Save State

前端 SHALL 在现有浮层内提供资料预览/取消/保存和未保存离开确认。服务端确认保存后头像与昵称立即更新到共享展示，没有额外的保存草稿步骤；确认前不得提示保存成功。本地偏好重置不得清账号资料。

#### Scenario: 预览取消或离开
- GIVEN 选择了新头像或修改了文字但尚未保存
- WHEN 取消、切换栏目或关闭
- THEN 取消可还原基线，离开先确认，不执行写入并释放废弃预览

#### Scenario: 请求结果未知
- GIVEN 保存后超时、断线或非确定性服务端失败
- WHEN 客户端未收到确认
- THEN 保留当前输入并提供简短的重试提示，不展示用户手动处理的草稿核对面板，不自动重放写请求或冒充成功；用户再次点击保存时内部读取最新版本再提交

### Requirement: Profile Session And Workspace Compatibility

新资料状态 SHALL 与现有 auth/session context 兼容，保存不轮换 Token、不改会话身份、不重建业务页；迟到低版本与旧账号响应不得覆盖新资料。共享展示优先昵称，登录名始终独立只读，邮箱不声称已验证/可找回。

#### Scenario: 保存后刷新与侧栏展示
- GIVEN 服务端已确认保存
- WHEN 同账号刷新或重新读取上下文
- THEN 显示服务端真实昵称/邮箱/头像，侧栏与资料一致，问答/草稿/SSE 不被资料保存重置

#### Scenario: 旧响应与身份变化
- GIVEN 已保存新版本或已退出/换号
- WHEN 旧上下文/头像/保存结果迟到
- THEN 不回填旧版本或旧身份，清理旧预览和 Blob URL，组织授权保持服务端规则

## MODIFIED Requirements

### Requirement: Truthful And Accessible Frontend Settings

设置 SHALL 提供既有外观偏好、真实账户与工作空间，并在资料编辑接口可用时提供有明确保存状态的头像/昵称/资料邮箱表单。身份、归属、登录名、密码与密钥不得用演示数据或未接通按钮冒充能力；旧后端缺少资料接口时保留只读降级并说明不可编辑。浮层 SHALL 支持一致主题、窄屏、键盘焦点、字段标签/错误与未保存确认。

#### Scenario: 身份缺失
- GIVEN 当前认证资料不可用
- WHEN 查看资料或尝试进入编辑
- THEN 显示资料暂不可用，不猜测身份、不解析 Token 或持久化个人资料补值，不允许向未知目标保存

#### Scenario: 只读资料
- GIVEN 账号资料已知
- WHEN 查看用户名、组织或账号安全项
- THEN 保持真实字段及缺失降级，登录名/组织只读，不提供密码或 API 密钥演示操作；可编辑字段仅来自已接通的资料接口

#### Scenario: 键盘与主题
- GIVEN 用户打开设置
- WHEN 使用 Tab/Escape 或切换外观
- THEN 保持焦点在浮层内、关闭后归还触发器、主题一致且窄屏无横向溢出，并遵守未保存确认

#### Scenario: 资料服务不可用
- GIVEN 资料接口缺失或读取失败
- WHEN 打开账户栏目
- THEN 展示真实已知只读字段及失败/重试说明，不提供假保存，不从旧演示页补值

#### Scenario: 键盘与未保存确认
- GIVEN 正在编辑并存在未保存草稿
- WHEN 使用 Tab/Escape 或关闭入口
- THEN 焦点留在浮层/行内确认，取消后归还合理控件，其他偏好保存方式不受影响
