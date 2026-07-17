package su.afk.yummy.tv.feature.messages.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.messages.mobile.R
import su.afk.yummy.tv.feature.messages.view.DialogMobileRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogsMobileScreen(
    state: DialogsState.State,
    effect: Flow<DialogsState.Effect>,
    onEvent: (DialogsState.Event) -> Unit,
) {
    val dialogs = state.dialogs.collectAsLazyPagingItems()
    androidx.compose.material3.Scaffold(
        topBar = {
            MobileTopBar(
                title = stringResource(R.string.messages_title),
                onBack = { onEvent(DialogsState.Event.BackSelected) },
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
                onAction = { onEvent(DialogsState.Event.LoginSelected) },
                modifier = Modifier.fillMaxSize(),
            )

            else -> PullToRefreshBox(
                isRefreshing = dialogs.loadState.refresh is LoadState.Loading && dialogs.itemCount > 0,
                onRefresh = dialogs::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = padding.calculateTopPadding() + 12.dp,
                        end = 16.dp,
                        bottom = padding.calculateBottomPadding() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when {
                        dialogs.loadState.refresh is LoadState.Loading -> item {
                            Box(
                                Modifier.fillParentMaxHeight(.7f),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }

                        dialogs.loadState.refresh is LoadState.Error -> item {
                            StateMessage(
                                title = stringResource(R.string.messages_error),
                                actionLabel = stringResource(R.string.messages_retry),
                                onAction = dialogs::retry,
                                fillMaxSize = false,
                            )
                        }

                        dialogs.itemCount == 0 -> item {
                            StateMessage(
                                title = stringResource(R.string.messages_empty),
                                fillMaxSize = false,
                            )
                        }

                        else -> items(
                            count = dialogs.itemCount,
                            key = dialogs.itemKey { it.userId },
                        ) { index ->
                            dialogs[index]?.let { dialog ->
                                DialogMobileRow(dialog) {
                                    onEvent(DialogsState.Event.DialogSelected(dialog.userId))
                                }
                            }
                        }
                    }
                    if (dialogs.loadState.append is LoadState.Loading) item {
                        LinearProgressIndicator(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
