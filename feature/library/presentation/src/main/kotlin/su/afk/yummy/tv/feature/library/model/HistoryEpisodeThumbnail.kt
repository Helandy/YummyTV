package su.afk.yummy.tv.feature.library.model

import su.afk.yummy.tv.core.common.episode.episodeGroupKey

/**
 * Модель для Coil: превью серии из истории просмотров по animeId+episode.
 * В отличие от [su.afk.yummy.tv.core.utils.kodik.KodikThumbnail] здесь ещё нет iframe-урла —
 * его резолвит [HistoryEpisodeThumbnailFetcher] из уже закэшированного (без сети) списка видео
 * аниме.
 */
data class HistoryEpisodeThumbnail(val animeId: Int, val episode: String) {
    val cacheKey: String
        get() = "history_thumb:$animeId:${episode.episodeGroupKey()}"
}
