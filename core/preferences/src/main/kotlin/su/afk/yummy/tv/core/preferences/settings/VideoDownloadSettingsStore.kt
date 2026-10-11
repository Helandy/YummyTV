package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow

/** Офлайн-загрузки серий: сетевая политика. */
interface VideoDownloadSettingsStore {

    /** Качать только по безлимитной сети (Wi-Fi, Ethernet); иначе загрузки ждут в очереди. */
    val videoDownloadWifiOnly: Flow<Boolean>

    suspend fun setVideoDownloadWifiOnly(enabled: Boolean)
}
