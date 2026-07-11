# RAG Eval Baseline 指南

这个目录用于建立可复现的 RAG baseline evaluation。目标不是马上优化算法，而是先固定一组测试文档、问题、期望来源和指标，让后续每次调整 chunk、query rewrite、rerank、hybrid search 或 prompt 时都能做前后对比。

## 1. 当前可直接用于评测的字段

### `docs/eval/rag_eval_set.jsonl`

每行是一条 JSON 样本，当前字段为：

- `id`：样本编号。
- `question`：要提问的问题。
- `type`：题型，当前包括 `fact`、`definition`、`reasoning`、`multi_hop`、`no_answer`。
- `difficulty`：难度，当前包括 `easy`、`medium`、`hard`。
- `expected_sources`：期望命中的文档名。
- `expected_keywords`：期望答案包含的关键词。
- `expected_answer_points`：期望答案要点，用于人工检查或后续 LLM judge。
- `expected_contexts`：期望召回上下文，每个元素包含 `source` 和 `contains`。
- `should_answer`：是否应该回答。无答案题为 `false`。
- `notes`：样本设计说明。

### `POST /api/qa/debug/retrieve`

该接口只执行检索，不调用 LLM，不保存历史，适合评测检索质量。baseline runner 使用它计算：

- Recall@3
- Recall@5
- MRR
- Top1 source accuracy

debug 输出应包含：

- `question`
- `queryVariants`
- `contexts[].rank`
- `contexts[].score`
- `contexts[].source`
- `contexts[].displaySource`
- `contexts[].documentId`
- `contexts[].chunkId`
- `contexts[].chunkIndex`
- `contexts[].contentPreview`
- `contexts[].metadata`

### `POST /api/qa/ask`

该接口执行完整 RAG 问答。baseline runner 使用它计算：

- answer keyword hit rate
- citation source hit rate
- citation snippet hit rate
- unsupported citation count
- no-answer citation violation count
- no-answer accuracy

可直接使用的字段：

- `data.answer`
- `data.citations`
- `data.contexts`
- `data.metadata.status`
- `data.metadata.citationValidation`
- `data.metadata.validCitations`
- `data.metadata.droppedCitations`
- `data.metadata.citationCoverage`

当前不足：

- `answer` 没有结构化 answer points。
- `citation snippet hit rate` 只能验证 citation 是否能回连到 returned contexts，不等价于逐句事实支持。

## 2. 准备测试知识库

推荐为 baseline 单独创建一个知识库，例如：

```bash
curl -X POST "http://localhost:8080/api/knowledge-bases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "RAG Eval Baseline",
    "description": "Baseline eval knowledge base built from test-data markdown files",
    "isPublic": false
  }'
```

记录响应中的知识库 ID，后续运行脚本时传给 `--kb-id`。

## 3. 上传 test-data 文档

baseline 评测集基于以下三份文档：

- `test-data/java-interview-guide.md`
- `test-data/rag-technology-guide.md`
- `test-data/springboot-basics.md`

上传示例：

```bash
curl -X POST "http://localhost:8080/api/knowledge-bases/<kb-id>/documents" \
  -H "Authorization: Bearer <token>" \
  -F "file=@test-data/java-interview-guide.md" \
  -F "title=java-interview-guide.md"

curl -X POST "http://localhost:8080/api/knowledge-bases/<kb-id>/documents" \
  -H "Authorization: Bearer <token>" \
  -F "file=@test-data/rag-technology-guide.md" \
  -F "title=rag-technology-guide.md"

curl -X POST "http://localhost:8080/api/knowledge-bases/<kb-id>/documents" \
  -H "Authorization: Bearer <token>" \
  -F "file=@test-data/springboot-basics.md" \
  -F "title=springboot-basics.md"
```

上传后需要等待异步索引任务完成。可以在前端任务状态处观察，也可以调用任务查询接口。只有文档状态为 `COMPLETED` 后再运行 eval。

## 4. 本地依赖和环境变量

### Docker 依赖

`docker-compose.yml` 会启动：

- MySQL 8.0：宿主机端口默认 `3306`，root 密码 `123456`，数据库 `rag_qa`。
- Redis 7：宿主机端口默认 `6379`，密码 `123456`。
- Milvus standalone：宿主机端口默认 `19530`，依赖 compose 内的 etcd 和 MinIO。

`.env.example` 只配置宿主机端口，不配置后端连接密码。启动后端时必须显式设置：

- `DB_PASSWORD=123456`
- `REDIS_PASSWORD=123456`
- `NVIDIA_API_KEY=<你的 NVIDIA NIM key>`，当前默认 LLM 和 embedding 都走 NVIDIA OpenAI-compatible API。

如果不用 NVIDIA，也可以按 `application.yml` 改为 Qwen 或其他 provider，但必须保证 embedding provider 和 LLM provider 都可用。

### 可选代理

`application.yml` 当前默认：

```yaml
proxy:
  enabled: true
  host: 127.0.0.1
  port: 7897
```

如果本机没有这个代理，启动后端时加：

```powershell
--proxy.enabled=false
```

也可以在 `.env.local` 或启动环境中设置 `PROXY_ENABLED=false`；`PROXY_HOST`、`PROXY_PORT` 可分别覆盖代理地址和端口。启动评测前，建议先执行只读预检：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --preflight-only `
  --kb-name codex-stage1-repro-eval
```

该命令只验证登录、既有评测 KB 和三份 fixture 的索引状态，不会创建/删除 KB、上传文档，也不会调用 `/api/qa/ask` 或 LLM judge。

## 5. 完整命令顺序

### 5.1 启动基础设施

```powershell
Copy-Item .env.example .env.local
docker compose --env-file .env.local up -d
docker compose ps
```

确认 `rag-mysql`、`rag-redis`、`rag-milvus`、`rag-etcd`、`rag-minio` 都处于 running/healthy 或至少 running。

### 5.2 启动后端

本地完整运行需要：

- MySQL
- Redis
- Milvus
- 可用的 Embedding provider 配置
- 可用的 LLM provider 配置

PowerShell 示例：

```powershell
$env:DB_PASSWORD="123456"
$env:REDIS_PASSWORD="123456"
$env:NVIDIA_API_KEY="<your-nvidia-api-key>"
mvn -pl rag-admin -am spring-boot:run -Dspring-boot.run.arguments="--proxy.enabled=false"
```

也可以先打包再启动：

```powershell
$env:DB_PASSWORD="123456"
$env:REDIS_PASSWORD="123456"
$env:NVIDIA_API_KEY="<your-nvidia-api-key>"
mvn -pl rag-admin -am install -DskipTests
java -jar rag-admin/target/rag-admin-1.0.0-SNAPSHOT.jar --proxy.enabled=false
```

后端入口类是：

```text
rag-admin/src/main/java/com/enterprise/rag/admin/RagQaApplication.java
```

### 5.3 可选启动前端

```powershell
cd rag-frontend
npm install
npm run dev
```

前端不是 eval runner 必需项，但可用于手工观察上传和问答。

### 5.4 登录并创建 eval 知识库

PowerShell 示例：

```powershell
$base="http://localhost:8080"
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$token = $login.data.accessToken

$headers = @{ Authorization = "Bearer $token" }
$kbBody = @{
  name = "RAG Eval Baseline"
  description = "Baseline eval knowledge base built from test-data markdown files"
  isPublic = $false
} | ConvertTo-Json

$kb = Invoke-RestMethod -Method Post -Uri "$base/api/knowledge-bases" -Headers $headers -ContentType "application/json" -Body $kbBody
$kbId = $kb.data.id
$kbId
```

### 5.5 上传三份 test-data 文档

```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/knowledge-bases/$kbId/documents" -Headers $headers -Form @{
  file = Get-Item "test-data/java-interview-guide.md"
  title = "java-interview-guide.md"
}

Invoke-RestMethod -Method Post -Uri "$base/api/knowledge-bases/$kbId/documents" -Headers $headers -Form @{
  file = Get-Item "test-data/rag-technology-guide.md"
  title = "rag-technology-guide.md"
}

Invoke-RestMethod -Method Post -Uri "$base/api/knowledge-bases/$kbId/documents" -Headers $headers -Form @{
  file = Get-Item "test-data/springboot-basics.md"
  title = "springboot-basics.md"
}
```

上传响应中会有 `taskId`。可以查询任务，也可以直接轮询文档列表：

```powershell
Invoke-RestMethod -Method Get -Uri "$base/api/knowledge-bases/$kbId/documents" -Headers $headers |
  ConvertTo-Json -Depth 8
```

等三份文档状态都变成 `COMPLETED` 后再跑评测。如果状态是 `PROCESSING`，继续等待；如果是 `FAILED`，先看后端日志，常见原因是 `NVIDIA_API_KEY` 缺失、embedding 超时、Milvus 未启动或维度不匹配。

### 5.6 运行 eval runner

脚本位置：

```bash
scripts/run_rag_eval.py
```

最小命令：

```bash
python scripts/run_rag_eval.py --kb-id <kb-id>
```

常用参数：

```bash
python scripts/run_rag_eval.py \
  --base-url http://localhost:8080 \
  --kb-id <kb-id> \
  --username admin \
  --password admin123 \
  --top-k 5 \
  --min-score 0.3 \
  --enable-rerank \
  --report docs/eval/reports/baseline-002-local.md \
  --after-report docs/eval/reports/after-citation-validator.md
```

为了避免 LLM API 限流污染干净 baseline，建议把 live eval 拆成两步。

先跑 retrieval-only clean baseline，只调用 `/api/qa/debug/retrieve`，不调用 `/api/qa/ask`：

```powershell
python scripts\run_rag_eval.py --kb-id 6 `
  --skip-ask `
  --report docs\eval\reports\baseline-004-retrieval-only.md `
  --details-json docs\eval\reports\baseline-004-retrieval-details.json `
  --no-overwrite
```

再跑 slow ask baseline，用延迟和 retry 降低 NVIDIA/Qwen/OpenAI-compatible API 的 429 风险：

```powershell
python scripts\run_rag_eval.py --kb-id 6 `
  --ask-delay-seconds 8 `
  --max-ask-retries 3 `
  --retry-backoff-seconds 10 `
  --report docs\eval\reports\baseline-004-generation-clean.md `
  --details-json docs\eval\reports\baseline-004-generation-details.json `
  --no-overwrite
```

新增可靠性参数：

- `--skip-ask`：只跑检索评测，generation/citation/no-answer 指标在报告中显示为 `skipped`，不会误算成 0。
- `--ask-delay-seconds`：每次调用 `/api/qa/ask` 前后等待指定秒数，默认 `0`。
- `--max-ask-retries`：`/api/qa/ask` 对 429、timeout、5xx 的最大重试次数，默认 `0`。
- `--retry-backoff-seconds`：重试基础等待秒数，当前使用简单线性退避。
- `--fail-on-ask-errors`：报告和 details JSON 仍然写出，但只要 `askErrors > 0`，进程以非 0 code 退出。
- `--no-overwrite`：目标 report、after-report 或 details JSON 已存在时拒绝覆盖，避免污染已经确认干净的 baseline。

报告顶部会标记：

- `Report status: CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED`
- `askErrors count`
- `retrieveErrors count`
- `skippedAsk count`
- `rateLimitErrors count`
- `retry count`
- `Metrics safe for comparison`

判断报告是否被 429 污染时，优先看报告顶部：

- `CLEAN`：没有 retrieve error，也没有 ask error，retrieval 和 generation/citation 指标都适合对比。
- `RETRIEVAL_ONLY`：只适合比较 Recall@3、Recall@5、MRR、Top1 source accuracy。
- `PARTIAL`：通常说明 ask 有失败；retrieval 完整时仍可比较 retrieval 指标，但 generation/citation 指标只基于 ask 成功样本。
- `FAILED`：retrieve 大面积失败或登录失败，本次结果不适合作为 baseline。
- `rateLimitErrors count > 0` 或 `askErrors count > 0`：不要把本次 generation/citation 指标当作干净 baseline。

也可以使用环境变量：

```bash
set RAG_EVAL_KB_ID=<kb-id>
set RAG_EVAL_USERNAME=admin
set RAG_EVAL_PASSWORD=admin123
python scripts/run_rag_eval.py
```

脚本会：

1. 读取 `docs/eval/rag_eval_set.jsonl`。
2. 调用 `/auth/login` 获取 token。
3. 对每个问题调用 `/api/qa/debug/retrieve`。
4. 对每个问题调用 `/api/qa/ask`。
5. 计算指标。
6. 生成 Markdown 报告到 `docs/eval/reports/baseline-002-local.md`。
7. 如果传了 `--after-report`，同时写出 `docs/eval/reports/after-citation-validator.md`，方便本轮 citation validator 前后对照留档。

如果后端未启动、登录失败、`kbId` 缺失、知识库不存在、检索接口失败、文档疑似未索引完成，脚本会在 stderr 和报告里写出明确提示。

## 6. 指标解释

| 指标 | 含义 | 来自接口 | 适合回答的问题 |
|---|---|---|---|
| Recall@3 | 前 3 个检索结果命中 expected contexts 的比例 | `debug/retrieve` | 检索有没有把正确片段排到前面 |
| Recall@5 | 前 5 个检索结果命中 expected contexts 的比例 | `debug/retrieve` | 当前 topK=5 时召回覆盖情况 |
| MRR | 第一个正确结果排名的倒数均值 | `debug/retrieve` | 正确证据是否排得足够靠前 |
| Top1 source accuracy | 第 1 个结果的 source 是否命中 expected sources | `debug/retrieve` | top1 来源是否可靠 |
| answer keyword hit rate | 答案中命中 expected keywords 的比例 | `ask` | 生成答案是否覆盖关键点 |
| citation source hit rate | citations 中命中 expected sources 的比例 | `ask` | 引用来源是否对得上 |
| citation snippet hit rate | citation snippet 是否能在本轮 returned contexts 中精确命中或 token overlap 命中 | `ask` | 引用片段是否真实来自检索证据 |
| unsupported citation count | 无法回连到 returned contexts 的 citation 数量 | `ask` | 是否还有伪造或漂移引用 |
| no-answer citation violation count | 无答案题仍返回 citation 的次数 | `ask` | 拒答时是否还挂了误导性引用 |
| no-answer accuracy | 无答案题是否明确拒答或说明知识库没有足够信息 | `ask` | 防幻觉能力 |

注意：当前 answer keyword hit rate 是字符串关键词匹配，不等价于完整答案正确率。citation snippet hit rate 只验证片段可回连，不验证答案每一句都被该片段支持。

## 7. 如何记录优化前后对比

当前本轮建议固定两个报告：

```bash
python scripts/run_rag_eval.py --kb-id <kb-id> --report docs/eval/reports/baseline-002-local.md
```

citation validator 接入后：

```bash
python scripts/run_rag_eval.py --kb-id <kb-id> --report docs/eval/reports/after-citation-validator.md
```

建议记录：

| 实验 | 改动 | Recall@5 | MRR | keyword hit | citation hit | no-answer | 结论 |
|---|---|---:|---:|---:|---:|---:|---|
| baseline-002-local | dense + heuristic rerank + 本地三文档 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| after-citation-validator | 接入 citation validator | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

每次只改一个变量，例如：

- 只改 `topK`。
- 只改 `minScore`。
- 只开关 rerank。
- 只替换 chunk 策略。
- 只增加 hybrid search。

这样才能判断指标变化来自哪里。

## 8. 下一步接入方向

### Hybrid Search

目标：解决 dense vector 对关键词、缩写、编号、精确术语召回不稳的问题。

建议接入点：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/VectorStore.java`

最小做法：

1. 保留当前 dense vector 召回。
2. 增加关键词召回结果。
3. 合并 dense score 和 lexical score。
4. 用 eval runner 对比 Recall@3、Recall@5、MRR。

### Reranker

目标：替换当前关键词启发式 rerank。

建议接入点：

- `QueryEngineImpl.rerank`
- 新增 `Reranker` 接口。

最小做法：

1. 把当前 heuristic rerank 抽成 `HeuristicReranker`。
2. 增加模型 reranker 或 mock reranker。
3. 对比 MRR 和 Top1 source accuracy。

### Citation Validator

当前已接入基础规则：

- citation 必须来自本轮 retrieved contexts。
- citation 保留 `source`、`documentId`、`chunkId`、`score`、`snippet`。
- snippet 必须在 context content 中精确出现，或达到足够 token overlap。
- 无法验证的 citation 会被丢弃。
- no-answer 响应不返回 citation。
- `metadata.citationValidation` 输出 `validCitations`、`droppedCitations`、`citationCoverage`。

后续目标：让引用从“片段可回连”升级为“答案句子可支持”。

建议接入点：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`

下一步最小做法：

1. 把答案拆成 claim。
2. 将 claim 与 citation snippet 做 entailment/关键词支持判断。
3. 对 unsupported claim 做标记或触发重答。
4. 用 eval runner 新增 claim support rate。

## 9. 当前 baseline 的边界

- 评测集只有 30 条，适合做开发 baseline，不适合当作最终论文级评测。
- `contentPreview` 只有片段预览，可能低估长上下文命中。
- 关键词答案评分无法替代人工评估。
- 无答案题依赖回答中的拒答信号和 `metadata.status=no_result`。
- citations 当前能证明“引用片段来自检索上下文”，但还不能证明“答案每句话都被引用片段蕴含”。
