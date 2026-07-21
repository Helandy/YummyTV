package su.afk.yummy.tv.feature.posts.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.feature.posts.tv.R

@Composable
internal fun PostSort.label() = when (this) {
    PostSort.NEW -> stringResource(R.string.posts_new)
    PostSort.OLD -> stringResource(R.string.posts_old)
    PostSort.BEST -> stringResource(R.string.posts_best)
}
