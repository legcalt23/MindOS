package com.mindos.app.features.arena

import com.mindos.app.features.arena.audit.RatingAuditLogger
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import kotlin.test.Test
import kotlin.test.assertEquals

/** R-04 conformance: every rating mutation (applied or rejected) produces an audit log entry. */
class RatingAuditLoggerTest {

    private val engine = BayesianRatingEngine()
    private val logger = RatingAuditLogger()
    private val prior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 300.0)

    @Test
    fun appliedUpdateProducesAuditLogWithBeforeAndAfter() {
        val result = engine.update(prior, ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.9))
        val applied = result as RatingUpdateResult.Applied
        val log = logger.audit(prior, result, "AUDIT_1", "SESSION_1", 100L)
        assertEquals("APPLIED", log.decisionState)
        assertEquals("STANDARD_UPDATE", log.reasonCode)
        assertEquals(1500.0, log.previousRatingMu)
        assertEquals(applied.posterior.mean.value, log.newRatingMu)
        assertEquals(applied.posterior.uncertainty.value, log.newRatingSigma)
        assertEquals("BayesianRatingEngine", log.sourceEngine)
    }

    @Test
    fun rejectedUpdateProducesAuditLogWithUnchangedRatingAndReason() {
        val result = engine.update(
            prior,
            ArenaTestFixtures.verifiedEvidence(integrityState = IntegrityStateEnum.HOLD),
        )
        val log = logger.audit(prior, result, "AUDIT_2", "SESSION_2", 200L)
        assertEquals("REJECTED", log.decisionState)
        assertEquals(RatingUpdateRejectionReason.INTEGRITY_NOT_VERIFIED.name, log.reasonCode)
        assertEquals(log.previousRatingMu, log.newRatingMu) // unchanged
        assertEquals(log.previousRatingSigma, log.newRatingSigma)
    }
}
