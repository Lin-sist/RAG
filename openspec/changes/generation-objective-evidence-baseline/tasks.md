# Tasks: C18 Generation Objective Evidence Baseline

## 0. 当前规划
- [x] C17阈值获批、三轮PASS、baseline接受与文件归档完成；未将被拒的commit写成成功。
- [x] 按原用户请求开始下一阶段规划，审计runner、RAGService、QueryEngine、generator及REST持久化。
- [x] 创建proposal/design（8条决策记录）/tasks/evaluation delta；C18唯一active change。
- [x] 当前仅规划，provider/backend calls=0、业务数据出站=false；C18规划提交责任于2026-09-11由用户授权更新为Agent本地提交。
- [ ] 用户审阅批准规划与W0/W1离线实施范围。

## 1. W0 调用与身份审计（零外调）
- [ ] 从实际Java纯逻辑枚举canary/full初始和解释回退variants，输出ID-only预算；绑定代码/数据/配置hash，冻结精确E，重验11/451初始项。
- [ ] 核对debug/ask检索、generation最坏调用数、runner/provider retries=0、答案缓存关闭、embedding cache计数、Router关闭及heuristic归因。
- [ ] 核对prompt/model/endpoint/temperature/token budget/timeout安全fingerprint来源，不读取或输出secret。
- [ ] 明确query count/history副作用和共享限流节奏，不修改既有业务语义。

## 2. W1 离线工具与验证
- [ ] RED→GREEN：C18 manifest/schema和模式互斥；ID/order/repeat、预算或source hash漂移在login前拒绝。
- [ ] RED→GREEN：query/generation HTTP调用前护栏，缓存/算法回退/provider fallback/自动retry分别统计；超限不发请求。
- [ ] RED→GREEN：exact150 compiler、details/metadata/hash/channel status；missing/error/identity负例。
- [ ] RED→GREEN：复用C9公式，不以debug contexts冒充ask provenance，不以成功子集补分母。
- [ ] safe summary/schema、no-overwrite与敏感字段负例；C17 profile/reference字节保持不变。
- [ ] Python全套；若新增Java测试/实现则聚焦及按风险全仓Maven。真实结果和跳过项写AGENT_LOG。
- [ ] canary/full plan-only：精确预算、模型、出站和副作用，前置缺失时fail closed。

## 3. W2 Canary（待单独授权）
- [ ] 提交形成clean HEAD，HTTP preflight只读验证3 fixtures/50 chunks、c17g3/model identity。
- [ ] 披露5 debug/5 ask/≤5 generation、精确E(canary)、fixed ID出站、timeout/retry/限流及query count/history写入，取得授权。
- [ ] 固定5条一次no-overwrite执行，judge/model rerank=0；失败保留并停止，无自动重跑或full。
- [ ] 验证canary预算/identity完整，不据小样本宣称质量达标。

## 4. W3 Full（待单独授权）
- [ ] canary clean后披露150 debug/150 ask/≤150 generation、精确E(full)及数据/副作用范围，取得独立授权。
- [ ] 一次完整150 run；raw全部保留，无自动retry、无拼接、无KB重建或历史清理。
- [ ] compiler COMPLETE、CLEAN/objective COMPLETE/judge SKIPPED；IDs/order/hash/errors/budget/provider归因完整。
- [ ] 分通道数值、分母和no-context bypass/调用计数；明确词法指标与单次run边界。

## 5. W4 收尾
- [ ] 用户验收真实baseline及边界；不激活objective/judge profile、不改C17阈值。
- [ ] approved delta接受进baseline，更新说明/债务/日志，验证exact suffix/链接/隐私。
- [ ] 用户授权归档后移动change并恢复IDLE；按明确提交责任处理commit，push/PR/deploy仍需单独授权。

## 2026-09-11 本地提交交接
- [x] 用户明确授权Agent本地提交并保持Git干净；C17收尾已提交，C18规划独立提交。
- [x] 本轮不开始C18实现或真实调用，保留上述尚未实施的任务与证据闸门。
