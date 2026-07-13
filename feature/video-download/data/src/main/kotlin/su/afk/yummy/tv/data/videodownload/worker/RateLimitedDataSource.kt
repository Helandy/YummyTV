package su.afk.yummy.tv.data.videodownload.worker

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource

/**
 * Caps the aggregate download throughput of every [DataSource] it wraps to a fixed bytes/sec.
 *
 * Alloha's CDN blocks a session (403 session_blocked) once segment pulls exceed a rate that
 * real-time playback never reaches; an offline downloader fetches as fast as the network allows and
 * trips that limit within seconds. Throttling the upstream keeps a download under the ceiling.
 *
 * The [DownloadRateLimiter] is created once per [Factory] and shared across every concurrent segment
 * [DataSource], so the cap applies to the total throughput rather than per-connection.
 */
@OptIn(UnstableApi::class)
internal class RateLimitedDataSource(
    private val upstream: DataSource,
    private val limiter: DownloadRateLimiter,
) : DataSource by upstream {

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = upstream.read(buffer, offset, length)
        if (read > 0) limiter.acquire(read)
        return read
    }

    class Factory(
        private val upstream: DataSource.Factory,
        bytesPerSecond: Long,
    ) : DataSource.Factory {
        private val limiter = DownloadRateLimiter(bytesPerSecond)

        override fun createDataSource(): DataSource =
            RateLimitedDataSource(upstream.createDataSource(), limiter)
    }
}

/**
 * Reservation-based rate limiter shared across threads. Each [acquire] reserves the next free slot
 * on a virtual timeline advancing at [bytesPerSecond]; the caller sleeps until its slot. Concurrent
 * callers therefore share a single smooth budget instead of each getting the full rate.
 */
internal class DownloadRateLimiter(private val bytesPerSecond: Long) {
    private val lock = Any()
    private var availableAtNanos = System.nanoTime()

    fun acquire(bytes: Int) {
        if (bytes <= 0 || bytesPerSecond <= 0L) return
        val waitNanos: Long
        synchronized(lock) {
            val now = System.nanoTime()
            val start = maxOf(now, availableAtNanos)
            val costNanos = (bytes.toDouble() / bytesPerSecond * NANOS_PER_SECOND).toLong()
            availableAtNanos = start + costNanos
            waitNanos = start - now
        }
        if (waitNanos > 0L) {
            try {
                Thread.sleep(waitNanos / NANOS_PER_MILLI, (waitNanos % NANOS_PER_MILLI).toInt())
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
