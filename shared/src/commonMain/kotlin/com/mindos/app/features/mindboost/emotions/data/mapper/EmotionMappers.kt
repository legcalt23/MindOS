package com.mindos.app.features.mindboost.emotions.data.mapper

import com.mindos.app.features.mindboost.emotions.data.dto.MetricSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.SafetyEventDto
import com.mindos.app.features.mindboost.emotions.data.dto.SelfReportDto
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEvent

/** Domain → persistence DTO mappers. Only summaries cross this boundary; enums are stored by name. */
object EmotionMappers {

    fun toMetricSummaryDtos(metrics: EmotionMetricSet): List<MetricSummaryDto> =
        metrics.values.map { MetricSummaryDto(it.key.name, it.value, it.n, it.confident, it.unit.name) }

    fun toSelfReportDto(report: SelfReport?): SelfReportDto? =
        report?.let { SelfReportDto(it.valence, it.arousal, it.intensity, it.label?.name) }

    fun toSafetyEventDtos(events: List<SafetyEvent>): List<SafetyEventDto> =
        events.map { SafetyEventDto(it.type.name, it.atOffsetMs, it.reportedIntensity, it.dominantProxy?.name) }
}
