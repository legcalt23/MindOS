package com.mindos.action.navigation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MindNavigationSerializationTest {

    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
    }

    @Test
    fun openStoryNavigationCommandHasStableGoldenJson() {
        val payload = json.encodeToString(MindNavigationCommand.serializer(), MindNavigationCommand.OpenStory("st_1"))

        assertEquals("""{"type":"open_story","storyId":"st_1"}""", payload)
        assertEquals(
            MindNavigationCommand.OpenStory("st_1"),
            json.decodeFromString(MindNavigationCommand.serializer(), payload),
        )
    }
}
