package su.afk.yummy.tv.feature.library.model

import su.afk.yummy.tv.core.utils.kodik.episodeThumbnailCacheKey

/**
 * Модель для Coil: превью серии из истории просмотров по animeId+episode.
 * В отличие от [su.afk.yummy.tv.core.utils.kodik.KodikThumbnail] здесь ещё нет iframe-урла —
 * его резолвит [HistoryEpisodeThumbnailFetcher] из уже закэшированного (без сети) списка видео
 * аниме. Ключ кэша общий с [su.afk.yummy.tv.core.utils.kodik.KodikThumbnail] той же серии,
 * поэтому превью, уже скачанное на любом экране, берётся с диска без списка видео и сети.
 */
data class HistoryEpisodeThumbnail(val animeId: Int, val episode: String) {
    val cacheKey: String
        get() = episodeThumbnailCacheKey(animeId, episode)
}
