package su.afk.yummy.tv.core.analytics

import android.content.Context
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import javax.inject.Inject

internal class AppMetricaAnalyticsInitializer @Inject constructor() : AnalyticsInitializer {

    override fun initialize(context: Context, apiKey: String) {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isEmpty()) return
        val config = AppMetricaConfig.newConfigBuilder(trimmedApiKey).build()
        AppMetrica.activate(context, config)
    }
}
