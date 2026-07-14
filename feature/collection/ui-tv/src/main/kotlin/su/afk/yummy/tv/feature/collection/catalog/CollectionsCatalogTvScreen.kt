package su.afk.yummy.tv.feature.collection.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.collection.R
import su.afk.yummy.tv.feature.collection.view.CollectionsCatalogGrid

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun CollectionsCatalogTvScreenDefaultPreview() = ScreenPreviewTheme {
    CollectionsCatalogTvScreen(CollectionsCatalogState.State(), emptyFlow()) {}
}

@Composable
fun CollectionsCatalogTvScreen(
    state: CollectionsCatalogState.State,
    effect: Flow<CollectionsCatalogState.Effect>,
    onEvent: (CollectionsCatalogState.Event) -> Unit,
) {
    val items = state.items.collectAsLazyPagingItems()
    val refreshState = items.loadState.refresh
    val appendState = items.loadState.append
    val itemCount = items.itemCount
    val snapshotItems = items.itemSnapshotList.items
    val isLoading = refreshState is LoadState.Loading
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { itemCount == 0 }
        ?.error
        ?.uiMessage()
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val loadingFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val itemIds = remember(snapshotItems) { snapshotItems.map { it.id } }
    val itemFocusRequesters = remember(itemCount) { List(itemCount) { FocusRequester() } }
    var lastFocusedItemId by rememberSaveable { mutableIntStateOf(0) }
    var focusContentAfterInitialLoad by remember { mutableStateOf(false) }
    val focusedIndex = remember(itemIds, lastFocusedItemId) {
        itemIds.indexOf(lastFocusedItemId).takeIf { it >= 0 } ?: 0
    }
    val preferredContentFocusRequester = when {
        itemCount > 0 -> itemFocusRequesters.getOrNull(focusedIndex)
        isLoading -> loadingFocusRequester
        initialError != null -> retryFocusRequester
        else -> null
    }
    val shouldShowEmpty = !isLoading && initialError == null && itemCount == 0

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    LaunchedEffect(isLoading, itemCount) {
        if (isLoading && itemCount == 0) {
            focusContentAfterInitialLoad = true
        }
    }

    LaunchedEffect(
        focusContentAfterInitialLoad,
        isLoading,
        itemIds,
        focusedIndex,
        itemFocusRequesters,
    ) {
        if (!focusContentAfterInitialLoad || isLoading || itemIds.isEmpty()) {
            return@LaunchedEffect
        }
        val focusRequester = itemFocusRequesters.getOrNull(focusedIndex) ?: return@LaunchedEffect
        var focused = false
        repeat(3) {
            if (!focused) {
                withFrameNanos { }
                focused = runCatching { focusRequester.requestFocus() }.getOrDefault(false)
            }
        }
        if (focused) {
            focusContentAfterInitialLoad = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading && itemCount == 0 -> TvLoadingScreen(
                modifier = Modifier
                    .focusRequester(loadingFocusRequester)
                    .focusable(),
            )

            initialError != null -> CollectionsCatalogMessage(
                message = initialError,
                onRetry = {
                    onEvent(CollectionsCatalogState.Event.RetrySelected)
                    items.retry()
                },
                retryFocusRequester = retryFocusRequester,
            )

            shouldShowEmpty -> Text(
                text = stringResource(R.string.collection_catalog_tv_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> CollectionsCatalogGrid(
                pagingItems = items,
                isLoadingMore = appendState is LoadState.Loading,
                itemFocusRequesters = itemFocusRequesters,
                onCollectionSelected = {
                    onEvent(CollectionsCatalogState.Event.CollectionSelected(it))
                },
                onCollectionFocused = { lastFocusedItemId = it },
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
        TvRetryButton(
            text = stringResource(R.string.retry),
            modifier = Modifier.focusRequester(retryFocusRequester),
            onClick = onRetry,
        )
    }
}

private fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
