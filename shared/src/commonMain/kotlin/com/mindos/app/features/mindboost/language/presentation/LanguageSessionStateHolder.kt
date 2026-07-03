// LanguageUiState is a flat aggregate of view fields; its breadth is the data model.
@file:Suppress("LongParameterList", "TooManyFunctions")

package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.claim.ClaimEngine
import com.mindos.app.features.mindboost.language.domain.claim.ClaimRequest
import com.mindos.app.features.mindboost.language.domain.claim.ClaimType
import com.mindos.app.features.mindboost.language.domain.claim.DefaultClaimEngine
import com.mindos.app.features.mindboost.language.domain.evidence.DefaultEvidenceEngine
import com.mindos.app.features.mindboost.language.domain.evidence.EvidenceEngine
import com.mindos.app.features.mindboost.language.domain.evidence.EvidenceInput
import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.projection.ProfileProjector
import com.mindos.app.features.mindboost.language.domain.repair.DefaultRepairEngine
import com.mindos.app.features.mindboost.language.domain.repair.RepairEngine
import com.mindos.app.features.mindboost.language.domain.scoring.ClosedScoringEngine
import com.mindos.app.features.mindboost.language.domain.scoring.ScoredResponse
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringContext
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringEngine
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringTiming
import com.mindos.app.features.mindboost.language.presentation.exercises.LanguageExercise
import com.mindos.app.features.mindboost.language.presentation.exercises.LanguageExerciseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the flow is: choosing an exercise, answering, reading feedback, or finished. */
enum class LanguageStage { MENU, QUESTION, FEEDBACK, DONE }

/** Flat view state the Compose screen renders. */
data class LanguageUiState(
    val stage: LanguageStage,
    val itemNumber: Int,
    val totalItems: Int,
    val item: DemoItem?,
    val lastAnswerCorrect: Boolean?,
    val feedback: String,
    val correctCount: Int,
    val accuracyPercent: Int?,
    val skillLines: List<String>,
    val summaryNote: String,
    /** Title of the running exercise (empty in the menu and in the legacy single-bank flow). */
    val exerciseTitle: String = "",
)

/**
 * Provisional state holder for the Lenguaje demo. Synchronous and platform-free: it drives the REAL
 * engines (scoring → evidence → projection → claim → repair) so the screen shows a faithful, honest
 * cognitive loop, not a mock. Timestamps are a monotone counter (ordering only) to avoid any clock
 * dependency. Fully unit-testable without Compose.
 */
class LanguageSessionStateHolder(
    userId: String,
    items: List<DemoItem> = LanguageDemoBank.items,
    /** Exercises offered when this holder runs in menu mode (mixed session first, then each exercise). */
    val menu: List<LanguageExercise> = LanguageExerciseCatalog.menu,
    /** When true the holder starts at [LanguageStage.MENU] and waits for [start]; legacy default is false. */
    private val menuMode: Boolean = false,
    private val scoring: ScoringEngine = ClosedScoringEngine(),
    private val evidence: EvidenceEngine = DefaultEvidenceEngine(),
    private val claims: ClaimEngine = DefaultClaimEngine(),
    private val repair: RepairEngine = DefaultRepairEngine(),
) {
    private val userIdHash = UserIdHash(userId.ifBlank { "anon" })
    private val packets = mutableListOf<EvidencePacket>()
    private var currentItems: List<DemoItem> = items
    private var currentExerciseTitle: String = ""
    private var index = 0
    private var correctCount = 0
    private var tick = 0L

    private val _state = MutableStateFlow(if (menuMode) menuState() else questionState())
    val state: StateFlow<LanguageUiState> = _state.asStateFlow()

    /** Load a chosen exercise's items and start it from the first question (menu → exercise). */
    fun start(exercise: LanguageExercise) {
        currentItems = exercise.items
        currentExerciseTitle = exercise.title
        resetCounters()
        _state.value = questionState()
    }

    /** Return to the exercise menu, discarding the current run's in-progress evidence (menu mode only). */
    fun backToMenu() {
        if (!menuMode) return
        currentItems = emptyList()
        currentExerciseTitle = ""
        resetCounters()
        _state.value = menuState()
    }

    /** Submit any closed response for the current item. Scores it, accumulates evidence, shows feedback. */
    fun submit(response: ResponsePayload) {
        if (_state.value.stage != LanguageStage.QUESTION) return
        val item = currentItems[index]
        val scored = scoring.score(item.key, response, ScoringTiming(), ScoringContext(bankType = item.bankType))
        evidence.evaluate(evidenceInput(item, scored)).packet?.let { packets += it }
        val correct = scored.correctness.value >= 1.0
        if (correct) correctCount++
        _state.value = _state.value.copy(
            stage = LanguageStage.FEEDBACK,
            lastAnswerCorrect = correct,
            feedback = feedbackFor(correct, scored),
            correctCount = correctCount,
        )
    }

    /** Convenience for single-choice items. */
    fun answer(optionId: String) = submit(ResponsePayload.MultipleChoice(optionId))

    /** Advance to the next item, or finish the session. */
    fun next() {
        if (_state.value.stage != LanguageStage.FEEDBACK) return
        if (index + 1 >= currentItems.size) finish() else { index++; _state.value = questionState() }
    }

    /** Restart the current exercise from its first question. */
    fun restart() {
        resetCounters()
        _state.value = questionState()
    }

    private fun resetCounters() {
        packets.clear()
        index = 0
        correctCount = 0
        tick = 0
    }

    private fun finish() {
        val profile = ProfileProjector.project(userIdHash, packets, nextTick())
        val cockpit = GenomeUiBuilder.evidenceCockpit(profile)
        val strongest = profile.traitEstimates.values.maxByOrNull { it.evidenceState.rank }
        val note = if (strongest == null) {
            "Aún no hay evidencia suficiente; sigue practicando."
        } else {
            val claimType = ClaimType.strongestFor(strongest.evidenceState) ?: ClaimType.TRAINED_IMPROVEMENT
            ProgressUiBuilder.claimSafeProgress(
                ClaimRequest(claimType, strongest.evidenceState, strongest.trait),
                claims,
            ).headline
        }
        _state.value = _state.value.copy(
            stage = LanguageStage.DONE,
            item = null,
            accuracyPercent = accuracyPercent(),
            skillLines = cockpit.items.map { "${it.displayName}: ${it.stateLabel}" },
            summaryNote = note,
        )
    }

    private fun accuracyPercent(): Int? =
        if (currentItems.isEmpty()) {
            null
        } else {
            Proportion.clamped(correctCount.toDouble() / currentItems.size).percent
        }

    private fun questionState(): LanguageUiState {
        val item = currentItems.getOrNull(index)
        return LanguageUiState(
            stage = if (item == null) LanguageStage.DONE else LanguageStage.QUESTION,
            itemNumber = index + 1,
            totalItems = currentItems.size,
            item = item,
            lastAnswerCorrect = null,
            feedback = "",
            correctCount = correctCount,
            accuracyPercent = null,
            skillLines = emptyList(),
            summaryNote = "",
            exerciseTitle = currentExerciseTitle,
        )
    }

    private fun menuState(): LanguageUiState = LanguageUiState(
        stage = LanguageStage.MENU,
        itemNumber = 0,
        totalItems = 0,
        item = null,
        lastAnswerCorrect = null,
        feedback = "",
        correctCount = 0,
        accuracyPercent = null,
        skillLines = emptyList(),
        summaryNote = "",
        exerciseTitle = "",
    )

    private fun evidenceInput(item: DemoItem, scored: ScoredResponse): EvidenceInput = EvidenceInput(
        packetId = "pk-${packets.size}",
        userIdHash = userIdHash,
        sourceAttemptId = AttemptId("att-${packets.size}-${item.taskId.value}"),
        trait = item.primaryTrait,
        bankType = item.bankType,
        operation = item.operation,
        itemInformation = 1.0,
        transferDistance = 0.0,
        createdAtEpochMs = nextTick(),
        isStrongUpdateAllowed = scored.isStrongUpdateAllowed,
        evidenceWeight = scored.evidenceWeight,
        gatesApplied = scored.gatesApplied,
    )

    private fun feedbackFor(correct: Boolean, scored: ScoredResponse): String = when {
        correct -> "Correcto. Tu respuesta está soportada por el texto."
        scored.errorTypes.isNotEmpty() -> "Revisa: ${repair.planForError(scored.errorTypes.first()).microLesson}"
        else -> "Aún no es exacto; revisa tu respuesta."
    }

    private fun nextTick(): Long = ++tick
}
