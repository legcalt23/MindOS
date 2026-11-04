@file:Suppress("MaxLineLength") // User-facing labels / builder expressions read best on one line.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionAggregateFactory
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackCopy
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier

/**
 * Minimal, PII-free history/progress model for the emotions menu (MC8u-B §4). Everything is session-scoped and
 * NON person-level: which exercise ran last, whether that last session was VALID (counted) or PRACTICE, the trained
 * capacity, the calibration state label, progress toward the valid sessions an official reading needs, the next
 * suggested practice, and a protective note if the last session was safety-stopped. It NEVER shows an official rank
 * before enough valid sessions and NEVER makes a claim about the person.
 */
data class EmotionHistoryUiModel(
    val isEmpty: Boolean,
    val lastExerciseName: String?,
    val lastSessionWasValid: Boolean,
    val lastSessionLabel: String,
    val capacityTrained: String?,
    val stateLabel: String,
    val validSessions: Int,
    val validSessionsProgress: String,
    val hasOfficialRank: Boolean,
    val nextSuggestion: String?,
    val safetyNote: String?,
    /** Ola 1 · N1 — the transversal confidence readout. Null only on the empty state (nothing to report yet). */
    val calibration: EmotionCalibrationUiModel? = null,
)

/** Pure builder for [EmotionHistoryUiModel] — deterministic from persisted summaries + the derived aggregate. */
object EmotionHistoryCardBuilder {

    fun empty(): EmotionHistoryUiModel = EmotionHistoryUiModel(
        isEmpty = true,
        lastExerciseName = null,
        lastSessionWasValid = false,
        lastSessionLabel = EmotionStrings.HISTORY_EMPTY,
        capacityTrained = null,
        stateLabel = EmotionStrings.tierLabel(ProfileTier.EXPLORATORY),
        validSessions = 0,
        validSessionsProgress = EmotionStrings.validSessionsProgress(0),
        hasOfficialRank = false,
        nextSuggestion = null,
        safetyNote = null,
    )

    fun build(summaries: List<EmotionSessionSummaryDto>, aggregate: EmotionAggregateDto?): EmotionHistoryUiModel {
        val last = summaries.maxByOrNull { it.startedAtMillis } ?: return empty()
        val definition = EmotionExerciseCatalog.all.values.firstOrNull { it.id.name == last.exerciseId }
        val validCount = aggregate?.sessionsCompleted
            ?: summaries.count { EmotionTerminalSemantics.isValidForAggregate(it) }
        val lastValid = EmotionTerminalSemantics.isValidForAggregate(last)
        val stoppedForSafety = last.endReason == EmotionTerminalSemantics.SAFETY_STOP
        return EmotionHistoryUiModel(
            isEmpty = false,
            lastExerciseName = definition?.displayName,
            lastSessionWasValid = lastValid,
            lastSessionLabel = if (lastValid) EmotionStrings.HISTORY_VALID_LABEL else EmotionStrings.HISTORY_PRACTICE_LABEL,
            capacityTrained = definition?.let { EmotionFeedbackCopy.focusOf(it.capacity) },
            stateLabel = EmotionStrings.tierLabel(EmotionAggregateFactory.tierFor(validCount)),
            validSessions = validCount,
            validSessionsProgress = EmotionStrings.validSessionsProgress(validCount),
            hasOfficialRank = validCount >= EmotionStrings.VALID_SESSION_TARGET,
            nextSuggestion = definition?.displayName,
            safetyNote = if (stoppedForSafety) EmotionStrings.DESREGULATION_CLOSE else null,
            calibration = EmotionCalibrationCardBuilder.build(
                EmotionAggregateFactory.calibrationOf(summaries),
                EmotionAggregateFactory.blockForecastsOf(summaries),
            ),
        )
    }
}
