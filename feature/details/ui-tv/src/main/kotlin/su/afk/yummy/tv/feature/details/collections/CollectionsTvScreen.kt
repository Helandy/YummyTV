package su.afk.yummy.tv.feature.details.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.collections.view.CollectionsGrid
import su.afk.yummy.tv.feature.details.collections.view.CollectionsMessage

@Composable
fun CollectionsTvScreen(

    state: CollectionsState.State,
    effect: Flow<CollectionsState.Effect>,
    onEvent: (CollectionsState.Event) -> Unit,

) {
    BackHandler { onEvent(CollectionsState.Event.BackSelected) }
    val error = state.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            error != null -> CollectionsMessage(
                text = error,
                onRetry = { onEvent(CollectionsState.Event.RetrySelected) },
                modifier = Modifier.align(Alignment.Center),
            )
            state.collections.isEmpty() -> Text(
                text = stringResource(R.string.details_collections_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> CollectionsGrid(
                collections = state.collections,
                onCollectionSelected = { onEvent(CollectionsState.Event.CollectionSelected(it)) },
            )
        }
    }
}
