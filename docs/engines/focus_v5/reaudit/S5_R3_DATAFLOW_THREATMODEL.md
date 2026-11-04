# S5_R3_DATAFLOW_THREATMODEL — focus2 (scoped, unmounted)

```
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW · scope = focus2 vertical (CONTRACT_INERT_NO_WIRING)
STATUS: focus2 is not mounted; this models the CONTRACT that Paso 4 would wire. Verdict axis: PRIVACY_DATA_LIFECYCLE = FAIL.
```

Informed by the privacy lens (`S5_R3_LENS_OUTPUTS.json`, findings L5-01..06) + source reading. Not a substitute
for a DPO/legal DPIA (escalated, RA-12/MB-FOCUS-0054-adjacent).

## 1. Per-field data-flow (summary tier only — no raw per-trial data persists, by TYPE)

| Field | Source | Purpose | At rest | Retention | Erasure | Notes / gaps |
|---|---|---|---|---|---|---|
| `SdtCell` tallies (hits/misses/FA/CR/omissions/anticipations) | on-device trial resolution | descriptive scoring | `SecureFocusSessionStore` (`mindboost_focus2`, encrypted; Android Keystore AES-256-GCM) | until account-deletion / drain | account eraser (AccountBound) | **GAP-1**: subject-revocation NOT wired for focus2 stores (L5-01); **no regression test** unlike sibling stores. |
| `FocusSessionDescriptor` (sealed pre-trial: spec, modality, a11y flags) | seal | reproducibility | same store | same | same | seal keeps 5 optional temporal params (MB-FOCUS-0048). |
| `screenReaderActive` / `switchAccessActive` / `SWITCH_SEQUENTIAL` modality | platform a11y | modality gating | in descriptor | same | same | **disability-proxy signal** — GDPR-sensitive; DPIA self-classified but not done (L5-05). |
| write-ahead record (`FocusWriteOutcome`) | recorder | crash recovery | same store | drained pre-session | drain (not adopt, D-R3-04) | **LENS4-02**: drain discards the typed outcome; **LENS4-03/L5-03**: subject-prefixed keys in a blanket-cleared shared namespace → cross-account collateral-wipe (GAP-2). |
| TRIAL_JOURNAL (opt-in raw, consent-gated) | `FocusJournalAdmission` | debug | store (if enabled) | — | — | **L5-04**: consent gate has ZERO production call-sites (control with no call-site = cover, not protection). |
| RT / latency | input event | SHADOW RT lane only | NOT official (v1) | — | — | never official; `OFFICIAL_ELIGIBLE` inconstructible. |

**No raw per-trial stream persists** — enforced by TYPE (per-condition counts only). That half is genuinely
minimized and matches ADR-FOCUS-DATA. The FAIL is on the lifecycle EVENTS (revocation/wipe), not the minimization.

## 2. Threat model (STRIDE/LINDDUN-lite, focus2 scope)

| Threat | Vector | Status | Control / gap |
|---|---|---|---|
| **Cross-account linkage** (Linkability) | shared physical namespace, subject-prefixed keys | **OPEN (GAP-2)** | LENS4-03/L5-03: account switch can collateral-wipe or expose another subject's keys. Fix = per-subject namespace isolation + test (mirrors the Sessions/SessionRuns fix already shipped). |
| **Retained data after revocation** (Non-compliance) | revocation not wired | **OPEN (GAP-1)** | L5-01: `SubjectRevocationAuthority` not composed into focus2 stores; no regression test. |
| **Disability inference** (Detectability/Disclosure) | screenReader/switch flags | **OPEN** | L5-05: disability-proxy at rest without DPIA; escalate to DPO. |
| **Tampering / replay** | SessionSeed/RngVersion | DESIGN | seed persistence + replay risk unaudited for wiring (§3.6.7). |
| **Minors data** | no age gate | **OPEN (product)** | L5-06/RA-12: no minors escalation anywhere; target population undefined. |
| **Repudiation / silent write loss** | drain discards outcome | **OPEN** | LENS4-02: consume `FocusWriteOutcome` (2026-07-14 learning). |

## 3. Verdict

`PRIVACY_DATA_LIFECYCLE = FAIL`. Contained by `CONTRACT_INERT_NO_WIRING` (unmounted) — not a live breach today,
but every GAP is a mount precondition. `security "APTO"` is retired to `DOCUMENTATION_SECURITY_REVIEW_ONLY`
(MB-FOCUS-0052). A DPO/legal DPIA (health-data / minors) is out of scope for this AI review and is escalated.
