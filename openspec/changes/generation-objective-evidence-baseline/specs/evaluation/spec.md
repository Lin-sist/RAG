# Evaluation Spec Delta: C18 Generation Objective Evidence Baseline

## ADDED Requirements

### Requirement: Versioned Generation Objective Execution Plan

C18 SHALL define a separate manifest binding the immutable v2 release, ordered selection, one measured repeat, generation/citation mode, judge off, ordinary Router-off QA, heuristic rerank, answer cache disabled, metric/prompt/runtime identity, zero automatic retry and exact provider-call bounds. It MUST NOT reuse the C17 retrieval-only manifest or alter its accepted profile/reference. Planning and offline validation SHALL make zero backend/provider calls and no business-data egress.

#### Scenario: Complete plan is validated offline
- GIVEN the fixed 150-sample release and audited source identities
- WHEN full plan-only executes
- THEN it declares one repeat, 150 debug retrieval, 150 ask, at most 150 generation, exact audited query embedding bound and zero judge/model-rerank calls
- AND validation occurs before login or any backend/provider request

#### Scenario: Budget or source identity is not frozen
- GIVEN nested ask retrieval or explanatory fallback variants lack a verified integer bound, or source/config identity drifts
- WHEN live execution is requested
- THEN the plan is BLOCKED before backend/provider calls
- AND neither 300 nor 902 is substituted as a complete authorization bound

#### Scenario: Execution modes conflict
- GIVEN C18 is combined with a C17/C7 manifest, partial full selection, multiple repeats, judge enabled or nonzero retry
- WHEN validation runs
- THEN it fails closed without requests or changes to the accepted retrieval profile

### Requirement: Bounded Authorized Generation Baseline Execution

Canary SHALL use the five fixed category IDs and one repeat. Full SHALL require separate authorization after clean canary and use exactly the 150 ordered v2 IDs. Execution MUST disclose tracked questions/variants, fixture contexts and prompt egress, sanitized provider/model/endpoint, timeout/retry, cost basis, rate limits and REST query-count/history persistence. Actual HTTP attempts, cache hits, generation bypass, algorithm fallback and provider fallback SHALL be distinguished. Budgets SHALL be enforced before requests; failure SHALL stop without automatic retry, subset stitching, implicit KB rebuild or cleanup.

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
- GIVEN a pending request exceeds the approved bound, or an error/429/timeout/identity drift occurs
- WHEN the condition is handled
- THEN further live execution stops, failure artifacts are retained, and automatic retry stays zero
- AND a later attempt requires new explicit scope and no-overwrite identity

### Requirement: Complete Generation Objective Baseline Evidence

C18 SHALL compile one exact ordered 150-observation run with matching details/metadata hashes, clean source Git provenance, dataset/fixture/KB model-generation identity, runtime/metric contracts and compliant call/error counters. COMPLETE SHALL require Report status CLEAN, objectiveMetricStatus COMPLETE, judgeMetricStatus SKIPPED and zero required execution errors/retries. Partial channels or missing IDs SHALL NOT be repaired using successful subsets. Existing C9a/C9b formulas and denominators SHALL be reused. Generation/citation/claim evidence MUST come from actual ask responses and validated returned citations, not independent debug contexts.

#### Scenario: One full run has complete channel evidence
- GIVEN all 150 IDs and required identities/counters are complete with judge deliberately off
- WHEN the local compiler validates the run
- THEN it returns COMPLETE and reports retrieval, generation, citation, lexical claim support, no-answer and errors separately with denominators
- AND judge remains SKIPPED, neither zero quality nor failure of complete objective evidence

#### Scenario: Samples or channels are missing
- GIVEN any required ID/observation/identity is missing, objective is partial, or errors exceed zero
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
