## ADDED Requirements

### Requirement: C20 Draft Objective Profile And Independent Judge Advisory

C20 SHALL provide a native C10 DRAFT profile for full accepted dataset and generation/citation identity. Only objective SHALL be required; judge SHALL remain advisory. Numeric targets/tolerances SHALL be unset pending separate approval. DRAFT review MUST remain NOT_EVALUABLE/exit4, without activation or PASS. C19 incomplete calibration SHALL remain deferred rather than accepted or spliced.

#### Scenario: Objective complete and judge skipped
- GIVEN compatible full objective evidence and skipped or partial judge
- WHEN reviewed locally
- THEN objective observations and denominators remain visible and judge advisory values are unavailable
- AND judge does not downgrade objective completeness

#### Scenario: Thresholds are not approved
- GIVEN a DRAFT profile with unset targets
- WHEN complete measurements exist
- THEN review remains NOT_EVALUABLE/exit4 and never PASS

### Requirement: C20 Existing Metric Reuse And Safe Local Review

C20 SHALL reuse C9a counts and denominators for keyword, citation, claim and no-answer through the shared evaluator. Missing, malformed, negative or impossible counts MUST suppress comparable observations. Policy/activation status SHALL be distinguished from measurement completeness. Review MUST be local-only, allowlisted, exclusive no-overwrite and MUST NOT change historical evidence, production behavior or numeric thresholds.

#### Scenario: Recovered retries exceed draft policy
- GIVEN complete objective evidence with retries above the draft policy
- WHEN reviewed
- THEN observations may be shown with an explicit policy blocker and no gate approval

#### Scenario: Identity or counts are invalid
- GIVEN mismatched dataset/ordered selection/run identity or malformed counts
- WHEN reviewed
- THEN affected values are unavailable with fixed safe reasons and zero provider calls

#### Scenario: Output exists or aliases input
- GIVEN a prior output or input selected as output
- WHEN the CLI writes
- THEN it refuses without overwriting either artifact

### Requirement: C20 Independent Repeat Intake And Threshold Review Packet

C20 SHALL compile three independently identified full objective candidate repeats from local details/metadata under the DRAFT profile. Index/total, full ordered selection, metadata binding, clean source provenance, runtime/metric identities, distinct artifacts and the existing strict policy MUST be checked. Missing, duplicated, drifted or ineligible evidence MUST NOT produce a successful-subset distribution. Per-rule repeat slots and denominators SHALL remain visible with unavailable values null. Complete comparable repeats MAY expose minimum/median/maximum/spread and an unset target/tolerance worksheet, but MUST NOT approve thresholds, activate profiles or produce a gate PASS. Compilation SHALL perform zero provider calls and exclusive safe output only.

#### Scenario: One historical run is supplied
- GIVEN only one accepted historical run with total=1 or policy-incompatible retries
- WHEN the reference intake runs
- THEN it reports missing/ineligible repeat evidence with no quality distribution or activation

#### Scenario: Three independent complete candidates exist
- GIVEN repeat indexes 1/2/3, total3, compatible identity and complete policy-eligible observations
- WHEN compiled locally
- THEN all three values/denominators and deterministic distribution are shown FOR_REVIEW
- AND targets/tolerances remain null, gateStatus NOT_EVALUABLE and exit4

#### Scenario: Duplicate or drifted evidence is supplied
- GIVEN a reused artifact/run timestamp or incompatible runtime identity
- WHEN compiled
- THEN status is NOT_COMPARABLE and no rule distribution is emitted
