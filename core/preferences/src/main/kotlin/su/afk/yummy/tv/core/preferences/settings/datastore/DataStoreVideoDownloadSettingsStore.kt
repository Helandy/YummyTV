package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoDownloadWifiOnlyKey
import su.afk.yummy.tv.core.preferences.settings.VideoDownloadSettingsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreVideoDownloadSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : VideoDownloadSettingsStore {

    override val videoDownloadWifiOnly: Flow<Boolean> =
        store.boolean(videoDownloadWifiOnlyKey, false)

    override suspend fun setVideoDownloadWifiOnly(enabled: Boolean) =
        store.setBoolean(videoDownloadWifiOnlyKey, enabled)
}
