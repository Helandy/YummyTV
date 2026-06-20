package su.afk.yummy.tv.core.featuretoggle

interface FeatureToggleProvider {
    fun isEnabled(flag: FeatureFlag.BooleanFlag): Boolean

    fun getString(flag: FeatureFlag.StringFlag): String
}
