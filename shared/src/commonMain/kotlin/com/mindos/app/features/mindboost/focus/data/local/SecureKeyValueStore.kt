package com.mindos.app.features.mindboost.focus.data.local

import androidx.compose.runtime.Composable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)

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

    override suspend fun get(key: String): String? = mutex.withLock { entries[key] }

    override suspend fun put(key: String, value: String) = mutex.withLock { entries[key] = value }

    override suspend fun remove(key: String) {
        mutex.withLock { entries.remove(key) }
    }

    override suspend fun keys(prefix: String): Set<String> =
        mutex.withLock { entries.keys.filter { it.startsWith(prefix) }.toSet() }

    override suspend fun clearNamespace() {
        mutex.withLock { entries.clear() }
    }
}

/**
 * Composition-scoped factory keyed by [namespace]. Production wiring obtains the
 * encrypted (Android) or in-memory (desktop/iOS) store here.
 */
@Composable
expect fun rememberSecureKeyValueStore(namespace: String): SecureKeyValueStore
