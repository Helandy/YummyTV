package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.MobilePosterGridHorizontalPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterCellWidth
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterSpacing
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterCardSize
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.model.anime.AnimeViewingOrderItem
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.details.mobile.viewingorder.view.ViewingOrderMobileCard
import su.afk.yummy.tv.feature.details.presentation.R

/** «Хронология франшизы»: все тайтлы франшизы лентой с таймлайном (год, нода, «ВЫ ЗДЕСЬ»), лента открывается на текущем, «Показать все» ведёт на отдельный экран. */
@Composable
internal fun DetailsUniverseSection(
    items: List<AnimeViewingOrderItem>,
    currentAnimeId: Int,
    onAnimeClick: (Int) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.size < 2) return

    val currentIndex = items.indexOfFirst { it.animeId == currentAnimeId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    // Как ряды на главной: не зависит от настройки размера карточек, ширина равна ячейке сетки.
    CompositionLocalProvider(LocalPosterCardSize provides PosterCardSize.STANDARD) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val cardWidth = currentMobilePosterCellWidth(maxWidth)
            val spacing = currentMobilePosterSpacing()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_universe_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MobileSectionHeader(
                    title = stringResource(R.string.details_mobile_title_universe),
                    modifier = Modifier.padding(horizontal = MobilePosterGridHorizontalPadding),
                    trailingActionLabel = stringResource(R.string.details_mobile_universe_show_all),
                    onTrailingActionClick = onShowAllClick,
                )
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = MobilePosterGridHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, item -> lazyKey("details_universe", "${item.animeId}:$index", index) },
                    ) { index, item ->
                        Column(modifier = Modifier.width(cardWidth)) {
                            UniverseTimelineNode(
                                index = index,
                                currentIndex = currentIndex,
                                count = items.size,
                                year = item.year,
                                typeKind = item.typeKind,
                                gap = spacing,
                            )
                            ViewingOrderMobileCard(
                                index = index + 1,
                                item = item,
                                selected = item.animeId == currentAnimeId,
                                onClick = { onAnimeClick(item.animeId) },
                                showIndexAndYear = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
