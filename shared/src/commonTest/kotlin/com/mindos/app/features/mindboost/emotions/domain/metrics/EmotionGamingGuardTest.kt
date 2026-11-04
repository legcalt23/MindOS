@file:Suppress("MagicNumber", "MaxLineLength") // RT/streak fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u-B §1E: direct coverage of the pure gaming/validity guard. */
class EmotionGamingGuardTest {

    private fun trial(outcome: EmotionTrialOutcome, rt: Long?, key: EmotionResponseKey): ClassifiedEmotionTrial {
        val stim = EmotionStimulus(0, EmotionResponseKey.LEFT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.LEFT, 0.6), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    @Test
    fun impossible_rt_share_counts_sub_floor_responses() {
        assertEquals(0.5, EmotionGamingGuard.impossibleRtShare(listOf(50.0, 60.0, 500.0, 600.0)), 1e-9)
        assertEquals(0.0, EmotionGamingGuard.impossibleRtShare(emptyList()), 1e-9)
    }

    @Test
    fun longest_same_key_streak_finds_the_run() {
        val keys = listOf(
            EmotionResponseKey.LEFT, EmotionResponseKey.LEFT, EmotionResponseKey.RIGHT,
            EmotionResponseKey.LEFT, EmotionResponseKey.LEFT, EmotionResponseKey.LEFT,
        )
        assertEquals(3, EmotionGamingGuard.longestSameKeyStreak(keys))
        assertEquals(0, EmotionGamingGuard.longestSameKeyStreak(emptyList()))
    }

    @Test
    fun honest_block_is_not_flagged() {
        val keys = listOf(EmotionResponseKey.LEFT, EmotionResponseKey.RIGHT)
        val honest = (0 until 10).map { trial(if (it % 3 == 0) EmotionTrialOutcome.INCORRECT else EmotionTrialOutcome.CORRECT, 550, keys[it % 2]) }
        assertFalse(EmotionGamingGuard.looksGamed(honest, chanceLevel = 0.5, checkSameKey = true))
    }

    @Test
    fun random_fast_guessing_is_flagged() {
        val fast = List(5) { trial(EmotionTrialOutcome.CORRECT, 180, EmotionResponseKey.LEFT) } +
            List(5) { trial(EmotionTrialOutcome.INCORRECT, 180, EmotionResponseKey.RIGHT) }
        assertTrue(EmotionGamingGuard.looksGamed(fast, chanceLevel = 0.5, checkSameKey = true))
    }
}
