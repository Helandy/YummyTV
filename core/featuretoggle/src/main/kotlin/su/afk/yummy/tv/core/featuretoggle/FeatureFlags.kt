package su.afk.yummy.tv.core.featuretoggle

object FeatureFlags {
    val all: Set<FeatureFlag<*>> = emptySet()

    val minSupportedAppVersion = FeatureFlag.StringFlag(
        key = "app_min_supported_version",
        defaultValue = "",
    )
}
