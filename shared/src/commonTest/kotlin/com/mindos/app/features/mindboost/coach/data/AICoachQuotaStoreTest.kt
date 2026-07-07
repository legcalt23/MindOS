@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.coach.data

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** MC8u-G §5 — the daily coach quota is durable (survives a restart) and resets on a new day. */
class AICoachQuotaStoreTest {

    @Test
    fun the_used_count_persists_within_the_same_day() = runTest {
        val disk = InMemorySecureKeyValueStore()
        val day = coachQuotaDayOf(1_700_000_000_000L)
        SecureAICoachQuotaStore(disk).record("u", day, used = 3)
        // "restart": a fresh store instance over the same encrypted disk, same day.
        assertEquals(3, SecureAICoachQuotaStore(disk).usedToday("u", day))
    }

    @Test
    fun the_quota_resets_on_a_new_day() = runTest {
        val disk = InMemorySecureKeyValueStore()
        val today = 100L
        SecureAICoachQuotaStore(disk).record("u", today, used = 20)
        assertEquals(0, SecureAICoachQuotaStore(disk).usedToday("u", today + 1), "a new day resets the quota")
    }

    @Test
    fun the_day_bucket_is_a_utc_day() {
        assertEquals(coachQuotaDayOf(0L), coachQuotaDayOf(86_399_999L), "same UTC day")
        assertEquals(coachQuotaDayOf(0L) + 1, coachQuotaDayOf(86_400_000L), "next UTC day")
    }

    @Test
    fun an_absent_record_reads_zero() = runTest {
        assertEquals(0, SecureAICoachQuotaStore(InMemorySecureKeyValueStore()).usedToday("u", 100L))
    }

    @Test
    fun an_invalid_user_id_is_rejected() = runTest {
        assertFailsWith<IllegalArgumentException> { SecureAICoachQuotaStore(InMemorySecureKeyValueStore()).record("bad:id", 1L, 1) }
    }
}
