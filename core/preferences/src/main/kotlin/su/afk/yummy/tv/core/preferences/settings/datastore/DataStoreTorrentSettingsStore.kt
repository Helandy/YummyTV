package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentSort
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.jackettApiKeyKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.jacRedEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.jackettEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.jackettUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrServerPasswordKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrServerUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrServerUserKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentIndexerKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentIndexerUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentSortKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentSortReversedKey
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreTorrentSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : TorrentSettingsStore {

    override val torrentSettings: Flow<TorrentSettings> = store.data.map { prefs ->
        // Раньше выбирался один индексатор (torrentIndexerKey): по нему задаём стартовые тумблеры.
        val legacyIndexer = prefs.enum(torrentIndexerKey, TorrentIndexer.JACRED)
        val legacyUrl = prefs[torrentIndexerUrlKey]
        // Миграция: раньше адрес был один на оба индексатора. Если его ещё не разделили и выбран Jackett,
        // сохранённый адрес принадлежит Jackett, а JacRed получает адрес по умолчанию.
        val migrateToJackett = prefs[jackettUrlKey] == null && legacyIndexer == TorrentIndexer.JACKETT
        val jackettEnabled = prefs[jackettEnabledKey] ?: (legacyIndexer == TorrentIndexer.JACKETT)
        TorrentSettings(
            enabled = prefs[torrentEnabledKey] ?: false,
            torrServerUrl = prefs[torrServerUrlKey].orEmpty(),
            torrServerUser = prefs[torrServerUserKey].orEmpty(),
            torrServerPassword = prefs[torrServerPasswordKey].orEmpty(),
            jacRedEnabled = prefs[jacRedEnabledKey] ?: (legacyIndexer == TorrentIndexer.JACRED),
            jackettEnabled = jackettEnabled,
            jacRedUrl = if (migrateToJackett) {
                TorrentSettings.DEFAULT_INDEXER_URL
            } else {
                legacyUrl ?: TorrentSettings.DEFAULT_INDEXER_URL
            },
            // Адрес Jackett у каждого свой: пока он не сохранён, в поле стоит адрес-пример (индексатор при этом выключен).
            jackettUrl = prefs[jackettUrlKey] ?: when {
                migrateToJackett -> legacyUrl.orEmpty()
                jackettEnabled -> ""
                else -> TorrentSettings.JACKETT_URL_HINT
            },
            jackettApiKey = prefs[jackettApiKeyKey].orEmpty(),
            sort = prefs.enum(torrentSortKey, TorrentSort.SEEDERS),
            sortReversed = prefs[torrentSortReversedKey] ?: false,
        )
    }

    override suspend fun setTorrentSettings(settings: TorrentSettings) {
        store.edit { prefs ->
            prefs[torrentEnabledKey] = settings.enabled
            prefs[torrServerUrlKey] = settings.torrServerUrl
            prefs[torrServerUserKey] = settings.torrServerUser
            prefs[torrServerPasswordKey] = settings.torrServerPassword
            prefs[jacRedEnabledKey] = settings.jacRedEnabled
            prefs[jackettEnabledKey] = settings.jackettEnabled
            prefs[torrentIndexerUrlKey] = settings.jacRedUrl
            prefs[jackettUrlKey] = settings.jackettUrl
            prefs[jackettApiKeyKey] = settings.jackettApiKey
            prefs[torrentSortKey] = settings.sort.name
            prefs[torrentSortReversedKey] = settings.sortReversed
        }
    }
}
