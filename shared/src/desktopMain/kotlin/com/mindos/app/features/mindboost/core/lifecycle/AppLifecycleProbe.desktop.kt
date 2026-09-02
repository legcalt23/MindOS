package com.mindos.app.features.mindboost.core.lifecycle

import androidx.compose.runtime.Composable

/**
 * Desktop has no foreground/background source wired → [NoOpAppLifecycleProbe], which declares the
 * absence (`NotObservable`) instead of reporting an empty, fabricated observation.
 */
@Composable
actual fun rememberAppLifecycleProbe(): AppLifecycleProbe = NoOpAppLifecycleProbe
