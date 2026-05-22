package su.afk.yummy.tv.core.storage.settings

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsStore(private val context: Context) {

    private val posterQualityKey = stringPreferencesKey("poster_quality")
    private val showScreenshotsOnFocusKey = booleanPreferencesKey("show_screenshots_on_focus")
    private val preferredPlayerKey = stringPreferencesKey("preferred_player")
    private val watchNextEnabledKey = booleanPreferencesKey("watch_next_enabled")
    private val previewCacheSizeKey = intPreferencesKey("preview_cache_size")

    val posterQuality: Flow<PosterQuality> = context.dataStore.data.map { prefs ->
        prefs[posterQualityKey]?.let { name ->
            runCatching { PosterQuality.valueOf(name) }.getOrNull()
        } ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PosterQuality.MEGA else PosterQuality.STANDARD
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

    suspend fun setPosterQuality(quality: PosterQuality) {
        context.dataStore.edit { prefs -> prefs[posterQualityKey] = quality.name }
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
        context.dataStore.edit { prefs -> prefs[previewCacheSizeKey] = size.megabytes }
    }
}
