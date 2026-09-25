package su.afk.yummy.tv.android.startup

import su.afk.yummy.tv.android.startup.model.StartupMetrics
import su.afk.yummy.tv.android.startup.utils.toStartupDurationBucket
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import javax.inject.Inject

class StartupAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Холодный старт до первого кадра (TTID) и до отрисованной главной (TTFD) у реального
     * пользователя. Шлётся не больше раза на процесс, фоновые подъёмы процесса не репортятся.
     *
     * Параметры: ui, entry, ttid_ms, ttid_bucket, ttfd_ms, ttfd_bucket, fully_drawn,
     * app_on_create_ms, ram_gb, api, start_type, start_reason (последние два — API 35+).
     */
    fun eventAppStartup(metrics: StartupMetrics) {
        tracker.track(
            EVENT_APP_STARTUP,
            analyticsParamsOf(
                PARAM_UI to metrics.ui.analyticsValue,
                PARAM_ENTRY to metrics.entry.analyticsValue,
                PARAM_TTID_MS to metrics.ttidMs,
                PARAM_TTID_BUCKET to metrics.ttidMs.toStartupDurationBucket(),
                PARAM_TTFD_MS to metrics.ttfdMs,
                PARAM_TTFD_BUCKET to metrics.ttfdMs?.toStartupDurationBucket(),
                PARAM_FULLY_DRAWN to (metrics.ttfdMs != null),
                PARAM_APP_ON_CREATE_MS to metrics.appOnCreateMs,
                PARAM_RAM_GB to metrics.ramGb,
                PARAM_API to metrics.apiLevel,
                PARAM_START_TYPE to metrics.startType,
                PARAM_START_REASON to metrics.startReason,
            ),
        )
    }

    private companion object {
        const val EVENT_APP_STARTUP = "app_startup"

        const val PARAM_UI = "ui"
        const val PARAM_ENTRY = "entry"
        const val PARAM_TTID_MS = "ttid_ms"
        const val PARAM_TTID_BUCKET = "ttid_bucket"
        const val PARAM_TTFD_MS = "ttfd_ms"
        const val PARAM_TTFD_BUCKET = "ttfd_bucket"
        const val PARAM_FULLY_DRAWN = "fully_drawn"
        const val PARAM_APP_ON_CREATE_MS = "app_on_create_ms"
        const val PARAM_RAM_GB = "ram_gb"
        const val PARAM_API = "api"
        const val PARAM_START_TYPE = "start_type"
        const val PARAM_START_REASON = "start_reason"
    }
}
