package su.afk.yummy.tv.source.animevost.data.utils

internal const val ANIMEVOST_BASE_URL = "https://animevost.org"

private val DATA = Regex("""var data = \{(.*?)\}\s*;""", RegexOption.DOT_MATCHES_ALL)
private val DATA_ITEM = Regex(""""([^"]*)"\s*:\s*"(\d{1,18})"""")
private val EPISODE_LABEL = Regex("""^\s*(\d{1,4})\s*сери""", RegexOption.IGNORE_CASE)
private val DOWNLOAD_LINK = Regex("""download="invoice"[^>]*href="(https?://[^"]+)"[^>]*>\s*(\d{3,4})p""")

/** Адрес страницы тайтла. */
internal fun animevostPageUrl(typePath: String, slug: String): String = "$ANIMEVOST_BASE_URL/tip/$typePath/$slug.html"

/** Плеер серии со ссылками на mp4. */
internal fun animevostFrameUrl(episodeId: String): String = "$ANIMEVOST_BASE_URL/frame5.php?play=$episodeId&old=1"

/** Id серии [episode] из `var data = {"1 серия":"123",…}` на странице тайтла; `null`, если такой серии нет (первая метка с номером). */
internal fun animevostEpisodeId(html: String, episode: Int): String? {
    val data = DATA.find(html)?.groupValues?.get(1) ?: return null
    return DATA_ITEM.findAll(data)
        .firstOrNull { EPISODE_LABEL.find(it.groupValues[1])?.groupValues?.get(1)?.toIntOrNull() == episode }
        ?.groupValues?.get(2)
}

/** Ссылки `720p`/`480p` из плеера, по возрастанию качества; пусто, если плеер ссылок не отдал. */
internal fun animevostQualities(html: String): LinkedHashMap<String, String> {
    val byHeight = sortedMapOf<Int, String>()
    DOWNLOAD_LINK.findAll(html).forEach {
        val height = it.groupValues[2].toIntOrNull() ?: return@forEach
        byHeight.putIfAbsent(height, it.groupValues[1].replace("&amp;", "&"))
    }
    val result = LinkedHashMap<String, String>()
    byHeight.forEach { (height, url) -> result["${height}p"] = url }
    return result
}
