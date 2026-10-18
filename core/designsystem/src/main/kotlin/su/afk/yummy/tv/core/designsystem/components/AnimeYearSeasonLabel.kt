package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.model.anime.AnimeSeason

/** «2024 · Весна», «2024» без сезона; null, если год неизвестен. */
@Composable
fun animeYearSeasonLabel(year: Int?, season: AnimeSeason?): String? {
    val knownYear = year?.takeIf { it > 0 } ?: return null
    val seasonTitle = season?.let {
        stringResource(
            when (it) {
                AnimeSeason.WINTER -> R.string.core_season_winter
                AnimeSeason.SPRING -> R.string.core_season_spring
                AnimeSeason.SUMMER -> R.string.core_season_summer
                AnimeSeason.FALL -> R.string.core_season_fall
            },
        )
    } ?: return knownYear.toString()
    return stringResource(R.string.core_year_season, knownYear.toString(), seasonTitle)
}
