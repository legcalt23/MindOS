package com.example.neuv3.core.media

data class PlatformFile(
    val bytes: ByteArray,
    val name: String,
    val mimeType: String,
    val size: Long,
    val durationMs: Long? = null,
    val thumbnailBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformFile) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (size != other.size) return false
        if (durationMs != other.durationMs) return false
        if (thumbnailBytes != null) {
            if (other.thumbnailBytes == null) return false
            if (!thumbnailBytes.contentEquals(other.thumbnailBytes)) return false
        } else if (other.thumbnailBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + (durationMs?.hashCode() ?: 0)
        result = 31 * result + (thumbnailBytes?.contentHashCode() ?: 0)
        return result
    }
}
