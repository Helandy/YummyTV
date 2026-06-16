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

    override fun reportError(
        message: String,
        throwable: Throwable,
        groupIdentifier: String?,
    ) {
        val message = message.trim()
        if (message.isEmpty()) return
        Log.d(
            TAG,
            "Would send analytics error: message=$message, group=$groupIdentifier",
            throwable,
        )
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
