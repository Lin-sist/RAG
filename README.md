# Enterprise RAG QA System

企业内部知识库问答系统（RAG），支持「文档上传 → 解析分块 → 向量检索 → 大模型生成回答 → 历史追踪/反馈」。

当前仓库已完成大部分核心链路，适合作为可运行版本进行演示与迭代优化。

## 1. 项目概览

- 后端：Java 17 + Spring Boot 3.2.1（Maven 多模块）
- 前端：Vue 3 + Vite + TypeScript + Element Plus
- 核心能力：
	- JWT 登录认证与刷新
	- 知识库管理（CRUD）
	- 文档上传与异步处理
	- 多格式文档解析与切片
	- Embedding 向量化与向量检索
	- RAG 同步问答 + SSE 流式问答
	- 问答历史与反馈

## 2. 仓库结构

```text
.
├─ rag-admin      # 主应用入口 + API Controller + 统一配置
├─ rag-auth       # 认证鉴权模块（JWT、过滤器、黑名单）
├─ rag-common     # 通用能力（响应封装、异常、Redis、限流、异步任务、Trace）
├─ rag-document   # 文档解析与切片模块（PDF/Markdown/Word/代码/纯文本）
├─ rag-core       # RAG 核心（Embedding、向量库适配、检索、生成）
├─ rag-frontend   # 前端项目（Vue3 + Vite）
├─ docs           # 使用文档与接口调用说明
└─ docker-compose.yml
```

## 3. 技术栈

### 后端基础
- Java 17
- Spring Boot 3.2.1
- Spring Security
- MyBatis-Plus 3.5.5
- Flyway（数据库迁移）
- Redis（缓存、限流、异步任务状态、黑名单）

### RAG / AI 能力
- LLM：OpenAI 兼容接口（当前配置可接 NVIDIA NIM），支持 Qwen 配置
- Embedding：OpenAI / Qwen / BGE（当前默认 openai provider）
- 向量数据库适配：Milvus / Qdrant / Elasticsearch（当前默认 Milvus）

### 基础设施（docker-compose）
- MySQL 8.0
- Redis 7
- Milvus 2.3.4（依赖 etcd + minio）

### 前端
- Vue 3.5
- TypeScript 5.7
- Vite 6
- Pinia + Vue Router + Axios
- Element Plus

## 4. 核心业务流程

```text
登录 -> 创建知识库 -> 上传文档 -> 异步处理(解析/切片/向量化/入库) -> 提问 -> 查看历史/反馈
```

文档处理是异步任务，上传后返回 `taskId`，通过任务接口轮询状态。

## 5. 已实现的主要接口（摘要）

### 认证
- `POST /auth/login`
- `POST /auth/logout`
- `POST /auth/refresh`

### 知识库与文档
- `POST /api/knowledge-bases`
- `GET /api/knowledge-bases`
- `GET /api/knowledge-bases/{id}`
- `PUT /api/knowledge-bases/{id}`
- `DELETE /api/knowledge-bases/{id}`
- `POST /api/knowledge-bases/{id}/documents`（multipart 上传）

### 问答
- `POST /api/qa/ask`（同步）
- `POST /api/qa/ask/stream`（SSE 流式）

### 历史与反馈
- `GET /api/history`
- `GET /api/history/{id}`
- `POST /api/history/{id}/feedback`

### 异步任务
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/result`

> 完整接口示例见 `docs/API-调用教程.md` 与 Swagger 页面。

## 6. 快速启动

### 6.1 启动依赖服务

在项目根目录执行：

```bash
docker compose up -d
```

### 6.2 启动后端

方式一（推荐）：IDE 运行 `RagQaApplication.main()`

方式二（Maven）：

```bash
mvn -pl rag-admin -am install -DskipTests
mvn -f rag-admin/pom.xml spring-boot:run
```

方式三（一键启动，推荐 Linux 本地开发）：

```bash
chmod +x start_backend.sh
./start_backend.sh --with-docker
```

说明：
- 脚本会自动读取 `.env.local`（若存在），并自动映射 MySQL/Redis 端口到 Spring 环境变量。
- 脚本默认使用 `root/123456`（MySQL）与 `123456`（Redis）作为本地开发密码，可通过环境变量覆盖。

后端默认地址：`http://localhost:8080`

Swagger：`http://localhost:8080/swagger-ui.html`

### 6.3 启动前端

```bash
cd rag-frontend
npm install
npm run dev
```

如果 `npm install` 报 `CERT_HAS_EXPIRED` 或仍访问 `r2.cnpmjs.org`：

```bash
npm config set registry https://registry.npmjs.org/
cd rag-frontend
rm -f package-lock.json
npm install --registry=https://registry.npmjs.org/
```

说明：旧的 `package-lock.json` 可能锁定了历史镜像地址，单独切换 registry 不足以生效，需重建 lock 文件。

### 6.4 Windows 端统一启动流程（PowerShell）

在项目根目录执行以下步骤：

1) 准备本机端口配置（首次执行）

```powershell
Copy-Item .env.example .env.local
```

如本机端口冲突，请修改 `.env.local` 中的 `MYSQL_HOST_PORT` / `REDIS_HOST_PORT` / `MILVUS_*` 等端口。

2) 一键启动依赖服务 + 后端

```powershell
./start-backend.ps1 -WithDocker
```

说明：
- 脚本会自动读取 `.env.local`，并映射数据库与 Redis 连接参数。
- 若提示脚本执行策略受限，可先在当前 PowerShell 会话执行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

3) 新开一个 PowerShell 窗口，启动前端

```powershell
cd rag-frontend
npm install
npm run dev
```

前端默认地址：`http://127.0.0.1:5173`
后端默认地址：`http://localhost:8080`
Swagger：`http://localhost:8080/swagger-ui.html`

## 7. 默认数据与账号

Flyway 会自动执行初始化脚本（`V1/V2/V3`），包含：
- 基础表结构（用户、角色、权限、知识库、文档、历史、任务等）
- 默认角色与权限
- 默认管理员账号（可在迁移脚本中查看）

建议在本地联调时根据实际环境修改默认密码与敏感配置。

## 8. 配置说明（关键项）

配置文件：`rag-admin/src/main/resources/application.yml`

重点分组：
- `spring.datasource`：MySQL 连接
- `spring.data.redis`：Redis 连接
- `jwt.*`：访问令牌与刷新令牌配置
- `rag.llm.*`：大模型生成参数
- `rag.embedding.*`：向量化模型参数
- `rag.vectorstore.*`：向量存储类型与连接参数
- `proxy.*`：外部 API 代理开关

建议通过环境变量覆盖 API Key，不要在生产环境明文存放。

推荐至少配置以下环境变量：

- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DB`
- `JWT_SECRET`
- `NVIDIA_API_KEY` / `QWEN_API_KEY`

## 9. 当前状态与后续优化方向

当前版本已具备端到端主链路，但仍有可优化空间：

- 异常处理与边界场景（如外部模型超时、向量库波动）
- 任务重试与可观测性（指标、告警、链路追踪）
- 向量检索效果调优（分块策略、召回参数、重排）
- 权限粒度与多租户隔离策略
- 前端类型安全与交互细节完善

## 10. 常用文档

- `docs/API-调用教程.md`
- `docs/frontend-spec.md`
- `docs/frontend-v2-design.md`

---

如果你准备继续迭代该项目，建议优先从「稳定性（错误重试与观测）+ 检索效果（分块和召回调优）」两条线并行推进。