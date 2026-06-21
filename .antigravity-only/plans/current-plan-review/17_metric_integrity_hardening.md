# 17 Metric Integrity Hardening

**Protection of Bayesian Competitive Rating:**
- **Global Rating:** Aggregate measure. Strictly hidden from user UI.
- **Block/Mode Rating:** Used for internal difficulty adaptation.
- **Farming Prevention:** Ratings only update if the task was completed under verified integrity conditions (no pauses, standard hardware, no accessibility overlays).
- **Premium Rules:** Premium accounts MAY access detailed historical performance charts. Premium accounts MUST NOT receive rating boosts, easier pools, or "second chance" attempts in ranked trials.
- **Public Ranked:** Explicitly FORBIDDEN until Phase 12 validation is complete and Codex signs off.

**Acceptance Criteria:**
1. `IntegrityStateEnum` must be `VERIFIED` for the Bayesian engine to apply a rating delta.
2. Premium flag is completely absent from the Bayesian math module.
