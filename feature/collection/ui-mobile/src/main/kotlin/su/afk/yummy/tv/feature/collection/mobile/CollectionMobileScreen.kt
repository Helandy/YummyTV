package su.afk.yummy.tv.feature.collection.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.collection.CollectionState
import su.afk.yummy.tv.feature.collection.mobile.view.CollectionEngagementPanel
import su.afk.yummy.tv.feature.collection.mobile.view.CollectionMobileHeader
import su.afk.yummy.tv.feature.collection.mobile.view.MobileCollectionEditDialog
import su.afk.yummy.tv.feature.collection.mobile.view.MobileDeleteCollectionDialog

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CollectionMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        CollectionMobileScreen(CollectionState.State(isLoading = false), emptyFlow()) {}
    }

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun CollectionMobileScreenLoadingPreview() = ScreenPreviewTheme {
    CollectionMobileScreen(CollectionState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun CollectionMobileScreenErrorPreview() = ScreenPreviewTheme {
    CollectionMobileScreen(
        CollectionState.State(
            isLoading = false,
            error = "Не удалось загрузить коллекцию"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionMobileScreen(
    state: CollectionState.State,
    effect: Flow<CollectionState.Effect>,
    onEvent: (CollectionState.Event) -> Unit,
) {
    val context = LocalContext.current
    val collection = state.collection

    if (state.isEditDialogVisible) {
        MobileCollectionEditDialog(
            title = state.editTitle,
            description = state.editDescription,
            isPublic = state.editIsPublic,
            isUpdating = state.isUpdating,
            onTitleChanged = { onEvent(CollectionState.Event.EditTitleChanged(it)) },
            onDescriptionChanged = { onEvent(CollectionState.Event.EditDescriptionChanged(it)) },
            onPublicChanged = { onEvent(CollectionState.Event.EditPublicChanged(it)) },
            onConfirm = { onEvent(CollectionState.Event.EditConfirmed) },
            onDismiss = { onEvent(CollectionState.Event.EditDismissed) },
        )
    }
    if (state.isDeleteDialogVisible) {
        MobileDeleteCollectionDialog(
            isDeleting = state.isDeleting,
            onConfirm = { onEvent(CollectionState.Event.DeleteConfirmed) },
            onDismiss = { onEvent(CollectionState.Event.DeleteDismissed) },
        )
    }

    LaunchedEffect(Unit) {
        effect.collect { event ->
            when (event) {
                is CollectionState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = collection?.title ?: stringResource(R.string.collection_mobile_title),
                onBack = { onEvent(CollectionState.Event.BackSelected) },
            )
        },
        isLoading = state.isLoading,
        error = state.error?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(CollectionState.Event.RetrySelected) },
        isEmpty = collection?.animes.orEmpty().isEmpty() && !state.isLoading,
        errorContent = state.error?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.collection_mobile_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        MobilePosterGrid(contentPadding = PaddingValues(bottom = 80.dp)) {
            if (collection != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CollectionMobileHeader(
                        collection = collection,
                        isOwner = state.isOwner,
                        isMutationLoading = state.isUpdating || state.isDeleting,
                        onEdit = { onEvent(CollectionState.Event.EditSelected) },
                        onDelete = { onEvent(CollectionState.Event.DeleteSelected) },
                    )
                }
                items(collection.animes, key = { lazyKey("collectionanime", it.id) }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        rating = item.rating,
                        posterOverlay = {
                            item.year?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.inverseOnSurface,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        },
                        onClick = { onEvent(CollectionState.Event.AnimeSelected(item.id)) },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CollectionEngagementPanel(
                        collection = collection,
                        voting = state.isVoteLoading,
                        onVote = { vote -> onEvent(CollectionState.Event.VoteSelected(vote)) },
                        onCommentsClick = { onEvent(CollectionState.Event.CommentsSelected) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
