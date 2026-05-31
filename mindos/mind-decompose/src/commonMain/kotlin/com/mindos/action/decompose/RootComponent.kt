package com.mindos.action.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.mindos.action.navigation.NavigationCoordinator

/**
 * Root of the Decompose component tree (ZIP10). Owns the child [stack] and exposes the single
 * [coordinator] through which all navigation flows. The app renders [stack]; the executor drives
 * [coordinator]. No UI here — pure navigation structure.
 */
interface RootComponent {
    val stack: Value<ChildStack<Config, RootChild>>
    val coordinator: NavigationCoordinator
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    initialConfig: Config = Config.Settings,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<Config, RootChild>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = initialConfig,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override val coordinator: NavigationCoordinator = DecomposeNavigationCoordinator(navigation)

    private fun createChild(config: Config, context: ComponentContext): RootChild = when (config) {
        is Config.Profile -> RootChild.Profile(DefaultProfileComponent(config.userId))
        is Config.Story -> RootChild.Story(DefaultStoryComponent(config.storyId))
        is Config.Chat -> RootChild.Chat(DefaultChatComponent(config.chatId))
        Config.Messages -> RootChild.Messages(DefaultMessagesComponent())
        Config.Settings -> RootChild.Settings(DefaultSettingsComponent())
    }
}
