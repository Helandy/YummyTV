package su.afk.yummy.tv.source.anime4k.data.ads

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.reportResolveStep
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Заглушка для сборки без рекламного SDK (`yummytv.ads.yandex=false`): рекламы нет,
 * согласие и награда выдаются сразу, как при выключенном флаге `anime4k_ads_enabled`.
 */
@Singleton
internal class YandexRewardedAdRepository @Inject constructor() : RewardedAdRepository {

    override val isAvailable: Boolean = true

    override suspend fun askConsent(): Boolean {
        reportResolveStep(PlayerStreamResolveStatus.AskingConsent)
        return true
    }

    override suspend fun confirmBeforeOpen(): Boolean = true

    override suspend fun showRewardedAd(): Boolean = true
}
