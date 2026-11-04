package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.model.anime.EpisodeFlags

/** Серия активной озвучки для списка выбора серии в плеере. */
@Immutable
data class PlayerEpisodeItem(
    val index: Int,
    val number: String,
    val screenshotUrl: String,
    val progress: PlayerEpisodeProgress = PlayerEpisodeProgress.None,
    /** Длительность серии «24:00», если она известна из списка серий; показывается, пока серия не начата. */
    val durationLabel: String? = null,
    /** Филлер/рекап из MyAnimeList; `null` — пометки нет или выключена. */
    val flags: EpisodeFlags? = null,
)
