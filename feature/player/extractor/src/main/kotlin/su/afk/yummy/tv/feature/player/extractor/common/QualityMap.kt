package su.afk.yummy.tv.feature.player.extractor.common

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

val STANDARD_QUALITY_KEYS =
    listOf("auto", "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")

/**
 * Reorders [raw] to follow [keys], appending any leftover entries (unknown quality labels) at the
 * end in their original order. [keyAliases] lets a caller accept an alternate raw key per quality
 * (e.g. a bare "720" in addition to "720p") without changing the emitted key.
 */
fun orderQualityMap(
    raw: LinkedHashMap<String, String>,
    keys: List<String> = STANDARD_QUALITY_KEYS,
    keyAliases: (String) -> List<String> = { listOf(it) },
): LinkedHashMap<String, String> {
    val ordered = LinkedHashMap<String, String>()
    keys.forEach { qualityKey ->
        keyAliases(qualityKey).firstNotNullOfOrNull { raw[it] }?.let { ordered[qualityKey] = it }
    }
    raw.forEach { (key, value) -> if (!ordered.containsKey(key)) ordered[key] = value }
    return ordered
}

/** Renames the "auto" entry (if any) to [autoQualityLabel], leaving other entries untouched. */
fun LinkedHashMap<String, String>.withAutoQualityLabel(
    autoQualityLabel: String,
): LinkedHashMap<String, String> {
    if (autoQualityLabel.isBlank() || autoQualityLabel == "auto") return this

    return entries.associateTo(LinkedHashMap()) { (quality, url) ->
        if (quality == "auto") autoQualityLabel to url else quality to url
    }
}

/** Common shape produced by the HTTP-based extractors before mapping to the domain result. */
data class ExtractedStream(
    val url: String,
    val headers: Map<String, String>,
    val qualities: LinkedHashMap<String, String>? = null,
) {
    fun toResolveResult(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )
}
