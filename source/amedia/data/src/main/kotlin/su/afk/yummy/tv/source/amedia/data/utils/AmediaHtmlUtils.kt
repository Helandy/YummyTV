package su.afk.yummy.tv.source.amedia.data.utils

import java.net.URI

private val VOD_FILE = Regex("""file:\s*"(https://[^"]+\.m3u8[^"]*)"""")
private val RESOLUTION = Regex("""RESOLUTION=\d+x(\d+)""")

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
