package com.mindos.app.features.arena.evidence.motor

/** Phase 06: Reasons a motor is blocked from affecting Competitive Rating. */
enum class BlockingCondition {
    UNKNOWN_MOTOR,
    NOT_APPROVED,
    MISSING_SIGNOFFS,
    EXPIRED,
    NO_RATING_CONTEXT,
}

/** Phase 06: Aggregated eligibility result for a motor. */
data class MotorEligibilityResult(
    val motorId: String,
    val eligible: Boolean,
    val approvedCurrent: Boolean,
    val blockingConditions: List<BlockingCondition>,
)

/**
 * Phase 06: Evaluates the blocking conditions for a motor (blocking_conditions_resolver.md).
 */
class BlockingConditionsResolver(
    private val registry: MotorLedgerRegistry,
    private val ledgerGate: LedgerGate = LedgerGate(),
) {
    fun resolveBlockingConditions(motorId: String, nowMs: Long): List<BlockingCondition> {
        val ledger = registry.ledgerFor(motorId) ?: return listOf(BlockingCondition.UNKNOWN_MOTOR)
        return buildList {
            if (!ledger.statusApproved) add(BlockingCondition.NOT_APPROVED)
            if (!ledgerGate.validateSignOffs(ledger)) add(BlockingCondition.MISSING_SIGNOFFS)
            if (ledgerGate.checkLedgerExpiry(ledger, nowMs)) add(BlockingCondition.EXPIRED)
            if (ledger.eligibleContexts.none { it.affectsRating }) add(BlockingCondition.NO_RATING_CONTEXT)
        }
    }
}

/**
 * Phase 06: Aggregates ledger status, sign-offs, expiry, and blocking conditions into one decision
 * (motor_eligibility_checker.md). A motor is rating-eligible only when it has no blocking conditions.
 */
class MotorEligibilityChecker(
    private val registry: MotorLedgerRegistry,
    private val ledgerGate: LedgerGate = LedgerGate(),
    private val resolver: BlockingConditionsResolver = BlockingConditionsResolver(registry, ledgerGate),
) {
    fun checkEligibility(motorId: String, nowMs: Long): MotorEligibilityResult {
        val blocking = resolver.resolveBlockingConditions(motorId, nowMs)
        val ledger = registry.ledgerFor(motorId)
        val approvedCurrent = ledger != null && ledgerGate.isApprovedCurrent(ledger, nowMs)
        return MotorEligibilityResult(
            motorId = motorId,
            eligible = blocking.isEmpty(),
            approvedCurrent = approvedCurrent,
            blockingConditions = blocking,
        )
    }
}
