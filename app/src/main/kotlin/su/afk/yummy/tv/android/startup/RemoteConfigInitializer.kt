package su.afk.yummy.tv.android.startup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.BuildConfig
import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlags
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleInitializer
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider
import su.afk.yummy.tv.core.network.yani.YaniApiConfig
import su.afk.yummy.tv.core.network.yummy.SiteApiConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Поднимает feature toggles и раздаёт удалённые адреса API сетевым конфигам. Должен отработать
 * до первого сетевого запроса, поэтому выполняется синхронно в Application.onCreate.
 */
@Singleton
class RemoteConfigInitializer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val featureToggleInitializer: FeatureToggleInitializer,
    private val featureToggleProvider: FeatureToggleProvider,
    private val siteApiConfig: SiteApiConfig,
    private val yaniApiConfig: YaniApiConfig,
) {
    fun initialize() {
        featureToggleInitializer.initialize(context, BuildConfig.VARIOQUB_CLIENT_ID)
        // Один раз за запуск: initialize уже применил закэшированный конфиг прошлой сессии.
        siteApiConfig.apply(
            remoteBaseUrl = featureToggleProvider.getString(FeatureFlags.siteApiBaseUrl),
            remoteToken = featureToggleProvider.getString(FeatureFlags.siteApiToken),
        )
        yaniApiConfig.apply(featureToggleProvider.getString(FeatureFlags.yaniApiBaseUrl))
    }
}
