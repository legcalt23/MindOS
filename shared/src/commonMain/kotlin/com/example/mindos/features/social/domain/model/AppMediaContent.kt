package com.example.mindos.features.social.domain.model

import com.example.mindos.features.social.domain.feed.GlobalFeedAttachment

data class AppMediaContent(
    val url: String,
    val storagePath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val thumbnailUrl: String? = null
)

fun GlobalFeedAttachment.toAppMediaContent(): AppMediaContent = AppMediaContent(
    url = url,
    storagePath = storagePath,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    thumbnailUrl = thumbnailUrl
)
