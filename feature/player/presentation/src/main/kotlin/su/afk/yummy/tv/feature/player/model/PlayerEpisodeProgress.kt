package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Immutable

/** Просмотр серии в списке серий плеера. */
@Immutable
sealed interface PlayerEpisodeProgress {
    data object None : PlayerEpisodeProgress

    /** [label] — «11:24 / 24:00». */
    data class InProgress(val label: String, val progress: Float) : PlayerEpisodeProgress

    /** [label] — полная длительность серии. */
    data class Watched(val label: String) : PlayerEpisodeProgress
}
