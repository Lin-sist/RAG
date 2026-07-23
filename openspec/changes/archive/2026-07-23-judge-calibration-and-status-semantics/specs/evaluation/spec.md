# Evaluation Spec Delta: C9b Judge Calibration And Status Semantics

## ADDED Requirements

### Requirement: Versioned Judge Contract And Calibration Corpus

C9b SHALL define a tracked judge contract identity covering rubric/prompt version and hash、strict parser version、faithfulness/relevance score thresholds、joint pass rule、context truncation policy、provider/model、temperature and protocol-relevant endpoint identity. API keys、Authorization、raw provider responses and secret-bearing configuration MUST NOT enter this identity or ordinary output.

Judge calibration SHALL use a versioned corpus independent from the normal evaluation dataset release. The initial corpus SHALL contain 24 human-reviewed static cases with exact quotas of 6 cases in each `faithful × relevant` boolean quadrant. Each case SHALL bind a synthetic question/answer to contexts resolved deterministically from tracked fixtures and SHALL record separate faithfulness/relevance gold labels、a derived joint label、review status and stable identity.

Calibration manifest/schema/case path、hash、bytes、count、order、quadrant quota、fixture grounding、gold consistency and review completeness MUST validate locally before any judge/provider call. Calibration cases MUST NOT modify or enter the denominators of `rag-eval-dev-v1/v2`.

#### Scenario: Calibration corpus is valid

- GIVEN the tracked calibration manifest、24 ordered cases、schema、fixture identities、quadrant quotas and review records all match
- WHEN the local calibration validator runs
- THEN validation status is `VALID`
- AND the plan binds the exact calibration and judge contract identities before estimating or making calls

#### Scenario: Calibration artifact drifts

- WHEN case content/order、manifest hash/count、quadrant quota、fixture grounding、gold relation or review status differs from the tracked contract
- THEN validation fails with a stable safe error code before any provider call
- AND the tool does not silently repair、drop or replace the case

#### Scenario: Normal dataset remains independent

- GIVEN calibration v1 is added or later versioned
- WHEN normal v1/v2 dataset identity is validated
- THEN question、annotation、review、fixture and default manifest identities remain unchanged
- AND calibration cases do not enter retrieval/generation/objective metric denominators

### Requirement: Strict Judge Parsing And Calibration Evidence

C9b SHALL accept a judge observation only when both `faithfulnessScore` and `relevanceScore` are present as finite JSON numbers within `[0,1]`. Missing、string、non-finite or out-of-range scores and invalid JSON/schema MUST fail closed as `invalid_judge_payload`; values MUST NOT be clamped、guessed from prose or included in quality denominators.

Normative `judgePass` SHALL be derived deterministically from both validated scores and the tracked thresholds. A provider-reported pass MAY be retained as a diagnostic boolean, but MUST NOT override the derived result; disagreement SHALL be counted explicitly.

The calibration runner SHALL support a fixed 4-case/1-repeat canary and a fixed 24-case/3-repeat full run. It SHALL preserve every expected case/repeat observation and report parse coverage、faithfulness/relevance/joint confusion and agreement、provider-pass disagreement and per-case repeat consistency. Missing or failed observations MUST make calibration evidence partial/not comparable and MUST NOT be removed to calculate a successful subset. C9b MUST NOT automatically optimize thresholds or define a production quality gate.

#### Scenario: Judge payload is valid

- GIVEN a response contains both numeric scores in range under the tracked parser contract
- WHEN the observation is evaluated
- THEN normative pass is derived from the tracked thresholds
- AND the observation enters dimension/joint agreement and repeat metrics

#### Scenario: Judge payload is invalid or incomplete

- WHEN JSON/schema is invalid OR either score is missing、non-numeric、non-finite or out of range
- THEN the observation records `invalid_judge_payload` without clamping or inference
- AND coverage/status reflects the failure while quality metrics exclude only the invalid value, not the expected observation

#### Scenario: Full calibration has a missing repeat

- GIVEN full calibration requires every approved case at each of three repeat indexes
- WHEN any call、parse result or case/repeat identity is missing
- THEN calibration status is `PARTIAL` or `NOT_COMPARABLE`
- AND no agreement conclusion is produced from a pruned successful subset

### Requirement: Objective Judge And Global Status Separation

Normal evaluation SHALL expose independent `objectiveMetricStatus` and `judgeMetricStatus` in addition to the existing global `Report status`. Objective status SHALL depend only on login、retrieval、ask、generation/citation/no-answer and applicable C9a objective completeness; judge errors or judge quality scores MUST NOT change objective values or objective completeness.

Judge status SHALL be `SKIPPED` when judge is disabled、`NOT_APPLICABLE` when enabled but no answerable ask result is eligible、`PARTIAL` when any eligible judge call/parse/coverage is incomplete, and `COMPLETE` only when every eligible sample has a valid judge observation. Judge pass rate or score magnitude MUST NOT determine completeness status.

Global status SHALL remain `CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED`. Objective failure/retrieval-only/partial SHALL retain precedence. When objective status is complete but explicitly enabled judge status is partial, global status MUST be `PARTIAL`; judge skipped/not-applicable MUST NOT prevent an otherwise complete objective run from being `CLEAN`.

#### Scenario: All judge calls fail after complete objective evaluation

- GIVEN retrieval、ask and applicable objective metrics are complete
- AND judge is explicitly enabled but every eligible judge observation fails
- WHEN statuses are aggregated
- THEN objective status is `COMPLETE`、judge status is `PARTIAL` and global status is `PARTIAL`
- AND objective metrics remain independently eligible for comparison under matching objective identity

#### Scenario: Judge is disabled

- GIVEN objective evaluation is complete and `judge-mode=off`
- WHEN statuses are aggregated
- THEN judge status is `SKIPPED` and global status may be `CLEAN`
- BUT the report does not claim calibrated faithfulness/relevance evidence

#### Scenario: Judge quality is low but coverage is complete

- GIVEN every eligible judge observation is schema-valid but pass/agreement scores are low
- WHEN statuses are aggregated
- THEN judge status is `COMPLETE`
- AND low quality remains a metric result rather than being mislabeled as incomplete execution

### Requirement: Per-channel Comparison Safety Compatibility And External-call Boundary

C9b reports/details SHALL expose structured per-channel comparison safety for objective and judge metrics. Judge partial/skipped/not-applicable MUST NOT downgrade a complete objective channel to retrieval-only. Judge metrics SHALL be comparison-eligible only when judge status is complete and compared reports match calibration、prompt/parser/threshold/provider/model/temperature/context identities. Historical reports lacking C9b fields MUST remain readable and SHALL treat C9b channel identity/status as unavailable rather than zero or inferred.

Direct、reproducible and calibration runners SHALL reuse one judge contract implementation. C9b MUST NOT change production QA、default judge mode、dataset release、C9a objective formulas、no-answer policy or C10 thresholds/exit gates. Aggregate and ordinary output MUST NOT add raw question、answer、context、reason、provider body、secret、Authorization or absolute local path.

Planning and offline implementation SHALL use zero real embedding、rerank、debug retrieval、ask、generation、judge or other provider calls and zero data egress. Live calibration MUST receive separate authorization after disclosing provider/model、a maximum of 4 canary plus 72 full judge calls、outbound tracked calibration content、cost、rate limit、timeout/retry and raw artifact handling.

#### Scenario: Judge partial but objective complete

- GIVEN global status is partial only because the judge channel is incomplete
- WHEN comparison safety is reported
- THEN objective channel remains eligible under matching objective identity
- AND judge channel is not eligible without mislabeling objective metrics as retrieval-only

#### Scenario: Historical report lacks C9b fields

- GIVEN a pre-C9b report remains stored with its original schema
- WHEN current tooling reads it
- THEN existing metrics remain readable
- AND judge contract、channel statuses and comparison safety are unavailable rather than backfilled

#### Scenario: Live calibration is not authorized

- GIVEN planning or offline implementation is underway without separate live-call authorization
- WHEN C9b tooling and tests run
- THEN only local fixtures、synthetic responses、static validation and plan-only paths execute
- AND real judge/provider calls、data egress and provider cost remain zero
