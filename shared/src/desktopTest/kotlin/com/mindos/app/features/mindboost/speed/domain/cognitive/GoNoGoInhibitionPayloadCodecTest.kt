package com.mindos.app.features.mindboost.speed.domain.cognitive

import com.mindos.app.features.mindboost.core.session.cognitive.ExerciseSpecificMetricPayload
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4 · The E8 payload codec roundtrips losslessly and is FAIL-CLOSED: a wrong type, an unsupported
 * version, or a missing field is rejected (thrown / invalid), never silently defaulted.
 */
class GoNoGoInhibitionPayloadCodecTest {

    private fun sample() = GoNoGoInhibitionPayload(
        goTrials = 20,
        noGoTrials = 12,
        goCorrect = 18,
        noGoCorrect = 10,
        commissions = 2,
        omissions = 1,
        anticipations = 1,
        lateResponses = 1,
        fastCommissions = 1,
        lostInputTaps = 3,
        category = "STRONG_CONTROL",
        timingIntegrityLevel = "CLEAN",
        commissionCapApplied = false,
        goAccuracyCapApplied = false,
        anticipationCapApplied = false,
        lowConfidence = false,
    )

    @Test
    fun `encode then decode roundtrips the payload`() {
        val encoded = GoNoGoInhibitionPayloadCodec.encode(sample())
        assertEquals("gonogo_inhibition", encoded.type)
        assertEquals(1, encoded.schemaVersion)
        assertEquals(sample(), GoNoGoInhibitionPayloadCodec.decode(encoded))
    }

    @Test
    fun `validate accepts a well-formed payload`() {
        assertTrue(GoNoGoInhibitionPayloadCodec.validate(GoNoGoInhibitionPayloadCodec.encode(sample())))
    }

    @Test
    fun `decode rejects a payload of the wrong type`() {
        val wrongType = GoNoGoInhibitionPayloadCodec.encode(sample()).copy(type = "something_else")
        assertFalse(GoNoGoInhibitionPayloadCodec.validate(wrongType))
        assertFailsWith<IllegalArgumentException> { GoNoGoInhibitionPayloadCodec.decode(wrongType) }
    }

    @Test
    fun `decode rejects an unsupported schema version`() {
        val newer = GoNoGoInhibitionPayloadCodec.encode(sample()).copy(schemaVersion = 2)
        assertFalse(GoNoGoInhibitionPayloadCodec.validate(newer))
        assertFailsWith<IllegalArgumentException> { GoNoGoInhibitionPayloadCodec.decode(newer) }
    }

    @Test
    fun `a payload missing a required field is invalid and fails to decode`() {
        val incomplete = ExerciseSpecificMetricPayload(
            type = "gonogo_inhibition",
            schemaVersion = 1,
            payload = buildJsonObject {
                put("goTrials", 20)
                // every other required key omitted
            },
        )
        assertFalse(GoNoGoInhibitionPayloadCodec.validate(incomplete))
        assertFailsWith<IllegalStateException> { GoNoGoInhibitionPayloadCodec.decode(incomplete) }
    }
}
