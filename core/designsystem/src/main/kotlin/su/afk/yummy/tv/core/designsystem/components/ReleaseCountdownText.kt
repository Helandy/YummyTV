package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import su.afk.yummy.tv.core.common.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.core.common.episode.releaseCountdown
import su.afk.yummy.tv.core.designsystem.R

/** «До выхода серии 2 дня»; `null`, если даты нет или серия уже вышла. */
@Composable
fun releaseCountdownText(nextEpisodeAtSeconds: Long?, nowEpochSeconds: Long): String? {
    val countdown = releaseCountdown(nextEpisodeAtSeconds, nowEpochSeconds) ?: return null
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.core_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.core_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.core_release_in_minutes
    }
    return pluralStringResource(resource, countdown.value, countdown.value)
}
