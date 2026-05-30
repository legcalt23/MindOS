package com.example.mindos.features.social.presentation.publisher

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.mindos.features.social.domain.publishing.PublisherState
import com.example.mindos.features.social.domain.publishing.TraceId
import com.example.mindos.features.social.domain.valueobject.UserId
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Composable
actual fun rememberPublisherLocalDraftStore(namespace: String): PublisherLocalDraftStore {
    val context = LocalContext.current.applicationContext
    return remember(context, namespace) {
        AndroidPublisherLocalDraftStore(context, namespace)
    }
}

private class AndroidPublisherLocalDraftStore(
    private val context: Context,
    private val namespace: String,
) : PublisherLocalDraftStore {
    private val securePreferences: SharedPreferences by lazy {
        context.getSharedPreferences(SecurePreferencesFileName, Context.MODE_PRIVATE)
    }

    private val legacyPreferencesName = "neu_publisher_${namespace.sanitizeStoreName()}"
    private val keyAlias = "${KeystoreAliasPrefix}${namespace.sanitizeStoreName()}"

    override suspend fun load(actorId: UserId): PublisherDraftSnapshot? {
        migrateLegacyDraftIfPresent(actorId)
        val key = secureEntryKey(actorId)
        val encoded = securePreferences.getString(key, null) ?: return null
        val decrypted = runCatching { decrypt(encoded) }.getOrNull()
        val snapshot = decrypted?.let(::publisherDraftSnapshotFromPersistedString)
        if (snapshot == null || snapshot.isExpired()) {
            clear(actorId)
            return null
        }
        return snapshot
    }

    override suspend fun save(snapshot: PublisherDraftSnapshot) {
        purgeExpiredEntries()
        val key = secureEntryKey(snapshot.actorId)
        val encrypted = encrypt(snapshot.toPersistedString())
        securePreferences.edit()
            .putString(key, encrypted)
            .apply()
    }

    override suspend fun clear(actorId: UserId) {
        securePreferences.edit()
            .remove(secureEntryKey(actorId))
            .apply()
        clearLegacyDraft(actorId)
    }

    private fun migrateLegacyDraftIfPresent(actorId: UserId) {
        val legacyPreferences = context.getSharedPreferences(legacyPreferencesName, Context.MODE_PRIVATE)
        val legacyRaw = legacyPreferences.getString(actorId.value, null) ?: return
        val migrated = publisherDraftSnapshotFromLegacyPersistedString(legacyRaw)
            ?.toMigratedSnapshot()
            ?.takeUnless { it.isExpired() }

        if (migrated != null) {
            securePreferences.edit()
                .putString(secureEntryKey(actorId), encrypt(migrated.toPersistedString()))
                .apply()
        }
        clearLegacyDraft(actorId)
    }

    private fun clearLegacyDraft(actorId: UserId) {
        val legacyPreferences = context.getSharedPreferences(legacyPreferencesName, Context.MODE_PRIVATE)
        if (legacyPreferences.contains(actorId.value)) {
            legacyPreferences.edit().remove(actorId.value).commit()
        }
        if (legacyPreferences.all.isEmpty()) {
            context.deleteSharedPreferences(legacyPreferencesName)
        }
    }

    private fun purgeExpiredEntries() {
        val editor = securePreferences.edit()
        var changed = false
        securePreferences.all.keys
            .filter { it.startsWith("${namespace.sanitizeStoreName()}|") }
            .forEach { key ->
                val encoded = securePreferences.getString(key, null)
                val snapshot = encoded
                    ?.let { runCatching { decrypt(it) }.getOrNull() }
                    ?.let(::publisherDraftSnapshotFromPersistedString)
                if (snapshot == null || snapshot.isExpired()) {
                    editor.remove(key)
                    changed = true
                }
            }
        if (changed) {
            editor.apply()
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(Int.SIZE_BYTES + iv.size + ciphertext.size)
            .putInt(iv.size)
            .put(iv)
            .put(ciphertext)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(payload)
        val ivSize = buffer.int
        require(ivSize in 12..16) { "Invalid IV size." }
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GcmTagLengthBits, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun secureEntryKey(actorId: UserId): String =
        "${namespace.sanitizeStoreName()}|${actorId.value}"
}

private fun PublisherDraftSnapshot.toMigratedSnapshot(): PublisherDraftSnapshot =
    copy(
        state = PublisherState.EditingDraft,
        postId = null,
        acceptedAt = null,
        uploadedMedia = emptyList(),
        lastError = null,
        attemptCount = 0,
        traceId = TraceId("draft-${idempotencyKey.value}"),
        requestId = com.example.mindos.features.social.domain.valueobject.RequestId("draft-${idempotencyKey.value}"),
    )

private fun PublisherDraftSnapshot.isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
    updatedAt.epochMillis < nowMillis - DraftTtlMillis

private fun String.sanitizeStoreName(): String =
    lowercase()
        .map { char -> if (char.isLetterOrDigit() || char == '_' || char == '-') char else '_' }
        .joinToString("")
        .ifBlank { "default" }

private const val SecurePreferencesFileName = "neu_secure_publisher_drafts"
private const val KeystoreAliasPrefix = "neu.publisher.drafts."
private const val DraftTtlMillis = 7L * 24L * 60L * 60L * 1000L
private const val GcmTagLengthBits = 128
private const val AndroidKeyStore = "AndroidKeyStore"
private const val Transformation = "AES/GCM/NoPadding"
