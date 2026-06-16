package su.afk.yummy.tv.core.analytics

import su.afk.yummy.tv.core.analytics.AnalyticsEvents.PARAM_AUTH_STATE
import su.afk.yummy.tv.core.analytics.AnalyticsEvents.PARAM_YANI_APPLICATION_TOKEN_STATE


/**
 * Factory for stable analytics events and shared analytics parameter values.
 *
 * These factories should only include event-specific params.
 */
object AnalyticsEvents {
    /**
     * Shared parameter names used across analytics events and context.
     */
    const val PARAM_ACTION = "action"
    const val PARAM_SCREEN = "screen"
    const val PARAM_AUTH_STATE = "auth_state"
    const val PARAM_YANI_APPLICATION_TOKEN_STATE = "yani_application_token_state"

    /**
     * Values for [PARAM_AUTH_STATE].
     */
    const val AUTH_STATE_AUTHORIZED = "authorized"
    const val AUTH_STATE_ANONYMOUS = "anonymous"

    /**
     * Values for [PARAM_YANI_APPLICATION_TOKEN_STATE].
     */
    const val YANI_APPLICATION_TOKEN_STATE_DEFAULT = "default"
    const val YANI_APPLICATION_TOKEN_STATE_CUSTOM = "custom"

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
     * Reports an update flow error.
     */
    fun updateError(
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "update_error",
        params = mapOf(PARAM_SCREEN to "update") + params,
    )

    /**
     * Reports an app session after auth state is resolved.
     */
    fun appSession(
        isAuthorized: Boolean,
        yaniApplicationTokenState: String,
    ): AnalyticsEvent = AnalyticsEvent(
        name = "app_session",
        params = analyticsParamsOf(
            PARAM_AUTH_STATE to authState(isAuthorized),
            PARAM_YANI_APPLICATION_TOKEN_STATE to yaniApplicationTokenState,
        ),
    )

    /**
     * Converts a boolean auth flag to the stable analytics value.
     */
    fun authState(isAuthorized: Boolean): String =
        if (isAuthorized) AUTH_STATE_AUTHORIZED else AUTH_STATE_ANONYMOUS
}
