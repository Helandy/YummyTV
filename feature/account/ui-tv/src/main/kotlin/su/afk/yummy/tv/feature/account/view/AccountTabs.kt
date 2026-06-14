@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.feature.account.AccountState
import su.afk.yummy.tv.feature.account.R

@Composable
internal fun AccountTabs(
    selected: AccountState.AccountTab,
    onSelected: (AccountState.AccountTab) -> Unit,
    selectedTabFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    onContentRequested: (() -> Unit)? = null,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
    autoFocusSelected: Boolean = true,
) {
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val scope = rememberCoroutineScope()
    val statsFocusRequester = remember { FocusRequester() }
    val notificationsFocusRequester = remember { FocusRequester() }
    val markAllReadFocusRequester = remember { FocusRequester() }
    val statsRequester =
        if (selected == AccountState.AccountTab.STATS && selectedTabFocusRequester != null) {
            selectedTabFocusRequester
        } else {
            statsFocusRequester
        }
    val notificationsRequester =
        if (selected == AccountState.AccountTab.NOTIFICATIONS && selectedTabFocusRequester != null) {
            selectedTabFocusRequester
        } else {
            notificationsFocusRequester
        }

    LaunchedEffect(selected, autoFocusSelected) {
        if (!autoFocusSelected) return@LaunchedEffect
        when (selected) {
            AccountState.AccountTab.STATS -> {
                runCatching { statsRequester.requestFocus() }
            }

            AccountState.AccountTab.NOTIFICATIONS -> {
                runCatching { notificationsRequester.requestFocus() }
            }
        }
    }

    fun requestMainMenuFocus(): Boolean {
        val requester = mainMenuFocusRequester ?: return false
        scope.launch {
            repeat(6) {
                runCatching { requester.requestFocus() }
                withFrameNanos { }
            }
        }
        return true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountTabButton(
                label = stringResource(R.string.account_tab_stats),
                selected = selected == AccountState.AccountTab.STATS,
                onClick = { onSelected(AccountState.AccountTab.STATS) },
                modifier = Modifier
                    .focusRequester(statsRequester)
                    .accountTabFocus(
                        contentFocusRequester = contentFocusRequester,
                        leftFocusRequester = mainMenuFocusRequester,
                        rightFocusRequester = notificationsRequester,
                        onDirectionLeft = ::requestMainMenuFocus,
                        onFocusSelected = { onSelected(AccountState.AccountTab.STATS) },
                        onActivated = {
                            onSelected(AccountState.AccountTab.STATS)
                            onContentRequested?.invoke()
                                ?: contentFocusRequester?.let { runCatching { it.requestFocus() } }
                        },
                    )
                    .onFocusChanged { if (it.isFocused) onSelected(AccountState.AccountTab.STATS) },
            )
            AccountTabButton(
                label = stringResource(R.string.account_tab_notifications),
                selected = selected == AccountState.AccountTab.NOTIFICATIONS,
                onClick = { onSelected(AccountState.AccountTab.NOTIFICATIONS) },
                modifier = Modifier
                    .focusRequester(notificationsRequester)
                    .accountTabFocus(
                        contentFocusRequester = contentFocusRequester,
                        leftFocusRequester = statsRequester,
                        rightFocusRequester = markAllReadFocusRequester.takeIf { onMarkAllRead != null },
                        onDirectionLeft = null,
                        onFocusSelected = { onSelected(AccountState.AccountTab.NOTIFICATIONS) },
                        onActivated = {
                            onSelected(AccountState.AccountTab.NOTIFICATIONS)
                            onContentRequested?.invoke()
                                ?: contentFocusRequester?.let { runCatching { it.requestFocus() } }
                        },
                    )
                    .onFocusChanged { if (it.isFocused) onSelected(AccountState.AccountTab.NOTIFICATIONS) },
            )
        }
        if (onMarkAllRead != null) {
            AccountAction(
                label = stringResource(R.string.account_mark_all_read),
                onClick = onMarkAllRead,
                modifier = Modifier
                    .width(260.dp)
                    .focusRequester(markAllReadFocusRequester)
                    .focusProperties { left = notificationsRequester },
                enabled = markAllReadEnabled,
            )
        }
    }
}

private fun Modifier.accountTabFocus(
    contentFocusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onDirectionLeft: (() -> Boolean)?,
    onFocusSelected: () -> Unit,
    onActivated: () -> Unit,
): Modifier = this
    .focusProperties {
        contentFocusRequester?.let { down = it }
        leftFocusRequester?.let { left = it }
        rightFocusRequester?.let { right = it }
    }
    .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionLeft -> {
                onDirectionLeft?.invoke() ?: leftFocusRequester?.let {
                    runCatching { it.requestFocus() }
                    true
                } ?: false
            }

            Key.DirectionRight -> {
                rightFocusRequester?.let {
                    runCatching { it.requestFocus() }
                }
                true
            }

            Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                onActivated()
                true
            }

            else -> false
        }
    }
    .onFocusChanged {
        if (it.isFocused) onFocusSelected()
    }
