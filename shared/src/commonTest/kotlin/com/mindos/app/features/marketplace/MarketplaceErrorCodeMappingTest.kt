package com.mindos.app.features.marketplace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketplaceErrorCodeMappingTest {

    @Test
    fun testErrorCodeMessagesAreNotBlank() {
        for (code in MarketplaceErrorCode.values()) {
            assertTrue(code.technicalMessage.isNotBlank(), "Technical message blank for $code")
            assertTrue(code.userMessageEs.isNotBlank(), "Spanish message blank for $code")
            assertTrue(code.userMessageEn.isNotBlank(), "English message blank for $code")
        }
    }

    @Test
    fun testErrorCodeRetryProperties() {
        assertTrue(MarketplaceErrorCode.RATE_LIMITED.isRetryable, "RATE_LIMITED should be retryable")
        assertTrue(MarketplaceErrorCode.OFFLINE_UNSUPPORTED.isRetryable, "OFFLINE_UNSUPPORTED should be retryable")
        assertTrue(!MarketplaceErrorCode.AUTH_REQUIRED.isRetryable, "AUTH_REQUIRED should not be retryable")
    }
}
