package su.afk.yummy.tv.feature.player.common.utils

import su.afk.yummy.tv.core.utils.episode.episodeThumbnailModels
import su.afk.yummy.tv.core.utils.episode.episodeThumbnailOf
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeItem

/**
 * Цепочка моделей превью серии для списка/ленты серий плеера: кадр серии (Kodik-iframe в `screenshotUrl`
 * резолвится Coil-фетчером), затем постер тайтла, если кадра нет или он не загрузился.
 */
fun PlayerEpisodeItem.thumbnailModels(animeId: Int, posterUrl: String): List<Any> =
    episodeThumbnailModels(
        thumbnail = episodeThumbnailOf(animeId, number, screenshotUrl),
        posterUrls = listOf(posterUrl).filter { it.isNotBlank() },
    )
