package su.afk.yummy.tv.data.videodownload.strategy

import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.withDownloadRequestHeaders
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem

/** Generic download behavior used by every player without its own strategy (Kodik, Aksor, VK, Rutube, Sibnet, Zedfilm). */
internal object DefaultDownloadStrategy : DownloadPlayerStrategy {
    override val playerLabel: String = "Default"

    override fun usesRotatingSegmentUrls(streamKind: StreamKind): Boolean = false

    override fun decorateHeaders(
        headers: Map<String, String>,
        iframeUrl: String
    ): Map<String, String> =
        headers.withDownloadRequestHeaders(iframeUrl)

    override fun manifestKeyToEvictOnRefresh(item: VideoDownloadItem): String? =
        item.cacheKey.takeIf { item.streamUrl.streamKind().isAdaptive }

    override val numericQualitiesOnly: Boolean = false
    override val allowsQualityFallbackToHighest: Boolean = false
    override val reusesHeadersOnRefresh: Boolean = false
}
