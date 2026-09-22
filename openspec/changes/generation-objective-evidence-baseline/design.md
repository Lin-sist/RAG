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

护栏优先复用可验证的本机执行入口/计数器。若必须新增runtime capability、API/DTO/持久化字段，先修订design/spec，不临场扩大范围。W1必须证明超限请求发出前即被拒绝。保留服务端限流；当前计划按共享USER键统一至少2.2秒，等待排除在单次请求latency外。

## 身份、原始数据与compiler

raw report/details/metadata写ignored `tmp/eval/c18/`，新执行ID且no-overwrite。绑定150 observations、ordered dataset、clean Git HEAD、fixture/content/chunk、runtime model/request/generation、prompt/token/citation/claim descriptors及retry/call counters。debug contexts不冒充实际ask上下文；生成/引用/claim指标以ask返回且经既有provenance验证的证据为准。

已新增 `scripts/compile_generation_objective_baseline.py`、`scripts/c18_generation_contract.py`、测试和schema。纯离线返回COMPLETE/INCOMPLETE/NOT_COMPARABLE/INVALID，复用C9a/C9b公式/状态，不重新评分、补缺或删失败。完整基线需exact150 ID/order、zero errors/retries、CLEAN/objective COMPLETE/judge SKIPPED、固定身份和调用预算合规；no-context bypass须显式记录，不要求150个LLM请求。

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

W0/W1离线切片已完成；live前必须提供最终E、runtime fingerprint、REST副作用和调用前护栏证据。当前没有外调授权，不宣称真实generation质量或canary/full已完成。

## 2026-09-11 W0执行冻结

用户已授权开始C18实现，当前按一次一个可验证切片完成W0及budget plan-only。真实RAGServiceImpl.ask在全空分支遍历全部解释fallback；每个可命中pass分别验证提前退出和一次generate。QueryEngineImpl.explainQueryVariants复用当前真实逻辑，外部依赖由测试替身隔离；无应用启动/真实provider/数据库调用。

- Canary：初始variants=11，解释回退variants=12，debug+ask最坏embedding=34，ask/generation上限=5。
- Full：初始variants=451，解释回退variants=590，debug+ask最坏embedding=1492，ask/generation上限=150。
- 证据：docs/eval/reports/c18-query-budget-audit-v1.json，包含150个ID的各pass计数和14个源码/配置/数据hash（UTF-8文本LF规范化）。它是离线调用图证据，不是真实HTTP调用或质量证据。
- 常规service单次有context只调用一次generate、无context零generate。额外本机合成HTTP503测试证明maxRetries=0时provider application retry=0、attempt=1；不能据此替代后续传输层重试关闭与真实HTTP预算护栏。
- QAController debug为60/60s、ask为30/60s，RateLimitInterceptor使用USER identity而不是endpoint路径构造key，SlidingWindowRateLimiter继续复用该key。正式计划改为每个debug或ask前至少2.2秒，合计低于30/60s；不修改服务端限流，其他并发请求仍可能消耗配额。
- c18_budget_contract.py是独立离线预算计划入口，不是正式live runner；executionReady=false。当时W1 manifest/护栏/compiler与runtime fingerprint仍未完成，随后W1补齐了前四项中的离线工具，但仍不能从该入口发出真实调用。

### 决策 9. 共享用户限流下的节奏
- **面临的选择**：沿用两端点各自1.2秒估算；按共享用户键统一至少2.2秒间隔；拆分限流键或关闭限流。
- **选了哪个 + 为什么**：按共享键统一2.2秒，遵循当前实际30次/60秒最严限制，不改变业务保护语义。
- **放弃的代价**：按独立端点估算漏掉共享配额，缓存命中快时会429；改键或关限流会改变生产行为且超出此切片。

### 10. W1护栏与compiler的边界
- **面临的选择**：修改Java服务增加分布式预算器；只在现有Python REST runner的请求边界加opt-in护栏；只做离线compiler而不拦截请求。
- **选了哪个 + 为什么**：选现有runner的opt-in护栏加独立C18 contract/compiler；它能在自有HTTP请求发出前fail closed，不改变业务API/DTO/持久化语义，W0的内部query embedding上限继续由源码审计绑定。
- **放弃的代价**：Java分布式预算器会扩大runtime/API范围；compiler-only不能证明超限请求未发出；两者都不适合作为本轮最小离线切片。

## W1实施结果

- `docs/eval/config/c18-generation-objective-v1.json`、`docs/eval/schema/c18-generation-objective-v1.json` 与 `docs/eval/schema/c18-generation-objective-evidence-v1.json` 固定v2数据/fixture、W0审计hash、canary/full selection、预算、runtime descriptor、指标与隐私allowlist。
- 两个runner在login前校验模式互斥、ID/order/repeat、输出目录、节奏和zero retry；child runner在debug/ask/judge请求前使用计数器，超限时不调用`urlopen`，父runner保留child非零退出码。
- `compile_generation_objective_baseline.py` 只消费本地raw details/metadata，验证exact selection、CLEAN/objective/judge状态、零错误重试、ask返回provenance、no-answer generation bypass和hash identity；tracked输出不复制raw文本或敏感字段。
- 本轮仍无runtime fingerprint、backend/provider/KB/SQL/Milvus调用，W2/W3外调授权与W4验收未推进。

## 2026-09-18 C18 整阶段授权与执行前护栏补齐

用户明确要求一次性完成 C18、不再逐步请示，并授权 Agent 本地提交。本次授权覆盖固定预算的 W2 canary、clean 后 W3 full、结果记录与阶段收尾；替代此前等待逐阶段授权的状态。保留 canary clean 才能 full、每轮 retry=0、失败证据不覆盖、不拼接、无 KB 重建/清理、无 push/PR/deploy 的边界。完整测量低分如实保留，不修改阈值或题目。
执行前代码审查发现 direct runner 在样本错误后仍继续后续请求；已补齐 C18 opt-in 失败即停，retrieval 错误时不发 ask，保留已执行 raw 并非零退出。Java 调用图和预算 34/1492 不变，仅同步 runner 源码 hash。

## 2026-09-18 本地兼容性修复验证

已修复Windows fixture路径规范化、judge模式从已验证的c18Execution读取、仅对完全匹配的公开runtime descriptor保留maxOutputTokens数值；任意同名字段仍脱敏。失败generation计数聚合现在保留null并单列unknown样本数，不再折算成0。真实runner的judge descriptor和sanitize函数已接入回归测试，全套Python 268 tests/OK，git diff --check PASS。仅同步相关源码hash，预算及模型未改。

上文描述的是原始失败run及原版compiler发现的问题；这些离线缺口已修复，但原始失败证据和原版摘要未改写、未重编成COMPLETE。下一步实际阻断仍为冻结生成模型HTTP410；没有追加provider请求，也没有执行full。

## 2026-09-18 模型可用性修复与新身份
用户授权继续全部剩余任务，涵盖对HTTP410的诊断、替代模型冻结及新canary/full执行；既定出站目的地与载荷不变。官方旧模型页面标Free Endpoint Deprecated；只读模型目录不含旧模型。目录中的mistral-large-2-instruct合成请求404（1次、32 token上限），不选用；官方免费端点Available的nvidia/nemotron-3-super-120b-a12b合成请求200、返回OK（1次、2048 token上限，实际12 completion tokens）。诊断与正式评测分开，均retry=0。
正式生成模型冻结为nvidia/nemotron-3-super-120b-a12b，manifestId增加nemotron3-super-r2；温度0.2、maxOutputTokens2048、timeout120s保持。embedding、题目/fixture、prompt、指标及预算34/1492、5/150不变。以进程启动参数覆盖生成模型，不改.env.local或生产默认。旧410 raw和旧manifest可通过源Git HEAD回放，不混入新run。

### 决策 11. 已退役免费模型的替代
- **面临的选择**：继续调用退役Qwen端点；迁移到收费合作方；同NVIDIA端点使用已验证可用的Nemotron 3 Super。
- **选了哪个 + 为什么**：选Nemotron 3 Super，新模型身份重冻结，维持用户已授权的目的地、免费原型账户与现有协议，合成检查200。
- **放弃的代价**：旧端点继续410不能生成基线；合作方需要新凭据/数据目的地/费用；替代模型与旧模型不可直接比较，报告必须明确模型更换。

## 2026-09-19 用户批准的有界瞬态重试修订
用户明确选择：仅对429/503，每个请求最多重试3次，完整记录失败尝试并修订C18契约。此授权替代C18此前的runner零重试约束；Java provider自动重试保持0，禁止叠加legacy ask retry，超时/其他状态不重试。
采用统一REST请求边界处理debug/ask，包括HTTP200内metadata.llmHttpStatus=429/503的失败响应。每个逻辑请求最多4次，退避5/10/20秒，所有尝试在guard前记预算；每次响应记录状态/尝试序号/类型/耗时/是否重试，不记录raw载荷或凭据。错误耗尽仍停止，不拼接旧run。
manifest身份升级为nemotron3-super-r3-transient；compiler身份v2。底层单次调用图不变，最坏预算保守乘4：canary debug/ask/generation/queryEmbedding=20/20/20/136；full=600/600/600/5968，judge/model rerank=0。实际只按观测记账，上限不当作实际值。REST重复ask可能增加query count，失败不写成功历史；同一个最终成功答案仅纳入指标一次。
完整性现在要求150个最终观测完整、未恢复错误0、所有瞬态失败有合法可核验attempt ledger；CLEAN仅表示最终通道完整，不能解读为没有发生重试或provider故障。旧零重试产物不可与新运行直接比较，历史证据保持原样。

### 决策 12. 瞬态故障恢复的层级与边界
- **面临的选择**：继续零重试整轮重跑；在Java provider隐式重试；在C18 opt-in REST边界记录最多3次429/503重试。
- **选了哪个 + 为什么**：选可审计的C18请求边界，用户明确批准；保留每次失败、预算与副作用，业务默认不变。
- **放弃的代价**：零重试整轮容易反复消耗成功请求；Java隐式重试不易关联runner证据且改变默认provider行为；C18重试需要独立新身份和更高预算，不能沿用旧零重试基线。

## 2026-09-20 提前断连重试边界修订

r5 full 在第115条收到后端HTTP 200，但生成元数据明确标记 `llmErrorCategory=network`、`llmErrorType=PrematureCloseException`；该失败发生在约19.5秒，不是120秒超时，且旧契约不能重试。用户要求继续完成C18剩余内容，因此以新身份 `nemotron3-super-r4-network-close` 将这一对精确字段加入C18请求边界重试。429/503、最多3次、5/10/20秒退避、预算20/600与136/5968均不变；timeout、其他network类型、其他状态仍不重试。attempt ledger新增provider error type/category，compiler v3验证每条恢复链并单列 `network:PrematureCloseException`，旧r5 raw和摘要保持不变。

### 决策 13. 提前断连的重试粒度
- **面临的选择**：把所有network错误都视为可重试；只允许精确的network/PrematureCloseException组合；仅重跑整轮但仍不在请求边界恢复。
- **选了哪个 + 为什么**：只允许已真实观测且可审计的精确组合，复用现有最多3次预算和ledger，不把timeout、连接配置错误或未知网络失败静默放宽。
- **放弃的代价**：全量network重试可能掩盖持续配置故障并扩大未披露行为；只重跑整轮会重复消耗大量已成功请求，仍可能在同类短暂断连处停止。

## 2026-09-20 代码质量复审加固

compiler v4不再把离线query embedding上限标成实际调用：`EmbeddingService`返回本次缓存/provider观测，`QueryEngineImpl`聚合每个variant，解释性回退累加各pass，debug与ask原始证据均保存四项计数。生成模型身份在单次ask返回后立即校验；逐样本指标重新聚合并与raw aggregate比对。输出使用互异路径、repo-root约束和原子no-overwrite。

### 决策 14. 重试成功但embedding失败路径不可观测时的证据等级
- **面临的选择**：把最终成功响应的embedding计数当作整条重试链实际值；继续只报告离线上限；保留重试ledger但将该run判为`INCOMPLETE`。
- **选了哪个 + 为什么**：选择保留重试证据并降为`INCOMPLETE`；失败响应当前不能完整返回其内部缓存/provider调用，严格摘要不能虚构全程实际值。
- **放弃的代价**：只看最终响应会系统性漏计失败attempt；只报上限仍回答不了实际调用问题；降级会使发生过可恢复503的run不能成为v4 COMPLETE基线，需要一轮无重试的clean run。

### 决策 15. 瞬态失败attempt的embedding观测闭环
- **面临的选择**：继续要求完整run零重试；只按最终成功响应估算失败attempt；让生成失败响应携带已经完成的retrieval/embedding diagnostics并由runner逐attempt记账。
- **选了哪个 + 为什么**：选择第三项；r8 canary在5条最终成功时仍恢复6次503，证明零重试不是稳定可执行前提。服务端已有本次检索diagnostics，只把四项非敏感计数合并进错误metadata，runner和compiler逐attempt校验后才能恢复`COMPLETE`，不改变重试范围、预算、题目、prompt或业务答案语义。
- **放弃的代价**：坚持零重试会使高负载provider下150条完整run几乎不可达；估算会漏计失败attempt；逐attempt观测增加Java错误响应和证据契约版本，但能保留真实调用事实。

### 决策 16. 失败ask缺少raw响应时的证据分类
- **面临的选择**：让compiler异常退出；把执行失败导致的raw缺失一律标成结构非法`INVALID`；把它稳定分类为`INCOMPLETE`并保留既有错误与调用账本。
- **选了哪个 + 为什么**：选择`INCOMPLETE`；r10证明provider重试耗尽可合法地产生无`askRawResponse`的部分证据，这时compiler应给出可审计降级结论，而不是崩溃或伪造观测模型。
- **放弃的代价**：异常退出无法形成安全摘要；一律`INVALID`会混淆真实执行失败与文件结构篡改；`INCOMPLETE`要求新compiler/manifest身份和回归测试，旧run也不能借此升级为正式baseline。
