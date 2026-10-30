package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.preferences.settings.EpisodePushSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.episodePushEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.episodePushKnownLibraryEpisodesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.episodePushKnownNotificationIdsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.episodePushLibraryEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.episodePushRecentPushesKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreEpisodePushSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : EpisodePushSettingsStore {

    override val knownNotificationIds: Flow<Set<Int>> =
        store.stringSet(episodePushKnownNotificationIdsKey)
            .map { ids -> ids.mapNotNull(String::toIntOrNull).toSet() }

    override val enabled: Flow<Boolean> = store.boolean(episodePushEnabledKey, true)

    override suspend fun setEnabled(enabled: Boolean) =
        store.setBoolean(episodePushEnabledKey, enabled)

    override val libraryEnabled: Flow<Boolean> = store.boolean(episodePushLibraryEnabledKey, true)

    override suspend fun setLibraryEnabled(enabled: Boolean) =
        store.setBoolean(episodePushLibraryEnabledKey, enabled)

    override val knownLibraryEpisodeKeys: Flow<Set<String>> =
        store.stringSet(episodePushKnownLibraryEpisodesKey)

    override suspend fun setKnownLibraryEpisodeKeys(keys: Set<String>) {
        store.edit { prefs -> prefs[episodePushKnownLibraryEpisodesKey] = keys }
    }

    override val recentPushes: Flow<Map<Int, Long>> =
        store.stringSet(episodePushRecentPushesKey).map { entries -> entries.parseRecentPushes() }

    override suspend fun recordPush(animeId: Int, seconds: Long) {
        store.edit { prefs ->
            val kept = prefs[episodePushRecentPushesKey].orEmpty().parseRecentPushes()
                .filter { (id, time) -> id != animeId && time >= seconds - RECENT_PUSHES_RETENTION_SECONDS }
            prefs[episodePushRecentPushesKey] =
                (kept + (animeId to seconds)).mapTo(mutableSetOf()) { (id, time) -> "$id:$time" }
        }
    }

    override suspend fun addKnownNotificationIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        store.edit { prefs ->
            val current = prefs[episodePushKnownNotificationIdsKey].orEmpty()
            prefs[episodePushKnownNotificationIdsKey] = current + ids.map(Int::toString)
        }
    }

    private fun Set<String>.parseRecentPushes(): Map<Int, Long> =
        mapNotNull { entry ->
            val (id, time) = entry.split(':', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            (id.toIntOrNull() ?: return@mapNotNull null) to (time.toLongOrNull() ?: return@mapNotNull null)
        }.toMap()

    private companion object {
        const val RECENT_PUSHES_RETENTION_SECONDS = 3L * 24 * 60 * 60
    }
}
