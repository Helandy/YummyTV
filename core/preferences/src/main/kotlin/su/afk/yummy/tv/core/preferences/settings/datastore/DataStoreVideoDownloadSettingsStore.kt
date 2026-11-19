package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.VideoDownloadPreset
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoDownloadPresetDubbingKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoDownloadPresetPlayerKey
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

    override fun videoDownloadPreset(animeId: Int): Flow<VideoDownloadPreset?> {
        val dubbingKey = videoDownloadPresetDubbingKey(animeId)
        val playerKey = videoDownloadPresetPlayerKey(animeId)
        return store.data
            .map { prefs ->
                val dubbing = prefs[dubbingKey].orEmpty()
                val player = prefs[playerKey].orEmpty()
                if (dubbing.isEmpty() || player.isEmpty()) null else VideoDownloadPreset(dubbing, player)
            }
            .distinctUntilChanged()
    }

    override suspend fun setVideoDownloadPreset(animeId: Int, preset: VideoDownloadPreset?) {
        val dubbingKey = videoDownloadPresetDubbingKey(animeId)
        val playerKey = videoDownloadPresetPlayerKey(animeId)
        store.edit { prefs ->
            if (preset == null || preset.dubbing.isEmpty() || preset.player.isEmpty()) {
                prefs.remove(dubbingKey)
                prefs.remove(playerKey)
            } else {
                prefs[dubbingKey] = preset.dubbing
                prefs[playerKey] = preset.player
            }
        }
    }
}
