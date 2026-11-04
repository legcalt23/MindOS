package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.core.lifecycle.ForegroundLoss
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.core.lifecycle.NotObservableReason
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E1VigiaGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricAbsence
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.isResponse
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** FOC-P0-05 · `FocusSessionAssembler.admit` at the assembler level: what a denied session may and may not keep. */
class FocusSessionAdmissionTest {

    private val assembler = FocusSessionAssembler(DefaultMetricsEngine())
    private val e1 = ExerciseCatalog.E1_VIGIA
    private val watched = LifecycleObservation.Observed(emptyList())
    private val interrupted = LifecycleObservation.Observed(listOf(ForegroundLoss(1_000L, 2_000L)))
    private val unwatched = LifecycleObservation.NotObservable(NotObservableReason.NO_PLATFORM_SOURCE)

    private fun block(id: String, seed: Long, respond: Boolean): BlockResult {
        val spec = BlockSpec(targetTrialCount = TRIALS, mode = Mode.TRAINING)
        val recorded = E1VigiaGenerator().generate(DifficultyVector.EASIEST, spec, seed).map {
            FocusSessionAssembler.RecordedTrial(it, if (respond && it.isTarget) RT_MS else null)
        }
        return assembler.assembleBlock(
            blockId = BlockId(id),
            definition = e1,
            mode = Mode.TRAINING,
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            thresholds = IndividualThresholds(),
            recorded = recorded,
            sessionIdPrefix = "user",
            epochMillis = EPOCH_MS,
        )
    }

    /**
     * R15 · a blind block: it answers a couple of NON-targets, so the session HAS responses (the
     * `NO_RESPONSES` guard is not what stops it) while nothing in it separates targets from non-targets.
     */
    private fun blindBlock(id: String, seed: Long): BlockResult {
        val spec = BlockSpec(targetTrialCount = TRIALS, mode = Mode.TRAINING)
        var answered = 0
        val recorded = E1VigiaGenerator().generate(DifficultyVector.EASIEST, spec, seed).map {
            val respond = !it.isTarget && answered < BLIND_COMMISSIONS
            if (respond) answered++
            FocusSessionAssembler.RecordedTrial(it, if (respond) RT_MS else null)
        }
        return assembler.assembleBlock(
            blockId = BlockId(id),
            definition = e1,
            mode = Mode.TRAINING,
            difficultyStart = DifficultyVector.EASIEST,
            difficultyEnd = DifficultyVector.EASIEST,
            thresholds = IndividualThresholds(),
            recorded = recorded,
            sessionIdPrefix = "user",
            epochMillis = EPOCH_MS,
        )
    }

    private fun session(vararg blocks: BlockResult): SessionResult = assembler.assembleSession(
        sessionId = SessionId("user:$EPOCH_MS"),
        mode = Mode.TRAINING,
        definition = e1,
        blocks = blocks.toList(),
        startedAtMs = 0,
        endedAtMs = EPOCH_MS,
        subjectiveState = null,
    )

    private fun SessionResult.allMetrics(): List<MetricValue> =
        blocks.flatMap { it.metrics.values } + sessionMetrics.values

    @Test
    fun an_honest_session_is_admitted_and_its_metrics_are_byte_identical() {
        val before = session(block("b0", seed = 11, respond = true))

        val after = assembler.admit(before, watched)

        assertEquals(SessionAdmission.Admitted, after.admission)
        assertEquals(watched, after.lifecycle)
        assertEquals(before.sessionMetrics, after.sessionMetrics)
        assertEquals(before.blocks.map { it.metrics }, after.blocks.map { it.metrics })
        assertTrue(after.allMetrics().any { it.confident }, "discriminating half: admitted keeps confident metrics")
    }

    @Test
    fun an_unwatched_session_withdraws_only_what_was_still_confident() {
        // One answered block and one nobody answered: the fixture must exercise BOTH halves of the rule.
        val before = session(block("b0", seed = 11, respond = true), block("b1", seed = 12, respond = false))

        val after = assembler.admit(before, unwatched)

        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.LIFECYCLE_NOT_OBSERVED), after.admission)
        val pairs = before.allMetrics().zip(after.allMetrics())
        assertTrue(pairs.any { (b, _) -> b.confident }, "anti-vacuity: some metric was confident before")
        assertTrue(pairs.any { (b, _) -> !b.confident }, "anti-vacuity: some metric was already absent before")
        pairs.forEach { (b, a) ->
            if (b.confident) {
                assertEquals(MetricAbsence.NOT_ADMITTED, a.absence, "${b.key} claim withdrawn")
                assertEquals(b.n, a.n, "${b.key} sample size kept")
                assertFalse(a.confident)
            } else {
                assertEquals(b, a, "${b.key}: an already-absent metric keeps its own, more specific reason")
            }
        }
        assertEquals(MetricAbsence.NO_RESPONSES, after.blocks[1].metrics.getValue(MetricKey.ACCURACY).absence)
    }

    @Test
    fun a_session_nobody_answered_is_not_measurable_and_keeps_no_confident_metric() {
        val after = assembler.admit(session(block("b0", seed = 11, respond = false)), watched)

        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES), after.admission)
        assertEquals(emptyList(), after.allMetrics().filter { it.confident }.map { it.key })
    }

    /**
     * R15 · the LIVE call-site of FocusAntiGaming. A detector counts only if the gate it claims to protect
     * actually runs it, and this is where that is pinned: the session is watched and it HAS responses, so the
     * only thing that can stop it is the response pattern itself.
     */
    @Test
    fun a_session_whose_response_pattern_carries_no_signal_is_withheld_rather_than_scored() {
        val before = session(blindBlock("b0", seed = 11))
        assertTrue(before.blocks.single().trials.any { it.outcome.isResponse }, "anti-vacuity: it did respond")

        val after = assembler.admit(before, watched)

        assertEquals(
            SessionAdmission.NotMeasurable(NotMeasurableReason.RESPONSE_PATTERN_NOT_INTERPRETABLE),
            after.admission,
        )
        assertEquals(emptyList(), after.allMetrics().filter { it.confident }.map { it.key })
        // The discriminating half: the same lane admits an honest session, so this gate is not denying everyone.
        val honest = assembler.admit(session(block("b0", seed = 11, respond = true)), watched)
        assertEquals(SessionAdmission.Admitted, honest.admission)
    }

    @Test
    fun an_observed_interruption_turns_every_metric_into_interrupted() {
        val after = assembler.admit(session(block("b0", seed = 11, respond = true)), interrupted)

        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED), after.admission)
        assertTrue(after.allMetrics().isNotEmpty())
        after.allMetrics().forEach { assertEquals(MetricAbsence.INTERRUPTED, it.absence, it.key.name) }
    }

    @Test
    fun the_honest_note_names_the_reason_and_the_admitted_note_is_unchanged() {
        val admitted = assembler.admit(session(block("b0", seed = 11, respond = true)), watched)
        assertEquals(
            "Esto describe tu práctica de hoy en este ejercicio — influida por sueño, hora y " +
                "ánimo. Por ahora se registra el entrenamiento; no se concluye nada sobre ti.",
            assembler.buildReport(admitted).honestNote,
        )

        val byLifecycle = mapOf(
            interrupted to "la app salió de primer plano durante el ejercicio.",
            unwatched to "no pudimos comprobar que la app siguiera en primer plano.",
        )
        byLifecycle.forEach { (lifecycle, clause) ->
            val denied = assembler.admit(session(block("b0", seed = 11, respond = true)), lifecycle)
            val note = assembler.buildReport(denied).honestNote
            assertTrue(note.startsWith("$PREFIX$clause"), note)
            assertFalse("demasiado corta" in note, "a denied session is not called short: $note")
        }
        val unanswered = assembler.admit(session(block("b0", seed = 11, respond = false)), watched)
        val note = assembler.buildReport(unanswered).honestNote
        assertTrue(note.startsWith("${PREFIX}no hubo ninguna respuesta."), note)
    }

    private companion object {
        const val TRIALS = 40
        const val BLIND_COMMISSIONS = 2
        const val RT_MS = 300L
        const val EPOCH_MS = 5_000L
        const val PREFIX = "Esta sesión no cuenta para tu progreso: "
    }
}
