# S5_R3_CEO_DECISION_PACKET — decisions the CEO alone can make (Part 10)

R3 stays **CANDIDATA / REOPENED**. None of these decisions promote it. `TRAINING_FREE` yes/no is replaced by a
capability matrix so the CEO chooses a *ceiling*, not a slogan.

## 1. TRAINING_FREE capability matrix (pick ONE column as the ceiling)

| attribute | A · Training-only STATELESS | B · Training-only PERSONALIZED | C · MEASUREMENT-CAPABLE |
|---|---|---|---|
| exercise playable | yes | yes | yes |
| immediate feedback | yes | yes | yes |
| score visible to user | no (practice only) | no official; local streak/effort only | yes (official) |
| RT visible | as feedback only | as feedback only | yes, as a measure |
| local difficulty adaptation | no | yes (session-local) | yes |
| longitudinal progression | no | no | yes (needs reliability) |
| local persistence | none | summaries only (encrypted) | summaries + history |
| cloud sync | no | no | only if privacy gates closed |
| scientific/normative use | no | no | only if ALL validity gates closed |
| eligibility label shown | "not measured" | "practice / not an assessment" | official state machine |
| retention | none | user-controlled, short | defined + DPIA |
| deletion / account isolation | n/a | required (F-01/F-08 gates) | required + verified |
| accessibility | any modality, no comparison | any modality, no comparison | modality-fair or separate norms (F-05) |
| **precondition gates** | none | F-01, F-08 (privacy/wiring) | **ALL**: F-01,02,03,04,05,08,10 + reliability + MNAR + DPIA |

- **A** ships today's honest ceiling with zero measurement risk. **B** unblocks adaptive UX without any official
  claim. **C** requires every open validity/privacy gate closed first — it is NOT reachable now.
- Recommended: **A or B** for any near-term human exposure; **C** only after Paso 4–6 + specialist sign-off.

## 2. The nine CEO decisions

| # | decision | note |
|---|---|---|
| 1 | **Intended use (official)** | training vs measurement device; sets the whole claims ceiling |
| 2 | **Claims ceiling** | C0 descriptive is the current max; no "capacity"/normative/reliable-change |
| 3 | **Initial markets / jurisdictions** | drives which legal regimes (GDPR/GDPR-K/COPPA/…) apply |
| 4 | **Target population** | adults-only vs includes minors (F-11) |
| 5 | **Accessibility / inclusion policy** | modality-fair measurement vs separate estimand/norms vs training-only for AT users (F-05) |
| 6 | **TRAINING_FREE policy (detailed)** | pick a column in §1 |
| 7 | **Data/telemetry risk budget** | what may be collected/retained; sets DPIA scope |
| 8 | **Commit / anchor authorization** | commit the R3 package to fix `FAIL_UNANCHORED` (F-06) |
| 9 | **Internal alpha vs external human exposure** | any human data trips the privacy/DPIA + population gates |

## 3. What is NOT an offerable CEO option

The **legal / high-risk / privacy screening is a mandatory GATE before any data wiring, not a column equivalent to
skipping it.** If DPO/legal screening finds high risk, a DPIA is required. The CEO authorizes/prioritizes the
screening and sets intended use, markets, claims, and resources; the CEO does **not** rule that health-data or
minors law does not apply — that is the DPO/legal determination (see §Roles in the a11y/privacy records).
