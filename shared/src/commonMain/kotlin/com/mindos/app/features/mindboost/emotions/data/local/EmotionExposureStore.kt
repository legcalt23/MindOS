@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.emotions.data.local

/**
 * MC8u-D §5 — local, PII-free tracking of recently-shown content phrases per exercise, so the EM3/EM10 exposure
 * guard can deprioritize items the user saw recently. This is an OPTIONAL capability implemented by the production
 * [LocalEmotionRepository]; the session holder accesses it via `repository as? EmotionExposureStore`, so test fakes
 * that only implement [EmotionRepository] are unaffected (they simply get no exposure history → pre-guard behavior).
 *
 * Stored data is minimal and technical: a capped, recency-ordered list of neutral bank phrases already visible in
 * the UI — never self-report, never scores, never free text. Erased with the rest of the namespace on account delete.
 */
interface EmotionExposureStore {
    /** The recently-shown phrases for [exerciseId] (most-recent first), capped. Empty when nothing is tracked yet. */
    suspend fun recentExposedPhrases(userId: String, exerciseId: String): List<String>

    /** Records [phrases] as freshly shown for [exerciseId], keeping only the most recent [EXPOSURE_CAP] distinct ones. */
    suspend fun recordExposedPhrases(userId: String, exerciseId: String, phrases: List<String>)

    companion object {
        /** Recency window: roughly one session's worth of trials, so the immediately-preceding session is deprioritized
         * while leaving enough fresh bank content to fill the next block (graceful fallback when the bank is small). */
        const val EXPOSURE_CAP = 20
    }
}
