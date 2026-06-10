package com.mindos.app.features.social.domain.publishing

import com.mindos.app.features.social.shared.util.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface OutboxMetricsProvider {
    suspend fun getPendingCount(): Long
    suspend fun getDeadLetterCount(): Long
    suspend fun getOldestPendingAgeMillis(nowEpoch: Long): Long
}

class PublishingSystemMetricsPoller(
    private val scope: CoroutineScope,
    private val clock: AppClock,
    private val metrics: PublishingMetricsRecorder,
    private val outboxMetrics: OutboxMetricsProvider,
    private val intervalMillis: Long = 60_000,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                measureSystemPressure()
                delay(intervalMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private suspend fun measureSystemPressure() {
        val now = clock.now().epochMillis
        val queueDepth = outboxMetrics.getPendingCount()
        val dlqSize = outboxMetrics.getDeadLetterCount()
        val oldestAge = outboxMetrics.getOldestPendingAgeMillis(now)

        metrics.recordDistribution(
            MetricName.QueueDepth,
            queueDepth,
            DefaultMetricTags(stage = Stage.OutboxRelay, outcome = Outcome.Succeeded)
        )
        metrics.recordDistribution(
            MetricName.DlqSize,
            dlqSize,
            DefaultMetricTags(stage = Stage.OutboxRelay, outcome = Outcome.Succeeded)
        )
        metrics.recordDistribution(
            MetricName.OutboxBacklog,
            oldestAge,
            DefaultMetricTags(stage = Stage.OutboxRelay, outcome = Outcome.Succeeded)
        )
        
        metrics.increment(MetricName.SystemBacklogChecked, DefaultMetricTags(stage = Stage.OutboxRelay, outcome = Outcome.Succeeded))
    }
}
