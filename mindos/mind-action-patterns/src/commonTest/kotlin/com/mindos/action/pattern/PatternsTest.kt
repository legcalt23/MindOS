package com.mindos.action.pattern

import com.mindos.action.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatternsTest {

    @Test
    fun parsesStaticAndTypedSlot() {
        val r = PatternTemplate.parse("open {target:username}")
        assertTrue(r is PatternTemplate.Result.Ok)
        val tokens = (r as PatternTemplate.Result.Ok).tokens
        assertEquals(2, tokens.size)
        assertEquals(PatternToken.Static("open"), tokens[0])
        val slot = (tokens[1] as PatternToken.Slot).slot
        assertEquals(EntityType.Username, slot.acceptedType)
        assertEquals(SlotCardinality.Single, slot.cardinality)
    }

    @Test
    fun parsesGreedyFreeText() {
        val r = PatternTemplate.parse("message {user:username} {text:free_text:greedy}")
        assertTrue(r is PatternTemplate.Result.Ok)
        val tokens = (r as PatternTemplate.Result.Ok).tokens
        val last = (tokens.last() as PatternToken.Slot).slot
        assertEquals(EntityType.FreeText, last.acceptedType)
        assertEquals(SlotCardinality.Greedy, last.cardinality)
    }

    @Test
    fun rejectsUnknownType() {
        assertTrue(PatternTemplate.parse("open {target:wat}") is PatternTemplate.Result.Error)
    }

    @Test
    fun rejectsMalformedSlot() {
        assertTrue(PatternTemplate.parse("open {target") is PatternTemplate.Result.Error)
        assertTrue(PatternTemplate.parse("open {:username}") is PatternTemplate.Result.Error)
    }

    @Test
    fun rejectsEmptyTemplate() {
        assertTrue(PatternTemplate.parse("   ") is PatternTemplate.Result.Error)
    }

    @Test
    fun parametricSpecificityCounts() {
        val tokens = (PatternTemplate.parse("message {user:username} {text:free_text:greedy}") as PatternTemplate.Result.Ok).tokens
        val pattern = ActionPattern.ParametricActionPattern(
            id = PatternId("p"),
            tokens = tokens,
            intentId = com.mindos.action.understanding.MindIntentId("send_message"),
        )
        assertEquals(1, pattern.staticTokenCount)
        assertEquals(1, pattern.typedSlotCount)
        assertEquals(1, pattern.freeTextSlotCount)
        assertEquals(2, pattern.slots.size)
    }

    @Test
    fun builderDslCollectsIntentsPatternsAliases() {
        val source = registrySource {
            exact("p1", "open settings", intent = "open_settings")
            alias("ajustes", locale = "es", intent = "open_settings")
            parametric("p2", "open {target:username}", intent = "open_profile")
        }
        assertEquals(2, source.declaredIntents.size)
        assertEquals(2, source.patterns.size)
        assertEquals(1, source.aliases.size)
        assertEquals(RegistrySchema.Current, source.schema)
    }
}
