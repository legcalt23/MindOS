package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * R10 · `MB-CATALOG-0017` — whether a session can support a MEASUREMENT claim is a function of
 * `(exercise, block size, difficulty)`, and until now no symbol in the repo related the three.
 *
 * The adaptive engine moves each exercise's `governingAxis`, and for all six governed exercises that axis is
 * exactly the one driving the condition proportion — so the engine, doing the only thing it knows how to do,
 * resizes the cells of the comparison. Since R9 made the split a quota, the consequence is deterministic and
 * can therefore be DECLARED. This file is the declaration's gate.
 *
 * See `docs/audits/ADR_MEASUREMENT_REACHABILITY_2026-08-20.md`.
 */
class MeasurementReachabilityTest {

    /**
     * How to read the drawn condition off a stimulus. Test-only on purpose: production reads cells off
     * `TrialResult.markers`, never off a `Stimulus`, so a copy of this in `MeasurementReachability` would be a
     * symbol with no production reader. Here it is the verification INSTRUMENT — what makes
     * [theDeclaredCellMatchesWhatTheRealGeneratorProduces] a real check instead of a tautology.
     */
    private val drawnFlag: Map<ExerciseId, (Stimulus) -> Boolean> = mapOf(
        ExerciseId.E2_FILTRO to { s -> (s.payload as StimulusPayload.Flanker).congruent.not() },
        ExerciseId.E4_FRENO to { s -> (s.payload as StimulusPayload.StopSignal).isStop },
        ExerciseId.E5_CONFLICTO to { s -> (s.payload as StimulusPayload.Conflict).congruent.not() },
        ExerciseId.E6_CAMBIO to { s -> (s.payload as StimulusPayload.TaskSwitch).isSwitch },
        ExerciseId.E8_DUO to { s -> (s.payload as StimulusPayload.DualTask).dual },
        ExerciseId.E13_PRESION to { s -> (s.payload as StimulusPayload.Pressure).underPressure },
    )

    /**
     * The block sizes production can actually mint, DERIVED rather than transcribed: the four training templates
     * plus every fixed baseline block. Change a template's `coreMs` or a baseline block and this set moves, which
     * is precisely what must not happen silently.
     */
    private val productionBlockSizes: List<Int> = (
        SessionTemplate.entries.map { it.estimatedTrialCount() } +
            BaselineProtocols.INITIAL.blocks.map { it.targetTrialCount }
        ).distinct().sorted()

    // ── The forcing function ────────────────────────────────────────────────────────────────────────

    /**
     * The set of exercises whose primary metric has its own central floor is DERIVED from
     * `MeasurementSufficiency.central`. Adding a comparative metric to the catalogue without declaring how its
     * cell is drawn — or declaring it a behavioural cell — fails here.
     */
    @Test
    fun theGovernedSetIsDerivedFromTheCentralTableNotEnumerated() {
        val owing = ExerciseCatalog.all.values
            .filter { definition ->
                definition.primaryMetrics.any { key ->
                    val central = MeasurementSufficiency.centralMetricFor(key)
                    central != null && central != ScoreMetricId.ACCURACY_RT_BASE
                }
            }
            .map { it.id }
            .toSet()

        assertTrue(owing.isNotEmpty(), "anti-vacuity: the derivation found no governed exercise at all")
        assertEquals(
            owing,
            MeasurementReachability.declaredExercises() + MeasurementReachability.behaviouralCells,
            "an exercise whose primary metric has its own central floor must either declare how its cell is " +
                "drawn or be declared a behavioural cell; this one does neither",
        )
    }

    // ── The ADR's abort criterion, executable ───────────────────────────────────────────────────────

    /**
     * **The abort criterion of the ADR.** The authority must reproduce EXACTLY the cell the real generator
     * produces, for every triple production can reach. One divergence and the authority is a second source of
     * truth, which is the defect this plan has spent three phases closing.
     *
     * Not a tautology: the quota arithmetic is shared on purpose (one definition of `round(n·p)`), but the
     * WIRING — which mapping, which slot count, which payload flag — is what this compares against reality, and
     * a generator is perfectly free to ignore its own mapping.
     */
    @Test
    fun theDeclaredCellMatchesWhatTheRealGeneratorProduces() {
        var comparisons = 0
        for (exerciseId in MeasurementReachability.declaredExercises()) {
            val flag = assertNotNull(drawnFlag[exerciseId], "no verification instrument for $exerciseId")
            val axis = assertNotNull(MeasurementReachability.axisFor(exerciseId))
            for (size in productionBlockSizes) {
                for (step in 0..AXIS_STEPS) {
                    val level = step.toDouble() / AXIS_STEPS
                    val difficulty = DifficultyVector.EASIEST.adjust(axis, AxisLevel(level))
                    val declared = assertNotNull(MeasurementReachability.verdictFor(exerciseId, size, difficulty))
                    val real = generate(exerciseId, difficulty, size).count(flag)
                    val realSmallest =
                        if (declared.comparesTwoCells) minOf(real, declared.slots - real) else real
                    assertEquals(
                        realSmallest,
                        declared.smallestCell,
                        "$exerciseId at n=$size, $axis=$level: the authority declares " +
                            "${declared.smallestCell} but the real generator produced $realSmallest",
                    )
                    comparisons++
                }
            }
        }
        assertTrue(comparisons > 0, "anti-vacuity: the grid compared nothing")
    }

    // ── The declared surface ────────────────────────────────────────────────────────────────────────

    /**
     * The surface of ADR §3, pinned. A proportion range, a `coreMs`, a baseline block size or a central floor
     * cannot move without landing here first.
     */
    @Test
    fun theReachabilitySurfaceIsPinnedAcrossEveryProductionBlockSize() {
        assertEquals(
            listOf(55, 60, 75, 94, 157, 160),
            productionBlockSizes,
            "the set of block sizes production can mint changed; recompute the ADR surface before editing this",
        )
        assertEquals(
            mapOf(
                55 to "E13:always E2:>=0.39 E4:never E5:0.27-0.98 E6:>=0.21 E8:<=0.70",
                60 to "E13:always E2:>=0.32 E4:never E5:>=0.19 E6:>=0.11 E8:<=0.78",
                75 to "E13:always E2:>=0.15 E4:never E5:>=0.03 E6:always E8:<=0.97",
                94 to "E13:always E2:>=0.02 E4:>=0.80 E5:always E6:always E8:always",
                157 to "E13:always E2:always E4:always E5:always E6:always E8:always",
                160 to "E13:always E2:always E4:always E5:always E6:always E8:always",
            ),
            productionBlockSizes.associateWith { size -> surfaceRow(size) },
            "the measurement-reachability surface moved",
        )
    }

    /**
     * **H1 — the dominant variable is not difficulty, it is the block size**, i.e. how many minutes the user
     * chose. At the longest templates every governed claim is reachable across the whole axis; at the shortest
     * most are not. The user picks that with no indication it has this consequence (`MB-CATALOG-0018`).
     */
    @Test
    fun blockSizeDominatesDifficulty() {
        val longest = SessionTemplate.DEVELOPMENT_20.estimatedTrialCount()
        val shortest = SessionTemplate.MAINTENANCE_5.estimatedTrialCount()
        assertTrue(shortest < longest, "anti-vacuity: the templates must actually differ in size")

        assertTrue(
            MeasurementReachability.declaredExercises().all { id -> alwaysReachable(id, longest) },
            "at n=$longest every governed claim used to be reachable across its whole axis; that changed",
        )
        assertTrue(
            MeasurementReachability.declaredExercises().any { id -> !alwaysReachable(id, shortest) },
            "at n=$shortest some governed claim used to be unreachable somewhere on its axis; that changed",
        )
    }

    /**
     * **H2 — non-monotone.** At the shortest block E5 denies at BOTH ends of the interference axis: at maximum
     * interference the CONGRUENT cell falls to 19, one trial below the floor. No verbal reasoning produces this;
     * only the arithmetic does. It is the sharpest possible demonstration that "harder" and "measurable" are
     * unrelated.
     */
    @Test
    fun e5IsNonMonotoneAtTheShortestBlock() {
        val n = SessionTemplate.MAINTENANCE_5.estimatedTrialCount()
        val axis = assertNotNull(MeasurementReachability.axisFor(ExerciseId.E5_CONFLICTO))
        fun at(level: Double) = assertNotNull(
            MeasurementReachability.verdictFor(
                ExerciseId.E5_CONFLICTO,
                n,
                DifficultyVector.EASIEST.adjust(axis, AxisLevel(level)),
            ),
        )

        assertTrue(!at(0.0).reachable, "E5 at minimum interference should be unreachable at n=$n")
        assertTrue(at(0.5).reachable, "E5 in the middle of the axis should be reachable at n=$n")
        assertTrue(!at(1.0).reachable, "E5 at maximum interference should be unreachable at n=$n")
        assertEquals(
            19,
            at(1.0).smallestCell,
            "the congruent cell at maximum interference is what makes this non-monotone; if it moved, the " +
                "ADR's H2 is stale",
        )
    }

    /**
     * **H3 — `MB-CATALOG-0016` refined.** That finding said SSRT is unreachable in a 60-trial block at any
     * difficulty. True, and also true at 55; but it IS reachable from n=94 upward. The ceiling is a block-sizing
     * fact, not a permanent one, and stating the crossover is what turns it into a decision someone can take.
     */
    @Test
    fun ssrtBecomesReachableOnceTheBlockIsLongEnough() {
        val axis = assertNotNull(MeasurementReachability.axisFor(ExerciseId.E4_FRENO))
        val hardest = DifficultyVector.EASIEST.adjust(axis, AxisLevel(1.0))
        fun at(n: Int) = assertNotNull(MeasurementReachability.verdictFor(ExerciseId.E4_FRENO, n, hardest))

        assertTrue(!at(55).reachable, "SSRT must stay unreachable at the shortest template")
        assertTrue(!at(60).reachable, "SSRT must stay unreachable in the 60-trial baseline block")
        assertTrue(at(94).reachable, "SSRT is reachable from n=94 at maximum inhibitory demand")
        assertTrue(at(94).smallestCell >= at(94).floor, "the verdict must agree with its own numbers")
    }

    // ── Tri-state ───────────────────────────────────────────────────────────────────────────────────

    /**
     * `null` means "this exercise has no comparative metric with a ratified floor, so the question does not
     * apply". It is never reused as "no" — a sentinel carrying two meanings is how a gate ends up with a
     * ceiling below its own threshold (`MB-CATALOG-0009`).
     */
    @Test
    fun anExerciseWithoutAComparativeCellHasNoVerdictRatherThanANegativeOne() {
        val vigilance = MeasurementReachability.verdictFor(ExerciseId.E1_VIGIA, 60, DifficultyVector.EASIEST)
        assertNull(vigilance, "E1's primary metrics are all ACCURACY_RT_BASE; the question does not apply")
        assertNull(MeasurementReachability.axisFor(ExerciseId.E1_VIGIA))
        assertTrue(
            ExerciseId.E3_GATILLO in MeasurementReachability.behaviouralCells,
            "E3's go/no-go split is generator-drawn, but its post-error cells are split by what the USER did",
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────────

    private fun alwaysReachable(id: ExerciseId, n: Int): Boolean {
        val axis = MeasurementReachability.axisFor(id) ?: return true
        return (0..AXIS_STEPS).all { step ->
            val difficulty = DifficultyVector.EASIEST.adjust(axis, AxisLevel(step.toDouble() / AXIS_STEPS))
            MeasurementReachability.verdictFor(id, n, difficulty)?.reachable == true
        }
    }

    /** One compact row of the ADR §3 surface, so a move shows up as a readable diff rather than a number. */
    private fun surfaceRow(n: Int): String =
        MeasurementReachability.declaredExercises().sortedBy { it.name }.joinToString(" ") { id ->
            val axis = requireNotNull(MeasurementReachability.axisFor(id))
            val ok = (0..AXIS_STEPS).map { step ->
                val difficulty = DifficultyVector.EASIEST.adjust(axis, AxisLevel(step.toDouble() / AXIS_STEPS))
                MeasurementReachability.verdictFor(id, n, difficulty)?.reachable == true
            }
            val short = id.name.substringBefore('_')
            val lo = ok.indexOfFirst { it }
            val hi = ok.indexOfLast { it }
            when {
                ok.all { it } -> "$short:always"
                ok.none { it } -> "$short:never"
                lo == 0 -> "$short:<=${fmt(hi)}"
                hi == AXIS_STEPS -> "$short:>=${fmt(lo)}"
                else -> "$short:${fmt(lo)}-${fmt(hi)}"
            }
        }

    /** An axis index as a share of the axis. Formatted from the INDEX, never round-tripped through a Double. */
    private fun fmt(step: Int): String = "${step / AXIS_STEPS}.${(step % AXIS_STEPS).toString().padStart(2, '0')}"

    private fun generate(id: ExerciseId, difficulty: DifficultyVector, size: Int): List<Stimulus> {
        val spec = BlockSpec(targetTrialCount = size, mode = Mode.TRAINING)
        FocusExerciseGenerators.versioned[id]?.let {
            return it.generate(difficulty, spec, SessionSeed(1L, RngVersion.V1))
        }
        val legacy = requireNotNull(FocusExerciseGenerators.legacy[id]) { "no generator registered for $id" }
        return legacy.generate(difficulty, spec, 1L)
    }

    private companion object {
        /** 101 points across the axis: fine enough to catch a one-trial crossover like E5's at n=55. */
        const val AXIS_STEPS = 100
    }
}
