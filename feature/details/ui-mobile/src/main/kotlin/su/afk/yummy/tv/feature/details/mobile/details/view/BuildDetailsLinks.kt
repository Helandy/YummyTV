package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.details.mobile.details.model.DetailsLink
import su.afk.yummy.tv.feature.details.presentation.R

/** Рецензии показываются только когда они есть ([reviewsCount] > 0). */
@Composable
internal fun buildDetailsLinks(
    reviewsCount: Int,
    reviewsBadge: String?,
    onReviewsSelected: () -> Unit,
): List<DetailsLink> = listOfNotNull(
    DetailsLink(
        label = stringResource(R.string.details_reviews),
        icon = Icons.Filled.RateReview,
        testTag = "details_link_reviews",
        onClick = onReviewsSelected,
        badge = reviewsBadge,
        underlined = true,
    ).takeIf { reviewsCount > 0 },
)

@Composable
internal fun commentsDetailsLink(commentsBadge: String?, onCommentsSelected: () -> Unit) = DetailsLink(
    label = stringResource(R.string.details_mobile_show_comments),
    icon = Icons.Filled.ChatBubbleOutline,
    testTag = "details_link_comments",
    onClick = onCommentsSelected,
    badge = commentsBadge,
)

@Composable
internal fun similarDetailsLink(onSimilarSelected: () -> Unit) = DetailsLink(
    label = stringResource(R.string.details_mobile_recommended_title),
    icon = Icons.Filled.AutoAwesome,
    testTag = "details_link_similar",
    onClick = onSimilarSelected,
    underlined = true,
)
