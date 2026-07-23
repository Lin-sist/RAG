# Evaluation Spec Delta: C10 Eval Quality Threshold Gates

## ADDED Requirements

### Requirement: Versioned Quality Gate Profile And Compatibility Identity

C10 SHALL define a tracked `rag-quality-gate-profile-v1` contract that binds profile id/version/status、dataset release and manifest identity、sample selection、run/metric identity、required metric channels、fixed slices、thresholds、tolerances、minimum denominators、missing/error policy and profile identity. Profile validation MUST complete before any quality result is produced.

Only an `ACTIVE` profile with a `VALID` versioned dataset and compatible evidence MAY produce `PASS`. A `DRAFT` profile MAY be validated and exercised with synthetic evidence but MUST NOT be represented as an active quality gate. Unversioned、historical identity-incomplete or selection-incomplete evidence MUST NOT be retroactively accepted by matching a filename or aggregate number.

#### Scenario: Active profile and evidence identities match

- GIVEN an ACTIVE profile、VALID dataset release、full declared selection and all required run/metric identities match
- WHEN the profile and details evidence are validated
- THEN profile compatibility is `VALID`
- AND gate evaluation may proceed without any backend/provider call

#### Scenario: Profile contract or identity drifts

- WHEN schema/version/profile hash、dataset、selection、channel、slice、operator、threshold、tolerance or required identity is missing or differs
- THEN validation fails closed with a stable safe error code
- AND no threshold result is inferred from partial fields

#### Scenario: Draft unversioned or historical evidence

- GIVEN the profile is DRAFT OR the evidence dataset is `UNVERSIONED` OR required C8/C9 identity is unavailable
- WHEN a consumer requests a gate result
- THEN the result is not `PASS`
- AND historical metrics remain readable without being backfilled or promoted into a C10 baseline

### Requirement: Deterministic Channel Slice And Threshold Evaluation

C10 SHALL evaluate only profile-declared metrics in their declared retrieval、objective or judge channel. Slice axes SHALL be limited to deterministic `all`、`type`、`difficulty` and `answerability` values resolved from the profile-bound versioned dataset by sample id. Arbitrary executable expressions MUST NOT be accepted.

Each rule SHALL expose channel、slice、metric、operator、target、observed value、denominator、minimum denominator、required flag、tolerance/reference if applicable and a safe result reason. Required missing values、insufficient denominators、failed observations or incomplete sample selection MUST NOT be filled with zero、dropped from the denominator or calculated from a successful subset.

Hard thresholds SHALL use inclusive minimum/maximum semantics. Reference regression tolerance MAY apply only when reference and candidate profile/dataset/run/metric identities match. When a hard threshold and reference rule both exist, both MUST pass; tolerance MUST NOT waive status、identity、error、missing or denominator requirements.

#### Scenario: Overall and category slices are reproducible

- GIVEN the same profile、versioned annotations and complete per-sample evidence
- WHEN overall、type、difficulty and answerability rules are evaluated repeatedly
- THEN each slice has the same sample membership、denominator、observed metric and result
- AND category degradation cannot be hidden by the overall aggregate

#### Scenario: Required metric or denominator is incomplete

- WHEN a required metric is unavailable OR its denominator is below the profile minimum OR any required sample observation is missing
- THEN that rule is `NOT_EVALUABLE`
- AND the evaluator does not substitute zero、remove failures or return PASS

#### Scenario: Hard floor and regression tolerance both apply

- GIVEN compatible complete candidate and locked reference evidence
- WHEN a rule declares both a hard threshold and maximum absolute regression
- THEN the rule passes only if both conditions pass
- AND tolerance cannot rescue a safety/error/status/identity failure

### Requirement: Gate Status And Stable Exit Code Semantics

C10 gate result SHALL be exactly one of `PASS`、`FAIL`、`NOT_EVALUABLE` or `INVALID`. `PASS` requires an ACTIVE compatible profile、complete required evidence and every required quality rule passing. `FAIL` SHALL mean complete compatible evidence exists but at least one required quality threshold fails. `NOT_EVALUABLE` SHALL mean policy-valid evidence cannot support a quality decision because of draft status、channel/status/error/missing/selection/denominator/comparison incompleteness. `INVALID` SHALL mean the profile or input contract is invalid.

The standalone evaluator CLI SHALL return `0` for PASS、`3` for FAIL、`4` for NOT_EVALUABLE and `2` for INVALID. An unclassified evaluator runtime failure SHALL remain exit code `1` and MUST NOT be relabeled as a quality failure. Gate summary SHALL preserve all expected rules and safe reason codes even when one rule already failed.

#### Scenario: Complete evidence passes every rule

- GIVEN an ACTIVE compatible profile and complete evidence
- AND every required threshold and regression rule passes
- WHEN gate evaluation completes
- THEN gate status is `PASS`
- AND CLI exit code is `0`

#### Scenario: Complete evidence is below a quality threshold

- GIVEN profile/evidence identities and required denominators are complete
- WHEN at least one required hard or regression threshold fails
- THEN gate status is `FAIL`
- AND CLI exit code is `3` without misreporting evidence as incomplete

#### Scenario: Evidence cannot support a quality decision

- WHEN a required channel is partial/skipped/ineligible OR errors exceed policy OR selection/metric/denominator is incomplete
- THEN gate status is `NOT_EVALUABLE`
- AND CLI exit code is `4` without presenting a successful-subset quality result

#### Scenario: Profile or input contract is invalid

- WHEN profile/input JSON、schema、version、hash、operator or finite numeric contract is invalid
- THEN gate status is `INVALID`
- AND CLI exit code is `2`; unexpected runtime failures remain `1`

### Requirement: Offline Gate Safety And Evidence Activation Boundary

C10 SHALL provide an offline evaluator that consumes local details evidence and writes allowlisted aggregate gate output. Ordinary JSON、Markdown and console output MUST NOT add raw question、answer、expected content、claim、citation、context、provider body、secret、Authorization or absolute local path. Evaluation of an existing details artifact MUST make zero backend、embedding、rerank、ask、generation、judge or other provider calls.

C10 planning and offline implementation SHALL use only synthetic/static evidence and zero data egress. Activation of any numeric profile from real evidence MUST receive separate authorization after disclosing dataset/selection、provider/model、maximum calls、egress、cost or zero-cost basis、rate limits、timeout/retry and raw artifact handling. C10 MUST NOT auto-learn thresholds、change metric formulas or alter dataset、production prompt、retrieval、rerank、citation、no-answer、default judge/provider or application behavior to make a gate pass.

#### Scenario: Offline evaluator replays existing evidence

- GIVEN a local details JSON and tracked profile
- WHEN the evaluator calculates and writes a gate summary
- THEN backend/provider call count and data egress are 0
- AND ordinary output contains only allowlisted identity、metric、denominator、threshold、status and safe reason fields

#### Scenario: Reference evidence is not separately authorized

- GIVEN offline implementation is approved but live/reference evidence is not
- WHEN C10 tests and documentation are completed
- THEN only synthetic fixtures、static validation and existing local artifacts are used
- AND any real-evidence profile remains DRAFT without a production or judge quality claim

