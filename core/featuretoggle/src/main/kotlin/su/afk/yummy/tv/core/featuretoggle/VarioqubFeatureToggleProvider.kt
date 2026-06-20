package su.afk.yummy.tv.core.featuretoggle

import com.yandex.varioqub.config.Varioqub
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VarioqubFeatureToggleProvider @Inject constructor(
    private val featureToggleState: VarioqubFeatureToggleState,
) : FeatureToggleProvider {

    override fun isEnabled(flag: FeatureFlag.BooleanFlag): Boolean {
        if (!featureToggleState.isInitialized) return flag.defaultValue
        return runCatching {
            Varioqub.getBoolean(flag.key, flag.defaultValue)
        }.getOrDefault(flag.defaultValue)
    }

    override fun getString(flag: FeatureFlag.StringFlag): String {
        if (!featureToggleState.isInitialized) return flag.defaultValue
        return runCatching {
            Varioqub.getString(flag.key, flag.defaultValue)
        }.getOrDefault(flag.defaultValue)
    }
}
