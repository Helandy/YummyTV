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

data class SettingsSnapshot(
    val appTheme: AppTheme,
    val posterQuality: PosterQuality,
    val posterCardSize: PosterCardSize,
    val libraryContinueWatchingCardSize: LibraryContinueWatchingCardSize,
    val preferredPlayer: PreferredPlayer,
    val watchNextEnabled: Boolean,
    val previewCacheSize: PreviewCacheSize,
    val autoSkipOpeningsEndings: Boolean,
    val yaniApplicationToken: String,
    val contentLanguage: YaniContentLanguage,
    val detailsButtonOrder: List<DetailsButtonAction>,
)

data class MainSettingsSnapshot(
    val appTheme: AppTheme,
    val posterQuality: PosterQuality,
    val posterCardSize: PosterCardSize,
    val yaniNickname: String,
    val yaniAvatarUrl: String,
    val yaniUnreadNotificationsCount: Int,
)

data class SupportPromptSnapshot(
    val dismissed: Boolean,
    val firstInstallTimeMs: Long,
)

class SettingsStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val posterQualityKey = stringPreferencesKey("poster_quality")
    private val posterCardSizeKey = stringPreferencesKey("poster_card_size")
    private val libraryContinueWatchingCardSizeKey =
        stringPreferencesKey("library_continue_watching_card_size")
    private val preferredPlayerKey = stringPreferencesKey("preferred_player")
    private val watchNextEnabledKey = booleanPreferencesKey("watch_next_enabled")
    private val previewCacheSizeKey = intPreferencesKey("preview_cache_size")
    private val autoSkipOpeningsEndingsKey = booleanPreferencesKey("auto_skip_openings_endings")
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
    private val yaniUnreadNotificationsCountKey = intPreferencesKey("yani_unread_notifications_count")
    private val lastStartedVersionCodeKey = intPreferencesKey("last_started_version_code")
    private val yaniContentLanguageKey = stringPreferencesKey("yani_content_language")
    private val supportPromptDismissedKey = booleanPreferencesKey("support_prompt_dismissed")
    private val supportPromptFirstInstallTimeMsKey =
        longPreferencesKey("support_prompt_first_install_time_ms")

    @Volatile private var previewCacheSizeSnapshot = PreviewCacheSize.MB_100

    val currentPreviewCacheSize: PreviewCacheSize
        get() = previewCacheSizeSnapshot

    val posterQuality: Flow<PosterQuality> = context.dataStore.data.map { prefs ->
        prefs[posterQualityKey]?.let { name ->
            runCatching { PosterQuality.valueOf(name) }.getOrNull()
        } ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PosterQuality.MEGA else PosterQuality.STANDARD
    }

    val posterCardSize: Flow<PosterCardSize> = context.dataStore.data.map { prefs ->
        prefs[posterCardSizeKey]?.let { name ->
            runCatching { PosterCardSize.valueOf(name) }.getOrNull()
        } ?: PosterCardSize.STANDARD
    }

    val libraryContinueWatchingCardSize: Flow<LibraryContinueWatchingCardSize> =
        context.dataStore.data.map { prefs ->
            prefs[libraryContinueWatchingCardSizeKey]?.let { name ->
                runCatching { LibraryContinueWatchingCardSize.valueOf(name) }.getOrNull()
            } ?: LibraryContinueWatchingCardSize.LARGE
        }

    val preferredPlayer: Flow<PreferredPlayer> = context.dataStore.data.map { prefs ->
        prefs[preferredPlayerKey]?.let { name ->
            runCatching { PreferredPlayer.valueOf(name) }.getOrNull()
        } ?: PreferredPlayer.NONE
    }

    val watchNextEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[watchNextEnabledKey] ?: true
    }

    val previewCacheSize: Flow<PreviewCacheSize> = context.dataStore.data.map { prefs ->
        val mb = prefs[previewCacheSizeKey] ?: PreviewCacheSize.MB_100.megabytes
        PreviewCacheSize.entries.firstOrNull { it.megabytes == mb } ?: PreviewCacheSize.MB_100
    }

    val autoSkipOpeningsEndings: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoSkipOpeningsEndingsKey] ?: false
    }

    val playerResizeMode: Flow<PlayerResizeMode> = context.dataStore.data.map { prefs ->
        prefs[playerResizeModeKey]?.let { name ->
            runCatching { PlayerResizeMode.valueOf(name) }.getOrNull()
        } ?: PlayerResizeMode.FIT
    }

    val playerZoomLevel: Flow<PlayerZoomLevel> = context.dataStore.data.map { prefs ->
        prefs[playerZoomLevelKey]?.let { name ->
            runCatching { PlayerZoomLevel.valueOf(name) }.getOrNull()
        } ?: PlayerZoomLevel.PERCENT_10
    }

    fun playerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerResizeSettings> {
        val key = playerScopedResizeSettingsKey(animeId, animeTitle, playerName)
        return context.dataStore.data.map { prefs ->
            prefs[key]?.toPlayerResizeSettings() ?: PlayerResizeSettings()
        }
    }

    fun playerMobileVideoTransformSettings(
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

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        prefs[appThemeKey]?.let { name ->
            runCatching { AppTheme.valueOf(name) }.getOrNull()
        } ?: AppTheme.WARM_AMBER
    }

    val detailsButtonOrder: Flow<List<DetailsButtonAction>> = context.dataStore.data.map { prefs ->
        prefs[detailsButtonOrderKey].toDetailsButtonOrder()
    }

    val yaniApplicationToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs.yaniApplicationToken()
    }

    val yaniApplicationTokenState: Flow<YaniApplicationTokenState> =
        context.dataStore.data.map { prefs ->
            prefs.yaniApplicationTokenState()
        }

    val yaniUserId: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[yaniUserIdKey] ?: 0
    }

    val yaniNickname: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[yaniNicknameKey].orEmpty()
    }

    val yaniAvatarUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[yaniAvatarUrlKey].orEmpty()
    }

    val yaniTokenRefreshAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[yaniTokenRefreshAtKey]?.toLongOrNull() ?: 0L
    }

    val yaniUnreadNotificationsCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[yaniUnreadNotificationsCountKey] ?: 0
    }

    val yaniContentLanguage: Flow<YaniContentLanguage> = context.dataStore.data.map { prefs ->
        YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
            ?: YaniContentLanguage.fromSystemLocale(context)
    }

    val settingsSnapshot: Flow<SettingsSnapshot> = context.dataStore.data.map { prefs ->
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
            libraryContinueWatchingCardSize =
                prefs[libraryContinueWatchingCardSizeKey]?.let { name ->
                    runCatching { LibraryContinueWatchingCardSize.valueOf(name) }.getOrNull()
                } ?: LibraryContinueWatchingCardSize.LARGE,
            preferredPlayer = prefs[preferredPlayerKey]?.let { name ->
                runCatching { PreferredPlayer.valueOf(name) }.getOrNull()
            } ?: PreferredPlayer.NONE,
            watchNextEnabled = prefs[watchNextEnabledKey] ?: true,
            previewCacheSize = (prefs[previewCacheSizeKey]
                ?: PreviewCacheSize.MB_100.megabytes).let { mb ->
                PreviewCacheSize.entries.firstOrNull { it.megabytes == mb }
                    ?: PreviewCacheSize.MB_100
            },
            autoSkipOpeningsEndings = prefs[autoSkipOpeningsEndingsKey] ?: false,
            yaniApplicationToken = prefs.yaniApplicationToken(),
            contentLanguage = YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
                ?: YaniContentLanguage.fromSystemLocale(context),
            detailsButtonOrder = prefs[detailsButtonOrderKey].toDetailsButtonOrder(),
        )
    }

    val mainSettingsSnapshot: Flow<MainSettingsSnapshot> = context.dataStore.data.map { prefs ->
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

    val supportPromptSnapshot: Flow<SupportPromptSnapshot> = context.dataStore.data.map { prefs ->
        SupportPromptSnapshot(
            dismissed = prefs[supportPromptDismissedKey] ?: false,
            firstInstallTimeMs = prefs[supportPromptFirstInstallTimeMsKey]
                ?: context.firstInstallTimeMs(),
        )
    }

    init {
        scope.launch {
            previewCacheSize.collect { size ->
                previewCacheSizeSnapshot = size
            }
        }
    }

    suspend fun setPosterQuality(quality: PosterQuality) {
        context.dataStore.edit { prefs -> prefs[posterQualityKey] = quality.name }
    }

    suspend fun setPosterCardSize(size: PosterCardSize) {
        context.dataStore.edit { prefs -> prefs[posterCardSizeKey] = size.name }
    }

    suspend fun setLibraryContinueWatchingCardSize(size: LibraryContinueWatchingCardSize) {
        context.dataStore.edit { prefs ->
            prefs[libraryContinueWatchingCardSizeKey] = size.name
        }
    }

    suspend fun setPreferredPlayer(player: PreferredPlayer) {
        context.dataStore.edit { prefs -> prefs[preferredPlayerKey] = player.name }
    }

    suspend fun setWatchNextEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[watchNextEnabledKey] = enabled }
    }

    suspend fun setPreviewCacheSize(size: PreviewCacheSize) {
        previewCacheSizeSnapshot = size
        context.dataStore.edit { prefs -> prefs[previewCacheSizeKey] = size.megabytes }
    }

    suspend fun setAutoSkipOpeningsEndings(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[autoSkipOpeningsEndingsKey] = enabled }
    }

    suspend fun setPlayerResizeMode(mode: PlayerResizeMode) {
        context.dataStore.edit { prefs -> prefs[playerResizeModeKey] = mode.name }
    }

    suspend fun setPlayerZoomLevel(level: PlayerZoomLevel) {
        context.dataStore.edit { prefs -> prefs[playerZoomLevelKey] = level.name }
    }

    suspend fun setPlayerResizeSettings(
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

    suspend fun setPlayerMobileVideoTransformSettings(
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

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[appThemeKey] = theme.name }
    }

    suspend fun setDetailsButtonOrder(order: List<DetailsButtonAction>) {
        context.dataStore.edit { prefs ->
            prefs[detailsButtonOrderKey] = order.normalizedDetailsButtonOrder().joinToString(DETAILS_BUTTON_ORDER_SEPARATOR) {
                it.name
            }
        }
    }

    suspend fun setYaniApplicationToken(token: String) {
        context.dataStore.edit { prefs ->
            val trimmedToken = token.trim()
            if (trimmedToken.isBlank()) {
                prefs.remove(yaniApplicationTokenKey)
            } else {
                prefs[yaniApplicationTokenKey] = trimmedToken
            }
        }
    }

    suspend fun setYaniAccount(
        userId: Int,
        nickname: String,
        avatarUrl: String?,
        refreshedAt: Long = System.currentTimeMillis(),
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

    suspend fun consumeLegacyYaniAccessToken(): String {
        var token = ""
        context.dataStore.edit { prefs ->
            token = prefs[yaniAccessTokenKey].orEmpty()
            prefs.remove(yaniAccessTokenKey)
        }
        return token
    }

    suspend fun clearYaniAccount() {
        context.dataStore.edit { prefs ->
            prefs.remove(yaniAccessTokenKey)
            prefs.remove(yaniUserIdKey)
            prefs.remove(yaniNicknameKey)
            prefs.remove(yaniAvatarUrlKey)
            prefs.remove(yaniTokenRefreshAtKey)
            prefs.remove(yaniUnreadNotificationsCountKey)
        }
    }

    suspend fun setYaniUnreadNotificationsCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[yaniUnreadNotificationsCountKey] = count.coerceAtLeast(0)
        }
    }

    suspend fun setYaniContentLanguage(language: YaniContentLanguage) {
        context.dataStore.edit { prefs -> prefs[yaniContentLanguageKey] = language.name }
    }

    suspend fun ensureYaniContentLanguageInitialized() {
        context.dataStore.edit { prefs ->
            if (YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey]) == null) {
                prefs[yaniContentLanguageKey] = YaniContentLanguage.fromSystemLocale(context).name
            }
        }
    }

    suspend fun ensureSupportPromptInstallTimeInitialized() {
        context.dataStore.edit { prefs ->
            if (prefs[supportPromptFirstInstallTimeMsKey] == null) {
                prefs[supportPromptFirstInstallTimeMsKey] = context.firstInstallTimeMs()
            }
        }
    }

    suspend fun dismissSupportPrompt() {
        context.dataStore.edit { prefs ->
            prefs[supportPromptDismissedKey] = true
        }
    }

    suspend fun markStartedVersion(versionCode: Int): Boolean {
        var isFreshVersion = true
        context.dataStore.edit { prefs ->
            val lastStartedVersionCode = prefs[lastStartedVersionCodeKey]
            isFreshVersion = lastStartedVersionCode != versionCode
            prefs[lastStartedVersionCodeKey] = versionCode
        }
        return isFreshVersion
    }

    private fun String?.toDetailsButtonOrder(): List<DetailsButtonAction> {
        if (isNullOrBlank()) return defaultDetailsButtonOrder
        return split(DETAILS_BUTTON_ORDER_SEPARATOR)
            .mapNotNull { name -> runCatching { DetailsButtonAction.valueOf(name) }.getOrNull() }
            .normalizedDetailsButtonOrder()
    }

    private fun Preferences.yaniApplicationToken(): String =
        this[yaniApplicationTokenKey]?.takeIf { it.isNotBlank() } ?: DEFAULT_YANI_APPLICATION_TOKEN

    private fun Context.firstInstallTimeMs(): Long =
        runCatching {
            packageManager.getPackageInfo(packageName, 0).firstInstallTime
        }.getOrDefault(System.currentTimeMillis())

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
        val complete = unique + defaultDetailsButtonOrder.filterNot { it in unique }
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

        val defaultDetailsButtonOrder: List<DetailsButtonAction> = listOf(
            DetailsButtonAction.WATCH,
            DetailsButtonAction.LIBRARY,
            DetailsButtonAction.FAVORITE,
            DetailsButtonAction.EPISODES,
            DetailsButtonAction.FULL_DETAILS,
            DetailsButtonAction.SUBSCRIPTIONS,
            DetailsButtonAction.TRAILERS,
            DetailsButtonAction.SIMILAR,
            DetailsButtonAction.VIEWING_ORDER,
            DetailsButtonAction.RATING,
            DetailsButtonAction.COLLECTIONS,
            DetailsButtonAction.SCREENSHOTS,
        )
    }
}
