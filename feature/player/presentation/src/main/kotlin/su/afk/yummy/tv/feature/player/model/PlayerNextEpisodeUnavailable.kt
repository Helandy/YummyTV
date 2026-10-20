package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * В активной озвучке на этом балансере серии N+1 нет. [options] — озвучки, где она есть, лучшие
 * первыми; [inCurrentDubbing] — среди них сама [currentDubbing] (на другом балансере). Пусто — серии
 * нет нигде, и при [canSubscribe] вместо простого «Понятно» предлагаем подписку на новые серии.
 */
@Immutable
data class PlayerNextEpisodeUnavailable(
    val episodeNumber: Int?,
    val currentDubbing: String,
    val inCurrentDubbing: Boolean,
    val options: ImmutableList<PlayerNextEpisodeSourceOption>,
    val canSubscribe: Boolean,
)
