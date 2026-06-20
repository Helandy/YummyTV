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
            val settings = VarioqubSettings.Builder(normalizedClientId).build()
            val adapter = if (BuildConfig.DEBUG) {
                NoAnalyticsVarioqubAdapter(context)
            } else {
                AppMetricaAdapter(context)
            }
            Varioqub.init(settings, adapter, context)
            featureToggleState.markInitialized()
            Varioqub.activateConfig { }
            fetchConfig()
        }.onFailure { throwable ->
            featureToggleState.markNotInitialized()
            Log.w(TAG, "Failed to initialize feature toggles", throwable)
        }
    }

    private fun fetchConfig() {
        Varioqub.fetchConfig(object : OnFetchCompleteListener {
            override fun onSuccess() = Unit

            override fun onError(message: String, error: FetchError) {
                Log.w(TAG, "Failed to fetch feature toggles: $error $message")
            }
        })
    }

    private companion object {
        const val TAG = "FeatureToggles"
    }
}
