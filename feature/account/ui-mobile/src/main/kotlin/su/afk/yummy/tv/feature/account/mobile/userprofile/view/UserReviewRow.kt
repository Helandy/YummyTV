package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun UserReviewRow(item: UserReviewSummary, onClick: () -> Unit) {
    UserProfileMediaRow(
        imageUrl = item.animePosterUrl,
        title = item.animeTitle.ifBlank { stringResource(R.string.user_profile_review) },
        subtitle = item.textPreview,
        onClick = onClick.takeIf { item.id > 0 },
    )
}
