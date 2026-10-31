package su.afk.yummy.tv.core.preferences.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object SettingsPreferenceKeys {
    val posterQualityKey = stringPreferencesKey("poster_quality")
    val posterCardSizeKey = stringPreferencesKey("poster_card_size")
    val titleCardLayoutKey = stringPreferencesKey("title_card_layout")
    val showCatalogTitleYearKey = booleanPreferencesKey("show_top_title_year")
    val showEpisodeFlagsKey = booleanPreferencesKey("show_episode_flags")
    val newEpisodesSectionEnabledKey = booleanPreferencesKey("new_episodes_section_enabled")
    val seasonalDecorationsEnabledKey = booleanPreferencesKey("seasonal_decorations_enabled")
    val seasonalAnnouncedKey = stringPreferencesKey("seasonal_announced_key")
    val newEpisodesSourcesKey = stringSetPreferencesKey("new_episodes_sources")
    val showLibraryTitleYearKey = booleanPreferencesKey("show_library_title_year")
    val libraryContinueWatchingCardSizeKey =
        stringPreferencesKey("library_continue_watching_card_size")
    val librarySortKey = stringPreferencesKey("library_sort")
    val librarySortDirectionKey = stringPreferencesKey("library_sort_direction")
    val preferredVideoQualityKey = stringPreferencesKey("preferred_video_quality")
    val adaptiveVideoQualityEnabledKey =
        booleanPreferencesKey("adaptive_video_quality_enabled")
    val watchNextEnabledKey = booleanPreferencesKey("watch_next_enabled")
    val betaUpdatesEnabledKey = booleanPreferencesKey("beta_updates_enabled")
    val appLogRecordingEnabledKey = booleanPreferencesKey("app_log_recording_enabled")
    val previewCacheSizeKey = intPreferencesKey("preview_cache_size")
    val autoSkipOpeningsEndingsKey = booleanPreferencesKey("auto_skip_openings_endings")
    val autoSkipDelaySecondsKey = intPreferencesKey("auto_skip_delay_seconds")
    val showOpeningOnTimelineKey = booleanPreferencesKey("show_opening_on_timeline_enabled")
    val autoPlayNextEpisodeKey = booleanPreferencesKey("auto_play_next_episode")
    val nextEpisodeSwitchDelaySecondsKey =
        intPreferencesKey("next_episode_switch_delay_seconds")
    val playerControlsAutoHideSecondsKey =
        intPreferencesKey("player_controls_auto_hide_seconds")

    val pictureInPictureEnabledKey = booleanPreferencesKey("picture_in_picture_enabled")
    val playerOrientationModeKey = stringPreferencesKey("player_orientation_mode")
    val suggestNextEpisodeOnWatchedKey =
        booleanPreferencesKey("suggest_next_episode_on_watched")
    val autoManageLibraryListsKey = booleanPreferencesKey("auto_manage_library_lists")
    val watchedShortRemainingMinutesKey = intPreferencesKey("watched_remaining_short_minutes")
    val watchedMediumRemainingMinutesKey = intPreferencesKey("watched_remaining_medium_minutes")
    val watchedLongRemainingMinutesKey = intPreferencesKey("watched_remaining_long_minutes")
    val mobilePlayerGestureTutorialDismissedKey =
        booleanPreferencesKey("player_mobile_gesture_tutorial_dismissed")
    val tvPlayerControlsTutorialDismissedKey =
        booleanPreferencesKey("player_tv_controls_tutorial_dismissed")
    val tvPlayerVolumeKeysEnabledKey =
        booleanPreferencesKey("tv_player_volume_keys_enabled")
    val advancedPlayerVolumeEnabledKey =
        booleanPreferencesKey("advanced_player_volume_enabled")
    val advancedPlayerVolumePercentKey =
        intPreferencesKey("advanced_player_volume_percent")
    val mobilePlayerBrightnessPercentKey =
        intPreferencesKey("mobile_player_brightness_percent")
    val playerPlaybackSpeedKey =
        floatPreferencesKey("player_playback_speed")
    val volumeStabilizationEnabledKey =
        booleanPreferencesKey("volume_stabilization_enabled")
    val playerBufferProfileKey = stringPreferencesKey("player_buffer_profile")
    val browserUserAgentProfileKey = stringPreferencesKey("browser_user_agent_profile")
    val playerResizeModeKey = stringPreferencesKey("player_resize_mode")
    val playerZoomLevelKey = stringPreferencesKey("player_zoom_level")
    val subtitleTextSizeKey = intPreferencesKey("subtitle_text_size_percent")
    val subtitleTextColorKey = stringPreferencesKey("subtitle_text_color")
    val subtitleBackgroundKey = stringPreferencesKey("subtitle_background")
    val subtitleOffsetKey = intPreferencesKey("subtitle_offset_percent")
    val subtitleEdgeKey = stringPreferencesKey("subtitle_edge")
    val libraryTabOrderKey = stringPreferencesKey("library_tab_order")
    val hiddenRecommendationIdsKey = stringSetPreferencesKey("hidden_recommendation_ids")
    val appThemeKey = stringPreferencesKey("app_theme")
    val backgroundStyleKey = stringPreferencesKey("background_style")
    val navTransitionStyleKey = stringPreferencesKey("nav_transition_style")
    val yaniApplicationTokenKey = stringPreferencesKey("yani_application_token")
    val yaniAccessTokenKey = stringPreferencesKey("yani_access_token")
    val yaniUserIdKey = intPreferencesKey("yani_user_id")
    val yaniNicknameKey = stringPreferencesKey("yani_nickname")
    val yaniAvatarUrlKey = stringPreferencesKey("yani_avatar_url")
    val yaniTokenRefreshAtKey = stringPreferencesKey("yani_token_refresh_at")
    val yaniUnreadNotificationsCountKey =
        intPreferencesKey("yani_unread_notifications_count")
    val lastStartedVersionCodeKey = intPreferencesKey("last_started_version_code")
    val yaniContentLanguageKey = stringPreferencesKey("yani_content_language")
    val supportPromptDismissedKey = booleanPreferencesKey("support_prompt_dismissed")
    val supportPromptFirstInstallTimeMsKey =
        longPreferencesKey("support_prompt_first_install_time_ms")
    val communityPromptDismissedKey = booleanPreferencesKey("community_prompt_dismissed")
    val communityPromptFirstUseTimeMsKey =
        longPreferencesKey("community_prompt_first_use_time_ms")
    val boostyPromptDismissedKey = booleanPreferencesKey("boosty_prompt_dismissed")
    val lastSeenAnnouncementIdKey =stringPreferencesKey("last_seen_announcement_id")
    val notificationPermissionRequestedKey =
        booleanPreferencesKey("notification_permission_requested")
    val episodePushPromptShownKey = booleanPreferencesKey("episode_push_prompt_shown")
    val episodePushEnabledKey = booleanPreferencesKey("episode_push_enabled")
    val previewChannelPromptAnsweredKey =
        booleanPreferencesKey("preview_channel_prompt_answered")
    val detailsSectionsHintDismissedKey =
        booleanPreferencesKey("details_sections_hint_dismissed")
    val videoExportDirectoryUriKey = stringPreferencesKey("video_export_directory_uri")
    val videoExportDirectoryNameKey = stringPreferencesKey("video_export_directory_name")
    val videoExportAutoEnabledKey = booleanPreferencesKey("video_export_auto_enabled")
    val videoDownloadWifiOnlyKey = booleanPreferencesKey("video_download_wifi_only")
    val saveLastSearchEnabledKey = booleanPreferencesKey("save_last_search_enabled")
    val lastSearchQueryKey = stringPreferencesKey("last_search_query")
    val starredDubbingsKey = stringSetPreferencesKey("starred_dubbings")
    val starredBalancersKey = stringSetPreferencesKey("starred_balancers")
    val lastSearchGenresKey = stringSetPreferencesKey("last_search_genres")
    val lastSearchExcludedGenresKey = stringSetPreferencesKey("last_search_excluded_genres")
    val lastSearchTypesKey = stringSetPreferencesKey("last_search_types")
    val lastSearchStatusesKey = stringSetPreferencesKey("last_search_statuses")
    val lastSearchSeasonsKey = stringSetPreferencesKey("last_search_seasons")
    val lastSearchAgeRatingsKey = stringSetPreferencesKey("last_search_age_ratings")
    val lastSearchFromYearKey = intPreferencesKey("last_search_from_year")
    val lastSearchToYearKey = intPreferencesKey("last_search_to_year")
    val lastSearchSortKey = stringPreferencesKey("last_search_sort")
    val lastSearchSortForwardKey = booleanPreferencesKey("last_search_sort_forward")
    val episodePushLibraryEnabledKey = booleanPreferencesKey("episode_push_library_enabled")
    val episodePushKnownLibraryEpisodesKey =
        stringSetPreferencesKey("episode_push_known_library_episodes")
    val episodePushRecentPushesKey = stringSetPreferencesKey("episode_push_recent_pushes")
    val episodePushKnownNotificationIdsKey =
        stringSetPreferencesKey("episode_push_known_notification_ids")
}
