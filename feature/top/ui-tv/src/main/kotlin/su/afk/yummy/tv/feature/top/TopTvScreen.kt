package su.afk.yummy.tv.feature.top

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.utils.LocalTopTvActiveDestination
import su.afk.yummy.tv.feature.top.view.TopBrowser

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun TopTvScreenDefaultPreview() = ScreenPreviewTheme {
    TopTvScreen(TopState.State(), emptyFlow()) {}
}

@Composable
fun TopTvScreen(
    state: TopState.State,
    effect: Flow<TopState.Effect>,
    onEvent: (TopState.Event) -> Unit,
) {
    val isActiveDestination = LocalTopTvActiveDestination.current
    val items = state.items.collectAsLazyPagingItems()
    val onItemSelected =
        remember(onEvent) { { item: AnimeTopItem -> onEvent(TopState.Event.AnimeSelected(item.id)) } }
    val onTypeSelected =
        remember(onEvent) { { type: AnimeTopType -> onEvent(TopState.Event.TypeSelected(type)) } }

    TopBrowser(
        pagingItems = items,
        selectedType = state.selectedType,
        isActiveDestination = isActiveDestination,
        showTitleYear = state.showTitleYear,
        onItemSelected = onItemSelected,
        onTypeSelected = onTypeSelected,
        onRetry = {
            onEvent(TopState.Event.RetrySelected)
            items.retry()
        },
    )
}
