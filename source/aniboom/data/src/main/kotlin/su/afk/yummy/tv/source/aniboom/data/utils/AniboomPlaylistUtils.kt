package su.afk.yummy.tv.source.aniboom.data.utils

import su.afk.yummy.tv.domain.player.utils.withMaxVideoHeight

private val RESOLUTION = Regex("RESOLUTION=\\d+x(\\d+)", RegexOption.IGNORE_CASE)

/**
 * Качества из мастер-плейлиста: метка `"720p"` → ссылка на тот же мастер с ограничением высоты. Звук у AniBoom
 * лежит отдельной группой `#EXT-X-MEDIA`, поэтому голая ссылка на вариант осталась бы без звука.
 * Порядок — по возрастанию качества, пусто — если вариантов меньше двух.
 */
internal fun aniboomQualities(master: String, masterUrl: String): LinkedHashMap<String, String> {
    val lines = master.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (lines.firstOrNull()?.startsWith("#EXTM3U") != true) return LinkedHashMap()

    val heights = lines
        .filter { it.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) }
        .mapNotNull { RESOLUTION.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        .toSortedSet()
    if (heights.size < 2) return LinkedHashMap()

    val qualities = LinkedHashMap<String, String>()
    heights.forEach { qualities["${it}p"] = masterUrl.withMaxVideoHeight(it) }
    return qualities
}
