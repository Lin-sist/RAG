# 独立原型验证记录

2026-10-12。Report status：`PROTOTYPE_VALIDATED`。代码起点与收尾 HEAD：`31ad3f8`。仅验证本轮静态原型，未迁入正式前端；F5 保持 ACTIVE。

## 工程验证

- `node --check prototype/rag-login-motion-demo/js/logo-motion.js` 和 `node --check prototype/rag-login-motion-demo/js/demo.js`：PASS。
- 正式前端 build 的声明顺序：在 `rag-frontend/` 执行 `node node_modules/vue-tsc/bin/vue-tsc.js -b`，再执行 `node node_modules/vite/bin/vite.js build`，均退出0。`npm` 不在当前PATH，使用项目已有入口，无新增/升级依赖。保留既有大于500kB chunk提示。
- 正式前端源文件、依赖、锁文件无改动。未重复运行其82项F5测试，本原型不改变F5代码或契约。
- tracked diff 及新目录文件的 whitespace、HTML/文档本地链接、SVG路径一致性、业务请求/storage调用扫描均通过；执行脚本和文件hash见本机证据。

## 浏览器与视觉证据

本机证据目录：`C:/Users/Lin/.codex/visualizations/2026/10/12/01a12e10-b383-7740-ab28-a05eb38d16c6/login-motion/`。

- 内置浏览器实际 WebGL 渲染、连续动画、暂停和主题切换已查看。其 viewport/screenshot 接口在本轮出现缩放与裁切不一致，早期无 `verified-` 前缀截图不纳入正式视觉证据。
- 使用机器已有 Chrome `155.0.8059.39`，独立临时用户目录、headless、SwiftShader 软件 WebGL；没有安装浏览器或工具。脚本为证据目录下 `check-browser.cjs`，结果为 `browser-results.json`。
- 39/39 浏览器检查 PASS，页面脚本异常0、外部请求0、非GET请求0。覆盖shader实际编译、暂停/恢复、绘制频率上限、纹理切换、降低动态效果及动态恢复、离屏暂停及返回、WebGL上下文丢失/恢复、实例销毁/再挂载、表单空态/焦点/密码键盘操作/输入清空、storage无写入、无JavaScript与WebGL不可用时降级。
- `file:///C:/_01_Code/RAG/prototype/rag-login-motion-demo/index.html` 加载、WebGL与主题切换 PASS；不存在运行时CDN、外部字体或业务API依赖。
- 1440×900、1024×768、390×844 × dark/light，实际CSS视口及DPR1均核对，六张 `verified-{theme}-{width}.png` 已逐图查看。页面横向溢出0，登录按钮均在首屏；移动端图形/控制位于表单后方，允许正常竖向滚动。
- 三种纹理截图、两张间隔2.2秒的运动画面与静态降级图作为补充；运动画面中轮廓与明暗发生实际变化，不以单帧图片宣称动画通过。
- 深浅表单文字、placeholder、按钮对比度达到4.5:1，输入边框达到3:1；具体数值见结果JSON。没有宣称整页WCAG认证。

核查过程中发现并修复：非整数纹理网格产生粗大拍频图案，改为整数设备像素网格；降低动态效果切换时动画已停但按钮偶尔未同步，补齐停止帧的状态通知。失败运行不计为通过。

## 范围与剩余限制

- 新文件仅在 `prototype/rag-login-motion-demo/`，仓库共享日志只追加本次执行记录。现有 Demo、logo源、正式前端、F5任务/活动指针、后端/API/DTO、评测、模型、受保护本地配置保持原状；用户已有AGENT_LOG改动与未跟踪PROJECT_REPORT保留。
- 未暂存、提交、push、建PR、发布或部署。静态预览只绑定 `127.0.0.1:8767`，不提供业务接口。真实backend/provider/embedding/rerank/ask/judge/feedback/delete调用0。
- 跳过真实认证与业务联调：本轮只授权独立静态原型。跳过Maven/Python评测：相关代码未改。跳过Lighthouse：本机依赖包未提供，不为原型新增工具或依赖。
- 未做真实手机、不同物理GPU、Safari/Firefox和长时间功耗测试。软件WebGL与帧数检查不能证明实际设备60fps或低功耗；页面隐藏暂停已实现，但本轮未单独模拟真实操作系统后台切换。
- 动态形体是本轮独立程序化近似，不是原作者源码或实际三维模型；审美贴近程度仍需用户观察后决定下一轮调整。

建议提交信息：`feat(原型): 新增RAG登录双翼点阵动效演示`。
