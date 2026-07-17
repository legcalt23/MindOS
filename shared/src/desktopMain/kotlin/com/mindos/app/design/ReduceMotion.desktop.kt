package com.mindos.app.design

import androidx.compose.runtime.Composable

/** Desktop (JVM) has no standard reduce-motion signal; default to full motion. */
@Composable
actual fun rememberSystemReduceMotion(): Boolean = false
