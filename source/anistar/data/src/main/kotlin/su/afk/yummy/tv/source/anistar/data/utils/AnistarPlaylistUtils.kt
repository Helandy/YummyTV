package su.afk.yummy.tv.source.anistar.data.utils

private val ENTRY_START = Regex("title:\\s*\"([^\"]*)\",\\s*media_id:")
private val FILES = Regex("(?<![A-Za-z_])files:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
private val FILES_AS = Regex("files_as:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
private val FILE = Regex("title:\\s*\"(\\d+)\",\\s*file:\\s*\"([^\"]+)\"")
private val NUMBER = Regex("\\d+")

/**
 * Серии из страницы `…/videoas_p2p_new.php?id={newsId}`. Ссылка на HLS — `…/360.mp4/index.m3u8`: ExoPlayer узнаёт HLS по
 * расширению. У части тайтлов в `files` для 720 лежит токенная ссылка `sfv…/media=hls5/…/720.mp4` (плейлист HLS без `.m3u8` в
 * адресе, плеер принимает её за mp4), а обычная ссылка на HLS — в `files_as`. Поэтому для каждого качества берётся первая
 * ссылка на `.m3u8` из `files` и `files_as`, а ссылка без расширения — только если другой нет. `files_mp4` не берётся.
 * Номер серии — первое число в названии («Серия 391»); записи без числа (фильм, спешл) получают порядковый номер.
 * Записи без ссылок пропускаются.
 */
internal fun parseAnistarPlaylist(html: String): List<AnistarPlaylistEntry> {
    val starts = ENTRY_START.findAll(html).toList()
    return starts.mapIndexedNotNull { index, start ->
        val block = html.substring(start.range.first, starts.getOrNull(index + 1)?.range?.first ?: html.length)
        val hls = block.hlsByQuality()
        if (hls.isEmpty()) return@mapIndexedNotNull null
        val number = NUMBER.find(start.groupValues[1])?.value?.toIntOrNull()?.takeIf { it > 0 } ?: (index + 1)
        AnistarPlaylistEntry(number, hls)
    }
}

private fun String.hlsByQuality(): Map<Int, String> {
    val primary = files(FILES)
    val all = primary + files(FILES_AS)
    return all.map { it.first }.toSortedSet().mapNotNull { quality ->
        val url = all.firstOrNull { it.first == quality && it.second.isHls() }?.second
            ?: primary.firstOrNull { it.first == quality }?.second
        url?.let { quality to it }
    }.toMap()
}

private fun String.files(pattern: Regex): List<Pair<Int, String>> =
    pattern.find(this)?.groupValues?.get(1).orEmpty().let { files ->
        FILE.findAll(files).mapNotNull { match ->
            match.groupValues[1].toIntOrNull()?.let { it to match.groupValues[2] }
        }.toList()
    }

private fun String.isHls(): Boolean = substringBefore('?').endsWith(".m3u8")
