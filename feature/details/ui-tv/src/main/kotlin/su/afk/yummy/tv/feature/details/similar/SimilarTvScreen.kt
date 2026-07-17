package su.afk.yummy.tv.feature.details.similar

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.similar.view.SimilarTab

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun SimilarTvScreenDefaultPreview() = ScreenPreviewTheme {
    SimilarTvScreen(SimilarState.State(), emptyFlow()) {}
}

@Composable
fun SimilarTvScreen(
    state: SimilarState.State,
    effect: Flow<SimilarState.Effect>,
    onEvent: (SimilarState.Event) -> Unit,
) {
    val context = LocalContext.current
    BackHandler { onEvent(SimilarState.Event.BackSelected) }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is SimilarState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            ignored = state.isRecommendationIgnored,
            recommendationMutationPending = state.isRecommendationMutationPending,
            pendingVoteAnimeIds = state.pendingVoteAnimeIds,
            onRecommendationVisibilityToggled = {
                onEvent(SimilarState.Event.RecommendationVisibilityToggled)
            },
            onVote = { id, vote -> onEvent(SimilarState.Event.VoteSelected(id, vote)) },
            modifier = Modifier.fillMaxSize(),
            onRetry = { onEvent(SimilarState.Event.RetrySelected) },
        )
    }
}
