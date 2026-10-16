package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.LastSearchSnapshot
import su.afk.yummy.tv.core.preferences.settings.SearchSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchAgeRatingsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchExcludedGenresKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchFromYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchGenresKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchQueryKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchSeasonsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchSortForwardKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchSortKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchStatusesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchToYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSearchTypesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.saveLastSearchEnabledKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreSearchSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : SearchSettingsStore {

    override val saveLastSearchEnabled: Flow<Boolean> =
        store.boolean(saveLastSearchEnabledKey, false)

    override val lastSearchSnapshot: Flow<LastSearchSnapshot> = store.data.map { prefs ->
        LastSearchSnapshot(
            query = prefs[lastSearchQueryKey].orEmpty(),
            genres = prefs[lastSearchGenresKey].orEmpty(),
            excludedGenres = prefs[lastSearchExcludedGenresKey].orEmpty(),
            types = prefs[lastSearchTypesKey].orEmpty(),
            statuses = prefs[lastSearchStatusesKey].orEmpty(),
            seasons = prefs[lastSearchSeasonsKey].orEmpty(),
            ageRatings = prefs[lastSearchAgeRatingsKey].orEmpty()
                .mapNotNull(String::toIntOrNull).toSet(),
            fromYear = prefs[lastSearchFromYearKey],
            toYear = prefs[lastSearchToYearKey],
            sortName = prefs[lastSearchSortKey] ?: "TOP",
            sortForward = prefs[lastSearchSortForwardKey] ?: true,
        )
    }

    override suspend fun setSaveLastSearchEnabled(enabled: Boolean) {
        store.setBoolean(saveLastSearchEnabledKey, enabled)
        if (!enabled) clearLastSearchSnapshot()
    }

    override suspend fun setLastSearchSnapshot(snapshot: LastSearchSnapshot) {
        store.edit { prefs ->
            prefs[lastSearchQueryKey] = snapshot.query
            prefs[lastSearchGenresKey] = snapshot.genres
            prefs[lastSearchExcludedGenresKey] = snapshot.excludedGenres
            prefs[lastSearchTypesKey] = snapshot.types
            prefs[lastSearchStatusesKey] = snapshot.statuses
            prefs[lastSearchSeasonsKey] = snapshot.seasons
            prefs[lastSearchAgeRatingsKey] = snapshot.ageRatings.map(Int::toString).toSet()
            val fromYear = snapshot.fromYear
            if (fromYear != null) {
                prefs[lastSearchFromYearKey] = fromYear
            } else {
                prefs.remove(lastSearchFromYearKey)
            }
            val toYear = snapshot.toYear
            if (toYear != null) {
                prefs[lastSearchToYearKey] = toYear
            } else {
                prefs.remove(lastSearchToYearKey)
            }
            prefs[lastSearchSortKey] = snapshot.sortName
            prefs[lastSearchSortForwardKey] = snapshot.sortForward
        }
    }

    private suspend fun clearLastSearchSnapshot() {
        store.edit { prefs ->
            prefs.remove(lastSearchQueryKey)
            prefs.remove(lastSearchGenresKey)
            prefs.remove(lastSearchExcludedGenresKey)
            prefs.remove(lastSearchTypesKey)
            prefs.remove(lastSearchStatusesKey)
            prefs.remove(lastSearchSeasonsKey)
            prefs.remove(lastSearchAgeRatingsKey)
            prefs.remove(lastSearchFromYearKey)
            prefs.remove(lastSearchToYearKey)
            prefs.remove(lastSearchSortKey)
            prefs.remove(lastSearchSortForwardKey)
        }
    }
}
