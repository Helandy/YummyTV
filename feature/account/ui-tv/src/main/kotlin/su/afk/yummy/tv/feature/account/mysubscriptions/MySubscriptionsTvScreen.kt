package su.afk.yummy.tv.feature.account.mysubscriptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.tv.TvStateContent
import su.afk.yummy.tv.feature.account.common.utils.isEmpty
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.view.MySubscriptionsGrid

@Composable
fun MySubscriptionsTvScreen(
    state: MySubscriptionsState.State,
    effect: Flow<MySubscriptionsState.Effect>,
    onEvent: (MySubscriptionsState.Event) -> Unit,
) {
    LaunchedEffect(Unit) { onEvent(MySubscriptionsState.Event.ScreenShown) }

    TvStateContent(
        isLoading = state.isLoading && state.subscriptions.isEmpty(),
        error = state.error.accountErrorMessage(),
        empty = state.subscriptions.isEmpty(),
        emptyText = stringResource(
            if (state.isSignedIn) {
                R.string.account_my_subscriptions_empty
            } else {
                R.string.account_my_subscriptions_signed_out
            }
        ),
        onRetry = { onEvent(MySubscriptionsState.Event.RetrySelected) },
    ) {
        MySubscriptionsGrid(state = state, onEvent = onEvent)
    }
}
