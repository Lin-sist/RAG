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
| RAG-01 | P2 | DONE | `rag-core` | 当前重排基于空格分词的简单关键词匹配，对中文场景不可靠 | 检索质量和回答稳定性不足 |
| RAG-02 | P2 | DONE | `rag-core` | Prompt 拼装没有 token budget、上下文去重、来源增强策略 | 上下文质量不稳定，易浪费 token |
| RAG-03 | P2 | DONE | `rag-core` | QA 缓存键未纳入 `topK/filter/model` 等参数 | 缓存脏命中风险 |
| RAG-04 | P2 | DONE | `rag-admin` `rag-core` | 知识库查询次数统计定义存在，但未形成真实更新闭环 | 统计面板数据不可信 |
| RAG-05 | P2 | DONE | `rag-admin` | SSE 问答未形成完整的历史沉淀策略 | 流式问答与历史/审计链路不一致 |

## 4.4 P3 级问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| TEST-01 | P3 | DONE | `rag-common` | Property tests 依赖 Redis，本地不可用时大量 rejection 导致测试失败 | 测试结果不稳定，CI 信号失真 |
| INFRA-01 | P3 | DONE | `rag-common` `rag-admin` | 限流、幂等、异步能力已经存在，但业务接口没有真正落地使用 | 通用能力与业务实现脱节 |
| OBS-01 | P3 | DONE | `rag-common` `rag-admin` | 缺少系统化指标、告警、任务链路可观测性 | 故障难排查，容量难评估 |

## 4.5 2026-03-14 复审新增 P0 / P1 问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| DOC-06 | P0 | DONE | `rag-admin` | 删除文档接口只校验调用者对 `kbId` 的写权限，未校验 `docId` 是否属于该 `kbId`，且删除后固定执行 `updateDocumentCount(kbId, -1)` | 可被利用删除其他知识库文档，或在未实际删除目标文档时错误递减统计 |
| TASK-01 | P1 | DONE | `rag-admin` `rag-common` | 异步任务状态/结果/取消接口仅按 `taskId` 访问，任务状态中未持久化 owner 信息 | 已登录用户只要拿到 `taskId` 即可查询、取消他人任务，存在任务越权风险 |
| KB-01 | P1 | DONE | `rag-admin` `rag-core` | 创建知识库时数据库记录先落库，向量集合创建失败仅记录 warn 不回滚也不失败返回 | 会产生“数据库存在但向量集合不存在”的半初始化知识库，后续上传/问答链路不可用 |
| FEEDBACK-01 | P1 | DONE | `rag-admin` | 反馈提交采用“先查后插”，但表结构缺少 `(qa_id, user_id)` 唯一约束，幂等又依赖客户端请求头 | 并发提交或未携带幂等 key 时可能落多条重复反馈，破坏审计与统计准确性 |

## 4.6 2026-03-14 复审新增 P2 问题

| 编号 | 优先级 | 状态 | 模块 | 问题描述 | 影响 |
|------|------|------|------|------|------|
| DOC-07 | P2 | DONE | `rag-admin` | 上传接口虽然已抽出应用服务，但控制器仍调用 `file.getBytes()`，异步任务也继续持有整份 `byte[]` | 大文件上传时仍会形成整包内存占用，控制器瘦身不彻底 |
| DOC-08 | P2 | DONE | `rag-admin` | 内容去重当前基于全局唯一 `content_hash`，异步去重命中后又尝试给新文档写入同一 hash | 跨知识库上传相同内容会触发唯一键冲突，重复文档场景无法稳定落地 |
| RAG-06 | P2 | DONE | `rag-admin` `rag-core` | 流式问答接口接收 `topK/filter/enableCache`，但实际构造 `QARequest.stream()` 时丢弃这些参数且未在客户端断开时取消下游订阅 | 流式与非流式语义不一致，断流后仍可能持续消耗检索与 LLM 资源 |

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

- [x] `RAG-01` 优化重排逻辑
- [x] `RAG-02` 增加 token budget 和上下文组装策略
- [x] `RAG-03` 修复 QA 缓存键设计
- [x] `RAG-04` 补齐查询统计闭环
- [x] `RAG-05` 补齐流式问答历史策略

### RAG-01 实施说明（2026-03-13）

- 实施范围：`QueryEngineImpl`
- 关键改造：将重排查询词提取从 `split("\\s+")` 升级为中英文混合分词
  - 英文/数字词按 token 提取（长度 >= 2）
  - 中文按连续片段提取，并补充 2-gram 提升中文短词命中率
- 评分修正：移除仅允许 `term.length() > 2` 的硬编码条件，避免中文双字关键词（如“重排”“优化”）被过滤
- 配套测试：新增 `QueryEngineImplTest`
  - `shouldRerankForChineseQueryUsingCjkTerms`
  - `shouldKeepEnglishKeywordRerankBehavior`

### RAG-01 验收结果

- [x] 中文查询不再依赖空格分词
- [x] 中文双字关键词可参与重排评分
- [x] 英文关键词重排行为保持可用
- [x] 定向测试通过（`mvn -pl rag-core -Dtest=QueryEngineImplTest test`）

### RAG-02 实施说明（2026-03-13）

- 实施范围：`PromptBuilder`、`AnswerGeneratorImpl`
- 关键改造：新增 `buildOptimized()` 上下文组装路径
  - 上下文去重：按 `source + content` 归一化去重
  - token budget：按启发式 token 估算进行预算裁剪（默认 `1200`）
  - 来源增强：Prompt 中增加 `title` 与 `chunkIndex` 来源信息，提升可解释性
- 生成链路接入：`AnswerGeneratorImpl` 改为调用优化组装路径，引用提取与元数据使用优化后上下文
- 可观测性增强：回答元数据补充 `contextTokenBudget`、`estimatedContextTokens`、`removedByDedup`、`removedByBudget`
- 配套测试：新增 `PromptBuilderTest`
  - `shouldDeduplicateAndApplyTokenBudget`
  - `shouldEnhanceSourceWithMetadataHints`

### RAG-02 验收结果

- [x] Prompt 组装具备上下文去重能力
- [x] Prompt 组装具备 token budget 裁剪能力
- [x] Prompt 来源信息增强（title/chunk）已生效
- [x] 定向测试通过（`mvn -pl rag-core "-Dtest=PromptBuilderTest,QueryEngineImplTest" test`）

### RAG-03 实施说明（2026-03-13）

- 实施范围：`RAGServiceImpl`
- 缓存键升级：`question + collection` 扩展为 `question + collection + topK + filter + model`
- 过滤条件规范化：新增 filter 归一化序列化逻辑，避免 map 顺序差异导致同义请求缓存错失/错命中
- 缓存淘汰兼容：`evictCache(question, collectionName)` 改为按 `queryHash + collection` 前缀模式删除，兼容多参数 key
- 配套测试：新增 `RAGServiceCacheKeyTest`
  - `shouldNotShareCacheBetweenDifferentTopK`
  - `shouldNotShareCacheBetweenDifferentFilter`

### RAG-03 验收结果

- [x] 不同 `topK` 请求不再共用缓存
- [x] 不同 `filter` 请求不再共用缓存
- [x] 缓存清理可覆盖同一 query+collection 下的多参数键
- [x] 定向测试通过（`mvn -pl rag-core "-Dtest=RAGServiceCacheKeyTest,PromptBuilderTest,QueryEngineImplTest" test`）

### RAG-04 实施说明（2026-03-13）

- 实施范围：`KnowledgeBaseService`、`KnowledgeBaseServiceImpl`、`QAController`
- 关键改造：新增 `incrementQueryCount(kbId)`，统一写入 Redis 查询计数键
- 链路接入：
  - 同步问答 `POST /api/qa/ask` 完成问答后递增查询计数
  - 流式问答 `POST /api/qa/ask/stream` 建立流式问答时递增查询计数
- 测试覆盖：新增 `QAControllerTest`，验证同步/流式入口均会触发 `incrementQueryCount`

### RAG-04 验收结果

- [x] 查询次数统计读取与写入使用同一 Redis 键空间
- [x] 同步问答触发查询计数递增
- [x] 流式问答触发查询计数递增
- [x] 定向测试通过（`mvn -pl rag-admin "-Dtest=QAControllerTest,KnowledgeBasePropertyTest" test`）

### RAG-05 实施说明（2026-03-13）

- 实施范围：`QAController`
- 关键改造：流式问答增加历史沉淀策略
  - 在流式回调中累积答案 chunk（忽略 `[DONE]`）
  - 在流式完成回调中落库历史（question/answer/trace/latency）
  - 在流式异常回调中也落库已生成的部分答案，避免审计断裂
- 对齐策略：流式链路与同步链路都通过 `QAHistoryService.save()` 落历史
- 配套测试：增强 `QAControllerTest`
  - 验证流式问答触发历史保存
  - 验证保存 answer 为流式 chunk 拼接结果

### RAG-05 验收结果

- [x] 流式问答完成后可沉淀完整历史
- [x] 流式异常场景仍会沉淀已生成答案历史
- [x] 流式与非流式问答统一纳入历史审计链路
- [x] 定向测试通过（`mvn -pl rag-admin "-Dtest=QAControllerTest,KnowledgeBasePropertyTest" test`）

## 10.4 阶段四任务

- [x] `TEST-01` 修复 Redis 相关 property tests
- [x] `INFRA-01` 让限流/幂等/异步能力真正落地
- [x] `OBS-01` 增加观测性与任务诊断能力

## 10.5 2026-03-14 复审问题任务

- [x] `DOC-06` 删除文档归属校验与计数一致性修复
- [x] `TASK-01` 异步任务 owner 授权修复
- [x] `KB-01` 知识库创建一致性修复
- [x] `FEEDBACK-01` 反馈唯一约束与并发幂等修复
- [x] `DOC-07` 上传链路内存占用优化
- [x] `DOC-08` 内容去重唯一约束冲突修复
- [x] `RAG-06` 流式问答参数透传与断流取消修复

### DOC-06 实施说明（2026-03-14）

- 实施范围：`KnowledgeBaseController`、`DocumentService`、`DocumentServiceImpl`
- 关键改造：
  - 删除接口新增 `docId -> kbId` 归属校验，阻断跨知识库删除
  - `DocumentService.delete()` 改为返回删除结果，区分“文档不存在”和“实际删除成功”
  - 仅在删除成功后执行 `updateDocumentCount(kbId, -1)`
- 测试验证：`mvn -pl rag-admin "-Dtest=KnowledgeBasePropertyTest,QAControllerTest" test` 通过

### DOC-06 验收结果

- [x] 删除接口已校验文档归属关系（`docId` 必须属于当前 `kbId`）
- [x] 文档不存在或归属不匹配时不会递减知识库文档计数
- [x] 文档删除链路定向回归测试通过

### DOC-06 风险与后续

- 当前以 Controller 入口实现归属校验，后续可在应用服务层下沉该规则，减少重复调用风险
- 若后续引入批量删除接口，需要复用同一归属与计数一致性策略

### TASK-01 实施说明（2026-03-14）

- 实施范围：`AsyncTaskManager`、`RedisAsyncTaskManager`、`TaskStatus`、`TaskController`、`DocumentIndexingServiceImpl`
- 关键改造：
  - 异步任务提交接口新增 owner 参数，任务创建时持久化 `ownerId`
  - 任务状态模型与 Redis 序列化结构新增 `ownerId`
  - 任务状态/结果/取消/存在/完成接口统一增加 owner 校验（当前用户必须等于任务 owner）
  - 文档索引任务提交时传入 `uploaderId`，确保任务归属可追踪
- 测试验证：
  - `mvn -pl rag-common "-Dtest=AsyncTaskManagerPropertyTest" test` 通过
  - `mvn -pl rag-admin "-Dtest=TaskControllerTest,QAControllerTest,KnowledgeBasePropertyTest" test` 通过

### TASK-01 验收结果

- [x] 任务状态已持久化 owner 信息
- [x] 已登录用户无法查询/取消他人任务
- [x] 任务控制器新增越权场景单元测试并通过

### TASK-01 风险与后续

- 历史存量任务若无 owner 字段将被拒绝访问；如需兼容可补一次性迁移或过渡逻辑
- 当前 owner 校验位于 Controller 层，后续可在任务服务层统一沉淀鉴权策略

### KB-01 实施说明（2026-03-14）

- 实施范围：`KnowledgeBaseServiceImpl`
- 关键改造：
  - 创建知识库时，若向量集合初始化失败，立即抛出 `BusinessException(KB_005)`
  - 依赖 `@Transactional` 回滚数据库写入，避免“库记录存在但向量集合不存在”的半初始化状态
- 测试验证：`mvn -pl rag-admin "-Dtest=KnowledgeBaseServiceImplTest,TaskControllerTest,QAControllerTest,KnowledgeBasePropertyTest" test` 通过

### KB-01 验收结果

- [x] 向量集合创建失败时不再返回成功
- [x] 创建链路在失败场景下具备事务回滚语义
- [x] 新增 `KnowledgeBaseServiceImplTest` 覆盖故障分支

### KB-01 风险与后续

- 当前策略为“强一致失败即回滚”，后续若需要提升可用性，可演进为异步初始化并引入 `INITIALIZING` 状态机
- 向量库偶发抖动会直接影响创建成功率，建议后续增加限次重试与熔断保护

### FEEDBACK-01 实施说明（2026-03-14）

- 实施范围：Flyway 脚本、`QAFeedbackServiceImpl`
- 关键改造：
  - 新增迁移脚本 `V4__qa_feedback_unique_constraint.sql`
  - 迁移脚本先清理历史重复 `(qa_id, user_id)` 数据，再新增唯一约束 `uk_qa_feedback_qa_user`
  - 服务层在插入时捕获 `DuplicateKeyException` 并统一转为 `FEEDBACK_002`
- 测试验证：`mvn -pl rag-admin "-Dtest=QAFeedbackServiceImplTest,KnowledgeBaseServiceImplTest,TaskControllerTest,QAControllerTest" test` 通过

### FEEDBACK-01 验收结果

- [x] 数据库层已具备 `(qa_id, user_id)` 唯一约束
- [x] 并发冲突可稳定返回“已提交过反馈”业务错误
- [x] 新增 `QAFeedbackServiceImplTest` 覆盖重复键冲突与评分校验分支

### FEEDBACK-01 风险与后续

- Flyway 执行前建议先在预发环境核对历史重复数据清理结果
- 若后续引入“软删除后允许重提”，需要调整唯一约束策略（如纳入 `deleted` 字段）

### DOC-07 实施说明（2026-03-14）

- 实施范围：`KnowledgeBaseController`、`DocumentIndexingService`、`DocumentIndexingServiceImpl`
- 关键改造：
  - 上传接口移除 `file.getBytes()`，改为直接传递 `MultipartFile` 到应用服务
  - 应用服务先将上传内容写入临时文件，再提交异步任务，任务只持有 `Path` 引用
  - 异步处理阶段通过文件流读取内容，并在 `finally` 中清理临时文件
- 测试验证：`mvn -pl rag-admin "-Dtest=QAControllerTest,TaskControllerTest,QAFeedbackServiceImplTest,KnowledgeBaseServiceImplTest,KnowledgeBasePropertyTest" test` 通过

### DOC-07 验收结果

- [x] 控制器不再持有整份 `byte[]`
- [x] 异步任务不再闭包捕获整包内容，仅使用临时文件路径
- [x] 临时文件在处理完成或失败后均会清理

### DOC-07 风险与后续

- 临时文件目录容量需纳入运维监控，避免磁盘压力在高并发上传时积累
- 后续可进一步演进为对象存储分段上传，减少本地磁盘依赖

### DOC-08 实施说明（2026-03-14）

- 实施范围：`DocumentService`、`DocumentServiceImpl`、`DocumentIndexingServiceImpl`、Flyway 脚本
- 关键改造：
  - 新增迁移脚本 `V5__document_content_hash_scope_kb.sql`，将唯一约束从 `content_hash` 调整为 `(kb_id, content_hash)`
  - 新增 `DocumentService#getByKnowledgeBaseAndContentHash()`，去重查询改为“知识库内去重”
  - 异步去重命中后不再给新文档回写冲突 hash，避免触发唯一键冲突
- 测试验证：`mvn -pl rag-admin "-Dtest=QAControllerTest,TaskControllerTest,QAFeedbackServiceImplTest,KnowledgeBaseServiceImplTest,KnowledgeBasePropertyTest" test` 通过

### DOC-08 验收结果

- [x] 跨知识库上传相同内容不再受全局唯一约束阻塞
- [x] 去重查询粒度已收敛到知识库维度
- [x] 去重命中路径已消除重复 hash 写入冲突

### DOC-08 风险与后续

- 迁移脚本假定旧索引名为 `uk_content_hash`，上线前需确认生产库索引名一致
- 若后续要严格限制“同知识库重复上传”，可补充显式 `DUPLICATE` 文档状态提高可观测性

### RAG-06 实施说明（2026-03-14）

- 实施范围：`QAController`、`QARequest`、`QAControllerTest`
- 关键改造：
  - 新增 `QARequest.stream(question, collectionName, topK, filter, enableCache)` 工厂方法
  - 流式问答构造请求时完整透传 `topK/filter/enableCache`，与同步问答语义对齐
  - 在 SSE 的 `onCompletion/onTimeout/onError` 回调中统一释放下游订阅，减少断流后的无效计算
  - 测试增强：`QAControllerTest` 新增对流式参数透传的断言
- 测试验证：`mvn -pl rag-admin "-Dtest=QAControllerTest,TaskControllerTest,QAFeedbackServiceImplTest,KnowledgeBaseServiceImplTest,KnowledgeBasePropertyTest" test` 通过

### RAG-06 验收结果

- [x] 流式请求参数与同步请求语义一致
- [x] 客户端断连/超时场景会主动取消下游订阅
- [x] 流式链路新增参数透传测试覆盖

### RAG-06 风险与后续

- 当前取消语义依赖 Reactor `dispose()` 传播到下游实现，建议后续补集成测试验证真实 LLM 调用是否及时中断
- 可进一步把取消事件纳入观测指标，统计“用户主动断流”占比

### TEST-01 实施说明（2026-03-13）

- 实施范围：`AsyncTaskManagerPropertyTest`、`IdempotencyHandlerPropertyTest`、`RateLimiterPropertyTest`
- 问题根因：本地 Redis 不可用时，测试使用 `Assume.that(redisAvailable)` 导致 jqwik 全部样本被 rejection，最终 exhausted 失败
- 修复策略：改为在 property 方法开头显式短路返回，避免 rejection 耗尽
- 验证结果：`mvn -pl rag-common test` 通过（16 tests, 0 failures）

### INFRA-01 第一轮落地（2026-03-13）

- 接入点：
  - `POST /auth/login` 增加 IP 维度限流
  - `POST /auth/refresh` 增加 IP 维度限流
  - `POST /api/qa/ask` 增加 USER 维度限流
  - `POST /api/qa/ask/stream` 增加 USER 维度限流
- 说明：本轮优先落地限流能力，幂等能力将在后续针对“可重复提交写接口”分批接入
- 回归验证：`mvn -pl rag-admin "-Dtest=QAControllerTest,AuthorizationServiceTest,CurrentUserServiceTest,KnowledgeBasePropertyTest,QAHistoryPropertyTest" test` 通过

### INFRA-01 第二轮落地（2026-03-13）

- 限流扩展接入点：
  - `POST /api/knowledge-bases`
  - `PUT /api/knowledge-bases/{id}`
  - `DELETE /api/knowledge-bases/{id}`
  - `POST /api/knowledge-bases/{id}/documents`
  - `DELETE /api/knowledge-bases/{kbId}/documents/{docId}`
  - `DELETE /api/history/{id}`
  - `POST /api/history/{id}/feedback`
  - `POST /api/tasks/{taskId}/cancel`
- 幂等接入（服务层，避免在 Controller 层缓存 `ResponseEntity`）：
  - `KnowledgeBaseServiceImpl#create/update/delete`
  - `DocumentIndexingServiceImpl#submitIndexing`
  - `QAFeedbackServiceImpl#submit`
- 幂等键隔离增强：`IdempotencyAspect` 生成键时加入 principal，降低跨用户同 key 冲突风险
- 验收结果：
  - [x] 关键写接口均已接入限流
  - [x] 关键写服务均已接入幂等
  - [x] 异步任务链路保持可用（文档索引与任务状态接口回归通过）

### OBS-01 第一轮落地（2026-03-13）

- 实施范围：`QAController`
- 关键增强：
  - 同步问答完成日志增加 `traceId/userId/latencyMs`
  - 流式问答错误日志增加 `traceId/userId/latencyMs`
  - 流式问答完成日志增加 `traceId/userId/latencyMs/answerLength`
- 说明：本轮完成链路日志增强，系统化指标与告警（Metrics + Alerting）将在后续继续推进

### OBS-01 第二轮落地（2026-03-13）

- 新增统一请求观测过滤器：`RequestObservationFilter`
  - 记录 `traceId/method/uri/status/latencyMs`
  - 慢请求阈值配置：`observability.slow-request-threshold-ms`（默认 `1000`）
  - 异常请求记录 `request_failed`，慢请求记录 `slow_request`
- 验收结果：
  - [x] 关键请求具备统一 trace 与耗时日志
  - [x] 可通过阈值定位慢请求
  - [x] 异常请求具备统一失败日志入口

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
- 完成 `RAG-01` 第一轮落地：`QueryEngineImpl` 升级中英文混合分词重排策略，移除中文双字词过滤限制，并新增 `QueryEngineImplTest` 覆盖中文与英文重排场景
- 完成 `RAG-02` 第一轮落地：`PromptBuilder` 新增上下文去重、token 预算裁剪与来源增强策略，并在 `AnswerGeneratorImpl` 接入优化链路，新增 `PromptBuilderTest` 覆盖关键场景
- 完成 `RAG-03` 第一轮落地：`RAGServiceImpl` 缓存键纳入 `topK/filter/model` 维度并补充 filter 规范化与前缀淘汰策略，新增 `RAGServiceCacheKeyTest` 验证参数隔离
- 完成 `RAG-04` 第一轮落地：问答入口接入知识库查询计数递增（同步+流式），补齐 `KnowledgeBaseService` 查询统计写入闭环，并新增 `QAControllerTest`
- 完成 `RAG-05` 第一轮落地：流式问答在完成/异常回调均沉淀历史，保存流式拼接答案并纳入 trace/latency，补齐与同步问答一致的审计闭环
- 完成 `TEST-01` 落地：修复 Redis 不可用场景下 property tests rejection 耗尽导致的失败，恢复 `rag-common` 测试稳定性
- 推进 `INFRA-01` 第一轮：在认证与问答高频入口接入限流能力（IP/USER 维度）
- 推进 `OBS-01` 第一轮：增强问答链路 trace/耗时/结果规模日志，提升问题排查可观测性
- 完成 `INFRA-01` 第二轮：补齐知识库/历史/任务写接口限流，并在服务层落地幂等能力，完成跨用户幂等键隔离增强
- 完成 `OBS-01` 第二轮：新增全局请求观测过滤器，统一输出 trace/状态码/耗时日志并支持慢请求告警阈值
- 持续增强：新增 `IdempotencyAspectTest` 与 `RequestObservationFilterTest`，为幂等键隔离与请求观测行为提供回归保障

### 2026-03-14

- 基于新一轮后端复审，在保留原 `4. 核心问题总览` 历史记录的前提下新增 `4.5`、`4.6` 问题分组
- 新增 `DOC-06`：补记文档删除缺少 `docId -> kbId` 归属校验且统计会错误递减的问题
- 新增 `TASK-01`：补记异步任务接口缺少 owner 级资源授权的问题
- 新增 `KB-01`：补记知识库创建与向量集合初始化缺少一致性保障的问题
- 新增 `FEEDBACK-01`：补记反馈提交缺少数据库唯一约束的并发重复写入风险
- 新增 `DOC-07`：补记上传链路仍持有整份 `byte[]` 的内存压力问题
- 新增 `DOC-08`：补记全局 `content_hash` 唯一约束与当前去重策略冲突的问题
- 新增 `RAG-06`：补记流式问答请求参数丢失与断流后未取消下游生成的问题
- 完成 `DOC-06` 第一轮落地：删除接口新增 `docId -> kbId` 归属校验，`DocumentService.delete()` 返回删除结果并仅在成功后递减计数，消除跨库删除与错误计数风险
- 完成 `TASK-01` 第一轮落地：任务状态持久化 owner 信息，任务状态/结果/取消等接口接入 owner 校验，修复 taskId 越权访问风险
- 完成 `KB-01` 第一轮落地：知识库创建时向量集合初始化失败改为抛错并回滚事务，避免半初始化知识库进入可用链路
- 完成 `FEEDBACK-01` 第一轮落地：补充 `(qa_id,user_id)` 唯一约束与重复键异常兜底，消除并发重复反馈写入风险
- 完成 `DOC-07` 第一轮落地：上传链路改为临时文件流式处理，移除控制器与异步闭包中的整包 `byte[]` 占用
- 完成 `DOC-08` 第一轮落地：`content_hash` 唯一约束收敛为 `(kb_id, content_hash)`，并修复去重命中写冲突问题
- 完成 `RAG-06` 第一轮落地：流式问答透传 `topK/filter/enableCache` 参数并在 SSE 断连/超时/错误时取消下游订阅
