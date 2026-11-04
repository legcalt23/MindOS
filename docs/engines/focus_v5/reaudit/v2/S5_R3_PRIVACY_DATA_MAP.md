# S5_R3_PRIVACY_DATA_MAP — focus2 data-flow map (Part 9B)

Design-level map of the focus2 (`mindboost_focus2`) data surface. The code is **CONTRACT_INERT / UNTRACKED WIP**;
this maps what it WOULD process when wired. Legal determinations (special-category/health, minors) are the
**DPO/legal's**, not this AI review's. Related: `S5_R3_DATAFLOW_THREATMODEL.md`.

| field / signal | purpose | storage | retention | telemetry? | notes / risk |
|---|---|---|---|---|---|
| `SdtCell` counts (hits/misses/FA/CR/omissions/anticipations) | scoring summary | encrypted `SecureKeyValueStore`, namespace `mindboost_focus2` (AccountBound) | session summary only (no raw trials — ADR-DATA option i) | aggregate events only | behavioral cognitive data → potentially sensitive |
| `SessionEvidence` / eligibility state | honest state machine | same | summary | state events (16 local) | no official score persisted (C0) |
| seal metadata: `probeCapability`, `clockDomain`, `inputTimestampPrecision`, `nominalHzBand` | timebase authority gate | same | summary | device-class events | **F-08: currently unpopulated by the only prod seal caller** |
| consent scopes (`FocusDataConsent`) | ADR-DATA (iii) opt-in gates | same | until revoked | no | revocation must be durable (F-01) |
| `TRIAL_JOURNAL` (opt-in raw-ish) | debug/journal | same, gated by `FocusJournalAdmission` | opt-in, capped | no | **L5-04: zero production call-sites (inert)** |
| a11y proxies: `screenReaderActive`, `switchAccessActive`, `SWITCH_SEQUENTIAL` | modality selection | in seal metadata | summary | device-capability events | **L5-05: disability proxies → DPIA (special-category risk); DPO** |
| crash / non-fatal | diagnostics | Crashlytics (typed events only) | per Crashlytics | yes (allowlisted `NonFatalTelemetryEvent`) | no raw Throwable (2026-06-20 rule); no PII |
| debug export | QA | private, pseudonymized, random filename | ephemeral | no | 2026-06-20 minimization rule |
| revocation tombstone | GDPR Art. 17 | `SubjectRevocationAuthority` namespace | permanent (anti-resurrection) | no | **F-01: not wired into focus2 stores + untested** |
| deletion (account) | GDPR Art. 17 | `AppGraph` eraser + `accountDeletion.ts` | on delete | no | `mindboost_focus2` in AccountBound; erase must fail-closed |
| account isolation | multi-account safety | subject-prefixed keys in shared namespace | — | — | **F-08/L5-03: cross-account collateral-wipe pattern reintroduced** |
| processors | — | Firebase (Google) for crash/telemetry; no focus2 cloud sync (inert) | — | — | no third-party focus2 data flow yet |
| derived data | eligibility label, difficulty | in-memory / summary | session | state events | no capacity/normative derivation (C0) |

**Open privacy blockers:** F-01 (revocation gap, untested), F-08 (namespace isolation, seal metadata), L5-05
(disability-proxy DPIA — DPO), F-11 (minors — DPO/legal). Roles: **controller/organization** is accountable and
decides; **DPO** advises/monitors independently and determines special-category/health + DPIA trigger;
**legal** interprets regulation; **CEO** sets intended use, markets, claims, resources.
