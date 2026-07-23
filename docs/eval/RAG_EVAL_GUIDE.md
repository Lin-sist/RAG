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

如果预检提示评测 KB 不存在，说明当前数据库中没有可复用的固定评测 KB。此时先**去掉** `--keep-existing` 执行一次 retrieval-only 初始化：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --report docs\eval\reports\stage1-rebuild-retrieval.md `
  --details-json docs\eval\reports\stage1-rebuild-retrieval-details.json `
  --metadata-json docs\eval\reports\stage1-rebuild-retrieval-metadata.json
```

该命令会删除同名且 marker 匹配的空/旧评测 KB，重新创建 KB、上传三份 fixture 并触发 Embedding。初始化成功后，再使用 `--preflight-only` 或 `--keep-existing --include-ask`。修复后的 `--keep-existing` 在 KB 缺失时会直接失败，不会再创建空 KB。

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
$env:RAG_EVAL_USERNAME="<your-username>"
$env:RAG_EVAL_PASSWORD="<your-password>"
$loginBody = @{
  username = $env:RAG_EVAL_USERNAME
  password = $env:RAG_EVAL_PASSWORD
} | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType "application/json" -Body $loginBody
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
  --username <your-username> \
  --password <your-password> \
  --top-k 5 \
  --min-score 0.3 \
  --enable-rerank \
  --report tmp/eval/local-eval.md \
  --details-json tmp/eval/local-eval-details.json
```

为了避免 LLM API 限流污染干净 baseline，建议把 live eval 拆成两步。

先跑 retrieval-only clean baseline，只调用 `/api/qa/debug/retrieve`，不调用 `/api/qa/ask`：

```powershell
python scripts\run_rag_eval.py --kb-id 6 `
  --skip-ask `
  --report tmp\eval\retrieval-only.md `
  --details-json tmp\eval\retrieval-only-details.json `
  --no-overwrite
```

再跑 slow ask baseline，用延迟和 retry 降低 NVIDIA/Qwen/OpenAI-compatible API 的 429 风险：

```powershell
python scripts\run_rag_eval.py --kb-id 6 `
  --ask-delay-seconds 8 `
  --max-ask-retries 3 `
  --retry-backoff-seconds 10 `
  --report tmp\eval\generation.md `
  --details-json tmp\eval\generation-details.json `
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
set RAG_EVAL_USERNAME=<your-username>
set RAG_EVAL_PASSWORD=<your-password>
python scripts/run_rag_eval.py
```

脚本会：

1. 读取 `docs/eval/rag_eval_set.jsonl`。
2. 调用 `/auth/login` 获取 token。
3. 对每个问题调用 `/api/qa/debug/retrieve`。
4. 对每个问题调用 `/api/qa/ask`。
5. 计算指标。
6. 默认生成 Markdown 报告到已被 Git 忽略的 `tmp/eval/local-eval.md`。
7. 只有经过确认、需要作为阶段证据长期保留的报告，才显式写入 `docs/eval/reports/`。

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

临时实验报告默认写入 `tmp/eval/`，避免把每次本地运行都提交到仓库：

```bash
python scripts/run_rag_eval.py --kb-id <kb-id> --report tmp/eval/experiment-before.md
```

改变一个变量后：

```bash
python scripts/run_rag_eval.py --kb-id <kb-id> --report tmp/eval/experiment-after.md
```

建议记录：

| 实验 | 改动 | Recall@5 | MRR | keyword hit | citation hit | no-answer | 结论 |
|---|---|---:|---:|---:|---:|---:|---|
| experiment-before | 当前稳定配置 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| experiment-after | 单变量候选配置 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

每次只改一个变量，例如：

- 只改 `topK`。
- 只改 `minScore`。
- 只开关 rerank。
- 只替换 chunk 策略。
- 只切换 hybrid 开关或调整一个融合参数。

这样才能判断指标变化来自哪里。

## 8. C7：Heuristic 与 NVIDIA Reranker 固定身份 A/B

C7 只比较 retrieval 指标与两类 latency，不调用 `/api/qa/ask`、LLM judge 或生成模型。正式结论必须来自 `comparisonStatus=COMPARABLE`；任何 identity 漂移、retrieve error、fallback、provider/model/protocol 不符、候选覆盖不完整或单侧零候选都会抑制 quality delta。

### 8.1 脱敏 arm manifest

仓库提供四份不含 key、Authorization 和完整 base URL 的 manifest：

- `docs/eval/config/c7-canary-heuristic-arm.json`
- `docs/eval/config/c7-canary-nvidia-arm.json`
- `docs/eval/config/c7-heuristic-arm.json`
- `docs/eval/config/c7-nvidia-arm.json`

canary 固定 3 个样本、每 arm 1 个 measured run + 3 次 warm-up；上限为 12 次 debug retrieval、12 次 query embedding、6 次 NVIDIA rerank。正式 full 固定 30 个样本、每 arm 3 个 measured runs + 每 arm 总计 3 次 warm-up；上限为 186 次 debug retrieval、186 次 query embedding、93 次 NVIDIA rerank。两阶段的 ask、judge 和 generation 调用量均为 0。

NVIDIA arm 固定模型 `nvidia/llama-nemotron-rerank-1b-v2`、协议 `nvidia-ranking-v1`、模型专属 endpoint path `/v1/retrieval/nvidia/llama-nemotron-rerank-1b-v2/reranking`、`truncate=END`、timeout `20000ms`、关闭 health check。费用为用户提供的外部事实：本账号 NIM 免费；这不消除速率、并发和配额风险，因此 runner 串行调用且 rerank 不做自动重试。

### 8.2 启动配置与只读预检

每次切换 arm 都要停止旧后端，再以相同 tracked Git HEAD 启动新后端。Heuristic arm 只设置：

```powershell
$env:RETRIEVAL_RERANK_PROVIDER="heuristic"
```

NVIDIA arm 使用本地凭据环境变量，禁止把真实 key 写入 manifest、命令历史、报告或 tracked 文件：

```powershell
$env:RETRIEVAL_RERANK_PROVIDER="nvidia"
$env:NVIDIA_RERANK_ENABLED="true"
$env:NVIDIA_RERANK_BASE_URL="https://ai.api.nvidia.com"
$env:NVIDIA_RERANK_ENDPOINT_PATH="/v1/retrieval/nvidia/llama-nemotron-rerank-1b-v2/reranking"
$env:NVIDIA_RERANK_MODEL="nvidia/llama-nemotron-rerank-1b-v2"
$env:NVIDIA_RERANK_TRUNCATE="END"
$env:NVIDIA_RERANK_TIMEOUT_MILLIS="20000"
$env:NVIDIA_RERANK_HEALTH_CHECK_ENABLED="false"
# NVIDIA_RERANK_API_KEY 由本地安全环境提供；不要在日志中打印它。
```

NVIDIA 当前 hosted API reference 将该 rerank 模型发布在 `https://ai.api.nvidia.com/v1/retrieval/nvidia/llama-nemotron-rerank-1b-v2/reranking`。Embedding 仍可使用 `https://integrate.api.nvidia.com/v1/embeddings`；两者的 hosted 主机不能因同属 NVIDIA API 而混用。真实执行前应复核当前官方模型 API reference，避免文档或 endpoint 漂移。

每次 backend 启动后先做 mutation-free preflight。它只复用已有固定 KB，不创建、上传、删除或重建资源：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --preflight-only `
  --keep-existing `
  --username $env:RAG_EVAL_USERNAME `
  --password $env:RAG_EVAL_PASSWORD
```

### 8.3 Canary

固定 canary IDs 为 `fact-001`、`fact-006`、`definition-001`。先在 heuristic backend 运行：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --keep-existing --repeat 1 `
  --arm-manifest docs\eval\config\c7-canary-heuristic-arm.json `
  --sample-id fact-001 --sample-id fact-006 --sample-id definition-001 `
  --report tmp\eval\c7-canary-heuristic.md `
  --details-json tmp\eval\c7-canary-heuristic-details.json `
  --metadata-json tmp\eval\c7-canary-heuristic-metadata.json `
  --username $env:RAG_EVAL_USERNAME --password $env:RAG_EVAL_PASSWORD `
  --no-overwrite
```

重启为 NVIDIA backend 后，用 `c7-canary-nvidia-arm.json` 和 `c7-canary-nvidia.*` 输出名运行相同 3 个 IDs。然后离线比较：

```powershell
python -B scripts\compare_reranker_ab.py `
  --heuristic-details tmp\eval\c7-canary-heuristic-details.json `
  --model-details tmp\eval\c7-canary-nvidia-details.json `
  --output-json tmp\eval\c7-canary-comparison.json `
  --output-markdown tmp\eval\c7-canary-comparison.md
```

只有 canary 为 `COMPARABLE`、model eligible coverage 为 100%、fallback 为 0、无 429/timeout/provider drift，才可请求 full 预算确认；否则停止并保留诊断，不自动重试或删样本。

### 8.4 Full 的交替顺序

full 使用 `--run-index` 把 measured runs 按 `H1/N1、N2/H2、H3/N3` 执行。首次运行每个 logical arm 时执行 3 次 warm-up；同一 arm 的后续 run index 加 `--skip-warmup`，从而保持已批准的每 arm 总计 `W=3` 和模型上限 93 次。若改为每次 backend 重启都 warm-up，模型上限将升为 99 次，必须重新取得用户授权。

每次 invocation 均使用同一组通用参数，并为 arm/run index 设置独立输出基名：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --keep-existing --repeat 3 --run-index 1 `
  --arm-manifest docs\eval\config\c7-heuristic-arm.json `
  --report tmp\eval\c7-heuristic.md `
  --details-json tmp\eval\c7-heuristic-details.json `
  --metadata-json tmp\eval\c7-heuristic-metadata.json `
  --username $env:RAG_EVAL_USERNAME --password $env:RAG_EVAL_PASSWORD `
  --no-overwrite
```

这会生成带 `-run1` 后缀的 measured outputs。后续调用把 `--run-index` 改为 `2` 或 `3` 并加 `--skip-warmup`；NVIDIA arm 改用 `c7-nvidia-arm.json` 和 `c7-nvidia.*` 输出基名。每次切换 backend 后都先执行 8.2 的 preflight。

三轮完成后离线比较全部六份 details：

```powershell
python -B scripts\compare_reranker_ab.py `
  --heuristic-details tmp\eval\c7-heuristic-details-run1.json `
  --heuristic-details tmp\eval\c7-heuristic-details-run2.json `
  --heuristic-details tmp\eval\c7-heuristic-details-run3.json `
  --model-details tmp\eval\c7-nvidia-details-run1.json `
  --model-details tmp\eval\c7-nvidia-details-run2.json `
  --model-details tmp\eval\c7-nvidia-details-run3.json `
  --output-json tmp\eval\c7-comparison.json `
  --output-markdown tmp\eval\c7-comparison.md
```

compact comparison 不复制 question、contexts、passages、raw provider response 或凭据。它分别报告 Recall@5、MRR、Top1 source accuracy，以及 server-side rerank stage latency 和 client-observed debug retrieval wall-clock latency 的 observation count、P50、P95；warm-up 不进入 measured metrics。

### 8.5 结论边界

- `RETRIEVAL_ONLY` 是单次报告状态，`COMPARABLE / NOT_COMPARABLE / FAILED` 是独立比较状态。
- 只有 `COMPARABLE` 才展示 model - heuristic 的质量差值；不可比较时不计算成功子集收益。
- 30 条开发样本只能证明本 eval-set、fixture、KB、配置、provider/model 和 Git HEAD 下的观察结果。
- C7 不证明 generation、citation、no-answer 或 judge 改善，不建立生产 SLA，也不自动修改默认 reranker。

## 9. C8a：版本化评测数据 Release

正式评测默认绑定 `docs/eval/dataset-manifest.json`，当前默认 release 为已验收的 `rag-eval-dev-v2`；首个 `rag-eval-dev-v1` 仍通过显式 manifest 保留。v1 固定 30 条 question set、`rag-eval-sample-v1` schema 和 3 份 `test-data/*.md` fixture；其 JSONL、schema 和 fixture 原始字节没有因 C8b 改写。

两个 runner 都会先完成本地 manifest/schema/question/fixture 校验，再考虑 login、preflight、KB mutation 或 provider 调用。可以用以下命令做零外调计划检查：

```powershell
python -B scripts\run_rag_eval.py --plan-only --kb-id 0 --sample-limit 1
python -B scripts\run_reproducible_rag_eval.py --plan-only
```

输出中的 `datasetReleaseIdentity.validationStatus=VALID`、`releaseVersion`、manifest/question/fixture hash、sample count 和 distribution 共同说明本地 release 身份匹配；`estimated*Calls` 是后续实际运行的估算，不是本次 plan 已发生的调用。

### 9.1 Formal 与 UNVERSIONED

- 默认正式路径只接受 manifest 中的 question set 和 fixture corpus；custom eval-set 或 fixture mismatch 会在任何 backend/provider 调用前非零退出。
- 若要让另一套数据形成正式 release，先在仓库内新增独立 manifest/schema/artifact，并通过 `--dataset-manifest <repo-relative-path> --eval-set <manifest-question-path>` 显式选择；新 manifest 必须使用新的唯一 `releaseVersion`，完整声明各版本、hash、bytes、count/order、fixture 与 distribution，不能复用 `rag-eval-dev-v1` 覆盖当前身份。
- 本地诊断确有需要时，必须显式增加 `--allow-unversioned-eval-set`。该结果的 metadata/report 标为 `UNVERSIONED`，`Metrics safe for comparison` 固定为 `no`，不得进入正式 baseline、跨 run 可比较结论或质量门禁。
- `--preflight-only` 仍只检查已有资源，`--keep-existing` 仍只复用已有 KB/document；manifest 不会触发隐式建库、上传、删除或覆盖报告。
- C7 及更早报告保留当时的 hash、metadata 和结论边界，不回写 C8a 才引入的 release version 或 validation status。

### 9.2 Version bump matrix

| 变化 | 必须 bump | 同时生成新 release | 说明 |
|---|---|---:|---|
| 样本 membership、order、ID 或 question text | `questionSetVersion` | 是 | 重算 question bytes/hash/count/order identity |
| allowed/required field、类型、enum、ID pattern 或条件规则 | `sampleSchemaVersion` | 是 | 新建 schema 文件并记录其 bytes/hash |
| expected source/keyword/answer point/context、`should_answer`、type/difficulty 或 notes 审核变化 | `annotationVersion` | 是 | 即使 question text 不变也必须 bump |
| fixture 集合、文件名或任意 bytes | `fixtureCorpusVersion` | 是 | 重算每份 fixture identity 与逻辑 KB expected document names |

任一组成 identity 改变都必须生成新的 `releaseVersion`。同一个 release version 不得对应另一份 manifest hash、question/schema/annotation/corpus identity；不能通过只改 hash、保留旧 version 来覆盖历史 release。

### 9.3 Drift 恢复

1. 若变化是误改，恢复 manifest 所指 artifact 的原始 bytes，再重新运行 plan。
2. 若变化是有意演进，按上表创建新 question/schema/annotation/corpus version，更新所有 path/hash/bytes/count/order/distribution，再生成新的 release version。
3. schema 不兼容时保留旧 schema 文件和旧 release，不原地覆盖；annotation-only 或 fixture-only 变化也必须产生新 release。
4. 不把绝对路径、`..`、secret、numeric KB ID、vector collection、provider 原始响应或 Git HEAD 写进静态 manifest。Git/config/observed KB identity 由运行时 metadata 追加。

C8a 只完成 schema/versioning 与 fail-fast 门禁，不代表 C8b 数据扩充、C9 claim/judge、C10 quality gate 或 C14 isolation evaluation 已完成。

### 9.4 C8b expanded v2（已验收归档）

C8b 已验收并将 `rag-eval-dev-v2` 设为默认 release：`docs/eval/releases/rag-eval-dev-v2.jsonl` 共 150 条，前 30 条与 v1 seed 的对象、顺序及原始行 bytes 一致，后续追加 120 条。sample schema 和 3 份 fixture 未改变，因此继续使用 `rag-eval-sample-v1` 与 `fixtures-v1`；question、annotation 和 release 分别升级为 `questions-v2`、`annotations-v2`、`rag-eval-dev-v2`。

批准并固化的 exact quota 为：fact 35、definition 30、reasoning 40、multi_hop 25、no_answer 20；easy 50、medium 65、hard 35；answerable/no-answer 为 130/20。type×difficulty 15 格矩阵由 v2 manifest 固定，不能只满足边际总量。三份 fixture 的 answerable coverage 分别为 Java 49、RAG 43、Spring Boot 44，均不少于 35 且不超过 130 的 45%。

v2 manifest schema 新增并验证三类治理身份：v1 seed release、expanded quota/grounding/duplicate facts，以及 150 条 review sidecar。新增的 103 条 answerable 样本共校验 242 个 exact fixture contexts；17 条新增 no-answer 均按三个固定 fixture 做全 corpus 语义复核。normalized exact duplicate 为 0，0.82 阈值下 near-duplicate candidate 为 0。所有检查都是本地确定性检查，不调用 embedding、rerank、ask、judge 或 LLM/provider。

默认 runner 现在直接选择 v2；需要审计或复现旧 release 时，可显式选择保留的 v1 manifest。以下命令展示显式 v2 plan-only：

```powershell
python -B scripts\run_rag_eval.py --plan-only --skip-ask --kb-id 1 --sample-limit 1 `
  --dataset-manifest docs/eval/releases/rag-eval-dev-v2-manifest.json `
  --eval-set docs/eval/releases/rag-eval-dev-v2.jsonl

python -B scripts\run_reproducible_rag_eval.py --plan-only --sample-limit 1 `
  --dataset-manifest docs/eval/releases/rag-eval-dev-v2-manifest.json `
  --eval-set docs/eval/releases/rag-eval-dev-v2.jsonl
```

`docs/eval/releases/rag-eval-dev-v1-manifest.json` 与 v1 question set 仍可独立验证。默认 manifest 与显式 v2 manifest byte-identical；v2 冻结后任何题目、标注、quota、review 或 corpus 修订都必须创建后续新 version，不能根据 provider/算法结果原地改写。

C8b expanded data 是 tracked 开发评测集，不是隐藏 benchmark、生产分布或论文级数据集；其通过只说明数据治理契约成立，不代表 retrieval、generation、citation、no-answer 或 judge 质量改善，也不代表 C9/C10/C14 完成。

## 10. 下一步接入方向

### Hybrid Search

当前已经完成 dense vector + BM25 + RRF 混合检索，并通过固定评测 KB 验证。后续重点不是重复接入 hybrid，而是扩充生产样本、观察召回失败类型，并在不污染既有基线的前提下做单变量实验。

### Reranker

当前已经具备 `HeuristicReranker`、`ModelReranker` 和 provider 降级机制，但真实 model reranker 尚未完成 A/B 验证。

建议接入点：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/rerank/ModelReranker.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/rerank/RerankerRegistry.java`

最小做法：

1. 适配真实 rerank provider 的请求/响应协议。
2. 固定同一 KB、fixture 和配置，分别运行 heuristic 与 model reranker。
3. 对比 Recall@5、MRR、Top1 source accuracy、延迟和失败降级行为。

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

## 10. 当前 baseline 的边界

- 当前默认、已验收的开发数据 release 是 150 条 v2；30 条 v1 仍可显式复现。两者都不是生产分布、隐藏 benchmark 或最终论文级评测。
- `contentPreview` 只有片段预览，可能低估长上下文命中。
- 关键词答案评分无法替代人工评估。
- 无答案题依赖回答中的拒答信号和 `metadata.status=no_result`。
- citations 当前能证明“引用片段来自检索上下文”，但还不能证明“答案每句话都被引用片段蕴含”。
