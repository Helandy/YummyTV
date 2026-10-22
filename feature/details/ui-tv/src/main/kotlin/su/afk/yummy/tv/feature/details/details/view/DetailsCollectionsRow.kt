package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.CollectionStatBadges
import su.afk.yummy.tv.core.designsystem.tv.TvCollectionCard
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.feature.details.presentation.R

/** Коллекции с тайтлом; данные уже загружены вместе с бэйджем кнопки. */
@Composable
internal fun DetailsCollectionsRow(
    collections: List<AnimeCollectionSummary>,
    onCollectionClick: (Int) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemFocusModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    if (collections.isEmpty()) return
    val showAllFocus = rememberHeaderActionFocus()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailsSectionHeader(
            title = stringResource(R.string.details_collections_button),
            modifier = Modifier.padding(horizontal = DetailsSectionHorizontalPadding),
            actionLabel = stringResource(R.string.details_mobile_universe_show_all),
            onActionClick = onShowAllClick,
            actionFocus = showAllFocus,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = DetailsSectionHorizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(collections, key = { _, it -> lazyKey("details_collection", it.id) }) { index, item ->
                TvCollectionCard(
                    title = item.title,
                    posterUrls = item.posterUrls.ifEmpty { listOfNotNull(item.posterUrl) },
                    onClick = { onCollectionClick(item.id) },
                    badge = {
                        CollectionStatBadges(
                            likes = item.likes,
                            views = item.views,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        )
                    },
                    modifier = Modifier
                        .width(320.dp)
                        .then(itemFocusModifier(index))
                        .then(showAllFocus.opensOn(edgeKeys(index, collections.lastIndex))),
                )
            }
        }
    }
}
