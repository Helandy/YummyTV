package su.afk.yummy.tv.core.analytics

/**
 * Immutable analytics event payload before global [AnalyticsContext] params are merged in.
 *
 * Keep [params] limited to event-specific values. Common dimensions such as surface and auth state
 * belong in [AnalyticsContext] so they are applied consistently by [AnalyticsTracker].
 */
data class AnalyticsEvent(
    /**
     * Provider event name.
     */
    val name: String,

    /**
     * Event-specific parameters. Values must already be normalized to strings.
     */
    val params: Map<String, String> = emptyMap(),
)
