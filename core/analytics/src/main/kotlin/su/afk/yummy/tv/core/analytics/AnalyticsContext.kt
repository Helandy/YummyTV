package su.afk.yummy.tv.core.analytics

/**
 * Mutable process-wide analytics dimensions.
 *
 * These params are appended to every event by [AnalyticsTracker] implementations. Use this for
 * dimensions that describe the current app context, not for event-specific payload.
 */
interface AnalyticsContext {
    /**
     * Snapshot of currently active global params.
     */
    val params: Map<String, String>

    /**
     * Adds, replaces, or removes a global param.
     *
     * Passing null or a blank value removes the param. Blank keys are ignored.
     */
    fun setParam(key: String, value: String?)

    /**
     * Stores the current UI surface, for example mobile or TV.
     */
    fun setSurface(surface: String) {
        setParam(AnalyticsEvents.PARAM_SURFACE, surface)
    }

    /**
     * Stores the current Yani auth state.
     */
    fun setAuthState(isAuthorized: Boolean) {
        setParam(
            AnalyticsEvents.PARAM_AUTH_STATE,
            AnalyticsEvents.authState(isAuthorized),
        )
    }
}

internal class DefaultAnalyticsContext : AnalyticsContext {
    private val lock = Any()
    private var contextParams: Map<String, String> = emptyMap()

    override val params: Map<String, String>
        get() = synchronized(lock) { contextParams }

    override fun setParam(key: String, value: String?) {
        val normalizedKey = key.trim()
        if (normalizedKey.isBlank()) return

        synchronized(lock) {
            contextParams = if (value.isNullOrBlank()) {
                contextParams - normalizedKey
            } else {
                contextParams + (normalizedKey to value)
            }
        }
    }
}
