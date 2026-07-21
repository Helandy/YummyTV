package su.afk.yummy.tv.feature.collection.mobile.catalog

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileAppendError
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.collection.catalog.CollectionsCatalogState
import su.afk.yummy.tv.feature.collection.mobile.R
import su.afk.yummy.tv.feature.collection.mobile.utils.uiMessage
import su.afk.yummy.tv.feature.collection.mobile.view.CollectionLikesBadge
import su.afk.yummy.tv.feature.collection.mobile.view.CreateCollectionDialog

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CollectionsCatalogMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        CollectionsCatalogMobileScreen(CollectionsCatalogState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionsCatalogMobileScreen(
    state: CollectionsCatalogState.State,
    effect: Flow<CollectionsCatalogState.Effect>,
    onEvent: (CollectionsCatalogState.Event) -> Unit,
) {
    val context = LocalContext.current
    val pagingItems = state.items.collectAsLazyPagingItems()
    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append
    val initialError = (refreshState as? LoadState.Error)
        ?.takeIf { pagingItems.itemCount == 0 }
        ?.error
        ?.uiMessage()
    val gridState = rememberLazyGridState()

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is CollectionsCatalogState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (state.isCreateDialogVisible) {
        CreateCollectionDialog(
            title = state.createTitle,
            description = state.createDescription,
            isPublic = state.isCreatePublic,
            isCreating = state.isCreating,
            onTitleChanged = {
                onEvent(CollectionsCatalogState.Event.CreateTitleChanged(it))
            },
            onDescriptionChanged = {
                onEvent(CollectionsCatalogState.Event.CreateDescriptionChanged(it))
            },
            onPublicChanged = {
                onEvent(CollectionsCatalogState.Event.CreatePublicChanged(it))
            },
            onConfirm = { onEvent(CollectionsCatalogState.Event.CreateConfirmed) },
            onDismiss = { onEvent(CollectionsCatalogState.Event.CreateDismissed) },
        )
    }

    BaseScreen(
        isScroll = false,
        topBar = {
            MobileTopBar(
                title = stringResource(R.string.collection_catalog_mobile_title),
                onBack = { onEvent(CollectionsCatalogState.Event.BackSelected) },
            )
        },
        isLoading = refreshState is LoadState.Loading && pagingItems.itemCount == 0,
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = {
            onEvent(CollectionsCatalogState.Event.RetrySelected)
            pagingItems.retry()
        },
        errorContent = initialError?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.collection_mobile_retry),
                    onAction = retry,
                )
            }
        },
        floatingActionButtonEnd = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(CollectionsCatalogState.Event.CreateSelected) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.collection_create_button)) },
            )
        },
        floatingActionButtonBottomPadding = 8.dp,
    ) {
        MobilePosterGrid(
            contentPadding = PaddingValues(bottom = 80.dp),
            state = gridState,
        ) {
            val error = (appendState as? LoadState.Error)?.error?.uiMessage()
            if (error != null && pagingItems.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MobileAppendError(
                        message = error,
                        onRetry = { pagingItems.retry() },
                    )
                }
            }
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.id },
            ) { index ->
                pagingItems[index]?.let { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        posterOverlay = {
                            CollectionLikesBadge(
                                likesCount = item.likesCount,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                            )
                        },
                        onClick = {
                            onEvent(CollectionsCatalogState.Event.CollectionSelected(item.id))
                        },
                    )
                }
            }
            if (appendState is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}
