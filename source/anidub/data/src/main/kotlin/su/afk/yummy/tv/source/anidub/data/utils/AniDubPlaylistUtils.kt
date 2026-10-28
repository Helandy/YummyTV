package su.afk.yummy.tv.source.anidub.data.utils

internal const val ANIDUB_BASE_URL = "https://online.anidub.com"

/** Плеер, встроенный в страницу тайтла: его хост меняется (`player26.ladonyvesna2005.info`), `hash` вида `s10/87925c…`. */
internal data class AniDubPlayerRef(val host: String, val hash: String)

private val IFRAME_PLAYER = Regex(
    """<iframe[^>]+src="(https?://[\w.-]+(?::\d+)?)/index\.php\?v=/(s\d{1,4}/[A-Za-z0-9]{8,64})""",
)
private val HASH_PATTERN = Regex("^/?(s\\d{1,4}/[A-Za-z0-9]{8,64})$")

/** Серии плейлиста в JSON контекста плеера: `"link":"\/s10\/…"`, затем `"label":"3 серия"`. */
private val EPISODE_JSON = Regex(""""link"\s*:\s*"([^"]+)"[\s\S]{0,400}?"label"\s*:\s*"(\d+)\s*сери""")
private val EPISODE_SPAN = Regex("""<span[^>]+data="([^"]+)"[^>]*>\s*(\d+)\s*сери""")
private val VARIANT_URL = Regex("""^(https?://\S+)\s*$""", RegexOption.MULTILINE)

/** Имя релиза в плейлисте: `…s03e01.HD1080p.WEBRip…`; качество самого файла, из которого сделаны потоки. */
private val EVENT_FILENAME = Regex("""event-filename="([^"]+)"""")
private val RELEASE_HEIGHT = Regex("""(?:HD)?(2160|1080|720|480)[pр]""", RegexOption.IGNORE_CASE)
private const val DEFAULT_TOP_HEIGHT = 1080

/**
 * Узлы CDN, на которых плеер не работает мастер переспрашивается, пока не выпадет
 * другой узел. Узлы ротируются случайно на каждый запрос.
 */
private val BAD_NODES = setOf("cdn1.anivids.link")

private val URL_HOST = Regex("""^https?://([^/:?#]+)""")

/** Хост ссылки (`cdn5.anivids.link`); пусто, если это не абсолютный адрес. */
internal fun aniDubHost(url: String): String = URL_HOST.find(url)?.groupValues?.get(1)?.lowercase().orEmpty()

internal fun isAniDubBadNode(host: String): Boolean = host.lowercase() in BAD_NODES

/** Адрес страницы тайтла на сайте. */
internal fun aniDubPageUrl(slug: String): String = "$ANIDUB_BASE_URL/$slug.html"

/** Плеер со страницы тайтла; `null`, если плеера нет (старые тайтлы только на Sibnet). */
internal fun aniDubPlayer(html: String): AniDubPlayerRef? =
    IFRAME_PLAYER.find(html)?.let { AniDubPlayerRef(host = it.groupValues[1], hash = it.groupValues[2]) }

/** Адрес плейлиста серий плеера. */
internal fun AniDubPlayerRef.playlistUrl(): String = "$host/index.php?v=/$hash&playlist"

/** Адрес HLS-мастера серии с подписанными ссылками на CDN. */
internal fun AniDubPlayerRef.masterUrl(episodeHash: String): String = "$host/vid.php?v=/$episodeHash"

/**
 * Хэш серии [episode] из плейлиста плеера. Плейлиста нет (одиночное видео) — серия единственная, и это хэш самого плеера.
 * Единственная запись плейлиста — серия 1 при любой подписи (у фильма она «0 серия»). `null`, если такой серии в плейлисте нет.
 */
internal fun aniDubEpisodeHash(playlist: String?, episode: Int, playerHash: String): String? {
    if (playlist == null) return playerHash.takeIf { episode == 1 }
    val links = EPISODE_JSON.findAll(playlist).map { it.groupValues[2].toInt() to it.groupValues[1] }.toList()
        .ifEmpty { EPISODE_SPAN.findAll(playlist).map { it.groupValues[2].toInt() to it.groupValues[1] }.toList() }
    if (links.isEmpty()) return playerHash.takeIf { episode == 1 }
    val link = links.firstOrNull { it.first == episode }?.second
        ?: links.singleOrNull()?.second?.takeIf { episode == 1 }
        ?: return null
    return HASH_PATTERN.matchEntire(link.replace("\\/", "/").trim())?.groupValues?.get(1)
}

/** Высота релиза по имени файла в плейлисте (`HD1080p` → 1080); `null`, если плейлиста или имени нет. */
internal fun aniDubReleaseHeight(playlist: String?): Int? =
    playlist?.let { EVENT_FILENAME.find(it)?.groupValues?.get(1) }?.let { RELEASE_HEIGHT.find(it)?.groupValues?.get(1)?.toInt() }

/**
 * Качества из HLS-мастера: «1080p» → адрес плейлиста, по возрастанию высоты. Поле `RESOLUTION` в манифесте врёт (`fhd` описан как
 * 1280x720, а в сегментах 1920x1080; проверено `ffprobe`), поэтому подпись берётся из имени варианта, `fhd` — качество
 * релиза ([releaseHeight], по умолчанию 1080p), `hd` — 720p, `sd` — 480p; прочие варианты пропускаются. Если разные имена дают одну высоту,
 * остаётся старший вариант (`fhd`). Пусто, если вариантов нет.
 */
internal fun aniDubQualities(master: String, releaseHeight: Int?): Map<String, String> {
    val variants = VARIANT_URL.findAll(master).mapNotNull { match ->
        val url = match.groupValues[1]
        when {
            "/fhd.mp4/" in url -> Triple(3, releaseHeight ?: DEFAULT_TOP_HEIGHT, url)
            "/hd.mp4/" in url -> Triple(2, 720, url)
            "/sd.mp4/" in url -> Triple(1, 480, url)
            else -> null
        }
    }.groupBy { it.second }.mapValues { (_, same) -> same.maxBy { it.first }.third }.toSortedMap()
    return LinkedHashMap<String, String>().apply { variants.forEach { (height, url) -> put("${height}p", url) } }
}
