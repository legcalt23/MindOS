package com.mindos.action.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A typed, **post-resolution** navigation command carrying canonical ids only. Produced by the
 * executor, consumed by the [NavigationCoordinator]. This is the Decompose-facing vocabulary —
 * but the module does NOT depend on Decompose, so it stays KMP-pure and UI-agnostic.
 */
@Serializable
sealed interface MindNavigationCommand {
    val commandType: String

    @Serializable @SerialName("open_profile")
    data class OpenProfile(val userId: String) : MindNavigationCommand {
        override val commandType: String get() = "open_profile"
    }

    @Serializable @SerialName("open_story")
    data class OpenStory(val storyId: String) : MindNavigationCommand {
        override val commandType: String get() = "open_story"
    }

    @Serializable @SerialName("open_chat")
    data class OpenChat(val chatId: String) : MindNavigationCommand {
        override val commandType: String get() = "open_chat"
    }

    @Serializable @SerialName("open_messages")
    data object OpenMessages : MindNavigationCommand {
        override val commandType: String get() = "open_messages"
    }

    @Serializable @SerialName("open_settings")
    data object OpenSettings : MindNavigationCommand {
        override val commandType: String get() = "open_settings"
    }
}
