package su.afk.yummy.tv.source.amedia.data.utils

import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisode
import su.afk.yummy.tv.source.amedia.domain.model.AmediaEpisodeSource
import java.net.URI

private val ANCHOR_TAG = Regex("""<a\s[^>]*>""")
private val VOD_LINK = Regex("""https://aser\.pro/vod/(\d+)""")
private const val RUTUBE_EMBED_PREFIX = "https://rutube.ru/play/embed/"
private val VOD_FILE = Regex("""file:\s*"(https://[^"]+\.m3u8[^"]*)"""")
private val RESOLUTION = Regex("""RESOLUTION=\d+x(\d+)""")

private fun String.attr(name: String): String? =
    Regex("""\b$name="([^"]*)"""").find(this)?.groupValues?.get(1)

/**
 * Вышедшие серии из кнопок `data-vid` + `data-vlnk`: ссылка на `https://aser.pro/vod/{id}` или, у части серий, на Rutube-embed
 * вместо него. У ещё не вышедшей серии ссылка ведёт на `/index.php?do=nz…`, такие пропускаются.
 */
internal fun parseAmediaEpisodes(html: String): List<AmediaEpisode> =
    ANCHOR_TAG.findAll(html).mapNotNull { match ->
        val tag = match.value
        val number = tag.attr("data-vid")?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        val link = tag.attr("data-vlnk").orEmpty()
        val vodId = VOD_LINK.find(link)?.groupValues?.get(1)?.toLongOrNull()
        val source = when {
            vodId != null -> AmediaEpisodeSource.Vod(vodId)
            link.startsWith(RUTUBE_EMBED_PREFIX) && link.length > RUTUBE_EMBED_PREFIX.length -> AmediaEpisodeSource.Rutube(link)
            else -> return@mapNotNull null
        }
        AmediaEpisode(number, source)
    }.toList()

/** Адрес мастер-плейлиста HLS (только https) из `new Playerjs({file: "…"})` на странице `aser.pro/vod/{id}`. */
internal fun parseAmediaVodFile(html: String): String? = VOD_FILE.find(html)?.groupValues?.get(1)

/** Варианты качества мастер-плейлиста: высота кадра → абсолютный адрес плейлиста (относительные пути берутся от [masterUrl]). */
internal fun parseAmediaMasterPlaylist(text: String, masterUrl: String): Map<Int, String> {
    val result = LinkedHashMap<Int, String>()
    val lines = text.lines().map { it.trim() }
    lines.forEachIndexed { index, line ->
        if (!line.startsWith("#EXT-X-STREAM-INF")) return@forEachIndexed
        val height = RESOLUTION.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@forEachIndexed
        val path = lines.drop(index + 1).firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return@forEachIndexed
        val resolved = runCatching { URI(masterUrl).resolve(path).toString() }.getOrNull()
            ?.takeIf { it.startsWith("https://") } ?: return@forEachIndexed
        result.putIfAbsent(height, resolved)
    }
    return result
}
