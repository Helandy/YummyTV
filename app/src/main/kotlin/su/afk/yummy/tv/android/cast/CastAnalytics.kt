package su.afk.yummy.tv.android.cast

import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.utils.analyticsParamsOf
import su.afk.yummy.tv.core.utils.cast.CastSupport
import javax.inject.Inject

class CastAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Cast выключен гейтом [CastSupport] - шлём причину и версию GMS, по ней же калибруется порог
     * MIN_GMS_APK_VERSION. ТВ не репортим: это штатное состояние большей части установок, событие
     * на каждый запуск приставки ничего не объясняет.
     *
     * Параметры: reason, gms_version, availability.
     */
    fun eventCastUnavailable(decision: CastSupport.Decision) {
        when (decision.status) {
            CastSupport.Status.SUPPORTED, CastSupport.Status.TELEVISION -> return

            CastSupport.Status.PLAY_SERVICES_TOO_OLD -> tracker.track(
                EVENT_CAST_UNAVAILABLE,
                analyticsParamsOf(
                    PARAM_REASON to REASON_PLAY_SERVICES_TOO_OLD,
                    PARAM_GMS_VERSION to decision.gmsApkVersion,
                    PARAM_AVAILABILITY to decision.availabilityCode,
                ),
            )

            CastSupport.Status.CHECK_FAILED -> {
                val error = decision.error ?: return
                tracker.reportError(
                    groupIdentifier = EVENT_CAST_CHECK_FAILED,
                    message = ERROR_MESSAGE_CHECK_FAILED,
                    throwable = error,
                )
            }
        }
    }

    internal companion object {
        private const val PARAM_REASON = "reason"
        private const val PARAM_GMS_VERSION = "gms_version"
        private const val PARAM_AVAILABILITY = "availability"
        private const val REASON_PLAY_SERVICES_TOO_OLD = "play_services_too_old"
        private const val ERROR_MESSAGE_CHECK_FAILED = "Cast support check failed"

        const val EVENT_CAST_UNAVAILABLE = "cast_unavailable"
        const val EVENT_CAST_CHECK_FAILED = "cast_support_check_failed"
    }
}
