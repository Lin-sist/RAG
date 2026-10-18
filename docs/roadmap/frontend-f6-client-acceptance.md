# F6 前端客户端整体验收与分流

初次验证：2026-10-16；手工确认补录：2026-10-18。Report status：`USER_MANUAL_CONFIRMED_WITH_STREAM_EXCEPTION`。普通客户端路径已获用户手工确认，流式问答失败单列待查，见[手工验收补录](frontend-user-manual-acceptance-2026-10-18.md)。登录动效已经接受归档，不自动推进C19/C20。以下工程/浏览器证据和零真实调用统计均为10月16日Agent验证事实，不用于统计用户10月18日自行操作的调用。

## 范围与完成情况

F6按既有能力审计/Type B维护执行，未新增能力、接口、状态机或依赖，无新Type C。检查正式/login、/chat、/kb、/kb/:id、/history、/chat/:id及设置，1440×900、1024×768、390×844×深浅主题共42组；另有15张补充图。结果只覆盖指定视口的可见布局与指定客户端流程，不等于真实业务闭环或全面无障碍认证。

- [x] 登录动效接受归档，2 requirements/6 scenarios逐字进入baseline；26图/4报告hash一致。
- [x] 核对采证起点HEAD62961a2、原干净工作区及工具链；真实业务预算0。
- [x] 七页面×六组矩阵完成，42正式/15补充图已查看；最终57图均测得页面横向溢出0、DPR1、scale1。
- [x] 合成登录/退出、跨页导航、设置键盘循环/焦点归还、来源原生summary展开与错误恢复入口检查完成。
- [x] 分层汇总和真实证据分流完成，受影响链接、逐字baseline、源码范围及diff检查通过。
- [x] 工程按已有Agent本地提交授权保存；实际提交身份以Git及交付回复为准。
- [x] 2026-10-18用户手动验证并确认普通功能可用；流式异常明确保留，真实闭环和C19/C20仍独立。
- [ ] 定位并验证本次`STREAM_FAILED`；不能因普通路径确认而标为全量流式验收通过。

## 本轮修复

| 问题 | 已确认原因与改动 | 验证 |
| --- | --- | --- |
| F6-01：窄屏知识库列表栏目文字、详情返回入口被覆盖 | 侧栏固定按钮占据顶部12–48px；列表24px/详情22px顶边距不足。仅两页面≤768px增加padding-top:60px。 | 列表/详情在390、744、768px共6组，内容top60、按钮bottom48，无重叠/横溢出；两页面六组正式图重采。 |
| F6-02：失效历史详情的错误恢复行被覆盖 | 顶部错误提示没有为同一按钮留空。仅≤768px的messages-container直接错误提示margin-top:60px。 | 390px错误行top60、按钮bottom48，恢复按钮和返回链接可见，Enter返回历史页通过。 |

只改KnowledgeBaseList.vue、KnowledgeBaseDetail.vue和ChatPanel.vue各一条CSS规则。正常消息、历史只读、上传/删除/反馈/API/DTO/auth/request/SSE及登录动效逻辑不改。工具重复插入的等价CSS已去重，最终diff各1行；去重后再次正式build退出0。

## 工程与浏览器证据

- `node --test --test-timeout=20000 tests/knowledge-base.test.mjs tests/kb-upload-experience.test.mjs`：17/17 PASS。
- `node --test --test-timeout=20000 tests/chat-experience.test.mjs tests/qa-history.test.mjs`：14/14 PASS。
- `node node_modules/vue-tsc/bin/vue-tsc.js -b`后顺序执行`node node_modules/vite/bin/vite.js build`：最终exit0，等价项目build，保留既有>500kB chunk提示。Preflight中npm不可见，使用已有Node入口；无依赖安装/升级。
- 其余未变行为复用登录动效阶段102/102及41/41专项浏览器结果，不把它们记成本轮新跑的全套测试。未修改Java/Python评测，跳过Maven/Python评测；OpenSpec CLI未安装，采用文件级检查，不称官方validate通过。
- 实际CUA内置浏览器：42组矩阵、4张744/768px边界、来源展开1张、空/错误/反馈10张，共57张正式证据；[图像清单](frontend-f6-visual-evidence.json)记录hash、尺度、采证起点和三个源码指纹，原图在清单artifactRoot目录。
- 9个最终读取状态：KB空/错误/403/404、历史空/错误/失效详情、我的反馈空/错误；另观察我的反馈正常及loading。加上设置Tab循环/Shift+Tab、Escape与原触发按钮焦点归还、合成登录/退出、失效历史键盘返回，最终12项记录PASS；原始两条异步读取过早检查失败保留，稳定重验通过，不计作12项成功。
- 本轮操作2次合成登录、1次F1合成成功退出，键盘进入知识库详情、返回、历史详情和来源展开已观察；真实用户/凭据未使用。原生summary先按button定位失败，改用实际summary后展开成功，未改产品迁就测试。

## 采证限制与排除项

浏览器专用信任桥不可用，改用可用CUA；首次预览未启动时连接拒绝不计通过。20张早期图保存在initial/并排除：10张KB修复前、2张反馈异步读取未稳定、6张登录主题/完整帧未稳定、1张登录resize合成帧未稳定、1张历史错误修复前。补采图已查看，未将等待selector超时算通过。登录动效全循环/降级专项仍复用已归档证据，本轮不重新宣称全套动效验证。

fullPage不会展开应用内部滚动容器；本轮矩阵覆盖实际可见区域和指定交互，长页底部完整内容/所有滚动位置复用先前专项证据。未做像素匹配率、WCAG全量审计、320px/200%缩放、原生触屏、物理GPU、长时功耗、其他浏览器验证。

## 能力与后续分流

| 分类 | 事实与后续 |
| --- | --- |
| confirmed（指定客户端范围） | 七页面矩阵、主题/键盘指定流程、两类遮挡修复、三源码指纹、正式build和聚焦31项测试通过。 |
| partial | 10月18日用户截图补充同步答案/来源、索引完成与反馈列表证据；F4-LIVE/F5-LIVE完整响应及回读链条仍未补齐，不能称生产级/完整真实业务验收。 |
| unknown | 本次流式失败根因、F4-DROP/F4-CLEANUP、F5-CLEANUP、真实认证撤销/权限/完整持久化及物理设备/长时性能。 |
| planned | 优先定位流式异常；需要Agent真实重放时另披露目标/预算/副作用。C19→C20独立立项/验收，不自动运行。 |
| out_of_scope | 全量搜索/Ctrl+K、多轮/重命名、账号管理、工作台、反馈改写/删除、后端/模型/评测契约变化。 |

封闭预览configFile:false、127.0.0.1:5188、无proxy，所有合成数据只在内存；未知业务写请求拒绝。真实backend/auth/provider/ask/upload/embedding/rerank/judge/feedback/delete、业务写入及费用0，无真实provider/model/timeout/retry数据。临时浏览器页关闭并重置视口，预览停止；原始证据保留。后端、依赖/lock、prototype、受保护配置、.agents和学习目录未改；日志只追加。

建议提交信息：`fix(前端): 归档登录动效并完成F6客户端验收修复`。

[总规划](frontend-demo-backend-integration-plan.md) · [登录动效归档](../../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/acceptance.md) · [技术债](technical-debt.md)
