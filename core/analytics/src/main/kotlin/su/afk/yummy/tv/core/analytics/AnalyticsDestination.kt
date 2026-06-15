package su.afk.yummy.tv.core.analytics

/**
 * Navigation destination that can be reported as an analytics screen.
 *
 * Feature API modules implement this on their NavKey objects so the app shell can report
 * screen visibility without depending on concrete feature screens.
 */
interface AnalyticsDestination {
    /**
     * Stable analytics screen identifier.
     */
    val screenName: String

    /**
     * Optional screen-specific parameters, such as an entity id.
     */
    val screenParams: Map<String, String>
        get() = emptyMap()
}
