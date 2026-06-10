package com.mindos.app.features.social.domain.feed

import platform.Foundation.NSDate

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
