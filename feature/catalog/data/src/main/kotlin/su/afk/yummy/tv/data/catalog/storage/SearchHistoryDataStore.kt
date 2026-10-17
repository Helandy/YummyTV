package su.afk.yummy.tv.data.catalog.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Отдельный файл: история — данные каталога, а не настройки приложения. Битый файл заменяется
// пустым, иначе каждая запись падала бы с CorruptionException.
internal val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "search_history",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Запросы хранятся JSON-массивом: порядок важен, а `stringSet` его не сохраняет. */
internal val searchHistoryKey = stringPreferencesKey("queries")
