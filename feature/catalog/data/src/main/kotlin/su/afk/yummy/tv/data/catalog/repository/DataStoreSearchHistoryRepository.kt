package su.afk.yummy.tv.data.catalog.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.data.catalog.storage.mapper.toSearchHistory
import su.afk.yummy.tv.data.catalog.storage.mapper.toSearchHistoryStorage
import su.afk.yummy.tv.data.catalog.storage.searchHistoryKey
import su.afk.yummy.tv.domain.catalog.repository.SearchHistoryRepository
import java.io.IOException

internal class DataStoreSearchHistoryRepository(
    private val dataStore: DataStore<Preferences>,
) : SearchHistoryRepository {

    override fun observe(): Flow<List<String>> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { prefs -> prefs[searchHistoryKey].toSearchHistory() }
        .distinctUntilChanged()

    override suspend fun update(transform: (List<String>) -> List<String>) {
        dataStore.edit { prefs ->
            prefs[searchHistoryKey] = transform(prefs[searchHistoryKey].toSearchHistory())
                .toSearchHistoryStorage()
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(searchHistoryKey) }
    }
}
