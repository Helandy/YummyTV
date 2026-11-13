package su.afk.yummy.tv.core.featuretoggle

import androidx.lifecycle.LifecycleOwner
import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleInitializer
import su.afk.yummy.tv.core.utils.system.ProcessLifecycleCoordinator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FeatureToggleInitializer.initialize] запускает SDK один раз при холодном старте (см.
 * `RemoteConfigInitializer` в :app); этот класс дополнительно дёргает лёгкий
 * [FeatureToggleInitializer.refresh] при каждом возврате приложения на передний план, чтобы
 * A/B-эксперименты и флаги не протухали, пока пользователь долго держит приложение в фоне.
 */
@Singleton
class FeatureToggleRefreshCoordinator @Inject constructor(
    private val featureToggleInitializer: FeatureToggleInitializer,
) : ProcessLifecycleCoordinator() {

    override fun onStart(owner: LifecycleOwner) {
        featureToggleInitializer.refresh()
    }
}
