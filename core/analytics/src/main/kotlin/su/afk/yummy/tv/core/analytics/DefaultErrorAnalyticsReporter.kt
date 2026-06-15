package su.afk.yummy.tv.core.analytics

import io.appmetrica.analytics.AppMetrica
import javax.inject.Inject

internal class DefaultErrorAnalyticsReporter @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ErrorAnalyticsReporter {

    override fun reportCoroutineError(owner: String, throwable: Throwable) {
        analyticsTracker.track(AnalyticsEvents.coroutineError(owner, throwable))
        if (!BuildConfig.DEBUG) {
            AppMetrica.reportError(
                ERROR_IDENTIFIER,
                buildErrorMessage(owner, throwable),
                throwable,
            )
        }
    }

    private fun buildErrorMessage(owner: String, throwable: Throwable): String =
        "$owner: ${throwable::class.java.name}"

    private companion object {
        const val ERROR_IDENTIFIER = "coroutine_error"
    }
}
