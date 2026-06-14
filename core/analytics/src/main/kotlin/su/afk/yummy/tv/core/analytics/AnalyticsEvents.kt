package su.afk.yummy.tv.core.analytics

object AnalyticsEvents {
    const val PARAM_ACTION = "action"
    const val PARAM_SCREEN = "screen"
    const val PARAM_SURFACE = "surface"
    const val PARAM_AUTH_STATE = "auth_state"

    const val SURFACE_MOBILE = "mobile"
    const val SURFACE_TV = "tv"

    const val AUTH_STATE_AUTHORIZED = "authorized"
    const val AUTH_STATE_ANONYMOUS = "anonymous"

    fun screenView(
        screenName: String,
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "screen_view",
        params = mapOf(PARAM_SCREEN to screenName) + params,
    )

    fun uiAction(
        screenName: String,
        action: String,
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "ui_action",
        params = mapOf(
            PARAM_SCREEN to screenName,
            PARAM_ACTION to action,
        ) + params,
    )

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

    fun playerError(
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "player_error",
        params = mapOf(PARAM_SCREEN to "player") + params,
    )

    fun errorShown(
        params: Map<String, String> = emptyMap(),
    ): AnalyticsEvent = AnalyticsEvent(
        name = "error_shown",
        params = mapOf(PARAM_SCREEN to "error") + params,
    )

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

    fun appSession(
        surface: String,
        isAuthorized: Boolean,
    ): AnalyticsEvent = AnalyticsEvent(
        name = "app_session",
        params = mapOf(
            PARAM_SURFACE to surface,
            PARAM_AUTH_STATE to authState(isAuthorized),
        ),
    )

    fun authState(isAuthorized: Boolean): String =
        if (isAuthorized) AUTH_STATE_AUTHORIZED else AUTH_STATE_ANONYMOUS
}
