package com.mindos.app.features.profile.domain

import com.mindos.app.features.profile.domain.audio.ProfileFormatting
import com.mindos.app.features.profile.domain.model.DonationProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileFormattingTest {

    @Test
    fun compactFormatsThousandsWithCommaDecimal() {
        assertEquals("0", ProfileFormatting.compact(0))
        assertEquals("999", ProfileFormatting.compact(999))
        assertEquals("1K", ProfileFormatting.compact(1_000))
        assertEquals("1,7K", ProfileFormatting.compact(1_700))
        assertEquals("10K", ProfileFormatting.compact(10_000))
        assertEquals("1,7M", ProfileFormatting.compact(1_700_000))
    }

    @Test
    fun donationLabelMatchesDesign() {
        assertEquals("0/10K€", ProfileFormatting.donationLabel(DonationProgress()))
        assertEquals(
            "5K/10K€",
            ProfileFormatting.donationLabel(DonationProgress(currentAmount = 5_000)),
        )
    }

    @Test
    fun observerLabelUsesObservadores() {
        assertEquals("1,7K observadores", ProfileFormatting.observerLabel(1_700))
        assertEquals("0 observadores", ProfileFormatting.observerLabel(0))
    }

    @Test
    fun donationFractionIsClamped() {
        assertEquals(0f, DonationProgress(currentAmount = 0).fraction)
        assertEquals(0.5f, DonationProgress(currentAmount = 5_000).fraction)
        assertEquals(1f, DonationProgress(currentAmount = 99_999).fraction)
    }
}
