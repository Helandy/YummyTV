package su.afk.yummy.tv.source.ashdi.data.utils

private val VOD_URL = Regex("""^https://ashdi\.vip/vod/\d{1,12}(?:\?.*)?$""")
private val FILE = Regex("""\bfile\s*:\s*['"](https://[^'"\s]+\.m3u8[^'"\s]*)['"]""")

/** Заглушка гео-блока Ashdi: страница «Змініть країну перегляду» / «Плеєр недоступний … в цій країні» вместо плеера. */
private val REGION_STUB = Regex("""Змініть країну|в цій країні""")
private val RESOLUTION = Regex("RESOLUTION=\\d+x(\\d+)", RegexOption.IGNORE_CASE)

/** Адрес страницы серии на Ashdi (`https://ashdi.vip/vod/104245`); другие хосты и пути не принимаются. */
internal fun isAshdiVodUrl(url: String): Boolean = VOD_URL.matches(url)

/** HLS-мастер из `new Playerjs({ file:'…/index.m3u8' })` на странице плеера. */
internal fun ashdiMaster(html: String): String? = FILE.find(html)?.groupValues?.get(1)

/** Страница Ashdi — заглушка гео-блока, а не плеер. */
internal fun ashdiRegionBlocked(html: String): Boolean = REGION_STUB.containsMatchIn(html)

/** Качества из мастера: у Ashdi каждый вариант — отдельный поток со звуком. Порядок по возрастанию высоты, "720p" → адрес. */
internal fun ashdiQualities(master: String): LinkedHashMap<String, String> {
    val lines = master.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
    if (lines.firstOrNull()?.startsWith("#EXTM3U") != true) return LinkedHashMap()
    val variants = lines.withIndex().mapNotNull { (i, line) ->
        if (!line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) return@mapNotNull null
        val height = RESOLUTION.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
        val url = lines.getOrNull(i + 1)?.takeIf { it.startsWith("https://") } ?: return@mapNotNull null
        height to url
    }
    val result = LinkedHashMap<String, String>()
    variants.sortedBy { it.first }.forEach { (height, url) -> result["${height}p"] = url }
    return result
}
