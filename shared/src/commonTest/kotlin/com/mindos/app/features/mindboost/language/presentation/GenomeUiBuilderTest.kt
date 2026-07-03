package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.model.AbilityPosterior
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.TraitEstimate
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import com.mindos.app.features.mindboost.language.presentation.state.ReliabilityBand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the Skill Genome / Evidence Cockpit projection (§11.2/§11.3) and the honesty rule that a
 * probable range only appears when real, reliable evidence earns it (§11.6).
 */
class GenomeUiBuilderTest {

    private fun estimate(state: EvidenceState, reliability: Double) = TraitEstimate(
        trait = TraitCode.INF,
        thetaMean = 0.0,
        thetaSd = 0.5,
        reliability = reliability,
        evidenceState = state,
        practiceEffectRisk = 0.0,
        confounderRisk = 0.0,
        fairnessRisk = 0.0,
        estimatorVersion = "v6-test",
        updatedAtEpochMs = 1L,
    )

    private fun profileWith(vararg estimates: Pair<TraitCode, TraitEstimate>) =
        UserLanguageProfile(UserIdHash("u1"), estimates.toMap(), updatedAtEpochMs = 1L)

    @Test
    fun genomeCoversEveryTraitAndMarksUnmeasuredAsCalibrating() {
        val profile = profileWith(TraitCode.INF to estimate(EvidenceState.E2_NEAR_TRANSFER, 0.7))
        val genome = GenomeUiBuilder.skillGenome(profile)

        assertEquals(TraitCode.entries.size, genome.nodes.size)
        val inf = genome.nodes.first { it.trait == TraitCode.INF }
        assertTrue(inf.isMeasured)
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, inf.evidenceState)
        assertEquals(ReliabilityBand.MEDIUM_HIGH, inf.reliabilityBand)

        val unmeasured = genome.nodes.first { it.trait == TraitCode.ARG }
        assertFalse(unmeasured.isMeasured)
        assertEquals(EvidenceState.E0_CALIBRATING, unmeasured.evidenceState)
        assertEquals(ReliabilityBand.LOW, unmeasured.reliabilityBand)
    }

    @Test
    fun cockpitShowsMeasuredSkillsStrongestFirst() {
        val profile = profileWith(
            TraitCode.INF to estimate(EvidenceState.E1_TRAINED_IMPROVEMENT, 0.3).copy(trait = TraitCode.INF),
            TraitCode.SUM to estimate(EvidenceState.E3_FUNCTIONAL_TRANSFER, 0.5).copy(trait = TraitCode.SUM),
        )
        val cockpit = GenomeUiBuilder.evidenceCockpit(profile)
        assertEquals(2, cockpit.items.size)
        assertEquals(TraitCode.SUM, cockpit.items.first().trait) // E3 before E1
    }

    @Test
    fun rangeIsNullWithoutPosteriorOrWhenReliabilityTooLow() {
        val profile = profileWith(TraitCode.INF to estimate(EvidenceState.E2_NEAR_TRANSFER, 0.4))
        // No posterior at all.
        assertNull(GenomeUiBuilder.evidenceCockpit(profile).items.first().probableRange)
        // Posterior present but reliability below the bar.
        val posterior = posterior(low = -1.0, high = 1.0)
        val withLowReliability = GenomeUiBuilder.evidenceCockpit(
            profile,
            posteriors = mapOf(TraitCode.INF to posterior),
        )
        assertNull(withLowReliability.items.first().probableRange)
    }

    @Test
    fun rangeAppearsOnlyWithRealReliableEvidence() {
        val profile = profileWith(TraitCode.INF to estimate(EvidenceState.E2_NEAR_TRANSFER, 0.8))
        val cockpit = GenomeUiBuilder.evidenceCockpit(
            profile,
            posteriors = mapOf(TraitCode.INF to posterior(low = -1.0, high = 1.0)),
        )
        val range = assertNotNull(cockpit.items.first().probableRange)
        assertEquals(35, range.low) // 50 + (-1)*15
        assertEquals(65, range.high) // 50 + 1*15
    }

    @Test
    fun nextEvidenceAndLimitationAreHonestForState() {
        val profile = profileWith(TraitCode.INF to estimate(EvidenceState.E1_TRAINED_IMPROVEMENT, 0.7))
        val item = GenomeUiBuilder.evidenceCockpit(profile).items.first()
        assertTrue(item.nextEvidenceNeeded.contains("material nuevo"))
        assertTrue(item.limitation.contains("tarea aplicada"))
    }

    @Test
    fun reliabilityBandThresholds() {
        assertEquals(ReliabilityBand.LOW, ReliabilityBand.fromReliability(0.49))
        assertEquals(ReliabilityBand.MEDIUM, ReliabilityBand.fromReliability(0.5))
        assertEquals(ReliabilityBand.MEDIUM_HIGH, ReliabilityBand.fromReliability(0.7))
        assertEquals(ReliabilityBand.HIGH, ReliabilityBand.fromReliability(0.85))
    }

    private fun posterior(low: Double, high: Double) = AbilityPosterior(
        trait = TraitCode.INF,
        thetaMean = 0.0,
        thetaSd = 0.5,
        credibleInterval90Low = low,
        credibleInterval90High = high,
        reliability = 0.8,
        evidenceState = EvidenceState.E2_NEAR_TRANSFER,
        nearTransferEvidence = 0.5,
        functionalTransferEvidence = 0.0,
        maintenanceEvidence = 0.0,
        practiceEffectRisk = 0.0,
        confounderRisk = 0.0,
        fairnessRisk = 0.0,
        estimatorVersion = "v6-test",
        lastUpdatedAtEpochMs = 1L,
    )
}
