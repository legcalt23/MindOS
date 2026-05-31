package com.example.mindos.features.profile.presentation.audio

/**
 * The raw output of a finished recording, before it is turned into a saved
 * [com.example.mindos.features.profile.domain.model.ProfileAudioClip]. The
 * waveform is computed by the state holder from the amplitude samples it polled
 * during capture (see AudioWaveform), not carried here.
 */
class ProfileAudioCapture(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMillis: Long,
)
