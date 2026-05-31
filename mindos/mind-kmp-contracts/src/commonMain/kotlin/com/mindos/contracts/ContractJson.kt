package com.mindos.contracts

import kotlinx.serialization.json.Json

object ContractJson {
    val Default: Json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encodeAction(action: RouterActionDto): String =
        Default.encodeToString(RouterActionDto.serializer(), action)

    fun decodeAction(payload: String): RouterActionDto =
        Default.decodeFromString(RouterActionDto.serializer(), payload)

    fun encodeResult(result: RouterResultDto): String =
        Default.encodeToString(RouterResultDto.serializer(), result)

    fun decodeResult(payload: String): RouterResultDto =
        Default.decodeFromString(RouterResultDto.serializer(), payload)
}
