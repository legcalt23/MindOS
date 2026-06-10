package com.mindos.app.features.mindboost.focus.data.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS actual: in-memory stub. A Keychain-backed durable store is deferred to the
 * macOS build phase (consistent with the repo's other deferred iOS actuals).
 * Until then, focus data on iOS is session-scoped and never written unencrypted.
 */
@Composable
actual fun rememberSecureKeyValueStore(namespace: String): SecureKeyValueStore =
    remember(namespace) { InMemorySecureKeyValueStore() }
