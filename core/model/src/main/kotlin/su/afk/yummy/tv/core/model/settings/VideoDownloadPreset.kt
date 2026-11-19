package su.afk.yummy.tv.core.model.settings

/**
 * Запомненный выбор источника для скачивания серий: озвучка и плеер.
 * Качество в пресет не входит — его выбирают при каждой загрузке.
 */
data class VideoDownloadPreset(
    val dubbing: String,
    val player: String,
)
