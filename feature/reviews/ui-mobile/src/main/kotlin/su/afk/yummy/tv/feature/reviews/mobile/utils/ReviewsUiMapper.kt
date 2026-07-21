package su.afk.yummy.tv.feature.reviews.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.feature.reviews.mobile.R

@Composable
internal fun ReviewSort.label() = when (this) {
    ReviewSort.NEW -> stringResource(R.string.reviews_new)
    ReviewSort.OLD -> stringResource(R.string.reviews_old)
    ReviewSort.TOP -> stringResource(R.string.reviews_top)
}
