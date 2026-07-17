package com.mindos.app.features.mindboost.focus.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.UnrecoverableKeyException
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
actual fun rememberPlatformSecureKeyValueStore(namespace: String): SecureKeyValueStore {
    val context = LocalContext.current.applicationContext
    return remember(context, namespace) { AndroidSecureKeyValueStore(context, namespace) }
}

// Clase de cripto (AES-256-GCM + Keystore) compuesta de muchos helpers pequeños y
// cohesivos (KV ops + encrypt/decrypt/get-or-create/generate/sanitize). El recuento de
// funciones es inherente a la responsabilidad; separarlas reduciría la cohesión.
@Suppress("TooManyFunctions")
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

    override suspend fun read(key: String): SecureReadResult = withContext(Dispatchers.IO) {
        val encoded = prefs.getString(physical(key), null) ?: return@withContext SecureReadResult.Missing
        val envelope = parseEnvelope(encoded)
        if (envelope is Envelope.Unsupported) {
            return@withContext SecureReadResult.UnsupportedVersion(envelope.version)
        }
        if (envelope is Envelope.Corrupt) return@withContext SecureReadResult.Corrupt
        val payload = (envelope as Envelope.Supported).payload
        try {
            SecureReadResult.Value(decrypt(payload))
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            if (error.hasKeyInvalidationCause()) {
                // Drop the cached SecretKey so the next op re-resolves from the Keystore (the key may
                // have rotated / been re-enrolled). The blob itself is quarantined by the caller.
                dropCachedSecretKey()
                SecureReadResult.KeyInvalidated
            } else {
                SecureReadResult.Corrupt
            }
        }
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(physical(key), "$ENVELOPE_PREFIX${encrypt(value)}").apply()
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) { prefs.edit().remove(physical(key)).apply() }
    }

    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) {
        withContext(Dispatchers.IO) {
            val source = physical(key)
            val encoded = prefs.getString(source, null)
            val quarantineKey = "$quarantinePrefix${key.hashCode()}_${System.currentTimeMillis()}"
            val editor = prefs.edit().remove(source)
            if (encoded != null) editor.putString(quarantineKey, encoded)
            editor.apply()
        }
        reportSecureStorageQuarantine(failure)
    }

    override suspend fun keys(prefix: String): Set<String> = withContext(Dispatchers.IO) {
        val physicalPrefixWithKey = physical(prefix)
        prefs.all.keys
            .filter { it.startsWith(physicalPrefixWithKey) && !it.startsWith(quarantinePrefix) }
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

    private fun parseEnvelope(encoded: String): Envelope {
        val separator = encoded.indexOf(':')
        return when {
            !encoded.startsWith("v") || separator < 0 -> Envelope.Supported(encoded)
            separator <= 1 -> Envelope.Corrupt
            else -> {
                val version = encoded.substring(1, separator).toIntOrNull()
                val payload = encoded.substring(separator + 1)
                when {
                    version == null || payload.isBlank() -> Envelope.Corrupt
                    version != ENVELOPE_VERSION -> Envelope.Unsupported(version)
                    else -> Envelope.Supported(payload)
                }
            }
        }
    }

    private fun Throwable.hasKeyInvalidationCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is KeyPermanentlyInvalidatedException || current is UnrecoverableKeyException) return true
            current = current.cause
        }
        return false
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // No IV is supplied: AndroidKeyStore GCM generates a fresh random 12-byte
        // nonce per call and forbids caller-supplied IVs, so nonce uniqueness per
        // key is guaranteed by the platform (never reuse with the same key).
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
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
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    // Cache the resolved SecretKey so we don't re-run KeyStore.load(null) + getKey on every op (D1). The
    // key is a Keystore handle (the material never leaves the secure element), so reusing it across Cipher
    // inits is safe; only the Cipher is stateful and is therefore created fresh per call (never shared).
    @Volatile
    private var cachedSecretKey: SecretKey? = null
    private val secretKeyLock = Any()

    private fun secretKey(): SecretKey =
        cachedSecretKey ?: synchronized(secretKeyLock) {
            cachedSecretKey ?: getOrCreateSecretKey().also { cachedSecretKey = it }
        }

    private fun dropCachedSecretKey() {
        synchronized(secretKeyLock) { cachedSecretKey = null }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        // Prefer a StrongBox-backed key (dedicated tamper-resistant secure element)
        // when the device supports it; fall back to the TEE-backed key otherwise.
        return runCatching { generateKey(strongBox = true) }
            .getOrElse { error ->
                if (error is StrongBoxUnavailableException) generateKey(strongBox = false)
                else throw error
            }
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            // Auth-binding (require device unlock per use) is deliberately OFF:
            // cognitive baselines are read automatically at startup, so gating
            // them behind biometric/PIN would break background access. Enabling it
            // is a product decision (see D-P2.3), not a blanket hardening.
            .setUserAuthenticationRequired(false)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        generator.init(builder.build())
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
        const val ENVELOPE_VERSION = 1
        const val ENVELOPE_PREFIX = "v$ENVELOPE_VERSION:"
    }

    private val quarantinePrefix = "${physicalPrefix}__quarantine__|"

    private sealed interface Envelope {
        data class Supported(val payload: String) : Envelope
        data class Unsupported(val version: Int) : Envelope
        data object Corrupt : Envelope
    }
}
