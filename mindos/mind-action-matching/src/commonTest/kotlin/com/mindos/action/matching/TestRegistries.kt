package com.mindos.action.matching

import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.observability.AuditSink
import com.mindos.action.observability.MetricSink
import com.mindos.action.observability.MetricTags
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.NoOpAuditSink
import com.mindos.action.observability.NoOpMetricSink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.observability.NoOpStructuredLogSink
import com.mindos.action.observability.StructuredLogSink
import com.mindos.action.pattern.RegistrySource
import com.mindos.action.pattern.registrySource
import com.mindos.security.AbuseGuard
import com.mindos.security.InputSecurityPolicy
import com.mindos.security.NoOpAbuseGuard
import com.mindos.security.SecurityClock
import com.mindos.security.SecurityClocks

/** Shared fixtures for matcher tests. */
internal object TestRegistries {

    val sample: RegistrySource = registrySource {
        exact("p_settings", "open settings", intent = "open_settings")
        exact("p_settings_short", "settings", intent = "open_settings")
        alias("ajustes", locale = "es", intent = "open_settings")
        alias("configuración", locale = "es", intent = "open_settings")
        alias("home", locale = "und", intent = "open_home")
        parametric("p_profile", "open {target:username}", intent = "open_profile")
        parametric("p_story", "open story {storyId:story_id}", intent = "open_story")
        parametric("p_msg", "message {user:username} {text:free_text:greedy}", intent = "send_message")
    }

    fun matcher(
        source: RegistrySource = sample,
        telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
        policy: MatchPolicy = MatchPolicy(),
        options: CompileOptions = CompileOptions(),
        securityPolicy: InputSecurityPolicy = InputSecurityPolicy.Default,
        abuseGuard: AbuseGuard = NoOpAbuseGuard,
        audit: AuditSink = NoOpAuditSink,
        clock: SecurityClock = SecurityClocks.Zero,
        metrics: MetricSink = NoOpMetricSink,
        logs: StructuredLogSink = NoOpStructuredLogSink,
        metricTags: MetricTags = MetricTags.Default,
    ): DefaultActionMatcher =
        DefaultActionMatcher(
            RegistryCompiler.compile(source, options).getOrThrow(),
            telemetry,
            policy,
            securityPolicy,
            abuseGuard,
            audit,
            clock,
            metrics,
            logs,
            metricTags,
        )
}
