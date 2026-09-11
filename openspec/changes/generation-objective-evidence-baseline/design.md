# Design: C18 Generation Objective Evidence Baseline

## 当前代码与契约

- `scripts/run_reproducible_rag_eval.py`：selection/repeat、keep-existing、plan/preflight、retry开关；estimate_live_calls缺少C18嵌套成本，不复用C17 reference-only manifest。
- `scripts/run_rag_eval.py`：debug retrieval后REST ask、enableCache=false；C9a objective claim和C9b channel/global status分离。保留既有指标公式与分母，compiler只验证身份和完整性。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`：同义词/解释/去口语化variants，每variant可触发query embedding。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`：无context可进入解释fallback及其variants；有context才generate。Router保持default-off。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`、同目录LLMProperties.java：prompt、provider重试及输出token预算；W0需证明单题generation HTTP上限。
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`：ask增加query count并按既有成功条件保存历史，C18不改该语义。
- tracked application.yml默认chat=qwen/qwen3.5-122b-a10b、temperature=0.2、max tokens=2048、timeout=120s/max-retries=0；embedding=nvidia/nemotron-3-embed-1b、2048维、timeout=60000ms/retry=0。环境可覆盖，live值待核验，不读取/输出secret来“证明”。

## 执行契约与预算

拟新增 `docs/eval/config/c18-generation-objective-v1.json` 及schema，绑定v2 manifest/hash、canary IDs和full ordered150 selection、repeat=1/index=1、topK=5/minScore=0.3、heuristic、judge=off、Router=off、answer cache=false、zero retries、节奏、expected endpoint/model/request identity、prompt/metric/config/source hash、全部调用上限与raw no-overwrite。

C18参数与C17/C7模式互斥；plan-only登录前验证，显示精确整数预算、expected本地副作用、出站类别和runtime身份。缺预算审计返回BLOCKED，不生成可执行live计划。

对集合S：`E(S)=Σ|V(q)|debug + Σ|V(q)|ask + ΣΣ|V(fallbackQuery)|`。第三项覆盖解释回退所有可达分支；不根据当前context命中或缓存减预算。用实际Java纯函数/离线捕获调用图获得ID-only计数，不在Python重写检索语义。C17初始canary11/full451需W0重验后冻结最终E。

generation最多N次的前提是runner/provider自动重试都0且无额外生成回路；不能证明就阻止live，不扩大已批准预算。记录query/provider HTTP、cache hit、generation/no-context bypass、algorithm fallback与provider fallback；缺实际计数意味着审计不完整。

护栏优先复用可验证的本机执行入口/计数器。若必须新增runtime capability、API/DTO/持久化字段，先修订design/spec，不临场扩大范围。W1必须证明超限请求发出前即被拒绝。保留服务端限流；初始计划retrieval前1.2秒、ask前后1.2秒，W0核对共享限流键与每题两端点最坏节奏。等待排除在单次请求latency外。

## 身份、原始数据与compiler

raw report/details/metadata写ignored `tmp/eval/c18/`，新执行ID且no-overwrite。绑定150 observations、ordered dataset、clean Git HEAD、fixture/content/chunk、runtime model/request/generation、prompt/token/citation/claim descriptors及retry/call counters。debug contexts不冒充实际ask上下文；生成/引用/claim指标以ask返回且经既有provenance验证的证据为准。

拟新增 `scripts/compile_generation_objective_baseline.py`、测试和schema。纯离线返回COMPLETE/INCOMPLETE/NOT_COMPARABLE/INVALID，复用C9a/C9b公式/状态，不重新评分、补缺或删失败。完整基线需exact150 ID/order、zero errors/retries、CLEAN/objective COMPLETE/judge SKIPPED、固定身份和调用预算合规；no-context bypass须显式记录，不要求150个LLM请求。

聚合分别保留retrieval、answer keyword、citation source/snippet、unsupported citation、claim support、no-answer、错误/latency/调用数及分母。完整低分不触发题目或算法调整。tracked产物只存allowlisted身份、hash、counts/aggregates、status/reason，不含问题/答案/context/claim/provider body、凭据、数字KB ID、collection或绝对路径。

## 状态与闸门

`PLANNED → OFFLINE_VERIFIED → RUNTIME_READY → CANARY_CLEAN → FULL_COMPLETE → ACCEPTED`。

W0/W1完成且canary具体范围获批后才能进入真实执行。HTTP preflight只检查既有fixture KB/model generation。canary固定fact-001/definition-001/reasoning-001/multi-hop-001/no-answer-001，一次完整执行，不能代替full。任何error/429/timeout/身份/预算漂移停止并保留，不自动新run；full需新授权。最终可验收完整低分测量，不能验收PARTIAL为完整baseline。

## 决策记录

### 1. 正式样本与重复
- **面临的选择**：30条seed；150条一次；150×3。
- **选了哪个 + 为什么**：150条一次，遵循C18蓝图，先补齐v2生成事实，不建立release阈值。
- **放弃的代价**：30条不能代表完整v2；三轮增加调用与失败面，留待未来独立profile evidence。

### 2. 是否同时运行judge
- **面临的选择**：judge同跑；关闭judge；mock judge补分。
- **选了哪个 + 为什么**：关闭并记SKIPPED，避免未校准主观通道污染objective完整性。
- **放弃的代价**：同跑引入未校准偏差；mock分数没有真实质量含义，不能代替C19校准。

### 3. embedding预算
- **面临的选择**：旧300；初始variants翻倍902；包含ask解释回退并冻结精确整数。
- **选了哪个 + 为什么**：第三项，W0按真实调用图冻结；902只是不含解释回退的初始项。
- **放弃的代价**：前两者漏算冷缓存嵌套调用，可能突破授权；全面核算需要额外离线审计。

### 4. 常规算法回退
- **面临的选择**：关闭解释回退；开启Router缩减检索；保留默认算法并单独记账。
- **选了哪个 + 为什么**：保留现有语义，区分算法回退/provider fallback/HTTP retry，避免测量对象改变。
- **放弃的代价**：禁用回退或开启Router改变普通RAG路径，本轮便不再验证既有行为。

### 5. 是否复用C17 profile/compiler
- **面临的选择**：直接套retrieval profile；扩充C17 reference；独立C18工具复用纯指标函数。
- **选了哪个 + 为什么**：独立C18，生成通道与单次run不适用C17 retrieval-only三轮门禁。
- **放弃的代价**：直接套用会假装retrieval PASS证明生成；修改冻结reference破坏版本和可比性。

### 6. ask是否只读
- **面临的选择**：按cache=false假定零副作用；service-only旁路；披露现有REST持久化。
- **选了哪个 + 为什么**：保留REST路径并在专用fixture KB授权中披露query count/history写入。
- **放弃的代价**：假定只读与代码不符；旁路避开真实入口且引入新契约；保留REST需明确本地写入范围。

### 7. 失败后的处理
- **面临的选择**：自动重试拼接；降低样本数；失败保留，新授权新身份完整执行。
- **选了哪个 + 为什么**：第三项，每次retry=0，分母和运行身份可审计。
- **放弃的代价**：拼接隐藏失败/漂移；缩样本使v2不完整；重跑有额外调用需明确批准。

### 8. 差结果是否触发优化
- **面临的选择**：逐题改prompt/标注；临时加生产门槛；如实保留完整测量。
- **选了哪个 + 为什么**：C18只建baseline，低分按通道列为后续独立问题。
- **放弃的代价**：逐题调整过拟合开发集；临时生产门槛没有代表性证据，并混淆完整测量与产品达标。

## 待实施闸门确认

规划批准后才启动W0/W1；live前必须提供最终E、runtime fingerprint、REST副作用和调用前护栏证据。当前没有外调授权，不宣称新增工具已实现。
