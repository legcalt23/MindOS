package com.mindos.action.execution

import com.mindos.action.matching.DefaultActionMatcher
import com.mindos.action.matching.RegistryCompiler
import com.mindos.action.model.ReferenceKind
import com.mindos.action.pattern.registrySource
import com.mindos.action.resolver.InMemoryReferenceResolver
import com.mindos.action.resolver.ResolverRegistry
import com.mindos.action.validation.DefaultActionValidator
import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.testkit.CommandGrammar
import com.mindos.testkit.CommandTemplate

/** Fixtures wiring the full ZIP02→ZIP03 pipeline for reliability suites. Intents here all map+resolve. */
internal object ExecReliability {

    val source = registrySource {
        exact("p_settings", "open settings", intent = "open_settings")
        alias("inbox", locale = "und", intent = "open_messages")
        parametric("p_profile", "open {target:username}", intent = "open_profile")
        parametric("p_story", "open story {storyId:story_id}", intent = "open_story")
        parametric("p_story_of_user_es", "abre la historia de {target:username}", intent = "open_story_of_user")
    }

    val matcher = DefaultActionMatcher(RegistryCompiler.compile(source).getOrThrow())

    val resolvers = ResolverRegistry(
        listOf(
            InMemoryReferenceResolver(
                ReferenceKind.USERNAME_TO_USER_ID,
                mapOf("ana" to "u_ana", "bob" to "u_bob", "carol" to "u_carol", "dave" to "u_dave", "emma" to "u_emma"),
            ),
            InMemoryReferenceResolver(
                ReferenceKind.STORY_SLUG_TO_STORY_ID,
                mapOf("abc123" to "st_abc", "st_42" to "st_42x", "xyz789" to "st_xyz", "id0001" to "st_0001"),
            ),
            InMemoryReferenceResolver(
                ReferenceKind.CURRENT_STORY_BY_USER_ID,
                mapOf("u_ana" to "st_ana_current", "u_bob" to "st_bob_current"),
            ),
        ),
    )

    val grammar = CommandGrammar(
        templates = listOf(
            CommandTemplate("open_settings", "open settings"),
            CommandTemplate("open_profile", "open {target}"),
            CommandTemplate("open_story", "open story {storyId}"),
            CommandTemplate("open_story_of_user", "abre la historia de {target}"),
        ),
        slots = mapOf(
            "target" to listOf("ana", "bob", "carol", "dave", "emma"),
            "storyId" to listOf("abc123", "st_42", "xyz789", "id0001"),
        ),
    )

    val aliases = listOf("inbox" to "open_messages")

    fun executor(coordinator: NavigationCoordinator) =
        DefaultActionExecutor(DefaultActionValidator(), resolvers, coordinator)
}
