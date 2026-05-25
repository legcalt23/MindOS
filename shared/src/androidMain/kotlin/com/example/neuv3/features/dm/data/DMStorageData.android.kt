package com.example.neuv3.features.dm.data

import dev.gitlive.firebase.storage.Data

actual fun dmStorageData(bytes: ByteArray): Data = Data(bytes)
