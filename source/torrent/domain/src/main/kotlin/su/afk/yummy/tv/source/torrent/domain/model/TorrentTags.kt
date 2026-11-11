package su.afk.yummy.tv.source.torrent.domain.model

/** Метки раздачи, найденные в названии: разрешение, источник записи, кодек и наличие субтитров. */
data class TorrentTags(
    val resolution: String?,
    val source: String?,
    val codec: String?,
    val hasSubtitles: Boolean,
) {
    /** Метки для карточки по важности: разрешение, источник, кодек, субтитры. */
    fun toBadges(subtitlesLabel: String): List<String> =
        listOfNotNull(resolution, source, codec, subtitlesLabel.takeIf { hasSubtitles })
}

private val RESOLUTION_REGEX = Regex("""(?<!\d)(2160|1440|1080|720|576|480)\s?[pi]""", RegexOption.IGNORE_CASE)
private val FOUR_K_REGEX = Regex("""(?<![a-z0-9])(4k|uhd)(?![a-z0-9])""", RegexOption.IGNORE_CASE)
private val FULL_HD_REGEX = Regex("""(?<![a-z0-9])(full\s?hd|fhd)(?![a-z0-9])""", RegexOption.IGNORE_CASE)
private val SUBTITLES_REGEX = Regex(
    """(?<![a-zа-я0-9])(sub|subs|subtitles|multisub|multi-sub|hardsub|softsub|сабы|саб|субтитры|субтитр)(?![a-zа-я0-9])""",
    RegexOption.IGNORE_CASE,
)

/**
 * Разбирает название раздачи на метки. Распознаёт только очевидные варианты (`1080p`, `WEB-DL`, `HEVC`, `Sub`),
 * остальное не угадывает: пустые метки лучше неверных.
 */
fun parseTorrentTags(title: String): TorrentTags {
    val lower = title.lowercase()
    val resolution = RESOLUTION_REGEX.find(title)?.groupValues?.get(1)?.let { if (it == "2160") "4K" else "${it}p" }
        ?: if (FOUR_K_REGEX.containsMatchIn(title)) "4K" else null
        ?: if (FULL_HD_REGEX.containsMatchIn(title)) "1080p" else null
    val source = when {
        "remux" in lower -> "BD Remux"
        Regex("""bd[\s\-]?rip|blu[\s\-]?ray|(?<![a-z])bd(?![a-z])""").containsMatchIn(lower) -> "BDRip"
        Regex("""web[\s\-]?dl""").containsMatchIn(lower) -> "WEB-DL"
        Regex("""web[\s\-]?rip""").containsMatchIn(lower) -> "WEBRip"
        Regex("""hdtv|tv[\s\-]?rip""").containsMatchIn(lower) -> "TVRip"
        Regex("""dvd""").containsMatchIn(lower) -> "DVD"
        else -> null
    }
    val codec = when {
        Regex("""hevc|x\.?265|h\.?265""").containsMatchIn(lower) -> "HEVC"
        Regex("""av1""").containsMatchIn(lower) -> "AV1"
        Regex("""(?<![a-z])avc(?![a-z])|x\.?264|h\.?264""").containsMatchIn(lower) -> "AVC"
        else -> null
    }
    return TorrentTags(resolution, source, codec, SUBTITLES_REGEX.containsMatchIn(title))
}
