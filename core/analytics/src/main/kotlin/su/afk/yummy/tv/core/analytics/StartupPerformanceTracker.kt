package su.afk.yummy.tv.core.analytics

import android.os.Process
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks cold startup timing markers for the current process.
 *
 * Durations are measured from process start immediately when a marker happens. Events are kept in
 * memory until [flushPending] is called so startup timings can still include global analytics
 * context such as surface and auth state.
 */
@Singleton
class StartupPerformanceTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) {
    private val lock = Any()
    private val processStartElapsedRealtime = Process.getStartElapsedRealtime()
    private var activityStartup: ActivityStartup? = null
    private var analyticsContextReady = false
    private var pendingMetrics: List<StartupMetric> = emptyList()
    private var recordedMarkers: Set<String> = emptySet()

    /**
     * Marks the first UI Activity creation for this process.
     *
     * Only the first activity is used. If it appears too late after process start, startup markers
     * are ignored because the process was likely started by background work.
     */
    fun markUiActivityCreated(activity: String) {
        val processAgeMs = elapsedSinceProcessStart()
        synchronized(lock) {
            if (activityStartup != null) return
            activityStartup = ActivityStartup(
                activity = activity,
                processAgeMs = processAgeMs,
                isColdStart = processAgeMs <= MAX_COLD_START_PROCESS_AGE_MS,
            )
        }
    }

    /**
     * Records the first frame marker, usually from the Activity root view pre-draw callback.
     */
    fun markFirstFrame() {
        recordMarker(marker = MARKER_FIRST_FRAME)
    }

    /**
     * Records the first visible analytics destination marker.
     */
    fun markFirstDestinationVisible(screenName: String) {
        recordMarker(
            marker = MARKER_FIRST_DESTINATION_VISIBLE,
            screenName = screenName,
        )
    }

    /**
     * Allows pending startup metrics to be reported.
     *
     * Call this after required global analytics context has been set. Future markers are reported
     * immediately.
     */
    fun flushPending() {
        val metrics = synchronized(lock) {
            analyticsContextReady = true
            pendingMetrics.also { pendingMetrics = emptyList() }
        }
        metrics.forEach(::track)
    }

    private fun recordMarker(marker: String, screenName: String? = null) {
        val metric = synchronized(lock) {
            val startup = activityStartup ?: return
            if (!startup.isColdStart || marker in recordedMarkers) return
            recordedMarkers = recordedMarkers + marker
            val metric = StartupMetric(
                marker = marker,
                durationMs = elapsedSinceProcessStart(),
                activity = startup.activity,
                screenName = screenName,
                processAgeAtActivityCreateMs = startup.processAgeMs,
            )
            if (analyticsContextReady) {
                metric
            } else {
                pendingMetrics = pendingMetrics + metric
                null
            }
        }
        metric?.let(::track)
    }

    private fun track(metric: StartupMetric) {
        analyticsTracker.track(
            AnalyticsEvents.appStartupTime(
                marker = metric.marker,
                durationMs = metric.durationMs,
                activity = metric.activity,
                screenName = metric.screenName,
                processAgeAtActivityCreateMs = metric.processAgeAtActivityCreateMs,
            )
        )
    }

    private fun elapsedSinceProcessStart(): Long =
        (SystemClock.elapsedRealtime() - processStartElapsedRealtime).coerceAtLeast(0L)

    private data class ActivityStartup(
        val activity: String,
        val processAgeMs: Long,
        val isColdStart: Boolean,
    )

    private data class StartupMetric(
        val marker: String,
        val durationMs: Long,
        val activity: String,
        val screenName: String?,
        val processAgeAtActivityCreateMs: Long,
    )

    private companion object {
        const val MARKER_FIRST_FRAME = "first_frame"
        const val MARKER_FIRST_DESTINATION_VISIBLE = "first_destination_visible"
        const val MAX_COLD_START_PROCESS_AGE_MS = 30_000L
    }
}
