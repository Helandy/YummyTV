package su.afk.yummy.tv.feature.details.rating

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
