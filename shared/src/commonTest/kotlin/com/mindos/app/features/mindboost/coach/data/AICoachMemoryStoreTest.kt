@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.coach.data

import com.mindos.app.features.mindboost.coach.domain.CoachCalibrationState
import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.coach.domain.CoachContribution
import com.mindos.app.features.mindboost.coach.domain.CoachSummary
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** MC8u-F — coach memory is local-first, round-trips, is deletable (borrar memoria IA), and is minimized (no raw/clinical). */
class AICoachMemoryStoreTest {

    private fun memory() = AICoachMemory(summaryMd = "s", strengthsMd = "a", weaknessesMd = "b", currentPlanMd = "p", recommendationsMd = "r", lastUpdatedAtMillis = 1L, version = 1)

    @Test
    fun save_load_and_delete_round_trip() = runTest {
        val store = SecureAICoachMemoryStore(InMemorySecureKeyValueStore())
        store.save("u", memory())
        assertEquals(memory(), store.load("u"))
        store.delete("u")
        assertNull(store.load("u"), "delete supports 'borrar memoria IA' + account deletion")
    }

    @Test
    fun factory_produces_minimized_non_clinical_memory() {
        val summary = CoachSummary(listOf(CoachContribution("EMOCIONES", CoachCalibrationState.PROVISIONAL, CoachConfidenceLevel.MEDIUM, patterns = listOf("atencion"))), CoachConfidenceLevel.MEDIUM)
        val m = AICoachMemoryFactory.fromSummary(summary, currentPlanMd = "plan de hoy", nowMillis = 1_000L, version = 1)
        val blob = listOf(m.summaryMd, m.strengthsMd, m.weaknessesMd, m.recommendationsMd, m.currentPlanMd).joinToString(" ").lowercase()
        listOf("rtnanos", "latencyms", "trial", "valence", "arousal", "selfreport", "diagnostic", "medicacion", "@").forEach {
            assertFalse(blob.contains(it), "memory must not carry raw/clinical/PII marker '$it'")
        }
    }

    @Test
    fun an_invalid_user_id_is_rejected() = runTest {
        assertFailsWith<IllegalArgumentException> { SecureAICoachMemoryStore(InMemorySecureKeyValueStore()).save("bad:id", memory()) }
    }
}
