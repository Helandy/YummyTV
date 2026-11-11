package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.TorrentIndexer
import su.afk.yummy.tv.core.model.settings.TorrentSettings
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.jackettApiKeyKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrServerPasswordKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrServerUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrServerUserKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentIndexerKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.torrentIndexerUrlKey
import su.afk.yummy.tv.core.preferences.settings.TorrentSettingsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreTorrentSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : TorrentSettingsStore {

    override val torrentSettings: Flow<TorrentSettings> = store.data.map { prefs ->
        TorrentSettings(
            enabled = prefs[torrentEnabledKey] ?: false,
            torrServerUrl = prefs[torrServerUrlKey].orEmpty(),
            torrServerUser = prefs[torrServerUserKey].orEmpty(),
            torrServerPassword = prefs[torrServerPasswordKey].orEmpty(),
            indexer = prefs.enum(torrentIndexerKey, TorrentIndexer.JACRED),
            indexerUrl = prefs[torrentIndexerUrlKey] ?: TorrentSettings.DEFAULT_INDEXER_URL,
            jackettApiKey = prefs[jackettApiKeyKey].orEmpty(),
        )
    }

    override suspend fun setTorrentSettings(settings: TorrentSettings) {
        store.edit { prefs ->
            prefs[torrentEnabledKey] = settings.enabled
            prefs[torrServerUrlKey] = settings.torrServerUrl
            prefs[torrServerUserKey] = settings.torrServerUser
            prefs[torrServerPasswordKey] = settings.torrServerPassword
            prefs[torrentIndexerKey] = settings.indexer.name
            prefs[torrentIndexerUrlKey] = settings.indexerUrl
            prefs[jackettApiKeyKey] = settings.jackettApiKey
        }
    }
}
