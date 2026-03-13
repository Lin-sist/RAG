# RAG 项目后端优化与重构文档

## 1. 文档说明

- 文档目标：作为当前 RAG 项目后端优化、重构、验收和长期维护的统一依据。
- 使用对象：项目维护者、协作开发者、Copilot/Codex 等辅助重构工具。
- 适用范围：`rag-admin`、`rag-auth`、`rag-core`、`rag-common`、`rag-document` 五个后端模块。
- 当前状态：基于 2026-03-13 的代码审查结果整理。

## 2. 维护规则

### 2.1 状态枚举

- `TODO`：已确认问题，尚未开始处理
- `DOING`：正在处理
- `DONE`：已完成并验证
- `BLOCKED`：存在外部依赖或阻塞

### 2.2 更新要求

- 每次开始一个优化项前，先更新对应任务状态。
- 每次完成一个优化项后，补充“实施说明 / 验收结果 / 风险”。
- 不允许只改代码不更新文档。
- 若重构改变了接口行为、任务流程、数据结构或配置项，必须同步更新本文件。

### 2.3 推荐维护字段

| 字段 | 说明 |
|------|------|
| 编号 | 问题或任务唯一标识，如 `AUTH-01` |
| 优先级 | `P0 / P1 / P2 / P3` |
| 状态 | `TODO / DOING / DONE / BLOCKED` |
| 模块 | 受影响模块 |
| 问题描述 | 当前实现的问题 |
| 目标状态 | 重构后的预期行为 |
| 验收标准 | 如何判断改造完成 |
| 风险/依赖 | 改造前需要注意的事项 |

## 3. 当前后端架构概览

### 3.1 模块职责

- `rag-admin`：主应用入口、Controller、知识库/历史/任务 API
- `rag-auth`：认证与鉴权，JWT、过滤器、安全配置
- `rag-core`：Embedding、向量库适配、检索、Prompt、答案生成
- `rag-common`：异常、响应封装、Redis、异步任务、限流、幂等、Trace
- `rag-document`：文档解析、分块、文档处理

### 3.2 当前主流程

```text
登录
-> 创建知识库
-> 上传文档
-> 控制器中直接发起异步解析/分块/向量化/入库
-> 问答时向量检索
-> 简单重排
-> 组装 Prompt
-> 调用 LLM 生成答案
-> 保存问答历史与反馈
```

### 3.3 当前定位

- 当前版本具备演示级端到端闭环
- 当前版本不具备生产级安全性、一致性和可维护性
- 当前版本适合分阶段重构，不适合继续在现状上堆功能

## 4. 核心问题总览

## 4.1 P0 级问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| AUTH-01 | P0 | DONE | `rag-admin` `rag-auth` | 控制器从 `userDetails.getUsername()` 解析用户 ID，失败后默认回退到 `1L` | 多用户数据串号，owner/uploader/history/feedback 归属错误 |
| AUTH-02 | P0 | DONE | `rag-auth` `rag-admin` | 仅做“是否登录”校验，缺少知识库、历史记录等资源级权限校验 | 已登录用户可越权访问、修改或删除他人数据 |
| AUTH-03 | P0 | DONE | `rag-auth` | JWT 过期时间单位定义和使用不一致；logout 后 refresh token 仍可继续刷新 | Token 生命周期失真，安全策略无效 |
| SEC-01 | P0 | DONE | `rag-admin` | 配置文件中存在数据库密码、Redis 密码、JWT secret、第三方 API key | 严重的敏感信息泄露风险 |

## 4.2 P1 级问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| DOC-01 | P1 | DONE | `rag-admin` `rag-document` | 上传控制器直接持有整份 `byte[]` 并在控制器内完成完整索引流程编排 | 大文件并发时内存压力大，职责混乱 |
| DOC-02 | P1 | DONE | `rag-admin` | 文档上传后未完整持久化 `contentHash`、chunk 记录、vectorId | 删除、去重、回溯、审计链路不完整 |
| DOC-03 | P1 | DONE | `rag-admin` `rag-core` | 删除文档时向量集合名使用 `kb_{kbId}`，与知识库真实 `vectorCollection` 不一致 | 单文档删除不能正确清理向量数据 |
| DOC-04 | P1 | DONE | `rag-document` | 文档去重依赖 JVM 内存中的 `ConcurrentHashMap` | 单实例有效，多实例/重启后失效 |
| DOC-05 | P1 | DONE | `rag-admin` | 未明确配置上传大小限制、文件校验和非法输入保护 | 文件上传边界不清晰，容易触发异常和资源耗尽 |

## 4.3 P2 级问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| RAG-01 | P2 | TODO | `rag-core` | 当前重排基于空格分词的简单关键词匹配，对中文场景不可靠 | 检索质量和回答稳定性不足 |
| RAG-02 | P2 | TODO | `rag-core` | Prompt 拼装没有 token budget、上下文去重、来源增强策略 | 上下文质量不稳定，易浪费 token |
| RAG-03 | P2 | TODO | `rag-core` | QA 缓存键未纳入 `topK/filter/model` 等参数 | 缓存脏命中风险 |
| RAG-04 | P2 | TODO | `rag-admin` `rag-core` | 知识库查询次数统计定义存在，但未形成真实更新闭环 | 统计面板数据不可信 |
| RAG-05 | P2 | TODO | `rag-admin` | SSE 问答未形成完整的历史沉淀策略 | 流式问答与历史/审计链路不一致 |

## 4.4 P3 级问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| TEST-01 | P3 | TODO | `rag-common` | Property tests 依赖 Redis，本地不可用时大量 rejection 导致测试失败 | 测试结果不稳定，CI 信号失真 |
| INFRA-01 | P3 | TODO | `rag-common` `rag-core` | 限流、幂等、异步能力已经存在，但业务接口没有真正落地使用 | 通用能力与业务实现脱节 |
| OBS-01 | P3 | TODO | 全模块 | 缺少系统化指标、告警、任务链路可观测性 | 故障难排查，容量难评估 |

## 5. 重构总目标

## 5.1 功能目标

- 保持现有核心业务闭环不丢失
- 在不破坏主要接口语义的前提下完成结构优化
- 建立“认证/鉴权、文档索引、RAG 检索、基础设施”的清晰边界

## 5.2 工程目标

- 控制器只负责参数校验和响应，不负责复杂业务编排
- 业务流程可追踪、可补偿、可审计
- 文档记录、chunk 记录、向量记录形成完整闭环
- 多用户、多知识库场景下具备正确的资源隔离
- 测试体系可稳定运行并为重构提供回归保障

## 5.3 非目标

- 本阶段不追求一次性引入完整多租户架构
- 本阶段不强制改造成微服务
- 本阶段不直接重写前端

## 6. 推荐重构原则

### 6.1 分层原则

- Controller：只做入参校验、鉴权入口、返回值封装
- Application Service：负责编排完整用例流程
- Domain/Business Service：处理知识库、文档、历史、权限等核心规则
- Infrastructure：处理 Redis、向量库、LLM、消息队列、文件存储

### 6.2 一致性原则

- 数据库状态和外部系统状态不能假设天然一致
- 所有跨系统调用都必须定义失败状态、补偿动作和重试策略
- 文档状态应明确区分：`PENDING / PROCESSING / COMPLETED / FAILED`

### 6.3 渐进式原则

- 优先修复 P0 和 P1 问题
- 每次重构只动一条主链路
- 每次重构必须配套测试和文档更新

## 7. 分阶段优化路线图

## 7.1 阶段一：身份与安全整改

### 目标

- 修复用户身份提取错误
- 建立统一资源级权限校验
- 修复 JWT 生命周期与登出策略
- 清理仓库中的明文敏感配置

### 建议改造

- 统一使用 `UserPrincipal.id` 作为用户身份来源
- 抽出统一的 `CurrentUser` 访问方式，禁止控制器自行解析用户名
- 增加 `AuthorizationService` 或资源级校验组件
- 对知识库、文档、问答、历史、反馈接口增加 owner/permission 校验
- 统一 JWT 配置单位，明确使用秒或毫秒，不允许混用
- refresh token 刷新时校验会话有效性
- 将数据库密码、Redis 密码、JWT secret、外部 API key 改为环境变量注入

### 验收标准

- 不再出现默认回退用户 `1L`
- 用户无法访问、删除、更新他人的知识库和历史记录
- logout 后 refresh token 不可继续使用
- 代码仓库中不再保留敏感明文配置

## 7.2 阶段二：文档索引链路重构

### 目标

- 将“上传”和“索引”职责拆开
- 建立完整的文档索引状态机
- 让文档、chunk、向量数据形成闭环

### 建议改造

- 上传接口只做：
  - 文件校验
  - 创建 `document` 记录
  - 保存文件引用或原始内容引用
  - 提交异步索引任务
- 异步索引任务负责：
  - 解析文档
  - 计算内容哈希
  - 分块
  - 批量 embedding
  - 批量写入向量库
  - 落库 `document_chunk`
  - 持久化 `vectorId`
  - 更新 `contentHash`、`chunkCount`、`status`
- 删除文档时必须使用知识库实际 `vectorCollection`
- 去重逻辑从 JVM 内存迁移到数据库唯一约束或 Redis/DB 协同机制

### 建议落库字段

#### `document`

- `id`
- `kb_id`
- `uploader_id`
- `title`
- `file_path` 或对象存储 key
- `file_type`
- `content_hash`
- `status`
- `chunk_count`
- 必要的时间字段

#### `document_chunk`

- `document_id`
- `vector_id`
- `chunk_index`
- `content`
- `start_pos`
- `end_pos`
- `metadata`

### 验收标准

- 上传成功后，索引状态可完整追踪
- 删除文档后，对应 chunk 记录和向量记录都能被清理
- 重复文档在多实例条件下也可正确处理
- 控制器中不再保留完整索引编排逻辑

## 7.3 阶段三：RAG 检索与生成链路优化

### 目标

- 提升检索质量
- 提升上下文拼装质量
- 提升回答可解释性与缓存正确性

### 建议改造

- 将检索链路拆为：
  - query embedding
  - recall
  - rerank
  - context assemble
  - prompt build
  - answer generate
- 召回结果在进入 Prompt 前进行：
  - 去重
  - 来源增强
  - token budget 裁剪
  - 噪声片段过滤
- 优化中文场景的重排逻辑，不继续依赖简单空格分词
- 调整 QA 缓存键，纳入：
  - collectionName
  - question
  - topK
  - filter
  - model/provider
- 明确 SSE 问答的历史保存策略

### 验收标准

- 缓存不再出现不同参数错误命中
- 中英文问题在检索质量上明显更稳定
- 上下文数量、来源、裁剪行为可解释
- 流式与非流式问答的审计能力一致

## 7.4 阶段四：测试、稳定性与可观测性

### 目标

- 建立可稳定运行的测试体系
- 明确基础设施能力的业务接入方式
- 提高故障排查效率

### 建议改造

- 将 Redis 相关测试改为：
  - Testcontainers
  - 或按条件显式跳过，而非依赖 jqwik rejection
- 修复 `mvn test` 失败问题，确保 CI 可作为真实质量门禁
- 对异步任务、索引任务、问答链路补充日志与指标
- 明确限流、幂等能力在业务接口的接入点
- 为文档索引、问答链路增加失败重试与告警策略

### 验收标准

- `mvn test` 可稳定运行
- 测试失败能真实反映问题，而非环境偶然性
- 关键流程具备 trace、耗时、失败原因和任务状态追踪

## 8. 推荐实施顺序

| 顺序 | 阶段 | 原因 |
|------|------|------|
| 1 | 身份与安全整改 | 不先修这一层，后续所有数据改造都不可靠 |
| 2 | 文档索引链路重构 | 当前数据一致性问题最集中 |
| 3 | RAG 检索与生成优化 | 业务效果提升建立在链路稳定之上 |
| 4 | 测试与可观测性建设 | 为后续持续演进提供安全网 |

## 9. Copilot/辅助工具执行约束

后续将本文件交给 Copilot 或其他辅助工具时，必须遵守以下约束：

- 不允许一次性重写全部模块
- 优先按阶段拆任务，每次只改一个主问题
- 不允许在未补测试和验收标准前直接提交大范围重构
- 不允许引入新的明文密钥或临时默认用户兜底
- 不允许继续扩散“控制器承载复杂业务编排”的写法
- 任何数据结构变更必须同步更新 Flyway 脚本和文档

## 10. 分阶段任务清单

## 10.1 阶段一任务

- [x] `AUTH-01` 修复当前用户 ID 获取方式
- [x] `AUTH-02` 增加资源级授权校验
- [x] `AUTH-03` 修复 JWT 时间单位与 refresh 会话校验
- [x] `SEC-01` 清理明文配置并改为环境变量

### AUTH-01 实施说明（2026-03-13）

- 实施范围：`KnowledgeBaseController`、`QAController`、`HistoryController`
- 关键改造：新增统一组件 `CurrentUserService`，只允许从 `UserPrincipal.id` 获取当前用户 ID
- 风险收敛：删除“解析 username 失败回退 `1L`”逻辑，身份异常直接返回未认证错误
- 配套测试：新增 `CurrentUserServiceTest`，覆盖正常 principal、null principal、非 `UserPrincipal`、`id=null` 四种场景

### AUTH-01 验收结果

- [x] 不再出现默认回退用户 `1L`
- [x] 控制器中不再存在通过 `Long.parseLong(userDetails.getUsername())` 获取用户 ID 的逻辑
- [x] 关键身份提取逻辑已具备单元测试覆盖

### AUTH-01 风险与后续

- 当前仅解决“身份提取正确性”，尚未覆盖资源级权限（`AUTH-02`）
- 下一步应引入统一资源授权组件，拦截知识库/历史/反馈的越权访问

### AUTH-02 实施说明（2026-03-13）

- 实施范围：`KnowledgeBaseController`、`QAController`、`HistoryController`
- 新增组件：`AuthorizationService`
- 权限模型落地：
  - 知识库读取：owner / public / `READ` 可访问
  - 知识库写入（文档上传、删除）：owner / `WRITE`
  - 知识库管理（更新、删除）：owner / `ADMIN`
  - 历史记录与反馈：仅 owner（`history.userId == currentUserId`）
- 控制器接入方式：在业务执行前统一进行资源级校验，失败返回 `AUTH_004`（403）
- 配套测试：新增 `AuthorizationServiceTest`，覆盖读/写/管权限与历史 owner 校验

### AUTH-02 验收结果

- [x] 已登录但无权限用户无法访问私有知识库内容
- [x] 已登录但无写权限用户无法上传/删除文档
- [x] 已登录但无管理权限用户无法更新/删除知识库
- [x] 非 owner 用户无法查看/删除他人历史或操作其反馈
- [x] 授权核心逻辑已具备单元测试覆盖

### AUTH-02 风险与后续

- 当前授权主要落在 Controller 入口，后续可逐步下沉到 Application Service 层，减少重复校验
- 文档删除仍未校验 `docId` 与 `kbId` 归属一致性（属于 `DOC-03` 链路，可在阶段二一起修）

### AUTH-03 实施说明（2026-03-13）

- 实施范围：`AuthServiceImpl`、`application.yml`、`application-test.yml`
- 时间单位统一：`jwt.access-token-expiration` / `jwt.refresh-token-expiration` 全部统一为秒
- 会话校验增强：refresh 时新增 Redis 会话校验，要求
  - session key 存在
  - session 中 `refreshToken` 与请求 token 完全一致
- 登出失效增强：logout 时除了拉黑 access token，还会拉黑当前 session 中的 refresh token，再删除会话
- 会话 TTL 对齐：session 过期时间改为 refresh token 生命周期，避免 token 与会话生命周期错位
- 配套测试：新增 `AuthServiceImplTest`，覆盖 session 缺失/不匹配拒绝刷新与登录响应 `expiresIn` 的秒单位行为

### AUTH-03 验收结果

- [x] JWT 过期时间配置与代码使用单位一致（秒）
- [x] logout 后 refresh token 无法继续刷新
- [x] refresh token 刷新必须通过会话有效性校验
- [x] rag-auth 与 rag-admin 回归测试通过

### AUTH-03 风险与后续

- 当前 refresh 会话校验依赖 Redis，可在后续增加 Redis 异常场景的降级策略评估
- 后续可补充集成测试，验证真实登录-登出-刷新完整链路（非 mock）

### SEC-01 实施说明（2026-03-13）

- 实施范围：`rag-admin/src/main/resources/application.yml`、`JwtProperties`、`README.md`
- 配置治理：将数据库、Redis、JWT、第三方模型 API Key 全部改为环境变量注入
- 风险收敛：移除配置中的真实/可用默认密钥，避免仓库泄露直接导致生产风险
- 文档同步：补充 README 的环境变量清单，便于本地和部署配置

### SEC-01 验收结果

- [x] 主配置中不再包含明文数据库密码
- [x] 主配置中不再包含明文 Redis 密码
- [x] 主配置中不再包含明文 JWT 密钥
- [x] 主配置中不再包含第三方 API 明文 Key

### SEC-01 风险与后续

- 当前 `docker-compose.yml` 与部分指南文档仍包含本地开发默认密码（非生产），后续可在阶段四统一治理为 `.env` 注入
- 建议在 CI 中增加 secret scan（如 gitleaks）防止回归

## 13. 可执行实现步骤 Todo（阶段化）

> 目标：按“每次只改一条主链路”推进，做到“改造-测试-验收-文档同步”闭环。

### 13.1 阶段一（身份与安全）

1. `AUTH-01`（已完成）
  - 统一当前用户 ID 读取入口
  - 移除默认用户兜底
2. `AUTH-02`
  - 设计 `AuthorizationService`：`checkKbOwner(kbId, userId)`、`checkHistoryOwner(historyId, userId)`
  - 在 Controller 入口接入 owner 校验
  - 补越权测试（401/403）
3. `AUTH-03`
  - 统一 access/refresh 过期单位（秒或毫秒二选一）
  - 刷新 token 时校验会话状态与黑名单
  - 增加 logout 后 refresh 失效测试
4. `SEC-01`
  - 清理仓库明文密钥
  - 改为环境变量注入并补充配置说明

### 13.2 阶段二（文档索引链路）

1. `DOC-01`
  - Controller 仅保留上传参数校验 + 提交任务
  - 新建应用服务承接索引编排
2. `DOC-02`
  - 增加 `contentHash`、chunk/vector 关联落库
  - 处理失败状态统一写回 `FAILED`
3. `DOC-03`
  - 删除文档时改用知识库真实 `vectorCollection`
4. `DOC-04`
  - 去重从 JVM 内存迁移到 DB 唯一约束或 Redis
5. `DOC-05`
  - 增加上传大小、类型白名单、恶意输入防护

### 13.3 阶段三（RAG 质量）

1. `RAG-01` 优化中文重排
2. `RAG-02` 增加 token budget + 上下文去重
3. `RAG-03` 完善 QA 缓存键参数维度
4. `RAG-04` 打通查询统计更新闭环
5. `RAG-05` 统一流式与非流式历史沉淀

### 13.4 阶段四（测试与可观测）

1. `TEST-01` Redis 相关测试改造（Testcontainers 或条件跳过）
2. `INFRA-01` 在业务接口落地限流/幂等/异步能力
3. `OBS-01` 补充 trace、耗时、任务状态与告警

## 10.2 阶段二任务

- [x] `DOC-01` 控制器瘦身，抽出上传应用服务
- [x] `DOC-02` 补全文档和分块持久化
- [x] `DOC-03` 修复向量集合名使用错误
- [x] `DOC-04` 将去重逻辑迁移出 JVM 内存
- [x] `DOC-05` 增加文件上传边界控制

## 10.3 阶段三任务

- [ ] `RAG-01` 优化重排逻辑
- [ ] `RAG-02` 增加 token budget 和上下文组装策略
- [ ] `RAG-03` 修复 QA 缓存键设计
- [ ] `RAG-04` 补齐查询统计闭环
- [ ] `RAG-05` 补齐流式问答历史策略

## 10.4 阶段四任务

- [ ] `TEST-01` 修复 Redis 相关 property tests
- [ ] `INFRA-01` 让限流/幂等/异步能力真正落地
- [ ] `OBS-01` 增加观测性与任务诊断能力

## 11. 验收检查表

每次阶段性提交前至少完成以下检查：

- [ ] 相关文档已更新
- [ ] 新增或修改的配置项已说明
- [ ] 核心链路已有回归测试
- [ ] 无新增明文敏感信息
- [ ] 无新增控制器臃肿编排逻辑
- [ ] 关键失败路径有明确状态和日志
- [ ] 如涉及数据结构变更，已同步迁移脚本

## 12. 变更记录

### 2026-03-13

- 初始化文档
- 基于一次完整后端代码审查结果建立优化路线
- 明确四个阶段的长期重构方向：
  - 身份与安全整改
  - 文档索引链路重构
  - RAG 检索与生成优化
  - 测试、稳定性与可观测性建设
- 完成 `AUTH-01` 第一轮落地：统一 `CurrentUserService`，移除控制器默认 `1L` 兜底，并补充单元测试
- 完成 `AUTH-02` 第一轮落地：新增 `AuthorizationService` 并在知识库/问答/历史接口接入资源级授权校验，补充授权单元测试
- 完成 `AUTH-03` 第一轮落地：统一 JWT 时间单位为秒，补充 refresh 会话一致性校验，并修复 logout 后 refresh 可继续使用问题
- 完成 `SEC-01` 第一轮落地：主配置改为环境变量注入，移除明文数据库/Redis/JWT/API key
- 新增“可执行实现步骤 Todo（阶段化）”用于后续逐项推进和验收- 完成 `DOC-03` 落地：`DocumentServiceImpl.delete()` 改用 KB 真实 `vectorCollection`，移除硬编码 `kb_{kbId}` 逻辑
- 完成 `DOC-05` 落地：`application.yml` 新增 multipart 50MB 限制，Controller 增加文件非空/文件名校验，Service 增加文件类型白名单校验
- 完成 `DOC-02` 落地：`DocumentService` 新增 `updateContentHash()`，向量写入后持久化 `DocumentChunk` 记录并回写 `contentHash`
- 完成 `DOC-04` 落地：异步任务内通过 `documentService.getByContentHash()` 对 DB 做去重校验，覆盖重启后 in-memory map 失效场景
- 完成 `DOC-01` 落地：抽取 `DocumentIndexingService` / `DocumentIndexingServiceImpl`，Controller 只保留鉴权+文件校验，索引编排完全移入应用服务层
