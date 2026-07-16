package su.afk.yummy.tv.feature.details.trailers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.trailers.view.TrailerTab

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun TrailersTvScreenDefaultPreview() = ScreenPreviewTheme {
    TrailersTvScreen(TrailersState.State(isLoading = false), emptyFlow()) {}
}

@Composable
fun TrailersTvScreen(
    state: TrailersState.State,
    effect: Flow<TrailersState.Effect>,
    onEvent: (TrailersState.Event) -> Unit,
) {
    BackHandler { onEvent(TrailersState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> TvLoadingScreen()

            state.error != null -> TvStateMessage(
                title = state.error.orEmpty(),
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(TrailersState.Event.RetrySelected) },
            )

            state.trailers.isEmpty() -> TvStateMessage(
                title = stringResource(R.string.details_trailer_empty),
                icon = Icons.Outlined.Movie,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
            ) {
                item {
                    TrailerTab(
                        trailers = state.trailers,
                    )
                }
            }
        }
    }
}
