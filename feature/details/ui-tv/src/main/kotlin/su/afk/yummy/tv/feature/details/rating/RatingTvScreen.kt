package su.afk.yummy.tv.feature.details.rating

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.rating.view.RatingBody
import su.afk.yummy.tv.feature.details.view.common.DetailsError

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun RatingTvScreenDefaultPreview() = ScreenPreviewTheme {
    RatingTvScreen(RatingState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun RatingTvScreenLoadingPreview() = ScreenPreviewTheme {
    RatingTvScreen(RatingState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun RatingTvScreenErrorPreview() = ScreenPreviewTheme {
    RatingTvScreen(
        RatingState.State(isLoading = false, error = "Не удалось загрузить рейтинг"),
        emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RatingTvScreen(
    state: RatingState.State,
    effect: Flow<RatingState.Effect>,
    onEvent: (RatingState.Event) -> Unit,
) {
    val error = state.error
    val context = LocalContext.current
    BackHandler { onEvent(RatingState.Event.BackSelected) }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is RatingState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && error == null -> TvLoadingScreen()
            error != null -> DetailsError(
                message = error,
                onRetry = { onEvent(RatingState.Event.RetrySelected) },
            )

            else -> RatingBody(
                ratingSummary = state.ratingSummary,
                listStats = state.listStats,
                selectedUserRating = state.selectedUserRating,
                onRatingSelected = { rating -> onEvent(RatingState.Event.RatingSelected(rating)) },
                onRatingDeleted = { onEvent(RatingState.Event.RatingDeleted) },
            )
        }
    }
}
