package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.view.common.RelatedTitleCardWidth
import su.afk.yummy.tv.feature.details.viewingorder.view.ViewingOrderCard

/** «Хронология франшизы»: порядок просмотра лентой с таймлайном, открывается на текущем тайтле. */
@Composable
internal fun DetailsUniverseRow(
    items: List<AnimeViewingOrderItem>,
    currentAnimeId: Int,
    onAnimeClick: (Int) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemFocusModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    if (items.size < 2) return
    val currentIndex = remember(items, currentAnimeId) {
        items.indexOfFirst { it.animeId == currentAnimeId }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    val showAllFocus = rememberHeaderActionFocus()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailsSectionHeader(
            title = stringResource(R.string.details_mobile_title_universe),
            modifier = Modifier.padding(horizontal = DetailsSectionHorizontalPadding),
            actionLabel = stringResource(R.string.details_mobile_universe_show_all),
            onActionClick = onShowAllClick,
            actionFocus = showAllFocus,
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = DetailsSectionHorizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> lazyKey("details_universe", "${item.animeId}:$index", index) },
            ) { index, item ->
                Column(modifier = Modifier.width(RelatedTitleCardWidth)) {
                    UniverseTimelineNode(
                        index = index,
                        currentIndex = currentIndex,
                        count = items.size,
                        year = item.year,
                        typeKind = item.typeKind,
                        gap = ItemSpacing,
                    )
                    ViewingOrderCard(
                        index = index + 1,
                        item = item,
                        onClick = { onAnimeClick(item.animeId) },
                        modifier = itemFocusModifier(index)
                            .then(showAllFocus.opensOn(edgeKeys(index, items.lastIndex))),
                        selected = item.animeId == currentAnimeId,
                        showIndexAndYear = false,
                    )
                }
            }
        }
    }
}

private val ItemSpacing = 12.dp
