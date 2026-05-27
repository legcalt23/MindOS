package com.example.mindos.features.dm.data

import dev.gitlive.firebase.storage.Data

actual fun dmStorageData(bytes: ByteArray): Data = Data(bytes)
