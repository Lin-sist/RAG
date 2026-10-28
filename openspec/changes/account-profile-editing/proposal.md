# 头像与个人资料编辑

> 状态：`APPROVED · REVIEW_READY`。2026-10-27 用户选择“头像、显示昵称和邮箱”并回复“确认方案”，授权按本方案实现与本地验证。实现与验证边界见[实施记录](implementation-review.md)。登录用户名与工作空间只读；原设置任务保留待验收。实际数据库/账号写入、业务出站预算 0，无提交、部署、接受或归档授权。

## 为什么改

目前账户浮层只能查看资料，头像只能显示首字母。用户需要把头像和个人资料真正保存到账号，在刷新、重新登录和其他浏览器访问时取得相同资料，而不是只修改当前页面的演示数据。

## 首版范围

- 在现有“账户与工作空间”浮层内编辑显示昵称、邮箱，选择/预览/替换/移除头像。
- 使用显式“保存修改／取消”，一次提交资料与头像，成功后统一更新侧栏和账户摘要。
- 新增自助资料读取、带版本检查的保存及受认证保护的头像读取；以当前服务端身份确定目标用户。
- 使用数据库保存小尺寸头像，不新增对象存储、外部图床或依赖。
- 登录用户名、组织归属、权限与知识库授权保持只读；邮箱仅作资料邮箱，不标注已验证，也不承担找回密码或登录别名。

## 已核对事实

`confirmed`：当前 user 表有 username、email、version，邮箱有全局唯一索引；登录与 Token 刷新按 username 查询。现有 /api/session/context 只读返回账号/工作空间。`partial`：旧 UserProfile.vue 有演示表单与假数据，但 /profile 已重定向，不能作为真实编辑能力。`planned`：本提案的头像、显示昵称、邮箱与读写流程。`unknown`：真实数据库迁移、实体设备与跨浏览器最终效果。`out_of_scope`：改登录名/密码、手机、邮箱验证/找回、组织管理、账号注销、图床、裁剪器和多租户生产 SLA。

直接依据：[用户表](../../../rag-admin/src/main/resources/db/migration/V1__init_schema.sql)、[认证查询](../../../rag-auth/src/main/java/com/enterprise/rag/auth/persistence/mapper/AuthUserMapper.java)、[认证服务](../../../rag-auth/src/main/java/com/enterprise/rag/auth/service/impl/AuthServiceImpl.java)、[上下文](../../../rag-admin/src/main/java/com/enterprise/rag/admin/session/SessionContextDTO.java)、[路由](../../../rag-frontend/src/router/index.ts)。本轮没有读取真实账号数据或敏感配置。

## 验证、授权与成本

接口、迁移、编辑状态与失败处理见 [design](design.md)，实施与验收见 [tasks](tasks.md) 和 [规格](specs/rag-system/spec.md)。用户已确认并完成本地实现验证；原设置任务保留待验收，不自动接受旧任务。上文 planned 是事前现状，当前实现及仍未知的真实 MySQL/设备效果以实施记录为准。

真实 ask/embedding/rerank/judge/provider 与数据出站预算均为 0；不重启真实后端、不对真实数据库迁移或写入资料。实现时可运行聚焦/相称 Maven、前端测试/build 与本地合成浏览器；真实数据库和实际账号联调另按已批准环境范围执行。无 Agent 提交、push、部署或接受/归档授权。
