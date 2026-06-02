package com.mindos.action.execution

import com.mindos.action.entity.DeterministicConfidence
import com.mindos.action.entity.EntityType
import com.mindos.action.entity.ExtractedEntity
import com.mindos.action.model.MindAction
import com.mindos.action.understanding.MindIntent
import com.mindos.action.understanding.MindIntentId
import com.mindos.action.understanding.SlotName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntentToActionMapperTest {

    private val mapper = IntentToActionMapper(counterRequestIds())

    private fun entity(value: String) =
        ExtractedEntity(EntityType.Username, value, value, 1, 2, DeterministicConfidence.HIGH)

    private fun intent(id: String) = MindIntent(MindIntentId(id))

    @Test
    fun mapsOpenProfileWithTargetEntity() {
        val action = mapper.map(intent("open_profile"), mapOf(SlotName("target") to entity("ana")))
        assertTrue(action is MindAction.OpenProfile)
        assertEquals("ana", (action as MindAction.OpenProfile).userRef)
    }

    @Test
    fun mapsOpenStoryOfUserWithTargetEntity() {
        val action = mapper.map(intent("open_story_of_user"), mapOf(SlotName("target") to entity("ana")))
        assertTrue(action is MindAction.OpenStoryOfUser)
        assertEquals("ana", (action as MindAction.OpenStoryOfUser).userRef)
    }

    @Test
    fun mapsReferencelessIntent() {
        assertTrue(mapper.map(intent("open_settings"), emptyMap()) is MindAction.OpenSettings)
    }

    @Test
    fun returnsNullWhenRequiredEntityMissing() {
        assertNull(mapper.map(intent("open_profile"), emptyMap()))
    }

    @Test
    fun returnsNullForUnknownIntent() {
        assertNull(mapper.map(intent("florble"), emptyMap()))
    }

    @Test
    fun requestIdsAreUniquePerMapping() {
        val a = mapper.map(intent("open_settings"), emptyMap()) as MindAction.OpenSettings
        val b = mapper.map(intent("open_settings"), emptyMap()) as MindAction.OpenSettings
        assertTrue(a.requestId != b.requestId)
    }
}
