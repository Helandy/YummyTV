package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.CollectionStatBadges
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobileCollectionCard
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.feature.details.presentation.R

/** Лента коллекций с тайтлом прямо в деталях: данные уже загружены вместе с бэйджем, лишнего запроса нет. */
@Composable
internal fun DetailsCollectionsSection(
    collections: List<AnimeCollectionSummary>,
    onCollectionClick: (Int) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (collections.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("details_collections_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MobileSectionHeader(
            title = stringResource(R.string.details_collections_button),
            modifier = Modifier.padding(horizontal = 16.dp),
            trailingActionLabel = stringResource(R.string.details_mobile_universe_show_all),
            onTrailingActionClick = onShowAllClick,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(collections, key = { lazyKey("details_collection", it.id) }) { item ->
                MobileCollectionCard(
                    title = item.title,
                    posterUrls = item.posterUrls.ifEmpty { listOfNotNull(item.posterUrl) },
                    badge = {
                        CollectionStatBadges(
                            likes = item.likes,
                            views = item.views,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        )
                    },
                    onClick = { onCollectionClick(item.id) },
                    modifier = Modifier.width(260.dp),
                )
            }
        }
    }
}
