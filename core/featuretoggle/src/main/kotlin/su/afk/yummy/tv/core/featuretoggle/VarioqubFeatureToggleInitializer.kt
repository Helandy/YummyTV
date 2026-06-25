package su.afk.yummy.tv.core.featuretoggle

import android.content.Context
import android.os.StrictMode
import com.yandex.varioqub.analyticadapter.VarioqubConfigAdapter
import com.yandex.varioqub.appmetricaadapter.AppMetricaAdapter
import com.yandex.varioqub.config.FetchError
import com.yandex.varioqub.config.OnFetchCompleteListener
import com.yandex.varioqub.config.Varioqub
import com.yandex.varioqub.config.VarioqubSettings
import su.afk.yummy.tv.core.logger.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VarioqubFeatureToggleInitializer @Inject constructor(
    private val featureToggleState: VarioqubFeatureToggleState,
) : FeatureToggleInitializer {

    override fun initialize(context: Context, clientId: String) {
        val normalizedClientId = clientId.trim()
        if (normalizedClientId.isEmpty()) {
            featureToggleState.markNotInitialized()
            return
        }

        runCatching {
            val settingsBuilder = VarioqubSettings.Builder(normalizedClientId)
            if (BuildConfig.DEBUG) {
                settingsBuilder
                    .withThrottleInterval(DEBUG_THROTTLE_INTERVAL_SECONDS)
                    .withLogs()
            }
            val settings = settingsBuilder.build()
            val adapter = if (BuildConfig.DEBUG) {
                NoAnalyticsVarioqubAdapter()
            } else {
                AppMetricaAdapter(context)
            }
            initializeVarioqub(settings, adapter, context)
            featureToggleState.markInitialized()
            activateConfig()
            fetchConfig()
        }.onFailure { throwable ->
            featureToggleState.markNotInitialized()
            AppLogger.w(TAG, throwable) { "Failed to initialize feature toggles" }
        }
    }

    private fun initializeVarioqub(
        settings: VarioqubSettings,
        adapter: VarioqubConfigAdapter,
        context: Context,
    ) {
        if (!BuildConfig.DEBUG) {
            Varioqub.init(settings, adapter, context)
            return
        }

        val previousPolicy = StrictMode.allowThreadDiskReads()
        try {
            Varioqub.init(settings, adapter, context)
        } finally {
            StrictMode.setThreadPolicy(previousPolicy)
        }
    }

    private fun fetchConfig() {
        Varioqub.fetchConfig(object : OnFetchCompleteListener {
            override fun onSuccess() {
                AppLogger.d(TAG) { "Feature toggles fetched successfully" }
                activateConfig()
            }

            override fun onError(message: String, error: FetchError) {
                AppLogger.w(TAG) { "Failed to fetch feature toggles: $error $message" }
            }
        })
    }

    private fun activateConfig() {
        Varioqub.activateConfig {
            AppLogger.d(TAG) { "Feature toggles activated" }
            featureToggleState.notifyConfigActivated()
        }
    }

    private companion object {
        const val TAG = "FeatureToggles"
        const val DEBUG_THROTTLE_INTERVAL_SECONDS = 2L
    }
}
