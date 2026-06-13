package su.afk.yummy.tv.core.analytics

import io.appmetrica.analytics.AppMetrica
import javax.inject.Inject

internal class AppMetricaAnalyticsTracker @Inject constructor() : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        val eventName = event.name.trim()
        if (eventName.isEmpty()) return
        if (event.params.isEmpty()) {
            AppMetrica.reportEvent(eventName)
        } else {
            AppMetrica.reportEvent(eventName, event.params)
        }
    }
}
