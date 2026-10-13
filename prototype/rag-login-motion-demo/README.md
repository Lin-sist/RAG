# RAG 登录动效独立原型

Type B 静态视觉实验，2026-10-12。正式前端和旧 Demo 均不修改，F5 活动任务不变。本目录不是第二套正式前端，不进行身份认证。

## 打开

直接打开 `index.html`，或使用本地静态服务：

```powershell
python -m http.server 8767 --bind 127.0.0.1 --directory prototype
```

访问 `http://127.0.0.1:8767/rag-login-motion-demo/`。页面仅加载本目录的 HTML/CSS/JS/SVG，不使用外部字体、CDN、API 或新增 npm 依赖。静态服务以 `prototype/` 为根，允许显式打开原版对照。交互偏好只保留在内存，表单不写 storage、不发送请求，演示后清空输入。请使用演示信息。

## 本轮范围

设计读法：保留现有登录页构图的品牌动效实验，使用原生 CSS 与系统字体。设计变化/动效强度/信息密度取5/6/3；变化集中在右侧曲面，标题、词标和表单保持原有主线。

- 保留既有登录页的品牌、双列布局、衬线标题、用户名/密码顺序；默认黑白，可切米白。
- 右侧以原生 WebGL1 的单次片元绘制，生成两片连续变形的曲面明暗场；浅投影与弯曲为二维程序化近似，不是实际三维模型。
- 固定网格以细点、交叉或字符显示明暗。字符字形由本机等宽字体绘制成图集，未生成大量 DOM 节点。
- 暂停/恢复；降低动态效果时静态；页面隐藏或图形离开视口停止调度；上限30次绘制/秒、DPR上限1.5及1200像素画布边长。
- WebGL 不可用、shader 编译失败或上下文丢失时显示原始静态 logo；恢复后重新初始化。`mount()` 返回 `destroy()` 供后续组件生命周期使用。
- 移动端先展示完整标题与表单，动效放在其后；所有纹理和暂停按钮可键盘操作。

## 参考与实现归属

视觉参考：[Praveen Kumar 的 X 动效](https://x.com/praveenisomer/status/2105983637615526301)。未取得作者源码，不宣称与原作实现相同或逐帧复刻。

现有版式来自 `../chatgpt-ui-demo/login-dark.html`；品牌路径来自 `../logo-designer/logos/export/icon.svg`。动态轮廓是基于该品牌方向的新程序化近似，静态与顶栏保留原始路径。

技术研究参考：[Paper Dithering](https://shaders.paper.design/dithering)、[ditherwave](https://github.com/sahilsaini5/ditherwave)。本轮未安装、复制或引入上述库，shader 和生命周期代码为本轮独立实现。

## 后续接入边界

本原型先用于动效与浏览器验证。正式迁入 `rag-frontend/` 时，复用 `AuthLayout.vue`、`LoginView.vue` 的真实登录和主题契约；动画独立组件化。引入新依赖或扩大为跨模块工程须按 AGENTS.md 单独判定 Type C，不接管当前 F5。

本轮验证记录在 [VALIDATION.md](VALIDATION.md)；源码与视觉/运行验证分开报告，不把原型表单当作真实认证证据。
