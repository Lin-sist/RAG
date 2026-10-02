# F4 客户端接受与归档记录

日期：2026-10-02。工程起点 HEAD：`be74d3902b4d7af4f3b932cc7a1c3c31bafbe281`；工程提交：`da4bc681fc12641cd000e31621e6568d87c302ce`；证据提交：`e238ee9ebd2c56cd8c73fc257ff9d9b884a016ba`。用户在上一轮已披露余项后要求“可以为我归档F4”；本次按已交付客户端契约接受归档，沿用本轮Agent提交授权，不声称完整真实F4业务闭环。

## 本次接受范围与余项移交

接受四项spec delta及工程/合成/36张正式矩阵与已完成交互证据；客户端规范不新增provider、API或后端保证，逐字接受进rag-system baseline。原3.4拖放与4.1–4.3真实预算/闭环/清理目标移交[技术债](../../../../docs/roadmap/technical-debt.md#f4-客户端归档后的证据债务2026-10-02)和tasks延后节，保持未验证。当前真实调用及真实业务写入0；用户归档请求不构成真实调用/删除、push/PR/发布/部署授权。

## 结论与证据边界

| 层次 | 状态 | 证据与限制 |
| --- | --- | --- |
| 工程 | confirmed | 客户端切片实现、66/66 Node测试、vue-tsc/Vite正式build通过 |
| 封闭合成 | confirmed | 127.0.0.1:5188独立fixture，无proxy；CRUD/upload/status只在内存模拟；接受不等于索引完成 |
| 视觉矩阵 | confirmed | 六组六态36张正式页、18张Demo参考、25张稳定补充图；另1张含删除按钮loading动画的确认图，未计稳定矩阵；共80项哈希/尺度核验 |
| 浏览器上传 | partial | 用户手动选择后真实浏览器提交，多文件/混合失败/失败重提/接受不明/查询恢复通过；拖放动作未独立确认；API仍为封闭合成 |
| 本轮真实业务/API/provider | unknown / 未执行 | 调用0，真实KB/文档/query/history写入0；不能用fixture证明持久化、embedding或generation |
| 客户端接受/归档 | confirmed | 四项客户端delta接受并归档；本次接受范围任务完成，ACTIVE=IDLE；拖放与真实链路移交债务，不计为通过 |

## 实现与验证

- 列表/网格、已加载范围本地筛选、空/错/无结果；统计独立loading/error/retry且绑定KB身份。
- 每次编辑打开回填、空描述更新、防重复提交；失败保留输入。公开/私有说明遵守tenant与服务端授权，未新增权限模型。
- 详情hero、三项实际统计、文档五态和标题/类型筛选、基本信息及可展开向量身份；窄屏按阅读顺序堆叠。
- 上传每次操作捕获文件快照；只有202且非空taskId才移出待选队列，失败/接受不明保留，不自动重发。503/断网/202缺id明确提醒先核查文档。
- 沿用唯一task poller；查询失败恢复只GET同taskId，不重传上传；终态/删除成功刷新文档、统计、列表，局部读取失败不抹掉确认事实；切换/卸载丢弃迟到结果。
- `/chat?kbId=`只在新问答校验正整数及可访问列表后预选；非法/不可访问/列表错误不回退，用户显式选择优先；历史忽略导航参数、保持只读，R5 parser/terminal/来源资格与F3正文实现未改。

Preflight：Node、Maven、Java、Python可用，npm未发现；沿用已安装且已声明的依赖入口，没有安装/升级。

| 命令（rag-frontend目录） | 结果 |
| --- | --- |
| `node --test tests/*.test.mjs` | 66/66 PASS：编辑重开/失败/防双击、混合结果/不明/卸载、统计竞态/CRUD迟到、删除取消/拒绝/刷新、导航/历史与既有R5/F3/poller回归 |
| `node node_modules/vue-tsc/bin/vue-tsc.js -b` | PASS |
| `node node_modules/vite/bin/vite.js build` | PASS；沿用既有大于500kB chunk提示 |
| `node --check tests/preview.mjs` | PASS |

直接依次执行vue-tsc和Vite，等同package.json既有build脚本，不用单独Vite冒充类型检查。首个新增CRUD测试因Element Plus SSR外部化缺document而失败，修正测试fixture隔离后通过；业务代码未为此修改。

封闭preview显式场景cookie隔离内存会话，最多262144字节的最小multipart合成载荷，单次单file；无真实文件解析/embedding。HTTP检查确认创建201/更新描述清空/上传202和fail.txt400/任务到COMPLETED/删除后6→5；未知或默认写请求405、取消405、任务DELETE405、未知GET404、超限multipart400。此证据不替代浏览器文件交互。

## 图像与交互

截图目录：`C:/Users/Lin/.codex/visualizations/2026/10/01/01a0f822-93b5-76a2-ad64-6e82fce4da68/f4/`。文件名、SHA-256、DOM尺度、主题、overflow、连续帧稳定性见[清单](visual-evidence.json)。图像未纳入Git；保留本轮本地输出。

正式矩阵：CSS1440×900、1024×768、390×844，各深/浅两组；每组list/grid/detail/create/edit/task六态。全部36张稳定帧已查看联系表，另查看桌面详情、窄屏编辑/任务原图。18张Demo参考和全部补充图也逐组查看；不报告像素匹配率。本轮新增六张任务矩阵、四张浏览器上传/恢复补充图和三张login兼容图，共13张；80项名称唯一且SHA-256、PNG尺度与实际文件匹配。

`f4-task-matrix`仅在测试preview中按文件名提供RUNNING/FAILED/CANCELLED及首个GET503后COMPLETED，unknown.txt返回202缺taskId；没有真实后台。该场景只暂停任务区CSS旋转/条纹动画以取稳定帧，不改progress、轮询或File对象，不作为动画行为验收。最初认领页未处于视口控制窗口，错误尺度采图已排除；用公开claimTab接口重新认领后六组尺寸正确。未绕过浏览器权限或注入文件。

Chrome DPR1.25，初次physical1440×900仅得到CSS1152×720，校准探索图排除。最终通过physical1800×1125/1280×960/488×1055取得上述CSS尺寸、scale=1、页面overflow=0；390截图实际390×843，底边1px取整，与DOM390×844区别单列。截图清单中的`stable=false`仅为删除确认期间背景loading动画，已查看原图，不计稳定矩阵；早期表单过渡图已重采。

对齐：页头、工具栏、列表/网格、hero、文档主区/信息侧区与主题节奏；契约适配：实际三项统计替代Demo大小/命中/趋势；文档五态、已加载筛选、服务端授权说明替代Demo混合全库/mock配置。窄屏正式页不照搬Demo竖排标题和压缩工具栏。冻结Demo在390首页打开侧栏控件不可见，先桌面进入页面再缩窄取参考，不回改原件。

浏览器已确认：创建成功且不重复；同对象编辑重开与描述清空；列表筛选无结果；五态文档和文档筛选无结果；403/404详情、统计/文档独立错误；删除取消保留、拒绝保留、成功后局部刷新失败且只读重试；指定901预选、999不回退；`/chat/901?kbId=999`历史仍显示记录KB且发送/选库禁用；来源Enter展开；Tab从名称到描述、Escape关闭及返回入口、窄屏表格ArrowRight滚动（clientWidth319/scrollWidth407）。首页深浅、设置深色、F3来源/历史兼容图已查看。login使用同一封闭服务的独立localhost origin，没有用户登录态，深/浅1440与浅390图已复核，未登录真实账号。窄屏login的scrollWidth比innerWidth小5px，无正向横向溢出。

本轮上传复核：用户手动选择pass.txt/fail.txt后，pass.txt完成且统计刷新至6篇、12向量；失败项保留。用户又选入fail.txt，两个不同File对象均显示为失败项；明确再提交这两个失败项后，pass.txt文档仍只有一份。现有契约没有按文件名去重要求，未因此改业务行为。

用户随后一次选入running.txt/query.txt/failed.txt/cancelled.txt/unknown.txt，Agent点击“开始上传（5个文件）”；四项接受并显示上述任务状态，unknown.txt保留且提示先核查文档。点击“重新查询状态”只恢复原taskId的GET，query.txt完成，文档数维持9、向量数6→12；没有上传重发。取消任务不伪造文档CANCELLED状态，文档仍按现有契约显示PROCESSING。独立HTTP聚焦检查也确认四任务状态、503→COMPLETED、202缺id不生成fixture文档、统计9/12/0及未知写/取消/任务DELETE405。

Chrome自动文件选择仍被扩展文件权限拒绝，chrome://extensions只读核查也被浏览器安全策略拒绝；用户手动选择后完成了网页提交，不再尝试绕过。用户回复“已选择”不足以区分文件选择与拖放，真实拖放尚未独立验证；动态loading未单独截图，未使用真实移动设备验证触屏。登录页代码和全局样式未改。

## 范围、安全与剩余风险

Demo四项blob与起点一致；实施只改design允许的正式KB/文档组件、KB store、纯客户端utils、ChatPanel导航、测试/preview及本change治理文件。归档只更新本change、rag-system baseline逐字接受、ACTIVE_TASK、总规划、technical-debt和日志追加。无后端/API/DTO/auth/request/token/useSSE/sseParser/qaPresentation、模型/评测/依赖/锁文件、prototype、本地配置、学习文件或其他页面业务改动；没有push/PR/发布/部署。

沿用R5已接受的KB17指定ANSWER证据只说明原契约路径，不证明本轮上传闭环或完整异常矩阵。真实provider、持久化、C19/C20及质量收益未验证；本轮没有业务外部调用或真实KB/document/query/history写入。为后续运行准备当前源代码产物，`mvn -q -pl rag-admin -am package -DskipTests`正常依赖下载后PASS；首次沙箱网络拒绝后经平台审批运行成功，不是Java测试通过。未启动后台应用或调用provider；Java/评测未改，跳过Java/Python测试。既有66/66和vue-tsc/Vite成功检查在业务源码/依赖未变时复用，本轮只重跑改变的preview语法与聚焦HTTP检查。OpenSpec CLI未发现，不安装，文件级结构/链接检查不冒充官方validate。

余项保留并移交债务：拖放动作未独立验证、自动文件选择权限仍阻塞、专用真实闭环授权/执行与后续清理。跨刷新任务恢复接口缺失、既有大chunk与截图底边1px限制继续保留。取消、原文预览/下载、全量搜索、F1 logout/F5与后端能力均为out_of_scope。

[proposal](proposal.md) · [design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md)
