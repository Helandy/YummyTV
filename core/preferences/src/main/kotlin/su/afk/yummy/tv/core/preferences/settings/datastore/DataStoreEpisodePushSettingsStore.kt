package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.preferences.settings.EpisodePushSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.episodePushKnownNotificationIdsKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreEpisodePushSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : EpisodePushSettingsStore {

    override val knownNotificationIds: Flow<Set<Int>> =
        store.stringSet(episodePushKnownNotificationIdsKey)
            .map { ids -> ids.mapNotNull(String::toIntOrNull).toSet() }

    override suspend fun addKnownNotificationIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        store.edit { prefs ->
            val current = prefs[episodePushKnownNotificationIdsKey].orEmpty()
            prefs[episodePushKnownNotificationIdsKey] = current + ids.map(Int::toString)
        }
    }
}
