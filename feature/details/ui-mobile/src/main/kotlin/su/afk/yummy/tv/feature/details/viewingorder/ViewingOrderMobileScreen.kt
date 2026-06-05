package su.afk.yummy.tv.feature.details.viewingorder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.viewingorder.view.ViewingOrderMobileCard

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ViewingOrderMobileScreen(

    state: ViewingOrderState.State,
    effect: Flow<ViewingOrderState.Effect>,
    onEvent: (ViewingOrderState.Event) -> Unit,

) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_viewing_order),
                onBack = { onEvent(ViewingOrderState.Event.BackSelected) },
            )
        },
        contentModifier = Modifier.padding(bottom = 24.dp)
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            empty = state.items.isEmpty() && !state.isLoading,
        ) {
            MobilePosterGrid(contentPadding = PaddingValues(0.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.details_mobile_title_universe),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                itemsIndexed(state.items, key = { _, item -> item.animeId }) { index, item ->
                    ViewingOrderMobileCard(
                        index = index + 1,
                        item = item,
                        selected = item.animeId == state.currentAnimeId,
                        onClick = { onEvent(ViewingOrderState.Event.AnimeSelected(item.animeId)) },
                    )
                }
            }
        }
    }
}
