package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeSeason
import su.afk.yummy.tv.core.utils.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.core.utils.episode.releaseCountdown
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.presentation.R

@Composable
internal fun LibraryItem.tvReleaseCountdownText(nowEpochSeconds: Long): String? {
    val countdown = releaseCountdown(nextEpisodeAtSeconds, nowEpochSeconds) ?: return null
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.library_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.library_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.library_release_in_minutes
    }
    return pluralStringResource(resource, countdown.value, countdown.value)
}

/**
 * Год выхода и, если он известен, сезон: «2024 · Зима». Без года бейджа нет вовсе.
 */
@Composable
internal fun LibraryItem.tvYearSeasonText(): String? {
    val year = year?.takeIf { it > 0 } ?: return null
    val season = season ?: return year.toString()
    return stringResource(R.string.library_year_season, year.toString(), season.tvTitle())
}

@Composable
private fun AnimeSeason.tvTitle(): String = when (this) {
    AnimeSeason.WINTER -> stringResource(R.string.library_season_winter)
    AnimeSeason.SPRING -> stringResource(R.string.library_season_spring)
    AnimeSeason.SUMMER -> stringResource(R.string.library_season_summer)
    AnimeSeason.FALL -> stringResource(R.string.library_season_fall)
}
