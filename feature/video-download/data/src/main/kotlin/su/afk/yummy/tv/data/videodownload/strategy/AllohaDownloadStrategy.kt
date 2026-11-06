package su.afk.yummy.tv.data.videodownload.strategy

import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.withDownloadRequestHeaders
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject

/**
 * Alloha's CDN plays only with its Origin/Referer and Chrome-like headers, so downloads override
 * them and keep their own cache-key handling.
 */
internal class AllohaDownloadStrategy @Inject constructor() : DownloadPlayerStrategy {
    override val playerLabel: String = "Alloha"

    override fun usesRotatingSegmentUrls(streamKind: StreamKind): Boolean =
        streamKind == StreamKind.Hls

    override fun decorateHeaders(
        headers: Map<String, String>,
        iframeUrl: String,
        defaultUserAgent: String,
    ): Map<String, String> =
        headers.withDownloadRequestHeaders(iframeUrl, defaultUserAgent, originOverride = ALLOHA_ORIGIN)

    override fun manifestKeyToEvictOnRefresh(item: VideoDownloadItem): String? =
        item.cacheKey.takeIf { item.streamUrl.streamKind().isAdaptive }

    override val numericQualitiesOnly: Boolean = true
    override val allowsQualityFallbackToHighest: Boolean = true
    override val reusesHeadersOnRefresh: Boolean = true
    // Throughput cap inherited from DownloadPlayerStrategy.DEFAULT_DOWNLOAD_BYTES_PER_SECOND.

    private companion object {
        const val ALLOHA_ORIGIN = "https://alloha.yani.tv"
    }
}
