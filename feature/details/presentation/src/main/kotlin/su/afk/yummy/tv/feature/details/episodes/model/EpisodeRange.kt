package su.afk.yummy.tv.feature.details.episodes.model

import androidx.compose.runtime.Immutable

/**
 * Диапазон серий для быстрого перехода по длинному списку.
 *
 * [startIndex] и [endIndex] задают включительные границы в текущем списке видимых серий,
 * а [firstEpisode] и [lastEpisode] — отображаемые номера граничных серий.
 */
@Immutable
data class EpisodeRange(
    val startIndex: Int,
    val endIndex: Int,
    val firstEpisode: String,
    val lastEpisode: String,
)
