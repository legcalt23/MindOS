package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.data.ProfileCardAppearanceCodec
import com.mindos.app.features.profile.card.data.ProfileCardDocCodec
import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.GradientKind
import com.mindos.app.features.profile.card.domain.ProfileCardAppearance
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileRank
import com.mindos.app.features.profile.card.domain.RankDivision
import com.mindos.app.features.profile.card.domain.FlagOrTeam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileCardDtoSerializationTest {

    private fun appearance() = ProfileCardAppearance(
        accentArgb = 0xFFE8C77AL,
        screenBackground = BackgroundPaint.Gradient(listOf(0xFF1B2746L, 0xFF05070DL), GradientKind.RADIAL_TOP),
        cardBackground = BackgroundPaint.Solid(0xFF0E1426L),
        pegatinaIndex = 1,
        insigniaOn = true,
        flagOrTeam = FlagOrTeam.TEAM,
        level = 47,
        rank = ProfileRank.DIAMANTE,
        division = RankDivision.III,
    )

    private fun media() = ProfileCardMedia(
        id = "m123",
        kind = ProfileCardMediaKind.GIF,
        storagePath = "profile_cards_public/u1/media/m123.gif",
        downloadUrl = "https://test.local/m123",
        mimeType = "image/gif",
        width = 320,
        height = 200,
        sizeBytes = 4096L,
        createdAtMillis = 1700000000000L,
    )

    @Test
    fun docV2_roundTrips_withAppearanceMediaAndCanvas() {
        val doc = ProfileCardDoc(appearance = appearance(), backgroundMedia = media(), canvas = emptyList())
        val decoded = ProfileCardDocCodec.decode(ProfileCardDocCodec.SCHEMA_VERSION, ProfileCardDocCodec.encode(doc))
        assertEquals(doc, decoded)
    }

    @Test
    fun legacyV1Blob_migratesTo_v2DocWithJustAppearance() {
        val legacyPayload = ProfileCardAppearanceCodec.encode(appearance()) // v1 appearance-only blob
        val migrated = ProfileCardDocCodec.decode(schemaVersion = 1, payload = legacyPayload)
        assertEquals(ProfileCardDoc(appearance = appearance()), migrated)
        assertNull(migrated?.backgroundMedia)
        assertTrue(migrated?.canvas?.isEmpty() == true)
    }

    @Test
    fun encodedDoc_isMetadataOnly_carriesStoragePathNotBytes() {
        val payload = ProfileCardDocCodec.encode(
            ProfileCardDoc(appearance = appearance(), backgroundMedia = media()),
        )
        assertTrue(payload.contains("profile_cards_public/u1/media/m123.gif"))
        // Metadata is tiny; a base64 image would be orders of magnitude larger.
        assertTrue(payload.length < 2000, "payload should be metadata, not bytes (was ${payload.length})")
    }

    @Test
    fun decode_nullOrGarbage_returnsNull() {
        assertNull(ProfileCardDocCodec.decode(2, null))
        assertNull(ProfileCardDocCodec.decode(2, ""))
        assertNull(ProfileCardDocCodec.decode(2, "not json"))
    }
}
