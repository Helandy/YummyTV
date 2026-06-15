package su.afk.yummy.tv.core.analytics

import android.util.Log
import javax.inject.Inject

internal class LogcatAnalyticsTracker @Inject constructor(
    private val analyticsContext: AnalyticsContext,
) : AnalyticsTracker {

    override fun track(eventName: String, params: Map<String, String>) {
        val eventName = eventName.trim()
        if (eventName.isEmpty()) return
        val mergedParams = analyticsContext.params + params
        if (mergedParams.isEmpty()) {
            Log.d(TAG, "Would send analytics event: $eventName")
        } else {
            Log.d(TAG, "Would send analytics event: $eventName, params=$mergedParams")
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
