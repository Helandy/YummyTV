package su.afk.yummy.tv.feature.top.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.R

@Composable
internal fun AnimeTopType.label(): String = when (this) {
    AnimeTopType.TV -> stringResource(R.string.top_type_tv)
    AnimeTopType.MOVIE -> stringResource(R.string.top_type_movie)
    AnimeTopType.ONA -> name
}

@Composable
internal fun AnimeTopType.shortLabel(): String = when (this) {
    AnimeTopType.TV -> stringResource(R.string.top_type_tv_short)
    AnimeTopType.MOVIE -> stringResource(R.string.top_type_movie_short)
    AnimeTopType.ONA -> name
}
