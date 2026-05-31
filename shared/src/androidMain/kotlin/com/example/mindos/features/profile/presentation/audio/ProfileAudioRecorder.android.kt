package com.example.mindos.features.profile.presentation.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real Android recorder backed by [MediaRecorder]. The waveform is built by the
 * state holder polling [currentAmplitude] (MediaRecorder.getMaxAmplitude),
 * giving a waveform that reflects the actual recorded loudness — never random.
 */
private class AndroidProfileAudioRecorder(
    private val appContext: Context,
    private val onError: (String) -> Unit,
) : ProfileAudioRecorder {

    override val isSupported: Boolean = true

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMillis: Long = 0L

    override suspend fun start() {
        withContext(Dispatchers.IO) {
            cancel()
            val file = File.createTempFile("mindos_profile_audio", ".m4a", appContext.cacheDir)
            val newRecorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(BIT_RATE)
                setAudioSamplingRate(SAMPLE_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            outputFile = file
            startedAtMillis = SystemClock.elapsedRealtime()
        }
    }

    override fun currentAmplitude(): Float {
        val active = recorder ?: return 0f
        return runCatching { active.maxAmplitude }
            .getOrDefault(0)
            .coerceAtLeast(0)
            .toFloat()
            .div(MAX_PCM_AMPLITUDE)
            .coerceIn(0f, 1f)
    }

    override suspend fun stop(): ProfileAudioCapture? = withContext(Dispatchers.IO) {
        val active = recorder ?: return@withContext null
        val durationMillis = SystemClock.elapsedRealtime() - startedAtMillis
        val captured = runCatching {
            active.stop()
            true
        }.getOrElse { error ->
            onError(error.message ?: "No se pudo finalizar la grabación.")
            false
        }
        active.release()
        recorder = null
        val file = outputFile
        outputFile = null
        if (!captured || file == null) {
            file?.delete()
            return@withContext null
        }
        val bytes = runCatching { file.readBytes() }.getOrDefault(ByteArray(0))
        file.delete()
        if (bytes.isEmpty()) null else ProfileAudioCapture(bytes, "audio/mp4", durationMillis)
    }

    override fun cancel() {
        recorder?.let { active ->
            runCatching { active.stop() }
            active.release()
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }

    private companion object {
        const val BIT_RATE = 96_000
        const val SAMPLE_RATE = 44_100
        const val MAX_PCM_AMPLITUDE = 32_767f
    }
}

@Composable
actual fun rememberProfileAudioRecorder(onError: (String) -> Unit): ProfileAudioRecorder {
    val appContext = LocalContext.current.applicationContext
    val currentOnError by rememberUpdatedState(onError)
    return remember(appContext) {
        AndroidProfileAudioRecorder(appContext) { currentOnError(it) }
    }
}
