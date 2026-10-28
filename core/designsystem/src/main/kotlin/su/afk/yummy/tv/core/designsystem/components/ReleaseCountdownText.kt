package su.afk.yummy.tv.core.designsystem.components

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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

/**
 * «пт, 25 окт., 18:30» — когда выйдет серия, в локали и 12/24-часовом формате устройства;
 * `null`, если даты нет или серия уже вышла.
 */
@Composable
fun releaseDateText(nextEpisodeAtSeconds: Long?, nowEpochSeconds: Long): String? {
    val at = nextEpisodeAtSeconds?.takeIf { it > nowEpochSeconds } ?: return null
    val context = LocalContext.current
    return DateUtils.formatDateTime(
        context,
        at * MILLIS_PER_SECOND,
        DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
