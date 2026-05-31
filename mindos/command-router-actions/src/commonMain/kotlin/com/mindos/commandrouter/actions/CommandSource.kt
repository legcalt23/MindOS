package com.mindos.commandrouter.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface CommandSource {
    @Serializable @SerialName("user_input")
    data object UserInput : CommandSource

    @Serializable @SerialName("api_call")
    data object ApiCall : CommandSource

    @Serializable @SerialName("system")
    data object System : CommandSource

    @Serializable @SerialName("test")
    data object Test : CommandSource
}
