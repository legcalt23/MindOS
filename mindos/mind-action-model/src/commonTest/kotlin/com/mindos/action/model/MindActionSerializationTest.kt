package com.mindos.action.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MindActionSerializationTest {

    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
    }

    @Test
    fun storyOfUserActionHasStableVersionedGoldenJson() {
        val envelope = VersionedMindActionEnvelope(
            action = MindAction.OpenStoryOfUser(RequestId("req-1"), userRef = "marta"),
        )

        val payload = json.encodeToString(VersionedMindActionEnvelope.serializer(), envelope)

        assertEquals(
            """{"schemaVersion":1,"action":{"type":"open_story_of_user","requestId":"req-1","userRef":"marta"}}""",
            payload,
        )
        assertEquals(envelope, json.decodeFromString(VersionedMindActionEnvelope.serializer(), payload))
    }
}
