package su.afk.yummy.tv.feature.details.mobile.collections

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.collections.CollectionsState
import su.afk.yummy.tv.feature.details.mobile.R

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CollectionsMobileScreenDefaultPreview() = ScreenPreviewTheme {
    CollectionsMobileScreen(CollectionsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun CollectionsMobileScreenLoadingPreview() = ScreenPreviewTheme {
    CollectionsMobileScreen(CollectionsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun CollectionsMobileScreenErrorPreview() = ScreenPreviewTheme {
    CollectionsMobileScreen(
        CollectionsState.State(
            isLoading = false,
            error = "Не удалось загрузить коллекции"
        ), emptyFlow()
    ) {}
}

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
            MobilePosterGrid(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                items(state.collections, key = { it.id }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.poster?.mega ?: item.poster?.fullsize ?: item.posterUrl,
                        onClick = { onEvent(CollectionsState.Event.CollectionSelected(item.id)) },
                    )
                }
            }
        }
    }
}
