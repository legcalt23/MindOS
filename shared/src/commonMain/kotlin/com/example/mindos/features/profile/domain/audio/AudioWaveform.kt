package com.example.mindos.features.profile.domain.audio

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure, testable transforms that turn raw recorded amplitude samples into the
 * fixed-width waveform the profile audio card renders.
 *
 * The amplitudes MUST come from the real recording (e.g. `MediaRecorder
 * .getMaxAmplitude()` polled while capturing) — never from random noise. This
 * object only reshapes those real samples; it never invents data.
 */
object AudioWaveform {

    /** Number of bars rendered in the profile waveform (matches the design density). */
    const val BarCount = 38

    /** Lower visual floor so even quiet bars stay perceptible (fraction of full height). */
    const val MinBarHeight = 0.14f

    /**
     * Reduce arbitrary raw amplitude [samples] (each `>= 0`) to exactly [bars]
     * values in `0f..1f`, peak-normalized so the loudest moment reaches `1f`.
     *
     * - Empty input → all-zero bars (caller renders a flat line instead).
     * - More samples than bars → bucket-average (downsample).
     * - Fewer samples than bars → nearest-sample upsample (no fabricated detail).
     */
    fun fromAmplitudes(samples: List<Float>, bars: Int = BarCount): List<Float> {
        require(bars > 0) { "bars must be positive" }
        val sanitized = samples.map { if (it.isNaN() || it < 0f) 0f else it }
        if (sanitized.isEmpty()) return List(bars) { 0f }

        val peak = sanitized.max()
        if (peak <= 0f) return List(bars) { 0f }

        val reduced = if (sanitized.size >= bars) {
            val bucket = sanitized.size.toFloat() / bars
            List(bars) { i ->
                val start = (i * bucket).toInt()
                val end = max(start + 1, ((i + 1) * bucket).toInt()).coerceAtMost(sanitized.size)
                var sum = 0f
                for (j in start until end) sum += sanitized[j]
                sum / (end - start)
            }
        } else {
            List(bars) { i ->
                val idx = ((i.toFloat() / bars) * sanitized.size).toInt().coerceAtMost(sanitized.lastIndex)
                sanitized[idx]
            }
        }
        return reduced.map { (it / peak).coerceIn(0f, 1f) }
    }

    /** Whether [waveform] carries any real signal (vs. a flat line). */
    fun hasSignal(waveform: List<Float>): Boolean = waveform.any { it > 0f }

    /**
     * Seconds remaining label for a clip of [durationMillis], counting down from
     * [elapsedMillis] of playback. Returns whole seconds, clamped to `>= 0`.
     */
    fun remainingSeconds(durationMillis: Long, elapsedMillis: Long): Int {
        val remaining = (durationMillis - elapsedMillis).coerceAtLeast(0L)
        return ((remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    /** Whole-second ceiling of a duration, clamped to `1..maxSeconds`. */
    fun durationSecondsLabel(durationMillis: Long, maxSeconds: Int): Int {
        val seconds = ((durationMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
        return seconds.coerceIn(1, maxSeconds)
    }

    private const val MILLIS_PER_SECOND = 1000L
}

/**
 * Compact, European-style (comma decimal) number formatting used by the
 * donation bar ("0/10K€") and the observer count ("1,7K observadores").
 */
object ProfileFormatting {

    private const val THOUSAND = 1_000L
    private const val MILLION = 1_000_000L

    /** e.g. `0 -> "0"`, `1700 -> "1,7K"`, `10000 -> "10K"`, `1_700_000 -> "1,7M"`. */
    fun compact(amount: Long): String = when {
        amount < THOUSAND -> amount.toString()
        amount < MILLION -> compactScaled(amount, THOUSAND, "K")
        else -> compactScaled(amount, MILLION, "M")
    }

    /** Donation label, e.g. `"0/10K€"`. */
    fun donationLabel(donation: com.example.mindos.features.profile.domain.model.DonationProgress): String =
        "${compact(donation.currentAmount)}/${compact(donation.targetAmount)}${currencySymbol(donation.currency)}"

    /** Observer count label, e.g. `"1,7K observadores"`. */
    fun observerLabel(count: Int): String = "${compact(count.toLong())} observadores"

    private fun compactScaled(amount: Long, unit: Long, suffix: String): String {
        val whole = amount / unit
        val tenths = ((amount % unit) * 10 / unit).toInt()
        return if (tenths == 0) "$whole$suffix" else "$whole,$tenths$suffix"
    }

    private fun currencySymbol(currency: String): String = when (currency.uppercase()) {
        "EUR" -> "€"
        "USD" -> "$"
        "GBP" -> "£"
        else -> currency
    }
}

/** Round a `0f..1f` fraction to a percent integer (UI helper kept pure for tests). */
internal fun Float.toPercentInt(): Int = (this.coerceIn(0f, 1f) * 100f).roundToInt()
