package su.afk.yummy.tv.data.videodownload.strategy

import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.core.utils.player.isAllohaPlayerUrl
import su.afk.yummy.tv.core.utils.player.isCvhPlayerUrl
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject

/** Picks the [DownloadPlayerStrategy] matching a download's source player. */
internal class DownloadPlayerStrategyResolver @Inject constructor(
    private val allohaStrategy: AllohaDownloadStrategy,
    private val cvhStrategy: CvhDownloadStrategy,
) {
    fun resolve(item: VideoDownloadItem): DownloadPlayerStrategy =
        resolve(iframeUrl = item.iframeUrl, playerName = item.playerName)

    fun resolve(iframeUrl: String, playerName: String): DownloadPlayerStrategy = when {
        iframeUrl.isAllohaPlayerUrl() || playerName.isAllohaPlayerUrl() -> allohaStrategy
        iframeUrl.isCvhPlayerUrl() -> cvhStrategy
        else -> DefaultDownloadStrategy
    }

    /**
     * Схема ключей кэша для новой загрузки. Считается один раз при постановке в очередь и дальше
     * только читается: пересчёт посреди загрузки осиротил бы уже скачанные сегменты.
     */
    fun cacheKeyScheme(
        iframeUrl: String,
        playerName: String,
        streamUrl: String,
    ): VideoDownloadCacheKeyScheme =
        if (resolve(iframeUrl, playerName).usesRotatingSegmentUrls(streamUrl.streamKind())) {
            VideoDownloadCacheKeyScheme.NamespacedRotating
        } else {
            VideoDownloadCacheKeyScheme.Namespaced
        }
}
