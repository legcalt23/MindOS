package com.mindos.testkit

/** A reproducible property failure: replay by re-running with [seed] or feeding [input] directly. */
data class PropertyFailure(
    val seed: Long,
    val iteration: Int,
    val input: String,
    val message: String,
)

data class PropertyReport(
    val cases: Int,
    val failures: List<PropertyFailure>,
) {
    val passed: Boolean get() = failures.isEmpty()

    fun describe(): String =
        if (passed) {
            "PASS ($cases cases)"
        } else {
            "FAIL (${failures.size} failing of $cases):\n" +
                failures.joinToString("\n") { " - seed=${it.seed} iter=${it.iteration} input=<<${it.input}>> :: ${it.message}" }
        }
}

/**
 * Minimal, deterministic property-based runner (no external library). Generates [cases] inputs from
 * a seeded RNG, checks an invariant (the property throws on violation), and on failure performs
 * light delta-shrinking to report a minimal reproducing input together with the exact seed.
 */
object PropertyRunner {

    fun forAllStrings(
        seed: Long,
        cases: Int,
        maxFailuresReported: Int = 10,
        generator: (SeededRng) -> String,
        property: (String) -> Unit,
    ): PropertyReport {
        val rng = SeededRng(seed)
        val failures = ArrayList<PropertyFailure>()
        for (i in 0 until cases) {
            val input = generator(rng)
            val error = runProperty(property, input)
            if (error != null) {
                val shrunk = shrink(input) { runProperty(property, it) != null }
                failures += PropertyFailure(seed, i, shrunk, error)
                if (failures.size >= maxFailuresReported) break
            }
        }
        return PropertyReport(cases, failures)
    }

    private fun runProperty(property: (String) -> Unit, input: String): String? =
        try {
            property(input)
            null
        } catch (e: Throwable) {
            e.message ?: (e::class.simpleName ?: "failure")
        }

    /** Greedy single-character delta shrink: keep removing chars while the property still fails. */
    private fun shrink(input: String, stillFails: (String) -> Boolean): String {
        var current = input
        var improved = true
        while (improved && current.length > 1) {
            improved = false
            for (i in current.indices) {
                val candidate = current.removeRange(i, i + 1)
                if (candidate.isNotEmpty() && stillFails(candidate)) {
                    current = candidate
                    improved = true
                    break
                }
            }
        }
        return current
    }
}
