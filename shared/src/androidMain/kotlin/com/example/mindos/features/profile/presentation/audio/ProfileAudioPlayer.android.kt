package com.example.mindos.features.profile.presentation.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.mindos.features.profile.domain.model.ProfileAudioClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Real Android player backed by [MediaPlayer]. Plays freshly-recorded local
 * bytes by staging them in the cache dir. Remote (reference/URL) playback is a
 * documented TODO that arrives with the upload backend.
 */
private class AndroidProfileAudioPlayer(
    private val appContext: Context,
    private val onError: (String) -> Unit,
) : ProfileAudioPlayer {

    override val isSupported: Boolean = true

    private var player: MediaPlayer? = null
    private var stagedFile: File? = null

    override suspend fun play(clip: ProfileAudioClip): Boolean {
        val bytes = clip.localBytes
        if (bytes == null || bytes.isEmpty()) {
            onError("Este audio aún no está disponible para reproducir.")
            return false
        }
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("mindos_profile_play", ".m4a", appContext.cacheDir).apply {
                writeBytes(bytes)
            }
        }
        stagedFile = file
        return suspendCancellableCoroutine { continuation ->
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            mediaPlayer.setOnCompletionListener {
                cleanup()
                if (continuation.isActive) continuation.resume(true)
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                cleanup()
                if (continuation.isActive) continuation.resume(false)
                true
            }
            val started = runCatching {
                mediaPlayer.setDataSource(file.absolutePath)
                mediaPlayer.prepare()
                mediaPlayer.start()
            }.isSuccess
            if (!started) {
                onError("No se pudo reproducir el audio.")
                cleanup()
                if (continuation.isActive) continuation.resume(false)
            }
            continuation.invokeOnCancellation { cleanup() }
        }
    }

    override fun stop() = cleanup()

    private fun cleanup() {
        player?.let { active ->
            runCatching { if (active.isPlaying) active.stop() }
            active.release()
        }
        player = null
        stagedFile?.delete()
        stagedFile = null
    }
}

@Composable
actual fun rememberProfileAudioPlayer(onError: (String) -> Unit): ProfileAudioPlayer {
    val appContext = LocalContext.current.applicationContext
    val currentOnError by rememberUpdatedState(onError)
    return remember(appContext) {
        AndroidProfileAudioPlayer(appContext) { currentOnError(it) }
    }
}
