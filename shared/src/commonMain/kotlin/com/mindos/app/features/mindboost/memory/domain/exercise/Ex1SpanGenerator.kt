package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.content.MemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.sampling.SamplingFilters
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.random.Random

/** Concrete EX1 parameters decoded from the relevant difficulty axes. */
internal data class Ex1Params(
    val spanLength: Int,
    val processingPerItem: Int,
    val sameCategory: Boolean,
    val backwardRecall: Boolean,
)

/**
 * Maps the difficulty vector to EX1 (Span Complejo) parameters. Reads ONLY the
 * axes declared relevant for EX1 (LOAD, CONCURRENCY, SIMILARITY, RETRIEVAL); other
 * axes are inert. The literals are the interpolation endpoints (the tuning), so
 * MagicNumber is suppressed — same pattern as focus's E1DifficultyMapping.
 */
@Suppress("MagicNumber")
internal object Ex1DifficultyMapping {
    private const val THRESHOLD = 0.5

    fun map(d: MemoryDifficultyVector): Ex1Params = Ex1Params(
        // Span grows with load: 2 → 8 memoranda.
        spanLength = DifficultyDecode.decodeCount(d.load, 2, 8),
        // Concurrent processing tasks per item: 1 → 2 (complex span always has ≥1).
        processingPerItem = DifficultyDecode.decodeCount(d.concurrency, 1, 2),
        // High similarity ⇒ draw memoranda from a single category (more confusable).
        sameCategory = d.similarity.value >= THRESHOLD,
        // High retrieval demand ⇒ backward recall.
        backwardRecall = d.retrieval.value >= THRESHOLD,
    )
}

/**
 * EX1 Span Complejo generator. Produces memoranda interleaved with arithmetic
 * processing tasks (the dual-task demand), recalled forward or backward.
 * Deterministic: identical (difficulty, content, seed, excludes) → identical plan.
 *
 * NOTE: "similarity" is approximated by same-category sampling (the content layer
 * carries no phonological tags in V1); documented as a V1 simplification.
 */
class Ex1SpanGenerator : MemoryTrialGenerator {
    override val exerciseId: MemoryExerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO

    override fun generate(
        difficulty: MemoryDifficultyVector,
        content: MemoryContent,
        seed: Long,
        excludeRecentWordIds: Set<WordId>,
        excludeRecentPairIds: Set<PairId>,
    ): MemoryTrialPlan {
        val params = Ex1DifficultyMapping.map(difficulty)
        val rng = Random(seed)
        val sampler = content.samplerFor(exerciseId)

        val filters = if (params.sameCategory) {
            SamplingFilters(category = pickCategory(content, rng))
        } else {
            SamplingFilters.None
        }

        val memoranda = sampler.sampleWords(
            count = params.spanLength,
            seed = seed,
            filters = filters,
            excludeRecentIds = excludeRecentWordIds,
        )

        val steps = buildList {
            memoranda.forEach { word ->
                add(Memorandum(word))
                repeat(params.processingPerItem) { add(processingTask(rng)) }
            }
        }

        return ComplexSpanTrial(
            steps = steps,
            recallType = if (params.backwardRecall) RecallType.SERIAL_BACKWARD else RecallType.SERIAL_FORWARD,
            expectedRecall = if (params.backwardRecall) memoranda.reversed() else memoranda,
        )
    }

    /** Picks a category deterministically; restricted to those the training pool can serve. */
    private fun pickCategory(content: MemoryContent, rng: Random): WordCategory {
        val pool = content.trainingWordPool()
        val available = WordCategory.entries.filter { pool.byCategory(it).isNotEmpty() }
        return available[rng.nextInt(available.size)]
    }

    /** A single-step arithmetic verification task ("a + b = shown"), correct ~half the time. */
    @Suppress("MagicNumber") // operand/answer ranges of the arithmetic processing task.
    private fun processingTask(rng: Random): ProcessingTask {
        val a = rng.nextInt(1, 10)
        val b = rng.nextInt(1, 10)
        val real = a + b
        val correct = rng.nextBoolean()
        val shown = if (correct) real else real + (if (rng.nextBoolean()) 1 else -1)
        return ProcessingTask(prompt = "$a + $b = $shown", correct = shown == real)
    }
}
