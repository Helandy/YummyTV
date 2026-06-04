@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = horizontalPadding, vertical = TvScreenPadding.Vertical),
    ) {
        if (!state.isSignedIn) {
            LoginPanel(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AccountHubPanel(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
