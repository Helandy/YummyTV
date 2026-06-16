package su.afk.yummy.tv.core.update

import android.os.Build
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import javax.inject.Inject

internal class UpdateAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {

    fun eventDismiss(version: String?) {
        trackAction(ACTION_DISMISS, version)
    }

    fun eventConfirm(version: String?) {
        trackAction(ACTION_CONFIRM, version)
    }

    fun eventRetry(version: String?) {
        trackAction(ACTION_RETRY, version)
    }

    fun eventDownloadError(version: String?, error: Throwable) {
        trackError(PHASE_DOWNLOAD, version, error)
    }

    fun eventInstallError(version: String?, error: Throwable) {
        trackError(PHASE_INSTALL, version, error)
    }

    private fun trackAction(action: String, version: String?) {
        tracker.track(
            AnalyticsEvents.updateAction(
                action = action,
                params = analyticsParamsOf(PARAM_VERSION to version),
            )
        )
    }

    private fun trackError(phase: String, version: String?, error: Throwable) {
        val params = errorParams(phase, version, error)
        tracker.track(AnalyticsEvents.updateError(params))
        tracker.reportError(
            groupIdentifier = EVENT_UPDATE_ERROR,
            message = errorReportMessage(params),
            throwable = error,
        )
    }

    private fun errorParams(
        phase: String,
        version: String?,
        error: Throwable
    ): Map<String, String> =
        analyticsParamsOf(
            PARAM_VERSION to version,
            PARAM_PHASE to phase,
            PARAM_SDK_INT to Build.VERSION.SDK_INT,
            PARAM_ERROR_TYPE to error.analyticsType(),
            PARAM_ERROR_MESSAGE to error.analyticsMessage(),
        )

    private fun errorReportMessage(params: Map<String, String>): String =
        buildString {
            append("Update failed")
            listOf(PARAM_PHASE, PARAM_SDK_INT, PARAM_VERSION, PARAM_ERROR_TYPE).forEach { key ->
                params[key]?.let { value ->
                    append(", ")
                    append(key)
                    append("=")
                    append(value)
                }
            }
        }

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    private fun Throwable.analyticsMessage(): String? =
        (localizedMessage ?: message)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.lineSequence()
            ?.joinToString(" ")
            ?.take(MAX_ERROR_MESSAGE_LENGTH)

    private companion object {
        const val ACTION_CONFIRM = "confirm"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_RETRY = "retry"
        const val EVENT_UPDATE_ERROR = "update_error"
        const val PHASE_DOWNLOAD = "download"
        const val PHASE_INSTALL = "install"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_ERROR_TYPE = "error_type"
        const val PARAM_PHASE = "phase"
        const val PARAM_SDK_INT = "sdk_int"
        const val PARAM_VERSION = "version"
        const val MAX_ERROR_MESSAGE_LENGTH = 300
    }
}
