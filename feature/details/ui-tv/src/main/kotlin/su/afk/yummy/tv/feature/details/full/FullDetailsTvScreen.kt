package su.afk.yummy.tv.feature.details.full

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.feature.details.full.view.FullDetailsBody
import su.afk.yummy.tv.feature.details.view.common.DetailsError

@Composable
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
            details != null -> FullDetailsBody(details)
            else -> TvLoadingScreen()
        }
    }
}
