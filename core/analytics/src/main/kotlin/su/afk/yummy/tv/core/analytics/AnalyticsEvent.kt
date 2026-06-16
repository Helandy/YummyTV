package su.afk.yummy.tv.core.analytics

/**
 * Immutable analytics event payload before global [AnalyticsContext] params are merged in.
 *
 * Keep [params] limited to event-specific values. Use [AnalyticsContext] only for dimensions that
 * truly need to be applied consistently by [AnalyticsTracker].
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
