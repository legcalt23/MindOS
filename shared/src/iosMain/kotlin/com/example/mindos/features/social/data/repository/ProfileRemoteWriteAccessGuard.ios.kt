package com.example.mindos.features.social.data.repository

actual fun defaultProfileRemoteWriteAccessGuard(): ProfileRemoteWriteAccessGuard =
    NoOpProfileRemoteWriteAccessGuard
