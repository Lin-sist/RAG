# 正式登录双翼动效接入任务

## 0. 事前闸门

- [x] 0.1 核查AGENTS/活动指针、F5/F1提交、正式AuthLayout/LoginView/theme与冻结原型。
- [x] 0.2 准备proposal/design/spec delta，明确差异、允许路径、决策、三视口及零真实请求。
- [ ] 0.3 用户批准本具体审查包；未批准不改动效业务代码。

## 1. 组件化切片

- [ ] 1.1 复核Git/授权/冻结hash，移植renderer与typed接口、核对shader字符串。
- [ ] 1.2 新Vue组件负责mount/watch/refresh/destroy与静态SVG；装饰失败不阻断真实表单。
- [ ] 1.3 AuthLayout右侧接组件/局部颜色，保留统一theme、现有词标/标题/真实表单，无控件/几何优化。

## 2. 验证

- [ ] 2.1 生命周期/降级/主题/资源释放聚焦测试，既有认证/退出/历史契约保全。
- [ ] 2.2 正式含vue-tsc build与必要回归、冻结参考/范围/敏感内容/diff检查。
- [ ] 2.3 六组正式矩阵及原型参考，过渡三阶段、reduce/降级、窄屏布局逐图核验。
- [ ] 2.4 合成真实浏览器表单/等待/拒绝/登录成功/F1退出/主题/键盘流程，真实请求0。
- [ ] 2.5 acceptance/visual-evidence与执行日志分层呈交，性能和真实设备限制保留。

## 3. 收口

- [ ] 3.1 按本轮已有Agent提交授权精确提交实现/证据，不push/PR/部署。
- [ ] 3.2 用户接受并授权后baseline/归档；整个change未完成不清活动指针。
- [ ] 3.3 独立进入F6全页面客户端验收与剩余真实证据分流，不把本change称为完整Demo。

[proposal](proposal.md) · [design](design.md) · [spec delta](specs/rag-system/spec.md)
