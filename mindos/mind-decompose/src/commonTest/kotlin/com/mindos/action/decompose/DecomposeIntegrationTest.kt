package com.mindos.action.decompose

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.mindos.action.execution.DefaultActionExecutor
import com.mindos.action.execution.ExecutionResult
import com.mindos.action.execution.IntentToActionMapper
import com.mindos.action.matching.DefaultActionMatcher
import com.mindos.action.matching.RegistryCompiler
import com.mindos.action.model.MindAction
import com.mindos.action.model.ReferenceKind
import com.mindos.action.model.RequestId
import com.mindos.action.model.RequestIdFactory
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.pattern.registrySource
import com.mindos.action.resolver.InMemoryReferenceResolver
import com.mindos.action.resolver.ResolverRegistry
import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.validation.DefaultActionValidator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Headless Decompose integration tests (ZIP10): Action→NavigationCommand, NavigationCommand→
 * Component, DeepLink→Action, and Resolver→Navigation — the full chain text → understanding →
 * execution → coordinator → Decompose child stack, with no UI.
 */
class DecomposeIntegrationTest {

    private fun ids(): RequestIdFactory {
        var n = 0
        return RequestIdFactory { RequestId("dl-${n++}") }
    }

    private fun newRoot(initial: Config = Config.Settings): DefaultRootComponent {
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle)
        val root = DefaultRootComponent(context, initial)
        lifecycle.resume()
        return root
    }

    private fun usernameResolvers() =
        ResolverRegistry(listOf(InMemoryReferenceResolver(ReferenceKind.USERNAME_TO_USER_ID, mapOf("ana" to "u_ana"))))

    @Test
    fun initialChildIsSettings() {
        assertTrue(newRoot().stack.value.active.instance is RootChild.Settings)
    }

    @Test
    fun navigationCommandMapsToProfileComponent() = runTest {
        val root = newRoot()
        root.coordinator.navigate(MindNavigationCommand.OpenProfile("u_ana"))
        val active = root.stack.value.active.instance
        assertTrue(active is RootChild.Profile)
        assertEquals("u_ana", (active as RootChild.Profile).component.userId)
    }

    @Test
    fun navigationCommandMapsToStoryAndChatComponents() = runTest {
        val root = newRoot()
        root.coordinator.navigate(MindNavigationCommand.OpenStory("st_1"))
        assertEquals("st_1", (root.stack.value.active.instance as RootChild.Story).component.storyId)
        root.coordinator.navigate(MindNavigationCommand.OpenChat("ch_1"))
        assertEquals("ch_1", (root.stack.value.active.instance as RootChild.Chat).component.chatId)
    }

    @Test
    fun deepLinkConvertsToTypedAction() {
        val bridge = DeepLinkBridge(ids())
        assertEquals("ana", (bridge.resolve("mindos://profile/ana") as MindAction.OpenProfile).userRef)
        assertEquals("xyz", (bridge.resolve("https://mindos.app/story/xyz") as MindAction.OpenStory).storyRef)
        assertEquals("team", (bridge.resolve("mindos://chat/team") as MindAction.OpenChat).chatRef)
        assertTrue(bridge.resolve("mindos://notifications") is MindAction.OpenMessages)
        assertTrue(bridge.resolve("mindos://settings") is MindAction.OpenSettings)
        assertNull(bridge.resolve("garbage"))
        assertNull(bridge.resolve("mindos://profile")) // missing id
    }

    @Test
    fun resolverToNavigationFullPipeline() = runTest {
        val root = newRoot()
        val source = registrySource {
            exact("p_settings", "open settings", intent = "open_settings")
            parametric("p_profile", "open {target:username}", intent = "open_profile")
        }
        val matcher = DefaultActionMatcher(RegistryCompiler.compile(source).getOrThrow())
        val mapper = IntentToActionMapper(ids())
        val executor = DefaultActionExecutor(DefaultActionValidator(), usernameResolvers(), root.coordinator)

        val understood = matcher.understand("open ana") as ActionUnderstandingResult.Understood
        val action = mapper.map(understood.intent, understood.entities)!!
        val result = executor.execute(action)

        assertTrue(result is ExecutionResult.Executed)
        // username "ana" resolved to "u_ana" and pushed as a Profile child.
        assertEquals("u_ana", (root.stack.value.active.instance as RootChild.Profile).component.userId)
    }

    @Test
    fun deepLinkThroughExecutorReachesNavigation() = runTest {
        val root = newRoot()
        val bridge = DeepLinkBridge(ids())
        val executor = DefaultActionExecutor(DefaultActionValidator(), usernameResolvers(), root.coordinator)

        val action = bridge.resolve("mindos://profile/ana")!!
        executor.execute(action)

        assertEquals("u_ana", (root.stack.value.active.instance as RootChild.Profile).component.userId)
    }
}
