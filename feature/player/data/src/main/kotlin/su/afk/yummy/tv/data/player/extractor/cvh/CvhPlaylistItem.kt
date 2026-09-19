package su.afk.yummy.tv.data.player.extractor.cvh

/** One entry of the CdnVideoHub playlist, flattened out of the JSON response. */
internal data class CvhPlaylistItem(
    val vkId: String,
    val voiceStudio: String,
    val voiceType: String,
    /** null for movies - the playlist omits `episode`/`season` entirely when `isSerial` is false. */
    val episode: Int?,
)

private const val DUBBING_LABEL_PREFIX = "Озвучка "

/**
 * Picks the playlist entry to play.
 *
 * Episode filtering is skipped for movies: their items carry no `episode` field, and treating the
 * missing value as 0 used to drop the only candidate, failing the whole extraction.
 *
 * The voice match falls back through several keys because `dubbing_code` doesn't always equal
 * `voiceStudio` - a localized studio name arrives transliterated ("ort" vs "ОРТ"), and only the
 * `dubbing` label ("Озвучка Дубляж ОРТ") spells it the way the playlist does.
 */
internal fun selectCvhItem(
    items: List<CvhPlaylistItem>,
    isSerial: Boolean,
    episodeNum: Int,
    dubbingCode: String,
    dubbingLabel: String,
): CvhPlaylistItem? {
    val candidates = if (!isSerial) {
        items
    } else {
        items.filter { it.episode == null || it.episode == episodeNum }
    }
    if (candidates.isEmpty()) return null

    val label = dubbingLabel.removePrefix(DUBBING_LABEL_PREFIX).trim()

    return candidates.firstOrNull { it.voiceStudio.equalsKey(dubbingCode) }
        ?: candidates.firstOrNull { "${it.voiceType} ${it.voiceStudio}".trim().equalsKey(label) }
        ?: candidates.firstOrNull { it.voiceStudio.equalsKey(label) }
        ?: candidates.first()
}

private fun String.equalsKey(key: String): Boolean =
    key.isNotBlank() && equals(key, ignoreCase = true)
