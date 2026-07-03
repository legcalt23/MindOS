package com.mindos.app.features.arena.hardware

/**
 * Phase 05 / Task 5: Raised when an offline match/comparison is attempted that violates a hardware
 * comparability invariant (e.g. ranking Mental Speed across differing hardware classes).
 *
 * The message is composed only of enum names / hardware-class ids — never UIDs or PII — so it is
 * safe to surface. This is an offline domain invariant only; it is NOT live matchmaking.
 */
class IllegalMatchException(message: String) : IllegalStateException(message)
