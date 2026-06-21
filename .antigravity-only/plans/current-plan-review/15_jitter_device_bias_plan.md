# 15 Device Latency / Jitter Tolerance Profiling

**Problem:** Android and iOS screens, OS scheduling, and touch polling rates vary by 30-80ms, invalidating cross-platform comparison for Mental Speed (Choice Reaction).

**Plan:**
1. **Device Class Segmentation:** Segment matching pools by OS (Android vs iOS) and Refresh Rate (60Hz vs 90Hz/120Hz).
2. **Forbidden Comparisons:** Mental Speed tasks CANNOT be ranked globally across device boundaries. They may only be ranked within the same Hardware Class (e.g., 'Android-120Hz-Class').
3. **Required Calibration Protocol:** A future phase must introduce a 'Tap-hold-release' latency calibration task to measure exact device touch-pipeline lag before assigning a Hardware Class.

**Acceptance Criteria:**
1. `SessionInit` requires a `HardwareClass` enum.
2. `MatchmakingPoolState` refuses to match players if `taskType == MentalSpeed` and `HardwareClass` differs.

**Claude Code Implementation Prompt:**
"Implement HardwareClass segmentation in MatchmakingPoolState. Add an invariant test ensuring that a Mental Speed match request between differing Hardware Classes throws an `IllegalMatchException`."
