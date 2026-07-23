# Evaluation Spec Delta: C9a Claim Evidence Objective Metrics

## ADDED Requirements

### Requirement: Deterministic Answer Claim Extraction

C9a SHALL extract ordered claim units from successful, non-empty answerable generation outputs using a tracked deterministic splitter. The splitter SHALL use paragraph/list boundaries and Chinese/English sentence-ending punctuation without calling an embedding、LLM、judge or other external provider. It MUST preserve the visible claim text and order, assign stable per-output indexes, and record splitter/version identity in run metadata.

Claim extraction SHALL NOT use expected answer points as a substitute for generated claims and SHALL NOT semantically rewrite、merge or summarize the generated answer. No-answer samples MUST remain in the independent no-answer metric channel and MUST NOT enter the objective claim-support denominator.

#### Scenario: Answerable output is split reproducibly

- GIVEN a successful answerable ask contains paragraphs、sentences or list items
- WHEN the C9a splitter runs repeatedly with the same algorithm version
- THEN it returns the same ordered claim units、indexes and hashes
- AND no provider or backend call is made by claim extraction

#### Scenario: Empty and structural-only fragments

- GIVEN an answer contains blank text、pure numbering、punctuation、heading markers or isolated citation markers
- WHEN claim extraction runs
- THEN those structural-only fragments do not become claims
- BUT a non-empty eligible answer that yields zero claims records `empty_claim_set` and a partial claim metric status

#### Scenario: No-answer remains separate

- GIVEN a sample has `should_answer=false`
- WHEN generation/no-answer metrics are evaluated
- THEN its refusal remains governed by no-answer accuracy and citation-violation metrics
- AND refusal text does not enter the C9a claim-support numerator or denominator

### Requirement: Validated Citation Evidence And Objective Lexical Alignment

C9a claim evidence SHALL be limited to returned citations that first pass the existing citation identity and snippet-to-returned-context provenance checks. Unvalidated citations and retrieved contexts that were not explicitly represented by a validated returned citation MUST NOT be used to support a claim.

Each claim SHALL be classified as `exact`、`token_overlap` or `unsupported`. `exact` requires deterministic normalized containment. `token_overlap` SHALL use the tracked ASCII-token/CJK-bigram tokenizer with claim tokens as denominator、a fixed `0.70` minimum coverage and at least 2 claim tokens. A claim with no eligible evidence、insufficient comparable tokens or coverage below threshold MUST remain in the denominator as `unsupported` with a stable reason.

The report SHALL name this result objective/lexical alignment. It MUST NOT describe the metric as semantic entailment、ground truth、complete factual correctness or independent faithfulness.

#### Scenario: Claim matches validated evidence

- GIVEN a claim and at least one returned citation that passes provenance validation
- WHEN the claim is contained by the eligible evidence or reaches the tracked token coverage threshold
- THEN the claim is supported as `exact` or `token_overlap`
- AND details record the deterministic best evidence、method、coverage and algorithm identity

#### Scenario: Citation fails provenance validation

- GIVEN a returned citation cannot be matched to its returned context or its snippet lacks provenance support
- WHEN C9a builds eligible evidence
- THEN that citation is excluded from claim matching
- AND any claim without another eligible match is counted as `unsupported`

#### Scenario: Claim has no sufficient lexical support

- GIVEN a claim has no eligible citation、fewer than 2 comparable tokens or best coverage below `0.70`
- WHEN objective alignment is aggregated
- THEN the claim stays in the denominator as `unsupported`
- AND the result exposes a stable reason without raw exception or secret content

### Requirement: Claim Metric Denominator And Local Status

The objective claim support rate SHALL equal supported extracted answerable claims divided by all successfully extracted answerable claims. Claims without citations or matches MUST NOT be removed from the denominator. Per-sample and aggregate outputs SHALL expose claim total、supported/unsupported count、exact/token count、support rate、eligible evidence count and completeness/error counts.

C9a SHALL expose an independent `COMPLETE / PARTIAL / SKIPPED / NOT_APPLICABLE` claim metric status. Ask failures、empty eligible answers or extraction failures SHALL make the relevant claim channel partial rather than silently shrinking the sample set. Retrieval-only execution SHALL be skipped; an evaluated selection with no answerable samples SHALL be not applicable.

C9a MUST NOT change the existing global `CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED` Report status or judge status semantics. Objective/judge global status separation and judge calibration remain C9b scope.

#### Scenario: Complete claim metrics

- GIVEN every selected answerable sample has a successful non-empty ask and complete claim alignment
- WHEN results are aggregated
- THEN claim metric status is `COMPLETE`
- AND support rate uses every extracted claim including unsupported claims

#### Scenario: Ask or extraction is incomplete

- GIVEN at least one selected answerable sample has ask failure、empty answer or extraction failure
- WHEN claim results are aggregated
- THEN claim metric status is `PARTIAL`
- AND the report exposes affected sample counts without claiming a complete denominator

#### Scenario: Retrieval-only or no answerable samples

- GIVEN ask is skipped OR the evaluated selection contains no answerable samples
- WHEN C9a status is produced
- THEN status is respectively `SKIPPED` or `NOT_APPLICABLE`
- AND missing claim metrics are not reported as zero support

### Requirement: Algorithm Identity Compatibility And Safety Boundary

C9a reports and details SHALL record claim metric、splitter、tokenizer、threshold、minimum-token and evidence-policy identity. Results with differing C9a identities MUST NOT be presented as directly comparable. Historical reports without C9a fields MUST remain readable and SHALL be interpreted as “claim metric unavailable”, not as zero.

C9a changes SHALL be additive and MUST NOT modify dataset release/question/annotation/fixture identity、existing retrieval/generation/citation/no-answer/judge formulas、production prompt/citation behavior or default provider. Aggregate and ordinary output MUST NOT add raw question、answer、claim、citation snippet、context、secret、Authorization or absolute local path. Per-sample raw details MAY retain claim text only under the existing local raw-evidence boundary.

C9a planning and offline implementation SHALL use zero real embedding、rerank、ask、judge、LLM/provider calls and zero data egress. Any formal generation evidence run MUST receive separate authorization after disclosing call bounds、provider/model、egress、cost、rate limit、timeout/retry and raw artifact handling.

#### Scenario: Algorithm identity differs

- GIVEN two reports use different splitter、tokenizer、threshold or evidence-policy identity
- WHEN a consumer attempts to compare objective claim support
- THEN the reports are marked not directly comparable for C9a
- AND no delta is inferred from the shared dataset name alone

#### Scenario: Historical report lacks C9a fields

- GIVEN a pre-C9a report remains stored with its original content
- WHEN current tooling reads that report
- THEN existing metrics remain readable
- AND claim metrics are unavailable rather than backfilled or treated as zero

#### Scenario: No external-call authorization

- GIVEN planning or offline implementation is underway without separate live-run authorization
- WHEN C9a code and tests are executed
- THEN only local deterministic fixtures、static checks and unit tests are used
- AND real embedding、rerank、ask、judge、LLM/provider calls and data egress remain zero
