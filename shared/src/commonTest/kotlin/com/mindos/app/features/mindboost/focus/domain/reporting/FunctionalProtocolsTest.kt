package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionalProtocolsTest {

    @Test
    fun everyContext_hasAProtocol() {
        assertEquals(FunctionalContext.entries.size, FunctionalProtocols.all.size)
        FunctionalContext.entries.forEach { assertEquals(it, FunctionalProtocols.forContext(it).context) }
    }

    @Test
    fun allProtocols_areWeakSubjective_withADisclaimer() {
        FunctionalProtocols.all.forEach {
            assertEquals(TransferStrength.WEAK_SUBJECTIVE, it.strength)
            assertTrue(it.disclaimer.isNotBlank())
            assertTrue(it.exercises.isNotEmpty())
        }
    }

    @Test
    fun disclaimer_neverPromisesProvenTransfer() {
        val disclaimer = FunctionalProtocol.DEFAULT_DISCLAIMER.lowercase()
        assertTrue("no está demostrada" in disclaimer)
        assertTrue("no una garantía" in disclaimer || "no es una garantía" in disclaimer)
    }

    @Test
    fun contextsMapToExpectedExercises() {
        fun exercises(c: FunctionalContext) = FunctionalProtocols.forContext(c).exercises
        assertTrue(ExerciseId.E13_PRESION in exercises(FunctionalContext.COMPETITION))
        assertTrue(ExerciseId.E10_FLUJO in exercises(FunctionalContext.READING))
        assertTrue(ExerciseId.E4_FRENO in exercises(FunctionalContext.DECISION_UNDER_PRESSURE))
        assertTrue(ExerciseId.E11_ANCLA in exercises(FunctionalContext.STUDY))
    }
}
