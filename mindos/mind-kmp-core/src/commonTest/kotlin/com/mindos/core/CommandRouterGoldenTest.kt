package com.mindos.core

import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.NoMatchFailureDto
import com.mindos.contracts.OpenMessagesActionDto
import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.OpenSettingsActionDto
import com.mindos.contracts.OpenStoryOfUserActionDto
import com.mindos.contracts.RouterFailureResultDto
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterSuccessDto
import com.mindos.contracts.UnsafeInputFailureDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandRouterGoldenTest {

    @Test
    fun stableEnglishNavigationCommands() = runTest {
        val router = CommandRouterImpl()

        assertEquals(
            OpenSettingsActionDto("golden-en-settings"),
            assertIs<RouterSuccessDto>(
                router.route(RouterRequestDto("golden-en-settings", "open settings", localeTag = "en")),
            ).action,
        )
        assertEquals(
            OpenMessagesActionDto("golden-en-messages"),
            assertIs<RouterSuccessDto>(
                router.route(RouterRequestDto("golden-en-messages", "open messages", localeTag = "en")),
            ).action,
        )
        assertEquals(
            OpenProfileActionDto("golden-en-profile", "ana"),
            assertIs<RouterSuccessDto>(
                router.route(RouterRequestDto("golden-en-profile", "open @Ana", localeTag = "en")),
            ).action,
        )
    }

    @Test
    fun stableSpanishNavigationCommands() = runTest {
        val router = CommandRouterImpl()

        assertEquals(
            OpenSettingsActionDto("golden-es-settings"),
            assertIs<RouterSuccessDto>(
                router.route(RouterRequestDto("golden-es-settings", "ajustes", localeTag = "es")),
            ).action,
        )
        assertEquals(
            OpenStoryOfUserActionDto("golden-es-story", "marta"),
            assertIs<RouterSuccessDto>(
                router.route(RouterRequestDto("golden-es-story", "abre la historia de @Marta", localeTag = "es")),
            ).action,
        )
    }

    @Test
    fun noMatchAmbiguityAndUnsafeInputRemainTypedFailures() = runTest {
        val noMatch = CommandRouterImpl().route(RouterRequestDto("golden-no-match", "florble"))
        assertIs<NoMatchFailureDto>(assertIs<RouterFailureResultDto>(noMatch).reason)

        val ambiguousRouter = CommandRouterImpl(
            matcher = PatternMatcher {
                CoreMatchResult.Ambiguous(listOf("open_profile", "open_chat"))
            },
        )
        val ambiguous = ambiguousRouter.route(RouterRequestDto("golden-ambiguous", "open ana"))
        assertIs<AmbiguousFailureDto>(assertIs<RouterFailureResultDto>(ambiguous).reason)

        val unsafe = CommandRouterImpl().route(RouterRequestDto("golden-unsafe", "open\u0001settings"))
        assertIs<UnsafeInputFailureDto>(assertIs<RouterFailureResultDto>(unsafe).reason)
    }

    @Test
    fun terminalAsrPunctuationCurrentBehaviorIsNoMatch() = runTest {
        val router = CommandRouterImpl()

        listOf("open settings.", "open settings?", "open settings!").forEachIndexed { index, input ->
            val result = router.route(RouterRequestDto("golden-punctuation-$index", input, localeTag = "en"))
            assertIs<NoMatchFailureDto>(assertIs<RouterFailureResultDto>(result).reason)
        }
    }
}
