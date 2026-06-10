package com.mindos.app.features.mindboost.focus.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android actual: values are AES-256-GCM encrypted with a key held in the Android
 * Keystore (never exported), then stored Base64 in private SharedPreferences. This
 * is the same vetted scheme used by the publisher draft store — replicated, not
 * re-invented. Cognitive data is therefore encrypted at rest.
 */
@Composable
actual fun rememberSecureKeyValueStore(namespace: String): SecureKeyValueStore {
    val context = LocalContext.current.applicationContext
    return remember(context, namespace) { AndroidSecureKeyValueStore(context, namespace) }
}

private class AndroidSecureKeyValueStore(
    private val context: Context,
    namespace: String,
) : SecureKeyValueStore {

    private val safeNamespace = namespace.sanitize()
    private val keyAlias = "$KEYSTORE_ALIAS_PREFIX$safeNamespace"
    private val physicalPrefix = "$safeNamespace$SEPARATOR"

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        val encoded = prefs.getString(physical(key), null) ?: return@withContext null
        runCatching { decrypt(encoded) }.getOrNull()
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(physical(key), encrypt(value)).apply()
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) { prefs.edit().remove(physical(key)).apply() }
    }

    override suspend fun keys(prefix: String): Set<String> = withContext(Dispatchers.IO) {
        val physicalPrefixWithKey = physical(prefix)
        prefs.all.keys
            .filter { it.startsWith(physicalPrefixWithKey) }
            .map { it.removePrefix(physicalPrefix) }
            .toSet()
    }

    override suspend fun clearNamespace() {
        withContext(Dispatchers.IO) {
            val editor = prefs.edit()
            prefs.all.keys.filter { it.startsWith(physicalPrefix) }.forEach(editor::remove)
            editor.apply()
        }
    }

    private fun physical(key: String): String = "$physicalPrefix$key"

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
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
        require(ivSize in MIN_IV_BYTES..MAX_IV_BYTES) { "Invalid IV size." }
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun String.sanitize(): String =
        lowercase()
            .map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }
            .joinToString("")
            .ifBlank { "default" }

    private companion object {
        const val PREFERENCES_FILE = "neu_secure_focus"
        const val KEYSTORE_ALIAS_PREFIX = "mindos.focus."
        const val SEPARATOR = "|"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val AES_KEY_BITS = 256
        const val MIN_IV_BYTES = 12
        const val MAX_IV_BYTES = 16
    }
}
