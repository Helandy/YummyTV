package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.reviews.model.ReviewStatus
import su.afk.yummy.tv.feature.reviews.mobile.R

@Composable
internal fun ReviewStatus.reviewStatusLabel(): String = when (this) {
    ReviewStatus.APPROVED -> stringResource(R.string.review_approved)
    ReviewStatus.WAITING -> stringResource(R.string.review_waiting)
    ReviewStatus.DECLINED -> stringResource(R.string.review_declined)
}

@Composable
internal fun ReviewStatus.reviewStatusColor(): Color = when (this) {
    ReviewStatus.APPROVED -> MaterialTheme.colorScheme.primary
    ReviewStatus.WAITING -> MaterialTheme.colorScheme.tertiary
    ReviewStatus.DECLINED -> MaterialTheme.colorScheme.error
}
