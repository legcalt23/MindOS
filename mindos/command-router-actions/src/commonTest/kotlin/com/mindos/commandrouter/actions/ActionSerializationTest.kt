package com.mindos.commandrouter.actions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionSerializationTest {

    private val json = Json { encodeDefaults = true }

    private fun metadata(type: ActionType) = ActionMetadata(
        actionId = ActionId("test-id"),
        actionType = type,
        source = CommandSource.Test,
        timestamp = 1_000L
    )

    @Test
    fun navigateAction_roundTrip() {
        val action: CommandAction = NavigateAction(
            actionId = ActionId("nav-1"),
            metadata = metadata(ActionTypes.Navigate),
            targetId = "settings",
            parameters = mapOf("tab" to "privacy")
        )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CommandAction>(encoded)
        assertEquals(action, decoded)
    }

    @Test
    fun noOpAction_roundTrip() {
        val action: CommandAction = NoOpAction(
            actionId = ActionId("noop-1"),
            metadata = metadata(ActionTypes.NoOp)
        )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CommandAction>(encoded)
        assertEquals(action, decoded)
    }

    @Test
    fun unknownAction_roundTrip() {
        val action: CommandAction = UnknownAction(
            actionId = ActionId("unk-1"),
            metadata = metadata(ActionTypes.Unknown),
            rawInput = "blorp zorp"
        )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CommandAction>(encoded)
        assertEquals(action, decoded)
    }

    @Test
    fun systemAction_roundTrip() {
        val action: CommandAction = SystemAction(
            actionId = ActionId("sys-1"),
            metadata = metadata(ActionTypes.System),
            operation = "reload",
            payload = mapOf("force" to "true")
        )
        val encoded = json.encodeToString(action)
        val decoded = json.decodeFromString<CommandAction>(encoded)
        assertEquals(action, decoded)
    }

    @Test
    fun commandSource_roundTrip() {
        listOf(
            CommandSource.UserInput,
            CommandSource.ApiCall,
            CommandSource.System,
            CommandSource.Test
        ).forEach { source ->
            val encoded = json.encodeToString<CommandSource>(source)
            val decoded = json.decodeFromString<CommandSource>(encoded)
            assertEquals(source, decoded)
        }
    }

    @Test
    fun actionId_valueClass_serializesAsString() {
        val id = ActionId("abc-123")
        val encoded = json.encodeToString(id)
        assertEquals("\"abc-123\"", encoded)
    }

    @Test
    fun actionMetadata_equalityIsDeterministic() {
        val m1 = metadata(ActionTypes.Navigate)
        val m2 = metadata(ActionTypes.Navigate)
        assertEquals(m1, m2)
    }

    @Test
    fun navigateAction_emptyParameters_defaultsToEmpty() {
        val action = NavigateAction(
            actionId = ActionId("nav-2"),
            metadata = metadata(ActionTypes.Navigate),
            targetId = "home"
        )
        assertEquals(emptyMap(), action.parameters)
        val encoded = json.encodeToString<CommandAction>(action)
        val decoded = json.decodeFromString<CommandAction>(encoded)
        assertEquals(action, decoded)
    }
}
