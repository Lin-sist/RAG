# 正式登录双翼动效设计与事前闸门

## 当前事实与冻结参考

2026-10-14，F5归档提交f595099，F1退出提交9061f1c；无其他活动change。AuthLayout原有直接root/localStorage逻辑已按Type B改为唯一theme store，独立验证不并入本change。真实LoginView/useAuth/requestClient与路由保持既有契约。

参考为prototype/rag-login-motion-demo最终已确认版本，迁移前复核Git blob：logo-motion.js `920f2a3504e230aa9accccae7d6081704574a6f2`；login.css `a56b7e602ee5ff290430d4d45f4e9f7d65ffb1b6`；assets/logo.svg `23d5dd8414bc8aed3442e629413fd06db0cb15fa`。原型读取作参考，不修改或从其demo.js迁入假登录流程。

## 差异表与保留/适配/延后

| 区域 | 当前正式页 | 本次处理 |
| --- | --- | --- |
| 顶部词标/标题/真实表单 | AuthLayout/现有LoginView | 保留已有主线与真实登录；不复制原型演示表单 |
| 右侧装饰 | LoginPixelArt的16×16多形轮换 | 接已确认双翼曲面、拖尾、三纹理及透明切换；旧组件不顺手删除 |
| 黑白/米白 | 统一theme store（Type B修复后） | 从有效主题刷新绘制，保留手动选择与system，不新增主题所有者 |
| 动效开关/暂停文字 | 原型最终已移除 | 正式页同样不添加；尊重系统reduce自动静态 |
| 不支持/上下文丢失 | 原型静态降级 | 组件装饰层内用同源静态SVG；canvas隐藏/aria-hidden，不增加表单焦点 |
| 账号/注册/改密/工作台 | 无当前迁移契约 | 延后；不建立假入口或新API |

## 组件与状态边界

新增LoginLogoMotion.vue和utils/loginLogoMotion.ts。renderer导出mount(canvas, options)与refresh/destroy最小公开接口，不挂window.RagLogoMotion，不读localStorage、表单、token、history或KB。将已确认shader字符串原样保留，仅补模块/类型与显式故障收敛；转换后的字符串核对参考hash，视觉不按重画处理。内部debug data属性仅用于合成验证，不进入用户流程文案。

Vue在onMounted创建renderer，在主题watch的nextTick刷新色彩，在onUnmounted必定destroy；任一步失败仅显示静态品牌图。destroy幂等，清RAF、ResizeObserver/IntersectionObserver、media/document/canvas listeners以及有效WebGL资源。无WebGL/高精度不支持/shader失败/上下文lost均静态，restored可重建并复位当前静态/运行状态，失败保持降级。ResizeObserver/IntersectionObserver不可用时也静态；reduce切换在透明间隙内仍重绘完整帧，隐藏/离屏不推进有效时间。所有异常不输出凭据、URL或业务内容。

继承原型单pass、30fps绘制上限、DPR≤1.5及边长约1200px预算；响应式大小由组件容器CSS控制。canvas仍方形，本次不裁切改变几何或缩放shader比例；整体尺寸适应原AuthLayout右侧/窄屏下方。父布局只新增--art-ink/--art-accent两套变量与装饰组件，未授权视觉细节不扩展到表单/全局CSS。

## 允许文件

- rag-frontend/src/layouts/AuthLayout.vue
- rag-frontend/src/components/common/LoginLogoMotion.vue（新增）
- rag-frontend/src/utils/loginLogoMotion.ts（新增）
- rag-frontend/src/assets/login-logo.svg（由冻结SVG另存，源不改）
- rag-frontend/tests/login-motion.test.mjs（新增）
- rag-frontend/tests/preview.mjs（仅封闭登录失败/等待fixture，若需要）
- 本change下proposal/design/tasks/specs/rag-system/spec/acceptance/visual-evidence，.ai/ACTIVE_TASK.md、.ai/AGENT_LOG.md与docs/roadmap/frontend-demo-backend-integration-plan.md、technical-debt.md（仅相关记录）

禁止后端/API/DTO/store/requestClient/路由/SSE/评测/provider/依赖/lock、受保护配置、prototype与旧Demo修改。LoginView行为不改；发现表单契约缺口停止并另定范围，不借本装饰change修认证。

## 验证与停止条件

- 聚焦组件/renderer：挂载→reduce→主题刷新→失效降级→恢复→卸载→重挂；定时器与监听清理、静态失败不抛到表单。生命周期测试模拟资源只作合成证据，WebGL实际效果另由浏览器证明。
- 工程：node --test --test-timeout=20000 tests/*.test.mjs；vue-tsc -b后Vite正式build，原无关成功验证按AGENTS复用。不升级/安装依赖。
- 视觉：1440×900、1024×768、390×844 × light/dark，6张正式完整帧与6张冻结原型参考；另检查旧形态淡出/透明间隙/新形态淡入、reduce/静态降级、窄屏首屏表单/登录按钮与横向溢出。采证在加载/主题/resize稳定后按实际画面确认；不报告像素匹配率。
- 交互：真实浏览器中合成登录空输入/失败/等待防重复/成功导航/F1退出返回、主题往返、键盘可达；装饰不抢焦点。封闭预览未知请求fail closed。登录成功只称合成客户端流程，不测真实凭据。
- 停止：需新依赖、业务API/数据/真实auth/provider、原型几何优化或不可修的构建/范围异常时停受影响项。缺浏览器/GPU能力记录unknown，不以mock代替实际WebGL。
- 接受：独立报告confirmed/partial/planned/out_of_scope/unknown；用户接受及归档另获授权，未通过完整change不改IDLE。其后F6单独做全页面矩阵和剩余项分流。

## 决策记录

### 决策1：采用哪个装饰版本
- **面临的选择**：保留旧网格；迁入已确认双翼；重新画一版双翼。
- **选了哪个 + 为什么**：拟迁入已确认双翼，沿用最新用户认可的曲面/拖尾/过渡，待用户事前闸门确认。
- **放弃的代价**：旧网格无法承接本轮品牌探索；重画会再次引入审美偏差与无关优化。

### 决策2：怎样进入Vue产品代码
- **面临的选择**：iframe嵌入完整原型；全局script加载；显式TypeScript模块与Vue组件。
- **选了哪个 + 为什么**：拟用显式模块与组件，装饰生命周期归Vue、认证仍归既有表单，待用户事前闸门确认。
- **放弃的代价**：iframe会带入假表单/重复主题与布局；全局script会留下跨路由共享状态和不清晰销毁责任。

### 决策3：主题与故障处理
- **面临的选择**：复制原型dataset主题和独立存储；消费已有有效主题并静态降级。
- **选了哪个 + 为什么**：消费已有theme store且故障只降级装饰，维持唯一所有者与可登录路径。
- **放弃的代价**：独立主题会重新制造登录/侧栏不一致和存储异常；让装饰故障阻断表单会扩大认证可用性风险。

### 决策4：本轮是否扩展账号与真实证据
- **面临的选择**：装饰迁移同时改账号能力/真实auth；仅客户端迁移，真实链路另披露授权。
- **选了哪个 + 为什么**：仅客户端，沿用零真实预算，当前缺口可用封闭证据验证。
- **放弃的代价**：同时扩展会改变权限/API契约与副作用，失去单一可验证切片；账号、真实撤销与业务闭环继续保留unknown债务。

[proposal](proposal.md) · [tasks](tasks.md) · [spec delta](specs/rag-system/spec.md) · [原型](../../../prototype/rag-login-motion-demo/README.md)
