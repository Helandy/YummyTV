package su.afk.yummy.tv.core.analytics

import android.content.Context

/**
 * Initializes the analytics provider for the process.
 */
interface AnalyticsInitializer {
    /**
     * Configures the provider with [apiKey]. Implementations may ignore blank keys.
     */
    fun initialize(context: Context, apiKey: String)
}
