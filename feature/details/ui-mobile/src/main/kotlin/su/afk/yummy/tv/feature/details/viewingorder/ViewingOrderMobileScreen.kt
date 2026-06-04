package su.afk.yummy.tv.feature.details.viewingorder

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.view.DetailsMobileScaffold
import su.afk.yummy.tv.feature.details.viewingorder.utils.bestUrl

@Composable
fun ViewingOrderMobileScreen(

    state: ViewingOrderState.State,
    effect: Flow<ViewingOrderState.Effect>,
    onEvent: (ViewingOrderState.Event) -> Unit,

) {
    DetailsMobileScaffold(
        title = stringResource(R.string.details_mobile_viewing_order),
        onBack = { onEvent(ViewingOrderState.Event.BackSelected) },
    ) { padding ->
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            empty = state.items.isEmpty() && !state.isLoading,
        ) {
            MobilePosterGrid(contentPadding = padding) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.details_mobile_title_universe)) }
                items(state.items, key = { it.animeId }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.poster.bestUrl(),
                        subtitle = listOfNotNull(item.relation, item.year?.toString()).joinToString(" • "),
                        onClick = { onEvent(ViewingOrderState.Event.AnimeSelected(item.animeId)) },
                    )
                }
            }
        }
    }
}
