package su.afk.yummy.tv.core.analytics.logcat

import su.afk.yummy.tv.core.analytics.api.AnalyticsDeviceIdProvider
import javax.inject.Inject

internal class NoOpAnalyticsDeviceIdProvider @Inject constructor() : AnalyticsDeviceIdProvider {

    override suspend fun deviceId(): String? = null
}
