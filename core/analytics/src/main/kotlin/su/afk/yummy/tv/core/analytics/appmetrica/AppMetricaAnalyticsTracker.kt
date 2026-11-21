package su.afk.yummy.tv.core.analytics.appmetrica

import io.appmetrica.analytics.AppMetrica
import io.ktor.client.plugins.ResponseException
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.DiagnosticLogSink
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import su.afk.yummy.tv.core.analytics.utils.isHttpNotFound
import su.afk.yummy.tv.core.analytics.utils.isReportableError
import javax.inject.Inject

internal class AppMetricaAnalyticsTracker @Inject constructor(
    private val diagnosticLogSink: DiagnosticLogSink,
    private val activationState: AppMetricaActivationState,
) : AnalyticsTracker {

    override fun track(eventName: String, params: Map<String, String>) {
        val eventName = eventName.trim()
        if (eventName.isEmpty()) return
        if (!activationState.isActivated) return
        if (params.isEmpty()) {
            AppMetrica.reportEvent(eventName)
        } else {
            AppMetrica.reportEvent(eventName, params)
        }
    }

    override fun reportError(
        message: String,
        throwable: Throwable,
        groupIdentifier: String?,
    ) {
        val message = message.trim()
        if (message.isEmpty()) return
        if (!throwable.isReportableError()) return
        if (!activationState.isActivated) return
        if (throwable.isHttpNotFound()) {
            // Штатный 404 — событие, а не error-лог.
            track(
                EVENT_API_NOT_FOUND,
                analyticsParamsOf(
                    "source" to groupIdentifier,
                    "url" to (throwable as? ResponseException)?.response?.call?.request?.url?.toString(),
                ),
            )
            return
        }
        val groupIdentifier = groupIdentifier?.trim().orEmpty()
        if (groupIdentifier.isEmpty()) {
            AppMetrica.reportError(message, throwable)
        } else {
            AppMetrica.reportError(groupIdentifier, message, throwable)
        }
        AppMetrica.sendEventsBuffer()
    }

    override fun log(tag: String, throwable: Throwable?, message: () -> String) {
        // Free-form diagnostics are never forwarded to production analytics. Only whitelisted tags
        // are kept, and only in the app log file; the rest stays a no-op.
        if (tag !in PersistedLogTags.persisted) return
        diagnosticLogSink.write(tag, throwable, message())
    }

    private companion object {
        const val EVENT_API_NOT_FOUND = "api_not_found"
    }
}
