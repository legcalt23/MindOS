package com.mindos.app.features.mindboost.emotions

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u P0-3 guard: the deleted 10-competence-score `EmotionalRegulationProfile` model must never reappear in
 * production. Wiring block metrics into per-competence "scores" would convert gameable/low-n/chance-inflated
 * signals into a judgment of the user's emotional worth (charter §2). This source-scan fails the build the
 * moment any production file DECLARES or CONSTRUCTS the type. (Desktop test — file IO is fine; not Detekt-scanned.)
 */
class EmotionalRegulationProfileAbsenceTest {

    @Test
    fun emotional_regulation_profile_type_is_not_present() {
        val offenders = EmotionSourceLocator.kotlinFiles(EmotionSourceLocator.mainDir())
            .filter { file ->
                val text = file.readText()
                text.contains("class EmotionalRegulationProfile") || text.contains("EmotionalRegulationProfile(")
            }
            .map { it.name }
        assertTrue(
            offenders.isEmpty(),
            "EmotionalRegulationProfile must not be declared or constructed in production; found in: $offenders",
        )
    }
}
