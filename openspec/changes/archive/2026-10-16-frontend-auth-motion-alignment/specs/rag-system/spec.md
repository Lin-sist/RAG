## ADDED Requirements

### Requirement: Login Brand Motion Presentation

正式登录页 SHALL 将已确认双翼装饰作为独立组件接入，保留真实LoginView、词标、标题及用户名/密码/提交顺序。装饰 SHALL 使用现有有效主题、细点/交叉/字符循环与背景透明切换，不新增纹理/拖尾/暂停操作栏、不读取凭据或业务内容、不模拟认证。

#### Scenario: 动效与真实表单分离
- GIVEN 用户打开正式/login
- WHEN 装饰循环或登录表单输入/提交
- THEN 动效只改变装饰画面，登录沿用唯一已有auth请求链，不带入原型演示表单或额外认证调用

#### Scenario: 主题与纹理过渡
- GIVEN 当前使用共享有效主题
- WHEN 切换主题或纹理在有效播放时间轮换
- THEN 整体装饰按当前主题刷新，旧形态淡出后在透明背景间隙切换再淡入新形态，表单/标题不随之淡出

#### Scenario: 窄屏与键盘
- GIVEN 深浅主题及指定三视口
- WHEN 键盘操作登录或页面收窄
- THEN 表单按钮可达、装饰不抢焦点，局部布局不造成页面横向溢出，静态缺失字段与认证错误不伪造成功

### Requirement: Login Motion Lifecycle And Safe Fallback

登录装饰 SHALL 由组件生命周期负责挂载与幂等销毁；降低动态效果 SHALL 显示完整静态帧。隐藏/离屏 MUST 停止绘制与有效轮换时间。WebGL/所需浏览器能力不支持、编译失败或上下文丢失 SHALL 静态降级且不阻断表单；资源及监听 MUST 在卸载时释放。

#### Scenario: 降低动态效果与离屏
- GIVEN 装饰正在透明过渡或后台/离屏
- WHEN 用户开启降低动态效果或可见性改变
- THEN reduce显示完整静态帧，后台/离屏停止调度，返回继续有效播放且不按后台时长跳过纹理

#### Scenario: 渲染故障
- GIVEN WebGL初始化失败或上下文丢失
- WHEN 用户查看或提交表单
- THEN 装饰显示同源静态品牌图，表单继续已有认证路径，恢复失败不伪装成动态渲染通过

#### Scenario: 路由卸载与重挂
- GIVEN 登录装饰已有RAF/observer/media/document/canvas监听或WebGL资源
- WHEN 离开登录页或再次进入
- THEN 原实例幂等释放、不再绘制，重入创建新实例；迟到回调不更新卸载组件，不累积动画与监听
