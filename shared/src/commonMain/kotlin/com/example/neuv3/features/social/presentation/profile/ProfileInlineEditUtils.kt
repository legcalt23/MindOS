package com.example.neuv3.features.social.presentation.profile

import com.example.neuv3.features.social.domain.model.ProfileUserphraseMaxLength

internal fun sanitizePublicPhrase(text: String): String {
    val builder = StringBuilder(text.length)
    var index = 0
    while (index < text.length && builder.length < ProfileUserphraseMaxLength) {
        val current = text[index]
        if (current.isHighSurrogateCodeUnit() && index + 1 < text.length && text[index + 1].isLowSurrogateCodeUnit()) {
            val high = current.code - 0xD800
            val low = text[index + 1].code - 0xDC00
            val codePoint = 0x10000 + (high shl 10) + low
            if (!isEmojiCodePoint(codePoint) && builder.length + 2 <= ProfileUserphraseMaxLength) {
                builder.append(current)
                builder.append(text[index + 1])
            }
            index += 2
            continue
        }

        if (!isEmojiCodePoint(current.code)) {
            builder.append(current)
        }
        index += 1
    }
    return builder.toString().take(ProfileUserphraseMaxLength)
}

internal fun containsEmoji(text: String): Boolean =
    sanitizePublicPhrase(text) != text

private fun isEmojiCodePoint(codePoint: Int): Boolean =
    codePoint == 0x200D ||
        codePoint == 0xFE0F ||
        codePoint == 0x20E3 ||
        codePoint in 0x1F1E6..0x1F1FF ||
        codePoint in 0x1F300..0x1F5FF ||
        codePoint in 0x1F600..0x1F64F ||
        codePoint in 0x1F680..0x1F6FF ||
        codePoint in 0x1F700..0x1F77F ||
        codePoint in 0x1F780..0x1F7FF ||
        codePoint in 0x1F800..0x1F8FF ||
        codePoint in 0x1F900..0x1F9FF ||
        codePoint in 0x1FA70..0x1FAFF ||
        codePoint in 0x2600..0x26FF ||
        codePoint in 0x2700..0x27BF

private fun Char.isHighSurrogateCodeUnit(): Boolean =
    code in 0xD800..0xDBFF

private fun Char.isLowSurrogateCodeUnit(): Boolean =
    code in 0xDC00..0xDFFF
