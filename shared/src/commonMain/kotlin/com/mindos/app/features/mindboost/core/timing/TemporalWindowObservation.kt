package com.mindos.app.features.mindboost.core.timing

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §5/§7 [R2-HIST] — what a probe drained for ONE response window (trial).
 *
 * Ephemeral by design: it travels as DATA into the runtime input, is folded into integer tallies,
 * and is never persisted (persisting ordered per-window records is prohibited — privacy P0-02).
 * All quantities are intra-domain durations/counts; no absolute timestamp ever leaves the adapter.
 */
data class TemporalWindowObservation(
    val sampledFrames: Int,
    val droppedFrames: Int,
    val criticalFrameJitter: Boolean,
    /**
     * Render-marker delay: state-set → first frame ISSUED at/after it, or `null` where the platform
     * has no such anchor (iOS S5, desktop).
     *
     * DESCRIPTIVE ONLY — it does not classify anything (Paso 5, B6/L1). It is a proxy for how late
     * the frame pipeline was, not a measurement of when the user saw the stimulus, so no verdict may
     * be derived from it until a validated end-to-end error bound exists. It is still recorded
     * because the shadow series is what a future physical validation (Paso 6) would be calibrated
     * against; recording a quantity and letting it decide are different things.
     */
    val presentToFirstFrameCommitNanos: Long? = null,
    /**
     * S5 · Paso 5 (B1) — the probe's bounded ring had to overwrite frames of THIS window, so the
     * sample is incomplete. Fail-closed at the policy: an incomplete sample is `NoObservation`,
     * never CLEAN (absence must not fabricate perfection) and never EXCLUDED (our buffer size is
     * not evidence about the user's trial).
     */
    val bufferOverflow: Boolean = false,
    /**
     * S5 · Paso 5 (B4/F-R3-E) — the cadence the PLATFORM STATES the display should deliver, or
     * `null` where the platform cannot honestly state one (iOS in S5: CADisplayLink reports the
     * system's refresh cadence, not what this app's render loop was targeting). A window with no
     * reference cadence has no definition of "dropped frame", so it is unclassifiable, not clean.
     */
    val expectedCadenceHz: Double? = null,
    /**
     * The cadence the frames were ACTUALLY delivered at (median inter-frame delta). Kept as a fact
     * separate from [expectedCadenceHz] precisely so their DISAGREEMENT is visible: a stream pacing
     * at half the panel's rate is a different situation from a stream dropping half its frames, and
     * a single `refreshHz` doing both jobs cannot tell them apart.
     */
    val detectedCadenceHz: Double? = null,
) {
    /** Drop fraction, or `null` when nothing was sampled (never a fabricated 0.0). */
    fun frameDropFraction(): Double? =
        if (sampledFrames > 0) droppedFrames.toDouble() / sampledFrames else null
}
