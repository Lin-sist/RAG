# 资料保存失败与即时生效修复

日期：2026-10-28。用户反馈保存头像和昵称后没有生效，界面反而要求处理“草稿核对”。本轮按既有能力内的 bugfix 修复，沿用当前 change，不增加新接口、迁移或依赖。

## 为什么保存失败

头像以顶层 byte[] 传给 mapper。普通 MyBatis 能处理，但生产使用的 MyBatis-Plus 在收集参数时把这个基本类型数组当成 Object[] 转换，抛出 ClassCastException，整次保存返回 500。因此截图中服务器仍是旧昵称与无头像，并非成功保存后延迟显示。前轮 MockMvc/普通 MyBatis 测试和合成浏览器接口没有覆盖生产参数处理器，这是本次漏测。

本轮先用真实 Tomcat HTTP、生产 MyBatis-Plus 配置及插件、隔离 H2 复现：昵称单独保存通过，带头像保存为 500。直接调用生产 mapper 链也复现 `MybatisParameterHandler.toCollection` 的基本类型数组转换错误。原始复现输出位于忽略目录 `.tmp/account-profile-editing/http-plus-before-fix.txt` 和 `mapper-root-cause.txt`，只包含合成资料。

## 修复后的行为

- mapper 头像参数改用带 data 属性的值对象，SQL 绑定其内部 bytes，仍在同一事务保存资料。生产参数处理器不会再把头像误判为批量参数。
- 保存返回 200 后，昵称与账户摘要立即更新；本次已获服务器确认的规范 PNG 直接发布到共享头像内存，侧栏立即显示，不等待第二次下载。失败前不发布到共享展示。
- 移除服务器副本、“草稿已保留”和手动核对面板。正常界面只有编辑、取消与保存。错误时保留输入，显示字段错误或简短失败/重试提示，允许继续编辑。
- 不自动重放写入，不用本地预览冒充保存。网络/服务故障或冲突后的重试，由用户再次点击保存触发，客户端内部读取最新版本再提交；不要求用户手动处理版本与副本。

## 验证

| 项目 | 结果 |
| --- | --- |
| 根因复现 | 修复前生产配置下真实头像 PUT 为 500，直接 mapper 调用复现数组转换异常；未用普通 MyBatis 的通过结论代替生产配置。 |
| 真实 HTTP 回归 | 新增 [ProfileHttpTest](../../../rag-admin/src/test/java/com/enterprise/rag/admin/profile/ProfileHttpTest.java) 三项通过：头像与昵称保存立即 GET 读回、单独昵称保存读回、生产 mapper 接受头像 bytes。真实 Tomcat、真实 MyBatis-Plus、Spring 事务与隔离 H2；没有连接实际账号或数据库。 |
| 完整 Maven | `mvn -q test` 退出 0；本次运行开始后更新的 134 suites / 706 tests，0 failures、0 errors、22 skipped。MySQL 容器仍因 Docker 不可用而跳过。 |
| 前端回归与构建 | 完整串行 146/146 通过，FrontendBuild 执行 vue-tsc 与 Vite，通过；保留既有包体警告。 |
| Chrome | [资料脚本](../../../rag-frontend/tests/profile-browser.mjs) 23 组、24 图通过：原深浅/宽窄/取消/文件方向/SSE 场景，以及错误简短提示、无核对面板、用户手动重试。特意让头像下载接口失败，200 保存后侧栏仍立即使用已确认头像与新昵称，证明不依赖额外下载。无页面异常、无外部出站。 |
| 静态门禁 | SensitiveLogs、DocAndScope 与 diff-check 通过；日志随本次修复记录。 |

新运行输出保存在 `.tmp/account-profile-editing/fix-maven.txt`、`http-after-fix.txt`、`fix-frontend-tests.txt`、`fix-build.txt`、`browser-fix/evidence.json`。本机预览端口已有隔离进程时复用，没有终止未知进程。

## 适用边界与提交

上述证据证明代码修复与隔离链路，不是实际账号验收。没有重启实际后端、执行实际数据库迁移或提交真实资料；当前运行进程需要重新加载后端修复。MySQL、真实账号、实体设备边界沿用原记录，未接受或归档。

未暂存、提交、push 或部署。建议提交：`fix(账户): 修复头像保存异常并让资料保存后立即生效`。
