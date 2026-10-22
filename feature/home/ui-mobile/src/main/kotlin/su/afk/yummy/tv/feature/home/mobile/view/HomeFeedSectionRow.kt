package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.CollectionStatBadges
import su.afk.yummy.tv.core.designsystem.components.lazy.KeepLazyListAtStartOnNewHead
import su.afk.yummy.tv.core.designsystem.dimensions.MobilePosterGridHorizontalPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterCellWidth
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterSpacing
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileCollectionCard
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.mobile.utils.newEpisodeBadge
import su.afk.yummy.tv.feature.home.mobile.utils.newEpisodeSubtitle
import su.afk.yummy.tv.feature.home.mobile.utils.showMobileCardMetadata

@Composable
internal fun HomeFeedSectionRow(
    section: HomeFeedSection,
    onItemSelected: (HomeFeedItem) -> Unit,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onItemLongClick: ((HomeFeedItem) -> Unit)? = null,
) {
    val showCardMetadata = section.type.showMobileCardMetadata()
    val listState = rememberLazyListState()
    KeepLazyListAtStartOnNewHead(state = listState, headKey = section.items.firstOrNull()?.id)

    // Главная не зависит от настройки размера карточек: ряды всегда считаются по стандартному размеру.
    CompositionLocalProvider(LocalPosterCardSize provides PosterCardSize.STANDARD) {
        BoxWithConstraints {
            val cardWidth = currentMobilePosterCellWidth(maxWidth)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MobileSectionHeader(
                    title = section.title,
                    modifier = Modifier.padding(horizontal = MobilePosterGridHorizontalPadding),
                    trailingActionLabel = actionLabel,
                    onTrailingActionClick = onActionClick,
                )
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = MobilePosterGridHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(currentMobilePosterSpacing()),
                ) {
                    items(section.items, key = { lazyKey("homeitem", it.id) }) { item ->
                        Box(modifier = Modifier.testTag("anime_card")) {
                            val collection = item.collection
                            if (collection != null) {
                                MobileCollectionCard(
                                    title = item.title,
                                    posterUrls = collection.posterUrls,
                                    onClick = { onItemSelected(item) },
                                    badge = {
                                        CollectionStatBadges(
                                            likes = collection.likes,
                                            views = collection.views,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp),
                                        )
                                    },
                                    modifier = Modifier.width(cardWidth * 2),
                                )
                            } else {
                                HomeItemCard(
                                    item = item,
                                    showMetadata = showCardMetadata,
                                    subtitle = item.newEpisodeSubtitle(),
                                    badge = item.newEpisodeBadge(),
                                    showYear = section.type == HomeFeedSectionType.RECOMMENDATIONS,
                                    cardWidth = cardWidth,
                                    onClick = { onItemSelected(item) },
                                    onLongClick = onItemLongClick?.let { { it(item) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
