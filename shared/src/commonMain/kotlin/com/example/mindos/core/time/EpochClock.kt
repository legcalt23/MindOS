package com.example.mindos.core.time

import kotlinx.datetime.Clock

/**
 * Source of "now" in epoch milliseconds.
 *
 * Why this exists: calling `kotlinx.datetime.Clock.System.now()` directly inside
 * presentation/domain code makes those code paths impossible to unit-test
 * deterministically, and — more concretely — kotlinx-datetime 0.7.1 (pulled
 * transitively over the pinned 0.6.0 by Compose/Kotlin) removed `Clock.System`
 * from the runtime classpath, causing `NoClassDefFoundError` on the desktop test
 * JVM (see CLAUDE.md, 2026-05-29).
 *
 * The fix is to inject an [EpochClock]. Production uses [SystemEpochClock] (the
 * single, isolated place that touches `Clock.System`); tests inject a fake and
 * therefore never trip on the missing class.
 *
 * This is the epoch-millis (Long) counterpart to the social feature's
 * `AppClock`, which returns a domain `Timestamp` value object.
 */
fun interface EpochClock {
    fun nowMillis(): Long
}

/**
 * Real wall-clock implementation. The ONLY place in shared code that should call
 * `Clock.System.now()`. Keep it that way so the kotlinx-datetime runtime fragility
 * stays contained to a single, test-free line.
 */
object SystemEpochClock : EpochClock {
    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
