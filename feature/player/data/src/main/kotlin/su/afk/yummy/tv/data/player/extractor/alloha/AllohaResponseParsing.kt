package su.afk.yummy.tv.data.player.extractor.alloha

import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack

private const val LOG_TAG = PersistedLogTags.ALLOHA_EXTRACTOR

internal data class AllohaParsedSources(
    val audioTracks: List<AllohaAudioTrack>,
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
                (quality.optNonBlankString(label) ?: return@forEach)
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
                AllohaAudioTrack(
                    // audioId is Alloha's own identifier; fall back to the index so a source
                    // without one still stays selectable.
                    id = source.optNonBlankString("audioId") ?: index.toString(),
                    label = source.optNonBlankString("label")?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: "#${index + 1}",
                    isDefault = source.optBoolean("default"),
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
            val src = entry.optNonBlankString("src")?.normalizeStreamUrl()
                ?.takeIf(String::isNotBlank)
                ?: continue
            add(
                AllohaSubtitleTrack(
                    label = entry.optNonBlankString("label")?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: "#${index + 1}",
                    url = src,
                    language = entry.optNonBlankString("language"),
                    format = src.substringBefore('?').substringAfterLast('.', "")
                        .lowercase()
                        .takeIf(String::isNotBlank),
                )
            )
        }
    }

    analytics.log(LOG_TAG) {
        "bnsi audioTracks=${audioTracks.map(AllohaAudioTrack::label)} " +
                "subtitles=${subtitles.map(AllohaSubtitleTrack::label)}"
    }
    return AllohaParsedSources(audioTracks = audioTracks, subtitles = subtitles)
}

/**
 * `optString` у Android `org.json` на JSON `null` возвращает строку `"null"`, которая не пуста и
 * превращалась бы в «ссылку» или идентификатор дорожки. Здесь `null` и пустая строка — отсутствие.
 */
private fun JSONObject.optNonBlankString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

internal fun String.normalizeStreamUrl(): String = if (startsWith("//")) "https:$this" else this

private fun String.normalizeQualityLabel(): String =
    trim().let { if (it.endsWith("p")) it else "${it}p" }
