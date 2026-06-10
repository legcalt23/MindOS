package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MemoryTrialModelsTest {

    private fun word(id: String) = WordItem(
        id = WordId(id),
        text = "w$id",
        category = WordCategory.ANIMALS,
        concreteness = Concreteness.CONCRETE,
        frequency = WordFrequency.HIGH,
        lengthChars = 3,
    )

    @Test
    fun complexSpanTrial_rejectsNonSerialRecall() {
        assertFailsWith<IllegalArgumentException> {
            ComplexSpanTrial(
                steps = listOf(Memorandum(word("1"))),
                recallType = RecallType.FREE,
                expectedRecall = listOf(word("1")),
            )
        }
    }

    @Test
    fun complexSpanTrial_rejectsMismatchBetweenStepsAndExpected() {
        assertFailsWith<IllegalArgumentException> {
            ComplexSpanTrial(
                steps = listOf(Memorandum(word("1"))),
                recallType = RecallType.SERIAL_FORWARD,
                expectedRecall = listOf(word("2")),
            )
        }
    }

    @Test
    fun recallListTrial_cueCategoryRequiredIffCued() {
        assertFailsWith<IllegalArgumentException> {
            RecallListTrial(
                studyList = listOf(word("1")),
                recallType = RecallType.CUED,
                studyMsPerItem = 1000,
                delayMs = 0,
                cueCategory = null,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RecallListTrial(
                studyList = listOf(word("1")),
                recallType = RecallType.FREE,
                studyMsPerItem = 1000,
                delayMs = 0,
                cueCategory = WordCategory.ANIMALS,
            )
        }
    }

    @Test
    fun cuedRecallProbe_rejectsCueEqualsTarget() {
        assertFailsWith<IllegalArgumentException> {
            CuedRecallProbe(cue = word("1"), expectedTarget = word("1"), direction = RecallDirection.FORWARD)
        }
    }
}
