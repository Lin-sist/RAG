# RAG System Spec Delta: C1 JWT Secret Production Guard

## ADDED Requirements

### Requirement: 生产 JWT Secret 启动守卫

系统在精确 active profile `prod` 下 MUST 在构造 JWT signing key 前校验 `jwt.secret`。当值为空白、等于仓库已知默认值、包含首尾空白或整个值仍为未解析占位符时，应用 MUST 拒绝启动；失败信息 MUST NOT 回显该值、长度、hash 或内容片段。

通过自定义 production guard 的候选值 SHALL 保持原样并以 UTF-8 bytes 交给 JJWT。自定义 guard MUST NOT 复制 JJWT 的算法最小 key-length 规则；过短 key SHALL 继续由 JJWT 在所有 profile 下拒绝。

#### Scenario: 生产环境使用仓库已知默认值

- GIVEN active profiles 包含精确的 `prod`
- AND `jwt.secret` 等于 tracked config 的已知默认值
- WHEN Spring 创建 JWT signing provider
- THEN 应用启动失败
- AND 错误指出 `jwt.secret` 使用了 `known-default`
- AND 错误与普通日志不包含 secret 原文、长度或 hash

#### Scenario: 生产环境 secret 为空白或带首尾空白

- GIVEN active profiles 包含精确的 `prod`
- WHEN `jwt.secret` 为 `null`、空串、纯空白或首尾包含空白
- THEN 应用启动失败
- AND 系统不通过自动 trim 改变输入后继续启动

#### Scenario: 生产环境仍收到未解析占位符

- GIVEN active profiles 包含精确的 `prod`
- WHEN 整个 `jwt.secret` 值符合未解析占位符结构 `${...}`
- THEN 应用启动失败
- AND 错误只报告 `unresolved-placeholder` 类别与修复指引

#### Scenario: 生产环境使用合法候选值

- GIVEN active profiles 包含精确的 `prod`
- AND `jwt.secret` 不命中 production guard 的精确拒绝规则
- WHEN 系统构造 JWT signing provider
- THEN 候选值以原始 UTF-8 bytes 交给 JJWT
- AND 是否满足算法 key-strength 由 JJWT 决定

#### Scenario: 非生产环境保留本地兼容性

- GIVEN active profiles 不包含精确的 `prod`
- WHEN 系统使用当前 dev/test 配置启动
- THEN production guard 不因仓库 fallback 值阻断启动
- BUT JJWT 仍按 UTF-8 bytes 拒绝过短 signing key
