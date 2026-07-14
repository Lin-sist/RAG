# Design: C1 JWT Secret Production Guard

## 1. 设计目标

在不改变认证协议、token shape 和非 prod 开发体验的前提下，为 `prod` active profile 增加确定性的 JWT secret 启动守卫。守卫必须先识别“长度足够但不安全”的公开默认值及明确配置错误，再把合法候选交给现有 JJWT key 构造逻辑。

## 2. 当前数据流

```text
application.yml / JWT_SECRET
        ↓ Spring ConfigurationProperties 绑定
JwtProperties.secret
        ↓ UTF-8 bytes
JwtTokenProvider 构造
        ↓
JJWT Keys.hmacShaKeyFor
```

当前缺口位于属性绑定与 JJWT key 构造之间：JJWT 能拒绝过短 key，但无法判断一个长度足够的值是否是仓库公开默认值。

## 3. 目标数据流

```text
application.yml / JWT_SECRET
        ↓ Spring ConfigurationProperties 绑定
JwtProperties.secret + active profiles
        ↓
JwtSecretProductionGuard（prod 才执行自定义规则）
        ├─ default / blank / exact misconfiguration → fail-fast，无 secret 回显
        └─ allowed candidate
                ↓ 原值，不 trim、不改写
          UTF-8 bytes
                ↓
          JJWT Keys.hmacShaKeyFor（继续负责算法最小长度）
```

## 4. 主要决策

### 4.1 守卫放在 `rag-auth`

JWT secret 的语义归属认证模块。计划新增一个无外部依赖、核心判定可纯函数测试的 production guard/policy，由 `JwtTokenProvider` 在构造 JJWT key 前显式调用。profile 判断使用 Spring `Environment` 的 active profiles，并只对精确 profile 名 `prod` 启用。

显式构造依赖用于保证执行顺序；不采用 `ApplicationRunner` 或依赖不确定 Bean 初始化顺序的旁路检查。

### 4.2 精确判定，不做模糊黑名单

首版拒绝集合草案：

1. `null` 或 `trim().isEmpty()`；
2. 与当前 tracked fallback `change-me-in-production-please-use-at-least-32-bytes` 完全相等；
3. `!value.equals(value.trim())`，即首尾空白；
4. 整个值匹配未解析占位符结构 `^\$\{[^{}]+}$`。

通过的值保持原样交给 JJWT，不自动 trim 或 normalize，避免应用实际签名 key 与运维输入不一致。异常只返回稳定错误类别，例如 `blank`、`known-default`、`surrounding-whitespace`、`unresolved-placeholder`，不附带值、长度、hash 或内容片段。

本规则集是 proposal/design 审查的核心确认点；用户批准前不实现。

### 4.3 不重复 JJWT 长度校验

`JwtTokenProvider` 当前已经使用 `secret.getBytes(StandardCharsets.UTF_8)`，JJWT 会按实际字节拒绝弱 key。C1 不新增 `>= 32` 等平行长度判断，也不改用字符数。

测试会锁定以下职责边界：

- prod 自定义守卫负责公开默认值、空白和精确误配置；
- JJWT 继续在所有 profile 下负责算法 key-strength；
- 多字节字符以现有 UTF-8 bytes 结果进入 JJWT，不按 Java 字符数判断。

### 4.4 保留 dev/test 兼容性

不删除 `application.yml` 的本地 fallback，不修改 `application-test.yml`。当 active profiles 不含精确的 `prod` 时，自定义 deny rules 不阻断启动；但 JJWT 原有 key-strength 约束仍然生效。

### 4.5 错误与日志安全

启动失败应使用明确异常类型或配置异常包装，消息仅包含：

- 配置键 `jwt.secret`；
- 稳定错误类别；
- “请通过 `JWT_SECRET` 注入非默认 secret”之类修复指引。

禁止包含 secret 原值、前后缀、长度、hash、环境变量内容或异常对象的敏感 payload。现有普通日志脱敏规则继续适用。

## 5. 预计影响文件

以下仅是实现阶段计划，当前草案阶段不修改：

- `rag-auth/src/main/java/com/enterprise/rag/auth/config/`：新增 prod secret guard/policy，必要时调整配置接线。
- `rag-auth/src/main/java/com/enterprise/rag/auth/provider/JwtTokenProvider.java`：在 JJWT key 构造前显式调用 guard。
- `rag-auth/src/test/java/...`：新增规则边界、UTF-8/JJWT 职责和无 secret 泄露测试；适配现有直接构造测试。
- `rag-admin/src/test/java/...` 或 `rag-auth/src/test/java/...`：用最小 context 验证 prod 启动拒绝/允许语义。
- 可能更新 `README.md` 的 `JWT_SECRET` 生产配置说明；不修改本地 `.env.local` 或部署 secret。

不计划修改 API、DTO、数据库、前端、RAG pipeline、评测脚本或依赖版本。

## 6. 测试设计

### 6.1 纯单元测试

- `prod` + 当前 known default → 拒绝；
- `prod` + `null`、空串、纯空白 → 拒绝；
- `prod` + 首尾空白 → 拒绝且不自动修剪；
- `prod` + 完整未解析占位符 → 拒绝；
- `prod` + 合法候选 → guard 通过；
- `dev`/`test` + known default → 自定义 guard 不拒绝；
- 所有异常消息均不包含候选 secret。

### 6.2 JJWT 职责回归

- 过短候选继续由 `Keys.hmacShaKeyFor` 拒绝；
- 使用包含多字节字符且 UTF-8 字节数满足要求的候选，证明边界基于 UTF-8 bytes 而非字符数；
- 不在 production guard 中复制最小长度常量。

### 6.3 最小 Spring context

使用 `ApplicationContextRunner` 或等价的窄上下文测试，只装载 JWT 相关配置：

- active profile 为 `prod` 且使用 known default 时 context 失败；
- active profile 为 `prod` 且 secret 合法时 JWT 相关 bean 可创建；
- 测试不启动数据库、Redis、Milvus、LLM、embedding 或 rerank provider。

### 6.4 命令级验证

实现阶段计划运行：

```powershell
mvn -q -pl rag-auth -am test
mvn -q test
git diff --check
```

若完整测试受本地基础设施影响，将记录具体失败和聚焦替代证据，不把跳过写成通过。

## 7. 外部调用与费用

设计、实现、单测和 context 测试均为本地行为。embedding/rerank/judge/ask 调用量分别为 `0/0/0/0`，无业务数据出站、无模型、无限流风险、费用为 0；零费用依据是“不发生调用”，不是 NVIDIA NIM 免费假设。若实现方案变化导致业务外调，必须停在调用前重新提案并取得授权。

## 8. 备选方案

### 方案 A：移除所有默认值

拒绝。它会改变 dev/test 启动体验并扩大 C1 范围；本 change 只要求 prod fail-fast。

### 方案 B：仅依赖 JJWT

拒绝。当前公开默认值长度足够，JJWT 不会识别它是已知默认值。

### 方案 C：用 `ApplicationRunner` 启动后检查

拒绝。执行过晚，且 JWT provider 可能已先构造；不满足稳定的启动前守卫顺序。

### 方案 D：模糊匹配 `secret`、`change` 等字符串

拒绝。误报边界不可控，也难以形成稳定契约。首版只采用精确值和精确结构规则。

## 9. 回滚

实现尚未开始。未来若实现需回滚，应同时撤销 guard 接线、相关测试与本 change 的未接受 spec delta，使行为恢复到“仅由 JJWT 校验 key-strength”。不得只删除测试或只改 baseline spec 来掩盖实现差异。

## 10. 审查请求

请重点确认：

1. 是否接受 prod 精确拒绝集合：blank、tracked default、首尾空白、完整未解析占位符；
2. 是否接受“非 prod 保留 fallback，JJWT 继续独占最小长度校验”的职责边界；
3. 是否接受在 `rag-auth` 通过显式构造依赖保证 guard 先于 JJWT key 构造执行；
4. 是否接受错误消息完全不回显值、长度或 hash。

## 11. 提交责任

`用户手动提交`。Agent 不暂存、不提交、不 push。
