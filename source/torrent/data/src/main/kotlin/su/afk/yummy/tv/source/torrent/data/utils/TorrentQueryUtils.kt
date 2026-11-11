package su.afk.yummy.tv.source.torrent.data.utils

import su.afk.yummy.tv.core.model.anime.AnimeDetails

private val CYRILLIC = Regex("[\\p{IsCyrillic}]")

/**
 * Названия для поиска: основное и первое латинское из `otherTitles` (индексаторы часто хранят английское/ромадзи
 * название). Не больше двух: каждый запрос — отдельный поход в индексатор.
 */
internal fun AnimeDetails.searchQueries(): List<String> {
    val latin = otherTitles.firstOrNull { it.isNotBlank() && !CYRILLIC.containsMatchIn(it) }
    return listOfNotNull(title.takeIf { it.isNotBlank() }, latin).distinct()
}
