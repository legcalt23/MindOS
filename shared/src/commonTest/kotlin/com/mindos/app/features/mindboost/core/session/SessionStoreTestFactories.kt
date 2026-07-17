package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore

/**
 * Test factories for the two subject-scoped stores (SYSTEM-1 slice 6-R3, P2-01). Production constructors require an
 * explicit `revocationStore` (the shared `mindboost_revocation` backing) with NO default — that keeps every
 * production wiring site compiler-checked (a missed site is a build error, never a silent partial migration). For
 * tests that don't exercise revocation, this factory supplies a FRESH in-memory revocation backing per store; a
 * test that DOES exercise cross-instance revocation passes a shared [revocationStore] explicitly.
 */
fun testSessionRecordStore(
    store: SecureKeyValueStore,
    subject: String,
    revocationStore: SecureKeyValueStore = InMemorySecureKeyValueStore(),
): SecureSessionRecordStore = SecureSessionRecordStore(store, revocationStore, subject)

fun testCognitiveRunStore(
    store: SecureKeyValueStore,
    subject: String,
    revocationStore: SecureKeyValueStore = InMemorySecureKeyValueStore(),
): SecureCognitiveSessionRunStore = SecureCognitiveSessionRunStore(store, revocationStore, subject)
