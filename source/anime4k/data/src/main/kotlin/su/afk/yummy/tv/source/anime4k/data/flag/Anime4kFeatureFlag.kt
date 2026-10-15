package su.afk.yummy.tv.source.anime4k.data.flag

import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlag

/**
 * Удалённый выключатель источника: по умолчанию включён, флаг Varioqub `anime4k_enabled = false` отключает его.
 * Без рекламного блока и секрета подписи источник скрыт независимо от флага.
 */
internal object Anime4kFeatureFlag {
    val enabled = FeatureFlag.BooleanFlag(key = "anime4k_enabled", defaultValue = true)
}
