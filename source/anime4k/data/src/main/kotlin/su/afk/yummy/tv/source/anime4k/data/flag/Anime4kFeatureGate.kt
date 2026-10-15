package su.afk.yummy.tv.source.anime4k.data.flag

import su.afk.yummy.tv.core.featuretoggle.api.FeatureToggleProvider

/**
 * Включён ли источник «4K». В debug-сборке включён всегда ([forceEnabled]), чтобы проверять его без Varioqub;
 * в релизе решает удалённый флаг `anime4k_enabled` (по умолчанию включён, `false` отключает источник).
 */
internal class Anime4kFeatureGate(
    private val featureToggleProvider: FeatureToggleProvider,
    private val forceEnabled: Boolean,
) {
    fun isEnabled(): Boolean = forceEnabled || featureToggleProvider.isEnabled(Anime4kFeatureFlag.enabled)
}
