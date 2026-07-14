# Proposal: C1 JWT Secret Production Guard

## Why

当前 `rag-admin/src/main/resources/application.yml` 为 `jwt.secret` 提供了仓库内已知默认值 `change-me-in-production-please-use-at-least-32-bytes`。该值按 UTF-8 编码后满足 JJWT 的最小 HMAC key 长度，因此即使生产环境忘记注入真实 secret，应用也可能继续启动。攻击者一旦知道这个公开默认值，就可能伪造可被服务接受的 JWT。

现有 `JwtTokenProvider` 已通过 `getBytes(StandardCharsets.UTF_8)` 把 secret 交给 JJWT；JJWT 会在所有 profile 下拒绝过短 key。C1 只补生产环境的已知默认值、空值和明确误配置守卫，不重复实现 JJWT 的算法长度校验。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，运维如果忘记设置 `JWT_SECRET`，生产服务仍可能带着仓库里人人可见的默认密钥启动，拿到默认值的人就有机会伪造登录令牌；改之后，只要 `prod` 环境检测到默认、空白或明确误配置的 secret，应用会在对外提供服务前直接启动失败，并给出不泄露 secret 的配置错误提示。

## Current Status

- `confirmed`：B0、B1、v4 关闭裁决和本地质量门禁均已完成；C1 前置顺序满足。
- `confirmed`：当前默认 secret 是 tracked config 中的已知值，且其 UTF-8 字节长度足以通过 JJWT 最小长度检查。
- `confirmed`：当前没有针对 `prod` active profile 的已知默认值守卫。
- `partial`：JJWT 已覆盖过短 key，但它不能识别“长度足够但公开已知”的默认 secret。
- `planned`：增加 prod 专属、可单元测试、错误信息不回显 secret 的启动守卫。
- `out_of_scope`：数据库用户、bootstrap、注册/用户管理 API、token claim/过期策略调整，均留给 C2 或后续 change。
- `unknown`：不同部署环境最终如何注入/轮换 `JWT_SECRET`；C1 只定义启动安全边界，不选择 secret manager。

## Scope

- 在 `prod` active profile 中，对 JWT secret 执行启动前校验。
- 拒绝以下精确类别：
  - `null`、空字符串或 `trim()` 后为空；
  - 与仓库当前已知默认值完全相等；
  - 首尾包含空白字符；
  - 整个值仍是未解析占位符形式（例如 `${JWT_SECRET}`）。
- 采用精确匹配/精确结构判断，不做 `contains("secret")`、前缀猜测等模糊黑名单。
- 错误只报告配置类别与修复方向，不记录或回显 secret。
- 保持非 prod 的本地开发、测试默认行为兼容。
- 保持 `JwtTokenProvider` 现有 UTF-8 转换与 JJWT key-strength 校验职责。
- 补充聚焦单元测试和最小 Spring context 启动测试。

## Non-goals

- 不改变认证数据源、用户模型、角色权限或 token DTO/API。
- 不移除 dev/test 的本地默认值，不要求开发者每次本地启动都配置 secret。
- 不自研或重复 JJWT 的最小 HMAC key 长度规则。
- 不新增依赖、secret manager、KMS、Vault、部署平台配置或 secret 轮换能力。
- 不修改 embedding、retrieval、rerank、generation、citation、no-answer 或 judge 行为及指标。
- 本草案阶段不修改任何业务代码、配置或测试。

## External Calls And Authorization

本 change 的规格、实现和验证均不需要 RAG 业务外部调用：

| 调用类型 | 预计调用量 | 数据出站 | 模型 | 限流风险 | 费用与零费用依据 | 授权状态 |
|---|---:|---|---|---|---|---|
| embedding | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0；不依赖 NVIDIA 免费假设 | 不适用 |
| rerank | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0；不依赖 NVIDIA 免费假设 | 不适用 |
| judge | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0；不依赖 NVIDIA 免费假设 | 不适用 |
| ask | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0；不依赖 NVIDIA 免费假设 | 不适用 |

实现阶段允许的 Maven 测试只解析仓库已声明依赖并在本地执行，不发送问题、文档、prompt、context 或 secret。若后续计划发生任何上述业务外调，必须先修改 proposal、说明调用预算并重新取得用户授权。

## Risks

- 对“误配置”的判定过宽会误伤合法 secret：本草案只使用精确规则，并把规则集作为本次评审重点。
- 守卫执行晚于 `JwtTokenProvider` 构造会导致错误来源不稳定：design 要求通过显式依赖保证 prod policy 先于 JJWT key 构造执行。
- 错误信息可能泄露 secret：异常消息与日志只允许出现 `jwt.secret`、错误类别和修复提示。
- 仅检查 profile 名称 `prod` 不能替代部署治理：这是 C1 的有意边界，其他 profile 的生产化命名需另行 change。

## Acceptance Evidence

- 用户先审阅并明确批准本 proposal、design 与 spec delta，之后才允许修改业务代码。
- `prod` + 已知默认值：最小 Spring context 启动失败，错误不含 secret 原文。
- `prod` + 空白/首尾空白/未解析占位符：启动失败。
- `prod` + 合法且满足 JJWT 要求的 secret：通过 C1 守卫并能构造 JWT provider。
- 非 `prod`：保留当前本地开发和测试兼容性；过短 key 仍由 JJWT 按 UTF-8 字节拒绝。
- 聚焦 `rag-auth` 测试与完整 `mvn -q test` 通过。
- 不发生 embedding、rerank、judge、ask 或其他 provider 业务调用。
- `git diff --check` 与 change 结构/Markdown 相对链接检查通过。

## Commit Responsibility

`用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、发布或部署，只在每个可验证切片结束后提供建议的中文 Conventional Commit message。
