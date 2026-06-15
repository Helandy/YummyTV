package su.afk.yummy.tv.core.analytics

import su.afk.yummy.tv.core.analytics.AnalyticsEvents.PARAM_AUTH_STATE
import su.afk.yummy.tv.core.analytics.AnalyticsEvents.PARAM_SURFACE


/**
 * Factory for stable analytics events and shared analytics parameter values.
 *
 * These factories should only include event-specific params. Global dimensions such as surface and
 * auth state are stored in [AnalyticsContext] and merged by [AnalyticsTracker].
 */
object AnalyticsEvents {
    /**
     * Shared parameter names used across analytics events and context.
     */
    const val PARAM_ACTION = "action"
    const val PARAM_SCREEN = "screen"
    const val PARAM_SURFACE = "surface"
    const val PARAM_AUTH_STATE = "auth_state"

    /**
     * Values for [PARAM_SURFACE].
     */
    const val SURFACE_MOBILE = "mobile"
    const val SURFACE_TV = "tv"

    /**
     * Values for [PARAM_AUTH_STATE].
     */
    const val AUTH_STATE_AUTHORIZED = "authorized"
    const val AUTH_STATE_ANONYMOUS = "anonymous"

    /**
     * Reports a destination becoming visible.
     */
    fun screenView(
        screenName: String,
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "screen_view",
        params = mapOf(PARAM_SCREEN to screenName) + params,
    )

    /**
     * Reports an explicit search submission.
     */
    fun searchSubmit(
        screenName: String,
        hasQuery: Boolean,
        filterCount: Int,
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "search_submit",
        params = mapOf(
            PARAM_SCREEN to screenName,
            "has_query" to hasQuery.toString(),
            "filter_count" to filterCount.toString(),
        ) + params,
    )

    /**
     * Reports applying or clearing search filters.
     */
    fun searchFiltersApply(
        screenName: String,
        action: String,
        hasQuery: Boolean,
        filterCount: Int,
    ): AnalyticsEvent = AnalyticsEvent(
        name = "search_filters_apply",
        params = mapOf(
            PARAM_SCREEN to screenName,
            PARAM_ACTION to action,
            "has_query" to hasQuery.toString(),
            "filter_count" to filterCount.toString(),
        ),
    )

    /**
     * Reports a settings value change.
     */
    fun settingChange(
        setting: String,
        value: String,
    ): AnalyticsEvent = AnalyticsEvent(
        name = "setting_change",
        params = mapOf(
            "setting" to setting,
            "value" to value,
        ),
    )

    /**
     * Reports an action in the player.
     */
    fun playerAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "player_action",
        params = mapOf(
            PARAM_SCREEN to "player",
            PARAM_ACTION to action,
        ) + params,
    )

    /**
     * Reports a player error.
     */
    fun playerError(
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "player_error",
        params = mapOf(PARAM_SCREEN to "player") + params,
    )

    /**
     * Reports an error screen being shown.
     */
    fun errorShown(
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "error_shown",
        params = mapOf(PARAM_SCREEN to "error") + params,
    )

    /**
     * Reports an unhandled coroutine error from a ViewModel.
     */
    fun coroutineError(
        viewModel: String,
        throwable: Throwable,
    ): AnalyticsEvent = AnalyticsEvent(
        name = "coroutine_error",
        params = analyticsParamsOf(
            "view_model" to viewModel,
            "exception_type" to throwable::class.java.name,
            "exception_message" to throwable.message?.take(MAX_EXCEPTION_MESSAGE_LENGTH),
            "cause_type" to throwable.cause?.let { it::class.java.name },
        ),
    )

    /**
     * Reports an update flow action.
     */
    fun updateAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "update_action",
        params = mapOf(
            PARAM_SCREEN to "update",
            PARAM_ACTION to action,
        ) + params,
    )

    /**
     * Reports an app session after auth state is resolved.
     */
    fun appSession(): AnalyticsEvent = AnalyticsEvent(name = "app_session")

    /**
     * Reports a cold startup timing marker.
     *
     * [durationMs] is measured when the marker happens, even if the event is flushed later after
     * analytics context is ready. [screenName] is only present for destination-based markers.
     */
    fun appStartupTime(
        marker: String,
        durationMs: Long,
        activity: String,
        screenName: String? = null,
        processAgeAtActivityCreateMs: Long? = null,
    ): AnalyticsEvent = AnalyticsEvent(
        name = "app_startup_time",
        params = analyticsParamsOf(
            "marker" to marker,
            "duration_ms" to durationMs,
            "activity" to activity,
            PARAM_SCREEN to screenName,
            "process_age_at_activity_create_ms" to processAgeAtActivityCreateMs,
        ),
    )

    /**
     * Converts a boolean auth flag to the stable analytics value.
     */
    fun authState(isAuthorized: Boolean): String =
        if (isAuthorized) AUTH_STATE_AUTHORIZED else AUTH_STATE_ANONYMOUS

    private const val MAX_EXCEPTION_MESSAGE_LENGTH = 300
}
