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
