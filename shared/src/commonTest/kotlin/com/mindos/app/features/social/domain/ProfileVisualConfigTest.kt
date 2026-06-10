package com.mindos.app.features.social.domain

import com.mindos.app.features.social.domain.model.ProfileLayoutPreset
import com.mindos.app.features.social.domain.model.ProfileLayoutSlot
import com.mindos.app.features.social.domain.model.ProfileMediaMeta
import com.mindos.app.features.social.domain.model.ProfileMediaUpload
import com.mindos.app.features.social.domain.model.ProfileMediaUploadKind
import com.mindos.app.features.social.domain.model.ProfileModuleId
import com.mindos.app.features.social.domain.model.ProfileVideoMaxDurationMillis
import com.mindos.app.features.social.domain.model.ProfileVisualConfig
import com.mindos.app.features.social.data.mapper.firestoreMapToProfileVisualConfig
import com.mindos.app.features.social.data.mapper.toClientCreateFirestoreMap
import com.mindos.app.features.social.data.mapper.toClientWritableFirestoreMap
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileVisualConfigTest {
    @Test
    fun defaultConfigHasSemanticThreeModuleLayout() {
        val config = defaultConfig()

        assertEquals(3, config.modules.size)
        assertEquals(ProfileLayoutPreset.LargeLeft, config.layoutPreset)
        assertEquals(ProfileLayoutSlot.LargeLeft, config.module(ProfileModuleId.Main).slot)
        assertEquals(ProfileLayoutSlot.SmallTopRight, config.module(ProfileModuleId.SmallOne).slot)
        assertEquals(ProfileLayoutSlot.SmallBottomRight, config.module(ProfileModuleId.SmallTwo).slot)
    }

    @Test
    fun draggingMainToRightMovesSmallModulesToLeft() {
        val moved = defaultConfig().moveModuleToSlot(ProfileModuleId.Main, ProfileLayoutSlot.LargeRight)

        assertEquals(ProfileLayoutPreset.LargeRight, moved.layoutPreset)
        assertEquals(ProfileLayoutSlot.LargeRight, moved.module(ProfileModuleId.Main).slot)
        assertEquals(ProfileLayoutSlot.SmallTopLeft, moved.module(ProfileModuleId.SmallOne).slot)
        assertEquals(ProfileLayoutSlot.SmallBottomLeft, moved.module(ProfileModuleId.SmallTwo).slot)
    }

    @Test
    fun draggingSmallToOppositeSideSnapsWithoutOverlap() {
        val moved = defaultConfig().moveModuleToSlot(ProfileModuleId.SmallTwo, ProfileLayoutSlot.SmallTopLeft)

        assertEquals(ProfileLayoutPreset.LargeRight, moved.layoutPreset)
        assertEquals(ProfileLayoutSlot.LargeRight, moved.module(ProfileModuleId.Main).slot)
        assertEquals(ProfileLayoutSlot.SmallTopLeft, moved.module(ProfileModuleId.SmallTwo).slot)
        assertEquals(ProfileLayoutSlot.SmallBottomLeft, moved.module(ProfileModuleId.SmallOne).slot)
    }

    @Test
    fun videoDurationMustBeStrictlyUnderFifteenSeconds() {
        val valid = videoUpload(duration = ProfileVideoMaxDurationMillis - 1)
        val invalid = videoUpload(duration = ProfileVideoMaxDurationMillis)
        val zeroDuration = videoUpload(duration = 0L)

        assertNull(valid.validationError())
        assertNotNull(invalid.validationError())
        assertNotNull(zeroDuration.validationError())
    }

    @Test
    fun firestoreUpdatePayloadExcludesImmutableOwnerAndCounters() {
        val config = defaultConfig().withSavedMedia(
            moduleId = ProfileModuleId.Main,
            media = ProfileMediaMeta(
                url = "https://example.invalid/profile_visuals/firebase_uid/main/2_media.jpg",
                storagePath = "profile_visuals/firebase_uid/main/2_media.jpg",
                thumbnailStoragePath = "profile_visuals/firebase_uid/main/2_thumb.jpg",
                mimeType = "image/jpeg",
                uploadedAt = Timestamp(2L),
            ),
        )

        val payload = config.toClientWritableFirestoreMap()

        assertFalse(payload.containsKey("userId"))
        assertFalse(payload.containsKey("observersCount"))
        assertFalse(payload.containsKey("createdAt"))
        assertEquals("owner", payload["username"])
        assertEquals("profile_visuals/firebase_uid/main/2_media.jpg", payload["mainProfilePhotoUrl"])
        assertTrue(payload.containsKey("updatedAt"))
        val module = (payload.getValue("modules") as List<*>).first() as Map<*, *>
        val media = module["media"] as Map<*, *>
        assertEquals("https://example.invalid/profile_visuals/firebase_uid/main/2_media.jpg", media["url"])
        assertEquals("profile_visuals/firebase_uid/main/2_media.jpg", media["storagePath"])
    }

    @Test
    fun firestoreCreatePayloadIncludesFullSchemaAndStartsCountersAtZero() {
        val payload = defaultConfig()
            .copy(userphrase = "Perfil publico")
            .toClientCreateFirestoreMap()

        assertEquals(1, payload["schemaVersion"])
        assertEquals("owner", payload["userId"])
        assertEquals("owner", payload["username"])
        assertEquals("Perfil publico", payload["userphrase"])
        assertEquals(true, payload["searchable"])
        assertEquals(0, payload["observersCount"])
        assertEquals(1, payload["layoutVersion"])
        assertTrue(payload.containsKey("createdAt"))
        assertTrue(payload.containsKey("updatedAt"))
    }

    @Test
    fun firestoreMapFallsBackToExplicitMainProfilePhotoField() {
        val config = firestoreMapToProfileVisualConfig(
            raw = mapOf(
                "userId" to "owner",
                "username" to "owner",
                "mainProfilePhotoUrl" to "profile_visuals/owner/main/5_media.jpg",
            ),
            fallbackUserId = UserId("owner"),
        )

        assertEquals("profile_visuals/owner/main/5_media.jpg", config?.mainProfilePhotoUrl)
        assertEquals("profile_visuals/owner/main/5_media.jpg", config?.module(ProfileModuleId.Main)?.media?.storagePath)
    }

    @Test
    fun missingIdentityFallsBackToStableUserDerivedValues() {
        val config = firestoreMapToProfileVisualConfig(
            raw = emptyMap(),
            fallbackUserId = UserId("curro_uid"),
        )

        assertEquals("curro_uid", config?.username)
        assertEquals("curro_uid", config?.displayName)
    }

    @Test
    fun firestoreUpdatePayloadPreservesSearchableVisibilityFlag() {
        val payload = defaultConfig()
            .copy(searchable = false)
            .toClientWritableFirestoreMap()

        assertEquals(false, payload["searchable"])
    }

    @Test
    fun firestoreCreatePayloadSanitizesInvalidFallbackUsername() {
        val config = ProfileVisualConfig.createDefault(
            userId = UserId("firebase_uid"),
            displayName = "Owner",
            username = "FirebaseUid-WithUppercase-And-Too-Long",
            observersCount = 7,
            now = Timestamp(1L),
        )

        val payload = config.toClientCreateFirestoreMap()

        assertEquals("firebaseuid_withuppercase_and", payload["username"])
        assertEquals(0, payload["observersCount"])
    }

    @Test
    fun profileTextNormalizationRejectsControlPayloadsAndKeepsNaturalSqlLikeText() {
        val config = defaultConfig()
            .copy(
                displayName = "  Ada   Lovelace\n",
                userphrase = "I can write ' OR '1'='1 safely",
            )
            .withTextModule(ProfileModuleId.SmallOne, "Linea 1\r\n\r\n\r\nLinea 2")
            .normalized()

        assertEquals("Ada Lovelace", config.displayName)
        assertEquals("I can write ' OR '1'='1 safely", config.userphrase)
        assertEquals("Linea 1\n\nLinea 2", config.module(ProfileModuleId.SmallOne).text)
    }

    @Test
    fun mediaValidationRejectsUnexpectedMimeTypesAndOversizedImages() {
        val invalidMime = ProfileMediaUpload(
            kind = ProfileMediaUploadKind.Photo,
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "image/gif",
        )
        val unsupportedVideo = ProfileMediaUpload(
            kind = ProfileMediaUploadKind.Video,
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "video/webm",
            durationMillis = 1000L,
        )
        val oversizedImage = ProfileMediaUpload(
            kind = ProfileMediaUploadKind.Photo,
            bytes = ByteArray((10 * 1024 * 1024) + 1),
            mimeType = "image/jpeg",
        )

        assertNotNull(invalidMime.validationError())
        assertNotNull(unsupportedVideo.validationError())
        assertNotNull(oversizedImage.validationError())
    }

    private fun defaultConfig(): ProfileVisualConfig =
        ProfileVisualConfig.createDefault(
            userId = UserId("owner"),
            displayName = "Owner",
            username = "owner",
            observersCount = 12,
            now = Timestamp(1L),
        )

    private fun videoUpload(duration: Long): ProfileMediaUpload =
        ProfileMediaUpload(
            kind = ProfileMediaUploadKind.Video,
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "video/mp4",
            durationMillis = duration,
        )
}
