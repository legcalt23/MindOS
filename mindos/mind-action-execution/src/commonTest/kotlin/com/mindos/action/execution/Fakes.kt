package com.mindos.action.execution

import com.mindos.action.model.ReferenceKind
import com.mindos.action.model.RequestId
import com.mindos.action.model.RequestIdFactory
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.action.navigation.NavigationOutcome
import com.mindos.action.resolver.InMemoryReferenceResolver
import com.mindos.action.resolver.ResolverRegistry

internal class FakeCoordinator(
    private val outcome: NavigationOutcome = NavigationOutcome.Success,
) : NavigationCoordinator {
    var calls = 0
    var lastCommand: MindNavigationCommand? = null
    override suspend fun navigate(command: MindNavigationCommand): NavigationOutcome {
        calls++
        lastCommand = command
        return outcome
    }
}

internal fun counterRequestIds(): RequestIdFactory {
    var n = 0
    return RequestIdFactory { RequestId("req-${n++}") }
}

internal fun sampleResolvers(): ResolverRegistry = ResolverRegistry(
    listOf(
        InMemoryReferenceResolver(ReferenceKind.USERNAME_TO_USER_ID, mapOf("ana" to "u_ana", "bob" to "u_bob")),
        InMemoryReferenceResolver(ReferenceKind.STORY_SLUG_TO_STORY_ID, mapOf("my-story" to "st_1")),
        InMemoryReferenceResolver(ReferenceKind.CHAT_ALIAS_TO_CHAT_ID, mapOf("team" to "ch_1")),
    ),
)
