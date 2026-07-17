@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.session.InMemorySessionRecordStore
import com.mindos.app.features.mindboost.core.session.cognitive.InMemoryCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.sync.DefaultSyncSessionIdGenerator
import com.mindos.app.features.mindboost.core.sync.SyncSessionIdGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoConfig
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4B · e8-$seed micro-audit (CEO-mandated before slice 6).
 *
 * The old sessionId was `"e8-$seed"` with `seed = clock.nowMillis() + runSeq`, and `runSeq` resets to 0 per holder
 * instance — so two E8 entries in the SAME wall-clock millisecond produced the SAME id → a silent collision / false
 * idempotency in the run store. The fix injects a [SyncSessionIdGenerator] whose random suffix disambiguates
 * same-millisecond starts, and DECOUPLES the RNG seed (stimulus randomization only) from the sessionId. These tests
 * pin the fix; they inject a deterministic incrementing suffix so distinctness does not rely on randomness.
 */
class GoNoGoSessionIdentityTest {

    private val sameMsClock = EpochClock { 1_000L }

    /** Two independent generators sharing one counter → distinct suffixes without relying on RNG (mirrors global Random). */
    private fun incrementingGenerators(): Pair<SyncSessionIdGenerator, SyncSessionIdGenerator> {
        var n = 0
        val source = { "s${n++}" }
        return DefaultSyncSessionIdGenerator(suffixSource = source) to DefaultSyncSessionIdGenerator(suffixSource = source)
    }

    private fun holder(
        scope: CoroutineScope,
        gen: SyncSessionIdGenerator,
        runs: InMemoryCognitiveSessionRunStore = InMemoryCognitiveSessionRunStore(),
        clock: EpochClock = sameMsClock,
    ) = GoNoGoSessionStateHolder(
        scope = scope,
        sessionRecordStore = InMemorySessionRecordStore(),
        cognitiveRunStore = runs,
        config = GoNoGoConfig(),
        clock = clock,
        sessionIdGenerator = gen,
        trialCount = 16,
    )

    private fun playRun(h: GoNoGoSessionStateHolder) {
        h.startSession()
        repeat(16) {
            h.onStimulusShown()
            val type = h.state.value.currentStimulus!!.trialType
            if (type == GoNoGoTrialType.GO) h.onStimulusTap(300L) else h.onResponseWindowElapsed()
            h.beginNextTrial()
        }
    }

    @Test
    fun two_E8_sessions_with_same_config_and_start_time_get_distinct_sessionIds() = runTest {
        val (g1, g2) = incrementingGenerators()
        val h1 = holder(this, g1)
        val h2 = holder(this, g2)
        h1.startSession()
        h2.startSession()
        val id1 = assertNotNull(h1.state.value.sessionId)
        val id2 = assertNotNull(h2.state.value.sessionId)
        assertNotEquals(id1, id2, "same config + same start ms must NOT collide (the old e8-\$seed did)")
        coroutineContext.cancelChildren()
    }

    @Test
    fun repeating_a_block_in_the_same_millisecond_does_not_reuse_the_sessionId() = runTest {
        val gen = DefaultSyncSessionIdGenerator(suffixSource = incrementingSuffix())
        val h = holder(this, gen)
        h.startSession()
        val first = assertNotNull(h.state.value.sessionId)
        h.startSession() // immediate repeat, same clock ms
        val second = assertNotNull(h.state.value.sessionId)
        assertNotEquals(first, second, "a quick repeat must not reuse the sessionId even at the same clock ms")
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_new_E8_session_is_not_a_false_idempotent_duplicate_of_a_prior_one() = runTest {
        val runs = InMemoryCognitiveSessionRunStore()
        val gen = DefaultSyncSessionIdGenerator(suffixSource = incrementingSuffix())
        val h = holder(this, gen, runs = runs)
        playRun(h) // session 1
        playRun(h) // session 2 (a genuinely new session, same clock ms)
        advanceUntilIdle()
        val saved = runs.listRecent(10)
        assertEquals(2, saved.size, "two DISTINCT sessions persist as two runs; a new session is never a retry")
        assertEquals(2, saved.map { it.sessionId }.toSet().size, "the two runs have distinct sessionIds")
        coroutineContext.cancelChildren()
    }

    private fun incrementingSuffix(): () -> String {
        var n = 0
        return { "s${n++}" }
    }
}
