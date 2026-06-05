package su.afk.yummy.tv.feature.details.collections

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.mobile.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionsMobileScreen(
    state: CollectionsState.State,
    effect: Flow<CollectionsState.Effect>,
    onEvent: (CollectionsState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_collections),
                onBack = { onEvent(CollectionsState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(CollectionsState.Event.RetrySelected) },
            empty = state.collections.isEmpty() && !state.isLoading,
        ) {
            MobilePosterGrid(contentPadding = PaddingValues(0.dp)) {
                items(state.collections, key = { it.id }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.poster?.mega ?: item.poster?.fullsize ?: item.posterUrl,
                        subtitle = item.views?.let { stringResource(R.string.details_mobile_views, it) },
                        onClick = { onEvent(CollectionsState.Event.CollectionSelected(item.id)) },
                    )
                }
            }
        }
    }
}
