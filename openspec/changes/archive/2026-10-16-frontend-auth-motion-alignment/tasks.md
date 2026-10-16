# 正式登录双翼动效接入任务

## 0. 事前闸门

- [x] 0.1 核查AGENTS/活动指针、F5/F1提交、正式AuthLayout/LoginView/theme与冻结原型。
- [x] 0.2 准备proposal/design/spec delta，明确差异、允许路径、决策、三视口及零真实请求。
- [x] 0.3 2026-10-14用户明确“批准登录动效接入（推荐）”，按本审查包实施，复用本地提交授权，真实请求0。

## 1. 组件化切片

- [x] 1.1 复核Git/授权/冻结hash，移植renderer与typed接口、核对shader字符串。
- [x] 1.2 新Vue组件负责mount/watch/refresh/destroy与静态SVG；装饰失败不阻断真实表单。
- [x] 1.3 AuthLayout右侧接组件/局部颜色，保留统一theme、现有词标/标题/真实表单，无控件/几何优化。

## 2. 验证

- [x] 2.1 生命周期/降级/主题/资源释放聚焦测试，既有认证/退出/历史契约保全。
- [x] 2.2 102/102、正式含vue-tsc build通过；冻结参考/范围/敏感内容/diff收尾检查通过。
- [x] 2.3 六组正式/六张参考、14补充文件逐图核验；26文件/25不同hash、过渡/reduce/降级/窄屏通过，早期两图排除并重采。
- [x] 2.4 完整浏览器复验41/41，定向9/9/稳态3/3；表单/等待/拒绝/成功/F1退出/主题/键盘通过，真实请求0，首轮定位失败保留。
- [x] 2.5 acceptance/visual-evidence与执行日志分层呈交，性能和真实设备限制保留。

## 3. 收口

- [x] 3.1 按本轮已有Agent授权提交实现/证据，不push/PR/部署；提交完成以本轮Git核验为准，日志Commit: pending随提交写入。
- [x] 3.2 2026-10-16用户确认成功并要求归档；2 requirements/6 scenarios逐字进入baseline并归档。
- [x] 3.3 移交独立F6全页面客户端验收与真实证据分流，见[F6执行记录](../../../../docs/roadmap/frontend-f6-client-acceptance.md)；不代表完整Demo。

[proposal](proposal.md) · [design](design.md) · [spec delta](specs/rag-system/spec.md)
