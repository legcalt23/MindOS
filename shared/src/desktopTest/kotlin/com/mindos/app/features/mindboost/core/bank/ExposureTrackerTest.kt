package com.mindos.app.features.mindboost.core.bank

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** INFRA-04 · exposure counts, holdout exhaustion, and rotation that never serves a holdout item. */
class ExposureTrackerTest {

    private data class TestItem(
        override val id: String,
        override val partition: ItemPartition,
        override val correctAnswerPosition: Int? = null,
        override val interRaterAgreement: Double? = null,
        override val difficultyLevel: Int = 3,
        override val constructTag: String = "tag",
        override val priorKnowledge: Boolean = false,
        override val culturalLoad: Double? = null,
    ) : BankItem

    private data class TestBank(
        override val items: List<BankItem>,
        override val bankId: String = "test",
        override val profileUpdating: Boolean = true,
    ) : ItemBank {
        override val requirements = BankRequirements(20, true, false, null, 3.84)
    }

    @Test
    fun recordServed_incrementsCount() {
        val tracker = ExposureTracker()
        tracker.recordServed("u", "i1")
        tracker.recordServed("u", "i1")
        assertEquals(2, tracker.exposureCount("u", "i1"))
        assertEquals(0, tracker.exposureCount("u", "i2"))
        assertEquals(0, tracker.exposureCount("other", "i1"))
    }

    @Test
    fun servedFraction_reflectsSeenItems() {
        val tracker = ExposureTracker()
        tracker.recordServed("u", "a")
        assertEquals(0.5, tracker.servedFraction("u", listOf("a", "b")))
        assertEquals(0.0, tracker.servedFraction("u", emptyList()))
    }

    @Test
    fun holdoutExhausted_onlyWhenAllServed() {
        val tracker = ExposureTracker()
        val holdout = listOf("h1", "h2")
        assertFalse(tracker.isHoldoutExhausted("u", holdout))
        tracker.recordServed("u", "h1")
        assertFalse(tracker.isHoldoutExhausted("u", holdout))
        tracker.recordServed("u", "h2")
        assertTrue(tracker.isHoldoutExhausted("u", holdout))
    }

    @Test
    fun rotation_neverServesHoldout_andRotatesTrainingItems() {
        val bank = TestBank(
            listOf(
                TestItem("t1", ItemPartition.TRAINING),
                TestItem("t2", ItemPartition.TRAINING),
                TestItem("h1", ItemPartition.HOLDOUT),
            ),
        )
        val tracker = ExposureTracker()
        val first = BankRotation.selectNextTraining(bank, tracker, "u")
        assertNotNull(first)
        assertEquals(ItemPartition.TRAINING, first.partition)
        tracker.recordServed("u", first.id)

        val second = BankRotation.selectNextTraining(bank, tracker, "u")
        assertNotNull(second)
        assertTrue(first.id != second.id, "rotation should pick the less-exposed training item")

        // Many rounds of practice must NEVER serve the holdout item.
        repeat(10) {
            BankRotation.selectNextTraining(bank, tracker, "u")?.let { tracker.recordServed("u", it.id) }
        }
        assertEquals(0, tracker.exposureCount("u", "h1"))
    }
}
