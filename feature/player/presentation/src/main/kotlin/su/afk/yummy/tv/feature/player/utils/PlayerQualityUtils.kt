package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.feature.player.PlayerState

private val ALL_QUALITY_LEVELS = listOf(360, 480, 720, 1080, 1440, 2160)

// From URL like ".../1080.mp4:hls:manifest.m3u8" generates quality map up to detected max.
internal fun deriveQualityUrls(url: String): LinkedHashMap<String, String> {
    val match = Regex("""(\d+)\.mp4:hls:manifest\.m3u8""").find(url)
        ?: return linkedMapOf("auto" to url)
    val detected = match.groupValues[1].toIntOrNull() ?: return linkedMapOf("auto" to url)
    val available = ALL_QUALITY_LEVELS.filter { it <= detected }
    return available.associateTo(LinkedHashMap()) { q ->
        "${q}p" to url.replace("$detected.mp4:hls:", "$q.mp4:hls:")
    }
}

/** Адаптация качества работает: включена в настройках и в этой серии качество не выбрано вручную. */
internal fun adaptiveQualityActive(state: PlayerState.State): Boolean =
    state.adaptiveQualityEnabled && state.manualQualityEpisode != activeEpisode(state)

/**
 * Метка, до которой доходит адаптация при потолке [ceilingHeight]: самая высокая не выше потолка.
 * Если все качества выше потолка, берётся лучшее из доступных — так же выбирает старт потока.
 */
internal fun adaptiveLimitLabel(labels: Collection<String>, ceilingHeight: Int): String? {
    val levels = labels.mapNotNull { label -> label.qualityHeight()?.let { label to it } }
    return levels.filter { it.second <= ceilingHeight }.maxByOrNull { it.second }?.first
        ?: levels.maxByOrNull { it.second }?.first
}

/** Сохраняемое качество для метки потока: ближайшая ступень настроек не выше её высоты. */
internal fun preferredQualityForLabel(label: String): PreferredVideoQuality? {
    val height = label.qualityHeight() ?: return null
    return PreferredVideoQuality.entries
        .filter { quality -> (quality.height ?: Int.MAX_VALUE) <= height }
        .maxByOrNull { quality -> quality.height ?: 0 }
        ?: PreferredVideoQuality.P360
}
