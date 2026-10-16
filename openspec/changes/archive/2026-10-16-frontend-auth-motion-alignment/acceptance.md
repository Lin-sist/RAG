# 正式登录双翼动效客户端验收

2026-10-14。Report status：`CLIENT_VALIDATED_AWAITING_ACCEPTANCE`。用户已明确批准具体接入方案，沿用Agent本地提交授权；客户端实施与本轮验证完成，等待接受，baseline/归档/push/PR/部署未授权。采证起点HEAD为2259b0e；visual-evidence保留采证时未提交状态，不将该元数据改成后续提交身份。

## 交付与能力分类

| 能力 | 分类 | 本轮事实与边界 |
| --- | --- | --- |
| 正式/login双翼装饰 | confirmed（客户端） | LoginLogoMotion组件与原生TypeScript renderer，保留曲面/拖尾/三纹理与背景过渡；两段shader字符串及静态SVG与冻结原型一致。无全局RagLogoMotion、操作栏或原型假表单。 |
| 主题与真实表单保全 | confirmed（客户端合成范围） | 使用已有有效主题；标题/词标/真实LoginView和API不改。浅深切换、键盘、合成校验/拒绝/等待/成功/F1退出返回通过；装饰不读凭据或业务数据。 |
| 生命周期与故障降级 | confirmed（本轮范围） | reduce在过渡中变完整静态、离屏停有效时钟；WebGL/所需能力不支持、编译/资源失败静态；实际上下文loss/restore与路由卸载/重入通过。隐藏状态和资源释放次数由合成测试覆盖，不宣称真实OS后台/显存或长时功耗已验。 |
| 视觉与响应式 | confirmed（指定矩阵） | CSS1440×900、1024×768、390×844深浅六组，提交按钮首屏可达、横向溢出0；保留原表单尺寸/间距，不为对照复制原型演示控件或说明。 |
| 真实认证/硬件与完整Demo | unknown / out_of_scope | 无真实auth/backend/provider请求；Chrome软件WebGL不能证明物理GPU/手机/Safari/Firefox或长时性能。F6、F4/F5真实闭环及清理、账号/搜索/多轮/工作台/C19/C20独立保留。 |
| 接受与归档 | planned | 仍ACTIVE，待用户接受；不自动追加baseline或归档。 |

## 工程与行为

- 新增9项动效测试，真实Vue/store与renderer加合成WebGL/observer/RAF环境；覆盖冻结shader/SVG、十类渲染故障、缺能力/部分setup失败、单时钟与透明切换、reduce、隐藏/离屏、context恢复、主题刷新、卸载后回调和重挂。
- `node --test --test-timeout=20000 tests/*.test.mjs`：102/102 PASS（原93项+新增9项）。初次Vue测试在app._instance尚未赋值时访问ref失败，改用当前组件instance构造测试引用后重验；失败运行不计通过。
- `node node_modules/vue-tsc/bin/vue-tsc.js -b` → `node node_modules/vite/bin/vite.js build`：正式build等价顺序PASS；既有大于500kB chunk警告保留。提升Preflight可见npm，但受限执行PATH的npm.cmd不可用；使用已有direct Node入口完成，不安装/升级依赖。
- `node --check tests/preview.mjs` PASS。封闭预览configFile:false、127.0.0.1:5188、无proxy；新增登录401/3秒等待fixture与只读五资源白名单原型对照。未知文件404、原型写入/refresh/未知API写入405四项HTTP拒绝检查PASS。所有状态仅localhost内存。

## 浏览器与图像

专用browser-client连接因信任桥不可用失败，后用已安装Chrome155.0.8059.41、隔离profile及SwiftShader完成实际WebGL测试；无依赖下载。完整浏览器复验41/41 PASS，定向流程9/9及状态稳态补采3/3分别PASS；page异常和外部请求均0。内置浏览器/CUA另实际观察webgl及主题切换，未将其视口算入六组正式矩阵。

- 完整细点→交叉→字符→细点；旧形态淡出、透明间隙切换、新形态部分透明再完整显示均核对。reduce在过渡中开启仍完整静态且跨6.3秒冻结，恢复继续。离屏停时钟、返回不跳过后台时间；真实WEBGL_lose_context降级与恢复通过。
- 无WebGL能力时仍可输入/提交既有表单。空输入0POST，401合成拒绝保留输入；等待期间重复Enter仅一次POST。成功离页后原canvas不连接且frame停止；F1退出回到新renderer、主题一致、主题按钮Enter可达，装饰无tab stop。完整干净浏览器复验只发2个合成login POST和1个合成logout POST，独立复验请求单独记录，不当作真实认证。
- 首轮完整脚本在最后菜单定位超时：整页刷新使不持久化userInfo恢复中性名称，脚本仍按合成用户名定位。保留initial-check-browser.cjs/initial-results.json；通过只读GET设fixture cookie并保持SPA身份校正后，定向和完整复验通过；不修改产品认证以迁就测试。
- 6张正式矩阵、6张冻结参考、14张补充文件，共26文件/25个不同hash已逐图查看并核对SHA-256/尺寸。context-lost与fallback静态画面相同，文件分别绑定两个已验证场景，不称26个独立画面。
- 登录拒绝/退出返回的初拍说明图处于校验或主题过渡早期，重采稳定图替换正式引用；旧login-rejected.png/flow-return.png仍保留但排除。正式矩阵等待完整帧并用reduce固定后采证；矩阵的静态完整帧不代替独立动态检查。窄屏fullPage PNG可高于844px，DOM按钮仍在首屏，不混淆viewport与整页尺寸。不报告像素匹配率。

完整清单见[visual-evidence.json](visual-evidence.json)，本机图像/执行脚本目录：`C:/Users/Lin/.codex/visualizations/2026/10/14/01a138a8-6928-70b3-b103-683ed642ae6c/login-integration/`。正式原图和报告保持在本机，需保留目录以复核，不作为产品运行资源提交。

## 范围、跳过与剩余风险

产品源/资源仅AuthLayout.vue、LoginLogoMotion.vue、loginLogoMotion.ts及login-logo.svg；测试仅login-motion.test.mjs/preview.mjs；其他变更为本change/活动指针/总规划/日志。保全LoginView/useAuth/API/DTO/theme store/requestClient/路由/SSE、后端/模型/评测、依赖/锁文件、prototype及旧Demo、受保护配置。baseline和其他归档不变，日志只追加。

真实auth/backend/provider/ask/upload/embedding/rerank/judge/feedback/delete调用、业务数据写入及费用0；无真实provider/model/timeout/retry结果。Java/Python评测未改不跑Maven/Python评测；OpenSpec CLI未安装，不安装且不称官方validate通过，使用结构/链接/requirement与场景/冻结hash/范围/whitespace替代核验。

程序化品牌近似保持，不证明真实数据筛选；静态fallback只覆盖装饰。物理设备/GPU、真实OS后台、其他浏览器及长时功耗仍unknown，软件WebGL与合成监听清理不作这类保证。主bundle大chunk继续保留。F6整体验收与真实业务债务未扩入。

建议/本轮提交信息：`feat(前端): 接入正式登录双翼动效`。

[proposal](proposal.md) · [design](design.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md)

## 接受与归档补录（2026-10-16）

当前状态：`ACCEPTED_CLIENT_SCOPE_ARCHIVED`。用户确认登录动效已成功接入并明确要求归档、推进下一规划。实施提交62961a2619d0ad88b42191d7bbbaf116c6552c3a；本轮重验26图与4份报告SHA-256全部一致，复用102/102、含vue-tsc正式build及41/41浏览器结果。原始visual-evidence的采证HEAD/状态/时间保持不变，上文早期“待接受”仅记录当时事实。2项delta/6场景逐字并入baseline。物理设备/长时性能与真实认证仍unknown，F6及F4/F5真实债务独立；真实业务调用0。沿用本地提交授权，无push/PR/部署。
