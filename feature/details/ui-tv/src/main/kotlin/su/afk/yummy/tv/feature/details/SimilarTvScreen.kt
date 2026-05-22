package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.view.SimilarTab

@Composable
fun SimilarTvScreen(
    state: SimilarState.State,
    effect: Flow<SimilarState.Effect>,
    onEvent: (SimilarState.Event) -> Unit,
) {
    BackHandler { onEvent(SimilarState.Event.BackSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimilarTab(
            state = state.similarState,
            fromAi = state.fromAi,
            onToggle = { onEvent(SimilarState.Event.SourceToggled) },
            onAnimeSelected = { id -> onEvent(SimilarState.Event.AnimeSelected(id)) },
            onItemFocused = { id -> onEvent(SimilarState.Event.ItemFocused(id)) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
