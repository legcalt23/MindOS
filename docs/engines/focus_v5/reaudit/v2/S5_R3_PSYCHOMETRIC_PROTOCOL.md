# S5_R3_PSYCHOMETRIC_PROTOCOL — validity preconditions (Part 9A)

Artifact only; the determinations are the **psychometrician's**. This is the gate list that must be closed before
any official measurement / longitudinal comparison (ceiling C in the CEO packet). None is closed today.

| element | specification / open question | gate |
|---|---|---|
| **construct** | per-condition signal-detection sensitivity (d′) on a flanker (attention/interference) and a vigilance/Sentinel (sustained attention) tracer. NOT "attention capacity". | named |
| **estimand** | within-condition d′ LEVEL (never a between-condition DELTA — retired per Hedge 2018). Modality-specific (visual); AT modalities need a SEPARATE estimand (F-05). | named |
| **ICC (test-retest reliability)** | UNKNOWN_UNTIL_MEASURED for THIS implementation (F-10). Measure per-tracer ICC(2,1) across ≥2 sessions. Not assumed low (Hedge) nor fine (Kucina). | **BLOCKER_BEFORE_CLAIM** |
| **SEM** | derive from ICC + SD; must exist before any "change" statement. | BLOCKER |
| **MDC / RCI** | minimal detectable change / reliable-change index from SEM; no reliable-change UI until computed. | BLOCKER |
| **practice effects** | flanker/vigilance have strong practice/regression-to-mean; separate practice from signal (e.g., burn-in sessions, plateau detection). | BLOCKER |
| **test-retest design** | interval, number of sessions, device-held-constant vs varied; pre-registered. | required |
| **missingness / MNAR** | `excludedFraction` exclusion is likely MNAR w.r.t. device/fatigue/disability (F-04). Pre-register: reasons+coverage visible, sensitivity/bounds, all-designed vs observed vs technically-valid; no hidden attrition. | **BLOCKER** |
| **strata** | device Hz/precision class, accessibility modality, age band — analyze within-stratum; MNAR may be irreducible per stratum → separate norms. | required |
| **measurement invariance** | longitudinal (within person over sessions), by device class, by input modality. No cross-stratum comparison without invariance evidence. | BLOCKER |
| **power** | sample size for the ICC CI width + invariance tests before recruiting. | required pre-study |
| **claims ladder** | C0 descriptive (current) → C1 within-person reliable change (needs ICC/SEM/MDC) → C2 normative percentile (needs norms + invariance) → C3 transfer (needs far-transfer evidence — not in scope). Each rung gated by the row above. | governs claims |

**Verdict:** the two R3 *contract* properties are EXECUTED_PRIMARY (non-interference + monotonicity), but contract
correctness ≠ measurement validity. Reliability, MNAR, and invariance are empirical and OPEN; a passing unit test
cannot close them.
