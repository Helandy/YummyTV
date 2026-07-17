package su.afk.yummy.tv.core.preferences.settings

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class DataStoreSettingsStore(private val context: Context) : SettingsStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val posterQualityKey = stringPreferencesKey("poster_quality")
    private val posterCardSizeKey = stringPreferencesKey("poster_card_size")
    private val showTopTitleYearKey = booleanPreferencesKey("show_top_title_year")
    private val libraryContinueWatchingCardSizeKey =
        stringPreferencesKey("library_continue_watching_card_size")
    private val preferredPlayerKey = stringPreferencesKey("preferred_player")
    private val preferredVideoQualityKey = stringPreferencesKey("preferred_video_quality")
    private val watchNextEnabledKey = booleanPreferencesKey("watch_next_enabled")
    private val previewCacheSizeKey = intPreferencesKey("preview_cache_size")
    private val autoSkipOpeningsEndingsKey = booleanPreferencesKey("auto_skip_openings_endings")
    private val autoPlayNextEpisodeKey = booleanPreferencesKey("auto_play_next_episode")
    private val pictureInPictureEnabledKey = booleanPreferencesKey("picture_in_picture_enabled")
    private val suggestNextEpisodeOnWatchedKey =
        booleanPreferencesKey("suggest_next_episode_on_watched")
    private val refreshContinueWatchingProgressOnLaunchKey =
        booleanPreferencesKey("refresh_continue_watching_progress_on_launch")
    private val mobilePlayerGestureTutorialDismissedKey =
        booleanPreferencesKey("player_mobile_gesture_tutorial_dismissed")
    private val playerResizeModeKey = stringPreferencesKey("player_resize_mode")
    private val playerZoomLevelKey = stringPreferencesKey("player_zoom_level")
    private val detailsButtonOrderKey = stringPreferencesKey("details_button_order")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val yaniApplicationTokenKey = stringPreferencesKey("yani_application_token")
    private val yaniAccessTokenKey = stringPreferencesKey("yani_access_token")
    private val yaniUserIdKey = intPreferencesKey("yani_user_id")
    private val yaniNicknameKey = stringPreferencesKey("yani_nickname")
    private val yaniAvatarUrlKey = stringPreferencesKey("yani_avatar_url")
    private val yaniTokenRefreshAtKey = stringPreferencesKey("yani_token_refresh_at")
    private val yaniUnreadNotificationsCountKey =
        intPreferencesKey("yani_unread_notifications_count")
    private val lastStartedVersionCodeKey = intPreferencesKey("last_started_version_code")
    private val yaniContentLanguageKey = stringPreferencesKey("yani_content_language")
    private val supportPromptDismissedKey = booleanPreferencesKey("support_prompt_dismissed")
    private val supportPromptFirstInstallTimeMsKey =
        longPreferencesKey("support_prompt_first_install_time_ms")
    private val legacyStreamingCachePrunedKey =
        booleanPreferencesKey("legacy_streaming_cache_pruned")

    @Volatile
    private var previewCacheSizeSnapshot = PreviewCacheSize.MB_100

    override val currentPreviewCacheSize: PreviewCacheSize
        get() = previewCacheSizeSnapshot

    override val posterQuality: Flow<PosterQuality> = context.dataStore.data.map { prefs ->
        prefs[posterQualityKey]?.let { name ->
            runCatching { PosterQuality.valueOf(name) }.getOrNull()
        }
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PosterQuality.MEGA else PosterQuality.STANDARD
    }

    override val posterCardSize: Flow<PosterCardSize> = context.dataStore.data.map { prefs ->
        prefs[posterCardSizeKey]?.let { name ->
            runCatching { PosterCardSize.valueOf(name) }.getOrNull()
        } ?: PosterCardSize.STANDARD
    }

    override val showTopTitleYear: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[showTopTitleYearKey] ?: false
    }

    override val libraryContinueWatchingCardSize: Flow<LibraryContinueWatchingCardSize> =
        context.dataStore.data.map { prefs ->
            prefs[libraryContinueWatchingCardSizeKey]?.let { name ->
                runCatching { LibraryContinueWatchingCardSize.valueOf(name) }.getOrNull()
            } ?: LibraryContinueWatchingCardSize.LARGE
        }

    override val preferredPlayer: Flow<PreferredPlayer> = context.dataStore.data.map { prefs ->
        prefs[preferredPlayerKey]?.let { name ->
            runCatching { PreferredPlayer.valueOf(name) }.getOrNull()
        } ?: PreferredPlayer.NONE
    }

    override val preferredVideoQuality: Flow<PreferredVideoQuality> =
        context.dataStore.data.map { prefs ->
            prefs[preferredVideoQualityKey]?.let { name ->
                runCatching { PreferredVideoQuality.valueOf(name) }.getOrNull()
            } ?: PreferredVideoQuality.BEST
        }

    override val watchNextEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[watchNextEnabledKey] ?: true
    }

    override val previewCacheSize: Flow<PreviewCacheSize> = context.dataStore.data.map { prefs ->
        val mb = prefs[previewCacheSizeKey] ?: PreviewCacheSize.MB_100.megabytes
        PreviewCacheSize.entries.firstOrNull { it.megabytes == mb } ?: PreviewCacheSize.MB_100
    }

    override val autoSkipOpeningsEndings: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoSkipOpeningsEndingsKey] ?: false
    }

    override val autoPlayNextEpisode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoPlayNextEpisodeKey] ?: false
    }

    override val pictureInPictureEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pictureInPictureEnabledKey] ?: true
    }

    override val suggestNextEpisodeOnWatched: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[suggestNextEpisodeOnWatchedKey] ?: true
    }

    override val refreshContinueWatchingProgressOnLaunch: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[refreshContinueWatchingProgressOnLaunchKey] ?: false
        }

    override val mobilePlayerGestureTutorialDismissed: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[mobilePlayerGestureTutorialDismissedKey] ?: false
        }

    override val playerResizeMode: Flow<PlayerResizeMode> = context.dataStore.data.map { prefs ->
        prefs[playerResizeModeKey]?.let { name ->
            runCatching { PlayerResizeMode.valueOf(name) }.getOrNull()
        } ?: PlayerResizeMode.FIT
    }

    override val playerZoomLevel: Flow<PlayerZoomLevel> = context.dataStore.data.map { prefs ->
        prefs[playerZoomLevelKey]?.let { name ->
            runCatching { PlayerZoomLevel.valueOf(name) }.getOrNull()
        } ?: PlayerZoomLevel.PERCENT_10
    }

    override fun playerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerResizeSettings> {
        val key = playerScopedResizeSettingsKey(animeId, animeTitle, playerName)
        return context.dataStore.data.map { prefs ->
            prefs[key]?.toPlayerResizeSettings() ?: PlayerResizeSettings()
        }
    }

    override fun playerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerMobileVideoTransformSettings> {
        val key = playerScopedMobileVideoTransformSettingsKey(animeId, animeTitle, playerName)
        return context.dataStore.data.map { prefs ->
            prefs[key]?.toPlayerMobileVideoTransformSettings()
                ?: PlayerMobileVideoTransformSettings()
        }
    }

    override val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        prefs[appThemeKey]?.let { name ->
            runCatching { AppTheme.valueOf(name) }.getOrNull()
        } ?: AppTheme.WARM_AMBER
    }

    override val detailsButtonOrder: Flow<List<DetailsButtonAction>> =
        context.dataStore.data.map { prefs ->
            prefs[detailsButtonOrderKey].toDetailsButtonOrder()
        }

    override val yaniApplicationToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs.yaniApplicationToken()
    }

    override val yaniApplicationTokenState: Flow<YaniApplicationTokenState> =
        context.dataStore.data.map { prefs ->
            prefs.yaniApplicationTokenState()
        }

    override val yaniUserId: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[yaniUserIdKey] ?: 0
    }

    override val yaniNickname: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[yaniNicknameKey].orEmpty()
    }

    override val yaniAvatarUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[yaniAvatarUrlKey].orEmpty()
    }

    override val yaniTokenRefreshAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[yaniTokenRefreshAtKey]?.toLongOrNull() ?: 0L
    }

    override val yaniUnreadNotificationsCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[yaniUnreadNotificationsCountKey] ?: 0
    }

    override val yaniContentLanguage: Flow<YaniContentLanguage> =
        context.dataStore.data.map { prefs ->
            YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
                ?: YaniContentLanguage.fromSystemLocale(context)
        }

    override val settingsSnapshot: Flow<SettingsSnapshot> = context.dataStore.data.map { prefs ->
        SettingsSnapshot(
            appTheme = prefs[appThemeKey]?.let { name ->
                runCatching { AppTheme.valueOf(name) }.getOrNull()
            } ?: AppTheme.WARM_AMBER,
            posterQuality = prefs[posterQualityKey]?.let { name ->
                runCatching { PosterQuality.valueOf(name) }.getOrNull()
            }
                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PosterQuality.MEGA else PosterQuality.STANDARD,
            posterCardSize = prefs[posterCardSizeKey]?.let { name ->
                runCatching { PosterCardSize.valueOf(name) }.getOrNull()
            } ?: PosterCardSize.STANDARD,
            showTopTitleYear = prefs[showTopTitleYearKey] ?: false,
            libraryContinueWatchingCardSize =
                prefs[libraryContinueWatchingCardSizeKey]?.let { name ->
                    runCatching { LibraryContinueWatchingCardSize.valueOf(name) }.getOrNull()
                } ?: LibraryContinueWatchingCardSize.LARGE,
            preferredPlayer = prefs[preferredPlayerKey]?.let { name ->
                runCatching { PreferredPlayer.valueOf(name) }.getOrNull()
            } ?: PreferredPlayer.NONE,
            preferredVideoQuality = prefs[preferredVideoQualityKey]?.let { name ->
                runCatching { PreferredVideoQuality.valueOf(name) }.getOrNull()
            } ?: PreferredVideoQuality.BEST,
            watchNextEnabled = prefs[watchNextEnabledKey] ?: true,
            previewCacheSize = (prefs[previewCacheSizeKey]
                ?: PreviewCacheSize.MB_100.megabytes).let { mb ->
                PreviewCacheSize.entries.firstOrNull { it.megabytes == mb }
                    ?: PreviewCacheSize.MB_100
            },
            autoSkipOpeningsEndings = prefs[autoSkipOpeningsEndingsKey] ?: false,
            autoPlayNextEpisode = prefs[autoPlayNextEpisodeKey] ?: false,
            pictureInPictureEnabled = prefs[pictureInPictureEnabledKey] ?: true,
            suggestNextEpisodeOnWatched = prefs[suggestNextEpisodeOnWatchedKey] ?: true,
            refreshContinueWatchingProgressOnLaunch =
                prefs[refreshContinueWatchingProgressOnLaunchKey] ?: false,
            yaniApplicationToken = prefs.yaniApplicationToken(),
            contentLanguage = YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
                ?: YaniContentLanguage.fromSystemLocale(context),
            detailsButtonOrder = prefs[detailsButtonOrderKey].toDetailsButtonOrder(),
        )
    }

    override val mainSettingsSnapshot: Flow<MainSettingsSnapshot> =
        context.dataStore.data.map { prefs ->
            MainSettingsSnapshot(
                appTheme = prefs[appThemeKey]?.let { name ->
                    runCatching { AppTheme.valueOf(name) }.getOrNull()
                } ?: AppTheme.WARM_AMBER,
                posterQuality = prefs[posterQualityKey]?.let { name ->
                    runCatching { PosterQuality.valueOf(name) }.getOrNull()
                }
                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PosterQuality.MEGA else PosterQuality.STANDARD,
                posterCardSize = prefs[posterCardSizeKey]?.let { name ->
                    runCatching { PosterCardSize.valueOf(name) }.getOrNull()
                } ?: PosterCardSize.STANDARD,
                yaniNickname = prefs[yaniNicknameKey].orEmpty(),
                yaniAvatarUrl = prefs[yaniAvatarUrlKey].orEmpty(),
                yaniUnreadNotificationsCount = prefs[yaniUnreadNotificationsCountKey] ?: 0,
            )
        }

    override val supportPromptSnapshot: Flow<SupportPromptSnapshot> =
        context.dataStore.data.map { prefs ->
            SupportPromptSnapshot(
                dismissed = prefs[supportPromptDismissedKey] ?: false,
                firstEligibleTimeMs = prefs[supportPromptFirstInstallTimeMsKey]
                    ?: System.currentTimeMillis(),
            )
        }

    init {
        scope.launch {
            previewCacheSize.collect { size ->
                previewCacheSizeSnapshot = size
            }
        }
    }

    override suspend fun setPosterQuality(quality: PosterQuality) {
        context.dataStore.edit { prefs -> prefs[posterQualityKey] = quality.name }
    }

    override suspend fun setPosterCardSize(size: PosterCardSize) {
        context.dataStore.edit { prefs -> prefs[posterCardSizeKey] = size.name }
    }

    override suspend fun setShowTopTitleYear(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[showTopTitleYearKey] = enabled }
    }

    override suspend fun setLibraryContinueWatchingCardSize(size: LibraryContinueWatchingCardSize) {
        context.dataStore.edit { prefs ->
            prefs[libraryContinueWatchingCardSizeKey] = size.name
        }
    }

    override suspend fun setPreferredPlayer(player: PreferredPlayer) {
        context.dataStore.edit { prefs -> prefs[preferredPlayerKey] = player.name }
    }

    override suspend fun setPreferredVideoQuality(quality: PreferredVideoQuality) {
        context.dataStore.edit { prefs -> prefs[preferredVideoQualityKey] = quality.name }
    }

    override suspend fun setWatchNextEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[watchNextEnabledKey] = enabled }
    }

    override suspend fun setPreviewCacheSize(size: PreviewCacheSize) {
        previewCacheSizeSnapshot = size
        context.dataStore.edit { prefs -> prefs[previewCacheSizeKey] = size.megabytes }
    }

    override suspend fun setAutoSkipOpeningsEndings(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[autoSkipOpeningsEndingsKey] = enabled }
    }

    override suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[autoPlayNextEpisodeKey] = enabled }
    }

    override suspend fun setPictureInPictureEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[pictureInPictureEnabledKey] = enabled }
    }

    override suspend fun setSuggestNextEpisodeOnWatched(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[suggestNextEpisodeOnWatchedKey] = enabled }
    }

    override suspend fun setRefreshContinueWatchingProgressOnLaunch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[refreshContinueWatchingProgressOnLaunchKey] = enabled
        }
    }

    override suspend fun dismissMobilePlayerGestureTutorial() {
        context.dataStore.edit { prefs ->
            prefs[mobilePlayerGestureTutorialDismissedKey] = true
        }
    }

    override suspend fun resetMobilePlayerGestureTutorial() {
        context.dataStore.edit { prefs ->
            prefs[mobilePlayerGestureTutorialDismissedKey] = false
        }
    }

    override suspend fun setPlayerResizeMode(mode: PlayerResizeMode) {
        context.dataStore.edit { prefs -> prefs[playerResizeModeKey] = mode.name }
    }

    override suspend fun setPlayerZoomLevel(level: PlayerZoomLevel) {
        context.dataStore.edit { prefs -> prefs[playerZoomLevelKey] = level.name }
    }

    override suspend fun setPlayerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerResizeSettings,
    ) {
        val key = playerScopedResizeSettingsKey(animeId, animeTitle, playerName)
        context.dataStore.edit { prefs ->
            prefs[key] = settings.toPreferenceValue()
        }
    }

    override suspend fun setPlayerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerMobileVideoTransformSettings,
    ) {
        val key = playerScopedMobileVideoTransformSettingsKey(animeId, animeTitle, playerName)
        context.dataStore.edit { prefs ->
            prefs[key] = settings.toPreferenceValue()
        }
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[appThemeKey] = theme.name }
    }

    override suspend fun setDetailsButtonOrder(order: List<DetailsButtonAction>) {
        context.dataStore.edit { prefs ->
            prefs[detailsButtonOrderKey] =
                order.normalizedDetailsButtonOrder().joinToString(DETAILS_BUTTON_ORDER_SEPARATOR) {
                    it.name
                }
        }
    }

    override suspend fun setYaniApplicationToken(token: String) {
        context.dataStore.edit { prefs ->
            val trimmedToken = token.trim()
            if (trimmedToken.isBlank()) {
                prefs.remove(yaniApplicationTokenKey)
            } else {
                prefs[yaniApplicationTokenKey] = trimmedToken
            }
        }
    }

    override suspend fun setYaniAccount(
        userId: Int,
        nickname: String,
        avatarUrl: String?,
        refreshedAt: Long,
    ) {
        context.dataStore.edit { prefs ->
            prefs[yaniUserIdKey] = userId
            prefs[yaniNicknameKey] = nickname
            prefs[yaniTokenRefreshAtKey] = refreshedAt.toString()
            if (avatarUrl.isNullOrBlank()) {
                prefs.remove(yaniAvatarUrlKey)
            } else {
                prefs[yaniAvatarUrlKey] = avatarUrl
            }
        }
    }

    override suspend fun clearLegacyYaniAccessToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(yaniAccessTokenKey)
        }
    }

    override suspend fun clearYaniAccount() {
        context.dataStore.edit { prefs ->
            prefs.remove(yaniAccessTokenKey)
            prefs.remove(yaniUserIdKey)
            prefs.remove(yaniNicknameKey)
            prefs.remove(yaniAvatarUrlKey)
            prefs.remove(yaniTokenRefreshAtKey)
            prefs.remove(yaniUnreadNotificationsCountKey)
        }
    }

    override suspend fun setYaniUnreadNotificationsCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[yaniUnreadNotificationsCountKey] = count.coerceAtLeast(0)
        }
    }

    override suspend fun setYaniContentLanguage(language: YaniContentLanguage) {
        context.dataStore.edit { prefs -> prefs[yaniContentLanguageKey] = language.name }
    }

    override suspend fun ensureYaniContentLanguageInitialized() {
        context.dataStore.edit { prefs ->
            if (YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey]) == null) {
                prefs[yaniContentLanguageKey] = YaniContentLanguage.fromSystemLocale(context).name
            }
        }
    }

    override suspend fun ensureSupportPromptInstallTimeInitialized() {
        context.dataStore.edit { prefs ->
            if (prefs[supportPromptFirstInstallTimeMsKey] == null) {
                prefs[supportPromptFirstInstallTimeMsKey] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun dismissSupportPrompt() {
        context.dataStore.edit { prefs ->
            prefs[supportPromptDismissedKey] = true
        }
    }

    override suspend fun markStartedVersion(versionCode: Int): Boolean {
        var isFreshVersion = true
        context.dataStore.edit { prefs ->
            val lastStartedVersionCode = prefs[lastStartedVersionCodeKey]
            isFreshVersion = lastStartedVersionCode != versionCode
            if (isFreshVersion && prefs[supportPromptDismissedKey] != true) {
                prefs[supportPromptFirstInstallTimeMsKey] = System.currentTimeMillis()
            }
            prefs[lastStartedVersionCodeKey] = versionCode
        }
        return isFreshVersion
    }

    /**
     * Returns `true` exactly once: the first time this is called after the streaming/download
     * cache split, so callers can prune the now-unbounded-legacy entries a single time. Every
     * later call returns `false`.
     */
    override suspend fun consumeLegacyStreamingCachePruneFlag(): Boolean {
        var shouldPrune = false
        context.dataStore.edit { prefs ->
            shouldPrune = prefs[legacyStreamingCachePrunedKey] != true
            prefs[legacyStreamingCachePrunedKey] = true
        }
        return shouldPrune
    }

    private fun String?.toDetailsButtonOrder(): List<DetailsButtonAction> {
        if (isNullOrBlank()) return SettingsStore.defaultDetailsButtonOrder
        return split(DETAILS_BUTTON_ORDER_SEPARATOR)
            .mapNotNull { name -> runCatching { DetailsButtonAction.valueOf(name) }.getOrNull() }
            .normalizedDetailsButtonOrder()
    }

    private fun Preferences.yaniApplicationToken(): String =
        this[yaniApplicationTokenKey]?.takeIf { it.isNotBlank() } ?: DEFAULT_YANI_APPLICATION_TOKEN

    private fun Preferences.yaniApplicationTokenState(): YaniApplicationTokenState {
        val token = this[yaniApplicationTokenKey]?.trim().orEmpty()
        return if (token.isNotBlank() && token != DEFAULT_YANI_APPLICATION_TOKEN) {
            YaniApplicationTokenState.CUSTOM
        } else {
            YaniApplicationTokenState.DEFAULT
        }
    }

    private fun List<DetailsButtonAction>.normalizedDetailsButtonOrder(): List<DetailsButtonAction> {
        val unique = distinct()
        val complete = unique + SettingsStore.defaultDetailsButtonOrder.filterNot { it in unique }
        val withoutFavorite = complete.filterNot { it == DetailsButtonAction.FAVORITE }
        val libraryIndex = withoutFavorite.indexOf(DetailsButtonAction.LIBRARY)
        if (libraryIndex == -1) return complete
        return withoutFavorite.toMutableList().apply {
            add(libraryIndex + 1, DetailsButtonAction.FAVORITE)
        }
    }

    private fun playerScopedResizeSettingsKey(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Preferences.Key<String> {
        val titleKey = if (animeId > 0) {
            "anime_id:$animeId"
        } else {
            "title:${animeTitle.normalizedPlayerResizeKeyPart()}"
        }
        val playerKey = playerName.normalizedPlayerResizeKeyPart()
        return stringPreferencesKey("player_resize_settings|$titleKey|player:$playerKey")
    }

    private fun playerScopedMobileVideoTransformSettingsKey(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Preferences.Key<String> {
        val titleKey = if (animeId > 0) {
            "anime_id:$animeId"
        } else {
            "title:${animeTitle.normalizedPlayerResizeKeyPart()}"
        }
        val playerKey = playerName.normalizedPlayerResizeKeyPart()
        return stringPreferencesKey("player_mobile_video_transform|$titleKey|player:$playerKey")
    }

    private fun String.normalizedPlayerResizeKeyPart(): String =
        trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .ifBlank { "unknown" }

    private fun PlayerResizeSettings.toPreferenceValue(): String =
        "${resizeMode.name}|${zoomLevel.name}"

    private fun String.toPlayerResizeSettings(): PlayerResizeSettings? {
        val parts = split('|')
        val mode = parts.getOrNull(0)?.let { name ->
            runCatching { PlayerResizeMode.valueOf(name) }.getOrNull()
        } ?: return null
        val level = parts.getOrNull(1)?.let { name ->
            runCatching { PlayerZoomLevel.valueOf(name) }.getOrNull()
        } ?: PlayerZoomLevel.PERCENT_10
        return PlayerResizeSettings(
            resizeMode = mode,
            zoomLevel = level,
        )
    }

    private fun PlayerMobileVideoTransformSettings.toPreferenceValue(): String =
        "$scale|$offsetX|$offsetY"

    private fun String.toPlayerMobileVideoTransformSettings(): PlayerMobileVideoTransformSettings? {
        val parts = split('|')
        val scale = parts.getOrNull(0)?.toFloatOrNull() ?: return null
        val offsetX = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
        val offsetY = parts.getOrNull(2)?.toFloatOrNull() ?: 0f
        return PlayerMobileVideoTransformSettings(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
        )
    }

    companion object {
        private const val DEFAULT_YANI_APPLICATION_TOKEN = "ze645twqfeql6l1u"
        private const val DETAILS_BUTTON_ORDER_SEPARATOR = "|"
    }
}
