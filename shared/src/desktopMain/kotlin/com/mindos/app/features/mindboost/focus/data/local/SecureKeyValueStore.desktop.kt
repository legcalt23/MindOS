package com.mindos.app.features.mindboost.focus.data.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop actual: in-memory, non-durable (dev/test target). Cognitive data is
 * intentionally NOT written to disk on desktop — keeping sensitive data off an
 * unencrypted local store. A durable, OS-protected desktop store is out of scope.
 */
@Composable
actual fun rememberPlatformSecureKeyValueStore(namespace: String): SecureKeyValueStore =
    remember(namespace) { InMemorySecureKeyValueStore() }
