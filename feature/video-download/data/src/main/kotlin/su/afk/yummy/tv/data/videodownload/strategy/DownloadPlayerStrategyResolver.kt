package su.afk.yummy.tv.data.videodownload.strategy

import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.isCvhPlayerUrl
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject

/** Picks the [DownloadPlayerStrategy] matching a download's source player. */
internal class DownloadPlayerStrategyResolver @Inject constructor(
    private val allohaStrategy: AllohaDownloadStrategy,
    private val cvhStrategy: CvhDownloadStrategy,
) {
    fun resolve(item: VideoDownloadItem): DownloadPlayerStrategy = when {
        item.iframeUrl.isAllohaPlayerUrl() || item.playerName.isAllohaPlayerUrl() -> allohaStrategy
        item.iframeUrl.isCvhPlayerUrl() -> cvhStrategy
        else -> DefaultDownloadStrategy
    }
}
