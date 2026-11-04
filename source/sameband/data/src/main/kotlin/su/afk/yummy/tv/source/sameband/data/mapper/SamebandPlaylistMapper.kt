package su.afk.yummy.tv.source.sameband.data.mapper

import su.afk.yummy.tv.source.sameband.data.dto.SamebandPlaylistItemDto
import su.afk.yummy.tv.source.sameband.data.model.SamebandEpisode
import su.afk.yummy.tv.source.sameband.data.utils.samebandEpisodeNumber
import su.afk.yummy.tv.source.sameband.data.utils.samebandQualities

/**
 * Элементы плейлиста в серии. Номер берётся из подписи, а если её нет — по порядку (с 1). Элементы без разобранных качеств
 * пропускаются; при повторе номера остаётся первая серия.
 */
internal fun List<SamebandPlaylistItemDto>.toEpisodes(): List<SamebandEpisode> =
    mapIndexedNotNull { index, item ->
        val qualities = samebandQualities(item.file.orEmpty())
        if (qualities.isEmpty()) return@mapIndexedNotNull null
        SamebandEpisode(samebandEpisodeNumber(item.title, index + 1), qualities)
    }.distinctBy { it.number }
