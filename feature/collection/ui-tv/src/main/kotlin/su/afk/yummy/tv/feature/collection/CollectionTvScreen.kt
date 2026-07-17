package su.afk.yummy.tv.feature.collection

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.collection.model.CollectionVote
import su.afk.yummy.tv.feature.collection.view.CollectionGridPane
import su.afk.yummy.tv.feature.collection.view.TvCollectionEditDialog
import su.afk.yummy.tv.feature.collection.view.TvDeleteCollectionDialog

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun CollectionTvScreenDefaultPreview() = ScreenPreviewTheme {
    CollectionTvScreen(CollectionState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(
    name = "Loading",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
private fun CollectionTvScreenLoadingPreview() = ScreenPreviewTheme {
    CollectionTvScreen(CollectionState.State(isLoading = true), emptyFlow()) {}
}

@Preview(
    name = "Error",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun CollectionTvScreenErrorPreview() = ScreenPreviewTheme {
    CollectionTvScreen(
        CollectionState.State(
            isLoading = false,
            error = "Не удалось загрузить коллекцию"
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionTvScreen(
    state: CollectionState.State,
    effect: Flow<CollectionState.Effect>,
    onEvent: (CollectionState.Event) -> Unit,
) {
    val context = LocalContext.current
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val loadingFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val preferredContentFocusRequester = when {
        state.isLoading -> loadingFocusRequester
        state.error != null -> retryFocusRequester
        else -> null
    }
    val onAnimeSelected =
        remember(onEvent) { { animeId: Int -> onEvent(CollectionState.Event.AnimeSelected(animeId)) } }
    val onScrollPositionChanged = remember(onEvent) {
        { index: Int, offset: Int ->
            onEvent(
                CollectionState.Event.GridScrolled(
                    index,
                    offset
                )
            )
        }
    }
    val onRetry = remember(onEvent) { { onEvent(CollectionState.Event.RetrySelected) } }
    val onVote = remember(onEvent) {
        { vote: CollectionVote ->
            onEvent(
                CollectionState.Event.VoteSelected(vote)
            )
        }
    }
    val handleBack = remember(
        onEvent,
        state.isEditDialogVisible,
        state.isDeleteDialogVisible,
        state.isUpdating,
        state.isDeleting,
    ) {
        {
            when {
                state.isEditDialogVisible && !state.isUpdating ->
                    onEvent(CollectionState.Event.EditDismissed)

                state.isDeleteDialogVisible && !state.isDeleting ->
                    onEvent(CollectionState.Event.DeleteDismissed)

                !state.isEditDialogVisible && !state.isDeleteDialogVisible ->
                    onEvent(CollectionState.Event.BackSelected)
            }
        }
    }

    BackHandler { handleBack() }

    LaunchedEffect(effect, context) {
        effect.collect { event ->
            when (event) {
                is CollectionState.Effect.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    if (state.isEditDialogVisible) {
        TvCollectionEditDialog(
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
        TvDeleteCollectionDialog(
            isDeleting = state.isDeleting,
            onConfirm = { onEvent(CollectionState.Event.DeleteConfirmed) },
            onDismiss = { onEvent(CollectionState.Event.DeleteDismissed) },
        )
    }

    CollectionGridPane(
        collection = state.collection,
        isLoading = state.isLoading,
        isVoteLoading = state.isVoteLoading,
        isOwner = state.isOwner,
        isMutationLoading = state.isUpdating || state.isDeleting,
        error = state.error,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset,
        onAnimeSelected = onAnimeSelected,
        onScrollPositionChanged = onScrollPositionChanged,
        onVote = onVote,
        onEdit = { onEvent(CollectionState.Event.EditSelected) },
        onDelete = { onEvent(CollectionState.Event.DeleteSelected) },
        onComments = { onEvent(CollectionState.Event.CommentsSelected) },
        onRetry = onRetry,
        loadingFocusRequester = loadingFocusRequester,
        retryFocusRequester = retryFocusRequester,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.Back, Key.Escape -> {
                    handleBack()
                    true
                }

                else -> false
            }
        },
    )
}
