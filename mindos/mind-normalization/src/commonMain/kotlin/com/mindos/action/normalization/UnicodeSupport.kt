package com.mindos.action.normalization

/**
 * Platform Unicode primitives. KMP commonMain has no NFC/diacritic API, so these are
 * expect/actual. Kotlin's no-arg [String.lowercase] is already locale-invariant
 * (avoids the Turkish-i bug), so case folding does NOT need a platform actual.
 */
internal expect fun nfcNormalize(input: String): String

internal expect fun foldDiacritics(input: String): String
