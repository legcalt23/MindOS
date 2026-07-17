package com.mindos.app.features.mindboost.focus.data.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS actual: durable, Keychain-backed store. The pure [IndexedSecureKeyValueStore] (index / namespacing /
 * keys / clearNamespace / quarantine — unit-tested) sits over [IosKeychainBackend], whose native Security
 * interop is COMPILE-PENDING and must be verified via `:shared:compileKotlinIosSimulatorArm64` +
 * `:shared:iosSimulatorArm64Test` on macOS (see [IosKeychainBackend]) before iOS dev rollout.
 * By design, cognitive data on iOS is then encrypted at rest and survives app restart / device reboot.
 */
@Composable
actual fun rememberPlatformSecureKeyValueStore(namespace: String): SecureKeyValueStore =
    remember(namespace) { IndexedSecureKeyValueStore(IosKeychainBackend(namespace)) }
