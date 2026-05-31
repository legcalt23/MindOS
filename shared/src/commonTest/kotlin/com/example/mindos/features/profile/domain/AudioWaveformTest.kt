package com.example.mindos.features.profile.domain

import com.example.mindos.features.profile.domain.audio.AudioWaveform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioWaveformTest {

    @Test
    fun emptyAmplitudesProduceFlatZeroBars() {
        val bars = AudioWaveform.fromAmplitudes(emptyList(), bars = 8)
        assertEquals(8, bars.size)
        assertTrue(bars.all { it == 0f })
        assertFalse(AudioWaveform.hasSignal(bars))
    }

    @Test
    fun peakNormalizationScalesLoudestBarToOne() {
        val bars = AudioWaveform.fromAmplitudes(listOf(0.1f, 0.5f, 0.25f), bars = 3)
        assertEquals(3, bars.size)
        assertEquals(1f, bars.max())
        assertTrue(AudioWaveform.hasSignal(bars))
    }

    @Test
    fun downsamplesWhenMoreSamplesThanBars() {
        val samples = List(100) { (it % 10) / 10f + 0.01f }
        val bars = AudioWaveform.fromAmplitudes(samples, bars = 10)
        assertEquals(10, bars.size)
        assertTrue(bars.all { it in 0f..1f })
    }

    @Test
    fun upsamplesWhenFewerSamplesThanBars() {
        val bars = AudioWaveform.fromAmplitudes(listOf(0.2f, 0.8f), bars = 6)
        assertEquals(6, bars.size)
        assertEquals(1f, bars.max())
    }

    @Test
    fun negativeAndNaNSamplesAreTreatedAsSilence() {
        val bars = AudioWaveform.fromAmplitudes(listOf(-1f, Float.NaN, 0f), bars = 3)
        assertTrue(bars.all { it == 0f })
    }

    @Test
    fun remainingSecondsCountsDownAndClamps() {
        assertEquals(10, AudioWaveform.remainingSeconds(10_000, 0))
        assertEquals(8, AudioWaveform.remainingSeconds(10_000, 2_500))
        assertEquals(0, AudioWaveform.remainingSeconds(10_000, 10_000))
        assertEquals(0, AudioWaveform.remainingSeconds(10_000, 12_000))
    }

    @Test
    fun durationLabelCeilingClampsToRange() {
        assertEquals(10, AudioWaveform.durationSecondsLabel(9_500, maxSeconds = 10))
        assertEquals(2, AudioWaveform.durationSecondsLabel(1_500, maxSeconds = 10))
        assertEquals(1, AudioWaveform.durationSecondsLabel(0, maxSeconds = 10))
    }
}
