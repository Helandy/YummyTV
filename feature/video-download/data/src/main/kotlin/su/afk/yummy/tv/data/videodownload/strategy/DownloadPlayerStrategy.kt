package su.afk.yummy.tv.data.videodownload.strategy

import su.afk.yummy.tv.data.videodownload.worker.utils.StreamKind
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem

/**
 * Encapsulates everything that differs between download sources (players) so the worker and
 * refresher never need to match player names/URLs themselves.
 */
internal interface DownloadPlayerStrategy {
    val playerLabel: String

    /**
     * Ротируются ли у источника подписанные URL сегментов. Определяет, чем считать идентичность
     * медиафайла в кэше: стабильным именем файла или полным URI.
     */
    fun usesRotatingSegmentUrls(streamKind: StreamKind): Boolean

    fun preferOkHttpUpstream(streamKind: StreamKind): Boolean

    fun decorateHeaders(headers: Map<String, String>, iframeUrl: String): Map<String, String>

    /** Cache resource key to evict on refresh so a fresh manifest is fetched while segments stay cached. */
    fun manifestKeyToEvictOnRefresh(item: VideoDownloadItem): String?

    val numericQualitiesOnly: Boolean
    val allowsQualityFallbackToHighest: Boolean
    val reusesHeadersOnRefresh: Boolean

    /**
     * Aggregate download throughput cap in bytes/sec, or null for unlimited. Applied to every
     * download regardless of player: Alloha's CDN blocks a session once bulk pulls outrun real-time
     * playback, and a modest ceiling keeps every source polite instead of fetching at full line
     * speed. Individual strategies may override.
     */
    val downloadBytesPerSecond: Long? get() = DEFAULT_DOWNLOAD_BYTES_PER_SECOND

    companion object {
        const val DEFAULT_DOWNLOAD_BYTES_PER_SECOND = 3L * 1024 * 1024
    }
}
