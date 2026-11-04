package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.timing.TemporalWindowBucket
import com.mindos.app.features.mindboost.core.timing.TemporalWindowVerdict
import com.mindos.app.features.mindboost.core.timing.WindowExclusionReason
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §7/§15 [R2-HIST] — the reducer's SHADOW fold. The load-bearing assertions:
 *
 *  1. The official tallies are IDENTICAL with and without a temporal verdict — in S5 nothing
 *     per-window may alter the official path (an EXCLUDED verdict still counts officially).
 *  2. The sum of a condition's temporal buckets equals its official cell (consistency invariant).
 *  3. Discards (pause / cross-epoch) are attributed PER CONDITION with a typed reason, and
 *     `contaminatedCount == Σ (PAUSE_DISCARD + CLOCK_EPOCH)` (the per-session-only counter was the
 *     cherry-picking blind spot — eficacia P1-C).
 */
class FocusTemporalRuntimeTest {

    private val spec = VectorFilterTask.SPEC

    private fun running(): FocusRuntimeSnapshot {
        val initial = FocusRuntimeSnapshot.initial("s5-temporal")
        return FocusSessionReducer.reduce(initial, FocusRuntimeInput.StartSession(0L), spec).snapshot
    }

    private fun present(
        snapshot: FocusRuntimeSnapshot,
        at: Long,
        condition: ConditionId = ConditionId.CONGRUENT,
    ): FocusRuntimeSnapshot = FocusSessionReducer.reduce(
        snapshot,
        FocusRuntimeInput.PresentTrial(at, WINDOW_NANOS, PLANNED_TRIALS, condition),
        spec,
    ).snapshot

    private fun respond(
        snapshot: FocusRuntimeSnapshot,
        at: Long,
        verdict: TemporalWindowVerdict?,
        sampledFrames: Int = 30,
        condition: ConditionId = ConditionId.CONGRUENT,
    ): FocusRuntimeSnapshot = FocusSessionReducer.reduce(
        snapshot,
        FocusRuntimeInput.Respond(at, Response.LEFT, condition, isSignal = true, verdict, sampledFrames),
        spec,
    ).snapshot

    private fun FocusRuntimeSnapshot.temporalRow(condition: ConditionId, bucket: TemporalWindowBucket) =
        temporalCells.firstOrNull { it.conditionName == condition.name && it.bucketName == bucket.name }

    @Test
    fun present_storesTheOpenCondition_andResolveClearsIt() {
        val presented = present(running(), at = 1_000L, condition = ConditionId.INCONGRUENT)
        assertEquals(ConditionId.INCONGRUENT.name, presented.openConditionName)
        val resolved = respond(presented, at = 500_000_000L, verdict = TemporalWindowVerdict.Clean,
            condition = ConditionId.INCONGRUENT)
        assertNull(resolved.openConditionName)
    }

    @Test
    fun cleanVerdict_foldsTheSameOutcome_intoOfficialAndShadow() {
        val resolved = respond(present(running(), at = 1_000L), at = 500_000_000L, TemporalWindowVerdict.Clean)
        val official = resolved.cells().getValue(ConditionId.CONGRUENT)
        val shadow = resolved.temporalRow(ConditionId.CONGRUENT, TemporalWindowBucket.CLEAN)
        assertEquals(1, official.totalTrials)
        assertEquals(official, shadow?.cell, "the shadow bucket must carry the SAME outcome")
        assertEquals(30, resolved.temporalSampledFrames)
    }

    @Test
    fun excludedVerdict_stillCountsOfficially_shadowOnlyInS5() {
        val verdict = TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK)
        val resolved = respond(present(running(), at = 1_000L), at = 500_000_000L, verdict)
        val official = resolved.cells().getValue(ConditionId.CONGRUENT)
        val excludedRow = resolved.temporalRow(ConditionId.CONGRUENT, TemporalWindowBucket.EXCLUDED)
        assertEquals(1, official.totalTrials, "S5: no per-window signal may alter the official path")
        assertEquals(1, excludedRow?.cell?.totalTrials)
        assertEquals(1, excludedRow?.excludedByReason?.get(WindowExclusionReason.TRUNCATION_RISK.name))
    }

    @Test
    fun bucketSums_equalTheOfficialCell_consistencyInvariant() {
        var s = running()
        val verdicts = listOf(
            TemporalWindowVerdict.Clean,
            TemporalWindowVerdict.DegradedUsable,
            TemporalWindowVerdict.NoObservation,
            TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK),
        )
        var at = 1_000L
        verdicts.forEach { verdict ->
            s = present(s, at)
            s = respond(s, at + RT_NANOS, verdict)
            at += STEP_NANOS
        }
        val official = s.cells().getValue(ConditionId.CONGRUENT).totalTrials
        val shadowSum = s.temporalCells
            .filter { it.conditionName == ConditionId.CONGRUENT.name }
            .sumOf { it.cell.totalTrials }
        assertEquals(4, official)
        assertEquals(official, shadowSum, "Σ buckets must equal the official cell")
    }

    @Test
    fun nullVerdict_foldsNothing_soAbsenceStaysAbsence() {
        val resolved = respond(present(running(), at = 1_000L), at = 500_000_000L, verdict = null, sampledFrames = 0)
        assertEquals(1, resolved.cells().getValue(ConditionId.CONGRUENT).totalTrials)
        assertTrue(resolved.temporalCells.isEmpty(), "no probe wired must read as NotCaptured downstream")
        assertEquals(0, resolved.temporalSampledFrames)
    }

    @Test
    fun pauseWithOpenWindow_attributesTheDiscard_toItsCondition() {
        val presented = present(running(), at = 1_000L, condition = ConditionId.INCONGRUENT)
        val paused = FocusSessionReducer.reduce(presented, FocusRuntimeInput.Pause(2_000L), spec).snapshot
        val row = paused.temporalRow(ConditionId.INCONGRUENT, TemporalWindowBucket.EXCLUDED)
        assertEquals(1, row?.excludedByReason?.get(WindowExclusionReason.PAUSE_DISCARD.name))
        assertEquals(0, row?.cell?.totalTrials, "a discard was never resolved: no SDT outcome")
        assertEquals(paused.contaminatedCount, discardReasonTotal(paused), "legacy counter consistency")
    }

    @Test
    fun crossEpochResponse_attributesClockEpoch_toItsCondition() {
        val presented = present(running(), at = 1_000_000_000L)
        // A reading EARLIER than the presentation = different monotonic epoch.
        val resolved = respond(presented, at = 500L, verdict = null)
        val row = resolved.temporalRow(ConditionId.CONGRUENT, TemporalWindowBucket.EXCLUDED)
        assertEquals(1, row?.excludedByReason?.get(WindowExclusionReason.CLOCK_EPOCH.name))
        assertEquals(resolved.contaminatedCount, discardReasonTotal(resolved), "legacy counter consistency")
    }

    private fun discardReasonTotal(snapshot: FocusRuntimeSnapshot): Int = snapshot.temporalCells.sumOf { cell ->
        cell.excludedByReason
            .filterKeys {
                it == WindowExclusionReason.PAUSE_DISCARD.name || it == WindowExclusionReason.CLOCK_EPOCH.name
            }
            .values.sum()
    }

    private companion object {
        const val WINDOW_NANOS = 1_200_000_000L
        const val PLANNED_TRIALS = 50
        const val RT_NANOS = 400_000_000L
        const val STEP_NANOS = 2_000_000_000L
    }
}
