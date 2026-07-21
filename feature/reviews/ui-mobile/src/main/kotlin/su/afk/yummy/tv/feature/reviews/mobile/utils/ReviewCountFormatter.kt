package su.afk.yummy.tv.feature.reviews.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.feature.reviews.mobile.R

@Composable
internal fun Int.displayCompactReviewCount(): String {
    val compact = toCompactCount()
    return when {
        compact.endsWith("K") -> stringResource(
            R.string.review_count_thousands,
            compact.dropLast(1),
        )

        compact.endsWith("M") -> stringResource(
            R.string.review_count_millions,
            compact.dropLast(1),
        )

        else -> compact
    }
}
