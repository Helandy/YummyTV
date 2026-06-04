package su.afk.yummy.tv.feature.search.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.search.model.SearchSort
import su.afk.yummy.tv.feature.search.R
import su.afk.yummy.tv.feature.search.model.IntOption
import su.afk.yummy.tv.feature.search.model.StringOption

@Composable
internal fun statusOptions(): List<StringOption> = listOf(
    StringOption("released", stringResource(R.string.search_filter_status_released)),
    StringOption("ongoing", stringResource(R.string.search_filter_status_ongoing)),
    StringOption("announcement", stringResource(R.string.search_filter_status_announcement)),
)

@Composable
internal fun seasonOptions(): List<StringOption> = listOf(
    StringOption("winter", stringResource(R.string.search_filter_season_winter)),
    StringOption("spring", stringResource(R.string.search_filter_season_spring)),
    StringOption("summer", stringResource(R.string.search_filter_season_summer)),
    StringOption("fall", stringResource(R.string.search_filter_season_fall)),
)

@Composable
internal fun ageOptions(): List<IntOption> = listOf(
    IntOption(1, stringResource(R.string.search_filter_age_pg)),
    IntOption(2, stringResource(R.string.search_filter_age_pg13)),
    IntOption(3, stringResource(R.string.search_filter_age_r17)),
    IntOption(4, stringResource(R.string.search_filter_age_r)),
    IntOption(5, stringResource(R.string.search_filter_age_rx)),
)

@Composable
internal fun SearchSort.label(): String = when (this) {
    SearchSort.RELEVANCE -> stringResource(R.string.search_filter_sort_relevance)
    SearchSort.TITLE -> stringResource(R.string.search_filter_sort_title)
    SearchSort.YEAR -> stringResource(R.string.search_filter_sort_year)
    SearchSort.RATING -> stringResource(R.string.search_filter_sort_rating)
    SearchSort.RATING_COUNTERS -> stringResource(R.string.search_filter_sort_rating_counters)
    SearchSort.VIEWS -> stringResource(R.string.search_filter_sort_views)
    SearchSort.TOP -> stringResource(R.string.search_filter_sort_top)
    SearchSort.RANDOM -> stringResource(R.string.search_filter_sort_random)
    SearchSort.ID -> stringResource(R.string.search_filter_sort_id)
}
