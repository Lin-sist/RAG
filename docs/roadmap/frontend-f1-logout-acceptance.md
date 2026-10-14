# F1 退出修复验收（2026-10-14）

Report status：`CLIENT_VALIDATED`。用户要求先接受归档F5，再继续F1退出修复。按总规划中的Type B已有契约内缺陷处理，ACTIVE_TASK保持IDLE；默认用户手动提交，Commit: pending。

## 修改与能力分类

- confirmed（客户端）：正式侧栏复用useAuth.logout，先发送携带当前身份的单次POST `/auth/logout`，再清空本地access/refresh及userInfo，replace至登录页。请求期间按钮禁用且显示“正在退出…”，同一会话跨composable调用共用一个请求。
- confirmed（客户端）：超时/网络/401/429/503及HTTP200中的失败envelope均不自动刷新或重发；清理本地后提示“本地登录信息已清除，服务端退出未确认。”。旧退出响应校验sessionRevision，不能清除新登录或跳转其页面；已有refresh迟到不能恢复退出会话。移动抽屉的滚动/inert释放仍沿用既有逻辑。
- unknown：本轮真实服务端令牌黑名单、Redis会话删除、持久化及真实认证异常路径未验证。服务端成功确认仅限当前退出接口的既有语义，不推断所有设备/所有凭据撤销。
- out_of_scope：账号后端/API/DTO/权限契约、登录动效集成、其他旧组件清理、F2/F6/C19/C20、真实业务及provider调用。

## 验证

- `node --test --test-timeout=20000 tests/logout.test.mjs`：8/8 PASS，真实useAuth/store/API/requestClient加合成Axios adapter，覆盖凭据/双token清理、四类失败、业务失败envelope、共享请求、新会话与迟到响应、跨会话请求、迟到refresh、侧栏重复点击/抽屉关闭与提示。
- `node --test --test-timeout=20000 tests/*.test.mjs`：90/90 PASS，含原82项。首次新增测试的路由隔离失效、既有shell fixture缺window.navigator导致运行失败；补正确的虚拟路由与ElMessage边界后全套通过，失败运行不计通过。
- 正式build等价顺序：`node node_modules/vue-tsc/bin/vue-tsc.js -b` → `node node_modules/vite/bin/vite.js build`，两者PASS；保留既有大于500kB chunk警告，无依赖/锁文件变更。
- `node --check tests/preview.mjs` PASS。专用f1-success/f1-failure仅内存、configFile:false、无proxy，只有带F1场景cookie与合成bearer的POST可返回合成结果；错误方法/缺场景/缺bearer/refresh/未知写入5项HTTP拒绝检查PASS。
- 实际Codex内置浏览器/CUA：桌面暗色合成场景，点击退出时disabled等待态、成功后登录页；失败场景用Enter触发，503后清理本地并显示未确认提示，重新访问/chat跳回/login?redirect=/chat。服务器内存记录两场景各一个POST且authenticated=true，无自动重放。单元测试覆盖网络超时和401/429，其余浏览器视口/真实触屏本轮未重做。

图像保存在当前线程visualizations目录，3张截图已查看：

- [成功登录页](C:/Users/Lin/.codex/visualizations/2026/10/14/01a138a8-6928-70b3-b103-683ed642ae6c/f1-success.png)
- [禁用等待态](C:/Users/Lin/.codex/visualizations/2026/10/14/01a138a8-6928-70b3-b103-683ed642ae6c/f1-pending.png)
- [失败未确认提示](C:/Users/Lin/.codex/visualizations/2026/10/14/01a138a8-6928-70b3-b103-683ed642ae6c/f1-failure.png)

浏览器专用连接尝试因信任桥不可用而失败，改用可用CUA通道完成上述核验；未将专用通道失败称为页面失败。

## 跳过、安全与风险

真实auth/backend/provider/ask/upload/feedback/delete调用、业务写入及费用均0；不读取或输出真实凭据，无真实provider/model/timeout/retry证据。Java/Python评测未修改，不运行Maven/Python评测；F5未改源文件，复用既有浏览器矩阵且本轮73图hash一致。OpenSpec CLI未安装，仅报告文件级替代检查。

本轮业务源改动仅AppSidebar.vue与useAuth.ts；另改两个测试和封闭预览。后端/API/DTO/认证store/requestClient/SSE、模型/评测、依赖/锁文件、独立原型/冻结Demo及受保护配置未改，无暂存/commit/push/PR/部署。

沿用请求client的30秒timeout；网络故障最多等待该超时后本地清理，服务端撤销结果可能不明。真实认证、移动浏览器及物理触屏验收留待专用目标授权；既有大chunk保留。

建议提交：`fix(前端): 修复F1退出接线及会话竞态保护`。
