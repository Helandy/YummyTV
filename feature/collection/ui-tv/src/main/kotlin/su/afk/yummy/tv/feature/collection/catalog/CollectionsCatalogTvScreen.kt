package su.afk.yummy.tv.feature.collection.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.feature.collection.R
import su.afk.yummy.tv.feature.collection.view.CollectionsCatalogGrid

@Composable
fun CollectionsCatalogTvScreen(
    state: CollectionsCatalogState.State,
    effect: Flow<CollectionsCatalogState.Effect>,
    onEvent: (CollectionsCatalogState.Event) -> Unit,
) {
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val retryFocusRequester = remember { FocusRequester() }
    val itemIds = remember(state.items) { state.items.map { it.id } }
    val itemFocusRequesters = remember(itemIds) { List(state.items.size) { FocusRequester() } }
    var lastFocusedItemId by rememberSaveable { mutableIntStateOf(0) }
    val focusedIndex = remember(itemIds, lastFocusedItemId) {
        itemIds.indexOf(lastFocusedItemId).takeIf { it >= 0 } ?: 0
    }
    val preferredContentFocusRequester = when {
        state.items.isNotEmpty() -> itemFocusRequesters.getOrNull(focusedIndex)
        state.error != null -> retryFocusRequester
        else -> null
    }
    val shouldShowInitialError = state.error != null && state.items.isEmpty()
    val shouldShowEmpty = !state.isLoading && state.error == null && state.items.isEmpty()

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading && state.items.isEmpty() -> TvLoadingScreen()

            shouldShowInitialError -> CollectionsCatalogMessage(
                message = state.error.orEmpty(),
                onRetry = { onEvent(CollectionsCatalogState.Event.RetrySelected) },
                retryFocusRequester = retryFocusRequester,
            )

            shouldShowEmpty -> Text(
                text = stringResource(R.string.collection_catalog_tv_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> CollectionsCatalogGrid(
                items = state.items,
                canLoadMore = state.canLoadMore,
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                itemFocusRequesters = itemFocusRequesters,
                onCollectionSelected = {
                    onEvent(CollectionsCatalogState.Event.CollectionSelected(it))
                },
                onCollectionFocused = { lastFocusedItemId = it },
                onLoadMore = { onEvent(CollectionsCatalogState.Event.LoadMoreSelected) },
            )
        }
    }
}

@Composable
private fun CollectionsCatalogMessage(
    message: String,
    onRetry: () -> Unit,
    retryFocusRequester: FocusRequester,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            modifier = Modifier.focusRequester(retryFocusRequester),
            onClick = onRetry,
        ) {
            Text(text = stringResource(R.string.retry))
        }
    }
}
