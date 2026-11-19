package su.afk.yummy.tv.feature.player.utils

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Префикс `data:`-URI с синтетическим HLS master-плейлистом (см. [buildSyntheticHlsMasterUrl]). Именно base64:
 * Media3 режет декодированную часть URI по запятой, а в `STREAM-INF` запятые есть.
 */
const val SYNTHETIC_MASTER_URI_PREFIX = "data:application/x-mpegURL;base64,"

/**
 * Источники, у которых проверено: каждое качество — отдельный media-плейлист с muxed-аудио (без `EXT-X-MEDIA`)
 * и общей временной шкалой. Новые добавляем только после проверки живого плейлиста: у источников с отдельной
 * аудиогруппой master без неё потеряет звук.
 */
private val PER_QUALITY_HLS_PATTERNS = listOf(
    // Kodik: …/720.mp4:hls:manifest.m3u8
    Regex("""\d+\.mp4:hls:manifest\.m3u8"""),
    // AniLibria: …/videos/media/ts/<релиз>/<серия>/<качество>/<hash>.m3u8
    Regex("""/videos/media/ts/\d+/\d+/\d+/[0-9a-f]+\.m3u8"""),
    // AiLiberty: ailiberty.top/video/playlists/<токен>.m3u8?expires=…&signature=…
    Regex("""ailiberty\.top/video/playlists/[^/?#]+\.m3u8"""),
    // Sameband: sameband.studio/v/anime/<тайтл>/<файл>_<n>/index.m3u8
    Regex("""sameband\.studio/v/anime/[^?#]+/index\.m3u8"""),
    // StudioMir: …/animes/vids/encoded/<тайтл>/<серия>/hls/<качество>/<hash>_<качество>.m3u8
    Regex("""studiomir\.club/animes/vids/encoded/\d+/\d+/hls/\d+/[0-9a-f]+_\d+\.m3u8"""),
)

/** URL — наш синтетический master, а не ссылка источника. */
fun String.isSyntheticMasterUrl(): Boolean = startsWith(SYNTHETIC_MASTER_URI_PREFIX, ignoreCase = true)

/**
 * Собирает из ссылок по качествам один HLS master (`data:`-URI), чтобы ExoPlayer переключал варианты
 * сам: общий буфер, смена на границе сегмента без перезапуска потока и свежая оценка полосы на каждом
 * сегменте. Подходит источникам, где каждое качество — отдельный media-плейлист с одинаковой сеткой
 * сегментов (Kodik, AniLibria, AiLiberty, Sameband, StudioMir); остальным — `null`.
 *
 * BANDWIDTH — номинальный битрейт ступени ([QualityAdaptationPolicy.nominalBitrateBps]): реального в
 * манифестах нет. RESOLUTION обязателен: без него не работает ограничение по высоте.
 */
@OptIn(ExperimentalEncodingApi::class)
fun buildSyntheticHlsMasterUrl(qualities: Map<String, String>): String? {
    val variants = qualities
        .mapNotNull { (label, url) -> label.qualityHeight()?.let { height -> height to url } }
        .filter { (_, url) -> PER_QUALITY_HLS_PATTERNS.any { it.containsMatchIn(url) } }
        .distinctBy { it.first }
        .sortedBy { it.first }
    if (variants.size < 2 || variants.size != qualities.size) return null
    val playlist = buildString {
        append("#EXTM3U\n")
        variants.forEach { (height, url) ->
            val width = (height * 16 / 9 + 1) / 2 * 2
            append("#EXT-X-STREAM-INF:BANDWIDTH=")
                .append(QualityAdaptationPolicy.nominalBitrateBps(height))
                .append(",RESOLUTION=").append(width).append('x').append(height).append('\n')
            append(url).append('\n')
        }
    }
    return SYNTHETIC_MASTER_URI_PREFIX + Base64.Default.encode(playlist.toByteArray())
}

/** Самая качественная ссылка внутри синтетического master (для Cast: ресивер `data:` не откроет). */
@OptIn(ExperimentalEncodingApi::class)
fun String.syntheticMasterTopVariantUrl(): String? {
    if (!isSyntheticMasterUrl()) return null
    val playlist = String(Base64.Default.decode(substring(SYNTHETIC_MASTER_URI_PREFIX.length)))
    return playlist.lineSequence().lastOrNull { it.isNotBlank() && !it.startsWith("#") }?.trim()
}
