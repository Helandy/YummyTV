package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.VideoDownloadPreset

/** Офлайн-загрузки серий: сетевая политика и запомненный источник. */
interface VideoDownloadSettingsStore {

    /** Качать только по безлимитной сети (Wi-Fi, Ethernet); иначе загрузки ждут в очереди. */
    val videoDownloadWifiOnly: Flow<Boolean>

    suspend fun setVideoDownloadWifiOnly(enabled: Boolean)

    /** Запомненные озвучка и плеер для скачивания тайтла; `null` — пресет не задан. */
    fun videoDownloadPreset(animeId: Int): Flow<VideoDownloadPreset?>

    /** Сохраняет пресет источника тайтла; `null` сбрасывает его. */
    suspend fun setVideoDownloadPreset(animeId: Int, preset: VideoDownloadPreset?)
}
