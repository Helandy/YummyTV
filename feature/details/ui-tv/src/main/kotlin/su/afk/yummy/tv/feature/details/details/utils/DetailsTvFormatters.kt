package su.afk.yummy.tv.feature.details.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun UserAnimeList.label(): String = stringResource(
    when (this) {
        UserAnimeList.WATCHING -> R.string.details_library_list_watching
        UserAnimeList.PLANNED -> R.string.details_library_list_planned
        UserAnimeList.COMPLETED -> R.string.details_library_list_completed
        UserAnimeList.POSTPONED -> R.string.details_library_list_postponed
        UserAnimeList.DROPPED -> R.string.details_library_list_dropped
    }
)
