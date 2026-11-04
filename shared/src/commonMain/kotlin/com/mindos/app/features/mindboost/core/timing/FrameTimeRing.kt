package com.mindos.app.features.mindboost.core.timing

/**
 * S5 · Paso 5 (F-R3-D, observer-effect blockers B1/B2) — the frame-time buffer of a windowed probe.
 *
 * Replaces the `ArrayList<Long>` the probes used to hold. That was two defects, not one:
 *  - **B1 unbounded.** `ArrayList(1024)` is an INITIAL CAPACITY, not a cap: a window that is never
 *    drained (a stuck session, a lost boundary) grows without limit. This ring is a fixed
 *    [LongArray] — the memory budget is `capacity * 8 bytes`, decided once, for the whole session.
 *  - **B2 autoboxing.** `ArrayList<Long>.add` boxes EVERY frame, so the claim "0 allocations per
 *    frame" was false by inspection while the probe's own garbage was one of the things it then
 *    classified as jank (the self-fulfilling observer effect, B5). [record] touches only primitives.
 *
 * **Overflow is a first-class FACT, not a silent wrap.** The ring wraps (so the newest frames — and
 * therefore the anchor for the next interval — are always correct), but it remembers that it did.
 * A window whose frames were partly overwritten is one we hold an INCOMPLETE sample of, and the
 * honest position for an incomplete sample is that it cannot be classified at all: the policy maps
 * it to `NoObservation`, never to CLEAN (absence must never fabricate perfection) and never to
 * EXCLUDED either — withdrawing the user's accuracy because OUR buffer was too small would let the
 * instrument corrupt the measurement it exists to protect.
 *
 * Not thread-safe by design: like the probes that own it, it is confined to the frame-callback
 * thread (main), which is also the only thread that drains it.
 */
internal class FrameTimeRing(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity >= MIN_CAPACITY) { "a ring smaller than $MIN_CAPACITY frames cannot yield a delta" }
    }

    private val buffer = LongArray(capacity)
    private var writeIndex = 0
    private var size = 0
    private var overflowed = false

    /** One interval's frames plus whether the ring had to overwrite any of them to hold them. */
    class Drained(val frameTimesNanos: LongArray, val overflowed: Boolean)

    /** Records one frame time. Allocation-free by construction: primitive array store + Int arithmetic. */
    fun record(frameTimeNanos: Long) {
        buffer[writeIndex] = frameTimeNanos
        writeIndex = (writeIndex + 1) % capacity
        if (size < capacity) size += 1 else overflowed = true
    }

    /**
     * Returns this interval's frame times in chronological order WITH its overflow fact, and starts
     * the next interval, RETAINING the last frame time as its anchor so no inter-drain gap is
     * silently lost. The fact travels WITH the data (rather than as a flag the caller must remember
     * to read before draining) because a dropped overflow fact silently downgrades an incomplete
     * sample into a confident classification.
     *
     * Allocates one [LongArray] + one holder per drain — bounded, off the per-frame path, and
     * unboxed: a boxed copy at the response boundary is precisely the GC the probe would then
     * misread as the user's jank.
     */
    fun drain(): Drained {
        val out = LongArray(size)
        val start = (writeIndex - size + capacity) % capacity
        for (i in 0 until size) out[i] = buffer[(start + i) % capacity]
        val drained = Drained(out, overflowed)
        size = 0
        writeIndex = 0
        overflowed = false
        if (out.isNotEmpty()) record(out[out.size - 1])
        return drained
    }

    /** Drops everything, including the anchor and the overflow fact (session start / stop). */
    fun reset() {
        size = 0
        writeIndex = 0
        overflowed = false
    }

    companion object {
        /**
         * MEMORY BUDGET (declared, not incidental): 1024 frames × 8 B = 8 KiB per probe, fixed for the
         * session. At 120 Hz that is ~8.5 s of frames, and a window is drained twice per trial, so a
         * healthy session never approaches it — the cap exists for the pathological case, where it
         * bounds memory instead of the old unbounded growth.
         */
        const val DEFAULT_CAPACITY: Int = 1024

        /** Two frame times are needed for one delta; below that nothing is derivable. */
        private const val MIN_CAPACITY: Int = 2
    }
}
