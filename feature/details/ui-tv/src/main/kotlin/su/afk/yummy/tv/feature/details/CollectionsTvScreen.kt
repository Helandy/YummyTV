package su.afk.yummy.tv.feature.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary

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

@Composable
private fun CollectionsGrid(
    collections: List<AnimeCollectionSummary>,
    onCollectionSelected: (Int) -> Unit,
) {
    val posterQuality = LocalPosterQuality.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 172.dp),
        contentPadding = PaddingValues(
            start = TvScreenPadding.Horizontal,
            end = TvScreenPadding.Horizontal,
            top = TvScreenPadding.Vertical,
            bottom = TvScreenPadding.Vertical,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.details_collections_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        items(collections, key = { it.id }) { collection ->
            TvTitleCard(
                title = collection.title,
                posterUrl = collection.posterUrl(posterQuality),
                onClick = { onCollectionSelected(collection.id) },
            )
        }
    }
}

private fun AnimeCollectionSummary.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> poster?.medium ?: poster?.big ?: poster?.fullsize ?: poster?.small ?: posterUrl
    PosterQuality.STANDARD -> poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small ?: posterUrl
    PosterQuality.MEGA -> poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.fullsize ?: poster?.small ?: posterUrl
    PosterQuality.HIGH -> poster?.fullsize ?: poster?.mega ?: poster?.big ?: poster?.medium ?: poster?.small ?: posterUrl
}

@Composable
private fun CollectionsMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = text, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}
