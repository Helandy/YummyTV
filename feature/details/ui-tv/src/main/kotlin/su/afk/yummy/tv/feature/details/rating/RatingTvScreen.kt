package su.afk.yummy.tv.feature.details.rating

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.feature.details.rating.view.RatingBody
import su.afk.yummy.tv.feature.details.view.common.DetailsError

@Composable
fun RatingTvScreen(
    state: RatingState.State,
    effect: Flow<RatingState.Effect>,
    onEvent: (RatingState.Event) -> Unit,
) {
    val error = state.error
    BackHandler { onEvent(RatingState.Event.BackSelected) }

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
