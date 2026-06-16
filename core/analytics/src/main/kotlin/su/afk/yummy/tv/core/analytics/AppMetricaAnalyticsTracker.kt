package su.afk.yummy.tv.core.analytics

import io.appmetrica.analytics.AppMetrica
import javax.inject.Inject

internal class AppMetricaAnalyticsTracker @Inject constructor(
    private val analyticsContext: AnalyticsContext,
) : AnalyticsTracker {

    override fun track(eventName: String, params: Map<String, String>) {
        val eventName = eventName.trim()
        if (eventName.isEmpty()) return
        val mergedParams = analyticsContext.params + params
        if (mergedParams.isEmpty()) {
            AppMetrica.reportEvent(eventName)
        } else {
            AppMetrica.reportEvent(eventName, mergedParams)
        }
    }

    override fun reportError(
        message: String,
        throwable: Throwable,
        groupIdentifier: String?,
    ) {
        val message = message.trim()
        if (message.isEmpty()) return
        val groupIdentifier = groupIdentifier?.trim().orEmpty()
        if (groupIdentifier.isEmpty()) {
            AppMetrica.reportError(message, throwable)
        } else {
            AppMetrica.reportError(groupIdentifier, message, throwable)
        }
        AppMetrica.sendEventsBuffer()
    }
}
