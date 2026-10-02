# F4 工程与阶段验证记录

日期：2026-10-02。起点 HEAD：`be74d3902b4d7af4f3b932cc7a1c3c31bafbe281`。用户已批准实施及本轮 Agent commit；本文不是完整 F4 接受或归档记录，提交时执行 hash 由 Git 给出，日志按下一次写操作补录。

## 结论与证据边界

| 层次 | 状态 | 证据与限制 |
| --- | --- | --- |
| 工程 | confirmed | 客户端切片实现、66/66 Node测试、vue-tsc/Vite正式build通过 |
| 封闭合成 | confirmed | 127.0.0.1:5188独立fixture，无proxy；CRUD/upload/status只在内存模拟；接受不等于索引完成 |
| 视觉 | partial | 六组五态30张正式页、18张Demo参考、18张稳定补充图；另1张含删除按钮loading动画的确认图，未计稳定矩阵 |
| 浏览器上传 | unknown | filechooser已经触发且multiple=true；setFiles被Chrome扩展文件权限拒绝，未实际选择/提交文件；用户表示将启用，尚未确认已启用 |
| 本轮真实业务/API/provider | unknown / 未执行 | 调用0，真实KB/文档/query/history写入0；不能用fixture证明持久化、embedding或generation |
| 最终接受/归档 | planned | tasks3.3/3.4及真实闭环4.x保留未完成；baseline不变，ACTIVE仍指向F4 |

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

正式矩阵：CSS1440×900、1024×768、390×844，各深/浅两组；每组list/grid/detail/create/edit五态。全部30张稳定帧已查看联系表，另查看桌面详情、窄屏编辑原图。18张Demo参考和全部补充图也逐组查看；不报告像素匹配率。六张任务进行中/查询恢复图尚缺，原36图目标不改。

Chrome DPR1.25，初次physical1440×900仅得到CSS1152×720，校准探索图排除。最终通过physical1800×1125/1280×960/488×1055取得上述CSS尺寸、scale=1、页面overflow=0；390截图实际390×843，底边1px取整，与DOM390×844区别单列。截图清单中的`stable=false`仅为删除确认期间背景loading动画，已查看原图，不计稳定矩阵；早期表单过渡图已重采。

对齐：页头、工具栏、列表/网格、hero、文档主区/信息侧区与主题节奏；契约适配：实际三项统计替代Demo大小/命中/趋势；文档五态、已加载筛选、服务端授权说明替代Demo混合全库/mock配置。窄屏正式页不照搬Demo竖排标题和压缩工具栏。冻结Demo在390首页打开侧栏控件不可见，先桌面进入页面再缩窄取参考，不回改原件。

浏览器已确认：创建成功且不重复；同对象编辑重开与描述清空；列表筛选无结果；五态文档和文档筛选无结果；403/404详情、统计/文档独立错误；删除取消保留、拒绝保留、成功后局部刷新失败且只读重试；指定901预选、999不回退；`/chat/901?kbId=999`历史仍显示记录KB且发送/选库禁用；来源Enter展开；Tab从名称到描述、Escape关闭及返回入口、窄屏表格ArrowRight滚动（clientWidth319/scrollWidth407）。首页深浅、设置深色、F3来源/历史兼容图已查看。

尚未完成：浏览器文件选择、多文件、拖放、混合上传失败/接受不明、任务终态及查询恢复视觉；loading动态态未单独截图。Chrome上传需用户手动启用ChatGPT扩展“允许访问文件网址”，不使用DOM注入或绕过权限。当前合成登录态访问/login自动跳转，login未单独复验；未改其代码或全局样式。未使用真实移动设备验证触屏。

## 范围、安全与剩余风险

Demo四项blob与起点一致；只改design允许的正式KB/文档组件、KB store、纯客户端utils、ChatPanel导航、测试/preview及本change治理文件。无后端/API/DTO/auth/request/token/useSSE/sseParser/qaPresentation、模型/评测/依赖/锁文件、prototype、accepted specs、本地配置、学习文件或其他页面业务改动；日志纯追加，提交仅列出的计划内文件。没有push/PR/发布/部署。

沿用R5已接受的KB17指定ANSWER证据只说明原契约路径，不证明本轮上传闭环或完整异常矩阵。真实provider、持久化、C19/C20及质量收益未验证；Java/评测未改，跳过Maven/Python。OpenSpec CLI未发现，不安装，文件级结构/链接检查不冒充官方validate。

余项保留：浏览器上传权限及任务矩阵、专用真实闭环授权/执行、最终接受/归档、跨刷新任务恢复接口缺失、既有大chunk与截图底边1px限制。取消、原文预览/下载、全量搜索、F1 logout/F5与后端能力均为out_of_scope。

[proposal](proposal.md) · [design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md)
