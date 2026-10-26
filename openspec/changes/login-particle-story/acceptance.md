# 登录页粒子改造 · 首轮客户端验收记录

> 证据版本说明（2026-10-26）：下列结果及 visual-evidence.json 仅对应首轮三图样。第二轮品牌首幕、轮廓聚合和左侧文字联动的实现证据另见 [acceptance-v2.md](acceptance-v2.md)；本记录不证明第二轮目标，也不表示用户已接受最终效果。首轮现已本地提交为 4b842f5，文内“未提交”等语句描述采证当时状态。

Report status：`CLIENT_VALIDATED_AWAITING_ACCEPTANCE`。日期：2026-10-26。用户已确认设计并授权实现/验证，尚未接受最终效果或授权 baseline/归档。本文件记录工程证据，不代替用户视觉接受。

## 结果与范围

正式登录页已接入知识文档、检索关联、引用回答三种原创点云。画布局部滚轮控制连续变形，鼠标轨迹拨动粒子并回弹；图样按钮与暂停/继续可用。原生 WebGL1，无新增依赖，旧组件、双翼模块、SVG 与独立原型保留。登录表单、共享主题、认证/API/SSE 和其他业务页实现不变。

## 自动化与构建

| 检查 | 结果与证据边界 |
| --- | --- |
| 改前基线 | HEAD `158b56d016083cf2a3414be2a51eb83db50e63ea`；工作区起始干净；前端 102/102；FrontendBuild 含 vue-tsc 通过。 |
| 新增聚焦测试 | `node --test tests/login-particle.test.mjs`：12/12，通过几何确定性/边界、端点/散开、反向连续、滚轮保护、单 RAF、暂停/reduce、鼠标恢复、后台/离屏、GPU 故障/恢复/释放、Vue 迟到回调等检查。 |
| 实施后全量前端测试 | `node --test tests/*.test.mjs`：114/114，0 失败/跳过。覆盖旧模块的冻结测试仍通过，但新效果以新增测试和实际浏览器为证。 |
| 正式构建 | `scripts/run_local_quality_gates.ps1 -Mode FrontendBuild` 通过，实际执行 vue-tsc 后再 Vite 构建；保留既有主包 >500 kB 警告。 |
| 规划文件 | 四类文件结构、MODIFIED 名称与 baseline 对应、ADDED 名称无冲突，3 requirements / 12 scenarios，GIVEN/WHEN/THEN 完整。 |
| OpenSpec CLI | 当前命令不可用，未安装依赖，未声称官方 validate 通过；使用文件级结构、链接、范围和差异检查。 |
| DocAndScope | 最终结果见下方治理记录；门禁之外另查新增 Markdown、实际改动范围及保留文件。 |

登录路由 JS 的 Vite gzip 日志由约 6.11 kB 变为约 6.72 kB，CSS 由约 1.38 kB 变为约 1.74 kB。这里是构建日志口径的包体观察，不是性能收益；没有执行硬件帧率基准。

## 浏览器与图样检查

内置浏览器连接失败后，使用 CUA 的 Chrome 扩展完成验证，未使用真实后端或模型。图片、哈希和浏览器状态集中记录于 [visual-evidence.json](visual-evidence.json)。截图保存在本地可视化目录，未将二进制图片加入仓库。

- 深浅主题的 1440×900、1024×768、390×844 CSS 视口均检查，无页面横向溢出。窄屏截图包含页面顶部与滚动后的下部；实际图片像素尺寸单独保留，不拉伸成宣称的 CSS 尺寸。
- 桌面两主题均检查三种完整图样及 progress=0.5400 的中间形态。已逐图检查文字、表单、图样边界及控制区；一次截图与合成器刷新不同步的旧记录已用稳定画面替换。
- 实际滚轮使进度停在中间；反向操作观察到 0.1800 → 0.1193，画面未跳至完整图样。实际拖动后观察到粒子偏移 0.0978，离开后衰减；该值是控制器装饰坐标，不是硬件性能指标。
- 暂停后选图能单次绘制完整场景；窄屏 Space 选中引用回答，progress=2.0000。按钮高度至少 44 CSS px。
- 合成登录由 Enter 提交，等待态显示禁用的“登录中...”，随后进入 `/chat`，登录画布数量为 0；错误场景显示本地合成错误提示。成功退出后返回登录页，重新创建且仅存在一幅新画布。

## 浏览器故障检查的性质

本次新增 `tests/particle-checks.html`，仅由已有封闭预览服务器的 `/__particle-checks` 提供，不进入正式路由或构建。它使用真正的浏览器 WebGL 和生产控制器，提供显式测试按钮，避免通过生产界面加入调试参数或操作栏。

| 场景 | 实际观察 |
| --- | --- |
| 模拟减少动态效果 | reduced=true，pendingFrames=0；手动选图从 progress=0 变为 1，时间不累积，仍无连续 RAF。 |
| 模拟 document.hidden | pendingFrames=0；间隔采样的 frame/time 均保持相同，恢复后重新调度。 |
| 真正 IntersectionObserver | 将画布设为不显示，异步 observer 稳定后 pendingFrames=0；重新显示后恢复为 1。第一次隐藏后的采样可能早于 observer 回调，以稳定样本为准。 |
| WebGL 上下文 | 使用真正的 WEBGL_lose_context 扩展触发丢失，renderer=static、pendingFrames=0；恢复后 renderer=webgl、pendingFrames=1。 |
| 销毁与重挂 | 销毁后画布为 0×0、pendingFrames=0；重挂后只出现一条 RAF 链。 |
| 模拟不支持 WebGL | renderer=static、pendingFrames=0，检查页显示既有 SVG。正式 Vue 按钮禁用由组件状态实现和聚焦测试覆盖。 |
| 浏览器滚轮事件保护 | 在真实浏览器分发受控 DOM WheelEvent，Ctrl/Meta、横向、不可取消、起点向外和画布外六类事件的 defaultPrevented 均为 false。不是物理系统缩放手势测试。 |

reduce/document.hidden 是检查页内模拟，未修改操作系统偏好；真实离屏通知、WebGL 丢失/恢复和实际滚轮/鼠标/键盘操作分别报告。此证据不证明物理手机、真实认证或生产长稳。

## 范围复核、流程与剩余风险

本轮生产实现仅涉及 AuthLayout、新组件及两份工具模块；另有聚焦测试、测试检查页/预览入口、OpenSpec 和活动指针/日志。受保护的 LoginView、旧双翼组件/模块、SVG、独立原型、package.json/lock 和 backend 未改。文件哈希与采证时的改动状态见证据 JSON。

本次按新工作流先完成四类方案，用户确认 1 次后连续完成实现和验证；没有逐切片重复确认。测试检查页是已批准验证范围内的工具，设计已补充这一验证方式。实际依赖、provider、认证与持久化副作用范围没有扩大。

剩余 unknown：实体触屏设备、系统级减少动态效果手势、不同 GPU/驱动及长期性能；源程序的 30 fps/粒子上限是预算，不是硬件结果。真实业务调用为 0；合成认证请求确有发生，但只到本地白名单 fixture。

工程验证完成后保持活动指针，等待用户接受视觉效果及独立归档授权。当前无暂存、commit、push 或 PR。建议提交信息：`feat(登录页): 增加可交互的三图样粒子动效`。

## 最终治理记录

DocAndScope PASS：`diff-check=OK, protected-paths=OK, md-files=6, links-verified=45, agent-log-lines=103 (<=200 OK)`。门禁按 Git 状态只看到未跟踪目录，因此另外递归检查本 change 的全部新增 Markdown：4 个相对链接目标均有效，3 requirements / 12 scenarios 结构完整。

独立范围审计 PASS：实际 15 个改动/新增文件均在计划范围内，7 个实现/测试源码哈希、10 个保留文件哈希、31 张图片哈希匹配；原热日志内容按原顺序完整保留，只追加本次记录；暂存文件为 0，HEAD 未改变。最终文档/任务状态更新后再次运行同一治理门禁通过。
