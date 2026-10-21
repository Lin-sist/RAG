## ADDED Requirements

### Requirement: C19 Frozen Calibration Execution Identity

C19 SHALL bind the accepted calibration corpus, ordered case/repeat selection, shared judge contract, canonical credential-free endpoint host/port/path, provider/model, temperature, timeout, output/context limits, zero-retry policy, audited source hashes and clean Git HEAD before live execution. Local plan/preflight MUST make zero backend/provider calls. Missing model or invalid identity MUST block live readiness without exposing rejected configuration.

#### Scenario: Local plan without a selected model
- GIVEN corpus and budget validate but the live judge model is not selected
- WHEN C19 local readiness runs
- THEN it reports the 4/72 planned budgets and blocks live execution with zero outbound requests

#### Scenario: Endpoint or source identity drifts
- GIVEN a frozen approved execution identity
- WHEN endpoint path, source hash, runtime setting or case/repeat order differs
- THEN execution or compilation fails closed without using the observations as comparable calibration evidence

### Requirement: C19 Bounded Durable Judge Attempts

C19 SHALL use a fixed four-case one-repeat canary and a separate 24-case three-repeat full with independent authorization, at most 4 and 72 judge POST attempts, respectively. It MUST reserve budget before every request, persist attempt start/outcome in an exclusive no-overwrite local run directory, use zero automatic retries and stop after the first failed observation. It MUST NOT follow redirects to another provider, resume ambiguous calls or splice successful observations from different runs. Gold labels MUST NOT enter provider payloads.

#### Scenario: A request would exceed the approved budget
- GIVEN the current stage has exhausted its attempt allowance
- WHEN another request is prepared
- THEN it is rejected before the network boundary and no additional provider request occurs

#### Scenario: Failure or interruption during a run
- GIVEN attempts have already been persisted
- WHEN one observation fails or execution stops before its outcome is known
- THEN existing evidence is retained without overwrite, unattempted observations stay missing, and unknown outcomes are not silently replayed

### Requirement: C19 Independently Compiled Safe Evidence

C19 SHALL recompute calibration coverage and quality diagnostics from validated case/repeat observations and attempt evidence, rather than trusting a supplied summary. Missing, invalid, duplicate, unexpected or identity-mismatched observations MUST suppress comparable agreement and repeat-consistency rate conclusions. Complete valid evidence with low agreement SHALL remain measurement-complete. Tracked summaries MUST contain only safe identities, hashes, IDs, counts, statuses and aggregate metrics; raw text, provider reason/body, credentials and absolute local paths MUST remain excluded. C19 MUST NOT activate C20 gates or modify accepted thresholds or production behavior.

#### Scenario: A full repeat is missing
- GIVEN full calibration expects 72 observations and one is absent
- WHEN the compiler evaluates the run
- THEN coverage and missing counts remain visible and agreement/repeat rates are unavailable rather than calculated from a successful subset

#### Scenario: Complete measurement has low agreement
- GIVEN all expected observations, attempts and identities are valid
- WHEN the measured agreement is low
- THEN evidence may be COMPLETE with the observed low values and no automatic threshold change or production release decision

#### Scenario: Raw response contains private content
- GIVEN local raw evidence contains a provider reason or answer text
- WHEN the safe review summary is compiled
- THEN that content is excluded and compilation performs no external calls

### Requirement: C19 Offline Acceptance And Deferred Live Evidence

C19 offline execution identity, bounded attempt/ledger tooling and safe evidence compilation MAY be accepted and archived independently of a complete live calibration run when the user explicitly approves that scope. Outstanding full live calibration SHALL remain an identified P1 evidence debt. Historical partial runs MUST retain their original status and MUST NOT become complete or comparable through archive acceptance. Any future live run SHALL require independently reviewed identity and authorization; archive acceptance grants no new provider calls.

#### Scenario: User accepts the implemented offline scope
- GIVEN offline contract/toolchain checks pass and the user explicitly accepts only that scope
- WHEN C19 is archived
- THEN accepted baseline contains the verified contracts and P1 debt retains the incomplete live calibration target
- AND historical live evidence and unavailable agreement/repeat conclusions remain unchanged

#### Scenario: Live calibration is resumed after archive
- GIVEN the offline toolchain is archived but the P1 live evidence debt remains open
- WHEN a future live run is proposed
- THEN its identity and call budget require separate review and authorization without resuming or splicing old runs
