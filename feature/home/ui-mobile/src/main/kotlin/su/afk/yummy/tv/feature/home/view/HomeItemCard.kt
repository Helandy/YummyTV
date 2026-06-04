package su.afk.yummy.tv.feature.home.view

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileContentPosterCard
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.feature.home.utils.bestUrl

@Composable
internal fun HomeItemCard(
    item: HomeFeedItem,
    showMetadata: Boolean,
    onClick: () -> Unit,
) {
    MobileContentPosterCard(
        title = item.title,
        posterUrl = item.poster.bestUrl(),
        description = item.description,
        rating = item.rating,
        showMetadata = showMetadata,
        onClick = onClick,
    )
}
