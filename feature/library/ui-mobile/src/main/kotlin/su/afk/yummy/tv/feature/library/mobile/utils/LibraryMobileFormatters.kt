package su.afk.yummy.tv.feature.library.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeSeason
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.utils.episode.EpisodeReleaseCountdown
import su.afk.yummy.tv.core.utils.episode.releaseCountdown
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.mobile.R
import su.afk.yummy.tv.feature.library.model.LibraryTab

@Composable
internal fun LibraryTab.mobileTitle(): String = when (this) {
    LibraryTab.CONTINUE_WATCHING -> stringResource(R.string.library_mobile_tab_continue_watching)
    LibraryTab.HISTORY -> stringResource(R.string.library_mobile_tab_history)
    LibraryTab.FAVORITES -> stringResource(R.string.library_mobile_tab_favorites)
    LibraryTab.WATCHING -> stringResource(R.string.library_mobile_tab_watching)
    LibraryTab.PLANNED -> stringResource(R.string.library_mobile_tab_planned)
    LibraryTab.COMPLETED -> stringResource(R.string.library_mobile_tab_completed)
    LibraryTab.POSTPONED -> stringResource(R.string.library_mobile_tab_postponed)
    LibraryTab.DROPPED -> stringResource(R.string.library_mobile_tab_dropped)
}

@Composable
internal fun LibrarySort.mobileLabel(): String = when (this) {
    LibrarySort.ADDED_DATE -> stringResource(R.string.library_mobile_sort_added_date)
    LibrarySort.YEAR -> stringResource(R.string.library_mobile_sort_year)
    LibrarySort.RATING -> stringResource(R.string.library_mobile_sort_rating)
    LibrarySort.USER_RATING -> stringResource(R.string.library_mobile_sort_user_rating)
    LibrarySort.TITLE -> stringResource(R.string.library_mobile_sort_title)
}

internal fun HomeContinueWatchingItem.watchProgress(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

/** Обратный отсчёт до следующей серии, `null` если даты нет или серия уже вышла. */
@Composable
internal fun LibraryItem.mobileReleaseCountdownText(nowEpochSeconds: Long): String? {
    val countdown = releaseCountdown(nextEpisodeAtSeconds, nowEpochSeconds) ?: return null
    val resource = when (countdown.unit) {
        EpisodeReleaseCountdown.TimeUnit.DAYS -> R.plurals.library_mobile_release_in_days
        EpisodeReleaseCountdown.TimeUnit.HOURS -> R.plurals.library_mobile_release_in_hours
        EpisodeReleaseCountdown.TimeUnit.MINUTES -> R.plurals.library_mobile_release_in_minutes
    }
    return pluralStringResource(resource, countdown.value, countdown.value)
}

/**
 * Год выхода и, если он известен, сезон: «2024 · Зима». Без года бейджа нет вовсе.
 */
@Composable
internal fun LibraryItem.mobileYearSeasonText(): String? {
    val year = year?.takeIf { it > 0 } ?: return null
    val season = season ?: return year.toString()
    return stringResource(
        R.string.library_mobile_year_season,
        year.toString(),
        season.mobileTitle(),
    )
}

@Composable
private fun AnimeSeason.mobileTitle(): String = when (this) {
    AnimeSeason.WINTER -> stringResource(R.string.library_mobile_season_winter)
    AnimeSeason.SPRING -> stringResource(R.string.library_mobile_season_spring)
    AnimeSeason.SUMMER -> stringResource(R.string.library_mobile_season_summer)
    AnimeSeason.FALL -> stringResource(R.string.library_mobile_season_fall)
}
