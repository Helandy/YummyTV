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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.subscriptions.view.SubscriptionMobileRow
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
            error = "Не удалось загрузить подписки"
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
    BaseBottomSheet(
        onDismissRequest = { onEvent(SubscriptionsState.Event.BackSelected) },
        title = stringResource(R.string.details_mobile_subscriptions),
    ) {
        when {
            state.isLoading && state.subscriptions.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.subscriptions.isEmpty() -> MobileMessage(
                title = state.error.orEmpty(),
                icon = Icons.Filled.Warning,
                actionLabel = stringResource(CoreR.string.retry),
                onAction = { onEvent(SubscriptionsState.Event.RetrySelected) },
                fillMaxSize = false,
            )

            state.subscriptions.isEmpty() -> MobileMessage(
                title = stringResource(R.string.details_mobile_subscriptions_empty),
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
                            onEvent(SubscriptionsState.Event.SubscriptionToggled(option.key))
                        },
                    )
                }
            }
        }
    }
}
