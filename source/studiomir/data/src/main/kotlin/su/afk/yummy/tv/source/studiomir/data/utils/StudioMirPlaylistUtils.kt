// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.source.studiomir.data.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal const val STUDIOMIR_BASE_URL = "https://studiomir.club"

/** Страница тайтла: `{base}/anime/{slug}`. */
internal fun studioMirPageUrl(slug: String): String = "$STUDIOMIR_BASE_URL/anime/$slug"

private val PLAYERJS = Regex("""Playerjs\s*\(\s*\{.*?file\s*:\s*(\[.*?\])\s*\}\s*\)""", RegexOption.DOT_MATCHES_ALL)
private val EPISODE_TITLE = Regex("""^\s*Сери[яи]\s+(\d+)\s*$""", RegexOption.IGNORE_CASE)

/** Вариант в `file` серии: `[720p]https://….m3u8`. */
private val VARIANT = Regex("""\[(\d{3,4})p]\s*(https?://[^\s,\[\]]+)""")

/**
 * HLS-варианты серии [episode] из плейлиста плеера TSM в HTML страницы: «720p» → адрес, по возрастанию качества (лучшее —
 * последнее). Пусто, если плейлиста или такой серии нет; «Трейлер» и прочие записи без номера серии пропускаются.
 */
internal fun studioMirEpisodeQualities(html: String, episode: Int): Map<String, String> {
    val json = PLAYERJS.find(html)?.groupValues?.get(1) ?: return emptyMap()
    val items: JsonArray = runCatching { Json.parseToJsonElement(json).jsonArray }.getOrNull() ?: return emptyMap()
    val file = items.asSequence()
        .map { it as? JsonObject }
        .filterNotNull()
        .firstOrNull { item ->
            val title = item["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            EPISODE_TITLE.matchEntire(title)?.groupValues?.get(1)?.toIntOrNull() == episode
        }
        ?.get("file")?.jsonPrimitive?.contentOrNull
        ?: return emptyMap()
    val variants = VARIANT.findAll(file).map { it.groupValues[1].toInt() to it.groupValues[2] }.sortedBy { it.first }.toList()
    if (variants.isNotEmpty()) {
        return LinkedHashMap<String, String>().apply { variants.forEach { (height, url) -> put("${height}p", url) } }
    }
    // Одиночная ссылка без метки качества.
    val single = file.trim().takeIf { it.startsWith("http") } ?: return emptyMap()
    return mapOf("Auto" to single)
}
