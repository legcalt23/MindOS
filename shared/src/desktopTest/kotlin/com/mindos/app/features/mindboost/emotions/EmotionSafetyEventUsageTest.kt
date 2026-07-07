package com.mindos.app.features.mindboost.emotions

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u-B §3/§8: `SafetyEventType.SESSION_SHORTENED` is RESERVED / designed-only — no session flow emits it and no UI
 * renders it. This source-scan fails the build the moment any production file OTHER than its declaration sites
 * references it, so it can never silently become a live-but-broken (emitted or shown) path without real, tested
 * shortening behavior. (Desktop test — file IO is fine; not Detekt-scanned.)
 */
class EmotionSafetyEventUsageTest {

    // The only files allowed to name the reserved token: the enum declaration and the (unused) string declaration.
    private val declarationFiles = setOf("SafetyEvent.kt", "EmotionStrings.kt")

    @Test
    fun session_shortened_state_is_either_wired_or_not_user_visible() {
        val offenders = EmotionSourceLocator.kotlinFiles(EmotionSourceLocator.mainDir())
            .filter { it.name !in declarationFiles && it.readText().contains("SESSION_SHORTENED") }
            .map { it.name }
        assertTrue(
            offenders.isEmpty(),
            "SESSION_SHORTENED is designed-only; a live reference must wire real, tested shortening behavior: $offenders",
        )
    }
}
