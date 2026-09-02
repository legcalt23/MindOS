package com.mindos.app.features.mindboost.core.lifecycle

import androidx.compose.runtime.Composable

/**
 * iOS has no `UIApplication` foreground/background capture wired yet → [NoOpAppLifecycleProbe]. It
 * declares the absence (`NotObservable`) rather than fabricating an empty observation. Porting the
 * `willResignActive`/`didBecomeActive` notifications is deferred with the rest of `BLOQUEADO_IOS`;
 * this `actual` compiles under Kotlin/Native but has NOT been executed on a device or simulator.
 */
@Composable
actual fun rememberAppLifecycleProbe(): AppLifecycleProbe = NoOpAppLifecycleProbe
