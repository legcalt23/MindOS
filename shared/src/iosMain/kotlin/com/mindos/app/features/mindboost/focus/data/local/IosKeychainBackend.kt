@file:OptIn(ExperimentalForeignApi::class)

package com.mindos.app.features.mindboost.focus.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS actual [KeyValueBackend] over the Keychain (generic-password items). The [service] is the MindBoost
 * namespace (e.g. `mindboost_sync_queue`) and the account is the store key, so namespaces never collide.
 *
 * - **Encryption:** the Keychain encrypts items at rest (hardware-backed).
 * - **Accessibility:** `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` — available after first unlock,
 *   never synced to iCloud Keychain, never restored to another device (privacy > cross-device).
 * - **Durability:** survives app restart and device reboot.
 * - **Upsert atomicity:** [write] does `SecItemAdd`, and on `errSecDuplicateItem` updates the item in place
 *   (`SecItemUpdate`) instead of delete-then-add — there is no window where the key is momentarily absent.
 * - **Fail-closed reads AND writes:** every `OSStatus` is checked; a non-success add/update/delete throws
 *   [KeyValueBackendException] (a status code only — never a key, value, or identifier), so the store above
 *   never indexes a key the Keychain did not persist. [read] returns `null` ONLY for a truly absent
 *   (`errSecItemNotFound`) or non-UTF-8 item; ANY other genuine read failure THROWS, so a failed read never
 *   masquerades as "absent" (which would let erasure silently skip data). Values are UTF-8; nothing is logged.
 *
 * **EMULATOR/DEVICE-GATED — COMPILE-UNVERIFIED HERE.** This is the only file whose Kotlin/Native Security
 * interop is NOT compiled in this environment (no macOS/Xcode). It must pass
 * `:shared:compileKotlinIosSimulatorArm64` and be behaviour-verified on a simulator/device before iOS dev
 * rollout. The durable-store LOGIC (index, namespacing, keys, clearNamespace, quarantine, atomic put) lives in
 * the pure, unit-tested [IndexedSecureKeyValueStore]; this file only performs the raw upsert/read/delete.
 */
class IosKeychainBackend(private val service: String) : KeyValueBackend {

    override suspend fun read(key: String): String? {
        val serviceRef = CFBridgingRetain(NSString.create(string = service))
        val accountRef = CFBridgingRetain(NSString.create(string = key))
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        return try {
            addIdentity(query, serviceRef, accountRef)
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
            copyMatchingValue(query)
        } finally {
            releaseDict(query)
            releaseRefs(serviceRef, accountRef)
        }
    }

    override suspend fun write(key: String, value: String) {
        val serviceRef = CFBridgingRetain(NSString.create(string = service))
        val accountRef = CFBridgingRetain(NSString.create(string = key))
        val valueRef = CFBridgingRetain(NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding))
        val addQuery = CFDictionaryCreateMutable(null, 0, null, null)
        try {
            addIdentity(addQuery, serviceRef, accountRef)
            CFDictionaryAddValue(addQuery, kSecValueData, valueRef)
            CFDictionaryAddValue(addQuery, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            val addStatus = SecItemAdd(addQuery, null)
            when {
                addStatus == errSecDuplicateItem -> updateExisting(serviceRef, accountRef, valueRef)
                addStatus != errSecSuccess -> throw KeyValueBackendException(addStatus.toInt())
            }
        } finally {
            releaseDict(addQuery)
            releaseRefs(serviceRef, accountRef, valueRef)
        }
    }

    override suspend fun delete(key: String) {
        val serviceRef = CFBridgingRetain(NSString.create(string = service))
        val accountRef = CFBridgingRetain(NSString.create(string = key))
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        try {
            addIdentity(query, serviceRef, accountRef)
            val status = SecItemDelete(query)
            if (status != errSecSuccess && status != errSecItemNotFound) {
                throw KeyValueBackendException(status.toInt())
            }
        } finally {
            releaseDict(query)
            releaseRefs(serviceRef, accountRef)
        }
    }

    /** Updates the value of an already-present item in place (called only on `errSecDuplicateItem`). */
    private fun updateExisting(serviceRef: CFTypeRef?, accountRef: CFTypeRef?, valueRef: CFTypeRef?) {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        val attributes = CFDictionaryCreateMutable(null, 0, null, null)
        try {
            addIdentity(query, serviceRef, accountRef)
            CFDictionaryAddValue(attributes, kSecValueData, valueRef)
            val status = SecItemUpdate(query, attributes)
            if (status != errSecSuccess) throw KeyValueBackendException(status.toInt())
        } finally {
            releaseDict(query)
            releaseDict(attributes)
        }
    }

    /**
     * Runs the query and decodes the returned data as UTF-8. `errSecItemNotFound` (true absence) and a
     * non-UTF-8 decode are `null`; ANY other non-success status THROWS (symmetric with [delete]) so a genuine
     * read failure never masquerades as "absent" — critical for the reserved index key, where a false-empty
     * read would let [IndexedSecureKeyValueStore.clearNamespace] silently skip real data during erasure.
     */
    private fun copyMatchingValue(query: CFMutableDictionaryRef?): String? = memScoped {
        val out = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, out.ptr)
        when {
            status == errSecItemNotFound -> return@memScoped null
            status != errSecSuccess -> throw KeyValueBackendException(status.toInt())
        }
        (CFBridgingRelease(out.value) as? NSData)?.let { NSString.create(it, NSUTF8StringEncoding) as String? }
    }

    private fun addIdentity(dict: CFMutableDictionaryRef?, serviceRef: CFTypeRef?, accountRef: CFTypeRef?) {
        CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dict, kSecAttrService, serviceRef)
        CFDictionaryAddValue(dict, kSecAttrAccount, accountRef)
    }

    private fun releaseDict(dict: CFMutableDictionaryRef?) {
        if (dict != null) CFRelease(dict)
    }

    private fun releaseRefs(vararg refs: CFTypeRef?) {
        for (ref in refs) CFBridgingRelease(ref)
    }
}
