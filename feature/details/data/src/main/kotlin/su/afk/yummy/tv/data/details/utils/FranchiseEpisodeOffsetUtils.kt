package su.afk.yummy.tv.data.details.utils

import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.model.anime.AnimeTypeKind
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem

/**
 * id предыдущих TV-записей франшизы (в порядке просмотра) перед [animeId]. В TMDB их серии могут идти сквозной
 * нумерацией, которую продолжает текущая запись. Пусто, если [animeId] нет в списке или перед ним нет сезонов.
 */
internal fun List<AnimeViewingOrderItem>.previousTvEntryIds(animeId: Int): List<Int> {
    val index = indexOfFirst { it.animeId == animeId }
    if (index <= 0) return emptyList()
    return take(index).filter { it.typeKind == AnimeTypeKind.TV }.map { it.animeId }
}

/** Сколько серий в записи: общее число, а если оно неизвестно — вышедшие; null, если неизвестно и то и другое. */
internal fun AnimeEpisodes?.totalOrAiredCount(): Int? =
    this?.count?.takeIf { it > 0 } ?: this?.aired?.takeIf { it > 0 }
