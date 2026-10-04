package su.afk.yummy.tv.data.player.extractor.alloha

import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack

private const val LOG_TAG = "AllohaExtractor"

internal fun parseHeaders(headersJson: String): Map<String, String> {
    val objectValue = JSONObject(headersJson)
    return buildMap {
        objectValue.keys().forEach { key ->
            objectValue.optString(key).takeIf(String::isNotBlank)
                ?.let { put(key.lowercase(), it) }
        }
    }
}

/**
 * One `hlsSource` entry: a dubbing with its own quality ladder. Alloha's player shows exactly this
 * list behind its gear icon, so it - not the site-level catalog - is the source of truth for what
 * voices this episode actually has.
 */
internal data class AllohaParsedAudioTrack(
    val track: AllohaAudioTrack,
    val qualities: LinkedHashMap<String, String>,
)

internal data class AllohaParsedSources(
    val audioTracks: List<AllohaParsedAudioTrack>,
    val subtitles: List<AllohaSubtitleTrack>,
)

internal fun parseSources(
    responseJson: String,
    analytics: AnalyticsTracker,
): AllohaParsedSources {
    val root = JSONObject(responseJson)
    val sources = root.optJSONArray("hlsSource")
        ?: throw AllohaSourceUnavailableException("hlsSource is missing")

    val audioTracks = buildList {
        for (index in 0 until sources.length()) {
            val source = sources.optJSONObject(index) ?: continue
            val quality = source.optJSONObject("quality") ?: continue
            val qualities = linkedMapOf<String, String>()
            quality.keys().forEach { label ->
                quality.optString(label)
                    .split(" or ")
                    .firstOrNull()
                    ?.trim()
                    ?.normalizeStreamUrl()
                    ?.takeIf(String::isNotBlank)
                    ?.let { qualities.putIfAbsent(label.normalizeQualityLabel(), it) }
            }
            if (qualities.isEmpty()) continue
            val sorted = qualities.entries
                .sortedBy { it.key.filter(Char::isDigit).toIntOrNull() ?: 0 }
                .associateTo(linkedMapOf()) { it.toPair() }
            add(
                AllohaParsedAudioTrack(
                    track = AllohaAudioTrack(
                        // audioId is Alloha's own identifier; fall back to the index so a source
                        // without one still stays selectable.
                        id = source.optString("audioId").takeIf(String::isNotBlank)
                            ?: index.toString(),
                        label = source.optString("label").trim().takeIf(String::isNotBlank)
                            ?: "#${index + 1}",
                        isDefault = source.optBoolean("default"),
                    ),
                    qualities = sorted,
                )
            )
        }
    }
    if (audioTracks.isEmpty()) throw AllohaSourceUnavailableException("no HLS qualities found")

    val subtitles = buildList {
        val tracks = root.optJSONArray("tracks") ?: return@buildList
        for (index in 0 until tracks.length()) {
            val entry = tracks.optJSONObject(index) ?: continue
            if (!entry.optString("kind").equals("captions", ignoreCase = true)) continue
            val src = entry.optString("src").normalizeStreamUrl().takeIf(String::isNotBlank)
                ?: continue
            add(
                AllohaSubtitleTrack(
                    label = entry.optString("label").trim().takeIf(String::isNotBlank)
                        ?: "#${index + 1}",
                    url = src,
                    language = entry.optString("language").takeIf(String::isNotBlank),
                    format = src.substringBefore('?').substringAfterLast('.', "")
                        .lowercase()
                        .takeIf(String::isNotBlank),
                )
            )
        }
    }

    analytics.log(LOG_TAG) {
        "bnsi audioTracks=${audioTracks.map { it.track.label }} " +
                "subtitles=${subtitles.map(AllohaSubtitleTrack::label)}"
    }
    return AllohaParsedSources(audioTracks = audioTracks, subtitles = subtitles)
}

/** Used both here and by [su.afk.yummy.tv.data.player.extractor.alloha.AllohaExtractor]'s bridge. */
internal fun String.normalizeStreamUrl(): String = if (startsWith("//")) "https:$this" else this

private fun String.normalizeQualityLabel(): String =
    trim().let { if (it.endsWith("p")) it else "${it}p" }
