package com.mindos.action.execution

import com.mindos.action.model.MindAction
import com.mindos.action.validation.ValidationContext

/** Deterministic source of audit timestamps (no hidden wall-clock in core). App injects real time. */
fun interface ExecutionClock {
    fun nowMillis(): Long
}

/** A fixed-zero clock for tests and contract usage where real time is irrelevant. */
object ExecutionClocks {
    val Zero = ExecutionClock { 0L }
}

/** Per-request execution inputs. */
data class ExecutionContext(
    val validation: ValidationContext = ValidationContext(),
    val sessionId: String? = null,
)

/**
 * Executes a validated [MindAction] (ZIP03 ACTION_EXECUTOR): validate → resolve references →
 * produce a typed navigation command → hand to the coordinator → emit telemetry + audit.
 *
 * `suspend` (resolution and navigation are async). The executor never parses text and never
 * touches UI; navigation is owned by the [com.mindos.action.navigation.NavigationCoordinator].
 */
interface ActionExecutor {
    suspend fun execute(action: MindAction, context: ExecutionContext = ExecutionContext()): ExecutionResult
}
