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

## 2026-10-13 前缘留白精修

Report status：`REFINEMENT_VALIDATED`。起点HEAD为`389df6dfa3270776109b4cf66f25779807f82e38`，首版已由现有提交保存；本轮未暂存、提交。此前2026-10-12记录保留其原时点含义。

- 用户指出前方三角形别扭；本轮仅修改`js/logo-motion.js`中的轮廓场：删除带竖直封口的三角切口，改为贯通上下翼缘的曲线窄缝；圆滑汇聚处、随呼吸微调缝宽，外层折面降低密度，外翼轮廓略微弯曲。纹理、生命周期、布局、原始logo路径和表单不改。
- `node --check prototype/rag-login-motion-demo/js/logo-motion.js`与`git diff --check`通过。正式前端源码/配置/依赖与上轮成功build保持一致，复用已通过的vue-tsc/Vite证据，本轮不重复正式build、F5/Maven/Python验证。
- 复用既有封闭浏览器脚本，在已有Chrome/SwiftShader上重新执行39/39 PASS，页面异常/外部请求/非GET请求均0；两主题×1440×900、1024×768、390×844六张矩阵逐图查看，横向溢出0；补查间隔2.2秒的动态画面。WebGL实际编译/降级/恢复、三纹理、降低动态效果/暂停/离屏、销毁重建与file://等检查覆盖新shader。
- 本轮证据独立保存至原证据目录下`refinement-2026-10-13/`，首版截图及检查结果不覆盖。初次运行因上轮静态服务已停止而连接拒绝，启动仅绑定127.0.0.1的静态服务后重验通过，不将该失败计为通过。
- 范围安全：产品改动仅上述shader；文档为本README/VALIDATION及共享日志追加。F5/活动指针、正式前端、旧Demo、logo源、后端/API/评测/依赖与用户已有提交保留，无真实业务调用或提交/发布动作。
- 剩余限制沿用首版：本轮仍为程序化近似，未做真实手机/GPU性能、Safari/Firefox、真实认证或质量评测；Lighthouse工具缺失且不新增依赖。

建议提交：`fix(原型): 将登录动效三角切口改为曲面留白`。


## 2026-10-13 数据汇聚拖尾

Report status：`CONVERGENCE_VALIDATED`。起点HEAD389df6d，保留上一轮尚未提交的曲面精修；未暂存或提交。

- 品牌视觉：宽尾处的稀疏碎片沿收束弧线向右移动，多数进入双翼后淡出，少数细流汇向金色尖端；主曲面在开启拖尾时稍缩小、右移，为输入数据留空间。保留圆润开放窄缝，不修改原始SVG路径。文字“万千数据，汇成有用的信息”说明品牌理念，不意味着读取或筛选实际业务数据。
- 增加默认开启的汇聚拖尾开关，可鼠标或Space比较；降低动态效果时允许静态对比。拖尾与主体共享呼吸投影，单次WebGL片元绘制，无粒子循环、DOM粒子或新增依赖。暂停时同步最后内部时间，避免切换纹理/拖尾时微跳一帧。控制行可换行。
- 语法检查两项node --check与git diff --check通过。复用已有Chrome155.0.8059.39/独立headless/SwiftShader执行44/44 PASS；包括新增开关默认状态、暂停同相位画面变化、键盘恢复原帧、reduce静态切换、无脚本禁用；降级禁用也覆盖新增按钮。异常、外部请求、非GET请求及storage写入0。深浅三视口六张矩阵与间隔2.2秒动态画面复核，页面与控制行横向溢出0。
- 初次对比检查揭示暂停帧与内部时钟差一帧，修正后重验通过；该失败不计通过。内置浏览器旧脚本缓存导致新增按钮暂时禁用，添加本地资源版本参数后重验，实际开关0→1可用。资源仍为本地，file://测试通过。
- 证据：本线程visualizations/login-motion/convergence-2026-10-13/保存本轮独立执行脚本、browser-results.json、前态hash/源码及14张验证截图，不覆盖前轮证据。文档删除前轮意外重复的相同段落，原结论保留；共享AGENT_LOG只追加。
- 范围核验：改动仅本原型HTML/CSS/两JS/README/VALIDATION及共享日志；正式前端、旧Demo、logo源、OpenSpec/F5/活动指针、后端/API/评测、依赖与锁文件无差异，未暂存/提交/push/PR/部署，无真实业务调用。
- 跳过与复用：正式前端源码/配置/依赖未改，复用首版含vue-tsc的正式build，不重复F5/Maven/Python评测；真实认证与provider不在本轮范围。真实手机/物理GPU/Safari/Firefox/长时功耗未验证，软件WebGL不证明真实设备性能；Lighthouse缺少工具且不新增依赖。

建议提交：`feat(原型): 为登录双翼动效加入数据汇聚拖尾`。
