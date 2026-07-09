package su.afk.yummy.tv.data.videodownload.worker.utils

import android.os.SystemClock
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.InterruptedIOException

/**
 * Spaces cache-miss requests made by an HLS downloader.
 *
 * All DataSources created by this factory share one gate. Playlists are intentionally excluded so
 * a refreshed signed manifest can be read immediately; media, init and key requests are paced.
 */
internal class PacedHlsDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val intervalMs: Long,
) : DataSource.Factory {
    private val requestGate = RequestGate(intervalMs)

    override fun createDataSource(): DataSource =
        PacedHlsDataSource(upstreamFactory.createDataSource(), requestGate)
}

private class PacedHlsDataSource(
    private val upstream: DataSource,
    private val requestGate: RequestGate,
) : DataSource by upstream {
    override fun open(dataSpec: DataSpec): Long {
        if (!dataSpec.uri.path.orEmpty().endsWith(".m3u8", ignoreCase = true)) {
            requestGate.awaitTurn()
        }
        return upstream.open(dataSpec)
    }
}

private class RequestGate(
    private val intervalMs: Long,
) {
    private val lock = Any()
    private var nextRequestAtMs = 0L

    fun awaitTurn() {
        val waitMs = synchronized(lock) {
            val now = SystemClock.elapsedRealtime()
            val requestAt = maxOf(now, nextRequestAtMs)
            nextRequestAtMs = requestAt + intervalMs
            requestAt - now
        }
        if (waitMs <= 0L) return
        try {
            Thread.sleep(waitMs)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted while pacing HLS segment requests").apply {
                initCause(error)
            }
        }
    }
}
