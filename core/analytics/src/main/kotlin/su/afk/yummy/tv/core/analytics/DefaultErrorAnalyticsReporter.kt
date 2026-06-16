package su.afk.yummy.tv.core.analytics

import javax.inject.Inject

internal class DefaultErrorAnalyticsReporter @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ErrorAnalyticsReporter {

    override fun reportCoroutineError(owner: String, throwable: Throwable) {
        analyticsTracker.reportError(
            groupIdentifier = ERROR_IDENTIFIER,
            message = buildErrorMessage(owner, throwable),
            throwable = throwable,
        )
    }

    private fun buildErrorMessage(owner: String, throwable: Throwable): String =
        "$owner: ${throwable::class.java.name}"

    private companion object {
        const val ERROR_IDENTIFIER = "coroutine_error"
    }
}
