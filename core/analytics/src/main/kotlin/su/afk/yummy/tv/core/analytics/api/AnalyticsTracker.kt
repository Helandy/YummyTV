package su.afk.yummy.tv.core.analytics.api

/**
 * Sends analytics events to the configured destination.
 */
interface AnalyticsTracker {
    /**
     * Reports an analytics event by name. Blank event names are ignored by concrete implementations.
     */
    fun track(eventName: String, params: Map<String, String> = emptyMap())

    /**
     * Reports a non-fatal error. Blank messages are ignored by concrete implementations.
     */
    fun reportError(
        message: String,
        throwable: Throwable,
        groupIdentifier: String? = null,
    )

    /**
     * Free-form debug diagnostic. In debug it goes to logcat. In release it is a no-op, except for
     * tags listed in [PersistedLogTags.persisted], which are kept in the app log file.
     */
    fun log(tag: String, throwable: Throwable? = null, message: () -> String)
}
