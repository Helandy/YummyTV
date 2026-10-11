package su.afk.yummy.tv.core.analytics.appmetrica

import android.content.Context
import android.content.res.Configuration
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import io.appmetrica.analytics.PredefinedDeviceTypes
import su.afk.yummy.tv.core.analytics.api.initialize.AnalyticsInitializer
import javax.inject.Inject

internal class AppMetricaAnalyticsInitializer @Inject constructor(
    private val activationState: AppMetricaActivationState,
) : AnalyticsInitializer {

    override fun initialize(context: Context, apiKey: String) {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isEmpty()) return
        val config = AppMetricaConfig.newConfigBuilder(trimmedApiKey)
            .withDeviceType(context.resolveAppMetricaDeviceType())
            .withAdvIdentifiersTracking(false)
            .build()
        AppMetrica.activate(context, config)
        activationState.markActivated()
    }
}

private fun Context.resolveAppMetricaDeviceType(): String {
    val configuration = resources.configuration
    return when (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) {
        Configuration.UI_MODE_TYPE_TELEVISION -> PredefinedDeviceTypes.TV
        Configuration.UI_MODE_TYPE_CAR -> PredefinedDeviceTypes.CAR
        else -> if (configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP) {
            PredefinedDeviceTypes.TABLET
        } else {
            PredefinedDeviceTypes.PHONE
        }
    }
}

private const val TABLET_SMALLEST_WIDTH_DP = 600
