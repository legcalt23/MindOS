package com.mindos.action.decompose

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.navigation.NavigationCoordinator
import com.mindos.action.navigation.NavigationOutcome

/** Maps a resolved navigation command to a Decompose child-stack config. */
internal fun MindNavigationCommand.toConfig(): Config = when (this) {
    is MindNavigationCommand.OpenProfile -> Config.Profile(userId)
    is MindNavigationCommand.OpenStory -> Config.Story(storyId)
    is MindNavigationCommand.OpenChat -> Config.Chat(chatId)
    MindNavigationCommand.OpenMessages -> Config.Messages
    MindNavigationCommand.OpenSettings -> Config.Settings
}

/**
 * The **only navigation entry point** (ZIP10): consumes a typed [MindNavigationCommand] from the
 * executor and performs the Decompose child-stack transition. Implements the engine-level
 * [NavigationCoordinator] interface, so the executor depends on the abstraction, not on Decompose.
 *
 * `pushNew` is idempotent on the top entry (re-navigating to the current screen is a no-op), which
 * keeps repeated commands safe.
 */
class DecomposeNavigationCoordinator(
    private val navigation: StackNavigation<Config>,
) : NavigationCoordinator {

    override suspend fun navigate(command: MindNavigationCommand): NavigationOutcome {
        navigation.pushNew(command.toConfig())
        return NavigationOutcome.Success
    }
}
