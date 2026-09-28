package su.afk.yummy.tv.feature.details.mobile.full.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeEpisodes
import su.afk.yummy.tv.core.utils.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.details.utils.rememberReleaseCountdown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val epochSecondsFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

internal fun Long.formatEpochSeconds(): String = epochSecondsFormatter.format(Date(this * 1000))

/** «5 из 12»; `null`, пока не вышло ни одной серии. */
@Composable
internal fun AnimeEpisodes.formatAiredCount(): String? {
    val airedCount = aired ?: return null
    val totalCount = count?.toString() ?: stringResource(R.string.details_mobile_unknown_count)
    return stringResource(R.string.details_mobile_aired_progress, airedCount, totalCount)
}

/** «02.10.2026 03:00 · через 3 дня»; без отсчёта, если дата уже прошла. */
@Composable
internal fun AnimeEpisodes.formatNextEpisode(): String? {
    val date = nextDateEpochSeconds?.formatEpochSeconds() ?: return null
    val countdown = rememberReleaseCountdown() ?: return date
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.details_mobile_full_next_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.details_mobile_full_next_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.details_mobile_full_next_in_minutes
    }
    return stringResource(
        R.string.details_mobile_full_next_episode_with_countdown,
        date,
        pluralStringResource(resource, countdown.value, countdown.value),
    )
}
