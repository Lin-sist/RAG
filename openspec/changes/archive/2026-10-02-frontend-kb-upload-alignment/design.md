# F4 设计与事前审查包

## 事实基线

2026-10-01 HEAD 为 `be74d3902b4d7af4f3b932cc7a1c3c31bafbe281`，main 领先 origin/main 2 个提交，起始工作区干净。F3 已归档并接受 baseline；开始规划前 ACTIVE_TASK 为 IDLE。本轮不回改已归档文档或 accepted spec。

Demo 冻结四项 blob：index.html `260a1268f34c22a08c64d76777218c0bf17d1f05`；css/app.css `d5bc03b6abe2cf78511a51e7b7a96f364da3a225`；js/app.js `4be498e0d644d5a2f8287a8f990e810e16b38f8d`；favicon.svg `9b46e403dbe3c0803b25488f42c58ec7a473b63f`。本轮 hash-object 重验一致。

直接核查：KBCreateDialog.closed 清空 isEdit/editId，但 watch 只观察 editData，同对象重开存在模式丢失风险；上传循环结束清空全部文件，包括失败项；任务终态刷新文档/统计，文档删除仅刷新文档，列表事实可能滞后。统计展示尚未显式关联当前 KB 身份，路由切换与迟到响应需专项验证。文档五态已有处理失败/需要核对标签。

当前 REST 没有文件大小、原文预览或文档→taskId 查询；Document.inputSizeBytes 被 JsonIgnore。TaskController.cancel 返回 `{taskId,cancelled}`，与前端 void 声明不一致，本轮不启用取消、不改 DTO。后台 durable ledger 不等于浏览器拥有恢复接口。baseline 的 is_public 仅 tenant 内公开；私有仍可存在显式授权，不能标成“仅自己可见”。

## 逐区域保留、适配与延后

| Demo 区域 | 正式落点与适配 | 延后 |
| --- | --- | --- |
| 页头/列表/网格 | KnowledgeBaseList；真实名称、描述、文档数、更新时间、可见性与管理入口；列表默认，网格本地状态；筛选/数量注明已加载范围 | 混合全库文档、跨库搜索、文件大小、mock 就绪/评测标签 |
| 返回/hero/操作 | KnowledgeBaseDetail；编辑/删除、指定 KB 新问答入口；长名称/描述换行 | 分享、归档、会话重命名 |
| KPI/侧区 | KBStatsPanel 仅 documentCount/vectorCount/queryCount，标签文档数量/向量数量/查询次数；基本信息与真实向量身份可读 | 索引体积、检索命中、七日曲线、固定模型/分块配置、租户名称 |
| 文档列表 | DocList；已加载文档标题/类型筛选，五态保全，分块数仅完成时展示，图标按钮有名称 | 原文预览/下载、全量搜索 |
| 创建/编辑/统计 | 现有组件与 Element Plus；共享主题变量、弹窗不超视口、键盘关闭/返回焦点、独立错误与加载 | 新弹窗框架、权限模型变更 |
| 上传/任务 | DocUploader/DocProgress；真实文件选择、五态任务、真实 progress/message | Demo timer 模拟、取消、索引重放、跨刷新恢复 |

## 请求与数据契约

| 动作 | 现有网络入口 | 展示/更新条件 |
| --- | --- | --- |
| 列表/详情/统计 | api/knowledgeBase.ts：listKB/getKBById/getKBStatistics | 错误不当空；统计确认属于当前 id 才显示，失败不抹掉详情 |
| 创建/编辑 | createKB/updateKB | 服务端成功才更新；重开回填，失败保留输入，提交防重复；描述清空用既有可表达的空字符串 |
| KB 删除 | deleteKB | 确认目标、防重复；成功才移除/返回列表，失败保留 |
| 文档上传 | uploadDocument，multipart file/title | HTTP 202 且非空 taskId 才开始查询；不等同索引完成；每文件本次最多发送一次 |
| 文档列表/删除 | listDocuments/deleteDocument | 删除确认含标题；成功后刷新文档/统计/相关 KB 列表；刷新失败与删除失败分开反馈 |
| 状态查询 | useTaskPolling → createTaskPoller → getTaskStatus | 沿用 2000ms 请求完成后调度，同 taskId 单 job；终态停止；读取失败停止并允许恢复 GET |
| 新问答导航 | 本地 `/chat?kbId=<id>` | 可访问列表校验后预选；history 路由忽略 kbId；无自动 ask/history 写入 |

不以 ownerId/isPublic 推断最终写权限；服务端 403/404/429/503 规范化呈现，列表可见不等于可写。公开文案为“当前租户内公开，访问以服务端授权为准”，私有为“非公开，访问以服务端授权为准”；不增加 tenant selector。

## 状态与失败恢复

列表、文档分别区分 loading/error/empty/filtered-empty/ready；stats 独立 loading/error，重试不重载整页。建立 KB id/请求序号归属，切换/卸载和迟到统计/文档响应不得污染新页面；迟到编辑成功可更新正确列表记录，但不覆盖另一 KB 详情。删除与后续刷新分别反馈，刷新重试只读失败数据，不再次删除。

编辑打开时按 editData/id 回填，关闭复位仅影响本次表单，重开同一对象仍为编辑；提交捕获目标与载荷、防重复、失败保留输入。上传按待选文件快照逐个提交，移出已接受项、保留失败及接受不明项；202 缺 taskId/响应丢失明确“接受状态未确认”，提醒核查文档，禁止静默补发。FAILED 的再次上传可能产生新任务，不宣传为原任务安全恢复；RECONCILIATION_REQUIRED 明确需核对，不自动重放。

已知 taskId 查询失败保留文件名与最后状态，恢复只 GET 同 id；切换/卸载停止本地轮询，不声称服务端已取消。刷新后只读文档状态，不虚构 taskId/进度或恢复能力；未知状态中性展示，不降为成功。终态和删除后的文档/统计/KB 列表刷新分别处理错误，避免局部错误抹掉确认事实。

问答导航校验 kbId 正整数与可访问列表；加载失败可重试，参数非法/不可访问不回退到其他 KB；用户显式另选后按其选择。ChatPanel 在新问答消费参数，不自动发请求，历史仍只读；R5 terminal/parser、来源资格、同步默认和成功历史事件保全。

## 文件与停止边界

批准后允许：正式 KnowledgeBaseList.vue/KnowledgeBaseDetail.vue；KBCreateDialog.vue/KBStatsPanel.vue/KBCard.vue（确需正式复用时）；DocUploader.vue/DocProgress.vue/DocList.vue；stores/knowledgeBase.ts 的请求归属/刷新；ChatPanel.vue 仅 KB 导航预选/校验；新增纯客户端 utils/kbPresentation.ts、utils/uploadQueue.ts；utils/taskPoller.ts 仅必要查询健壮性修复，不改后台任务语义；现有 tests/knowledge-base.test.mjs、tests/task-polling.test.mjs、tests/qa-history.test.mjs 按受影响范围扩展、新增 F4 测试与 tests/preview.mjs 封闭 fixture；本 change、ACTIVE_TASK、总规划、AGENT_LOG 追加。

优先局部 CSS。必须改 global.css/variables.css 时先登记影响并覆盖首页/settings/login/KB/history/chat。禁止改 prototype、旧未路由 KB 页面、Java/API/DTO、auth/request/token、useSSE/sseParser/qaPresentation、后端默认行为/模型/配置、依赖/锁文件、accepted specs、学习/本地文件、C19/C20；不清理无关重复组件。接口不足或需新恢复/取消能力时先修订范围，不从 UI 扩展后端。

## 验证方案

规划检查结构、requirement/scenario、决策三行、路径/链接/whitespace；CLI 不可用仅报告替代检查。实施前 Preflight，行为回归覆盖编辑重开/描述清空/防双击、失败保留/成功不重发/接受不明、轮询同 id 单 job/恢复仅 GET/迟到结果、跨 KB 统计、删除后局部刷新失败、导航无误选及历史只读。运行 `node --test tests/*.test.mjs` 与含 vue-tsc 的正式 build；npm 缺失时依次调用已声明 vue-tsc/Vite 入口，不新增/升级依赖。

preview 使用 configFile:false、独立 localhost、无真实 proxy。F4 独立场景按白名单允许内存 CRUD/upload/status 模拟，其他未知 API/动作 fail closed；multipart 仅解析合成测试最小字段、限制大小/数量、不记录正文/凭据、不执行解析/embedding。模拟状态不表述为真实后台结果，不放开原有未知写请求；脚本语法/拒绝规则单独验证。

实际 CSS 视口 1440×900、1024×768、390×844 × dark/light 六组，每组列表/网格/详情有文档/创建/编辑/任务进行中或查询错误恢复至少六态，共至少 36 张；Demo 列表/网格/详情参考至少 18 张。补充 loading/error/empty/filtered-empty、详情403/404、统计/文档局部错误、混合上传失败/接受不明、三种任务终态/文档五态、删除确认/拒绝/局部刷新失败、长内容。核对稳定状态、图像/DOM尺度与哈希并查看图像，记录契约适配，不宣称像素匹配率。

交互检查 Tab/Enter/Space/Escape、焦点返回、菜单/弹窗、真实浏览器文件选择/多文件/拖放及混合失败、表格自身滚动/窄屏堆叠；兼容复核首页/settings、F3 答案/来源、历史只读与 login。合成、视觉、真实 API、真实 provider 与质量评测分层报告。

真实链路单独授权：锁定专用 KB/合成文件/问题与运行时配置，披露预算及实际服务端重试；索引服务已有应用重试，不能把前端一次发送说成 provider 零重试。授权后验证创建→上传→COMPLETED→文档/统计→指定 KB 问答→来源→历史回读，记录实际 IDs/调用/错误/persistence，凭据脱敏；不改 prompt 迎合问题。真实删除/清理另按明确目标授权；未执行则闭环任务未完成。

## 2026-10-02 归档接受边界

用户在已披露余项后要求归档F4，本次将接受范围固定为四项客户端delta、66/66测试、vue-tsc/Vite与已完成的36张矩阵及交互证据。原完整业务链路目标不作通过声明：拖放、真实预算/闭环/清理移交技术债及tasks延后节，未执行状态保留。归档阶段仅允许本change、rag-system baseline逐字接受、ACTIVE_TASK、总规划、technical-debt和AGENT_LOG追加；实施阶段的源码/API/provider/受保护配置边界保持不变。真实业务调用预算仍0，不因归档扩大。

## 决策记录

### 决策 1：正式落点
- **面临的选择**：改正式 KnowledgeBaseList/Detail；迁移旧 KBListView/DetailView；另建 Demo 前端。
- **选了哪个 + 为什么**：沿用正式组件与现有 client/store/poller，减少请求分叉。
- **放弃的代价**：旧页面需要重复接线；第二套前端产生授权、任务与数据状态分叉。

### 决策 2：字段与搜索
- **面临的选择**：真实字段及本地网格/筛选；照搬 Demo 混合资料和全部 KPI；新增跨库搜索/大小/趋势接口。
- **选了哪个 + 为什么**：真实字段与三项现有统计，不额外请求；其余 REST 字段/契约缺失。
- **放弃的代价**：mock 迁移会伪造数据/就绪状态；新接口扩大后端和 baseline 变化，无法隔离前端验收。

### 决策 3：上传失败
- **面临的选择**：全清空；保留失败/不明项、移出已接受项；静默自动重传。
- **选了哪个 + 为什么**：保留待处理文件，由用户明确提交，避免成功项重复与隐式调用。
- **放弃的代价**：全清空丢恢复上下文；自动重传在响应丢失时可能重复任务/embedding，扩大调用范围。

### 决策 4：任务恢复边界
- **面临的选择**：仅恢复已知 taskId 查询；本地持久化映射；新增服务端列表/重放接口。
- **选了哪个 + 为什么**：当前页面内恢复查询，跨刷新只看文档状态；现有接口无法重新查询文档关联任务。
- **放弃的代价**：本地映射可能跨身份/过期并误报恢复；新接口扩大权限/后台恢复契约，本方案保留跨刷新限制。

### 决策 5：问答入口
- **面临的选择**：仅跳聊天后手动再选；路由 kbId 校验预选；详情自动 ask。
- **选了哪个 + 为什么**：校验后预选减少错库，保留用户发送动作，历史忽略该参数。
- **放弃的代价**：手动再选增加错库风险；自动 ask 带来未披露调用/query/history 副作用。预选需新增路由错误回归。

### 决策 6：弹窗与刷新
- **面临的选择**：复用 Element Plus、局部修复；新增弹窗/状态框架；遇错整页重载。
- **选了哪个 + 为什么**：现有组件加 KB 身份归属，局部恢复，服务端成功后更新相关事实。
- **放弃的代价**：新框架扩大依赖/样式回归；整页重载可能丢任务/输入并掩盖局部错误。

### 决策 7：验收与范围
- **面临的选择**：合成/视觉先行、真实闭环另授权；沿用旧授权跑真实上传/删除；以 fixture 代替完整闭环。
- **选了哪个 + 为什么**：先验证客户端，再按当前模型/载荷/预算取得专用授权；F1/F5/取消/后端/质量评测另立范围。
- **放弃的代价**：旧授权不覆盖新数据及上传副作用；fixture 不能证明 provider/persistence，本方案完整闭环仍需真实证据。

### 决策 8：客户端阶段归档与余项移交
- **面临的选择**：继续保持整个F4活动直到取得真实闭环及拖放证据；按当前客户端验收范围归档并将未验证目标移交技术债。
- **选了哪个 + 为什么**：选择客户端阶段归档与债务移交，响应用户在知悉余项后提出的归档要求，四项客户端契约已有工程/合成/视觉证据且不要求本轮扩大外部调用。
- **放弃的代价**：继续等待会使已交付客户端切片持续占用唯一活动指针；选择归档则暂时缺少真实链路和拖放证据，必须登记后续目标并禁止把本次结论描述为完整真实F4闭环。
