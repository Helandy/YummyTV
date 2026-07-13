package su.afk.yummy.tv.data.videodownload.strategy

import androidx.media3.datasource.cache.CacheKeyFactory
import su.afk.yummy.tv.data.videodownload.cache.RotatingHlsCacheKeyFactory
import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.data.videodownload.worker.utils.withDownloadRequestHeaders
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.usecase.OpenAllohaStreamSessionUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import javax.inject.Inject

/**
 * Alloha serves HLS through a live, WebView-backed session with rotating signed headers/segments,
 * so downloads need their own session lifecycle, retry policy, and cache-key handling.
 */
internal class AllohaDownloadStrategy @Inject constructor(
    private val openAllohaStreamSession: OpenAllohaStreamSessionUseCase,
) : DownloadPlayerStrategy {
    override val playerLabel: String = "Alloha"

    override fun usesLiveSession(streamKind: StreamKind): Boolean = streamKind == StreamKind.Hls

    override suspend fun openLiveSession(item: VideoDownloadItem): AllohaStreamSession? =
        openAllohaStreamSession(
            PlayerStreamRequest(
                iframeUrl = item.iframeUrl,
                autoQualityLabel = item.qualityLabel,
                sessionFallbackTtlSeconds = ALLOHA_DOWNLOAD_FALLBACK_SESSION_TTL_SECONDS,
                reusePlaybackSession = false,
            )
        )

    override fun cacheKeyFactory(
        cacheKey: String,
        manifestUri: String,
        streamKind: StreamKind,
    ): CacheKeyFactory? =
        if (streamKind == StreamKind.Hls) {
            RotatingHlsCacheKeyFactory(downloadCacheKey = cacheKey, manifestUri = manifestUri)
        } else {
            null
        }

    override fun preferOkHttpUpstream(streamKind: StreamKind): Boolean = streamKind.isAdaptive

    override fun decorateHeaders(
        headers: Map<String, String>,
        iframeUrl: String
    ): Map<String, String> =
        headers.withDownloadRequestHeaders(iframeUrl, originOverride = ALLOHA_ORIGIN)

    override fun manifestKeyToEvictOnRefresh(item: VideoDownloadItem): String? =
        item.cacheKey.takeIf { item.streamUrl.streamKind().isAdaptive }

    override val numericQualitiesOnly: Boolean = true
    override val allowsQualityFallbackToHighest: Boolean = true
    override val reusesHeadersOnRefresh: Boolean = true
    // Throughput cap inherited from DownloadPlayerStrategy.DEFAULT_DOWNLOAD_BYTES_PER_SECOND.

    private companion object {
        const val ALLOHA_DOWNLOAD_FALLBACK_SESSION_TTL_SECONDS = 55
        const val ALLOHA_ORIGIN = "https://alloha.yani.tv"
    }
}
