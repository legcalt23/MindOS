package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionIdFactory
import com.mindos.commandrouter.actions.ActionMetadata
import com.mindos.commandrouter.actions.ActionType
import com.mindos.commandrouter.actions.CommandSource
import com.mindos.commandrouter.actions.CorrelationId
import com.mindos.commandrouter.actions.TimestampProvider

class ActionFactory(
    private val idFactory: ActionIdFactory,
    private val timestampProvider: TimestampProvider
) {
    fun metadata(
        type: ActionType,
        source: CommandSource = CommandSource.UserInput,
        correlationId: CorrelationId? = null
    ): ActionMetadata = ActionMetadata(
        actionId = idFactory.create(),
        actionType = type,
        correlationId = correlationId,
        source = source,
        timestamp = timestampProvider.nowMillis()
    )

    fun nextId(): ActionId = idFactory.create()
}
