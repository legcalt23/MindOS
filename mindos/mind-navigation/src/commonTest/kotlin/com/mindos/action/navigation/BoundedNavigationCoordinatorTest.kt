package com.mindos.action.navigation

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BoundedNavigationCoordinatorTest {

    @Test
    fun singletonDestinationsDoNotStackRepeatedTopEntries() = runTest {
        val delegate = RecordingCoordinator()
        val coordinator = BoundedNavigationCoordinator(delegate)

        coordinator.navigate(MindNavigationCommand.OpenSettings)
        coordinator.navigate(MindNavigationCommand.OpenSettings)

        assertEquals(1, delegate.commands.size)
        assertEquals(listOf(MindNavigationCommand.OpenSettings), coordinator.snapshot())
    }

    @Test
    fun rejectWhenFullKeepsStackBounded() = runTest {
        val delegate = RecordingCoordinator()
        val coordinator = BoundedNavigationCoordinator(
            delegate,
            NavigationPolicy(maxDepth = 2, overflowPolicy = StackOverflowPolicy.REJECT_WHEN_FULL),
        )

        coordinator.navigate(MindNavigationCommand.OpenProfile("u1"))
        coordinator.navigate(MindNavigationCommand.OpenProfile("u2"))
        coordinator.navigate(MindNavigationCommand.OpenProfile("u3"))

        assertEquals(2, delegate.commands.size)
        assertEquals(2, coordinator.snapshot().size)
    }

    @Test
    fun dropOldestPolicyKeepsLatestCommands() = runTest {
        val delegate = RecordingCoordinator()
        val coordinator = BoundedNavigationCoordinator(
            delegate,
            NavigationPolicy(maxDepth = 2, overflowPolicy = StackOverflowPolicy.DROP_OLDEST_WHEN_FULL),
        )

        coordinator.navigate(MindNavigationCommand.OpenProfile("u1"))
        coordinator.navigate(MindNavigationCommand.OpenProfile("u2"))
        coordinator.navigate(MindNavigationCommand.OpenProfile("u3"))

        assertEquals(3, delegate.commands.size)
        assertEquals(
            listOf(MindNavigationCommand.OpenProfile("u2"), MindNavigationCommand.OpenProfile("u3")),
            coordinator.snapshot(),
        )
    }

    private class RecordingCoordinator : NavigationCoordinator {
        val commands = ArrayList<MindNavigationCommand>()

        override suspend fun navigate(command: MindNavigationCommand): NavigationOutcome {
            commands += command
            return NavigationOutcome.Success
        }
    }
}
