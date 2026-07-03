package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals

/** P1 EX1 (P4) · the accumulated span log appends copy-on-write and bounds its retention. */
class SpanRecordLogTest {

    private fun trial(items: Int) = SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = items)

    @Test
    fun append_addsTrialAndResponses() {
        val log = SpanRecordLog().append(trial(4), listOf(0, 1, 0))
        assertEquals(1, log.trials.size)
        assertEquals(listOf(0, 1, 0), log.processingResponses)
        assertEquals(4, log.trials.single().serialCorrectItems)
    }

    @Test
    fun append_isCopyOnWrite_doesNotMutateSource() {
        val original = SpanRecordLog()
        original.append(trial(3), listOf(1))
        assertEquals(0, original.trials.size)
        assertEquals(0, original.processingResponses.size)
    }

    @Test
    fun trials_areCappedToMostRecent() {
        var log = SpanRecordLog()
        repeat(SpanRecordLog.MAX_RETAINED_TRIALS + 10) { i -> log = log.append(trial(i), emptyList()) }
        assertEquals(SpanRecordLog.MAX_RETAINED_TRIALS, log.trials.size)
        // The retained window keeps the most recent trials (last appended is last in the list).
        assertEquals(SpanRecordLog.MAX_RETAINED_TRIALS + 9, log.trials.last().serialCorrectItems)
    }

    @Test
    fun processingResponses_areCappedToMostRecent() {
        var log = SpanRecordLog()
        repeat(SpanRecordLog.MAX_RETAINED_RESPONSES + 50) { log = log.append(trial(1), listOf(1)) }
        assertEquals(SpanRecordLog.MAX_RETAINED_RESPONSES, log.processingResponses.size)
    }
}
