package com.mindos.core

import com.mindos.contracts.InputTooLongFailureDto
import com.mindos.contracts.NoMatchFailureDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.OpenStoryOfUserActionDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterAbuseContextDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.contracts.UnsafeInputFailureDto
import com.mindos.action.observability.RecordingAuditSink
import com.mindos.security.InMemoryAbuseGuard
import com.mindos.security.AbuseAction
import com.mindos.security.AbuseScope
import com.mindos.security.RateLimitPolicy
import com.mindos.security.SecurityClock
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CommandRouterImplTest {

    @Test
    fun routesDefaultParametricProfileCommand() = runTest {
        val router = CommandRouterImpl()

        val result = router.route(RouterRequestDto("req-1", "open @Ana"))

        val success = assertIs<RouterSuccessDto>(result)
        val action = assertIs<OpenProfileActionDto>(success.action)
        assertEquals("ana", action.username)
    }

    @Test
    fun routesLocalizedAlias() = runTest {
        val router = CommandRouterImpl()

        val result = router.route(RouterRequestDto("req-2", "ajustes", localeTag = "es"))

        assertIs<OpenSettingsActionDto>(assertIs<RouterSuccessDto>(result).action)
    }

    @Test
    fun regionalSpanishLocaleFallsBackToSpanishNaturalAliases() = runTest {
        val router = CommandRouterImpl()

        listOf("ajustes", "abre ajustes", "abrir ajustes", "configuración", "ir a ajustes")
            .forEachIndexed { index, input ->
                val result = router.route(RouterRequestDto("req-regional-$index", input, localeTag = "es-US"))
                assertIs<OpenSettingsActionDto>(assertIs<RouterSuccessDto>(result).action)
            }
    }

    @Test
    fun routerPropagatesOpaqueAbuseContextToMatcherGuard() = runTest {
        val guard = InMemoryAbuseGuard(
            ratePolicies = listOf(
                RateLimitPolicy(
                    name = "voice-session",
                    scope = AbuseScope.SESSION,
                    maxCommands = 1,
                    windowMillis = 60_000L,
                    action = AbuseAction.THROTTLE,
                ),
            ),
            clock = SecurityClock { 1L },
        )
        val router = CommandRouterImpl(matcher = PatternMatcherImpl(abuseGuard = guard))
        val context = RouterAbuseContextDto(sessionHash = "opaque-session")

        assertIs<RouterSuccessDto>(
            router.route(RouterRequestDto("req-abuse-1", "settings", abuseContext = context)),
        )
        assertIs<UnsafeInputFailureDto>(
            assertIs<RouterFailureResultDto>(
                router.route(RouterRequestDto("req-abuse-2", "settings", abuseContext = context)),
            ).reason,
        )
    }

    @Test
    fun routesLocaleAgnosticAliasFallback() = runTest {
        val router = CommandRouterImpl()

        val result = router.route(RouterRequestDto("req-3", "home", localeTag = "es"))

        assertIs<OpenMessagesActionDto>(assertIs<RouterSuccessDto>(result).action)
    }

    @Test
    fun unknownCommandReturnsTypedFailure() = runTest {
        val router = CommandRouterImpl()

        val result = router.route(RouterRequestDto("req-4", "florble"))

        assertIs<NoMatchFailureDto>(assertIs<RouterFailureResultDto>(result).reason)
    }

    @Test
    fun unsafeInputReturnsTypedFailureBeforeMatching() = runTest {
        val router = CommandRouterImpl()

        val result = router.route(RouterRequestDto("req-5", "open\u0001settings"))

        assertIs<UnsafeInputFailureDto>(assertIs<RouterFailureResultDto>(result).reason)
    }

    @Test
    fun maxLengthReturnsTypedFailure() = runTest {
        val matcher = PatternMatcherImpl(config = CoreRouterConfig(maxInputLength = 3))
        val router = CommandRouterImpl(matcher = matcher)

        val result = router.route(RouterRequestDto("req-6", "open settings"))

        assertIs<InputTooLongFailureDto>(assertIs<RouterFailureResultDto>(result).reason)
    }

    @Test
    fun routesStoryOfUserCommandAsTypedContractAction() = runTest {
        val router = CommandRouterImpl()

        val result = router.route(RouterRequestDto("req-story-user", "abre la historia de @Marta", localeTag = "es"))

        val action = assertIs<OpenStoryOfUserActionDto>(assertIs<RouterSuccessDto>(result).action)
        assertEquals("marta", action.username)
    }

    @Test
    fun dtoSecurityGateRejectsHomoglyphEntityAfterMatching() = runTest {
        val router = CommandRouterImpl(
            matcher = PatternMatcher {
                CoreMatchResult.Matched("open_profile", mapOf("target" to "m\u0430rta"))
            },
        )

        val result = router.route(RouterRequestDto("req-spoof", "open profile"))

        assertIs<UnsafeInputFailureDto>(assertIs<RouterFailureResultDto>(result).reason)
    }

    @Test
    fun productionFactoryRejectsNoOpRuntimeWiring() {
        assertFailsWith<IllegalArgumentException> {
            CommandRouterFactory.forProduction(
                CommandRouterRuntimeConfig(environment = CommandRouterEnvironment.TEST),
            )
        }
    }

    @Test
    fun productionFactoryAllowsExplicitSecureRuntimeWiring() = runTest {
        val clock = SecurityClock { 1L }
        val router = CommandRouterFactory.forProduction(
            CommandRouterRuntimeConfig(
                environment = CommandRouterEnvironment.TEST,
                abuseGuard = InMemoryAbuseGuard(clock = clock),
                audit = RecordingAuditSink(),
                clock = clock,
                safetyPolicy = ProductionSafetyPolicy(
                    metricsExplicitlyDisabled = true,
                    logsExplicitlyDisabled = true,
                    telemetryExplicitlyDisabled = true,
                ),
            ),
        )

        assertEquals(OpenSettingsActionDto("req-prod"), assertIs<RouterSuccessDto>(router.route(RouterRequestDto("req-prod", "settings"))).action)
    }

    @Test
    fun routeAllMapsFlowOfRequests() = runTest {
        val router = CommandRouterImpl()
        val requests = listOf(
            RouterRequestDto("req-7", "settings"),
            RouterRequestDto("req-8", "unknown"),
        ).asFlow()

        val results = router.routeAll(requests).toList()

        assertIs<RouterSuccessDto>(results[0])
        assertIs<RouterFailureResultDto>(results[1])
    }
}
