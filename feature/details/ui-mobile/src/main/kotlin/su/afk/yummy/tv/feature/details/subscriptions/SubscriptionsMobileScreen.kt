package su.afk.yummy.tv.feature.details.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.subscriptions.view.SubscriptionMobileRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsMobileScreen(
    state: SubscriptionsState.State,
    effect: Flow<SubscriptionsState.Effect>,
    onEvent: (SubscriptionsState.Event) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onEvent(SubscriptionsState.Event.BackSelected) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.details_mobile_subscriptions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            when {
                state.isLoading && state.subscriptions.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                state.error != null && state.subscriptions.isEmpty() -> Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                state.subscriptions.isEmpty() -> Text(
                    text = stringResource(R.string.details_mobile_subscriptions_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
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

            Spacer(Modifier.height(16.dp))
        }
    }
}
