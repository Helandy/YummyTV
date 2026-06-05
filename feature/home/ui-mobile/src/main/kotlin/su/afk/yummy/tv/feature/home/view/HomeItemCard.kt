package su.afk.yummy.tv.feature.home.view

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.feature.home.utils.bestUrl

@Composable
internal fun HomeItemCard(
    item: HomeFeedItem,
    showMetadata: Boolean,
    onClick: () -> Unit,
) {
    MobilePosterCard(
        title = item.title,
        posterUrl = item.poster.bestUrl(),
        subtitle = item.description.takeIf { showMetadata && it.isNotBlank() },
        rating = item.rating.takeIf { showMetadata },
        titleMinLines = if (showMetadata) 1 else 2,
        onClick = onClick,
    )
}
