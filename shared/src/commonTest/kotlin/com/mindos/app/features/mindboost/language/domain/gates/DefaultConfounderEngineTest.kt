package com.mindos.app.features.mindboost.language.domain.gates

import com.mindos.app.features.mindboost.language.domain.model.DependencyLevel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins confounder assessment (§5.6): clean signals raise no risk; unvalidated variants, high
 * prior knowledge, suspicious speed/accuracy and undeclared domains each raise their flag.
 */
class DefaultConfounderEngineTest {

    private val engine = DefaultConfounderEngine()

    @Test
    fun cleanSignalsRaiseNoRisk() {
        val assessment = engine.assess(ConfounderSignals())
        assertFalse(assessment.hasHighPriorKnowledge)
        assertFalse(assessment.variantBiasRisk)
        assertFalse(assessment.externalDomainUndeclared)
    }

    @Test
    fun unvalidatedVariantRaisesBiasRisk() {
        val assessment = engine.assess(ConfounderSignals(variantValidated = false))
        assertTrue(assessment.variantBiasRisk)
    }

    @Test
    fun highPriorKnowledgeIsFlagged() {
        val assessment = engine.assess(
            ConfounderSignals(priorKnowledgeDependency = DependencyLevel.HIGH),
        )
        assertTrue(assessment.hasHighPriorKnowledge)
    }

    @Test
    fun suspiciousSpeedAccuracyEscalatesPriorKnowledgeToHigh() {
        val assessment = engine.assess(
            ConfounderSignals(
                priorKnowledgeDependency = DependencyLevel.LOW,
                suspiciousSpeedAccuracy = true,
            ),
        )
        assertTrue(assessment.hasHighPriorKnowledge)
    }

    @Test
    fun undeclaredExternalDomainIsFlagged() {
        val assessment = engine.assess(ConfounderSignals(externalDomainDeclared = false))
        assertTrue(assessment.externalDomainUndeclared)
    }
}
