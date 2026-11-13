package su.afk.yummy.tv.source.anime4k.data.flag

import su.afk.yummy.tv.core.featuretoggle.api.FeatureFlag

/**
 * Удалённый выключатель источника: по умолчанию включён, флаг Varioqub `anime4k_enabled = false` отключает его.
 * Без рекламного блока и секрета подписи источник скрыт независимо от флага.
 */
internal object Anime4kFeatureFlag {
    val enabled = FeatureFlag.BooleanFlag(key = "anime4k_enabled", defaultValue = true)

    /** Показ rewarded-рекламы Яндекса перед 4K. false (по умолчанию) = реклама пропускается. */
    val adsEnabled = FeatureFlag.BooleanFlag(key = "anime4k_ads_enabled", defaultValue = false)

    /** Ad unit id rewarded-блока Яндекса. Пусто = берётся локальный `yummytv.yandexRewardedAdUnitId`. */
    val rewardedAdUnitId = FeatureFlag.StringFlag(key = "anime4k_yandex_rewarded_ad_unit_id", defaultValue = "")
}
