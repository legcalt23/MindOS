package com.mindos.app.features.social.data.repository

actual fun defaultProfileRemoteWriteAccessGuard(): ProfileRemoteWriteAccessGuard =
    NoOpProfileRemoteWriteAccessGuard
