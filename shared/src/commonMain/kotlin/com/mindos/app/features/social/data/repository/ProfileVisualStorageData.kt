package com.mindos.app.features.social.data.repository

import dev.gitlive.firebase.storage.Data

expect fun profileVisualStorageData(bytes: ByteArray): Data
