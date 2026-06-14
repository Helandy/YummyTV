package su.afk.yummy.tv.core.analytics

import io.appmetrica.analytics.AppMetrica
import javax.inject.Inject

internal class AppMetricaAnalyticsTracker @Inject constructor(
    private val analyticsContext: AnalyticsContext,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        val eventName = event.name.trim()
        if (eventName.isEmpty()) return
        val params = analyticsContext.params + event.params
        if (params.isEmpty()) {
            AppMetrica.reportEvent(eventName)
        } else {
            AppMetrica.reportEvent(eventName, params)
        }
    }
}
