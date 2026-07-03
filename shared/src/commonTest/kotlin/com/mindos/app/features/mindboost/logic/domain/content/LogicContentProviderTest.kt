package com.mindos.app.features.mindboost.logic.domain.content

import com.mindos.app.features.mindboost.logic.domain.model.DistractorType
import com.mindos.app.features.mindboost.logic.domain.model.LogicExerciseType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogicContentProviderTest {
    private val items = DefaultLogicContentProvider().sessionItems()

    @Test
    fun initialPackCoversFiveFamiliesAndIncludesIsomorphicProbe() {
        assertEquals(LogicExerciseType.entries.toSet(), items.map { it.exerciseType }.toSet())
        val probe = items.first { it.isIsomorphicProbe }
        assertTrue(items.any { !it.isIsomorphicProbe && it.logicalStructureId == probe.logicalStructureId })
    }

    @Test
    fun contradictionPackContainsFalsePositiveControl() {
        val item = items.first { it.exerciseType == LogicExerciseType.CONTRADICTION_DETECTION }
        assertTrue(item.answerOptions.any { it.distractorType == DistractorType.FALSE_CONTRADICTION })
        assertTrue(item.answerOptions.first { it.isCorrect }.text.contains("consistente"))
    }
}

