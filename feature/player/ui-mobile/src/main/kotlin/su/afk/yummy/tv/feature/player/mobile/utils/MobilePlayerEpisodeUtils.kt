package su.afk.yummy.tv.feature.player.mobile.utils

import su.afk.yummy.tv.feature.player.model.PlayerEpisodeItem
import su.afk.yummy.tv.feature.player.model.PlayerEpisodeProgress

/** Подпись тайминга серии: длительность, «позиция / длительность» или время просмотра. */
internal fun PlayerEpisodeItem.timingLabel(): String? = when (val progress = progress) {
    PlayerEpisodeProgress.None -> durationLabel
    is PlayerEpisodeProgress.InProgress -> progress.label
    is PlayerEpisodeProgress.Watched -> progress.label
}
