package su.afk.yummy.tv.feature.player.view

internal const val TAG = "YummyPlayer"

private val ALL_QUALITY_LEVELS = listOf(360, 480, 720, 1080, 1440, 2160)

// From URL like ".../1080.mp4:hls:manifest.m3u8" generates quality map up to detected max.
fun deriveQualityUrls(url: String): LinkedHashMap<String, String> {
    val match = Regex("""(\d+)\.mp4:hls:manifest\.m3u8""").find(url)
        ?: return linkedMapOf("auto" to url)
    val detected = match.groupValues[1].toIntOrNull() ?: return linkedMapOf("auto" to url)
    val available = ALL_QUALITY_LEVELS.filter { it <= detected }
    return available.associateTo(LinkedHashMap()) { q ->
        "${q}p" to url.replace("$detected.mp4:hls:", "$q.mp4:hls:")
    }
}
