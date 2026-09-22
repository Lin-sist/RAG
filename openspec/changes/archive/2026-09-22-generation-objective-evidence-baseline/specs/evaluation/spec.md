# Evaluation Spec Delta: C18 Generation Objective Evidence Baseline

## ADDED Requirements

### Requirement: Versioned Generation Objective Execution Plan

C18 SHALL define a separate manifest binding the immutable v2 release, ordered selection, one measured repeat, generation/citation mode, judge off, ordinary Router-off QA, heuristic rerank, answer cache disabled, metric/prompt/runtime identity, the approved C18-only bounded transient retry policy and exact provider-call bounds. It MUST NOT reuse the C17 retrieval-only manifest or alter its accepted profile/reference. Planning and offline validation SHALL make zero backend/provider calls and no business-data egress.

#### Scenario: Complete plan is validated offline
- GIVEN the fixed 150-sample release and audited source identities
- WHEN full plan-only executes
- THEN it declares one repeat, 150 logical debug retrieval and ask requests, at most 600 attempts each, at most 600 generation reservations, query embedding upper bound 5968 and zero judge/model-rerank calls
- AND validation occurs before login or any backend/provider request

#### Scenario: Budget or source identity is not frozen
- GIVEN nested ask retrieval or explanatory fallback variants lack a verified integer bound, or source/config identity drifts
- WHEN live execution is requested
- THEN the plan is BLOCKED before backend/provider calls
- AND neither 300 nor 902 is substituted as a complete authorization bound

#### Scenario: Execution modes conflict
- GIVEN C18 is combined with a C17/C7 manifest, partial full selection, multiple repeats, judge enabled, nonzero backend/legacy retry, or a transient retry policy different from the approved 429/503 plus exact network/PrematureCloseException three retries
- WHEN validation runs
- THEN it fails closed without requests or changes to the accepted retrieval profile

### Requirement: Bounded Authorized Generation Baseline Execution

Canary SHALL use the five fixed category IDs and one repeat. Full SHALL require separate authorization after clean canary and use exactly the 150 ordered v2 IDs. Execution MUST disclose tracked questions/variants, fixture contexts and prompt egress, sanitized provider/model/endpoint, timeout/retry, cost basis, rate limits and REST query-count/history persistence. Actual HTTP attempts, cache hits, generation bypass, algorithm fallback and provider fallback SHALL be distinguished. Budgets SHALL be enforced before requests; unrecoverable failure SHALL stop without subset stitching, implicit KB rebuild or cleanup; only HTTP/provider 429/503 or the exact provider error pair `network/PrematureCloseException` SHALL permit up to three C18 REST retries with 5/10/20 second backoff and a complete attempt ledger.

#### Scenario: Canary approval does not authorize full
- GIVEN only five-case canary is authorized with exact budgets and data scope
- WHEN canary finishes
- THEN full remains unauthorized until the user grants its separate budget/data scope
- AND canary observations are excluded from formal baseline

#### Scenario: Empty contexts avoid generation
- GIVEN ordinary QA deterministically finds no usable context after its existing algorithm
- WHEN it returns no-answer without generation
- THEN evidence records the reason and actual generation count as bypassed
- AND the runner does not force a model request or represent unexecuted generation as successful

#### Scenario: Budget or execution fails
- GIVEN a pending request exceeds the approved bound, or an unrecoverable error, exhausted 429/503/PrematureCloseException chain, timeout or identity drift occurs
- WHEN the condition is handled
- THEN further live execution stops and every failed attempt is retained; backend provider and legacy ask retry stay zero
- AND a later attempt requires new explicit scope and no-overwrite identity

### Requirement: Complete Generation Objective Baseline Evidence

C18 SHALL compile one exact ordered 150-observation run with matching details/metadata hashes, clean source Git provenance, dataset/fixture/KB model-generation identity, runtime/metric contracts and compliant call/error counters. COMPLETE SHALL require Report status CLEAN, objectiveMetricStatus COMPLETE, judgeMetricStatus SKIPPED and zero unrecovered execution errors and a valid bounded transient attempt ledger with all recovered failures/retries reported. Partial channels or missing IDs SHALL NOT be repaired using successful subsets. Existing C9a/C9b formulas and denominators SHALL be reused. Generation/citation/claim evidence MUST come from actual ask responses and validated returned citations, not independent debug contexts.

#### Scenario: One full run has complete channel evidence
- GIVEN all 150 IDs and required identities/counters are complete with judge deliberately off
- WHEN the local compiler validates the run
- THEN it returns COMPLETE and reports retrieval, generation, citation, lexical claim support, no-answer and errors separately with denominators
- AND judge remains SKIPPED, neither zero quality nor failure of complete objective evidence

#### Scenario: Samples or channels are missing
- GIVEN any required ID/observation/identity is missing, objective is partial, or unrecovered errors exceed zero or retry evidence is invalid
- WHEN compilation runs
- THEN status is INCOMPLETE, NOT_COMPARABLE or INVALID with a stable safe reason
- AND no clean baseline is inferred from a successful subset

#### Scenario: Full measurement has low quality
- GIVEN a complete compatible run has low answer/citation/claim/no-answer metrics but no execution incompleteness
- WHEN reviewed
- THEN low values remain visible baseline facts
- AND thresholds, prompts, retrieval, labels and samples are not changed to make the run pass

### Requirement: Objective Baseline Privacy And Acceptance Boundary

Raw artifacts SHALL remain local ignored no-overwrite files; tracked output SHALL contain only allowlisted safe identity, hashes, counts, aggregates, statuses and reasons. Raw questions, answers, contexts, claims, provider payloads, credentials, numeric KB IDs, collection names and absolute local paths MUST NOT enter ordinary tracked output. C18 acceptance SHALL establish single-run development measurement only; it SHALL NOT activate objective/judge release profiles, modify C17 accepted numbers, imply semantic entailment from lexical alignment, or claim production quality/SLA/Agentic RAG readiness. Acceptance/archive require user approval.

#### Scenario: Offline summary is generated
- GIVEN local raw artifacts are available
- WHEN compiler creates a tracked summary
- THEN backend/provider calls and egress are zero
- AND output contains safe aggregates and artifact hashes without raw content or secrets

#### Scenario: A future release profile is proposed
- GIVEN C18 has one accepted full run
- WHEN objective gates are discussed
- THEN new profile/version and separately approved repeat evidence are required in the later phase
- AND one run or retrieval PASS cannot authorize objective/judge thresholds

#### Scenario: Baseline is accepted for archive
- GIVEN required offline/live evidence is complete and the user accepts its explicit limits
- WHEN C18 closes
- THEN approved delta is accepted, current documentation/risks synchronized, and the change may be archived
- AND skipped judge, production and semantic-faithfulness claims remain unproven

## 2026-09-11 W0预算与节奏具体化

当前源码审计冻结canary query embedding upper bound=34、full=1492，包含debug初始、ask初始与全部可达解释回退variants；generation上限仍为5/150。每个请求前至少2.2秒以遵循共享USER键的30次/60秒限制。源码/数据/配置hash漂移必须BLOCKED并重新离线审计，不自动放大预算。本节只具体化已批准规划，不授权真实调用；W1 runtime护栏与完整evidence compiler已通过离线负例验证，仍不等于runtime fingerprint或真实canary/full授权。

## 2026-09-19 Approved superseding transient retry contract
The user approved C18-only REST retries for HTTP/provider 429 and 503, at most three retries per logical debug/ask request, with 5/10/20 second backoff. This supersedes the zero runner retry requirements above; backend provider retry MUST remain zero. Timeouts and all other status codes MUST NOT retry. Every attempt MUST be budgeted before dispatch and preserved in a sanitized ordered ledger. Canary budgets become 20/20/20/136 and full budgets 600/600/600/5968 for debug/ask/generation/query embedding. Final failed observations still stop execution. COMPLETE requires exact final observation coverage, no unrecovered errors, and valid attempt chains; recovered failures and all retries MUST remain visible and MUST NOT be described as zero-error transport. No subset stitching is allowed.

## 2026-09-20 Approved exact premature-close extension

The user instructed completion after the r5 full run stopped on a provider-reported `llmErrorCategory=network` and `llmErrorType=PrematureCloseException`. C18 MAY retry only this exact pair in addition to 429/503, under the same three-retry and 5/10/20-second policy. Budgets remain 20/20/20/136 for canary and 600/600/600/5968 for full. Timeout, other network errors and all other statuses remain unrecoverable. The ledger MUST preserve provider error category/type and compiler v3 MUST reject any recovered chain outside this allowlist.

## 2026-09-20 Quality-hardening evidence contract

C18 compiler v4 SHALL require the observed generation model to equal the frozen runtime model, SHALL reconstruct aggregate metrics from per-sample evidence, and SHALL reject malformed nested raw evidence without crashing. Query embedding logical calls, cache hits, provider calls and provider fallback calls SHALL be directly observed in both debug and ask diagnostics and SHALL satisfy their arithmetic and frozen upper bound. Missing observations make evidence INCOMPLETE. Because failed retry responses do not expose complete internal embedding facts, any run with an HTTP retry remains ledger-valid but SHALL be INCOMPLETE for v4 acceptance. Output paths SHALL be distinct, repository-scoped and atomically no-overwrite.

## 2026-09-21 Attempt-level embedding evidence contract

C18 generation failure responses SHALL preserve completed retrieval diagnostics, and the runner SHALL record query embedding logical calls, cache hits, provider calls and provider fallback calls for every debug/ask attempt. Compiler v5 and later MAY accept a run with recovered 429/503/PrematureCloseException attempts only when every attempt has complete non-negative embedding facts, per-sample aggregates match the ordered ledger, global totals remain within the frozen budget and all other completeness requirements pass. Missing, inconsistent or transport-unobservable attempt facts SHALL remain INCOMPLETE.

## 2026-09-22 Failure-safe compiler contract

A provider failure MAY yield a sample with errors and attempt facts but no `askRawResponse`. Compiler v6 and later SHALL classify this evidence as INCOMPLETE with stable safe reasons and SHALL NOT crash, invent an observed model or promote a partial run. Structurally malformed or identity-tampered raw evidence remains INVALID or NOT_COMPARABLE under the existing rules. This compiler-only revision MUST NOT change provider/model, retry policy, budgets, samples, prompts or runtime behavior.
