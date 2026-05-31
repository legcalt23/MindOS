package com.mindos.action.normalization

import java.text.Normalizer

internal actual fun nfcNormalize(input: String): String =
    Normalizer.normalize(input, Normalizer.Form.NFC)

internal actual fun foldDiacritics(input: String): String {
    val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
    val sb = StringBuilder(decomposed.length)
    for (ch in decomposed) {
        // Drop combining marks (the decomposed accents); keep base letters.
        if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) sb.append(ch)
    }
    return Normalizer.normalize(sb.toString(), Normalizer.Form.NFC)
}
