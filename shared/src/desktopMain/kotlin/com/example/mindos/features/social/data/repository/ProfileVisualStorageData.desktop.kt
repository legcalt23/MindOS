package com.example.mindos.features.social.data.repository

import dev.gitlive.firebase.storage.Data

actual fun profileVisualStorageData(bytes: ByteArray): Data = Data(bytes)
