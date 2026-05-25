package com.example.neuv3.features.social.data.repository

actual fun defaultProfileRemoteWriteAccessGuard(): ProfileRemoteWriteAccessGuard =
    NoOpProfileRemoteWriteAccessGuard
