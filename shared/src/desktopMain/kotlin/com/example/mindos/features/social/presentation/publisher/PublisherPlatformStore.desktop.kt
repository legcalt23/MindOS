package com.example.mindos.features.social.presentation.publisher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.mindos.features.social.domain.valueobject.UserId
import java.io.File

@Composable
actual fun rememberPublisherLocalDraftStore(namespace: String): PublisherLocalDraftStore =
    remember(namespace) {
        FilePublisherLocalDraftStore(
            directory = File(
                File(System.getProperty("user.home"), ".neu"),
                "publisher/${namespace.sanitizeStoreName()}",
            ),
        )
    }

private class FilePublisherLocalDraftStore(
    private val directory: File,
) : PublisherLocalDraftStore {
    override suspend fun load(actorId: UserId): PublisherDraftSnapshot? {
        val file = fileFor(actorId)
        return if (file.exists()) {
            publisherDraftSnapshotFromPersistedString(file.readText())
        } else {
            null
        }
    }

    override suspend fun save(snapshot: PublisherDraftSnapshot) {
        directory.mkdirs()
        fileFor(snapshot.actorId).writeText(snapshot.toPersistedString())
    }

    override suspend fun clear(actorId: UserId) {
        fileFor(actorId).delete()
    }

    private fun fileFor(actorId: UserId): File =
        File(directory, "${actorId.value.sanitizeStoreName()}.draft")
}

private fun String.sanitizeStoreName(): String =
    lowercase()
        .map { char -> if (char.isLetterOrDigit() || char == '_' || char == '-') char else '_' }
        .joinToString("")
        .ifBlank { "default" }
