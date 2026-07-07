@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.coach.prompt

import com.mindos.app.features.mindboost.coach.domain.CoachCalibrationState
import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.coach.domain.CoachContribution
import com.mindos.app.features.mindboost.coach.domain.CoachSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u-F — the prompt is structured, carries fixed disclaimers + schema, marks low-confidence, is size-capped, and never carries raw/PII. */
class AICoachPromptBuilderTest {

    private fun summary(confidence: CoachConfidenceLevel) =
        CoachSummary(listOf(CoachContribution("EMOCIONES", CoachCalibrationState.ESTABLISHED, confidence, patterns = listOf("atencion"))), confidence)

    @Test
    fun prompt_includes_fixed_policy_disclaimers_and_schema() {
        val p = AICoachPromptBuilder.build(summary(CoachConfidenceLevel.HIGH), "hola", null, emptyList())
        assertTrue(p.text.contains("NO diagnostica"), "must state MindBoost does not diagnose")
        assertTrue(p.text.contains("SCHEMA_VERSION"))
        assertEquals(1, p.schemaVersion)
    }

    @Test
    fun prompt_marks_low_confidence_data() {
        assertTrue(AICoachPromptBuilder.build(summary(CoachConfidenceLevel.LOW), "x", null, emptyList()).text.contains("LOW_CONFIDENCE"))
    }

    @Test
    fun prompt_states_allowed_and_forbidden_claims() {
        val t = AICoachPromptBuilder.build(summary(CoachConfidenceLevel.HIGH), "x", null, emptyList()).text
        assertTrue(t.contains("ALLOWED_CLAIMS"))
        assertTrue(t.contains("FORBIDDEN_CLAIMS"))
    }

    @Test
    fun prompt_is_capped_at_max_chars() {
        val huge = CoachSummary(List(400) { CoachContribution("B$it", CoachCalibrationState.CALIBRATING, CoachConfidenceLevel.LOW, patterns = listOf("dato de relleno")) }, CoachConfidenceLevel.LOW)
        assertTrue(AICoachPromptBuilder.build(huge, "x", null, emptyList()).text.length <= AICoachPromptBuilder.MAX_PROMPT_CHARS)
    }

    @Test
    fun prompt_contains_no_raw_trial_or_pii_markers() {
        val t = AICoachPromptBuilder.build(summary(CoachConfidenceLevel.HIGH), "hola coach", null, emptyList()).text
        listOf("rtNanos", "latencyMs", "selfReport", "valence", "arousal", "@", "userId").forEach {
            assertFalse(t.contains(it, ignoreCase = true), "prompt must not carry raw/PII marker '$it'")
        }
    }
}
