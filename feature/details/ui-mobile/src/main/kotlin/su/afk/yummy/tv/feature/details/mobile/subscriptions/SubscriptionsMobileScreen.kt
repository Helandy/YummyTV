package su.afk.yummy.tv.feature.details.mobile.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.error.asString
import su.afk.yummy.tv.core.designsystem.mobile.NotificationPermissionGateHost
import su.afk.yummy.tv.core.designsystem.mobile.rememberNotificationPermissionGate
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.mobile.subscriptions.view.SubscriptionMobileRow
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.subscriptions.SubscriptionsState
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun SubscriptionsMobileScreenDefaultPreview() = ScreenPreviewTheme {
    SubscriptionsMobileScreen(SubscriptionsState.State(isLoading = false), emptyFlow()) {}
}

@Composable
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun SubscriptionsMobileScreenLoadingPreview() = ScreenPreviewTheme {
    SubscriptionsMobileScreen(SubscriptionsState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun SubscriptionsMobileScreenErrorPreview() = ScreenPreviewTheme {
    SubscriptionsMobileScreen(
        SubscriptionsState.State(
            isLoading = false,
            error = UiText.Failure(IOException())
        ), emptyFlow()
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SubscriptionsMobileScreen(
    state: SubscriptionsState.State,
    effect: Flow<SubscriptionsState.Effect>,
    onEvent: (SubscriptionsState.Event) -> Unit,
) {
    val notificationPermissionGate = rememberNotificationPermissionGate()
    BaseBottomSheet(
        onDismissRequest = { onEvent(SubscriptionsState.Event.BackSelected) },
        title = stringResource(R.string.details_subscriptions),
    ) {
        when {
            state.isLoading && state.subscriptions.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                YummyLoadingIndicator()
            }

            state.error != null && state.subscriptions.isEmpty() -> MobileMessage(
                title = state.error?.asString().orEmpty(),
                icon = Icons.Filled.Warning,
                actionLabel = stringResource(CoreR.string.common_retry),
                onAction = { onEvent(SubscriptionsState.Event.RetrySelected) },
                fillMaxSize = false,
            )

            state.subscriptions.isEmpty() -> MobileMessage(
                title = stringResource(R.string.details_subscriptions_empty),
                fillMaxSize = false,
            )

            else -> LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.subscriptions, key = { it.key }) { option ->
                    SubscriptionMobileRow(
                        option = option,
                        onClick = {
                            val toggle = SubscriptionsState.Event.SubscriptionToggled(option.key)
                            // Разрешение спрашиваем в момент ценности — при подписке, не при отписке.
                            if (option.isSubscribed) {
                                onEvent(toggle)
                            } else {
                                notificationPermissionGate { onEvent(toggle) }
                            }
                        },
                    )
                }
            }
        }
    }

    NotificationPermissionGateHost(
        state = notificationPermissionGate,
        permissionWasRequested = state.notificationPermissionRequested,
        onPermissionRequested = { onEvent(SubscriptionsState.Event.NotificationPermissionRequested) },
        explanationRes = CoreR.string.notification_permission_explanation_episode_push,
    )
}
