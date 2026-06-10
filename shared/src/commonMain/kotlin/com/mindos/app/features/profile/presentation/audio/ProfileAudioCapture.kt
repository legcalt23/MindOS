package com.mindos.app.features.profile.presentation.audio

/**
 * The raw output of a finished recording, before it is turned into a saved
 * [com.mindos.app.features.profile.domain.model.ProfileAudioClip]. The
 * waveform is computed by the state holder from the amplitude samples it polled
 * during capture (see AudioWaveform), not carried here.
 */
class ProfileAudioCapture(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMillis: Long,
)
