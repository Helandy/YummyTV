package su.afk.yummy.tv.core.preferences.settings

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val posterQualityKey = stringPreferencesKey("poster_quality")
    private val posterCardSizeKey = stringPreferencesKey("poster_card_size")
    private val showScreenshotsOnFocusKey = booleanPreferencesKey("show_screenshots_on_focus")
    private val preferredPlayerKey = stringPreferencesKey("preferred_player")
    private val watchNextEnabledKey = booleanPreferencesKey("watch_next_enabled")
    private val previewCacheSizeKey = intPreferencesKey("preview_cache_size")
    private val autoSkipOpeningsEndingsKey = booleanPreferencesKey("auto_skip_openings_endings")
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

    val showScreenshotsOnFocus: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[showScreenshotsOnFocusKey] ?: false
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

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        prefs[appThemeKey]?.let { name ->
            runCatching { AppTheme.valueOf(name) }.getOrNull()
        } ?: AppTheme.WARM_AMBER
    }

    val detailsButtonOrder: Flow<List<DetailsButtonAction>> = context.dataStore.data.map { prefs ->
        prefs[detailsButtonOrderKey].toDetailsButtonOrder()
    }

    val yaniApplicationToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[yaniApplicationTokenKey].orEmpty()
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

    suspend fun setShowScreenshotsOnFocus(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[showScreenshotsOnFocusKey] = enabled }
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

    companion object {
        private const val DETAILS_BUTTON_ORDER_SEPARATOR = "|"

        val defaultDetailsButtonOrder: List<DetailsButtonAction> = listOf(
            DetailsButtonAction.WATCH,
            DetailsButtonAction.LIBRARY,
            DetailsButtonAction.FAVORITE,
            DetailsButtonAction.EPISODES,
            DetailsButtonAction.SUBSCRIPTIONS,
            DetailsButtonAction.FULL_DETAILS,
            DetailsButtonAction.TRAILERS,
            DetailsButtonAction.SIMILAR,
            DetailsButtonAction.VIEWING_ORDER,
            DetailsButtonAction.RATING,
            DetailsButtonAction.COLLECTIONS,
            DetailsButtonAction.SCREENSHOTS,
        )
    }
}
