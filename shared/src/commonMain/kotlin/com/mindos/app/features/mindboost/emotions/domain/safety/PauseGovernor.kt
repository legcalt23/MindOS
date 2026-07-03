package com.mindos.app.features.mindboost.emotions.domain.safety

/** Persisted-light state for the pause anti-gaming policy. */
data class PauseGovernorState(
    val pauseCount: Int = 0,
    val lastPromptAtMs: Long? = null,
)

/**
 * Decision after a pause. [reduceIntensity]/[recommendShorterSession] trip when pauses are over-used;
 * [promptCheckIn] is gated by the 45s cooldown (anti-nag) ONLY — it never suppresses the limit counter.
 */
data class PauseDecision(
    val reduceIntensity: Boolean,
    val recommendShorterSession: Boolean,
    val promptCheckIn: Boolean,
    val state: PauseGovernorState,
)

/**
 * Two-counter anti-gaming for the strategic pause (decision G): the LIMIT counter increments on EVERY
 * pause (so a burst of rapid pauses trips reduce + a shorter-session recommendation), while the 45s
 * cooldown only suppresses duplicate check-in PROMPTS. Pausing is always allowed and never penalizes
 * the score — it is logged as a safety event by the caller.
 */
class PauseGovernor {

    fun onPause(state: PauseGovernorState, nowMs: Long): PauseDecision {
        val count = state.pauseCount + 1
        val overUsed = count >= EmotionSafetyLimits.RAPID_PAUSES_FOR_REDUCE
        val canPrompt = state.lastPromptAtMs == null ||
            (nowMs - state.lastPromptAtMs) >= EmotionSafetyLimits.PAUSE_COOLDOWN_MS
        return PauseDecision(
            reduceIntensity = overUsed,
            recommendShorterSession = overUsed,
            promptCheckIn = canPrompt,
            state = state.copy(
                pauseCount = count,
                lastPromptAtMs = if (canPrompt) nowMs else state.lastPromptAtMs,
            ),
        )
    }
}
