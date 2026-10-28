package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.feature.library.presentation.R

/** Порядок вариантов сортировки в ТВ-оверлее. */
internal val LibraryTvSortOrder = listOf(
    LibrarySort.NEXT_EPISODE,
    LibrarySort.ADDED_DATE,
    LibrarySort.YEAR,
    LibrarySort.RATING,
    LibrarySort.USER_RATING,
    LibrarySort.TITLE,
)

@Composable
internal fun LibrarySort.tvLabel(): String = when (this) {
    LibrarySort.ADDED_DATE -> stringResource(R.string.library_mobile_sort_added_date)
    LibrarySort.YEAR -> stringResource(R.string.library_mobile_sort_year)
    LibrarySort.RATING -> stringResource(R.string.library_mobile_sort_rating)
    LibrarySort.USER_RATING -> stringResource(R.string.library_mobile_sort_user_rating)
    LibrarySort.TITLE -> stringResource(R.string.library_mobile_sort_title)
    LibrarySort.NEXT_EPISODE -> stringResource(R.string.library_mobile_sort_next_episode)
}
