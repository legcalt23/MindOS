package com.mindos.commandrouter.actions

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ActionId(val value: String)

@Serializable
@JvmInline
value class ActionType(val value: String)

@Serializable
@JvmInline
value class CommandId(val value: String)

@Serializable
@JvmInline
value class CorrelationId(val value: String)
