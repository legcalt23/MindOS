package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.integrity.engine.IntegrityGateBuilder
import com.mindos.app.features.arena.integrity.engine.SecrecyPolicy
import com.mindos.app.features.arena.integrity.model.IntegritySignals
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.session.explanation.InternalReasonCode
import com.mindos.app.features.arena.session.explanation.UserSafeExplanationMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 05 / P5.2: Integrity gate builder, secrecy, human-review, and rating-engine integration. */
class IntegrityGateBuilderTest {

    private val builder = IntegrityGateBuilder()

    @Test
    fun cleanSignalsProduceVerifiedWithFullMultiplier() {
        val decision = builder.buildIntegrityGate(IntegritySignals())
        assertEquals(IntegrityStateEnum.VERIFIED, decision.state)
        assertEquals(1.0, decision.updateMultiplier)
    }

    @Test
    fun collusionInvalidatesAndOutranksOtherSignals() {
        val decision = builder.buildIntegrityGate(IntegritySignals(collusion = true, boosting = true))
        assertEquals(IntegrityStateEnum.INVALIDATED, decision.state)
        assertEquals(0.0, decision.updateMultiplier)
    }

    @Test
    fun boostingOrSharingHoldsTheAccount() {
        assertEquals(IntegrityStateEnum.HOLD, builder.buildIntegrityGate(IntegritySignals(boosting = true)).state)
        assertEquals(
            IntegrityStateEnum.HOLD,
            builder.buildIntegrityGate(IntegritySignals(accountSharing = true)).state,
        )
    }

    @Test
    fun smurfDoesNotBlockButRaisesSigmaSuspicion() {
        val decision = builder.buildIntegrityGate(IntegritySignals(smurf = true))
        assertEquals(IntegrityStateEnum.VERIFIED, decision.state)
        assertEquals(SmurfSuspicionLevel.HIGH, builder.smurfSuspicionFrom(IntegritySignals(smurf = true)))
        assertEquals(SmurfSuspicionLevel.NONE, builder.smurfSuspicionFrom(IntegritySignals()))
    }

    @Test
    fun lagSwitchIsComparabilityNotIntegrityHold() {
        val decision = builder.buildIntegrityGate(IntegritySignals(lagSwitch = true))
        assertEquals(IntegrityStateEnum.VERIFIED, decision.state)
        assertTrue(builder.lagSwitchForcesNotComparable(IntegritySignals(lagSwitch = true)))
    }

    @Test
    fun severeOutcomesRequireHumanReview() {
        assertTrue(builder.requiresHumanReview(builder.buildIntegrityGate(IntegritySignals(collusion = true))))
        assertTrue(builder.requiresHumanReview(builder.buildIntegrityGate(IntegritySignals(boosting = true))))
        assertTrue(!builder.requiresHumanReview(builder.buildIntegrityGate(IntegritySignals())))
    }

    @Test
    fun decisionReasonCodesNeverLeakDetectionInternals() {
        for (signals in listOf(
            IntegritySignals(),
            IntegritySignals(collusion = true),
            IntegritySignals(boosting = true),
            IntegritySignals(accountSharing = true),
        )) {
            val decision = builder.buildIntegrityGate(signals)
            assertTrue(SecrecyPolicy.isSafeForUser(decision.reasonCode), "leak in ${decision.reasonCode}")
            val userCopy = UserSafeExplanationMapper.copyFor(InternalReasonCode.fromRaw(decision.reasonCode))
            assertTrue(SecrecyPolicy.isSafeForUser(userCopy), "leak in user copy: $userCopy")
        }
        assertTrue(!SecrecyPolicy.isSafeForUser("Blocked by the collusion graph and smurf scoring"))
    }

    @Test
    fun verifiedDecisionLetsEngineApplyAndHoldBlocksIt() {
        val engine = BayesianRatingEngine()
        val verified = builder.buildIntegrityGate(IntegritySignals())
        val applied = engine.update(
            ArenaTestFixtures.posterior(),
            ArenaTestFixtures.verifiedEvidence(integrityState = verified.state),
        )
        assertTrue(applied is RatingUpdateResult.Applied)

        val hold = builder.buildIntegrityGate(IntegritySignals(boosting = true))
        val rejected = engine.update(
            ArenaTestFixtures.posterior(),
            ArenaTestFixtures.verifiedEvidence(integrityState = hold.state),
        )
        assertEquals(
            RatingUpdateRejectionReason.INTEGRITY_NOT_VERIFIED,
            (rejected as RatingUpdateResult.Rejected).reason,
        )
    }
}
