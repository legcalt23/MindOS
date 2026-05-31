package com.mindos.commandrouter.core

import com.mindos.commandrouter.actions.CommandId
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.CorrelationId

data class CommandInput(
    val rawText: String,
    val source: CommandSource = CommandSource.UserInput,
    val commandId: CommandId? = null,
    val correlationId: CorrelationId? = null,
    val locale: String = "en"
)
