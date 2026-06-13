package su.afk.yummy.tv.core.analytics

import android.content.Context
import javax.inject.Inject

internal class NoOpAnalyticsInitializer @Inject constructor() : AnalyticsInitializer {

    override fun initialize(context: Context, apiKey: String) = Unit
}
