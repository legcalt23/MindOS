package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.domain.ProfileCardMediaPolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileCardMediaPolicyTest {

    @Test
    fun acceptsSupportedImageAndGifWithinLimit() {
        assertNull(ProfileCardMediaPolicy.rejectionReason("image/jpeg", 1024))
        assertNull(ProfileCardMediaPolicy.rejectionReason("image/png", 1024))
        assertNull(ProfileCardMediaPolicy.rejectionReason("image/webp", 1024))
        assertNull(ProfileCardMediaPolicy.rejectionReason("image/gif", ProfileCardMediaPolicy.MAX_BYTES))
        assertTrue(ProfileCardMediaPolicy.isAcceptable("image/jpeg", 500))
    }

    @Test
    fun rejectsUnsupportedMimeType() {
        assertNotNull(ProfileCardMediaPolicy.rejectionReason("application/pdf", 1024))
        assertNotNull(ProfileCardMediaPolicy.rejectionReason("video/mp4", 1024))
        assertFalse(ProfileCardMediaPolicy.isAcceptable("text/plain", 10))
    }

    @Test
    fun rejectsEmptyAndOversized() {
        assertNotNull(ProfileCardMediaPolicy.rejectionReason("image/jpeg", 0))
        assertNotNull(ProfileCardMediaPolicy.rejectionReason("image/jpeg", ProfileCardMediaPolicy.MAX_BYTES + 1))
    }
}
