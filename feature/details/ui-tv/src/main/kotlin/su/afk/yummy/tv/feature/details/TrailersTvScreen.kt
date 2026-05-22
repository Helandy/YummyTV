package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.feature.details.view.TrailerTab

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
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
            ) {
                item {
                    TrailerTab(
                        trailers = state.trailers,
                        onTrailerSelected = { url -> onEvent(TrailersState.Event.TrailerSelected(url)) },
                    )
                }
            }
        }
    }
}
