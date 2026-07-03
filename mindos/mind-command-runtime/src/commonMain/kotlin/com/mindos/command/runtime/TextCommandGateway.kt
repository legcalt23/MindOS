package com.mindos.command.runtime

import com.mindos.action.execution.ActionExecutor
import com.mindos.action.execution.ExecutionContext
import com.mindos.action.execution.ExecutionResult
import com.mindos.action.model.MindAction
import com.mindos.contracts.ContractSchema
import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.RouterAbuseContextDto
import com.mindos.contracts.RouterCommandSourceDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterSuccessDto
import kotlinx.coroutines.CancellationException

interface TextCommandGateway {
    suspend fun routeAndExecute(request: TextCommandRequest): CommandDispatchResult
    suspend fun confirmAndExecute(request: CommandConfirmationRequest): CommandDispatchResult
    suspend fun cancelConfirmation(request: CommandConfirmationRequest): CommandConfirmationCancellationResult
}

fun interface CommandRuntimeClock {
    fun nowMillis(): Long
}

object CommandRuntimeClocks {
    val Zero = CommandRuntimeClock { 0L }
}

class DefaultTextCommandGateway(
    private val router: KmpCommandRouter,
    private val adapter: RouterActionExecutionAdapter,
    private val executor: ActionExecutor,
    private val safetyPolicy: CommandSafetyPolicy,
    private val confirmationStore: ConfirmationStore,
    private val config: CommandRuntimeConfig = CommandRuntimeConfig(),
    private val clock: CommandRuntimeClock = CommandRuntimeClocks.Zero,
) : TextCommandGateway {

    override suspend fun routeAndExecute(request: TextCommandRequest): CommandDispatchResult {
        if (!config.enabled) return rejected(request.requestId, CommandRejectionReason.RuntimeDisabled)

        val routingStartedAt = clock.nowMillis()
        val routing = try {
            router.route(
                RouterRequestDto(
                    requestId = request.requestId,
                    input = request.input,
                    localeTag = request.localeTag,
                    sessionId = request.sessionId,
                    source = request.source.toRouterSource(),
                    abuseContext = request.abuseContext.withSessionFallback(request.sessionId),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return rejected(
                request.requestId,
                CommandRejectionReason.InternalFailure(RuntimeStage.ROUTING),
                CommandDispatchTimings(routingMs = elapsed(routingStartedAt)),
            )
        }
        val timings = CommandDispatchTimings(routingMs = elapsed(routingStartedAt))

        if (routing.requestId != request.requestId || routing.schemaVersion != ContractSchema.Current) {
            return rejected(request.requestId, CommandRejectionReason.ResultCorrelationMismatch, timings)
        }
        val success = when (routing) {
            is RouterFailureResultDto ->
                return CommandDispatchResult.RoutingFailed(request.requestId, routing, timings)
            is RouterSuccessDto -> routing
        }
        if (success.action.requestId != request.requestId) {
            return rejected(request.requestId, CommandRejectionReason.ResultCorrelationMismatch, timings)
        }
        val mapping = try {
            adapter.map(success.action)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return rejected(request.requestId, CommandRejectionReason.InternalFailure(RuntimeStage.MAPPING), timings)
        }
        val action = when (mapping) {
            is MindActionMappingResult.Mapped -> mapping.action
            is MindActionMappingResult.InvalidPayload ->
                return rejected(request.requestId, CommandRejectionReason.MappingFailed(mapping.reason), timings)
            is MindActionMappingResult.Unsupported ->
                return rejected(request.requestId, CommandRejectionReason.MappingFailed("unsupported_action"), timings)
        }
        if (action.requestId.value != request.requestId) {
            return rejected(request.requestId, CommandRejectionReason.ResultCorrelationMismatch, timings)
        }

        val safetyContext = request.toSafetyContext()
        val safety = evaluateSafety(action, safetyContext)
            ?: return rejected(
                request.requestId,
                CommandRejectionReason.InternalFailure(RuntimeStage.SAFETY),
                timings,
            )
        return when (safety) {
            is CommandSafetyDecision.Deny ->
                rejected(request.requestId, CommandRejectionReason.SafetyDenied(safety.reason), timings)
            CommandSafetyDecision.Allow -> {
                if (request.dryRun) dryRun(action, timings) else execute(action, request.sessionId, timings)
            }
            is CommandSafetyDecision.RequireConfirmation -> {
                if (request.dryRun) {
                    dryRun(action, timings)
                } else {
                    requestConfirmation(action, safetyContext, safety, timings)
                }
            }
        }
    }

    override suspend fun confirmAndExecute(request: CommandConfirmationRequest): CommandDispatchResult {
        if (!config.enabled) return rejected(request.requestId, CommandRejectionReason.RuntimeDisabled)

        val consumed = try {
            confirmationStore.consume(request.token, request.requestId, request.sessionId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return rejected(request.requestId, CommandRejectionReason.InternalFailure(RuntimeStage.CONFIRMATION))
        }
        val pending = when (consumed) {
            is ConfirmationConsumeResult.Accepted -> consumed.pending
            is ConfirmationConsumeResult.Rejected ->
                return rejected(
                    request.requestId,
                    CommandRejectionReason.ConfirmationRejected(consumed.reason),
                )
        }
        val safety = evaluateSafety(pending.action, pending.safetyContext)
            ?: return rejected(request.requestId, CommandRejectionReason.InternalFailure(RuntimeStage.SAFETY))
        return when (safety) {
            is CommandSafetyDecision.Deny ->
                rejected(request.requestId, CommandRejectionReason.SafetyDenied(safety.reason))
            CommandSafetyDecision.Allow,
            is CommandSafetyDecision.RequireConfirmation,
            -> execute(pending.action, pending.sessionId)
        }
    }

    override suspend fun cancelConfirmation(
        request: CommandConfirmationRequest,
    ): CommandConfirmationCancellationResult {
        if (!config.enabled) return CommandConfirmationCancellationResult.RuntimeDisabled

        return try {
            when (val consumed = confirmationStore.consume(request.token, request.requestId, request.sessionId)) {
                is ConfirmationConsumeResult.Accepted -> CommandConfirmationCancellationResult.Cancelled
                is ConfirmationConsumeResult.Rejected ->
                    CommandConfirmationCancellationResult.Rejected(consumed.reason)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            CommandConfirmationCancellationResult.InternalFailure
        }
    }

    private suspend fun requestConfirmation(
        action: MindAction,
        context: CommandSafetyContext,
        decision: CommandSafetyDecision.RequireConfirmation,
        timings: CommandDispatchTimings,
    ): CommandDispatchResult {
        val sessionId = context.sessionId
            ?: return rejected(context.requestId, CommandRejectionReason.ConfirmationRequiresSession, timings)
        val issued = try {
            confirmationStore.issue(context.requestId, sessionId, action, context, decision.ttlMs)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return rejected(
                context.requestId,
                CommandRejectionReason.InternalFailure(RuntimeStage.CONFIRMATION),
                timings,
            )
        }
        return when (issued) {
            is ConfirmationIssueResult.Issued -> CommandDispatchResult.NeedsConfirmation(
                requestId = context.requestId,
                actionType = action.actionType,
                prompt = decision.prompt,
                token = issued.pending.token,
                expiresAtMillis = issued.pending.expiresAtMillis,
                timings = timings,
            )
            ConfirmationIssueResult.TokenCollision ->
                rejected(
                    context.requestId,
                    CommandRejectionReason.InternalFailure(RuntimeStage.CONFIRMATION),
                    timings,
                )
        }
    }

    private suspend fun execute(
        action: MindAction,
        sessionId: String?,
        timings: CommandDispatchTimings = CommandDispatchTimings(),
    ): CommandDispatchResult {
        val executionStartedAt = clock.nowMillis()
        val result = try {
            executor.execute(action, ExecutionContext(sessionId = sessionId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return rejected(
                action.requestId.value,
                CommandRejectionReason.InternalFailure(RuntimeStage.EXECUTION),
                timings.copy(executionMs = elapsed(executionStartedAt)),
            )
        }
        val completedTimings = timings.copy(executionMs = elapsed(executionStartedAt))
        return when (result) {
            is ExecutionResult.Executed ->
                CommandDispatchResult.Executed(action.requestId.value, action.actionType, result, completedTimings)
            is ExecutionResult.Failed,
            is ExecutionResult.Rejected,
            -> CommandDispatchResult.ExecutionFailed(
                action.requestId.value,
                action.actionType,
                result,
                completedTimings,
            )
        }
    }

    private fun dryRun(action: MindAction, timings: CommandDispatchTimings): CommandDispatchResult.DryRun =
        CommandDispatchResult.DryRun(
            requestId = action.requestId.value,
            actionType = action.actionType,
            summary = "Command is valid and would be submitted to the executor.",
            timings = timings,
        )

    private fun TextCommandRequest.toSafetyContext(): CommandSafetyContext =
        CommandSafetyContext(
            requestId = requestId,
            sessionId = sessionId,
            localeTag = localeTag,
            source = source,
            abuseContext = abuseContext.withSessionFallback(sessionId),
        )

    private fun RouterAbuseContextDto.withSessionFallback(sessionId: String?): RouterAbuseContextDto =
        if (sessionHash != null || sessionId == null) this else copy(sessionHash = sessionId)

    private fun CommandSource.toRouterSource(): RouterCommandSourceDto = when (this) {
        CommandSource.TYPED_TEXT -> RouterCommandSourceDto.TYPED_TEXT
        CommandSource.VOICE_FINAL_TRANSCRIPT -> RouterCommandSourceDto.VOICE_BATCH
        CommandSource.SHORTCUT -> RouterCommandSourceDto.SHORTCUT
        CommandSource.DEEP_LINK -> RouterCommandSourceDto.DEEP_LINK
        CommandSource.NOTIFICATION -> RouterCommandSourceDto.NOTIFICATION
    }

    private fun evaluateSafety(action: MindAction, context: CommandSafetyContext): CommandSafetyDecision? =
        try {
            safetyPolicy.evaluate(action, context)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

    private fun rejected(
        requestId: String,
        reason: CommandRejectionReason,
        timings: CommandDispatchTimings = CommandDispatchTimings(),
    ) = CommandDispatchResult.Rejected(requestId, reason, timings)

    private fun elapsed(startedAt: Long): Long = (clock.nowMillis() - startedAt).coerceAtLeast(0L)
}
