package com.mindos.app.features.mindboost.focus.data.local

import androidx.compose.runtime.Composable
import com.mindos.app.core.logging.ObservabilityHub
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SecureReadResult {
    data class Value(val value: String) : SecureReadResult
    data object Missing : SecureReadResult

    sealed interface Failure : SecureReadResult {
        val telemetryCode: String
    }

    data object Corrupt : Failure {
        override val telemetryCode: String = "corrupt"
    }

    data object KeyInvalidated : Failure {
        override val telemetryCode: String = "key_invalidated"
    }

    data class UnsupportedVersion(val version: Int) : Failure {
        override val telemetryCode: String = "unsupported_version"
    }
}

class SecureStorageReadException(
    val failure: SecureReadResult.Failure,
) : IllegalStateException("Secure storage read failed: ${failure.telemetryCode}")

/**
 * Minimal, platform-backed key-value store for MindBoost local-first persistence.
 *
 * Cognitive data (profile, baseline, probe results, session history) is sensitive,
 * so the ONLY platform/crypto surface lives here and is deliberately tiny:
 * - Android: AES-256-GCM encrypted SharedPreferences (Android Keystore).
 * - Desktop / iOS: in-memory (non-durable dev stubs; iOS Keychain deferred).
 *
 * Everything else (DTOs, mappers, repositories) is plain `commonMain` and is
 * unit-tested against [InMemorySecureKeyValueStore], so no device is required.
 * Values are opaque strings (the repositories serialize/deserialize).
 */
interface SecureKeyValueStore {
    suspend fun read(key: String): SecureReadResult
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun quarantine(key: String, failure: SecureReadResult.Failure)

    /** All keys (within this namespace) starting with [prefix]. */
    suspend fun keys(prefix: String): Set<String>

    /** Removes every entry in this namespace (e.g. account deletion / opt-out). */
    suspend fun clearNamespace()
}

/**
 * In-memory implementation. Backs the desktop/iOS actuals and all tests. Durable
 * only within a single process run. Thread-safe via a [Mutex].
 */
class InMemorySecureKeyValueStore : SecureKeyValueStore {
    private val mutex = Mutex()
    private val entries = linkedMapOf<String, String>()
    private val failures = linkedMapOf<String, SecureReadResult.Failure>()
    private val quarantined = linkedMapOf<String, Pair<String?, SecureReadResult.Failure>>()

    override suspend fun read(key: String): SecureReadResult = mutex.withLock {
        failures[key] ?: entries[key]?.let(SecureReadResult::Value) ?: SecureReadResult.Missing
    }

    override suspend fun put(key: String, value: String) = mutex.withLock {
        failures.remove(key)
        entries[key] = value
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            entries.remove(key)
            failures.remove(key)
        }
    }

    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) {
        mutex.withLock {
            quarantined[key] = entries.remove(key) to failure
            failures.remove(key)
        }
        reportSecureStorageQuarantine(failure)
    }

    override suspend fun keys(prefix: String): Set<String> =
        mutex.withLock { entries.keys.filter { it.startsWith(prefix) }.toSet() }

    override suspend fun clearNamespace() {
        mutex.withLock {
            entries.clear()
            failures.clear()
            quarantined.clear()
        }
    }

    internal suspend fun simulateReadFailure(key: String, failure: SecureReadResult.Failure) {
        mutex.withLock { failures[key] = failure }
    }

    internal suspend fun quarantinedValue(key: String): String? = mutex.withLock { quarantined[key]?.first }

    internal suspend fun quarantineFailure(key: String): SecureReadResult.Failure? =
        mutex.withLock { quarantined[key]?.second }
}

suspend fun SecureKeyValueStore.readValueOrNull(key: String): String? = when (val result = read(key)) {
    is SecureReadResult.Value -> result.value
    SecureReadResult.Missing -> null
    is SecureReadResult.Failure -> {
        quarantine(key, result)
        null
    }
}

@Suppress("SwallowedException")
suspend fun <T> SecureKeyValueStore.readParsed(
    key: String,
    parser: (String) -> T,
): T? {
    val raw = readValueOrNull(key) ?: return null
    return try {
        parser(raw)
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        quarantine(key, SecureReadResult.Corrupt)
        null
    }
}

suspend fun <T> SecureKeyValueStore.readVersioned(
    key: String,
    currentVersion: Int,
    parser: (String) -> T,
    versionOf: (T) -> Int,
): T? {
    val parsed = readParsed(key, parser)
    return if (parsed == null) {
        null
    } else {
        val version = versionOf(parsed)
        if (version > currentVersion) {
            quarantine(key, SecureReadResult.UnsupportedVersion(version))
            null
        } else {
            parsed
        }
    }
}

internal fun reportSecureStorageQuarantine(failure: SecureReadResult.Failure) {
    ObservabilityHub.breadcrumb("secure_storage_quarantined:${failure.telemetryCode}")
}

/**
 * Composition-scoped factory keyed by [namespace]. Production wiring obtains the
 * encrypted (Android) or in-memory (desktop/iOS) store here.
 */
@Composable
expect fun rememberSecureKeyValueStore(namespace: String): SecureKeyValueStore
