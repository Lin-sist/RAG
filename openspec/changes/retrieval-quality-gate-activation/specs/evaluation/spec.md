# Evaluation Spec Delta: C17 Retrieval Quality Gate Activation

## ADDED Requirements

### Requirement: Versioned Retrieval Reference Plan And External-call Boundary

C17 SHALL define a tracked `c17-retrieval-reference-v1` execution contract that binds the target quality profile、`rag-eval-dev-v2` manifest identity、full 150-sample selection、three measured repeat indexes、retrieval-only run identity、expected heuristic rerank attribution、zero-error/retry policy、raw artifact policy and external-call upper bounds. Contract validation MUST complete before login、backend mutation or any retrieval/provider call.

Planning、offline implementation、compiler and plan-only validation SHALL make zero backend、embedding、rerank、ask、generation、judge or other provider calls and zero business-data egress. A fixed five-sample/one-repeat canary MAY make at most 5 debug retrieval and 5 query embedding calls; the formal reference MAY make exactly 150×3=450 debug retrieval and at most 450 query embedding calls. External rerank、ask、generation and judge calls SHALL remain 0, and automatic retry SHALL remain disabled. Canary and full execution MUST receive separate authorization after disclosing the sanitized runtime provider/model identity、tracked question egress、cost or zero-cost basis、rate limits/quotas、timeout and raw artifact handling.

#### Scenario: Offline plan validates the full budget

- GIVEN the tracked C17 manifest、v2 dataset and full selection are unchanged
- WHEN plan-only validates the three-repeat execution
- THEN it reports 450 debug retrieval and at most 450 query embedding calls
- AND backend mutation、external rerank、ask、generation、judge and actual provider calls remain 0

#### Scenario: Canary and full authorization remain separate

- GIVEN the user authorized only the fixed five-sample canary
- WHEN the canary completes or fails
- THEN no full 150×3 execution starts without a new authorization
- AND canary observations do not enter the formal reference aggregate

#### Scenario: Existing evaluation KB is not ready

- GIVEN mutation-free preflight cannot find the matching ready KB、fixture documents or complete indexing state
- WHEN C17 prepares canary or full evidence
- THEN execution stops without creating/deleting a KB、uploading fixtures or triggering indexing embedding
- AND any rebuild/mutation/call budget requires separate scope and authorization

### Requirement: Complete Fixed-identity Retrieval Reference Evidence

C17 formal reference evidence SHALL contain all three expected repeats and exactly 150 ordered sample observations per repeat. Each repeat SHALL be `RETRIEVAL_ONLY` with a VALID versioned dataset、matching sample selection and matching dataset/fixture/document/KB、tracked config、Git HEAD、retrieval/metric contract and repeat identity. Every rerank-eligible observation MUST report requested/effective provider=`heuristic`、fallback count=0 and model rerank call count=0.

Missing or unexpected run/sample observations、identity drift、retrieve/rate-limit errors、retry、fallback、model rerank call、non-finite required metric or incompatible Report/channel status MUST prevent a complete reference. The compiler MUST preserve expected and actual counts and safe reasons; it MUST NOT remove failed observations、shrink denominators、fill missing values with zero or calculate reference metrics from a successful subset.

#### Scenario: Three full repeats are complete and comparable

- GIVEN run indexes 1、2 and 3 each contain the same ordered 150-sample selection
- AND all strict identities、statuses、provider attribution、error and retry facts satisfy the manifest
- WHEN the local compiler validates the artifacts
- THEN reference status is `COMPLETE`
- AND all 450 expected observations enter each applicable rule denominator

#### Scenario: A repeat or sample is missing

- GIVEN the manifest requires three ordered 150-sample repeats
- WHEN any expected run index or `runIndex + sampleId` observation is missing、duplicated or unexpected
- THEN reference status is `INCOMPLETE` or `INVALID`
- AND no median reference or threshold decision is produced from the remaining successful observations

#### Scenario: Identity or provider attribution drifts

- GIVEN three artifacts are submitted as one formal reference release
- WHEN Git/config/fixture/KB/run/metric identity differs across reference repeats
- OR any eligible observation is not effective heuristic、has fallback or has a model rerank call
- THEN reference status is `NOT_COMPARABLE`
- AND no retrieval quality baseline is inferred from a clean subset

### Requirement: Human-reviewed Threshold Approval And Locked Reference

For each profile rule, a COMPLETE C17 evidence pack SHALL report the three per-repeat observed values and denominators plus deterministic minimum、median、maximum and spread. The pack MUST retain all repeat values and safe identity/completeness facts while excluding raw question、expected answer/context、retrieved context、provider body、credentials、numeric KB identity、vector collection and absolute local paths.

C17 tooling MUST NOT auto-learn、auto-write or auto-approve hard floors or regression tolerances. The target profile SHALL remain `DRAFT / PENDING_REFERENCE_EVIDENCE` until the user reviews complete evidence and approves every required rule's exact target and tolerance. Activation SHALL explicitly change the profile version from `v1-draft` to `v1`, set `ACTIVE / APPROVED`, and bind the locked reference to the final profile hash、dataset and run/metric identity. Each locked rule's reference observed value SHALL be the deterministic median of its three approved repeat values; minimum、maximum and spread SHALL remain visible in the evidence pack.

#### Scenario: Complete evidence awaits threshold review

- GIVEN all three reference repeats are COMPLETE
- WHEN the compiler creates the sanitized review pack
- THEN every rule contains three observed values、denominators、minimum、median、maximum and spread
- AND the profile remains DRAFT and cannot produce PASS before explicit threshold approval

#### Scenario: User approves exact thresholds and tolerances

- GIVEN the user reviewed complete evidence and approved every required numeric target and tolerance
- WHEN the canonical profile is activated and the reference is locked
- THEN profile version/status/threshold status become `v1 / ACTIVE / APPROVED`
- AND the locked reference binds the final profile SHA-256 and uses each rule's three-repeat median

#### Scenario: Threshold approval or final binding is incomplete

- GIVEN a COMPLETE evidence pack is proposed for profile activation
- WHEN any required target/tolerance lacks approval OR active profile/reference identity differs
- THEN activation is refused and the profile remains or returns to DRAFT review state
- AND no profile/reference artifact is represented as an active quality gate

### Requirement: Active Retrieval Gate Verification And Claim Boundary

Before C17 acceptance, each of the three source reference details SHALL be replayed locally against the final ACTIVE profile and locked reference. Every required rule MUST be evaluable and satisfy both its approved hard floor and reference tolerance; any `NOT_EVALUABLE`、`INVALID` or required-rule failure SHALL block acceptance rather than trigger automatic threshold weakening. Future candidate evidence MAY use a different Git HEAD, but it MUST match the locked profile、dataset、selection、run/metric and slice identity and preserve candidate Git provenance.

C17 activation SHALL NOT modify dataset/fixture、retrieval、chunking、embedding、rerank、metric formulas、production QA、default provider、generation、citation、no-answer answer policy、judge or CI platform configuration. Its conclusion SHALL be limited to the approved retrieval-only development gate under the locked identity; it MUST NOT be described as generation/citation/judge quality、production SLA、production multi-tenancy or Agentic RAG readiness. After acceptance, any profile threshold、tolerance、rule or reference change MUST create a new version rather than silently editing the accepted profile/reference.

#### Scenario: Final active reference replays cleanly

- GIVEN the ACTIVE profile、locked median reference and all three source reference details share compatible identity
- WHEN each source repeat is evaluated offline
- THEN every required rule is evaluable and passes its approved hard and reference checks
- AND replay makes zero backend/provider calls and no raw sample content enters ordinary output

#### Scenario: Candidate comes from a later Git revision

- GIVEN candidate Git HEAD differs from the locked reference HEAD
- AND profile、dataset、selection、run/metric、slice and evidence completeness identities still match
- WHEN the active gate evaluates the candidate
- THEN the differing Git HEAD is preserved as provenance without alone making evidence incompatible
- AND the gate still fails closed on any policy、dataset、run or metric identity drift

#### Scenario: Retrieval gate is active but other quality channels are unproven

- GIVEN the C17 retrieval profile and reference are ACTIVE and accepted
- WHEN project capability is reported
- THEN claims are limited to the fixed retrieval-only development gate
- AND generation、citation、no-answer answer quality、judge、production defaults、SLA and Agentic RAG remain unproven or out of scope
