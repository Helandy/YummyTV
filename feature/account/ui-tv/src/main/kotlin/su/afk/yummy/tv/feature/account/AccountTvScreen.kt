@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.feature.account.utils.LocalAccountTvActiveDestination
import su.afk.yummy.tv.feature.account.view.AccountHubPanel
import su.afk.yummy.tv.feature.account.view.LoginPanel

@Composable
fun AccountTvScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    BackHandler { onEvent(AccountState.Event.BackSelected) }
    val horizontalPadding = if (state.isSignedIn) 32.dp else TvScreenPadding.Horizontal
    val preferredFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val isActiveDestination = LocalAccountTvActiveDestination.current
    val scope = rememberCoroutineScope()
    var isStatsContentFocused by remember { mutableStateOf(false) }

    fun shouldOpenMainMenuFromLeft(): Boolean =
        state.selectedTab == AccountState.AccountTab.STATS && !isStatsContentFocused

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

    DisposableEffect(preferredFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key != Key.DirectionLeft) return@onPreviewKeyEvent false
                if (!shouldOpenMainMenuFromLeft()) return@onPreviewKeyEvent false
                requestMainMenuFocus()
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (event.key != Key.DirectionLeft) return@onKeyEvent false
                if (!shouldOpenMainMenuFromLeft()) return@onKeyEvent false
                requestMainMenuFocus()
            }
            .padding(horizontal = horizontalPadding, vertical = TvScreenPadding.Vertical),
    ) {
        if (!state.isSignedIn) {
            LoginPanel(
                state = state,
                onEvent = onEvent,
                initialFocusRequester = preferredFocusRequester,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AccountHubPanel(
                state = state,
                onEvent = onEvent,
                initialFocusRequester = preferredFocusRequester,
                isActiveDestination = isActiveDestination,
                onStatsContentFocusChanged = { isStatsContentFocused = it },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
