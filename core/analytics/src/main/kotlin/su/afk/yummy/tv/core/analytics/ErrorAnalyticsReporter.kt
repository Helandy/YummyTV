package su.afk.yummy.tv.core.analytics

/**
 * Reports app errors that should be visible in analytics and crash diagnostics.
 */
interface ErrorAnalyticsReporter {
    fun reportCoroutineError(owner: String, throwable: Throwable)
}
