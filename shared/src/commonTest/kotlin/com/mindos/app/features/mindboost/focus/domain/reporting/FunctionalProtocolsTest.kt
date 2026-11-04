package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.core.copy.ForbiddenLexicon
import com.mindos.app.features.mindboost.core.copy.MindBoostCopySafetyScanner
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
        assertTrue("no una promesa" in disclaimer, "the disclaimer must bound itself: $disclaimer")
        assertTrue("instrumento independiente" in disclaimer, "it must name what is missing: $disclaimer")
        assertNoClaimAboveC0(disclaimer)
    }

    /**
     * The second half, and the one this test did not have before FOC-P0-00. `FocusClaimLevelGateTest`
     * scans production string LITERALS, so it cannot see a sentence composed at runtime from two of
     * them — which is exactly what the notes in this package are. Checking the composed string here
     * closes that declared ceiling instead of restating the gate.
     */
    private fun assertNoClaimAboveC0(composed: String) {
        val hits = MindBoostCopySafetyScanner.scan(composed, ForbiddenLexicon.EFFICACY_CLAIMS_C0)
        assertTrue(hits.isEmpty(), "composed copy claims above C0 ${hits.map { it.term }}: $composed")
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
