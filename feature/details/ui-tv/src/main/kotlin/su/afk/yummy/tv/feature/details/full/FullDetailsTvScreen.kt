package su.afk.yummy.tv.feature.details.full

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.full.view.FullDetailsBody
import su.afk.yummy.tv.feature.details.view.common.DetailsError

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun FullDetailsTvScreenDefaultPreview() = ScreenPreviewTheme {
    FullDetailsTvScreen(FullDetailsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun FullDetailsTvScreenLoadingPreview() = ScreenPreviewTheme {
    FullDetailsTvScreen(FullDetailsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun FullDetailsTvScreenErrorPreview() = ScreenPreviewTheme {
    FullDetailsTvScreen(
        FullDetailsState.State(
            isLoading = false,
            error = "Не удалось загрузить описание"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FullDetailsTvScreen(

    state: FullDetailsState.State,
    effect: Flow<FullDetailsState.Effect>,
    onEvent: (FullDetailsState.Event) -> Unit,

    ) {
    val details = state.details
    val error = state.error
    BackHandler { onEvent(FullDetailsState.Event.BackSelected) }
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && details == null -> TvLoadingScreen()
            error != null && details == null -> DetailsError(
                message = error,
                onRetry = { onEvent(FullDetailsState.Event.RetrySelected) },
            )

            details != null -> FullDetailsBody(
                details = details,
                onGenreSelected = { onEvent(FullDetailsState.Event.GenreSelected(it)) },
                onStudioSelected = { id, url ->
                    onEvent(FullDetailsState.Event.StudioSelected(id, url))
                },
                onDirectorSelected = { onEvent(FullDetailsState.Event.DirectorSelected(it)) },
            )
            else -> DetailsError(
                message = error.orEmpty(),
                onRetry = { onEvent(FullDetailsState.Event.RetrySelected) },
            )
        }
    }
}
