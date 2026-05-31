package com.mindos.core

import com.mindos.contracts.InputTooLongFailureDto
import com.mindos.contracts.NoMatchFailureDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.contracts.UnsafeInputFailureDto
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
