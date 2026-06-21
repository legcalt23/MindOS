# Daily Streak Display Validation Report

**Review Mode:** `FAST_FEATURE_REVIEW`  
**Proposal:** "Add a simple daily streak display to the MindBoost dashboard."  
**Consolidated Verdict:** **NO-GO**  
**Orchestration ID:** Run 1

---

## 1. Squad Voting & Attributions

| Subagent | Conversation ID | Vote | Confidence | Evidence Grade | Key Focus |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Product Strategy** | `dd442d85-b1e6-4b45-95f0-fdaa786e87cd` | **NO-GO** | High | A / C | Strategic fit, OvertrainingGuard conflicts. |
| **UX/UI Experience** | `319e0817-cc0b-4583-ad2c-16aa375967b2` | **REVISE** | High | A | Visual style, active recovery freezes. |
| **Tech Architecture** | `79bbef71-cb31-4e65-9583-12d8550710a4` | **REVISE** | High | B | Modularity boundaries, database models. |
| **QA/Evidence** | `99479896-c19f-44f6-a78a-e9ed8b8e7a96` | **NO-GO** | High | B / C | Test coverage, calculation bugs. |
| **Red Team** | `d3c94ca2-ee91-4a41-b9a9-769d9651c4ff` | **NO-GO** | High | B | Exploits, clock cheating, motivation risks. |

---

## 2. Key Findings & Risks

### A. Strategic Conflict (The Overtraining Paradox)
MindBoost's cognitive exercises (E1 and E6) target prefrontal cortex attention pathways, which require periodic recovery. The `OvertrainingGuard` is designed to force rest days (`ForceRest` status) when user performance drops. 
A daily streak display incentivizes users to train even when fatigued or forced to rest, causing a direct conflict between the gamified reward loop and the cognitive training methodology.

### B. Logic & Code Quality Bugs
The existing rough prototype in `FocusHomeStateHolder.kt` suffers from two critical errors:
1.  **Stale Streak Bug:** The `estimateStreak` helper continues to show active streaks even if the last session occurred months ago, due to a missing timestamp threshold comparison.
2.  **Timezone Shift Bug:** The code divides the raw millisecond timebase by `MILLIS_PER_DAY` in GMT. This ignores the user's local timezone, causing midnights to shift and resetting streaks prematurely for users in other regions.
3.  **Zero Test Coverage:** No tests are defined in `FocusHomeStateHolderTest.kt` to cover streak increment, reset, or timezone behaviors.

### C. Aesthetic and Retention Risks
- Target high-performing developers and traders reject standard gamified mechanics like colorful fire icons or progress bars, which dilute the clinical, high-utility brand voice.
- Traditional daily streaks lead to "streak-break abandonment"—when users miss a day due to travel or work, the loss of progress causes them to abandon the app entirely.

---

## 3. Required Revisions & Alternatives

To resolve the NO-GO verdict, any daily consistency tracking must be replaced with the following professional equivalents:

1.  **Baseline Calibration Freshness:** A rolling indicator showing how recently the baseline was verified (e.g., "Baseline verified 2 days ago") rather than consecutive calendar days.
2.  **Weekly Training Target:** Track target sessions per week (e.g., 3 out of 4 target days) allowing users to rest on weekends or busy days without breaking progress.
3.  **Active Recovery Freezes:** If streaks are maintained, any day where the `OvertrainingGuard` recommends or forces a rest must freeze the streak, aligning gamification with cognitive safety.
4.  **Local Timezone Logic:** Rewrite `estimateStreak` to utilize local midnights instead of GMT offsets.
5.  **Aesthetic Monospace Representation:** Replace graphic badges with clean, low-contrast monospace text chips (e.g., `RACHA: 5` or `STREAK: 5`), matching the `StatChip` design system.
