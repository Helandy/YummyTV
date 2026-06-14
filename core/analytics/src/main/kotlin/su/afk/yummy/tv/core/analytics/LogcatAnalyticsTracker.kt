package su.afk.yummy.tv.core.analytics

import android.util.Log
import javax.inject.Inject

internal class LogcatAnalyticsTracker @Inject constructor(
    private val analyticsContext: AnalyticsContext,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        val eventName = event.name.trim()
        if (eventName.isEmpty()) return
        val params = analyticsContext.params + event.params
        if (params.isEmpty()) {
            Log.d(TAG, "Would send analytics event: $eventName")
        } else {
            Log.d(TAG, "Would send analytics event: $eventName, params=$params")
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
