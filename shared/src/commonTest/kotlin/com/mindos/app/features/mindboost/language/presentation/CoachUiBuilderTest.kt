package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.repair.DefaultRepairEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the Coach Card / Session Feedback projection (§11.5): repair-aware cards, honest evidence
 * lines, factual (never overstated) result lines, and aggregate feedback with null-not-zero accuracy.
 */
class CoachUiBuilderTest {

    private val repairEngine = DefaultRepairEngine()

    private fun summary(items: Int = 4, correct: Int = 3, strong: Int = 2) = SessionSummary(
        sessionId = LanguageSessionId("s1"),
        userIdHash = UserIdHash("u1"),
        startedAtEpochMs = 1L,
        completedAtEpochMs = 2L,
        itemsAttempted = items,
        correctCount = correct,
        perTraitCorrect = mapOf(TraitCode.INF to correct),
        perTraitAttempts = mapOf(TraitCode.INF to items),
        strongUpdateCount = strong,
    )

    @Test
    fun coachCardWithRepairNamesPatternAndRetry() {
        val plan = repairEngine.planForError(ErrorType.INF_OVER)
        val card = CoachUiBuilder.coachCard(TraitCode.INF, summary(), EvidenceState.E1_TRAINED_IMPROVEMENT, plan)
        assertEquals(TraitCode.INF.display, card.trainedSkill)
        assertEquals(plan.explanation, card.detectedPattern)
        assertEquals(plan.microLesson, card.repairApplied)
        assertEquals(plan.isomorphicRetry, card.nextStep)
    }

    @Test
    fun coachCardWithoutRepairUsesNextEvidenceAsNextStep() {
        val card = CoachUiBuilder.coachCard(TraitCode.INF, summary(), EvidenceState.E2_NEAR_TRANSFER)
        assertNull(card.detectedPattern)
        assertNull(card.repairApplied)
        assertTrue(card.nextStep.contains("tarea funcional"))
    }

    @Test
    fun evidenceLineFlagsMissingTransferBelowNearTransfer() {
        val below = CoachUiBuilder.coachCard(TraitCode.INF, summary(), EvidenceState.E1_TRAINED_IMPROVEMENT)
        assertTrue(below.evidenceLine.contains("aún no hay transferencia"))
        val atNear = CoachUiBuilder.coachCard(TraitCode.INF, summary(), EvidenceState.E2_NEAR_TRANSFER)
        assertTrue(!atNear.evidenceLine.contains("aún no hay transferencia"))
    }

    @Test
    fun resultLineIsFactualAndReflectsAccuracy() {
        val good = CoachUiBuilder.coachCard(
            TraitCode.INF, summary(items = 4, correct = 4), EvidenceState.E1_TRAINED_IMPROVEMENT,
        )
        assertTrue(good.result.contains("Buen desempeño"))
        val weak = CoachUiBuilder.coachCard(
            TraitCode.INF, summary(items = 4, correct = 1), EvidenceState.E1_TRAINED_IMPROVEMENT,
        )
        assertTrue(weak.result.contains("margen de mejora"))
    }

    @Test
    fun sessionFeedbackComputesAccuracyAndPerTraitLines() {
        val feedback = CoachUiBuilder.sessionFeedback(summary(items = 4, correct = 3, strong = 2))
        assertEquals(75, feedback.accuracyPercent)
        assertEquals(4, feedback.itemsAttempted)
        assertEquals(2, feedback.strongUpdates)
        assertTrue(feedback.perTraitLines.any { it.contains("${TraitCode.INF.display}: 3/4") })
    }

    @Test
    fun accuracyIsNullWhenNothingAttempted() {
        val feedback = CoachUiBuilder.sessionFeedback(summary(items = 0, correct = 0, strong = 0))
        assertNull(feedback.accuracyPercent)
        assertEquals("Entrenaste 0 ítems.", feedback.headline)
    }
}
