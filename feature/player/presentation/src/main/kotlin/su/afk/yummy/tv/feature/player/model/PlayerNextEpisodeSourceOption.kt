package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.feature.player.PlayerSourceSelection

/**
 * Озвучка в списке диалога «серии нет» — строка как в меню озвучек плеера. [selection] ведёт на
 * серию N+1 этой озвучки: на текущем балансере, если она там есть, иначе на первом из [sourceNames].
 */
@Immutable
data class PlayerNextEpisodeSourceOption(
    val selection: PlayerSourceSelection,
    val dubbing: String,
    val views: Int,
    val episodeCount: Int,
    /** Балансеры, где у озвучки есть серия N+1, через « • ». */
    val sourceNames: String,
)
