# F5 设计与事前审查包

## 事实基线

2026-10-02起点HEAD `407f21d2fe5ff2832894802d00bcf7f0dcceb0c5`，main ahead5，工作区干净、暂存区为空、ACTIVE_TASK=IDLE。F4仅客户端范围已归档，拖放/真实闭环/清理债务继续保留，不回改其接受结论。

Demo四项blob本轮重验一致：index.html `260a1268f34c22a08c64d76777218c0bf17d1f05`；css/app.css `d5bc03b6abe2cf78511a51e7b7a96f364da3a225`；js/app.js `4be498e0d644d5a2f8287a8f990e810e16b38f8d`；favicon.svg `9b46e403dbe3c0803b25488f42c58ec7a473b63f`。

正式/history指向views/history/ChatHistory.vue；HistoryView.vue、HistoryItem.vue、FeedbackDialog.vue属旧页引用链，不切回旧页或顺手清理。当前ChatHistory按size100自动读取全部分页再本地分组/过滤，无请求序号/会话/卸载保护；删除按钮只hover显示。批量清空对全部已加载ID逐个DELETE，更新事件可能在批量未结束时重载并回填旧结果。

HistoryFeedback接收真实historyId，ChatPanel用该ID作key；POST成功已保存并锁定、重复错误后只读核查。但读取/提交未捕获会话/生命周期，返回首条未额外核对qaId；响应丢失可能回到可直接再POST状态。ChatPanel历史读取有序号，卸载未增加历史序号，本轮只修复相关边界。

HistoryController仅page/size/kbId，无关键词；page从1开始，size最大100；按createdAt倒序，无稳定快照/cursor。服务端tenant/user授权为事实源。逐条DELETE限流30次/60秒，反馈POST20次/60秒；无批量DELETE端点，service的deleteByUserId不等于公开API。反馈重复提示存在FEEDBACK_001/002；QAFeedbackDTO无问题标题/KB名称。getMyFeedbacks已声明、未被正式页消费。

## 保留、适配与延后

| Demo/现有区域 | 正式适配 | 延后 |
| --- | --- | --- |
| 最近聊天行/分组 | 当前正式页显式分页，每条一问一答，独立打开链接和删除按钮 | 多轮/标题重命名 |
| 查找弹窗 | 历史页工具栏本页问题/KB ID过滤，可选服务端kbId范围 | Ctrl+K、全量关键词、跨库搜索 |
| 我的反馈 | 页内只读区域，真实评分/评论/日期/qaId，题目缺失降级 | mock统计、逐条补题、反馈改写/删除 |
| 详情/评分 | 复用ChatPanel/HistoryFeedback/F3正文来源，局部状态保护 | 点赞点踩自动映射、猜测新问答historyId |
| 删除/清理 | 逐条API、目标确认、部分失败/刷新恢复 | 新批量后端、原子清空、自动重试 |
| 侧栏 | 最多20条和既有更新事件 | 新共享缓存框架、侧栏搜索入口 |

## 分页、查找与归属

默认page1/size20；导航仅GET目标页，不自动预读全部历史。kbId校验正整数，切换范围回page1；KB名称仅来自现有可访问列表，没有名称用ID，不把KB不可访问当作历史不存在。可选KB列表失败不阻止全部历史浏览。

关键词仅过滤当前成功加载页的问题/KB ID，标注“仅筛选本页已加载记录”及匹配/本页数量；total/totalPages仅为服务端kbId范围元数据。翻页保留关键词，清空关键词不发请求，显式刷新当前页。删空末页导致越界只补一次合法末页GET。offset分页并发新增/删除可能重复/遗漏，提供刷新、不承诺一致快照。

各读取所有者校验请求序号、auth.sessionRevision、page/kbId/historyId和mounted；新请求/切范围/切记录/身份变化/卸载/确认删除使旧请求失效，finally也校验归属。旧列表不恢复已确认删除项，新身份不展示旧历史/反馈。沿用api/history.ts/request，不改认证或SSE。

## 删除与批量快照

单条确认含ID/问题摘要；打开链接与删除按钮独立，键盘/触屏不依赖hover。确认到写入结束防重复，服务端确认成功才移除/派发rag-history-updated。删除失败保留；成功后GET失败显示“删除已完成，列表刷新失败”，恢复只GET。

保留批量能力，入口为“批量删除历史”。用户点击后才顺序读取当前用户全部历史（不受本页关键词/kbId限制），size100、首个响应totalPages固定读取上界、ID去重；可中止读取而不DELETE。枚举失败/身份变化不进入确认。确认明确实际N条、全部知识库、当前读取快照非事务快照，期间新增可能不包含；total不冒充已枚举数量。

确认后锁定ID快照/会话，顺序逐条DELETE。普通单条失败记录并继续；429或写响应不明立即停止余项，身份变化停止后续请求并清理旧身份UI，不宣称撤销已发请求。报告确认成功/失败或状态不明/未执行数量；迟到读取不复活成功项。结束只派发一次成功事件并只读刷新，刷新失败与删除失败分开。重试必须重新核查并确认新目标，不自动重放或循环突破限流。历史删除不宣称关联反馈已清理。

## 单条反馈与我的反馈

成功读取当前history才展示反馈。返回项必须匹配qaId及auth.userInfo中可用userId；没有本地身份字段时仍以会话归属及服务端授权为边界，不伪造身份。评分1–5整数、评论可选，保留前端500字符限制、不称后端校验；失败保留输入，提交期间锁定。POST校验捕获目标，已知成功先保存/锁定再GET，读取失败只GET恢复。FEEDBACK_001/002维持重复核查，其他错误不当成功。

写请求已发无确定响应时展示“提交结果待确认”，先GET原historyId；核查失败保持不明，不自动POST。发现反馈锁定；成功GET为空仍不能证明原POST不会迟到落库，后续只能说明风险、由用户主动确认再提交，不宣称exactly-once。记录key/会话/卸载保护阻止旧提交/读取污染新页面。

我的反馈仅首次切换、用户刷新或同用户确认反馈更新后按需GET，四态区分。优先直接展示列表，不将评分作为质量指标。题目仅复用同会话已加载历史，缺失用“历史记录 #ID”，不逐条GET补题、不扩DTO。点击只读详情经既有授权，已删除/无权限明确不可用；切tab/路由/会话/卸载丢弃迟到结果。

## 文件与停止边界

批准后允许：views/history/ChatHistory.vue；components/history/HistoryFeedback.vue；新components/history/MyFeedbackList.vue页内区域；components/layout/AppSidebar.vue仅最近记录刷新；components/chat/ChatPanel.vue仅历史读取归属/只读错误恢复/反馈兼容；utils/historyPresentation.ts及必要的纯客户端utils/historyActions.ts；tests/qa-history.test.mjs、确受影响的现有主题/侧栏测试、新tests/history-experience.test.mjs和tests/preview.mjs封闭fixture；本change、ACTIVE_TASK、总规划和AGENT_LOG追加。knowledgeBase store只消费，不改其逻辑；实现文件名调整先登记等价落点。

局部CSS继承主题，不改global.css/variables.css、shell整体或路由结构。若必须扩大，先修订允许路径及兼容验收。禁止改Java/API/DTO/persistence/auth/request/token、useSSE/sseParser/qaPresentation、provider/model/检索/prompt/评测、依赖/锁文件、prototype、旧页/无关重复组件、accepted specs、本地配置/.agents/学习文件。缺搜索/反馈编辑/批量接口时不扩后端，另立方案。

## 验证方案

规划：四件套、需求/场景与baseline名称、决策三行、链接/旧路径、日志纯追加、diff whitespace及新增敏感内容扫描。OpenSpec CLI未发现，不安装，不冒充官方validate。

实施前Preflight；受控延迟/失败行为测试覆盖：单页GET/切页kbId竞态/过滤与total分离/卸载身份；单删双击/成功后GET失败；批量枚举失败或取消零DELETE/去重/部分失败/429或不明停止/身份变化/旧列表不复活；反馈ID归属/输入保留/双击/成功后GET失败仅读/重复/不明先GET；我的反馈零N+1。保全F3/R5和历史只读。运行 `node --test tests/*.test.mjs` 与 `npm run build`（vue-tsc -b && vite build）；npm缺失则顺序调用已声明工具入口，不加依赖。

独立localhost封闭preview：configFile:false、无proxy；仅F5场景白名单history页/详情/feedback读取及内存DELETE/POST，未知GET404/写405。默认/F3/F4不放开新写入；限制请求体、拒绝无效评分，确定性错误/迟到/429/写入不明。语法/拒绝规则单验；不记录真实正文/凭据，不触发provider。

CSS视口1440×900、1024×768、390×844 × light/dark六组，每组历史有记录、第二页或过滤无结果、我的反馈、详情反馈四态，至少24张稳定矩阵。Demo每组最近聊天/查找/我的反馈各一张至少18张区域参考；无独立历史分页整页，记录适配，不编造参考页。补loading/error/empty/长文/403或404/反馈失败不明/删除确认及部分失败限流/局部刷新失败。逐图查看并核对SHA-256及图像/DOM尺度，探索图不计矩阵；既有窄屏底边1px取整保留，不宣称像素匹配率。

交互验Tab/Enter/Space、Escape取消和焦点返回、分页/筛选/tab、窄屏无hover删除、长文本换行/无页面横向溢出、侧栏一致性。兼容首页/settings/login、F3回答来源、F4列表详情/KB导航，无自动ask。工程/合成/视觉与真实/质量分层，客户端完整验收后才呈交接受/归档。

## 决策记录

### 决策 1：落点与状态
- **面临的选择**：改当前ChatHistory并复用网络入口；切到旧HistoryView；新共享历史缓存框架。
- **选了哪个 + 为什么**：选正式页、按读取所有者加归属、保留事件，减少路由及数据所有权变化。
- **放弃的代价**：旧页需重复接线与视觉回归；共享框架扩大缓存失效/身份隔离设计。

### 决策 2：分页与查找
- **面临的选择**：自动拉全再过滤；显式分页/本页过滤；新增全量关键词API和Ctrl+K跨库搜索。
- **选了哪个 + 为什么**：选显式分页、注明本页范围，复用page/size/kbId，读取量可控。
- **放弃的代价**：自动拉全放大请求；新搜索扩大后端/权限契约。本页方案失去一次输入查遍全部历史的体验，必须明确范围。

### 决策 3：批量删除
- **面临的选择**：改为只删本页；保留全部范围但枚举确认快照；新增事务批量DELETE。
- **选了哪个 + 为什么**：选快照后逐条删除，保留能力、显式限流/部分失败；2026-10-03用户批准本方案实施，入口为批量删除历史、确认实际快照数量和全部知识库范围。
- **放弃的代价**：只删本页收窄能力且易误解；新端点扩大后端/persistence。快照方案有读取成本/并发漂移，不能原子清空。

### 决策 4：我的反馈字段
- **面临的选择**：现有字段并复用已加载题目；逐条GET补标题；照搬mock或扩DTO。
- **选了哪个 + 为什么**：选现有getMyFeedbacks页内展示与中性ID降级，零N+1。
- **放弃的代价**：逐条补题放大读取且失效历史会失败；mock伪造事实、扩DTO增加后端范围；降级可能暂时只有ID。

### 决策 5：反馈不明
- **面临的选择**：网络失败自动重发；先GET核查由用户决定；新后端幂等协议。
- **选了哪个 + 为什么**：选核查/明确确认，已知成功仅读，不猜测响应丢失为失败。
- **放弃的代价**：重发可能重复写或收到重复错误；新幂等需改后端。GET为空仍有迟到风险，保留不确定说明。

### 决策 6：范围与证据
- **面临的选择**：客户端验收、真实证据另授权；同时补F4/F1/F6；沿用旧调用/提交授权。
- **选了哪个 + 为什么**：选一个F5客户端change、真实预算0、默认手动提交；F4债务/C19/C20保持独立。
- **放弃的代价**：跨阶段扩大副作用/验收面；借旧授权超目标/载荷/责任；客户端证据不覆盖本轮持久化/质量收益。

[proposal](proposal.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md) · [正式路由](../../../rag-frontend/src/router/index.ts) · [历史API](../../../rag-frontend/src/api/history.ts)
