package su.afk.yummy.tv.core.analytics.appmetrica

import io.appmetrica.analytics.AppMetrica
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.DiagnosticLogSink
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.analytics.utils.isReportableError
import javax.inject.Inject

internal class AppMetricaAnalyticsTracker @Inject constructor(
    private val diagnosticLogSink: DiagnosticLogSink,
) : AnalyticsTracker {

    override fun track(eventName: String, params: Map<String, String>) {
        val eventName = eventName.trim()
        if (eventName.isEmpty()) return
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
}
