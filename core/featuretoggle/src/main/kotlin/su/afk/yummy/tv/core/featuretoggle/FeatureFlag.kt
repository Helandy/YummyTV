package su.afk.yummy.tv.core.featuretoggle

sealed class FeatureFlag<T>(
    val key: String,
    val defaultValue: T,
) {
    class BooleanFlag(
        key: String,
        defaultValue: Boolean,
    ) : FeatureFlag<Boolean>(key, defaultValue)

    class StringFlag(
        key: String,
        defaultValue: String,
    ) : FeatureFlag<String>(key, defaultValue)
}
