package su.afk.yummy.tv.core.featuretoggle

import android.content.Context
import android.util.Log
import com.yandex.varioqub.appmetricaadapter.AppMetricaAdapter
import com.yandex.varioqub.config.FetchError
import com.yandex.varioqub.config.OnFetchCompleteListener
import com.yandex.varioqub.config.Varioqub
import com.yandex.varioqub.config.VarioqubSettings
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
                NoAnalyticsVarioqubAdapter(context)
            } else {
                AppMetricaAdapter(context)
            }
            Varioqub.init(settings, adapter, context)
            featureToggleState.markInitialized()
            activateConfig()
            fetchConfig()
        }.onFailure { throwable ->
            featureToggleState.markNotInitialized()
            Log.w(TAG, "Failed to initialize feature toggles", throwable)
        }
    }

    private fun fetchConfig() {
        Varioqub.fetchConfig(object : OnFetchCompleteListener {
            override fun onSuccess() {
                Log.d(TAG, "Feature toggles fetched successfully")
                activateConfig()
            }

            override fun onError(message: String, error: FetchError) {
                Log.w(TAG, "Failed to fetch feature toggles: $error $message")
            }
        })
    }

    private fun activateConfig() {
        Varioqub.activateConfig {
            Log.d(TAG, "Feature toggles activated")
            featureToggleState.notifyConfigActivated()
        }
    }

    private companion object {
        const val TAG = "FeatureToggles"
        const val DEBUG_THROTTLE_INTERVAL_SECONDS = 2L
    }
}
