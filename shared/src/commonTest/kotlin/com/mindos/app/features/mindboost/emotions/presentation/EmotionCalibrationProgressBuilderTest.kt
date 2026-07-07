@file:Suppress("MagicNumber", "MaxLineLength") // Progress fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MC8u-D §6 — the reusable calibration-progress model: provisional vs official (never official before 5 valid
 * sessions), 0/5→5/5 progress, the 0–100 block score ("—" while calibrating), coarse not-counted/low-confidence
 * notes, non-clinical copy, no raw enum, and full coverage of the dashboard integration contract.
 */
class EmotionCalibrationProgressBuilderTest {

    @Test
    fun zero_of_five_is_provisional_and_calibrating() {
        val m = EmotionCalibrationProgressBuilder.build(validSessions = 0, blockScore = null, lastSessionCounted = null, lastSessionLowConfidence = false, nextRecommendation = null)
        assertEquals(EmotionStrings.PROGRESS_PROVISIONAL, m.statusLabel)
        assertFalse(m.officialRangeUnlocked)
        assertTrue(m.blockScoreCalibrating)
        assertTrue(m.blockScoreText.contains("—"), "a calibrating score shows an em dash, never a fabricated 0")
        assertNull(m.lastSessionText)
    }

    @Test
    fun four_of_five_does_not_unlock_the_official_range() {
        val m = EmotionCalibrationProgressBuilder.build(4, 70, lastSessionCounted = true, lastSessionLowConfidence = false, nextRecommendation = "Pausa")
        assertFalse(m.officialRangeUnlocked, "4/5 must never show an official range")
        assertEquals(EmotionStrings.PROGRESS_PROVISIONAL, m.statusLabel)
        assertTrue(m.progressText.contains("4/5"))
    }

    @Test
    fun five_of_five_unlocks_the_official_range_and_shows_the_block_score() {
        val m = EmotionCalibrationProgressBuilder.build(5, 82, lastSessionCounted = true, lastSessionLowConfidence = false, nextRecommendation = null)
        assertTrue(m.officialRangeUnlocked)
        assertEquals(EmotionStrings.PROGRESS_OFFICIAL_UNLOCKED, m.statusLabel)
        assertFalse(m.blockScoreCalibrating)
        assertTrue(m.blockScoreText.contains("82/100"))
    }

    @Test
    fun not_counted_and_low_confidence_notes_appear_only_when_relevant() {
        val flagged = EmotionCalibrationProgressBuilder.build(2, null, lastSessionCounted = false, lastSessionLowConfidence = true, nextRecommendation = null)
        assertEquals(EmotionStrings.PROGRESS_SESSION_NOT_COUNTED, flagged.notCountedNote)
        assertEquals(EmotionStrings.PROGRESS_LOW_CONFIDENCE, flagged.lowConfidenceNote)
        assertEquals(EmotionStrings.PROGRESS_LAST_PRACTICE, flagged.lastSessionText)

        val clean = EmotionCalibrationProgressBuilder.build(3, 60, lastSessionCounted = true, lastSessionLowConfidence = false, nextRecommendation = null)
        assertNull(clean.notCountedNote)
        assertNull(clean.lowConfidenceNote)
        assertEquals(EmotionStrings.PROGRESS_LAST_VALID, clean.lastSessionText)
    }

    @Test
    fun status_label_is_a_human_label_never_a_raw_enum() {
        val m = EmotionCalibrationProgressBuilder.build(5, 82, lastSessionCounted = true, lastSessionLowConfidence = false, nextRecommendation = "Separador")
        listOf("EXPLORATORY", "PRELIMINARY", "INITIAL_TREND", "STABLE").forEach {
            assertFalse(m.statusLabel.contains(it), "status must never leak the raw ProfileTier enum name '$it'")
        }
        assertTrue(m.disclaimer.isNotBlank())
    }

    @Test
    fun dashboard_contract_elements_are_all_renderable_from_the_model() {
        val m = EmotionCalibrationProgressBuilder.build(3, 60, lastSessionCounted = false, lastSessionLowConfidence = true, nextRecommendation = "Pausa")
        assertTrue(m.blockScoreText.isNotBlank())          // blockScore
        assertTrue(m.progressText.isNotBlank())            // calibrationProgress
        assertTrue(m.statusLabel.isNotBlank())             // provisionalOrOfficial
        assertNotNull(m.notCountedNote)                    // sessionNotCounted
        assertNotNull(m.lowConfidenceNote)                 // lowConfidence
        assertNotNull(m.lastSessionText)                   // lastSession
        assertNotNull(m.nextRecommendationText)            // nextRecommendation
        assertTrue(m.disclaimer.isNotBlank())              // nonClinicalDisclaimer
        assertEquals(8, EmotionDashboardContract.REQUIRED_SURFACE_ELEMENTS.size, "all 8 contract elements are covered")
    }
}
