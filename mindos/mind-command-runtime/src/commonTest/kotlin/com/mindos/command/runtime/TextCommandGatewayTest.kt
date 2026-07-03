package com.mindos.command.runtime

import com.mindos.action.execution.ExecutionFailure
import com.mindos.action.execution.ExecutionResult
import com.mindos.action.model.MindAction
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.NoMatchFailureDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterAbuseContextDto
import com.mindos.contracts.RouterCommandSourceDto
import com.mindos.contracts.RouterSuccessDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TextCommandGatewayTest {

    @Test
    fun runtimeIsDisabledByDefaultWithoutCallingRouterOrExecutor() = runTest {
        val router = successRouter()
        val executor = successfulExecutor()
        val gateway = gateway(router, executor, config = CommandRuntimeConfig())

        val result = gateway.routeAndExecute(TextCommandRequest("settings", "req"))

        assertIs<CommandRejectionReason.RuntimeDisabled>(assertIs<CommandDispatchResult.Rejected>(result).reason)
        assertTrue(router.requests.isEmpty())
        assertTrue(executor.actions.isEmpty())
    }

    @Test
    fun routesMapsAndDelegatesExecution() = runTest {
        val router = successRouter()
        val executor = successfulExecutor()

        val result = gateway(router, executor).routeAndExecute(
            TextCommandRequest("settings", "req", sessionId = "session"),
        )

        assertIs<CommandDispatchResult.Executed>(result)
        assertIs<MindAction.OpenSettings>(executor.actions.single())
        assertEquals("session", executor.contexts.single().sessionId)
        val routed = router.requests.single()
        assertEquals(RouterCommandSourceDto.TYPED_TEXT, routed.source)
        assertEquals("session", routed.abuseContext.sessionHash)
    }

    @Test
    fun propagatesExplicitOpaqueAbuseContextToRouter() = runTest {
        val router = successRouter()
        val context = RouterAbuseContextDto(opaqueUserHash = "opaque-user", deviceHash = "opaque-device")

        gateway(router, successfulExecutor()).routeAndExecute(
            TextCommandRequest("settings", "req", sessionId = "session", abuseContext = context),
        )

        assertEquals(
            context.copy(sessionHash = "session"),
            router.requests.single().abuseContext,
        )
    }

    @Test
    fun noMatchAndAmbiguityNeverExecute() = runTest {
        listOf(NoMatchFailureDto, AmbiguousFailureDto(listOf("open_profile", "open_chat"))).forEach { failure ->
            val router = RecordingRouter { request -> RouterFailureResultDto(request.requestId, failure) }
            val executor = successfulExecutor()

            val result = gateway(router, executor).routeAndExecute(TextCommandRequest("unknown", "req"))

            assertIs<CommandDispatchResult.RoutingFailed>(result)
            assertTrue(executor.actions.isEmpty())
        }
    }

    @Test
    fun dryRunNeverCallsExecutorOrIssuesConfirmation() = runTest {
        val router = successRouter()
        val executor = successfulExecutor()
        val denyIssueStore = object : ConfirmationStore {
            override suspend fun issue(
                requestId: String,
                sessionId: String,
                action: MindAction,
                safetyContext: CommandSafetyContext,
                ttlMs: Long,
            ): ConfirmationIssueResult = error("dry-run must not issue confirmation")

            override suspend fun consume(
                token: ConfirmationToken,
                requestId: String,
                sessionId: String,
            ): ConfirmationConsumeResult = error("not used")
        }
        val sensitive = CommandSafetyPolicy { _, _ ->
            CommandSafetyDecision.RequireConfirmation("Confirm.", 1_000L)
        }

        val result = gateway(router, executor, sensitive, denyIssueStore).routeAndExecute(
            TextCommandRequest("settings", "req", sessionId = "session", dryRun = true),
        )

        assertIs<CommandDispatchResult.DryRun>(result)
        assertTrue(executor.actions.isEmpty())
    }

    @Test
    fun safetyDenialNeverCallsExecutor() = runTest {
        val executor = successfulExecutor()
        val safety = CommandSafetyPolicy { _, _ -> CommandSafetyDecision.Deny("blocked") }

        val result = gateway(successRouter(), executor, safety).routeAndExecute(
            TextCommandRequest("settings", "req"),
        )

        assertEquals(
            "blocked",
            assertIs<CommandRejectionReason.SafetyDenied>(
                assertIs<CommandDispatchResult.Rejected>(result).reason,
            ).reason,
        )
        assertTrue(executor.actions.isEmpty())
    }

    @Test
    fun unexpectedSafetyFailureBecomesTypedRejectionWithoutExecution() = runTest {
        val executor = successfulExecutor()
        val safety = CommandSafetyPolicy { _, _ -> error("policy failure") }

        val result = gateway(successRouter(), executor, safety).routeAndExecute(
            TextCommandRequest("settings", "req"),
        )

        assertEquals(
            RuntimeStage.SAFETY,
            assertIs<CommandRejectionReason.InternalFailure>(
                assertIs<CommandDispatchResult.Rejected>(result).reason,
            ).stage,
        )
        assertTrue(executor.actions.isEmpty())
    }

    @Test
    fun unrecoverableRouterErrorPropagatesWithoutExecution() = runTest {
        val fatal = AssertionError("fatal router invariant")
        val router = RecordingRouter { throw fatal }
        val executor = successfulExecutor()
        var observed: AssertionError? = null

        try {
            gateway(router, executor).routeAndExecute(TextCommandRequest("settings", "req"))
        } catch (caught: AssertionError) {
            observed = caught
        }

        assertTrue(observed === fatal)
        assertTrue(executor.actions.isEmpty())
    }

    @Test
    fun sensitiveCommandRequiresConfirmationThenExecutesOnce() = runTest {
        val executor = successfulExecutor()
        val store = testConfirmationStore()
        val safety = CommandSafetyPolicy { _, _ ->
            CommandSafetyDecision.RequireConfirmation("Confirm.", 1_000L)
        }
        val gateway = gateway(successRouter(), executor, safety, store)
        val pending = assertIs<CommandDispatchResult.NeedsConfirmation>(
            gateway.routeAndExecute(TextCommandRequest("settings", "req", sessionId = "session")),
        )
        assertTrue(executor.actions.isEmpty())

        assertIs<CommandDispatchResult.Executed>(
            gateway.confirmAndExecute(CommandConfirmationRequest(pending.token, "req", "session")),
        )
        val replay = assertIs<CommandDispatchResult.Rejected>(
            gateway.confirmAndExecute(CommandConfirmationRequest(pending.token, "req", "session")),
        )

        assertEquals(1, executor.actions.size)
        assertEquals(
            ConfirmationRejectionReason.REPLAY,
            assertIs<CommandRejectionReason.ConfirmationRejected>(replay.reason).reason,
        )
    }

    @Test
    fun cancellingSensitiveCommandConsumesTokenWithoutExecution() = runTest {
        val executor = successfulExecutor()
        val store = testConfirmationStore()
        val safety = CommandSafetyPolicy { _, _ ->
            CommandSafetyDecision.RequireConfirmation("Confirm.", 1_000L)
        }
        val gateway = gateway(successRouter(), executor, safety, store)
        val pending = assertIs<CommandDispatchResult.NeedsConfirmation>(
            gateway.routeAndExecute(TextCommandRequest("settings", "req", sessionId = "session")),
        )

        assertIs<CommandConfirmationCancellationResult.Cancelled>(
            gateway.cancelConfirmation(CommandConfirmationRequest(pending.token, "req", "session")),
        )
        val replay = assertIs<CommandDispatchResult.Rejected>(
            gateway.confirmAndExecute(CommandConfirmationRequest(pending.token, "req", "session")),
        )

        assertTrue(executor.actions.isEmpty())
        assertEquals(
            ConfirmationRejectionReason.REPLAY,
            assertIs<CommandRejectionReason.ConfirmationRejected>(replay.reason).reason,
        )
    }

    @Test
    fun executorFailureRemainsTyped() = runTest {
        val executor = RecordingExecutor { _, _ ->
            ExecutionResult.Failed(ExecutionFailure.NavigationFailed("offline"))
        }

        val result = gateway(successRouter(), executor).routeAndExecute(TextCommandRequest("settings", "req"))

        assertIs<ExecutionResult.Failed>(assertIs<CommandDispatchResult.ExecutionFailed>(result).execution)
    }

    @Test
    fun mismatchedRouterCorrelationNeverExecutes() = runTest {
        val router = RecordingRouter {
            RouterSuccessDto(it.requestId, OpenSettingsActionDto("different-request"))
        }
        val executor = successfulExecutor()

        val result = gateway(router, executor).routeAndExecute(TextCommandRequest("settings", "req"))

        assertIs<CommandRejectionReason.ResultCorrelationMismatch>(
            assertIs<CommandDispatchResult.Rejected>(result).reason,
        )
        assertTrue(executor.actions.isEmpty())
    }

    private fun successRouter() = RecordingRouter {
        RouterSuccessDto(it.requestId, OpenSettingsActionDto(it.requestId))
    }

    private fun successfulExecutor() = RecordingExecutor { _, _ ->
        ExecutionResult.Executed(MindNavigationCommand.OpenSettings)
    }

    private fun gateway(
        router: RecordingRouter,
        executor: RecordingExecutor,
        safety: CommandSafetyPolicy = DefaultCommandSafetyPolicy(),
        store: ConfirmationStore = testConfirmationStore(),
        config: CommandRuntimeConfig = CommandRuntimeConfig(enabled = true),
    ) = DefaultTextCommandGateway(
        router = router,
        adapter = DefaultRouterActionExecutionAdapter(),
        executor = executor,
        safetyPolicy = safety,
        confirmationStore = store,
        config = config,
    )
}
