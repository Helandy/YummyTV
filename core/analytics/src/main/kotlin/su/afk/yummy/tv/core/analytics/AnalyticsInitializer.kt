package su.afk.yummy.tv.core.analytics

import android.content.Context

interface AnalyticsInitializer {
    fun initialize(context: Context, apiKey: String)
}
