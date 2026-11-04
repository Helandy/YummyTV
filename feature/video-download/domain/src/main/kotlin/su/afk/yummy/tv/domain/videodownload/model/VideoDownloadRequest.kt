package su.afk.yummy.tv.domain.videodownload.model

import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips

data class VideoDownloadRequest(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
    val episode: String,
    val videoId: Int,
    val playerName: String,
    val playerId: Int?,
    val dubbing: String,
    val iframeUrl: String,
    val screenshotUrl: String,
    val quality: VideoDownloadQualityOption,
    val headers: Map<String, String>,
    /** Тайминги опенинга/эндинга на момент скачивания: нужны автопропуску офлайн. */
    val skips: AnimeVideoSkips = AnimeVideoSkips(),
)
