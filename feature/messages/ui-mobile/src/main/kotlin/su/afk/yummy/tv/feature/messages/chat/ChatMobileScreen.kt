package su.afk.yummy.tv.feature.messages.chat

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.messages.mobile.R
import su.afk.yummy.tv.feature.messages.view.ChatManagementMobileDialogs
import su.afk.yummy.tv.feature.messages.view.ChatMessageMobileBubble
import su.afk.yummy.tv.feature.messages.view.ChatMobileActionsMenu
import su.afk.yummy.tv.feature.messages.view.ChatMobileComposer
import su.afk.yummy.tv.feature.messages.view.EditMessageMobileDialog
import su.afk.yummy.tv.feature.messages.view.MessageHistoryMobileDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMobileScreen(
    state: ChatState.State,
    effect: Flow<ChatState.Effect>,
    onEvent: (ChatState.Event) -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var previousLastMessageId by remember { mutableIntStateOf(0) }
    ChatMobileLifecycleEffect(onEvent)

    LaunchedEffect(effect, context) {
        effect.collect { item ->
            if (item is ChatState.Effect.ShowMessage) {
                val message = when (item.type) {
                    ChatState.MessageType.SEND_FAILED -> R.string.messages_send_failed
                    ChatState.MessageType.EDIT_FAILED -> R.string.messages_edit_failed
                    ChatState.MessageType.READ_FAILED -> R.string.messages_read_failed
                    ChatState.MessageType.DELETE_FAILED -> R.string.messages_delete_failed
                    ChatState.MessageType.RESTORE_FAILED -> R.string.messages_restore_failed
                    ChatState.MessageType.CLAIM_FAILED -> R.string.messages_claim_failed
                    ChatState.MessageType.CLAIM_SENT -> R.string.messages_claim_sent
                    ChatState.MessageType.BAN_FAILED -> R.string.messages_ban_failed
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(listState, state.canLoadOlder) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { first ->
                if (first == 0 && state.canLoadOlder) {
                    onEvent(ChatState.Event.LoadOlderSelected)
                }
            }
    }
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        val lastMessageId = state.messages.lastOrNull()?.id ?: 0
        val visibleLastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index.orZero()
        val wasNearBottom = previousLastMessageId == 0 ||
                visibleLastIndex >= state.messages.lastIndex - 3
        if (lastMessageId != previousLastMessageId && state.messages.isNotEmpty() && wasNearBottom) {
            listState.scrollToItem(state.messages.lastIndex)
        }
        previousLastMessageId = lastMessageId
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            MobileTopBar(
                title = state.peer?.nickname?.takeIf { it.isNotBlank() }
                    ?: state.fallbackNickname.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.messages_unknown_user, state.userId),
                onBack = { onEvent(ChatState.Event.BackSelected) },
                actions = {
                    (state.peer?.avatarUrl ?: state.fallbackAvatarUrl)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { avatarUrl ->
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape),
                            )
                        }
                    state.peer?.let { peer ->
                        ChatMobileActionsMenu(
                            isBanned = peer.isBanned,
                            enabled = !state.isMutating,
                            onBanToggle = { onEvent(ChatState.Event.BanToggleSelected) },
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            !state.isAuthResolved -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            !state.isAuthorized -> StateMessage(
                title = stringResource(R.string.messages_auth_required),
                actionLabel = stringResource(R.string.messages_sign_in),
                onAction = { onEvent(ChatState.Event.LoginSelected) },
                modifier = Modifier.fillMaxSize(),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(ChatState.Event.RefreshSelected) },
                    modifier = Modifier.weight(1f),
                ) {
                    when {
                        state.isLoading -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        state.hasLoadError && state.messages.isEmpty() -> StateMessage(
                            title = stringResource(R.string.messages_chat_error),
                            actionLabel = stringResource(R.string.messages_retry),
                            onAction = { onEvent(ChatState.Event.RefreshSelected) },
                            modifier = Modifier.fillMaxSize(),
                        )

                        state.messages.isEmpty() -> StateMessage(
                            title = stringResource(R.string.messages_chat_empty),
                            modifier = Modifier.fillMaxSize(),
                        )

                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                top = padding.calculateTopPadding() + 12.dp,
                                end = 12.dp,
                                bottom = 12.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.isLoadingOlder) item(key = "older_loading") {
                                Box(
                                    Modifier.fillParentMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            items(state.messages, key = { it.id }) { message ->
                                ChatMessageMobileBubble(
                                    message = message,
                                    isOwn = message.fromUserId == state.currentUserId,
                                    onEdit = { onEvent(ChatState.Event.EditSelected(message.id)) },
                                    onDelete = { onEvent(ChatState.Event.DeleteSelected(message.id)) },
                                    onRestore = { onEvent(ChatState.Event.RestoreSelected(message.id)) },
                                    onHistory = { onEvent(ChatState.Event.HistorySelected(message.id)) },
                                    onClaim = { onEvent(ChatState.Event.ClaimSelected(message.id)) },
                                )
                            }
                        }
                    }
                }
                if (state.peer?.isBanned == true) {
                    Text(
                        text = stringResource(R.string.messages_banned_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                ChatMobileComposer(
                    text = state.draft,
                    enabled = !state.isMutating && state.peer?.isBanned != true,
                    onTextChange = { onEvent(ChatState.Event.DraftChanged(it)) },
                    onSend = { onEvent(ChatState.Event.SendSelected) },
                )
            }
        }
    }

    if (state.editingMessageId != null) {
        EditMessageMobileDialog(
            text = state.editingText,
            enabled = !state.isMutating,
            onTextChange = { onEvent(ChatState.Event.EditTextChanged(it)) },
            onConfirm = { onEvent(ChatState.Event.EditConfirmed) },
            onDismiss = { onEvent(ChatState.Event.EditCancelled) },
        )
    }

    ChatManagementMobileDialogs(
        showDeleteConfirmation = state.pendingDeleteMessageId != null,
        showClaimConfirmation = state.pendingClaimMessageId != null,
        showBanConfirmation = state.isBanConfirmationVisible,
        isBanned = state.peer?.isBanned == true,
        isMutating = state.isMutating,
        onDeleteConfirm = { onEvent(ChatState.Event.DeleteConfirmed) },
        onDeleteDismiss = { onEvent(ChatState.Event.DeleteDismissed) },
        onClaimConfirm = { onEvent(ChatState.Event.ClaimConfirmed) },
        onClaimDismiss = { onEvent(ChatState.Event.ClaimDismissed) },
        onBanConfirm = { onEvent(ChatState.Event.BanToggleConfirmed) },
        onBanDismiss = { onEvent(ChatState.Event.BanToggleDismissed) },
    )

    if (state.historyMessageId != null) {
        MessageHistoryMobileDialog(
            entries = state.messageHistory,
            isLoading = state.isHistoryLoading,
            hasError = state.hasHistoryError,
            onRetry = { onEvent(ChatState.Event.HistoryRetrySelected) },
            onDismiss = { onEvent(ChatState.Event.HistoryDismissed) },
        )
    }
}

@Composable
private fun ChatMobileLifecycleEffect(onEvent: (ChatState.Event) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> currentOnEvent(ChatState.Event.ScreenStarted)
                Lifecycle.Event.ON_STOP -> currentOnEvent(ChatState.Event.ScreenStopped)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            currentOnEvent(ChatState.Event.ScreenStarted)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentOnEvent(ChatState.Event.ScreenStopped)
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0
