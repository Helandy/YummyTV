package su.afk.yummy.tv.feature.top100.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.top100.model.AnimeTopType
import su.afk.yummy.tv.feature.top100.mobile.R

@Composable
internal fun AnimeTopType.label(): String = when (this) {
    AnimeTopType.TV -> stringResource(R.string.top100_type_tv)
    AnimeTopType.MOVIE -> stringResource(R.string.top100_type_movie)
    AnimeTopType.ONA -> stringResource(R.string.top100_type_ona)
}
