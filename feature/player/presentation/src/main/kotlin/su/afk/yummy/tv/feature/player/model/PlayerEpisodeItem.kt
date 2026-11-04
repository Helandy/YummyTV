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
    /** Филлер/рекап из MyAnimeList; `null` — пометки нет или выключена. */
    val flags: EpisodeFlags? = null,
)
