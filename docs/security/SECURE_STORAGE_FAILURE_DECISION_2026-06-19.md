# Secure storage failure and quarantine decision

Date: 2026-06-19  
Scope: Fase 0, P0-007

## Result model

Secure reads no longer collapse every condition to nullable data. `SecureReadResult` distinguishes:

- `Value`
- `Missing`
- `Corrupt`
- `KeyInvalidated`
- `UnsupportedVersion`

Repositories parse decrypted JSON through shared helpers. Corrupt JSON and future schema versions are quarantined rather than treated as valid defaults. The account-deletion journal is stricter: any typed failure raises `SecureStorageReadException`, preventing a pending deletion from being silently skipped.

## Quarantine and overwrite policy

Android quarantine moves the original encrypted value to a namespace-private quarantine key before removing the active logical key. Common tests use the same move semantics. Reads never rewrite defaults over the failing value; a later legitimate write may create a new active record while the original raw value remains available for controlled recovery/forensics.

Quarantine keys are excluded from repository key enumeration and are removed by whole-namespace account deletion. Telemetry emits only `secure_storage_quarantined:<failure-code>` and never includes namespace, logical key, UID, ciphertext or plaintext.

## Envelope compatibility

New Android writes use the `v1:` encryption-envelope prefix. Existing unprefixed ciphertext remains readable. A legacy Base64 payload can begin with the character `v`, so it is considered versioned only when a colon delimiter is present; otherwise it follows the legacy v1 path. A numeric future envelope version produces `UnsupportedVersion` without attempting decryption.

## Key invalidation

`KeyPermanentlyInvalidatedException` and `UnrecoverableKeyException`, including wrapped causes, map to `KeyInvalidated`. Other decode, Base64, envelope and AEAD failures map to `Corrupt`. Neither condition is logged with sensitive identifiers.

## Residual risk

iOS and desktop actuals remain non-durable in-memory stores, as established by the existing platform policy. This phase hardens typed behavior and Android durable storage; implementing an iOS Keychain backend belongs to a later explicitly scoped phase.
