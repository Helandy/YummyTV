package su.afk.yummy.tv.feature.account.mobile.mysubscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.domain.account.model.SubscriptionKeys
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.mysubscriptions.view.MySubscriptionMobileRow
import su.afk.yummy.tv.feature.account.mysubscriptions.MySubscriptionsState
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubscriptionsMobileScreen(
    state: MySubscriptionsState.State,
    effect: Flow<MySubscriptionsState.Effect>,
    onEvent: (MySubscriptionsState.Event) -> Unit,
) {
    LaunchedEffect(Unit) { onEvent(MySubscriptionsState.Event.ScreenShown) }

    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.account_mobile_my_subscriptions),
                onBack = { onEvent(MySubscriptionsState.Event.BackSelected) },
            )
        },
    ) {
        when {
            state.isLoading && state.subscriptions.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> MobileMessage(
                title = state.error.accountErrorMessage().orEmpty(),
                icon = Icons.Filled.Warning,
                actionLabel = stringResource(CoreR.string.retry),
                onAction = { onEvent(MySubscriptionsState.Event.RetrySelected) },
            )

            state.subscriptions.isEmpty() -> MobileMessage(
                title = stringResource(R.string.account_mobile_my_subscriptions_empty),
                icon = Icons.Filled.Notifications,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    state.subscriptions,
                    key = { SubscriptionKeys.animePlayerKey(it.animeId, it.playerId, it.player) },
                ) { subscription ->
                    MySubscriptionMobileRow(
                        subscription = subscription,
                        onClick = {
                            onEvent(
                                MySubscriptionsState.Event.SubscriptionSelected(subscription.animeId),
                            )
                        },
                    )
                }
            }
        }
    }
}
