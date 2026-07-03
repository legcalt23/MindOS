@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.decisiones.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.decisiones.presentation.state.DecisionResultUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.PolygonAxis
import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionRound
import com.mindverse.mindboost.core.session.DecisionSessionOrchestrator
import com.mindverse.mindboost.core.session.DecisionSessionResult

/**
 * Assembles a full Decisiones session — the integration point where the check-in and every exercise
 * round are folded into one [DecisionSession] via [DecisionSessionOrchestrator.assembleSession] — and
 * maps it to the result UI: a five-axis cognitive polygon plus the coach message. Pure and
 * unit-testable; the diary capture and the Compose screen are layered on in later slices.
 */
class DecisionResultStateHolder(
    private val sessionId: String,
    private val timestampMs: Long,
    checkIn: CheckInInput,
    rounds: List<DecisionRound>,
    orchestrator: DecisionSessionOrchestrator = DecisionSessionOrchestrator(),
) {
    /** The assembled result: session metrics, readiness, per-round outcomes and coach message. */
    val result: DecisionSessionResult = orchestrator.assembleSession(
        sessionId = sessionId,
        timestampMs = timestampMs,
        checkIn = checkIn,
        rounds = rounds,
    )

    /** The persisted shape of this session (handed to the repository by a later wiring slice). */
    val session: DecisionSession get() = result.session

    var uiState: DecisionResultUiState by mutableStateOf(buildState())
        private set

    /** The user's free-text reflection. Capped and frozen once the session is sealed. */
    var diaryText: String by mutableStateOf("")
        private set

    /** Once true, the session is committed and the diary can no longer be edited. */
    var sealed: Boolean by mutableStateOf(false)
        private set

    /** Updates the diary draft (no-op after sealing); content is capped at [MAX_DIARY_CHARS]. */
    fun onDiaryTextChange(text: String) {
        if (sealed) return
        diaryText = text.take(MAX_DIARY_CHARS)
    }

    /** Seals the session and returns the diary entry to persist. Idempotent. */
    fun seal(): DiaryEntry {
        sealed = true
        return buildDiaryEntry()
    }

    /**
     * Builds the [DiaryEntry] for this session. The content is stored as plaintext bytes for now —
     * at-rest encryption is Track A — and [DiaryEntry.safetyFlag] stays false until a dedicated,
     * non-diagnostic safety screen exists; it is not fabricated here.
     */
    fun buildDiaryEntry(): DiaryEntry = DiaryEntry(
        id = sessionId,
        timestampMs = timestampMs,
        encryptedContent = diaryText.encodeToByteArray(),
        safetyFlag = false,
    )

    private fun buildState(): DecisionResultUiState {
        val session = result.session
        val integrity = ((1.0 - session.gamingScore) * MAX_AXIS).coerceIn(0.0, MAX_AXIS)
        val freshness = ((1.0 - session.fatigueScore) * MAX_AXIS).coerceIn(0.0, MAX_AXIS)
        return DecisionResultUiState(
            axes = listOf(
                PolygonAxis(label = "Calidad", value = session.icd.coerceIn(0.0, MAX_AXIS)),
                PolygonAxis(label = "Efectividad", value = session.ied.coerceIn(0.0, MAX_AXIS)),
                PolygonAxis(label = "Preparación", value = result.readiness.criValue.coerceIn(0.0, MAX_AXIS)),
                PolygonAxis(label = "Integridad", value = integrity),
                PolygonAxis(label = "Frescura", value = freshness),
            ),
            complexity = session.cde.coerceIn(0.0, MAX_AXIS),
            coachMessage = result.coachMessage,
        )
    }

    private companion object {
        const val MAX_AXIS = 100.0
        const val MAX_DIARY_CHARS = 2_000
    }
}
