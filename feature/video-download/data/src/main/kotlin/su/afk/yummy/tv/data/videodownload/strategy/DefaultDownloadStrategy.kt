package su.afk.yummy.tv.data.videodownload.strategy

import androidx.media3.datasource.cache.CacheKeyFactory
import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.withDownloadRequestHeaders
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem

/** Generic download behavior used by every player without its own strategy (Kodik, Aksor, VK, Rutube, Sibnet, Zedfilm). */
internal object DefaultDownloadStrategy : DownloadPlayerStrategy {
    override val playerLabel: String = "Default"

    override fun usesLiveSession(streamKind: StreamKind): Boolean = false

    override suspend fun openLiveSession(item: VideoDownloadItem): AllohaStreamSession? = null

    override fun cacheKeyFactory(
        cacheKey: String,
        manifestUri: String,
        streamKind: StreamKind,
    ): CacheKeyFactory? = null

    override fun preferOkHttpUpstream(streamKind: StreamKind): Boolean = false

    override fun decorateHeaders(
        headers: Map<String, String>,
        iframeUrl: String
    ): Map<String, String> =
        headers.withDownloadRequestHeaders(iframeUrl)

    override fun shouldRefreshBeforeStart(item: VideoDownloadItem, runAttemptCount: Int): Boolean =
        false

    override fun manifestKeyToEvictOnRefresh(item: VideoDownloadItem): String? = null

    override val retriesViaLiveSession: Boolean = false
    override val numericQualitiesOnly: Boolean = false
    override val allowsQualityFallbackToHighest: Boolean = false
    override val reusesHeadersOnRefresh: Boolean = false
}
