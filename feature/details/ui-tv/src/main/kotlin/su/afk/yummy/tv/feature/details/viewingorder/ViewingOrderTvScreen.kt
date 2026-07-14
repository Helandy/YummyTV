package su.afk.yummy.tv.feature.details.viewingorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.viewingorder.view.ViewingOrderRow

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun ViewingOrderTvScreenDefaultPreview() = ScreenPreviewTheme {
    ViewingOrderTvScreen(ViewingOrderState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun ViewingOrderTvScreenLoadingPreview() = ScreenPreviewTheme {
    ViewingOrderTvScreen(ViewingOrderState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun ViewingOrderTvScreenErrorPreview() = ScreenPreviewTheme {
    ViewingOrderTvScreen(
        ViewingOrderState.State(
            isLoading = false,
            error = "Не удалось загрузить порядок просмотра"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ViewingOrderTvScreen(
    state: ViewingOrderState.State,
    effect: Flow<ViewingOrderState.Effect>,
    onEvent: (ViewingOrderState.Event) -> Unit,
) {
    BackHandler { onEvent(ViewingOrderState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )

            state.items.isEmpty() -> Text(
                text = stringResource(R.string.details_viewing_order_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
            ) {
                item {
                    ViewingOrderRow(
                        items = state.items,
                        currentAnimeId = state.currentAnimeId,
                        onAnimeSelected = { id -> onEvent(ViewingOrderState.Event.AnimeSelected(id)) },
                    )
                }
            }
        }
    }
}
