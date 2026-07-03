package com.mindos.app.runtime

import com.mindos.action.execution.ActionExecutor
import com.mindos.action.execution.ExecutionContext
import com.mindos.action.execution.ExecutionResult
import com.mindos.action.execution.ExecutionClocks
import com.mindos.action.execution.InMemoryIdempotencyStore
import com.mindos.action.model.MindAction
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.action.navigation.NavigationOutcome
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.NoOpAuditSink
import com.mindos.action.observability.NoOpMetricSink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.observability.NoOpStructuredLogSink
import com.mindos.action.resolver.ResolverRegistry
import com.mindos.action.validation.DefaultActionValidator
import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.CommandRejectionReason
import com.mindos.command.runtime.CommandRuntimeClock
import com.mindos.command.runtime.ConfirmationClock
import com.mindos.command.runtime.ConfirmationToken
import com.mindos.command.runtime.ConfirmationTokenFactory
import com.mindos.command.runtime.DefaultCommandSafetyPolicy
import com.mindos.command.runtime.InMemoryConfirmationStore
import com.mindos.command.runtime.TextCommandRequest
import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterResultDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.security.NoOpAbuseGuard
import com.mindos.security.SecurityClocks
import com.mindos.speech.VoiceCommandOrchestrator
import com.mindos.speech.VoiceCommandOutcome
import com.mindos.speech.VoiceSession
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindCommandRuntimeBuilderTest {

    @Test
    fun appFacingApiBuildsWithFakesAndUsesExplicitCoordinator() = runTest {
        val coordinator = RecordingCoordinator()
        val executor = RecordingExecutor()
        var observedCoordinator: NavigationCoordinator? = null
        val builder = testingBuilder(
            coordinator = coordinator,
            executorFactory = MindActionExecutorFactory {
                observedCoordinator = it
                executor
            },
        )

        val runtime = assertIs<MindCommandRuntimeBuildResult.Ready>(
            builder.build(MindCommandRuntimeConfig(enabled = true)),
        ).runtime
        val result = runtime.textGateway.routeAndExecute(TextCommandRequest("settings", "req"))

        assertIs<CommandDispatchResult.Executed>(result)
        assertTrue(observedCoordinator === coordinator)
        assertEquals(1, executor.invocationCount)
        assertNull(runtime.voiceOrchestrator)
    }

    @Test
    fun voiceDisabledDoesNotInitializeSpeech() {
        var voiceInitializations = 0
        val builder = testingBuilder(
            voiceFactory = MindVoiceRuntimeFactory {
                voiceInitializations += 1
                RecordingVoiceOrchestrator
            },
        )

        val runtime = assertIs<MindCommandRuntimeBuildResult.Ready>(
            builder.build(MindCommandRuntimeConfig(enabled = true, voiceEnabled = false)),
        ).runtime

        assertEquals(0, voiceInitializations)
        assertNull(runtime.voiceOrchestrator)
    }

    @Test
    fun disabledRuntimeInitializesNoComponentsAndReturnsTypedDisabledResult() = runTest {
        var executorInitializations = 0
        var voiceInitializations = 0
        val router = RecordingRouter()
        val builder = testingBuilder(
            router = router,
            executorFactory = MindActionExecutorFactory {
                executorInitializations += 1
                RecordingExecutor()
            },
            voiceFactory = MindVoiceRuntimeFactory {
                voiceInitializations += 1
                RecordingVoiceOrchestrator
            },
        )

        val runtime = assertIs<MindCommandRuntimeBuildResult.Ready>(builder.build()).runtime
        val result = assertIs<CommandDispatchResult.Rejected>(
            runtime.textGateway.routeAndExecute(TextCommandRequest("settings", "req")),
        )

        assertIs<CommandRejectionReason.RuntimeDisabled>(result.reason)
        assertEquals(0, router.invocationCount)
        assertEquals(0, executorInitializations)
        assertEquals(0, voiceInitializations)
    }

    @Test
    fun voiceRequiresEnabledRuntimeAndFactory() {
        val disabled = testingBuilder().build(MindCommandRuntimeConfig(voiceEnabled = true))
        assertIs<MindCommandRuntimeBuildError.VoiceRequiresRuntime>(
            assertIs<MindCommandRuntimeBuildResult.Failed>(disabled).error,
        )

        val missingFactory = testingBuilder().build(
            MindCommandRuntimeConfig(enabled = true, voiceEnabled = true),
        )
        assertIs<MindCommandRuntimeBuildError.VoiceFactoryMissing>(
            assertIs<MindCommandRuntimeBuildResult.Failed>(missingFactory).error,
        )
    }

    @Test
    fun productionBuilderRejectsUnsafeWiringWithTypedError() {
        val dependencies = MindCommandProductionDependencies(
            navigationCoordinator = RecordingCoordinator(),
            actionValidator = DefaultActionValidator(),
            resolvers = ResolverRegistry(emptyList()),
            abuseGuard = NoOpAbuseGuard,
            securityClock = SecurityClocks.Zero,
            executionClock = ExecutionClocks.Zero,
            idempotencyStore = InMemoryIdempotencyStore(),
            observability = MindCommandObservability(
                telemetry = NoOpMindTelemetrySink,
                audit = NoOpAuditSink,
                metrics = NoOpMetricSink,
                logs = NoOpStructuredLogSink,
                metricTags = MetricTags.Default,
            ),
            commandSafetyPolicy = DefaultCommandSafetyPolicy(),
            confirmationStore = confirmationStore(),
            commandRuntimeClock = CommandRuntimeClock { 0L },
        )

        val result = MindCommandRuntimeBuilder.forProduction(dependencies)
            .build(MindCommandRuntimeConfig(enabled = true))

        assertIs<MindCommandRuntimeBuildError.UnsafeProductionWiring>(
            assertIs<MindCommandRuntimeBuildResult.Failed>(result).error,
        )
    }

    @Test
    fun voiceFactoryCancellationPropagates() {
        val cancellation = CancellationException("cancel voice initialization")
        val builder = testingBuilder(
            voiceFactory = MindVoiceRuntimeFactory { throw cancellation },
        )
        var observed: CancellationException? = null

        try {
            builder.build(MindCommandRuntimeConfig(enabled = true, voiceEnabled = true))
        } catch (caught: CancellationException) {
            observed = caught
        }

        assertTrue(observed === cancellation)
    }

    private fun testingBuilder(
        coordinator: NavigationCoordinator = RecordingCoordinator(),
        router: RecordingRouter = RecordingRouter(),
        executorFactory: MindActionExecutorFactory = MindActionExecutorFactory { RecordingExecutor() },
        voiceFactory: MindVoiceRuntimeFactory? = null,
    ) = MindCommandRuntimeBuilder.forTesting(
        navigationCoordinator = coordinator,
        router = router,
        executorFactory = executorFactory,
        commandSafetyPolicy = DefaultCommandSafetyPolicy(),
        confirmationStore = confirmationStore(),
        commandRuntimeClock = CommandRuntimeClock { 0L },
        voiceFactory = voiceFactory,
    )

    private fun confirmationStore() = InMemoryConfirmationStore(
        tokenFactory = ConfirmationTokenFactory { ConfirmationToken("token") },
        clock = ConfirmationClock { 0L },
    )
}

private class RecordingRouter : KmpCommandRouter {
    var invocationCount = 0
        private set

    override suspend fun route(request: RouterRequestDto): RouterResultDto {
        invocationCount += 1
        return RouterSuccessDto(request.requestId, OpenSettingsActionDto(request.requestId))
    }
}

private class RecordingExecutor : ActionExecutor {
    var invocationCount = 0
        private set

    override suspend fun execute(action: MindAction, context: ExecutionContext): ExecutionResult {
        invocationCount += 1
        return ExecutionResult.Executed(MindNavigationCommand.OpenSettings)
    }
}

private class RecordingCoordinator : NavigationCoordinator {
    override suspend fun navigate(command: MindNavigationCommand): NavigationOutcome = NavigationOutcome.Success
}

private object RecordingVoiceOrchestrator : VoiceCommandOrchestrator {
    override suspend fun runOnce(session: VoiceSession): VoiceCommandOutcome =
        error("Not used by builder tests")
}
