package com.mindos.action.decompose

import kotlinx.serialization.Serializable

/**
 * Decompose child-stack configurations — the serializable navigation state of the [RootComponent].
 * Mirrors [com.mindos.action.navigation.MindNavigationCommand] but is a Decompose-internal concern;
 * it must be `@Serializable` for state preservation. Resolved ids only (no raw user references).
 */
@Serializable
sealed interface Config {

    @Serializable
    data object Settings : Config

    @Serializable
    data object Messages : Config

    @Serializable
    data class Profile(val userId: String) : Config

    @Serializable
    data class Story(val storyId: String) : Config

    @Serializable
    data class Chat(val chatId: String) : Config
}
