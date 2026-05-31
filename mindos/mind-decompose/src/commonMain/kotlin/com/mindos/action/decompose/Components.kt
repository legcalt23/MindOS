package com.mindos.action.decompose

/**
 * Decompose component contracts (ZIP10 COMPONENTS). Each screen is modelled as a component
 * interface; the [RootComponent] hosts them in a child stack. UI-agnostic — these carry navigation
 * data only, no Compose/Android types, so the boundary "executor never knows Compose" holds.
 */
interface ProfileComponent {
    val userId: String
}

interface StoryComponent {
    val storyId: String
}

interface ChatComponent {
    val chatId: String
}

interface MessagesComponent

interface SettingsComponent

internal class DefaultProfileComponent(override val userId: String) : ProfileComponent
internal class DefaultStoryComponent(override val storyId: String) : StoryComponent
internal class DefaultChatComponent(override val chatId: String) : ChatComponent
internal class DefaultMessagesComponent : MessagesComponent
internal class DefaultSettingsComponent : SettingsComponent

/** The active child of the [RootComponent]'s stack. */
sealed interface RootChild {
    data class Profile(val component: ProfileComponent) : RootChild
    data class Story(val component: StoryComponent) : RootChild
    data class Chat(val component: ChatComponent) : RootChild
    data class Messages(val component: MessagesComponent) : RootChild
    data class Settings(val component: SettingsComponent) : RootChild
}
