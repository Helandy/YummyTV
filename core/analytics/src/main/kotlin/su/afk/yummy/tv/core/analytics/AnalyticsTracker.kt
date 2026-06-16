package su.afk.yummy.tv.core.analytics

/**
 * Sends analytics events to the configured destination.
 *
 * Implementations merge [AnalyticsContext.params] with event params before reporting.
 * Event params intentionally win on key collisions so a rare event can override a global value.
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
     * Reports [event].
     */
    fun track(event: AnalyticsEvent) {
        track(event.name, event.params)
    }
}
