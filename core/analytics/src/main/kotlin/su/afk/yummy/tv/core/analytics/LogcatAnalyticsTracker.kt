package su.afk.yummy.tv.core.analytics

import android.util.Log
import javax.inject.Inject

internal class LogcatAnalyticsTracker @Inject constructor() : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        val eventName = event.name.trim()
        if (eventName.isEmpty()) return
        if (event.params.isEmpty()) {
            Log.d(TAG, "Would send analytics event: $eventName")
        } else {
            Log.d(TAG, "Would send analytics event: $eventName, params=${event.params}")
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
