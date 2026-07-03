package com.mindos.app.runtime

import com.mindos.action.execution.ActionExecutionEnvironment
import com.mindos.action.execution.ActionExecutionRuntimeConfig
import com.mindos.action.execution.ActionExecutor
import com.mindos.action.execution.ActionExecutorFactory
import com.mindos.action.execution.ExecutionClock
import com.mindos.action.execution.IdempotencyStore
import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.MetricSink
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.StructuredLogSink
import com.mindos.action.resolver.ResolverRegistry
import com.mindos.action.validation.ActionValidator
import com.mindos.command.runtime.CommandConfirmationRequest
import com.mindos.command.runtime.CommandConfirmationCancellationResult
import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.CommandRejectionReason
import com.mindos.command.runtime.CommandRuntimeClock
import com.mindos.command.runtime.CommandRuntimeConfig
import com.mindos.command.runtime.CommandSafetyPolicy
import com.mindos.command.runtime.ConfirmationStore
import com.mindos.command.runtime.DefaultRouterActionExecutionAdapter
import com.mindos.command.runtime.DefaultTextCommandGateway
import com.mindos.command.runtime.TextCommandGateway
import com.mindos.command.runtime.TextCommandRequest
import com.mindos.contracts.KmpCommandRouter
import com.mindos.core.CommandRouterEnvironment
import com.mindos.core.CommandRouterFactory
import com.mindos.core.CommandRouterRuntimeConfig
import com.mindos.security.AbuseGuard
import com.mindos.security.SecurityClock
import com.mindos.speech.VoiceCommandOrchestrator
import kotlin.coroutines.cancellation.CancellationException

data class MindCommandRuntimeConfig(
    val enabled: Boolean = false,
    val voiceEnabled: Boolean = false,
)

data class MindCommandObservability(
    val telemetry: MindTelemetrySink,
    val audit: AuditSink,
    val metrics: MetricSink,
    val logs: StructuredLogSink,
    val metricTags: MetricTags,
)

data class MindCommandProductionDependencies(
    val navigationCoordinator: NavigationCoordinator,
    val actionValidator: ActionValidator,
    val resolvers: ResolverRegistry,
    val abuseGuard: AbuseGuard,
    val securityClock: SecurityClock,
    val executionClock: ExecutionClock,
    val idempotencyStore: IdempotencyStore,
    val observability: MindCommandObservability,
    val commandSafetyPolicy: CommandSafetyPolicy,
    val confirmationStore: ConfirmationStore,
    val commandRuntimeClock: CommandRuntimeClock,
    val voiceFactory: MindVoiceRuntimeFactory? = null,
)

fun interface MindVoiceRuntimeFactory {
    fun create(textGateway: TextCommandGateway): VoiceCommandOrchestrator
}

fun interface MindActionExecutorFactory {
    fun create(navigationCoordinator: NavigationCoordinator): ActionExecutor
}

interface MindCommandRuntime {
    val textGateway: TextCommandGateway
    val voiceOrchestrator: VoiceCommandOrchestrator?
}

sealed interface MindCommandRuntimeBuildError {
    data object VoiceRequiresRuntime : MindCommandRuntimeBuildError
    data object VoiceFactoryMissing : MindCommandRuntimeBuildError
    data object UnsafeProductionWiring : MindCommandRuntimeBuildError
    data object ComponentInitializationFailed : MindCommandRuntimeBuildError
    data object VoiceInitializationFailed : MindCommandRuntimeBuildError
}

sealed interface MindCommandRuntimeBuildResult {
    data class Ready(val runtime: MindCommandRuntime) : MindCommandRuntimeBuildResult
    data class Failed(val error: MindCommandRuntimeBuildError) : MindCommandRuntimeBuildResult
}

class MindCommandRuntimeBuilder private constructor(
    private val gatewayFactory: () -> TextCommandGateway,
    private val voiceFactory: MindVoiceRuntimeFactory?,
    private val production: Boolean,
) {

    fun build(config: MindCommandRuntimeConfig = MindCommandRuntimeConfig()): MindCommandRuntimeBuildResult {
        if (config.voiceEnabled && !config.enabled) {
            return MindCommandRuntimeBuildResult.Failed(MindCommandRuntimeBuildError.VoiceRequiresRuntime)
        }
        if (!config.enabled) {
            return MindCommandRuntimeBuildResult.Ready(DefaultMindCommandRuntime(DisabledTextCommandGateway, null))
        }

        val gateway = try {
            gatewayFactory()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IllegalArgumentException) {
            val error = if (production) {
                MindCommandRuntimeBuildError.UnsafeProductionWiring
            } else {
                MindCommandRuntimeBuildError.ComponentInitializationFailed
            }
            return MindCommandRuntimeBuildResult.Failed(error)
        } catch (_: Exception) {
            return MindCommandRuntimeBuildResult.Failed(MindCommandRuntimeBuildError.ComponentInitializationFailed)
        }

        if (!config.voiceEnabled) {
            return MindCommandRuntimeBuildResult.Ready(DefaultMindCommandRuntime(gateway, null))
        }
        val factory = voiceFactory
            ?: return MindCommandRuntimeBuildResult.Failed(MindCommandRuntimeBuildError.VoiceFactoryMissing)
        val voice = try {
            factory.create(gateway)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return MindCommandRuntimeBuildResult.Failed(MindCommandRuntimeBuildError.VoiceInitializationFailed)
        }
        return MindCommandRuntimeBuildResult.Ready(DefaultMindCommandRuntime(gateway, voice))
    }

    companion object {
        fun forProduction(dependencies: MindCommandProductionDependencies): MindCommandRuntimeBuilder =
            MindCommandRuntimeBuilder(
                gatewayFactory = {
                    val router = CommandRouterFactory.forProduction(
                        CommandRouterRuntimeConfig(
                            environment = CommandRouterEnvironment.PRODUCTION,
                            abuseGuard = dependencies.abuseGuard,
                            audit = dependencies.observability.audit,
                            clock = dependencies.securityClock,
                            telemetry = dependencies.observability.telemetry,
                            metrics = dependencies.observability.metrics,
                            logs = dependencies.observability.logs,
                            metricTags = dependencies.observability.metricTags,
                        ),
                    )
                    val executor = ActionExecutorFactory.forProduction(
                        ActionExecutionRuntimeConfig(
                            environment = ActionExecutionEnvironment.PRODUCTION,
                            validator = dependencies.actionValidator,
                            resolvers = dependencies.resolvers,
                            coordinator = dependencies.navigationCoordinator,
                            telemetry = dependencies.observability.telemetry,
                            audit = dependencies.observability.audit,
                            clock = dependencies.executionClock,
                            metrics = dependencies.observability.metrics,
                            logs = dependencies.observability.logs,
                            metricTags = dependencies.observability.metricTags,
                            idempotencyStore = dependencies.idempotencyStore,
                        ),
                    )
                    createGateway(
                        router = router,
                        executor = executor,
                        safetyPolicy = dependencies.commandSafetyPolicy,
                        confirmationStore = dependencies.confirmationStore,
                        clock = dependencies.commandRuntimeClock,
                    )
                },
                voiceFactory = dependencies.voiceFactory,
                production = true,
            )

        fun forTesting(
            navigationCoordinator: NavigationCoordinator,
            router: KmpCommandRouter,
            executorFactory: MindActionExecutorFactory,
            commandSafetyPolicy: CommandSafetyPolicy,
            confirmationStore: ConfirmationStore,
            commandRuntimeClock: CommandRuntimeClock,
            voiceFactory: MindVoiceRuntimeFactory? = null,
        ): MindCommandRuntimeBuilder =
            MindCommandRuntimeBuilder(
                gatewayFactory = {
                    createGateway(
                        router = router,
                        executor = executorFactory.create(navigationCoordinator),
                        safetyPolicy = commandSafetyPolicy,
                        confirmationStore = confirmationStore,
                        clock = commandRuntimeClock,
                    )
                },
                voiceFactory = voiceFactory,
                production = false,
            )

        private fun createGateway(
            router: KmpCommandRouter,
            executor: ActionExecutor,
            safetyPolicy: CommandSafetyPolicy,
            confirmationStore: ConfirmationStore,
            clock: CommandRuntimeClock,
        ): TextCommandGateway =
            DefaultTextCommandGateway(
                router = router,
                adapter = DefaultRouterActionExecutionAdapter(),
                executor = executor,
                safetyPolicy = safetyPolicy,
                confirmationStore = confirmationStore,
                config = CommandRuntimeConfig(enabled = true),
                clock = clock,
            )
    }
}

private data class DefaultMindCommandRuntime(
    override val textGateway: TextCommandGateway,
    override val voiceOrchestrator: VoiceCommandOrchestrator?,
) : MindCommandRuntime

private object DisabledTextCommandGateway : TextCommandGateway {
    override suspend fun routeAndExecute(request: TextCommandRequest): CommandDispatchResult =
        CommandDispatchResult.Rejected(request.requestId, CommandRejectionReason.RuntimeDisabled)

    override suspend fun confirmAndExecute(request: CommandConfirmationRequest): CommandDispatchResult =
        CommandDispatchResult.Rejected(request.requestId, CommandRejectionReason.RuntimeDisabled)

    override suspend fun cancelConfirmation(
        request: CommandConfirmationRequest,
    ): CommandConfirmationCancellationResult = CommandConfirmationCancellationResult.RuntimeDisabled
}
